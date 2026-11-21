#!/usr/bin/env python3
"""RC5 narrow stability patch, applied after RC3 YAML and RC4 Waystone patches."""
from pathlib import Path
import sys,re
root=Path(sys.argv[1])
base=root/"src/main/java/site/cadera/arunika/dungeon"
def edit(p,old,new):
    s=p.read_text()
    if s.count(old)!=1: raise SystemExit("Unexpected baseline: "+str(p)+" / "+old[:120])
    p.write_text(s.replace(old,new,1))
p=base/"exchange/ExchangeMigration.java"
edit(p,'  if(!Files.isDirectory(source,LinkOption.NOFOLLOW_LINKS))throw new IOException("Legacy Exchange folder missing");','  if(Files.notExists(source,LinkOption.NOFOLLOW_LINKS))return fresh(target);\n  if(!Files.isDirectory(source,LinkOption.NOFOLLOW_LINKS))throw new IOException("Unsafe legacy Exchange path");')
edit(p,' private static void delete(Path p)throws IOException', ''' private static boolean fresh(Path target)throws Exception{
  Files.createDirectories(target.getParent());Path temp=Files.createTempDirectory(target.getParent(),".exchange-fresh-");
  try{
   Files.writeString(temp.resolve("config.yml"),"token:\\n  template-base64: ''\\n  fresh-token-unset: true\\n");
   Files.writeString(temp.resolve("rewards.yml"),"format: 2\\nrewards: {}\\n");
   Class.forName("org.sqlite.JDBC");
   try(var c=DriverManager.getConnection("jdbc:sqlite:"+temp.resolve("exchange.db"));var s=c.createStatement()){
    s.execute("CREATE TABLE reward_stock(reward_id TEXT PRIMARY KEY,configured INTEGER NOT NULL,remaining INTEGER NOT NULL)");
    s.execute("CREATE TABLE purchases(player_id TEXT NOT NULL,reward_id TEXT NOT NULL,count INTEGER NOT NULL,PRIMARY KEY(player_id,reward_id))");
    s.execute("CREATE TABLE transactions(id TEXT PRIMARY KEY,player_id TEXT NOT NULL,player_name TEXT NOT NULL,reward_id TEXT NOT NULL,reward_type TEXT NOT NULL,cost INTEGER NOT NULL,status TEXT NOT NULL,detail TEXT NOT NULL DEFAULT '',created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,updated_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP)");
    s.execute("CREATE INDEX idx_transactions_status ON transactions(status)");
    s.execute("CREATE INDEX idx_transactions_player ON transactions(player_id,status)");
    try(var r=s.executeQuery("PRAGMA integrity_check")){if(!r.next()||!"ok".equals(r.getString(1))||r.next())throw new SQLException("Fresh database integrity failure");}
   }
   Files.writeString(temp.resolve("migration-complete.txt"),"RC5 fresh initialization; no legacy data existed\\n");
   if(!Files.notExists(target,LinkOption.NOFOLLOW_LINKS))throw new IOException("Exchange appeared during initialization; refused overwrite");
   Files.move(temp,target,StandardCopyOption.ATOMIC_MOVE);return true;
  }finally{if(Files.exists(temp))delete(temp);}
 }
 private static void delete(Path p)throws IOException''')
