package net.kravos.bass.music

/**
 * Core music theory: pitch classes, correctly spelled notes, intervals, chords, keys.
 * Pitch class: 0 = C … 11 = H (B natural). Letters: 0 = C, 1 = D … 6 = H/B.
 */

enum class Notation { H, B }

private val LETTER_PC = intArrayOf(0, 2, 4, 5, 7, 9, 11)

/** A note name with a letter and an accidental (-2..2). */
data class Spelled(val letter: Int, val acc: Int) {
    val pc: Int get() = mod12(LETTER_PC[letter] + acc)

    fun name(n: Notation): String {
        if (n == Notation.H && letter == 6) {
            return when (acc) {
                -1 -> "B"
                -2 -> "Bb"
                0 -> "H"
                1 -> "H#"
                else -> "Hx"
            }
        }
        val l = if (n == Notation.H) "CDEFGAH"[letter] else "CDEFGAB"[letter]
        return l.toString() + accString(acc)
    }

    /** Transpose by an interval, keeping correct letter spelling. */
    operator fun plus(i: Interval): Spelled {
        val newLetter = (letter + i.steps) % 7
        val target = mod12(pc + i.semis)
        var a = target - LETTER_PC[newLetter]
        while (a > 6) a -= 12
        while (a < -6) a += 12
        return Spelled(newLetter, a)
    }

    companion object {
        fun parse(s: String): Spelled {
            val letter = "CDEFGAH".indexOf(s[0]).let { if (it < 0) "CDEFGAB".indexOf(s[0]) else it }
            require(letter >= 0) { "bad note $s" }
            val rest = s.substring(1)
            val acc = when (rest) {
                "" -> 0
                "#" -> 1
                "x", "##" -> 2
                "b" -> -1
                "bb" -> -2
                else -> error("bad accidental $s")
            }
            return Spelled(letter, acc)
        }
    }
}

fun accString(acc: Int) = when (acc) {
    0 -> ""
    1 -> "#"
    2 -> "x"
    -1 -> "b"
    -2 -> "bb"
    else -> ""
}

fun mod12(x: Int) = ((x % 12) + 12) % 12

/** Generic name for a bare pitch class (used when there's no key context). */
fun pcName(pc: Int, n: Notation): String = when (mod12(pc)) {
    0 -> "C"
    1 -> "C#/Db"
    2 -> "D"
    3 -> "D#/Eb"
    4 -> "E"
    5 -> "F"
    6 -> "F#/Gb"
    7 -> "G"
    8 -> "G#/Ab"
    9 -> "A"
    10 -> if (n == Notation.H) "A#/B" else "A#/Bb"
    else -> if (n == Notation.H) "H" else "B"
}

/** Spelling of a pitch class as a sharp (used on the fretboard without key context). */
fun pcSharp(pc: Int): Spelled = when (mod12(pc)) {
    0 -> Spelled(0, 0); 1 -> Spelled(0, 1); 2 -> Spelled(1, 0); 3 -> Spelled(1, 1)
    4 -> Spelled(2, 0); 5 -> Spelled(3, 0); 6 -> Spelled(3, 1); 7 -> Spelled(4, 0)
    8 -> Spelled(4, 1); 9 -> Spelled(5, 0); 10 -> Spelled(6, -1); else -> Spelled(6, 0)
}

val NATURAL_PCS = setOf(0, 2, 4, 5, 7, 9, 11)

enum class Interval(val semis: Int, val steps: Int, val short: String, val long: String) {
    R(0, 0, "R", "prima (koren)"),
    m2(1, 1, "b2", "mala sekunda"),
    M2(2, 1, "2", "velika sekunda"),
    m3(3, 2, "b3", "mala terca"),
    M3(4, 2, "3", "velika terca"),
    P4(5, 3, "4", "čista kvarta"),
    TT(6, 4, "b5", "smanjena kvinta (tritonus)"),
    P5(7, 4, "5", "čista kvinta"),
    A5(8, 4, "#5", "povećana kvinta"),
    m6(8, 5, "b6", "mala seksta"),
    M6(9, 5, "6", "velika seksta"),
    d7(9, 6, "bb7", "smanjena septima"),
    m7(10, 6, "b7", "mala septima"),
    M7(11, 6, "7", "velika septima"),
    P8(12, 7, "8", "oktava");

