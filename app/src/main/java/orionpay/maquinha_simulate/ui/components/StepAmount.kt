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
import orionpay.maquinha_simulate.OrionNavyLight
import orionpay.maquinha_simulate.OrionNavyMid
import orionpay.maquinha_simulate.OrionText
import orionpay.maquinha_simulate.OrionTextMuted
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.util.*


@Composable
fun StepAmount(
    formattedAmount: String, rawAmount: String,
    onDigit: (String) -> Unit, onBackspace: () -> Unit,
    onClear: () -> Unit, onNext: () -> Unit
) {
    Column(Modifier.fillMaxSize()) {
        Column(
            Modifier.weight(1f).padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("Qual o valor da venda?", color = OrionTextMuted, fontSize = 16.sp)
            Spacer(Modifier.height(12.dp))
            Text(
                formattedAmount,
                color      = OrionText,
                fontSize   = 42.sp,
                fontWeight = FontWeight.Bold,
                textAlign  = TextAlign.Center
            )
        }

        // Teclado numérico + botão próximo
        val navBarPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .background(OrionNavyMid)
                .padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 20.dp + navBarPadding)
        ) {
            listOf(listOf("1","2","3"), listOf("4","5","6"), listOf("7","8","9"), listOf(".","0","⌫")).forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    row.forEach { key ->
                        NumPadKey(
                            key         = key,
                            onTap       = { when (key) { "⌫" -> onBackspace(); "." -> {} else -> onDigit(key) } }
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))
            }
            Spacer(Modifier.height(8.dp))
            Button(
                onClick  = onNext,
                enabled  = rawAmount.isNotEmpty() && rawAmount != "0",
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape    = RoundedCornerShape(14.dp),
                colors   = ButtonDefaults.buttonColors(
                    containerColor         = OrionBlue,
                    disabledContainerColor = OrionNavyLight
                )
            ) {
                Text("Próximo", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}