# ARUNIKA Dungeon Waystone — Java ItemsAdder Models v1.0.0

Addon aset untuk tiga plugin RC1 yang sudah diberikan. Target ItemsAdder4.0.16 dan Java Minecraft1.21.11. Ini source content ItemsAdder, bukan resource pack client standalone; gabungkan melalui ItemsAdder agar item/HUD/furniture lain tetap masuk pack yang sama.

## Model

| Peran | Model asli | ID baru |
|---|---|---|
| HUB | Blackstone | arunika_dungeon_waystones:waystone_blackstone |
| RETURN | Portstone | arunika_dungeon_waystones:waystone_portstone |

Geometri, UV, PNG, dan setting furniture berasal dari arsip pengguna. Hanya dua model dan tekstur yang mereka perlukan yang dimasukkan. Namespace baru mencegah penimpaan folder/ID vephilim_waystones yang mungkin dipakai survival. Tidak ada recipe, command teleport, registry, atau data pemain baru.

## Pasang

1. Backup konfigurasi ItemsAdder dan ArunikaDungeonWaystone. STOP server untuk menyalin file.
2. Salin folder `plugins/ItemsAdder/contents/arunika_dungeon_waystones/` dari paket ke lokasi yang sama di server. Jangan menaruh ZIP langsung di folder packs Geyser atau di folder resourcepacks client.
3. START server. Jalankan `/iareload`, kemudian `/iazip`. Tunggu keduanya selesai; periksa console bila ada error. Jika pack di-host manual, upload generated.zip terbaru dan perbarui URL/hash sesuai hosting yang sudah dipakai. Terima pack baru pada client Java/rejoin.
4. Jalankan sebagai admin:

```text
/adw model hub arunika_dungeon_waystones:waystone_blackstone
/adw model return arunika_dungeon_waystones:waystone_portstone
/adw refresh
/adw status
```

Command model menyimpan pilihan per role tanpa mengganti UUID/binding/node yang sudah ada. Jalankan di luar battle; refresh mengubah visual loaded node. Node di chunk lain dimuat saat chunk tersedia.

5. Lihat HUB dan RETURN dari Java. Model harus muncul, tidak ungu-hitam/invisible, berada di anchor yang benar, dan klik membuka GUI sekali. Uji ulang dari Bedrock; pack Bedrock lama tetap dipakai.
6. Jika belum ada node: lihat Lodestone baru dengan AIR di atas, `/adw hub` atau `/adw return <floor> <arena>`. Setup HUB/RETURN dan gateway tetap mengikuti panduan paket RC1. Jangan sekadar menaruh item furniture sebagai pengganti node terdaftar.

Untuk tes aset saja, `/iaget arunika_dungeon_waystones:waystone_blackstone` dan `/iaget arunika_dungeon_waystones:waystone_portstone`. Item tes tidak otomatis menjadi gateway; gunakan /adw untuk registry.

## Rollback addon

Jalankan `/adw model hub blackstone` dan `/adw model return portstone` untuk kembali ke ID lama vephilim_waystones bila aset tersebut tersedia. STOP server, pindahkan hanya folder arunika_dungeon_waystones keluar contents, START lalu /iareload dan /iazip. Bila ID lama tidak ada, gunakan Lodestone fallback sampai aset yang benar dipasang. Jangan menghapus anchors.yml atau mengganti database.

## Bukti pemeriksaan

JSON/YAML, seluruh texture reference, integritas PNG, rotasi vanilla yang valid, dan kesamaan elements/UV/tekstur dengan arsip asli sudah diperiksa. JAR ItemsAdder berbayar dan client Java tidak tersedia di lingkungan ini: runtime/model appearance masih NOT TESTED. File ini tidak mengklaim hasil screenshot atau posisi model yang belum diuji.

Referensi primer konfigurasi: https://wiki.itemsadder.com/adding-content/items/modern-items-creation/ ; https://wiki.itemsadder.com/adding-content/furnitures/ ; https://github.com/ItemsAdder/wiki/blob/main/plugin-usage/beginners/configs-and-resourcepack.mdx . Binding command berasal dari source plugin RC1 hasil audit.
