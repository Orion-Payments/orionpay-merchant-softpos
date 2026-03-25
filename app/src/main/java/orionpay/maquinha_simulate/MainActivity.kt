package orionpay.maquinha_simulate

import android.content.Intent
import orionpay.maquinha_simulate.BuildConfig
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.IsoDep
import android.os.Bundle
import android.util.Log
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.input.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.*
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.util.*

// =============================================================================
// CORES ORIONPAY
// =============================================================================
val OrionNavy      = Color(0xFF0D1B2A)
val OrionNavyMid   = Color(0xFF132338)
val OrionNavyLight = Color(0xFF1C3050)
val OrionBlue      = Color(0xFF2563EB)
val OrionBlueDark  = Color(0xFF1D4ED8)
val OrionText      = Color(0xFFFFFFFF)
val OrionTextMuted = Color(0xFF94A3B8)
val OrionTextSub   = Color(0xFF64748B)
val OrionSuccess   = Color(0xFF10B981)
val OrionError     = Color(0xFFEF4444)
val OrionWarning   = Color(0xFFF59E0B)
val OrionGreenTap  = Color(0xFF10C97A)   // cor do efeito toque nos números

// =============================================================================
// ENUMS E DATA CLASSES
// =============================================================================
enum class ProductType(val label: String, val apiKey: String) {
    CREDIT_AVISTA("Crédito à vista", "CREDIT_A_VISTA"),
    CREDIT_2X("Crédito 2x",          "CREDIT_PARCELADO_2"),
    CREDIT_3X("Crédito 3x",          "CREDIT_PARCELADO_3"),
    CREDIT_6X("Crédito 6x",          "CREDIT_PARCELADO_6"),
    CREDIT_12X("Crédito 12x",        "CREDIT_PARCELADO_12"),
    DEBIT("Débito",                   "DEBIT"),
    PIX("Pix",                        "PIX"),
}

enum class TxState { IDLE, LOADING, SUCCESS, ERROR, RETRYING }

// Erros que permitem retry com a mesma chave de idempotência
fun TxResult.isRetryable(): Boolean =
    state == TxState.ERROR && (
            message.contains("Timeout",       ignoreCase = true) ||
                    message.contains("timeout",       ignoreCase = true) ||
                    message.contains("indisponível",  ignoreCase = true) ||
                    message.contains("conectar",      ignoreCase = true) ||
                    message.contains("conexão",       ignoreCase = true) ||
                    message.contains("ConnectException", ignoreCase = true) ||
                    message.contains("SocketTimeout", ignoreCase = true)
            )

data class TxResult(
    val state: TxState = TxState.IDLE,
    val message: String = "",
    val authCode: String = "",
    val nsu: String = "",
    val transactionId: String = "",   // ID retornado pela API para usar no endpoint de e-mail
    val rawJson: String = ""
)

data class Tlv(val tag: String, val length: Int, val value: ByteArray)

data class ComprovanteData(
    val amount: String,
    val product: String,
    val brand: String,
    val maskedPan: String,
    val holder: String,
    val authCode: String,
    val nsu: String,
    val transactionId: String,        // usado no endpoint de envio de e-mail
    val dateTime: String,
    val terminalSn: String,
    val merchantId: String,
    val entryMode: String,
    val message: String
)
data class AflEntry(val sfi: Int, val start: Int, val end: Int)

// =============================================================================
// MAINACTIVITY — LEITOR NFC
// =============================================================================
// Objeto global para comunicação NFC → TransactionActivity
object NfcDataBus {
    var onCardRead: ((CardData) -> Unit)? = null
    var onCardError: ((String) -> Unit)? = null
}

data class CardData(
    val brand: String,
    val holder: String,
    val panRaw: String,
    val expiry: String,
    val cvv2: String,
    val entryMode: String,
    val cryptogram: String,
    val atc: String,
    val iad: String,
    val aip: String,
    val tvr: String
)

class MainActivity : FragmentActivity(), NfcAdapter.ReaderCallback {

    private var nfcAdapter: NfcAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        // Exibe splash antes de ir para a tela de venda
        startActivity(Intent(this, SplashActivity::class.java))
        finish()
    }

    override fun onResume() {
        super.onResume()
        nfcAdapter?.enableReaderMode(
            this, this,
            NfcAdapter.FLAG_READER_NFC_A or
                    NfcAdapter.FLAG_READER_NFC_B or
                    NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK,
            null
        )
    }

    override fun onPause() {
        super.onPause()
        nfcAdapter?.disableReaderMode(this)
    }

    override fun onTagDiscovered(tag: Tag) {
        val isoDep = IsoDep.get(tag) ?: return
        try {
            isoDep.connect()
            isoDep.timeout = 5000
            val card = readEmvCard(isoDep)
            if (card != null) NfcDataBus.onCardRead?.invoke(card)
            else NfcDataBus.onCardError?.invoke("Cartão não suportado")
        } catch (e: Exception) {
            NfcDataBus.onCardError?.invoke(e.message ?: "Erro NFC")
        } finally {
            try { isoDep.close() } catch (_: Exception) {}
        }
    }
}

// Leitura EMV — usada pelo MainActivity e pela TransactionActivity
fun readEmvCard(isoDep: IsoDep): CardData? {
    var aid: String? = null
    val ppseResp = transceiveSafe(isoDep, apduSelect("2PAY.SYS.DDF01"))
    if (ppseResp != null && isSw9000(ppseResp))
        aid = extractAid(flattenTlv(parseTlv(ppseResp.dropSw())))
    if (aid == null) {
        for (candidate in knownAids) {
            val resp = transceiveSafe(isoDep, apduSelectHex(candidate)) ?: continue
            if (isSw9000(resp)) { aid = candidate; break }
        }
    }
    if (aid == null) return null

    var selectResp = transceive(isoDep, apduSelectHex(aid))
    selectResp = getResponse(isoDep, selectResp)
    if (!isSw9000(selectResp)) return null

    val pdol = findTag(flattenTlv(parseTlv(selectResp.dropSw())), "9F38")
    val gpoResp = getResponse(isoDep, transceive(isoDep, buildGpo(pdol)))
    if (!isSw9000(gpoResp)) return null

    val allTlvs = mutableListOf<Tlv>()
    val afl = extractAflFromGpo(gpoResp.dropSw())
    if (afl != null) {
        for (entry in parseAfl(afl)) {
            for (rec in entry.start..entry.end) {
                var resp = transceiveSafe(isoDep, buildReadRecord(rec, entry.sfi)) ?: continue
                resp = getResponse(isoDep, resp)
                if (isSw9000(resp)) allTlvs.addAll(flattenTlv(parseTlv(resp.dropSw())))
            }
        }
    } else {
        for (sfi in listOf(1, 2)) {
            for (rec in 1..4) {
                var resp = transceiveSafe(isoDep, buildReadRecord(rec, sfi)) ?: continue
                resp = getResponse(isoDep, resp)
                if (isSw9000(resp)) allTlvs.addAll(flattenTlv(parseTlv(resp.dropSw())))
            }
        }
    }

    val pan    = findPan(allTlvs)
    val track2 = findTag(allTlvs, "57")?.toHex()
    val panRaw = pan?.toHex()?.trimEnd('F','f')?.filter { it.isLetterOrDigit() }
        ?: track2?.let { extractPanFromTrack2(it) } ?: ""
    val name   = findTag(allTlvs, "5F20")?.toAscii()
        ?.trim()?.replace(Regex("[\u0000-\u001F\u007F-\uFFFF]"), "")?.trim() ?: ""

    return CardData(
        brand      = identifyBrand(aid),
        holder     = name,
        panRaw     = panRaw,
        expiry     = findTag(allTlvs, "5F24")?.toHex()?.let { formatExpiry(it) } ?: "",
        cvv2       = track2?.let { extractCvv2FromTrack2(it) } ?: "",
        entryMode  = if (findTag(allTlvs, "56") != null) "MANUAL" else "CHIP",
        cryptogram = findTag(allTlvs, "9F26")?.toHex() ?: "",
        atc        = findTag(allTlvs, "9F36")?.toHex() ?: "",
        iad        = findTag(allTlvs, "9F10")?.toHex() ?: "",
        aip        = findTag(allTlvs, "82")?.toHex() ?: "",
        tvr        = findTag(allTlvs, "95")?.toHex() ?: ""
    )
}
// =============================================================================
// SPLASHACTIVITY — TELA DE ENTRADA ANIMADA
// =============================================================================
class SplashActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            OrionPayTheme {
                SplashScreen {
                    startActivity(Intent(this, TransactionActivity::class.java))
                    finish()
                }
            }
        }
    }
}

@Composable
fun SplashScreen(onFinished: () -> Unit) {
    // Animações escalonadas
    val logoScale     = remember { Animatable(0f) }
    val logoAlpha     = remember { Animatable(0f) }
    val nameAlpha     = remember { Animatable(0f) }
    val nameOffsetY   = remember { Animatable(24f) }
    val taglineAlpha  = remember { Animatable(0f) }
    val ringScale1    = remember { Animatable(0f) }
    val ringAlpha1    = remember { Animatable(0f) }
    val ringScale2    = remember { Animatable(0f) }
    val ringAlpha2    = remember { Animatable(0f) }
    val bgAlpha       = remember { Animatable(1f) }

    LaunchedEffect(Unit) {
        // 1. Anéis expandindo
        launch {
            delay(100)
            ringScale1.animateTo(1f, tween(600, easing = FastOutSlowInEasing))
        }
        launch {
            delay(100)
            ringAlpha1.animateTo(1f, tween(400))
        }
        launch {
            delay(280)
            ringScale2.animateTo(1f, tween(700, easing = FastOutSlowInEasing))
        }
        launch {
            delay(280)
            ringAlpha2.animateTo(0.5f, tween(500))
        }
        // 2. Logo aparece com bounce
        delay(300)
        launch {
            logoScale.animateTo(
                1.15f, tween(350, easing = FastOutSlowInEasing)
            )
            logoScale.animateTo(1f, tween(150, easing = FastOutSlowInEasing))
        }
        launch { logoAlpha.animateTo(1f, tween(300)) }
        // 3. Nome desliza para cima
        delay(500)
        launch { nameAlpha.animateTo(1f, tween(400)) }
        launch { nameOffsetY.animateTo(0f, tween(400, easing = FastOutSlowInEasing)) }
        // 4. Tagline aparece
        delay(800)
        taglineAlpha.animateTo(1f, tween(400))
        // 5. Aguarda e faz fade-out
        delay(1200)
        bgAlpha.animateTo(0f, tween(500))
        onFinished()
    }

    Box(
        Modifier
            .fillMaxSize()
            .graphicsLayer { alpha = bgAlpha.value }
            .background(OrionNavy),
        contentAlignment = Alignment.Center
    ) {
        // Anel externo
        Box(
            Modifier
                .size(260.dp)
                .graphicsLayer {
                    scaleX = ringScale2.value
                    scaleY = ringScale2.value
                    alpha  = ringAlpha2.value
                }
                .clip(RoundedCornerShape(130.dp))
                .background(OrionGreenTap.copy(alpha = 0.08f))
        )
        // Anel médio
        Box(
            Modifier
                .size(190.dp)
                .graphicsLayer {
                    scaleX = ringScale1.value
                    scaleY = ringScale1.value
                    alpha  = ringAlpha1.value
                }
                .clip(RoundedCornerShape(95.dp))
                .background(OrionGreenTap.copy(alpha = 0.14f))
        )

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // Logo
            Box(
                Modifier
                    .size(90.dp)
                    .graphicsLayer {
                        scaleX = logoScale.value
                        scaleY = logoScale.value
                        alpha  = logoAlpha.value
                    }
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(OrionBlue, OrionGreenTap.copy(alpha = 0.85f))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Shield,
                    contentDescription = null,
                    tint     = Color.White,
                    modifier = Modifier.size(46.dp)
                )
            }

            Spacer(Modifier.height(28.dp))

            // Nome do app
            Box(
                Modifier.graphicsLayer {
                    alpha        = nameAlpha.value
                    translationY = nameOffsetY.value * density
                }
            ) {
                Text(
                    "OrionPay",
                    color      = OrionText,
                    fontSize   = 34.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }

            Spacer(Modifier.height(10.dp))

            // Tagline
            Box(Modifier.graphicsLayer { alpha = taglineAlpha.value }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(OrionGreenTap)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Pagamentos seguros e rápidos",
                        color    = OrionTextMuted,
                        fontSize = 14.sp
                    )
                    Spacer(Modifier.width(8.dp))
                    Box(
                        Modifier
                            .size(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(OrionGreenTap)
                    )
                }
            }
        }
    }
}

