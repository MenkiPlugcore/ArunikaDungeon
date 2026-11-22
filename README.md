# ARUNIKA Dungeon Waystone — RC2 / STAGING

Upgrade GUI admin dan nama Waystone, 22 November 2026. **Belum Official Stable.**

| Komponen | Versi |
|---|---|
| ArunikaDungeon | 1.0.3-RC2 |
| ArunikaDungeonWaystone | 1.0.1-RC2 |
| ArunikaDungeonWaystoneBedrock | 1.0.1-RC2 |
| Bedrock pack / Java ItemsAdder addon | 1.0.0 (model tidak diubah) |

Mulai dari **GUI-SETUP-ID.md**. `/opdg` membuka editor penukaran; `/adw` membuka pengaturan Waystone bernama. GUI admin tersedia bagi OP atau permission admin terkait.

Alur pemain: `/warp dungeon` → klik HUB → Shared / Private → pilih boss → arena → RETURN → HUB. Teleport masuk mengikuti spawn arena; RETURN menuju titik pulang HUB. Waystone ini khusus Dungeon.

Stop server, backup ketiga folder plugin, ganti ketiga JAR sebagai satu set, lalu start. Jangan menghapus data/config atau memasang JAR versi lama bersamaan. Pack Java disertakan di `Java-ItemsAdder`; pack Bedrock lama tetap kompatibel. Registry lama tanpa nama dibaca menggunakan nama otomatis; UUID, binding dan koordinat dipertahankan.

Laporan lama berada di `docs/historical-RC1`. Bukti pengujian RC2 ada di RC2-TEST-REPORT.md. Klik GUI, teleport gameplay, rendering Java/Android, dan integrasi vendor belum diuji dengan klien nyata.

Build canonical: JDK 21, Maven 3.9.x, `mvn -B -ntp clean verify`, lalu `python3 scripts/package.py`. Source lengkap tersedia; dependency resmi tidak dibundel sebagai stub.
