package com.luckyprospector.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val ForestGreen = Color(0xFF0B5A35)
private val DeepForest = Color(0xFF073B25)
private val LeafGreen = Color(0xFF3C9B57)
private val LuckyGold = Color(0xFFE0A91B)
private val WarmIvory = Color(0xFFFFFBF1)
private val SoftSage = Color(0xFFE7F2E6)
private val PaleGold = Color(0xFFFFE8A8)

// The forest band behind the status bar, the navigation bar and the system
// navigation. It is the brand frame and stays the same in light and dark mode.
private val BarForestDeep = Color(0xFF052F20)
private val BarForestMid = Color(0xFF0B6139)
private val BarForestEdge = Color(0xFF154D33)

private val LightColors = lightColorScheme(
    primary = ForestGreen,
    onPrimary = Color.White,
    primaryContainer = SoftSage,
    onPrimaryContainer = DeepForest,
    secondary = LeafGreen,
    secondaryContainer = Color(0xFFD9EEDB),
    onSecondaryContainer = DeepForest,
    tertiary = LuckyGold,
    tertiaryContainer = PaleGold,
    onTertiaryContainer = Color(0xFF3B2B00),
    background = WarmIvory,
    surface = Color(0xFFFFFDF8),
    surfaceVariant = Color(0xFFEDF3E9),
    onBackground = Color(0xFF172019),
    onSurface = Color(0xFF172019),
    outline = Color(0xFF718075),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF8FD6A2),
    onPrimary = Color(0xFF003920),
    primaryContainer = Color(0xFF1B4D32),
    onPrimaryContainer = Color(0xFFCDEFD4),
    secondary = Color(0xFF9ED3A8),
    onSecondary = Color(0xFF0B3A1D),
    secondaryContainer = Color(0xFF244A31),
    onSecondaryContainer = Color(0xFFD9EEDB),
    tertiary = Color(0xFFE7B63A),
    onTertiary = Color(0xFF3B2B00),
    tertiaryContainer = Color(0xFF5A4300),
    onTertiaryContainer = PaleGold,
    background = Color(0xFF0F1712),
    surface = Color(0xFF151F18),
    surfaceVariant = Color(0xFF223027),
    onBackground = Color(0xFFE1E8E0),
    onSurface = Color(0xFFE1E8E0),
    onSurfaceVariant = Color(0xFFC0C9BF),
    outline = Color(0xFF8A968B),
)

/**
 * Brand colours that Material's scheme has no slot for. Screens read them from
 * [LuckyTheme.colors] instead of writing hex values inline, so dark mode and any
 * future palette change happen in this file only.
 */
@Immutable
data class LuckyColors(
    val barGradient: List<Color> = listOf(BarForestDeep, BarForestMid, BarForestEdge),
    val barSolid: Color = BarForestDeep,
    val navSelectedIcon: Color = Color(0xFF083B27),
    val navSelectedLabel: Color = Color(0xFFFFEDB0),
    val navIndicator: Color = Color(0xFFE7B63A),
    val navUnselectedIcon: Color = Color.White.copy(alpha = .94f),
    val navUnselectedLabel: Color = Color.White.copy(alpha = .90f),
    /** Text and icons on the gold selection used by chips and the nav indicator. */
    val onGold: Color = Color(0xFF083B27),
    /** Last stop of the gradient behind every main screen. */
    val screenGradientEnd: Color,
    val mapPaper: Color,
    val mapGrid: Color,
    val mapTrail: Color,
    val mapClover: Color = Color(0xFF176B3A),
    val mapPatch: Color = Color(0xFF76A66A),
    val mapFading: Color = Color(0xFFD5A62E),
    // Drawn over the live camera image, so identical in both modes.
    val cameraText: Color = Color.White,
    val cameraLuckyText: Color = Color(0xFFFFE082),
    val cameraAiBadge: Color = Color(0xFFB9F6CA),
    val cameraDemoBadge: Color = Color(0xFFFFD54F),
    val cameraError: Color = Color(0xFFFF8A80),
    val cameraBoxFourLeaf: Color = Color(0xFFFFC928),
    /** Faint squares on ordinary clovers; alpha is applied where drawn. */
    val cameraHint: Color = Color.White,
) {
    val barBrush: Brush get() = Brush.horizontalGradient(barGradient)
}

private val LightLuckyColors = LuckyColors(
    screenGradientEnd = Color(0xFFD5E9D5),
    mapPaper = Color(0xFFF2EEDB),
    mapGrid = Color(0xFFD5D7C2),
    mapTrail = Color(0xFFBFC8A5),
)

private val DarkLuckyColors = LuckyColors(
    screenGradientEnd = Color(0xFF12301F),
    mapPaper = Color(0xFF1E2A22),
    mapGrid = Color(0xFF34443A),
    mapTrail = Color(0xFF4C5E45),
)

private val LocalLuckyColors = staticCompositionLocalOf { LightLuckyColors }

object LuckyTheme {
    val colors: LuckyColors
        @Composable get() = LocalLuckyColors.current
}

private val Shapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
)

@Composable
fun LuckyProspectorTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalLuckyColors provides if (darkTheme) DarkLuckyColors else LightLuckyColors) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColors else LightColors,
            shapes = Shapes,
            content = content,
        )
    }
}

/** The background gradient shared by the main screens. */
@Composable
fun luckyScreenBrush(): Brush = Brush.verticalGradient(
    listOf(
        MaterialTheme.colorScheme.background,
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = .72f),
        LuckyTheme.colors.screenGradientEnd,
    ),
)
