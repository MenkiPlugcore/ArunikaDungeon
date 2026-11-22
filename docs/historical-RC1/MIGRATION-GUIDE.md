# Migrasi dari paket audit

Baseline terverifikasi: Dungeon 1.0.1, core 0.2.0, Bedrock 0.2.0; checksum 17 file input cocok. Source terkini merupakan reconstructed Dungeon Phase7+8+9 serta core Phase10 dan Bedrock Phase11. Tidak memakai source RC6 sebagai baseline implementasi.

STOP, backup, ganti tiga JAR duplikat, lalu ganti hanya pack Dungeon. Tidak ada reset DB, token metadata, serial, cooldown, UUID atau data survival.

Skema anchors tetap 1, Dungeon config tetap 2, ticket ledger format tetap sama. Validasi coordinate Node sekarang lebih ketat; registry invalid dikunci dan tidak ditimpa default. Jangan menghapus registry ketika terkunci.

Pack UUID tetap, version naik ke1.0.0; namespace internal diisolasi. Config renderer dengan nama file default0.2.0 otomatis dibackup dan diarahkan ke nama pack baru. Setting custom lain dipertahankan.

Perubahan export penting: posisi pulang sekarang posisi pemain saat `/adw export`, bukan di atas blok model. Berdiri di lokasi aman lalu export/import. Import membackup config dan mengubah arena exit menjadi lokasi HUB yang sama dalam satu transaksi; gunakan hanya saat seluruh sesi/queue/party selesai.

Definisi Geyser membutuhkan full restart. Berikan permission staging secara eksplisit; default config tidak membuka gameplay ke semua pemain. Jangan mengubah grup inventory Multiverse saat pemain membawa item berharga.
