package orionpay.maquinha_simulate.ui.activities

import android.os.Bundle
import android.util.Log
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import orionpay.maquinha_simulate.BuildConfig
import orionpay.maquinha_simulate.OrionBlue
import orionpay.maquinha_simulate.OrionBlueDark
import orionpay.maquinha_simulate.OrionError
import orionpay.maquinha_simulate.OrionPayTheme
import orionpay.maquinha_simulate.OrionSuccess
import orionpay.maquinha_simulate.OrionText
import orionpay.maquinha_simulate.OrionTextLight
import orionpay.maquinha_simulate.OrionWhite
import orionpay.maquinha_simulate.buildTxPayload
import orionpay.maquinha_simulate.config.ApiConfig
import orionpay.maquinha_simulate.data.model.TxResult
import orionpay.maquinha_simulate.domain.enums.TxState
import orionpay.maquinha_simulate.getInternalAuthToken
import orionpay.maquinha_simulate.orionTextFieldColors
import orionpay.maquinha_simulate.sendTransactionRaw
import orionpay.maquinha_simulate.ui.components.DiagCard
import orionpay.maquinha_simulate.ui.components.DiagRow
import orionpay.maquinha_simulate.ui.components.OrionHeader
import java.net.HttpURLConnection
import java.net.URL

class DiagnosticActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!BuildConfig.DEBUG) { finish(); return }
        enableEdgeToEdge()
        setContent {
            OrionPayTheme { DiagnosticScreen(onBack = { finish() }) }
        }
    }
}

