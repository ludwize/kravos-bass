package net.kravos.bass.music

import kotlin.random.Random

enum class MarkerStyle { STRONG, SOFT, GHOST, QUESTION, TARGET }

/** A dot on the fretboard. string: 0 = E (lowest) … 3 = G. */
data class BoardMarker(
    val string: Int,
    val fret: Int,
    val label: String? = null,
    val role: Role? = null,
    val style: MarkerStyle = MarkerStyle.STRONG,
)

sealed class Answer {
    data class Choice(val options: List<String>, val correct: Int, val columns: Int = 2) : Answer()
    class Tap(val check: (string: Int, fret: Int) -> Boolean, val solution: List<BoardMarker>) : Answer()
}

data class Question(
    val kind: QuizKind,
    val prompt: String,
    val markers: List<BoardMarker>,
    val answer: Answer,
    val explain: String,
    /** Notes to play when the answer is revealed (MIDI). */
    val sound: List<Int> = emptyList(),
)

data class QuizCtx(val n: Notation, val maxFret: Int, val naturalsOnly: Boolean = false, val strings: List<Int> = listOf(0, 1, 2, 3))

enum class QuizKind(val title: String, val desc: String, val group: String) {
    NOTE_NAME("Nota na pragu", "Vidiš tačku na vratu — koja je nota?", "Vrat"),
    FIND_NOTE("Nađi notu", "Tapni zadatu notu na zadatoj žici", "Vrat"),
    OCTAVE("Oktave", "Nađi oktavu od zadate note", "Vrat"),
    INTERVAL_TAP("Intervali na vratu", "Od korena tapni traženi interval", "Intervali"),
    INTERVAL_NAME("Prepoznaj interval", "Dve tačke — koji je interval?", "Intervali"),
    CHORD_SPELL("Sastav akorda", "Koje note čine akord?", "Akordi"),
    CHORD_TONE("Ton akorda", "Koja je terca / kvinta / septima akorda?", "Akordi"),
    CHORD_TAP("Tonovi akorda na vratu", "Od korena akorda nađi tercu, kvintu, septimu", "Akordi"),
    KEY_DEGREE("Akordi tonaliteta", "Koji akord je na datom stepenu?", "Tonalitet"),
    KEY_FUNCTION("Stepen akorda", "Koji stepen je dati akord u tonalitetu?", "Tonalitet"),
    KEY_MEMBER("Uljez", "Koji akord ne pripada tonalitetu?", "Tonalitet"),
    RELATIVE("Relativni tonalitet", "Dur ↔ mol parovi", "Tonalitet"),
    PENTA("Pentatonika", "Koja nota je u pentatonici?", "Improvizacija"),
    SAFE_NOTE("Sigurna nota", "Tonalitet + akord → koja nota sigurno leži", "Improvizacija"),
}

object QuizGen {
    private val CHORD_ROOTS = listOf("C", "D", "E", "F", "G", "A", "H", "Hb", "Eb", "Ab", "F#", "C#", "Db", "G#").map { Spelled.parse(it) }

    fun make(kind: QuizKind, r: Random, c: QuizCtx): Question = when (kind) {
        QuizKind.NOTE_NAME -> noteName(r, c)
        QuizKind.FIND_NOTE -> findNote(r, c)
        QuizKind.OCTAVE -> octave(r, c)
        QuizKind.INTERVAL_TAP -> intervalTap(r, c)
        QuizKind.INTERVAL_NAME -> intervalName(r, c)
        QuizKind.CHORD_SPELL -> chordSpell(r, c)
        QuizKind.CHORD_TONE -> chordTone(r, c)
        QuizKind.CHORD_TAP -> chordTap(r, c)
        QuizKind.KEY_DEGREE -> keyDegree(r, c)
        QuizKind.KEY_FUNCTION -> keyFunction(r, c)
        QuizKind.KEY_MEMBER -> keyMember(r, c)
        QuizKind.RELATIVE -> relative(r, c)
        QuizKind.PENTA -> penta(r, c)
        QuizKind.SAFE_NOTE -> safeNote(r, c)
    }

