package net.kravos.bass.music

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class TheoryTest {
    private val H = Notation.H

    private fun key(t: String, m: Mode) = Key(Spelled.parse(t), m)

    @Test fun scales() {
        assertEquals("E F# G A H C D", key("E", Mode.MINOR).scale.joinToString(" ") { it.name(H) })
        assertEquals("F G A B C D E", key("F", Mode.MAJOR).scale.joinToString(" ") { it.name(H) })
        assertEquals("F G A Bb C D E", key("F", Mode.MAJOR).scale.joinToString(" ") { it.name(Notation.B) })
        assertEquals("H C# D# E F# G# A#", key("H", Mode.MAJOR).scale.joinToString(" ") { it.name(H) })
        assertEquals("D E F G A B C", key("D", Mode.MINOR).scale.joinToString(" ") { it.name(H) })
    }

    @Test fun diatonic() {
        assertEquals("Em F#dim G Am Hm C D", key("E", Mode.MINOR).diatonic().joinToString(" ") { it.chord.name(H) })
        assertEquals("I ii iii IV V vi vii°", key("C", Mode.MAJOR).diatonic().joinToString(" ") { it.roman })
        assertEquals("Gmaj7 Am7 Hm7 Cmaj7 D7 Em7 F#m7b5", key("G", Mode.MAJOR).diatonic(true).joinToString(" ") { it.chord.name(H) })
        assertEquals("E-mol", key("G", Mode.MAJOR).relative.name(H))
        assertEquals("C-dur", key("A", Mode.MINOR).relative.name(H))
    }

    @Test fun chords() {
        assertEquals("A C# E G", Chord(Spelled.parse("A"), ChordQuality.DOM7).tones.joinToString(" ") { it.second.name(H) })
        assertEquals("F A C", Chord(Spelled.parse("F"), ChordQuality.MAJ).tones.joinToString(" ") { it.second.name(H) })
        assertEquals("B D F", Chord(Spelled.parse("Hb"), ChordQuality.MAJ).tones.joinToString(" ") { it.second.name(H) })
        assertEquals("H D F A", Chord(Spelled.parse("H"), ChordQuality.HALFDIM).tones.joinToString(" ") { it.second.name(H) })
    }

    @Test fun bassNeck() {
        assertEquals(4, Bass.pc(0, 0)) // E
        assertEquals(11, Bass.pc(0, 7)) // H
        assertEquals(0, Bass.pc(1, 3)) // C
        assertEquals(9, Bass.pc(3, 2)) // A
    }

    @Test fun progressions() {
        val em = key("E", Mode.MINOR)
        assertEquals("Em C D Em", Progressions.ALL.first { it.name == "Epski metal" }.steps.joinToString(" ") { it.chord(em).name(H) })
        assertEquals("E5 F5 E5", Progressions.ALL.first { it.name == "Frigijski riff" }.steps.joinToString(" ") { it.chord(em).name(H) })
        Progressions.ALL.forEach { p ->
            Key.all(p.mode).forEach { k -> p.steps.forEach { it.chord(k).name(H) } }
        }
        assertEquals(12, Progressions.ALL.first { it.name == "12-taktni blues" }.totalBars)
    }

    @Test(timeout = 20_000) fun everyQuizGenerates() {
        val r = Random(42)
        for (maxFret in listOf(5, 7, 12, 15)) for (nat in listOf(false, true)) for (kind in QuizKind.entries) repeat(150) {
            val q = QuizGen.make(kind, r, QuizCtx(H, maxFret, nat))
            when (val a = q.answer) {
                is Answer.Choice -> {
                    assertTrue("$kind correct idx", a.correct in a.options.indices)
                    assertEquals("$kind options unique: ${a.options}", a.options.size, a.options.distinct().size)
                }
                is Answer.Tap -> {
                    assertTrue("$kind has solution", a.solution.isNotEmpty())
                    a.solution.forEach { assertTrue("$kind solution passes check", a.check(it.string, it.fret)) }
                }
            }
        }
    }

    @Test(timeout = 10_000) fun lessonsBuild() {
        Lessons.ALL.forEach { l ->
            l.blocks.filterIsInstance<Block.Board>().forEach { assertTrue(l.id, it.build(H).isNotEmpty()) }
            QuizKind.entries.forEach { _ -> }
            repeat(40) { l.quiz.forEach { k -> QuizGen.make(k, Random(it), QuizCtx(H, l.quizMaxFret, l.naturalsOnly, l.quizStrings)) } }
            l.jam?.let { j -> assertTrue("jam $j exists", Progressions.ALL.any { it.name == j }) }
        }
    }
}
