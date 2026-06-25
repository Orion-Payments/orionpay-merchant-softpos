package orionpay.maquinha_simulate.ui.merchant

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import java.util.Locale
import orionpay.maquinha_simulate.config.ApiConfig
import orionpay.maquinha_simulate.domain.model.MerchantDomain
import orionpay.maquinha_simulate.domain.model.MerchantSummaryDomain
import orionpay.maquinha_simulate.domain.model.MerchantTransactionDomain
import orionpay.maquinha_simulate.infrastructure.auth.HttpTerminalAuthAdapter
import orionpay.maquinha_simulate.infrastructure.network.HttpMerchantAdapter
import orionpay.maquinha_simulate.ui.theme.*

@Composable
fun MerchantDashboard() {
    var currentSubScreen by remember { mutableStateOf("dashboard") }
    val scope = rememberCoroutineScope()
    val merchantAdapter = remember { HttpMerchantAdapter() }
    val authAdapter = remember { HttpTerminalAuthAdapter() }
    
    var summary by remember { mutableStateOf<MerchantSummaryDomain?>(null) }
    var merchantInfo by remember { mutableStateOf<MerchantDomain?>(null) }
    var transactions by remember { mutableStateOf<List<MerchantTransactionDomain>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        scope.launch {
            isLoading = true
            val token = authAdapter.login()
            if (token != null) {
                val summaryJob = async { merchantAdapter.getSummary(token) }
                val transactionsJob = async { merchantAdapter.getTransactions(token) }
                val merchantJob = async { merchantAdapter.getMerchantMe(token) }
                
                summary = summaryJob.await()
                transactions = transactionsJob.await()
                merchantInfo = merchantJob.await()
            }
            isLoading = false
        }
    }

    Crossfade(targetState = currentSubScreen, label = "merchant_nav") { screen ->
        when (screen) {
            "dashboard" -> {
                if (isLoading) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = OrionBlue)
                    }
                } else {
                    DashboardContent(
                        summary = summary,
                        merchant = merchantInfo,
                        recentTransactions = transactions,
                        onNavigate = { currentSubScreen = it }
                    )
                }
            }
            "transactions" -> MerchantTransactionsScreen(onBack = { currentSubScreen = "dashboard" })
            "terminals" -> MerchantTerminalsScreen(onBack = { currentSubScreen = "dashboard" })
            "anticipation" -> MerchantAnticipationScreen(onBack = { currentSubScreen = "dashboard" })
            "support" -> MerchantSupportScreen(onBack = { currentSubScreen = "dashboard" })
            "settings" -> MerchantSettingsScreen(onBack = { currentSubScreen = "dashboard" })
            else -> PlaceholderMerchantScreen(screen) { currentSubScreen = "dashboard" }
        }
    }
}

