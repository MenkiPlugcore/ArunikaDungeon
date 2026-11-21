package site.cadera.arunika.dungeon.config;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.yaml.snakeyaml.*;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.representer.Representer;
/** Strict, safe YAML parsing; no arbitrary Java object construction. */
public final class SnakeYamlCodec implements SmartConfiguration.Codec {
    private Yaml yaml(){LoaderOptions l=new LoaderOptions();l.setAllowDuplicateKeys(false);l.setMaxAliasesForCollections(0);l.setNestingDepthLimit(40);l.setCodePointLimit(2_000_000);DumperOptions d=new DumperOptions();d.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);d.setIndent(2);return new Yaml(new SafeConstructor(l),new Representer(d),d,l);}
    public Map<String,Object> read(byte[] bytes)throws IOException{try{Object obj=yaml().load(new String(bytes,StandardCharsets.UTF_8));if(!(obj instanceof Map<?,?> map))throw new IOException("YAML root must be mapping");return normalize(map,0);}catch(RuntimeException e){throw new IOException("Invalid YAML",e);}}
    private Map<String,Object> normalize(Map<?,?> raw,int depth)throws IOException{if(depth>40)throw new IOException("YAML nesting limit");Map<String,Object> out=new LinkedHashMap<>();for(var e:raw.entrySet()){if(!(e.getKey() instanceof String key))throw new IOException("YAML keys must be strings");out.put(key,value(e.getValue(),depth+1));}return out;}
    private Object value(Object v,int depth)throws IOException{if(depth>40)throw new IOException("YAML nesting limit");if(v instanceof Map<?,?> m)return normalize(m,depth);if(v instanceof List<?> l){List<Object> r=new ArrayList<>();for(Object o:l)r.add(value(o,depth+1));return r;}if(v==null||v instanceof String||v instanceof Boolean||v instanceof Integer||v instanceof Long||v instanceof Double)return v;throw new IOException("Unsupported YAML value: "+v.getClass().getSimpleName());}
    public byte[] write(Map<String,Object> data)throws IOException{try{return yaml().dump(normalize(data,0)).getBytes(StandardCharsets.UTF_8);}catch(RuntimeException e){throw new IOException("YAML encoding failed",e);}}
}
