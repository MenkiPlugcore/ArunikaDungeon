# Performa

Tidak ada benchmark TPS/MSPT/CPU/RAM50pemain. Tidak ada klaim persentase peningkatan.

Renderer satu task interval20tick(default), clamp10..100; maksimal64proxy, radius8..96. Hanya node registered loaded di sekitar client yang diproses; tidak full-world scan. Cache connection max1024, tombstone max256; preparedvisibility dibersihkan saat quit/world/chunk/disable. Delta show/hide menghindari pengulangan packet ketika state sama. Refleksi Geyser/core pada renderer dihapus.

Core visual refresh satu task100tick dengan maxnode64default/128cap. Nearby scan hanya radius kecil pada registerednode. Warning model dibatasi sekali pernode per60detik. Tidak force-load chunk.

Dungeon memelihara satu battle scheduler dan maintenance1detik; spawn budget/batch dan performanceguard dipertahankan. GUI refresh maksimal16menu permaintenance, tanpa animasi tick. SQLite Exchange satu workerqueue128; DB foundation async.

Risiko tersisa: fsync ledger tiket dan cooldown masih sinkron pada main thread di transisi transaksi, scan proximity model tergantung furnitureprovider, dan biaya plugin eksternal belum diprofil. Jangan menyatakan lag-free pada server100+plugin. Tidak ada perubahan pipeline ledger besar karena memerlukan pembuktian crash-safety lanjutan.

Tahap idle/5/15/30/50pemain dengan spark: **NOT TESTED**. Catat TPS/MSPT, heap,CPU,GC,tasks,trackedmobs,proxies,DBqueue,sessions pada serverstaging sesungguhnya.