// =============================================================================
// TRANSACTIONACTIVITY — TELA DE PAGAMENTO
// =============================================================================
class TransactionActivity : FragmentActivity(), NfcAdapter.ReaderCallback {

    private var nfcAdapter: NfcAdapter? = null
    private var nfcEnabled = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        enableEdgeToEdge()
        setContent {
            OrionPayTheme {
                TransactionScreen(
                    onEnableNfc  = { enable -> toggleNfc(enable) },
                    onBack       = { finish() }
                )
            }
        }
    }

    fun toggleNfc(enable: Boolean) {
        nfcEnabled = enable
        if (enable) {
            nfcAdapter?.enableReaderMode(
                this, this,
                NfcAdapter.FLAG_READER_NFC_A or
                        NfcAdapter.FLAG_READER_NFC_B or
                        NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK,
                null
            )
        } else {
            nfcAdapter?.disableReaderMode(this)
        }
    }

    override fun onPause() {
        super.onPause()
        nfcAdapter?.disableReaderMode(this)
    }

    override fun onResume() {
        super.onResume()
        if (nfcEnabled) {
            nfcAdapter?.enableReaderMode(
                this, this,
                NfcAdapter.FLAG_READER_NFC_A or
                        NfcAdapter.FLAG_READER_NFC_B or
                        NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK,
                null
            )
        }
    }

    override fun onTagDiscovered(tag: Tag) {
        val isoDep = IsoDep.get(tag) ?: return
        try {
            isoDep.connect()
            isoDep.timeout = 5000
            val card = readEmvCard(isoDep)
            if (card != null) runOnUiThread { NfcDataBus.onCardRead?.invoke(card) }
            else runOnUiThread { NfcDataBus.onCardError?.invoke("Cartão não suportado") }
        } catch (e: Exception) {
            runOnUiThread { NfcDataBus.onCardError?.invoke(e.message ?: "Erro NFC") }
        } finally {
            try { isoDep.close() } catch (_: Exception) {}
        }
    }
}

// =============================================================================
// DIAGNOSTICACTIVITY — TESTE DE CONEXÃO E PAYLOAD
// =============================================================================
class DiagnosticActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Diagnóstico bloqueado em builds de produção — dados reais de cartão não devem ser expostos
        if (!BuildConfig.DEBUG) { finish(); return }
        enableEdgeToEdge()
        setContent {
            OrionPayTheme { DiagnosticScreen(onBack = { finish() }) }
        }
    }
}

@Composable
fun DiagnosticScreen(onBack: () -> Unit) {
    val scope = rememberCoroutineScope()

    // Campos editáveis do payload de teste
    var url        by remember { mutableStateOf(API_URL) }
    var amount     by remember { mutableStateOf("2.14") }
    var merchantId by remember { mutableStateOf("3f90ed27-6eca-4bf6-a4e1-607ac55ea73b") }
    var cardNumber by remember { mutableStateOf("5454545454545454") }
    var cardHolder by remember { mutableStateOf("MARIA S OLIVEIRA") }
    var expiry     by remember { mutableStateOf("11/29") }
    var cvv        by remember { mutableStateOf("123") }
    var cardBrand  by remember { mutableStateOf("MASTERCARD") }
    var entryMode  by remember { mutableStateOf("CHIP") }
    var extRef     by remember { mutableStateOf("PEDIDO-TESTE-001") }
    var terminalSn by remember { mutableStateOf("POS-ORION-992") }

    var pingState  by remember { mutableStateOf<String?>(null) }   // null=não testado
    var pingOk     by remember { mutableStateOf(false) }
    var pingMs     by remember { mutableStateOf(0L) }

    var txResult   by remember { mutableStateOf<TxResult?>(null) }
    var txLoading  by remember { mutableStateOf(false) }
    var rawResponse by remember { mutableStateOf("") }
    var sentPayload by remember { mutableStateOf("") }

    Box(Modifier.fillMaxSize().background(OrionNavy)) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 40.dp)
        ) {
            OrionHeader(showBack = true, onBack = onBack)

            Column(Modifier.padding(horizontal = 20.dp)) {
                Spacer(Modifier.height(20.dp))
                Text("Diagnóstico de API", color = OrionText, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text("Teste de conexão e envio de transação", color = OrionTextMuted, fontSize = 14.sp)
                Spacer(Modifier.height(20.dp))

                // ── Teste de conexão ────────────────────────────────────────
                DiagCard {
                    Text("Endpoint", color = OrionTextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
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
                                            connectTimeout = 5_000
                                            readTimeout    = 5_000
                                        }
                                        conn.connect()
                                        val code = conn.responseCode
                                        conn.disconnect()
                                        code < 600
                                    } catch (_: Exception) {
                                        // POST vazio — alguns endpoints não aceitam HEAD
                                        try {
                                            val conn = (URL(url).openConnection() as HttpURLConnection).apply {
                                                requestMethod  = "POST"
                                                setRequestProperty("Content-Type", "application/json")
                                                connectTimeout = 5_000
                                                readTimeout    = 5_000
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
                        colors = ButtonDefaults.buttonColors(containerColor = OrionNavyLight)
                    ) {
                        if (pingState == "testando") {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(8.dp))
                        }
                        Text("Testar conexão", color = OrionText, fontSize = 14.sp)
                    }

                    // resultado do ping
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
                                else        "Servidor não respondeu (${pingMs}ms) — verifique se a API está rodando",
                                color = if (pingOk) OrionSuccess else OrionError,
                                fontSize = 13.sp
                            )
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))

                // ── Payload de teste ─────────────────────────────────────────
                DiagCard {
                    Text("Payload de teste", color = OrionTextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(10.dp))

                    DiagRow("Valor (R$)", amount, { amount = it }, KeyboardType.Decimal)
                    Spacer(Modifier.height(8.dp))
                    DiagRow("Número do cartão", cardNumber, { cardNumber = it }, KeyboardType.Number)
                    Spacer(Modifier.height(8.dp))
                    DiagRow("Titular", cardHolder, { cardHolder = it })
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Column(Modifier.weight(1f)) {
                            Text("Validade", color = OrionTextMuted, fontSize = 11.sp)
                            Spacer(Modifier.height(4.dp))
                            OutlinedTextField(value = expiry, onValueChange = { expiry = it },
                                colors = orionTextFieldColors(), shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth(), singleLine = true)
                        }
                        Column(Modifier.weight(1f)) {
                            Text("CVV", color = OrionTextMuted, fontSize = 11.sp)
                            Spacer(Modifier.height(4.dp))
                            OutlinedTextField(value = cvv, onValueChange = { cvv = it },
                                colors = orionTextFieldColors(), shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth(), singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    DiagRow("Referência", extRef, { extRef = it })
                }

                Spacer(Modifier.height(14.dp))

                // ── Botão enviar ──────────────────────────────────────────────
                Button(
                    onClick = {
                        scope.launch {
                            txLoading = true
                            txResult  = null
                            rawResponse = ""

                            // 1. Obtém o token (Invisível)
                            val token = getInternalAuthToken()

                            if (token != null) {
                                val amtDouble = amount.toDoubleOrNull() ?: 0.0
                                val payload = buildTxPayload(
                                    merchantId, amtDouble, "CREDIT_A_VISTA",
                                    terminalSn, extRef, entryMode,
                                    cardBrand, cardHolder, cardNumber, expiry, cvv
                                )

                                sentPayload = payload.toString(2)

                                // 2. AQUI ESTAVA O ERRO: Agora enviamos o token recuperado acima
                                val result  = sendTransactionRaw(payload, token)

                                rawResponse = result.second
                                txResult    = result.first
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
                        disabledContainerColor = OrionNavyLight
                    )
                ) {
                    if (txLoading) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                    }
                    Text(
                        if (txLoading) "Enviando..." else "Enviar transação de teste",
                        fontSize = 15.sp, fontWeight = FontWeight.Bold,
                        color = if (!txLoading) Color.White else OrionTextSub
                    )
                }

                // ── Resultado ────────────────────────────────────────────────
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

                    // Resposta bruta da API
                    DiagCard {
                        Text("Resposta bruta da API", color = OrionTextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF060E1A))
                                .padding(12.dp)
                        ) {
                            Text(
                                rawResponse.ifEmpty { "(sem corpo)" },
                                color      = Color(0xFF7DD3FC),
                                fontSize   = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                lineHeight = 17.sp
                            )
                        }
                    }

                    Spacer(Modifier.height(10.dp))

                    // Payload enviado
                    DiagCard {
                        Text("Payload enviado", color = OrionTextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF060E1A))
                                .padding(12.dp)
                        ) {
                            Text(
                                sentPayload,
                                color      = Color(0xFF86EFAC),
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

@Composable
fun DiagCard(
    color: Color = Color.Transparent,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(
                if (color == Color.Transparent) OrionNavyMid
                else color.copy(alpha = 0.1f)
            )
            .border(
                1.dp,
                if (color == Color.Transparent) OrionNavyLight else color.copy(alpha = 0.35f),
                RoundedCornerShape(14.dp)
            )
            .padding(16.dp),
        content = content
    )
}

@Composable
fun DiagRow(
    label: String, value: String, onValueChange: (String) -> Unit,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    Text(label, color = OrionTextMuted, fontSize = 11.sp)
    Spacer(Modifier.height(4.dp))
    OutlinedTextField(
        value = value, onValueChange = onValueChange,
        colors = orionTextFieldColors(),
        shape  = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType)
    )
}

// =============================================================================
// THEME
// =============================================================================
@Composable
fun OrionPayTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary    = OrionBlue,
            background = OrionNavy,
            surface    = OrionNavyMid,
        ),
        content = content
    )
}


// =============================================================================
// TELA DE TRANSAÇÃO — FLUXO DE 6 ETAPAS
// Etapa 1: Valor
// Etapa 2: Forma de pagamento
// Etapa 3: Aproxime o cartão (leitura NFC acontece aqui)
// Etapa 4: Processando (chama API)
// Etapa 5: Resultado
// (FlowHeader mostra 1-5 internamente; os labels externos são ajustados)
// =============================================================================

