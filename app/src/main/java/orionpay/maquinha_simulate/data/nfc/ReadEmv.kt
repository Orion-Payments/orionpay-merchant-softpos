package orionpay.maquinha_simulate.data.nfc

import android.nfc.tech.IsoDep
import android.util.Log
import orionpay.maquinha_simulate.data.model.CardData
import orionpay.maquinha_simulate.data.model.Tlv
import orionpay.maquinha_simulate.utils.*

class ReadEmvReader {

    private val TAG = "ReadEmvReader"

    fun readEmvCard(isoDep: IsoDep, amount: Double = 0.0): CardData? {
        try {
            var aid: String? = null
            
            // 1. SELECT PPSE
            val ppseResp = transceiveSafe(isoDep, apduSelect("2PAY.SYS.DDF01"))
            if (ppseResp != null && isSw9000(ppseResp)) {
                val tlvs = flattenTlv(parseTlv(dropSw(ppseResp)))
                aid = findTag(tlvs, "4F")?.value?.toHex()
            }

            // 2. Fallback AIDs
            if (aid == null) {
                for (candidate in knownAids) {
                    val resp = transceiveSafe(isoDep, apduSelectHex(candidate)) ?: continue
                    if (isSw9000(resp)) {
                        aid = candidate
                        break
                    }
                }
            }

            if (aid == null) return null

            // 3. SELECT AID
            var selectResp = transceive(isoDep, apduSelectHex(aid))
            selectResp = getResponse(isoDep, selectResp)
            if (!isSw9000(selectResp)) return null

            // 4. GPO
            val selectTlvs = flattenTlv(parseTlv(dropSw(selectResp)))
            val pdol = findTag(selectTlvs, "9F38")
            
            // Limpar sessão EMV para garantir novo UN se necessário, ou manter se for a mesma transação
            // No caso de uma nova leitura, chamamos clear para renovar o 9F37
            clearEmvSession()
            
            val gpoResp = getResponse(isoDep, transceive(isoDep, buildGpo(pdol, amount)))
            
            val allTlvs = mutableListOf<Tlv>()
            if (gpoResp != null && isSw9000(gpoResp)) {
                allTlvs.addAll(flattenTlv(parseTlv(dropSw(gpoResp))))
            }

            // 5. READ RECORDS
            val afl = extractAflFromGpo(dropSw(gpoResp ?: byteArrayOf()))
            if (afl != null) {
                for (entry in parseAfl(afl)) {
                    for (rec in entry.start..entry.end) {
                        var resp = transceiveSafe(isoDep, buildReadRecord(rec, entry.sfi)) ?: continue
                        resp = getResponse(isoDep, resp)
                        if (isSw9000(resp)) {
                            allTlvs.addAll(flattenTlv(parseTlv(dropSw(resp))))
                        }
                    }
                }
            }

            // 6. FORÇAR GERAÇÃO DE CRIPTOGRAMA (GENERATE AC)
            // Comando essencial para obter o 9F26 (Cryptogram) e atualizar o 9F36 (ATC)
            Log.d(TAG, "Solicitando Generate AC para obter Cryptogram...")
            
            // CDOL1 Parsing (Opcional, mas recomendado para robustez)
            // Se o cartão exigir dados específicos no Generate AC, eles estão no CDOL1 (Tag 8C)
            val cdol1 = findTag(allTlvs, "8C")
            val acData = fillDol(cdol1?.value ?: byteArrayOf(0x9F.toByte(), 0x02.toByte(), 0x06.toByte(), 0x9F.toByte(), 0x37.toByte(), 0x04.toByte()), amount)
            
            val generateAc = byteArrayOf(
                0x80.toByte(), 0xAE.toByte(), 0x80.toByte(), 0x00.toByte(), acData.size.toByte()
            ) + acData
            
            val acRespRaw = transceiveSafe(isoDep, generateAc)
            if (acRespRaw != null) {
                val acResp = getResponse(isoDep, acRespRaw)
                if (isSw9000(acResp)) {
                    allTlvs.addAll(flattenTlv(parseTlv(dropSw(acResp))))
                }
            }

            // 7. FORÇAR LEITURA DE ATC (GET DATA)
            var atc = findTag(allTlvs, "9F36")?.value?.toHex() ?: ""
            if (atc.isEmpty()) {
                val getDataAtc = byteArrayOf(0x80.toByte(), 0xCA.toByte(), 0x9F.toByte(), 0x36.toByte(), 0x00.toByte())
                val respAtc = transceiveSafe(isoDep, getDataAtc)
                if (respAtc != null && isSw9000(respAtc)) {
                    val atcTlvs = parseTlv(dropSw(respAtc))
                    atc = findTag(atcTlvs, "9F36")?.value?.toHex() ?: ""
                }
            }

            // 8. Extração do PAN
            val panTlv = findTag(allTlvs, "5A")
            val track2Tlv = findTag(allTlvs, "57")
            val panRaw = if (panTlv != null) {
                panTlv.value.toHex().trimEnd('F', 'f').filter { it.isDigit() }
            } else if (track2Tlv != null) {
                extractPanFromTrack2(track2Tlv.value.toHex())
            } else ""

            if (panRaw.isEmpty()) return null

            val cryptogram = findTag(allTlvs, "9F26")?.value?.toHex() ?: ""
            Log.d(TAG, "DADOS REAIS LIDOS -> ATC: $atc | ARQC: $cryptogram")

            return CardData(
                brand = identifyBrand(aid),
                holder = findTag(allTlvs, "5F20")?.value?.toAscii()?.trim() ?: "",
                panRaw = panRaw,
                expiry = findTag(allTlvs, "5F24")?.value?.toHex()?.let { formatExpiry(it) } ?: "",
                cvv2 = "",
                entryMode = "CHIP",
                cryptogram = cryptogram,
                atc = atc,
                iad = findTag(allTlvs, "9F10")?.value?.toHex() ?: "",
                aip = findTag(allTlvs, "82")?.value?.toHex() ?: "",
                tvr = findTag(allTlvs, "95")?.value?.toHex() ?: "",
                unpredictableNumber = getUnpredictableNumber().toHex(),
                cid = findTag(allTlvs, "9F27")?.value?.toHex() ?: "",
                transactionDate = findTag(allTlvs, "9A")?.value?.toHex() ?: "",
                transactionType = findTag(allTlvs, "9C")?.value?.toHex() ?: "",
                transactionCurrencyCode = findTag(allTlvs, "5F2A")?.value?.toHex() ?: "",
                terminalCountryCode = findTag(allTlvs, "9F1A")?.value?.toHex() ?: "",
                amountOther = findTag(allTlvs, "9F03")?.value?.toHex() ?: "",
                terminalCapabilities = findTag(allTlvs, "9F33")?.value?.toHex() ?: "",
                cvmResults = findTag(allTlvs, "9F34")?.value?.toHex() ?: "",
                terminalType = findTag(allTlvs, "9F35")?.value?.toHex() ?: "",
                transactionSequenceCounter = findTag(allTlvs, "9F41")?.value?.toHex() ?: "",
                dfName = findTag(allTlvs, "84")?.value?.toHex() ?: "",
                panSequenceNumber = findTag(allTlvs, "5F34")?.value?.toHex() ?: "",
                track2 = track2Tlv?.value?.toHex() ?: "",
                aid = aid ?: ""
            )
        } catch (e: Exception) {
            Log.e(TAG, "Erro na leitura EMV", e)
            return null
        }
    }
}
