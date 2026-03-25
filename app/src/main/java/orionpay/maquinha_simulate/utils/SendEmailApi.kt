package orionpay.maquinha_simulate.utils

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import orionpay.maquinha_simulate.config.ApiConfig
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL


suspend fun callSendEmailApi(
    transactionId: String,
    email: String
): Pair<Boolean, String> = withContext(Dispatchers.IO) {
    try {
        val txId     = transactionId.ifEmpty { "unknown" }
        val endpoint = ApiConfig.TRANSACTIONS_SEND_EMAIL.replace("{transactionId}", txId)
        Log.d("ORION_EMAIL", "POST $endpoint  email=$email")

        val conn = (URL(endpoint).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            setRequestProperty("Content-Type",  "application/json; charset=utf-8")
            setRequestProperty("Accept",         "application/json")
            setRequestProperty("X-Merchant-Id",  ApiConfig.MERCHANT_ID)
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