@Composable
fun DiagnosticScreen(onBack: () -> Unit) {
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()

    var url        by remember { mutableStateOf(ApiConfig.TRANSACTIONS_AUTHORIZE) }
    var amount     by remember { mutableStateOf("2.14") }
    var merchantId by remember { mutableStateOf(ApiConfig.MERCHANT_ID) }
    var cardNumber by remember { mutableStateOf("5454545454545454") }
    var cardHolder by remember { mutableStateOf("MARIA S OLIVEIRA") }
    var expiry     by remember { mutableStateOf("11/29") }
    var cvv        by remember { mutableStateOf("123") }
    var cardBrand  by remember { mutableStateOf("MASTERCARD") }
    var entryMode  by remember { mutableStateOf("CHIP") }
    var extRef     by remember { mutableStateOf("PEDIDO-TESTE-001") }
    var terminalSn by remember { mutableStateOf("POS-ORION-992") }
    var pin        by remember { mutableStateOf("") }
    var stan       by remember { mutableStateOf("000001") }

    var pingState  by remember { mutableStateOf<String?>(null) }
    var pingOk     by remember { mutableStateOf(false) }
    var pingMs     by remember { mutableStateOf(0L) }

    var txResult   by remember { mutableStateOf<TxResult?>(null) }
    var txLoading  by remember { mutableStateOf(false) }
    var rawResponse by remember { mutableStateOf("") }
    var sentPayload by remember { mutableStateOf("") }

    var idempotencyKey  by remember { mutableStateOf("") }

    if (idempotencyKey.isEmpty()) {
        idempotencyKey = java.util.UUID.randomUUID().toString()
    }

    // Auto-scroll ao receber resultado
    LaunchedEffect(txResult, txLoading) {
        if (txResult != null && !txLoading) {
            delay(100)
            scrollState.animateScrollTo(scrollState.maxValue)
        }
    }

    Box(Modifier.fillMaxSize().background(OrionWhite)) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(bottom = 40.dp)
        ) {
            OrionHeader(showBack = true, onBack = onBack)

            Column(Modifier.padding(horizontal = 20.dp)) {
                Spacer(Modifier.height(20.dp))
                Text("Diagnóstico de API", color = OrionText, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text("Teste de conexão e envio de transação", color = OrionTextLight, fontSize = 14.sp)
                Spacer(Modifier.height(20.dp))

                DiagCard {
                    Text("Endpoint", color = OrionTextLight, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = url, onValueChange = { url = it },
                        colors = orionTextFieldColors(),
                        shape  = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(Modifier.height(10.dp))
                    Button(
                        onClick = {
                            scope.launch {
                                pingState = "testando"
                                val t0 = System.currentTimeMillis()
                                pingOk = withContext(Dispatchers.IO) {
                                    try {
                                        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
                                            requestMethod  = "HEAD"
                                            connectTimeout = ApiConfig.CONNECT_TIMEOUT
                                            readTimeout    = ApiConfig.READ_TIMEOUT
                                        }
                                        conn.connect()
                                        val code = conn.responseCode
                                        conn.disconnect()
                                        code < 600
                                    } catch (_: Exception) {
                                        try {
                                            val conn = (URL(url).openConnection() as HttpURLConnection).apply {
                                                requestMethod  = "POST"
                                                setRequestProperty("Content-Type", "application/json")
                                                connectTimeout = ApiConfig.CONNECT_TIMEOUT
                                                readTimeout    = ApiConfig.READ_TIMEOUT
                                                doOutput = true
                                            }
                                            conn.outputStream.write("{}".toByteArray())
                                            val code = conn.responseCode
                                            conn.disconnect()
                                            code < 600
                                        } catch (_: Exception) { false }
                                    }
                                }
                                pingMs = System.currentTimeMillis() - t0
                                pingState = if (pingOk) "ok" else "erro"
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(44.dp),
                        shape  = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = OrionBlue)
                    ) {
                        if (pingState == "testando") {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(8.dp))
                        }
                        Text("Testar conexão", color = OrionWhite, fontSize = 14.sp)
                    }

                    if (pingState != null && pingState != "testando") {
                        Spacer(Modifier.height(8.dp))
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (pingOk) OrionSuccess.copy(0.1f) else OrionError.copy(0.1f))
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                if (pingOk) Icons.Default.CheckCircle else Icons.Default.Error,
                                contentDescription = null,
                                tint = if (pingOk) OrionSuccess else OrionError,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                if (pingOk) "Servidor respondeu em ${pingMs}ms"
                                else        "Servidor não respondeu (${pingMs}ms) — verifique o ApiConfig",
                                color = if (pingOk) OrionSuccess else OrionError,
                                fontSize = 13.sp
                            )
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))

                DiagCard {
                    Text("Payload de teste", color = OrionTextLight, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(10.dp))

                    DiagRow("Valor (R$)", amount, { amount = it }, KeyboardType.Decimal)
                    Spacer(Modifier.height(8.dp))
                    DiagRow("Número do cartão", cardNumber, { cardNumber = it }, KeyboardType.Number)
                    Spacer(Modifier.height(8.dp))
                    DiagRow("Titular", cardHolder, { cardHolder = it })
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Column(Modifier.weight(1f)) {
                            Text("Validade", color = OrionTextLight, fontSize = 11.sp)
                            Spacer(Modifier.height(4.dp))
                            OutlinedTextField(value = expiry, onValueChange = { expiry = it },
                                colors = orionTextFieldColors(), shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth(), singleLine = true)
                        }
                        Column(Modifier.weight(1f)) {
                            Text("CVV", color = OrionTextLight, fontSize = 11.sp)
                            Spacer(Modifier.height(4.dp))
                            OutlinedTextField(value = cvv, onValueChange = { cvv = it },
                                colors = orionTextFieldColors(), shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth(), singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    DiagRow("PIN (opcional)", pin, { pin = it }, KeyboardType.Number)
                    Spacer(Modifier.height(8.dp))
                    DiagRow("STAN (Campo 11)", stan, { stan = it }, KeyboardType.Number)
                    Spacer(Modifier.height(8.dp))
                    DiagRow("Referência", extRef, { extRef = it })
                }

                Spacer(Modifier.height(14.dp))

                Button(
                    onClick = {
                        scope.launch {
                            txLoading = true
                            txResult  = null
                            rawResponse = ""

                            // Força uma nova transação gerando novas chaves
                            idempotencyKey = java.util.UUID.randomUUID().toString()
                            extRef = "DIAG-${System.currentTimeMillis() % 100000}"

                            val token = getInternalAuthToken()

                            if (token != null) {
                                val amtDouble = amount.toDoubleOrNull() ?: 0.0
                                val finalPin = if (pin == "1234") "1234567890ABCDEF" 
                                              else orionpay.maquinha_simulate.utils.PinBlockUtil.format0(pin, cardNumber)

                                val payload = buildTxPayload(
                                    merchantId, amtDouble, "CREDIT_A_VISTA",
                                    terminalSn, extRef, entryMode,
                                    cardBrand, cardHolder, cardNumber, expiry, cvv,
                                    pinData = finalPin,
                                    stan = stan
                                )

                                sentPayload = payload.toString(2)
                                txResult = sendTransactionRaw(payload, token, idempotencyKey)
                                rawResponse = txResult?.rawJson ?: ""
                            } else {
                                rawResponse = "ERRO: Falha ao obter token de autenticação automática."
                                txResult = TxResult(TxState.ERROR, "Erro de Login")
                            }

                            txLoading = false
                        }
                    },
                    enabled  = !txLoading,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape    = RoundedCornerShape(12.dp),
                    colors   = ButtonDefaults.buttonColors(
                        containerColor         = OrionBlue,
                        disabledContainerColor = Color(0xFFE2E8F0)
                    )
                ) {
                    if (txLoading) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                    }
                    Text(
                        if (txLoading) "Enviando..." else "Enviar transação de teste",
                        fontSize = 15.sp, fontWeight = FontWeight.Bold,
                        color = if (!txLoading) Color.White else OrionTextLight
                    )
                }

                txResult?.let { result ->
                    Spacer(Modifier.height(14.dp))
                    val ok = result.state == TxState.SUCCESS
                    DiagCard(color = if (ok) OrionSuccess else OrionError) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                if (ok) Icons.Default.CheckCircle else Icons.Default.Error,
                                contentDescription = null,
                                tint = if (ok) OrionSuccess else OrionError,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                if (ok) "Transação APROVADA" else "Transação RECUSADA",
                                color = if (ok) OrionSuccess else OrionError,
                                fontSize = 15.sp, fontWeight = FontWeight.Bold
                            )
                        }
                        if (result.message.isNotEmpty()) {
                            Spacer(Modifier.height(6.dp))
                            Text("Mensagem: ${result.message}", color = OrionText, fontSize = 13.sp)
                        }
                        if (result.authCode.isNotEmpty()) {
                            Text("Autorização: ${result.authCode}", color = OrionText, fontSize = 13.sp)
                        }
                        if (result.nsu.isNotEmpty()) {
                            Text("NSU: ${result.nsu}", color = OrionText, fontSize = 13.sp)
                        }
                    }

                    Spacer(Modifier.height(10.dp))

                    DiagCard {
                        Text("Resposta bruta da API", color = OrionTextLight, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFF1F5F9))
                                .padding(12.dp)
                        ) {
                            Text(
                                rawResponse.ifEmpty { "(sem corpo)" },
                                color      = OrionBlueDark,
                                fontSize   = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                lineHeight = 17.sp
                            )
                        }
                    }

                    Spacer(Modifier.height(10.dp))

                    DiagCard {
                        Text("Payload enviado", color = OrionTextLight, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFF1F5F9))
                                .padding(12.dp)
                        ) {
                            Text(
                                sentPayload,
                                color      = OrionSuccess,
                                fontSize   = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                lineHeight = 17.sp
                            )
                        }
                    }
                }
                Spacer(Modifier.height(20.dp))
            }
        }
    }
}
