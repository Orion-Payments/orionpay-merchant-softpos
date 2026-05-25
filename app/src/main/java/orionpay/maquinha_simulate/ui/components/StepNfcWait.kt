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


import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import orionpay.maquinha_simulate.ui.theme.*
import androidx.compose.ui.graphics.RectangleShape

@Composable
fun StepNfcWait(amount: String, product: ProductType, status: String, hasError: Boolean, onNext: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "nfc")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.92f, targetValue = 1.08f,
        animationSpec = infiniteRepeatable(tween(1200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "nfc_pulse"
    )

    Column(
        Modifier.fillMaxSize().background(OrionBlue),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Toolbar style
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = OrionWhite, modifier = Modifier.size(24.dp))
            Spacer(Modifier.weight(1f))
            Text("OrionPay", color = OrionWhite, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            Icon(Icons.AutoMirrored.Filled.HelpOutline, null, tint = OrionWhite, modifier = Modifier.size(24.dp))
        }

        Spacer(Modifier.height(32.dp))
        Text(
            "Aproxime o cartão ou dispositivo\natrás deste celular",
            color = OrionWhite,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 40.dp)
        )

        Spacer(Modifier.weight(1f))

        // NFC Icon Area
        Box(contentAlignment = Alignment.Center) {
             // Outer pulse
             Box(
                Modifier
                    .size(200.dp)
                    .scale(pulse)
                    .clip(CircleShape)
                    .background(OrionWhite.copy(alpha = 0.1f))
            )
            // Phone/Card Icon Placeholder
            Surface(
                modifier = Modifier.size(160.dp),
                shape = CircleShape,
                color = OrionWhite.copy(alpha = 0.15f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.Contactless,
                        contentDescription = null,
                        tint = OrionWhite,
                        modifier = Modifier.size(80.dp)
                    )
                }
            }
        }
        
        Spacer(Modifier.height(24.dp))
        Text("01:00", color = OrionWhite, fontSize = 14.sp)
        Spacer(Modifier.height(8.dp))
        Box(Modifier.width(80.dp).height(2.dp).background(OrionWhite.copy(alpha = 0.3f)))

        Spacer(Modifier.weight(1f))

        // Status Error
        AnimatedVisibility(visible = hasError) {
            Text(status, color = OrionWhite, fontSize = 14.sp, textAlign = TextAlign.Center,
                modifier = Modifier.padding(16.dp).fillMaxWidth())
        }

        // Rodapé com valor estilo OrionPay
        Surface(
            Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
            color = OrionWhite
        ) {
            Column(
                Modifier.padding(horizontal = 24.dp, vertical = 24.dp).padding(bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Valor no ${product.label.lowercase()}", color = OrionTextLight, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text(amount, color = OrionBlue, fontSize = 36.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
