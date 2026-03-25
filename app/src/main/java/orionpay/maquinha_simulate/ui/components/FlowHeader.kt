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
import orionpay.maquinha_simulate.OrionSuccess
import orionpay.maquinha_simulate.OrionText
import orionpay.maquinha_simulate.OrionTextSub


@Composable
fun FlowHeader(currentStep: Int, totalSteps: Int, onBack: () -> Unit) {
    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    Column(
        Modifier
            .fillMaxWidth()
            .background(OrionNavyMid)
            .padding(top = statusBarHeight + 12.dp, bottom = 14.dp, start = 20.dp, end = 20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.ArrowBack, null, tint = OrionText, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(8.dp))
            // Bolinhas de etapa
            Row(
                Modifier.weight(1f),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                (1..totalSteps).forEach { step ->
                    val isDone    = step < currentStep
                    val isCurrent = step == currentStep
                    // Bolinha
                    Box(
                        Modifier
                            .size(if (isCurrent) 30.dp else 22.dp)
                            .clip(RoundedCornerShape(15.dp))
                            .background(
                                when {
                                    isCurrent -> OrionBlue
                                    isDone    -> OrionSuccess
                                    else      -> OrionNavyLight
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isDone) {
                            Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(12.dp))
                        } else {
                            Text(
                                "$step",
                                color    = if (isCurrent) Color.White else OrionTextSub,
                                fontSize = if (isCurrent) 13.sp else 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    // Linha entre bolinhas
                    if (step < totalSteps) {
                        Box(
                            Modifier
                                .width(14.dp)
                                .height(2.dp)
                                .background(if (isDone) OrionSuccess else OrionNavyLight)
                        )
                    }
                }
            }
            Spacer(Modifier.width(32.dp))
        }
    }
}