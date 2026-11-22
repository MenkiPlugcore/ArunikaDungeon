> Panduan dasar RC1 tetap berlaku kecuali versi JAR dan alur admin yang diperbarui pada **GUI-SETUP-ID.md**. RC2: Dungeon 1.0.3-RC2; core/Bedrock 1.0.1-RC2.

# Instalasi staging

1. STOP server; backup JAR lama, ketiga folder konfigurasi/data Dungeon, konfigurasi Geyser, packs, serta world terkait. Jangan menghapus database/ledger.
2. Pasang hanya tiga JAR RC dari folder Plugins/STAGING. Singkirkan JAR versi duplikat dari folder plugins, simpan untuk rollback.
3. Pasang `ArunikaDungeonWaystone-Bedrock-Models.mcpack` di `plugins/Geyser-Spigot/packs/`. Hapus hanya salinan pack Dungeon versi 0.2.0 dari folder packs setelah membackupnya; UUID pack sama, jangan memuat dua versinya sekaligus. Jangan mengganti pack survival.
4. Pertahankan resource pack Java ItemsAdder lama beserta Blackstone/Portstone. Tidak ada pack Java baru.
5. Geyser 2.12.1 build 1280 memerlukan jembatan protokol ViaVersion yang mendukung protokol Java 26.3 untuk Paper 1.21.11. Log server uji menunjukkan peringatan ini. Floodgate tetap diperlukan jika memakai auth Floodgate.
6. Pastikan `gameplay.force-resource-packs: true` pada Geyser 2.12.x. Renderer sengaja mempertahankan visual lama jika pack opsional, tidak valid, atau tidak ditawarkan ke sesi.
7. START penuh. Jalankan `/adw status`, `/adwbedrock status`, `/adg status`, `/adg gateway check`, `/adg diagnostics`, `/opdg status`.
8. Harapkan `registered=true (2/2)`, `packValid=true`, `packsRequired=true`, `callbackFailed=false`. Ini diagnostik registrasi; tetap uji tampilan dan klik di HP.

Gameplay pada instalasi baru tetap nonaktif secara default. Ikuti DUNGEON-SETUP-GUIDE. Jangan memakai /reload Bukkit, PlugMan, atau reload Geyser untuk mengganti custom entity definitions.

Plugin tidak mengatur Multiverse-Inventories. Samakan grup inventory HUB dan arena bila item harus tetap sama; verifikasi menggunakan salinan inventory. Jangan menguji dengan item produksi berharga.
