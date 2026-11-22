# Setup Dungeon

1. Pastikan MythicMobs aktif dan ID mob benar-benar ada. Plugin tidak menyediakan definisi boss/skill baru.
2. Gunakan `/adg admin` atau `/adg setup boss <floor> <Mythic-ID>`. Floor awal: queen_bee, mega_warden, the_doom, goblin, skeleton. Nomor/nama dan floor tambahan tetap dinamis.
3. Atur count/wave dan arena melalui wizard. Capture kedua sudut batas, spawn pemain, spawn minion, spawn boss, exit HUB. Semua spawn berada di batas arena; antar-arena tidak boleh overlap. Chunk tujuan harus loaded; plugin tidak force-load.
4. Buat setidaknya arena Shared dan arena Private yang terpisah untuk setiap boss yang hendak menyediakan dua mode. Satu arena Shared per floor; hingga 32 arena per floor, pagination lintas seluruh floor. Satu boss dapat memiliki beberapa arena Private.
5. Di config.yml ArunikaDungeon, pertahankan `foundation-only: true`; aktifkan `battle-preview`, `shared-preview`, `private-preview`, dan `loot-preview` hanya pada staging yang siap diuji. `/adg reload all`.
6. Aktifkan battle floor melalui `/adg setup enable <floor>` setelah isi wave/boss/arena valid.
7. Buat HUB/RETURN, `/adw export` dari titik pulang aman, lalu `/adg gateway import`. Command mengimpor bindings dan menyamakan exit seluruh arena secara atomic dengan backup. Import memerlukan battle-preview dan minimal satu battle valid.
8. Gunakan wizard drop editor atau `/adg loot addhand <floor> <shared|private> <boss|wave-N> <key> <chance> <min> <max>`. Pegang item non-ticket. `/adg reload all` setelah edit drop.

Shared gratis; countdown konfigurabel (default 30 detik); boss cooldown 1.800 detik sejak boss mati; join baru saat battle/cooldown ditolak. Tidak pindah ke arena alternatif.

Private maksimal lima pemain: satu tiket leader per sesi, leader tidak harus Ready. Ready + online + satu world leader dapat ikut; anggota yang tidak memenuhi syarat tidak memblokir Start. Invite lintas world diizinkan. Tiket berada di main hand leader dan divalidasi sebelum debit.

Universal Ticket custom loot harus memakai CUSTOM `dungeon_ticket`; dilarang menyimpan tiket serial sebagai template item untuk diduplikasi. Loot physical hanya untuk peserta sesi valid; reward ambigu disimpan untuk pemeriksaan, tidak replay otomatis.
