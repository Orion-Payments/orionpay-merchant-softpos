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
import androidx.compose.ui.graphics.StrokeCap
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


import orionpay.maquinha_simulate.ui.theme.*
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.HelpOutline

@Composable
fun StepProcessing(amount: String) {
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
            "Processando pagamento...",
            color = OrionWhite,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.weight(1f))

        // Multi-color Spinner placeholder
        Box(contentAlignment = Alignment.Center) {
            // Animating spinner (simplified for now with a circular indicator)
            CircularProgressIndicator(
                modifier = Modifier.size(80.dp),
                color = OrionWhite,
                strokeWidth = 6.dp,
                strokeCap = StrokeCap.Round
            )
        }

        Spacer(Modifier.weight(1f))

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
                Text("Valor no débito", color = OrionTextLight, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text(amount.replace("R$", "").trim(), color = OrionBlue, fontSize = 36.sp, fontWeight = FontWeight.Bold)
                Text("BRL", color = OrionTextLight, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
