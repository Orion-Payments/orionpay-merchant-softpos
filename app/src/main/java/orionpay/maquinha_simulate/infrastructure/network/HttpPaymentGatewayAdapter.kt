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
                connectTimeout = ApiConfig.CONNECT_TIMEOUT
                readTimeout = ApiConfig.READ_TIMEOUT
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
                setRequestProperty("Accept", "application/json")
                setRequestProperty("Authorization", "Bearer $token")
                setRequestProperty("X-Merchant-Id", tx.merchantId)
                setRequestProperty("X-Idempotency-Key", idempotencyKey)
            }

            // Destaque para depuração de campos EMV
            Log.d("ORION_GATEWAY", "--------------------------------------------------")
            Log.d("ORION_GATEWAY", "VALIDANDO DADOS EMV PARA O BACKEND:")
            Log.d("ORION_GATEWAY", "ATC lido: ${tx.atc ?: "NULO (será enviado mock 0001)"}")
            Log.d("ORION_GATEWAY", "Cryptogram lido: ${tx.applicationCryptogram ?: "NULO (será enviado mock zeros)"}")
            Log.d("ORION_GATEWAY", "Expiração: ${tx.expirationDate}")
            Log.d("ORION_GATEWAY", "--------------------------------------------------")

            // Limpeza de dados para evitar rejeição por formato ou máscara
            val cleanPan    = tx.cardNumber.filter { it.isDigit() }
            val cleanExpiry = tx.expirationDate.replace("/", "").ifEmpty { "1229" }

            // Log de auditoria SEM MÁSCARA para validar o PAN real e evitar Erro 12 (Roteamento)
            Log.d("ORION_GATEWAY", "--------------------------------------------------")
            Log.d("ORION_GATEWAY", "PREPARANDO PAYLOAD ISO-COMPLIANT (DADOS REAIS):")
            Log.d("ORION_GATEWAY", "PAN ENVIADO (Campo 2): $cleanPan")
            Log.d("ORION_GATEWAY", "Expiração (Campo 14): $cleanExpiry")
            Log.d("ORION_GATEWAY", "--------------------------------------------------")

            val body = JSONObject().apply {
                put("merchantId", tx.merchantId)
                put("amount", tx.amount)
                put("productType", tx.productType.name)
                put("terminalSn", tx.terminalSn)
                put("externalReference", tx.externalReference)
                put("entryMode", tx.entryMode)
                put("transactionDate", tx.transactionDateIso)
                put("currencyCode", tx.currencyCode)
                put("countryCode", tx.countryCode)

                // Dados do Cartão (PAN e Validade sem máscaras/separadores)
                // O Gateway C++ exige o PAN real para extrair o BIN e rotear a transação
                put("cardNumber", cleanPan)
                put("expirationDate", cleanExpiry) 
                put("expiryDate", cleanExpiry)
                put("cvv", if (tx.cvv.isNullOrEmpty() || tx.cvv == "000") "" else tx.cvv)

                // Campos EMV puros para o Gateway C++
                put("applicationCryptogram", tx.applicationCryptogram ?: "")
                put("atc", tx.atc ?: "")
                put("cardBrand", tx.cardBrand)
                put("cardHolderName", tx.cardHolderName)
            }.toString()

            Log.d("ORION_GATEWAY", "JSON ENVIADO: $body")
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
                    authCode = json.optString("authorizationCode") ?: json.optString("authCode") ?: "",
                    nsu = json.optString("nsu") ?: json.optString("nsuHost") ?: "",
                    transactionId = json.optString("id") ?: json.optString("transactionId") ?: "",
                    rawJson = resp
                )
            } else {
                TransactionResultDomain(
                    state = TxState.ERROR,
                    message = json.optString("message") ?: json.optString("error") ?: "Erro $code",
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
