package orionpay.maquinha_simulate.domain.model

import orionpay.maquinha_simulate.domain.enums.ProductType

/**
 * Modelo de domínio mínimo para representar uma transação.
 * Nesta etapa espelha os campos já usados no payload JSON, sem alterar regras.
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
    val stan: String? = null
)

