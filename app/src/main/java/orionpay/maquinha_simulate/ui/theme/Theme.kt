package orionpay.maquinha_simulate.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = NuPurple,
    secondary = NuPurpleDark,
    tertiary = NuGray,
    background = NuBlack,
    surface = NuBlack,
    onPrimary = NuBackground,
    onSecondary = NuBackground,
    onTertiary = NuText,
    onBackground = NuBackground,
    onSurface = NuBackground,
)

private val LightColorScheme = lightColorScheme(
    primary = NuPurple,
    secondary = NuPurpleDark,
    tertiary = NuGray,
    background = NuBackground,
    surface = NuBackground,
    onPrimary = NuBackground,
    onSecondary = NuBackground,
    onTertiary = NuText,
    onBackground = NuText,
    onSurface = NuText,
)

@Composable
fun Maquinha_simulateTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
