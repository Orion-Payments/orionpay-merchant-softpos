package orionpay.maquinha_simulate

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import orionpay.maquinha_simulate.config.ApiConfig
import orionpay.maquinha_simulate.data.model.TxResult
import orionpay.maquinha_simulate.domain.enums.TxState
import orionpay.maquinha_simulate.domain.model.TransactionDomain
import orionpay.maquinha_simulate.infrastructure.auth.HttpTerminalAuthAdapter
import orionpay.maquinha_simulate.infrastructure.network.HttpPaymentGatewayAdapter
import org.json.JSONObject
import orionpay.maquinha_simulate.utils.isoNow
import orionpay.maquinha_simulate.utils.maskSensitiveLog
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

// Legacy-style helpers used by DiagnosticActivity, implemented on top of the
// new hexagonal adapters. This keeps UI/screens unchanged.

private val terminalAuth = HttpTerminalAuthAdapter(baseUrl = ApiConfig.BASE_URL)
private val paymentGateway = HttpPaymentGatewayAdapter(baseUrl = ApiConfig.BASE_URL)

suspend fun getInternalAuthToken(): String? = withContext(Dispatchers.IO) {
    terminalAuth.login()
}

fun buildTxPayload(
    merchantId: String,
    amount: Double,
    productType: String,
    terminalSn: String,
    externalRef: String,
    entryMode: String,
    cardBrand: String,
    cardHolder: String,
    cardNumber: String,
    expirationDate: String,
    cvv: String,
    // Campos EMV opcionais — enviados quando disponíveis
    cryptogram: String = "",
    atc: String = "",
    iad: String = "",
    aip: String = "",
    tvr: String = "",
    pinData: String = "",
    stan: String? = null
): JSONObject = JSONObject().apply {
    // ── Campos obrigatórios ───────────────────────────────────────
    put("merchantId", merchantId)
    put("amount", java.math.BigDecimal(amount).setScale(2, java.math.RoundingMode.HALF_UP))
    put("productType", productType)
    put("terminalSn", terminalSn)
    put("externalReference", externalRef)
    put("entryMode", entryMode)
    put("cardBrand", cardBrand)
    put("cardHolderName", cardHolder.ifEmpty { "NAO INFORMADO" })
    put("cardNumber", cardNumber.filter { it.isDigit() })
    put("expirationDate", expirationDate)

    // Condicional de CVV: enviado apenas se for MANUAL. CHIP/CONTACTLESS envia ""
    val finalCvv = if (entryMode == "MANUAL") cvv else ""
    put("cvv", finalCvv)

    put("currencyCode", "986")
    put("countryCode", "076")
    put("transactionDate", isoNow())

    // ── Campos EMV complementares (enviados se presentes) ─────────
    // cryptogram e atc são sempre incluídos se não estiverem vazios (capturados via NFC)
    if (cryptogram.isNotEmpty()) put("applicationCryptogram", cryptogram)
    if (atc.isNotEmpty()) put("atc", atc)

    if (iad.isNotEmpty()) put("issuerApplicationData", iad)
    if (aip.isNotEmpty()) put("aip", aip)
    if (tvr.isNotEmpty()) put("tvr", tvr)
    if (pinData.isNotEmpty()) put("pinData", pinData)
    
    // Gerar STAN aleatório se não fornecido (6 dígitos, ISO-8583 compliant)
    val finalStan = stan ?: (1..999999).random().toString().padStart(6, '0')
    put("stan", finalStan)
}

/**
 * Envia transação usando o novo fluxo de domínio (TransactionDomain).
 */
suspend fun sendTransactionRaw(
    payload: JSONObject,
    token: String,
    idempotencyKey: String = java.util.UUID.randomUUID().toString()
): TxResult = withContext(Dispatchers.IO) {
    val resultAndRaw = sendTransactionRaw_(payload, token, idempotencyKey)
    return@withContext resultAndRaw.first
}


suspend fun sendTransactionRaw_(
    payload: JSONObject,
    token: String,
    idempotencyKey: String = java.util.UUID.randomUUID().toString()
): Pair<TxResult, String> = withContext(Dispatchers.IO)
{
    val tx = TransactionDomain(
        merchantId = payload.getString("merchantId"),
        amount = payload.getDouble("amount"),
        productType = orionpay.maquinha_simulate.domain.enums.ProductType.valueOf(
            payload.getString("productType")
        ),
        terminalSn = payload.getString("terminalSn"),
        externalReference = payload.getString("externalReference"),
        entryMode = payload.getString("entryMode"),
        cardBrand = payload.getString("cardBrand"),
        cardHolderName = payload.getString("cardHolderName"),
        cardNumber = payload.getString("cardNumber"),
        expirationDate = payload.getString("expirationDate"),
        cvv = payload.getString("cvv"),
        currencyCode = payload.optString("currencyCode", "986"),
        countryCode = payload.optString("countryCode", "076"),
        transactionDateIso = payload.getString("transactionDate"),
        applicationCryptogram = payload.optString("applicationCryptogram").takeIf { it.isNotEmpty() },
        atc = payload.optString("atc").takeIf { it.isNotEmpty() } ?: "01",
        issuerApplicationData = payload.optString("issuerApplicationData").takeIf { it.isNotEmpty() },
        aip = payload.optString("aip").takeIf { it.isNotEmpty() },
        tvr = payload.optString("tvr").takeIf { it.isNotEmpty() },
        pinData = payload.optString("pinData").takeIf { it.isNotEmpty() },
        stan = payload.optString("stan").takeIf { it.isNotEmpty() }
    )

    return@withContext try {
        Log.d("ORION_IDEM", "Enviando via Domain — X-Idempotency-Key: $idempotencyKey")
        val result = paymentGateway.process(tx, idempotencyKey, token)

        val txResult = TxResult(
            state = when (result.state) {
                TxState.SUCCESS -> TxState.SUCCESS
                TxState.ERROR -> TxState.ERROR
                TxState.IDLE -> TxState.IDLE
                TxState.LOADING -> TxState.LOADING
                TxState.RETRYING -> TxState.RETRYING
            },
            message = result.message,
            authCode = result.authCode ?: "",
            nsu = result.nsu ?: "",
            transactionId = result.transactionId ?: "",
            rawJson = result.rawJson ?: ""
        )
        Pair(txResult, result.rawJson ?: "")
    } catch (e: Exception) {
        Log.e("ORION_TX", "Erro no processamento via Domain", e)
        val errorResult = TxResult(
            state = TxState.ERROR,
            message = e.message ?: "Erro ao enviar transação"
        )
        Pair(errorResult, "")
    }
}