    // ---------- helpers ----------

    private fun randomPos(r: Random, c: QuizCtx, maxFret: Int = c.maxFret): Pair<Int, Int> {
        while (true) {
            val s = c.strings.random(r)
            val f = r.nextInt(0, maxFret + 1)
            if (!c.naturalsOnly || Bass.pc(s, f) in NATURAL_PCS) return s to f
        }
    }

    private fun allPositions(c: QuizCtx, pred: (Int, Int) -> Boolean): List<Pair<Int, Int>> =
        (0..3).flatMap { s -> (0..c.maxFret).filter { f -> pred(s, f) }.map { s to it } }

    private fun options(r: Random, correct: String, pool: List<String>, count: Int = 4): Pair<List<String>, Int> {
        val distract = pool.filter { it != correct }.distinct().shuffled(r).take(count - 1)
        val all = (distract + correct).shuffled(r)
        return all to all.indexOf(correct)
    }

    private fun randomKey(r: Random, mode: Mode? = null): Key {
        val m = mode ?: if (r.nextBoolean()) Mode.MAJOR else Mode.MINOR
        // Bias toward the keys you actually meet in rock/metal.
        return if (r.nextInt(3) < 2) Key.common().filter { mode == null || it.mode == m }.random(r) else Key.all(m).random(r)
    }

    private fun randomChord(r: Random, qualities: List<ChordQuality>): Chord {
        val q = qualities.random(r)
        return Chord(CHORD_ROOTS.random(r), q)
    }

    private fun midiAt(p: Pair<Int, Int>) = Bass.midi(p.first, p.second)

    // ---------- generators ----------

    private fun noteName(r: Random, c: QuizCtx): Question {
        val p = randomPos(r, c)
        val pc = Bass.pc(p.first, p.second)
        val opts = if (c.naturalsOnly) NATURAL_PCS.sorted() else (0..11).toList()
        val names = opts.map { pcName(it, c.n) }
        val strName = Bass.STRING_NAMES[p.first]
        return Question(
            QuizKind.NOTE_NAME,
            "Koja je ovo nota? (${strName} žica, ${p.second}. prag)",
            listOf(BoardMarker(p.first, p.second, "?", style = MarkerStyle.QUESTION)),
            Answer.Choice(names, opts.indexOf(pc), columns = if (c.naturalsOnly) 4 else 3),
            explainNote(p.first, p.second, c.n),
            listOf(midiAt(p)),
        )
    }

    private fun explainNote(s: Int, f: Int, n: Notation): String {
        val name = pcName(Bass.pc(s, f), n)
        val tip = when {
            f == 0 -> "Prazna žica."
            f == 12 -> "12. prag je oktava prazne žice."
            f > 12 -> "Prag $f = prag ${f - 12} oktavu više."
            f == 5 || f == 7 -> "Orijentir: ${f}. prag = nota prazne žice iznad${if (f == 7) " (oktava niže)" else ""}."
            else -> {
                val nearest = listOf(0, 3, 5, 7, 9, 12).minBy { kotlin.math.abs(it - f) }
                "Od $nearest. praga (${pcName(Bass.pc(s, nearest), n)}) ${if (f > nearest) "+" else "−"}${kotlin.math.abs(f - nearest)} polustepen(a)."
            }
        }
        return "$name. $tip"
    }

    private fun findNote(r: Random, c: QuizCtx): Question {
        val s = c.strings.random(r)
        val candidates = (0..c.maxFret).filter { !c.naturalsOnly || Bass.pc(s, it) in NATURAL_PCS }
        val f = candidates.random(r)
        val pc = Bass.pc(s, f)
        val sol = (0..c.maxFret).filter { Bass.pc(s, it) == pc }.map { BoardMarker(s, it, pcSharp(pc).name(c.n), style = MarkerStyle.TARGET) }
        return Question(
            QuizKind.FIND_NOTE,
            "Tapni ${pcName(pc, c.n)} na ${Bass.STRING_NAMES[s]} žici",
            emptyList(),
            Answer.Tap({ ss, ff -> ss == s && Bass.pc(ss, ff) == pc }, sol),
            explainNote(s, f, c.n),
            listOf(Bass.midi(s, f)),
        )
    }

