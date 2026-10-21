package net.kravos.bass.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.kravos.bass.music.Bass
import net.kravos.bass.music.Boards
import net.kravos.bass.music.Chord
import net.kravos.bass.music.ChordQuality
import net.kravos.bass.music.Interval
import net.kravos.bass.music.MarkerStyle
import net.kravos.bass.music.Spelled
import net.kravos.bass.ui.components.ChipGroup
import net.kravos.bass.ui.components.Fretboard
import net.kravos.bass.ui.components.Label
import net.kravos.bass.ui.components.LocalSettings
import net.kravos.bass.ui.components.RoleLegend
import net.kravos.bass.ui.components.SectionCard
import net.kravos.bass.ui.components.Sound

val CHORD_ROOT_CHOICES = listOf("C", "C#", "D", "Eb", "E", "F", "F#", "G", "Ab", "A", "Hb", "H").map { Spelled.parse(it) }

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun ChordsScreen(pad: PaddingValues) {
    val settings = LocalSettings.current
    val n = settings.notation
    val scope = rememberCoroutineScope()
    var rootIdx by rememberSaveable { mutableIntStateOf(9) } // A
    var qIdx by rememberSaveable { mutableIntStateOf(1) } // mol
    var labelNotes by rememberSaveable { mutableStateOf(false) }
    var focusFret by remember { mutableIntStateOf(-1) }
    val chord = Chord(CHORD_ROOT_CHOICES[rootIdx], ChordQuality.EXPLORER[qIdx])

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(top = pad.calculateTopPadding() + 8.dp, bottom = pad.calculateBottomPadding() + 16.dp, start = 16.dp, end = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SectionCard {
            Label("Koren")
            ChipGroup(CHORD_ROOT_CHOICES.indices.toList(), rootIdx, { CHORD_ROOT_CHOICES[it].name(n) }) { rootIdx = it; focusFret = -1 }
            Label("Vrsta")
            ChipGroup(ChordQuality.EXPLORER.indices.toList(), qIdx, { i -> ChordQuality.EXPLORER[i].let { if (it.suffix.isEmpty()) "dur" else it.suffix } }) { qIdx = it }
        }

        Row(verticalAlignment = androidx.compose.ui.Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(chord.name(n), style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
            Text(chord.quality.label, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 6.dp))
        }
        Text(
            chord.tones.joinToString("   ") { "${it.first.short} = ${it.second.name(n)}" },
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        Text("Formula: ${chord.quality.formula}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

        SingleChoiceSegmentedButtonRow {
            SegmentedButton(!labelNotes, { labelNotes = false }, SegmentedButtonDefaults.itemShape(0, 2)) { Text("Intervali") }
            SegmentedButton(labelNotes, { labelNotes = true }, SegmentedButtonDefaults.itemShape(1, 2)) { Text("Note") }
        }

        val all = Boards.chord(chord, n, settings.maxFret, labelNotes)
        val markers = if (focusFret < 0) all else all.map {
            if (kotlin.math.abs(it.fret - focusFret) <= 3 || (it.fret == 0 && focusFret <= 3)) it else it.copy(style = MarkerStyle.GHOST)
        }
        Fretboard(markers, frets = settings.maxFret, onTap = { s, f ->
            Sound.fret(s, f, settings)
            val iv = chord.intervalOfPc(Bass.pc(s, f))
            if (iv == Interval.R && s <= 1) focusFret = if (focusFret == f) -1 else f
        })
        RoleLegend(seventh = chord.quality.intervals.size == 4)
        Text(
            if (focusFret < 0) "Tapni koren (crveno) na E ili A žici da vidiš oblik oko te pozicije." else "Pozicija oko ${focusFret}. praga — tapni koren opet za ceo vrat.",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilledTonalButton(onClick = {
                scope.launch {
                    val base = Bass.midi(0, 0) + net.kravos.bass.music.mod12(chord.root.pc - 4)
                    val notes = chord.quality.intervals.map { base + it.semis } + (base + 12)
                    (notes + notes.reversed().drop(1)).forEach { Sound.midi(it, settings); delay(240) }
                }
            }) {
                Icon(Icons.Filled.PlayArrow, null, Modifier.size(18.dp)); Spacer(Modifier.size(6.dp)); Text("Arpeđo")
            }
            FilledTonalButton(onClick = {
                scope.launch {
                    val base = Bass.midi(0, 0) + net.kravos.bass.music.mod12(chord.root.pc - 4)
                    listOf(0, 7, 12, 7).forEach { Sound.midi(base + it, settings); delay(300) }
                }
            }) { Text("R – 5 – 8") }
        }

        SectionCard {
            Text("Kako ovo koristiš", style = MaterialTheme.typography.titleMedium)
            Text(usage(chord.quality), style = MaterialTheme.typography.bodyMedium)
        }
        Spacer(Modifier.height(8.dp))
    }
}

private fun usage(q: ChordQuality): String = when (q) {
    ChordQuality.MAJ -> "Dur: velika terca (žica gore, prag levo od korena) je ono što ga čini „veselim“. Na jaku dobu: R, 3 ili 5. Za prelaz do sledećeg akorda koristi note iz lestvice ili hromatski prilaz."
    ChordQuality.MIN -> "Mol: mala terca (žica gore, dva praga levo). Dodaj b7 (dve žice gore, isti prag) i 4 — dobiješ mol pentatoniku iz istog oblika."
    ChordQuality.POWER -> "Power akord nema tercu — zato ti je sve dozvoljeno između korena, kvinte i oktave, a b3 i b7 iz mol pentatonike skoro uvek rade preko metal riffova."
    ChordQuality.DOM7 -> "Dominantni 7: blues/rokenrol. R – 3 – 5 – 6 – b7 je klasičan boogie obrazac. b7 jako „vuče“ ka sledećem akordu (kvartu gore)."
    ChordQuality.MIN7 -> "m7: mekši mol. R – b3 – 5 – b7 arpeđo je osnova funk i disko linija; oktava sa b7 ispod je super fill."
    ChordQuality.MAJ7 -> "maj7: velika septima je pola stepena ispod oktave — lepa ali osetljiva nota. Kao sletište koristi R, 3, 5."
    ChordQuality.HALFDIM -> "Polusmanjeni (vii u duru, ii u molu): b5 je karakteristična nota. Obično traje kratko i vodi ka V ili i."
    ChordQuality.DIM -> "Smanjeni: sve male terce. Prelazni akord — sviraj koren i hromatski idi ka sledećem."
    ChordQuality.AUG -> "Povećani: #5 vuče pola stepena gore. Retko u roku, više kao prelaz."
    ChordQuality.DIM7 -> "Smanjeni septakord: simetričan, svaka nota može biti koren. Prelaz/napetost."
}
