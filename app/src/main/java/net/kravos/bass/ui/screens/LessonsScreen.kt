package net.kravos.bass.ui.screens

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import net.kravos.bass.music.Block
import net.kravos.bass.music.Lesson
import net.kravos.bass.music.Lessons
import net.kravos.bass.ui.components.Fretboard
import net.kravos.bass.ui.components.Label
import net.kravos.bass.ui.components.LocalPrefs
import net.kravos.bass.ui.components.LocalSettings
import net.kravos.bass.ui.components.RichText
import net.kravos.bass.ui.components.RoleLegend
import net.kravos.bass.ui.components.SectionCard
import net.kravos.bass.ui.components.Sound

@Composable
fun LessonsScreen(pad: PaddingValues, onOpen: (Lesson) -> Unit) {
    val progress by LocalPrefs.current.progress.collectAsState()
    val done = progress.doneLessons
    val nextId = Lessons.ALL.firstOrNull { it.id !in done }?.id
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = pad.calculateTopPadding() + 8.dp, bottom = pad.calculateBottomPadding() + 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            SectionCard {
                val n = Lessons.ALL.count { it.id in done }
                Text("Put: teorija → improvizacija", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Kratke lekcije, svaka sa proverom. Redom: vrat, intervali, akordi, tonalitet, pa improvizacija. Lekcija je savladana kad na proveri imaš 75%+.",
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                LinearProgressIndicator(progress = { n / Lessons.ALL.size.toFloat() }, modifier = Modifier.fillMaxWidth())
                Text("$n od ${Lessons.ALL.size} savladano", style = MaterialTheme.typography.labelMedium)
            }
        }
        val units = Lessons.ALL.groupBy { it.unit }
        units.forEach { (unit, lessons) ->
            item { Label(unit, Modifier.padding(top = 10.dp, start = 4.dp)) }
            items(lessons, key = { it.id }) { l ->
                LessonRow(l, l.id in done, l.id == nextId) { onOpen(l) }
            }
        }
    }
}

@Composable
private fun LessonRow(l: Lesson, done: Boolean, next: Boolean, onClick: () -> Unit) {
    Surface(
        Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = if (next) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                Modifier.size(32.dp).then(Modifier),
                contentAlignment = Alignment.Center,
            ) {
                Surface(shape = CircleShape, color = if (done) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest, modifier = Modifier.size(32.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        if (done) Icon(Icons.Filled.Check, null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(18.dp))
                        else Text("${Lessons.ALL.indexOf(l) + 1}", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
            Column(Modifier.weight(1f)) {
                Text(l.title, style = MaterialTheme.typography.titleMedium)
                Text(l.goal, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LessonDetail(
    lesson: Lesson,
    onBack: () -> Unit,
    onQuiz: () -> Unit,
    onJam: (String) -> Unit,
) {
    val settings = LocalSettings.current
    val done = LocalPrefs.current.progress.collectAsState().value.doneLessons.contains(lesson.id)
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(lesson.unit, style = MaterialTheme.typography.titleMedium) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Nazad") } },
            )
        },
    ) { pad ->
        Column(
            Modifier.padding(pad).fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(lesson.title, style = MaterialTheme.typography.headlineMedium)
            Text(lesson.goal, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.primary)
            var legendShown = false
            lesson.blocks.forEach { b ->
                when (b) {
                    is Block.Text -> RichText(b.text)
                    is Block.Tip -> Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.secondaryContainer) {
                        Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Icon(Icons.Filled.Lightbulb, null, tint = MaterialTheme.colorScheme.primary)
                            RichText(b.text)
                        }
                    }
                    is Block.Board -> Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(b.caption, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        Fretboard(b.build(settings.notation), frets = b.frets, onTap = { s, f -> Sound.fret(s, f, settings) })
                        if (!legendShown) {
                            legendShown = true
                            RoleLegend(extra = "ostalo")
                        }
                    }
                }
            }
            Spacer(Modifier.height(4.dp))
            Button(onClick = onQuiz, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                Text(if (done) "Ponovi proveru" else "Proveri se (8 pitanja)")
            }
            lesson.jam?.let { j ->
                OutlinedButton(onClick = { onJam(j) }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Filled.MusicNote, null)
                    Spacer(Modifier.size(8.dp))
                    Text("Probaj u Džemu: $j")
                }
            }
            if (done) Text("✓ Savladano", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(32.dp))
        }
    }
}