p=base/"exchange/ExchangeCatalog.java"
edit(p,'String token=blob(str(map(config.get("token")),"template-base64"));','var tokenConfig=map(config.get("token"));String rawToken=str(tokenConfig,"template-base64");String token=rawToken.isEmpty()&&Boolean.TRUE.equals(tokenConfig.get("fresh-token-unset"))?"":blob(rawToken);')
p=base/"paper/PaperExchange.java"
edit(p,'private void apply(Loaded l){ItemStack t=decode(l.catalog.token());','private void apply(Loaded l){if(l.catalog.token().isEmpty()){catalog=l.catalog;token=null;previews=Map.of();enabled=false;status="Fresh Exchange ready: hold currency and use /opdg token set";return;}ItemStack t=decode(l.catalog.token());')
edit(p,'t.put("template-base64",hand);c.put("token",t);','t.put("template-base64",hand);t.remove("fresh-token-unset");c.put("token",t);')
edit(p,'s.put("enabled",a[1].equals("on"));','if(a[1].equals("on")&&ExchangeCatalog.map(c.get("token")).get("template-base64").equals(""))throw new IllegalStateException("Set token first: /opdg token set");s.put("enabled",a[1].equals("on"));')
edit(p,'if(r.type()==Type.TICKET)icon=icon(Material.PAPER,"Universal Dungeon Ticket");','if(r.type()==Type.TICKET)icon=icon(Material.PAPER,"Universal Dungeon Ticket");')
edit(p,'if(!p.isOnline()||p.getOpenInventory().getTopInventory().getHolder()!=h||!access())return;','if(!eligible(p)||p.getOpenInventory().getTopInventory().getHolder()!=h||!access())return;')
edit(p,'if(r!=null&&allowed(r))confirm(p,h,r);','if(r!=null&&catalog.rewards().contains(r)&&allowed(r))confirm(p,h,r);')
edit(p,'String status(){return status+(enabled?" | enabled":" | locked");}','String status(){return status+(enabled?" | enabled":" | locked");}\n String diagnostics(){return status()+" | busy players="+busy.size()+" | worker queue="+worker.getQueue().size()+"/128 | editing="+editing;}')
edit(p,'h.inv.setItem(45,icon(Material.ARROW,"Previous page"));','for(int slot=45;slot<54;slot++)h.inv.setItem(slot,icon(Material.GRAY_STAINED_GLASS_PANE," "));h.inv.setItem(45,icon(Material.ARROW,"Previous page"));')
p=base/"exchange/ExchangeLedger.java"
edit(p,'if(reward.limit()>0&&count>=reward.limit())','if(count<0||count==Integer.MAX_VALUE)throw new IllegalStateException("Purchase count requires reconciliation");if(reward.limit()>0&&count>=reward.limit())')
# Track whether inventory debit is uncertain before touching inventory; do not auto-cancel uncertain reservations.
# Existing RESERVED/DEBITED design deliberately leaves uncertain transactions pending; do not replay them.
p=base/"core/PerformanceGuard.java"
p.write_text('''package site.cadera.arunika.dungeon.core;
/** Main-thread state; one sample per second from the caller. Fail closed on invalid metrics. */
public final class PerformanceGuard {
 private boolean healthy=true;private int good;
 public boolean sample(double tps,double mspt,double minTps,double maxMspt){
  if(!Double.isFinite(tps)||!Double.isFinite(mspt)||tps<minTps||mspt<0||mspt>maxMspt){healthy=false;good=0;}
  else if(!healthy){if(tps>=Math.min(20,minTps+0.25)&&mspt<=maxMspt*0.95){if(++good>=5){healthy=true;good=0;}}else good=0;}
  return healthy;
 }
 public boolean healthy(){return healthy;}
}
''')
p=base/"paper/ArunikaDungeonPlugin.java"
edit(p,'    private long nextMaintenance;','    private long nextMaintenance;\n    private PerformanceGuard guard=new PerformanceGuard();private long nextGuardSample;private double sampledTps=20,sampledMspt;')
edit(p,'    private boolean performanceOk(){return !configBusy&&getServer().getTPS()[0]>=snapshot.minTps()&&getServer().getAverageTickTime()<=snapshot.maxMspt();}','    private boolean performanceOk(){long n=System.nanoTime();if(nextGuardSample==0||n-nextGuardSample>=0){sampledTps=getServer().getTPS()[0];sampledMspt=getServer().getAverageTickTime();guard.sample(sampledTps,sampledMspt,snapshot.minTps(),snapshot.maxMspt());nextGuardSample=n+1_000_000_000L;}return !configBusy&&guard.healthy();}')
edit(p,'sessions=registry;snapshot=next;','sessions=registry;snapshot=next;guard=new PerformanceGuard();nextGuardSample=0;')
edit(p,'if(getServer().getTPS()[0]<snapshot.minTps()||getServer().getAverageTickTime()>snapshot.maxMspt())','if(!performanceOk())')
edit(p,'            case "admin" ->','            case "diagnostics" -> {boolean ok=performanceOk();sender.sendMessage("Performance guard: "+(ok?"READY":"PAUSED")+" | TPS="+sampledTps+" | MSPT="+sampledMspt);sender.sendMessage("Sessions="+sessions.snapshot().size()+" | tracked mobs="+(battle==null?0:battle.activeEntities())+" | ticket ledger="+(ticketFault?"LOCKED":"ready"));if(exchange!=null)sender.sendMessage(exchange.diagnostics());sender.sendMessage("Pending transactions: /opdg tx list");}\n            case "admin" ->')
# Clarify logging, not enable gameplay.
edit(p,'/adg gateway [check|open] | /adg admin','/adg gateway [check|open] | /adg diagnostics | /adg admin')
p=root/"src/test/java/site/cadera/arunika/dungeon/exchange/ExchangeTest.java"
edit(p,'Path legacy=Files.createDirectory(temp.resolve("legacy"));','''Path fresh=temp.resolve("fresh-live"),missing=temp.resolve("missing");check(ExchangeMigration.importOnce(missing,fresh),"fresh Exchange created");check(!ExchangeMigration.importOnce(missing,fresh),"fresh initialization idempotent");try(var freshDb=new ExchangeLedger(fresh.resolve("exchange.db"))){check(freshDb.pending().isEmpty(),"fresh pending empty");var ft=freshDb.reserve(p,"fresh",r);freshDb.cancel(ft);}check(!ExchangeMigration.importOnce(missing,fresh),"fresh DB preserved after transactions");Path partial=Files.createDirectory(temp.resolve("partial"));reject(()->ExchangeMigration.importOnce(partial,temp.resolve("not-created")),"partial legacy not silently reset");Path incomplete=Files.createDirectory(temp.resolve("incomplete"));reject(()->ExchangeMigration.importOnce(missing,incomplete),"incomplete live not overwritten");var codec=new site.cadera.arunika.dungeon.config.SnakeYamlCodec();var freshConfig=codec.read(Files.readAllBytes(fresh.resolve("config.yml")));check(ExchangeCatalog.map(freshConfig.get("token")).get("template-base64").equals(""),"no invented currency template");Path legacy=Files.createDirectory(temp.resolve("legacy"));''')
p=root/"src/test/java/site/cadera/arunika/dungeon/core/PerformanceGuardTest.java"
p.write_text('''package site.cadera.arunika.dungeon.core;
public final class PerformanceGuardTest {
 public static void main(String[] args){var g=new PerformanceGuard();check(g.sample(20,30,18,50));check(!g.sample(17,30,18,50));for(int i=0;i<4;i++)check(!g.sample(20,30,18,50));check(g.sample(20,30,18,50));check(!g.sample(20,51,18,50));check(!g.sample(18.1,30,18,50));check(!g.sample(Double.NaN,30,18,50));System.out.println("PerformanceGuard: 10 PASS");}
 static void check(boolean ok){if(!ok)throw new AssertionError("guard hysteresis");}
}
''')
# Fresh exchange test needs real SnakeYAML in JDBC branch. The official build uses real dependencies.
p=root/"tools/build-verified.sh"
edit(p,'java -cp "$cp" site.cadera.arunika.dungeon.core.ShutdownTest','java -cp "$cp" site.cadera.arunika.dungeon.core.ShutdownTest\njava -cp "$cp" site.cadera.arunika.dungeon.core.PerformanceGuardTest')
p=root/"tools/build-verified.ps1"
s=p.read_text();print("Windows build script retains existing regression tests; run PerformanceGuardTest separately if necessary.")
for q in [root/"pom.xml",root/"src/main/resources/plugin.yml",base/"paper/ArunikaDungeonPlugin.java",root/"tools/build-verified.sh",root/"tools/build-verified.ps1",root/"tools/VerifyArtifact.java",root/"tools/smoke-paper.py",root/".github/workflows/build.yml"]:
    s=q.read_text();q.write_text(re.sub(r"(?<![A-Za-z])RC4(?![0-9])","RC5",s))
(root/"docs/RC5-STABILITY.md").write_text("""# RC5 stability preview
Existing live data wins; existing but incomplete paths are rejected. Missing legacy and live creates an empty database with unset currency, not a fabricated token. Use /opdg token set while holding the genuine token, /opdg ticket 50 on, /opdg exchange on. Existing ticket and exchange ambiguity policies remain fail-closed. /adg diagnostics shows sample TPS/MSPT, tracked mobs and Exchange worker backlog. Guard samples at most once/sec, pauses new sessions/spawn on overload, requires five healthy samples to resume. Battle deadlines still run. No new gameplay, no verified client/runtime stress results. Stop and back up before installing; retain Waystone JARs. GUI changes limited to Exchange navigation/stale click checks. Crash recovery does not replay ambiguous debits or rewards.
""")
print("Applied RC5 stability patch")
