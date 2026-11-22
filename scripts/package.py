"""Validates newly built JARs and packs retained original mesh sources. No dependency stubs."""
from pathlib import Path
import zipfile,hashlib,shutil,json,struct
root=Path(__file__).resolve().parents[1];out=root/'dist';out.mkdir(exist_ok=True)
modules=[('arunika-dungeon','ArunikaDungeon','1.0.2-RC1'),('arunika-dungeon-waystone','ArunikaDungeonWaystone','1.0.0-RC1'),('arunika-dungeon-waystone-bedrock','ArunikaDungeonWaystoneBedrock','1.0.0-RC1')]
for module,name,version in modules:
 jar=root/module/'target'/f'{name}-{version}.jar';assert jar.is_file(),jar
 with zipfile.ZipFile(jar) as z:
  assert z.testzip() is None
  names=z.namelist();assert len(names)==len(set(names))
  metadata=z.read('plugin.yml').decode();assert f"version: '{version}'" in metadata
  main=next(line.split(': ',1)[1] for line in metadata.splitlines() if line.startswith('main: '))
  assert main.replace('.','/')+'.class' in names
  for n in names:
   if n.endswith('.class'):
    assert n.startswith(('site/cadera/arunika/dungeon/','dev/arunika/dungeonwaystone/')),n
    assert struct.unpack('>H',z.read(n)[6:8])[0]==65,n
  compiled=root/module/'target/classes'
  expected={p.relative_to(compiled).as_posix():p.read_bytes() for p in compiled.rglob('*.class')}
  assert set(expected)=={n for n in names if n.endswith('.class')}
  for n,data in expected.items():assert z.read(n)==data
  for p in (root/module/'src/main/java').rglob('*.java'):assert p.relative_to(root/module/'src/main/java').as_posix().replace('.java','.class') in names
  for p in (root/module/'src/main/resources').rglob('*'):
   if p.is_file():assert z.read(p.relative_to(root/module/'src/main/resources').as_posix())==p.read_bytes()
 shutil.copyfile(jar,out/jar.name)
source=root/'resource-packs/bedrock'
with zipfile.ZipFile(out/'ArunikaDungeonWaystone-Bedrock-Models.mcpack','w',zipfile.ZIP_DEFLATED) as z:
 for p in sorted(source.rglob('*')):
  if p.is_file():
   if p.suffix=='.json':json.loads(p.read_text())
   z.write(p,p.relative_to(source).as_posix())
lines=[]
for p in sorted(out.iterdir()):
 if p.is_file() and p.name!='SHA256SUMS.txt':lines.append(hashlib.sha256(p.read_bytes()).hexdigest()+'  '+p.name)
(out/'SHA256SUMS.txt').write_text('\n'.join(lines)+'\n')
print('PASS: three newly built Java 21 JARs; source resources match; pack JSON valid; SHA-256 written')
