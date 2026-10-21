# Kravos Bas

Lična Android aplikacija za učenje teorije na bas gitari — fokus na primeni: od tonaliteta i akorda do improvizacije. Sideload, bez reklama, bez interneta.

## Šta ima

- **Lekcije** (14, u 5 celina): vrat → intervali → akordi → tonalitet → improvizacija. Svaka ima interaktivne prikaze vrata i proveru (8 pitanja, prolaz 75%).
- **Vežbe**: 14 vrsta pitanja — nota na pragu, nađi notu, oktave, intervali (tapkanjem i prepoznavanjem), sastav i tonovi akorda, akordi i stepeni u tonalitetu, relativni tonaliteti, pentatonika, „sigurna nota“ preko akorda. Statistika po vrsti.
- **Akordi**: bilo koji koren × 10 vrsta akorda, tonovi obojeni po ulozi (R / terca / kvinta / septima), fokus na poziciju, arpeđo.
- **Tonalitet**: lestvica, dijatonski akordi (trozvuci/septakordi, rimski brojevi), lestvica/pentatonika/blues na vratu, tipične progresije.
- **Džem**: pratnja bez basa (bubanj + akordi) za 13 progresija u bilo kom tonalitetu; vrat uživo prikazuje tonove trenutnog akorda i prolazne note.

4 žice, standardni štim (E A D G), nazivi nota H/B (podesivo).

## Build

GitHub Actions builduje potpisan APK na svaki push na `main` (uz unit testove teorije) i kači ga na Releases.
Potpisivanje: `signing/release.jks.enc`, CI treba repo secret `KP_SIGNING_PASS`.
