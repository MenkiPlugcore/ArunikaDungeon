> Panduan dasar RC1 tetap berlaku kecuali versi JAR dan alur admin yang diperbarui pada **GUI-SETUP-ID.md**. RC2: Dungeon 1.0.3-RC2; core/Bedrock 1.0.1-RC2.

# Rollback

1. STOP server sepenuhnya. Simpan salinan state RC saat ini untuk pemeriksaan.
2. Kembalikan tiga JAR baseline dan pack Dungeon0.2.0 dari backup; pindahkan pack1.0.0 keluar folder packs. Pertahankan pack survival.
3. Kembalikan config Waystone/Dungeon/renderer dari snapshot pra-upgrade bila diperlukan. Bila import gateway mengubah koordinat, gunakan backup config yang dicatat command atau snapshot pra-upgrade.
4. **Jangan mengembalikan ledger tiket/Exchange DB/cooldown ke snapshot lama jika sudah ada transaksi RC.** Ini dapat menghidupkan serial/stock lama. Simpan ledger terbaru; format tidak berubah. Untuk pemulihan seluruh server gunakan snapshot konsisten dan rekonsiliasi transaksi terlebih dahulu.
5. Bila harus memulihkan DB, STOP seluruh penulis dan tangani database beserta WAL/SHM sesuai backup konsisten. Jangan menyalin satu file .db aktif lalu menganggap transaksi lengkap.
6. START dan cek UUID, HUB/RETURN, ticket inspect, transaksi pending, config, serta client.

Jar baseline tersedia dalam paket input audit yang dipertahankan pada Build Provenance. Jangan memasang ZIP source/provenance sebagai plugin.
