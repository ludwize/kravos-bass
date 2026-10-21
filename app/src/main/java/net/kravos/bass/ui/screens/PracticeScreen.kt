package net.kravos.bass.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import net.kravos.bass.music.QuizCtx
import net.kravos.bass.music.QuizKind
import net.kravos.bass.ui.components.ChipGroup
import net.kravos.bass.ui.components.Label
import net.kravos.bass.ui.components.LocalPrefs
import net.kravos.bass.ui.components.LocalSettings
import net.kravos.bass.ui.components.SectionCard

data class PracticeRequest(val title: String, val kinds: List<QuizKind>, val ctx: QuizCtx, val count: Int)

@Composable
fun PracticeScreen(pad: PaddingValues, onStart: (PracticeRequest) -> Unit) {
    val settings = LocalSettings.current
    val progress by LocalPrefs.current.progress.collectAsState()
    var maxFret by rememberSaveable { mutableIntStateOf(settings.maxFret) }
    var naturals by rememberSaveable { mutableStateOf(false) }
    var count by rememberSaveable { mutableIntStateOf(10) }
    fun ctx() = QuizCtx(settings.notation, maxFret, naturals)

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = pad.calculateTopPadding() + 8.dp, bottom = pad.calculateBottomPadding() + 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            SectionCard {
                Text("Podešavanje vežbe", style = MaterialTheme.typography.titleMedium)
                Label("Pragovi")
                ChipGroup(listOf(5, 7, 12, 15), maxFret, { "0–$it" }) { maxFret = it }
                Label("Broj pitanja")
                ChipGroup(listOf(10, 20, 30), count, { "$it" }) { count = it }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Samo prirodne note", style = MaterialTheme.typography.bodyLarge)
                        Text("Bez # i b (za vrat)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(naturals, { naturals = it })
                }
                if (progress.bestStreak > 0) Text("Najduži niz tačnih: ${progress.bestStreak}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            }
        }
        item {
            Surface(
                Modifier.fillMaxWidth().clickable { onStart(PracticeRequest("Miks svega", QuizKind.entries, ctx(), count)) },
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
            ) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Filled.Shuffle, null)
                    Column(Modifier.weight(1f)) {
                        Text("Miks svega", style = MaterialTheme.typography.titleMedium)
                        Text("Nasumično iz svih oblasti", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
        QuizKind.entries.groupBy { it.group }.forEach { (group, kinds) ->
            item { Label(group, Modifier.padding(top = 10.dp, start = 4.dp)) }
            item {
                Surface(
                    Modifier.fillMaxWidth().clickable { onStart(PracticeRequest(group, kinds, ctx(), count)) },
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                ) {
                    Text("Sve iz oblasti „$group“", Modifier.padding(horizontal = 16.dp, vertical = 10.dp), style = MaterialTheme.typography.labelLarge)
                }
            }
            items(kinds, key = { it.name }) { k ->
                val st = progress.stats[k.name]
                Surface(
                    Modifier.fillMaxWidth().clickable { onStart(PracticeRequest(k.title, listOf(k), ctx(), count)) },
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer,
                ) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(k.title, style = MaterialTheme.typography.titleMedium)
                            Text(k.desc, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (st != null && st.total > 0) {
                            Column(horizontalAlignment = Alignment.End) {
                                Text("${st.pct}%", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                                Text("${st.total} odg.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }
}
