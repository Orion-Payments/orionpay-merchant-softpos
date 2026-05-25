package orionpay.maquinha_simulate

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Cores Oficiais OrionPay
val OrionBlue      = Color(0xFF0082C5)
val OrionBlueDark  = Color(0xFF005A87)
val OrionNavy      = Color(0xFF0D1B2A)
val OrionText      = Color(0xFF111827) // Texto principal (Preto/Azul escuro)
val OrionTextLight = Color(0xFF6B7280) // Texto secundário (Cinza)
val OrionWhite     = Color(0xFFFFFFFF)
val OrionSuccess   = Color(0xFF22C55E)
val OrionError     = Color(0xFFEF4444)

@Composable
fun OrionPayTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = MaterialTheme.colorScheme.copy(
            primary = OrionBlue,
            onPrimary = OrionWhite,
            background = OrionWhite,
            onBackground = OrionText,
            surface = OrionWhite,
            onSurface = OrionText,
            error = OrionError
        ),
        content = content
    )
}

@Composable
fun orionTextFieldColors(): TextFieldColors = OutlinedTextFieldDefaults.colors(
    focusedBorderColor  = OrionBlue,
    unfocusedBorderColor = Color(0xFFE2E8F0),
    focusedTextColor     = OrionText,
    unfocusedTextColor   = OrionText,
    cursorColor          = OrionBlue
)
