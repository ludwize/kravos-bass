package net.kravos.bass.music

sealed class Block {
    /** Light markup: **bold**, lines starting with "• " are bullets. */
    data class Text(val text: String) : Block()
    class Board(val caption: String, val frets: Int = 12, val build: (Notation) -> List<BoardMarker>) : Block()
    data class Tip(val text: String) : Block()
}

data class Lesson(
    val id: String,
    val unit: String,
    val title: String,
    val goal: String,
    val blocks: List<Block>,
    val quiz: List<QuizKind>,
    val naturalsOnly: Boolean = false,
    val quizMaxFret: Int = 12,
    val quizStrings: List<Int> = listOf(0, 1, 2, 3),
    /** Optional jam to try right after the lesson: progression name. */
    val jam: String? = null,
)

private fun k(t: String, m: Mode) = Key(Spelled.parse(t), m)
private fun ch(r: String, q: ChordQuality) = Chord(Spelled.parse(r), q)

object Lessons {
    val ALL: List<Lesson> = listOf(
        // ================= 1. VRAT =================
        Lesson(
            "neck1", "1 · Vrat", "Prirodne note na E i A žici",
            "Da bez brojanja znaš gde su C D E F G A H na dve donje žice.",
            listOf(
                Block.Text(
                    "Sve na basu kreće od korena akorda, a koreni su skoro uvek na **E i A žici**. Zato prvo njih.\n\n" +
                        "Prirodne note idu: **C D E F G A H**, pa opet C. Između svake dve note je **stepen** (2 praga), osim između **E–F** i **H–C** gde je **polustepen** (1 prag).",
                ),
                Block.Board("Prirodne note na E i A žici", 12) { n -> Boards.naturals(n, 12, listOf(0, 1)) },
                Block.Text(
                    "Orijentiri koje vredi zapamtiti napamet:\n" +
                        "• E žica: **3 = G, 5 = A, 7 = H, 8 = C, 10 = D, 12 = E**\n" +
                        "• A žica: **3 = C, 5 = D, 7 = E, 8 = F, 10 = G, 12 = A**\n" +
                        "• Tačke na vratu (3, 5, 7, 9, 12) su tvoji putokazi — kad znaš notu na tački, do susedne stižeš za prag-dva.",
                ),
                Block.Tip("Vežba bez basa: kaži naglas „E žica, 7“ — „H“. Pet minuta dnevno i za nedelju dana ti je ovo refleks."),
            ),
            listOf(QuizKind.NOTE_NAME, QuizKind.FIND_NOTE),
            naturalsOnly = true, quizStrings = listOf(0, 1),
        ),
        Lesson(
            "neck2", "1 · Vrat", "Povisilice, snizilice i cela četiri žice",
            "Svih 12 nota na svim žicama, i zašto se ista nota zove dva imena.",
            listOf(
                Block.Text(
                    "Prag desno = **povisilica (#)**, prag levo = **snizilica (b)**. Zato je nota između F i G i **F#** i **Gb** — isti prag, dva imena.\n\n" +
                        "U našoj (nemačkoj) notaciji: **H** je „obično“ si, a **B** je si-bemol (Hb). Ako gledaš engleske tabove, njihovo „B“ je naše H, a njihovo „Bb“ je naše B.",
                ),
                Block.Board("Svih 12 nota (prirodne svetlije)", 12) { n -> Boards.allNotes(n, 12) },
                Block.Text(
                    "Trik za D i G žicu: one su **kvarta iznad** žice ispod (kao i A iznad E). Dakle 5. prag svake žice = prazna žica iznad nje.\n" +
                        "• G žica: 2 = A, 4 = H, 5 = C, 7 = D, 9 = E\n" +
                        "• D žica: 2 = E, 3 = F, 5 = G, 7 = A, 9 = H, 10 = C",
                ),
            ),
            listOf(QuizKind.NOTE_NAME, QuizKind.FIND_NOTE),
        ),
        Lesson(
            "neck3", "1 · Vrat", "Oktave — ista nota, drugi sprat",
            "Da svaku notu nađeš na bar dva mesta, za čas.",
            listOf(
                Block.Text(
                    "**Oktava** je ista nota, duplo viša frekvencija. Oblik na basu je uvek isti: **dve žice gore, dva praga desno**.\n\n" +
                        "To je i najlakši način da „premestiš“ koren kad ti je nezgodno: A na 5. pragu E žice = A na 7. pragu D žice.",
                ),
                Block.Board("A na E žici (5) i njegova oktava (D žica, 7)") { _ ->
                    listOf(BoardMarker(0, 5, "A", Role.ROOT), BoardMarker(2, 7, "A", Role.ROOT), BoardMarker(1, 0, "A", Role.ROOT, MarkerStyle.SOFT), BoardMarker(3, 2, "A", Role.ROOT, MarkerStyle.SOFT))
                },
                Block.Tip("Metal klasik: koren pa oktava, naizmenično, osmine. Pola Maiden „galopa“ i disco-rok linija je samo to."),
            ),
            listOf(QuizKind.OCTAVE, QuizKind.NOTE_NAME),
            quizMaxFret = 12,
        ),

        // ================= 2. INTERVALI =================
        Lesson(
            "int1", "2 · Intervali", "Intervali = oblici pod prstima",
            "Da interval vidiš kao oblik na vratu, ne kao broj.",
            listOf(
                Block.Text(
                    "Interval je razmak između dve note. Na basu je super to što je **svaki interval uvek isti oblik**, od bilo kog korena (bez praznih žica). Nauči oblike — ne note.\n\n" +
                        "Najbitniji za bas:\n" +
                        "• **Kvinta (5)** — žica gore, 2 praga desno. Tvoj najbolji drug.\n" +
                        "• **Oktava (8)** — dve žice gore, 2 praga desno.\n" +
                        "• **Kvarta (4)** — isti prag, žica gore.\n" +
                        "• **Velika terca (3)** — žica gore, prag levo. Dur zvuk.\n" +
                        "• **Mala terca (b3)** — žica gore, 2 praga levo (ili 3 praga desno na istoj žici). Mol zvuk.\n" +
                        "• **Mala septima (b7)** — dve žice gore, isti prag.",
                ),
                Block.Board("Oblici od korena C (A žica, 3. prag)") { _ ->
                    listOf(
                        BoardMarker(1, 3, "R", Role.ROOT),
                        BoardMarker(2, 1, "b3", Role.THIRD), BoardMarker(2, 2, "3", Role.THIRD),
                        BoardMarker(2, 3, "4", Role.OTHER), BoardMarker(2, 5, "5", Role.FIFTH),
                        BoardMarker(3, 3, "b7", Role.SEVENTH), BoardMarker(3, 4, "7", Role.SEVENTH), BoardMarker(3, 5, "8", Role.ROOT),
                        BoardMarker(0, 3, "5", Role.FIFTH, MarkerStyle.SOFT),
                    )
                },
                Block.Text("Primeti kvintu i ispod korena (E žica, isti prag) — „5 dole“ je jednako korisna kao gore: koren–kvinta dole je bas osnova kantrija, panka i polovine rokenrola."),
            ),
            listOf(QuizKind.INTERVAL_TAP, QuizKind.INTERVAL_NAME),
            quizMaxFret = 12,
        ),
        Lesson(
            "int2", "2 · Intervali", "Dur i mol lestvica",
            "Formula lestvice i zašto je mol „tužan“.",
            listOf(
                Block.Text(
                    "**Durska lestvica**: stepen-stepen-**pola**-stepen-stepen-stepen-**pola** → 1 2 3 4 5 6 7\n" +
                        "**Prirodni mol**: 1 2 **b3** 4 5 **b6 b7** — tri note niže nego dur.\n\n" +
                        "Ti ne moraš da sviraš lestvice gore-dole. Bitno je da znaš **koje note pripadaju pesmi**, pa da biraš između njih.",
                ),
                Block.Board("E prirodni mol — prsti u jednoj poziciji (0–5)", 7) { n -> Boards.scale(Boards.keyScale(k("E", Mode.MINOR)), n, 7, labelNotes = false) },
                Block.Text("E-mol je **metal tonalitet broj jedan** jer je najniža nota basa (prazna E) tonika. Ovo gore ti je cela „mapa“ za pola repertoara."),
                Block.Board("G-dur — iste note kao E-mol!", 7) { n -> Boards.scale(Boards.keyScale(k("G", Mode.MAJOR)), n, 7, labelNotes = false) },
                Block.Tip("G-dur i E-mol imaju potpuno iste note — to su relativni tonaliteti (lekcija 4). Razlikuje se samo koja nota je „kuća“."),
            ),
            listOf(QuizKind.INTERVAL_NAME, QuizKind.PENTA),
        ),

        // ================= 3. AKORDI =================
        Lesson(
            "ch1", "3 · Akordi", "Trozvuk: koren, terca, kvinta",
            "Svaki akord je R – 3 – 5, a terca odlučuje dur ili mol.",
            listOf(
                Block.Text(
                    "Gitarista svira akord, ti sviraš **tonove tog akorda** — to je cela tajna „lepih“ bas linija.\n\n" +
                        "• **Dur** = R – 3 – 5 (velika terca)\n" +
                        "• **Mol** = R – b3 – 5 (mala terca)\n" +
                        "• **Power akord (5)** = R – 5, bez terce. Metal ga obožava jer nije ni dur ni mol — zato preko power akorda **koren, kvinta i oktava** uvek rade.",
                ),
                Block.Board("A-mol: R (crveno), b3 (plavo), 5 (zeleno)") { n -> Boards.chord(ch("A", ChordQuality.MIN), n, 12) },
                Block.Board("A-dur: samo se terca pomeri za prag") { n -> Boards.chord(ch("A", ChordQuality.MAJ), n, 12) },
                Block.Tip("Kad ne znaš da li je akord dur ili mol — sviraj samo R, 5 i 8. Nikad ne greše."),
            ),
            listOf(QuizKind.CHORD_SPELL, QuizKind.CHORD_TAP),
        ),
        Lesson(
            "ch2", "3 · Akordi", "Septakordi: 7, m7, maj7",
            "Četvrta nota koja daje boju — i jako dobra „prolazna“ nota.",
            listOf(
                Block.Text(
                    "Dodaš septimu na trozvuk:\n" +
                        "• **7 (dominantni)** = R 3 5 **b7** — blues, rokenrol, AC/DC\n" +
                        "• **m7** = R b3 5 **b7** — mol sa „mekšim“ zvukom, funk\n" +
                        "• **maj7** = R 3 5 **7** — balade, džez, „sanjarski“\n\n" +
                        "Za tebe je najkorisnija **b7**: dve žice gore, isti prag. R – 5 – b7 – 8 je osnova svakog blues/boogie bas obrasca.",
                ),
                Block.Board("A7 — R 3 5 b7") { n -> Boards.chord(ch("A", ChordQuality.DOM7), n, 12) },
                Block.Tip("Boogie obrazac preko A7: A – C# – E – F# – G – F# – E – C#  (R 3 5 6 b7 6 5 3). Probaj u Džem modu na 12-taktnom bluesu."),
            ),
            listOf(QuizKind.CHORD_TONE, QuizKind.CHORD_SPELL, QuizKind.CHORD_TAP),
            jam = "12-taktni blues",
        ),

        // ================= 4. TONALITET =================
        Lesson(
            "key1", "4 · Tonalitet", "Akordi u tonalitetu (rimski brojevi)",
            "Kad znaš tonalitet, znaš i koji akordi mogu da dođu.",
            listOf(
                Block.Text(
                    "Na svakom stepenu lestvice gradiš akord samo od nota te lestvice. Ispadne uvek isti raspored:\n\n" +
                        "**Dur:** I – ii – iii – IV – V – vi – vii°\n" +
                        "**Mol:** i – ii° – III – iv – v – VI – VII\n\n" +
                        "Velika slova = dur, mala = mol, ° = smanjeni.\n\n" +
                        "Primer **E-mol**: Em – F#° – G – Am – Hm – C – D\n" +
                        "Primer **A-dur**: A – Hm – C#m – D – E – F#m – G#°\n\n" +
                        "Zašto je ovo zlatno: muzičari pričaju „i – VI – VII“ umesto imena akorda, jer **isti obrazac radi u svakom tonalitetu**. Naučiš oblik jednom, sviraš ga svuda.",
                ),
                Block.Board("Koreni akorda E-mola (brojevi = stepen)", 10) { _ ->
                    val kk = k("E", Mode.MINOR)
                    val dia = kk.diatonic()
                    Boards.positions(10) { s, f -> s <= 1 && dia.any { it.chord.root.pc == Bass.pc(s, f) } }.map { (s, f) ->
                        val d = dia.first { it.chord.root.pc == Bass.pc(s, f) }
                        BoardMarker(s, f, d.roman, if (d.degree == 0) Role.ROOT else Role.OTHER, if (d.degree == 0) MarkerStyle.STRONG else MarkerStyle.SOFT)
                    }
                },
            ),
            listOf(QuizKind.KEY_DEGREE, QuizKind.KEY_FUNCTION, QuizKind.KEY_MEMBER),
        ),
        Lesson(
            "key2", "4 · Tonalitet", "Kako da prepoznaš tonalitet pesme",
            "Praktični trikovi kad ti niko ne kaže „u E-molu je“.",
            listOf(
                Block.Text(
                    "Redom, od najpouzdanijeg:\n" +
                        "• **Akord na kome se pesma završava** (i na kom se refren „odmara“) je skoro uvek tonika.\n" +
                        "• **Prvi akord** je tonika u većini rok pesama.\n" +
                        "• Ako je glavni riff na praznoj E (ili spuštenoj) — verovatno E-mol / E-frigijski.\n" +
                        "• Pogledaj sve akorde: ako se uklapaju u jedan dur/mol par (npr. G, C, D, Em, Am), to je taj par. Onda odluči da li je „kuća“ dur ili mol.\n\n" +
                        "**Relativni tonaliteti**: svaki dur ima mol sa istim notama — **mala terca niže** (G-dur ↔ E-mol, C-dur ↔ A-mol, D-dur ↔ H-mol). Note su iste, ali tonika (i tvoj „sigurni koren“) nije.",
                ),
                Block.Tip("Rok i metal vole da „pozajmljuju“: bVII u duru (D u E-duru) ili dur V u molu. Ako jedan akord ne štima sa ostalima — to je pozajmica, ne drugi tonalitet."),
            ),
            listOf(QuizKind.RELATIVE, QuizKind.KEY_MEMBER, QuizKind.KEY_FUNCTION),
        ),
        Lesson(
            "key3", "4 · Tonalitet", "Kvintni krug, na brzinu",
            "Zašto su E, A, D, G „gitarski“ tonaliteti i kako ide I – IV – V.",
            listOf(
                Block.Text(
                    "Idi kvintu gore od C: **C → G → D → A → E → H → F#**. Svaki sledeći dur ima jednu povisilicu više. Na drugu stranu (kvarte): **C → F → B → Es → As**, jednu snizilicu više.\n\n" +
                        "Za bas je bitnije ovo: **IV i V su susedi tonike u krugu**. Na vratu: IV = isti prag žica gore, V = žica gore + 2 praga. Zato I–IV–V „pada pod prste“ u svakom tonalitetu.",
                ),
                Block.Board("I – IV – V u A-duru: A, D, E") { _ ->
                    listOf(BoardMarker(0, 5, "I", Role.ROOT), BoardMarker(1, 5, "IV", Role.OTHER), BoardMarker(1, 7, "V", Role.FIFTH), BoardMarker(0, 0, "V", Role.FIFTH, MarkerStyle.SOFT))
                },
            ),
            listOf(QuizKind.KEY_DEGREE, QuizKind.INTERVAL_TAP),
            jam = "Rok I–IV–V",
        ),

        // ================= 5. IMPROVIZACIJA =================
        Lesson(
            "imp1", "5 · Improvizacija", "Sigurne note: R – 5 – 8",
            "Prvi korak improvizacije: nikad pogrešna nota.",
            listOf(
                Block.Text(
                    "Improvizacija na basu nije sviranje brzih solaža — to je **biranje nota preko akorda koji se menjaju**. Počni od toga da nikad ne promašiš:\n\n" +
                        "• Na **prvu dobu** svakog akorda: **koren**.\n" +
                        "• Ostalo: **kvinta** (gore ili dole) i **oktava**.\n" +
                        "• Ritam ti je bitniji od nota — drži se bubnja (kik!).\n\n" +
                        "Kad ovo radiš bez razmišljanja preko bilo koje progresije, već improvizuješ.",
                ),
                Block.Board("R – 5 – 8 oblik (pomera se zajedno sa korenom)", 10) { _ ->
                    listOf(BoardMarker(0, 5, "R", Role.ROOT), BoardMarker(1, 7, "5", Role.FIFTH), BoardMarker(2, 7, "8", Role.ROOT), BoardMarker(0, 0, "5", Role.FIFTH, MarkerStyle.SOFT))
                },
                Block.Tip("U Džem modu stavi „Epski metal“ u E-molu na 90 BPM. Prvo samo koren u osminama, pa dodaj kvintu i oktavu."),
            ),
            listOf(QuizKind.CHORD_TAP, QuizKind.SAFE_NOTE),
            jam = "Epski metal",
        ),
        Lesson(
            "imp2", "5 · Improvizacija", "Tonovi akorda + prolazne note",
            "Kako da povežeš akorde da linija „hoda“.",
            listOf(
                Block.Text(
                    "Pravilo koje koriste svi basisti: **na jaku dobu ton akorda, između — šta god vodi do sledećeg tona akorda**.\n\n" +
                        "Prolazne note koje uvek rade:\n" +
                        "• **Note iz lestvice** između dva tona akorda (npr. R → 2 → b3).\n" +
                        "• **Hromatski prilaz**: prag ispod (ili iznad) korena SLEDEĆEG akorda, na poslednju dobu. Zvuči „namerno“ čak i kad je van lestvice, jer odmah razrešava.\n" +
                        "• **Kvinta sledećeg akorda** pa njegov koren — staro kao rokenrol.\n\n" +
                        "Primer u E-molu, Em pa C:\n" +
                        "• E – G – H – **H** | **C** — kvinta Em (H) je prag ispod korena C, savršen prilaz.\n" +
                        "• E – E – D – **C#** | **D** — ka D akordu: C# nije u E-molu, ali traje osminu i odmah razrešava na D, pa zvuči namerno.",
                ),
                Block.Board("Em (jako) + ostale note E-mola (slabo)") { n ->
                    Boards.chordOver(ch("E", ChordQuality.MIN), Boards.keyScale(k("E", Mode.MINOR)), n, 12, false)
                },
                Block.Tip("U Džem modu uključi prikaz „Akord + lestvica“: jake tačke su sletišta, slabe su stepenice između njih."),
            ),
            listOf(QuizKind.SAFE_NOTE, QuizKind.CHORD_TONE),
            jam = "Andaluzijska",
        ),
        Lesson(
            "imp3", "5 · Improvizacija", "Pentatonika: jedna mapa za celu pesmu",
            "Kad nemaš vremena da pratiš svaki akord.",
            listOf(
                Block.Text(
                    "**Mol pentatonika** = 1 – b3 – 4 – 5 – b7. Pet nota, nema „opasnih“ polustepena, pa skoro sve zvuči dobro preko celog mol tonaliteta.\n" +
                        "**Dur pentatonika** = 1 – 2 – 3 – 5 – 6 (iste note kao relativna mol pentatonika).\n" +
                        "**Blues lestvica** = mol pentatonika + **b5**. Rok/metal začin broj jedan.\n\n" +
                        "Dva načina razmišljanja — oba su ti potrebna:\n" +
                        "• **Preko cele pesme** jedna pentatonika tonike → brzo, sigurno, za fill-ove.\n" +
                        "• **Po akordu** (tonovi akorda) → melodičnije, „pametnije“, za linije.",
                ),
                Block.Board("E mol pentatonika, 0–12") { n ->
                    Boards.scale(k("E", Mode.MINOR).pentatonic, n, 12, labelNotes = false)
                },
                Block.Board("A blues lestvica (5. prag) — b5 je ljubičasta zona") { n ->
                    Boards.scale(k("A", Mode.MINOR).blues, n, 10, labelNotes = false).map { if (it.label == "b5") it.copy(role = Role.SEVENTH) else it }
                },
                Block.Tip("Fill na kraju fraze: poslednja 2 udarca pre novog dela — pentatonika naniže do korena. To je 80% metal bas fill-ova."),
            ),
            listOf(QuizKind.PENTA, QuizKind.SAFE_NOTE),
            jam = "Mol rok",
        ),
        Lesson(
            "imp4", "5 · Improvizacija", "Metal specifično: b2, power akordi, galop",
            "Boje koje čuješ kod Metallice, Slayera, Sepulture i Sabbatha.",
            listOf(
                Block.Text(
                    "• **Frigijska b2**: tonika pa prag iznad (E → F). To je onaj preteći zvuk. Ako je u pesmi akord F (ili F5) preko E-mola — sviraš frigijski; tada je i u tvojoj lestvici F umesto F#.\n" +
                        "• **Tritonus (b5)**: E → Bb. Sabbath „Black Sabbath“ zvuk. Koristi kao prolaz, ne sletište.\n" +
                        "• **Power akordi** su samo R + 5 → ti si slobodan: koren, kvinta, oktava, plus b3 i b7 iz mol pentatonike skoro uvek rade.\n" +
                        "• **Galop** (Maiden): osmina + dve šesnaestine na istoj noti. Tehnika, ne teorija — ali kad ga sviraš na tonovima akorda, odjednom zvučiš kao Steve Harris.",
                ),
                Block.Board("E frigijski: kao E-mol, ali b2 (F) umesto F#", 8) { n ->
                    val notes = listOf(Interval.R, Interval.m2, Interval.m3, Interval.P4, Interval.P5, Interval.m6, Interval.m7).map { it to (Spelled.parse("E") + it) }
                    Boards.scale(notes, n, 8, labelNotes = false).map { if (it.label == "b2") it.copy(role = Role.SEVENTH, style = MarkerStyle.STRONG) else it }
                },
                Block.Tip("Za improvizaciju preko metal riffa: prati KORENE riffa (gitaristove najniže note) i ubacuj oktave/kvinte. Kad riff stoji, ti možeš da se „šetaš“ pentatonikom."),
            ),
            listOf(QuizKind.PENTA, QuizKind.SAFE_NOTE, QuizKind.KEY_MEMBER),
            jam = "Frigijski riff",
        ),
    )

    fun byId(id: String) = ALL.firstOrNull { it.id == id }
}
