package orionpay.maquinha_simulate.ui.merchant

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.AddCard
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import orionpay.maquinha_simulate.domain.model.MerchantTransactionDomain
import orionpay.maquinha_simulate.infrastructure.auth.HttpTerminalAuthAdapter
import orionpay.maquinha_simulate.infrastructure.network.HttpMerchantAdapter
import orionpay.maquinha_simulate.ui.theme.*

@Composable
fun MerchantTransactionsScreen(onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    val merchantAdapter = remember { HttpMerchantAdapter() }
    val authAdapter = remember { HttpTerminalAuthAdapter() }
    
    var transactions by remember { mutableStateOf<List<MerchantTransactionDomain>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedTransaction by remember { mutableStateOf<MerchantTransactionDomain?>(null) }

    LaunchedEffect(Unit) {
        scope.launch {
            isLoading = true
            val token = authAdapter.login()
            if (token != null) {
                transactions = merchantAdapter.getTransactions(token, 0, 50)
            }
            isLoading = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(OrionBackground)
    ) {
        Surface(
            color = OrionBlue,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 48.dp, bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = OrionWhite)
                }
                Text(
                    "Transações",
                    color = OrionWhite,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { /* Filtro */ }) {
                    Icon(Icons.Default.FilterList, null, tint = OrionWhite)
                }
            }
        }

        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = OrionBlue)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                item {
                    Spacer(modifier = Modifier.height(24.dp))
                    // Gráfico de Volume nas Transações (Paridade com Web)
                    val chartData = transactions.take(10).map { it.amount }.reversed()
                    if (chartData.isNotEmpty()) {
                        TransactionVolumeChart(chartData)
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }

                item {
                    OutlinedTextField(
                        value = "",
                        onValueChange = {},
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp),
                        placeholder = { Text("Buscar transação...", color = OrionTextLight) },
                        leadingIcon = { Icon(Icons.Default.Search, null, tint = OrionTextLight) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = OrionBlue,
                            unfocusedBorderColor = OrionWhite,
                            focusedContainerColor = OrionWhite,
                            unfocusedContainerColor = OrionWhite,
                            cursorColor = OrionBlue
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                }

                items(transactions) { tx ->
                    Box(modifier = Modifier.padding(horizontal = 24.dp)) {
                        TransactionCard(tx, onClick = { selectedTransaction = tx })
                    }
                }
            }
        }

        if (selectedTransaction != null) {
            TransactionDetailDialog(
                tx = selectedTransaction!!,
                onDismiss = { selectedTransaction = null }
            )
        }
    }
}

@Composable
fun TransactionCard(tx: MerchantTransactionDomain, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
            .clickable { onClick() },
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
                    imageVector = if (tx.brand == "VISA") Icons.Default.CreditCard else Icons.Default.AddCard,
                    contentDescription = null,
                    tint = OrionBlue,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(tx.brand, color = OrionText, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(tx.date, color = OrionTextLight, fontSize = 12.sp)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("R$ ${String.format(java.util.Locale.US, "%.2f", tx.amount)}", color = OrionText, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(
                    text = tx.status,
                    color = if (tx.isSuccess) OrionSuccess else OrionError,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun TransactionDetailDialog(tx: MerchantTransactionDomain, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("FECHAR", color = OrionBlue, fontWeight = FontWeight.Bold)
            }
        },
        title = {
            Text("Detalhes da Venda", color = OrionText, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                DetailRow("NSU", tx.nsu)
                DetailRow("Data", tx.date)
                DetailRow("Bandeira", tx.brand)
                DetailRow("Final Cartão", tx.lastFour)
                DetailRow("Tipo", tx.productType)
                DetailRow("Cod. Autorização", tx.authCode)
                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = OrionBackground)
                DetailRow("Valor Bruto", "R$ ${String.format(java.util.Locale.US, "%.2f", tx.amount)}")
                DetailRow("Valor Líquido", "R$ ${String.format(java.util.Locale.US, "%.2f", tx.netAmount)}")
                DetailRow("Status", tx.status, color = if (tx.isSuccess) OrionSuccess else OrionError)
            }
        },
        containerColor = OrionWhite,
        shape = RoundedCornerShape(24.dp)
    )
}

@Composable
fun DetailRow(label: String, value: String, color: Color = OrionText) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = OrionTextLight, fontSize = 14.sp)
        Text(value, color = color, fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun TransactionVolumeChart(data: List<Double>) {
    Column(modifier = Modifier.padding(horizontal = 24.dp)) {
        Text(
            text = "Volume das Últimas Transações",
            color = OrionText,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(12.dp))
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
            color = OrionWhite,
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, OrionBackground)
        ) {
            androidx.compose.foundation.Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 16.dp, bottom = 8.dp, start = 8.dp, end = 8.dp)
            ) {
                val width = size.width
                val height = size.height
                val maxVal = (data.maxOrNull() ?: 1.0).toFloat() * 1.1f
                val stepX = width / (data.size - 1).coerceAtLeast(1)

                val path = androidx.compose.ui.graphics.Path()
                val fillPath = androidx.compose.ui.graphics.Path()

                data.forEachIndexed { i, value ->
                    val x = i * stepX
                    val y = height - (value.toFloat() / maxVal * height)
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
                    brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                        colors = listOf(OrionBlue.copy(alpha = 0.1f), Color.Transparent)
                    )
                )
                drawPath(
                    path = path,
                    color = OrionBlue,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
                )
            }
        }
    }
}
