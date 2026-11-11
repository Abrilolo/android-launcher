package com.adrian.minimallauncher.ui.theme

import androidx.compose.foundation.IndicationNodeFactory
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val LauncherBlack = Color(0xFF000000)
// Diseño 2026-11-09: texto principal #f8f9f7; secundario #E8E8E8 (sustituye al gris #B0B0B0 que se perdía).
val LauncherWhite = Color(0xFFF8F9F7)
val LauncherGray = Color(0xFFE8E8E8)
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

// Escala estricta (diseño 2026-11-10):
//  Headline: vista 40 (headlineLarge), sección 24 (headlineMedium), cifra destacada 32 (headlineSmall).
//  Title: subsección 20 (titleLarge), elemento 17 (titleMedium), elemento chico 15 (titleSmall).
//  Body: 17 / 15 / 14.  Caption: 15 / 13 / 12 (labelLarge / labelMedium / labelSmall).
private val typography = Typography(
    displayLarge = TextStyle(fontSize = 72.sp, fontWeight = FontWeight.Light, letterSpacing = (-2).sp),
    headlineLarge = TextStyle(fontSize = 40.sp, lineHeight = 44.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.5).sp),
    headlineMedium = TextStyle(fontSize = 24.sp, lineHeight = 30.sp, fontWeight = FontWeight.Bold),
    headlineSmall = TextStyle(fontSize = 32.sp, lineHeight = 36.sp, fontWeight = FontWeight.ExtraBold),
    titleLarge = TextStyle(fontSize = 20.sp, lineHeight = 26.sp, fontWeight = FontWeight.Bold),
    titleMedium = TextStyle(fontSize = 17.sp, lineHeight = 22.sp, fontWeight = FontWeight.SemiBold),
    titleSmall = TextStyle(fontSize = 15.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = TextStyle(fontSize = 17.sp, lineHeight = 22.sp, fontWeight = FontWeight.Medium),
    bodyMedium = TextStyle(fontSize = 15.sp, lineHeight = 20.sp, fontWeight = FontWeight.Normal),
    bodySmall = TextStyle(fontSize = 14.sp, lineHeight = 18.sp, fontWeight = FontWeight.Normal),
    labelLarge = TextStyle(fontSize = 15.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold),
    labelMedium = TextStyle(fontSize = 13.sp, lineHeight = 18.sp, fontWeight = FontWeight.SemiBold),
    labelSmall = TextStyle(fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.5.sp),
)

/** Suppresses Material's default rectangular ripple/press box so only dot/stroke animations react on tap. */
private object NoIndication : IndicationNodeFactory {
    override fun create(interactionSource: InteractionSource): DelegatableNode = object : Modifier.Node() {}
    override fun hashCode(): Int = -1
    override fun equals(other: Any?): Boolean = other === this
}

@Composable
fun LauncherTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = colors, typography = typography) {
        CompositionLocalProvider(
            LocalContentColor provides LauncherWhite,
            LocalIndication provides NoIndication,
            content = content,
        )
    }
}
