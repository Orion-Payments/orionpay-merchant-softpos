package orionpay.maquinha_simulate.domain.model

data class MerchantSummaryDomain(
    val availableBalance: Double,
    val toReceive: Double,
    val totalVolume: Double,
    val netRevenue: Double,
    val averageTicket: Double,
    val approvalRate: Double,
    val activeTerminals: Int,
    val inactiveTerminals: Int,
    val chartData: List<Double> = emptyList()
)

data class MerchantTransactionDomain(
    val id: String,
    val nsu: String = "",
    val brand: String,
    val amount: Double,
    val netAmount: Double = 0.0,
    val date: String,
    val status: String,
    val isSuccess: Boolean,
    val lastFour: String = "",
    val authCode: String = "",
    val productType: String = "",
    val externalId: String = ""
)

data class MerchantTerminalDomain(
    val model: String,
    val serialNumber: String,
    val isActive: Boolean
)

data class SupportTicketDomain(
    val id: String,
    val title: String,
    val status: String,
    val lastUpdate: String
)

data class MerchantDomain(
    val id: String,
    val name: String,
    val document: String,
    val email: String,
    val status: String
)

data class AnticipationDetailsDomain(
    val totalGross: Double,
    val totalCost: Double,
    val totalNet: Double,
    val items: List<AnticipationItemDomain>
)

data class AnticipationItemDomain(
    val settlementId: String,
    val date: String,
    val grossAmount: Double,
    val netAmount: Double,
    val cost: Double,
    val days: Int,
    val isBlocked: Boolean,
    val status: String,
    val reason: String? = null
)
