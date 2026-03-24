package orionpay.maquinha_simulate.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import orionpay.maquinha_simulate.data.model.ProductType
import orionpay.maquinha_simulate.data.model.TxState
import orionpay.maquinha_simulate.ui.components.*
import orionpay.maquinha_simulate.ui.theme.*
import orionpay.maquinha_simulate.ui.viewmodel.TransactionViewModel
import java.util.Locale

@Composable
fun PaymentScreen(
    viewModel: TransactionViewModel,
    onBack: () -> Unit,
    onFinish: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val cardData by viewModel.cardData.collectAsState()
    val amount by viewModel.amount.collectAsState()
    val selectedProduct by viewModel.selectedProduct.collectAsState()

    var cvv by remember { mutableStateOf("") }
    
    val amountDouble = (amount.toLongOrNull() ?: 0L) / 100.0
    val formattedAmount = java.text.NumberFormat.getCurrencyInstance(Locale("pt", "BR"))
        .format(amountDouble)

    // Navegar para o resultado quando a transação terminar
    LaunchedEffect(uiState.state) {
        if (uiState.state == TxState.SUCCESS || uiState.state == TxState.ERROR) {
            onFinish()
        }
    }

    Scaffold(
        topBar = { NuHeader(title = "Confirmar Pagamento", onBack = onBack) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp)
        ) {
            Text(text = "Valor", color = NuTextMuted, fontSize = 14.sp)
            Text(text = formattedAmount, fontSize = 32.sp, fontWeight = FontWeight.Bold, color = NuPurple)

            Spacer(Modifier.height(24.dp))

            NuCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CreditCard, contentDescription = null, tint = NuPurple)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(text = cardData.brand, fontWeight = FontWeight.Bold)
                        Text(text = "Final ${cardData.number.takeLast(4)}", color = NuTextMuted)
                        Text(text = cardData.holder, fontSize = 12.sp)
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            Text(text = "Método de pagamento", fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))

            ProductType.entries.forEach { product ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .background(
                            if (selectedProduct == product) NuPurple.copy(alpha = 0.1f) else NuGray,
                            RoundedCornerShape(12.dp)
                        )
                        .clickable { viewModel.onProductSelected(product) }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = selectedProduct == product,
                        onClick = { viewModel.onProductSelected(product) },
                        colors = RadioButtonDefaults.colors(selectedColor = NuPurple)
                    )
                    Text(text = product.label, modifier = Modifier.padding(start = 8.dp))
                }
            }

            Spacer(Modifier.height(24.dp))

            OutlinedTextField(
                value = cvv,
                onValueChange = { if (it.length <= 4) cvv = it },
                label = { Text("CVV") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NuPurple,
                    focusedLabelColor = NuPurple
                )
            )

            Spacer(modifier = Modifier.height(32.dp))

            NuButton(
                text = "Pagar agora",
                onClick = { viewModel.processTransaction(cvv) },
                isLoading = uiState.state == TxState.LOADING,
                enabled = cvv.length >= 3
            )
        }
    }
}
