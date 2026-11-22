#!/usr/bin/env python3
"""Offline, non-destructive legacy staging. Stop Paper first. Python 3 stdlib only."""
import argparse, hashlib, json, os, shutil, sqlite3, tempfile
from pathlib import Path
from contextlib import closing

LEGACY = ('CdrDungeonSpawner', 'ArunikaDungeonExchange')

def sha(path):
    h = hashlib.sha256()
    with path.open('rb') as f:
        for chunk in iter(lambda: f.read(1024 * 1024), b''): h.update(chunk)
    return h.hexdigest()

def inventory(root):
    result = {}
    for name in LEGACY:
        base = root / name
        if base.is_symlink() or not base.is_dir(): raise ValueError(f'Missing or unsafe input: {base}')
        for p in sorted(base.rglob('*')):
            if p.is_symlink(): raise ValueError(f'Symlink refused: {p}')
            if p.is_file(): result[p.relative_to(root).as_posix()] = sha(p)
            elif not p.is_dir(): raise ValueError(f'Unsupported file: {p}')
    for required in ('CdrDungeonSpawner/config.yml','ArunikaDungeonExchange/config.yml','ArunikaDungeonExchange/rewards.yml','ArunikaDungeonExchange/exchange.db'):
        if required not in result: raise ValueError(f'Missing required input {required}')
    return result

def snapshot(root, dest):
    root, dest = root.resolve(), dest.resolve()
    if dest.exists(): raise FileExistsError(f'No overwrite: {dest}')
    if any(dest.is_relative_to(root / x) for x in LEGACY) or root.is_relative_to(dest): raise ValueError('Unsafe destination')
    before = inventory(root)
    dest.parent.mkdir(parents=True, exist_ok=True)
    tmp = Path(tempfile.mkdtemp(prefix='.legacy-stage-', dir=dest.parent))
    try:
        raw = tmp / 'original'
        for name in LEGACY: shutil.copytree(root / name, raw / name)
        for key,val in before.items():
            if sha(raw/key)!=val: raise RuntimeError('Input changed during copy: '+key)
        # Work only on a disposable copy. SQLite may update WAL/SHM even for a reader.
        work = tmp/'sqlite-work'; work.mkdir()
        for suffix in ('','-wal','-shm'):
            p=raw/'ArunikaDungeonExchange'/('exchange.db'+suffix)
            if p.exists(): shutil.copy2(p,work/p.name)
        # Rebuild SHM from copied WAL; the raw SHM is preserved unchanged.
        (work/'exchange.db-shm').unlink(missing_ok=True)
        converted = tmp/'exchange'; converted.mkdir()
        shutil.copy2(raw/'ArunikaDungeonExchange/config.yml',converted/'config.yml')
        shutil.copy2(raw/'ArunikaDungeonExchange/rewards.yml',converted/'rewards.yml')
        with closing(sqlite3.connect(work/'exchange.db')) as src, closing(sqlite3.connect(converted/'exchange.db')) as out:
            if src.execute('pragma integrity_check').fetchall()!=[('ok',)]: raise RuntimeError('Legacy SQLite integrity failed')
            src.backup(out)
            if out.execute('pragma integrity_check').fetchall()!=[('ok',)]: raise RuntimeError('Snapshot SQLite integrity failed')
            stats={}
            for table in ('transactions','purchases','reward_stock'):
                a=src.execute(f'SELECT COUNT(*) FROM {table}').fetchone()[0]
                b=out.execute(f'SELECT COUNT(*) FROM {table}').fetchone()[0]
                if a!=b: raise RuntimeError(f'Row count mismatch: {table}')
                # Compare rows, including ID, status, count and timestamps (not exposed in report).
                if sorted(src.execute(f'SELECT * FROM {table}').fetchall(), key=repr)!=sorted(out.execute(f'SELECT * FROM {table}').fetchall(), key=repr): raise RuntimeError(f'Row mismatch: {table}')
                stats[table]=a
        shutil.rmtree(work)
        if inventory(root)!=before: raise RuntimeError('Input changed during snapshot. Stop server first.')
        staged={p.relative_to(tmp).as_posix():sha(p) for p in sorted(tmp.rglob('*')) if p.is_file()}
        manifest={'format':1,'mode':'offline staging only; not a live cutover','source_sha256':before,'staged_sha256':staged,'sqlite_rows':stats}
        (tmp/'manifest.json').write_text(json.dumps(manifest,indent=2,sort_keys=True)+'\n',encoding='utf-8')
        if dest.exists(): raise FileExistsError(f'No overwrite: {dest}')
        # Same filesystem publish; caller must not run two imports to the same destination.
        tmp.rename(dest)
        return manifest
    finally:
        if tmp.exists(): shutil.rmtree(tmp)

def main():
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('--plugins',required=True,type=Path);p.add_argument('--output',required=True,type=Path);p.add_argument('--server-stopped',action='store_true',required=True,help='Confirm the legacy Paper server is offline.')
    a=p.parse_args();m=snapshot(a.plugins,a.output);print(json.dumps({'result':'staged','sqlite_rows':m['sqlite_rows'],'files_preserved':len(m['source_sha256']),'output':str(a.output)},indent=2))
if __name__=='__main__':main()
