package io.github.nmorrione.forgia.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.Typeface
import androidx.compose.ui.unit.sp

/** I colori dell'HTML originale, chiaro e scuro. */
@Immutable
data class Palette(
    val bg: Color,
    val surface: Color,
    val surface2: Color,
    val ink: Color,
    val muted: Color,
    val line: Color,
    val accent: Color,
    val good: Color,
    val warn: Color,
    val bad: Color,
    val btn: Color,
    val btnInk: Color,
    val dark: Boolean,
)

val LightPalette = Palette(
    bg = Color(0xFFEDF0F2), surface = Color.White, surface2 = Color(0xFFF5F7F8), ink = Color(0xFF17202A),
    muted = Color(0xFF5A6571), line = Color(0xFFD3D9DF), accent = Color(0xFF2356C4), good = Color(0xFF2B9455),
    warn = Color(0xFFD9A400), bad = Color(0xFFCF3A2E), btn = Color(0xFF17202A), btnInk = Color.White, dark = false,
)

val DarkPalette = Palette(
    bg = Color(0xFF11161C), surface = Color(0xFF1A2129), surface2 = Color(0xFF212A33), ink = Color(0xFFE7EBEF),
    muted = Color(0xFF98A4B0), line = Color(0xFF2D3742), accent = Color(0xFF5B8BF0), good = Color(0xFF46B874),
    warn = Color(0xFFF0C030), bad = Color(0xFFEE5D50), btn = Color(0xFFE7EBEF), btnInk = Color(0xFF11161C), dark = true,
)

/** Colori delle sessioni: (nome, chiaro, scuro). */
val SessionColors = listOf(
    Triple("Blu", Color(0xFF2356C4), Color(0xFF5B8BF0)),
    Triple("Giallo", Color(0xFFD9A400), Color(0xFFF0C030)),
    Triple("Rosso", Color(0xFFCF3A2E), Color(0xFFEE5D50)),
    Triple("Verde", Color(0xFF2B9455), Color(0xFF46B874)),
    Triple("Grigio", Color(0xFFA3ADB7), Color(0xFF5D6873)),
    Triple("Viola", Color(0xFF7B4BC4), Color(0xFFA585F0)),
    Triple("Arancio", Color(0xFFD9691E), Color(0xFFF0954D)),
    Triple("Azzurro", Color(0xFF1F8A99), Color(0xFF3FB8C8)),
    Triple("Rosa", Color(0xFFC43F7E), Color(0xFFF07AAE)),
)

val LocalPalette = staticCompositionLocalOf { LightPalette }

val Pal: Palette
    @Composable @ReadOnlyComposable get() = LocalPalette.current

@Composable
@ReadOnlyComposable
fun sessionColor(index: Int): Color {
    val c = SessionColors[index.mod(SessionColors.size)]
    return if (LocalPalette.current.dark) c.third else c.second
}

/** Equivalente di "Barlow Condensed": il condensato di sistema. */
val Condensed = FontFamily(Typeface(android.graphics.Typeface.create("sans-serif-condensed", android.graphics.Typeface.NORMAL)))

fun cond(size: Int, weight: FontWeight = FontWeight.SemiBold, lineHeight: Double = 1.15) =
    TextStyle(fontFamily = Condensed, fontWeight = weight, fontSize = size.sp, lineHeight = (size * lineHeight).sp)

val Body = TextStyle(fontSize = 16.sp, lineHeight = 24.sp)
val Small = TextStyle(fontSize = 14.sp, lineHeight = 20.sp)
val Tiny = TextStyle(fontSize = 13.sp, lineHeight = 18.sp)

@Composable
fun ForgiaTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val p = if (dark) DarkPalette else LightPalette
    val scheme = if (dark) {
        darkColorScheme(
            primary = p.accent, onPrimary = p.btnInk, secondary = p.accent, background = p.bg, onBackground = p.ink,
            surface = p.surface, onSurface = p.ink, surfaceVariant = p.surface2, onSurfaceVariant = p.muted,
            outline = p.line, outlineVariant = p.line, error = p.bad,
            surfaceContainer = p.surface, surfaceContainerHigh = p.surface, surfaceContainerHighest = p.surface2,
            surfaceContainerLow = p.surface, surfaceContainerLowest = p.bg,
        )
    } else {
        lightColorScheme(
            primary = p.accent, onPrimary = Color.White, secondary = p.accent, background = p.bg, onBackground = p.ink,
            surface = p.surface, onSurface = p.ink, surfaceVariant = p.surface2, onSurfaceVariant = p.muted,
            outline = p.line, outlineVariant = p.line, error = p.bad,
            surfaceContainer = p.surface, surfaceContainerHigh = p.surface, surfaceContainerHighest = p.surface2,
            surfaceContainerLow = p.surface, surfaceContainerLowest = p.bg,
        )
    }
    CompositionLocalProvider(LocalPalette provides p) {
        MaterialTheme(colorScheme = scheme, content = content)
    }
}
