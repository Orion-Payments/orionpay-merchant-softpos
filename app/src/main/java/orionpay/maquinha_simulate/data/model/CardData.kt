package orionpay.maquinha_simulate.data.model

data class CardData(
    val brand: String,
    val holder: String,
    val panRaw: String,
    val expiry: String,
    val cvv2: String,
    val entryMode: String,
    val cryptogram: String,
    val atc: String,
    val iad: String,
    val aip: String,
    val tvr: String
)