    private fun octave(r: Random, c: QuizCtx): Question {
        while (true) {
            val p = randomPos(r, c.copy(strings = listOf(0, 1)), maxFret = minOf(c.maxFret, 10))
            val m = midiAt(p)
            val sol = allPositions(c) { s, f -> Bass.midi(s, f) == m + 12 }
            if (sol.isEmpty()) continue
            val name = pcSharp(m).name(c.n)
            return Question(
                QuizKind.OCTAVE,
                "Ovo je $name. Tapni oktavu više.",
                listOf(BoardMarker(p.first, p.second, name, Role.ROOT)),
                Answer.Tap({ s, f -> Bass.midi(s, f) == m + 12 }, sol.map { BoardMarker(it.first, it.second, "8", Role.ROOT, MarkerStyle.TARGET) }),
                "Oblik oktave: dve žice gore, dva praga desno. (Ili ista žica + 12 pragova.)",
                listOf(m, m + 12),
            )
        }
    }

    private fun intervalTap(r: Random, c: QuizCtx): Question {
        while (true) {
            val iv = Interval.QUIZ.random(r)
            val p = randomPos(r, c.copy(strings = listOf(0, 1, 2)), maxFret = minOf(c.maxFret, 9))
            val m = midiAt(p)
            val target = m + iv.semis
            val sol = allPositions(c) { s, f -> Bass.midi(s, f) == target && kotlin.math.abs(f - p.second) <= 5 }
            if (sol.isEmpty()) continue
            val root = pcSharp(m)
            val tname = (root + iv).name(c.n)
            return Question(
                QuizKind.INTERVAL_TAP,
                "Koren je ${root.name(c.n)}. Tapni ${iv.long} (${iv.short}) iznad njega.",
                listOf(BoardMarker(p.first, p.second, "R", Role.ROOT)),
                Answer.Tap({ s, f -> Bass.midi(s, f) == target }, sol.map { BoardMarker(it.first, it.second, iv.short, iv.role, MarkerStyle.TARGET) }),
                "${iv.short} od ${root.name(c.n)} je $tname — ${iv.semis} polustepen(a). ${shapeHint(iv)}",
                listOf(m, target),
            )
        }
    }

    fun shapeHint(iv: Interval): String = when (iv) {
        Interval.m2 -> "Prag pored korena."
        Interval.M2 -> "Dva praga desno, ista žica."
        Interval.m3 -> "3 praga desno na istoj žici, ili žica gore + 2 praga levo."
        Interval.M3 -> "Žica gore, jedan prag levo."
        Interval.P4 -> "Isti prag, žica gore."
        Interval.TT -> "Žica gore + 1 prag desno (između kvarte i kvinte)."
        Interval.P5 -> "Žica gore, dva praga desno — „power chord“ oblik."
        Interval.m6, Interval.A5 -> "Dve žice gore, dva praga levo (ili žica gore + 3 praga)."
        Interval.M6 -> "Dve žice gore, jedan prag levo (ili žica gore + 4 praga)."
        Interval.m7 -> "Dve žice gore, isti prag."
        Interval.M7 -> "Dve žice gore, jedan prag desno (pola stepena ispod oktave)."
        Interval.P8 -> "Dve žice gore, dva praga desno."
        else -> ""
    }

