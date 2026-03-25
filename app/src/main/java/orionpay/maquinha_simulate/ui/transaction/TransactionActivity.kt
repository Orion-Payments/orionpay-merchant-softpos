package orionpay.maquinha_simulate.ui.transaction

import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.IsoDep
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.fragment.app.FragmentActivity
import orionpay.maquinha_simulate.NfcDataBus
import orionpay.maquinha_simulate.OrionPayTheme
import orionpay.maquinha_simulate.data.nfc.ReadEmvReader

class TransactionActivity : FragmentActivity(), NfcAdapter.ReaderCallback {

    private var nfcAdapter: NfcAdapter? = null
    private var isNfcEnabled = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        enableEdgeToEdge()
        setContent {
            OrionPayTheme {
                TransactionScreenRoot(
                    onEnableNfc = { enabled ->
                        isNfcEnabled = enabled
                        updateNfcState()
                    },
                    onBack = { finish() }
                )
            }
        }
    }

    private fun updateNfcState() {
        if (isNfcEnabled) {
            nfcAdapter?.enableReaderMode(
                this,
                this,
                NfcAdapter.FLAG_READER_NFC_A or
                        NfcAdapter.FLAG_READER_NFC_B or
                        NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK,
                null
            )
        } else {
            nfcAdapter?.disableReaderMode(this)
        }
    }

    override fun onResume() {
        super.onResume()
        if (isNfcEnabled) updateNfcState()
    }

    override fun onPause() {
        super.onPause()
        nfcAdapter?.disableReaderMode(this)
    }

    override fun onTagDiscovered(tag: Tag) {
        val readEmvReader = ReadEmvReader()
        val isoDep = IsoDep.get(tag) ?: return
        try {
            isoDep.connect()
            isoDep.timeout = 10000 // Aumentado para 10s para maior estabilidade
            val card = readEmvReader.readEmvCard(isoDep)
            if (card != null) {
                runOnUiThread {
                    NfcDataBus.onCardRead?.invoke(card)
                }
            } else {
                runOnUiThread {
                    NfcDataBus.onCardError?.invoke("Cartão não suportado ou erro de leitura")
                }
            }
        } catch (e: Exception) {
            runOnUiThread {
                NfcDataBus.onCardError?.invoke(e.message ?: "Erro NFC")
            }
        } finally {
            try {
                isoDep.close()
            } catch (_: Exception) {
            }
        }
    }
}
