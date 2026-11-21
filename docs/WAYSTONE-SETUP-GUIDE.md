# Waystone setup

Keep CdrWaystone 0.7.0 and CdrWaystoneBedrock 0.2.0 installed separately. Their normal personal/public/global/survival network is not converted into dungeon entry. Only configured UUIDs are intercepted.

1. Create a dedicated permanent admin Waystone using the existing Waystone plugin. It must be hidden from its normal network and not bound to the legacy DungeonModule. This build does not invent or document unverified Waystone creation commands.
2. Look directly at its anchor and run `/adg gateway`; copy the reported UUID. Run `/adg gateway check` to verify public ABI. Existing local bytecode inspection is recorded; actual server interaction must still be tested.
3. Edit `integrations/waystone.yml`: keep `enabled: false` while setting `gateway-id`, `hub` mapping (`world,x,y,z`), `interaction-radius` (default 6), and `return-waystones` mapping of return UUID to `floor/arena`.
4. One hub opens all configured Shared/Private floors. Hub location must be in the same world, near the hub anchor and outside every arena; every arena's exit must equal that configured hub point exactly.
5. Each return stone must be a dedicated permanent hidden admin stone physically inside its own configured arena. Registered return actions require a living legitimate participant in that exact floor/arena. Spectators/outsiders are rejected.
6. Configure battle/mode/loot flags, actual mobs/rewards and permissions, then set gateway enabled and `/adg reload all`. Failed initialization locks gateway. It is not auto-enabled by installation.
7. Test normal survival/personal/public/global teleport first, then Java main-hand anchor/entity interactions, Bedrock renderer interaction and `/adg gateway open` while looking at the anchor. The fallback still validates location, world, radius, permission and arena membership.

No claim of perfect Bedrock interaction compatibility is made. Removed, unhidden, moved or inaccessible stones must invalidate access. Double-click/offhand/remote GUI tests remain mandatory.
