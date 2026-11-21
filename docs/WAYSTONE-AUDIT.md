# Static archive audit, v0.8.0

Sources: archive-2026-11-21T054813Z.tar.gz; `jar tf`, `unzip -p plugin.yml` and `javap -p/-c/-v` on the two original binaries. No source code or live Waystone database/config in archive. No bytecode was changed.

CdrWaystone 0.7.1-InventorySafety: registry.find(Location), registry.get(UUID), WaystoneData.id/location/isAdmin/permanent/coreActive/alwaysActive/collisionOwned, VisualService.waystoneFromEntity(Entity)/isAnchorValid, AccessService.canAccess, TravelGuardService.status, TeleportService.isWarping/isSuppressed and DungeonModule.bound/hidden/channeling are public in the inspected binaries. Original WaystoneListener block and furniture listeners are HIGH with ignoreCancelled=true. Adapter listens at NORMAL, cancels only registered dedicated UUID interaction, and leaves all other stones to original handlers. Managed barrier blocks resolve to the anchor below only when collisionOwned matches. Adapter uses reflection against exact public ABI; no private fields or data modifications. Legacy sourceProblem also checks origin radius, permission, suppression and loaded anchor; bridge implements its own proximity and guard checks. Dedicated stones must be hidden and not have a legacy binding.

CdrWaystoneBedrock 0.2.1-CoreCompatibility: depends on CdrWaystone and Geyser-Spigot. It uses Geyser custom definitions/renderer plus ArmorStand marker proxies; lambda$sync$6 sets marker, nonpersistent, noncollidable; no Waystone PDC marker set there. Original visual resolver checks its own PDC. Therefore no unsupported claim that proxy clicks work; native anchor and target-checked fallback are provided. No modification to renderer, definitions, skins or pack. Full Bedrock runtime verification required.

Original archive binary SHA-256:
4b043409c1e53dc62db7c15d6fa297c4823a8f352b591f84528d15edc22a854e  CdrWaystone-0.7.1-InventorySafety.jar
f0f335804c9bf6f11badcba7363fc66b8050518b463bf133332caa9060f37557  CdrWaystoneBedrock-0.2.1-CoreCompatibility.jar
