package orionpay.maquinha_simulate.utils

import android.nfc.tech.IsoDep
import orionpay.maquinha_simulate.data.model.Tlv

// EMV / APDU helper utilities used by ReadEmvReader.
// Implemented in a more robust way to handle multi-byte tags and PDOLs.
// ---- Generic byte helpers ----
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

// ---- APDU builders ----

fun apduSelect(aidAscii: String): ByteArray {
    val aidBytes = aidAscii.toByteArray(Charsets.US_ASCII)
    val header = byteArrayOf(0x00, 0xA4.toByte(), 0x04, 0x00, aidBytes.size.toByte())
    return header + aidBytes
}

fun apduSelectHex(aidHex: String): ByteArray {
    val aidBytes = aidHex.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
    val header = byteArrayOf(0x00, 0xA4.toByte(), 0x04, 0x00, aidBytes.size.toByte())
    return header + aidBytes
}

fun buildGpo(pdolTlv: Tlv?): ByteArray {
    val pdolValue = pdolTlv?.value ?: byteArrayOf()
    val constructedData = mutableListOf<Byte>()
    
    var i = 0
    while (i < pdolValue.size) {
        var tagSize = 1
        if ((pdolValue[i].toInt() and 0x1F) == 0x1F) {
            tagSize = 2
            if (i + 1 < pdolValue.size && (pdolValue[i+1].toInt() and 0x80) != 0) tagSize = 3
        }
        val tagBytes = pdolValue.copyOfRange(i, minOf(i + tagSize, pdolValue.size))
        val tagHex = tagBytes.toHex()
        i += tagSize
        
        if (i >= pdolValue.size) break
        val len = pdolValue[i].toInt() and 0xFF
        i++
        
        val mockData = when (tagHex) {
            "9F66" -> byteArrayOf(0x36, 0x00, 0x00, 0x00) // TTQ
            "9F02" -> ByteArray(6) { 0x00 } // Amount
            "9F03" -> ByteArray(6) { 0x00 } // Amount Other
            "9F1A" -> byteArrayOf(0x00, 0x76) // Country Code (Brazil)
            "5F2A" -> byteArrayOf(0x09, 0x86.toByte()) // Currency Code (BRL)
            "9A"   -> byteArrayOf(0x24, 0x01, 0x01) // Date
            "9C"   -> byteArrayOf(0x00) // Transaction Type
            "9F37" -> byteArrayOf(0x12, 0x34, 0x56, 0x78) // Unpredictable Number
            else   -> ByteArray(len) { 0x00 }
        }
        
        val actualData = if (mockData.size > len) mockData.copyOf(len) 
                        else if (mockData.size < len) mockData + ByteArray(len - mockData.size)
                        else mockData
        
        constructedData.addAll(actualData.toList())
    }
    
    val dataBytes = constructedData.toByteArray()
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

// ---- Transceive helpers ----

fun transceive(isoDep: IsoDep, apdu: ByteArray): ByteArray = isoDep.transceive(apdu)

fun transceiveSafe(isoDep: IsoDep, apdu: ByteArray): ByteArray? = try {
    isoDep.transceive(apdu)
} catch (e: Exception) {
    null
}

// ---- TLV parsing helpers ----

fun parseTlv(data: ByteArray): List<Tlv> {
    val result = mutableListOf<Tlv>()
    var i = 0
    while (i < data.size) {
        val tagStart = i
        var b = data[i].toInt() and 0xFF
        i++
        if (b == 0x00 || b == 0xFF) continue
        
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
            if (remaining > 0) {
                result.add(Tlv(tag, remaining, data.copyOfRange(i, data.size)))
            }
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
            if ((firstByte and 0x20) != 0) { // Constructed tag
                result.addAll(flattenTlv(parseTlv(tlv.value)))
            }
        }
    }
    return result
}

fun findTag(tlvs: List<Tlv>, tag: String): Tlv? = tlvs.firstOrNull { it.tag.equals(tag, ignoreCase = true) }

fun findPan(tlvs: List<Tlv>): Tlv? =
    findTag(tlvs, "5A") ?: findTag(tlvs, "4F")

// ---- AFL helpers ----

data class AflEntry(val sfi: Int, val start: Int, val end: Int)

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

fun extractAflFromGpo(gpoData: ByteArray): ByteArray? {
    val tlvs = flattenTlv(parseTlv(gpoData))
    return findTag(tlvs, "94")?.value
}

// ---- Track2 / PAN / CVV / expiry helpers ----

fun extractPanFromTrack2(track2Hex: String): String {
    val idx = track2Hex.uppercase().indexOf('D')
    val panHex = if (idx > 0) track2Hex.substring(0, idx) else track2Hex
    return panHex.filter { it.isDigit() }
}

fun extractCvv2FromTrack2(track2Hex: String): String = ""

fun formatExpiry(yyMmHex: String): String {
    return if (yyMmHex.length >= 4) {
        val yy = yyMmHex.substring(0, 2)
        val mm = yyMmHex.substring(2, 4)
        "$mm/$yy"
    } else ""
}

// ---- AID / brand helpers ----
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

fun extractAid(tlvs: List<Tlv>): String? = findTag(tlvs, "4F")?.value?.toHex()

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

fun interpretSw(resp: ByteArray): String {
    if (resp.size < 2) return "Resposta vazia"
    val sw1 = resp[resp.size-2].toInt() and 0xFF
    val sw2 = resp[resp.size-1].toInt() and 0xFF
    return when {
        sw1 == 0x90 && sw2 == 0x00 -> "9000: Sucesso"
        sw1 == 0x69 && sw2 == 0x85 -> "6985: Condições não satisfeitas (PDOL/Sequência)"
        sw1 == 0x67 && sw2 == 0x00 -> "6700: Tamanho (Lc) incorreto"
        sw1 == 0x6A && sw2 == 0x82 -> "6A82: Aplicação/Arquivo não encontrado"
        sw1 == 0x6A && sw2 == 0x81 -> "6A81: Função não suportada"
        sw1 == 0x6C -> "6C${"%02X".format(sw2)}: Tamanho (Le) incorreto"
        sw1 == 0x61 -> "61${"%02X".format(sw2)}: Dados pendentes"
        else -> "Status: ${"%02X%02X".format(sw1, sw2)}"
    }
}

fun maskPan(pan: String): String {
    val c = pan.filter { it.isDigit() }
    return if (c.length >= 8) c.take(6) + "*".repeat(c.length - 10) + c.takeLast(4) else c
}
