package dev.arunika.dungeonwaystone.bedrock;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.*;
import java.util.zip.*;
class PackValidatorTest {
 @TempDir Path temp;
 Path pack(boolean omitTexture)throws Exception{
  Path src=Path.of("../resource-packs/bedrock"),out=temp.resolve(omitTexture?"missing.mcpack":"valid.mcpack");
  try(var z=new ZipOutputStream(Files.newOutputStream(out));var files=Files.walk(src)){
   for(var p:files.filter(Files::isRegularFile).sorted().toList()){
    String name=src.relativize(p).toString().replace('\\','/');
    if(omitTexture&&name.endsWith(".png"))continue;
    z.putNextEntry(new ZipEntry(name));z.write(Files.readAllBytes(p));z.closeEntry();
   }
  }return out;
 }
 @Test void validatesActualPackSources()throws Exception{assertTrue(PackValidator.valid(pack(false)));}
 @Test void missingTexturesKeepFallback()throws Exception{assertFalse(PackValidator.valid(pack(true)));}
 @Test void invalidOrMissingArchiveKeepsFallback()throws Exception{Path p=temp.resolve("bad.mcpack");Files.writeString(p,"bad");assertFalse(PackValidator.valid(p));assertFalse(PackValidator.valid(temp.resolve("missing")));}
 @Test void officialApiContainsTheRequiredSignatures()throws Exception{
  assertNotNull(org.geysermc.geyser.api.event.EventRegistrar.class.getMethod("of",Object.class));
  assertNotNull(org.geysermc.geyser.api.event.EventBus.class.getMethod("unregisterAll",Object.class));
  assertNotNull(org.geysermc.geyser.api.event.java.ServerSpawnEntityEvent.class.getMethod("preSpawnConsumer",java.util.function.Consumer.class));
  assertNotNull(org.geysermc.geyser.api.event.java.ServerSpawnEntityEvent.class.getMethod("definition",org.geysermc.geyser.api.entity.definition.GeyserEntityDefinition.class));
 }
}
