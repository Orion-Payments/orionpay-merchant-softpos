package orionpay.maquinha_simulate.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import orionpay.maquinha_simulate.OrionBlue
import orionpay.maquinha_simulate.OrionNavyLight
import orionpay.maquinha_simulate.OrionNavyMid
import orionpay.maquinha_simulate.OrionSuccess
import orionpay.maquinha_simulate.OrionText

@Composable
fun OrionHeader(showBack: Boolean, onBack: () -> Unit = {}) {
    // statusBarsPadding() garante que o header não fique sob a status bar (edge-to-edge)
    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    Row(
        Modifier
            .fillMaxWidth()
            .background(OrionNavyMid)
            .padding(top = statusBarHeight + 12.dp, bottom = 12.dp, start = 20.dp, end = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (showBack) {
            Icon(
                Icons.Default.ArrowBack,
                contentDescription = "Voltar",
                tint = OrionText,
                modifier = Modifier
                    .size(36.dp)
                    .clickable(onClick = onBack)
            )
            Spacer(Modifier.width(8.dp))
        }
        Box(
            Modifier.size(34.dp).clip(RoundedCornerShape(8.dp)).background(OrionBlue),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Shield, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(10.dp))
        Text("OrionPay", color = OrionText, fontSize = 17.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.weight(1f))
        Row(
            Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(OrionNavyLight)
                .padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Lock, contentDescription = null, tint = OrionSuccess, modifier = Modifier.size(11.dp))
            Spacer(Modifier.width(4.dp))
            Text("Seguro", color = OrionSuccess, fontSize = 11.sp)
        }
    }
}
