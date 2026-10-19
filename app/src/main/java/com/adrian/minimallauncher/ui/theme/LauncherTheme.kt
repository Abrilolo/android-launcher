package com.adrian.minimallauncher.ui.theme
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
val LauncherBlack = Color(0xFF000000)
val LauncherWhite = Color(0xFFFFFFFF)
val LauncherGray = Color(0xFF8A8A8A)
val LauncherDim = Color(0xFF4A4A4A)
private val colors = darkColorScheme(
    primary = LauncherWhite,
    onPrimary = LauncherBlack,
    secondary = LauncherGray,
    onSecondary = LauncherBlack,
    background = LauncherBlack,
    onBackground = LauncherWhite,
    surface = LauncherBlack,
    onSurface = LauncherWhite,
    onSurfaceVariant = LauncherGray,
    outline = LauncherDim,
)
private val typography = Typography(
    displayLarge = TextStyle(fontSize = 72.sp, fontWeight = FontWeight.Light, letterSpacing = (-2).sp),
    titleMedium = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Normal, letterSpacing = 2.sp),
    headlineSmall = TextStyle(fontSize = 26.sp, fontWeight = FontWeight.Normal),
    bodyLarge = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Normal),
    bodyMedium = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Normal),
)
@Composable
fun LauncherTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = colors, typography = typography) {
        CompositionLocalProvider(LocalContentColor provides LauncherWhite, content = content)
    }
}
