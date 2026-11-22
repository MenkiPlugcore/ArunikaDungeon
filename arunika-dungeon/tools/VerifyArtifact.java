import java.io.*;
import java.nio.file.*;
import java.security.*;
import java.util.*;
import java.util.jar.*;
import java.util.regex.Pattern;
import java.util.zip.CheckedInputStream;
import java.util.zip.CRC32;

/** Build gate; no Bukkit stubs, no external libraries, no runtime claims. */
public class VerifyArtifact {
    static void require(boolean ok,String message) throws IOException { if(!ok) throw new IOException(message); }
    static String sha(Path p) throws Exception {
        var md=MessageDigest.getInstance("SHA-256");
        try(var in=Files.newInputStream(p)){byte[] b=new byte[65536];int n;while((n=in.read(b))!=-1)md.update(b,0,n);}
        return HexFormat.of().formatHex(md.digest());
    }
    static final String BASE="site/cadera/arunika/dungeon/";
    static final String VERSION="1.0.3-RC2";
    static void checkJar(Path jar, Path source) throws Exception {
        try(var zip=new JarFile(jar.toFile())) {
            Set<String> names=new HashSet<>(); var en=zip.entries();int classes=0;
            while(en.hasMoreElements()) {
                var x=en.nextElement();String n=x.getName();require(names.add(n),"Duplicate entry: "+n);
                if(x.isDirectory())continue;
                require(!n.startsWith("/")&&!n.contains("../"),"Unsafe archive entry: "+n);
                var crc=new CRC32();
                try(var in=new DataInputStream(new CheckedInputStream(zip.getInputStream(x),crc))) {
                    if(n.endsWith(".class")) {
                        require(n.startsWith(BASE),"Unexpected bundled class: "+n);
                        require(in.readInt()==0xCAFEBABE,"Bad class header: "+n);
                        in.readUnsignedShort();require(in.readUnsignedShort()==65,"Expected Java 21 class: "+n);classes++;
                    }
                    in.transferTo(OutputStream.nullOutputStream());
                    require(crc.getValue()==x.getCrc(),"CRC mismatch: "+n);
                }
            }
            require(classes>0,"No plugin classes");
            require(zip.getManifest()!=null&&VERSION.equals(zip.getManifest().getMainAttributes().getValue("Implementation-Version")),"Manifest version mismatch");
            try(var walk=Files.walk(source)) {
                for(var p:walk.filter(f->f.toString().endsWith(".java")).toList()) {
                    String name=source.relativize(p).toString().replace(File.separatorChar,'/').replaceFirst("\\.java$",".class");
                    require(names.contains(name),"Missing compiled source class: "+name);
                }
            }
            require(names.contains(BASE+"paper/ArunikaDungeonPlugin.class"),"Missing entry point");
            Path resources=source.resolveSibling("resources");
            try(var walk=Files.walk(resources)) {
                for(var p:walk.filter(Files::isRegularFile).toList()) {
                    String name=resources.relativize(p).toString().replace(File.separatorChar,'/');
                    require(names.contains(name),"Missing resource: "+name);
                    try(var input=zip.getInputStream(zip.getJarEntry(name))) {
                        require(Arrays.equals(input.readAllBytes(),Files.readAllBytes(p)),"Resource differs from source: "+name);
                    }
                }
            }
            var y=zip.getJarEntry("plugin.yml");require(y!=null,"Missing plugin.yml");
            String plugin;try(var in=zip.getInputStream(y)){plugin=new String(in.readAllBytes(),java.nio.charset.StandardCharsets.UTF_8);}
            require(plugin.lines().anyMatch(l->l.equals("version: '"+VERSION+"'")),"Plugin version mismatch");
            require(plugin.lines().anyMatch(l->l.equals("api-version: '1.21.11'")),"Unexpected api-version");
            require(plugin.lines().anyMatch(l->l.equals("main: site.cadera.arunika.dungeon.paper.ArunikaDungeonPlugin")),"Wrong main class");
            System.out.println("PASS plugin archive: "+classes+" Java 21 classes; all source classes present; no bundled external APIs");
        }
    }
    static List<String> dependencyReport(String cp) throws Exception {
        var expected=Map.of("paper-api-1.21.11-","org/bukkit/Bukkit.class","snakeyaml-2.3.jar","org/yaml/snakeyaml/Yaml.class","sqlite-jdbc-3.46.1.3.jar","org/sqlite/JDBC.class","api-5.4.jar","net/luckperms/api/LuckPermsProvider.class");
        var found=new HashSet<String>();var rows=new ArrayList<String>();
        for(String item:cp.strip().split(Pattern.quote(File.pathSeparator))) {
            Path p=Path.of(item);require(Files.isRegularFile(p),"Non-file dependency: "+p);
            String n=p.getFileName().toString();
            try(var j=new JarFile(p.toFile())) {
                for(var e:expected.entrySet())if(e.getKey().endsWith("-") ? n.startsWith(e.getKey())&&n.endsWith(".jar") : n.equals(e.getKey())) {
                    require(j.getJarEntry(e.getValue())!=null,"Missing representative dependency class: "+n);
                    require(found.add(e.getKey()),"Duplicate direct dependency: "+n);
                }
            }
            rows.add(n+"  "+sha(p));
        }
        require(found.equals(expected.keySet()),"Missing expected dependencies; found "+found);
        Collections.sort(rows);return rows;
    }
    public static void main(String[] args)throws Exception {
        require(args.length==3,"Usage: VerifyArtifact <candidate.jar> <classpath.txt> <verified-dir>");
        Path jar=Path.of(args[0]),cp=Path.of(args[1]),out=Path.of(args[2]);
        checkJar(jar,Path.of("src/main/java"));
        var rows=dependencyReport(Files.readString(cp));
        require(!Files.exists(out),"Verified output already exists; clear it first");
        Files.createDirectories(out.getParent());Path pending=Files.createTempDirectory(out.getParent(),".pending-");
        try {
            String filename=jar.getFileName().toString();require(filename.equals("ArunikaDungeon-"+VERSION+".jar"),"Wrong candidate filename");
            Path copy=Files.copy(jar,pending.resolve(filename));require(sha(copy).equals(sha(jar)),"Copy checksum mismatch");
            Files.writeString(pending.resolve(filename+".sha256"),sha(copy)+"  "+filename+"\n");
            Files.write(pending.resolve("dependencies.sha256"),rows);
            Files.writeString(pending.resolve("BUILD-STATUS.txt"),"Dependency-backed build and standalone tests completed by build script.\nArchive structure and SHA-256 verified.\nPaper runtime NOT tested; staging only.\n");
            Files.move(pending,out);
            System.out.println("Verified staging artifact: "+out.resolve(filename));
        } finally {if(Files.exists(pending))try(var walk=Files.walk(pending)){for(var p:walk.sorted(Comparator.reverseOrder()).toList())Files.deleteIfExists(p);}}
    }
}
