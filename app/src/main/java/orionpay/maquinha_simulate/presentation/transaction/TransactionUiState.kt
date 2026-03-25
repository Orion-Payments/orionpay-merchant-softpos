package orionpay.maquinha_simulate.presentation.transaction

import orionpay.maquinha_simulate.data.model.CardData
import orionpay.maquinha_simulate.data.model.TxResult
import orionpay.maquinha_simulate.domain.enums.ProductType

/**
 * Estado imutável da tela de transação.
 *
 * Nesta primeira fase usamos diretamente os modelos já existentes (CardData, TxResult)
 * para não alterar regras de negócio nem mapeamentos. Futuramente podemos
 * trocar por modelos de domínio puros, sem impacto na UI.
 */
data class TransactionUiState(
    val step: Int = 1,
    val rawAmount: String = "",
    val formattedAmount: String = "R$ 0,00",
    val selectedProduct: ProductType = ProductType.CREDIT_AVISTA,
    val cardData: CardData? = null,
    val nfcStatus: String = "Aguardando cartão...",
    val nfcError: Boolean = false,
    val txResult: TxResult? = null,
    // merchantId é preenchido pela camada atual (TransactionScreen/MainActivity),
    // aqui mantemos apenas um default vazio para evitar dependência de constante privada.
    val merchantId: String = "",
    val terminalSn: String = "POS-ORION-992",
    val externalReference: String = "",
    val idempotencyKey: String = "",
    val isSubmitting: Boolean = false
)

