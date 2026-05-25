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
import orionpay.maquinha_simulate.data.model.ComprovanteData
import orionpay.maquinha_simulate.data.model.TxResult
import orionpay.maquinha_simulate.domain.enums.ProductType
import orionpay.maquinha_simulate.domain.enums.TxState
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.util.*




import orionpay.maquinha_simulate.ui.theme.*
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.ReceiptLong

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
            onBackToHome  = onNewSale
        )
        return
    }

    Column(
        Modifier.fillMaxSize().background(OrionWhite),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Toolbar style
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = OrionBlue, modifier = Modifier.size(24.dp).clickable { onBack() })
            Spacer(Modifier.weight(1f))
            Text("OrionPay", color = OrionBlue, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            Icon(Icons.AutoMirrored.Filled.HelpOutline, null, tint = OrionBlue, modifier = Modifier.size(24.dp))
        }

        Spacer(Modifier.height(32.dp))

        Text(
            if (ok) "Pagamento efetuado!" else "Pagamento não autorizado",
            color = if (ok) OrionBlueDark else OrionError,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(48.dp))

        // Ícone de resultado
        Surface(
            modifier = Modifier.size(140.dp),
            shape = CircleShape,
            color = if (ok) OrionSuccess else OrionError.copy(alpha = 0.1f),
            border = if (ok) null else BorderStroke(2.dp, OrionError)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    if (ok) Icons.Default.Check else Icons.Default.Close,
                    contentDescription = null,
                    tint = if (ok) OrionWhite else OrionError,
                    modifier = Modifier.size(80.dp)
                )
            }
        }

        Spacer(Modifier.height(64.dp))

        Text("Valor pago no ${product.label.lowercase()}", color = OrionTextLight, fontSize = 14.sp)
        Spacer(Modifier.height(8.dp))
        Text(amount, color = OrionBlue, fontSize = 36.sp, fontWeight = FontWeight.Bold)

        if (!ok && result?.message?.isNotEmpty() == true) {
            Spacer(Modifier.height(16.dp))
            Text(result.message, color = OrionError, fontSize = 14.sp, textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 40.dp))
        }

        Spacer(Modifier.weight(1f))

        // Botão de ação (Estilo barra inferior OrionPay)
        Surface(
            color = OrionBlue,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .height(64.dp)
                    .clickable {
                        if (ok) showComprovante = true
                        else if (canRetry) onRetry()
                        else onNewSale()
                    },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.padding(start = 20.dp).size(24.dp)
                )
                
                Text(
                    text = if (ok) "Ver comprovante" else if (canRetry) "Tentar novamente" else "Nova venda",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = OrionWhite
                )

                Icon(
                    Icons.AutoMirrored.Filled.ReceiptLong,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.padding(end = 20.dp).size(24.dp)
                )
            }
        }
    }
}
