# RC6 final validation
This is a staging candidate, not a production approval.
The official build includes a 50-request SQLite test using the production single-owner model (expected exactly 10 stock deliveries), pending transaction reopen, debit/no-refund and integrity checks. This is not a 50-client Minecraft test.
## Isolated Paper
Obtain official Paper 1.21.11 locally and read Minecraft EULA. If accepted, run from patched source:
  python3 tools/smoke-paper.py --paper /absolute/path/paper.jar --plugin target/verified/ArunikaDungeon-1.0.0-RC6.jar --output /absolute/path/new-smoke --accept-eula
The output must not exist. Loopback only; two boots, fresh Exchange locked without currency, diagnostics, reload, shutdown, marker persistence. No third-party providers are covered.
## Manual acceptance
Use backed-up staging with actual MythicMobs, Waystone, Geyser/Floodgate and Nusa Karsa. Test Java and Bedrock physical hub click and return, survival Waystone unchanged, 5-player private party, shared cooldown persistence, simultaneous sessions, ticket replay rejection, pending transaction review, physical loot pickup isolation, disconnect and restart recovery. Test with real clients ramping 5/15/30/50, capture spark profiles/TPS/MSPT/heap before and during; do not treat SQLite request count as player count. Verify no stuck sessions or orphan entities. Roll back by stopping server and restoring matching previous JAR and backed-up data together. Do not switch on public gateway until acceptance is complete.
