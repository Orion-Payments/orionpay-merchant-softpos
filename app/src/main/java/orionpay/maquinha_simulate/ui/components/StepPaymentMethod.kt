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
import orionpay.maquinha_simulate.OrionTextSub
import orionpay.maquinha_simulate.domain.enums.ProductType
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.util.*

@Composable
fun StepPaymentMethod(
    amount: String, selected: ProductType,
    onSelect: (ProductType) -> Unit, onNext: () -> Unit
) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Column(Modifier.padding(horizontal = 24.dp)) {
            Spacer(Modifier.height(24.dp))
            Text(
                "Escolha a forma de pagamento",
                color = OrionText, fontSize = 20.sp, fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(6.dp))

            // Chip do valor
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Valor da venda", color = OrionTextMuted, fontSize = 13.sp)
                Spacer(Modifier.width(8.dp))
                Box(
                    Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(OrionBlue.copy(alpha = 0.15f))
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text(amount, color = OrionBlue, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(Modifier.height(20.dp))
            Text("Forma de pagamento", color = OrionTextMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(10.dp))
        }

        // Lista de produtos
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(OrionNavyMid)
                .border(1.dp, OrionNavyLight, RoundedCornerShape(16.dp))
                .padding(horizontal = 24.dp)
        ) {
            ProductType.entries.forEachIndexed { idx, pt ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { onSelect(pt) }
                        .background(if (pt == selected) OrionBlue.copy(0.08f) else Color.Transparent)
                        .padding(vertical = 16.dp, horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Ícone do produto
                    Box(
                        Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(OrionNavyLight),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            when {
                                pt.apiKey.contains("DEBIT") -> Icons.Default.AccountBalance
                                pt.apiKey == "PIX"          -> Icons.Default.Bolt
                                else                         -> Icons.Default.CreditCard
                            },
                            contentDescription = null,
                            tint     = if (pt == selected) OrionBlue else OrionTextMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(Modifier.width(14.dp))
                    Text(pt.label, color = OrionText, fontSize = 15.sp, modifier = Modifier.weight(1f))
                    RadioButton(
                        selected = pt == selected, onClick = { onSelect(pt) },
                        colors   = RadioButtonDefaults.colors(selectedColor = OrionBlue, unselectedColor = OrionTextSub)
                    )
                }
                if (idx < ProductType.entries.lastIndex)
                    HorizontalDivider(color = OrionNavyLight)
            }
        }

        Spacer(Modifier.height(24.dp))
        val navPad2 = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        Box(Modifier.padding(start = 24.dp, end = 24.dp, bottom = navPad2 + 8.dp)) {
            Button(
                onClick  = onNext,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape    = RoundedCornerShape(14.dp),
                colors   = ButtonDefaults.buttonColors(containerColor = OrionBlue)
            ) {
                Text("Vender", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}
