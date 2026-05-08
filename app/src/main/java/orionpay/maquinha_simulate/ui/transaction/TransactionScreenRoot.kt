package orionpay.maquinha_simulate.ui.transaction

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import orionpay.maquinha_simulate.TransactionScreen
import orionpay.maquinha_simulate.presentation.transaction.TransactionViewModel

// Composable raiz que conecta o TransactionViewModel à TransactionScreen existente
@Composable
fun TransactionScreenRoot(
    initialProduct: String? = null,
    onEnableNfc: (Boolean) -> Unit = {},
    onBack: () -> Unit = {}
) {
    val viewModel: TransactionViewModel = viewModel()
    
    TransactionScreen(
        initialProduct = initialProduct,
        onEnableNfc = onEnableNfc,
        onBack = onBack
    )
}
