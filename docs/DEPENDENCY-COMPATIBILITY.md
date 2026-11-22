> Panduan dasar RC1 tetap berlaku kecuali versi JAR dan alur admin yang diperbarui pada **GUI-SETUP-ID.md**. RC2: Dungeon 1.0.3-RC2; core/Bedrock 1.0.1-RC2.

# Dependency

| Dependency | Bukti | Batas |
|---|---|---|
| JDK21 Temurin21.0.13.1 | Compile dan server nyata | Semua class plugin major65 |
| Paper API1.21.11 snapshot91 | Dependency resmi; build nyata | Paper server132 startup/restart diuji |
| Geyser API2.12.1-20261120.123911-34 | Artifact asli repo OpenCollab; compile typed | API eksperimental, tidak seluruh2.12.x dijamin |
| Geyser-Spigot2.12.1 build1280 | Artifact resmi checksum diverifikasi; registrasi2/2 dan shutdown | Zero real clients; ViaVersion diperlukan pada target1.21.11 |
| SnakeYAML2.3 / SQLiteJDBC3.46.1.3 | Maven nyata dan integrationtests | Tidak ada DB produksi |
| LuckPerms API5.4 | Compile provided | Grant runtime belum diuji |
| ItemsAdder4.0.16 | Reflection existing API; originalmodelIDs | JAR vendor tidak tersedia, API/runtime **NOT TESTED** |
| MythicMobs5.12.1 | Reflection existing API | JAR vendor tidak tersedia, boss runtime **NOT TESTED** |
| Floodgate, ModelEngine, NusaKarsa, AE, Multiverse, WG | Soft integration/policy dipertahankan | Versi server aktual tidak tersedia; **NOT TESTED** |
| ViaVersion | Geyser build1280 startup memberi warning protokolJava26.3 | Tidak terpasang pada test, join client belum diuji |

Dependency provided tidak dibundel ke3JAR. Paper plugin.yml menyediakan libraries YAML/SQLite untuk Dungeon. Test memakai dependency asli; tidak ada stubBukkit/Geyser untuk klaim kompatibilitas.

Referensi primer: https://repo.papermc.io/repository/maven-public/ ; https://repo.opencollab.dev/main/ ; https://geysermc.org/wiki/geyser/custom-entities/ ; https://github.com/GeyserMC/Geyser ; https://fill.papermc.io/v3/projects/paper/versions/1.21.11/builds ; https://download.geysermc.org/v2/projects/geyser/versions/2.12.1/builds/1280 . Hash artifact ada pada bukti build.
