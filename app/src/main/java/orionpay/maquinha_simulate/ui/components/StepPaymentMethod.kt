package orionpay.maquinha_simulate.ui.components

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import orionpay.maquinha_simulate.domain.enums.ProductType
import orionpay.maquinha_simulate.ui.theme.*

@Composable
fun StepPaymentMethod(
    amount: String, selected: ProductType,
    onSelect: (ProductType) -> Unit, onNext: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // Toolbar
        Row(
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                null,
                tint = OrionBlue,
                modifier = Modifier
                    .size(24.dp)
                    .clickable { onBack() })
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
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            Column(Modifier.padding(horizontal = 20.dp)) {
                Spacer(Modifier.height(16.dp))
                Text(
                    "Escolha a forma de pagamento",
                    color = OrionText, 
                    fontSize = 20.sp, 
                    fontWeight = FontWeight.Bold,
                    lineHeight = 26.sp
                )

                Spacer(Modifier.height(24.dp))
                Text("Valor da venda", color = OrionTextLight, fontSize = 14.sp)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        amount.replace("R$", "").trim(),
                        color = OrionBlue, 
                        fontSize = 32.sp, 
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        "BRL",
                        color = OrionTextLight,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    IconButton(onClick = { onBack() }) {
                        Icon(
                            Icons.Default.Edit,
                            "Editar",
                            tint = OrionBlue,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))
                HorizontalDivider(color = Color(0xFFEDF2F7), thickness = 1.dp)
                Spacer(Modifier.height(24.dp))

                Text(
                    "Forma de pagamento do seu cliente",
                    color = OrionText,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(12.dp))
            }

            // Lista de produtos
            Column(Modifier.padding(horizontal = 20.dp)) {
                ProductType.entries.filter { it.apiKey != "PIX" }.forEach { pt ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                            .clickable { onSelect(pt) },
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(
                            1.dp,
                            if (pt == selected) OrionBlue else Color(0xFFEDF2F7)
                        )
                    ) {
                        Row(
                            Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = CircleShape,
                                border = BorderStroke(1.dp, Color(0xFFEDF2F7)),
                                modifier = Modifier.size(40.dp),
                                color = Color.White
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        when {
                                            pt.apiKey.contains("DEBIT") -> Icons.Default.AccountBalance
                                            else -> Icons.Default.CreditCard
                                        },
                                        contentDescription = null,
                                        tint = OrionBlue,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Spacer(Modifier.width(16.dp))
                            Text(
                                pt.label,
                                color = OrionText,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.weight(1f)
                            )
                            RadioButton(
                                selected = pt == selected, 
                                onClick = { onSelect(pt) },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = OrionBlue,
                                    unselectedColor = Color(0xFFD1D5DB)
                                )
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(32.dp))
        }

        // Barra inferior OrionPay com ação "Vender"
        Surface(
            color = OrionBlue,
            modifier = Modifier.fillMaxWidth()
        ) {
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
                        .clickable { onNext() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Vender",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
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
    }
}
