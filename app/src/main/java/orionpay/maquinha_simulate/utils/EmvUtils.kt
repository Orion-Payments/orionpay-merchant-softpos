package orionpay.maquinha_simulate.utils

import android.nfc.tech.IsoDep
import orionpay.maquinha_simulate.data.model.Tlv

// EMV / APDU helper utilities.
fun ByteArray.toHex(): String = joinToString("") { "%02X".format(it) }

fun ByteArray.toAscii(): String = map { b ->
    val c = (b.toInt() and 0xFF).toChar()
    if (c.code in 32..126) c else '\u0000'
}.joinToString("").trim('\u0000')

fun isSw9000(resp: ByteArray): Boolean =
    resp.size >= 2 && resp[resp.size - 2] == 0x90.toByte() && resp[resp.size - 1] == 0x00.toByte()

fun dropSw(resp: ByteArray): ByteArray =
    if (resp.size <= 2) ByteArray(0) else resp.copyOf(resp.size - 2)

fun getResponse(isoDep: IsoDep, resp: ByteArray): ByteArray {
    var currentResp = resp
    while (currentResp.size >= 2 && currentResp[currentResp.size - 2] == 0x61.toByte()) {
        val le = currentResp[currentResp.size - 1]
        val getRespApdu = byteArrayOf(0x00, 0xC0.toByte(), 0x00, 0x00, le)
        currentResp = isoDep.transceive(getRespApdu)
    }
    return currentResp
}

fun apduSelectHex(aidHex: String): ByteArray {
    val aidBytes = aidHex.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
    val header = byteArrayOf(0x00, 0xA4.toByte(), 0x04, 0x00, aidBytes.size.toByte())
    return header + aidBytes
}

fun apduSelect(aidAscii: String): ByteArray {
    val aidBytes = aidAscii.toByteArray(Charsets.US_ASCII)
    val header = byteArrayOf(0x00, 0xA4.toByte(), 0x04, 0x00, aidBytes.size.toByte())
    return header + aidBytes
}

// Persistir o Número Imprevisível para a sessão de leitura
private var sessionUnpredictableNumber: ByteArray? = null

fun getUnpredictableNumber(): ByteArray {
    if (sessionUnpredictableNumber == null) {
        sessionUnpredictableNumber = (1..4).map { (0..255).random().toByte() }.toByteArray()
    }
    return sessionUnpredictableNumber!!
}

fun clearEmvSession() {
    sessionUnpredictableNumber = null
}

/**
 * Constrói os dados para um DOL (PDOL ou CDOL).
 */
fun fillDol(dolValue: ByteArray, amount: Double): ByteArray {
    val constructedData = mutableListOf<Byte>()
    
    val amountCents = (amount * 100).toLong()
    val amountHex = amountCents.toString().padStart(12, '0')
    val amountBytes = amountHex.chunked(2).map { it.toInt(16).toByte() }.toByteArray()

    val now = java.time.LocalDate.now()
    val dateBytes = "%02X%02X%02X".format(now.year % 100, now.monthValue, now.dayOfMonth)
        .chunked(2).map { it.toInt(16).toByte() }.toByteArray()

    var i = 0
    while (i < dolValue.size) {
        var tagSize = 1
        if ((dolValue[i].toInt() and 0x1F) == 0x1F) {
            tagSize = 2
            if (i + 1 < dolValue.size && (dolValue[i+1].toInt() and 0x80) != 0) tagSize = 3
        }
        val tagHex = dolValue.copyOfRange(i, minOf(i + tagSize, dolValue.size)).toHex()
        i += tagSize
        
        if (i >= dolValue.size) break
        val len = dolValue[i].toInt() and 0xFF
        i++
        
        val data = when (tagHex) {
            "9F66" -> byteArrayOf(0x36, 0x00, 0x00, 0x00) // TTQ
            "9F02" -> amountBytes
            "9F03" -> ByteArray(6) { 0x00 }
            "9F1A" -> byteArrayOf(0x00, 0x76) // Brazil
            "5F2A" -> byteArrayOf(0x09, 0x86.toByte()) // BRL
            "9A"   -> dateBytes
            "9C"   -> byteArrayOf(0x00) // Goods and Services
            "9F37" -> getUnpredictableNumber()
            "9F35" -> byteArrayOf(0x22) // Terminal Type (mPOS)
            "9F1E" -> "ORIONPOS".toByteArray().copyOf(len)
            "9F33" -> byteArrayOf(0xE0.toByte(), 0xB8.toByte(), 0xC8.toByte()) // Terminal Capabilities
            "9F40" -> byteArrayOf(0x00, 0x00, 0x00, 0x00, 0x00) // Additional Terminal Capabilities
            else   -> ByteArray(len) { 0x00 }
        }
        
        val actualData = if (data.size > len) data.copyOf(len) 
                        else if (data.size < len) data + ByteArray(len - data.size)
                        else data
        constructedData.addAll(actualData.toList())
    }
    return constructedData.toByteArray()
}

