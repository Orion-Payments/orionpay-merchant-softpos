package orionpay.maquinha_simulate.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import orionpay.maquinha_simulate.data.model.TxResult
import orionpay.maquinha_simulate.data.model.TxState
import orionpay.maquinha_simulate.ui.components.NuButton
import orionpay.maquinha_simulate.ui.theme.NuError
import orionpay.maquinha_simulate.ui.theme.NuSuccess
import orionpay.maquinha_simulate.ui.theme.NuTextMuted

@Composable
fun ResultScreen(
    result: TxResult,
    onDone: () -> Unit
) {
    val isSuccess = result.state == TxState.SUCCESS

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = if (isSuccess) Icons.Default.CheckCircle else Icons.Default.Error,
            contentDescription = null,
            tint = if (isSuccess) NuSuccess else NuError,
            modifier = Modifier.size(100.dp)
        )

        Spacer(Modifier.height(32.dp))

        Text(
            text = if (isSuccess) "Pagamento aprovado" else "Pagamento recusado",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(12.dp))

        Text(
            text = result.message,
            fontSize = 16.sp,
            color = NuTextMuted,
            textAlign = TextAlign.Center
        )

        if (isSuccess && result.nsu.isNotEmpty()) {
            Text(
                text = "NSU: ${result.nsu}",
                fontSize = 14.sp,
                color = NuTextMuted,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        Spacer(Modifier.height(48.dp))

        NuButton(
            text = "Fechar",
            onClick = onDone
        )
    }
}
