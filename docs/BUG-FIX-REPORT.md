> Laporan historis RC1. Pengujian upgrade GUI/nama ada di **RC2-TEST-REPORT.md**.

# Perubahan yang dilaksanakan

| ID | Masalah/bukti | Perbaikan dan bukti |
|---|---|---|
| F01 | Arena ID pilihan Shared tidak diteruskan; saat ini baseline mengizinkan satu Shared per floor | Arena diteruskan GUI→command→queue→start; SelectedArenaTest |
| F02 | Shared.join menerima peserta selama battle/cooldown | Tolak jika aktif/cooldown/tidak tersedia; SharedTest + SelectedArenaTest |
| F03 | PrivatePort mewajibkan gateway walau gateway disabled | Syarat HUB hanya ketika gateway enabled; source diperbaiki; runtime battle belum diuji |
| F04 | Geyser cleanup memakai unsubscribeAll yang tidak ada pada API resmi | Typed API dan unregisterAll; API contract test serta shutdown Paper nyata |
| F05 | Geyser/core reflection mencari method tiap siklus; callback membaca Bukkit config dari thread Geyser | Typed official API + core dependency; snapshot scale volatile |
| F06 | Original furniture disembunyikan hanya berdasarkan definitions global; pack belum diverifikasi/per-connection callback belum siap | Validasi pack, wajib force packs, tawaran pack per koneksi, pre-spawn callback sebelum hide; real client pending |
| F07 | Proxy marker tanpa hitbox, WIDTH/HEIGHT 0 | Proxy non-marker dengan dimensi1×2, core mengenali PDC proxy; barrier anchor dipertahankan; client tap pending |
| F08 | showEntity/hideEntity dikirim tiap siklus tanpa perubahan state | Cache delta visibility; cleanup quit/world/chunk/disable, cache bounded |
| F09 | Proxy yaw tidak mengikuti model Java | Ambil yaw core dan rematerialisasi saat rotasi berubah; visual comparison pending |
| F10 | Pack mendefinisikan geometry/controller/texture path namespace survival lama | Isolasi internalnamespace tanpa perubahan cube/UV/PNG; JSON/asset checks |
| F11 | Barrier dapat tertinggal tanpa visual setelah provider hilang/unload | Core unload cleanup + Lodestone fallback + refresh loaded terpusat100tick; runtime ItemsAdder pending |
| F12 | getNode allowed hanya cek radius; entity/menu bisa diakses menembus dinding | Raytrace obstruction ke anchor saat validasi; client integration pending |
| F13 | ChunkLoad deferred task dapat materialize Node yang telah dihapus/pindah | Periksa identitas record masih sama di registry sebelum materialize |
| F14 | Export HUB y+1 menempatkan return di barrier model | Export posisi pemain aman; import command atomic dengan backup dan sinkronisasi exit |
| F15 | Metadata coordinates yang hilang dapat default0; write registry belum fsync | Wajib coordinate integer/bounds + fsync tmp/directory; Node tests |
| F16 | Duplikasi deklarasi permission admin memunculkan warning startup | Bedrock menggunakan permission milik core tanpa deklarasi ulang |
| F17 | Maven dapat melewatkan main-method test suites | JUnit dynamic adapters menjalankan semua22 legacy suites Dungeon dan ModelCatalog |
| F18 | Legacy verifier fixture tidak memiliki Manifest-Version implementasi sehingga gagal sebelum skenario missing class | Fixture manifest benar; 4 negative verifier cases lulus |
| F19 | Teleport internal hanya memeriksa chunk/world | Tambahkan body/head passable, height, border, hazard checks; actual player teleport pending |

Tambahan admin terbatas: /adw move dan /adw model; tidak ada fitur gameplay besar baru. UUID proxy yang dihapus diberi tombstone dengan batas jumlah dan durasi5detik untuk menolak callback spawn yang terlambat setelah cleanup.
