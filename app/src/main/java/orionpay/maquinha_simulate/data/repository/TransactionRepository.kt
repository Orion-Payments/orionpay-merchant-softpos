package orionpay.maquinha_simulate.data.repository

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import orionpay.maquinha_simulate.data.model.TxResult
import orionpay.maquinha_simulate.data.model.TxState
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*

class TransactionRepository {

    private companion object {
        const val API_URL = "http://10.0.2.2:8080/api/v1/transactions/authorize"
    }

    suspend fun authorize(payload: JSONObject): TxResult = withContext(Dispatchers.IO) {
        try {
            val conn = (URL(API_URL).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json")
                doOutput = true
                connectTimeout = 10000
                readTimeout = 30000
            }
            
            OutputStreamWriter(conn.outputStream).use { it.write(payload.toString()) }
            
            val code = conn.responseCode
            val resp = (if (code in 200..299) conn.inputStream else conn.errorStream)
                .bufferedReader().readText()
            
            val json = JSONObject(resp)
            if (code in 200..299) {
                TxResult(
                    state = TxState.SUCCESS,
                    message = json.optString("message", "Aprovada"),
                    authCode = json.optString("authorizationCode"),
                    nsu = json.optString("nsu")
                )
            } else {
                TxResult(
                    state = TxState.ERROR,
                    message = json.optString("message", "Erro $code")
                )
            }
        } catch (e: Exception) {
            Log.e("TransactionRepo", "Network Error", e)
            TxResult(TxState.ERROR, message = e.message ?: "Erro de conexão")
        }
    }

    fun buildPayload(
        merchantId: String,
        amount: Double,
        productType: String,
        terminalSn: String,
        externalRef: String,
        entryMode: String,
        brand: String,
        holder: String,
        number: String,
        expiry: String,
        cvv: String
    ): JSONObject = JSONObject().apply {
        put("merchantId", merchantId)
        put("amount", amount)
        put("productType", productType)
        put("terminalSn", terminalSn)
        put("externalReference", externalRef)
        put("entryMode", entryMode)
        put("cardBrand", brand)
        put("cardHolderName", holder.ifEmpty { "NAO INFORMADO" })
        put("cardNumber", number)
        put("expirationDate", expiry)
        put("cvv", cvv)
        put("currencyCode", "986")
        put("countryCode", "076")
        put("transactionDate", SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).format(Date()))
    }
}
