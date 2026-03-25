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
import orionpay.maquinha_simulate.OrionGreenTap
import orionpay.maquinha_simulate.OrionNavyLight
import orionpay.maquinha_simulate.OrionText
import orionpay.maquinha_simulate.OrionTextMuted
import orionpay.maquinha_simulate.OrionTextSub

// ─── Componente: tecla do teclado numérico com efeito de toque verde ─────────

@Composable
fun RowScope.NumPadKey(key: String, onTap: () -> Unit) {
    val isBs  = key == "⌫"
    val isDot = key == "."
    val tapAnim  = remember { Animatable(0f) }
    val tapScale = remember { Animatable(1f) }
    val scope    = rememberCoroutineScope()

    Box(
        Modifier
            .weight(1f)
            .height(58.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(
                when {
                    isBs  -> OrionNavyLight
                    isDot -> Color.Transparent
                    else  -> OrionNavyLight.copy(alpha = 0.4f)
                }
            )
            .clickable(
                indication = null,
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
            ) {
                scope.launch {
                    launch { tapAnim.snapTo(1f); tapAnim.animateTo(0f, tween(380, easing = FastOutSlowInEasing)) }
                    launch { tapScale.snapTo(0.88f); tapScale.animateTo(1f, tween(220, easing = FastOutSlowInEasing)) }
                }
                onTap()
            },
        contentAlignment = Alignment.Center
    ) {
        // Flash verde arredondado
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = tapAnim.value }
                .clip(RoundedCornerShape(14.dp))
                .background(OrionGreenTap.copy(alpha = 0.35f))
        )
        // Conteúdo com escala
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer { scaleX = tapScale.value; scaleY = tapScale.value },
            contentAlignment = Alignment.Center
        ) {
            if (isBs) {
                Icon(Icons.Default.Backspace, null, tint = OrionTextMuted, modifier = Modifier.size(22.dp))
            } else {
                Text(
                    key,
                    color      = if (isDot) OrionTextSub else OrionText,
                    fontSize   = 22.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}