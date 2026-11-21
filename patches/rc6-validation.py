#!/usr/bin/env python3
"""RC6 validation changes applied to the verified RC5 baseline."""
from pathlib import Path
import re,sys
root=Path(sys.argv[1])
base=root/"src/main/java/site/cadera/arunika/dungeon"
def edit(p,old,new):
    s=p.read_text()
    if s.count(old)!=1: raise SystemExit("Unexpected baseline: "+str(p)+" / "+old[:100])
    p.write_text(s.replace(old,new,1))
for q in [root/"pom.xml",root/"src/main/resources/plugin.yml",base/"paper/ArunikaDungeonPlugin.java",root/"tools/build-verified.sh",root/"tools/build-verified.ps1",root/"tools/VerifyArtifact.java",root/"tools/smoke-paper.py",root/".github/workflows/build.yml"]:
    q.write_text(re.sub(r"(?<![A-Za-z])RC5(?![0-9])","RC6",q.read_text()))
p=root/"src/main/resources/plugin.yml"
edit(p,"|gateway|admin|","|gateway|diagnostics|admin|")
p=root/"src/test/java/site/cadera/arunika/dungeon/exchange/ExchangeStressTest.java"
p.write_text('''package site.cadera.arunika.dungeon.exchange;
import java.nio.file.*;
import java.sql.*;
import java.util.*;
import java.util.concurrent.*;
/** Exercises the production single-owner SQLite model, not simulated Minecraft clients. */
public final class ExchangeStressTest {
 static void check(boolean ok,String label){if(!ok)throw new AssertionError(label);System.out.println("PASS "+label);}
 public static void main(String[] args)throws Exception{
  Path dir=Files.createTempDirectory("adg-exchange-stress");
  var owner=Executors.newSingleThreadExecutor();
  var clients=Executors.newFixedThreadPool(8);
  try{
   Path live=dir.resolve("live");ExchangeMigration.importOnce(dir.resolve("absent"),live);
   Path file=live.resolve("exchange.db");
   var reward=new ExchangeCatalog.Reward("stress",UUID.randomUUID(),ExchangeCatalog.Type.ITEM,"",1,10,1,true,1,"Test",false,List.of());
   try(var db=new ExchangeLedger(file)){
    List<Future<Boolean>> results=new ArrayList<>();
    for(int i=0;i<50;i++){final UUID player=UUID.randomUUID();
     results.add(clients.submit(()->owner.submit(()->{
      ExchangeLedger.Tx tx;
      try{tx=db.reserve(player,"test",reward);}catch(IllegalStateException e){if(!e.getMessage().equals("Out of stock"))throw e;return false;}
      db.debit(tx);db.delivered(tx,"stress");return true;
     }).get(20,TimeUnit.SECONDS)));
    }
    int wins=0;for(var result:results)if(result.get(30,TimeUnit.SECONDS))wins++;
    check(wins==10,"50 requests, exactly 10 deliveries");
    check(db.pending().isEmpty(),"no pending transactions after successful delivery");
   }
   var pendingReward=new ExchangeCatalog.Reward("pending",UUID.randomUUID(),ExchangeCatalog.Type.ITEM,"",1,1,1,true,1,"Test",false,List.of());
   UUID player=UUID.randomUUID();ExchangeLedger.Tx pending;
   try(var db=new ExchangeLedger(file)){pending=db.reserve(player,"pending",pendingReward);}
   try(var db=new ExchangeLedger(file)){
    check(db.find(pending.id()).state()==ExchangeLedger.State.RESERVED,"reserved transaction survives reopen");
    boolean rejected=false;try{db.reserve(player,"retry",pendingReward);}catch(IllegalStateException e){rejected=true;}
    check(rejected,"duplicate retry blocked after reopen");
    db.cancel(pending);var tx=db.reserve(player,"retry",pendingReward);db.debit(tx);
    pending=tx;
   }
   try(var db=new ExchangeLedger(file)){
    check(db.find(pending.id()).state()==ExchangeLedger.State.DEBITED,"debited transaction survives reopen");
    boolean rejected=false;try{db.cancel(pending);}catch(IllegalStateException e){rejected=true;}
    check(rejected,"debited transaction cannot be refunded");
    db.delivered(pending,"manual verified");check(db.pending().isEmpty(),"manual resolution clears pending");
   }
   try(var c=DriverManager.getConnection("jdbc:sqlite:"+file);var s=c.createStatement();var rs=s.executeQuery("PRAGMA integrity_check")){check(rs.next()&&rs.getString(1).equals("ok"),"SQLite integrity after stress");}
  }finally{clients.shutdownNow();owner.shutdownNow();clients.awaitTermination(10,TimeUnit.SECONDS);owner.awaitTermination(10,TimeUnit.SECONDS);try(var walk=Files.walk(dir)){for(var f:walk.sorted(Comparator.reverseOrder()).toList())Files.deleteIfExists(f);}}
 }
}
''')
p=root/"tools/build-verified.sh"
edit(p,'java -cp "$cp" site.cadera.arunika.dungeon.core.PerformanceGuardTest','java -cp "$cp" site.cadera.arunika.dungeon.core.PerformanceGuardTest\njava -cp "$cp" site.cadera.arunika.dungeon.exchange.ExchangeStressTest\npython3 tools/test-smoke.py')
p=root/"tools/smoke-paper.py"
edit(p,'DB_READY = \'Database: ready | sessions: 0 | cooldown storage: healthy\'','DB_READY = \'Database: ready | sessions: 0 | cooldown storage: healthy\'\nFRESH_LOCK = \'Fresh Exchange ready: hold currency and use /opdg token set | locked\'')
edit(p,'            start=len(monitor.lines);send(\'arunikadungeon:arunikadungeon reload all\')','''            end=time.monotonic()+60
            while True:
                start=len(monitor.lines);send('arunikadungeon:opdg status')
                wait_for(lambda:monitor.contains(FRESH_LOCK,start) or monitor.contains('Starting',start) or monitor.contains('Exchange unavailable',start) or monitor.contains('Catalog rejected',start),10,'fresh Exchange status')
                if monitor.contains(FRESH_LOCK,start): break
                if monitor.contains('Exchange unavailable',start) or monitor.contains('Catalog rejected',start) or time.monotonic()>end: raise SmokeError('Fresh Exchange not initialized safely')
                time.sleep(1)
            start=len(monitor.lines);send('arunikadungeon:arunikadungeon diagnostics')
            wait_for(lambda:monitor.contains('Performance guard:',start) and monitor.contains('ticket ledger=ready',start),15,'diagnostics')
            start=len(monitor.lines);send('arunikadungeon:arunikadungeon reload all')''')
