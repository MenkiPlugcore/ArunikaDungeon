# Waystone RC2

Baca GUI-SETUP-ID.md untuk alur terbaru GUI, penamaan, titik pulang dan spawn arena.

## Membuat Waystone dan memilih tujuan

Lodestone biasa tidak otomatis berubah ketika dipasang OP. Daftarkan secara sengaja lewat GUI atau command. Pakai Lodestone baru, bukan Waystone survival; sisakan AIR di atasnya.

1. Buka `/adg admin` untuk membuat batas arena, spawn, boss dan wave. ID contoh di bawah harus diganti sesuai floor/arena nyata. Log `Arenas configured=0` berarti arena belum siap dipilih.
2. Lihat Lodestone di HUB (maksimal 8 blok), buka `/adw` → **Buat HUB pada Lodestone yang dilihat**, lalu ketik nama, misalnya `Hub Dungeon`.
3. Berdiri di titik pulang aman dalam 6 blok dari HUB. Buka `/adw` → **Titik pulang HUB = posisi saya**, lalu konfirmasi. Binding diterapkan otomatis; perhatikan pesan sukses atau penolakan di chat.
4. Di dalam batas arena, lihat Lodestone RETURN. `/adw` → **Buat RETURN pada Lodestone yang dilihat** → pilih arena → ketik nama, misalnya `Return Queen Bee`.
5. Berdiri di tujuan teleport masuk arena. `/adw` → **Set spawn arena di posisi saya** → pilih arena. Posisi harus aman dan berada dalam batas/world arena tersebut. Tombol ini juga tersedia dari menu edit RETURN.
6. Dari menu utama `/adw`, klik **Terapkan binding terbaru**. Titik pulang HUB sebelumnya dipertahankan. Setelah semua arena/boss siap, gunakan tombol **Aktifkan mode Dungeon** dan cek `/adg gateway check`.
7. Untuk uji masuk, gunakan Survival/Adventure. Klik HUB → pilih mode/boss. Creative/Spectator memang ditolak untuk masuk gameplay. RETURN hanya melayani peserta pada arena/session yang terikat.

Satu HUB total dan satu RETURN per floor/arena. Tidak menyediakan daftar teleport umum antar-Waystone. Nama membantu admin mengatur node; tujuan masuk ditentukan spawn arena, tujuan keluar ditentukan titik pulang HUB.

## Mengubah nama/lokasi

`/adw` menampilkan daftar nama, jenis, arena dan koordinat. Klik salah satu untuk **Ganti nama**, **Ganti binding arena** (RETURN), atau **Pindahkan ke Lodestone yang dilihat**. Nama harus unik tanpa membedakan huruf besar/kecil; 1–40 huruf/angka/spasi/underscore/titik/tanda minus. Nama tampil di menu dan detail admin, bukan hologram baru.

Pindah node mempertahankan UUID dan nama. Chunk asal harus loaded. RETURN harus tetap di dalam arena terikat; jika berpindah arena, pilih binding arena yang cocok. Setelah pindah, atur ulang titik pulang HUB bila diperlukan dan terapkan binding terbaru. Node yang tidak cocok dengan arena akan ditolak gateway.

Registry RC1 tanpa nama dibaca dengan nama otomatis HUB/RETURN + potongan UUID. Penyimpanan pertama RC2 membuat `anchors-before-RC2.yml`; tidak mereset registry lama. Tetap simpan backup server sendiri sebelum upgrade.

## Command in-game opsional

| Kebutuhan | Command |
|---|---|
| Editor penukaran | `/opdg` |
| Daftar/edit Waystone GUI | `/adw` atau `/adg waystones` |
| Buat HUB bernama sambil melihat Lodestone | `/adw hub Hub Dungeon` |
| Buat RETURN bernama sambil melihat Lodestone | `/adw return queen_bee shared_1 Return Queen Bee` |
| Ubah nama Waystone yang dilihat | `/adw rename Hub Utama` |
| Pindahkan node bernama ke Lodestone yang dilihat | `/adw move Hub Utama` |
| Simpan posisi pulang dekat HUB dan import | `/adw sethub` |
| Simpan posisi spawn arena saat ini | `/adg setspawn queen_bee shared_1` |
| Detail registry/gateway | `/adw list`, `/adw inspect`, `/adg gateway check` |

Nama dengan spasi tidak membutuhkan tanda kutip. ID floor/arena pada contoh bukan arena otomatis yang dibuat plugin.

## Model Java ItemsAdder

Salin isi `Java-ItemsAdder/plugins/ItemsAdder/contents/arunika_dungeon_waystones` ke folder contents ItemsAdder. Baca README addon, jalankan `/iareload` dan `/iazip`, lalu terima pack Java yang baru. Dalam `/adw` pilih **Pakai model Java Dungeon**. Ini mengatur HUB ke `arunika_dungeon_waystones:waystone_blackstone` dan RETURN ke `arunika_dungeon_waystones:waystone_portstone`.

Model asal tidak diganti geometrinya. Ketepatan posisi, rotasi, skala dan render perlu dites di Java nyata. Bedrock memakai pack renderer terpisah dan tidak memakai addon ItemsAdder Java ini.

## Permission

Editor exchange: `arunika.dg.admin` (atau akses panel dari `arunika.dungeon.admin`); toggle: `arunika.dg.toggle`. Panel Waystone: `arunika.dungeon.admin` dan `arunika.dungeonwaystone.admin`. Semua permission admin default OP. Gameplay/test gate lama tetap berlaku.
