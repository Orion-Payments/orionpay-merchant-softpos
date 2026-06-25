package orionpay.maquinha_simulate.infrastructure.network

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import orionpay.maquinha_simulate.config.ApiConfig
import orionpay.maquinha_simulate.domain.model.*
import orionpay.maquinha_simulate.domain.port.MerchantPort
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * Implementação real do MerchantPort conectando à API do OrionPay.
 */
class HttpMerchantAdapter(
    private val baseUrl: String = ApiConfig.BASE_URL
) : MerchantPort {

    private suspend fun makeRequest(
        path: String,
        token: String,
        method: String = "GET",
        body: String? = null
    ): String? = withContext(Dispatchers.IO) {
        val fullUrl = "${ApiConfig.BASE_URL}$path"
        try {
            Log.d("ORION_MERCHANT", "Request: $method $fullUrl")
            val url = URL(fullUrl)
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = method
                connectTimeout = ApiConfig.CONNECT_TIMEOUT
                readTimeout = ApiConfig.READ_TIMEOUT
                setRequestProperty("Authorization", "Bearer $token")
                setRequestProperty("X-Merchant-Id", ApiConfig.MERCHANT_ID)
                setRequestProperty("Accept", "application/json")
                
                if (body != null) {
                    setRequestProperty("Content-Type", "application/json")
                    doOutput = true
                    outputStream.writer().use { it.write(body) }
                }
            }

            val code = conn.responseCode
            val stream: InputStream? = if (code in 200..299) conn.inputStream else conn.errorStream
            val response = stream?.bufferedReader()?.use { it.readText() }

            if (code !in 200..299) {
                Log.e("ORION_MERCHANT", "Erro na API ($code) em $path: $response")
                return@withContext null
            }
            Log.d("ORION_MERCHANT", "Response ($code) from $path: $response")
            response
        } catch (e: Exception) {
            Log.e("ORION_MERCHANT", "Falha de conexão com a API em $path", e)
            null
        }
    }

    override suspend fun getSummary(token: String): MerchantSummaryDomain = withContext(Dispatchers.IO) {
        val response = makeRequest("/api/v1/dashboard/${ApiConfig.MERCHANT_ID}/summary", token)
        
        if (response != null) {
            try {
                val json = JSONObject(response)
                val chartArray = json.optJSONArray("chartData")
                val chartData = if (chartArray != null) {
                    List(chartArray.length()) { i -> chartArray.optDouble(i, 0.0) }
                } else {
                    // Mock data para o gráfico caso a API ainda não retorne, 
                    // para manter a paridade visual com o Portal Web
                    listOf(4.0, 7.0, 5.0, 9.0, 6.0, 8.0, 10.0, 7.0, 9.0, 12.0)
                }

                return@withContext MerchantSummaryDomain(
                    availableBalance = json.optDouble("availableBalance", 0.0),
                    toReceive = json.optDouble("futureReceivables", 0.0),
                    totalVolume = json.optDouble("totalTpv", 0.0),
                    netRevenue = json.optDouble("totalNetRevenue", 0.0),
                    averageTicket = json.optDouble("averageTicket", 0.0),
                    approvalRate = json.optDouble("approvalRate", 100.0),
                    activeTerminals = json.optInt("activeTerminals", 0),
                    inactiveTerminals = json.optInt("inactiveTerminals", 0),
                    chartData = chartData
                )
            } catch (e: Exception) {
                Log.e("ORION_MERCHANT", "Erro ao processar Summary JSON", e)
            }
        }
        MerchantSummaryDomain(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0, 0)
    }

    override suspend fun getTransactions(token: String, page: Int, size: Int): List<MerchantTransactionDomain> = withContext(Dispatchers.IO) {
        val response = makeRequest("/api/v1/transactions/extrato?page=$page&size=$size&sort=createdAt,desc", token)
        
        if (response != null) {
            try {
                val jsonRes = JSONObject(response)
                val array = jsonRes.optJSONArray("content") ?: JSONArray()

                return@withContext List(array.length()) { i ->
                    val obj = array.getJSONObject(i)
                    val status = obj.optString("status", "PENDENTE")
                    val cardBrand = if (obj.has("cardBrand") && !obj.isNull("cardBrand")) obj.getString("cardBrand") else obj.optString("brand", "N/A")
                    val lastFour = if (obj.has("cardLastFour") && !obj.isNull("cardLastFour")) obj.getString("cardLastFour") else obj.optString("lastFour", "****")
                    
                    MerchantTransactionDomain(
                        id = obj.optString("id"),
                        nsu = obj.optString("nsu", ""),
                        brand = cardBrand,
                        amount = obj.optDouble("amount", 0.0),
                        netAmount = obj.optDouble("netAmount", 0.0),
                        date = obj.optString("createdAt", "Data desconhecida"),
                        status = status,
                        isSuccess = listOf("PAID", "AUTHORIZED", "CONFIRMED", "CAPTURED").contains(status.uppercase()),
                        lastFour = lastFour,
                        authCode = obj.optString("authCode", ""),
                        productType = obj.optString("productType", ""),
                        externalId = obj.optString("externalId", "")
                    )
                }
            } catch (e: Exception) {
                Log.e("ORION_MERCHANT", "Erro ao processar Extrato JSON", e)
            }
        }
        emptyList()
    }

    override suspend fun getTransactionDetail(token: String, transactionId: String): MerchantTransactionDomain? = withContext(Dispatchers.IO) {
        val response = makeRequest("/api/v1/transactions/$transactionId/detail", token)
        
        if (response != null) {
            try {
                val obj = JSONObject(response)
                val status = obj.optString("status", "PENDENTE")
                val cardBrand = if (obj.has("cardBrand") && !obj.isNull("cardBrand")) obj.getString("cardBrand") else obj.optString("brand", "N/A")
                val lastFour = if (obj.has("cardLastFour") && !obj.isNull("cardLastFour")) obj.getString("cardLastFour") else obj.optString("lastFour", "****")

                return@withContext MerchantTransactionDomain(
                    id = obj.optString("id"),
                    nsu = obj.optString("nsu", ""),
                    brand = cardBrand,
                    amount = obj.optDouble("amount", 0.0),
                    netAmount = obj.optDouble("netAmount", 0.0),
                    date = obj.optString("createdAt", "Data desconhecida"),
                    status = status,
                    isSuccess = listOf("PAID", "AUTHORIZED", "CONFIRMED", "CAPTURED").contains(status.uppercase()),
                    lastFour = lastFour,
                    authCode = obj.optString("authCode", ""),
                    productType = obj.optString("productType", ""),
                    externalId = obj.optString("externalId", "")
                )
            } catch (e: Exception) {
                Log.e("ORION_MERCHANT", "Erro ao processar Detalhe Transação JSON", e)
            }
        }
        null
    }

    override suspend fun getTerminals(token: String): List<MerchantTerminalDomain> = withContext(Dispatchers.IO) {
        val response = makeRequest("/api/v1/terminals", token)
        
        if (response != null) {
            try {
                val array = JSONArray(response)
                return@withContext List(array.length()) { i ->
                    val obj = array.getJSONObject(i)
                    MerchantTerminalDomain(
                        model = obj.optString("model", "Smart POS"),
                        serialNumber = obj.optString("serialNumber", "N/A"),
                        isActive = obj.optBoolean("active", true)
                    )
                }
            } catch (e: Exception) {
                Log.e("ORION_MERCHANT", "Erro ao processar Terminais JSON", e)
            }
        }
        emptyList()
    }

    override suspend fun getTickets(token: String): List<SupportTicketDomain> = withContext(Dispatchers.IO) {
        val response = makeRequest("/api/v1/support/tickets", token)
        
        if (response != null) {
            try {
                val array = JSONArray(response)
                return@withContext List(array.length()) { i ->
                    val obj = array.getJSONObject(i)
                    SupportTicketDomain(
                        id = obj.optString("id", "N/A"),
                        title = obj.optString("subject") ?: obj.optString("title", "Chamado"),
                        status = obj.optString("status", "ABERTO"),
                        lastUpdate = obj.optString("updatedAt", "")
                    )
                }
            } catch (e: Exception) {
                Log.e("ORION_MERCHANT", "Erro ao processar Tickets JSON", e)
            }
        }
        emptyList()
    }

    override suspend fun requestAnticipation(token: String, amount: Double): Boolean = withContext(Dispatchers.IO) {
        val body = JSONObject().apply { put("amount", amount) }.toString()
        val response = makeRequest("/api/v1/merchants/${ApiConfig.MERCHANT_ID}/anticipation/execute", token, "POST", body)
        response != null
    }

    override suspend fun getMerchantMe(token: String): MerchantDomain? = withContext(Dispatchers.IO) {
        val response = makeRequest("/api/auth/me", token)
        
        if (response != null) {
            try {
                val json = JSONObject(response)
                val data = if (json.has("merchant")) json.getJSONObject("merchant") else json
                
                val id = if (data.has("id")) data.getString("id") else data.optString("merchantId", "")
                
                return@withContext MerchantDomain(
                    id = id,
                    name = data.optString("name") ?: data.optString("companyName", "Lojista"),
                    document = data.optString("document") ?: data.optString("cnpj", ""),
                    email = data.optString("email", ""),
                    status = data.optString("status", "ACTIVE")
                )
            } catch (e: Exception) {
                Log.e("ORION_MERCHANT", "Erro ao processar Me JSON", e)
            }
        }
        null
    }

    override suspend fun getAnticipationDetails(token: String, merchantId: String): AnticipationDetailsDomain? = withContext(Dispatchers.IO) {
        val response = makeRequest("/api/v1/merchants/$merchantId/anticipation/available", token)
        
        if (response != null) {
            try {
                val json = JSONObject(response)
                val itemsArray = json.optJSONArray("items") ?: JSONArray()
                val items = List(itemsArray.length()) { i ->
                    val obj = itemsArray.getJSONObject(i)
                    AnticipationItemDomain(
                        settlementId = obj.optString("settlementId", ""),
                        date = obj.optString("originalSettlementDate", ""),
                        grossAmount = obj.optDouble("grossAmount", 0.0),
                        netAmount = obj.optDouble("netAmount", 0.0),
                        cost = obj.optDouble("anticipationCost", 0.0),
                        days = obj.optInt("daysToAnticipate", 0),
                        isBlocked = obj.optBoolean("isBlocked", false),
                        status = obj.optString("status", ""),
                        reason = if (obj.isNull("reason")) null else obj.optString("reason")
                    )
                }
                
                return@withContext AnticipationDetailsDomain(
                    totalGross = json.optDouble("totalGrossToAnticipate", 0.0),
                    totalCost = json.optDouble("totalCost", 0.0),
                    totalNet = json.optDouble("totalNetToReceive", 0.0),
                    items = items
                )
            } catch (e: Exception) {
                Log.e("ORION_MERCHANT", "Erro ao processar Anticipation JSON", e)
            }
        }
        null
    }
}
