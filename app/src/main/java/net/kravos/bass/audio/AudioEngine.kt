package net.kravos.bass.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin
import kotlin.math.tanh

/** Something that produces sound. render() adds into buf and returns false once finished. */
interface Voice {
    /** Frames to wait before starting (lets a sequencer place hits inside a block). */
    var delay: Int
    fun render(buf: FloatArray, from: Int, n: Int): Boolean
}

/** Called once per audio block so a sequencer can schedule voices with sample accuracy. */
interface BlockListener {
    fun onBlock(startFrame: Long, n: Int, spawn: (Voice, Int) -> Unit)
}

/**
 * Tiny software synth: a single streaming AudioTrack that mixes plucked bass notes,
 * a drum kit and a chord pad. Runs while the app is in the foreground.
 */
object AudioEngine {
    const val SR = 44100
    private const val BLOCK = 256

    @Volatile private var running = false
    private var thread: Thread? = null
    @Volatile private var track: AudioTrack? = null
    private val incoming = ConcurrentLinkedQueue<Voice>()
    @Volatile var listener: BlockListener? = null
    @Volatile private var framesWritten = 0L
    @Volatile var master = 0.9f

    fun start() {
        if (running) return
        running = true
        thread = Thread({ loop() }, "kravos-audio").apply {
            priority = Thread.MAX_PRIORITY
            start()
        }
    }

    fun stop() {
        running = false
        thread?.join(500)
        thread = null
    }

    /** Frame (in the same timeline as the sequencer) that is being heard right now. */
    fun playedFrame(): Long {
        val t = track ?: return 0
        return try {
            t.playbackHeadPosition.toLong() and 0xFFFFFFFFL
        } catch (_: Exception) {
            0
        }
    }

    fun currentWriteFrame(): Long = framesWritten

    fun play(v: Voice) {
        incoming.add(v)
    }

    fun pluck(midi: Int, velocity: Float = 0.9f) {
        play(Pluck(midiFreq(midi), velocity))
    }

    private fun loop() {
        val minBuf = AudioTrack.getMinBufferSize(SR, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT)
        val t = try {
            AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build(),
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setSampleRate(SR)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .build(),
                )
                .setBufferSizeInBytes(maxOf(minBuf, BLOCK * 2 * 8))
                .setTransferMode(AudioTrack.MODE_STREAM)
                .setPerformanceMode(AudioTrack.PERFORMANCE_MODE_LOW_LATENCY)
                .build()
        } catch (e: Exception) {
            running = false
            return
        }
        track = t
        framesWritten = 0
        t.play()
        val active = ArrayList<Voice>()
        val mix = FloatArray(BLOCK)
        val out = ShortArray(BLOCK)
        try {
            while (running) {
                while (true) active.add(incoming.poll() ?: break)
                listener?.onBlock(framesWritten, BLOCK) { v, offset ->
                    v.delay = offset
                    active.add(v)
                }
                java.util.Arrays.fill(mix, 0f)
                val it = active.iterator()
                while (it.hasNext()) {
                    val v = it.next()
                    var from = 0
                    if (v.delay > 0) {
                        if (v.delay >= BLOCK) {
                            v.delay -= BLOCK
                            continue
                        }
                        from = v.delay
                        v.delay = 0
                    }
                    if (!v.render(mix, from, BLOCK - from)) it.remove()
                }
                val g = master
                for (i in 0 until BLOCK) {
                    out[i] = (tanh(mix[i] * g) * 32000f).toInt().toShort()
                }
                t.write(out, 0, BLOCK)
                framesWritten += BLOCK
            }
        } catch (_: Exception) {
        } finally {
            try {
                t.stop()
            } catch (_: Exception) {
            }
            t.release()
            track = null
            running = false
        }
    }
}

fun midiFreq(midi: Int): Double = 440.0 * Math.pow(2.0, (midi - 69) / 12.0)

private val rnd = java.util.Random()

/** Karplus-Strong plucked string: surprisingly convincing bass for ~30 lines. */
class Pluck(freq: Double, private val vel: Float, private val sustain: Float = 1.6f) : Voice {
    override var delay = 0
    private val len = maxOf(2, (AudioEngine.SR / freq).toInt())
    private val ring = FloatArray(len)
    private var idx = 0
    private var t = 0
    private val total = (AudioEngine.SR * sustain).toInt()
    // Damping: lower notes ring longer naturally; keep it bass-like (a bit dark).
    private val damp = 0.996f

