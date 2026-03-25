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
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.util.*


@Composable
fun StepProcessing(amount: String) {
    val infiniteTransition = rememberInfiniteTransition(label = "proc")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(1200, easing = LinearEasing)),
        label = "rotation"
    )

    Column(
        Modifier.fillMaxSize().background(OrionBlue),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.weight(1f))

        Text("Processando pagamento...", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(48.dp))

        Box(Modifier.size(100.dp), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(
                modifier    = Modifier.size(100.dp),
                color       = Color.White,
                strokeWidth = 6.dp
            )
        }

        Spacer(Modifier.weight(1f))

        val navPad5 = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        Column(
            Modifier
                .fillMaxWidth()
                .background(Color.White.copy(alpha = 0.1f))
                .padding(start = 24.dp, end = 24.dp, top = 20.dp, bottom = 20.dp + navPad5),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Valor do pagamento", color = Color.White.copy(alpha = 0.8f), fontSize = 13.sp)
            Spacer(Modifier.height(4.dp))
            Text(amount, color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        }
    }
}