package orionpay.maquinha_simulate.ui.components


import android.content.Intent
import orionpay.maquinha_simulate.BuildConfig
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.IsoDep
import android.os.Bundle
import android.util.Log
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.input.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.*
import org.json.JSONObject
import orionpay.maquinha_simulate.OrionBlue
import orionpay.maquinha_simulate.OrionError
import orionpay.maquinha_simulate.OrionNavyLight
import orionpay.maquinha_simulate.OrionNavyMid
import orionpay.maquinha_simulate.OrionSuccess
import orionpay.maquinha_simulate.OrionText
import orionpay.maquinha_simulate.OrionTextMuted
import orionpay.maquinha_simulate.data.model.ComprovanteData
import orionpay.maquinha_simulate.data.model.TxResult
import orionpay.maquinha_simulate.domain.enums.ProductType
import orionpay.maquinha_simulate.domain.enums.TxState
import orionpay.maquinha_simulate.ui.theme.OrionWarning
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.util.*




@Composable
fun StepResult(
    result: TxResult?, amount: String, product: ProductType,
    comprovante: ComprovanteData? = null,
    canRetry: Boolean = false,
    onRetry: () -> Unit = {},
    onNewSale: () -> Unit,
    onBack: () -> Unit
) {
    val ok = result?.state == TxState.SUCCESS
    var showComprovante by remember { mutableStateOf(false) }

    if (showComprovante && comprovante != null) {
        ComprovanteScreen(
            data          = comprovante,
            onClose       = { showComprovante = false },
            // onNewSale reseta todo o estado do fluxo e volta para a etapa 1
            onBackToHome  = onNewSale
        )
        return
    }

    Column(
        Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.weight(1f))

        // Ícone de resultado
        Box(
            Modifier
                .size(120.dp)
                .clip(RoundedCornerShape(60.dp))
                .background(if (ok) OrionSuccess.copy(0.15f) else OrionError.copy(0.15f))
                .border(3.dp, if (ok) OrionSuccess else OrionError, RoundedCornerShape(60.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                if (ok) Icons.Default.Check else Icons.Default.Close,
                contentDescription = null,
                tint     = if (ok) OrionSuccess else OrionError,
                modifier = Modifier.size(56.dp)
            )
        }

        Spacer(Modifier.height(24.dp))

        Text(
            if (ok) "Pagamento efetuado!" else "Pagamento recusado",
            color      = if (ok) OrionSuccess else OrionError,
            fontSize   = 22.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(6.dp))

        if (result?.message?.isNotEmpty() == true) {
            Text(result.message, color = OrionTextMuted, fontSize = 14.sp, textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 40.dp))
        }

        Spacer(Modifier.height(20.dp))

        // Detalhes
        Column(
            Modifier
                .padding(horizontal = 24.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(OrionNavyMid)
                .border(1.dp, OrionNavyLight, RoundedCornerShape(16.dp))
                .padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("${product.label}", color = OrionTextMuted, fontSize = 13.sp)
            Spacer(Modifier.height(4.dp))
            Text(amount, color = OrionText, fontSize = 30.sp, fontWeight = FontWeight.Bold)
            if (result?.authCode?.isNotEmpty() == true) {
                Spacer(Modifier.height(10.dp))
                HorizontalDivider(color = OrionNavyLight)
                Spacer(Modifier.height(10.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Autorização", color = OrionTextMuted, fontSize = 13.sp)
                    Text(result.authCode, color = OrionText, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                }
            }
            if (result?.nsu?.isNotEmpty() == true) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("NSU", color = OrionTextMuted, fontSize = 13.sp)
                    Text(result.nsu, color = OrionText, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                }
            }
        }

        Spacer(Modifier.weight(1f))

        // Botões de ação
        val navPad4 = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        Column(Modifier.padding(start = 24.dp, end = 24.dp, top = 20.dp, bottom = 20.dp + navPad4)) {
            if (ok) {
                Button(
                    onClick  = { showComprovante = true },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape    = RoundedCornerShape(14.dp),
                    colors   = ButtonDefaults.buttonColors(containerColor = OrionBlue)
                ) {
                    Icon(Icons.Default.Receipt, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Ver comprovante", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
                Spacer(Modifier.height(10.dp))
            }
            // Retry — só aparece em erros de rede (timeout/sem conexão)
            // Reutiliza a mesma X-Idempotency-Key para evitar cobrança dupla
            if (!ok && canRetry) {
                Button(
                    onClick  = onRetry,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape    = RoundedCornerShape(14.dp),
                    colors   = ButtonDefaults.buttonColors(containerColor = OrionWarning)
                ) {
                    Icon(Icons.Default.Refresh, null, modifier = Modifier.size(18.dp), tint = Color.White)
                    Spacer(Modifier.width(8.dp))
                    Text("Tentar novamente", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    "A mesma chave de idempotência será reutilizada — sem risco de cobrança dupla.",
                    color    = OrionTextMuted,
                    fontSize = 11.sp,
                    textAlign= TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
                Spacer(Modifier.height(10.dp))
            }
            OutlinedButton(
                onClick  = onNewSale,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape    = RoundedCornerShape(14.dp),
                border   = BorderStroke(1.dp, OrionNavyLight),
                colors   = ButtonDefaults.outlinedButtonColors(contentColor = OrionText)
            ) {
                Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Nova venda", fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
