package com.mckaifu.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat

object ZalithThemeState {
    val themeMode = mutableStateOf("system")
    val isDark = mutableStateOf(true)

    fun init(context: android.content.Context) {
        themeMode.value = context.getSharedPreferences("mckaifu_theme", android.content.Context.MODE_PRIVATE)
            .getString("theme_mode", "system") ?: "system"
    }

    fun setMode(mode: String, context: android.content.Context) {
        themeMode.value = mode
        context.getSharedPreferences("mckaifu_theme", android.content.Context.MODE_PRIVATE)
            .edit().putString("theme_mode", mode).apply()
    }
}

private fun token(dark: Color, light: Color): Color = if (ZalithThemeState.isDark.value) dark else light

val ZalithPrimary: Color get() = token(Color(0xFF9B6DFF), Color(0xFF7C3AED))
val ZalithSecondary: Color get() = token(Color(0xFF00D4FF), Color(0xFF0097A7))
val ZalithTertiary: Color get() = token(Color(0xFFFF6B9D), Color(0xFFE91E63))
val ZalithSurface: Color get() = token(Color(0xFF0D0D1A), Color(0xFFF5F5FF))
val ZalithSurfaceVariant: Color get() = token(Color(0xFF1A1A2E), Color(0xFFE8E8F5))
val ZalithCard: Color get() = token(Color(0xFF16162A), Color.White)
val ZalithCardBorder: Color get() = token(Color(0xFF2A2A4A), Color(0xFFDCDCF0))
val ZalithBackground: Color get() = token(Color(0xFF070712), Color(0xFFF8F8FF))
val ZalithGradientStart: Color get() = token(Color(0xFF1A1040), Color(0xFFEDE0FF))
val ZalithGradientEnd: Color get() = token(Color(0xFF0D0D1A), Color(0xFFF0F0FF))

val ServerOnline = Color(0xFF2ED573)
val ServerOffline = Color(0xFF6B7280)
val ServerStarting = Color(0xFFFFA502)
val ServerError = Color(0xFFFF4757)
val ServerStopping = Color(0xFFFFA502)

val ConsoleBg: Color get() = token(Color(0xFF0A0A14), Color(0xFFF2F2F8))
val TextPrimary: Color get() = token(Color(0xFFF0F0FF), Color(0xFF1A1A2E))
val TextSecondary: Color get() = token(Color(0xFF9090B0), Color(0xFF5A5A6E))

val LogInfo: Color get() = token(Color(0xFFC0C0D0), Color(0xFF404050))
val LogWarn: Color get() = token(Color(0xFFFFAA00), Color(0xFFB26A00))
val LogError: Color get() = token(Color(0xFFFF5555), Color(0xFFC62828))
val LogSuccess: Color get() = token(Color(0xFF55FF55), Color(0xFF1B8A2C))
val LogDebug: Color get() = token(Color(0xFF5555FF), Color(0xFF3333CC))
val LogChat: Color get() = token(Color.White, Color(0xFF1A1A2E))
val LogCommand: Color get() = token(Color(0xFFFF55FF), Color(0xFF9C27B0))
val LogSystem: Color get() = token(Color(0xFF55FFFF), Color(0xFF00838F))

val GradientButton: Brush get() = Brush.linearGradient(listOf(ZalithPrimary, Color(0xFF7B4FFF)))
val GradientAccent: Brush get() = Brush.linearGradient(listOf(ZalithSecondary, Color(0xFF0099CC)))
val GradientDanger: Brush get() = Brush.linearGradient(listOf(ServerError, Color(0xFFCC2233)))

private val ZalithDarkColorScheme = darkColorScheme(
    primary = ZalithPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF3D1A8A),
    onPrimaryContainer = Color(0xFFE8D0FF),
    secondary = ZalithSecondary,
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF003344),
    onSecondaryContainer = Color(0xFF99EEFF),
    tertiary = ZalithTertiary,
    onTertiary = Color.White,
    background = ZalithBackground,
    onBackground = TextPrimary,
    surface = ZalithSurface,
    onSurface = TextPrimary,
    surfaceVariant = ZalithSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    error = ServerError,
    onError = Color.White,
    outline = ZalithCardBorder,
    outlineVariant = Color(0xFF1E1E3A),
    inverseSurface = Color(0xFFF0F0FF),
    inverseOnSurface = Color(0xFF0D0D1A),
)

private val ZalithLightColorScheme = lightColorScheme(
    primary = Color(0xFF7C3AED),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFEDE0FF),
    onPrimaryContainer = Color(0xFF1D0B58),
    secondary = Color(0xFF0097A7),
    onSecondary = Color.White,
    background = Color(0xFFF8F8FF),
    onBackground = Color(0xFF1A1A2E),
    surface = Color.White,
    onSurface = Color(0xFF1A1A2E),
    surfaceVariant = Color(0xFFF0F0FF),
    onSurfaceVariant = Color(0xFF4A4A5A),
    error = Color(0xFFD32F2F),
    onError = Color.White,
)

val ZalithTypography = Typography(
    displayLarge = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        letterSpacing = (-0.5).sp
    ),
    headlineLarge = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        letterSpacing = 0.sp
    ),
    headlineMedium = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        letterSpacing = 0.sp
    ),
    titleLarge = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        letterSpacing = 0.sp
    ),
    titleMedium = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        letterSpacing = 0.15.sp
    ),
    titleSmall = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        letterSpacing = 0.1.sp
    ),
    bodyLarge = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        letterSpacing = 0.5.sp
    ),
    bodyMedium = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        letterSpacing = 0.25.sp
    ),
    bodySmall = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        letterSpacing = 0.4.sp
    ),
    labelLarge = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        letterSpacing = 0.1.sp
    ),
    labelMedium = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        letterSpacing = 0.5.sp
    ),
    labelSmall = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 10.sp,
        letterSpacing = 0.5.sp
    ),
)

val MonospaceTypography = TextStyle(
    fontFamily = FontFamily.Monospace,
    fontSize = 12.sp,
    letterSpacing = 0.sp
)

val ZalithShapes = Shapes(
    extraSmall = androidx.compose.foundation.shape.RoundedCornerShape(6.dp),
    small = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
    medium = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
    large = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
    extraLarge = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
)

@Composable
fun McKaiFuTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    ZalithThemeState.isDark.value = darkTheme
    val colorScheme = if (darkTheme) ZalithDarkColorScheme else ZalithLightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = ZalithTypography,
        shapes = ZalithShapes,
        content = content
    )
}
