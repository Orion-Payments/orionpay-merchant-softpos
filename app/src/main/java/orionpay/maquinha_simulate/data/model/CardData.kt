package orionpay.maquinha_simulate.data.model

data class CardData(
    val brand: String,
    val holder: String,
    val panRaw: String,
    val expiry: String,
    val cvv2: String,
    val entryMode: String,
    val cryptogram: String, // 9F26
    val atc: String,        // 9F36
    val iad: String,        // 9F10
    val aip: String,        // 82
    val tvr: String,        // 95
    val unpredictableNumber: String, // 9F37
    val cid: String = "",   // 9F27
    val transactionDate: String = "", // 9A
    val transactionType: String = "", // 9C
    val transactionCurrencyCode: String = "", // 5F2A
    val terminalCountryCode: String = "", // 9F1A
    val amountOther: String = "", // 9F03
    val terminalCapabilities: String = "", // 9F33
    val cvmResults: String = "", // 9F34
    val terminalType: String = "", // 9F35
    val transactionSequenceCounter: String = "", // 9F41
    val dfName: String = "", // 84
    val panSequenceNumber: String = "", // 5F34
    val track2: String = "", // 57
    val aid: String = ""
)
