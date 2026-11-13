package net.kravos.bass.data

import org.json.JSONArray
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File

/** Runs the same merge cases as web/check/sync-test.js, so the phone and the web always agree. */
class SyncDocTest {
    @Test fun sharedCases() {
        val f = listOf(File("../web/check/sync-cases.json"), File("web/check/sync-cases.json")).first { it.exists() }
        val cases = JSONArray(f.readText())
        for (i in 0 until cases.length()) {
            val c = cases.getJSONObject(i)
            val a = SyncDoc.fromJson(c.getJSONObject("a").toString())
            val b = SyncDoc.fromJson(c.getJSONObject("b").toString())
            val expect = SyncDoc.fromJson(c.getJSONObject("expect").toString())
            val name = c.getString("name")
            assertEquals(name, expect, a.merge(b))
            assertEquals("$name (b,a)", expect, b.merge(a))
            assertEquals("$name round-trip", expect, SyncDoc.fromJson(expect.toJson()))
        }
    }

    @Test fun recordingOnTwoDevices() {
        var web = SyncDoc().withAnswer("w-1", "NOTE_NAME", true, 10).withAnswer("w-1", "NOTE_NAME", false, 11).withStreak(3, 11).withLesson("neck1", true, 12)
        val phone = SyncDoc().withAnswer("a-1", "NOTE_NAME", true, 20).withLesson("neck2", true, 20)
        val p = web.merge(phone).progress()
        assertEquals(Stat(2, 3), p.stats["NOTE_NAME"])
        assertEquals(setOf("neck1", "neck2"), p.doneLessons)
        assertEquals(3, p.bestStreak)
        web = web.merge(phone)
        assertEquals(web, web.merge(web))
    }
}
