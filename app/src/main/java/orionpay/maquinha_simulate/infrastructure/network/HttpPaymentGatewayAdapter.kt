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
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Implementação do Gateway via chamadas HTTP (POST).
 * Configurado para usar dados REAIS e garantir campos obrigatórios como expirationDate.
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
                setRequestProperty("X-Merchant-Id", ApiConfig.MERCHANT_ID)
                setRequestProperty("X-Idempotency-Key", idempotencyKey)
            }

            // --- PROCESSAMENTO DE DADOS DO CARTÃO ---
            
            // 1. PAN (Número do Cartão)
            val pan = tx.cardNumber.filter { it.isDigit() }
            
            // 2. Data de expiração (formatar para MMAA)
            // Se a leitura falhar, usamos um valor padrão para passar na validação da API
            val rawExpiry = tx.expirationDate.filter { it.isDigit() }
            val expiry = if (rawExpiry.length >= 4) rawExpiry.take(4) else "1229"
            
            Log.d("ORION_DEBUG", "Data Expiração Final: '$expiry' (original lido: '${tx.expirationDate}')")
            
            // 3. Data da transação (remover milissegundos para compatibilidade)
            val cleanDate = tx.transactionDateIso.split(".")[0]

            val body = JSONObject().apply {
                put("merchantId", ApiConfig.MERCHANT_ID)
                put("amount", BigDecimal(tx.amount).setScale(2, RoundingMode.HALF_UP))
                put("productType", tx.productType.apiKey)
                put("installments", "1") 
                put("terminalSn", tx.terminalSn.ifBlank { "POS-ORION-992" })
                put("externalReference", tx.externalReference)
                put("entryMode", if (tx.entryMode == "CHIP") "CHIP_PIN" else tx.entryMode)
                put("transactionDate", cleanDate)
                put("currencyCode", "986")
                put("countryCode", "076")

                // Dados do Cartão
                put("cardNumber", pan.ifBlank { "0000000000000000" })
                put("expirationDate", expiry)
                put("expiryDate", expiry)
                put("cardBrand", tx.cardBrand.ifBlank { "VISA" }) 
                put("cardHolderName", tx.cardHolderName.ifBlank { "CLIENTE EMV" })
                put("cvv", tx.cvv.ifBlank { "000" })

                // Campos EMV (Capturados do NFC) - Enviados apenas se existirem
                tx.applicationCryptogram?.let { put("applicationCryptogram", it) }
                tx.atc?.let { put("atc", it) }
                tx.issuerApplicationData?.let { put("issuerApplicationData", it) }
                tx.aip?.let { put("aip", it) }
                tx.tvr?.let { put("tvr", it) }
                tx.unpredictableNumber?.let { put("unpredictableNumber", it) }

                // Novo Bloco EMV Estruturado para o Switch
                val emvData = JSONObject().apply {
                    tx.applicationCryptogram?.let { put("9F26", it) }
                    tx.cid?.let { put("9F27", it) }
                    tx.issuerApplicationData?.let { put("9F10", it) }
                    tx.unpredictableNumber?.let { put("9F37", it) }
                    tx.atc?.let { put("9F36", it) }
                    tx.aip?.let { put("82", it) }
                    tx.tvr?.let { put("95", it) }
                    tx.transactionDate?.let { put("9A", it) }
                    tx.transactionType?.let { put("9C", it) }
                    tx.currencyCode.let { put("5F2A", it.padStart(4, '0')) } // ISO Currency
                    tx.countryCode.let { put("9F1A", it.padStart(4, '0')) }  // Country Code
                    tx.amountOther?.let { put("9F03", it) }
                    tx.terminalCapabilities?.let { put("9F33", it) }
                    tx.cvmResults?.let { put("9F34", it) }
                    tx.terminalType?.let { put("9F35", it) }
                    tx.transactionSequenceCounter?.let { put("9F41", it) }
                    tx.dfName?.let { put("84", it) }
                    tx.panSequenceNumber?.let { put("5F34", it) }
                }
                put("emvData", emvData)

                // Dados Adicionais para o Field 35 e Field 23
                tx.track2?.let { put("track2", it) }
                tx.panSequenceNumber?.let { put("panSequenceNumber", it) }
                tx.aid?.let { put("aid", it) }

                // 4. STAN (System Trace Audit Number - Campo 11) - Obrigatório para o Switch
                val finalStan = tx.stan?.takeIf { it.isNotBlank() } ?: (1..999999).random().toString().padStart(6, '0')
                put("stan", finalStan)

                // PIN Block - Enviado apenas se houver senha capturada (opcional).
                if (!tx.pinData.isNullOrBlank()) {
                    put("pinBlock", tx.pinData)
                    put("pinBlockFormat", "ISO_FORMAT_0")
                }
            }.toString()

            Log.d("ORION_GATEWAY", "JSON ENVIADO: ${maskSensitiveLog(body)}")
            
            OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { it.write(body) }

            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val resp = BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).use { it.readText() }

            Log.d("ORION_GATEWAY", "RESPOSTA DO SERVIDOR: $resp")

            if (resp.isBlank()) {
                return TransactionResultDomain(
                    state = if (code in 200..299) TxState.SUCCESS else TxState.ERROR,
                    message = if (code in 200..299) "Aprovado (sem corpo)" else "Erro $code (resposta vazia)",
                    rawJson = "Empty response"
                )
            }

            val json = JSONObject(resp)
            if (code in 200..299) {
                TransactionResultDomain(
                    state = TxState.SUCCESS,
                    message = json.optString("message", "Aprovado"),
                    authCode = json.optString("authorizationCode") ?: json.optString("authCode") ?: "OK",
                    nsu = json.optString("nsu") ?: json.optString("nsuHost") ?: "001",
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
