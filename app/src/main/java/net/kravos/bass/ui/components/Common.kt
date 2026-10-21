package net.kravos.bass.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import net.kravos.bass.audio.AudioEngine
import net.kravos.bass.data.Prefs
import net.kravos.bass.data.Settings
import net.kravos.bass.music.Bass
import net.kravos.bass.ui.theme.RoleColors

val LocalPrefs = compositionLocalOf<Prefs> { error("no prefs") }
val LocalSettings = compositionLocalOf { Settings() }

object Sound {
    fun fret(s: Int, f: Int, st: Settings) = midi(Bass.midi(s, f), st)
    fun midi(m: Int, st: Settings) {
        if (!st.sound) return
        AudioEngine.pluck(m + if (st.octaveUp) 12 else 0)
    }
}

fun rich(s: String, bold: Color? = null): AnnotatedString = buildAnnotatedString {
    var i = 0
    var isBold = false
    while (i < s.length) {
        val j = s.indexOf("**", i)
        val end = if (j < 0) s.length else j
        val chunk = s.substring(i, end)
        if (isBold) withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = bold ?: Color.Unspecified)) { append(chunk) } else append(chunk)
        if (j < 0) break
        isBold = !isBold
        i = j + 2
    }
}

@Composable
fun RichText(text: String, modifier: Modifier = Modifier) {
    val accent = MaterialTheme.colorScheme.primary
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        text.split("\n").forEach { line ->
            when {
                line.isBlank() -> Spacer(Modifier.size(2.dp))
                line.startsWith("• ") -> Row {
                    Text("•", color = accent, modifier = Modifier.width(16.dp), style = MaterialTheme.typography.bodyLarge)
                    Text(rich(line.removePrefix("• "), accent), style = MaterialTheme.typography.bodyLarge)
                }
                else -> Text(rich(line, accent), style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}

@Composable
fun SectionCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Surface(
        modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) { content() }
    }
}

@Composable
fun Label(text: String, modifier: Modifier = Modifier) {
    Text(text.uppercase(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = modifier)
}

@Composable
fun RoleLegend(seventh: Boolean = true, extra: String? = null) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        LegendDot(RoleColors.root, "R")
        LegendDot(RoleColors.third, "terca")
        LegendDot(RoleColors.fifth, "kvinta")
        if (seventh) LegendDot(RoleColors.seventh, "septima")
        if (extra != null) LegendDot(RoleColors.other, extra)
    }
}

@Composable
private fun LegendDot(c: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).background(c, CircleShape))
        Spacer(Modifier.width(4.dp))
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@OptIn(ExperimentalLayoutApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun <T> ChipGroup(items: List<T>, selected: T?, label: (T) -> String, onSelect: (T) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(0.dp)) {
        items.forEach { item ->
            FilterChip(selected = item == selected, onClick = { onSelect(item) }, label = { Text(label(item)) })
        }
    }
}
