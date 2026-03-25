package orionpay.maquinha_simulate.presentation.transaction

import orionpay.maquinha_simulate.data.model.CardData
import orionpay.maquinha_simulate.domain.enums.ProductType

/** Eventos disparados pela UI ou pelo fluxo NFC para a tela de transação. */
sealed class TransactionEvent {
    data class OnDigit(val digit: Char) : TransactionEvent()
    object OnBackspace : TransactionEvent()
    object OnClearAmount : TransactionEvent()
    object OnNextFromAmount : TransactionEvent()

    data class OnPaymentMethodSelected(val productType: ProductType) : TransactionEvent()
    object OnNextFromPayment : TransactionEvent()

    data class OnCardRead(val cardData: CardData) : TransactionEvent()
    data class OnCardError(val message: String) : TransactionEvent()

    object OnRetry : TransactionEvent()
    object OnNewSale : TransactionEvent()
    object OnBackPressed : TransactionEvent()
}

