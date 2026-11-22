# HUB dan RETURN

Gunakan Lodestone baru yang tidak terdaftar pada CdrWaystone survival. Pastikan blok di atasnya AIR.

- Lihat Lodestone HUB: `/adw hub`.
- Di dalam arena yang sesuai, lihat Lodestone RETURN: `/adw return queen_bee shared_1` (contoh ID; gunakan ID arena nyata).
- Satu HUB total dan satu RETURN per floor/arena; registry memakai world UUID dan node UUID.
- Default Java HUB Blackstone, RETURN Portstone. ItemsAdder ID berasal dari skins.yml lama. Jika provider/model tidak tersedia, anchor Lodestone tetap dipertahankan.
- Berdiri di lokasi pulang yang aman dekat HUB lalu `/adw export`. Jangan berdiri di dalam barrier model. Kemudian `/adg gateway import`; tidak perlu menyalin UUID manual.
- `/adw move <uuid>` mempertahankan UUID. Load chunk asal terlebih dahulu. Setelah pindah, export/import lagi; RETURN harus tetap di batas arena yang terikat.
- `/adw model hub blackstone` atau `/adw model return portstone` mengatur model Java. Renderer Bedrock dalam paket ini hanya memetakan dua model original tersebut.

Klik melalui dinding ditolak; GUI tetap mengecek jarak, role, UUID, permission dan session. Fallback barrier/Lodestone dan proxy sah dapat menerima interaksi. Bedrock air-tap tetap membutuhkan Floodgate dan pandangan ke Waystone terikat. Konfirmasi klik Android belum tersedia.

Core tidak pernah membuka GUI atau melakukan teleport sendiri. Semua keputusan gameplay berada di ArunikaDungeon.