    private fun intervalName(r: Random, c: QuizCtx): Question {
        while (true) {
            val iv = Interval.QUIZ.random(r)
            val p = randomPos(r, c.copy(strings = listOf(0, 1, 2)), maxFret = minOf(c.maxFret, 9))
            val m = midiAt(p)
            val sol = allPositions(c) { s, f -> Bass.midi(s, f) == m + iv.semis && kotlin.math.abs(f - p.second) <= 4 && s != p.first }
                .ifEmpty { allPositions(c) { s, f -> Bass.midi(s, f) == m + iv.semis && kotlin.math.abs(f - p.second) <= 5 } }
            if (sol.isEmpty()) continue
            val q = sol.random(r)
            val label = { i: Interval -> "${i.short} · ${i.long}" }
            val (opts, idx) = options(r, label(iv), Interval.QUIZ.map(label))
            return Question(
                QuizKind.INTERVAL_NAME,
                "Koji je interval od R do „?“",
                listOf(BoardMarker(p.first, p.second, "R", Role.ROOT), BoardMarker(q.first, q.second, "?", style = MarkerStyle.QUESTION)),
                Answer.Choice(opts, idx, columns = 1),
                "${iv.long}: ${iv.semis} polustepen(a). ${shapeHint(iv)}",
                listOf(m, m + iv.semis),
            )
        }
    }

    private fun chordSpell(r: Random, c: QuizCtx): Question {
        val qs = listOf(ChordQuality.MAJ, ChordQuality.MIN, ChordQuality.DOM7, ChordQuality.MIN7, ChordQuality.MAJ7, ChordQuality.POWER, ChordQuality.HALFDIM)
        val ch = randomChord(r, qs)
        fun spell(x: Chord) = x.tones.joinToString(" ") { it.second.name(c.n) }
        val correct = spell(ch)
        val pool = buildList {
            ChordQuality.entries.filter { it.intervals.size == ch.quality.intervals.size && it != ch.quality }.forEach { add(spell(Chord(ch.root, it))) }
            add(spell(Chord(ch.root + Interval.m2, ch.quality)))
            add(spell(Chord(ch.root + Interval.M2, ch.quality)))
        }
        val (opts, idx) = options(r, correct, pool)
        return Question(
            QuizKind.CHORD_SPELL,
            "Koje note čine ${ch.name(c.n)}?",
            emptyList(),
            Answer.Choice(opts, idx, columns = 1),
            "${ch.name(c.n)} (${ch.quality.label}) = ${ch.quality.formula} → $correct",
            ch.tones.map { 40 + mod12(it.second.pc - 4) },
        )
    }

    private fun chordTone(r: Random, c: QuizCtx): Question {
        val qs = listOf(ChordQuality.MAJ, ChordQuality.MIN, ChordQuality.DOM7, ChordQuality.MIN7, ChordQuality.MAJ7)
        val ch = randomChord(r, qs)
        val (iv, note) = ch.tones.drop(1).random(r)
        val what = when (iv.role) {
            Role.THIRD -> "terca"
            Role.FIFTH -> "kvinta"
            Role.SEVENTH -> "septima"
            else -> iv.long
        }
        val correct = note.name(c.n)
        val pool = (ch.tones.map { it.second.name(c.n) } + (ch.root + Interval.M3).name(c.n) + (ch.root + Interval.m3).name(c.n) +
            (ch.root + Interval.P4).name(c.n) + (ch.root + Interval.M6).name(c.n) + (ch.root + Interval.m7).name(c.n) + (ch.root + Interval.M7).name(c.n))
        val (opts, idx) = options(r, correct, pool)
        return Question(
            QuizKind.CHORD_TONE,
            "Koja je $what u ${ch.name(c.n)}?",
            emptyList(),
            Answer.Choice(opts, idx, columns = 4),
            "${ch.name(c.n)}: ${ch.tones.joinToString(" – ") { "${it.first.short}=${it.second.name(c.n)}" }}",
            listOf(40 + mod12(ch.root.pc - 4), 40 + mod12(ch.root.pc - 4) + iv.semis),
        )
    }

