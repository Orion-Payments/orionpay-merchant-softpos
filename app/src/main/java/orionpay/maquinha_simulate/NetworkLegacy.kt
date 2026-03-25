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
    put("transactionDate", isoNow())
    // ── Campos EMV complementares (enviados se presentes) ─────────
    if (cryptogram.isNotEmpty()) put("applicationCryptogram", cryptogram)
    if (atc.isNotEmpty())        put("atc",                   atc)
    if (iad.isNotEmpty())        put("issuerApplicationData", iad)
    if (aip.isNotEmpty())        put("aip",                   aip)
    if (tvr.isNotEmpty())        put("tvr",                   tvr)
}



suspend fun sendTransactionRaw(
    payload: JSONObject,
    token: String,
    idempotencyKey: String = java.util.UUID.randomUUID().toString()
): TxResult = withContext(Dispatchers.IO) {
    Log.d("ORION_IDEM", "Enviando — X-Idempotency-Key: $idempotencyKey")
    try {
        val conn = (URL(ApiConfig.TRANSACTIONS_AUTHORIZE).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            setRequestProperty("Content-Type",       "application/json; charset=utf-8")
            setRequestProperty("Accept",              "application/json")


            setRequestProperty("Authorization",      "Bearer $token")

            setRequestProperty("X-Merchant-Id",       payload.optString("merchantId", ApiConfig.MERCHANT_ID))
            setRequestProperty("X-Idempotency-Key",   idempotencyKey)
            doOutput       = true
            connectTimeout = ApiConfig.CONNECT_TIMEOUT
            readTimeout    = ApiConfig.READ_TIMEOUT
        }
        val body = payload.toString()
        Log.d("ORION_TX", "POST ${ApiConfig.TRANSACTIONS_AUTHORIZE}\n${maskSensitiveLog(body)}")
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
        TxResult(TxState.ERROR, message = "Servidor indisponível — verifique se a API está no IP ${ApiConfig.BASE_URL}")
    } catch (e: java.net.SocketTimeoutException) {
        TxResult(TxState.ERROR, message = "Timeout — API não respondeu")
    } catch (e: Exception) {
        Log.e("ORION_TX", "Erro", e)
        TxResult(TxState.ERROR, message = e.message ?: "Erro desconhecido")
    }
}



suspend fun sendTransactionRaw_(payload: JSONObject, token: String,  idempotencyKey: String = java.util.UUID.randomUUID().toString()): Pair<TxResult, String> =
    withContext(Dispatchers.IO) {
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
            currencyCode = payload.getString("currencyCode"),
            countryCode = payload.getString("countryCode"),
            transactionDateIso = payload.getString("transactionDate")
        )

        return@withContext try {
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
                transactionId = result.transactionId ?: ""
            )
            Pair(txResult, result.rawJson ?: "")
        } catch (e: Exception) {
            val errorResult = TxResult(
                state = TxState.ERROR,
                message = e.message ?: "Erro ao enviar transação"
            )
            Pair(errorResult, "")
        }
    }