    /** Role used for colouring on the fretboard. */
    val role: Role
        get() = when (this) {
            R, P8 -> Role.ROOT
            m3, M3 -> Role.THIRD
            P5, TT, A5 -> Role.FIFTH
            m7, M7, d7 -> Role.SEVENTH
            else -> Role.OTHER
        }

    companion object {
        /** Intervals practised in quizzes (one per semitone, no enharmonic duplicates). */
        val QUIZ = listOf(m2, M2, m3, M3, P4, TT, P5, m6, M6, m7, M7, P8)
    }
}

enum class Role { ROOT, THIRD, FIFTH, SEVENTH, OTHER }

enum class ChordQuality(val suffix: String, val label: String, val intervals: List<Interval>) {
    MAJ("", "dur", listOf(Interval.R, Interval.M3, Interval.P5)),
    MIN("m", "mol", listOf(Interval.R, Interval.m3, Interval.P5)),
    DIM("dim", "smanjeni", listOf(Interval.R, Interval.m3, Interval.TT)),
    AUG("aug", "povećani", listOf(Interval.R, Interval.M3, Interval.A5)),
    POWER("5", "power (kvinta)", listOf(Interval.R, Interval.P5)),
    MAJ7("maj7", "veliki septakord", listOf(Interval.R, Interval.M3, Interval.P5, Interval.M7)),
    DOM7("7", "dominantni septakord", listOf(Interval.R, Interval.M3, Interval.P5, Interval.m7)),
    MIN7("m7", "mol septakord", listOf(Interval.R, Interval.m3, Interval.P5, Interval.m7)),
    HALFDIM("m7b5", "polusmanjeni", listOf(Interval.R, Interval.m3, Interval.TT, Interval.m7)),
    DIM7("dim7", "smanjeni septakord", listOf(Interval.R, Interval.m3, Interval.TT, Interval.d7));

    val formula: String get() = intervals.joinToString(" – ") { it.short }

    companion object {
        val EXPLORER = listOf(MAJ, MIN, POWER, DOM7, MIN7, MAJ7, HALFDIM, DIM, AUG, DIM7)
    }
}

data class Chord(val root: Spelled, val quality: ChordQuality) {
    fun name(n: Notation) = root.name(n) + quality.suffix
    val tones: List<Pair<Interval, Spelled>> get() = quality.intervals.map { it to (root + it) }
    val pcs: Set<Int> get() = tones.map { it.second.pc }.toSet()
    fun intervalOfPc(pc: Int): Interval? = tones.firstOrNull { it.second.pc == mod12(pc) }?.first
}

enum class Mode(val label: String, val pattern: List<Interval>) {
    MAJOR("dur", listOf(Interval.R, Interval.M2, Interval.M3, Interval.P4, Interval.P5, Interval.M6, Interval.M7)),
    MINOR("mol", listOf(Interval.R, Interval.M2, Interval.m3, Interval.P4, Interval.P5, Interval.m6, Interval.m7));
}

data class Key(val tonic: Spelled, val mode: Mode) {
    fun name(n: Notation) = tonic.name(n) + "-" + mode.label

    /** Short name used on chips: "E-mol" / "G-dur". */
    fun shortName(n: Notation) = name(n)

    val scale: List<Spelled> get() = mode.pattern.map { tonic + it }
    val scalePcs: Set<Int> get() = scale.map { it.pc }.toSet()

    /** Pentatonic: major = 1 2 3 5 6, minor = 1 b3 4 5 b7. */
    val pentatonic: List<Pair<Interval, Spelled>>
        get() = when (mode) {
            Mode.MAJOR -> listOf(Interval.R, Interval.M2, Interval.M3, Interval.P5, Interval.M6)
            Mode.MINOR -> listOf(Interval.R, Interval.m3, Interval.P4, Interval.P5, Interval.m7)
        }.map { it to (tonic + it) }

    /** Blues scale on the tonic: minor pentatonic + b5 (used in rock/blues over both dur and mol). */
    val blues: List<Pair<Interval, Spelled>>
        get() = listOf(Interval.R, Interval.m3, Interval.P4, Interval.TT, Interval.P5, Interval.m7)
            .map { it to (tonic + it) }

