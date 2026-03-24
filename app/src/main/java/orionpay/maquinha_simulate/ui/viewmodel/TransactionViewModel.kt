package orionpay.maquinha_simulate.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import orionpay.maquinha_simulate.data.model.CardData
import orionpay.maquinha_simulate.data.model.ProductType
import orionpay.maquinha_simulate.data.model.TxResult
import orionpay.maquinha_simulate.data.model.TxState
import orionpay.maquinha_simulate.data.repository.TransactionRepository

class TransactionViewModel : ViewModel() {
    private val repository = TransactionRepository()

    private val _uiState = MutableStateFlow(TxResult())
    val uiState: StateFlow<TxResult> = _uiState.asStateFlow()

    private val _cardData = MutableStateFlow(CardData())
    val cardData: StateFlow<CardData> = _cardData.asStateFlow()

    private val _amount = MutableStateFlow("")
    val amount: StateFlow<String> = _amount.asStateFlow()

    private val _selectedProduct = MutableStateFlow(ProductType.CREDIT_AVISTA)
    val selectedProduct: StateFlow<ProductType> = _selectedProduct.asStateFlow()

    fun updateCardData(data: CardData) {
        _cardData.value = data
    }

    fun onAmountChange(newAmount: String) {
        if (newAmount.length <= 9) _amount.value = newAmount
    }

    fun onProductSelected(product: ProductType) {
        _selectedProduct.value = product
    }

    fun processTransaction(cvv: String) {
        val card = _cardData.value
        val amountDouble = (_amount.value.toLongOrNull() ?: 0L) / 100.0
        
        val payload = repository.buildPayload(
            merchantId = "3f90ed27-6eca-4bf6-a4e1-607ac55ea73b",
            amount = amountDouble,
            productType = _selectedProduct.value.apiKey,
            terminalSn = "POS-ORION-992",
            externalRef = "REF-${System.currentTimeMillis() % 10000}",
            entryMode = card.entryMode,
            brand = card.brand,
            holder = card.holder,
            number = card.number,
            expiry = card.expiry,
            cvv = cvv.ifEmpty { "000" }
        )

        viewModelScope.launch {
            _uiState.value = TxResult(TxState.LOADING)
            _uiState.value = repository.authorize(payload)
        }
    }
    
    fun reset() {
        _uiState.value = TxResult()
        _amount.value = ""
    }
}
