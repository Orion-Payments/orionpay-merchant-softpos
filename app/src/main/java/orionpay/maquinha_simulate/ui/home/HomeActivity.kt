package orionpay.maquinha_simulate.ui.home

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import orionpay.maquinha_simulate.OrionPayTheme
import orionpay.maquinha_simulate.domain.enums.ProductType
import orionpay.maquinha_simulate.ui.components.OrionHeader
import orionpay.maquinha_simulate.ui.theme.*
import orionpay.maquinha_simulate.ui.transaction.TransactionActivity

class HomeActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            OrionPayTheme {
                MainHomeScreen(
                    onOptionClick = { product ->
                        val intent = Intent(this, TransactionActivity::class.java).apply {
                            putExtra("PRODUCT_TYPE", product.name)
                        }
                        startActivity(intent)
                    }
                )
            }
        }
    }
}

@Composable
fun MainHomeScreen(onOptionClick: (ProductType) -> Unit) {
    var selectedItem by remember { mutableIntStateOf(0) }
    val items = listOf("Vender", "Histórico", "Gestão", "Ajustes")
    val icons = listOf(Icons.Default.Storefront, Icons.Default.History, Icons.Default.BarChart, Icons.Default.Settings)
    val selectedIcons = listOf(Icons.Filled.Storefront, Icons.Filled.History, Icons.Filled.BarChart, Icons.Filled.Settings)

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = OrionWhite,
                tonalElevation = 8.dp
            ) {
                items.forEachIndexed { index, item ->
                    NavigationBarItem(
                        icon = { Icon(if (selectedItem == index) selectedIcons[index] else icons[index], contentDescription = item) },
                        label = { Text(item) },
                        selected = selectedItem == index,
                        onClick = { selectedItem = index },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = OrionBlue,
                            selectedTextColor = OrionBlue,
                            unselectedIconColor = OrionTextLight,
                            unselectedTextColor = OrionTextLight,
                            indicatorColor = OrionBlue.copy(alpha = 0.1f)
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(Modifier.padding(innerPadding)) {
            when (selectedItem) {
                0 -> HomeScreen(onOptionClick)
                else -> PlaceholderScreen(items[selectedItem])
            }
        }
    }
}

@Composable
fun HomeScreen(onOptionClick: (ProductType) -> Unit) {
    var showInstallments by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize().background(OrionBackground)) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            OrionHeader(
                showBack = showInstallments,
                onBack = { showInstallments = false }
            )

            Column(Modifier.padding(horizontal = 24.dp)) {
                Spacer(Modifier.height(24.dp))
                
                if (!showInstallments) {
                    Text(
                        "O que vamos vender hoje?",
                        color = OrionText, fontSize = 22.sp, fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Selecione uma opção de recebimento",
                        color = OrionTextLight, fontSize = 14.sp
                    )

                    Spacer(Modifier.height(32.dp))

                    MenuButton(
                        title = "Crédito à Vista",
                        subtitle = "Pagamento único",
                        icon = Icons.Default.CreditCard,
                        onClick = { onOptionClick(ProductType.CREDIT_AVISTA) }
                    )

                    Spacer(Modifier.height(16.dp))

                    MenuButton(
                        title = "Crédito Parcelado",
                        subtitle = "Venda em até 12x",
                        icon = Icons.Default.Payments,
                        onClick = { showInstallments = true }
                    )

                    Spacer(Modifier.height(16.dp))

                    MenuButton(
                        title = "Débito",
                        subtitle = "Pagamento instantâneo",
                        icon = Icons.Default.AccountBalance,
                        onClick = { onOptionClick(ProductType.DEBIT) }
                    )

                    Spacer(Modifier.height(16.dp))

                    MenuButton(
                        title = "Pix",
                        subtitle = "Receba via QR Code",
                        icon = Icons.Default.Bolt,
                        onClick = { onOptionClick(ProductType.PIX) }
                    )
                } else {
                    Text(
                        "Escolha o parcelamento",
                        color = OrionText, fontSize = 22.sp, fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Selecione a quantidade de parcelas",
                        color = OrionTextLight, fontSize = 14.sp
                    )

                    Spacer(Modifier.height(32.dp))

                    val installments = listOf(
                        ProductType.CREDIT_2X,
                        ProductType.CREDIT_3X,
                        ProductType.CREDIT_6X,
                        ProductType.CREDIT_12X
                    )

                    installments.forEach { product ->
                        MenuButton(
                            title = product.label,
                            subtitle = "Parcelamento no crédito",
                            icon = Icons.Default.AddCard,
                            onClick = { onOptionClick(product) }
                        )
                        Spacer(Modifier.height(12.dp))
                    }
                }
                
                Spacer(Modifier.height(40.dp))
            }
        }
    }
}

@Composable
fun MenuButton(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = OrionWhite,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth(),
        border = BorderStroke(1.dp, OrionBackground)
    ) {
        Row(
            Modifier
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(OrionBlue.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = OrionBlue, modifier = Modifier.size(24.dp))
            }
            
            Spacer(Modifier.width(16.dp))
            
            Column(Modifier.weight(1f)) {
                Text(title, color = OrionText, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text(subtitle, color = OrionTextLight, fontSize = 13.sp)
            }
            
            Icon(Icons.Default.ChevronRight, null, tint = OrionTextLight)
        }
    }
}

@Composable
fun PlaceholderScreen(title: String) {
    Box(Modifier.fillMaxSize().background(OrionBackground), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.Construction, null, modifier = Modifier.size(64.dp), tint = OrionTextLight)
            Spacer(Modifier.height(16.dp))
            Text(title, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = OrionText)
            Text("Em desenvolvimento", color = OrionTextLight)
        }
    }
}