    /** Diatonic triads (or 7th chords) on each degree. */
    fun diatonic(sevenths: Boolean = false): List<DegreeChord> = scale.indices.map { i ->
        val root = scale[i]
        val third = scale[(i + 2) % 7]
        val fifth = scale[(i + 4) % 7]
        val seventh = scale[(i + 6) % 7]
        val t = mod12(third.pc - root.pc)
        val f = mod12(fifth.pc - root.pc)
        val s = mod12(seventh.pc - root.pc)
        val q = when {
            !sevenths && t == 4 && f == 7 -> ChordQuality.MAJ
            !sevenths && t == 3 && f == 7 -> ChordQuality.MIN
            !sevenths && t == 3 && f == 6 -> ChordQuality.DIM
            !sevenths -> ChordQuality.AUG
            t == 4 && s == 11 -> ChordQuality.MAJ7
            t == 4 && s == 10 -> ChordQuality.DOM7
            t == 3 && f == 7 -> ChordQuality.MIN7
            else -> ChordQuality.HALFDIM
        }
        DegreeChord(i, roman(i, q), Chord(root, q))
    }

    val relative: Key
        get() = when (mode) {
            Mode.MAJOR -> Key(tonic + Interval.M6, Mode.MINOR)
            Mode.MINOR -> Key(tonic + Interval.m3, Mode.MAJOR)
        }

    companion object {
        val MAJOR_TONICS = listOf("C", "G", "D", "A", "E", "H", "F#", "Db", "Ab", "Eb", "Hb", "F")
        val MINOR_TONICS = listOf("A", "E", "H", "F#", "C#", "G#", "D#", "D", "G", "C", "F", "Hb")
        fun all(mode: Mode): List<Key> =
            (if (mode == Mode.MAJOR) MAJOR_TONICS else MINOR_TONICS).map { Key(Spelled.parse(it), mode) }

        /** Keys a cover-band bassist meets most often (rock/metal friendly). */
        fun common(): List<Key> = listOf(
            Key(Spelled.parse("E"), Mode.MINOR), Key(Spelled.parse("A"), Mode.MINOR), Key(Spelled.parse("D"), Mode.MINOR),
            Key(Spelled.parse("H"), Mode.MINOR), Key(Spelled.parse("G"), Mode.MINOR), Key(Spelled.parse("F#"), Mode.MINOR),
            Key(Spelled.parse("E"), Mode.MAJOR), Key(Spelled.parse("A"), Mode.MAJOR), Key(Spelled.parse("G"), Mode.MAJOR),
            Key(Spelled.parse("D"), Mode.MAJOR), Key(Spelled.parse("C"), Mode.MAJOR), Key(Spelled.parse("F"), Mode.MAJOR),
        )
    }
}

data class DegreeChord(val degree: Int, val roman: String, val chord: Chord)

private val ROMANS = listOf("I", "II", "III", "IV", "V", "VI", "VII")

fun roman(degree: Int, q: ChordQuality): String {
    val r = ROMANS[degree]
    return when (q) {
        ChordQuality.MAJ, ChordQuality.POWER, ChordQuality.AUG -> r + if (q == ChordQuality.AUG) "+" else ""
        ChordQuality.MIN -> r.lowercase()
        ChordQuality.DIM -> r.lowercase() + "°"
        ChordQuality.MAJ7 -> r + "maj7"
        ChordQuality.DOM7 -> r + "7"
        ChordQuality.MIN7 -> r.lowercase() + "7"
        ChordQuality.HALFDIM -> r.lowercase() + "ø7"
        ChordQuality.DIM7 -> r.lowercase() + "°7"
    }
}

/** Standard 4-string bass tuning, low to high: E1 A1 D2 G2 (MIDI 28, 33, 38, 43). */
object Bass {
    val OPEN_MIDI = intArrayOf(28, 33, 38, 43)
    val STRING_NAMES = listOf("E", "A", "D", "G")
    fun midi(string: Int, fret: Int) = OPEN_MIDI[string] + fret
    fun pc(string: Int, fret: Int) = mod12(midi(string, fret))
    fun freq(midi: Int) = 440.0 * Math.pow(2.0, (midi - 69) / 12.0)
}
