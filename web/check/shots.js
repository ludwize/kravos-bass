// Opens the site in a real browser (desktop light/dark + phone), clicks through every page, saves screenshots
// and logs page errors. usage: node web/check/shots.js <url> <out-dir>
const { chromium } = require("playwright");
const fs = require("fs");

const URL = process.argv[2].replace(/\/?$/, "/");
const OUT = process.argv[3];
fs.mkdirSync(OUT, { recursive: true });

(async () => {
  const opts = process.env.PW_CHROMIUM ? { executablePath: process.env.PW_CHROMIUM } : {};
  const b = await chromium.launch(opts);
  const errs = [];
  const log = [];
  for (const [name, w, hgt, scheme] of [["desk", 1366, 900, "dark"], ["deskl", 1366, 900, "light"], ["mob", 390, 844, "dark"]]) {
    const pg = await b.newPage({ viewport: { width: w, height: hgt }, colorScheme: scheme, deviceScaleFactor: name === "mob" ? 2 : 1 });
    pg.on("pageerror", (e) => errs.push(`${name}: ${e.message}`));
    pg.on("console", (m) => { if (m.type() === "error") errs.push(`${name} console: ${m.text()}`); });
    const go = async (hash, wait = 600) => { await pg.goto(URL + hash); await pg.waitForTimeout(wait); };
    const shot = async (n, full = false) => { await pg.screenshot({ path: `${OUT}/${name}-${n}.png`, fullPage: full }); log.push(`${name}-${n}`); };

    await go("#/lekcije"); await shot("01-lekcije", true);
    await go("#/lekcija/neck1"); await shot("02-lekcija", name !== "deskl");
    await go("#/lekcija/imp2"); await shot("03-lekcija-imp2", true);
    const answer = async () => {
      if (await pg.$(".answers button")) await pg.click(".answers button >> nth=0");
      else { const box = await (await pg.$(".neck svg")).boundingBox(); await pg.mouse.click(box.x + Math.min(box.width, w - 40) * 0.45, box.y + 60); }
      await pg.waitForTimeout(400);
    };
    await go("#/lekcija/neck1/kviz"); await answer(); await shot("04-kviz-odgovor");
    await go("#/kviz/INTERVAL_TAP"); await answer(); await shot("05-kviz-vrat");
    await go("#/kviz/SAFE_NOTE"); await answer(); await shot("05b-kviz-sigurna");
    await go("#/vezbe"); await shot("06-vezbe", true);
    await go("#/akordi"); await shot("07-akordi", true);
    await go("#/tonalitet");
    await pg.click(".dchip >> nth=5"); await pg.waitForTimeout(300); await shot("08-tonalitet", true);
    await go("#/dzem"); await pg.click("#jplay"); await pg.waitForTimeout(2600); await shot("09-dzem-svira");
    await pg.waitForTimeout(2600); await shot("10-dzem-svira2", true);
    await go("#/lekcije", 400); await shot("11-pill");
    await pg.click("#setbtn"); await pg.waitForTimeout(300); await shot("12-podesavanja");
    await pg.keyboard.press("Escape");
    await pg.close();
  }
  fs.writeFileSync(`${OUT}/stats.txt`, `shots: ${log.join(", ")}\npage errors: ${JSON.stringify(errs)}\n`);
  console.log(`page errors: ${errs.length}`); errs.forEach((e) => console.log(e));
  await b.close();
})();
