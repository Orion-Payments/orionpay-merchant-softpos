package orionpay.maquinha_simulate.data.nfc

import android.nfc.tech.IsoDep
import android.util.Log
import orionpay.maquinha_simulate.data.model.CardData
import orionpay.maquinha_simulate.data.model.Tlv
import orionpay.maquinha_simulate.utils.*

class ReadEmvReader {

    private val TAG = "ReadEmvReader"

    fun readEmvCard(isoDep: IsoDep): CardData? {
        try {
            var aid: String? = null
            
            // 1. SELECT PPSE
            Log.d(TAG, "Tentando SELECT PPSE...")
            val ppseResp = transceiveSafe(isoDep, apduSelect("2PAY.SYS.DDF01"))
            if (ppseResp != null && isSw9000(ppseResp)) {
                val tlvs = flattenTlv(parseTlv(dropSw(ppseResp)))
                aid = findTag(tlvs, "4F")?.value?.toHex()
                Log.d(TAG, "AID encontrado via PPSE: $aid")
            }

            // 2. Fallback AIDs
            if (aid == null) {
                Log.d(TAG, "PPSE falhou ou sem AID, tentando AIDs conhecidos...")
                for (candidate in knownAids) {
                    val resp = transceiveSafe(isoDep, apduSelectHex(candidate)) ?: continue
                    if (isSw9000(resp)) {
                        aid = candidate
                        Log.d(TAG, "AID encontrado via Brute Force: $aid")
                        break
                    }
                }
            }

            if (aid == null) {
                Log.e(TAG, "Nenhum AID de pagamento encontrado.")
                return null
            }

            // 3. SELECT AID
            Log.d(TAG, "Selecionando AID: $aid")
            var selectResp = transceive(isoDep, apduSelectHex(aid))
            selectResp = getResponse(isoDep, selectResp)
            if (!isSw9000(selectResp)) {
                Log.e(TAG, "Falha ao selecionar AID: ${interpretSw(selectResp)}")
                return null
            }

            // 4. GPO
            Log.d(TAG, "Enviando GPO...")
            val selectTlvs = flattenTlv(parseTlv(dropSw(selectResp)))
            val pdol = findTag(selectTlvs, "9F38")
            val gpoResp = getResponse(isoDep, transceive(isoDep, buildGpo(pdol)))
            
            if (!isSw9000(gpoResp)) {
                Log.w(TAG, "GPO falhou: ${interpretSw(gpoResp)}")
            }

            val allTlvs = mutableListOf<Tlv>()
            
            // 5. READ RECORDS
            val afl = extractAflFromGpo(dropSw(gpoResp))
            if (afl != null) {
                Log.d(TAG, "Lendo records via AFL: ${afl.toHex()}")
                for (entry in parseAfl(afl)) {
                    for (rec in entry.start..entry.end) {
                        var resp = transceiveSafe(isoDep, buildReadRecord(rec, entry.sfi)) ?: continue
                        resp = getResponse(isoDep, resp)
                        if (isSw9000(resp)) {
                            val recordTlvs = flattenTlv(parseTlv(dropSw(resp)))
                            allTlvs.addAll(recordTlvs)
                            Log.d(TAG, "Record lido (SFI ${entry.sfi}, Rec $rec): ${recordTlvs.map { it.tag }.joinToString()}")
                        }
                    }
                }
            } else {
                Log.d(TAG, "AFL não encontrado, tentando Brute Force de records...")
                for (sfi in 1..5) {
                    for (rec in 1..10) {
                        var resp = transceiveSafe(isoDep, buildReadRecord(rec, sfi)) ?: continue
                        resp = getResponse(isoDep, resp)
                        if (isSw9000(resp)) {
                            val recordTlvs = flattenTlv(parseTlv(dropSw(resp)))
                            allTlvs.addAll(recordTlvs)
                            Log.d(TAG, "Record lido (SFI $sfi, Rec $rec): ${recordTlvs.map { it.tag }.joinToString()}")
                        }
                    }
                }
            }

            // 6. Extraction
            val panTlv = findTag(allTlvs, "5A")
            val track2Tlv = findTag(allTlvs, "57")
            
            val panRaw = if (panTlv != null) {
                panTlv.value.toHex().trimEnd('F', 'f').filter { it.isDigit() }
            } else if (track2Tlv != null) {
                extractPanFromTrack2(track2Tlv.value.toHex())
            } else ""

            if (panRaw.isEmpty()) {
                Log.e(TAG, "PAN não encontrado. Tags lidas: ${allTlvs.map { it.tag }.distinct().joinToString()}")
                return null
            }

            val name = findTag(allTlvs, "5F20")?.value?.toAscii()?.trim() ?: ""
            val expiry = findTag(allTlvs, "5F24")?.value?.toHex()?.let { formatExpiry(it) } ?: ""

            Log.d(TAG, "Sucesso: PAN=$panRaw | Nome=$name | Validade=$expiry")

            return CardData(
                brand = identifyBrand(aid),
                holder = name,
                panRaw = panRaw,
                expiry = expiry,
                cvv2 = "",
                entryMode = "CHIP",
                cryptogram = findTag(allTlvs, "9F26")?.value?.toHex() ?: "",
                atc = findTag(allTlvs, "9F36")?.value?.toHex() ?: "",
                iad = findTag(allTlvs, "9F10")?.value?.toHex() ?: "",
                aip = findTag(allTlvs, "82")?.value?.toHex() ?: "",
                tvr = findTag(allTlvs, "95")?.value?.toHex() ?: ""
            )
        } catch (e: Exception) {
            Log.e(TAG, "Erro na leitura EMV", e)
            return null
        }
    }
}
