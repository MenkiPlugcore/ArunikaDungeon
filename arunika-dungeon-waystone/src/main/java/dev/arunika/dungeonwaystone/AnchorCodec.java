package dev.arunika.dungeonwaystone;
import java.io.*;
import java.util.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
/** Backward-compatible metadata codec. A failed decode never writes or resets the registry. */
public final class AnchorCodec {
 public record Data(Map<UUID,Node> nodes,Map<UUID,String> names){public Data{nodes=Collections.unmodifiableMap(new LinkedHashMap<>(nodes));names=Collections.unmodifiableMap(new LinkedHashMap<>(names));}}
 private AnchorCodec(){}
 public static Data read(File file)throws IOException{
  if(!file.exists())return new Data(Map.of(),Map.of());
  YamlConfiguration y=new YamlConfiguration();try{y.load(file);}catch(Exception e){throw new IOException("Invalid registry YAML",e);}return decode(y);
 }
 public static Data decode(YamlConfiguration y)throws IOException{
  if(y.getInt("schema",-1)!=1)throw new IOException("Unsupported storage schema");var root=y.getConfigurationSection("nodes");
  if(root==null){if(y.contains("nodes"))throw new IOException("Corrupt nodes section");return new Data(Map.of(),Map.of());}
  if(root.getKeys(false).size()>10000)throw new IOException("Oversize registry");
  Map<UUID,Node> nodes=new LinkedHashMap<>();Map<UUID,String> labels=new LinkedHashMap<>();Set<String> blocks=new HashSet<>(),returns=new HashSet<>();int hubs=0;
  for(String raw:root.getKeys(false))try{
   UUID id=UUID.fromString(raw);ConfigurationSection s=root.getConfigurationSection(raw);if(s==null||nodes.containsKey(id))throw new IllegalArgumentException("Invalid/duplicate UUID entry");
   for(String coord:List.of("x","y","z"))if(!s.isInt(coord))throw new IllegalArgumentException("Missing/invalid coordinate "+coord);
   Node n=new Node(id,Node.Role.valueOf(s.getString("role","")),s.getString("target",""),UUID.fromString(s.getString("world-id","")),s.getString("world-name",""),s.getInt("x"),s.getInt("y"),s.getInt("z"));
   if(!blocks.add(n.worldId()+":"+n.x()+":"+n.y()+":"+n.z()))throw new IllegalArgumentException("Duplicate block");
   if(n.role()==Node.Role.HUB&&++hubs>1)throw new IllegalArgumentException("Multiple HUB nodes");
   if(n.role()==Node.Role.RETURN&&!returns.add(n.target()))throw new IllegalArgumentException("Duplicate RETURN arena");
   String label=NodeNames.clean(s.getString("name",NodeNames.fallback(n)));NodeNames.requireUnique(labels,id,label);labels.put(id,label);nodes.put(id,n);
  }catch(RuntimeException e){throw new IOException("Invalid entry "+raw,e);}
  return new Data(nodes,labels);
 }
 public static YamlConfiguration encode(Map<UUID,Node> nodes,Map<UUID,String> labels){
  var y=new YamlConfiguration();y.set("schema",1);Map<UUID,String> used=new LinkedHashMap<>();
  for(Node n:nodes.values()){
   String label=NodeNames.clean(labels.getOrDefault(n.id(),NodeNames.fallback(n)));NodeNames.requireUnique(used,n.id(),label);used.put(n.id(),label);String p="nodes."+n.id()+".";
   y.set(p+"name",label);y.set(p+"role",n.role().name());y.set(p+"target",n.target());y.set(p+"world-id",n.worldId().toString());y.set(p+"world-name",n.worldName());y.set(p+"x",n.x());y.set(p+"y",n.y());y.set(p+"z",n.z());
  }return y;
 }
}
