/*
 * Kravos Bas — Web Audio: plucked bass (Karplus-Strong, pre-rendered), drum kit and a chord pad,
 * plus the jam sequencer (same timeline as the app's JamSequencer: count-in, then the progression on loop).
 */
(function (root) {
  "use strict";
  let ctx = null, master = null;
  const SR = 44100;
  const cache = new Map();

  function ac() {
    if (!ctx) {
      const C = root.AudioContext || root.webkitAudioContext;
      if (!C) return null;
      ctx = new C({ latencyHint: "interactive" });
      const comp = ctx.createDynamicsCompressor();
      comp.threshold.value = -10; comp.ratio.value = 4;
      master = ctx.createGain(); master.gain.value = 0.8;
      master.connect(comp).connect(ctx.destination);
    }
    if (ctx.state === "suspended") ctx.resume();
    return ctx;
  }
  const freq = (m) => 440 * Math.pow(2, (m - 69) / 12);

  function buffer(key, seconds, fill) {
    if (cache.has(key)) return cache.get(key);
    const c = ac();
    const len = Math.floor(c.sampleRate * seconds);
    const b = c.createBuffer(1, len, c.sampleRate);
    fill(b.getChannelData(0), c.sampleRate);
    cache.set(key, b);
    return b;
  }

  function pluckBuf(midi) {
    return buffer("p" + midi, 1.6, (out, sr) => {
      const n = Math.max(2, Math.round(sr / freq(midi)));
      const ring = new Float32Array(n);
      let prev = 0, avg = 0;
      for (let i = 0; i < n; i++) { prev += 0.35 * (Math.random() * 2 - 1 - prev); ring[i] = prev; avg += prev; }
      avg /= n;
      for (let i = 0; i < n; i++) ring[i] = (ring[i] - avg) * 1.6;
      let idx = 0;
      const fade = 2000;
      for (let t = 0; t < out.length; t++) {
        const nx = (idx + 1) % n, v = ring[idx];
        ring[idx] = 0.996 * 0.5 * (v + ring[nx]);
        idx = nx;
        out[t] = v * (t > out.length - fade ? (out.length - t) / fade : 1);
      }
    });
  }
  const kickBuf = () => buffer("kick", 0.35, (o, sr) => {
    let ph = 0;
    for (let t = 0; t < o.length; t++) { const s = t / sr; ph += (2 * Math.PI * (45 + 95 * Math.exp(-s * 28))) / sr; o[t] = Math.sin(ph) * Math.exp(-s * 9); }
  });
  const snareBuf = () => buffer("snare", 0.22, (o, sr) => {
    let lp = 0;
    for (let t = 0; t < o.length; t++) { const s = t / sr; lp += 0.6 * (Math.random() * 2 - 1 - lp); o[t] = lp * Math.exp(-s * 18) * 0.7 + Math.sin(2 * Math.PI * 185 * s) * Math.exp(-s * 30) * 0.5; }
  });
  const hatBuf = () => buffer("hat", 0.08, (o, sr) => {
    let prev = 0;
    for (let t = 0; t < o.length; t++) { const n = Math.random() * 2 - 1; o[t] = (n - prev) * Math.exp((-t / sr) * 55); prev = n; }
  });
  const clickBuf = (f) => buffer("c" + f, 0.05, (o, sr) => { for (let t = 0; t < o.length; t++) o[t] = Math.sin(2 * Math.PI * f * (t / sr)) * Math.exp((-t / sr) * 70); });

  function playBuf(b, when, gain) {
    const c = ac(); if (!c) return;
    const src = c.createBufferSource(); src.buffer = b;
    const g = c.createGain(); g.gain.value = gain;
    src.connect(g).connect(master);
    src.start(Math.max(when, c.currentTime));
  }

  function pad(midis, when, dur, vel = 0.05) {
    const c = ac(); if (!c) return;
    const g = c.createGain();
    const lp = c.createBiquadFilter(); lp.type = "lowpass"; lp.frequency.value = 1400;
    g.gain.setValueAtTime(0, when);
    g.gain.linearRampToValueAtTime(vel, when + 0.04);
    g.gain.setValueAtTime(vel, Math.max(when + 0.05, when + dur - 0.12));
    g.gain.linearRampToValueAtTime(0, when + dur);
    lp.connect(g).connect(master);
    for (const m of midis) for (const det of [-5, 5]) {
      const o = c.createOscillator(); o.type = "sawtooth"; o.frequency.value = freq(m); o.detune.value = det;
      o.connect(lp); o.start(when); o.stop(when + dur + 0.02);
    }
  }

  const Audio = {
    unlock() { ac(); },
    available() { return !!(root.AudioContext || root.webkitAudioContext); },
    pluck(midi, when) { const c = ac(); if (!c) return; playBuf(pluckBuf(midi), when ?? c.currentTime, 0.9); },
    seq(midis, gap = 0.26) { const c = ac(); if (!c) return; const t0 = c.currentTime + 0.02; midis.forEach((m, i) => Audio.pluck(m, t0 + i * gap)); },
    now() { return ctx ? ctx.currentTime : 0; },
  };

  // --------------------------------------------------------------- jam sequencer

  /** chords: [{pcs:[...], root pc}] as midi voicings; beats: beats per chord. */
  class Jam {
    constructor({ bpm, voicings, beats, drums, padOn, countIn = 4 }) {
      Object.assign(this, { bpm, voicings, beats, drums, padOn, countIn });
      this.spb = 60 / bpm;
      this.loop = beats.reduce((a, b) => a + b, 0);
      this.starts = []; let acc = 0; for (const b of beats) { this.starts.push(acc); acc += b; }
      this.half = 0; this.timer = null; this.t0 = 0;
    }
    start() {
      const c = ac(); if (!c) return;
      this.t0 = c.currentTime + 0.12;
      this.timer = setInterval(() => this.tick(), 25);
      this.tick();
    }
    stop() { clearInterval(this.timer); this.timer = null; }
    tick() {
      const ahead = ctx.currentTime + 0.15;
      for (;;) {
        const when = this.t0 + (this.half * this.spb) / 2;
        if (when > ahead) break;
        if (when >= ctx.currentTime - 0.01) this.trigger(this.half, when);
        this.half++;
      }
    }
    trigger(half, when) {
      const on = half % 2 === 0, beat = Math.floor(half / 2);
      if (beat < this.countIn) { if (on) playBuf(clickBuf(beat === 0 ? 1760 : 1320), when, 0.35); return; }
      const b = beat - this.countIn, inLoop = b % this.loop, bar = b % 4;
      if (this.drums) {
        playBuf(hatBuf(), when, on ? 0.2 : 0.12);
        if (on) {
          if (bar === 0 || bar === 2) playBuf(kickBuf(), when, 0.9);
          if (bar === 1 || bar === 3) playBuf(snareBuf(), when, 0.55);
        } else if (bar === 2) playBuf(kickBuf(), when, 0.55);
      } else if (on) playBuf(clickBuf(bar === 0 ? 1760 : 1320), when, 0.25);
      if (this.padOn && on) {
        const ci = this.starts.indexOf(inLoop);
        if (ci >= 0) pad(this.voicings[ci], when, this.beats[ci] * this.spb - 0.01);
      }
    }
    position() {
      if (!ctx || !this.timer) return null;
      const t = ctx.currentTime - this.t0;
      if (t < 0) return null;
      const beat = Math.floor(t / this.spb);
      if (beat < this.countIn) return { countIn: true, beat, beatsInChord: this.countIn, chord: 0 };
      const b = beat - this.countIn, inLoop = b % this.loop;
      let ci = this.beats.length - 1;
      for (let i = 0; i < this.beats.length; i++) if (inLoop < this.starts[i] + this.beats[i]) { ci = i; break; }
      return { countIn: false, beat: inLoop - this.starts[ci], beatsInChord: this.beats[ci], chord: ci, bar: b % 4 };
    }
  }
  Audio.Jam = Jam;

  root.KBAudio = Audio;
})(window);
