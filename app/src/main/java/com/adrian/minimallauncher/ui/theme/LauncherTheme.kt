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
val LauncherWhite = Color(0xFFFFFFFF)
// Secondary text: light enough to read without effort (raised from #8A8A8A, see docs/design/PLAN_VISUAL.md).
val LauncherGray = Color(0xFFB0B0B0)
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
    bodySmall = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Normal),
    labelSmall = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Normal, letterSpacing = 1.5.sp),
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
