/*
 * Kravos Bas — progress sync between the web and the Android app.
 * Same document format and merge rules as app/.../data/SyncDoc.kt; stored as a secret GitHub Gist (file kravos-bas.json)
 * on the user's own account, using a classic token with only the "gist" scope.
 */
(function (root) {
  "use strict";
  const FILE = "kravos-bas.json";
  const API = "https://api.github.com";
  const DESCRIPTION = "Kravos Bas — napredak (sinhronizacija telefon ↔ web)";
  const TOKEN_URL = "https://github.com/settings/tokens/new?scopes=gist&description=Kravos%20Bas%20sinhronizacija";

  const empty = () => ({ reset: 0, lessons: {}, stats: {}, best: 0, bestT: 0 });

  function fromJson(text) {
    if (!text) return empty();
    try {
      const o = JSON.parse(text), d = empty();
      d.reset = o.reset || 0;
      for (const [id, m] of Object.entries(o.lessons || {})) d.lessons[id] = { done: m.d === 1, t: m.t || 0 };
      for (const [dev, s] of Object.entries(o.stats || {})) {
        const k = {};
        for (const [kind, a] of Object.entries(s.k || {})) k[kind] = [a[0] | 0, a[1] | 0];
        d.stats[dev] = { t: s.t || 0, k };
      }
      d.best = (o.best && o.best.n) || 0;
      d.bestT = (o.best && o.best.t) || 0;
      return d;
    } catch {
      return empty();
    }
  }
  const sorted = (o) => Object.keys(o).sort();
  function toJson(d) {
    // keys in sorted order, like the app, so equal documents give equal text
    const l = {}, s = {};
    for (const id of sorted(d.lessons)) l[id] = { d: d.lessons[id].done ? 1 : 0, t: d.lessons[id].t };
    for (const dev of sorted(d.stats)) {
      const k = {};
      for (const kind of sorted(d.stats[dev].k)) k[kind] = d.stats[dev].k[kind].slice();
      s[dev] = { t: d.stats[dev].t, k };
    }
    return JSON.stringify({ v: 1, reset: d.reset, lessons: l, stats: s, best: { n: d.best, t: d.bestT } });
  }
  const same = (a, b) => toJson(a) === toJson(b);

  function merge(a, b) {
    const r = Math.max(a.reset, b.reset), out = { reset: r, lessons: {}, stats: {}, best: 0, bestT: r };
    for (const id of new Set([...Object.keys(a.lessons), ...Object.keys(b.lessons)])) {
      const x = a.lessons[id], y = b.lessons[id];
      let w = !x ? y : !y ? x : y.t > x.t ? y : y.t === x.t && y.done && !x.done ? y : x;
      if (w && w.t >= r) out.lessons[id] = { done: w.done, t: w.t };
    }
    for (const dev of new Set([...Object.keys(a.stats), ...Object.keys(b.stats)])) {
      const x = a.stats[dev], y = b.stats[dev];
      const w = !x ? y : !y ? x : y.t > x.t ? y : x;
      if (w && w.t >= r) out.stats[dev] = JSON.parse(JSON.stringify(w));
    }
    const cands = [[a.best, a.bestT], [b.best, b.bestT]].filter((c) => c[1] >= r).sort((p, q) => p[0] - q[0] || p[1] - q[1]);
    if (cands.length) [out.best, out.bestT] = cands[cands.length - 1];
    return out;
  }

  function progress(d) {
    const stats = {};
    for (const s of Object.values(d.stats)) for (const [kind, [ri, t]] of Object.entries(s.k)) {
      const c = stats[kind] || [0, 0];
      stats[kind] = [c[0] + ri, c[1] + t];
    }
    return { done: Object.keys(d.lessons).filter((id) => d.lessons[id].done), stats, best: d.best };
  }
  const withLesson = (d, id, done, now) => Object.assign({}, d, { lessons: Object.assign({}, d.lessons, { [id]: { done, t: now } }) });
  function withAnswer(d, dev, kind, right, now) {
    const cur = d.stats[dev] && d.stats[dev].t >= d.reset ? d.stats[dev] : { t: now, k: {} };
    const s = cur.k[kind] || [0, 0];
    const k = Object.assign({}, cur.k, { [kind]: [s[0] + (right ? 1 : 0), s[1] + 1] });
    return Object.assign({}, d, { stats: Object.assign({}, d.stats, { [dev]: { t: now, k } }) });
  }
  const withStreak = (d, n, now) => (n > d.best ? Object.assign({}, d, { best: n, bestT: now }) : d);

  // ---------------------------------------------------------------- GitHub Gist

  class SyncError extends Error {}
  async function gh(token, path, opts = {}) {
    let r;
    try {
      r = await fetch(API + path, {
        method: opts.method || "GET",
        cache: "no-store",
        headers: { Authorization: "Bearer " + token, Accept: "application/vnd.github+json", "X-GitHub-Api-Version": "2022-11-28", ...(opts.body ? { "Content-Type": "application/json" } : {}) },
        body: opts.body ? JSON.stringify(opts.body) : undefined,
      });
    } catch {
      throw new SyncError("Nema veze sa GitHub-om.");
    }
    if (r.status === 401) throw new SyncError("Token nije ispravan ili je istekao.");
    if (r.status === 403 || r.status === 404) throw Object.assign(new SyncError(`Token nema dozvolu „gist“ (HTTP ${r.status}).`), { status: r.status });
    if (!r.ok) throw new SyncError(`GitHub greška HTTP ${r.status}.`);
    return r.json();
  }
  async function find(token) {
    for (let page = 1; page <= 10; page++) {
      const arr = await gh(token, `/gists?per_page=100&page=${page}`);
      const g = arr.find((x) => x.files && x.files[FILE]);
      if (g) return g.id;
      if (arr.length < 100) break;
    }
    return null;
  }
  async function read(token, id) {
    try {
      const g = await gh(token, `/gists/${id}`);
      const f = g.files && g.files[FILE];
      if (!f) return null;
      const text = f.truncated && f.raw_url ? await (await fetch(f.raw_url, { cache: "no-store" })).text() : f.content;
      return fromJson(text);
    } catch (e) {
      if (e.status === 404) return null;
      throw e;
    }
  }
  const create = async (token, d) => (await gh(token, "/gists", { method: "POST", body: { description: DESCRIPTION, public: false, files: { [FILE]: { content: toJson(d) } } } })).id;
  const write = (token, id, d) => gh(token, `/gists/${id}`, { method: "PATCH", body: { files: { [FILE]: { content: toJson(d) } } } });

  /** Pull → merge → push. getLocal/setLocal let the page keep recording while the request runs. */
  async function syncOnce(token, gistId, getLocal) {
    let id = gistId;
    let remote = id ? await read(token, id) : null;
    if (!remote) { id = await find(token); remote = id ? await read(token, id) : null; }
    const local = getLocal();
    const merged = remote ? merge(local, remote) : local;
    if (!id) id = await create(token, merged);
    else if (!same(merged, remote)) await write(token, id, merged);
    return { id, merged };
  }

  const KBSync = { FILE, TOKEN_URL, empty, fromJson, toJson, merge, same, progress, withLesson, withAnswer, withStreak, syncOnce, SyncError };
  if (typeof module !== "undefined" && module.exports) module.exports = KBSync;
  else root.KBSync = KBSync;
})(typeof window !== "undefined" ? window : globalThis);
