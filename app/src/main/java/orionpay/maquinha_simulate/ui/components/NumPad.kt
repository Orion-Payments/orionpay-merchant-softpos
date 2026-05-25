package orionpay.maquinha_simulate.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import orionpay.maquinha_simulate.ui.theme.OrionBlue
import orionpay.maquinha_simulate.ui.theme.OrionText
import orionpay.maquinha_simulate.ui.theme.OrionTextLight
import orionpay.maquinha_simulate.ui.theme.OrionSuccess
import androidx.compose.material.icons.filled.CheckCircle

// ─── Componente: tecla do teclado numérico estilo OrionPay ─────────

@Composable
fun RowScope.NumPadKey(key: String, onTap: () -> Unit) {
    val isBs  = key == "⌫"
    val isDot = key == "."
    val isCheck = key == "check"
    val scope    = rememberCoroutineScope()

    Box(
        Modifier
            .weight(1f)
            .height(64.dp)
            .background(if (isCheck) OrionSuccess.copy(alpha = 0.1f) else Color.Transparent)
            .clickable { onTap() },
        contentAlignment = Alignment.Center
    ) {
        if (isBs) {
            Icon(Icons.Default.Backspace, null, tint = OrionTextLight, modifier = Modifier.size(24.dp))
        } else if (key == "check") {
            Icon(Icons.Default.CheckCircle, null, tint = OrionSuccess.copy(alpha = 0.6f), modifier = Modifier.size(28.dp))
        } else {
            Text(
                key,
                color      = if (isDot) OrionTextLight else OrionText,
                fontSize   = 24.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
