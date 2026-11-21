# Nusa Karsa dependency audit / v0.9.0
Inputs: original archive 20261121T054813Z (four legacy JARs, no Nusa/Mythic JAR); library artifact ArunikaNusaKarsa-1.2.10-GUI-Polish-Java-Bedrock.zip, including NusaKarsa source and 1.2.10 JAR. GitHub main only contains IMPORT.md and workflow directory, not full source. No Nusa binaries changed. Exact deployed Nusa version is unknown.

Findings from source (id.arunika.nusakarsa):
- `NusaKarsa` has private RaceStore and ClassProgression. Public `hasSelectedRace(UUID)` checks identity only. No public Race XP award API; `RaceStore.award` updates ClassProgress, using SkillTree.Source, and its advancement claim format only accepts minecraft namespace paths. Do not fabricate those claims or edit profile files directly.
- `ClassProgression.command` supports `/nkxp grant <player> <xp> [source]` with `COMBAT`, permission checks, online exact-name lookup, selected class, progression/disabled-worlds gates and 1..100000 range. Command returns boolean for handling, not confirmed XP amount. Class bonuses can modify award. It writes actual profile data (synchronous critical path).
- `Awakening.death` already handles eligible `Monster` kill and direct killer plus its own trial/expiry checks; no duplicate artificial kills or steps. Custom non-Monster and indirect kills may not count. Native additional awakening progress is deliberately not dispatched.
- `ClassAbilities`, `ElementalRunes`, `SpecialCombat` rely on Bukkit damage event types and entity/weapon predicates. Native melee/arrow damage and existing equipment metadata are left untouched; no new stat or consumption formulas. Early and late arena event guards prevent obvious cross-session direct hits. Special spell entities, pets and summoned custom mobs may be blocked by ownership policy; no broad bypass. Skill/Forge/Rune/Legendary weapon compatibility not certified in runtime. Serialized physical drops preserve ItemStack metadata; custom item ID providers retain earlier implementation.

## Configuration
`integrations/nusa-karsa.yml` is merged automatically:
```
enabled: false
class-exp-per-boss: 0
native-class-xp: false
awakening-progress: false
```
For staging with exactly Nusa Karsa 1.2.10, enable `loot-preview` (collection phase required) and set `enabled: true`, `class-exp-per-boss: 250` (example), `native-class-xp: true`, leave `awakening-progress: false`. Current code only attempts the exact audited plugin version and checks plugin command owner. If different version deployed, adapter does not grant. `/adg status` reports RPG configured and optional registered provider status. Existing native Nusa progression remains independent of this bonus and may still occur in preview unless disabled in its staging world.

After confirmed boss kill, track direct/player-shot damage contribution for active, living players present in the arena. No duplicate event dispatch from repeated boss death; direct admin no-loot test runs don't dispatch additional awards. Bonus queued at boss death; bounded 512 requests across active sessions, one dispatch per battle scheduler pass only when TPS/MSPT guard permits, while recipient remains in Loot Frenzy. Queue items cancelled at arena close/leave, if plugin mismatched or crash; no automatic retry/replay. This is at-most-once attempted dispatch, not durable exactly-once grant; command handler can reject or write fail. Under load or short collection, XP may be skipped (close/full queue logged). Bonus is non-item progression only, no personal loot. Setting `awakening-progress` only sends an event to an explicitly installed custom service, not native Nusa Karsa.

Future custom provider: `DungeonRpgApi` Bukkit ServicesManager contract v1, `BossClear` with session+player unique event ID, mode, floor, class EXP and awakening flag. Provider must implement durable deduplication and safe main-thread callbacks; no provider included. Registered compatible service takes precedence over native command, never both. Provider failures do not restart battle and no retry. Before official release, add provider-based native confirmed results, durable replay handling and alternative class/support/summon attribution if desired. Do not assume Race XP is a separate real resource.

## Runtime gates
Real Paper 1.21.11, MythicMobs, Nusa Karsa, Legendary/MMOItems/ItemsAdder, Geyser, Waystone and database tests outstanding. Test class selection, disabled world, max level, permission, 1/5/30-player boss kill, low TPS queue skip, direct versus projectile, reconnect/leave, awakening trial, each weapon skill, rune/forge gear, custom pet/summon, and verify no duplicate progression or external side effects. No live profiling in this release.
