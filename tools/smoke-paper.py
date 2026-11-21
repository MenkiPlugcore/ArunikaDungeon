#!/usr/bin/env python3
"""Isolated local Paper foundation smoke test. No production world or plugin data copied."""
import argparse
import hashlib
import json
import pathlib
import queue
import re
import shutil
import subprocess
import sys
import threading
import time
import zipfile

VERSION = '1.0.0'
READY = re.compile(r'Done \([^)]+\)! For help, type')
STATUS = f'[ArunikaDungeon {VERSION}] Shared/Private staging / public gateway disabled'
LOCKED = 'Battle: false | Shared: false | Private: false | Gateway: false'
DB_READY = 'Database: ready | sessions: 0 | cooldown storage: healthy'
FRESH_LOCK = 'Fresh Exchange ready: hold currency and use /opdg token set | locked'
RELOAD = 'Configuration applied:'
FATAL = re.compile(r'Foundation disabled|Shutdown failed:|Foundation DB failed|Error occurred while (?:enabling|disabling) ArunikaDungeon|Could not load .*ArunikaDungeon|UnsupportedClassVersionError|NoClassDefFoundError|NoSuchMethodError|ClassNotFoundException|InvalidPluginException|InvalidDescriptionException|Exception in server tick loop|Encountered an unexpected exception|Could not pass event .*ArunikaDungeon', re.I)
ANSI = re.compile(r'\x1b\[[0-?]*[ -/]*[@-~]')

class SmokeError(RuntimeError): pass

def digest(path):
    with path.open('rb') as src:
        return hashlib.file_digest(src, 'sha256').hexdigest()

def verify_input(jar):
    if jar.name != f'ArunikaDungeon-{VERSION}.jar': raise SmokeError('Expected 1.0.0 JAR from target/verified')
    checksum = jar.with_name(jar.name + '.sha256')
    if not checksum.is_file(): raise SmokeError('Missing adjacent SHA-256 file')
    chunks = checksum.read_text(encoding='utf-8').split()
    if len(chunks)!=2 or chunks[1]!=jar.name or chunks[0].lower()!=digest(jar): raise SmokeError('JAR checksum mismatch')
    with zipfile.ZipFile(jar) as z:
        if z.testzip(): raise SmokeError('JAR CRC failure')
        if f"version: '{VERSION}'" not in z.read('plugin.yml').decode().splitlines(): raise SmokeError('JAR plugin version mismatch')
        if 'site/cadera/arunika/dungeon/paper/ArunikaDungeonPlugin.class' not in z.namelist(): raise SmokeError('Missing plugin entry class')
    return digest(jar)

class LogMonitor:
    def __init__(self):
        self.lines=[]
        self.fatal=[]
    def add(self, line):
        line=ANSI.sub('',line).strip()
        self.lines.append(line)
        if FATAL.search(line): self.fatal.append(line)
    def contains(self, value, start=0):
        return any(value in line for line in self.lines[start:])
    def check(self):
        if self.fatal: raise SmokeError('Runtime error: '+self.fatal[0])

