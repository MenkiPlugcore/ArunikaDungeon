package site.cadera.arunika.dungeon.core;

import java.io.IOException;
import java.util.*;
import java.util.regex.*;

/**
 * Conservative mapping-only YAML defaults migration.
 * Preserves existing lines/comments and user values; refuses unsafe/ambiguous constructs
 * instead of serializing or overwriting user configurations.
 * Handles YAML mapping paths from owned templates. Lists/scalars remain opaque.
 */
public final class YamlDefaultsMerge {
    private static final Pattern KEY=Pattern.compile("^([A-Za-z0-9_-]+):(.*)$");
    private record Entry(String path, String parent, int line, int indent, boolean mapping, String value) {}
    private YamlDefaultsMerge() {}

    private static LinkedHashMap<String,Entry> parse(List<String> lines) throws IOException {
        LinkedHashMap<String,Entry> result=new LinkedHashMap<>();
        Deque<Entry> parents=new ArrayDeque<>();
        int skipListIndent=-1;
        for(int i=0;i<lines.size();i++) {
            String raw=lines.get(i); if(raw.isBlank() || raw.stripLeading().startsWith("#")) continue;
            if(raw.indexOf('\t')>=0) throw new IOException("Tabs refused in YAML line "+(i+1));
            int indent=0; while(indent<raw.length()&&raw.charAt(indent)==' ')indent++;
            String t=raw.substring(indent);
            if(t.equals("---")||t.equals("..."))throw new IOException("Multiple YAML documents are unsupported");
            if(skipListIndent>=0){ if(indent>skipListIndent)continue; skipListIndent=-1; }
            if(t.startsWith("- ")||t.equals("-")){skipListIndent=indent;continue;}
            Matcher m=KEY.matcher(t);
            if(!m.matches())throw new IOException("Unsupported YAML mapping syntax, line "+(i+1));
            while(!parents.isEmpty()&&parents.peek().indent()>=indent) parents.pop();
            String parent=parents.isEmpty()?"":parents.peek().path();
            if(!parents.isEmpty()&&!parents.peek().mapping())throw new IOException("Scalar parent at "+parent);
            String path=parent.isEmpty()?m.group(1):parent+"."+m.group(1);
            if(result.containsKey(path)) throw new IOException("Duplicate YAML key: "+path);
            String value=m.group(2).strip();
            boolean mapping=value.isEmpty() || value.startsWith("#");
            Entry e=new Entry(path,parent,i,indent,mapping,value);
            result.put(path,e);
            if(mapping)parents.push(e);
            else if(value.equals("[]")||value.startsWith("["))skipListIndent=indent;
        }
        return result;
    }
    static int schema(String content) throws IOException {
        var map=parse(Arrays.asList(content.split("\\R",-1)));
        Entry e=map.get("schema-version");
        if(e==null||e.mapping())throw new IOException("Managed YAML must contain scalar schema-version");
        try {int n=Integer.parseInt(e.value().split("\\s+#",2)[0].strip());if(n<1)return -1;return n;}
        catch(NumberFormatException ex){throw new IOException("Invalid schema-version",ex);}
    }
    public static String merge(String current,String template) throws IOException {
        int oldSchema=schema(current), targetSchema=schema(template);
        if(oldSchema>targetSchema)throw new IOException("Newer config schema "+oldSchema+" > supported "+targetSchema+"; downgrade refused");
        List<String> defaults=Arrays.asList(template.split("\\R",-1));
        var expected=parse(defaults);
        List<String> lines=new ArrayList<>(Arrays.asList(current.split("\\R",-1)));
        if(lines.getLast().isEmpty())lines.removeLast();
        // Parse existing data, reject key collisions and scalar/mapping mismatches.
        var actual=parse(lines);
        for(Entry desired:expected.values()) {
            Entry have=actual.get(desired.path());
            if(have!=null) {
                if(have.mapping()!=desired.mapping() && !desired.path().equals("schema-version"))
                    throw new IOException("Incompatible YAML shape: "+desired.path());
                continue;
            }
            Entry parent=actual.get(desired.parent());
            if(!desired.parent().isEmpty()&&(parent==null||!parent.mapping()))
                throw new IOException("Missing/incompatible YAML parent: "+desired.parent());
            // Place inside parent's mapping before next sibling, preserving custom keys.
            int at=lines.size(); int indent=0;
            if(parent!=null){
                indent=parent.indent()+2;
                int j=parent.line()+1;
                for(;j<lines.size();j++){
                    String s=lines.get(j);
                    if(s.isBlank()||s.stripLeading().startsWith("#"))continue;
                    int lead=0;while(lead<s.length()&&s.charAt(lead)==' ')lead++;
                    if(lead<=parent.indent())break;
                }
                at=j;
            }
            String fragment=" ".repeat(indent)+desired.path().substring(desired.path().lastIndexOf('.')+1)+":"+
                (desired.mapping()?"":(desired.value().isEmpty()?"":" "+desired.value()));
            lines.add(at,fragment);
            actual=parse(lines);
        }
        if(oldSchema<targetSchema) {
            Entry schemaLine=actual.get("schema-version");
            lines.set(schemaLine.line(),"schema-version: "+targetSchema);
        }
        return String.join("\n",lines)+"\n";
    }
}