fun buildGpo(pdolTlv: Tlv?, amount: Double): ByteArray {
    val dataBytes = if (pdolTlv != null) fillDol(pdolTlv.value, amount) else byteArrayOf()
    val body = if (dataBytes.isEmpty()) {
        byteArrayOf(0x83.toByte(), 0x00)
    } else {
        byteArrayOf(0x83.toByte(), dataBytes.size.toByte()) + dataBytes
    }
    return byteArrayOf(0x80.toByte(), 0xA8.toByte(), 0x00, 0x00, body.size.toByte()) + body
}

fun buildReadRecord(record: Int, sfi: Int): ByteArray {
    val p2 = ((sfi shl 3) or 4).toByte()
    return byteArrayOf(0x00, 0xB2.toByte(), record.toByte(), p2, 0x00)
}

fun transceive(isoDep: IsoDep, apdu: ByteArray): ByteArray = isoDep.transceive(apdu)

fun transceiveSafe(isoDep: IsoDep, apdu: ByteArray): ByteArray? = try {
    isoDep.transceive(apdu)
} catch (e: Exception) {
    null
}

fun parseTlv(data: ByteArray): List<Tlv> {
    val result = mutableListOf<Tlv>()
    var i = 0
    while (i < data.size) {
        val tagStart = i
        var b = data[i].toInt() and 0xFF
        if (b == 0x00 || b == 0xFF) { i++; continue }
        
        i++
        if ((b and 0x1F) == 0x1F) {
            while (i < data.size && (data[i].toInt() and 0x80) != 0) { i++ }
            i++
        }
        val tag = data.copyOfRange(tagStart, minOf(i, data.size)).toHex()
        
        if (i >= data.size) break
        b = data[i].toInt() and 0xFF
        i++
        var len = 0
        if ((b and 0x80) == 0) {
            len = b
        } else {
            val count = b and 0x7F
            for (j in 0 until count) {
                if (i >= data.size) break
                len = (len shl 8) or (data[i].toInt() and 0xFF)
                i++
            }
        }
        
        if (i + len > data.size) {
            val remaining = data.size - i
            if (remaining > 0) result.add(Tlv(tag, remaining, data.copyOfRange(i, data.size)))
            break
        }
        
        val value = data.copyOfRange(i, i + len)
        result.add(Tlv(tag, len, value))
        i += len
    }
    return result
}

fun flattenTlv(list: List<Tlv>): List<Tlv> {
    val result = mutableListOf<Tlv>()
    for (tlv in list) {
        result.add(tlv)
        if (tlv.tag.isNotEmpty()) {
            val firstByte = tlv.tag.substring(0, 2).toInt(16)
            if ((firstByte and 0x20) != 0) {
                result.addAll(flattenTlv(parseTlv(tlv.value)))
            }
        }
    }
    return result
}

fun findTag(tlvs: List<Tlv>, tag: String): Tlv? = tlvs.firstOrNull { it.tag.equals(tag, ignoreCase = true) }

fun parseAfl(afl: ByteArray): List<AflEntry> {
    val res = mutableListOf<AflEntry>()
    var i = 0
    while (i + 3 < afl.size) {
        val sfi = (afl[i].toInt() and 0xFF) shr 3
        val start = afl[i + 1].toInt() and 0xFF
        val end = afl[i + 2].toInt() and 0xFF
        res.add(AflEntry(sfi, start, end))
        i += 4
    }
    return res
}

data class AflEntry(val sfi: Int, val start: Int, val end: Int)

fun extractAflFromGpo(gpoData: ByteArray): ByteArray? {
    val tlvs = flattenTlv(parseTlv(gpoData))
    return findTag(tlvs, "94")?.value
}

fun extractPanFromTrack2(track2Hex: String): String {
    val idx = track2Hex.uppercase().indexOf('D')
    val panHex = if (idx > 0) track2Hex.substring(0, idx) else track2Hex
    return panHex.filter { it.isDigit() }
}

fun formatExpiry(yyMmHex: String): String {
    return if (yyMmHex.length >= 4) {
        val yy = yyMmHex.substring(0, 2)
        val mm = yyMmHex.substring(2, 4)
        "$mm/$yy"
    } else ""
}

val knownAids: List<String> = listOf(
    "A0000000031010", "A0000000032010", "A0000000033010",
    "A0000000041010", "A0000000043060", "A0000000042203",
    "A0000000181010", "A0000000182010",
    "A00000002501",
    "A0000001523010", "A0000001523012"
)

fun maskDisplay(number: String): String {
    val c = number.filter { it.isDigit() }
    return if (c.length >= 8) c.take(4) + " •••• •••• " + c.takeLast(4) else number
}

fun identifyBrand(aid: String?): String {
    if (aid == null) return "UNKNOWN"
    val upper = aid.uppercase()
    return when {
        upper.startsWith("A000000003") -> "VISA"
        upper.startsWith("A000000004") -> "MASTERCARD"
        upper.startsWith("A000000018") -> "ELO"
        upper.startsWith("A000000025") -> "AMEX"
        upper.startsWith("A000000152") -> "HIPERCARD"
        else -> "UNKNOWN"
    }
}

fun maskPan(pan: String): String {
    val c = pan.filter { it.isDigit() }
    return if (c.length >= 8) c.take(6) + "*".repeat(c.length - 10) + c.takeLast(4) else c
}
