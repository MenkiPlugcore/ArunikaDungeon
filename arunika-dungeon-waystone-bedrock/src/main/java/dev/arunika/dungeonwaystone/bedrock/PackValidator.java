package dev.arunika.dungeonwaystone.bedrock;

import com.google.gson.*;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;
import java.io.*;

/** Bounded validation of the exact dungeon namespace. No files are extracted. */
public final class PackValidator {
    private PackValidator(){}
    public static boolean valid(Path path){
        try(ZipFile zip=new ZipFile(path.toFile())){
            if(Files.size(path)>16_000_000||zip.size()>256)return false;
            JsonObject manifest=json(zip,"manifest.json");
            if(!manifest.getAsJsonObject("header").get("uuid").getAsString().equals("370b11e6-501f-529c-a183-85deb81d6246"))return false;
            Set<String> found=new HashSet<>();
            for(String role:List.of("hub","return")){
                var d=json(zip,"entity/arunika_dungeon_waystone_"+role+".entity.json")
                  .getAsJsonObject("minecraft:client_entity").getAsJsonObject("description");
                String id=d.get("identifier").getAsString();
                if(!id.equals("arunika_dungeon:waystone_"+role)||!found.add(id))return false;
                for(var e:d.getAsJsonObject("textures").entrySet())if(zip.getEntry(e.getValue().getAsString()+".png")==null)return false;
                String skin=role.equals("hub")?"blackstone":"portstone";
                String geo=d.getAsJsonObject("geometry").get("default").getAsString();
                var geometry=json(zip,"models/entity/waystone_"+skin+".geo.json").getAsJsonArray("minecraft:geometry");
                boolean match=false;for(var g:geometry)if(g.getAsJsonObject().getAsJsonObject("description").get("identifier").getAsString().equals(geo))match=true;
                if(!match)return false;
                var controllers=json(zip,"render_controllers/waystone_"+skin+".render_controllers.json").getAsJsonObject("render_controllers");
                for(var c:d.getAsJsonArray("render_controllers"))if(!controllers.has(c.getAsString()))return false;
            }
            return found.size()==2;
        }catch(Exception e){return false;}
    }
    private static JsonObject json(ZipFile zip,String name)throws IOException{
        ZipEntry e=zip.getEntry(name);if(e==null||e.getSize()<0||e.getSize()>2_000_000)throw new IOException("Invalid entry "+name);
        try(InputStream in=zip.getInputStream(e)){
            byte[] bytes=in.readNBytes(2_000_001);if(bytes.length>2_000_000)throw new IOException("Oversized JSON");
            return JsonParser.parseString(new String(bytes,java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }
}
