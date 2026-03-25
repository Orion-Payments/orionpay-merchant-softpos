package orionpay.maquinha_simulate

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Core Orion color palette used across the app UI
val OrionBlue      = Color(0xFF2563EB)
val OrionNavy      = Color(0xFF020617)
val OrionNavyMid   = Color(0xFF0B1220)
val OrionNavyLight = Color(0xFF1E293B)
val OrionGreenTap  = Color(0xFF22C55E)
val OrionSuccess   = Color(0xFF22C55E)
val OrionError     = Color(0xFFEF4444)
val OrionText      = Color(0xFFF9FAFB)
val OrionTextMuted = Color(0xFF9CA3AF)
val OrionTextSub   = Color(0xFFCBD5F5)

@Composable
fun OrionPayTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = MaterialTheme.colorScheme.copy(
            primary = OrionBlue,
            secondary = OrionGreenTap,
            error = OrionError,
        ),
        typography = MaterialTheme.typography,
        content = content
    )
}

@Composable
fun orionTextFieldColors(): TextFieldColors = OutlinedTextFieldDefaults.colors(
    focusedBorderColor  = OrionBlue,
    unfocusedBorderColor = OrionNavyLight,
    focusedLabelColor    = OrionText,
    unfocusedLabelColor  = OrionTextMuted,
    focusedTextColor     = OrionText,
    unfocusedTextColor   = OrionText,
    cursorColor          = OrionBlue
)

