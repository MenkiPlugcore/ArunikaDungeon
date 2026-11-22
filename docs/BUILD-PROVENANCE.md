> Laporan historis RC1. Pengujian upgrade GUI/nama ada di **RC2-TEST-REPORT.md**.

# Build provenance

Input ZIP SHA256: e9d0956a018cdce905bb5380dcb9a0d04957fe63769c588060ebe3644aa97ace

Sourcebaseline dan urutan rekonstruksi ada pada ZIP input/04_BUILD_PROVENANCE/REPRODUKSI_SOURCE.md. Repo main pada awal audit:35f397807cc4f848f589673a9bf1cca33f4cfa37. BedrockPhase11 snapshot:aae449c44639204c60f244338b29cea9a2de40d1.

Dependency asli GeyserAPI:f6bed4f84b12c666b0963d48ec369edcaa624fb16ccda0e96989d1b1d3e51ce4

Runtime provider inputs:

```json
[
  {
    "file": "tools/paper.jar",
    "sha256": "5ffef465eeeb5f2a3c23a24419d97c51afd7dbb4923ff42df9a3f58bba1ccfba",
    "url": "https://fill-data.papermc.io/v1/objects/5ffef465eeeb5f2a3c23a24419d97c51afd7dbb4923ff42df9a3f58bba1ccfba/paper-1.21.11-132.jar"
  },
  {
    "file": "tools/Geyser-Spigot.jar",
    "sha256": "8b01d2f8c7ef7de1dda6e8acd5d3e12ad377767b4d842580dac443a5f99031b2",
    "url": "https://download.geysermc.org/v2/projects/geyser/versions/2.12.1/builds/1280/downloads/spigot"
  }
]
```
