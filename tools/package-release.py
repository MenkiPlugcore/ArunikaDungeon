#!/usr/bin/env python3
"""Package verified staging outputs. Does not declare production readiness."""
from pathlib import Path
import hashlib, shutil, subprocess, zipfile
ROOT=Path(__file__).resolve().parent.parent
DIST=ROOT/'dist'
VERSION='1.0.0'
def digest(path):return hashlib.sha256(path.read_bytes()).hexdigest()
def main():
    jar=ROOT/f'target/verified/ArunikaDungeon-{VERSION}.jar'
    checksum=jar.with_name(jar.name+'.sha256')
    if not jar.is_file() or not checksum.is_file():raise SystemExit('Run verified build first')
    if checksum.read_text().split()!=[digest(jar),jar.name]:raise SystemExit('Verified JAR hash mismatch')
    DIST.mkdir(exist_ok=True)
    shutil.copy2(jar,DIST/jar.name)
    source=DIST/f'ArunikaDungeon-{VERSION}-source.zip'
    # HEAD is the exact canonical commit; never include local DBs, worlds or untracked state.
    subprocess.run(['git','archive','--format=zip',f'--prefix=ArunikaDungeon-{VERSION}/','-o',str(source),'HEAD'],cwd=ROOT,check=True)
    docs=DIST/f'ArunikaDungeon-{VERSION}-docs.zip'
    with zipfile.ZipFile(docs,'w',zipfile.ZIP_DEFLATED) as out:
        for name in ['README.md','CHANGELOG.md','SECURITY.md']:
            out.write(ROOT/name,name)
        for path in sorted((ROOT/'docs').glob('*')):
            if path.is_file():out.write(path,'docs/'+path.name)
        out.write(ROOT/'build-logs/verified-build.log','BUILD-LOG.txt')
        out.write(ROOT/'target/verified/dependencies.sha256','dependencies.sha256')
    shutil.copy2(ROOT/'build-logs/verified-build.log',DIST/'BUILD-LOG.txt')
    for name in ['CHANGELOG.md','FULL-AUDIT-REPORT.md','SECURITY-REPORT.md','PERFORMANCE-REPORT.md','REGRESSION-TEST-REPORT.md','INSTALLATION-GUIDE.md','MIGRATION-GUIDE.md','ROLLBACK-GUIDE.md','ADMIN-COMMANDS.md','WAYSTONE-SETUP-GUIDE.md','DUNGEON-SETUP-GUIDE.md','EXCHANGE-SETUP-GUIDE.md','DEPENDENCY-COMPATIBILITY.md','RUNTIME-ACCEPTANCE-CHECKLIST.md','KNOWN-ISSUES.md','RELEASE-NOTES.md','RUNTIME-TEST-REPORT.md']:
        shutil.copy2(ROOT/'docs'/name,DIST/name)
    assets=sorted(p for p in DIST.iterdir() if p.is_file() and p.name!='SHA256SUMS.txt')
    (DIST/'SHA256SUMS.txt').write_text(''.join(digest(p)+'  '+p.name+'\n' for p in assets))
    print(f'Packaged {len(assets)} assets; draft/staging only. Source commit: '+subprocess.check_output(['git','rev-parse','HEAD'],cwd=ROOT,text=True).strip())
if __name__=='__main__':main()
