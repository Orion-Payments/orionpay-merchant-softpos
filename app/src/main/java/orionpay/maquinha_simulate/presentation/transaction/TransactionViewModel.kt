package orionpay.maquinha_simulate.presentation.transaction

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import orionpay.maquinha_simulate.data.model.TxResult
import orionpay.maquinha_simulate.domain.enums.ProductType

/**
 * ViewModel responsável por orquestrar o fluxo de transação.
 *
 * Nesta etapa inicial ele funciona como um "façade" em volta da
 * lógica que ainda reside em TransactionScreen, permitindo migrar
 * comportamento gradualmente sem alterar o layout.
 */
class TransactionViewModel : ViewModel() {

    // Estado observável da tela
    var uiState: TransactionUiState = TransactionUiState()
        private set

    /**
     * Manipula eventos recebidos da UI.
     * Nesta primeira versão, mantemos as transições simples e deixamos
     * a lógica pesada (envio de transação, NFC etc.) ainda no código
     * existente, para evitar alteração de comportamento.
     */
    fun onEvent(event: TransactionEvent) {
        when (event) {
            is TransactionEvent.OnDigit -> {
                if (uiState.rawAmount.length < 9) {
                    val newRaw = uiState.rawAmount + event.digit
                    updateAmount(newRaw)
                }
            }
            TransactionEvent.OnBackspace -> {
                if (uiState.rawAmount.isNotEmpty()) {
                    val newRaw = uiState.rawAmount.dropLast(1)
                    updateAmount(newRaw)
                }
            }
            TransactionEvent.OnClearAmount -> {
                updateAmount("")
            }
            TransactionEvent.OnNextFromAmount -> {
                val amountLong = uiState.rawAmount.toLongOrNull() ?: 0L
                if (amountLong > 0) {
                    uiState = uiState.copy(step = 2)
                }
            }
            is TransactionEvent.OnPaymentMethodSelected -> {
                uiState = uiState.copy(selectedProduct = event.productType)
            }
            TransactionEvent.OnNextFromPayment -> {
                uiState = uiState.copy(step = 3)
            }
            is TransactionEvent.OnCardRead -> {
                uiState = uiState.copy(
                    cardData = event.cardData,
                    nfcError = false,
                    nfcStatus = "Cartão lido! ${event.cardData.brand}"
                )
            }
            is TransactionEvent.OnCardError -> {
                uiState = uiState.copy(
                    nfcError = true,
                    nfcStatus = event.message
                )
            }
            TransactionEvent.OnRetry -> {
                // Nesta fase apenas limpamos o resultado e voltamos para etapa 4;
                // a lógica de idempotência ainda está na camada existente.
                uiState = uiState.copy(
                    txResult = null,
                    isSubmitting = false,
                    step = 4
                )
            }
            TransactionEvent.OnNewSale -> {
                uiState = TransactionUiState(
                    step = 1,
                    selectedProduct = ProductType.CREDIT_AVISTA,
                    externalReference = "PEDIDO-${System.currentTimeMillis() % 100000}"
                )
            }
            TransactionEvent.OnBackPressed -> {
                // A ação de back continua sendo tratada pela Activity/Composable,
                // aqui não fechamos tela para manter o fluxo atual.
            }
        }
    }

    private fun updateAmount(newRaw: String) {
        val amountDouble = (newRaw.toLongOrNull() ?: 0L) / 100.0
        val formatted = java.text.NumberFormat.getCurrencyInstance(java.util.Locale("pt", "BR"))
            .format(amountDouble)
        uiState = uiState.copy(rawAmount = newRaw, formattedAmount = formatted)
    }

    /**
     * Ponto de entrada futuro para envio da transação via use case.
     * Ainda não é usado para não duplicar a lógica atual de envio.
     */
    fun submitTransactionLegacy(delegate: suspend () -> TxResult?) {
        if (uiState.isSubmitting) return
        uiState = uiState.copy(isSubmitting = true)
        viewModelScope.launch {
            val result = delegate()
            uiState = uiState.copy(
                txResult = result,
                isSubmitting = false,
                step = 5
            )
        }
    }
}

