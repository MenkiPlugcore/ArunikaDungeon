# RC2 — 22 November 2026

- /opdg tanpa argumen membuka GUI admin: token dari tangan, tambah hadiah, harga, kategori, enable/disable dan kontrol tiket; penyimpanan memakai backend transaksi yang sama.
- /adw membuka GUI pengaturan di ArunikaDungeon: nama node, buat HUB/RETURN, pilih binding, simpan titik pulang/spawn dan pilih model Java addon.
- Nama Unicode tervalidasi, unik case-insensitive, disimpan tanpa mengubah record/API Node atau UUID. File RC1 tanpa nama tetap terbaca; backup awal otomatis sebelum penulisan registry.
- /adw hub/return/rename/move menerima nama; /adw sethub dan /adg setspawn menangkap posisi pemain.
- Input chat admin dibatasi 60 detik/128 prompt, dibersihkan ketika keluar/shutdown, diproses pada main thread dan permission diperiksa ulang.
- Penyuntingan registry baru ditolak ketika sesi, party, antrean, pending return atau konfigurasi masih aktif. Validasi transaksi/tiket, batas arena dan ownership gameplay tetap dipakai.
- Pack Bedrock tetap 1.0.0. Java ItemsAdder addon 1.0.0 disertakan tanpa mengubah mesh original.

Riwayat audit awal ada di historical-RC1/CHANGELOG.md.
