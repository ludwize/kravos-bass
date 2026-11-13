package net.kravos.bass.data

import org.json.JSONArray
import org.json.JSONObject

/**
 * Progress that syncs between devices (phone app + web). The same format and merge rules live in web/sync.js.
 *
 * Merge is conflict-free, so devices can sync in any order:
 *  - lessons: per lesson the newest mark (done / not done) wins;
 *  - stats: every device keeps its own counters, totals are the sum over devices (newest copy of each device wins);
 *  - best streak: the larger one;
 *  - reset: newest reset time wins and drops everything recorded before it.
 *
 * JSON: {"v":1,"reset":0,"lessons":{"neck1":{"d":1,"t":ms}},"stats":{"a-1f2e":{"t":ms,"k":{"NOTE_NAME":[3,5]}}},"best":{"n":4,"t":ms}}
 */
data class LessonMark(val done: Boolean, val t: Long)
data class DeviceStats(val t: Long, val k: Map<String, Stat>)

data class SyncDoc(
    val reset: Long = 0,
    val lessons: Map<String, LessonMark> = emptyMap(),
    val stats: Map<String, DeviceStats> = emptyMap(),
    val best: Int = 0,
    val bestT: Long = 0,
) {
    fun merge(o: SyncDoc): SyncDoc {
        val r = maxOf(reset, o.reset)
        val ls = (lessons.keys + o.lessons.keys).mapNotNull { id ->
            val a = lessons[id]
            val b = o.lessons[id]
            val w = when {
                a == null -> b
                b == null -> a
                b.t > a.t -> b
                b.t == a.t && b.done && !a.done -> b
                else -> a
            }
            w?.takeIf { it.t >= r }?.let { id to it }
        }.toMap()
        val ss = (stats.keys + o.stats.keys).mapNotNull { dev ->
            val a = stats[dev]
            val b = o.stats[dev]
            val w = if (a == null) b else if (b == null) a else if (b.t > a.t) b else a
            w?.takeIf { it.t >= r }?.let { dev to it }
        }.toMap()
        val (bn, bt) = listOf(best to bestT, o.best to o.bestT).filter { it.second >= r }.maxWithOrNull(compareBy({ it.first }, { it.second })) ?: (0 to r)
        return SyncDoc(r, ls, ss, bn, bt)
    }

    fun progress(): Progress {
        val totals = HashMap<String, Stat>()
        for (d in stats.values) for ((kind, s) in d.k) {
            val cur = totals[kind] ?: Stat()
            totals[kind] = Stat(cur.right + s.right, cur.total + s.total)
        }
        return Progress(lessons.filter { it.value.done }.keys, totals, best)
    }

    fun withLesson(id: String, done: Boolean, now: Long) = copy(lessons = lessons + (id to LessonMark(done, now)))

    fun withAnswer(device: String, kind: String, right: Boolean, now: Long): SyncDoc {
        val d = stats[device]?.takeIf { it.t >= reset } ?: DeviceStats(now, emptyMap())
        val s = d.k[kind] ?: Stat()
        return copy(stats = stats + (device to DeviceStats(now, d.k + (kind to Stat(s.right + if (right) 1 else 0, s.total + 1)))))
    }

    fun withStreak(n: Int, now: Long) = if (n > best) copy(best = n, bestT = now) else this

    fun toJson(): String {
        val o = JSONObject()
        o.put("v", 1)
        o.put("reset", reset)
        val l = JSONObject()
        for ((id, m) in lessons.toSortedMap()) l.put(id, JSONObject().put("d", if (m.done) 1 else 0).put("t", m.t))
        o.put("lessons", l)
        val s = JSONObject()
        for ((dev, d) in stats.toSortedMap()) {
            val k = JSONObject()
            for ((kind, st) in d.k.toSortedMap()) k.put(kind, JSONArray().put(st.right).put(st.total))
            s.put(dev, JSONObject().put("t", d.t).put("k", k))
        }
        o.put("stats", s)
        o.put("best", JSONObject().put("n", best).put("t", bestT))
        return o.toString()
    }

    companion object {
        fun fromJson(text: String?): SyncDoc {
            if (text.isNullOrBlank()) return SyncDoc()
            return try {
                val o = JSONObject(text)
                val l = o.optJSONObject("lessons") ?: JSONObject()
                val lessons = l.keys().asSequence().associateWith { id ->
                    val m = l.getJSONObject(id)
                    LessonMark(m.optInt("d") == 1, m.optLong("t"))
                }
                val s = o.optJSONObject("stats") ?: JSONObject()
                val stats = s.keys().asSequence().associateWith { dev ->
                    val d = s.getJSONObject(dev)
                    val k = d.optJSONObject("k") ?: JSONObject()
                    DeviceStats(d.optLong("t"), k.keys().asSequence().associateWith { kind ->
                        val a = k.getJSONArray(kind)
                        Stat(a.optInt(0), a.optInt(1))
                    })
                }
                val b = o.optJSONObject("best")
                SyncDoc(o.optLong("reset"), lessons, stats, b?.optInt("n") ?: 0, b?.optLong("t") ?: 0)
            } catch (e: Exception) {
                SyncDoc()
            }
        }
    }
}
