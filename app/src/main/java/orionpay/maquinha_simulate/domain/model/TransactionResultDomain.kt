package orionpay.maquinha_simulate.domain.model

import orionpay.maquinha_simulate.domain.enums.TxState

/** Versão de domínio de TxResult, sem detalhes de transporte. */
data class TransactionResultDomain(
    val state: TxState,
    val message: String,
    val authCode: String? = null,
    val nsu: String? = null,
    val transactionId: String? = null,
    val rawJson: String? = null
)

