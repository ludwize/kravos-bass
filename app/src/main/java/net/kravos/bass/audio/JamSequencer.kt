package net.kravos.bass.audio

import net.kravos.bass.music.Chord
import net.kravos.bass.music.mod12

data class JamPosition(
    val countIn: Boolean,
    /** Beat within the count-in or within the current chord's span. */
    val beat: Int,
    val beatsInChord: Int,
    val chordIndex: Int,
    val nextChordIndex: Int,
    /** Beat within the bar (0..3). */
    val barBeat: Int,
)

/**
 * Backing band WITHOUT bass (you're the bass player): drums + chord pad, looping a progression.
 * Timeline is deterministic from [startFrame] so the UI can compute what is heard from the playback head.
 */
class JamSequencer(
    bpm: Int,
    private val chords: List<Chord>,
    /** Length of each chord in beats. */
    private val beats: List<Int>,
    private val drums: Boolean,
    private val pad: Boolean,
    private val countInBeats: Int = 4,
) : BlockListener {
    private val framesPerBeat: Double = AudioEngine.SR * 60.0 / bpm
    private val loopBeats = beats.sum()
    private val chordStartBeat: IntArray = IntArray(beats.size).also { arr ->
        var acc = 0
        for (i in beats.indices) { arr[i] = acc; acc += beats[i] }
    }

    @Volatile var startFrame: Long = -1
        private set
    private var nextHalfBeat = 0L // counts eighth notes since start

    override fun onBlock(startFrame: Long, n: Int, spawn: (Voice, Int) -> Unit) {
        if (this.startFrame < 0) this.startFrame = startFrame + 512 // tiny lead-in
        val blockEnd = startFrame + n
        while (true) {
            val frame = this.startFrame + (nextHalfBeat * framesPerBeat / 2.0).toLong()
            if (frame >= blockEnd) break
            if (frame >= startFrame) trigger(nextHalfBeat, (frame - startFrame).toInt(), spawn)
            nextHalfBeat++
        }
    }

    private fun trigger(halfBeat: Long, offset: Int, spawn: (Voice, Int) -> Unit) {
        val onBeat = halfBeat % 2L == 0L
        val beat = (halfBeat / 2).toInt()
        if (beat < countInBeats) {
            if (onBeat) spawn(Click(if (beat == 0) 1760.0 else 1320.0), offset)
            return
        }
        val b = beat - countInBeats
        val inLoop = b % loopBeats
        val barBeat = b % 4
        if (drums) {
            spawn(Hat(if (onBeat) 0.2f else 0.12f), offset)
            if (onBeat) {
                if (barBeat == 0 || barBeat == 2) spawn(Kick(), offset)
                if (barBeat == 1 || barBeat == 3) spawn(Snare(), offset)
            } else if (barBeat == 2) {
                // little push on the "and" of 3
                spawn(Kick(0.55f), offset)
            }
        } else if (onBeat) {
            spawn(Click(if (barBeat == 0) 1760.0 else 1320.0, 0.25f), offset)
        }
        if (pad && onBeat) {
            val ci = chordStartBeat.indexOf(inLoop)
            if (ci >= 0) {
                val len = (beats[ci] * framesPerBeat).toInt() - 200
                spawn(Pad(voicing(chords[ci]), len), offset)
            }
        }
    }

    /** Close voicing of the chord between E3 and E4-ish, root lowest. */
    private fun voicing(c: Chord): List<Int> {
        val rootMidi = 52 + mod12(c.root.pc - 4) // E3..D#4
        return c.tones.map { (_, s) ->
            var m = rootMidi + mod12(s.pc - c.root.pc)
            if (m < rootMidi) m += 12
            m
        }
    }

    fun positionAt(frame: Long): JamPosition? {
        if (startFrame < 0 || frame < startFrame) return null
        val beat = ((frame - startFrame) / framesPerBeat).toInt()
        if (beat < countInBeats) return JamPosition(true, beat, countInBeats, 0, 0, beat % 4)
        val b = beat - countInBeats
        val inLoop = b % loopBeats
        var ci = beats.size - 1
        for (i in beats.indices) if (inLoop < chordStartBeat[i] + beats[i]) { ci = i; break }
        return JamPosition(false, inLoop - chordStartBeat[ci], beats[ci], ci, (ci + 1) % chords.size, b % 4)
    }
}
