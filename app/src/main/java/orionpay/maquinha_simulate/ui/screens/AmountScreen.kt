package orionpay.maquinha_simulate.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import orionpay.maquinha_simulate.ui.components.AmountKeyboard
import orionpay.maquinha_simulate.ui.components.NuButton
import orionpay.maquinha_simulate.ui.components.NuHeader
import orionpay.maquinha_simulate.ui.theme.NuPurple
import orionpay.maquinha_simulate.ui.theme.NuTextMuted
import java.util.Locale

@Composable
fun AmountScreen(
    amount: String,
    onAmountChange: (String) -> Unit,
    onContinue: () -> Unit,
    onDiagnostic: () -> Unit
) {
    val amountDouble = (amount.toLongOrNull() ?: 0L) / 100.0
    val formattedAmount = java.text.NumberFormat.getCurrencyInstance(Locale("pt", "BR"))
        .format(amountDouble)

    Scaffold(
        topBar = { NuHeader(title = "Valor da venda") }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp)
        ) {
            Text(
                text = "Quanto você quer cobrar?",
                fontSize = 16.sp,
                color = NuTextMuted
            )
            
            Text(
                text = formattedAmount,
                fontSize = 40.sp,
                fontWeight = FontWeight.Bold,
                color = NuPurple,
                modifier = Modifier.padding(vertical = 16.dp)
            )

            Spacer(modifier = Modifier.weight(1f))

            AmountKeyboard(
                onDigit = { onAmountChange(amount + it) },
                onBackspace = { if (amount.isNotEmpty()) onAmountChange(amount.dropLast(1)) }
            )

            Spacer(modifier = Modifier.height(24.dp))

            NuButton(
                text = "Continuar",
                onClick = onContinue,
                enabled = amountDouble > 0
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            NuButton(
                text = "Diagnóstico de API",
                onClick = onDiagnostic,
                isSecondary = true
            )
        }
    }
}
