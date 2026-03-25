package orionpay.maquinha_simulate.data.model

data class Tlv(val tag: String, val length: Int, val value: ByteArray)