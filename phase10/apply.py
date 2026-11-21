#!/usr/bin/env python3
"""Fail-closed Phase10 overlay: restore original ItemsAdder model visuals to dungeon-only anchors."""
from pathlib import Path
import sys
root=Path(sys.argv[1])
java=root/"src/main/java/dev/arunika/dungeonwaystone/DungeonWaystonePlugin.java"
s=java.read_text()
def patch(before,after):
    global s
    if s.count(before)!=1: raise SystemExit(f"Patch mismatch {before[:100]!r}, count={s.count(before)}")
    s=s.replace(before,after,1)
patch("private boolean ready;","private boolean ready;\n    private DungeonFurnitureVisuals visuals;")
patch("        getServer().getPluginManager().registerEvents(this, this);",
      "        visuals = new DungeonFurnitureVisuals(this);\n        getServer().getPluginManager().registerEvents(this, this);\n        getServer().getScheduler().runTaskLater(this, () -> { if (ready && visuals != null) visuals.refreshLoaded(); }, 100L);")
patch("@Override public void onDisable() { ready = false; nodes.clear(); byBlock.clear(); }",
      "@Override public void onDisable() { if (visuals != null) visuals.removeAll(true); ready = false; nodes.clear(); byBlock.clear(); }")
patch("if (!ready || block == null || block.getType() != Material.LODESTONE) return null;\n        UUID id = byBlock.get(key(block.getWorld().getUID(),block.getX(),block.getY(),block.getZ()));",
      "if (!ready || block == null || (block.getType() != Material.LODESTONE && block.getType() != Material.BARRIER)) return null;\n        UUID id = byBlock.get(key(block.getWorld().getUID(),block.getX(),block.getY(),block.getZ()));\n        if (id == null && block.getType() == Material.BARRIER) id = byBlock.get(key(block.getWorld().getUID(),block.getX(),block.getY()-1,block.getZ()));")
patch("/** No model entity is interactive. Both Java and Bedrock use native blocks. */\n    public Node getNode(Entity entity) { return null; }",
      "/** Only entity models tagged with our own node UUID are interactive. */\n    public Node getNode(Entity entity) { return ready && visuals != null ? visuals.node(entity) : null; }")
patch("l.getBlock().getType()==Material.LODESTONE;",
      "(l.getBlock().getType()==Material.LODESTONE || l.getBlock().getType()==Material.BARRIER);")
patch('if (action.equals("status")||action.equals("check")) {',
      'if (action.equals("refresh")) { visuals.refreshLoaded(); sender.sendMessage("Refreshed loaded Dungeon Waystone models."); return true; }\n        if (action.equals("status")||action.equals("check")) {')
patch("persist(next);sender.sendMessage(\"Unregistered \"+n.id());",
      "persist(next);visuals.remove(n,true);sender.sendMessage(\"Unregistered \"+n.id());")
patch("/adw [hub|return <floor> <arena>|remove|list|status|inspect|export|check]",
      "/adw [hub|return <floor> <arena>|remove|list|status|inspect|export|check|refresh]")
patch('if(legacyCollision(b.getLocation())){',
      'if(b.getRelative(0,1,0).getType()!=Material.AIR){sender.sendMessage("The block above the Lodestone must be AIR.");return true;}\n        if(legacyCollision(b.getLocation())){')
patch('persist(next);sender.sendMessage("Created "+role+" UUID="+n.id()+" target="+target+". Run /adw export, then /adg reload all.");',
      'persist(next);if(visuals.enabled() && !visuals.materialize(n))sender.sendMessage("Model unavailable; Lodestone fallback active.");sender.sendMessage("Created "+role+" UUID="+n.id()+" target="+target+". Run /adw export, then /adg reload all.");')
patch('List.of("hub","return","remove","list","status","inspect","export","check")',
      'List.of("hub","return","remove","list","status","inspect","export","check","refresh")')
patch("    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void blockBreak",
      '''    @EventHandler public void onChunkLoad(org.bukkit.event.world.ChunkLoadEvent e) {
        for (Node n : getNodes()) if (n.worldId().equals(e.getWorld().getUID()) && (n.x() >> 4) == e.getChunk().getX() && (n.z() >> 4) == e.getChunk().getZ())
            getServer().getScheduler().runTask(this, () -> { if (ready && visuals != null) visuals.materialize(n); });
    }
    @EventHandler public void onProviderEnabled(org.bukkit.event.server.PluginEnableEvent e) {
        if (e.getPlugin().getName().equals("ItemsAdder"))
            getServer().getScheduler().runTaskLater(this, () -> { if (ready && visuals != null) visuals.refreshLoaded(); }, 80L);
    }
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void blockBreak''')
java.write_text(s)
pom=root/"pom.xml"
p=pom.read_text()
assert p.count("0.1.0")==2
pom.write_text(p.replace("0.1.0","0.2.0"))
meta=root/"src/main/resources/plugin.yml"
s=meta.read_text()
assert s.count("version: '0.1.0'")==1
s=s.replace("version: '0.1.0'","version: '0.2.0'")
s=s.replace("softdepend: [CdrWaystone]","softdepend: [CdrWaystone, ItemsAdder]")
s=s.replace("export|check]","export|check|refresh]")
meta.write_text(s)
config=root/"src/main/resources/config.yml"
c=config.read_text()
assert "visuals:" not in c
config.write_text(c+"\nvisuals:\n  enabled: true\n  hub-skin: blackstone\n  return-skin: portstone\n  rotation-degrees: 0\n")
assert (root/"src/main/java/dev/arunika/dungeonwaystone/DungeonFurnitureVisuals.java").exists()
assert (root/"src/main/resources/skins.yml").exists()
print("Phase10: original furniture models integrated without touching ArunikaDungeon or survival Waystone data.")
