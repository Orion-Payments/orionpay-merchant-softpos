package orionpay.maquinha_simulate.ui.splash

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import orionpay.maquinha_simulate.OrionPayTheme
import orionpay.maquinha_simulate.ui.home.HomeActivity
import orionpay.maquinha_simulate.ui.theme.*
import orionpay.maquinha_simulate.ui.transaction.TransactionActivity

class SplashActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            OrionPayTheme {
                SplashScreen {
                    startActivity(Intent(this, HomeActivity::class.java))
                    finish()
                }
            }
        }
    }
}

@Composable
fun SplashScreen(onFinished: () -> Unit) {
    // Animações escalonadas
    val logoScale     = remember { Animatable(0f) }
    val logoAlpha     = remember { Animatable(0f) }
    val nameAlpha     = remember { Animatable(0f) }
    val nameOffsetY   = remember { Animatable(24f) }
    val taglineAlpha  = remember { Animatable(0f) }
    val ringScale1    = remember { Animatable(0f) }
    val ringAlpha1    = remember { Animatable(0f) }
    val ringScale2    = remember { Animatable(0f) }
    val ringAlpha2    = remember { Animatable(0f) }
    val bgAlpha       = remember { Animatable(1f) }

    LaunchedEffect(Unit) {
        // 1. Anéis expandindo
        launch {
            delay(100)
            ringScale1.animateTo(1f, tween(600, easing = FastOutSlowInEasing))
        }
        launch {
            delay(100)
            ringAlpha1.animateTo(1f, tween(400))
        }
        launch {
            delay(280)
            ringScale2.animateTo(1f, tween(700, easing = FastOutSlowInEasing))
        }
        launch {
            delay(280)
            ringAlpha2.animateTo(0.5f, tween(500))
        }
        // 2. Logo aparece com bounce
        delay(300)
        launch {
            logoScale.animateTo(
                1.15f, tween(350, easing = FastOutSlowInEasing)
            )
            logoScale.animateTo(1f, tween(150, easing = FastOutSlowInEasing))
        }
        launch { logoAlpha.animateTo(1f, tween(300)) }
        // 3. Nome desliza para cima
        delay(500)
        launch { nameAlpha.animateTo(1f, tween(400)) }
        launch { nameOffsetY.animateTo(0f, tween(400, easing = FastOutSlowInEasing)) }
        // 4. Tagline aparece
        delay(800)
        taglineAlpha.animateTo(1f, tween(400))
        // 5. Aguarda e faz fade-out
        delay(1200)
        bgAlpha.animateTo(0f, tween(500))
        onFinished()
    }

    Box(
        Modifier
            .fillMaxSize()
            .graphicsLayer { alpha = bgAlpha.value }
            .background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        // Anel externo
        Box(
            Modifier
                .size(260.dp)
                .graphicsLayer {
                    scaleX = ringScale2.value
                    scaleY = ringScale2.value
                    alpha  = ringAlpha2.value
                }
                .clip(RoundedCornerShape(130.dp))
                .background(OrionBlue.copy(alpha = 0.08f))
        )
        // Anel médio
        Box(
            Modifier
                .size(190.dp)
                .graphicsLayer {
                    scaleX = ringScale1.value
                    scaleY = ringScale1.value
                    alpha  = ringAlpha1.value
                }
                .clip(RoundedCornerShape(95.dp))
                .background(OrionBlue.copy(alpha = 0.14f))
        )

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // Logo
            Box(
                Modifier
                    .size(90.dp)
                    .graphicsLayer {
                        scaleX = logoScale.value
                        scaleY = logoScale.value
                        alpha  = logoAlpha.value
                    }
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(OrionBlue, OrionBlue.copy(alpha = 0.85f))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Shield,
                    contentDescription = null,
                    tint     = Color.White,
                    modifier = Modifier.size(46.dp)
                )
            }

            Spacer(Modifier.height(28.dp))

            // Nome do app
            Box(
                Modifier.graphicsLayer {
                    alpha        = nameAlpha.value
                    translationY = nameOffsetY.value * density
                }
            ) {
                Text(
                    "OrionPay",
                    color      = OrionText,
                    fontSize   = 34.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }

            Spacer(Modifier.height(10.dp))

            // Tagline
            Box(Modifier.graphicsLayer { alpha = taglineAlpha.value }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(OrionBlue)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Pagamentos seguros e rápidos",
                        color    = OrionTextLight,
                        fontSize = 14.sp
                    )
                    Spacer(Modifier.width(8.dp))
                    Box(
                        Modifier
                            .size(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(OrionBlue)
                    )
                }
            }
        }
    }
}
