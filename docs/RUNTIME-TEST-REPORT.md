# Pengujian server nyata

Status: **startup / restart / registrasi Geyser PASS; gameplay dan client acceptance belum diuji.**

Lingkungan terisolasi: Temurin Java21.0.13.1, Paper1.21.11 build132, Geyser2.12.1 build1280, tiga JAR RC1. Hash JAR pada testserver sama persis dengan JAR deliverable. Dua boot terakhir sukses dengan database ready, fresh Exchange locked, command diagnostics/status, reload config, dan shutdown ketiga plugin. Geyser menunjukkan `registered=true (2/2)`, `packValid=true`, `packsRequired=true`, `callbackFailed=false`. Tidak ada pemain, proxy aktif=0.

Provider yang tidak tersedia: ItemsAdder, MythicMobs, Floodgate, ModelEngine, NusaKarsa, AE, Multiverse-Inventories, WG. Battle dan gateway tetap disabled pada smoke test. Tidak ada client Java/Bedrock Android.

Log Geyser menyatakan Java26.3 diperlukan, sehingga targetPaper1.21.11 memerlukan ViaVersion yang kompatibel. Testnetwork memunculkan warning auth/version/locale/signaling; autentikasi Bedrock dinonaktifkan hanya pada server uji tanpa client agar bootstrap dapat diperiksa. Setelan tersebut tidak didistribusikan atau direkomendasikan untuk produksi. Tidak ada klaim keseluruhan stack tanpa warning.

| # | Skenario wajib | Bukti otomatis | Runtime dengan pemain |
|---|---|---|---|
| 1 | Shared entry | SharedTest | NOT TESTED |
| 2 | Private Solo | SimpleWaystonePartyTest | NOT TESTED |
| 3 | Private Party dua pemain | PartySizeTest | NOT TESTED |
| 4 | Private Party lima pemain | PartySizeTest | NOT TESTED |
| 5 | Leader tanpa semua anggota Ready | SimpleWaystonePartyTest | NOT TESTED |
| 6 | Undangan lintas world | SimpleWaystonePartyTest | NOT TESTED |
| 7 | Anggota beda world dikecualikan | SimpleWaystonePartyTest | NOT TESTED |
| 8 | Anggota offline | PrivateTest / SharedTest | NOT TESTED |
| 9 | Anggota belum Ready | SimpleWaystonePartyTest | NOT TESTED |
| 10 | Hanya tiket leader | PartySizeTest | NOT TESTED |
| 11 | Ticket double-spend | PrivateTest / TicketConcurrencyTest | NOT TESTED |
| 12 | Ticket refund | PrivateTest / TicketConcurrencyTest | NOT TESTED |
| 13 | Crash recovery | PrivateTest / ExchangeTest / RecoveryJournalTest | NOT TESTED |
| 14 | Duplicate loot | LootTest / BattleTest / AccountingGuardTest | NOT TESTED |
| 15 | Arena unavailable | SelectedArenaTest | NOT TESTED |
| 16 | Cooldown boss | SharedTest | NOT TESTED |
| 17 | Arena spesifik | SelectedArenaTest / SimpleWaystonePartyTest | NOT TESTED |
| 18 | Return Waystone | GatewayTest (policy) | NOT TESTED |
| 19 | Unauthorized Return | GatewayTest (policy) | NOT TESTED |
| 20 | Teleport bypass | TeleportSafetyTest / GatewayTest | NOT TESTED |
| 21 | Java model interaction | Static source review; bukan bukti runtime | NOT TESTED |
| 22 | Bedrock model interaction | Static source review; bukan bukti runtime | NOT TESTED |
| 23 | Duplicate GUI prevention | GatewayPhase6Test / GatewayMenuStyleTest (policy) | NOT TESTED |
| 24 | Model lifecycle | Static source review; bukan bukti runtime | NOT TESTED |
| 25 | Chunk unload/reload | Static source review; bukan bukti runtime | NOT TESTED |
| 26 | Player disconnect | Static source review; bukan bukti runtime | NOT TESTED |
| 27 | World change | Static source review; bukan bukti runtime | NOT TESTED |
| 28 | Server restart | Ledger/config/SQL reopen tests | PASS dua boot, tanpa sesi/pemain |
| 29 | Configuration migration | ConfigTest / DatabaseMigrationTest / GatewayImportTest | PASS config baru/reload; bukan snapshot produksi |
| 30 | Plugin shutdown | ShutdownTest | PASS ketiga plugin, tanpa sesi |
| 31 | Dependency missing | Static source review; bukan bukti runtime | PASS startup tanpa vendor providers; mode battle nonaktif |
| 32 | Invalid resource pack | PackValidatorTest | NOT TESTED |
| 33 | Geyser registration failure | PackValidatorTest (API signature only) | Registrasi sukses 2/2; failure injection runtime NOT TESTED |
| 34 | Survival coexistence | Static source review; bukan bukti runtime | NOT TESTED |

Benchmark spark idle/5/15/30/50 pemain, TPS/MSPT/CPU/heap/GC, entity interactions, boss dan physical loot pada stack lengkap: **NOT TESTED**. Gate Production Ready tetap terbuka.