    init {
        // Fingerstyle-ish excitation: low-passed noise burst.
        var prev = 0f
        for (i in 0 until len) {
            val n = rnd.nextFloat() * 2f - 1f
            prev = prev + 0.35f * (n - prev)
            ring[i] = prev
        }
        // remove DC
        val avg = ring.average().toFloat()
        for (i in 0 until len) ring[i] = (ring[i] - avg) * 1.6f
    }

    override fun render(buf: FloatArray, from: Int, n: Int): Boolean {
        for (k in 0 until n) {
            val next = (idx + 1) % len
            val v = ring[idx]
            ring[idx] = damp * 0.5f * (v + ring[next])
            idx = next
            val env = if (t > total - 2000) maxOf(0f, (total - t) / 2000f) else 1f
            buf[from + k] += v * vel * env
            t++
            if (t >= total) return false
        }
        return true
    }
}

class Kick(private val vel: Float = 0.9f) : Voice {
    override var delay = 0
    private var t = 0
    private var phase = 0.0
    private val total = (AudioEngine.SR * 0.35).toInt()
    override fun render(buf: FloatArray, from: Int, n: Int): Boolean {
        for (k in 0 until n) {
            val s = t.toDouble() / AudioEngine.SR
            val f = 45.0 + 95.0 * exp(-s * 28.0)
            phase += 2 * PI * f / AudioEngine.SR
            buf[from + k] += (sin(phase) * exp(-s * 9.0) * vel).toFloat()
            t++
            if (t >= total) return false
        }
        return true
    }
}

class Snare(private val vel: Float = 0.55f) : Voice {
    override var delay = 0
    private var t = 0
    private val total = (AudioEngine.SR * 0.22).toInt()
    private var lp = 0f
    override fun render(buf: FloatArray, from: Int, n: Int): Boolean {
        for (k in 0 until n) {
            val s = t.toDouble() / AudioEngine.SR
            val noise = rnd.nextFloat() * 2f - 1f
            lp += 0.6f * (noise - lp)
            val body = sin(2 * PI * 185.0 * s) * exp(-s * 30.0) * 0.5
            buf[from + k] += ((lp * exp(-s * 18.0) * 0.7 + body) * vel).toFloat()
            t++
            if (t >= total) return false
        }
        return true
    }
}

class Hat(private val vel: Float = 0.18f, private val decay: Double = 55.0) : Voice {
    override var delay = 0
    private var t = 0
    private val total = (AudioEngine.SR * 0.08).toInt()
    private var prev = 0f
    override fun render(buf: FloatArray, from: Int, n: Int): Boolean {
        for (k in 0 until n) {
            val s = t.toDouble() / AudioEngine.SR
            val noise = rnd.nextFloat() * 2f - 1f
            val hp = noise - prev
            prev = noise
            buf[from + k] += (hp * exp(-s * decay) * vel).toFloat()
            t++
            if (t >= total) return false
        }
        return true
    }
}

class Click(private val freq: Double, private val vel: Float = 0.35f) : Voice {
    override var delay = 0
    private var t = 0
    private val total = (AudioEngine.SR * 0.05).toInt()
    override fun render(buf: FloatArray, from: Int, n: Int): Boolean {
        for (k in 0 until n) {
            val s = t.toDouble() / AudioEngine.SR
            buf[from + k] += (sin(2 * PI * freq * s) * exp(-s * 70.0) * vel).toFloat()
            t++
            if (t >= total) return false
        }
        return true
    }
}

/** Soft, slightly detuned saw pad for the chords (sits above the bass register). */
class Pad(midis: List<Int>, lengthFrames: Int, private val vel: Float = 0.07f) : Voice {
    override var delay = 0
    private val freqs = midis.flatMap { m -> val f = midiFreq(m); listOf(f * 0.997, f * 1.003) }
    private val phases = DoubleArray(freqs.size)
    private val total = lengthFrames
    private val attack = (AudioEngine.SR * 0.04).toInt()
    private val release = (AudioEngine.SR * 0.12).toInt()
    private var t = 0
    private var lp = 0f
    override fun render(buf: FloatArray, from: Int, n: Int): Boolean {
        for (k in 0 until n) {
            var s = 0.0
            for (i in freqs.indices) {
                phases[i] += freqs[i] / AudioEngine.SR
                if (phases[i] >= 1.0) phases[i] -= 1.0
                s += phases[i] * 2.0 - 1.0
            }
            lp += 0.08f * (s.toFloat() - lp)
            val env = when {
                t < attack -> t.toFloat() / attack
                t > total - release -> maxOf(0f, (total - t).toFloat() / release)
                else -> 1f
            }
            buf[from + k] += lp * env * vel
            t++
            if (t >= total) return false
        }
        return true
    }
}
