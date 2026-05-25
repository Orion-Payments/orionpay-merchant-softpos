package orionpay.maquinha_simulate.infrastructure.auth

import android.util.Log
import org.json.JSONObject
import orionpay.maquinha_simulate.config.ApiConfig
import orionpay.maquinha_simulate.domain.port.TerminalAuthPort
import orionpay.maquinha_simulate.utils.maskSensitiveLog
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

/**
 * Implementa o login do terminal na API /api/auth/login,
 * retornando o token para ser usado nas transações.
 */
class HttpTerminalAuthAdapter(
    private val baseUrl: String = ApiConfig.BASE_URL
) : TerminalAuthPort {

    override suspend fun login(): String? {
        return try {
            val url = URL(ApiConfig.LOGIN_URL)
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = ApiConfig.CONNECT_TIMEOUT
                readTimeout = ApiConfig.READ_TIMEOUT
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
            }

            val body = JSONObject().apply {
                put("email", ApiConfig.AUTH_EMAIL)
                put("password", ApiConfig.AUTH_PASSWORD)
            }.toString()

            Log.d("ORION_AUTH", "Login POST ${ApiConfig.LOGIN_URL}")
            Log.d("ORION_AUTH", "Body: ${maskSensitiveLog(body)}")

            OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { it.write(body) }

            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val resp = BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).use { br ->
                buildString {
                    var line: String?
                    while (br.readLine().also { line = it } != null) {
                        append(line)
                    }
                }
            }

            if (code !in 200..299) {
                Log.e("ORION_AUTH", "Login falhou ($code): $resp")
                return null
            }

            val json = JSONObject(resp)
            val token = json.optString("token", null) ?: json.optString("accessToken", null)
            Log.d("ORION_AUTH", "Token obtido com sucesso")
            token
        } catch (e: java.net.SocketTimeoutException) {
            Log.e("ORION_AUTH", "Timeout ao conectar em ${ApiConfig.BASE_URL}. Verifique se o IP ${ApiConfig.API_HOST} está correto e se o servidor está rodando.")
            null
        } catch (e: Exception) {
            Log.e("ORION_AUTH", "Erro ao autenticar: ${e.message}", e)
            null
        }
    }
}
