package orionpay.maquinha_simulate.ui.transaction

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import orionpay.maquinha_simulate.TransactionScreen
import orionpay.maquinha_simulate.presentation.transaction.TransactionViewModel

// Composable raiz que conecta o TransactionViewModel à TransactionScreen existente
@Composable
fun TransactionScreenRoot(
    onEnableNfc: (Boolean) -> Unit = {},
    onBack: () -> Unit = {}
) {
    val viewModel: TransactionViewModel = viewModel()
    // Por enquanto apenas delega para a TransactionScreen legada para
    // manter o comportamento; integração com o ViewModel será feita depois.
    TransactionScreen(
        onEnableNfc = onEnableNfc,
        onBack = onBack
    )
}
