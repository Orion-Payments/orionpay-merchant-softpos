package orionpay.maquinha_simulate.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import orionpay.maquinha_simulate.*

@Composable
fun StepPin(
    amount: String,
    onConfirm: (String) -> Unit
) {
    var pin by remember { mutableStateOf("") }

    Column(
        Modifier.fillMaxSize().padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(40.dp))
        
        Icon(
            Icons.Default.Lock,
            contentDescription = null,
            tint = OrionBlue,
            modifier = Modifier.size(48.dp)
        )
        
        Spacer(Modifier.height(16.dp))
        
        Text(
            "Digite sua senha",
            color = OrionText,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
        
        Text(
            "Valor: $amount",
            color = OrionTextLight,
            fontSize = 16.sp
        )

        Spacer(Modifier.height(32.dp))

        // Visualização do PIN (bolinhas)
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            repeat(4) { index ->
                val active = pin.length > index
                Box(
                    Modifier
                        .size(16.dp)
                        .background(
                            if (active) OrionBlue else Color(0xFFE2E8F0),
                            RoundedCornerShape(8.dp)
                        )
                )
            }
        }

        Spacer(Modifier.height(40.dp))

        // Teclado Numérico Customizado
        val numbers = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "Limpar", "0", "Confirma")
        
        Column(Modifier.fillMaxWidth()) {
            numbers.chunked(3).forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { label ->
                        Button(
                            onClick = {
                                when (label) {
                                    "Limpar" -> pin = ""
                                    "Confirma" -> if (pin.length >= 4) onConfirm(pin)
                                    else -> if (pin.length < 4) pin += label
                                }
                            },
                            modifier = Modifier.weight(1f).height(64.dp).padding(vertical = 4.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = when(label) {
                                    "Confirma" -> OrionBlue
                                    "Limpar" -> Color.Gray.copy(0.2f)
                                    else -> Color(0xFFF1F5F9)
                                }
                            ),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                        ) {
                            Text(
                                label, 
                                fontSize = 18.sp, 
                                fontWeight = FontWeight.Bold,
                                color = when(label) {
                                    "Confirma" -> Color.White
                                    "Limpar" -> OrionText
                                    else -> OrionText
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
