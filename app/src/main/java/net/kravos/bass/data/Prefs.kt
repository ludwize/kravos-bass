package net.kravos.bass.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.withLock
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

    // ---------------------------------------------------------------- progress (synced, see SyncDoc / GistSync)

    val deviceId: String = sp.getString("deviceId", null) ?: ("a-" + java.util.UUID.randomUUID().toString().take(8)).also {
        sp.edit().putString("deviceId", it).apply()
    }
    @Volatile private var doc: SyncDoc = loadDoc()

    /** Old (pre-sync) progress becomes this device's first sync document. */
    private fun loadDoc(): SyncDoc {
        sp.getString("syncdoc", null)?.let { return SyncDoc.fromJson(it) }
        val now = System.currentTimeMillis()
        val done = sp.getStringSet("done", emptySet())?.toSet() ?: emptySet()
        val stats = (sp.getString("stats", "") ?: "").split(';').filter { it.isNotBlank() }.mapNotNull {
            val p = it.split(':')
            if (p.size == 3) p[0] to Stat(p[1].toIntOrNull() ?: 0, p[2].toIntOrNull() ?: 0) else null
        }.toMap()
        val best = sp.getInt("bestStreak", 0)
        val d = SyncDoc(
            lessons = done.associateWith { LessonMark(true, now) },
            stats = if (stats.isEmpty()) emptyMap() else mapOf(deviceId to DeviceStats(now, stats)),
            best = best, bestT = if (best > 0) now else 0,
        )
        sp.edit().putString("syncdoc", d.toJson()).apply()
        return d
    }

    private val _progress = MutableStateFlow(doc.progress())
    val progress: StateFlow<Progress> = _progress

    @Synchronized private fun change(f: (SyncDoc) -> SyncDoc) {
        val d = f(doc)
        if (d == doc) return
        doc = d
        sp.edit().putString("syncdoc", d.toJson()).apply()
        _progress.value = d.progress()
        scheduleSync()
    }

    fun markLesson(id: String, done: Boolean = true) = change { it.withLesson(id, done, System.currentTimeMillis()) }
    fun record(kind: String, right: Boolean) = change { it.withAnswer(deviceId, kind, right, System.currentTimeMillis()) }
    fun streak(n: Int) = change { it.withStreak(n, System.currentTimeMillis()) }
    fun resetProgress() = change { SyncDoc(reset = System.currentTimeMillis()) }

    // ---------------------------------------------------------------- sync

    data class SyncState(val connected: Boolean, val busy: Boolean = false, val lastOk: Long = 0, val error: String? = null)

    private val _sync = MutableStateFlow(SyncState(sp.getString("ghToken", null) != null, lastOk = sp.getLong("syncOk", 0)))
    val sync: StateFlow<SyncState> = _sync
    private val scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.Dispatchers.IO)
    private val mutex = kotlinx.coroutines.sync.Mutex()
    private var pending: kotlinx.coroutines.Job? = null

    private fun scheduleSync() {
        if (sp.getString("ghToken", null) == null) return
        pending?.cancel()
        pending = scope.launch { kotlinx.coroutines.delay(2500); syncNow() }
    }

    /** Pull → merge → push (only if something changed). Safe to call any time. */
    fun syncNow() = scope.launch {
        val token = sp.getString("ghToken", null) ?: return@launch
        mutex.withLock {
            _sync.value = _sync.value.copy(connected = true, busy = true)
            try {
                val gh = GistSync(token)
                var id = sp.getString("gistId", null)
                var remote = id?.let { gh.read(it) }
                if (remote == null) {
                    id = gh.find()
                    remote = id?.let { gh.read(it) }
                }
                val local = doc
                val merged = if (remote == null) local else local.merge(remote)
                if (id == null) id = gh.create(merged) else if (merged != remote) gh.write(id, merged)
                sp.edit().putString("gistId", id).apply()
                synchronized(this@Prefs) {
                    // anything recorded while we were talking to GitHub is merged in too
                    val now = doc.merge(merged)
                    doc = now
                    sp.edit().putString("syncdoc", now.toJson()).apply()
                    _progress.value = now.progress()
                    if (now != merged) scheduleSync()
                }
                val t = System.currentTimeMillis()
                sp.edit().putLong("syncOk", t).apply()
                _sync.value = SyncState(true, false, t, null)
            } catch (e: Exception) {
                _sync.value = _sync.value.copy(busy = false, error = (e as? SyncError)?.message ?: "Nema veze sa GitHub-om.")
            }
        }
    }

    fun connect(token: String) {
        sp.edit().putString("ghToken", token.trim()).remove("gistId").apply()
        _sync.value = SyncState(true)
        syncNow()
    }

    fun disconnect() {
        sp.edit().remove("ghToken").remove("gistId").remove("syncOk").apply()
        _sync.value = SyncState(false)
    }

    companion object {
        @Volatile private var inst: Prefs? = null
        fun get(ctx: Context): Prefs = inst ?: synchronized(this) {
            inst ?: Prefs(ctx.applicationContext).also { inst = it }
        }
    }
}
