package orionpay.maquinha_simulate.data.model

import org.json.JSONObject

enum class ProductType(val label: String, val apiKey: String) {
    CREDIT_AVISTA("Crédito à vista", "CREDIT_A_VISTA"),
    CREDIT_2X("Crédito 2x",          "CREDIT_PARCELADO_2"),
    CREDIT_3X("Crédito 3x",          "CREDIT_PARCELADO_3"),
    CREDIT_6X("Crédito 6x",          "CREDIT_PARCELADO_6"),
    CREDIT_12X("Crédito 12x",        "CREDIT_PARCELADO_12"),
    DEBIT("Débito",                   "DEBIT"),
    PIX("Pix",                        "PIX"),
}

enum class TxState { IDLE, LOADING, SUCCESS, ERROR }

data class TxResult(
    val state: TxState = TxState.IDLE,
    val message: String = "",
    val authCode: String = "",
    val nsu: String = "",
    val rawJson: String = ""
)

data class CardData(
    val brand: String = "",
    val holder: String = "",
    val number: String = "",
    val expiry: String = "",
    val entryMode: String = "CHIP"
)

data class Tlv(val tag: String, val length: Int, val value: ByteArray)
data class AflEntry(val sfi: Int, val start: Int, val end: Int)
