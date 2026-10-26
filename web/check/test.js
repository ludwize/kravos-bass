// Theory checks for the web edition (mirror of the app's TheoryTest.kt). Run: node web/check/test.js
const assert = require("assert");
const KB = require("../theory.js");
const { P, Key, MODE, Chord, Q, Bass, PROGRESSIONS, stepChord, KINDS, makeQuestion, LESSONS } = KB;
const H = "H";
const names = (a) => a.map((s) => s.name(H)).join(" ");

assert.strictEqual(names(new Key(P("E"), MODE.MINOR).scale), "E F# G A H C D");
assert.strictEqual(names(new Key(P("F"), MODE.MAJOR).scale), "F G A B C D E");
assert.strictEqual(new Key(P("F"), MODE.MAJOR).scale.map((s) => s.name("B")).join(" "), "F G A Bb C D E");
assert.strictEqual(names(new Key(P("H"), MODE.MAJOR).scale), "H C# D# E F# G# A#");
assert.strictEqual(names(new Key(P("D"), MODE.MINOR).scale), "D E F G A B C");

assert.strictEqual(new Key(P("E"), MODE.MINOR).diatonic().map((d) => d.chord.name(H)).join(" "), "Em F#dim G Am Hm C D");
assert.strictEqual(new Key(P("C"), MODE.MAJOR).diatonic().map((d) => d.roman).join(" "), "I ii iii IV V vi vii°");
assert.strictEqual(new Key(P("G"), MODE.MAJOR).diatonic(true).map((d) => d.chord.name(H)).join(" "), "Gmaj7 Am7 Hm7 Cmaj7 D7 Em7 F#m7b5");
assert.strictEqual(new Key(P("G"), MODE.MAJOR).relative.name(H), "E-mol");
assert.strictEqual(new Key(P("A"), MODE.MINOR).relative.name(H), "C-dur");

assert.strictEqual(new Chord(P("A"), Q.DOM7).tones.map((t) => t[1].name(H)).join(" "), "A C# E G");
assert.strictEqual(new Chord(P("Hb"), Q.MAJ).tones.map((t) => t[1].name(H)).join(" "), "B D F");
assert.strictEqual(new Chord(P("H"), Q.HALFDIM).tones.map((t) => t[1].name(H)).join(" "), "H D F A");

assert.strictEqual(Bass.pc(0, 0), 4);
assert.strictEqual(Bass.pc(0, 7), 11);
assert.strictEqual(Bass.pc(1, 3), 0);
assert.strictEqual(Bass.pc(3, 2), 9);

const em = new Key(P("E"), MODE.MINOR);
assert.strictEqual(PROGRESSIONS.find((p) => p.name === "Epski metal").steps.map((s) => stepChord(s, em).name(H)).join(" "), "Em C D Em");
assert.strictEqual(PROGRESSIONS.find((p) => p.name === "Frigijski riff").steps.map((s) => stepChord(s, em).name(H)).join(" "), "E5 F5 E5");
assert.strictEqual(PROGRESSIONS.find((p) => p.name === "12-taktni blues").steps.reduce((a, s) => a + s.bars, 0), 12);

let n = 0;
for (const maxFret of [5, 7, 12, 15]) for (const naturals of [false, true]) for (const k of KINDS) for (let i = 0; i < 150; i++) {
  const q = makeQuestion(k.id, { n: H, maxFret, naturals });
  if (q.answer.type === "choice") {
    assert.ok(q.answer.correct >= 0 && q.answer.correct < q.answer.options.length, k.id + " correct idx");
    assert.strictEqual(new Set(q.answer.options).size, q.answer.options.length, k.id + " unique " + q.answer.options);
  } else {
    assert.ok(q.answer.solution.length > 0, k.id + " solution");
    q.answer.solution.forEach((m) => assert.ok(q.answer.check(m.s, m.f), k.id + " solution passes"));
  }
  n++;
}
for (const l of LESSONS) {
  l.blocks.filter((b) => b.type === "board").forEach((b) => assert.ok(b.build(H).length > 0, l.id));
  for (let i = 0; i < 40; i++) l.quiz.forEach((k) => makeQuestion(k, { n: H, maxFret: l.maxFret, naturals: l.naturals, strings: l.strings }));
  if (l.jam) assert.ok(PROGRESSIONS.some((p) => p.name === l.jam), "jam " + l.jam);
}
console.log(`ok — ${n} questions, ${LESSONS.length} lessons`);
