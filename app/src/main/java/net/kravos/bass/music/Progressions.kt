package net.kravos.bass.music

/** One chord of a progression, defined relative to the key's tonic so it transposes to any key. */
data class Step(val fromTonic: Interval, val quality: ChordQuality, val roman: String, val bars: Int = 1) {
    fun chord(key: Key) = Chord(key.tonic + fromTonic, quality)
}

data class Progression(
    val name: String,
    val mode: Mode,
    val steps: List<Step>,
    val note: String,
    /** Scale that works over the whole thing when you don't follow each chord. */
    val safeScale: SafeScale = SafeScale.PENTA,
) {
    val romanLine: String get() = steps.joinToString(" – ") { it.roman }
    val totalBars: Int get() = steps.sumOf { it.bars }
}

enum class SafeScale(val label: String) { PENTA("pentatonika"), SCALE("lestvica"), BLUES("blues lestvica") }

private fun s(i: Interval, q: ChordQuality, r: String, bars: Int = 1) = Step(i, q, r, bars)

object Progressions {
    private val I = Interval.R
    private val MAJ = ChordQuality.MAJ
    private val MIN = ChordQuality.MIN

    val ALL: List<Progression> = listOf(
        // --- mol (rock/metal) ---
        Progression(
            "Epski metal", Mode.MINOR,
            listOf(s(I, MIN, "i"), s(Interval.m6, MAJ, "VI"), s(Interval.m7, MAJ, "VII"), s(I, MIN, "i")),
            "Najčešći metal/hard rock potez (Maiden, Manowar…). U E-molu: Em – C – D – Em. Oblik na jednoj žici: VI je 4 praga ispod tonike, VII 2 praga ispod (E na 12. → C na 8., D na 10.).",
        ),
        Progression(
            "Mol rok", Mode.MINOR,
            listOf(s(I, MIN, "i"), s(Interval.m7, MAJ, "VII"), s(Interval.m6, MAJ, "VI"), s(Interval.m7, MAJ, "VII")),
            "Silazni i – VII – VI pa nazad. Klasika od 70-ih do danas. Mol pentatonika tonike radi preko svega.",
        ),
        Progression(
            "Mol kadenca", Mode.MINOR,
            listOf(s(I, MIN, "i"), s(Interval.P4, MIN, "iv"), s(Interval.P5, MIN, "v"), s(I, MIN, "i")),
            "i – iv – v: sva tri akorda su molska, svi u prirodnom molu. Dobro za vežbu „terca je mala svuda“.",
        ),
        Progression(
            "Mol sa dur V", Mode.MINOR,
            listOf(s(I, MIN, "i"), s(Interval.P4, MIN, "iv"), s(Interval.P5, MAJ, "V"), s(I, MIN, "i")),
            "Ista stvar, ali je V dur (harmonski mol) — vodica (velika terca od V) jako vuče nazad na toniku. Pazi tu jednu notu na V akordu!",
            SafeScale.SCALE,
        ),
        Progression(
            "Andaluzijska", Mode.MINOR,
            listOf(s(I, MIN, "i"), s(Interval.m7, MAJ, "VII"), s(Interval.m6, MAJ, "VI"), s(Interval.P5, MAJ, "V")),
            "i – VII – VI – V, silazni bas korak po korak. Odlična za vežbu tonova akorda jer se svaki akord menja za stepen.",
            SafeScale.SCALE,
        ),
        Progression(
            "Frigijski riff", Mode.MINOR,
            listOf(s(I, ChordQuality.POWER, "i5", 2), s(Interval.m2, ChordQuality.POWER, "bII5"), s(I, ChordQuality.POWER, "i5")),
            "Metallica/Slayer/Sepultura zvuk: tonika pa pola stepena gore (b2). b2 nije u prirodnom molu — to je frigijska boja. Preko power akorda sviraj korene, kvinte, oktave.",
            SafeScale.SCALE,
        ),
        Progression(
            "Pop u molu", Mode.MINOR,
            listOf(s(I, MIN, "i"), s(Interval.m6, MAJ, "VI"), s(Interval.m3, MAJ, "III"), s(Interval.m7, MAJ, "VII")),
            "i – VI – III – VII. Isti akordi kao I – V – vi – IV u relativnom duru, samo počinje od mola.",
        ),
        // --- dur ---
        Progression(
            "Rok I–IV–V", Mode.MAJOR,
            listOf(s(I, MAJ, "I"), s(Interval.P4, MAJ, "IV"), s(Interval.P5, MAJ, "V"), s(Interval.P4, MAJ, "IV")),
            "Osnova rokenrola (AC/DC, Status Quo…). Za IV i V: isti prag na sledećoj žici, pa još dva praga gore.",
        ),
        Progression(
            "12-taktni blues", Mode.MAJOR,
            listOf(
                s(I, ChordQuality.DOM7, "I7", 4), s(Interval.P4, ChordQuality.DOM7, "IV7", 2), s(I, ChordQuality.DOM7, "I7", 2),
                s(Interval.P5, ChordQuality.DOM7, "V7"), s(Interval.P4, ChordQuality.DOM7, "IV7"), s(I, ChordQuality.DOM7, "I7"), s(Interval.P5, ChordQuality.DOM7, "V7"),
            ),
            "Blues u duru, svi akordi dominantni (7). Preko svega radi blues lestvica tonike, a na svakom akordu R – 3 – 5 – 6 – b7 je klasičan walking/boogie obrazac.",
            SafeScale.BLUES,
        ),
        Progression(
            "Pop I–V–vi–IV", Mode.MAJOR,
            listOf(s(I, MAJ, "I"), s(Interval.P5, MAJ, "V"), s(Interval.M6, MIN, "vi"), s(Interval.P4, MAJ, "IV")),
            "„Četiri akorda“ — pola radio programa. Dur pentatonika tonike radi preko svega.",
        ),
        Progression(
            "50-te I–vi–IV–V", Mode.MAJOR,
            listOf(s(I, MAJ, "I"), s(Interval.M6, MIN, "vi"), s(Interval.P4, MAJ, "IV"), s(Interval.P5, MAJ, "V")),
            "Doo-wop krug. Dobra za vežbu prelaza: sviraj koren, pa hromatski priđi sledećem korenu u poslednjoj dobi.",
        ),
        Progression(
            "Miksolidijski rok", Mode.MAJOR,
            listOf(s(I, MAJ, "I"), s(Interval.m7, MAJ, "bVII"), s(Interval.P4, MAJ, "IV"), s(I, MAJ, "I")),
            "I – bVII – IV: „Sweet Home Alabama“ / „Back in Black“ tip zvuka. bVII nije u durskoj lestvici — tu radi mol/blues pentatonika tonike.",
            SafeScale.BLUES,
        ),
        Progression(
            "ii–V–I", Mode.MAJOR,
            listOf(s(Interval.M2, ChordQuality.MIN7, "ii7"), s(Interval.P5, ChordQuality.DOM7, "V7"), s(I, ChordQuality.MAJ7, "Imaj7", 2)),
            "Džez osnova, ali odlična vežba septakorda: m7 → 7 → maj7, samo jedna nota se menja po koraku.",
            SafeScale.SCALE,
        ),
    )

    fun forMode(mode: Mode) = ALL.filter { it.mode == mode }
}
