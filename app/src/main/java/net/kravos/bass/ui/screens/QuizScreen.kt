package net.kravos.bass.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.kravos.bass.music.Answer
import net.kravos.bass.music.Question
import net.kravos.bass.music.QuizCtx
import net.kravos.bass.music.QuizGen
import net.kravos.bass.music.QuizKind
import net.kravos.bass.ui.components.Fretboard
import net.kravos.bass.ui.components.LocalPrefs
import net.kravos.bass.ui.components.LocalSettings
import net.kravos.bass.ui.components.SectionCard
import net.kravos.bass.ui.components.Sound
import net.kravos.bass.ui.components.TapMark
import kotlin.random.Random

private val Good = Color(0xFF34C77B)
private val Bad = Color(0xFFFF5A4E)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizScreen(
    title: String,
    kinds: List<QuizKind>,
    ctx: QuizCtx,
    count: Int,
    passPct: Int? = null,
    onFinished: (right: Int, total: Int) -> Unit,
    onClose: () -> Unit,
) {
    val prefs = LocalPrefs.current
    val settings = LocalSettings.current
    val scope = rememberCoroutineScope()
    var round by remember { mutableIntStateOf(0) }
    var index by remember { mutableIntStateOf(0) }
    var right by remember { mutableIntStateOf(0) }
    var streak by remember { mutableIntStateOf(0) }
    var answered by remember { mutableStateOf(false) }
    var lastOk by remember { mutableStateOf(false) }
    var chosen by remember { mutableIntStateOf(-1) }
    var taps by remember { mutableStateOf(listOf<TapMark>()) }
    var done by remember { mutableStateOf(false) }
    val rng = remember(round) { Random(System.nanoTime()) }
    val question: Question = remember(round, index, ctx) { QuizGen.make(kinds.random(rng), rng, ctx) }

    fun playSeq(notes: List<Int>) {
        scope.launch {
            notes.forEach {
                Sound.midi(it, settings)
                delay(260)
            }
        }
    }

    fun resolve(ok: Boolean) {
        answered = true
        lastOk = ok
        if (ok) {
            right++
            streak++
            prefs.streak(streak)
        } else streak = 0
        prefs.record(question.kind.name, ok)
    }

    fun next() {
        if (index + 1 >= count) {
            done = true
            onFinished(right, count)
        } else {
            index++
            answered = false
            chosen = -1
            taps = emptyList()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title, maxLines = 1) },
                navigationIcon = { IconButton(onClick = onClose) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Nazad") } },
                actions = {
                    if (!done) Text("${index + 1}/$count", modifier = Modifier.padding(end = 16.dp), style = MaterialTheme.typography.titleMedium)
                },
            )
        },
    ) { pad ->
        if (done) {
            val pct = right * 100 / count
            Column(
                Modifier.padding(pad).fillMaxSize().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text("$right / $count", style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Bold)
                Text("$pct%", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(12.dp))
                val msg = when {
                    passPct != null && pct >= passPct -> "Lekcija savladana ✓"
                    passPct != null -> "Za prolaz treba $passPct%. Pročitaj još jednom pa probaj ponovo."
                    pct >= 90 -> "Odlično — pojačaj težinu (više pragova, sve note)."
                    pct >= 70 -> "Dobro. Još par krugova i postaje refleks."
                    else -> "Polako — bolje 5 minuta svaki dan nego sat jednom nedeljno."
                }
                Text(msg, textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.height(24.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = {
                        round++; index = 0; right = 0; streak = 0; answered = false; chosen = -1; taps = emptyList(); done = false
                    }) { Text("Ponovi") }
                    Button(onClick = onClose) { Text("Gotovo") }
                }
            }
            return@Scaffold
        }

        Column(
            Modifier.padding(pad).fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            LinearProgressIndicator(progress = { (index + if (answered) 1 else 0) / count.toFloat() }, modifier = Modifier.fillMaxWidth())
            Text(question.kind.title.uppercase(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            Text(question.prompt, style = MaterialTheme.typography.titleLarge)

            val ans = question.answer
            val showBoard = question.markers.isNotEmpty() || ans is Answer.Tap
            if (showBoard) {
                val markers = if (answered && ans is Answer.Tap) question.markers + ans.solution else question.markers
                Fretboard(
                    markers = markers,
                    frets = maxOf(ctx.maxFret, (question.markers.maxOfOrNull { it.fret } ?: 0)),
                    taps = taps,
                    onTap = if (ans is Answer.Tap) ({ s: Int, f: Int ->
                        if (!answered) {
                            val ok = ans.check(s, f)
                            taps = listOf(TapMark(s, f, ok))
                            Sound.fret(s, f, settings)
                            resolve(ok)
                        } else {
                            Sound.fret(s, f, settings)
                        }
                    }) else null,
                )
                if (ans is Answer.Tap && !answered) {
                    Text("Tapni na vrat.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            if (ans is Answer.Choice) {
                val cols = ans.columns
                ans.options.chunked(cols).forEachIndexed { rowIdx, row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        row.forEachIndexed { colIdx, opt ->
                            val i = rowIdx * cols + colIdx
                            val isCorrect = i == ans.correct
                            val colors = when {
                                answered && isCorrect -> ButtonDefaults.buttonColors(containerColor = Good, contentColor = Color.White)
                                answered && i == chosen -> ButtonDefaults.buttonColors(containerColor = Bad, contentColor = Color.White)
                                else -> ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                                    contentColor = MaterialTheme.colorScheme.onSurface,
                                )
                            }
                            Button(
                                onClick = {
                                    if (!answered) {
                                        chosen = i
                                        resolve(isCorrect)
                                        if (question.sound.isNotEmpty()) playSeq(question.sound)
                                    }
                                },
                                colors = colors,
                                shape = MaterialTheme.shapes.medium,
                                contentPadding = ButtonDefaults.ContentPadding,
                                modifier = Modifier.weight(1f).height(if (cols == 1) 52.dp else 50.dp),
                            ) { Text(opt, maxLines = if (cols == 1) 2 else 1, softWrap = cols == 1, textAlign = TextAlign.Center) }
                        }
                        repeat(cols - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }

            AnimatedVisibility(answered) {
                SectionCard {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(if (lastOk) Icons.Filled.CheckCircle else Icons.Filled.Cancel, null, tint = if (lastOk) Good else Bad)
                        Text(if (lastOk) (if (streak >= 3) "Tačno — niz $streak!" else "Tačno!") else "Nije.", style = MaterialTheme.typography.titleMedium)
                    }
                    Text(question.explain, style = MaterialTheme.typography.bodyLarge)
                    Button(onClick = { next() }, modifier = Modifier.fillMaxWidth()) {
                        Text(if (index + 1 >= count) "Rezultat" else "Dalje")
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}
