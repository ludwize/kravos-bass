package net.kravos.bass.music

/** Helpers that turn theory objects into fretboard markers. */
object Boards {
    fun positions(maxFret: Int, minFret: Int = 0, pred: (Int, Int) -> Boolean): List<Pair<Int, Int>> =
        (0..3).flatMap { s -> (minFret..maxFret).filter { f -> pred(s, f) }.map { s to it } }

    /** All chord tones, coloured by role. */
    fun chord(ch: Chord, n: Notation, maxFret: Int, labelNotes: Boolean = false, minFret: Int = 0): List<BoardMarker> =
        positions(maxFret, minFret) { s, f -> Bass.pc(s, f) in ch.pcs }.map { (s, f) ->
            val iv = ch.intervalOfPc(Bass.pc(s, f))!!
            val note = ch.tones.first { it.first == iv }.second
            BoardMarker(s, f, if (labelNotes) note.name(n) else iv.short, iv.role)
        }

    /** Scale notes as soft dots (labelled with the degree relative to the tonic). */
    fun scale(notes: List<Pair<Interval, Spelled>>, n: Notation, maxFret: Int, labelNotes: Boolean, style: MarkerStyle = MarkerStyle.SOFT): List<BoardMarker> {
        val byPc = notes.associateBy { it.second.pc }
        return positions(maxFret) { s, f -> Bass.pc(s, f) in byPc }.map { (s, f) ->
            val (iv, sp) = byPc.getValue(Bass.pc(s, f))
            BoardMarker(s, f, if (labelNotes) sp.name(n) else iv.short, if (iv == Interval.R) Role.ROOT else Role.OTHER, if (iv == Interval.R) MarkerStyle.STRONG else style)
        }
    }

    fun keyScale(k: Key): List<Pair<Interval, Spelled>> = k.mode.pattern.zip(k.scale)

    /** Chord tones strong + remaining notes of [background] soft, no duplicates. */
    fun chordOver(ch: Chord, background: List<Pair<Interval, Spelled>>, n: Notation, maxFret: Int, labelNotes: Boolean): List<BoardMarker> {
        val strong = chord(ch, n, maxFret, labelNotes)
        val taken = strong.map { it.string to it.fret }.toSet()
        val bgByPc = background.associateBy { it.second.pc }
        val soft = positions(maxFret) { s, f -> (s to f) !in taken && Bass.pc(s, f) in bgByPc }.map { (s, f) ->
            val sp = bgByPc.getValue(Bass.pc(s, f)).second
            // label relative to the chord root, so you see e.g. "2", "4", "6" passing tones
            val semis = mod12(sp.pc - ch.root.pc)
            val iv = Interval.entries.firstOrNull { it.semis == semis && it != Interval.A5 && it != Interval.d7 } ?: Interval.R
            BoardMarker(s, f, if (labelNotes) sp.name(n) else if (semis == 6) "#4" else iv.short, Role.OTHER, MarkerStyle.SOFT)
        }
        return soft + strong
    }

    /** Every natural note, named. */
    fun naturals(n: Notation, maxFret: Int, strings: List<Int> = listOf(0, 1, 2, 3)): List<BoardMarker> =
        positions(maxFret) { s, f -> s in strings && Bass.pc(s, f) in NATURAL_PCS }.map { (s, f) ->
            BoardMarker(s, f, pcSharp(Bass.pc(s, f)).name(n), if (f == 0 || f == 12) Role.ROOT else Role.OTHER, if (f == 0 || f == 12) MarkerStyle.STRONG else MarkerStyle.SOFT)
        }

    fun allNotes(n: Notation, maxFret: Int): List<BoardMarker> =
        positions(maxFret) { _, _ -> true }.map { (s, f) ->
            val pc = Bass.pc(s, f)
            BoardMarker(s, f, pcSharp(pc).name(n), if (pc in NATURAL_PCS) Role.OTHER else null, if (pc in NATURAL_PCS) MarkerStyle.SOFT else MarkerStyle.GHOST)
        }
}
