package orionpay.maquinha_simulate.domain.model

import orionpay.maquinha_simulate.domain.enums.ProductType

/**
 * Modelo de domínio para representar uma transação.
 * Atualizado para conter dados EMV completos para integração com Switch ISO8583.
 */
data class TransactionDomain(
    val merchantId: String,
    val amount: Double,
    val productType: ProductType,
    val terminalSn: String,
    val externalReference: String,
    val entryMode: String,
    val cardBrand: String,
    val cardHolderName: String,
    val cardNumber: String,
    val expirationDate: String,
    val cvv: String,
    val currencyCode: String = "986",
    val countryCode: String = "076",
    val transactionDateIso: String,
    val applicationCryptogram: String? = null,
    val atc: String? = null,
    val issuerApplicationData: String? = null,
    val aip: String? = null,
    val tvr: String? = null,
    val pinData: String? = null,
    val stan: String? = null,
    val unpredictableNumber: String? = null,
    
    // Novos campos EMV
    val cid: String? = null,
    val transactionDate: String? = null,
    val transactionType: String? = null,
    val terminalCapabilities: String? = null,
    val cvmResults: String? = null,
    val terminalType: String? = null,
    val transactionSequenceCounter: String? = null,
    val dfName: String? = null,
    val panSequenceNumber: String? = null,
    val track2: String? = null,
    val aid: String? = null,
    val amountOther: String? = null
)
