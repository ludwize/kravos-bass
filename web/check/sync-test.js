// Sync merge checks. The cases in sync-cases.json are also run by the app's SyncDocTest.kt, so both sides must agree.
const assert = require("assert");
const fs = require("fs");
const S = require("../sync.js");
const cases = JSON.parse(fs.readFileSync(__dirname + "/sync-cases.json", "utf8"));
for (const c of cases) {
  const got = S.toJson(S.merge(S.fromJson(JSON.stringify(c.a)), S.fromJson(JSON.stringify(c.b))));
  assert.strictEqual(got, S.toJson(S.fromJson(JSON.stringify(c.expect))), c.name);
  // merge is symmetric and idempotent
  assert.strictEqual(S.toJson(S.merge(S.fromJson(JSON.stringify(c.b)), S.fromJson(JSON.stringify(c.a)))), got, c.name + " (b,a)");
  const m = S.fromJson(got);
  assert.ok(S.same(S.merge(m, m), m), c.name + " idempotent");
}
// recording helpers
let d = S.empty();
d = S.withAnswer(d, "w-1", "NOTE_NAME", true, 10);
d = S.withAnswer(d, "w-1", "NOTE_NAME", false, 11);
d = S.withStreak(d, 3, 11);
d = S.withLesson(d, "neck1", true, 12);
const phone = S.withLesson(S.withAnswer(S.empty(), "a-1", "NOTE_NAME", true, 20), "neck2", true, 20);
const p = S.progress(S.merge(d, phone));
assert.deepStrictEqual(p.stats.NOTE_NAME, [2, 3]);
assert.deepStrictEqual(p.done.sort(), ["neck1", "neck2"]);
assert.strictEqual(p.best, 3);
console.log(`ok — ${cases.length} merge cases`);
