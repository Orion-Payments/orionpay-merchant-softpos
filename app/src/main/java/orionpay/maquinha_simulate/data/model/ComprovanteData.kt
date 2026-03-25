package orionpay.maquinha_simulate.data.model

data class ComprovanteData(
    val amount: String,
    val product: String,
    val brand: String,
    val maskedPan: String,
    val holder: String,
    val authCode: String,
    val nsu: String,
    val transactionId: String,        // usado no endpoint de envio de e-mail
    val dateTime: String,
    val terminalSn: String,
    val merchantId: String,
    val entryMode: String,
    val message: String
)