edit(p,"'database':'PASS','reload':'PASS'","'database':'PASS','fresh_exchange':'PASS','diagnostics':'PASS','reload':'PASS'")
edit(p,"        for run in (1,2): result['runs'].append(run_once(work,args.java,args.ram,args.timeout,run))",'''        marker=work/'plugins/ArunikaDungeon/exchange/live/migration-complete.txt'
        for run in (1,2):
            result['runs'].append(run_once(work,args.java,args.ram,args.timeout,run))
            if not marker.is_file(): raise SmokeError('Fresh Exchange migration marker missing')
            if run==1: initial=(marker.read_bytes(),marker.stat().st_mtime_ns)
            elif (marker.read_bytes(),marker.stat().st_mtime_ns)!=initial: raise SmokeError('Exchange unexpectedly recreated on restart')''')
(root/"docs/RC6-RELEASE-GATE.md").write_text("""# RC6 final validation
This is a staging candidate, not a production approval.
The official build includes a 50-request SQLite test using the production single-owner model (expected exactly 10 stock deliveries), pending transaction reopen, debit/no-refund and integrity checks. This is not a 50-client Minecraft test.
## Isolated Paper
Obtain official Paper 1.21.11 locally and read Minecraft EULA. If accepted, run from patched source:
  python3 tools/smoke-paper.py --paper /absolute/path/paper.jar --plugin target/verified/ArunikaDungeon-1.0.0-RC6.jar --output /absolute/path/new-smoke --accept-eula
The output must not exist. Loopback only; two boots, fresh Exchange locked without currency, diagnostics, reload, shutdown, marker persistence. No third-party providers are covered.
## Manual acceptance
Use backed-up staging with actual MythicMobs, Waystone, Geyser/Floodgate and Nusa Karsa. Test Java and Bedrock physical hub click and return, survival Waystone unchanged, 5-player private party, shared cooldown persistence, simultaneous sessions, ticket replay rejection, pending transaction review, physical loot pickup isolation, disconnect and restart recovery. Test with real clients ramping 5/15/30/50, capture spark profiles/TPS/MSPT/heap before and during; do not treat SQLite request count as player count. Verify no stuck sessions or orphan entities. Roll back by stopping server and restoring matching previous JAR and backed-up data together. Do not switch on public gateway until acceptance is complete.
""")
print("RC6 validation patch applied")