def run_once(work, java, ram, timeout, run):
    monitor=LogMonitor(); messages=queue.Queue(maxsize=2048)
    log=work/f'boot-{run}.log'
    with log.open('w',encoding='utf-8') as out:
        p=subprocess.Popen([java,'-Xms512M',f'-Xmx{ram}M','-jar','paper.jar','nogui'],cwd=work,stdin=subprocess.PIPE,stdout=subprocess.PIPE,stderr=subprocess.STDOUT,text=True,encoding='utf-8',errors='replace',bufsize=1)
        def reader():
            for line in p.stdout:
                out.write(line); out.flush(); messages.put(line)
            messages.put(None)
        thread=threading.Thread(target=reader,daemon=True);thread.start()
        def pump(seconds):
            try: line=messages.get(timeout=seconds)
            except queue.Empty: return
            if line is not None: monitor.add(line)
        def wait_for(check, seconds, label):
            end=time.monotonic()+seconds
            while time.monotonic()<end:
                monitor.check()
                if check(): return
                if p.poll() is not None: raise SmokeError(f'Paper exited early ({p.returncode}) during {label}')
                pump(.2)
            raise SmokeError(f'Timeout: {label}; inspect {log.name}')
        def send(cmd):
            p.stdin.write(cmd+'\n');p.stdin.flush()
        try:
            wait_for(lambda:any(READY.search(x) for x in monitor.lines),timeout,'Paper startup')
            wait_for(lambda:monitor.contains('1.0.0 staging; public entry disabled. Battle=false; Gateway=false'),20,'plugin enable')
            end=time.monotonic()+60
            while True:
                start=len(monitor.lines);send('arunikadungeon:arunikadungeon status')
                wait_for(lambda:monitor.contains(STATUS,start) and monitor.contains(LOCKED,start) and monitor.contains('Database:',start),10,'status command')
                if monitor.contains(DB_READY,start): break
                if monitor.contains('Database: failed',start) or time.monotonic()>end: raise SmokeError('Foundation database not ready')
                time.sleep(1)
            end=time.monotonic()+60
            while True:
                start=len(monitor.lines);send('arunikadungeon:opdg status')
                wait_for(lambda:monitor.contains(FRESH_LOCK,start) or monitor.contains('Starting',start) or monitor.contains('Exchange unavailable',start) or monitor.contains('Catalog rejected',start),10,'fresh Exchange status')
                if monitor.contains(FRESH_LOCK,start): break
                if monitor.contains('Exchange unavailable',start) or monitor.contains('Catalog rejected',start) or time.monotonic()>end: raise SmokeError('Fresh Exchange not initialized safely')
                time.sleep(1)
            start=len(monitor.lines);send('arunikadungeon:arunikadungeon diagnostics')
            wait_for(lambda:monitor.contains('Performance guard:',start) and monitor.contains('ticket ledger=ready',start),15,'diagnostics')
            start=len(monitor.lines);send('arunikadungeon:arunikadungeon reload all')
            wait_for(lambda:monitor.contains(RELOAD,start),30,'config reload')
            start=len(monitor.lines);send('arunikadungeon:arunikadungeon status')
            wait_for(lambda:monitor.contains(STATUS,start) and monitor.contains(LOCKED,start) and monitor.contains(DB_READY,start),15,'status after reload')
            send('stop')
            end=time.monotonic()+90
            while p.poll() is None and time.monotonic()<end: pump(.2)
            if p.poll() is None: raise SmokeError('Shutdown timeout')
            end=time.monotonic()+10
            while thread.is_alive() and time.monotonic()<end: pump(.1)
            if thread.is_alive(): raise SmokeError('Log reader did not finish')
            while not messages.empty(): pump(0)
            monitor.check()
            if p.returncode!=0: raise SmokeError(f'Paper exited {p.returncode}')
            if not monitor.contains('Disabling ArunikaDungeon v'+VERSION): raise SmokeError('Plugin disable marker missing')
            return {'boot':run,'startup':'PASS','database':'PASS','fresh_exchange':'PASS','diagnostics':'PASS','reload':'PASS','shutdown':'PASS'}
        finally:
            if p.poll() is None:
                try: send('stop')
                except (OSError,ValueError): pass
                end=time.monotonic()+20
                while p.poll() is None and time.monotonic()<end: pump(.2)
                if p.poll() is None:
                    p.kill();p.wait(timeout=10)
            # Drain the bounded log queue so the reader can finish.
            end=time.monotonic()+5
            while thread.is_alive() and time.monotonic()<end: pump(.1)
            thread.join(timeout=1)

