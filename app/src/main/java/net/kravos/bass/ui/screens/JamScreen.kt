package net.kravos.bass.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Icon
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import net.kravos.bass.audio.AudioEngine
import net.kravos.bass.audio.JamPosition
import net.kravos.bass.audio.JamSequencer
import net.kravos.bass.music.Boards
import net.kravos.bass.music.Key
import net.kravos.bass.music.Mode
import net.kravos.bass.music.Progression
import net.kravos.bass.music.Progressions
import net.kravos.bass.music.SafeScale
import net.kravos.bass.music.Spelled
import net.kravos.bass.ui.components.ChipGroup
import net.kravos.bass.ui.components.Fretboard
import net.kravos.bass.ui.components.Label
import net.kravos.bass.ui.components.LocalSettings
import net.kravos.bass.ui.components.RoleLegend
import net.kravos.bass.ui.components.SectionCard
import net.kravos.bass.ui.components.Sound

enum class JamView(val label: String) { CHORD("Akord"), PENTA("+ pentatonika"), SCALE("+ lestvica") }

/** Jam state lives outside the screen so it keeps playing while you look at other tabs. */
object Jam {
    var key by mutableStateOf(Key(Spelled.parse("E"), Mode.MINOR))
    var progression by mutableStateOf(Progressions.ALL.first())
    var bpm by mutableIntStateOf(90)
    var mult by mutableIntStateOf(1)
    var drums by mutableStateOf(true)
    var pad by mutableStateOf(true)
    var view by mutableStateOf(JamView.SCALE)
    var playing by mutableStateOf(false)
        private set
    private var seq: JamSequencer? = null

    fun sequencer() = seq

    fun start() {
        val p = progression
        val s = JamSequencer(bpm, p.steps.map { it.chord(key) }, p.steps.map { it.bars * 4 * mult }, drums, pad)
        seq = s
        AudioEngine.listener = s
        playing = true
    }

    fun stop() {
        AudioEngine.listener = null
        seq = null
        playing = false
    }

    fun restartIfPlaying() {
        if (playing) { stop(); start() }
    }

    fun load(k: Key, progName: String?) {
        val p = progName?.let { name -> Progressions.ALL.firstOrNull { it.name == name } }
        if (p != null) {
            progression = p
            key = if (k.mode == p.mode) k else k.relative.takeIf { it.mode == p.mode } ?: Key(Spelled.parse("E"), p.mode)
        } else {
            key = k
            if (progression.mode != k.mode) progression = Progressions.forMode(k.mode).first()
        }
        restartIfPlaying()
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun JamScreen(pad: PaddingValues) {
    val settings = LocalSettings.current
    val n = settings.notation
    var pos by androidx.compose.runtime.remember { mutableStateOf<JamPosition?>(null) }

    LaunchedEffect(Jam.playing) {
        if (!Jam.playing) { pos = null; return@LaunchedEffect }
        while (true) {
            withFrameMillis { }
            val s = Jam.sequencer() ?: break
            pos = s.positionAt(AudioEngine.playedFrame())
        }
    }

    val p = Jam.progression
    val key = Jam.key
    val chords = p.steps.map { it.chord(key) }
    val ci = pos?.takeIf { !it.countIn }?.chordIndex ?: 0
    val cur = chords[ci]
    val next = chords[(ci + 1) % chords.size]

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(top = pad.calculateTopPadding() + 8.dp, bottom = pad.calculateBottomPadding() + 16.dp, start = 16.dp, end = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // ---- now playing ----
        SectionCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        if (pos?.countIn == true) "Odbrojavanje… ${(pos?.beat ?: 0) + 1}" else "${p.steps[ci].roman}  ·  ${key.name(n)}",
                        style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary,
                    )
                    Text(cur.name(n), style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Bold)
                    Text(cur.tones.joinToString("  ") { "${it.first.short} ${it.second.name(n)}" }, style = MaterialTheme.typography.titleSmall)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("sledeći", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(next.name(n), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                }
            }
            BeatDots(pos)
        }

        val bg = when (Jam.view) {
            JamView.CHORD -> emptyList()
            JamView.PENTA -> when (p.safeScale) {
                SafeScale.BLUES -> key.blues
                else -> key.pentatonic
            }
            JamView.SCALE -> if (p.safeScale == SafeScale.BLUES) key.blues else Boards.keyScale(key)
        }
        val markers = if (bg.isEmpty()) Boards.chord(cur, n, settings.maxFret) else Boards.chordOver(cur, bg, n, settings.maxFret, false)
        Fretboard(markers, frets = settings.maxFret, onTap = { s, f -> Sound.fret(s, f, settings) })
        RoleLegend(seventh = cur.quality.intervals.size == 4, extra = if (bg.isEmpty()) null else "prolazne")
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            JamView.entries.forEachIndexed { i, v ->
                SegmentedButton(Jam.view == v, { Jam.view = v }, SegmentedButtonDefaults.itemShape(i, JamView.entries.size)) { Text(v.label, maxLines = 1) }
            }
        }

