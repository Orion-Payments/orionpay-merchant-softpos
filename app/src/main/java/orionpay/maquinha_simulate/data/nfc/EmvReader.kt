package orionpay.maquinha_simulate.data.nfc

import android.nfc.tech.IsoDep
import orionpay.maquinha_simulate.data.model.AflEntry
import orionpay.maquinha_simulate.data.model.CardData
import orionpay.maquinha_simulate.data.model.Tlv
import java.nio.charset.Charset

class EmvReader(private val isoDep: IsoDep) {

    private val knownAids = listOf("A0000000031010", "A0000000041010", "A0000000651010")

    fun readCard(): CardData {
        isoDep.connect()
        isoDep.timeout = 5000

        val aid = selectPpse() ?: findAidByFallback() ?: throw Exception("AID não encontrado")
        
        val selectResp = transceive(apduSelectHex(aid))
        val selectTlvs = flattenTlv(parseTlv(selectResp.dropSw()))
        val pdol = findTag(selectTlvs, "9F38")

        val gpoResp = transceive(buildGpo(pdol))
        val afl = extractAflFromGpo(gpoResp.dropSw())
        val allTlvs = mutableListOf<Tlv>()

        if (afl != null) {
            parseAfl(afl).forEach { entry ->
                for (rec in entry.start..entry.end) {
                    val resp = transceiveSafe(buildReadRecord(rec, entry.sfi)) ?: continue
                    if (isSw9000(resp)) {
                        allTlvs.addAll(flattenTlv(parseTlv(resp.dropSw())))
                    }
                }
            }
        }

        val pan = findPan(allTlvs)
        return CardData(
            brand = identifyBrand(aid),
            holder = findTag(allTlvs, "5F20")?.toAscii()?.trim() ?: "",
            number = pan?.toHex()?.trimEnd('F','f')?.filter { it.isDigit() } ?: "",
            expiry = findTag(allTlvs, "5F24")?.toHex()?.let { formatExpiry(it) } ?: "",
            entryMode = "CHIP"
        )
    }

    // --- Métodos Privados de Suporte EMV ---
    private fun selectPpse(): String? {
        val resp = transceiveSafe(apduSelect("2PAY.SYS.DDF01"))
        return if (resp != null && isSw9000(resp)) {
            findTag(flattenTlv(parseTlv(resp.dropSw())), "4F")?.toHex()
        } else null
    }

    private fun findAidByFallback(): String? {
        knownAids.forEach { aid ->
            val resp = transceiveSafe(apduSelectHex(aid))
            if (resp != null && isSw9000(resp)) return aid
        }
        return null
    }

    private fun transceive(cmd: ByteArray) = isoDep.transceive(cmd)
    private fun transceiveSafe(cmd: ByteArray) = try { isoDep.transceive(cmd) } catch (_: Exception) { null }
    private fun isSw9000(r: ByteArray) = r.size >= 2 && r[r.size-2] == 0x90.toByte() && r[r.size-1] == 0x00.toByte()
    private fun ByteArray.dropSw() = if (size >= 2) copyOfRange(0, size - 2) else this
    private fun ByteArray.toHex() = joinToString("") { "%02X".format(it) }
    private fun ByteArray.toAscii() = String(this, Charset.forName("ISO-8859-1"))
    
    private fun identifyBrand(aid: String) = when {
        aid.startsWith("A000000003") -> "VISA"
        aid.startsWith("A000000004") -> "MASTERCARD"
        else -> "ELO"
    }

    private fun formatExpiry(hex: String) = if(hex.length >= 4) "${hex.substring(2,4)}/${hex.substring(0,2)}" else hex

    private fun parseTlv(data: ByteArray): List<Tlv> {
        val list = mutableListOf<Tlv>()
        var i = 0
        while (i < data.size) {
            val tag = "%02X".format(data[i++])
            if (i >= data.size) break
            val len = data[i++].toInt() and 0xFF
            if (i + len > data.size) break
            list.add(Tlv(tag, len, data.copyOfRange(i, i + len)))
            i += len
        }
        return list
    }

    private fun flattenTlv(tlvs: List<Tlv>) = tlvs
    private fun findTag(tlvs: List<Tlv>, tag: String) = tlvs.firstOrNull { it.tag == tag }?.value
    private fun findPan(tlvs: List<Tlv>) = findTag(tlvs, "5A")
    private fun apduSelect(aid: String) = byteArrayOf(0x00, 0xA4.toByte(), 0x04, 0x00, aid.length.toByte()) + aid.toByteArray()
    private fun apduSelectHex(hex: String) = byteArrayOf(0x00, 0xA4.toByte(), 0x04, 0x00, (hex.length/2).toByte()) + hex.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
    private fun buildGpo(pdol: ByteArray?) = byteArrayOf(0x80.toByte(), 0xA8.toByte(), 0x00, 0x00, 0x02, 0x83.toByte(), 0x00)
    private fun extractAflFromGpo(data: ByteArray) = if(data.size > 2) data.copyOfRange(2, data.size) else null
    private fun parseAfl(afl: ByteArray): List<AflEntry> {
        val list = mutableListOf<AflEntry>()
        for (i in afl.indices step 4) {
            if(i+3 < afl.size) list.add(AflEntry(afl[i].toInt() shr 3, afl[i+1].toInt() and 0xFF, afl[i+2].toInt() and 0xFF))
        }
        return list
    }
    private fun buildReadRecord(rec: Int, sfi: Int) = byteArrayOf(0x00, 0xB2.toByte(), rec.toByte(), ((sfi shl 3) or 4).toByte(), 0x00)
}
