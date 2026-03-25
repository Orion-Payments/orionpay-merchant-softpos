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
import orionpay.maquinha_simulate.domain.enums.ProductType
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.util.*


@Composable
fun StepNfcWait(amount: String, product: ProductType, status: String, hasError: Boolean, onNext: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "nfc")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.88f, targetValue = 1.12f,
        animationSpec = infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "nfc_pulse"
    )

    Column(
        Modifier.fillMaxSize().background(
            OrionBlue),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.weight(1f))

        // Ícone NFC animado
        Box(contentAlignment = Alignment.Center) {
            Box(
                Modifier
                    .size(180.dp)
                    .scale(pulse)
                    .clip(RoundedCornerShape(90.dp))
                    .background(Color.White.copy(alpha = 0.08f))
            )
            Box(
                Modifier
                    .size(140.dp)
                    .clip(RoundedCornerShape(70.dp))
                    .background(Color.White.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Wifi,
                    contentDescription = null,
                    tint     = Color.White,
                    modifier = Modifier.size(72.dp)
                )
            }
        }

        Spacer(Modifier.height(32.dp))
        Text(
            "Aproxime o cartão ou dispositivo"+
                    "atrás deste celular",
            color     = Color.White,
            fontSize  = 18.sp,
            fontWeight= FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier  = Modifier.padding(horizontal = 40.dp)
        )

        Spacer(Modifier.weight(1f))

        // Status da leitura NFC
        AnimatedVisibility(visible = hasError) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFEF4444).copy(alpha = 0.25f))
                    .padding(12.dp)
            ) {
                Text(status, color = Color.White, fontSize = 13.sp, textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth())
            }
        }

        Spacer(Modifier.height(16.dp))

        // Rodapé com valor
        val navPad3 = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        Column(
            Modifier
                .fillMaxWidth()
                .background(Color.White.copy(alpha = 0.1f))
                .padding(start = 24.dp, end = 24.dp, top = 20.dp, bottom = 20.dp + navPad3),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(product.label, color = Color.White.copy(alpha = 0.8f), fontSize = 13.sp)
            Spacer(Modifier.height(4.dp))
            Text(amount, color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        }
    }
}