@Composable
private fun DashboardContent(
    summary: MerchantSummaryDomain?,
    merchant: MerchantDomain?,
    recentTransactions: List<MerchantTransactionDomain>,
    onNavigate: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(OrionBackground)
            .verticalScroll(rememberScrollState())
    ) {
        DashboardHeader(merchant?.name)
        
        Spacer(modifier = Modifier.height(24.dp))
        
        QuickIndicators(summary)
        
        Spacer(modifier = Modifier.height(24.dp))
        
        VolumeChart(summary?.chartData ?: emptyList())
        
        Spacer(modifier = Modifier.height(32.dp))
        
        QuickActions(onNavigate)
        
        Spacer(modifier = Modifier.height(32.dp))
        
        RecentTransactions(
            transactions = recentTransactions,
            onViewAll = { onNavigate("transactions") }
        )
        
        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun DashboardHeader(merchantName: String?) {
    Surface(
        color = OrionBlue,
        shape = RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 24.dp, end = 24.dp, top = 48.dp, bottom = 32.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Olá, ${merchantName?.split(" ")?.firstOrNull() ?: "Lojista"}",
                    color = OrionWhite.copy(alpha = 0.8f),
                    fontSize = 14.sp
                )
                Text(
                    text = "Portal do Lojista",
                    color = OrionWhite,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(OrionWhite.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Notifications,
                    contentDescription = "Notificações",
                    tint = OrionWhite,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

@Composable
private fun QuickIndicators(summary: MerchantSummaryDomain?) {
    var showValues by remember { mutableStateOf(true) }
    
    Column(modifier = Modifier.padding(horizontal = 24.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Indicadores de Hoje",
                color = OrionText,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
            IconButton(onClick = { showValues = !showValues }) {
                Icon(
                    imageVector = if (showValues) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                    contentDescription = null,
                    tint = OrionBlue
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(end = 24.dp)
        ) {
            item {
                IndicatorCard(
                    title = "SALDO DISPONÍVEL",
                    value = "R$ ${String.format(Locale.US, "%.2f", summary?.availableBalance ?: 0.0)}",
                    showValue = showValues,
                    trend = "Pronto para saque",
                    icon = Icons.Default.AccountBalanceWallet,
                    color = OrionSuccess
                )
            }
            item {
                IndicatorCard(
                    title = "A RECEBER",
                    value = "R$ ${String.format(Locale.US, "%.2f", summary?.toReceive ?: 0.0)}",
                    showValue = showValues,
                    trend = "Lançamentos futuros",
                    icon = Icons.Default.PendingActions,
                    color = OrionBlue
                )
            }
            item {
                IndicatorCard(
                    title = "VOLUME TOTAL (TPV)",
                    value = "R$ ${String.format(Locale.US, "%.2f", summary?.totalVolume ?: 0.0)}",
                    showValue = showValues,
                    trend = "Acumulado do Mês",
                    icon = Icons.Default.Payments,
                    color = OrionBlue
                )
            }
            item {
                IndicatorCard(
                    title = "RECEITA LÍQUIDA",
                    value = "R$ ${String.format(Locale.US, "%.2f", summary?.netRevenue ?: 0.0)}",
                    showValue = showValues,
                    trend = "Acumulado do Mês",
                    icon = Icons.Default.Description,
                    color = OrionBlue
                )
            }
            item {
                IndicatorCard(
                    title = "TICKET MÉDIO",
                    value = "R$ ${String.format(Locale.US, "%.2f", summary?.averageTicket ?: 0.0)}",
                    showValue = showValues,
                    trend = "Acumulado do Mês",
                    icon = Icons.Default.ConfirmationNumber,
                    color = OrionWarning
                )
            }
            item {
                IndicatorCard(
                    title = "TAXA DE APROVAÇÃO",
                    value = "${String.format(Locale.US, "%.1f", summary?.approvalRate ?: 0.0)}%",
                    showValue = true,
                    trend = "Acumulado do Mês",
                    icon = Icons.Default.CheckCircle,
                    color = OrionSuccess
                )
            }
        }
    }
}

@Composable
private fun IndicatorCard(
    title: String,
    value: String,
    showValue: Boolean,
    trend: String,
    icon: ImageVector,
    color: Color
) {
    Surface(
        modifier = Modifier
            .width(170.dp)
            .height(180.dp),
        color = OrionWhite,
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, OrionBackground)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(color.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = color, modifier = Modifier.size(20.dp))
            }
            
            Column {
                Text(title, color = OrionTextLight, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Text(
                    text = if (showValue) value else "••••••",
                    color = OrionText,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Text(trend, color = color, fontSize = 11.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun VolumeChart(data: List<Double>) {
    if (data.isEmpty()) return

    Column(modifier = Modifier.padding(horizontal = 24.dp)) {
        Text(
            text = "Volume de Vendas (7 dias)",
            color = OrionText,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
            color = OrionWhite,
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, OrionBackground)
        ) {
            Box(modifier = Modifier.padding(24.dp)) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val width = size.width
                    val height = size.height
                    val maxVal = (data.maxOrNull() ?: 1.0) * 1.2
                    val stepX = width / (data.size - 1).coerceAtLeast(1)
                    
                    val path = Path()
                    val fillPath = Path()
                    
                    data.forEachIndexed { i, value ->
                        val x = i * stepX
                        val y = height - (value.toFloat() / maxVal.toFloat() * height)
                        
                        if (i == 0) {
                            path.moveTo(x, y)
                            fillPath.moveTo(x, height)
                            fillPath.lineTo(x, y)
                        } else {
                            path.lineTo(x, y)
                            fillPath.lineTo(x, y)
                        }
                        
                        if (i == data.size - 1) {
                            fillPath.lineTo(x, height)
                            fillPath.close()
                        }
                    }
                    
                    drawPath(
                        path = fillPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                OrionBlue.copy(alpha = 0.15f),
                                Color.Transparent
                            )
                        )
                    )
                    
                    drawPath(
                        path = path,
                        color = OrionBlue,
                        style = Stroke(width = 3.dp.toPx())
                    )
                    
                    data.forEachIndexed { i, value ->
                        val x = i * stepX
                        val y = height - (value.toFloat() / maxVal.toFloat() * height)
                        drawCircle(
                            color = OrionBlue,
                            radius = 4.dp.toPx(),
                            center = Offset(x, y)
                        )
                        drawCircle(
                            color = OrionWhite,
                            radius = 2.dp.toPx(),
                            center = Offset(x, y)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickActions(onNavigate: (String) -> Unit) {
    Column(modifier = Modifier.padding(horizontal = 24.dp)) {
        Text(
            text = "Acesso Rápido",
            color = OrionText,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            QuickActionButton(
                modifier = Modifier.weight(1f),
                title = "Antecipar",
                icon = Icons.Default.FlashOn,
                color = OrionSuccess,
                onClick = { onNavigate("anticipation") }
            )
            QuickActionButton(
                modifier = Modifier.weight(1f),
                title = "Terminais",
                icon = Icons.Default.PointOfSale,
                color = OrionBlue,
                onClick = { onNavigate("terminals") }
            )
            QuickActionButton(
                modifier = Modifier.weight(1f),
                title = "Suporte",
                icon = Icons.Default.SupportAgent,
                color = OrionWarning,
                onClick = { onNavigate("support") }
            )
        }
    }
}

@Composable
private fun QuickActionButton(
    modifier: Modifier = Modifier,
    title: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit = {}
) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        color = OrionWhite,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, OrionBackground)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = color, modifier = Modifier.size(24.dp))
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(title, color = OrionText, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun RecentTransactions(
    transactions: List<MerchantTransactionDomain>,
    onViewAll: () -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 24.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Últimas Vendas",
                color = OrionText,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
            TextButton(onClick = onViewAll) {
                Text(
                    text = "Ver todas",
                    color = OrionBlue,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        transactions.take(6).forEach { tx ->
            TransactionItem(
                brand = tx.brand,
                amount = "R$ ${String.format(Locale.US, "%.2f", tx.amount)}",
                date = tx.date,
                status = tx.status,
                isError = !tx.isSuccess
            )
        }
    }
}

@Composable
private fun TransactionItem(
    brand: String,
    amount: String,
    date: String,
    status: String,
    isError: Boolean = false
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp),
        color = OrionWhite,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, OrionBackground)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(OrionBackground),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (brand == "VISA") Icons.Default.CreditCard else Icons.Default.AddCard,
                    contentDescription = null,
                    tint = OrionBlue,
                    modifier = Modifier.size(22.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(brand, color = OrionText, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text(date, color = OrionTextLight, fontSize = 12.sp)
            }
            
            Column(horizontalAlignment = Alignment.End) {
                Text(amount, color = OrionText, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text(
                    text = status,
                    color = if (isError) OrionError else OrionSuccess,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun PlaceholderMerchantScreen(title: String, onBack: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().background(OrionBackground),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            color = OrionBlue,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp)
        ) {
            Row(
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 48.dp, bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = OrionWhite)
                }
                Text(title.uppercase(), color = OrionWhite, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        }
        
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Construction, null, modifier = Modifier.size(64.dp), tint = OrionTextLight)
                Spacer(Modifier.height(16.dp))
                Text("Módulo $title", color = OrionText, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text("Em desenvolvimento para o Portal", color = OrionTextLight)
            }
        }
    }
}
