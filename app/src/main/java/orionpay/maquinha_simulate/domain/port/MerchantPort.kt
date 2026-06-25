package orionpay.maquinha_simulate.domain.port

import orionpay.maquinha_simulate.domain.model.*

interface MerchantPort {
    suspend fun getSummary(token: String): MerchantSummaryDomain
    suspend fun getTransactions(token: String, page: Int = 0, size: Int = 20): List<MerchantTransactionDomain>
    suspend fun getTransactionDetail(token: String, transactionId: String): MerchantTransactionDomain?
    suspend fun getTerminals(token: String): List<MerchantTerminalDomain>
    suspend fun getTickets(token: String): List<SupportTicketDomain>
    suspend fun requestAnticipation(token: String, amount: Double): Boolean
    suspend fun getMerchantMe(token: String): MerchantDomain?
    suspend fun getAnticipationDetails(token: String, merchantId: String): AnticipationDetailsDomain?
}
