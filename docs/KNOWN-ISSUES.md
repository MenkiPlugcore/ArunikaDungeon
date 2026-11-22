# Batasan yang masih terbuka

- Tampilan dan klik model ItemsAdder/Bedrock Android asli belum diuji. Registrasi Geyser 2/2 tidak membuktikan bahwa model terlihat pada HP.
- Shared, Private Solo/Party, skill boss, loot dan perpindahan inventory antar-world belum diuji pada stack vendor lengkap. Unit/JDBC tests lulus, tetapi tidak menggantikan gameplay acceptance.
- Geyser build1280 meminta protokol Java26.3. Paper1.21.11 membutuhkan ViaVersion yang kompatibel; client join belum diuji.
- fsync tiket/cooldown masih berjalan pada main thread saat transisi transaksi. Performa 30–50 pemain dan100+plugin belum diukur.
- Sesi/boss yang sedang berjalan tidak dilanjutkan sesudah crash. HELD direcover; SPENT ambigu memerlukan pemeriksaan staf; EXPOSED tidak direfund. Ini pemulihan fail-closed, bukan resume encounter.
- Pending return pemain offline berada dalam memori. Pemain stranded dipindah ke HUB saat login jika perlindungan battle/arena aktif; pengujian dengan pemain dan restart belum dilakukan.
- Baseline membatasi satu arena Shared per floor, hingga32arena per floor dan200floor. Boss/floor dapat ditambah; tidak dibatasi lima pilihan.
- Mengganti skin Java ke selain Blackstone/Portstone tidak otomatis menambah pemetaan Bedrock.
- Fixture database historis untuk test-exchange-sql.py tidak terlampir; suite itu NOT TESTED. JDBC fresh/stress/recovery tests sudah berjalan.

Status paket tetap RC/staging. Jangan menganggapnya Official Stable sebelum matriks runtime selesai.
