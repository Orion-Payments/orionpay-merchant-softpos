package orionpay.maquinha_simulate.infrastructure.network

import android.util.Log
import org.json.JSONObject
import orionpay.maquinha_simulate.config.ApiConfig
import orionpay.maquinha_simulate.domain.enums.TxState
import orionpay.maquinha_simulate.domain.model.TransactionDomain
import orionpay.maquinha_simulate.domain.model.TransactionResultDomain
import orionpay.maquinha_simulate.domain.port.PaymentGatewayPort
import orionpay.maquinha_simulate.utils.maskSensitiveLog
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

/**
 * Implementação do Gateway via chamadas HTTP (POST).
 */
class HttpPaymentGatewayAdapter(
    private val baseUrl: String = ApiConfig.BASE_URL
) : PaymentGatewayPort {

    override suspend fun process(
        tx: TransactionDomain,
        idempotencyKey: String,
        token: String
    ): TransactionResultDomain {
        return try {
            val url = URL(ApiConfig.TRANSACTIONS_AUTHORIZE)
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 15_000
                readTimeout = 30_000
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
                setRequestProperty("Accept", "application/json")
                setRequestProperty("Authorization", "Bearer $token")
                setRequestProperty("X-Merchant-Id", tx.merchantId)
                setRequestProperty("X-Idempotency-Key", idempotencyKey)
            }

            val body = JSONObject().apply {
                put("merchantId", tx.merchantId)
                put("amount", tx.amount)
                put("productType", tx.productType.name)
                put("terminalSn", tx.terminalSn)
                put("externalReference", tx.externalReference)
                put("entryMode", tx.entryMode)
                put("cardBrand", tx.cardBrand)
                put("cardHolderName", tx.cardHolderName)
                put("cardNumber", tx.cardNumber)
                put("expirationDate", tx.expirationDate)
                put("cvv", tx.cvv)
                put("currencyCode", tx.currencyCode)
                put("countryCode", tx.countryCode)
                put("transactionDate", tx.transactionDateIso)
            }.toString()

            Log.d("ORION_GATEWAY", "Enviando: ${maskSensitiveLog(body)}")
            OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { it.write(body) }

            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val resp = BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).use { it.readText() }

            Log.d("ORION_GATEWAY", "HTTP $code: $resp")

            val json = JSONObject(resp)
            if (code in 200..299) {
                TransactionResultDomain(
                    state = TxState.SUCCESS,
                    message = json.optString("message", "Aprovado"),
                    authCode = json.optString("authorizationCode"),
                    nsu = json.optString("nsu"),
                    transactionId = json.optString("id"),
                    rawJson = resp
                )
            } else {
                TransactionResultDomain(
                    state = TxState.ERROR,
                    message = json.optString("message", "Erro $code"),
                    rawJson = resp
                )
            }
        } catch (e: Exception) {
            Log.e("ORION_GATEWAY", "Falha na comunicação", e)
            TransactionResultDomain(
                state = TxState.ERROR,
                message = e.message ?: "Erro desconhecido",
                rawJson = e.toString()
            )
        }
    }
}
