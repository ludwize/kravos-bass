/* Kravos Bas — web edition UI. Hash routes: #/lekcije, #/lekcija/<id>, #/lekcija/<id>/kviz, #/vezbe, #/kviz/<KIND|mix|grupa:X>, #/akordi, #/tonalitet, #/dzem */
(function () {
  "use strict";
  const { P, Key, MODE, Chord, Q, Bass, PROGRESSIONS, SAFE, stepChord, romanLine, Boards, KINDS, KIND, makeQuestion, LESSONS, USAGE, EXPLORER, formula, I, mod12 } = window.KB;
  const A = window.KBAudio;
  const $ = (s, el = document) => el.querySelector(s);
  const view = $("#view");

  // ------------------------------------------------------------------ storage

  const store = {
    get(k, d) { try { const v = localStorage.getItem("kb." + k); return v == null ? d : JSON.parse(v); } catch { return d; } },
    set(k, v) { try { localStorage.setItem("kb." + k, JSON.stringify(v)); } catch {} },
  };
  const S = Object.assign({ n: "H", frets: 12, sound: true, octaveUp: true, theme: "system" }, store.get("settings", {}));
  const saveS = () => store.set("settings", S);
  const PR = Object.assign({ done: [], stats: {}, best: 0 }, store.get("progress", {}));
  const saveP = () => store.set("progress", PR);
  const PX = Object.assign({ frets: 12, count: 10, naturals: false }, store.get("practice", {}));
  const saveX = () => store.set("practice", PX);

  // ------------------------------------------------------------------ helpers

  const esc = (s) => String(s).replace(/[&<>"']/g, (c) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" })[c]);
  function rich(text) {
    const inline = (s) => esc(s).replace(/\*\*(.+?)\*\*/g, "<b>$1</b>");
    const out = [];
    let list = null;
    for (const line of text.split("\n")) {
      if (line.startsWith("• ")) { (list = list || []).push(`<li>${inline(line.slice(2))}</li>`); continue; }
      if (list) { out.push(`<ul>${list.join("")}</ul>`); list = null; }
      if (line.trim()) out.push(`<p>${inline(line)}</p>`);
    }
    if (list) out.push(`<ul>${list.join("")}</ul>`);
    return `<div class="rich">${out.join("")}</div>`;
  }
  function h(html) { const t = document.createElement("template"); t.innerHTML = html.trim(); return t.content.firstElementChild; }
  const ICON = {
    book: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M2 4h7a3 3 0 0 1 3 3v13a2 2 0 0 0-2-2H2z"/><path d="M22 4h-7a3 3 0 0 0-3 3v13a2 2 0 0 1 2-2h8z"/></svg>',
    quiz: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="3" width="18" height="18" rx="4"/><path d="M9.5 9a2.5 2.5 0 1 1 3.5 2.3c-.6.3-1 .9-1 1.6v.6"/><path d="M12 17h.01"/></svg>',
    chord: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><path d="M4 7h16M4 12h16M4 17h16"/><circle cx="8" cy="7" r="2" fill="currentColor"/><circle cx="15" cy="12" r="2" fill="currentColor"/><circle cx="10" cy="17" r="2" fill="currentColor"/></svg>',
    key: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="9"/><path d="M12 3v3M12 18v3M3 12h3M18 12h3M5.6 5.6l2.1 2.1M16.3 16.3l2.1 2.1M5.6 18.4l2.1-2.1M16.3 7.7l2.1-2.1"/><circle cx="12" cy="12" r="2.5" fill="currentColor"/></svg>',
    jam: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><path d="M4 10v4M8 6v12M12 3v18M16 7v10M20 10v4"/></svg>',
    play: '<svg viewBox="0 0 24 24" fill="currentColor"><path d="M7 4.5v15a1 1 0 0 0 1.5.86l12-7.5a1 1 0 0 0 0-1.72l-12-7.5A1 1 0 0 0 7 4.5z"/></svg>',
    stop: '<svg viewBox="0 0 24 24" fill="currentColor"><rect x="6" y="6" width="12" height="12" rx="2"/></svg>',
    bulb: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M9 18h6M10 22h4"/><path d="M12 2a7 7 0 0 0-4 12.7c.6.5 1 1.3 1 2.3h6c0-1 .4-1.8 1-2.3A7 7 0 0 0 12 2z"/></svg>',
    ok: '<svg viewBox="0 0 24 24" fill="currentColor"><circle cx="12" cy="12" r="11"/><path d="M7 12.5l3.2 3.2L17 9" stroke="#fff" stroke-width="2.4" fill="none" stroke-linecap="round" stroke-linejoin="round"/></svg>',
    no: '<svg viewBox="0 0 24 24" fill="currentColor"><circle cx="12" cy="12" r="11"/><path d="M8 8l8 8M16 8l-8 8" stroke="#fff" stroke-width="2.4" stroke-linecap="round"/></svg>',
    shuffle: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M16 3h5v5M4 20L21 3M21 16v5h-5M15 15l6 6M4 4l5 5"/></svg>',
    note: '<svg viewBox="0 0 24 24" fill="currentColor"><path d="M10 3v10.6A3.5 3.5 0 1 0 12 17V8h6V3z"/></svg>',
    back: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M15 18l-6-6 6-6"/></svg>',
    check: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3" stroke-linecap="round" stroke-linejoin="round"><path d="M5 12.5l4.5 4.5L19 7"/></svg>',
    sun: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><circle cx="12" cy="12" r="4"/><path d="M12 2v2M12 20v2M2 12h2M20 12h2M4.9 4.9l1.4 1.4M17.7 17.7l1.4 1.4M4.9 19.1l1.4-1.4M17.7 6.3l1.4-1.4"/></svg>',
    moon: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M21 12.8A9 9 0 1 1 11.2 3a7 7 0 0 0 9.8 9.8z"/></svg>',
    auto: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="9"/><path d="M12 3a9 9 0 0 1 0 18z" fill="currentColor"/></svg>',
  };
  const ROLECOL = { root: "var(--r-root)", third: "var(--r-third)", fifth: "var(--r-fifth)", seventh: "var(--r-seventh)", other: "var(--r-other)" };
  const ROLEHEX = { root: "#FF5A4E", third: "#4FA3F7", fifth: "#34C77B", seventh: "#B07CFF", other: "#8E96A3" };
  function legend({ seventh = true, extra = null } = {}) {
    const it = [["root", "R"], ["third", "terca"], ["fifth", "kvinta"]];
    if (seventh) it.push(["seventh", "septima"]);
    if (extra) it.push(["other", extra]);
    return `<div class="legend">${it.map(([r, l]) => `<span><i style="background:${ROLECOL[r]}"></i>${l}</span>`).join("")}</div>`;
  }
  const playMidi = (m) => { if (S.sound) A.pluck(m + (S.octaveUp ? 12 : 0)); };
  const playSeq = (ms, gap) => { if (S.sound) A.seq(ms.map((m) => m + (S.octaveUp ? 12 : 0)), gap); };

  // ------------------------------------------------------------------ fretboard

  let neckId = 0;
  const necks = new Set();
  class Neck {
    /** opts: { markers, frets, onTap(s,f), taps:[{s,f,ok}], hover:boolean } */
    constructor(opts) {
      this.id = ++neckId;
      this.el = h(`<div class="neck"></div>`);
      this.opts = opts;
      this.ro = new ResizeObserver(() => this.render());
      this.ro.observe(this.el);
      necks.add(this);
    }
    set(o) { Object.assign(this.opts, o); this.render(); }
    render() {
      const W = this.el.clientWidth;
      if (!W) return;
      const o = this.opts, frets = o.frets || 12;
      const small = W < 560;
      const nutW = small ? 30 : 38, rowH = small ? 36 : 42, numH = 20;
      const fretW = Math.max(small ? 30 : 34, (W - nutW) / frets);
      const totalW = Math.round(nutW + fretW * frets), boardH = rowH * 4, H = boardH + numH;
      const r = Math.min(fretW, rowH) * 0.4;
      const cx = (f) => (f === 0 ? nutW / 2 - 1 : nutW + (f - 0.5) * fretW);
      const cy = (s) => (3 - s) * rowH + rowH / 2;
      const g = `w${this.id}`;
      let s = `<svg width="${totalW}" height="${H}" viewBox="0 0 ${totalW} ${H}" role="img" aria-label="Vrat basa">`;
      s += `<defs><linearGradient id="${g}" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="#33241C"/><stop offset="1" stop-color="#231711"/></linearGradient></defs>`;
      s += `<rect x="0" y="0" width="${totalW}" height="${boardH}" rx="10" fill="url(#${g})"/>`;
      s += `<rect x="0" y="0" width="${nutW}" height="${boardH}" fill="#1A1310"/>`;
      for (const f of [3, 5, 7, 9, 15, 17]) if (f <= frets) s += `<circle cx="${nutW + (f - 0.5) * fretW}" cy="${boardH / 2}" r="${rowH * 0.15}" fill="#EFE4D0" opacity=".3"/>`;
      if (frets >= 12) for (const y of [rowH, rowH * 3]) s += `<circle cx="${nutW + 11.5 * fretW}" cy="${y}" r="${rowH * 0.15}" fill="#EFE4D0" opacity=".3"/>`;
      s += `<rect x="${nutW - 5}" y="0" width="5" height="${boardH}" fill="#E9E2D4"/>`;
      for (let f = 1; f <= frets; f++) s += `<rect x="${nutW + f * fretW - 1.25}" y="0" width="2.5" height="${boardH}" fill="#B9B2A8"/>`;
      for (let row = 0; row < 4; row++) {
        const st = 3 - row, y = row * rowH + rowH / 2;
        s += `<rect x="${nutW - 4}" y="${y - (1 + st * 0.55)}" width="${totalW - nutW + 4}" height="${2 + st * 1.1}" fill="#D9D2C6"/>`;
        s += `<text x="${nutW / 2 - 1}" y="${y}" text-anchor="middle" dominant-baseline="central" font-size="12" font-weight="600" fill="#6A5A4E">${Bass.NAMES[st]}</text>`;
      }
      for (let f = 0; f <= frets; f++) {
        if (f === 0 || [3, 5, 7, 9, 12, 15].includes(f) || fretW > 46) s += `<text x="${cx(f)}" y="${boardH + 13}" text-anchor="middle" font-size="11" style="fill:var(--faint)">${f}</text>`;
      }
      const order = { ghost: 0, soft: 1, strong: 2, target: 3, question: 4 };
      const ms = (o.markers || []).filter((m) => m.f <= frets).slice().sort((a, b) => order[a.style] - order[b.style]);
      for (const m of ms) {
        const x = cx(m.f), y = cy(m.s);
        const col = m.style === "question" ? "#FFB020" : ROLEHEX[m.role || "other"];
        if (m.style === "ghost") s += `<circle cx="${x}" cy="${y}" r="${r * 0.85}" fill="#fff" opacity=".18"/>`;
        else if (m.style === "soft") s += `<circle cx="${x}" cy="${y}" r="${r * 0.88}" fill="${col}" fill-opacity=".42" stroke="${col}" stroke-opacity=".8" stroke-width="2"/>`;
        else if (m.style === "target") s += `<circle cx="${x}" cy="${y}" r="${r}" fill="${col}"/><circle cx="${x}" cy="${y}" r="${r + 3}" fill="none" stroke="#fff" stroke-width="3"/>`;
        else s += `<circle cx="${x}" cy="${y}" r="${r}" fill="${col}"/>`;
        if (m.label) {
          const fs = m.label.length >= 4 ? 9.5 : m.label.length === 3 ? 10.5 : m.style === "soft" || m.style === "ghost" ? 11.5 : 13;
          const fill = m.style === "question" ? "#2A1800" : m.style === "soft" || m.style === "ghost" ? "#F3EEE6" : "#fff";
          s += `<text x="${x}" y="${y + 0.5}" text-anchor="middle" dominant-baseline="central" font-size="${fs}" font-weight="700" fill="${fill}">${esc(m.label)}</text>`;
        }
      }
      for (const t of o.taps || []) {
        const x = cx(t.f), y = cy(t.s), col = t.ok ? "#34C77B" : "#FF5A4E";
        s += `<circle cx="${x}" cy="${y}" r="${r * 1.14}" fill="none" stroke="${col}" stroke-width="4.5"/>`;
        if (!t.ok) { const d = r * 0.5; s += `<path d="M${x - d} ${y - d}L${x + d} ${y + d}M${x - d} ${y + d}L${x + d} ${y - d}" stroke="${col}" stroke-width="4.5" stroke-linecap="round"/>`; }
      }
      if (o.onTap) s += `<circle class="hov" r="${r * 1.05}" fill="none" stroke="#FFB020" stroke-width="2.5" opacity="0" cx="-99" cy="-99"/>`;
      s += `</svg>`;
      const prevScroll = this.el.scrollLeft;
      this.el.innerHTML = s;
      this.el.classList.toggle("tap", !!o.onTap);
      this.geom = { nutW, fretW, rowH, frets, cx, cy };
      const svg = this.el.firstChild;
      if (o.onTap) {
        const pos = (e) => {
          const b = svg.getBoundingClientRect(), x = e.clientX - b.left, y = e.clientY - b.top;
          if (y < 0 || y > boardH) return null;
          const st = 3 - Math.min(3, Math.max(0, Math.floor(y / rowH)));
          const f = x < nutW ? 0 : Math.min(frets, 1 + Math.floor((x - nutW) / fretW));
          return [st, f];
        };
        svg.addEventListener("click", (e) => { const p = pos(e); if (p) o.onTap(p[0], p[1]); });
        const hov = svg.querySelector(".hov");
        svg.addEventListener("mousemove", (e) => {
          const p = pos(e);
          if (!p) { hov.setAttribute("opacity", 0); return; }
          hov.setAttribute("cx", cx(p[1])); hov.setAttribute("cy", cy(p[0])); hov.setAttribute("opacity", 0.9);
        });
        svg.addEventListener("mouseleave", () => hov.setAttribute("opacity", 0));
      }
      // keep the interesting part in view when the neck scrolls
      const focus = ms.find((m) => m.style === "question") || ms.find((m) => m.style === "target") || ms.find((m) => m.label === "R");
      if (totalW > W) {
        if (focus && this.lastFocus !== `${focus.s}:${focus.f}`) {
          this.lastFocus = `${focus.s}:${focus.f}`;
          this.el.scrollLeft = Math.max(0, cx(focus.f) - W / 2);
        } else this.el.scrollLeft = prevScroll;
      }
    }
    destroy() { this.ro.disconnect(); necks.delete(this); }
  }
  function boardBlock(caption, opts) {
    const n = new Neck(opts);
    const el = h(`<div>${caption ? `<div class="board-cap">${esc(caption)}</div>` : ""}</div>`);
    el.appendChild(n.el);
    el.neck = n;
    return el;
  }

  // ------------------------------------------------------------------ routing

  const TABS = [["lekcije", "Lekcije", ICON.book], ["vezbe", "Vežbe", ICON.quiz], ["akordi", "Akordi", ICON.chord], ["tonalitet", "Tonalitet", ICON.key], ["dzem", "Džem", ICON.jam]];
  $("#tabs").innerHTML = TABS.map(([id, t, ic]) => `<a href="#/${id}" data-tab="${id}">${ic}<span>${t}</span></a>`).join("");
  let cleanup = [];
  const onLeave = (f) => cleanup.push(f);

  function route() {
    cleanup.forEach((f) => { try { f(); } catch {} });
    cleanup = [];
    necks.forEach((n) => n.destroy());
    const parts = (location.hash.replace(/^#\/?/, "") || "lekcije").split("/").map(decodeURIComponent);
    const tab = { lekcija: "lekcije", kviz: "vezbe" }[parts[0]] || parts[0];
    document.querySelectorAll("#tabs a").forEach((a) => a.classList.toggle("on", a.dataset.tab === tab));
    view.innerHTML = "";
    window.scrollTo(0, 0);
    const pages = {
      lekcije: pageLessons,
      lekcija: () => (parts[2] === "kviz" ? pageLessonQuiz(parts[1]) : pageLesson(parts[1])),
      vezbe: pagePractice,
      kviz: () => pageQuiz(parts[1]),
      akordi: pageChords,
      tonalitet: pageKey,
      dzem: pageJam,
    };
    (pages[parts[0]] || pageLessons)();
    document.title = ({ lekcije: "Lekcije", lekcija: "Lekcija", vezbe: "Vežbe", kviz: "Kviz", akordi: "Akordi", tonalitet: "Tonalitet", dzem: "Džem" }[parts[0]] || "Lekcije") + " · Kravos Bas";
  }
  window.addEventListener("hashchange", route);

  // ------------------------------------------------------------------ lessons

  function pageLessons() {
    const done = new Set(PR.done);
    const nDone = LESSONS.filter((l) => done.has(l.id)).length;
    const next = LESSONS.find((l) => !done.has(l.id));
    const el = h(`<div></div>`);
    el.appendChild(h(`<div class="card hero">
      <div>
        <div class="kicker">Put: teorija → improvizacija</div>
        <h1 style="margin-top:8px">Nauči teoriju koju možeš da odsviraš.</h1>
        <p class="dim" style="margin:10px 0 16px;max-width:560px">Kratke lekcije, svaka sa proverom. Redom: vrat, intervali, akordi, tonalitet, pa improvizacija. Lekcija je savladana kad na proveri imaš 75%+.</p>
        <div class="bar" style="max-width:420px"><i style="width:${(nDone / LESSONS.length) * 100}%"></i></div>
      </div>
      <div><div class="num">${nDone}<span class="dim" style="font-size:22px">/${LESSONS.length}</span></div><div class="dim small" style="text-align:right">savladano</div>
      ${next ? `<a class="btn" style="margin-top:14px" href="#/lekcija/${next.id}">${nDone ? "Nastavi" : "Počni"}: ${LESSONS.indexOf(next) + 1}. lekcija</a>` : ""}</div>
    </div>`));
    const units = [...new Set(LESSONS.map((l) => l.unit))];
    for (const u of units) {
      const sec = h(`<section class="unit"><h2>${esc(u)}</h2><div class="lgrid"></div></section>`);
      for (const l of LESSONS.filter((x) => x.unit === u)) {
        const i = LESSONS.indexOf(l) + 1, isDone = done.has(l.id);
        sec.lastChild.appendChild(h(`<a class="lcard ${isDone ? "done" : ""} ${next === l ? "next" : ""}" href="#/lekcija/${l.id}">
          <div class="lnum">${isDone ? ICON.check.replace("<svg", '<svg width="16" height="16"') : i}</div>
          <div><h3>${esc(l.title)}</h3><p>${esc(l.goal)}</p></div></a>`));
      }
      el.appendChild(sec);
    }
    view.appendChild(el);
  }

  function pageLesson(id) {
    const l = LESSONS.find((x) => x.id === id);
    if (!l) return pageLessons();
    const idx = LESSONS.indexOf(l), done = PR.done.includes(l.id);
    const el = h(`<div class="narrow stack" style="margin:0 auto"></div>`);
    el.appendChild(h(`<div class="page-head"><a class="kicker" href="#/lekcije">${esc(l.unit)} · lekcija ${idx + 1}</a>
      <h1 style="margin-top:6px">${esc(l.title)}</h1><p style="color:var(--accent);font-weight:600">${esc(l.goal)}</p></div>`));
    let legendShown = false;
    for (const b of l.blocks) {
      if (b.type === "text") el.appendChild(h(`<div>${rich(b.text)}</div>`));
      else if (b.type === "tip") el.appendChild(h(`<div class="tip">${ICON.bulb}<div>${rich(b.text)}</div></div>`));
      else {
        const bb = boardBlock(b.caption, { markers: b.build(S.n), frets: b.frets, onTap: (s, f) => playMidi(Bass.midi(s, f)) });
        if (!legendShown) { legendShown = true; bb.appendChild(h(`<div style="margin-top:8px">${legend({ extra: "ostalo" })}</div>`)); }
        el.appendChild(bb);
      }
    }
    const act = h(`<div class="row" style="flex-wrap:wrap;margin-top:24px"></div>`);
    act.appendChild(h(`<a class="btn" href="#/lekcija/${l.id}/kviz">${done ? "Ponovi proveru" : "Proveri se (8 pitanja)"}</a>`));
    if (l.jam) {
      const j = h(`<button class="btn ghost">${ICON.note}Probaj u Džemu: ${esc(l.jam)}</button>`);
      j.onclick = () => { Jam.load(null, l.jam); location.hash = "#/dzem"; };
      act.appendChild(j);
    }
    if (done) act.appendChild(h(`<span class="donebadge">${ICON.check.replace("<svg", '<svg width="18" height="18"')}Savladano</span>`));
    el.appendChild(act);
    const prev = LESSONS[idx - 1], next = LESSONS[idx + 1];
    el.appendChild(h(`<div class="lesson-nav">${prev ? `<a href="#/lekcija/${prev.id}"><span>← Prethodna</span>${esc(prev.title)}</a>` : "<i></i>"}
      ${next ? `<a href="#/lekcija/${next.id}" style="text-align:right"><span>Sledeća →</span>${esc(next.title)}</a>` : ""}</div>`));
    view.appendChild(el);
  }

  function pageLessonQuiz(id) {
    const l = LESSONS.find((x) => x.id === id);
    if (!l) return pageLessons();
    quiz({
      title: l.title, back: `#/lekcija/${l.id}`, kinds: l.quiz, count: 8, pass: 75,
      ctx: { n: S.n, maxFret: l.maxFret, naturals: l.naturals, strings: l.strings },
      onDone: (right, total) => { if ((right * 100) / total >= 75 && !PR.done.includes(l.id)) { PR.done.push(l.id); saveP(); } },
    });
  }

  // ------------------------------------------------------------------ quiz

  function quiz({ title, back, kinds, count, ctx, pass = null, onDone = () => {} }) {
    let index = 0, right = 0, streak = 0, answered = false, q = null, taps = [];
    const el = h(`<div class="narrow" style="margin:0 auto"></div>`);
    view.appendChild(el);
    let neck = null;

    const record = (ok) => {
      const st = PR.stats[q.kind] || [0, 0];
      PR.stats[q.kind] = [st[0] + (ok ? 1 : 0), st[1] + 1];
      if (ok) { right++; streak++; PR.best = Math.max(PR.best, streak); } else streak = 0;
      saveP();
    };

    function next() {
      if (index + 1 >= count) return finish();
      index++;
      show();
    }
    function finish() {
      onDone(right, count);
      const pct = Math.round((right * 100) / count);
      const msg = pass != null ? (pct >= pass ? "Lekcija savladana ✓" : `Za prolaz treba ${pass}%. Pročitaj još jednom pa probaj ponovo.`)
        : pct >= 90 ? "Odlično — pojačaj težinu (više pragova, sve note)." : pct >= 70 ? "Dobro. Još par krugova i postaje refleks." : "Polako — bolje 5 minuta svaki dan nego sat jednom nedeljno.";
      if (neck) { neck.destroy(); neck = null; }
      el.innerHTML = `<div class="result"><div class="kicker">${esc(title)}</div><div class="score" style="margin-top:12px">${right} / ${count}</div><div class="pct">${pct}%</div><p>${esc(msg)}</p>
        <div class="row" style="justify-content:center"><button class="btn soft" id="again">Ponovi</button><a class="btn" href="${back}">Gotovo</a></div></div>`;
      $("#again", el).onclick = () => { index = 0; right = 0; streak = 0; show(); };
      keyHandler = (e) => { if (e.key === "Enter") location.hash = back; };
    }

    function show() {
      keyHandler = null; answered = false; taps = [];
      q = makeQuestion(KB.pick(kinds), ctx);
      if (neck) { neck.destroy(); neck = null; }
      const isTap = q.answer.type === "tap";
      el.innerHTML = `
        <div class="qtop"><a class="icon" href="${back}" aria-label="Nazad">${ICON.back}</a><div class="bar"><i style="width:${(index / count) * 100}%"></i></div><span class="qcount">${index + 1}/${count}</span></div>
        <div class="kicker">${esc(KIND[q.kind].title)}</div>
        <div class="qprompt">${esc(q.prompt)}</div>
        <div id="qboard"></div>
        <div id="qans"></div>
        <div id="qfb"></div>`;
      if (q.markers.length || isTap) {
        const bb = boardBlock(null, { markers: q.markers, frets: Math.max(ctx.maxFret, ...q.markers.map((m) => m.f)), onTap: isTap ? onTap : (s, f) => { if (answered) playMidi(Bass.midi(s, f)); } });
        neck = bb.neck;
        $("#qboard", el).appendChild(bb);
        if (isTap) $("#qboard", el).appendChild(h(`<div class="hint">Klikni (tapni) na vrat.</div>`));
      }
      if (!isTap) {
        const a = q.answer, cols = window.innerWidth < 560 && a.cols === 4 && a.options.some((o) => o.length > 5) ? 2 : a.cols;
        const box = h(`<div class="answers" style="grid-template-columns:repeat(${cols},1fr)"></div>`);
        a.options.forEach((opt, i) => {
          const b = h(`<button>${i < 9 ? `<kbd class="hide-m">${i + 1}</kbd>` : ""}${esc(opt)}</button>`);
          b.onclick = () => choose(i);
          box.appendChild(b);
        });
        $("#qans", el).appendChild(box);
        if (window.innerWidth > 760) $("#qans", el).appendChild(h(`<div class="hint">Tastatura: ${a.options.length > 9 ? "1–9" : "1–" + a.options.length} za odgovor, Enter za dalje.</div>`));
      }
    }

    function choose(i) {
      if (answered) return;
      answered = true;
      const a = q.answer, ok = i === a.correct;
      const bs = el.querySelectorAll(".answers button");
      bs.forEach((b, j) => { b.disabled = true; if (j === a.correct) b.classList.add("good"); else if (j === i) b.classList.add("bad"); });
      record(ok);
      if (q.sound.length) playSeq(q.sound);
      feedback(ok);
    }
    function onTap(s, f) {
      playMidi(Bass.midi(s, f));
      if (answered) return;
      answered = true;
      const ok = q.answer.check(s, f);
      taps = [{ s, f, ok }];
      neck.set({ markers: q.markers.concat(q.answer.solution), taps });
      record(ok);
      feedback(ok);
    }
    function feedback(ok) {
      const fb = h(`<div class="feedback">
        <div class="verdict" style="color:${ok ? "var(--good)" : "var(--bad)"}">${ok ? ICON.ok : ICON.no}<span style="color:var(--ink)">${ok ? (streak >= 3 ? `Tačno — niz ${streak}!` : "Tačno!") : "Nije."}</span></div>
        <p>${esc(q.explain)}</p><button class="btn block" id="nextq">${index + 1 >= count ? "Rezultat" : "Dalje"}</button></div>`);
      $("#qfb", el).appendChild(fb);
      $("#nextq", el).onclick = next;
      $("#nextq", el).focus({ preventScroll: true });
      fb.scrollIntoView({ behavior: "smooth", block: "nearest" });
      $(".bar i", el).style.width = `${((index + 1) / count) * 100}%`;
    }

    let keyHandler = null;
    const onKey = (e) => {
      if (e.target.closest && e.target.closest("input, dialog")) return;
      if (keyHandler) return keyHandler(e);
      if (!answered && q && q.answer.type === "choice" && /^[1-9]$/.test(e.key)) {
        const i = Number(e.key) - 1;
        if (i < q.answer.options.length) choose(i);
      } else if (answered && (e.key === "Enter" || e.key === " ")) { e.preventDefault(); next(); }
    };
    document.addEventListener("keydown", onKey);
    onLeave(() => document.removeEventListener("keydown", onKey));
    show();
  }

  // ------------------------------------------------------------------ practice

  function pagePractice() {
    const el = h(`<div></div>`);
    el.appendChild(h(`<div class="page-head"><h1>Vežbe</h1><p>Kratki krugovi pitanja. Odgovaraš klikom (ili brojem na tastaturi), a na vratu klikom na prag. Statistika se pamti u ovom pregledaču.</p></div>`));
    const grid = h(`<div class="pgrid"></div>`);
    const side = h(`<div class="card stack sticky"></div>`);
    const chipRow = (label, items, cur, fmt, set) => {
      const w = h(`<div><div class="label" style="margin-bottom:8px">${label}</div><div class="chips"></div></div>`);
      items.forEach((it) => { const c = h(`<button class="chip ${it === cur() ? "on" : ""}">${fmt(it)}</button>`); c.onclick = () => { set(it); saveX(); w.querySelectorAll(".chip").forEach((x) => x.classList.remove("on")); c.classList.add("on"); }; w.lastChild.appendChild(c); });
      return w;
    };
    side.appendChild(h(`<h3 style="font-size:18px">Podešavanje vežbe</h3>`));
    side.appendChild(chipRow("Pragovi", [5, 7, 12, 15], () => PX.frets, (x) => `0–${x}`, (x) => (PX.frets = x)));
    side.appendChild(chipRow("Broj pitanja", [10, 20, 30], () => PX.count, (x) => `${x}`, (x) => (PX.count = x)));
    const nat = h(`<label class="row"><div class="grow"><div style="font-weight:600">Samo prirodne note</div><div class="dim small">Bez # i b (za pitanja o vratu)</div></div><span class="switch"><input type="checkbox" ${PX.naturals ? "checked" : ""}><span></span></span></label>`);
    nat.querySelector("input").onchange = (e) => { PX.naturals = e.target.checked; saveX(); };
    side.appendChild(nat);
    if (PR.best) side.appendChild(h(`<div class="small" style="color:var(--accent);font-weight:600">Najduži niz tačnih: ${PR.best}</div>`));
    grid.appendChild(side);
    const main = h(`<div></div>`);
    main.appendChild(h(`<a class="mix" href="#/kviz/mix">${ICON.shuffle}<div><h3>Miks svega</h3><p>Nasumično iz svih oblasti</p></div></a>`));
    const groups = [...new Set(KINDS.map((k) => k.group))];
    for (const g of groups) {
      const sec = h(`<section class="kgroup"><div class="kgroup-head"><h2>${esc(g)}</h2><a href="#/kviz/grupa:${encodeURIComponent(g)}" style="color:var(--accent);font-weight:600;font-size:13.5px">Sve iz oblasti →</a></div><div class="kgrid"></div></section>`);
      for (const k of KINDS.filter((x) => x.group === g)) {
        const st = PR.stats[k.id];
        sec.lastChild.appendChild(h(`<a class="kcard" href="#/kviz/${k.id}"><div class="grow"><h3>${esc(k.title)}</h3><p>${esc(k.desc)}</p></div>
          ${st && st[1] ? `<div class="kstat"><b>${Math.round((st[0] * 100) / st[1])}%</b><span>${st[1]} odg.</span></div>` : ""}</a>`));
      }
      main.appendChild(sec);
    }
    grid.appendChild(main);
    el.appendChild(grid);
    view.appendChild(el);
  }

  function pageQuiz(what) {
    let kinds, title;
    if (!what || what === "mix") { kinds = KINDS.map((k) => k.id); title = "Miks svega"; }
    else if (what.startsWith("grupa:")) { const g = what.slice(6); kinds = KINDS.filter((k) => k.group === g).map((k) => k.id); title = g; }
    else if (KIND[what]) { kinds = [what]; title = KIND[what].title; }
    if (!kinds || !kinds.length) return pagePractice();
    quiz({ title, back: "#/vezbe", kinds, count: PX.count, ctx: { n: S.n, maxFret: PX.frets, naturals: PX.naturals } });
  }

  // ------------------------------------------------------------------ chords

  const ROOTS = ["C", "C#", "D", "Eb", "E", "F", "F#", "G", "Ab", "A", "Hb", "H"].map(P);
  const CH = Object.assign({ root: 9, q: 1, notes: false }, store.get("chords", {}));

  function pageChords() {
    const el = h(`<div class="explore"></div>`);
    const side = h(`<div class="card stack sticky"></div>`);
    const mainCol = h(`<div class="stack"></div>`);
    el.append(side, mainCol);
    view.appendChild(el);
    let focus = -1;
    const nk = new Neck({ frets: S.frets, markers: [] });

    function renderSide() {
      side.innerHTML = `<div><div class="label" style="margin-bottom:8px">Koren</div><div class="chips" id="roots"></div></div>
        <div><div class="label" style="margin-bottom:8px">Vrsta</div><div class="chips" id="quals"></div></div>`;
      ROOTS.forEach((r, i) => { const c = h(`<button class="chip ${i === CH.root ? "on" : ""}">${r.name(S.n)}</button>`); c.onclick = () => { CH.root = i; focus = -1; store.set("chords", CH); renderSide(); renderMain(); }; $("#roots", side).appendChild(c); });
      EXPLORER.forEach((q, i) => { const c = h(`<button class="chip ${i === CH.q ? "on" : ""}">${q.suffix || "dur"}</button>`); c.onclick = () => { CH.q = i; store.set("chords", CH); renderSide(); renderMain(); }; $("#quals", side).appendChild(c); });
    }
    const chord = () => new Chord(ROOTS[CH.root], EXPLORER[CH.q]);
    function markers() {
      const ch = chord(), all = Boards.chord(ch, S.n, S.frets, CH.notes);
      return focus < 0 ? all : all.map((m) => (Math.abs(m.f - focus) <= 3 || (m.f === 0 && focus <= 3) ? m : Object.assign({}, m, { style: "ghost" })));
    }
    function renderMain() {
      const ch = chord();
      mainCol.innerHTML = `
        <div class="bigname"><h1>${esc(ch.name(S.n))}</h1><span class="dim" style="font-size:18px;font-weight:600">${esc(ch.q.label)}</span></div>
        <div class="tones">${ch.tones.map(([i, n]) => `<div class="tone"><b>${esc(n.name(S.n))}</b><span style="color:${ROLECOL[i.role]}">${i.short}</span></div>`).join("")}</div>
        <div class="dim small">Formula: ${formula(ch.q)}</div>
        <div class="row" style="flex-wrap:wrap"><div class="seg" id="lab"><button class="${CH.notes ? "" : "on"}">Intervali</button><button class="${CH.notes ? "on" : ""}">Note</button></div>
          <div class="grow"></div><button class="btn soft" id="arp">${ICON.play}Arpeđo</button><button class="btn soft" id="r58">R – 5 – 8</button></div>
        <div id="nk"></div>
        <div class="row" style="justify-content:space-between;flex-wrap:wrap">${legend({ seventh: ch.q.ivs.length === 4 })}
          <span class="dim small">${focus < 0 ? "Klikni koren (crveno) na E ili A žici da vidiš oblik oko te pozicije." : `Pozicija oko ${focus}. praga — klikni koren opet za ceo vrat.`}</span></div>
        <div class="card"><h3 style="font-size:17px;margin-bottom:6px">Kako ovo koristiš</h3><p style="margin:0">${esc(USAGE[ch.q.id])}</p></div>`;
      $("#nk", mainCol).appendChild(nk.el);
      nk.set({ frets: S.frets, markers: markers(), onTap: (s, f) => {
        playMidi(Bass.midi(s, f));
        if (ch.intervalOfPc(Bass.pc(s, f)) === I.R && s <= 1) { focus = focus === f ? -1 : f; renderMain(); }
      } });
      const [bi, bn] = $("#lab", mainCol).children;
      bi.onclick = () => { CH.notes = false; store.set("chords", CH); renderMain(); };
      bn.onclick = () => { CH.notes = true; store.set("chords", CH); renderMain(); };
      const base = 28 + mod12(ch.root.pc - 4);
      $("#arp", mainCol).onclick = () => { const ns = ch.q.ivs.map((i) => base + i.semis).concat([base + 12]); playSeq(ns.concat(ns.slice(0, -1).reverse()), 0.24); };
      $("#r58", mainCol).onclick = () => playSeq([0, 7, 12, 7].map((x) => base + x), 0.3);
    }
    renderSide(); renderMain();
  }

  // ------------------------------------------------------------------ key

  const KS = Object.assign({ minor: true, tonic: 1, degree: -1, sevenths: false, view: 0, notes: false }, store.get("key", {}));

  function pageKey() {
    const el = h(`<div class="explore"></div>`);
    const side = h(`<div class="card stack sticky"></div>`);
    const mainCol = h(`<div class="stack"></div>`);
    el.append(side, mainCol);
    view.appendChild(el);
    const nk = new Neck({ frets: S.frets, markers: [] });
    const mode = () => (KS.minor ? MODE.MINOR : MODE.MAJOR);
    const key = () => { const ks = Key.all(mode()); return ks[Math.min(KS.tonic, ks.length - 1)]; };
    const save = () => store.set("key", KS);

    function renderSide() {
      const ks = Key.all(mode());
      side.innerHTML = `<div class="seg" id="md"><button class="${KS.minor ? "" : "on"}">dur</button><button class="${KS.minor ? "on" : ""}">mol</button></div>
        <div><div class="label" style="margin-bottom:8px">Tonika</div><div class="chips" id="tons"></div></div>
        <p class="dim small" style="margin:0">Prelazak dur ↔ mol vodi u relativni tonalitet — iste note, druga „kuća“.</p>`;
      const sw = (minor) => { if (KS.minor === minor) return; const rel = key().relative; KS.minor = minor; KS.tonic = Math.max(0, Key.all(mode()).findIndex((k) => k.tonic.pc === rel.tonic.pc)); KS.degree = -1; save(); renderSide(); renderMain(); };
      const [bd, bm] = $("#md", side).children;
      bd.onclick = () => sw(false); bm.onclick = () => sw(true);
      ks.forEach((k, i) => { const c = h(`<button class="chip ${i === KS.tonic ? "on" : ""}">${k.tonic.name(S.n)}</button>`); c.onclick = () => { KS.tonic = i; KS.degree = -1; save(); renderSide(); renderMain(); }; $("#tons", side).appendChild(c); });
    }
    function renderMain() {
      const k = key(), dia = k.diatonic(KS.sevenths);
      const bg = [k.scaleIv, k.pentatonic, k.blues][KS.view];
      mainCol.innerHTML = `
        <div class="bigname"><h1>${esc(k.name(S.n))}</h1></div>
        <div class="dim">Relativni ${k.relative.mode.label}: <b style="color:var(--ink)">${esc(k.relative.name(S.n))}</b> — iste note, druga „kuća“.</div>
        <div class="scale-row">${k.scaleIv.map(([i, n]) => `<div><b>${esc(n.name(S.n))}</b><span>${i.short.replace("R", "1")}</span></div>`).join("")}</div>
        <div class="card"><div class="row"><h3 class="grow" style="font-size:17px">Akordi u tonalitetu</h3><span class="small dim">septakordi</span><label class="switch"><input type="checkbox" id="sev" ${KS.sevenths ? "checked" : ""}><span></span></label></div>
          <div class="dgrid" id="dia"></div>
          <div class="dim small" style="margin-top:10px">${KS.degree < 0 ? "Klikni akord da vidiš njegove tonove preko lestvice." : esc(dia[KS.degree].chord.name(S.n) + ": " + dia[KS.degree].chord.tones.map(([i, n]) => `${i.short} ${n.name(S.n)}`).join("  "))}</div></div>
        <div class="row" style="flex-wrap:wrap"><div class="seg" id="vw"><button>Lestvica</button><button>Pentatonika</button><button>Blues</button></div><div class="grow"></div>
          <span class="small dim">note</span><label class="switch"><input type="checkbox" id="nts" ${KS.notes ? "checked" : ""}><span></span></label></div>
        <div id="nk"></div>
        <div>${KS.degree >= 0 ? legend({ seventh: KS.sevenths, extra: "ostalo" }) : `<span class="dim small">Crveno = tonika (${esc(k.tonic.name(S.n))}). Brojevi = stepen od tonike.</span>`}</div>
        <div class="label" style="margin-top:22px">Tipične progresije u ${esc(k.name(S.n))} — klikni za Džem</div>
        <div class="plist" id="pl"></div>`;
      dia.forEach((d) => {
        const c = h(`<button class="dchip ${d.degree === KS.degree ? "on" : ""}"><span>${d.roman}</span><b>${esc(d.chord.name(S.n))}</b></button>`);
        c.onclick = () => { KS.degree = KS.degree === d.degree ? -1 : d.degree; save(); renderMain(); if (KS.degree >= 0) playSeq(d.chord.tones.map(([i]) => 28 + mod12(d.chord.root.pc - 4) + i.semis), 0.2); };
        $("#dia", mainCol).appendChild(c);
      });
      $("#sev", mainCol).onchange = (e) => { KS.sevenths = e.target.checked; save(); renderMain(); };
      $("#nts", mainCol).onchange = (e) => { KS.notes = e.target.checked; save(); renderMain(); };
      [...$("#vw", mainCol).children].forEach((b, i) => { b.classList.toggle("on", i === KS.view); b.onclick = () => { KS.view = i; save(); renderMain(); }; });
      $("#nk", mainCol).appendChild(nk.el);
      nk.set({ frets: S.frets, markers: KS.degree >= 0 ? Boards.chordOver(dia[KS.degree].chord, bg, S.n, S.frets, KS.notes) : Boards.scale(bg, S.n, S.frets, KS.notes), onTap: (s, f) => playMidi(Bass.midi(s, f)) });
      PROGRESSIONS.filter((p) => p.mode === k.mode).forEach((p) => {
        const b = h(`<button class="pitem"><div class="grow"><h3>${esc(p.name)}</h3><b>${esc(p.steps.map((s) => stepChord(s, k).name(S.n)).join(" – "))}</b><span>${esc(romanLine(p))}</span></div>${ICON.note}</button>`);
        b.onclick = () => { Jam.load(k, p.name); location.hash = "#/dzem"; };
        $("#pl", mainCol).appendChild(b);
      });
    }
    renderSide(); renderMain();
  }

  // ------------------------------------------------------------------ jam

  const JV = ["Akord", "+ pentatonika", "+ lestvica"];
  const Jam = {
    st: Object.assign({ tonic: "E", minor: true, prog: "Epski metal", bpm: 90, mult: 1, drums: true, pad: true, view: 2 }, store.get("jam", {})),
    seq: null,
    save() { store.set("jam", this.st); },
    key() { return new Key(P(this.st.tonic), this.st.minor ? MODE.MINOR : MODE.MAJOR); },
    prog() { return PROGRESSIONS.find((p) => p.name === this.st.prog) || PROGRESSIONS[0]; },
    chords() { const k = this.key(); return this.prog().steps.map((s) => stepChord(s, k)); },
    get playing() { return !!this.seq; },
    start() {
      A.unlock();
      const p = this.prog();
      const voicings = this.chords().map((c) => { const root = 52 + mod12(c.root.pc - 4); return c.tones.map(([, s]) => { let m = root + mod12(s.pc - c.root.pc); if (m < root) m += 12; return m; }); });
      this.seq = new A.Jam({ bpm: this.st.bpm, voicings, beats: p.steps.map((s) => s.bars * 4 * this.st.mult), drums: this.st.drums, padOn: this.st.pad });
      this.seq.start();
      pill();
    },
    stop() { if (this.seq) this.seq.stop(); this.seq = null; pill(); },
    restart() { if (this.playing) { this.stop(); this.start(); } },
    load(k, progName) {
      const p = PROGRESSIONS.find((x) => x.name === progName);
      if (k) { this.st.tonic = k.tonic.name("B"); this.st.minor = k.mode === MODE.MINOR; }
      if (p) {
        const minor = p.mode === MODE.MINOR;
        if (this.st.minor !== minor) { const rel = this.key().relative; this.st.tonic = rel.tonic.name("B"); this.st.minor = minor; }
        this.st.prog = p.name;
      } else if (this.prog().mode !== this.key().mode) this.st.prog = PROGRESSIONS.find((x) => x.mode === this.key().mode).name;
      this.save(); this.restart();
    },
  };
  function pill() {
    $("#jampill").classList.toggle("on", Jam.playing && !location.hash.startsWith("#/dzem"));
    $("#jampill-t").textContent = Jam.playing ? `Džem · ${Jam.chords()[0].name(S.n)}…` : "Džem";
  }
  window.addEventListener("hashchange", pill);

  function pageJam() {
    const el = h(`<div></div>`);
    view.appendChild(el);
    const nk = new Neck({ frets: S.frets, markers: [] });
    let lastKey = "";

    el.innerHTML = `
      <div class="card jam-now">
        <div><div class="kicker" id="jdeg"></div><div class="chord" id="jchord"></div><div class="dim" id="jtones" style="font-weight:600;margin-top:6px"></div><div class="beats" id="jbeats"></div></div>
        <div class="next"><div class="dim small">sledeći</div><b id="jnext"></b><div style="margin-top:16px"><button class="btn playbtn" id="jplay"></button></div></div>
      </div>
      <div class="chordstrip" id="jstrip"></div>
      <div style="margin-top:14px" id="nk"></div>
      <div class="row" style="flex-wrap:wrap;margin-top:10px"><div id="jleg" class="grow"></div><div class="seg" id="jview">${JV.map((v) => `<button>${v}</button>`).join("")}</div></div>
      <div class="jgrid">
        <div class="card stack" id="jinfo"></div>
        <div class="card stack" id="jset"></div>
      </div>`;
    $("#nk", el).appendChild(nk.el);

    function renderSetup() {
      const k = Jam.key(), p = Jam.prog(), st = Jam.st;
      $("#jinfo", el).innerHTML = `<div class="label">Progresija</div><h3 style="font-size:22px">${esc(p.name)}</h3>
        <div style="color:var(--accent);font-weight:700">${esc(Jam.chords().map((c) => c.name(S.n)).join(" – "))} <span class="dim" style="font-weight:500">(${esc(romanLine(p))})</span></div>
        <p style="margin:0">${esc(p.note)}</p>
        <div class="dim small">Preko cele progresije radi: ${esc(k.tonic.name(S.n))} ${p.safe === "PENTA" ? k.mode.label + " pentatonika" : p.safe === "SCALE" ? k.mode.label + " lestvica (ali prati akorde!)" : "blues lestvica"}</div>
        <div class="label" style="margin-top:18px">Progresije u ${k.mode.label}u</div><div class="chips" id="jprogs"></div>`;
      PROGRESSIONS.filter((x) => x.mode === k.mode).forEach((x) => {
        const c = h(`<button class="chip ${x === p ? "on" : ""}">${esc(x.name)}</button>`);
        c.onclick = () => { st.prog = x.name; Jam.save(); Jam.restart(); renderSetup(); };
        $("#jprogs", el).appendChild(c);
      });
      $("#jset", el).innerHTML = `<div class="label">Tonalitet</div>
        <div class="seg" id="jmode"><button class="${st.minor ? "" : "on"}">dur</button><button class="${st.minor ? "on" : ""}">mol</button></div>
        <div class="chips" id="jtonic"></div>
        <div class="row"><div class="label grow">Tempo</div><b style="color:var(--accent);font-variant-numeric:tabular-nums" id="jbpmv">${st.bpm} BPM</b></div>
        <input type="range" min="50" max="180" step="5" value="${st.bpm}" id="jbpm" aria-label="Tempo">
        <div class="label">Dužina akorda</div><div class="seg" id="jmult"><button class="${st.mult === 1 ? "on" : ""}">kao u progresiji</button><button class="${st.mult === 2 ? "on" : ""}">duplo duže</button></div>
        <label class="row"><span class="grow" style="font-weight:600">Bubanj</span><span class="switch"><input type="checkbox" id="jdr" ${st.drums ? "checked" : ""}><span></span></span></label>
        <label class="row"><span class="grow" style="font-weight:600">Akordi (klavijature)</span><span class="switch"><input type="checkbox" id="jpd" ${st.pad ? "checked" : ""}><span></span></span></label>
        <p class="dim small" style="margin:0">Pratnja nema bas — to si ti. Pusti zvuk kroz zvučnike ili slušalice i sviraj preko svog pojačala. Razmaknica = start/stop.</p>`;
      const [md, mm] = $("#jmode", el).children;
      const setMode = (minor) => { if (st.minor === minor) return; st.minor = minor; st.prog = PROGRESSIONS.find((x) => x.mode === Jam.key().mode).name; Jam.save(); Jam.restart(); renderSetup(); };
      md.onclick = () => setMode(false); mm.onclick = () => setMode(true);
      Key.all(k.mode).forEach((kk) => {
        const c = h(`<button class="chip ${kk.tonic.pc === k.tonic.pc ? "on" : ""}">${esc(kk.tonic.name(S.n))}</button>`);
        c.onclick = () => { st.tonic = kk.tonic.name("B"); Jam.save(); Jam.restart(); renderSetup(); };
        $("#jtonic", el).appendChild(c);
      });
      const bpm = $("#jbpm", el);
      bpm.oninput = () => { $("#jbpmv", el).textContent = bpm.value + " BPM"; };
      bpm.onchange = () => { st.bpm = Number(bpm.value); Jam.save(); Jam.restart(); };
      const [m1, m2] = $("#jmult", el).children;
      m1.onclick = () => { st.mult = 1; Jam.save(); Jam.restart(); renderSetup(); };
      m2.onclick = () => { st.mult = 2; Jam.save(); Jam.restart(); renderSetup(); };
      $("#jdr", el).onchange = (e) => { st.drums = e.target.checked; Jam.save(); Jam.restart(); };
      $("#jpd", el).onchange = (e) => { st.pad = e.target.checked; Jam.save(); Jam.restart(); };
      lastKey = "";
      frame();
    }

    [...$("#jview", el).children].forEach((b, i) => { b.onclick = () => { Jam.st.view = i; Jam.save(); lastKey = ""; frame(); }; });
    const playBtn = $("#jplay", el);
    playBtn.onclick = () => { Jam.playing ? Jam.stop() : Jam.start(); lastKey = ""; frame(); };
    const onKey = (e) => { if (e.key === " " && !e.target.closest("input, button, dialog")) { e.preventDefault(); playBtn.click(); } };
    document.addEventListener("keydown", onKey);

    function frame() {
      const pos = Jam.seq ? Jam.seq.position() : null;
      const chords = Jam.chords(), p = Jam.prog(), k = Jam.key();
      const ci = pos && !pos.countIn ? pos.chord : 0;
      const cur = chords[ci], nxt = chords[(ci + 1) % chords.length];
      const sig = [ci, Jam.st.view, Jam.playing, S.n, S.frets, Jam.st.tonic, Jam.st.minor, Jam.st.prog].join("|");
      $("#jdeg", el).textContent = pos && pos.countIn ? `Odbrojavanje… ${pos.beat + 1}` : `${p.steps[ci].roman} · ${k.name(S.n)}`;
      const total = pos ? Math.min(pos.beatsInChord, 16) : 4;
      $("#jbeats", el).innerHTML = Array.from({ length: total }, (_, i) => `<i class="${i % 4 === 0 ? "bar" : ""} ${pos && pos.beat === i ? "on" : ""} ${pos && pos.countIn ? "count" : ""}"></i>`).join("");
      if (sig !== lastKey) {
        lastKey = sig;
        $("#jchord", el).textContent = cur.name(S.n);
        $("#jnext", el).textContent = nxt.name(S.n);
        $("#jtones", el).textContent = cur.tones.map(([i, n]) => `${i.short} ${n.name(S.n)}`).join("  ·  ");
        $("#jstrip", el).innerHTML = chords.map((c, i) => `<span class="${i === ci && Jam.playing ? "on" : ""}">${esc(c.name(S.n))}</span>`).join("");
        playBtn.innerHTML = Jam.playing ? `${ICON.stop}Stop` : `${ICON.play}Sviraj`;
        playBtn.classList.toggle("stop", Jam.playing);
        [...$("#jview", el).children].forEach((b, i) => b.classList.toggle("on", i === Jam.st.view));
        const bg = Jam.st.view === 0 ? null : Jam.st.view === 1 ? (p.safe === "BLUES" ? k.blues : k.pentatonic) : p.safe === "BLUES" ? k.blues : k.scaleIv;
        nk.set({ frets: S.frets, markers: bg ? Boards.chordOver(cur, bg, S.n, S.frets, false) : Boards.chord(cur, S.n, S.frets), onTap: (s, f) => playMidi(Bass.midi(s, f)) });
        $("#jleg", el).innerHTML = legend({ seventh: cur.q.ivs.length === 4, extra: bg ? "prolazne" : null });
      }
    }
    let raf = 0;
    const loop = () => { frame(); raf = requestAnimationFrame(loop); };
    raf = requestAnimationFrame(loop);
    onLeave(() => { cancelAnimationFrame(raf); document.removeEventListener("keydown", onKey); pill(); });
    renderSetup();
  }

  // ------------------------------------------------------------------ settings + theme

  function applyTheme() {
    if (S.theme === "system") document.documentElement.removeAttribute("data-theme");
    else document.documentElement.setAttribute("data-theme", S.theme);
    $("#themebtn").innerHTML = { system: ICON.auto, dark: ICON.moon, light: ICON.sun }[S.theme];
    $("#themebtn").title = "Tema: " + { system: "kao sistem", dark: "tamna", light: "svetla" }[S.theme];
  }
  $("#themebtn").onclick = () => { S.theme = { system: "dark", dark: "light", light: "system" }[S.theme]; saveS(); applyTheme(); };

  const dlg = $("#settings");
  function openSettings() {
    let confirm = false;
    const sw = (id, label, val) => `<label class="row"><span class="grow">${label}</span><span class="switch"><input type="checkbox" id="${id}" ${val ? "checked" : ""}><span></span></span></label>`;
    dlg.innerHTML = `<div class="dh"><h2>Podešavanja</h2><button class="icon" id="dx" aria-label="Zatvori"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><path d="M6 6l12 12M18 6L6 18"/></svg></button></div>
      <div class="db">
        <div><div class="label" style="margin-bottom:8px">Nazivi nota</div><div class="seg" id="snot"><button class="${S.n === "H" ? "on" : ""}">H i B (naše)</button><button class="${S.n === "B" ? "on" : ""}">B i Bb (engleske)</button></div></div>
        <div><div class="label" style="margin-bottom:8px">Pragova na vratu</div><div class="seg" id="sfr"><button class="${S.frets === 12 ? "on" : ""}">12</button><button class="${S.frets === 15 ? "on" : ""}">15</button></div></div>
        ${sw("ssnd", "Zvuk kad klikneš vrat", S.sound)}
        ${sw("soct", "Zvuk oktavu više (za male zvučnike)", S.octaveUp)}
        <div class="dim small">Napredak i statistika se čuvaju u ovom pregledaču (nisu povezani sa Android aplikacijom).</div>
        <button class="danger" id="sreset">Resetuj napredak i statistiku</button>
        <a class="btn ghost block" href="https://github.com/ludwize/kravos-bass/releases/latest" target="_blank" rel="noopener">Preuzmi Android aplikaciju (APK)</a>
      </div>`;
    const rerender = () => { saveS(); route(); };
    const [nh, nb] = $("#snot", dlg).children;
    nh.onclick = () => { S.n = "H"; nh.classList.add("on"); nb.classList.remove("on"); rerender(); };
    nb.onclick = () => { S.n = "B"; nb.classList.add("on"); nh.classList.remove("on"); rerender(); };
    const [f12, f15] = $("#sfr", dlg).children;
    f12.onclick = () => { S.frets = 12; f12.classList.add("on"); f15.classList.remove("on"); rerender(); };
    f15.onclick = () => { S.frets = 15; f15.classList.add("on"); f12.classList.remove("on"); rerender(); };
    $("#ssnd", dlg).onchange = (e) => { S.sound = e.target.checked; saveS(); };
    $("#soct", dlg).onchange = (e) => { S.octaveUp = e.target.checked; saveS(); };
    $("#sreset", dlg).onclick = (e) => {
      if (!confirm) { confirm = true; e.target.textContent = "Sigurno? Klikni opet"; return; }
      PR.done = []; PR.stats = {}; PR.best = 0; saveP(); e.target.textContent = "Napredak obrisan"; route();
    };
    $("#dx", dlg).onclick = () => dlg.close();
    dlg.showModal();
  }
  $("#setbtn").onclick = openSettings;
  dlg.addEventListener("click", (e) => { if (e.target === dlg) dlg.close(); });

  applyTheme();
  route();
})();
