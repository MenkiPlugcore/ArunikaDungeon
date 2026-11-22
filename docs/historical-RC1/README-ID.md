# ARUNIKA Dungeon Waystone — RC1 / STAGING

Hasil audit 21 November 2026. **Belum Production Ready / Official Stable.**

| Komponen | Versi |
|---|---|
| ArunikaDungeon | 1.0.2-RC1 |
| ArunikaDungeonWaystone | 1.0.0-RC1 |
| ArunikaDungeonWaystoneBedrock | 1.0.0-RC1 |
| Bedrock Models | 1.0.0 (pack dalam paket staging) |

Alur tetap: /warp dungeon → klik HUB → Shared / Private → pilih boss → arena → Return → HUB.
Dungeon memiliki gameplay; Waystone core memiliki registry/model Java; Bedrock hanya renderer. Tidak ada fitur survival/personal/discovery/crafting/Vault baru.

Baca INSTALLATION-GUIDE, MIGRATION-GUIDE, KNOWN-ISSUES, dan RUNTIME-TEST-REPORT sebelum memasang. Gunakan salinan server untuk penerimaan Java dan Android.

Build source canonical dari root: `mvn -B -ntp clean verify`, lalu `python3 scripts/package.py`. Membutuhkan JDK 21 dan Maven 3.9.x. Semua source modul telah diekstrak; tidak membutuhkan rantai patch Phase 7–11 untuk build.

JAR terverifikasi berasal dari build baru dengan dependency resmi; berhasil build tidak sama dengan gameplay/client acceptance. SHA256SUMS memverifikasi integritas, bukan tanda tangan penerbit.
