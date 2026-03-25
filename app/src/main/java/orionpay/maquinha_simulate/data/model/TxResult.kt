package orionpay.maquinha_simulate.data.model

import orionpay.maquinha_simulate.domain.enums.TxState

data class TxResult(
    val state: orionpay.maquinha_simulate.domain.enums.TxState = TxState.IDLE,
    val message: String = "",
    val authCode: String = "",
    val nsu: String = "",
    val transactionId: String = "",   // ID retornado pela API para usar no endpoint de e-mail
    val rawJson: String = ""
)

fun TxResult.isRetryable(): Boolean =
    state == TxState.ERROR && (
            message.contains("Timeout",       ignoreCase = true) ||
                    message.contains("timeout",       ignoreCase = true) ||
                    message.contains("indisponível",  ignoreCase = true) ||
                    message.contains("conectar",      ignoreCase = true) ||
                    message.contains("conexão",       ignoreCase = true) ||
                    message.contains("ConnectException", ignoreCase = true) ||
                    message.contains("SocketTimeout", ignoreCase = true)
            )