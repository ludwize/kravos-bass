package net.kravos.bass.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import net.kravos.bass.music.Role

val Amber = Color(0xFFFFB020)

private val Dark = darkColorScheme(
    primary = Amber,
    onPrimary = Color(0xFF2A1800),
    primaryContainer = Color(0xFF4A3000),
    onPrimaryContainer = Color(0xFFFFDDB0),
    secondary = Color(0xFFD8C4A8),
    secondaryContainer = Color(0xFF3A3024),
    onSecondaryContainer = Color(0xFFF3E0C6),
    tertiary = Color(0xFF7FC8FF),
    background = Color(0xFF111214),
    onBackground = Color(0xFFE6E3DF),
    surface = Color(0xFF141518),
    onSurface = Color(0xFFE6E3DF),
    surfaceVariant = Color(0xFF26282C),
    onSurfaceVariant = Color(0xFFBDB8B1),
    surfaceContainerLowest = Color(0xFF0C0D0F),
    surfaceContainerLow = Color(0xFF18191C),
    surfaceContainer = Color(0xFF1C1E21),
    surfaceContainerHigh = Color(0xFF24262A),
    surfaceContainerHighest = Color(0xFF2D3034),
    outline = Color(0xFF85807A),
    outlineVariant = Color(0xFF3A3C40),
)

private val Light = lightColorScheme(
    primary = Color(0xFF8A5300),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDDB0),
    onPrimaryContainer = Color(0xFF2C1700),
    secondary = Color(0xFF6E5B40),
    secondaryContainer = Color(0xFFF8DFBC),
    tertiary = Color(0xFF1565C0),
    background = Color(0xFFFAF7F3),
    surface = Color(0xFFFCF9F5),
    surfaceContainerLow = Color(0xFFF4F0EA),
    surfaceContainer = Color(0xFFEEEAE3),
    surfaceContainerHigh = Color(0xFFE8E3DC),
    surfaceContainerHighest = Color(0xFFE2DDD5),
)

private val Type = Typography().let { t ->
    t.copy(
        titleMedium = t.titleMedium.copy(fontWeight = FontWeight.SemiBold),
        titleLarge = t.titleLarge.copy(fontWeight = FontWeight.SemiBold),
        headlineMedium = t.headlineMedium.copy(fontWeight = FontWeight.Bold),
    )
}

object RoleColors {
    val root = Color(0xFFFF5A4E)
    val third = Color(0xFF4FA3F7)
    val fifth = Color(0xFF34C77B)
    val seventh = Color(0xFFB07CFF)
    val other = Color(0xFF8E96A3)
    val question = Amber

    fun of(r: Role?): Color = when (r) {
        Role.ROOT -> root
        Role.THIRD -> third
        Role.FIFTH -> fifth
        Role.SEVENTH -> seventh
        Role.OTHER, null -> other
    }
}

@Composable
fun BassTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (isSystemInDarkTheme()) Dark else Light, typography = Type, content = content)
}
