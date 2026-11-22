> Laporan historis RC1. Pengujian upgrade GUI/nama ada di **RC2-TEST-REPORT.md**.

# Pengujian regresi

Build Java 21 pada 21 November 2026; paket diselesaikan 22 November 2026. **33 kasus/suite JUnit lulus, tanpa failure/error/skip.** Sebagian kasus adalah adapter untuk suite main-method lama yang berisi banyak assertion; angka 33 bukan jumlah skenario gameplay dengan client nyata.

| Modul | Suite | Tests | Failure | Error | Skip |
|---|---|---:|---:|---:|---:|
| arunika-dungeon-waystone-bedrock | audit.RegressionSuiteTest | 1 | 0 | 0 | 0 |
| arunika-dungeon-waystone-bedrock | dev.arunika.dungeonwaystone.bedrock.PackValidatorTest | 4 | 0 | 0 | 0 |
| arunika-dungeon | audit.RegressionSuiteTest | 22 | 0 | 0 | 0 |
| arunika-dungeon | site.cadera.arunika.dungeon.config.GatewayImportTest | 1 | 0 | 0 | 0 |
| arunika-dungeon | site.cadera.arunika.dungeon.privategame.PartySizeTest | 2 | 0 | 0 | 0 |
| arunika-dungeon | site.cadera.arunika.dungeon.privategame.TicketConcurrencyTest | 1 | 0 | 0 | 0 |
| arunika-dungeon | site.cadera.arunika.dungeon.shared.SelectedArenaTest | 1 | 0 | 0 | 0 |
| arunika-dungeon-waystone | dev.arunika.dungeonwaystone.NodeTest | 1 | 0 | 0 | 0 |

Tambahan: 4 negative-case verifier JAR lulus setelah fixture manifest diperbaiki; 5 unittest Python smoke monitor lulus. Main-method suite lama dieksekusi menggunakan JUnit, tidak hanya dikompilasi.

SQLite menggunakan JDBC asli dan temporary files: stock, limits, cancel/debit/delivery, reopen recovery, corruption guards, overflow, 50-request stress. Ticket concurrency memakai ledger asli: 50 hold pada satu serial, tepat satu berhasil. Ini pengujian transaksi/model, bukan benchmark 50 pemain.

Artifact checker memeriksa CRC ZIP, nama entry unik, seluruh class major65, semua source class ada, semua byte class sama dengan target/classes hasil clean build, semua resource sama dengan source, metadata dan main class benar, serta tidak ada external API class dibundel.

`test-exchange-sql.py` historis memerlukan snapshot database lama yang tidak ada dalam input: NOT TESTED. Jalur transaksi produksi yang sama diuji melalui JDBC temporary database; data produksi tidak dibutuhkan/diakses.
