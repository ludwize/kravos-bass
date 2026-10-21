package net.kravos.bass.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import net.kravos.bass.audio.AudioEngine
import net.kravos.bass.data.Prefs
import net.kravos.bass.data.Settings
import net.kravos.bass.music.Lesson
import net.kravos.bass.music.Lessons
import net.kravos.bass.music.Notation
import net.kravos.bass.music.QuizCtx
import net.kravos.bass.ui.components.ChipGroup
import net.kravos.bass.ui.components.Label
import net.kravos.bass.ui.components.LocalPrefs
import net.kravos.bass.ui.components.LocalSettings
import net.kravos.bass.ui.screens.ChordsScreen
import net.kravos.bass.ui.screens.Jam
import net.kravos.bass.ui.screens.JamScreen
import net.kravos.bass.ui.screens.KeyScreen
import net.kravos.bass.ui.screens.LessonDetail
import net.kravos.bass.ui.screens.LessonsScreen
import net.kravos.bass.ui.screens.PracticeRequest
import net.kravos.bass.ui.screens.PracticeScreen
import net.kravos.bass.ui.screens.QuizScreen
import net.kravos.bass.ui.theme.BassTheme

private data class Tab(val title: String, val icon: ImageVector)

private val TABS = listOf(
    Tab("Lekcije", Icons.AutoMirrored.Filled.MenuBook),
    Tab("Vežbe", Icons.Filled.Quiz),
    Tab("Akordi", Icons.Filled.Tune),
    Tab("Tonalitet", Icons.Filled.Key),
    Tab("Džem", Icons.Filled.GraphicEq),
)

private sealed class Overlay {
    data class LessonView(val id: String) : Overlay()
    data class Quiz(val req: PracticeRequest, val lessonId: String? = null) : Overlay()
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val prefs = Prefs.get(this)
        setContent {
            val settings by prefs.settings.collectAsState()
            CompositionLocalProvider(LocalPrefs provides prefs, LocalSettings provides settings) {
                BassTheme { App(prefs, settings) }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        AudioEngine.start()
    }

    override fun onStop() {
        super.onStop()
        Jam.stop()
        AudioEngine.stop()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun App(prefs: Prefs, settings: Settings) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var stack by remember { mutableStateOf(listOf<Overlay>()) }
    var showSettings by remember { mutableStateOf(false) }

    fun push(o: Overlay) { stack = stack + o }
    fun pop() { stack = stack.dropLast(1) }
    fun openJam(progName: String?, key: net.kravos.bass.music.Key? = null) {
        Jam.load(key ?: Jam.key, progName)
        stack = emptyList()
        tab = 4
    }

    BackHandler(enabled = stack.isNotEmpty()) { pop() }
    BackHandler(enabled = stack.isEmpty() && tab != 0) { tab = 0 }

    when (val top = stack.lastOrNull()) {
        is Overlay.LessonView -> {
            val l: Lesson = Lessons.byId(top.id) ?: return
            LessonDetail(
                l,
                onBack = { pop() },
                onQuiz = {
                    push(
                        Overlay.Quiz(
                            PracticeRequest(l.title, l.quiz, QuizCtx(settings.notation, l.quizMaxFret, l.naturalsOnly, l.quizStrings), 8),
                            l.id,
                        ),
                    )
                },
                onJam = { openJam(it) },
            )
        }
        is Overlay.Quiz -> QuizScreen(
            title = top.req.title,
            kinds = top.req.kinds,
            ctx = top.req.ctx,
            count = top.req.count,
            passPct = if (top.lessonId != null) 75 else null,
            onFinished = { right, total ->
                if (top.lessonId != null && right * 100 / total >= 75) prefs.markLesson(top.lessonId)
            },
            onClose = { pop() },
        )
        null -> Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(if (tab == 0) "Kravos Bas" else TABS[tab].title) },
                    actions = { IconButton(onClick = { showSettings = true }) { Icon(Icons.Filled.Settings, "Podešavanja") } },
                )
            },
            bottomBar = {
                NavigationBar {
                    TABS.forEachIndexed { i, t ->
                        NavigationBarItem(
                            selected = tab == i,
                            onClick = { tab = i },
                            icon = { Icon(t.icon, null) },
                            label = { Text(t.title, maxLines = 1, style = MaterialTheme.typography.labelSmall) },
                        )
                    }
                }
            },
        ) { pad ->
            when (tab) {
                0 -> LessonsScreen(pad) { push(Overlay.LessonView(it.id)) }
                1 -> PracticeScreen(pad) { push(Overlay.Quiz(it)) }
                2 -> ChordsScreen(pad)
                3 -> KeyScreen(pad) { k, p -> openJam(p, k) }
                else -> JamScreen(pad)
            }
        }
    }

    if (showSettings) SettingsDialog(prefs, settings) { showSettings = false }
}

@Composable
private fun SettingsDialog(prefs: Prefs, s: Settings, onDismiss: () -> Unit) {
    var confirmReset by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("Gotovo") } },
        title = { Text("Podešavanja") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Label("Nazivi nota")
                ChipGroup(Notation.entries, s.notation, { if (it == Notation.H) "H i B (naše)" else "B i Bb (engleske)" }) { n -> prefs.update { it.copy(notation = n) } }
                Label("Pragova na vratu")
                ChipGroup(listOf(12, 15), s.maxFret, { "$it" }) { f -> prefs.update { it.copy(maxFret = f) } }
                SwitchRow("Zvuk kad tapneš vrat", s.sound) { v -> prefs.update { it.copy(sound = v) } }
                SwitchRow("Zvuk oktavu više (za zvučnik telefona)", s.octaveUp) { v -> prefs.update { it.copy(octaveUp = v) } }
                TextButton(onClick = { if (confirmReset) { prefs.resetProgress(); confirmReset = false } else confirmReset = true }) {
                    Text(if (confirmReset) "Sigurno? Tapni opet" else "Resetuj napredak i statistiku", color = MaterialTheme.colorScheme.error)
                }
            }
        },
    )
}

@Composable
private fun SwitchRow(label: String, value: Boolean, onChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f).padding(end = 8.dp))
        Switch(value, onChange)
    }
}
