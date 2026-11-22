> Laporan historis RC1. Pengujian upgrade GUI/nama ada di **RC2-TEST-REPORT.md**.

# Keamanan dan anti-dupe

Ledger tiket tetap serial+generation durable. Hold sebelum debit, commit sebelum konsumsi fisik, exposure durable sebelum reward. Refund menaikkan generation; serial fisik lama tidak valid. EXPOSED/CONSUMED tidak dapat direfund. Hanya tiket leader digunakan.

Uji PrivateTest, SimpleWaystonePartyTest, PartySizeTest dan TicketConcurrencyTest meliputi tiket/refund/reopen/dua-lima pemain. Uji concurrency baru:50 percobaan hold satu serial melalui8worker, tepat1berhasil. Ini uji ledger asli dengan file temporer, bukan simulasi50client Minecraft.

Exchange SQLite menggunakan WAL, synchronousFULL, PreparedStatement, transaksi stock+limit, boundedworker128. Inventory save dan SQLite tidak dapat atomic bersama; status ambigu tetap terkunci dan tidak replay/refund otomatis. Stress asliJDBC:50request melalui worker tunggal, stock10 menghasilkan tepat10transaksi berhasil; reopen pending dan integrity_check diuji.

Gateway memeriksa UUID/role/arena/world/distance/permission/session. GUI cancellation/deferred click, validasi ulang dan cooldown gesture mencegah double action; tidak ada akses survival network. Raytrace dan dedicated ownership ditambah. Return sukarela mengeliminasi peserta tanpa refund normal.

Teleport permit internal exact-coordinate, sekali pakai. Teleport luar peserta dibatasi arena dan command teleport ditolak; pearl/chorus/skill harus tetap berada di arena. Tujuan internal ditolak jika blok tubuh/kepala padat, hazard, height/border invalid atau chunk belum loaded.

Keterbatasan: jalur event lengkap dengan ItemsAdder, MythicMobs, ModelEngine, NusaKarsa, AE, WG, Multiverse-Inventories dan client belum diuji. Tidak ada klaim anti-dupe absolut untuk seluruh107plugin. Database produksi tidak diakses/direset. Data/credential/playerUUID produksi tidak dimasukkan ke deliverable.
