#!/usr/bin/env python3
"""Apply the narrow RC3 YAML serialization fix to the checksum-verified source."""
from pathlib import Path
import sys

root = Path(sys.argv[1])
base = "site/cadera/arunika/dungeon/config/"
codec = root / "src/main/java" / base / "SnakeYamlCodec.java"
test = root / "src/test/java" / base / "ConfigTest.java"

def replace_once(text, old, new):
    if text.count(old) != 1:
        raise SystemExit("Unexpected baseline; refusing to patch: " + old)
    return text.replace(old, new, 1)

# The safe reader deliberately rejects collection aliases. Make the writer
# emit a tree of independent maps/lists, not a graph of shared identities.
codec_text = replace_once(codec.read_text(encoding="utf-8"),
    "yaml().dump(data)", "yaml().dump(normalize(data,0))")
test_text = replace_once(test.read_text(encoding="utf-8"),
    '        Path root=Files.createTempDirectory("adg-config-test-");try{',
    '''        if(args.length>0){
            var reused=List.of(m("mythicmob-id","TestMob","amount",1));
            var graph=m("first",reused,"second",reused);
            var bytes=codec.write(graph);
            check(codec.read(bytes).equals(graph),"real YAML repeated collection round trip");
            rejects(()->codec.read("first: &shared [1]\\nsecond: *shared\\n".getBytes(java.nio.charset.StandardCharsets.UTF_8)),"real YAML collection aliases still rejected");
        }
        Path root=Files.createTempDirectory("adg-config-test-");try{''')
codec.write_text(codec_text, encoding="utf-8")
test.write_text(test_text, encoding="utf-8")
print("Applied RC3 alias-free YAML writer and regression tests")
