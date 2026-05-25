package orionpay.maquinha_simulate.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import orionpay.maquinha_simulate.ui.theme.*

@Composable
fun StepAmount(
    formattedAmount: String, rawAmount: String,
    onDigit: (String) -> Unit, onBackspace: () -> Unit,
    onClear: () -> Unit, onNext: () -> Unit,
    onBack: () -> Unit
) {
    Column(Modifier.fillMaxSize().background(OrionWhite)) {
        // Toolbar
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack, 
                null, 
                tint = OrionBlue, 
                modifier = Modifier.size(24.dp).clickable { onBack() }
            )
            Spacer(Modifier.weight(1f))
            Text("OrionPay", color = OrionBlue, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            Icon(
                Icons.AutoMirrored.Filled.HelpOutline, 
                null, 
                tint = OrionBlue, 
                modifier = Modifier.size(24.dp)
            )
        }

        Column(
            Modifier.weight(1f).padding(horizontal = 24.dp).padding(top = 8.dp),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.Top
        ) {
            Text(
                "Qual o valor da venda?",
                color = OrionText, // Agora usando cor escura definida no tema
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "R$ ",
                    color = OrionBlue,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Light
                )
                Text(
                    formattedAmount.replace("R$", "").trim(),
                    color = OrionBlue,
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Barra inferior OrionPay com ação "Próximo"
        Surface(
            color = OrionBlue,
            modifier = Modifier.fillMaxWidth()
        ) {
            val enabled = rawAmount.isNotEmpty() && rawAmount != "0"
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .height(64.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Voltar",
                        tint = Color.White
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable(enabled = enabled) { onNext() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Próximo",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (enabled) Color.White else Color.White.copy(alpha = 0.5f)
                    )
                }

                Icon(
                    Icons.AutoMirrored.Filled.ReceiptLong,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.padding(end = 20.dp).size(24.dp)
                )
            }
        }

        // Teclado numérico
        Column(
            Modifier
                .fillMaxWidth()
                .background(Color(0xFFF1F5F9))
                .padding(bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding())
        ) {
            listOf(
                listOf("1", "2", "3"),
                listOf("4", "5", "6"),
                listOf("7", "8", "9"),
                listOf(".", "0", "⌫")
            ).forEach { row ->
                Row(Modifier.fillMaxWidth()) {
                    row.forEach { key ->
                        NumPadKey(
                            key = key,
                            onTap = { when (key) { "⌫" -> onBackspace(); "." -> {} else -> onDigit(key) } }
                        )
                    }
                }
            }
        }
    }
}