def main():
    a=argparse.ArgumentParser(description=__doc__)
    a.add_argument('--paper',type=pathlib.Path,required=True,help='Official Paper 1.21.11 server JAR')
    a.add_argument('--plugin',type=pathlib.Path,required=True,help='1.0.0 verified candidate JAR with adjacent checksum')
    a.add_argument('--output',type=pathlib.Path,required=True,help='New empty path (must not exist)')
    a.add_argument('--eula-file',type=pathlib.Path,help='Existing EULA acceptance from this owner’s authorized test environment')
    a.add_argument('--maven-cache',type=pathlib.Path,help='Verified real dependency cache to seed Paper library resolver offline')
    a.add_argument('--runtime-cache',type=pathlib.Path,help='Existing Paper binary libraries/cache only; no worlds or player data copied')
    a.add_argument('--accept-eula',action='store_true',help='You have read and accept Minecraft EULA')
    a.add_argument('--java',default='java');a.add_argument('--ram',type=int,default=2048);a.add_argument('--port',type=int,default=25578);a.add_argument('--timeout',type=int,default=300)
    args=a.parse_args()
    existing_eula=args.eula_file.read_text(encoding='utf-8') if args.eula_file else None
    if not args.accept_eula and (existing_eula is None or not re.search(r'^\s*eula\s*=\s*true\s*$',existing_eula,re.M)): a.error('Provide existing accepted --eula-file, or read https://aka.ms/MinecraftEULA and explicitly pass --accept-eula if accepted')
    if not 1024<=args.ram<=16384 or not 1024<=args.port<=65535 or not 30<=args.timeout<=1800: a.error('RAM, port or timeout outside supported bounds')
    paper=args.paper.resolve(strict=True); plugin=args.plugin.resolve(strict=True); work=args.output.resolve()
    if work.exists(): a.error('--output must not exist; production folders are never reused')
    if not paper.is_file() or not plugin.is_file(): a.error('Both inputs must be local files')
    hash_=verify_input(plugin)
    work.mkdir(parents=True,exist_ok=False);(work/'plugins').mkdir()
    shutil.copyfile(paper,work/'paper.jar');shutil.copyfile(plugin,work/'plugins'/plugin.name)
    if args.runtime_cache:
        for name in ('libraries','versions','cache'):
            source=args.runtime_cache/name
            if source.is_dir(): shutil.copytree(source,work/name)
    if args.maven_cache:
        for source in args.maven_cache.rglob('*'):
            if source.is_file() and source.suffix in ('.jar','.pom','.sha1'):
                dest=work/'libraries'/source.relative_to(args.maven_cache)
                dest.parent.mkdir(parents=True,exist_ok=True);shutil.copyfile(source,dest)
                if source.suffix in ('.jar','.pom'):
                    with (dest.parent/'_remote.repositories').open('a') as metadata: metadata.write(dest.name+'>=\n')
    flat_settings='{"biome":"minecraft:plains","layers":[{"block":"minecraft:bedrock","height":1},{"block":"minecraft:dirt","height":2},{"block":"minecraft:grass_block","height":1}]}'
    (work/'eula.txt').write_text(existing_eula if existing_eula is not None else 'eula=true\n');(work/'server.properties').write_text(f'server-ip=127.0.0.1\nserver-port={args.port}\nonline-mode=true\nwhite-list=true\nenforce-whitelist=true\nenable-rcon=false\nenable-query=false\nview-distance=3\nsimulation-distance=3\nlevel-type=minecraft:flat\ngenerator-settings={flat_settings}\nspawn-protection=0\n')
    result={'version':VERSION,'paper_sha256':digest(paper),'plugin_sha256':hash_,'scope':'isolated foundation only; providers, battle, crossplay and economy not tested','runs':[]}
    code=0
    try:
        marker=work/'plugins/ArunikaDungeon/exchange/live/migration-complete.txt'
        for run in (1,2):
            result['runs'].append(run_once(work,args.java,args.ram,args.timeout,run))
            if not marker.is_file(): raise SmokeError('Fresh Exchange migration marker missing')
            if run==1: initial=(marker.read_bytes(),marker.stat().st_mtime_ns)
            elif (marker.read_bytes(),marker.stat().st_mtime_ns)!=initial: raise SmokeError('Exchange unexpectedly recreated on restart')
        result['status']='PASS'
    except (SmokeError,OSError,subprocess.SubprocessError) as exc:
        result['status']='FAIL';result['error']=str(exc);code=1
    finally:
        (work/'smoke-result.json').write_text(json.dumps(result,indent=2)+'\n',encoding='utf-8')
    print(json.dumps(result,indent=2));return code

if __name__=='__main__': sys.exit(main())
