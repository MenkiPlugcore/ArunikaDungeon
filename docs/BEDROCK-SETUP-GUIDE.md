> Panduan dasar RC1 tetap berlaku kecuali versi JAR dan alur admin yang diperbarui pada **GUI-SETUP-ID.md**. RC2: Dungeon 1.0.3-RC2; core/Bedrock 1.0.1-RC2.

# Model Bedrock

Pack: `ArunikaDungeonWaystone-Bedrock-Models.mcpack`, versi 1.0.0. UUID header dan module lama dipertahankan agar upgrade dikenali; angka versi naik dari 0.2.0. Jangan memuat kedua versi bersamaan.

Entity ID tetap:

| Role | ID |
|---|---|
| HUB / Blackstone | arunika_dungeon:waystone_hub |
| RETURN / Portstone | arunika_dungeon:waystone_return |

Mesh/cube, UV dan PNG asli dipertahankan. Identifier geometry/render controller serta texture path sekarang di namespace arunika_dungeon, agar tidak mendefinisikan ulang namespace pack survival vephilim_waystones. Tidak ada mesh/tekstur baru.

Install sebelum full STOP/START di Geyser packs. Geyser 2.12.1 build1280 menggunakan API entity eksperimental; API compile dipin ke official 2.12.1-20261120.123911-34. Build1280 diuji startup dan registrasi 2/2, bukan tes render client.

Renderer hanya aktif jika definitions siap, pack lokal valid, Geyser memaksa pack, pack ditawarkan ke koneksi, dan callback Geyser mempersiapkan proxy. Visual ItemsAdder baru disembunyikan per pemain setelah callback tersebut. Callback ini bukan bukti piksel terlihat pada HP; client acceptance tetap wajib.

Jika pack opsional/tidak valid/API gagal, model asli dipertahankan dan anchor fisik tetap tersedia. Tidak ada perubahan konfigurasi Geyser otomatis. `pack.name` lama yang tepat sama dengan default 0.2.0 dimigrasikan dengan backup config renderer.

Periksa skala/rotasi/offset pada Java dan Android asli; default scale 1, offset-y 0, rotasi mengikuti core. Tap permukaan model atau anchor, coba dengan tangan kosong, uji pergantian world/chunk dan restart. Jika model terbalik/invisible, rollback dan simpan log/status serta rekaman client.