        Button(
            onClick = { if (Jam.playing) Jam.stop() else Jam.start() },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = if (Jam.playing) ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error) else ButtonDefaults.buttonColors(),
        ) {
            Icon(if (Jam.playing) Icons.Filled.Stop else Icons.Filled.PlayArrow, null)
            Spacer(Modifier.size(8.dp))
            Text(if (Jam.playing) "Stop" else "Sviraj", style = MaterialTheme.typography.titleMedium)
        }

        // ---- setup ----
        SectionCard {
            Text(p.name, style = MaterialTheme.typography.titleMedium)
            Text(p.steps.joinToString(" – ") { it.chord(key).name(n) } + "    (" + p.romanLine + ")", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
            Text(p.note, style = MaterialTheme.typography.bodyMedium)
            Text("Preko cele progresije radi: ${key.tonic.name(n)} ${safeLabel(p, key)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        SectionCard {
            Label("Tonalitet")
            SingleChoiceSegmentedButtonRow {
                Mode.entries.forEachIndexed { i, m ->
                    SegmentedButton(key.mode == m, {
                        if (key.mode != m) {
                            Jam.key = Key(key.tonic, m)
                            Jam.progression = Progressions.forMode(m).first()
                            Jam.restartIfPlaying()
                        }
                    }, SegmentedButtonDefaults.itemShape(i, 2)) { Text(m.label) }
                }
            }
            ChipGroup(Key.all(key.mode), Key.all(key.mode).firstOrNull { it.tonic.pc == key.tonic.pc }, { it.tonic.name(n) }) {
                Jam.key = it; Jam.restartIfPlaying()
            }
            Label("Progresija")
            ChipGroup(Progressions.forMode(key.mode), p, { it.name }) { Jam.progression = it; Jam.restartIfPlaying() }
        }

        SectionCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Tempo", Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
                Text("${Jam.bpm} BPM", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            }
            Slider(
                value = Jam.bpm.toFloat(),
                onValueChange = { Jam.bpm = (it / 5).toInt() * 5 },
                onValueChangeFinished = { Jam.restartIfPlaying() },
                valueRange = 50f..180f,
            )
            Label("Dužina akorda")
            ChipGroup(listOf(1, 2), Jam.mult, { if (it == 1) "kao u progresiji" else "duplo duže" }) { Jam.mult = it; Jam.restartIfPlaying() }
            ToggleRow("Bubanj", Jam.drums) { Jam.drums = it; Jam.restartIfPlaying() }
            ToggleRow("Akordi (klavijature)", Jam.pad) { Jam.pad = it; Jam.restartIfPlaying() }
            Text(
                "Pratnja nema bas — to si ti. Na telefonu stavi slušalice ili zvučnik, pa sviraj preko svog pojačala.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun safeLabel(p: Progression, k: Key): String = when (p.safeScale) {
    SafeScale.PENTA -> "${k.mode.label} pentatonika"
    SafeScale.SCALE -> "${k.mode.label} lestvica (ali prati akorde!)"
    SafeScale.BLUES -> "blues lestvica"
}

@Composable
private fun ToggleRow(label: String, value: Boolean, onChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        Switch(value, onChange)
    }
}

@Composable
private fun BeatDots(pos: JamPosition?) {
    val total = pos?.beatsInChord ?: 4
    val shown = minOf(total, 16)
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        for (i in 0 until shown) {
            val active = pos != null && pos.beat == i
            val barStart = i % 4 == 0
            Box(
                Modifier.size(if (active) 14.dp else 10.dp).background(
                    when {
                        active && pos?.countIn == true -> MaterialTheme.colorScheme.tertiary
                        active -> MaterialTheme.colorScheme.primary
                        barStart -> MaterialTheme.colorScheme.outline
                        else -> MaterialTheme.colorScheme.surfaceContainerHighest
                    },
                    CircleShape,
                ),
            )
        }
    }
}
