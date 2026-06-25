package orionpay.maquinha_simulate.ui.merchant

import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import orionpay.maquinha_simulate.config.ApiConfig
import orionpay.maquinha_simulate.domain.model.AnticipationDetailsDomain
import orionpay.maquinha_simulate.domain.model.MerchantSummaryDomain
import orionpay.maquinha_simulate.infrastructure.auth.HttpTerminalAuthAdapter
import orionpay.maquinha_simulate.infrastructure.network.HttpMerchantAdapter
import orionpay.maquinha_simulate.ui.theme.*
import java.util.Locale

@Composable
fun MerchantAnticipationScreen(onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    val merchantAdapter = remember { HttpMerchantAdapter() }
    val authAdapter = remember { HttpTerminalAuthAdapter() }
    
    var anticipationDetails by remember { mutableStateOf<AnticipationDetailsDomain?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var isConfirming by remember { mutableStateOf(false) }
    val inputAmountState = remember { mutableStateOf("") }
    var inputAmount by inputAmountState

    LaunchedEffect(Unit) {
        isLoading = true
        try {
            val token = authAdapter.login()
            if (token != null) {
                Log.d("ORION_ANTICIPATION", "Token obtido, buscando detalhes...")
                
                // Primeiro busca o MerchantId real do token
                val me = merchantAdapter.getMerchantMe(token)
                val merchantId = if (!me?.id.isNullOrBlank()) me?.id!! else ApiConfig.MERCHANT_ID
                Log.d("ORION_ANTICIPATION", "Usando Merchant ID: $merchantId (obtido: ${me?.id})")
                
                val details = merchantAdapter.getAnticipationDetails(token, merchantId)
                anticipationDetails = details
                Log.d("ORION_ANTICIPATION", "Detalhes: $details")
                // Inicializa o input com o valor total disponível se houver
                if (details != null && details.totalGross > 0) {
                    val initialVal = String.format(Locale.US, "%.2f", details.totalGross)
                    inputAmount = initialVal
                    Log.d("ORION_ANTICIPATION", "inputAmount inicializado com: $initialVal")
                }
            } else {
                Log.e("ORION_ANTICIPATION", "Falha ao obter token de autenticação")
            }
        } catch (e: Exception) {
            Log.e("ORION_ANTICIPATION", "Erro no LaunchedEffect", e)
        } finally {
            isLoading = false
        }
    }

    val totalAvailable = anticipationDetails?.totalGross ?: 0.0
    
    // Calcula o valor a antecipar tratando o texto do input
    val amountToAnticipate = remember(inputAmount) {
        inputAmount.replace(",", ".").toDoubleOrNull() ?: 0.0
    }
    
    // Proporção de custo baseada no retorno da API ou 2.5% como fallback
    val costRatio = remember(anticipationDetails) {
        val details = anticipationDetails
        if (details != null && details.totalGross > 0) details.totalCost / details.totalGross else 0.025
    }
    
    val currentFee = amountToAnticipate * costRatio
    val currentNet = amountToAnticipate - currentFee
    
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
                    "Antecipação",
                    color = OrionWhite,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = OrionBlue)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Spacer(modifier = Modifier.height(24.dp))

                Surface(
                    color = OrionWhite,
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.fillMaxWidth(),
                    border = BorderStroke(1.dp, OrionBackground)
                ) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        Text("Valor elegível total", color = OrionTextLight, fontSize = 14.sp)
                        Text(
                            "R$ ${String.format(Locale.US, "%.2f", totalAvailable)}",
                            color = OrionText,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        LinearProgressIndicator(
                            progress = { 1f },
                            modifier = Modifier.fillMaxWidth(),
                            color = OrionSuccess,
                            trackColor = OrionBackground
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                Text("Simulador de Taxas", color = OrionText, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text("Quanto você deseja antecipar?", color = OrionTextLight, fontSize = 14.sp)

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = inputAmount,
                    onValueChange = { newValue ->
                        Log.d("ORION_ANTICIPATION", "Input change: $newValue")
                        // Aceita apenas números e um único separador decimal (ponto ou vírgula)
                        val sanitized = newValue.replace(',', '.')
                        if (sanitized.isEmpty() || (sanitized.count { it == '.' } <= 1 && sanitized.all { it.isDigit() || it == '.' })) {
                            inputAmount = sanitized
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Valor para antecipar", color = OrionTextLight) },
                    placeholder = { Text("0.00", color = OrionTextLight) },
                    prefix = { Text("R$ ", color = OrionText, fontWeight = FontWeight.Bold) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = OrionBlue,
                        unfocusedBorderColor = OrionBackground,
                        focusedTextColor = OrionText,
                        unfocusedTextColor = OrionText,
                        focusedContainerColor = OrionWhite,
                        unfocusedContainerColor = OrionWhite,
                        cursorColor = OrionBlue
                    ),
                    shape = RoundedCornerShape(12.dp)
                )

                if (totalAvailable > 0 && amountToAnticipate > totalAvailable) {
                    Text(
                        "Valor excede o disponível (R$ ${String.format(Locale.US, "%.2f", totalAvailable)})",
                        color = OrionError,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                SummaryRow("Valor Bruto", "R$ ${String.format(Locale.US, "%.2f", amountToAnticipate)}")
                SummaryRow("Custo da Operação", "- R$ ${String.format(Locale.US, "%.2f", currentFee)}")
                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = OrionBackground)
                SummaryRow("Valor Líquido a Receber", "R$ ${String.format(Locale.US, "%.2f", currentNet)}", isTotal = true)

                if (anticipationDetails?.items?.isNotEmpty() == true) {
                    Spacer(modifier = Modifier.height(32.dp))
                    Text("Detalhamento dos Lotes", color = OrionText, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    anticipationDetails!!.items.forEach { item ->
                        LoteItem(item)
                    }
                }

                Spacer(modifier = Modifier.height(40.dp))

                Button(
                    onClick = {
                        scope.launch {
                            isConfirming = true
                            val token = authAdapter.login()
                            if (token != null) {
                                val success = merchantAdapter.requestAnticipation(token, amountToAnticipate)
                                if (success) {
                                    onBack()
                                }
                            }
                            isConfirming = false
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = OrionSuccess),
                    shape = RoundedCornerShape(16.dp),
                    enabled = !isConfirming && amountToAnticipate > 0 && amountToAnticipate <= totalAvailable
                ) {
                    if (isConfirming) {
                        CircularProgressIndicator(color = OrionWhite, modifier = Modifier.size(24.dp))
                    } else {
                        Text("CONFIRMAR ANTECIPAÇÃO", fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Info, null, tint = OrionWarning, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "O valor será creditado em sua conta em até 1 hora.",
                        color = OrionTextLight,
                        fontSize = 12.sp
                    )
                }
                
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
fun LoteItem(item: orionpay.maquinha_simulate.domain.model.AnticipationItemDomain) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
        color = OrionWhite,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, OrionBackground)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Vencimento: ${item.date.take(10)}",
                    color = OrionText,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Text(
                    text = "ID: ${item.settlementId}",
                    color = OrionTextLight,
                    fontSize = 12.sp
                )
                if (item.isBlocked) {
                    Text(
                        text = item.reason ?: "Bloqueado",
                        color = OrionError,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "R$ ${String.format(Locale.US, "%.2f", item.grossAmount)}",
                    color = OrionText,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Text(
                    text = "${item.days} dias",
                    color = OrionBlue,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun SummaryRow(label: String, value: String, isTotal: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = if (isTotal) OrionText else OrionTextLight, fontSize = if (isTotal) 16.sp else 14.sp, fontWeight = if (isTotal) FontWeight.Bold else FontWeight.Normal)
        Text(value, color = if (isTotal) OrionSuccess else OrionText, fontSize = if (isTotal) 18.sp else 14.sp, fontWeight = FontWeight.Bold)
    }
}
