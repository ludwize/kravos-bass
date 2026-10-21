package net.kravos.bass.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import net.kravos.bass.music.Boards
import net.kravos.bass.music.Key
import net.kravos.bass.music.Mode
import net.kravos.bass.music.Progressions
import net.kravos.bass.ui.components.ChipGroup
import net.kravos.bass.ui.components.Fretboard
import net.kravos.bass.ui.components.Label
import net.kravos.bass.ui.components.LocalSettings
import net.kravos.bass.ui.components.RoleLegend
import net.kravos.bass.ui.components.SectionCard
import net.kravos.bass.ui.components.Sound

private enum class KeyView(val label: String) { SCALE("Lestvica"), PENTA("Pentatonika"), BLUES("Blues") }

@OptIn(ExperimentalLayoutApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun KeyScreen(pad: PaddingValues, onJam: (Key, String) -> Unit) {
    val settings = LocalSettings.current
    val n = settings.notation
    var minor by rememberSaveable { mutableStateOf(true) }
    var tonicIdx by rememberSaveable { mutableIntStateOf(1) } // E-mol / G-dur
    var degree by rememberSaveable { mutableIntStateOf(-1) }
    var sevenths by rememberSaveable { mutableStateOf(false) }
    var viewIdx by rememberSaveable { mutableIntStateOf(0) }
    var labelNotes by rememberSaveable { mutableStateOf(false) }
    val mode = if (minor) Mode.MINOR else Mode.MAJOR
    val keys = Key.all(mode)
    val key = keys[tonicIdx.coerceIn(keys.indices)]
    val dia = key.diatonic(sevenths)
    val view = KeyView.entries[viewIdx]

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(top = pad.calculateTopPadding() + 8.dp, bottom = pad.calculateBottomPadding() + 16.dp, start = 16.dp, end = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SectionCard {
            SingleChoiceSegmentedButtonRow {
                SegmentedButton(!minor, {
                    if (minor) { // jump to the relative major so you keep "the same notes"
                        val rel = key.relative; minor = false; tonicIdx = Key.all(Mode.MAJOR).indexOfFirst { it.tonic.pc == rel.tonic.pc }.coerceAtLeast(0); degree = -1
                    }
                }, SegmentedButtonDefaults.itemShape(0, 2)) { Text("dur") }
                SegmentedButton(minor, {
                    if (!minor) {
                        val rel = key.relative; minor = true; tonicIdx = Key.all(Mode.MINOR).indexOfFirst { it.tonic.pc == rel.tonic.pc }.coerceAtLeast(0); degree = -1
                    }
                }, SegmentedButtonDefaults.itemShape(1, 2)) { Text("mol") }
            }
            ChipGroup(keys.indices.toList(), tonicIdx, { keys[it].tonic.name(n) }) { tonicIdx = it; degree = -1 }
        }

        Text(key.name(n), style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
        Text(
            "Relativni ${key.relative.mode.label}: ${key.relative.name(n)} — iste note, druga „kuća“.",
            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        // scale row
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
            key.scale.zip(key.mode.pattern).forEach { (note, iv) ->
                Surface(Modifier.weight(1f), shape = RoundedCornerShape(10.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh) {
                    Column(Modifier.padding(vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(note.name(n), style = MaterialTheme.typography.titleMedium)
                        Text(iv.short.replace("R", "1"), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        SectionCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Akordi u tonalitetu", Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                Text("septakordi", style = MaterialTheme.typography.labelMedium)
                Switch(sevenths, { sevenths = it }, Modifier.padding(start = 6.dp))
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                dia.forEach { d ->
                    val sel = d.degree == degree
                    Surface(
                        Modifier.width(76.dp).clickable { degree = if (sel) -1 else d.degree },
                        shape = RoundedCornerShape(12.dp),
                        color = if (sel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest,
                    ) {
                        Column(Modifier.padding(vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(d.roman, style = MaterialTheme.typography.labelMedium, color = if (sel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary)
                            Text(d.chord.name(n), style = MaterialTheme.typography.titleMedium, color = if (sel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface, textAlign = TextAlign.Center)
                        }
                    }
                }
            }
            Text(
                if (degree < 0) "Tapni akord da vidiš njegove tonove preko lestvice." else
                    "${dia[degree].chord.name(n)}: " + dia[degree].chord.tones.joinToString("  ") { "${it.first.short} ${it.second.name(n)}" },
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            KeyView.entries.forEachIndexed { i, v ->
                SegmentedButton(viewIdx == i, { viewIdx = i }, SegmentedButtonDefaults.itemShape(i, KeyView.entries.size)) { Text(v.label) }
            }
        }
        val bg = when (view) {
            KeyView.SCALE -> Boards.keyScale(key)
            KeyView.PENTA -> key.pentatonic
            KeyView.BLUES -> key.blues
        }
        val markers = if (degree >= 0) Boards.chordOver(dia[degree].chord, bg, n, settings.maxFret, labelNotes)
        else Boards.scale(bg, n, settings.maxFret, labelNotes)
        Fretboard(markers, frets = settings.maxFret, onTap = { s, f -> Sound.fret(s, f, settings) })
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                if (degree >= 0) RoleLegend(seventh = sevenths, extra = "ostalo") else Text(
                    "Crveno = tonika (${key.tonic.name(n)}). Brojevi = stepen od tonike.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text("note", style = MaterialTheme.typography.labelMedium)
            Switch(labelNotes, { labelNotes = it }, Modifier.padding(start = 6.dp))
        }

        Label("Tipične progresije u ${key.name(n)}", Modifier.padding(top = 8.dp))
        Progressions.forMode(mode).forEach { p ->
            Surface(
                Modifier.fillMaxWidth().clickable { onJam(key, p.name) },
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
            ) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(p.name, style = MaterialTheme.typography.titleSmall)
                        Text(p.steps.joinToString(" – ") { it.chord(key).name(n) }, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                        Text(p.romanLine, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Icon(Icons.Filled.MusicNote, "Džem", tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}