    private fun chordTap(r: Random, c: QuizCtx): Question {
        while (true) {
            val qs = listOf(ChordQuality.MAJ, ChordQuality.MIN, ChordQuality.DOM7, ChordQuality.MIN7, ChordQuality.MAJ7)
            val ch = randomChord(r, qs)
            val rootPositions = allPositions(c) { s, f -> s <= 1 && Bass.pc(s, f) == ch.root.pc && f in 1..minOf(c.maxFret, 10) }
            if (rootPositions.isEmpty()) continue
            val rp = rootPositions.random(r)
            val (iv, note) = ch.tones.drop(1).random(r)
            val what = when (iv.role) {
                Role.THIRD -> "tercu"
                Role.FIFTH -> "kvintu"
                Role.SEVENTH -> "septimu"
                else -> iv.long
            }
            val sol = allPositions(c) { s, f -> Bass.pc(s, f) == note.pc && kotlin.math.abs(f - rp.second) <= 4 && Bass.midi(s, f) > midiAt(rp) }
            if (sol.isEmpty()) continue
            return Question(
                QuizKind.CHORD_TAP,
                "Akord ${ch.name(c.n)}, koren je označen. Tapni $what (${note.name(c.n)}).",
                listOf(BoardMarker(rp.first, rp.second, "R", Role.ROOT)),
                Answer.Tap({ s, f -> Bass.pc(s, f) == note.pc }, sol.map { BoardMarker(it.first, it.second, iv.short, iv.role, MarkerStyle.TARGET) }),
                "${iv.short} od ${ch.root.name(c.n)} = ${note.name(c.n)}. ${shapeHint(iv)}",
                listOf(midiAt(rp), midiAt(rp) + iv.semis),
            )
        }
    }

    private fun keyDegree(r: Random, c: QuizCtx): Question {
        val k = randomKey(r)
        val dia = k.diatonic()
        val target = dia.drop(1).random(r)
        val correct = target.chord.name(c.n)
        val pool = dia.map { it.chord.name(c.n) } +
            listOf(Chord(target.chord.root, if (target.chord.quality == ChordQuality.MAJ) ChordQuality.MIN else ChordQuality.MAJ).name(c.n))
        val (opts, idx) = options(r, correct, pool)
        return Question(
            QuizKind.KEY_DEGREE,
            "${k.name(c.n)}: koji akord je ${target.roman}?",
            emptyList(),
            Answer.Choice(opts, idx, columns = 4),
            "${k.name(c.n)}: " + dia.joinToString("  ") { "${it.roman} ${it.chord.name(c.n)}" },
        )
    }

    private fun keyFunction(r: Random, c: QuizCtx): Question {
        val k = randomKey(r)
        val dia = k.diatonic()
        val target = dia.drop(1).random(r)
        val (opts, idx) = options(r, target.roman, dia.map { it.roman })
        return Question(
            QuizKind.KEY_FUNCTION,
            "Pesma je u ${k.name(c.n)}. Akord ${target.chord.name(c.n)} je koji stepen?",
            emptyList(),
            Answer.Choice(opts, idx, columns = 4),
            "${k.name(c.n)}: " + dia.joinToString("  ") { "${it.roman} ${it.chord.name(c.n)}" } +
                "\nVelika slova = dur, mala = mol, ° = smanjeni.",
        )
    }

    private fun keyMember(r: Random, c: QuizCtx): Question {
        val k = randomKey(r)
        val dia = k.diatonic().filter { it.chord.quality != ChordQuality.DIM }
        val inKey = dia.shuffled(r).take(3).map { it.chord.name(c.n) }
        // Outsider: a diatonic root with the wrong quality, or a chord a semitone off.
        val outsider = run {
            val names = k.diatonic().map { it.chord.name(c.n) }.toSet()
            val cands = buildList {
                dia.forEach { d ->
                    add(Chord(d.chord.root, if (d.chord.quality == ChordQuality.MAJ) ChordQuality.MIN else ChordQuality.MAJ))
                }
                add(Chord(k.tonic + Interval.m2, ChordQuality.MAJ))
                add(Chord(k.tonic + Interval.TT, ChordQuality.MAJ))
            }.map { it.name(c.n) }.filter { it !in names }
            cands.random(r)
        }
        val all = (inKey + outsider).shuffled(r)
        return Question(
            QuizKind.KEY_MEMBER,
            "Koji akord NE pripada tonalitetu ${k.name(c.n)}?",
            emptyList(),
            Answer.Choice(all, all.indexOf(outsider), columns = 4),
            "${k.name(c.n)}: " + k.diatonic().joinToString("  ") { "${it.roman} ${it.chord.name(c.n)}" },
        )
    }

