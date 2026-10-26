/*
 * Kravos Bas — music theory, quizzes and lessons for the web edition.
 * A straight port of the Android app's music/ package (Theory.kt, Progressions.kt, Quiz.kt, Boards.kt, Lessons.kt),
 * so the site and the app teach exactly the same thing. Pitch class: 0 = C … 11 = H.
 */
(function (root) {
  "use strict";

  const LETTER_PC = [0, 2, 4, 5, 7, 9, 11];
  const mod12 = (x) => ((x % 12) + 12) % 12;
  const NATURAL = new Set([0, 2, 4, 5, 7, 9, 11]);
  const accStr = (a) => ({ 0: "", 1: "#", 2: "x", "-1": "b", "-2": "bb" })[a] ?? "";

  // ------------------------------------------------------------------ notes

  class Sp {
    constructor(letter, acc) { this.l = letter; this.a = acc; }
    get pc() { return mod12(LETTER_PC[this.l] + this.a); }
    name(n) {
      if (n === "H" && this.l === 6) return { "-1": "B", "-2": "Bb", 0: "H", 1: "H#", 2: "Hx" }[this.a];
      return (n === "H" ? "CDEFGAH" : "CDEFGAB")[this.l] + accStr(this.a);
    }
    plus(iv) {
      const nl = (this.l + iv.steps) % 7;
      const target = mod12(this.pc + iv.semis);
      let a = target - LETTER_PC[nl];
      while (a > 6) a -= 12;
      while (a < -6) a += 12;
      return new Sp(nl, a);
    }
    eq(o) { return o && o.l === this.l && o.a === this.a; }
    static parse(s) {
      let l = "CDEFGAH".indexOf(s[0]);
      if (l < 0) l = "CDEFGAB".indexOf(s[0]);
      if (l < 0) throw new Error("bad note " + s);
      const a = { "": 0, "#": 1, x: 2, "##": 2, b: -1, bb: -2 }[s.slice(1)];
      if (a === undefined) throw new Error("bad accidental " + s);
      return new Sp(l, a);
    }
  }
  const P = (s) => Sp.parse(s);

  function pcName(pc, n) {
    pc = mod12(pc);
    if (pc === 10) return n === "H" ? "A#/B" : "A#/Bb";
    if (pc === 11) return n === "H" ? "H" : "B";
    return ["C", "C#/Db", "D", "D#/Eb", "E", "F", "F#/Gb", "G", "G#/Ab", "A"][pc];
  }
  const SHARP = [[0, 0], [0, 1], [1, 0], [1, 1], [2, 0], [3, 0], [3, 1], [4, 0], [4, 1], [5, 0], [6, -1], [6, 0]];
  const pcSharp = (pc) => new Sp(...SHARP[mod12(pc)]);

  // ------------------------------------------------------------------ intervals

  const ROLE = { ROOT: "root", THIRD: "third", FIFTH: "fifth", SEVENTH: "seventh", OTHER: "other" };
  function iv(id, semis, steps, short, long, acc) {
    const role = ["R", "P8"].includes(id) ? ROLE.ROOT : ["m3", "M3"].includes(id) ? ROLE.THIRD
      : ["P5", "TT", "A5"].includes(id) ? ROLE.FIFTH : ["m7", "M7", "d7"].includes(id) ? ROLE.SEVENTH : ROLE.OTHER;
    return { id, semis, steps, short, long, acc, role };
  }
  const I = {
    R: iv("R", 0, 0, "R", "prima (koren)", "primu (koren)"),
    m2: iv("m2", 1, 1, "b2", "mala sekunda", "malu sekundu"),
    M2: iv("M2", 2, 1, "2", "velika sekunda", "veliku sekundu"),
    m3: iv("m3", 3, 2, "b3", "mala terca", "malu tercu"),
    M3: iv("M3", 4, 2, "3", "velika terca", "veliku tercu"),
    P4: iv("P4", 5, 3, "4", "čista kvarta", "čistu kvartu"),
    TT: iv("TT", 6, 4, "b5", "smanjena kvinta (tritonus)", "smanjenu kvintu (tritonus)"),
    P5: iv("P5", 7, 4, "5", "čista kvinta", "čistu kvintu"),
    A5: iv("A5", 8, 4, "#5", "povećana kvinta", "povećanu kvintu"),
    m6: iv("m6", 8, 5, "b6", "mala seksta", "malu sekstu"),
    M6: iv("M6", 9, 5, "6", "velika seksta", "veliku sekstu"),
    d7: iv("d7", 9, 6, "bb7", "smanjena septima", "smanjenu septimu"),
    m7: iv("m7", 10, 6, "b7", "mala septima", "malu septimu"),
    M7: iv("M7", 11, 6, "7", "velika septima", "veliku septimu"),
    P8: iv("P8", 12, 7, "8", "oktava", "oktavu"),
  };
  const INTERVALS = Object.values(I);
  const QUIZ_INTERVALS = [I.m2, I.M2, I.m3, I.M3, I.P4, I.TT, I.P5, I.m6, I.M6, I.m7, I.M7, I.P8];

  // ------------------------------------------------------------------ chords

  const Q = {
    MAJ: { id: "MAJ", suffix: "", label: "dur", ivs: [I.R, I.M3, I.P5] },
    MIN: { id: "MIN", suffix: "m", label: "mol", ivs: [I.R, I.m3, I.P5] },
    DIM: { id: "DIM", suffix: "dim", label: "smanjeni", ivs: [I.R, I.m3, I.TT] },
    AUG: { id: "AUG", suffix: "aug", label: "povećani", ivs: [I.R, I.M3, I.A5] },
    POWER: { id: "POWER", suffix: "5", label: "power (kvinta)", ivs: [I.R, I.P5] },
    MAJ7: { id: "MAJ7", suffix: "maj7", label: "veliki septakord", ivs: [I.R, I.M3, I.P5, I.M7] },
    DOM7: { id: "DOM7", suffix: "7", label: "dominantni septakord", ivs: [I.R, I.M3, I.P5, I.m7] },
    MIN7: { id: "MIN7", suffix: "m7", label: "mol septakord", ivs: [I.R, I.m3, I.P5, I.m7] },
    HALFDIM: { id: "HALFDIM", suffix: "m7b5", label: "polusmanjeni", ivs: [I.R, I.m3, I.TT, I.m7] },
    DIM7: { id: "DIM7", suffix: "dim7", label: "smanjeni septakord", ivs: [I.R, I.m3, I.TT, I.d7] },
  };
  const QUALITIES = Object.values(Q);
  const EXPLORER = [Q.MAJ, Q.MIN, Q.POWER, Q.DOM7, Q.MIN7, Q.MAJ7, Q.HALFDIM, Q.DIM, Q.AUG, Q.DIM7];
  const formula = (q) => q.ivs.map((i) => i.short).join(" – ");

  class Chord {
    constructor(root, q) { this.root = root; this.q = q; }
    name(n) { return this.root.name(n) + this.q.suffix; }
    get tones() { return this.q.ivs.map((i) => [i, this.root.plus(i)]); }
    get pcs() { return new Set(this.tones.map((t) => t[1].pc)); }
    intervalOfPc(pc) { const t = this.tones.find((t) => t[1].pc === mod12(pc)); return t ? t[0] : null; }
  }

  // ------------------------------------------------------------------ keys

  const MODE = {
    MAJOR: { id: "MAJOR", label: "dur", pattern: [I.R, I.M2, I.M3, I.P4, I.P5, I.M6, I.M7] },
    MINOR: { id: "MINOR", label: "mol", pattern: [I.R, I.M2, I.m3, I.P4, I.P5, I.m6, I.m7] },
  };
  const ROMANS = ["I", "II", "III", "IV", "V", "VI", "VII"];
  function roman(d, q) {
    const r = ROMANS[d], lo = r.toLowerCase();
    return {
      MAJ: r, POWER: r, AUG: r + "+", MIN: lo, DIM: lo + "°", MAJ7: r + "maj7", DOM7: r + "7",
      MIN7: lo + "7", HALFDIM: lo + "ø7", DIM7: lo + "°7",
    }[q.id];
  }

  class Key {
    constructor(tonic, mode) { this.tonic = tonic; this.mode = mode; }
    name(n) { return this.tonic.name(n) + "-" + this.mode.label; }
    get scale() { return this.mode.pattern.map((i) => this.tonic.plus(i)); }
    get scalePcs() { return new Set(this.scale.map((s) => s.pc)); }
    get scaleIv() { return this.mode.pattern.map((i) => [i, this.tonic.plus(i)]); }
    get pentatonic() {
      const ivs = this.mode === MODE.MAJOR ? [I.R, I.M2, I.M3, I.P5, I.M6] : [I.R, I.m3, I.P4, I.P5, I.m7];
      return ivs.map((i) => [i, this.tonic.plus(i)]);
    }
    get blues() { return [I.R, I.m3, I.P4, I.TT, I.P5, I.m7].map((i) => [i, this.tonic.plus(i)]); }
    diatonic(sevenths = false) {
      const sc = this.scale;
      return sc.map((root, i) => {
        const t = mod12(sc[(i + 2) % 7].pc - root.pc), f = mod12(sc[(i + 4) % 7].pc - root.pc), s = mod12(sc[(i + 6) % 7].pc - root.pc);
        let q;
        if (!sevenths) q = t === 4 && f === 7 ? Q.MAJ : t === 3 && f === 7 ? Q.MIN : t === 3 && f === 6 ? Q.DIM : Q.AUG;
        else q = t === 4 && s === 11 ? Q.MAJ7 : t === 4 && s === 10 ? Q.DOM7 : t === 3 && f === 7 ? Q.MIN7 : Q.HALFDIM;
        return { degree: i, roman: roman(i, q), chord: new Chord(root, q) };
      });
    }
    get relative() {
      return this.mode === MODE.MAJOR ? new Key(this.tonic.plus(I.M6), MODE.MINOR) : new Key(this.tonic.plus(I.m3), MODE.MAJOR);
    }
    static all(mode) {
      const t = mode === MODE.MAJOR ? ["C", "G", "D", "A", "E", "H", "F#", "Db", "Ab", "Eb", "Hb", "F"]
        : ["A", "E", "H", "F#", "C#", "G#", "D#", "D", "G", "C", "F", "Hb"];
      return t.map((x) => new Key(P(x), mode));
    }
    static common() {
      return [["E", 1], ["A", 1], ["D", 1], ["H", 1], ["G", 1], ["F#", 1], ["E", 0], ["A", 0], ["G", 0], ["D", 0], ["C", 0], ["F", 0]]
        .map(([t, m]) => new Key(P(t), m ? MODE.MINOR : MODE.MAJOR));
    }
  }

  // ------------------------------------------------------------------ bass neck

  const Bass = {
    OPEN: [28, 33, 38, 43],
    NAMES: ["E", "A", "D", "G"],
    midi: (s, f) => Bass.OPEN[s] + f,
    pc: (s, f) => mod12(Bass.OPEN[s] + f),
  };

  // ------------------------------------------------------------------ progressions

  const st = (i, q, r, bars = 1) => ({ iv: i, q, roman: r, bars });
  const SAFE = { PENTA: "pentatonika", SCALE: "lestvica", BLUES: "blues lestvica" };
  const prog = (name, mode, steps, note, safe = "PENTA") => ({ name, mode, steps, note, safe });
  const PROGRESSIONS = [
    prog("Epski metal", MODE.MINOR, [st(I.R, Q.MIN, "i"), st(I.m6, Q.MAJ, "VI"), st(I.m7, Q.MAJ, "VII"), st(I.R, Q.MIN, "i")],
      "Najčešći metal/hard rock potez (Maiden, Manowar…). U E-molu: Em – C – D – Em. Oblik na jednoj žici: VI je 4 praga ispod tonike, VII 2 praga ispod (E na 12. → C na 8., D na 10.)."),
    prog("Mol rok", MODE.MINOR, [st(I.R, Q.MIN, "i"), st(I.m7, Q.MAJ, "VII"), st(I.m6, Q.MAJ, "VI"), st(I.m7, Q.MAJ, "VII")],
      "Silazni i – VII – VI pa nazad. Klasika od 70-ih do danas. Mol pentatonika tonike radi preko svega."),
    prog("Mol kadenca", MODE.MINOR, [st(I.R, Q.MIN, "i"), st(I.P4, Q.MIN, "iv"), st(I.P5, Q.MIN, "v"), st(I.R, Q.MIN, "i")],
      "i – iv – v: sva tri akorda su molska, svi u prirodnom molu. Dobro za vežbu „terca je mala svuda“."),
    prog("Mol sa dur V", MODE.MINOR, [st(I.R, Q.MIN, "i"), st(I.P4, Q.MIN, "iv"), st(I.P5, Q.MAJ, "V"), st(I.R, Q.MIN, "i")],
      "Ista stvar, ali je V dur (harmonski mol) — vodica (velika terca od V) jako vuče nazad na toniku. Pazi tu jednu notu na V akordu!", "SCALE"),
    prog("Andaluzijska", MODE.MINOR, [st(I.R, Q.MIN, "i"), st(I.m7, Q.MAJ, "VII"), st(I.m6, Q.MAJ, "VI"), st(I.P5, Q.MAJ, "V")],
      "i – VII – VI – V, silazni bas korak po korak. Odlična za vežbu tonova akorda jer se svaki akord menja za stepen.", "SCALE"),
    prog("Frigijski riff", MODE.MINOR, [st(I.R, Q.POWER, "i5", 2), st(I.m2, Q.POWER, "bII5"), st(I.R, Q.POWER, "i5")],
      "Metallica/Slayer/Sepultura zvuk: tonika pa pola stepena gore (b2). b2 nije u prirodnom molu — to je frigijska boja. Preko power akorda sviraj korene, kvinte, oktave.", "SCALE"),
    prog("Pop u molu", MODE.MINOR, [st(I.R, Q.MIN, "i"), st(I.m6, Q.MAJ, "VI"), st(I.m3, Q.MAJ, "III"), st(I.m7, Q.MAJ, "VII")],
      "i – VI – III – VII. Isti akordi kao I – V – vi – IV u relativnom duru, samo počinje od mola."),
    prog("Rok I–IV–V", MODE.MAJOR, [st(I.R, Q.MAJ, "I"), st(I.P4, Q.MAJ, "IV"), st(I.P5, Q.MAJ, "V"), st(I.P4, Q.MAJ, "IV")],
      "Osnova rokenrola (AC/DC, Status Quo…). Za IV i V: isti prag na sledećoj žici, pa još dva praga gore."),
    prog("12-taktni blues", MODE.MAJOR, [st(I.R, Q.DOM7, "I7", 4), st(I.P4, Q.DOM7, "IV7", 2), st(I.R, Q.DOM7, "I7", 2),
      st(I.P5, Q.DOM7, "V7"), st(I.P4, Q.DOM7, "IV7"), st(I.R, Q.DOM7, "I7"), st(I.P5, Q.DOM7, "V7")],
    "Blues u duru, svi akordi dominantni (7). Preko svega radi blues lestvica tonike, a na svakom akordu R – 3 – 5 – 6 – b7 je klasičan walking/boogie obrazac.", "BLUES"),
    prog("Pop I–V–vi–IV", MODE.MAJOR, [st(I.R, Q.MAJ, "I"), st(I.P5, Q.MAJ, "V"), st(I.M6, Q.MIN, "vi"), st(I.P4, Q.MAJ, "IV")],
      "„Četiri akorda“ — pola radio programa. Dur pentatonika tonike radi preko svega."),
    prog("50-te I–vi–IV–V", MODE.MAJOR, [st(I.R, Q.MAJ, "I"), st(I.M6, Q.MIN, "vi"), st(I.P4, Q.MAJ, "IV"), st(I.P5, Q.MAJ, "V")],
      "Doo-wop krug. Dobra za vežbu prelaza: sviraj koren, pa hromatski priđi sledećem korenu u poslednjoj dobi."),
    prog("Miksolidijski rok", MODE.MAJOR, [st(I.R, Q.MAJ, "I"), st(I.m7, Q.MAJ, "bVII"), st(I.P4, Q.MAJ, "IV"), st(I.R, Q.MAJ, "I")],
      "I – bVII – IV: „Sweet Home Alabama“ / „Back in Black“ tip zvuka. bVII nije u durskoj lestvici — tu radi mol/blues pentatonika tonike.", "BLUES"),
    prog("ii–V–I", MODE.MAJOR, [st(I.M2, Q.MIN7, "ii7"), st(I.P5, Q.DOM7, "V7"), st(I.R, Q.MAJ7, "Imaj7", 2)],
      "Džez osnova, ali odlična vežba septakorda: m7 → 7 → maj7, samo jedna nota se menja po koraku.", "SCALE"),
  ];
  const stepChord = (s, key) => new Chord(key.tonic.plus(s.iv), s.q);
  const romanLine = (p) => p.steps.map((s) => s.roman).join(" – ");

  // ------------------------------------------------------------------ boards (fretboard markers)
  // marker: { s: string 0=E..3=G, f: fret, label, role, style: strong|soft|ghost|question|target }

  const Boards = {
    positions(maxFret, pred, minFret = 0) {
      const out = [];
      for (let s = 0; s < 4; s++) for (let f = minFret; f <= maxFret; f++) if (pred(s, f)) out.push([s, f]);
      return out;
    },
    chord(ch, n, maxFret, labelNotes = false) {
      const pcs = ch.pcs;
      return Boards.positions(maxFret, (s, f) => pcs.has(Bass.pc(s, f))).map(([s, f]) => {
        const i = ch.intervalOfPc(Bass.pc(s, f));
        const note = ch.tones.find((t) => t[0] === i)[1];
        return { s, f, label: labelNotes ? note.name(n) : i.short, role: i.role, style: "strong" };
      });
    },
    scale(notes, n, maxFret, labelNotes, style = "soft") {
      const by = new Map(notes.map((t) => [t[1].pc, t]));
      return Boards.positions(maxFret, (s, f) => by.has(Bass.pc(s, f))).map(([s, f]) => {
        const [i, sp] = by.get(Bass.pc(s, f));
        const isR = i === I.R;
        return { s, f, label: labelNotes ? sp.name(n) : i.short, role: isR ? ROLE.ROOT : ROLE.OTHER, style: isR ? "strong" : style };
      });
    },
    chordOver(ch, bg, n, maxFret, labelNotes) {
      const strong = Boards.chord(ch, n, maxFret, labelNotes);
      const taken = new Set(strong.map((m) => m.s + ":" + m.f));
      const by = new Map(bg.map((t) => [t[1].pc, t]));
      const soft = Boards.positions(maxFret, (s, f) => !taken.has(s + ":" + f) && by.has(Bass.pc(s, f))).map(([s, f]) => {
        const sp = by.get(Bass.pc(s, f))[1];
        const semis = mod12(sp.pc - ch.root.pc);
        const i = INTERVALS.find((x) => x.semis === semis && x !== I.A5 && x !== I.d7) || I.R;
        return { s, f, label: labelNotes ? sp.name(n) : semis === 6 ? "#4" : i.short, role: ROLE.OTHER, style: "soft" };
      });
      return soft.concat(strong);
    },
    naturals(n, maxFret, strings = [0, 1, 2, 3]) {
      return Boards.positions(maxFret, (s, f) => strings.includes(s) && NATURAL.has(Bass.pc(s, f))).map(([s, f]) => {
        const edge = f === 0 || f === 12;
        return { s, f, label: pcSharp(Bass.pc(s, f)).name(n), role: edge ? ROLE.ROOT : ROLE.OTHER, style: edge ? "strong" : "soft" };
      });
    },
    allNotes(n, maxFret) {
      return Boards.positions(maxFret, () => true).map(([s, f]) => {
        const pc = Bass.pc(s, f), nat = NATURAL.has(pc);
        return { s, f, label: pcSharp(pc).name(n), role: nat ? ROLE.OTHER : null, style: nat ? "soft" : "ghost" };
      });
    },
  };

  // ------------------------------------------------------------------ quizzes

  const KINDS = [
    ["NOTE_NAME", "Nota na pragu", "Vidiš tačku na vratu — koja je nota?", "Vrat"],
    ["FIND_NOTE", "Nađi notu", "Tapni zadatu notu na zadatoj žici", "Vrat"],
    ["OCTAVE", "Oktave", "Nađi oktavu od zadate note", "Vrat"],
    ["INTERVAL_TAP", "Intervali na vratu", "Od korena tapni traženi interval", "Intervali"],
    ["INTERVAL_NAME", "Prepoznaj interval", "Dve tačke — koji je interval?", "Intervali"],
    ["CHORD_SPELL", "Sastav akorda", "Koje note čine akord?", "Akordi"],
    ["CHORD_TONE", "Ton akorda", "Koja je terca / kvinta / septima akorda?", "Akordi"],
    ["CHORD_TAP", "Tonovi akorda na vratu", "Od korena akorda nađi tercu, kvintu, septimu", "Akordi"],
    ["KEY_DEGREE", "Akordi tonaliteta", "Koji akord je na datom stepenu?", "Tonalitet"],
    ["KEY_FUNCTION", "Stepen akorda", "Koji stepen je dati akord u tonalitetu?", "Tonalitet"],
    ["KEY_MEMBER", "Uljez", "Koji akord ne pripada tonalitetu?", "Tonalitet"],
    ["RELATIVE", "Relativni tonalitet", "Dur ↔ mol parovi", "Tonalitet"],
    ["PENTA", "Pentatonika", "Koja nota je u pentatonici?", "Improvizacija"],
    ["SAFE_NOTE", "Sigurna nota", "Tonalitet + akord → koja nota sigurno leži", "Improvizacija"],
  ].map(([id, title, desc, group]) => ({ id, title, desc, group }));
  const KIND = Object.fromEntries(KINDS.map((k) => [k.id, k]));

  const rnd = (n) => Math.floor(Math.random() * n);
  const pick = (a) => a[rnd(a.length)];
  const shuffle = (a) => { a = a.slice(); for (let i = a.length - 1; i > 0; i--) { const j = rnd(i + 1); [a[i], a[j]] = [a[j], a[i]]; } return a; };
  const uniq = (a) => [...new Set(a)];
  const CHORD_ROOTS = ["C", "D", "E", "F", "G", "A", "H", "Hb", "Eb", "Ab", "F#", "C#", "Db", "G#"].map(P);

  function options(correct, pool, count = 4) {
    const all = shuffle(shuffle(uniq(pool.filter((x) => x !== correct))).slice(0, count - 1).concat([correct]));
    return [all, all.indexOf(correct)];
  }
  function randomPos(c, maxFret = c.maxFret, strings = c.strings) {
    for (;;) {
      const s = pick(strings), f = rnd(maxFret + 1);
      if (!c.naturals || NATURAL.has(Bass.pc(s, f))) return [s, f];
    }
  }
  const allPos = (c, pred) => Boards.positions(c.maxFret, pred);
  function randomKey(mode) {
    const m = mode || (Math.random() < 0.5 ? MODE.MAJOR : MODE.MINOR);
    return rnd(3) < 2 ? pick(Key.common().filter((k) => !mode || k.mode === m)) : pick(Key.all(m));
  }
  const randomChord = (qs) => new Chord(pick(CHORD_ROOTS), pick(qs));
  const lowMidi = (pc) => 40 + mod12(pc - 4);

  function shapeHint(i) {
    return {
      m2: "Prag pored korena.",
      M2: "Dva praga desno, ista žica.",
      m3: "3 praga desno na istoj žici, ili žica gore + 2 praga levo.",
      M3: "Žica gore, jedan prag levo.",
      P4: "Isti prag, žica gore.",
      TT: "Žica gore + 1 prag desno (između kvarte i kvinte).",
      P5: "Žica gore, dva praga desno — „power chord“ oblik.",
      m6: "Dve žice gore, dva praga levo (ili žica gore + 3 praga).",
      A5: "Dve žice gore, dva praga levo (ili žica gore + 3 praga).",
      M6: "Dve žice gore, jedan prag levo (ili žica gore + 4 praga).",
      m7: "Dve žice gore, isti prag.",
      M7: "Dve žice gore, jedan prag desno (pola stepena ispod oktave).",
      P8: "Dve žice gore, dva praga desno.",
    }[i.id] || "";
  }

  function explainNote(s, f, n) {
    const name = pcName(Bass.pc(s, f), n);
    let tip;
    if (f === 0) tip = "Prazna žica.";
    else if (f === 12) tip = "12. prag je oktava prazne žice.";
    else if (f > 12) tip = `Prag ${f} = prag ${f - 12} oktavu više.`;
    else if (f === 3 || f === 9) tip = `Orijentir: tačka na ${f}. pragu.`;
    else if (f === 5) tip = "Orijentir: 5. prag = nota prazne žice iznad.";
    else if (f === 7) tip = "Orijentir: 7. prag = kvinta prazne žice (ista nota kao prazna žica ispod, oktavu više).";
    else {
      const near = [0, 3, 5, 7, 9, 12].reduce((a, b) => (Math.abs(b - f) < Math.abs(a - f) ? b : a));
      tip = `Od ${near}. praga (${pcName(Bass.pc(s, near), n)}) ${f > near ? "+" : "−"}${Math.abs(f - near)} polustepen(a).`;
    }
    return `${name}. ${tip}`;
  }
  const roleWord = (i, acc) => ({ third: acc ? "tercu" : "terca", fifth: acc ? "kvintu" : "kvinta", seventh: acc ? "septimu" : "septima" })[i.role] || (acc ? i.acc : i.long);
  const keyLine = (k, n) => `${k.name(n)}: ` + k.diatonic().map((d) => `${d.roman} ${d.chord.name(n)}`).join("  ");

  // Question: { kind, prompt, markers, answer: {type:'choice', options, correct, cols} | {type:'tap', check(s,f), solution}, explain, sound }
  const GEN = {
    NOTE_NAME(c) {
      const [s, f] = randomPos(c), pc = Bass.pc(s, f);
      const opts = c.naturals ? [...NATURAL].sort((a, b) => a - b) : [...Array(12).keys()];
      return {
        prompt: `Koja je ovo nota? (${Bass.NAMES[s]} žica, ${f}. prag)`,
        markers: [{ s, f, label: "?", style: "question" }],
        answer: { type: "choice", options: opts.map((p) => pcName(p, c.n)), correct: opts.indexOf(pc), cols: c.naturals ? 4 : 3 },
        explain: explainNote(s, f, c.n), sound: [Bass.midi(s, f)],
      };
    },
    FIND_NOTE(c) {
      const s = pick(c.strings);
      const cand = [];
      for (let f = 0; f <= c.maxFret; f++) if (!c.naturals || NATURAL.has(Bass.pc(s, f))) cand.push(f);
      const f = pick(cand), pc = Bass.pc(s, f);
      const sol = [];
      for (let x = 0; x <= c.maxFret; x++) if (Bass.pc(s, x) === pc) sol.push({ s, f: x, label: pcSharp(pc).name(c.n), role: ROLE.OTHER, style: "target" });
      return {
        prompt: `Tapni ${pcName(pc, c.n)} na ${Bass.NAMES[s]} žici`, markers: [],
        answer: { type: "tap", check: (ss, ff) => ss === s && Bass.pc(ss, ff) === pc, solution: sol },
        explain: explainNote(s, f, c.n), sound: [Bass.midi(s, f)],
      };
    },
    OCTAVE(c) {
      for (;;) {
        const [s, f] = randomPos(c, Math.min(c.maxFret, 10), [0, 1]);
        const m = Bass.midi(s, f);
        const sol = allPos(c, (a, b) => Bass.midi(a, b) === m + 12);
        if (!sol.length) continue;
        const name = pcSharp(m).name(c.n);
        return {
          prompt: `Ovo je ${name}. Tapni oktavu više.`,
          markers: [{ s, f, label: name, role: ROLE.ROOT, style: "strong" }],
          answer: { type: "tap", check: (a, b) => Bass.midi(a, b) === m + 12, solution: sol.map(([a, b]) => ({ s: a, f: b, label: "8", role: ROLE.ROOT, style: "target" })) },
          explain: "Oblik oktave: dve žice gore, dva praga desno. (Ili ista žica + 12 pragova.)", sound: [m, m + 12],
        };
      }
    },
    INTERVAL_TAP(c) {
      for (;;) {
        const i = pick(QUIZ_INTERVALS);
        const [s, f] = randomPos(c, Math.min(c.maxFret, 9), [0, 1, 2]);
        const m = Bass.midi(s, f), target = m + i.semis;
        const sol = allPos(c, (a, b) => Bass.midi(a, b) === target && Math.abs(b - f) <= 5);
        if (!sol.length) continue;
        const root = pcSharp(m);
        return {
          prompt: `Koren je ${root.name(c.n)}. Tapni ${i.acc} (${i.short}) iznad njega.`,
          markers: [{ s, f, label: "R", role: ROLE.ROOT, style: "strong" }],
          answer: { type: "tap", check: (a, b) => Bass.midi(a, b) === target, solution: sol.map(([a, b]) => ({ s: a, f: b, label: i.short, role: i.role, style: "target" })) },
          explain: `${i.short} od ${root.name(c.n)} je ${root.plus(i).name(c.n)} — ${i.semis} polustepen(a). ${shapeHint(i)}`,
          sound: [m, target],
        };
      }
    },
    INTERVAL_NAME(c) {
      for (;;) {
        const i = pick(QUIZ_INTERVALS);
        const [s, f] = randomPos(c, Math.min(c.maxFret, 9), [0, 1, 2]);
        const m = Bass.midi(s, f);
        let sol = allPos(c, (a, b) => Bass.midi(a, b) === m + i.semis && Math.abs(b - f) <= 4 && a !== s);
        if (!sol.length) sol = allPos(c, (a, b) => Bass.midi(a, b) === m + i.semis && Math.abs(b - f) <= 5);
        if (!sol.length) continue;
        const [qs, qf] = pick(sol);
        const label = (x) => `${x.short} · ${x.long}`;
        const [opts, idx] = options(label(i), QUIZ_INTERVALS.map(label));
        return {
          prompt: "Koji je interval od R do „?“",
          markers: [{ s, f, label: "R", role: ROLE.ROOT, style: "strong" }, { s: qs, f: qf, label: "?", style: "question" }],
          answer: { type: "choice", options: opts, correct: idx, cols: 1 },
          explain: `${i.long}: ${i.semis} polustepen(a). ${shapeHint(i)}`, sound: [m, m + i.semis],
        };
      }
    },
    CHORD_SPELL(c) {
      const ch = randomChord([Q.MAJ, Q.MIN, Q.DOM7, Q.MIN7, Q.MAJ7, Q.POWER, Q.HALFDIM]);
      const spell = (x) => x.tones.map((t) => t[1].name(c.n)).join(" ");
      const correct = spell(ch);
      const pool = QUALITIES.filter((q) => q.ivs.length === ch.q.ivs.length && q !== ch.q).map((q) => spell(new Chord(ch.root, q)))
        .concat([spell(new Chord(ch.root.plus(I.m2), ch.q)), spell(new Chord(ch.root.plus(I.M2), ch.q))]);
      const [opts, idx] = options(correct, pool);
      return {
        prompt: `Koje note čine ${ch.name(c.n)}?`, markers: [],
        answer: { type: "choice", options: opts, correct: idx, cols: 1 },
        explain: `${ch.name(c.n)} (${ch.q.label}) = ${formula(ch.q)} → ${correct}`, sound: ch.tones.map((t) => lowMidi(t[1].pc)),
      };
    },
    CHORD_TONE(c) {
      const ch = randomChord([Q.MAJ, Q.MIN, Q.DOM7, Q.MIN7, Q.MAJ7]);
      const [i, note] = pick(ch.tones.slice(1));
      const correct = note.name(c.n);
      const pool = ch.tones.map((t) => t[1].name(c.n)).concat([I.M3, I.m3, I.P4, I.M6, I.m7, I.M7].map((x) => ch.root.plus(x).name(c.n)));
      const [opts, idx] = options(correct, pool);
      return {
        prompt: `Koja je ${roleWord(i, false)} u ${ch.name(c.n)}?`, markers: [],
        answer: { type: "choice", options: opts, correct: idx, cols: 4 },
        explain: `${ch.name(c.n)}: ` + ch.tones.map((t) => `${t[0].short}=${t[1].name(c.n)}`).join(" – "),
        sound: [lowMidi(ch.root.pc), lowMidi(ch.root.pc) + i.semis],
      };
    },
    CHORD_TAP(c) {
      for (;;) {
        const ch = randomChord([Q.MAJ, Q.MIN, Q.DOM7, Q.MIN7, Q.MAJ7]);
        const roots = allPos(c, (s, f) => s <= 1 && Bass.pc(s, f) === ch.root.pc && f >= 1 && f <= Math.min(c.maxFret, 10));
        if (!roots.length) continue;
        const [rs, rf] = pick(roots);
        const [i, note] = pick(ch.tones.slice(1));
        const rm = Bass.midi(rs, rf);
        const sol = allPos(c, (s, f) => Bass.pc(s, f) === note.pc && Math.abs(f - rf) <= 4 && Bass.midi(s, f) > rm);
        if (!sol.length) continue;
        return {
          prompt: `Akord ${ch.name(c.n)}, koren je označen. Tapni ${roleWord(i, true)} (${note.name(c.n)}).`,
          markers: [{ s: rs, f: rf, label: "R", role: ROLE.ROOT, style: "strong" }],
          answer: { type: "tap", check: (s, f) => Bass.pc(s, f) === note.pc, solution: sol.map(([s, f]) => ({ s, f, label: i.short, role: i.role, style: "target" })) },
          explain: `${i.short} od ${ch.root.name(c.n)} = ${note.name(c.n)}. ${shapeHint(i)}`, sound: [rm, rm + i.semis],
        };
      }
    },
    KEY_DEGREE(c) {
      const k = randomKey(), dia = k.diatonic(), t = pick(dia.slice(1));
      const correct = t.chord.name(c.n);
      const pool = dia.map((d) => d.chord.name(c.n)).concat([new Chord(t.chord.root, t.chord.q === Q.MAJ ? Q.MIN : Q.MAJ).name(c.n)]);
      const [opts, idx] = options(correct, pool);
      return { prompt: `${k.name(c.n)}: koji akord je ${t.roman}?`, markers: [], answer: { type: "choice", options: opts, correct: idx, cols: 4 }, explain: keyLine(k, c.n), sound: [] };
    },
    KEY_FUNCTION(c) {
      const k = randomKey(), dia = k.diatonic(), t = pick(dia.slice(1));
      const [opts, idx] = options(t.roman, dia.map((d) => d.roman));
      return {
        prompt: `Pesma je u ${k.name(c.n)}. Akord ${t.chord.name(c.n)} je koji stepen?`, markers: [],
        answer: { type: "choice", options: opts, correct: idx, cols: 4 },
        explain: keyLine(k, c.n) + "\nVelika slova = dur, mala = mol, ° = smanjeni.", sound: [],
      };
    },
    KEY_MEMBER(c) {
      const k = randomKey();
      const dia = k.diatonic().filter((d) => d.chord.q !== Q.DIM);
      const inKey = shuffle(dia).slice(0, 3).map((d) => d.chord.name(c.n));
      const names = new Set(k.diatonic().map((d) => d.chord.name(c.n)));
      const cands = dia.map((d) => new Chord(d.chord.root, d.chord.q === Q.MAJ ? Q.MIN : Q.MAJ))
        .concat([new Chord(k.tonic.plus(I.m2), Q.MAJ), new Chord(k.tonic.plus(I.TT), Q.MAJ)])
        .map((x) => x.name(c.n)).filter((x) => !names.has(x));
      const outsider = pick(cands);
      const all = shuffle(inKey.concat([outsider]));
      return { prompt: `Koji akord NE pripada tonalitetu ${k.name(c.n)}?`, markers: [], answer: { type: "choice", options: all, correct: all.indexOf(outsider), cols: 4 }, explain: keyLine(k, c.n), sound: [] };
    },
    RELATIVE(c) {
      const k = randomKey(), rel = k.relative, correct = rel.name(c.n);
      const [opts, idx] = options(correct, Key.all(rel.mode).map((x) => x.name(c.n)));
      const rule = k.mode === MODE.MAJOR ? "Relativni mol je na 6. stepenu dura (mala terca niže)." : "Relativni dur je mala terca iznad mola.";
      return {
        prompt: `Koji je relativni ${rel.mode === MODE.MINOR ? "mol" : "dur"} od ${k.name(c.n)}?`, markers: [],
        answer: { type: "choice", options: opts, correct: idx, cols: 2 },
        explain: `${correct} — iste note, isti akordi, drugi „centar“. ${rule}`, sound: [],
      };
    },
    PENTA(c) {
      const k = randomKey();
      const members = k.pentatonic.map((t) => t[1]);
      const mp = new Set(members.map((m) => m.pc));
      const correct = pick(members.slice(1)).name(c.n);
      const outs = [...Array(12).keys()].filter((p) => !mp.has(p)).map((p) => (k.scale.find((x) => x.pc === p) || pcSharp(p)).name(c.n));
      const all = shuffle(shuffle(outs).slice(0, 3).concat([correct]));
      return {
        prompt: `Koja nota JE u ${k.tonic.name(c.n)} ${k.mode.label} pentatonici?`, markers: [],
        answer: { type: "choice", options: all, correct: all.indexOf(correct), cols: 4 },
        explain: `${k.tonic.name(c.n)} ${k.mode.label} pentatonika: ` + k.pentatonic.map((t) => t[1].name(c.n)).join(" ") + " (" + k.pentatonic.map((t) => t[0].short).join(" ") + ")",
        sound: [],
      };
    },
    SAFE_NOTE(c) {
      const k = randomKey();
      const dc = pick(k.diatonic().filter((d) => d.chord.q !== Q.DIM));
      const ch = dc.chord, ct = pick(ch.tones), correct = ct[1].name(c.n), cp = ch.pcs;
      const inScale = k.scale.filter((x) => !cp.has(x.pc)).map((x) => x.name(c.n));
      const outside = [...Array(12).keys()].filter((p) => !k.scalePcs.has(p)).map((p) => pcSharp(p).name(c.n));
      const pool = uniq(shuffle(inScale).slice(0, 2).concat(shuffle(outside).slice(0, 2)));
      const all = shuffle(pool.slice(0, 3).concat([correct]));
      return {
        prompt: `Pesma u ${k.name(c.n)}, bend svira ${ch.name(c.n)} (${dc.roman}). Na koju notu možeš sigurno da staneš (ton akorda)?`, markers: [],
        answer: { type: "choice", options: all, correct: all.indexOf(correct), cols: 4 },
        explain: `${ch.name(c.n)} = ` + ch.tones.map((t) => `${t[0].short} ${t[1].name(c.n)}`).join(" – ") + ". Ostale note iz lestvice su dobre kao prolazne, ali na „jaku dobu“ stani na ton akorda.",
        sound: [],
      };
    },
  };
  function makeQuestion(kind, c) {
    const ctx = Object.assign({ n: "H", maxFret: 12, naturals: false, strings: [0, 1, 2, 3] }, c);
    const q = GEN[kind](ctx);
    q.kind = kind;
    return q;
  }

  // ------------------------------------------------------------------ lessons

  const K = (t, m) => new Key(P(t), m === "mol" ? MODE.MINOR : MODE.MAJOR);
  const C = (r, q) => new Chord(P(r), q);
  const txt = (t) => ({ type: "text", text: t });
  const tip = (t) => ({ type: "tip", text: t });
  const board = (caption, frets, build) => ({ type: "board", caption, frets, build });
  const M = (s, f, label, role, style = "strong") => ({ s, f, label, role, style });

  const LESSONS = [
    {
      id: "neck1", unit: "1 · Vrat", title: "Prirodne note na E i A žici", goal: "Da bez brojanja znaš gde su C D E F G A H na dve donje žice.",
      blocks: [
        txt("Sve na basu kreće od korena akorda, a koreni su skoro uvek na **E i A žici**. Zato prvo njih.\n\nPrirodne note idu: **C D E F G A H**, pa opet C. Između svake dve note je **stepen** (2 praga), osim između **E–F** i **H–C** gde je **polustepen** (1 prag)."),
        board("Prirodne note na E i A žici", 12, (n) => Boards.naturals(n, 12, [0, 1])),
        txt("Orijentiri koje vredi zapamtiti napamet:\n• E žica: **3 = G, 5 = A, 7 = H, 8 = C, 10 = D, 12 = E**\n• A žica: **3 = C, 5 = D, 7 = E, 8 = F, 10 = G, 12 = A**\n• Tačke na vratu (3, 5, 7, 9, 12) su tvoji putokazi — kad znaš notu na tački, do susedne stižeš za prag-dva."),
        tip("Vežba bez basa: kaži naglas „E žica, 7“ — „H“. Pet minuta dnevno i za nedelju dana ti je ovo refleks."),
      ],
      quiz: ["NOTE_NAME", "FIND_NOTE"], naturals: true, strings: [0, 1],
    },
    {
      id: "neck2", unit: "1 · Vrat", title: "Povisilice, snizilice i cela četiri žice", goal: "Svih 12 nota na svim žicama, i zašto se ista nota zove dva imena.",
      blocks: [
        txt("Prag desno = **povisilica (#)**, prag levo = **snizilica (b)**. Zato je nota između F i G i **F#** i **Gb** — isti prag, dva imena.\n\nU našoj (nemačkoj) notaciji: **H** je „obično“ si, a **B** je si-bemol (Hb). Ako gledaš engleske tabove, njihovo „B“ je naše H, a njihovo „Bb“ je naše B."),
        board("Svih 12 nota (prirodne svetlije)", 12, (n) => Boards.allNotes(n, 12)),
        txt("Trik za D i G žicu: one su **kvarta iznad** žice ispod (kao i A iznad E). Dakle 5. prag svake žice = prazna žica iznad nje.\n• G žica: 2 = A, 4 = H, 5 = C, 7 = D, 9 = E\n• D žica: 2 = E, 3 = F, 5 = G, 7 = A, 9 = H, 10 = C"),
      ],
      quiz: ["NOTE_NAME", "FIND_NOTE"],
    },
    {
      id: "neck3", unit: "1 · Vrat", title: "Oktave — ista nota, drugi sprat", goal: "Da svaku notu nađeš na bar dva mesta, za čas.",
      blocks: [
        txt("**Oktava** je ista nota, duplo viša frekvencija. Oblik na basu je uvek isti: **dve žice gore, dva praga desno**.\n\nTo je i najlakši način da „premestiš“ koren kad ti je nezgodno: A na 5. pragu E žice = A na 7. pragu D žice."),
        board("A na E žici (5) i njegova oktava (D žica, 7)", 12, () => [M(0, 5, "A", ROLE.ROOT), M(2, 7, "A", ROLE.ROOT), M(1, 0, "A", ROLE.ROOT, "soft"), M(3, 2, "A", ROLE.ROOT, "soft")]),
        tip("Metal klasik: koren pa oktava, naizmenično, osmine. Pola Maiden „galopa“ i disco-rok linija je samo to."),
      ],
      quiz: ["OCTAVE", "NOTE_NAME"],
    },
    {
      id: "int1", unit: "2 · Intervali", title: "Intervali = oblici pod prstima", goal: "Da interval vidiš kao oblik na vratu, ne kao broj.",
      blocks: [
        txt("Interval je razmak između dve note. Na basu je super to što je **svaki interval uvek isti oblik**, od bilo kog korena (bez praznih žica). Nauči oblike — ne note.\n\nNajbitniji za bas:\n• **Kvinta (5)** — žica gore, 2 praga desno. Tvoj najbolji drug.\n• **Oktava (8)** — dve žice gore, 2 praga desno.\n• **Kvarta (4)** — isti prag, žica gore.\n• **Velika terca (3)** — žica gore, prag levo. Dur zvuk.\n• **Mala terca (b3)** — žica gore, 2 praga levo (ili 3 praga desno na istoj žici). Mol zvuk.\n• **Mala septima (b7)** — dve žice gore, isti prag."),
        board("Oblici od korena C (A žica, 3. prag)", 12, () => [
          M(1, 3, "R", ROLE.ROOT), M(2, 1, "b3", ROLE.THIRD), M(2, 2, "3", ROLE.THIRD), M(2, 3, "4", ROLE.OTHER), M(2, 5, "5", ROLE.FIFTH),
          M(3, 3, "b7", ROLE.SEVENTH), M(3, 4, "7", ROLE.SEVENTH), M(3, 5, "8", ROLE.ROOT), M(0, 3, "5", ROLE.FIFTH, "soft"),
        ]),
        txt("Primeti kvintu i ispod korena (E žica, isti prag) — „5 dole“ je jednako korisna kao gore: koren–kvinta dole je bas osnova kantrija, panka i polovine rokenrola."),
      ],
      quiz: ["INTERVAL_TAP", "INTERVAL_NAME"],
    },
    {
      id: "int2", unit: "2 · Intervali", title: "Dur i mol lestvica", goal: "Formula lestvice i zašto je mol „tužan“.",
      blocks: [
        txt("**Durska lestvica**: stepen-stepen-**pola**-stepen-stepen-stepen-**pola** → 1 2 3 4 5 6 7\n**Prirodni mol**: 1 2 **b3** 4 5 **b6 b7** — tri note niže nego dur.\n\nTi ne moraš da sviraš lestvice gore-dole. Bitno je da znaš **koje note pripadaju pesmi**, pa da biraš između njih."),
        board("E prirodni mol — prsti u jednoj poziciji (0–5)", 7, (n) => Boards.scale(K("E", "mol").scaleIv, n, 7, false)),
        txt("E-mol je **metal tonalitet broj jedan** jer je najniža nota basa (prazna E) tonika. Ovo gore ti je cela „mapa“ za pola repertoara."),
        board("G-dur — iste note kao E-mol!", 7, (n) => Boards.scale(K("G", "dur").scaleIv, n, 7, false)),
        tip("G-dur i E-mol imaju potpuno iste note — to su relativni tonaliteti (lekcija 4). Razlikuje se samo koja nota je „kuća“."),
      ],
      quiz: ["INTERVAL_NAME", "PENTA"],
    },
    {
      id: "ch1", unit: "3 · Akordi", title: "Trozvuk: koren, terca, kvinta", goal: "Svaki akord je R – 3 – 5, a terca odlučuje dur ili mol.",
      blocks: [
        txt("Gitarista svira akord, ti sviraš **tonove tog akorda** — to je cela tajna „lepih“ bas linija.\n\n• **Dur** = R – 3 – 5 (velika terca)\n• **Mol** = R – b3 – 5 (mala terca)\n• **Power akord (5)** = R – 5, bez terce. Metal ga obožava jer nije ni dur ni mol — zato preko power akorda **koren, kvinta i oktava** uvek rade."),
        board("A-mol: R (crveno), b3 (plavo), 5 (zeleno)", 12, (n) => Boards.chord(C("A", Q.MIN), n, 12)),
        board("A-dur: samo se terca pomeri za prag", 12, (n) => Boards.chord(C("A", Q.MAJ), n, 12)),
        tip("Kad ne znaš da li je akord dur ili mol — sviraj samo R, 5 i 8. Nikad ne greše."),
      ],
      quiz: ["CHORD_SPELL", "CHORD_TAP"],
    },
    {
      id: "ch2", unit: "3 · Akordi", title: "Septakordi: 7, m7, maj7", goal: "Četvrta nota koja daje boju — i jako dobra „prolazna“ nota.",
      blocks: [
        txt("Dodaš septimu na trozvuk:\n• **7 (dominantni)** = R 3 5 **b7** — blues, rokenrol, AC/DC\n• **m7** = R b3 5 **b7** — mol sa „mekšim“ zvukom, funk\n• **maj7** = R 3 5 **7** — balade, džez, „sanjarski“\n\nZa tebe je najkorisnija **b7**: dve žice gore, isti prag. R – 5 – b7 – 8 je osnova svakog blues/boogie bas obrasca."),
        board("A7 — R 3 5 b7", 12, (n) => Boards.chord(C("A", Q.DOM7), n, 12)),
        tip("Boogie obrazac preko A7: A – C# – E – F# – G – F# – E – C#  (R 3 5 6 b7 6 5 3). Probaj u Džem modu na 12-taktnom bluesu."),
      ],
      quiz: ["CHORD_TONE", "CHORD_SPELL", "CHORD_TAP"], jam: "12-taktni blues",
    },
    {
      id: "key1", unit: "4 · Tonalitet", title: "Akordi u tonalitetu (rimski brojevi)", goal: "Kad znaš tonalitet, znaš i koji akordi mogu da dođu.",
      blocks: [
        txt("Na svakom stepenu lestvice gradiš akord samo od nota te lestvice. Ispadne uvek isti raspored:\n\n**Dur:** I – ii – iii – IV – V – vi – vii°\n**Mol:** i – ii° – III – iv – v – VI – VII\n\nVelika slova = dur, mala = mol, ° = smanjeni.\n\nPrimer **E-mol**: Em – F#° – G – Am – Hm – C – D\nPrimer **A-dur**: A – Hm – C#m – D – E – F#m – G#°\n\nZašto je ovo zlatno: muzičari pričaju „i – VI – VII“ umesto imena akorda, jer **isti obrazac radi u svakom tonalitetu**. Naučiš oblik jednom, sviraš ga svuda."),
        board("Koreni akorda E-mola (brojevi = stepen)", 10, () => {
          const dia = K("E", "mol").diatonic();
          return Boards.positions(10, (s, f) => s <= 1 && dia.some((d) => d.chord.root.pc === Bass.pc(s, f))).map(([s, f]) => {
            const d = dia.find((x) => x.chord.root.pc === Bass.pc(s, f));
            return M(s, f, d.roman, d.degree === 0 ? ROLE.ROOT : ROLE.OTHER, d.degree === 0 ? "strong" : "soft");
          });
        }),
      ],
      quiz: ["KEY_DEGREE", "KEY_FUNCTION", "KEY_MEMBER"],
    },
    {
      id: "key2", unit: "4 · Tonalitet", title: "Kako da prepoznaš tonalitet pesme", goal: "Praktični trikovi kad ti niko ne kaže „u E-molu je“.",
      blocks: [
        txt("Redom, od najpouzdanijeg:\n• **Akord na kome se pesma završava** (i na kom se refren „odmara“) je skoro uvek tonika.\n• **Prvi akord** je tonika u većini rok pesama.\n• Ako je glavni riff na praznoj E (ili spuštenoj) — verovatno E-mol / E-frigijski.\n• Pogledaj sve akorde: ako se uklapaju u jedan dur/mol par (npr. G, C, D, Em, Am), to je taj par. Onda odluči da li je „kuća“ dur ili mol.\n\n**Relativni tonaliteti**: svaki dur ima mol sa istim notama — **mala terca niže** (G-dur ↔ E-mol, C-dur ↔ A-mol, D-dur ↔ H-mol). Note su iste, ali tonika (i tvoj „sigurni koren“) nije."),
        tip("Rok i metal vole da „pozajmljuju“: bVII u duru (D u E-duru) ili dur V u molu. Ako jedan akord ne štima sa ostalima — to je pozajmica, ne drugi tonalitet."),
      ],
      quiz: ["RELATIVE", "KEY_MEMBER", "KEY_FUNCTION"],
    },
    {
      id: "key3", unit: "4 · Tonalitet", title: "Kvintni krug, na brzinu", goal: "Zašto su E, A, D, G „gitarski“ tonaliteti i kako ide I – IV – V.",
      blocks: [
        txt("Idi kvintu gore od C: **C → G → D → A → E → H → F#**. Svaki sledeći dur ima jednu povisilicu više. Na drugu stranu (kvarte): **C → F → B → Es → As**, jednu snizilicu više.\n\nZa bas je bitnije ovo: **IV i V su susedi tonike u krugu**. Na vratu: IV = isti prag žica gore, V = žica gore + 2 praga. Zato I–IV–V „pada pod prste“ u svakom tonalitetu."),
        board("I – IV – V u A-duru: A, D, E", 12, () => [M(0, 5, "I", ROLE.ROOT), M(1, 5, "IV", ROLE.OTHER), M(1, 7, "V", ROLE.FIFTH), M(0, 0, "V", ROLE.FIFTH, "soft")]),
      ],
      quiz: ["KEY_DEGREE", "INTERVAL_TAP"], jam: "Rok I–IV–V",
    },
    {
      id: "imp1", unit: "5 · Improvizacija", title: "Sigurne note: R – 5 – 8", goal: "Prvi korak improvizacije: nikad pogrešna nota.",
      blocks: [
        txt("Improvizacija na basu nije sviranje brzih solaža — to je **biranje nota preko akorda koji se menjaju**. Počni od toga da nikad ne promašiš:\n\n• Na **prvu dobu** svakog akorda: **koren**.\n• Ostalo: **kvinta** (gore ili dole) i **oktava**.\n• Ritam ti je bitniji od nota — drži se bubnja (kik!).\n\nKad ovo radiš bez razmišljanja preko bilo koje progresije, već improvizuješ."),
        board("R – 5 – 8 oblik (pomera se zajedno sa korenom)", 10, () => [M(0, 5, "R", ROLE.ROOT), M(1, 7, "5", ROLE.FIFTH), M(2, 7, "8", ROLE.ROOT), M(0, 0, "5", ROLE.FIFTH, "soft")]),
        tip("U Džem modu stavi „Epski metal“ u E-molu na 90 BPM. Prvo samo koren u osminama, pa dodaj kvintu i oktavu."),
      ],
      quiz: ["CHORD_TAP", "SAFE_NOTE"], jam: "Epski metal",
    },
    {
      id: "imp2", unit: "5 · Improvizacija", title: "Tonovi akorda + prolazne note", goal: "Kako da povežeš akorde da linija „hoda“.",
      blocks: [
        txt("Pravilo koje koriste svi basisti: **na jaku dobu ton akorda, između — šta god vodi do sledećeg tona akorda**.\n\nProlazne note koje uvek rade:\n• **Note iz lestvice** između dva tona akorda (npr. R → 2 → b3).\n• **Hromatski prilaz**: prag ispod (ili iznad) korena SLEDEĆEG akorda, na poslednju dobu. Zvuči „namerno“ čak i kad je van lestvice, jer odmah razrešava.\n• **Kvinta sledećeg akorda** pa njegov koren — staro kao rokenrol.\n\nPrimer u E-molu, Em pa C:\n• E – G – H – **H** | **C** — kvinta Em (H) je prag ispod korena C, savršen prilaz.\n• E – E – D – **C#** | **D** — ka D akordu: C# nije u E-molu, ali traje osminu i odmah razrešava na D, pa zvuči namerno."),
        board("Em (jako) + ostale note E-mola (slabo)", 12, (n) => Boards.chordOver(C("E", Q.MIN), K("E", "mol").scaleIv, n, 12, false)),
        tip("U Džem modu uključi prikaz „+ lestvica“: jake tačke su sletišta, slabe su stepenice između njih."),
      ],
      quiz: ["SAFE_NOTE", "CHORD_TONE"], jam: "Andaluzijska",
    },
    {
      id: "imp3", unit: "5 · Improvizacija", title: "Pentatonika: jedna mapa za celu pesmu", goal: "Kad nemaš vremena da pratiš svaki akord.",
      blocks: [
        txt("**Mol pentatonika** = 1 – b3 – 4 – 5 – b7. Pet nota, nema „opasnih“ polustepena, pa skoro sve zvuči dobro preko celog mol tonaliteta.\n**Dur pentatonika** = 1 – 2 – 3 – 5 – 6 (iste note kao relativna mol pentatonika).\n**Blues lestvica** = mol pentatonika + **b5**. Rok/metal začin broj jedan.\n\nDva načina razmišljanja — oba su ti potrebna:\n• **Preko cele pesme** jedna pentatonika tonike → brzo, sigurno, za fill-ove.\n• **Po akordu** (tonovi akorda) → melodičnije, „pametnije“, za linije."),
        board("E mol pentatonika, 0–12", 12, (n) => Boards.scale(K("E", "mol").pentatonic, n, 12, false)),
        board("A blues lestvica (5. prag) — b5 je ljubičasta zona", 10, (n) => Boards.scale(K("A", "mol").blues, n, 10, false).map((m) => (m.label === "b5" ? Object.assign(m, { role: ROLE.SEVENTH }) : m))),
        tip("Fill na kraju fraze: poslednja 2 udarca pre novog dela — pentatonika naniže do korena. To je 80% metal bas fill-ova."),
      ],
      quiz: ["PENTA", "SAFE_NOTE"], jam: "Mol rok",
    },
    {
      id: "imp4", unit: "5 · Improvizacija", title: "Metal specifično: b2, power akordi, galop", goal: "Boje koje čuješ kod Metallice, Slayera, Sepulture i Sabbatha.",
      blocks: [
        txt("• **Frigijska b2**: tonika pa prag iznad (E → F). To je onaj preteći zvuk. Ako je u pesmi akord F (ili F5) preko E-mola — sviraš frigijski; tada je i u tvojoj lestvici F umesto F#.\n• **Tritonus (b5)**: E → Bb. Sabbath „Black Sabbath“ zvuk. Koristi kao prolaz, ne sletište.\n• **Power akordi** su samo R + 5 → ti si slobodan: koren, kvinta, oktava, plus b3 i b7 iz mol pentatonike skoro uvek rade.\n• **Galop** (Maiden): osmina + dve šesnaestine na istoj noti. Tehnika, ne teorija — ali kad ga sviraš na tonovima akorda, odjednom zvučiš kao Steve Harris."),
        board("E frigijski: kao E-mol, ali b2 (F) umesto F#", 8, (n) => {
          const notes = [I.R, I.m2, I.m3, I.P4, I.P5, I.m6, I.m7].map((i) => [i, P("E").plus(i)]);
          return Boards.scale(notes, n, 8, false).map((m) => (m.label === "b2" ? Object.assign(m, { role: ROLE.SEVENTH, style: "strong" }) : m));
        }),
        tip("Za improvizaciju preko metal riffa: prati KORENE riffa (gitaristove najniže note) i ubacuj oktave/kvinte. Kad riff stoji, ti možeš da se „šetaš“ pentatonikom."),
      ],
      quiz: ["PENTA", "SAFE_NOTE", "KEY_MEMBER"], jam: "Frigijski riff",
    },
  ];
  LESSONS.forEach((l) => { l.naturals = !!l.naturals; l.strings = l.strings || [0, 1, 2, 3]; l.maxFret = 12; });

  const USAGE = {
    MAJ: "Dur: velika terca (žica gore, prag levo od korena) je ono što ga čini „veselim“. Na jaku dobu: R, 3 ili 5. Za prelaz do sledećeg akorda koristi note iz lestvice ili hromatski prilaz.",
    MIN: "Mol: mala terca (žica gore, dva praga levo). Dodaj b7 (dve žice gore, isti prag) i 4 — dobiješ mol pentatoniku iz istog oblika.",
    POWER: "Power akord nema tercu — zato ti je sve dozvoljeno između korena, kvinte i oktave, a b3 i b7 iz mol pentatonike skoro uvek rade preko metal riffova.",
    DOM7: "Dominantni 7: blues/rokenrol. R – 3 – 5 – 6 – b7 je klasičan boogie obrazac. b7 jako „vuče“ ka sledećem akordu (kvartu gore).",
    MIN7: "m7: mekši mol. R – b3 – 5 – b7 arpeđo je osnova funk i disko linija; oktava sa b7 ispod je super fill.",
    MAJ7: "maj7: velika septima je pola stepena ispod oktave — lepa ali osetljiva nota. Kao sletište koristi R, 3, 5.",
    HALFDIM: "Polusmanjeni (vii u duru, ii u molu): b5 je karakteristična nota. Obično traje kratko i vodi ka V ili i.",
    DIM: "Smanjeni: sve male terce. Prelazni akord — sviraj koren i hromatski idi ka sledećem.",
    AUG: "Povećani: #5 vuče pola stepena gore. Retko u roku, više kao prelaz.",
    DIM7: "Smanjeni septakord: simetričan, svaka nota može biti koren. Prelaz/napetost.",
  };

  const KB = {
    mod12, NATURAL, Sp, P, pcName, pcSharp, I, INTERVALS, QUIZ_INTERVALS, ROLE, Q, QUALITIES, EXPLORER, formula, Chord,
    MODE, Key, roman, Bass, PROGRESSIONS, SAFE, stepChord, romanLine, Boards, KINDS, KIND, makeQuestion, shapeHint,
    LESSONS, USAGE, shuffle, pick,
  };
  if (typeof module !== "undefined" && module.exports) module.exports = KB;
  else root.KB = KB;
})(typeof window !== "undefined" ? window : globalThis);
