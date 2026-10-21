package net.kravos.bass.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import net.kravos.bass.music.Notation

data class Settings(
    val notation: Notation = Notation.H,
    val maxFret: Int = 12,
    val sound: Boolean = true,
    /** Phone speakers can't reproduce 41 Hz; play one octave up so you actually hear it. */
    val octaveUp: Boolean = true,
    val showNoteNames: Boolean = true,
)

data class Stat(val right: Int = 0, val total: Int = 0) {
    val pct: Int get() = if (total == 0) 0 else right * 100 / total
}

data class Progress(
    val doneLessons: Set<String> = emptySet(),
    val stats: Map<String, Stat> = emptyMap(),
    val bestStreak: Int = 0,
)

class Prefs(ctx: Context) {
    private val sp: SharedPreferences = ctx.getSharedPreferences("kravos_bass", Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(readSettings())
    val settings: StateFlow<Settings> = _settings

    private val _progress = MutableStateFlow(readProgress())
    val progress: StateFlow<Progress> = _progress

    private fun readSettings() = Settings(
        notation = if (sp.getString("notation", "H") == "B") Notation.B else Notation.H,
        maxFret = sp.getInt("maxFret", 12),
        sound = sp.getBoolean("sound", true),
        octaveUp = sp.getBoolean("octaveUp", true),
        showNoteNames = sp.getBoolean("showNoteNames", true),
    )

    fun update(f: (Settings) -> Settings) {
        val s = f(_settings.value)
        sp.edit()
            .putString("notation", s.notation.name)
            .putInt("maxFret", s.maxFret)
            .putBoolean("sound", s.sound)
            .putBoolean("octaveUp", s.octaveUp)
            .putBoolean("showNoteNames", s.showNoteNames)
            .apply()
        _settings.value = s
    }

    private fun readProgress(): Progress {
        val done = sp.getStringSet("done", emptySet())?.toSet() ?: emptySet()
        val stats = (sp.getString("stats", "") ?: "").split(';').filter { it.isNotBlank() }.mapNotNull {
            val p = it.split(':')
            if (p.size == 3) p[0] to Stat(p[1].toIntOrNull() ?: 0, p[2].toIntOrNull() ?: 0) else null
        }.toMap()
        return Progress(done, stats, sp.getInt("bestStreak", 0))
    }

    private fun saveProgress(p: Progress) {
        sp.edit()
            .putStringSet("done", p.doneLessons)
            .putString("stats", p.stats.entries.joinToString(";") { "${it.key}:${it.value.right}:${it.value.total}" })
            .putInt("bestStreak", p.bestStreak)
            .apply()
        _progress.value = p
    }

    fun markLesson(id: String, done: Boolean = true) {
        val p = _progress.value
        saveProgress(p.copy(doneLessons = if (done) p.doneLessons + id else p.doneLessons - id))
    }

    fun record(kind: String, right: Boolean) {
        val p = _progress.value
        val s = p.stats[kind] ?: Stat()
        saveProgress(p.copy(stats = p.stats + (kind to Stat(s.right + if (right) 1 else 0, s.total + 1))))
    }

    fun streak(n: Int) {
        val p = _progress.value
        if (n > p.bestStreak) saveProgress(p.copy(bestStreak = n))
    }

    fun resetProgress() = saveProgress(Progress())

    companion object {
        @Volatile private var inst: Prefs? = null
        fun get(ctx: Context): Prefs = inst ?: synchronized(this) {
            inst ?: Prefs(ctx.applicationContext).also { inst = it }
        }
    }
}