    private fun relative(r: Random, c: QuizCtx): Question {
        val k = randomKey(r)
        val rel = k.relative
        val correct = rel.name(c.n)
        val pool = Key.all(rel.mode).map { it.name(c.n) }
        val (opts, idx) = options(r, correct, pool)
        val rule = if (k.mode == Mode.MAJOR) "Relativni mol je na 6. stepenu dura (mala terca niže)." else "Relativni dur je mala terca iznad mola."
        return Question(
            QuizKind.RELATIVE,
            "Koji je relativni ${if (rel.mode == Mode.MINOR) "mol" else "dur"} od ${k.name(c.n)}?",
            emptyList(),
            Answer.Choice(opts, idx, columns = 2),
            "$correct — iste note, isti akordi, drugi „centar“. $rule",
        )
    }

    private fun penta(r: Random, c: QuizCtx): Question {
        val k = randomKey(r)
        val members = k.pentatonic.map { it.second }
        val memberPcs = members.map { it.pc }.toSet()
        val correct = members.drop(1).random(r).name(c.n)
        val outsiders = (0..11).filter { it !in memberPcs }.map { pc ->
            k.scale.firstOrNull { it.pc == pc }?.name(c.n) ?: pcSharp(pc).name(c.n)
        }
        val all = (outsiders.shuffled(r).take(3) + correct).shuffled(r)
        return Question(
            QuizKind.PENTA,
            "Koja nota JE u ${k.tonic.name(c.n)} ${k.mode.label} pentatonici?",
            emptyList(),
            Answer.Choice(all, all.indexOf(correct), columns = 4),
            "${k.tonic.name(c.n)} ${k.mode.label} pentatonika: " + k.pentatonic.joinToString(" ") { it.second.name(c.n) } +
                " (" + k.pentatonic.joinToString(" ") { it.first.short } + ")",
        )
    }

    private fun safeNote(r: Random, c: QuizCtx): Question {
        val k = randomKey(r)
        val dc = k.diatonic().filter { it.chord.quality != ChordQuality.DIM }.random(r)
        val ch = dc.chord
        val correctTone = ch.tones.random(r)
        val correct = correctTone.second.name(c.n)
        val chordPcs = ch.pcs
        // Distractors: scale notes that are NOT chord tones (they work as passing notes, not landing notes) + outside notes.
        val inScaleNon = k.scale.filter { it.pc !in chordPcs }.map { it.name(c.n) }
        val outside = (0..11).filter { it !in k.scalePcs }.map { pcSharp(it).name(c.n) }
        val pool = (inScaleNon.shuffled(r).take(2) + outside.shuffled(r).take(2)).distinct()
        val all = (pool.take(3) + correct).shuffled(r)
        return Question(
            QuizKind.SAFE_NOTE,
            "Pesma u ${k.name(c.n)}, bend svira ${ch.name(c.n)} (${dc.roman}). Na koju notu možeš sigurno da staneš (ton akorda)?",
            emptyList(),
            Answer.Choice(all, all.indexOf(correct), columns = 4),
            "${ch.name(c.n)} = " + ch.tones.joinToString(" – ") { "${it.first.short} ${it.second.name(c.n)}" } +
                ". Ostale note iz lestvice su dobre kao prolazne, ali na „jaku dobu“ stani na ton akorda.",
        )
    }
}
