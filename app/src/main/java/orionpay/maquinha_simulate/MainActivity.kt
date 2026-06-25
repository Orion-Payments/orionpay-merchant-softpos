package orionpay.maquinha_simulate

import android.content.Intent
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.IsoDep
import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import orionpay.maquinha_simulate.data.model.CardData
import orionpay.maquinha_simulate.data.nfc.ReadEmvReader
import orionpay.maquinha_simulate.ui.splash.SplashActivity

// Objeto global para comunicação NFC → camada de apresentação
object NfcDataBus {
    var onCardRead: ((CardData) -> Unit)? = null
    var onCardError: ((String) -> Unit)? = null
    var amountToRead: Double = 0.0
}

class MainActivity : FragmentActivity(), NfcAdapter.ReaderCallback {

    private var nfcAdapter: NfcAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Inicializa o adaptador NFC aqui para que MainActivity possa controlar o reader mode
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        // Exibe splash antes de ir para a tela de venda
        startActivity(Intent(this, SplashActivity::class.java))
        finish()
    }

    override fun onResume() {
        super.onResume()
        nfcAdapter?.enableReaderMode(
            this,
            this,
            NfcAdapter.FLAG_READER_NFC_A or
                NfcAdapter.FLAG_READER_NFC_B or
                NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK,
            null
        )
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
            isoDep.timeout = 5000
            val card = readEmvReader.readEmvCard(isoDep, NfcDataBus.amountToRead)
            if (card != null) {
                NfcDataBus.onCardRead?.invoke(card)
            } else {
                NfcDataBus.onCardError?.invoke("Cartão não suportado")
            }
        } catch (e: Exception) {
            NfcDataBus.onCardError?.invoke(e.message ?: "Erro NFC")
        } finally {
            try {
                isoDep.close()
            } catch (_: Exception) {
            }
        }
    }
}
