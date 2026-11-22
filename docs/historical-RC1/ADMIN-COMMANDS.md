# Command aktual

| Command | Fungsi | Permission |
|---|---|---|
| /adw hub | Lihat Lodestone baru untuk membuat HUB tunggal | arunika.dungeonwaystone.admin |
| /adw return <floor> <arena> | Lihat Lodestone baru di arena; binding arena | sama |
| /adw move <uuid> | Pindah registry UUID ke Lodestone baru yang dilihat | sama |
| /adw model <hub\|return> <skin-or-namespaced-ID> | Model Java per role, misalnya blackstone / portstone | sama |
| /adw remove | Hapus node yang dilihat; pulihkan anchor | sama |
| /adw inspect | UUID dan role node yang dilihat | sama |
| /adw list | Daftar node | sama |
| /adw status / check | Registry dan dependency | sama |
| /adw refresh | Materialisasi ulang node loaded | sama |
| /adw export | Berdiri di titik pulang aman ≤6 blok HUB; ekspor binding | sama |
| /adwbedrock status | Registrasi, pack, proxy, client | arunika.dungeonwaystone.admin |
| /adwbedrock refresh | Sinkronisasi renderer | sama |
| /adwbedrock reload | Config renderer; definitions tetap perlu restart | sama |
| /adg gateway import | Impor ekspor /adw, backup config, sinkronkan semua arena exit | arunika.dungeon.admin |
| /adg gateway check | Validasi UUID HUB/RETURN | sama |
| /adg gateway open | Harus melihat Waystone fisik terikat, tetap validasi jarak/session | arunika.dungeon.gateway.test |
| /adg status / diagnostics | State modul/dependency/guard | arunika.dungeon.admin |
| /adg admin | Panel setup arena, wave, boss, loot, exchange | sama |
| /adg setup arena <floor> <id> <shared\|private> | Wizard koordinat | sama |
| /adg setup resume | Lanjut draft wizard | sama |
| /adg setup boss <floor> <Mythic-ID> | Pilih boss valid | sama |
| /adg setup count <floor> <1..30> | Jumlah wave | sama |
| /adg setup wave <floor> <n> <Mythic-ID> <amount> | Ganti wave | sama |
| /adg setup append-wave <floor> <n> <Mythic-ID> <amount> | Tambah grup mob | sama |
| /adg setup enable / disable <floor> | Battle floor | sama |
| /adg create <id> <number> [display name] | Floor tambahan | sama |
| /adg reload all | Reload config aman bila tidak ada operasi/sesi aktif | sama |
| /adg private accept / decline / ready / unready / leave / status | Aksi anggota; undangan lintas world | arunika.dungeon.private.test |
| /adg ticket give <player> | Buat satu tiket serial | arunika.dungeon.admin |
| /adg ticket inspect <serial> | Ledger serial | sama |
| /adg ticket recover <serial> <session> | Pemulihan SPENT yang tidak aktif; hanya setelah pemeriksaan staf | sama |
| /adg ticket claim <serial> | Klaim refund/tiket milik pemain | arunika.dungeon.private.test |
| /opdg status / reload | Exchange status/config | arunika.dg.admin |
| /opdg token set | Pegang item mata uang untuk Exchange baru | sama |
| /opdg ticket <cost> <on\|off> | Harga/aktif tiket Exchange | sama |
| /opdg exchange <on\|off> | Aktif/nonaktif Exchange | sama |
| /opdg tx list | Transaksi ambigu, tidak dikirim ulang otomatis | sama |

`arunika.dungeonwaystone.use` default true. Permission `.admin` dan `.test` default op. Untuk pemain staging, berikan `.gateway.test`, `.shared.test`, `.private.test`; Exchange juga memerlukan `.exchange.test` dan `arunika.dg.use`.

Saat gateway aktif, masuk/start/invite dilakukan melalui GUI fisik. Jangan mendokumentasikan `/adw sethub` atau `/adg gateway sethub`: keduanya tidak ada. Lokasi pulang diambil dari posisi pemain saat `/adw export`, kemudian `/adg gateway import`.