@Composable
fun TransactionScreen(
    onEnableNfc: (Boolean) -> Unit = {},
    onBack: () -> Unit = {}
) {
    val scope = rememberCoroutineScope()

    var step            by remember { mutableStateOf(1) }
    var rawAmount       by remember { mutableStateOf("") }
    var selectedProduct by remember { mutableStateOf(ProductType.CREDIT_AVISTA) }
    var cardData        by remember { mutableStateOf<CardData?>(null) }
    var nfcStatus       by remember { mutableStateOf("Aguardando cartão...") }
    var nfcError        by remember { mutableStateOf(false) }
    var txResult        by remember { mutableStateOf<TxResult?>(null) }
    var merchantId      by remember { mutableStateOf(MERCHANT_ID) }
    var terminalSn      by remember { mutableStateOf("POS-ORION-992") }
    var extRef          by remember { mutableStateOf("PEDIDO-${System.currentTimeMillis() % 100000}") }

    // ── IDEMPOTÊNCIA ────────────────────────────────────────────────────────
    // Gerada uma vez ao entrar na etapa 4 e mantida em memória para retries.
    // Só é resetada quando o usuário inicia uma NOVA venda.
    var idempotencyKey  by remember { mutableStateOf("") }
    // Bloqueia múltiplos cliques / disparos simultâneos
    var isSubmitting    by remember { mutableStateOf(false) }

    val amountDouble    = (rawAmount.toLongOrNull() ?: 0L) / 100.0
    val formattedAmount = java.text.NumberFormat.getCurrencyInstance(Locale("pt", "BR")).format(amountDouble)

    // Função de envio que preserva a chave para retries
    fun submitTransaction() {
        if (isSubmitting) return          // bloqueia clique duplo
        isSubmitting = true

        // Gera a chave APENAS na primeira tentativa; retry reutiliza a mesma
        if (idempotencyKey.isEmpty()) {
            idempotencyKey = java.util.UUID.randomUUID().toString()
        }
        Log.d("ORION_IDEM", "X-Idempotency-Key: $idempotencyKey")

        scope.launch {
            val token = getInternalAuthToken()

            Log.d("ORION_AUTH", "Token obtido: ${token}...")

            if (token == null) {
                txResult = TxResult(TxState.ERROR, "Falha na autenticação do terminal")
                isSubmitting = false
                step = 5
                return@launch
            }

            val card = cardData
            val payload = buildTxPayload(
                merchantId     = merchantId,
                amount         = amountDouble,
                productType    = selectedProduct.apiKey,
                terminalSn     = terminalSn,
                externalRef    = extRef,
                entryMode      = card?.entryMode ?: "CHIP",
                cardBrand      = card?.brand ?: "",
                cardHolder     = card?.holder ?: "",
                cardNumber     = card?.panRaw ?: "",
                expirationDate = card?.expiry ?: "",
                cvv            = if ((card?.entryMode ?: "CHIP") == "CHIP") "" else card?.cvv2 ?: "",
                cryptogram     = card?.cryptogram ?: "",
                atc            = card?.atc ?: "",
                iad            = card?.iad ?: "",
                aip            = card?.aip ?: "",
                tvr            = card?.tvr ?: ""
            )
            txResult    = sendTransaction(payload, idempotencyKey, token)
            isSubmitting = false
            step         = 5
        }
    }

    LaunchedEffect(step) { onEnableNfc(step == 3) }

    DisposableEffect(step) {
        if (step == 3) {
            NfcDataBus.onCardRead = { card ->
                cardData  = card
                nfcError  = false
                nfcStatus = "Cartão lido! ${card.brand} — ${maskPan(card.panRaw)}"
                step = 4
            }
            NfcDataBus.onCardError = { msg -> nfcError = true; nfcStatus = msg }
        }
        onDispose { NfcDataBus.onCardRead = null; NfcDataBus.onCardError = null }
    }

    Box(Modifier.fillMaxSize().background(OrionNavy)) {
        Column(Modifier.fillMaxSize()) {

            FlowHeader(currentStep = step, totalSteps = 5, onBack = {
                when {
                    step > 1 && step < 4 -> step--
                    step == 1            -> onBack()
                }
            })

            when (step) {

                1 -> StepAmount(
                    formattedAmount = formattedAmount,
                    rawAmount       = rawAmount,
                    onDigit         = { if (rawAmount.length < 9) rawAmount += it },
                    onBackspace     = { if (rawAmount.isNotEmpty()) rawAmount = rawAmount.dropLast(1) },
                    onClear         = { rawAmount = "" },
                    onNext          = { if (amountDouble > 0) step = 2 }
                )

                2 -> StepPaymentMethod(
                    amount   = formattedAmount,
                    selected = selectedProduct,
                    onSelect = { selectedProduct = it },
                    onNext   = { step = 3 }
                )

                3 -> StepNfcWait(
                    amount   = formattedAmount,
                    product  = selectedProduct,
                    status   = nfcStatus,
                    hasError = nfcError,
                    onNext   = { step = 4 }
                )

                4 -> {
                    // Loading overlay — bloqueia interação durante envio
                    StepProcessing(amount = formattedAmount)
                    LaunchedEffect(Unit) { submitTransaction() }
                }

                5 -> {
                    val result = txResult
                    // Backend retornou que já processou (Redis/idempotência) — trata como sucesso
                    val effectiveSuccess = result?.state == TxState.SUCCESS ||
                            result?.rawJson?.contains("already processed", ignoreCase = true) == true ||
                            result?.rawJson?.contains("idempotent",        ignoreCase = true) == true

                    val comp = if (effectiveSuccess) ComprovanteData(
                        amount        = formattedAmount,
                        product       = selectedProduct.label,
                        brand         = cardData?.brand ?: "",
                        maskedPan     = maskDisplay(cardData?.panRaw ?: ""),
                        holder        = cardData?.holder?.ifEmpty { "Não informado" } ?: "Não informado",
                        authCode      = result?.authCode ?: "",
                        nsu           = result?.nsu ?: "",
                        transactionId = result?.transactionId ?: "",
                        dateTime      = isoNow().replace("T", " "),
                        terminalSn    = terminalSn,
                        merchantId    = merchantId,
                        entryMode     = cardData?.entryMode ?: "CHIP",
                        message       = result?.message ?: ""
                    ) else null

                    StepResult(
                        result      = if (effectiveSuccess) result?.copy(state = TxState.SUCCESS) else result,
                        amount      = formattedAmount,
                        product     = selectedProduct,
                        comprovante = comp,
                        // Retry: volta para etapa 4 MANTENDO a idempotencyKey
                        canRetry    = result?.isRetryable() == true,
                        onRetry     = {
                            txResult     = null
                            isSubmitting = false
                            step         = 4   // idempotencyKey permanece intacta
                        },
                        onNewSale   = {
                            step           = 1
                            rawAmount      = ""
                            cardData       = null
                            txResult       = null
                            idempotencyKey = ""  // nova venda = nova chave
                            isSubmitting   = false
                            nfcStatus      = "Aguardando cartão..."
                            nfcError       = false
                            extRef         = "PEDIDO-${System.currentTimeMillis() % 100000}"
                        },
                        onBack      = onBack
                    )
                }
            }
        }

        // Loading overlay global — impede qualquer toque durante envio
        if (isSubmitting) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.45f))
                    .clickable(enabled = false) {},   // consome todos os eventos
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = OrionBlue, modifier = Modifier.size(48.dp), strokeWidth = 4.dp)
                    Spacer(Modifier.height(16.dp))
                    Text("Processando...", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

// ─── Componente: tecla do teclado numérico com efeito de toque verde ─────────
@Composable
fun RowScope.NumPadKey(key: String, onTap: () -> Unit) {
    val isBs  = key == "⌫"
    val isDot = key == "."
    val tapAnim  = remember { Animatable(0f) }
    val tapScale = remember { Animatable(1f) }
    val scope    = rememberCoroutineScope()

    Box(
        Modifier
            .weight(1f)
            .height(58.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(
                when {
                    isBs  -> OrionNavyLight
                    isDot -> Color.Transparent
                    else  -> OrionNavyLight.copy(alpha = 0.4f)
                }
            )
            .clickable(
                indication = null,
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
            ) {
                scope.launch {
                    launch { tapAnim.snapTo(1f); tapAnim.animateTo(0f, tween(380, easing = FastOutSlowInEasing)) }
                    launch { tapScale.snapTo(0.88f); tapScale.animateTo(1f, tween(220, easing = FastOutSlowInEasing)) }
                }
                onTap()
            },
        contentAlignment = Alignment.Center
    ) {
        // Flash verde arredondado
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = tapAnim.value }
                .clip(RoundedCornerShape(14.dp))
                .background(OrionGreenTap.copy(alpha = 0.35f))
        )
        // Conteúdo com escala
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer { scaleX = tapScale.value; scaleY = tapScale.value },
            contentAlignment = Alignment.Center
        ) {
            if (isBs) {
                Icon(Icons.Default.Backspace, null, tint = OrionTextMuted, modifier = Modifier.size(22.dp))
            } else {
                Text(
                    key,
                    color      = if (isDot) OrionTextSub else OrionText,
                    fontSize   = 22.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

// ─── Etapa 1: Valor ───────────────────────────────────────────────────────────
@Composable
fun StepAmount(
    formattedAmount: String, rawAmount: String,
    onDigit: (String) -> Unit, onBackspace: () -> Unit,
    onClear: () -> Unit, onNext: () -> Unit
) {
    Column(Modifier.fillMaxSize()) {
        Column(
            Modifier.weight(1f).padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("Qual o valor da venda?", color = OrionTextMuted, fontSize = 16.sp)
            Spacer(Modifier.height(12.dp))
            Text(
                formattedAmount,
                color      = OrionText,
                fontSize   = 42.sp,
                fontWeight = FontWeight.Bold,
                textAlign  = TextAlign.Center
            )
        }

        // Teclado numérico + botão próximo
        val navBarPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .background(OrionNavyMid)
                .padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 20.dp + navBarPadding)
        ) {
            listOf(listOf("1","2","3"), listOf("4","5","6"), listOf("7","8","9"), listOf(".","0","⌫")).forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    row.forEach { key ->
                        NumPadKey(
                            key         = key,
                            onTap       = { when (key) { "⌫" -> onBackspace(); "." -> {} else -> onDigit(key) } }
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))
            }
            Spacer(Modifier.height(8.dp))
            Button(
                onClick  = onNext,
                enabled  = rawAmount.isNotEmpty() && rawAmount != "0",
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape    = RoundedCornerShape(14.dp),
                colors   = ButtonDefaults.buttonColors(
                    containerColor         = OrionBlue,
                    disabledContainerColor = OrionNavyLight
                )
            ) {
                Text("Próximo", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}

// ─── Etapa 4: Forma de pagamento ──────────────────────────────────────────────
@Composable
fun StepPaymentMethod(
    amount: String, selected: ProductType,
    onSelect: (ProductType) -> Unit, onNext: () -> Unit
) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Column(Modifier.padding(horizontal = 24.dp)) {
            Spacer(Modifier.height(24.dp))
            Text(
                "Escolha a forma de pagamento",
                color = OrionText, fontSize = 20.sp, fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(6.dp))

            // Chip do valor
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Valor da venda", color = OrionTextMuted, fontSize = 13.sp)
                Spacer(Modifier.width(8.dp))
                Box(
                    Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(OrionBlue.copy(alpha = 0.15f))
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text(amount, color = OrionBlue, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(Modifier.height(20.dp))
            Text("Forma de pagamento", color = OrionTextMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(10.dp))
        }

        // Lista de produtos
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(OrionNavyMid)
                .border(1.dp, OrionNavyLight, RoundedCornerShape(16.dp))
                .padding(horizontal = 24.dp)
        ) {
            ProductType.entries.forEachIndexed { idx, pt ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { onSelect(pt) }
                        .background(if (pt == selected) OrionBlue.copy(0.08f) else Color.Transparent)
                        .padding(vertical = 16.dp, horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Ícone do produto
                    Box(
                        Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(OrionNavyLight),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            when {
                                pt.apiKey.contains("DEBIT") -> Icons.Default.AccountBalance
                                pt.apiKey == "PIX"          -> Icons.Default.Bolt
                                else                         -> Icons.Default.CreditCard
                            },
                            contentDescription = null,
                            tint     = if (pt == selected) OrionBlue else OrionTextMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(Modifier.width(14.dp))
                    Text(pt.label, color = OrionText, fontSize = 15.sp, modifier = Modifier.weight(1f))
                    RadioButton(
                        selected = pt == selected, onClick = { onSelect(pt) },
                        colors   = RadioButtonDefaults.colors(selectedColor = OrionBlue, unselectedColor = OrionTextSub)
                    )
                }
                if (idx < ProductType.entries.lastIndex)
                    HorizontalDivider(color = OrionNavyLight)
            }
        }

        Spacer(Modifier.height(24.dp))
        val navPad2 = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        Box(Modifier.padding(start = 24.dp, end = 24.dp, bottom = navPad2 + 8.dp)) {
            Button(
                onClick  = onNext,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape    = RoundedCornerShape(14.dp),
                colors   = ButtonDefaults.buttonColors(containerColor = OrionBlue)
            ) {
                Text("Vender", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}

// ─── Etapa 3: Aguardando aproximar cartão ─────────────────────────────────────
@Composable
fun StepNfcWait(amount: String, product: ProductType, status: String, hasError: Boolean, onNext: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "nfc")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.88f, targetValue = 1.12f,
        animationSpec = infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "nfc_pulse"
    )

    Column(
        Modifier.fillMaxSize().background(OrionBlue),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.weight(1f))

        // Ícone NFC animado
        Box(contentAlignment = Alignment.Center) {
            Box(
                Modifier
                    .size(180.dp)
                    .scale(pulse)
                    .clip(RoundedCornerShape(90.dp))
                    .background(Color.White.copy(alpha = 0.08f))
            )
            Box(
                Modifier
                    .size(140.dp)
                    .clip(RoundedCornerShape(70.dp))
                    .background(Color.White.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Wifi,
                    contentDescription = null,
                    tint     = Color.White,
                    modifier = Modifier.size(72.dp)
                )
            }
        }

        Spacer(Modifier.height(32.dp))
        Text(
            "Aproxime o cartão ou dispositivo"+
                    "atrás deste celular",
                    color     = Color.White,
            fontSize  = 18.sp,
            fontWeight= FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier  = Modifier.padding(horizontal = 40.dp)
        )

        Spacer(Modifier.weight(1f))

        // Status da leitura NFC
        AnimatedVisibility(visible = hasError) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFEF4444).copy(alpha = 0.25f))
                    .padding(12.dp)
            ) {
                Text(status, color = Color.White, fontSize = 13.sp, textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth())
            }
        }

        Spacer(Modifier.height(16.dp))

        // Rodapé com valor
        val navPad3 = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        Column(
            Modifier
                .fillMaxWidth()
                .background(Color.White.copy(alpha = 0.1f))
                .padding(start = 24.dp, end = 24.dp, top = 20.dp, bottom = 20.dp + navPad3),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(product.label, color = Color.White.copy(alpha = 0.8f), fontSize = 13.sp)
            Spacer(Modifier.height(4.dp))
            Text(amount, color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        }
    }
}

// ─── Etapa 6: Processando ────────────────────────────────────────────────────
@Composable
fun StepProcessing(amount: String) {
    val infiniteTransition = rememberInfiniteTransition(label = "proc")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(1200, easing = LinearEasing)),
        label = "rotation"
    )

    Column(
        Modifier.fillMaxSize().background(OrionBlue),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.weight(1f))

        Text("Processando pagamento...", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(48.dp))

        Box(Modifier.size(100.dp), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(
                modifier    = Modifier.size(100.dp),
                color       = Color.White,
                strokeWidth = 6.dp
            )
        }

        Spacer(Modifier.weight(1f))

        val navPad5 = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        Column(
            Modifier
                .fillMaxWidth()
                .background(Color.White.copy(alpha = 0.1f))
                .padding(start = 24.dp, end = 24.dp, top = 20.dp, bottom = 20.dp + navPad5),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Valor do pagamento", color = Color.White.copy(alpha = 0.8f), fontSize = 13.sp)
            Spacer(Modifier.height(4.dp))
            Text(amount, color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        }
    }
}

// ─── Etapa 7: Resultado ──────────────────────────────────────────────────────
@Composable
fun StepResult(
    result: TxResult?, amount: String, product: ProductType,
    comprovante: ComprovanteData? = null,
    canRetry: Boolean = false,
    onRetry: () -> Unit = {},
    onNewSale: () -> Unit,
    onBack: () -> Unit
) {
    val ok = result?.state == TxState.SUCCESS
    var showComprovante by remember { mutableStateOf(false) }

    if (showComprovante && comprovante != null) {
        ComprovanteScreen(
            data          = comprovante,
            onClose       = { showComprovante = false },
            // onNewSale reseta todo o estado do fluxo e volta para a etapa 1
            onBackToHome  = onNewSale
        )
        return
    }

    Column(
        Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.weight(1f))

        // Ícone de resultado
        Box(
            Modifier
                .size(120.dp)
                .clip(RoundedCornerShape(60.dp))
                .background(if (ok) OrionSuccess.copy(0.15f) else OrionError.copy(0.15f))
                .border(3.dp, if (ok) OrionSuccess else OrionError, RoundedCornerShape(60.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                if (ok) Icons.Default.Check else Icons.Default.Close,
                contentDescription = null,
                tint     = if (ok) OrionSuccess else OrionError,
                modifier = Modifier.size(56.dp)
            )
        }

        Spacer(Modifier.height(24.dp))

        Text(
            if (ok) "Pagamento efetuado!" else "Pagamento recusado",
            color      = if (ok) OrionSuccess else OrionError,
            fontSize   = 22.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(6.dp))

        if (result?.message?.isNotEmpty() == true) {
            Text(result.message, color = OrionTextMuted, fontSize = 14.sp, textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 40.dp))
        }

        Spacer(Modifier.height(20.dp))

        // Detalhes
        Column(
            Modifier
                .padding(horizontal = 24.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(OrionNavyMid)
                .border(1.dp, OrionNavyLight, RoundedCornerShape(16.dp))
                .padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("${product.label}", color = OrionTextMuted, fontSize = 13.sp)
            Spacer(Modifier.height(4.dp))
            Text(amount, color = OrionText, fontSize = 30.sp, fontWeight = FontWeight.Bold)
            if (result?.authCode?.isNotEmpty() == true) {
                Spacer(Modifier.height(10.dp))
                HorizontalDivider(color = OrionNavyLight)
                Spacer(Modifier.height(10.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Autorização", color = OrionTextMuted, fontSize = 13.sp)
                    Text(result.authCode, color = OrionText, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                }
            }
            if (result?.nsu?.isNotEmpty() == true) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("NSU", color = OrionTextMuted, fontSize = 13.sp)
                    Text(result.nsu, color = OrionText, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                }
            }
        }

        Spacer(Modifier.weight(1f))

        // Botões de ação
        val navPad4 = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        Column(Modifier.padding(start = 24.dp, end = 24.dp, top = 20.dp, bottom = 20.dp + navPad4)) {
            if (ok) {
                Button(
                    onClick  = { showComprovante = true },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape    = RoundedCornerShape(14.dp),
                    colors   = ButtonDefaults.buttonColors(containerColor = OrionBlue)
                ) {
                    Icon(Icons.Default.Receipt, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Ver comprovante", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
                Spacer(Modifier.height(10.dp))
            }
            // Retry — só aparece em erros de rede (timeout/sem conexão)
            // Reutiliza a mesma X-Idempotency-Key para evitar cobrança dupla
            if (!ok && canRetry) {
                Button(
                    onClick  = onRetry,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape    = RoundedCornerShape(14.dp),
                    colors   = ButtonDefaults.buttonColors(containerColor = OrionWarning)
                ) {
                    Icon(Icons.Default.Refresh, null, modifier = Modifier.size(18.dp), tint = Color.White)
                    Spacer(Modifier.width(8.dp))
                    Text("Tentar novamente", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    "A mesma chave de idempotência será reutilizada — sem risco de cobrança dupla.",
                    color    = OrionTextMuted,
                    fontSize = 11.sp,
                    textAlign= TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
                Spacer(Modifier.height(10.dp))
            }
            OutlinedButton(
                onClick  = onNewSale,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape    = RoundedCornerShape(14.dp),
                border   = BorderStroke(1.dp, OrionNavyLight),
                colors   = ButtonDefaults.outlinedButtonColors(contentColor = OrionText)
            ) {
                Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Nova venda", fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ─── Tela de Comprovante ─────────────────────────────────────────────────────
@Composable
fun ComprovanteScreen(data: ComprovanteData, onClose: () -> Unit, onBackToHome: () -> Unit = onClose) {
    val context      = androidx.compose.ui.platform.LocalContext.current
    val scope        = rememberCoroutineScope()
    val snackbarHost = remember { SnackbarHostState() }
    val navPad       = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val statusPad    = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    var email        by remember { mutableStateOf("") }
    var sending      by remember { mutableStateOf(false) }
    var emailError   by remember { mutableStateOf(false) }
    var sendApiError by remember { mutableStateOf("") }

    Box(Modifier.fillMaxSize().background(OrionNavy)) {

        // ── Conteúdo principal ────────────────────────────────────────────
        Column(Modifier.fillMaxSize()) {
            // Header
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(OrionNavyMid)
                    .padding(top = statusPad + 12.dp, bottom = 12.dp, start = 16.dp, end = 20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.ArrowBack, null, tint = OrionText, modifier = Modifier.size(20.dp))
                }
                Text("Comprovante", color = OrionText, fontSize = 17.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f))
                IconButton(onClick = {
                    val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(android.content.Intent.EXTRA_TEXT, buildComprovanteText(data))
                        putExtra(android.content.Intent.EXTRA_SUBJECT, "Comprovante OrionPay — ${data.amount}")
                    }
                    context.startActivity(android.content.Intent.createChooser(shareIntent, "Compartilhar comprovante"))
                }) {
                    Icon(Icons.Default.Share, null, tint = OrionBlue, modifier = Modifier.size(20.dp))
                }
            }

            // Body scrollável
            Column(
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
            ) {
                Spacer(Modifier.height(24.dp))

                // Cabeçalho do comprovante
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(OrionNavyMid)
                        .border(1.dp, OrionNavyLight, RoundedCornerShape(16.dp))
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(28.dp))
                            .background(OrionSuccess.copy(alpha = 0.15f))
                            .border(2.dp, OrionSuccess.copy(alpha = 0.5f), RoundedCornerShape(28.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Check, null, tint = OrionSuccess, modifier = Modifier.size(28.dp))
                    }
                    Spacer(Modifier.height(12.dp))
                    Text("Pagamento aprovado", color = OrionSuccess, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    Text(data.amount, color = OrionText, fontSize = 32.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    Text(data.dateTime, color = OrionTextMuted, fontSize = 12.sp)
                }

                Spacer(Modifier.height(14.dp))

                CompSection("Dados da transação") {
                    CompRow("Forma de pagamento", data.product)
                    CompRow("Bandeira",           data.brand.ifEmpty { "—" })
                    CompRow("Cartão",             data.maskedPan.ifEmpty { "—" })
                    CompRow("Portador",           data.holder)
                    CompRow("Modo de entrada",    data.entryMode)
                }

                Spacer(Modifier.height(14.dp))

                CompSection("Autorização") {
                    CompRow("Cód. autorização", data.authCode.ifEmpty { "—" })
                    CompRow("NSU",              data.nsu.ifEmpty { "—" })
                    CompRow("Terminal",         data.terminalSn)
                    CompRow("Estabelecimento",  data.merchantId.take(20) + "...")
                }

                Spacer(Modifier.height(24.dp))

                // Seção de e-mail
                Text("Enviar comprovante por e-mail",
                    color = OrionText, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text("O comprovante será enviado diretamente ao cliente.",
                    color = OrionTextMuted, fontSize = 13.sp)
                Spacer(Modifier.height(12.dp))

                OutlinedTextField(
                    value         = email,
                    onValueChange = { email = it; emailError = false; sendApiError = "" },
                    placeholder   = { Text("cliente@email.com", color = OrionTextSub) },
                    leadingIcon   = {
                        Icon(Icons.Default.Email, null, tint = OrionTextMuted, modifier = Modifier.size(18.dp))
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction    = ImeAction.Send
                    ),
                    keyboardActions = KeyboardActions(
                        onSend = { if (isValidEmail(email)) { sending = true; sendApiError = "" } else emailError = true }
                    ),
                    isError  = emailError,
                    colors   = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor      = OrionBlue,
                        unfocusedBorderColor    = OrionNavyLight,
                        errorBorderColor        = OrionError,
                        focusedTextColor        = OrionText,
                        unfocusedTextColor      = OrionText,
                        cursorColor             = OrionBlue,
                        focusedContainerColor   = OrionNavyMid,
                        unfocusedContainerColor = OrionNavyMid,
                        errorContainerColor     = OrionNavyMid,
                    ),
                    shape      = RoundedCornerShape(12.dp),
                    modifier   = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                if (emailError) {
                    Spacer(Modifier.height(4.dp))
                    Text("E-mail inválido", color = OrionError, fontSize = 12.sp)
                }

                Spacer(Modifier.height(10.dp))

                Button(
                    onClick = {
                        if (!isValidEmail(email)) {
                            emailError = true
                            return@Button
                        }
                        sending = true
                        sendApiError = ""
                        // Tudo no mesmo scope.launch — snackbarHost acessível aqui
                        scope.launch {
                            val emailCapture = email
                            val apiResult = callSendEmailApi(
                                transactionId = data.transactionId,
                                email         = emailCapture
                            )
                            sending = false
                            if (apiResult.first) {
                                val snackResult = snackbarHost.showSnackbar(
                                    message     = "Comprovante enviado para $emailCapture",
                                    actionLabel = "Nova transação",
                                    duration    = SnackbarDuration.Indefinite
                                )
                                if (snackResult == SnackbarResult.ActionPerformed) {
                                    onBackToHome()
                                }
                            } else {
                                sendApiError = apiResult.second
                            }
                        }
                    },
                    enabled  = email.isNotEmpty() && !sending,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape    = RoundedCornerShape(14.dp),
                    colors   = ButtonDefaults.buttonColors(
                        containerColor         = OrionBlue,
                        disabledContainerColor = OrionNavyLight
                    )
                ) {
                    if (sending) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                        Text("Enviando...", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    } else {
                        Icon(Icons.Default.Send, null, modifier = Modifier.size(17.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Enviar por e-mail", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }

                AnimatedVisibility(visible = sendApiError.isNotEmpty()) {
                    Column {
                        Spacer(Modifier.height(10.dp))
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(OrionError.copy(alpha = 0.1f))
                                .border(1.dp, OrionError.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Error, null, tint = OrionError, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(sendApiError, color = OrionError, fontSize = 13.sp, modifier = Modifier.weight(1f))
                        }
                    }
                }

                Spacer(Modifier.height(navPad + 24.dp))
            }
        }

        // Snackbar sobreposto no topo do Box — sempre visível
        SnackbarHost(
            hostState = snackbarHost,
            modifier  = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 24.dp)
        ) { snackData ->
            Snackbar(
                shape              = RoundedCornerShape(14.dp),
                containerColor     = OrionSuccess,
                contentColor       = Color.White,
                actionContentColor = Color.White,
                action = {
                    TextButton(onClick = {
                        snackData.performAction()
                        onBackToHome()
                    }) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Add, null, tint = Color.White, modifier = Modifier.size(15.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(
                                snackData.visuals.actionLabel ?: "Nova transação",
                                color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CheckCircle, null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(snackData.visuals.message, color = Color.White, fontSize = 14.sp)
                }
            }
        }
    } // fecha Box
}


// ── Seção do comprovante ──────────────────────────────────────────────────────
@Composable
fun CompSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(OrionNavyMid)
            .border(1.dp, OrionNavyLight, RoundedCornerShape(14.dp))
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .background(OrionNavyLight.copy(alpha = 0.5f))
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            Text(title, color = OrionTextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
        Column(Modifier.padding(horizontal = 16.dp, vertical = 4.dp), content = content)
    }
}

@Composable
fun CompRow(label: String, value: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 9.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = OrionTextMuted, fontSize = 13.sp, modifier = Modifier.weight(1f))
        Spacer(Modifier.width(12.dp))
        Text(
            value,
            color      = OrionText,
            fontSize   = 13.sp,
            fontWeight = FontWeight.Medium,
            textAlign  = TextAlign.End,
            modifier   = Modifier.weight(1.5f)
        )
    }
    HorizontalDivider(color = OrionNavyLight.copy(alpha = 0.5f))
}

// ── Helpers de e-mail ─────────────────────────────────────────────────────────
fun isValidEmail(email: String): Boolean =
    android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()

fun buildComprovanteText(data: ComprovanteData): String = buildString {
    appendLine("╔═══════════════════════════╗")
    appendLine("║     COMPROVANTE ORIONPAY  ║")
    appendLine("╚═══════════════════════════╝")
    appendLine()
    appendLine("✅ PAGAMENTO APROVADO")
    appendLine()
    appendLine("Valor         : ${data.amount}")
    appendLine("Data/Hora     : ${data.dateTime}")
    appendLine()
    appendLine("─────────────────────────────")
    appendLine("  DADOS DO PAGAMENTO")
    appendLine("─────────────────────────────")
    appendLine("Forma         : ${data.product}")
    appendLine("Bandeira      : ${data.brand.ifEmpty { "—" }}")
    appendLine("Cartão        : ${data.maskedPan.ifEmpty { "—" }}")
    appendLine("Portador      : ${data.holder}")
    appendLine("Modo entrada  : ${data.entryMode}")
    appendLine()
    appendLine("─────────────────────────────")
    appendLine("  AUTORIZAÇÃO")
    appendLine("─────────────────────────────")
    appendLine("Cód. Aut.     : ${data.authCode.ifEmpty { "—" }}")
    appendLine("NSU           : ${data.nsu.ifEmpty { "—" }}")
    appendLine("Terminal      : ${data.terminalSn}")
    appendLine("Estabelec.    : ${data.merchantId.take(18)}...")
    appendLine()
    appendLine("═════════════════════════════")
    appendLine("  OrionPay — Ambiente Seguro")
    appendLine("═════════════════════════════")
    appendLine()
    appendLine("Este comprovante é válido como")
    appendLine("prova de pagamento eletrônico.")
}

fun buildComprovanteHtml(data: ComprovanteData): String = """
<!DOCTYPE html>
<html lang="pt-BR">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<style>
  body { font-family: Arial, sans-serif; background: #f0f4f8; margin: 0; padding: 20px; }
  .card { background: #fff; border-radius: 12px; max-width: 480px; margin: 0 auto; overflow: hidden; box-shadow: 0 2px 12px rgba(0,0,0,.1); }
  .header { background: #0D1B2A; padding: 28px 24px; text-align: center; }
  .logo-row { display: flex; align-items: center; justify-content: center; gap: 10px; margin-bottom: 8px; }
  .logo-icon { width: 36px; height: 36px; background: linear-gradient(135deg,#2563EB,#10C97A); border-radius: 8px; display: flex; align-items: center; justify-content: center; }
  .logo-name { color: #fff; font-size: 20px; font-weight: 700; letter-spacing: 1px; }
  .status-chip { display: inline-block; background: #10B981; color: #fff; font-size: 13px; font-weight: 700; padding: 4px 14px; border-radius: 20px; margin-top: 4px; }
  .amount-block { background: #132338; padding: 20px 24px; text-align: center; }
  .amount-label { color: #94A3B8; font-size: 13px; }
  .amount-val { color: #fff; font-size: 36px; font-weight: 800; margin: 4px 0; }
  .amount-dt { color: #64748B; font-size: 12px; }
  .section { padding: 20px 24px 0; }
  .section-title { color: #64748B; font-size: 11px; font-weight: 700; text-transform: uppercase; letter-spacing: .6px; margin-bottom: 12px; }
  .row { display: flex; justify-content: space-between; padding: 9px 0; border-bottom: 1px solid #f1f5f9; }
  .row:last-child { border-bottom: none; }
  .row-label { color: #94A3B8; font-size: 13px; }
  .row-val { color: #1e293b; font-size: 13px; font-weight: 600; text-align: right; }
  .auth-code { font-family: monospace; background: #f0f9ff; padding: 2px 8px; border-radius: 4px; }
  .footer { background: #0D1B2A; padding: 16px 24px; text-align: center; margin-top: 20px; }
  .footer-text { color: #64748B; font-size: 11px; }
  .green-dot { display: inline-block; width: 6px; height: 6px; border-radius: 3px; background: #10C97A; margin: 0 4px; vertical-align: middle; }
</style>
</head>
<body>
<div class="card">
  <div class="header">
    <div class="logo-row">
      <div class="logo-icon"><svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="#fff" stroke-width="2"><path d="M12 2L4 6v6c0 5.25 3.5 10.14 8 11.29C16.5 22.14 20 17.25 20 12V6z"/></svg></div>
      <span class="logo-name">OrionPay</span>
    </div>
    <div class="status-chip">✓ PAGAMENTO APROVADO</div>
  </div>
  <div class="amount-block">
    <div class="amount-label">Valor pago</div>
    <div class="amount-val">${data.amount}</div>
    <div class="amount-dt">${data.dateTime}</div>
  </div>
  <div class="section">
    <div class="section-title">Dados do pagamento</div>
    <div class="row"><span class="row-label">Forma</span><span class="row-val">${data.product}</span></div>
    <div class="row"><span class="row-label">Bandeira</span><span class="row-val">${data.brand.ifEmpty { "—" }}</span></div>
    <div class="row"><span class="row-label">Cartão</span><span class="row-val">${data.maskedPan.ifEmpty { "—" }}</span></div>
    <div class="row"><span class="row-label">Portador</span><span class="row-val">${data.holder}</span></div>
    <div class="row"><span class="row-label">Entrada</span><span class="row-val">${data.entryMode}</span></div>
  </div>
  <div class="section" style="margin-top:8px">
    <div class="section-title">Autorização</div>
    <div class="row"><span class="row-label">Cód. autorização</span><span class="row-val"><span class="auth-code">${data.authCode.ifEmpty { "—" }}</span></span></div>
    <div class="row"><span class="row-label">NSU</span><span class="row-val">${data.nsu.ifEmpty { "—" }}</span></div>
    <div class="row"><span class="row-label">Terminal</span><span class="row-val">${data.terminalSn}</span></div>
  </div>
  <div class="footer" style="margin-top:20px">
    <div class="footer-text"><span class="green-dot"></span> OrionPay — Ambiente Seguro <span class="green-dot"></span></div>
    <div class="footer-text" style="margin-top:4px">Este comprovante é válido como prova de pagamento.</div>
  </div>
</div>
</body>
</html>
"""

// Chama API REST de envio de e-mail: POST /transactions/{transactionId}/send-email
// Retorna Pair(sucesso, mensagemErro)
suspend fun callSendEmailApi(
    transactionId: String,
    email: String
): Pair<Boolean, String> = withContext(Dispatchers.IO) {
    try {
        val txId     = transactionId.ifEmpty { "unknown" }
        val endpoint = API_EMAIL_URL.replace("{transactionId}", txId)
        Log.d("ORION_EMAIL", "POST $endpoint  email=$email")

        val conn = (URL(endpoint).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            setRequestProperty("Content-Type",  "application/json; charset=utf-8")
            setRequestProperty("Accept",         "application/json")
            setRequestProperty("X-Merchant-Id",  MERCHANT_ID)
            doOutput       = true
            connectTimeout = 15_000
            readTimeout    = 30_000
        }
        val body = JSONObject().apply { put("email", email) }.toString()
        OutputStreamWriter(conn.outputStream, "UTF-8").use { it.write(body) }

        val code = conn.responseCode
        val resp = runCatching {
            (if (code in 200..299) conn.inputStream else conn.errorStream)
                ?.bufferedReader()?.readText() ?: ""
        }.getOrDefault("")
        Log.d("ORION_EMAIL", "HTTP $code <- $resp")

        if (code in 200..299) {
            Pair(true, "")
        } else {
            val json = runCatching { JSONObject(resp) }.getOrNull()
            Pair(false, json?.optString("message") ?: json?.optString("error") ?: "Erro HTTP $code")
        }
    } catch (e: java.net.ConnectException) {
        Pair(false, "Servidor indisponível")
    } catch (e: java.net.SocketTimeoutException) {
        Pair(false, "Timeout — API não respondeu")
    } catch (e: Exception) {
        Log.e("ORION_EMAIL", "Erro", e)
        Pair(false, e.message ?: "Erro desconhecido")
    }
}


// ─── Header com indicador de etapas ──────────────────────────────────────────
@Composable
fun FlowHeader(currentStep: Int, totalSteps: Int, onBack: () -> Unit) {
    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    Column(
        Modifier
            .fillMaxWidth()
            .background(OrionNavyMid)
            .padding(top = statusBarHeight + 12.dp, bottom = 14.dp, start = 20.dp, end = 20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.ArrowBack, null, tint = OrionText, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(8.dp))
            // Bolinhas de etapa
            Row(
                Modifier.weight(1f),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                (1..totalSteps).forEach { step ->
                    val isDone    = step < currentStep
                    val isCurrent = step == currentStep
                    // Bolinha
                    Box(
                        Modifier
                            .size(if (isCurrent) 30.dp else 22.dp)
                            .clip(RoundedCornerShape(15.dp))
                            .background(
                                when {
                                    isCurrent -> OrionBlue
                                    isDone    -> OrionSuccess
                                    else      -> OrionNavyLight
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isDone) {
                            Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(12.dp))
                        } else {
                            Text(
                                "$step",
                                color    = if (isCurrent) Color.White else OrionTextSub,
                                fontSize = if (isCurrent) 13.sp else 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    // Linha entre bolinhas
                    if (step < totalSteps) {
                        Box(
                            Modifier
                                .width(14.dp)
                                .height(2.dp)
                                .background(if (isDone) OrionSuccess else OrionNavyLight)
                        )
                    }
                }
            }
            Spacer(Modifier.width(32.dp))
        }
    }
}
// =============================================================================
// COMPONENTES UI
// =============================================================================

@Composable
fun OrionHeader(showBack: Boolean, onBack: () -> Unit = {}) {
    // statusBarsPadding() garante que o header não fique sob a status bar (edge-to-edge)
    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    Row(
        Modifier
            .fillMaxWidth()
            .background(OrionNavyMid)
            .padding(top = statusBarHeight + 12.dp, bottom = 12.dp, start = 20.dp, end = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (showBack) {
            IconButton(onClick = onBack, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Voltar", tint = OrionText)
            }
            Spacer(Modifier.width(8.dp))
        }
        Box(
            Modifier.size(34.dp).clip(RoundedCornerShape(8.dp)).background(OrionBlue),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Shield, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(10.dp))
        Text("OrionPay", color = OrionText, fontSize = 17.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.weight(1f))
        Row(
            Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(OrionNavyLight)
                .padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Lock, contentDescription = null, tint = OrionSuccess, modifier = Modifier.size(11.dp))
            Spacer(Modifier.width(4.dp))
            Text("Seguro", color = OrionSuccess, fontSize = 11.sp)
        }
    }
}

@Composable
fun CardInfoPanel(brand: String, holder: String, number: String, expiry: String, entry: String) {
    val brandColor = when (brand.uppercase()) {
        "VISA"       -> Color(0xFF60A5FA)
        "MASTERCARD" -> Color(0xFFFC8181)
        "ELO"        -> Color(0xFFFFD700)
        "AMEX"       -> Color(0xFF6EE7B7)
        else          -> OrionBlue
    }
    Box(
        Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.linearGradient(listOf(OrionNavyMid, OrionNavyLight)))
            .border(1.dp, OrionNavyLight, RoundedCornerShape(16.dp))
            .padding(18.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(width=30.dp, height=22.dp).clip(RoundedCornerShape(3.dp)).background(OrionWarning.copy(alpha=0.85f)))
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(brand.ifEmpty { "—" }, color = brandColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text(entry, color = OrionTextMuted, fontSize = 11.sp)
                }
                Spacer(Modifier.weight(1f))
                Icon(Icons.Default.CreditCard, contentDescription = null, tint = OrionTextSub, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.height(14.dp))
            Text(
                maskDisplay(number).ifEmpty { "•••• •••• •••• ••••" },
                color = OrionText, fontSize = 16.sp, fontWeight = FontWeight.Medium,
                letterSpacing = 2.sp, fontFamily = FontFamily.Monospace
            )
            Spacer(Modifier.height(10.dp))
            Row {
                Column {
                    Text("TITULAR", color = OrionTextSub, fontSize = 10.sp)
                    Text(holder.ifEmpty { "—" }.uppercase(), color = OrionText, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                }
                Spacer(Modifier.width(28.dp))
                Column {
                    Text("VALIDADE", color = OrionTextSub, fontSize = 10.sp)
                    Text(expiry.ifEmpty { "—" }, color = OrionText, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

@Composable
fun AmountInput(
    rawAmount: String, formatted: String,
    onDigit: (String) -> Unit, onBackspace: () -> Unit, onClear: () -> Unit
) {
    Column(
        Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(OrionNavyMid)
            .border(1.dp, OrionNavyLight, RoundedCornerShape(16.dp))
            .padding(18.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            formatted,
            color      = if (rawAmount.isEmpty()) OrionTextMuted else OrionText,
            fontSize   = 34.sp,
            fontWeight = FontWeight.Bold,
            textAlign  = TextAlign.Center
        )
        Spacer(Modifier.height(14.dp))
        HorizontalDivider(color = OrionNavyLight)
        Spacer(Modifier.height(14.dp))
        listOf(
            listOf("1","2","3"),
            listOf("4","5","6"),
            listOf("7","8","9"),
            listOf("C","0","⌫")
        ).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { key ->
                    Box(
                        Modifier.weight(1f).height(60.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                when (key) {
                                    "C"  -> OrionError.copy(alpha = 0.15f)
                                    "⌫" -> OrionNavyLight.copy(alpha = 0.8f)
                                    else -> OrionNavyLight.copy(alpha = 0.5f)
                                }
                            )
                            .clickable {
                                when (key) { "⌫" -> onBackspace(); "C" -> onClear(); else -> onDigit(key) }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(key, color = if (key == "C") OrionError else OrionText, fontSize = 20.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
fun ProductSelector(selected: ProductType, onSelect: (ProductType) -> Unit) {
    Column(
        Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(OrionNavyMid)
            .border(1.dp, OrionNavyLight, RoundedCornerShape(16.dp))
    ) {
        ProductType.entries.forEachIndexed { idx, pt ->
            Row(
                Modifier.fillMaxWidth()
                    .clickable { onSelect(pt) }
                    .background(if (pt == selected) OrionBlue.copy(alpha = 0.12f) else Color.Transparent)
                    .padding(horizontal = 18.dp, vertical = 13.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = pt == selected, onClick = { onSelect(pt) },
                    colors   = RadioButtonDefaults.colors(selectedColor = OrionBlue, unselectedColor = OrionTextSub)
                )
                Spacer(Modifier.width(8.dp))
                Text(pt.label, color = OrionText, fontSize = 14.sp, modifier = Modifier.weight(1f))
                if (pt == selected)
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = OrionBlue, modifier = Modifier.size(17.dp))
            }
            if (idx < ProductType.entries.lastIndex)
                HorizontalDivider(color = OrionNavyLight)
        }
    }
}

@Composable
fun ExpandableSection(
    title: String, expanded: Boolean, onToggle: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(OrionNavyMid)
            .border(1.dp, OrionNavyLight, RoundedCornerShape(16.dp))
    ) {
        Row(
            Modifier.fillMaxWidth().clickable(onClick = onToggle).padding(horizontal = 18.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Settings, contentDescription = null, tint = OrionTextMuted, modifier = Modifier.size(17.dp))
            Spacer(Modifier.width(10.dp))
            Text(title, color = OrionText, fontSize = 14.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
            Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, contentDescription = null, tint = OrionTextMuted)
        }
        AnimatedVisibility(visible = expanded) {
            Column(Modifier.padding(horizontal = 18.dp, vertical = 4.dp), content = content)
        }
    }
}

@Composable
fun PayloadSummary(amount: String, product: ProductType, brand: String, masked: String, entry: String) {
    Column(
        Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(OrionNavyMid)
            .border(1.dp, OrionNavyLight, RoundedCornerShape(16.dp))
            .padding(18.dp)
    ) {
        Text("Resumo", color = OrionTextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(10.dp))
        listOf(
            "Valor"    to amount,
            "Produto"  to product.label,
            "Bandeira" to brand.ifEmpty { "—" },
            "Cartão"   to masked.ifEmpty { "—" },
            "Entrada"  to entry,
            "Moeda"    to "BRL (986)",
            "País"     to "Brasil (076)"
        ).forEach { (label, value) ->
            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(label, color = OrionTextMuted, fontSize = 13.sp)
                Text(value, color = OrionText,      fontSize = 13.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
fun ResultPanel(result: TxResult) {
    val ok = result.state == TxState.SUCCESS
    Column(
        Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (ok) OrionSuccess.copy(alpha = 0.1f) else OrionError.copy(alpha = 0.1f))
            .border(1.dp, if (ok) OrionSuccess.copy(alpha = 0.4f) else OrionError.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
            .padding(18.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                if (ok) Icons.Default.CheckCircle else Icons.Default.Error,
                contentDescription = null,
                tint = if (ok) OrionSuccess else OrionError, modifier = Modifier.size(22.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                if (ok) "Transação aprovada" else "Transação recusada",
                color = if (ok) OrionSuccess else OrionError, fontSize = 15.sp, fontWeight = FontWeight.Bold
            )
        }
        if (result.authCode.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Text("Autorização: ${result.authCode}", color = OrionText, fontSize = 13.sp)
        }
        if (result.nsu.isNotEmpty()) {
            Text("NSU: ${result.nsu}", color = OrionText, fontSize = 13.sp)
        }
        if (result.message.isNotEmpty()) {
            Spacer(Modifier.height(6.dp))
            Text(result.message, color = OrionTextMuted, fontSize = 12.sp)
        }
    }
}

@Composable
fun TxSectionLabel(text: String) {
    Text(text, color = OrionTextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(bottom = 8.dp))
}

@Composable
fun orionTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor    = OrionBlue,
    unfocusedBorderColor  = OrionNavyLight,
    focusedTextColor      = OrionText,
    unfocusedTextColor    = OrionText,
    cursorColor           = OrionBlue,
    focusedContainerColor   = OrionNavyMid,
    unfocusedContainerColor = OrionNavyMid,
)

// =============================================================================
// LÓGICA DE TRANSAÇÃO
// =============================================================================

// =============================================================================
// CONFIGURAÇÃO DE REDE
// Troque o IP conforme seu ambiente:
//   10.0.2.2       → Emulador Android Studio (AVD padrão) apontando para o PC
//   1192.168.201.156   → Emulador Genymotion / AVD bridge mode apontando para o PC
//   192.168.X.X    → Dispositivo físico na mesma rede Wi-Fi que o PC
// O IP do host PC no Genymotion/bridge é sempre 192.168.56.1
// =============================================================================
private const val API_HOST      = "192.168.201.156"          // ← altere aqui se necessário
private const val API_PORT      = "8080"
private const val API_BASE      = "http://$API_HOST:$API_PORT/api/v1"
private const val API_URL       = "$API_BASE/transactions/authorize"
private const val API_EMAIL_URL = "$API_BASE/transactions/{transactionId}/send-email"
private const val MERCHANT_ID   = "3f90ed27-6eca-4bf6-a4e1-607ac55ea73b"

private const val API_LOGIN_URL = "$API_BASE/auth/login" // Ajustado para o padrão /api/v1/auth/login

fun buildTxPayload(
    merchantId: String, amount: Double, productType: String,
    terminalSn: String, externalRef: String, entryMode: String,
    cardBrand: String, cardHolder: String, cardNumber: String,
    expirationDate: String, cvv: String,
    // Campos EMV opcionais — enviados quando disponíveis
    cryptogram: String = "",
    atc: String = "",
    iad: String = "",
    aip: String = "",
    tvr: String = ""
): JSONObject = JSONObject().apply {
    // ── Campos obrigatórios ───────────────────────────────────────
    put("merchantId",        merchantId)
    put("amount",            java.math.BigDecimal(amount).setScale(2, java.math.RoundingMode.HALF_UP))
    put("productType",       productType)
    put("terminalSn",        terminalSn)
    put("externalReference", externalRef)
    put("entryMode",         entryMode)
    put("cardBrand",         cardBrand)
    put("cardHolderName",    cardHolder.ifEmpty { "NAO INFORMADO" })
    put("cardNumber",        cardNumber.filter { it.isDigit() })
    put("expirationDate",    expirationDate)
    put("cvv",               cvv)
    put("currencyCode",      "986")
    put("countryCode",       "076")
    put("transactionDate",   isoNow())
    // ── Campos EMV complementares (enviados se presentes) ─────────
    if (cryptogram.isNotEmpty()) put("applicationCryptogram", cryptogram)
    if (atc.isNotEmpty())        put("atc",                   atc)
    if (iad.isNotEmpty())        put("issuerApplicationData", iad)
    if (aip.isNotEmpty())        put("aip",                   aip)
    if (tvr.isNotEmpty())        put("tvr",                   tvr)
}

// Versão que retorna também o body bruto — usada no diagnóstico
suspend fun sendTransactionRaw(payload: JSONObject, token: String): Pair<TxResult, String> = withContext(Dispatchers.IO) {
    try {
        val diagKey = java.util.UUID.randomUUID().toString()
        val conn = (URL(API_URL).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            setRequestProperty("Content-Type",      "application/json; charset=utf-8")
            setRequestProperty("Accept",             "application/json")

            // ADICIONE ESTA LINHA (Igual ao Postman)
            setRequestProperty("Authorization", "Bearer $token")

            setRequestProperty("X-Merchant-Id",      payload.optString("merchantId", MERCHANT_ID))
            setRequestProperty("X-Idempotency-Key",  diagKey)
            doOutput       = true
            connectTimeout = 15_000
            readTimeout    = 30_000
        }
        val body = payload.toString()
        Log.d("ORION_DIAG", "POST $API_URL  key=$diagKey\n${maskSensitiveLog(body)}")
        OutputStreamWriter(conn.outputStream, "UTF-8").use { it.write(body) }

        val code = conn.responseCode
        val raw  = runCatching {
            (if (code in 200..299) conn.inputStream else conn.errorStream)
                ?.bufferedReader()?.readText() ?: ""
        }.getOrDefault("")
        Log.d("ORION_DIAG", "HTTP $code <- $raw")

        val json = runCatching { JSONObject(raw) }.getOrNull()
        val result = if (code in 200..299) {
            TxResult(
                state         = TxState.SUCCESS,
                message       = json?.optString("message", "Aprovado") ?: "Aprovado",
                authCode      = json?.optString("authorizationCode") ?: json?.optString("authCode") ?: "",
                nsu           = json?.optString("nsu") ?: json?.optString("nsuHost") ?: "",
                transactionId = json?.optString("id") ?: json?.optString("transactionId") ?: json?.optString("transaction_id") ?: "",
                rawJson       = raw
            )
        } else {
            TxResult(
                state   = TxState.ERROR,
                message = json?.optString("message") ?: json?.optString("error") ?: "HTTP $code",
                rawJson = raw
            )
        }
        Pair(result, "HTTP $code\n\n$raw")
    } catch (e: java.net.ConnectException) {
        Pair(TxResult(TxState.ERROR, message = "Conexão recusada — API offline ou porta errada"), "ConnectException: ${e.message}")
    } catch (e: java.net.SocketTimeoutException) {
        Pair(TxResult(TxState.ERROR, message = "Timeout — API não respondeu em 30s"), "SocketTimeoutException: ${e.message}")
    } catch (e: Exception) {
        Pair(TxResult(TxState.ERROR, message = e.message ?: "Erro desconhecido"), "${e.javaClass.simpleName}: ${e.message}")
    }
}

// Mascara PAN, CVV e dados sensíveis antes de imprimir no Logcat
// Nunca imprima cardNumber ou cvv em texto claro — qualquer app com READ_LOGS captura
fun maskSensitiveLog(json: String): String {
    return json
        .replace(Regex(""""cardNumber"\s*:\s*"[^"]+""""))  { """"cardNumber":"****"""" }
        .replace(Regex(""""cvv"\s*:\s*"[^"]+""""))          { """"cvv":"***"""" }
        .replace(Regex(""""expirationDate"\s*:\s*"[^"]+"""")) { """"expirationDate":"**/**"""" }
        .replace(Regex(""""cardHolderName"\s*:\s*"[^"]+"""")) { m ->
            val name = m.value.substringAfter(":").trim().trim('"')
            val masked = name.take(1) + "*".repeat(maxOf(name.length - 2, 1)) + name.takeLast(1)
            """"cardHolderName":"$masked""""
        }
}

fun isoNow(): String {
    val c = Calendar.getInstance()
    return "%04d-%02d-%02dT%02d:%02d:%02d".format(
        c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH),
        c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), c.get(Calendar.SECOND)
    )
}

suspend fun sendTransaction(
    payload: JSONObject,
    idempotencyKey: String = java.util.UUID.randomUUID().toString(),
    token: String
): TxResult = withContext(Dispatchers.IO) {
    Log.d("ORION_IDEM", "Enviando — X-Idempotency-Key: $idempotencyKey")
    try {
        val conn = (URL(API_URL).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            setRequestProperty("Content-Type",       "application/json; charset=utf-8")
            setRequestProperty("Accept",              "application/json")


            setRequestProperty("Authorization",      "Bearer $token")

            setRequestProperty("X-Merchant-Id",       payload.optString("merchantId", MERCHANT_ID))
            setRequestProperty("X-Idempotency-Key",   idempotencyKey)
            doOutput       = true
            connectTimeout = 15_000
            readTimeout    = 30_000
        }
        val body = payload.toString()
        Log.d("ORION_TX", "POST $API_URL\n${maskSensitiveLog(body)}")
        OutputStreamWriter(conn.outputStream, "UTF-8").use { it.write(body) }
        val code = conn.responseCode
        val resp = runCatching {
            (if (code in 200..299) conn.inputStream else conn.errorStream)
                ?.bufferedReader()?.readText() ?: ""
        }.getOrDefault("")
        Log.d("ORION_TX", "HTTP $code ← $resp")

        if (code in 200..299) {
            val json = runCatching { JSONObject(resp) }.getOrNull()
            TxResult(
                state         = TxState.SUCCESS,
                message       = json?.optString("message", "Aprovado") ?: "Aprovado",
                authCode      = json?.optString("authorizationCode")
                    ?: json?.optString("authCode")
                    ?: json?.optString("authorization_code") ?: "",
                nsu           = json?.optString("nsu") ?: json?.optString("nsuHost") ?: "",
                transactionId = json?.optString("id")
                    ?: json?.optString("transactionId")
                    ?: json?.optString("transaction_id") ?: "",
                rawJson       = resp
            )
        } else {
            val json = runCatching { JSONObject(resp) }.getOrNull()
            TxResult(
                state   = TxState.ERROR,
                message = json?.optString("message")
                    ?: json?.optString("error")
                    ?: json?.optString("detail")
                    ?: "Erro HTTP $code",
                rawJson = resp
            )
        }
    } catch (e: java.net.ConnectException) {
        TxResult(TxState.ERROR, message = "Servidor indisponível — verifique se a API está na porta 8080")
    } catch (e: java.net.SocketTimeoutException) {
        TxResult(TxState.ERROR, message = "Timeout — API não respondeu em 30s")
    } catch (e: Exception) {
        Log.e("ORION_TX", "Erro", e)
        TxResult(TxState.ERROR, message = e.message ?: "Erro desconhecido")
    }
}

// =============================================================================
// EMV — SW HELPERS
// =============================================================================
fun isSw9000(r: ByteArray) =
    r.size >= 2 && (r[r.size-2].toInt() and 0xFF) == 0x90 && (r[r.size-1].toInt() and 0xFF) == 0x00

fun ByteArray.dropSw() = if (size >= 2) copyOfRange(0, size - 2) else this

fun ByteArray.swHex() =
    if (size >= 2) "%02X%02X".format(this[size-2].toInt() and 0xFF, this[size-1].toInt() and 0xFF)
    else "????"

// =============================================================================
// EMV — TLV PARSER
// =============================================================================
val knownAids = listOf(
    "A0000000031010", "A0000000032010", "A0000000033010",
    "A0000000041010", "A0000000043060",
    "A00000002501",   "A0000000651010"
)

fun parseTlv(data: ByteArray): List<Tlv> {
    val list = mutableListOf<Tlv>()
    var i = 0
    while (i < data.size) {
        val b0 = data[i].toInt() and 0xFF; i++
        val tag = if ((b0 and 0x1F) == 0x1F) {
            if (i >= data.size) break
            val b1 = data[i].toInt() and 0xFF; i++
            "%02X%02X".format(b0, b1)
        } else "%02X".format(b0)
        if (i >= data.size) break
        val lb = data[i].toInt() and 0xFF; i++
        val len = when (lb) {
            0x81 -> { if (i >= data.size) break; val v = data[i].toInt() and 0xFF; i++; v }
            0x82 -> { if (i+1 >= data.size) break; val v = ((data[i].toInt() and 0xFF) shl 8) or (data[i+1].toInt() and 0xFF); i+=2; v }
            else -> lb
        }
        if (i + len > data.size) break
        val value = data.copyOfRange(i, i + len); i += len
        if (tag != "00") list.add(Tlv(tag, len, value))
    }
    return list
}

fun flattenTlv(tlvs: List<Tlv>): List<Tlv> {
    val result = mutableListOf<Tlv>()
    for (tlv in tlvs) {
        result.add(tlv)
        val fb = tlv.tag.take(2).toIntOrNull(16) ?: continue
        if ((fb and 0x20) != 0 && tlv.value.isNotEmpty()) {
            val inner = parseTlv(tlv.value)
            if (inner.isNotEmpty()) result.addAll(flattenTlv(inner))
        }
    }
    return result
}

fun findTag(tlvs: List<Tlv>, tag: String): ByteArray? =
    tlvs.firstOrNull { it.tag.equals(tag, ignoreCase = true) }?.value

fun findPan(tlvs: List<Tlv>): ByteArray? {
    findTag(tlvs, "5A")?.let { return it }
    findTag(tlvs, "57")?.let { track ->
        val hex = track.toHex()
        val sep = hex.indexOf('D').takeIf { it > 0 } ?: return null
        val pan = hex.substring(0, sep).trimEnd('F','f')
        return pan.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
    }
    return null
}

// =============================================================================
// EMV — APDU
// =============================================================================
fun apduSelect(name: String): ByteArray {
    val b = name.toByteArray(Charsets.US_ASCII)
    return byteArrayOf(0x00, 0xA4.toByte(), 0x04, 0x00, b.size.toByte()) + b + byteArrayOf(0x00)
}

fun apduSelectHex(hex: String): ByteArray {
    val b = hex.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
    return byteArrayOf(0x00, 0xA4.toByte(), 0x04, 0x00, b.size.toByte()) + b + byteArrayOf(0x00)
}

fun buildGpo(pdol: ByteArray?): ByteArray {
    if (pdol == null || pdol.isEmpty())
        return byteArrayOf(0x80.toByte(),0xA8.toByte(),0x00,0x00,0x02,0x83.toByte(),0x00,0x00)
    val data = mutableListOf<Byte>()
    var i = 0
    while (i < pdol.size) {
        val b0 = pdol[i].toInt() and 0xFF; i++
        val tag = if ((b0 and 0x1F) == 0x1F) {
            if (i >= pdol.size) break
            val b1 = pdol[i].toInt() and 0xFF; i++
            "%02X%02X".format(b0, b1)
        } else "%02X".format(b0)
        if (i >= pdol.size) break
        val len = pdol[i].toInt() and 0xFF; i++
        data.addAll(pdolValueFor(tag, len).toList())
    }
    val pd = byteArrayOf(0x83.toByte(), data.size.toByte()) + data.toByteArray()
    return byteArrayOf(0x80.toByte(),0xA8.toByte(),0x00,0x00,pd.size.toByte()) + pd + byteArrayOf(0x00)
}

fun pdolValueFor(tag: String, len: Int): ByteArray = when (tag.uppercase()) {
    "9F66" -> byteArrayOf(0x36,0x00,0x40,0x00).padTo(len)
    "9F02" -> byteArrayOf(0x00,0x00,0x00,0x00,0x00,0x01).padTo(len)
    "9F03" -> ByteArray(len)
    "9F1A" -> byteArrayOf(0x00,0x76).padTo(len)
    "5F2A" -> byteArrayOf(0x09.toByte(),0x86.toByte()).padTo(len)
    "9A"   -> byteArrayOf(0x25,0x01,0x01).padTo(len)
    "9F21" -> byteArrayOf(0x12,0x00,0x00).padTo(len)
    "9C"   -> byteArrayOf(0x00).padTo(len)
    "9F37" -> byteArrayOf(0x01,0x02,0x03,0x04).padTo(len)
    "9F35" -> byteArrayOf(0x22).padTo(len)
    else   -> ByteArray(len)
}

fun ByteArray.padTo(size: Int) = if (this.size >= size) copyOf(size) else this + ByteArray(size - this.size)

fun buildReadRecord(record: Int, sfi: Int) =
    byteArrayOf(0x00, 0xB2.toByte(), record.toByte(), ((sfi shl 3) or 4).toByte(), 0x00)

fun getResponse(iso: IsoDep, resp: ByteArray): ByteArray {
    var cur = resp
    repeat(8) {
        if (cur.size < 2) return cur
        val sw1 = cur[cur.size-2].toInt() and 0xFF
        val sw2 = cur[cur.size-1].toInt() and 0xFF
        if (sw1 != 0x61) return cur
        val next = try { iso.transceive(byteArrayOf(0x00,0xC0.toByte(),0x00,0x00,sw2.toByte())) }
        catch (_: Exception) { return cur }
        cur = cur.dropSw() + next
    }
    return cur
}

fun extractAflFromGpo(data: ByteArray): ByteArray? {
    if (data.isEmpty()) return null
    return when (data[0].toInt() and 0xFF) {
        0x77 -> flattenTlv(parseTlv(data)).firstOrNull { it.tag.equals("94", ignoreCase = true) }?.value
        0x80 -> {
            if (data.size < 4) return null
            val len = data[1].toInt() and 0xFF
            if (data.size < 2 + len) return null
            val p = data.copyOfRange(2, 2 + len)
            if (p.size > 2) p.copyOfRange(2, p.size) else null
        }
        else -> null
    }
}

fun parseAfl(afl: ByteArray): List<AflEntry> {
    val list = mutableListOf<AflEntry>()
    var i = 0
    while (i + 3 < afl.size) {
        val sfi   = (afl[i].toInt() and 0xF8) shr 3
        val start = afl[i+1].toInt() and 0xFF
        val end   = afl[i+2].toInt() and 0xFF
        if (sfi > 0 && start > 0 && end >= start) list.add(AflEntry(sfi, start, end))
        i += 4
    }
    return list
}

// =============================================================================
// EMV — UTILS
// =============================================================================
fun transceive(iso: IsoDep, cmd: ByteArray): ByteArray = iso.transceive(cmd)
fun transceiveSafe(iso: IsoDep, cmd: ByteArray): ByteArray? =
    try { iso.transceive(cmd) } catch (_: Exception) { null }

fun ByteArray.toHex() = joinToString("") { "%02X".format(it) }
fun ByteArray.toAscii() = String(this, Charsets.ISO_8859_1)

fun extractAid(tlvs: List<Tlv>) = findTag(tlvs, "4F")?.toHex()

fun identifyBrand(aid: String) = when {
    aid.startsWith("A000000003") -> "VISA"
    aid.startsWith("A000000004") -> "MASTERCARD"
    aid.startsWith("A000000025") -> "AMEX"
    aid.startsWith("A000000065") -> "ELO"
    else -> "DESCONHECIDO"
}

fun maskPan(pan: String): String {
    val c = pan.trimEnd('F','f')
    return if (c.length >= 8) c.take(6) + "*".repeat(c.length - 10) + c.takeLast(4) else c
}

fun maskDisplay(number: String): String {
    val c = number.filter { it.isDigit() }
    return if (c.length >= 8) c.take(4) + " •••• •••• " + c.takeLast(4) else number
}

fun extractPanFromTrack2(track2Hex: String): String? {
    val sep = track2Hex.indexOf('D').takeIf { it > 0 } ?: return null
    return track2Hex.substring(0, sep).trimEnd('F','f')
}

// Track2: PAN D YYMM SC CVV2 PAD
// Após o separador D: 4 chars validade (YYMM) + 3 chars SC + 3 chars CVV2
fun extractCvv2FromTrack2(track2Hex: String): String {
    val sep = track2Hex.indexOf('D').takeIf { it > 0 } ?: return ""
    val afterSep = track2Hex.substring(sep + 1) // YYMM SC CVV2 PAD
    // YYMM = 4, SC = 3, CVV2 = 3 → começa no índice 7
    return if (afterSep.length >= 10) afterSep.substring(7, 10).trimEnd('F','f','D','d')
    else ""
}

// Tag 5F24 = YYMMDD (3 bytes = 6 hex chars) → MM/YY
// Tag 57 validade = YYMM (2 bytes = 4 hex chars) → MM/YY
fun formatExpiry(hex: String): String {
    val h = hex.uppercase().trimEnd('F')
    return when {
        h.length >= 6 -> "${h.substring(2, 4)}/${h.substring(0, 2)}" // YYMMDD → MM/YY
        h.length >= 4 -> "${h.substring(2, 4)}/${h.substring(0, 2)}" // YYMM   → MM/YY
        else          -> h
    }
}

fun interpretSw(resp: ByteArray): String {
    if (resp.size < 2) return "Resposta vazia"
    val sw1 = resp[resp.size-2].toInt() and 0xFF
    val sw2 = resp[resp.size-1].toInt() and 0xFF
    return when {
        sw1 == 0x69 && sw2 == 0x85 -> "6985: Condições não satisfeitas (PDOL inválido ou sequência errada)"
        sw1 == 0x67 && sw2 == 0x00 -> "6700: Lc errado — tamanho do GPO incorreto"
        sw1 == 0x6A && sw2 == 0x82 -> "6A82: Arquivo não encontrado"
        sw1 == 0x6A && sw2 == 0x86 -> "6A86: P1/P2 incorretos"
        sw1 == 0x6C -> "6C${"%02X".format(sw2)}: Le errado"
        sw1 == 0x61 -> "61${"%02X".format(sw2)}: Mais dados disponíveis"
        else -> "SW: ${"%02X%02X".format(sw1, sw2)}"
    }
}


suspend fun getInternalAuthToken(): String? = withContext(Dispatchers.IO) {
    try {
        val loginUrl = "http://192.168.15.187:8080/api/auth/login" // Ajuste o IP se necessário
        val conn = (URL(loginUrl).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
            setRequestProperty("Accept", "application/json")
            doOutput = true
            connectTimeout = 5000
            readTimeout = 5000
        }

        val loginBody = JSONObject().apply {
            put("email", "admin@orionpay.com.br")
            put("password", "password123")
        }

        OutputStreamWriter(conn.outputStream, "UTF-8").use { it.write(loginBody.toString()) }

        val code = conn.responseCode
        if (code == 200) {
            val response = conn.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(response)
            return@withContext json.optString("accessToken")
        } else {
            Log.e("ORION_AUTH", "Erro no login automático: HTTP $code")
            null
        }
    } catch (e: Exception) {
        Log.e("ORION_AUTH", "Falha de conexão no login", e)
        null
    }
}