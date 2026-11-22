# Audit lengkap — RC1 / STAGING

Audit dimulai21November2026 dan paket diselesaikan22November2026. Ketujuh belas checksum input cocok sebelum perubahan. Source canonical menggunakan Dungeon reconstructed Phase7+8+9, core Phase10, dan Bedrock Phase11. ZIP input asli disimpan sebagai provenance; build tidak memerlukan rantai patch tersebut.

Area yang diperiksa meliputi pembagian tanggung jawab modul, API/lifecycle Geyser, registry ownership, interaksi dan GUI, pilihan arena, party/readiness, tiket/refund/reward exposure, SQLite/WAL/migrasi, izin teleport, physical loot, provider/chunk cleanup, scheduler/cache, command/permission, pack namespace, dan kesesuaian source dengan binary.

BUG-FIX-REPORT mencatat perubahan dan bukti masing-masing. Ketiga JAR dibangun ulang dengan JDK21 dan dependency asli. Semua class major65, CRC/entry JAR valid, class bytes cocok dengan clean-build output, seluruh source class dan resource tersedia, dan tidak ada dependency API eksternal dibundel.33 kasus/suite JUnit serta9 tes verifier/monitor tambahan lulus.

Startup, dua boot, command, database, reload dan shutdown diuji pada Paper1.21.11/Geyser1280. Registrasi entity Geyser2/2 berhasil. Tidak ada client Java/Android atau provider berbayar dalam lingkungan tersebut. Gameplay dan tampilan client tetap NOT TESTED. Runtime report menyertakan warning protokol dan batas testnetwork.

Tidak ada database produksi, credential atau registry survival yang diubah. Source baru tersusun dalam tiga modul Maven, resource-packs, docs dan workflow build. Release gate masih STAGING karena acceptance client/gameplay/performance belum terpenuhi.

Baca laporan regresi, runtime, keamanan, performa, dependency, migrasi dan rollback untuk batas pembuktian yang tepat.
