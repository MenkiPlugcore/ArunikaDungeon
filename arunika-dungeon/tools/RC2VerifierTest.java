import java.nio.file.*;
import java.io.*;
import java.util.jar.*;
public class RC2VerifierTest {
 static void reject(Path p,Path source,String expect)throws Exception {try {VerifyArtifact.checkJar(p,source);throw new AssertionError("accepted invalid archive");} catch(IOException e){if(!e.getMessage().contains(expect))throw e;System.out.println("PASS rejected: "+expect);}}
 public static void main(String[] args)throws Exception {
  Path tmp=Files.createTempDirectory("rc2-verifier-"); Path source=Path.of("src/main/java");
  try{
   Path empty=tmp.resolve("empty.zip");try(var z=new JarOutputStream(Files.newOutputStream(empty))){} reject(empty,source,"No plugin classes");
   Path extra=tmp.resolve("extra.zip");try(var z=new JarOutputStream(Files.newOutputStream(extra))){z.putNextEntry(new JarEntry("org/bukkit/Bukkit.class"));z.write(0);z.closeEntry();}reject(extra,source,"Unexpected bundled class");
   Path incomplete=tmp.resolve("incomplete.zip");var manifest=new Manifest();manifest.getMainAttributes().putValue("Manifest-Version","1.0");manifest.getMainAttributes().putValue("Implementation-Version",VerifyArtifact.VERSION);try(var z=new JarOutputStream(Files.newOutputStream(incomplete),manifest)){String n="site/cadera/arunika/dungeon/core/StagingPolicy.class";z.putNextEntry(new JarEntry(n));z.write(Files.readAllBytes(Path.of(args[0]).resolve(n)));z.closeEntry();}reject(incomplete,source,"Missing compiled source class");
   try{VerifyArtifact.dependencyReport("");throw new AssertionError("accepted missing dependencies");}catch(IOException e){System.out.println("PASS rejected: missing dependencies");}
  }finally{try(var w=Files.walk(tmp)){for(var p:w.sorted(java.util.Comparator.reverseOrder()).toList())Files.deleteIfExists(p);}}
 }
}
