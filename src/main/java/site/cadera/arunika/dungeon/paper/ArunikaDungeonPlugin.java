package site.cadera.arunika.dungeon.paper;

import org.bukkit.command.*;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import site.cadera.arunika.dungeon.battle.*;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import site.cadera.arunika.dungeon.core.*;
import site.cadera.arunika.dungeon.config.*;
import site.cadera.arunika.dungeon.shared.*;
import site.cadera.arunika.dungeon.privategame.*;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/** Public entry remains disabled; optional admin-only battle preview. */
public final class ArunikaDungeonPlugin extends JavaPlugin implements DungeonGatewayApi {
    private volatile SessionRegistry sessions;
    private BukkitTask tick, dbTask, battleTick;
    private BattleEngine battle; private PaperBattlePort battlePort;
    private long nextMaintenance;
    private PerformanceGuard guard=new PerformanceGuard();private long nextGuardSample;private double sampledTps=20,sampledMspt;
    private PrivateParties parties; private TicketLedger ticketLedger; private PaperTickets tickets;
    private record Payment(UUID owner,TicketLedger.Ticket ticket){}
    private final Map<UUID,Payment> payments=new HashMap<>();
    private final Set<UUID> exposedPayments=new HashSet<>();
    private boolean beforeReward(UUID session){Payment payment=payments.get(session);if(payment==null)return sessions.find(session).map(s->s.mode()==SessionRegistry.Mode.SHARED).orElse(false);if(exposedPayments.contains(session))return true;try{ticketLedger.markExposed(payment.ticket(),session);exposedPayments.add(session);return true;}catch(Exception e){ticketFault=true;getLogger().severe("Reward blocked: ticket exposure record failed for "+session+": "+e);return false;}}
    private boolean ticketFault;
    private SharedQueue shared; private CooldownStore cooldownStore;
    private Map<String,Instant> initialCooldowns=Map.of();
    private final AtomicReference<String> database=new AtomicReference<>("pending");
    private Path legacyRoot;
    private SmartConfiguration config;
    private volatile SmartConfiguration.Snapshot snapshot;
    private boolean configBusy;
    private PaperExchange exchange;
    private PaperGateway gateway;
    private PaperAdmin admin;
    @Override public void onEnable(){
        try {
            Path root=getDataFolder().toPath();
            config=new SmartConfiguration(root,new SnakeYamlCodec());
            for(String dir:List.of("data","logs"))Files.createDirectories(root.resolve(dir));
            legacyRoot=root.toAbsolutePath().getParent();
            cooldownStore=new CooldownStore(root.resolve("data/shared-cooldowns.txt"));initialCooldowns=cooldownStore.load();
            ticketLedger=new TicketLedger(root.resolve("data/tickets"));
            try{for(UUID serial:ticketLedger.recoverHeld())getLogger().info("Recovered interrupted ticket hold: "+serial);}catch(Exception e){ticketFault=true;getLogger().severe("Ticket ledger locked: "+e);}
            apply(config.load());
            admin=new PaperAdmin(this);
            if(!ticketFault)exchange=new PaperExchange(this,ticketLedger,snapshot.ticketMaterial(),snapshot.ticketName());
            dbTask=getServer().getScheduler().runTaskAsynchronously(this,()->{
                try{FoundationDatabase.initialize(root.resolve("data/dungeon.db"));database.set("ready");}
                catch(Exception|LinkageError e){database.set("failed: "+e.getClass().getSimpleName());getLogger().severe("Foundation DB failed: "+e.getMessage());}
            });
            getServer().getServicesManager().register(DungeonGatewayApi.class,this,this,ServicePriority.Normal);
            getLogger().info("1.0.0 staging; public entry disabled. Battle="+(battle!=null)+"; Gateway="+(gateway!=null));
        }catch(Exception|LinkageError e){getLogger().log(java.util.logging.Level.SEVERE,"Foundation disabled",e);getServer().getPluginManager().disablePlugin(this);}
    }
    private void apply(SmartConfiguration.Snapshot next){
        var registry=new SessionRegistry(next.floors().stream().map(DungeonCatalog.Floor::id).collect(Collectors.toUnmodifiableSet()),next.privatePlayers(),next.privateSlots(),Duration.ofMinutes(30),Duration.ofSeconds(next.reservationSeconds()),Clock.systemUTC());
        registry.restoreCooldowns(sessions!=null?sessions.cooldownSnapshot():initialCooldowns);
        registry.setCooldownSink(values->{try{cooldownStore.save(values);}catch(java.io.IOException|RuntimeException e){getLogger().severe("Shared cooldown write failed; stop staging and repair storage before restart: "+e);throw e;}});
        if(gateway!=null){gateway.close();gateway=null;}
        if(shared!=null)shared.clear();shared=null;
        if(parties!=null)parties.clear();parties=null;tickets=null;
        if(tick!=null)tick.cancel();
        if(battleTick!=null)battleTick.cancel();
        if(battlePort!=null)HandlerList.unregisterAll(battlePort);
        battle=null; battlePort=null;
        sessions=registry;snapshot=next;if(exchange!=null)exchange.ticketAppearance(next.ticketMaterial(),next.ticketName());guard=new PerformanceGuard();nextGuardSample=0;
        if(next.battlePreview())try{
            // Never run two dungeon spawn controllers on the RC staging server.
            StagingPolicy.requireBattle(getServer().getPluginManager().getPlugin("CdrDungeonSpawner")!=null,
                getServer().getPluginManager().isPluginEnabled("MythicMobs"));
            var mm=getServer().getPluginManager().getPlugin("MythicMobs");
            if(mm==null||!mm.isEnabled())throw new IllegalStateException("MythicMobs not enabled");
            var mythic=new MythicBridge(mm);
            var mobIds=new HashSet<String>();for(var plan:next.battles().values())mobIds.addAll(plan.mobIds());mythic.validate(mobIds);
            battlePort=new PaperBattlePort(this,mythic);battle=new BattleEngine(registry,battlePort,next.mobBudget(),next.spawnBatch());battlePort.attach(battle);battlePort.progression(next.rpg());battlePort.beforeReward(this::beforeReward);
            getServer().getPluginManager().registerEvents(battlePort,this);
            battlePort.protect(next.battles().values(),next.gateway().enabled()?next.gateway().hub():null);
            if(next.sharedPreview())shared=new SharedQueue(registry,next.sharedCountdowns(),new SharedPort());
            if(next.privatePreview()&&!ticketFault)try{tickets=new PaperTickets(this,ticketLedger,next.ticketMaterial(),next.ticketName());parties=new PrivateParties(registry,next.privateLimits(),new PrivatePort());battlePort.onFinish(this::privateFinished);}catch(RuntimeException e){getLogger().warning("Private preview disabled: "+e);}

            battlePort.loot(new PaperLoot(this,next.lootPlans(),next.lootPreview(),next.lootBudget(),new PaperTickets(this,ticketLedger,next.ticketMaterial(),next.ticketName()),ticketLedger,this::beforeReward));
            if(next.gateway().enabled())try{gateway=new PaperGateway(this,next.gateway());getServer().getPluginManager().registerEvents(gateway,this);}catch(Exception|LinkageError e){if(gateway!=null){gateway.close();gateway=null;}getLogger().severe("Gateway locked: "+e.getMessage());}
            battleTick=getServer().getScheduler().runTaskTimer(this,()->{long now=now();boolean healthy=performanceOk();battle.tick(now,healthy);if(healthy)battlePort.drainProgression();if(now>=nextMaintenance){battlePort.maintenance();if(shared!=null)shared.tick(now,healthy);if(parties!=null)parties.tick(now,healthy&&!ticketFault);if(gateway!=null)gateway.refresh();nextMaintenance=now+1000;}},next.battleTicks(),next.battleTicks());
        }catch(Exception|LinkageError e){if(gateway!=null){gateway.close();gateway=null;}if(battleTick!=null){battleTick.cancel();battleTick=null;}if(battlePort!=null)HandlerList.unregisterAll(battlePort);battlePort=null;battle=null;shared=null;parties=null;tickets=null;getLogger().warning("Battle preview unavailable: "+e.getMessage());}
        tick=getServer().getScheduler().runTaskTimer(this,registry::expire,next.intervalTicks(),next.intervalTicks());
        getLogger().info("Configuration: "+next.floors().size()+" floors; "+next.changedFiles()+" files changed; backup="+next.backup());
        if(!next.warning().isEmpty())getLogger().warning(next.warning());
    }
    private void edit(CommandSender sender,String[] args,boolean create){
        if(configBusy||(exchange!=null&&exchange.hasOperation())){sender.sendMessage("Configuration or Exchange operation already running.");return;}
        // Shadow-only: never discard an active session in future versions.
        if(!sessions.storageHealthy()||!sessions.snapshot().isEmpty()||(shared!=null&&!shared.empty())||(parties!=null&&!parties.empty())||!payments.isEmpty()||(battlePort!=null&&battlePort.hasPendingReturns())){sender.sendMessage("Active sessions, queue, pending returns or cooldown storage failure; reload refused.");return;}
        if(create&&args.length<3){sender.sendMessage("/adg create <id> <floor> [display name]");return;}
        if(!create&&args.length>2){sender.sendMessage("/adg reload [all|id]");return;}
        final int number;try{number=create?Integer.parseInt(args[2]):0;}catch(NumberFormatException e){sender.sendMessage("Invalid floor number.");return;}
        String id=args.length>1?args[1]:"all";String name=create&&args.length>3?String.join(" ",Arrays.copyOfRange(args,3,args.length)):id;
        configBusy=true;
        getServer().getScheduler().runTaskAsynchronously(this,()->{
            try{var next=create?config.create(id,number,name):id.equals("all")?config.load():config.reload(id);
                getServer().getScheduler().runTask(this,()->{try{apply(next);sender.sendMessage("Configuration applied: "+next.changedFiles()+" changed; backup "+next.backup()+". Public gameplay still disabled.");}finally{configBusy=false;}});
            }catch(Exception e){getServer().getScheduler().runTask(this,()->{configBusy=false;sender.sendMessage("Configuration rejected; previous in-memory state retained: "+e.getMessage());});}
        });
    }
    private boolean performanceOk(){long n=System.nanoTime();if(nextGuardSample==0||n-nextGuardSample>=0){sampledTps=getServer().getTPS()[0];sampledMspt=getServer().getAverageTickTime();guard.sample(sampledTps,sampledMspt,snapshot.minTps(),snapshot.maxMspt());nextGuardSample=n+1_000_000_000L;}return !configBusy&&guard.healthy();}
    private final class SharedPort implements SharedQueue.Port {
        private BattlePlan.Arena arena(String floor){var p=snapshot.battles().get(floor);return p.arenas().stream().filter(a->a.mode()==SessionRegistry.Mode.SHARED).findFirst().orElseThrow();}
        public boolean eligible(UUID id,String floor){Player p=Bukkit.getPlayer(id);return p!=null&&p.isOnline()&&!p.isDead()&&!p.isFlying()&&p.getGameMode()!=org.bukkit.GameMode.CREATIVE&&p.getGameMode()!=org.bukkit.GameMode.SPECTATOR&&p.hasPermission("arunika.dungeon.shared.test")&&!battlePort.hasPendingReturn(id)&&(!snapshot.gateway().enabled()||(gateway!=null&&gateway.atHub(p)));}
        public boolean ready(String floor){return battle!=null&&!configBusy&&snapshot.sharedCountdowns().containsKey(floor)&&!battle.overlapsActive(arena(floor).bounds());}
        public Set<UUID> living(SessionRegistry.Session s){Set<UUID> ids=new HashSet<>();for(UUID id:s.participants())if(battle.eligible(id,s.id()))ids.add(id);return ids;}
        public void notify(UUID id,String message){Player p=Bukkit.getPlayer(id);if(p!=null)p.sendMessage("[Dungeon Test] "+message);}
        public void start(SessionRegistry.Session s){
            var plan=snapshot.battles().get(s.floor());var a=arena(s.floor());battlePort.validate(plan,a);
            for(Player p:Bukkit.getOnlinePlayers())if(a.bounds().contains(PaperBattlePort.point(p.getLocation()))&&!s.participants().contains(p.getUniqueId()))throw new IllegalStateException("Unregistered player in arena");
            var moved=new ArrayList<Player>();var target=PaperBattlePort.location(a.playerSpawn());
            try{for(UUID id:s.participants()){Player p=Bukkit.getPlayer(id);if(!eligible(id,s.floor())||p==null)throw new IllegalStateException("Participant unavailable");moved.add(p);if(!battlePort.authorizedTeleport(p,target))throw new IllegalStateException("Teleport rejected");}battle.start(s,plan,a,now());}
            catch(RuntimeException|LinkageError e){for(Player p:moved)battlePort.returnStagingPlayer(p,a.exit());throw e;}
        }
    }
    private void sharedCommand(CommandSender sender,String[] args){
        if(shared==null||configBusy){sender.sendMessage("Shared staging disabled. Enable battle-preview and shared-preview in config.yml.");return;}
        String action=args.length>1?args[1].toLowerCase(Locale.ROOT):"status";
        try{
            if(action.equals("status")){for(var v:shared.views(now(),performanceOk())){sender.sendMessage(v.floor()+" | "+v.state()+" | "+v.seconds()+"s | queue="+v.queued().size()+" | alive="+v.living().size()+"/"+v.active().size());if(args.length>2&&args[2].equals(v.floor())){sender.sendMessage("Queued: "+names(v.queued()));sender.sendMessage("Active: "+names(v.active()));sender.sendMessage("Living: "+names(v.living()));}}return;}
            if(!(sender instanceof Player p)){sender.sendMessage("Player required");return;}
            if(action.equals("join")&&args.length==3){shared.join(args[2],p.getUniqueId());p.sendMessage("Queued for "+args[2]+". Free staging; loot "+(snapshot.lootPreview()?"enabled by configuration":"disabled")+".");return;}
            if(action.equals("leave")){if(shared.leave(p.getUniqueId()))p.sendMessage("Queue left");else p.sendMessage("Not queued; active sessions cannot be left through this command.");return;}
            sender.sendMessage("/adg shared join <floor> | leave | status [floor]");
        }catch(RuntimeException e){sender.sendMessage("Shared rejected: "+e.getMessage());}
    }
    private final class PrivatePort implements PrivateParties.Port {
        public boolean invitable(UUID id,String floor){Player p=Bukkit.getPlayer(id);return p!=null&&p.isOnline()&&!p.isDead()&&!p.isFlying()&&p.getGameMode()!=org.bukkit.GameMode.CREATIVE&&p.getGameMode()!=org.bukkit.GameMode.SPECTATOR&&p.hasPermission("arunika.dungeon.private.test")&&(!snapshot.gateway().enabled()||p.hasPermission("arunika.dungeon.gateway.test"))&&!battlePort.hasPendingReturn(id);}
        private Optional<BattlePlan.Arena> arena(String floor){var plan=snapshot.battles().get(floor);if(plan==null)return Optional.empty();var active=battle.activeArenas().values();return plan.arenas().stream().filter(a->a.mode()==SessionRegistry.Mode.PRIVATE).filter(a->active.stream().noneMatch(b->a.bounds().overlaps(b.bounds()))).filter(a->Bukkit.getOnlinePlayers().stream().noneMatch(p->a.bounds().contains(PaperBattlePort.point(p.getLocation())))).findFirst();}
        public boolean eligible(UUID id,String floor){Player p=Bukkit.getPlayer(id);return p!=null&&p.isOnline()&&!p.isDead()&&!p.isFlying()&&p.getGameMode()!=org.bukkit.GameMode.CREATIVE&&p.getGameMode()!=org.bukkit.GameMode.SPECTATOR&&p.hasPermission("arunika.dungeon.private.test")&&!battlePort.hasPendingReturn(id)&&(!snapshot.gateway().enabled()||(gateway!=null&&gateway.atHub(p)));}
        public boolean arenaReady(String floor){return !configBusy&&!ticketFault&&arena(floor).isPresent();}
        public boolean ticketReady(UUID leader,TicketLedger.Ticket t)throws Exception{return tickets.has(Bukkit.getPlayer(leader),t);}
        public void notify(UUID id,String msg){Player p=Bukkit.getPlayer(id);if(p!=null)p.sendMessage("[Private Test] "+msg);}
        public void start(SessionRegistry.Session s,TicketLedger.Ticket t)throws Exception{
            var plan=snapshot.battles().get(s.floor());var a=arena(s.floor()).orElseThrow();battlePort.validate(plan,a);
            Player leader=Bukkit.getPlayer(s.leader());if(!tickets.has(leader,t))throw new IllegalStateException("Ticket unavailable");
            var moved=new ArrayList<Player>();boolean held=false,started=false;
            try{
                for(UUID id:s.participants())if(!eligible(id,s.floor()))throw new IllegalStateException("Participant unavailable");
                ticketLedger.hold(t,s.id(),s.leader());held=true;
                var target=PaperBattlePort.location(a.playerSpawn());
                for(UUID id:s.participants()){Player p=Bukkit.getPlayer(id);if(p==null)throw new IllegalStateException("Participant unavailable");moved.add(p);if(!battlePort.authorizedTeleport(p,target))throw new IllegalStateException("Teleport rejected");}
                if(!tickets.read(leader.getInventory().getItemInMainHand()).filter(t::equals).isPresent())throw new IllegalStateException("Ticket moved");
                battle.start(s,plan,a,now());started=true;
                ticketLedger.commit(t,s.id());
                tickets.remove(leader,t);
                payments.put(s.id(),new Payment(s.leader(),t));
                getLogger().info("Private ticket spent: "+t.serial()+" payer="+s.leader()+" session="+s.id());

            }catch(Exception|LinkageError e){
                if(started)battle.abort(s.id(),now(),"Private startup failed");else for(Player p:moved)battlePort.returnStagingPlayer(p,a.exit());
                if(held)try{refund(s.id(),new Payment(s.leader(),t));}catch(Exception refundFailure){ticketFault=true;getLogger().severe("Ticket refund requires repair: "+t.serial()+" session="+s.id()+" "+refundFailure);}
                throw e;
            }
            try{notify(s.leader(),"Ticket consumed; private session "+s.id());}catch(RuntimeException ignored){}
        }
    }
    private void refund(UUID session,Payment p)throws Exception{
        var next=ticketLedger.refund(p.ticket(),session);Player player=Bukkit.getPlayer(p.owner());
        getLogger().info("Ticket refunded: "+next.serial()+" owner="+p.owner()+" session="+session);
        if(player!=null){player.sendMessage("Ticket refund claim: /adg ticket claim "+next.serial());try{tickets.deliver(player,next);}catch(Exception e){getLogger().warning("Refund delivery pending: "+next.serial()+" "+e);}}
    }
    private void privateFinished(BattleEngine.View v){Payment p=payments.remove(v.id());boolean exposed=exposedPayments.remove(v.id());if(p==null)return;
        try {
            if(!exposed&&v.phase()==BattleEngine.Phase.FAILED&&(v.reason().startsWith("Battle error:")||v.reason().equals("Tracked mob missing")||v.reason().equals("Shutdown")||v.reason().equals("Stopped by admin")))refund(v.id(),p);
            else ticketLedger.complete(p.ticket(),v.id());
        }catch(Exception e){ticketFault=true;getLogger().severe("Private ticket settlement failed. Serial="+p.ticket().serial()+" session="+v.id()+" "+e);}
    }
    private void privateCommand(CommandSender sender,String[] args){
        if(!(sender instanceof Player p)||parties==null||configBusy||ticketFault){sender.sendMessage("Private staging unavailable");return;}
        UUID u=p.getUniqueId();String action=args.length>1?args[1].toLowerCase(Locale.ROOT):"status";long n=now();
        try{switch(action){
            case "create" -> {if(args.length!=3)throw new IllegalArgumentException("create <floor>");parties.create(args[2],u,n);}
            case "invite" -> {Player target=target(args);parties.invite(u,target.getUniqueId(),n);target.sendMessage(p.getName()+" invited you. "+(snapshot.gateway().enabled()?"Use Dungeon Hub > Accept invitation":"/adg private accept or decline")+" (60s)");}
            case "accept" -> parties.accept(u,n);
            case "decline" -> {parties.decline(u);p.sendMessage("Invitation declined");return;}
            case "ready" -> parties.ready(u,true,n);
            case "unready" -> parties.ready(u,false,n);
            case "kick" -> parties.kick(u,target(args).getUniqueId(),n);
            case "transfer" -> parties.transfer(u,target(args).getUniqueId(),n);
            case "cancel" -> parties.cancel(u,n);
            case "leave" -> {parties.leave(u,n);p.sendMessage("Party left");return;}
            case "start" -> parties.enqueue(u,tickets.hand(p),n);
            case "status" -> {}
            default -> {p.sendMessage("/adg private create <floor> | invite <player> | accept | decline | ready | unready | kick | transfer | cancel | leave | start | status");return;}
        }var v=parties.status(u);p.sendMessage(v.floor()+" | leader="+names(List.of(v.leader()))+" | members="+names(v.members())+" | ready="+names(v.ready())+" | queued="+v.queued());}catch(Exception e){p.sendMessage("Private rejected: "+e.getMessage());}
    }
    private Player target(String[] args){if(args.length!=3)throw new IllegalArgumentException("Player name required");Player p=Bukkit.getPlayerExact(args[2]);if(p==null)throw new IllegalArgumentException("Player offline");return p;}
    private void ticketCommand(CommandSender sender,String[] args){if(tickets==null||ticketFault||configBusy){sender.sendMessage("Ticket staging unavailable");return;}
        try{if(args.length==3&&args[1].equalsIgnoreCase("claim")&&sender instanceof Player p){var t=ticketLedger.claim(UUID.fromString(args[2]),p.getUniqueId());tickets.deliver(p,t);return;}
            if(!sender.hasPermission("arunika.dungeon.admin"))throw new IllegalStateException("Admin only");
            if(args.length==3&&args[1].equalsIgnoreCase("give")){Player p=target(args);var t=ticketLedger.issue(p.getUniqueId());sender.sendMessage("Issued serial: "+t.serial());p.sendMessage("Claim ID: "+t.serial());tickets.deliver(p,t);return;}
            if(args.length==3&&args[1].equalsIgnoreCase("inspect")){sender.sendMessage(ticketLedger.read(UUID.fromString(args[2])).toString());return;}
            if(args.length==4&&args[1].equalsIgnoreCase("recover")){UUID serial=UUID.fromString(args[2]),session=UUID.fromString(args[3]);if(sessions.find(session).isPresent()||payments.containsKey(session))throw new IllegalStateException("Active session");var record=ticketLedger.read(serial);if(!session.equals(record.session())||record.state()!=TicketLedger.State.SPENT)throw new IllegalStateException("Not an unresolved committed session");refund(session,new Payment(record.payer(),record.ticket()));sender.sendMessage("Recovery refund recorded. Verify crash circumstances before using this staff command.");return;}
            sender.sendMessage("/adg ticket give <player> | claim <serial> | inspect <serial> | recover <serial> <session>");
        }catch(Exception e){sender.sendMessage("Ticket rejected: "+e.getMessage());}
    }
    boolean gatewayUsable(){return !configBusy&&snapshot!=null&&snapshot.gateway().enabled()&&battle!=null&&sessions.storageHealthy();}
    boolean modeUsable(boolean priv){return gatewayUsable()&&(priv?parties!=null&&!ticketFault:shared!=null);}
    boolean gatewayFloor(String floor,boolean priv){return modeUsable(priv)&&(priv?snapshot.privateLimits():snapshot.sharedCountdowns()).containsKey(floor);}
    List<SessionRegistry.Session> gatewaySessions(){return sessions==null?List.of():sessions.snapshot();}
    boolean gatewayStoneInArena(String key,org.bukkit.Location l){if(l==null||snapshot==null)return false;return snapshot.battles().values().stream().anyMatch(b->b.arenas().stream().anyMatch(a->key.equals(b.floor()+"/"+a.id())&&a.bounds().contains(PaperBattlePort.point(l))));}
    boolean gatewayTicket(Player p){try{return tickets!=null&&tickets.has(p,tickets.hand(p));}catch(Exception e){return false;}}
    List<BattleEngine.View> gatewayBattles(){return battle==null?List.of():battle.views(now());}
    boolean gatewayReturnEligible(Player p,String target,org.bukkit.Location stone){if(target==null||battlePort==null||configBusy)return false;String[] a=target.split("/",2);return a.length==2&&battlePort.returnEligible(p,a[0],a[1],stone);}
    boolean gatewayReturn(Player p,String floor,String arena,org.bukkit.Location stone){return battlePort!=null&&!configBusy&&stone!=null&&battlePort.returnFromArena(p,floor,arena,stone);}
    void gatewayAction(Player p,boolean priv,String... args){if(!modeUsable(priv)||gateway==null||!gateway.atHub(p)||!p.hasPermission(priv?"arunika.dungeon.private.test":"arunika.dungeon.shared.test")){p.sendMessage("Dungeon gateway unavailable or permission denied.");return;}String[] full=new String[args.length+1];full[0]=priv?"private":"shared";System.arraycopy(args,0,full,1,args.length);if(priv)privateCommand(p,full);else sharedCommand(p,full);}
    private String names(Collection<UUID> ids){return ids.stream().map(id->{Player p=Bukkit.getPlayer(id);return p==null?id.toString():p.getName();}).collect(Collectors.joining(", "));}
    private static long now(){return System.nanoTime()/1_000_000;}
    private void preview(CommandSender sender,String[] args){
        if(battle==null||configBusy){sender.sendMessage("Preview disabled or configuration busy. See battle-preview in config.yml.");return;}
        try{
            String action=args.length>1?args[1]:"status";
            if(action.equals("status")){sender.sendMessage("Active tracked entities: "+battle.activeEntities());for(var v:battle.views(now()))sender.sendMessage(v.toString());return;}
            if(action.equals("stop")&&args.length==3){battle.abort(UUID.fromString(args[2]),now(),"Stopped by admin");return;}
            if(!(sender instanceof Player player)){sender.sendMessage("Player required.");return;}
            if(action.equals("exit")){if(!battlePort.retryExit(player))sender.sendMessage("No pending exit.");return;}
            if(action.equals("point")){var l=player.getLocation();sender.sendMessage("{world: '"+l.getWorld().getName()+"', x: "+l.getX()+", y: "+l.getY()+", z: "+l.getZ()+"}");return;}
            if(snapshot.gateway().enabled()&&action.equals("start"))throw new IllegalStateException("Direct preview entry disabled while gateway enabled");
            if(!action.equals("start")||args.length<4){sender.sendMessage("/adg preview start <floor-id> <arena-id> [online player names...] | status | stop <session UUID> | point | exit");return;}
            if(!performanceOk())throw new IllegalStateException("Server performance guard");
            var plan=Optional.ofNullable(snapshot.battles().get(args[2])).orElseThrow(()->new IllegalArgumentException("Battle not configured"));
            var arena=plan.arenas().stream().filter(a->a.id().equals(args[3])).findFirst().orElseThrow(()->new IllegalArgumentException("Unknown arena"));
            battlePort.validate(plan,arena);Set<UUID> participants=new HashSet<>();participants.add(player.getUniqueId());
            for(int i=4;i<args.length;i++){Player member=Bukkit.getPlayerExact(args[i]);if(member==null||!member.hasPermission("arunika.dungeon.admin"))throw new IllegalArgumentException("Preview requires online admin testers: "+args[i]);participants.add(member.getUniqueId());}
            for(Player online:Bukkit.getOnlinePlayers())if(arena.bounds().contains(PaperBattlePort.point(online.getLocation()))&&!participants.contains(online.getUniqueId()))throw new IllegalStateException("Unregistered player in arena: "+online.getName());
            var reservation=sessions.reserve(plan.floor(),arena.mode(),player.getUniqueId(),participants);
            var moved=new ArrayList<Player>();try{if(battle.overlapsActive(arena.bounds()))throw new IllegalStateException("Arena busy");for(UUID id:participants){Player tester=Bukkit.getPlayer(id);if(tester==null||tester.isDead()||tester.isFlying()||tester.getGameMode()==org.bukkit.GameMode.CREATIVE||tester.getGameMode()==org.bukkit.GameMode.SPECTATOR||battlePort.hasPendingReturn(id))throw new IllegalStateException("Use survival/adventure and land before starting preview");moved.add(tester);if(!battlePort.authorizedTeleport(tester,PaperBattlePort.location(arena.playerSpawn())))throw new IllegalStateException("Teleport rejected");}battle.start(reservation,plan,arena,now(),false);}catch(RuntimeException|LinkageError e){sessions.close(reservation.id());for(Player tester:moved)battlePort.returnStagingPlayer(tester,arena.exit());throw e;}
            sender.sendMessage("Preview started: "+reservation.id()+". No ticket or loot; staging only.");
        }catch(Exception|LinkageError e){sender.sendMessage("Preview rejected: "+e.getMessage());}
    }
    @Override public void onDisable(){
        var cleanup=new ShutdownSteps((name,error)->getLogger().log(java.util.logging.Level.SEVERE,"Shutdown failed: "+name,error));
        cleanup.run("battle scheduler",()->{if(battleTick!=null)battleTick.cancel();});
        cleanup.run("maintenance scheduler",()->{if(tick!=null)tick.cancel();});
        cleanup.run("admin GUI",()->{if(admin!=null)admin.close();});
        cleanup.run("gateway GUI",()->{if(gateway!=null)gateway.close();});
        cleanup.run("Exchange",()->{if(exchange!=null)exchange.close();});
        cleanup.run("active battle cleanup",()->{if(battle!=null)battle.abortAll(now());});
        cleanup.run("shared queue",()->{if(shared!=null)shared.clear();});
        cleanup.run("private parties",()->{if(parties!=null)parties.clear();});
        cleanup.run("database task",()->{if(dbTask!=null)dbTask.cancel();});
        cleanup.run("service registration",()->getServer().getServicesManager().unregisterAll(this));
        cleanup.run("session registry",()->{if(sessions!=null)sessions.clear();});
    }
    @Override public int apiVersion(){return 1;}
    org.bukkit.inventory.ItemStack exchangeTokenTemplate(){return exchange==null?null:exchange.tokenTemplate();}
    boolean inPrivateParty(UUID player){return sessions!=null&&sessions.party(player).isPresent();}
    @Override public Optional<String> queuedFloor(UUID player){return sessions==null?Optional.empty():sessions.queuedFloor(player);}
    @Override public List<DungeonGatewayApi.SharedFloorStatus> sharedStatus(){if(!Bukkit.isPrimaryThread())throw new IllegalStateException("Shared status requires main thread");if(shared==null)return List.of();return shared.views(now(),performanceOk()).stream().map(v->new DungeonGatewayApi.SharedFloorStatus(v.floor(),v.state().name(),v.seconds(),v.queued(),v.active(),v.living())).toList();}
    @Override public List<DungeonGatewayApi.PrivatePartyStatus> privateStatus(){if(!Bukkit.isPrimaryThread())throw new IllegalStateException("Main thread required");if(parties==null)return List.of();return parties.views().stream().map(v->new DungeonGatewayApi.PrivatePartyStatus(v.id(),v.floor(),v.leader(),v.members(),v.ready(),v.queued())).toList();}
    @Override public boolean entryEnabled(){return false;}
    @Override public List<DungeonCatalog.Floor> floors(){return snapshot==null?DungeonCatalog.INITIAL:snapshot.floors();}
    @Override public Optional<SessionRegistry.Session> session(UUID player){return sessions==null?Optional.empty():sessions.participant(player);}
    private void lootCommand(CommandSender sender,String[] a){
        if(a.length<2){sender.sendMessage("/adg loot list <floor> <shared|private> | addhand <floor> <mode> <wave-1|boss> <key> <chance> <min> <max> | remove <floor> <mode> <stage> <key>");return;}
        try{if(a[1].equals("list")&&a.length==4){var p=snapshot.lootPlans().get(a[2]+":"+a[3]);if(p==null)throw new IllegalArgumentException("Unknown floor/mode");for(var w:p.waves().entrySet())sender.sendMessage("wave-"+w.getKey()+": "+w.getValue().trigger()+" / "+w.getValue().entries().size()+" entries");sender.sendMessage("boss: "+p.boss().entries().size()+" entries");return;}
            if(configBusy||(exchange!=null&&exchange.hasOperation())||!sessions.storageHealthy()||!sessions.snapshot().isEmpty()||(shared!=null&&!shared.empty())||(parties!=null&&!parties.empty())||!payments.isEmpty()||(battlePort!=null&&battlePort.hasPendingReturns()))throw new IllegalStateException("Active sessions, pending returns or configuration busy");
            Map<String,Object> v=null;if(a[1].equals("addhand")&&a.length==9&&sender instanceof Player player){var item=player.getInventory().getItemInMainHand().clone();if(item.getType().isAir()||PaperLoot.hasTicketTag(item))throw new IllegalArgumentException("Hold a non-ticket item");item.setAmount(1);v=Map.of("type","SERIALIZED","id",Base64.getEncoder().encodeToString(item.serializeAsBytes()),"chance",Double.parseDouble(a[6]),"min",Integer.parseInt(a[7]),"max",Integer.parseInt(a[8]));}else if(!a[1].equals("remove")||a.length!=6)throw new IllegalArgumentException("Invalid editor syntax");
            final Map<String,Object> value=v;configBusy=true;getServer().getScheduler().runTaskAsynchronously(this,()->{try{String backup=config.editDrop(a[2],a[3],a[4],a[5],value);getServer().getScheduler().runTask(this,()->{configBusy=false;sender.sendMessage("Saved. Backup: "+backup+". /adg reload all to apply.");});}catch(Exception e){getServer().getScheduler().runTask(this,()->{configBusy=false;sender.sendMessage("Loot edit rejected: "+e.getMessage());});}});
        }catch(RuntimeException e){sender.sendMessage("Loot editor: "+e.getMessage());}
    }
    boolean configurationBusy(){return configBusy;}
    SmartConfiguration.Snapshot adminSnapshot(){return snapshot;}
    List<site.cadera.arunika.dungeon.exchange.ExchangeCatalog.Reward> adminExchangeRewards(){return exchange==null?List.of():exchange.adminRewards();}
    void adminEdit(CommandSender sender,String floor,String action,String id,int number,int amount,BattlePlan.Arena arena){
        if(!sender.hasPermission("arunika.dungeon.admin"))return;
        if(configBusy||(exchange!=null&&exchange.hasOperation())||!sessions.storageHealthy()||!sessions.snapshot().isEmpty()||(shared!=null&&!shared.empty())||(parties!=null&&!parties.empty())||!payments.isEmpty()||(battlePort!=null&&battlePort.hasPendingReturns())){sender.sendMessage("Configuration busy or session/queue/pending returns active; edit refused.");return;}
        // Validate actual Mythic IDs when editing a mob or boss, without spawning monsters.
        if(id!=null&&(action.equals("boss")||action.equals("wave")||action.equals("append-wave"))){try{var mm=getServer().getPluginManager().getPlugin("MythicMobs");if(mm==null||!mm.isEnabled())throw new IllegalStateException("MythicMobs unavailable");new MythicBridge(mm).validate(Set.of(id));}catch(Exception|LinkageError e){sender.sendMessage("Mythic ID rejected: "+e.getMessage());return;}}
        configBusy=true;getServer().getScheduler().runTaskAsynchronously(this,()->{try{var next=config.editBattle(floor,action,id,number,amount,arena);getServer().getScheduler().runTask(this,()->{try{apply(next);sender.sendMessage("Configuration saved; backup: "+next.backup()+". Staging only.");}catch(RuntimeException e){sender.sendMessage("Saved but runtime apply failed: "+e.getMessage());getLogger().severe("Admin config apply failed: "+e);}finally{configBusy=false;}});}catch(Exception e){getServer().getScheduler().runTask(this,()->{configBusy=false;sender.sendMessage("Edit rejected: "+e.getMessage());});}});
    }
    private void setupCommand(CommandSender sender,String[] a){try{
        if(a.length<2)throw new IllegalArgumentException("/adg setup boss|wave|append-wave|count|arena|resume|enable|disable");
        switch(a[1]){
            case "resume"->{if(!(sender instanceof Player p)||admin==null)throw new IllegalArgumentException("Player required");admin.resume(p);}
            case "arena"->{if(!(sender instanceof Player p)||admin==null||a.length!=5)throw new IllegalArgumentException("/adg setup arena <floor> <id> <shared|private>");admin.begin(p,a[2],a[3],SessionRegistry.Mode.valueOf(a[4].toUpperCase(Locale.ROOT)));}
            case "boss"->{if(a.length!=4)throw new IllegalArgumentException("/adg setup boss <floor> <Mythic-ID>");adminEdit(sender,a[2],a[1],a[3],0,0,null);}
            case "wave","append-wave"->{if(a.length!=6)throw new IllegalArgumentException("/adg setup wave|append-wave <floor> <number> <Mythic-ID> <amount>");adminEdit(sender,a[2],a[1],a[4],Integer.parseInt(a[3]),Integer.parseInt(a[5]),null);}
            case "count"->{if(a.length!=4)throw new IllegalArgumentException("/adg setup count <floor> <1..30>");adminEdit(sender,a[2],a[1],null,Integer.parseInt(a[3]),0,null);}
            case "enable","disable"->{if(a.length!=3)throw new IllegalArgumentException("/adg setup enable|disable <floor>");adminEdit(sender,a[2],a[1],null,0,0,null);}
            default->throw new IllegalArgumentException("Unknown setup action");
        }
    }catch(RuntimeException e){sender.sendMessage("Setup: "+e.getMessage());}}
    @Override public boolean onCommand(CommandSender sender,Command command,String label,String[] args){
        if(command.getName().equalsIgnoreCase("dg")||command.getName().equalsIgnoreCase("opdg")){if(exchange!=null)exchange.command(sender,command.getName().equalsIgnoreCase("opdg"),args);else sender.sendMessage("Exchange not ready");return true;}
        if(args.length>0&&args[0].equalsIgnoreCase("exchange")){if(exchange!=null)exchange.command(sender,args.length>1&&args[1].equalsIgnoreCase("admin"),Arrays.copyOfRange(args,args.length>1&&args[1].equalsIgnoreCase("admin")?2:1,args.length));else sender.sendMessage("Exchange not ready");return true;}
        if(args.length>0&&args[0].equalsIgnoreCase("gateway")){if(args.length==2&&args[1].equalsIgnoreCase("check")){if(sender.hasPermission("arunika.dungeon.admin"))PaperGateway.check(this,sender);else sender.sendMessage("No permission.");return true;}if(sender instanceof Player p){if(args.length==2&&args[1].equalsIgnoreCase("open")&&gateway!=null)gateway.openTarget(p);else if(p.hasPermission("arunika.dungeon.admin"))PaperGateway.inspect(this,p);else sender.sendMessage("Use the physical Waystone.");}else sender.sendMessage("Player required");return true;}
        if(snapshot!=null&&snapshot.gateway().enabled()&&args.length>0&&(args[0].equalsIgnoreCase("private")||args[0].equalsIgnoreCase("shared"))&&args.length>1&&!args[1].equalsIgnoreCase("status")){sender.sendMessage("Use the Dungeon Hub Waystone.");return true;}
        if(args.length>0&&args[0].equalsIgnoreCase("private")&&sender.hasPermission("arunika.dungeon.private.test")){privateCommand(sender,args);return true;}
        if(args.length>0&&args[0].equalsIgnoreCase("ticket")&&sender.hasPermission("arunika.dungeon.private.test")&&args.length>1&&args[1].equalsIgnoreCase("claim")){ticketCommand(sender,args);return true;}
        if(args.length>0&&args[0].equalsIgnoreCase("shared")&&sender.hasPermission("arunika.dungeon.shared.test")){sharedCommand(sender,args);return true;}
        if(!sender.hasPermission("arunika.dungeon.admin")){sender.sendMessage("No permission.");return true;}
        String sub=args.length==0?"status":args[0].toLowerCase(Locale.ROOT);
        switch(sub){
            case "status" -> {
                sender.sendMessage("[ArunikaDungeon 1.0.0] Shared/Private staging / public gateway disabled");
                sender.sendMessage("Battle: "+(battle!=null)+" | Shared: "+(shared!=null)+" | Private: "+(parties!=null&&!ticketFault)+" | Gateway: "+(gateway!=null));
                sender.sendMessage("Exchange: "+(exchange==null?"not ready":exchange.status()));
                sender.sendMessage("RPG: "+(snapshot.rpg().active()?"configured (provider required)":"disabled")+" | provider: "+(Bukkit.getServicesManager().getRegistration(DungeonRpgApi.class)!=null));
                sender.sendMessage("Database: "+database.get()+" | sessions: "+(sessions==null?0:sessions.snapshot().size())+" | cooldown storage: "+(sessions!=null&&sessions.storageHealthy()?"healthy":"FAILED"));
                for(String p:List.of("MythicMobs","CdrWaystone","CdrWaystoneBedrock","CdrDungeonSpawner","ArunikaDungeonExchange","ArunikaNusaKarsa","LuckPerms","MMOItems","ItemsAdder","Geyser-Spigot","floodgate")){var installed=getServer().getPluginManager().getPlugin(p);sender.sendMessage(p+": "+(installed==null?"not detected":installed.getDescription().getVersion()+" / "+(installed.isEnabled()?"enabled":"disabled")));}
                for(var f:floors()) sender.sendMessage("Floor "+f.number()+" - "+f.name()+" | configured; see /adg shared status");
            }
            case "diagnostics" -> {boolean ok=performanceOk();sender.sendMessage("Performance guard: "+(ok?"READY":"PAUSED")+" | TPS="+sampledTps+" | MSPT="+sampledMspt);sender.sendMessage("Sessions="+sessions.snapshot().size()+" | tracked mobs="+(battle==null?0:battle.activeEntities())+" | ticket ledger="+(ticketFault?"LOCKED":"ready"));if(exchange!=null)sender.sendMessage(exchange.diagnostics());sender.sendMessage("Arenas configured="+snapshot.battles().values().stream().mapToInt(p->p.arenas().size()).sum()+" | active="+(battle==null?0:battle.activeArenas().size())+" | pending spawn="+(battle==null?0:battle.views(now()).stream().mapToInt(BattleEngine.View::pending).sum()));sender.sendMessage("Loot entities="+(battlePort==null?0:battlePort.lootCount())+" | shared queued="+(shared==null?0:shared.views(now(),ok).stream().mapToInt(v->v.queued().size()).sum())+" | private parties="+(parties==null?0:parties.views().size()));sender.sendMessage("Gateway="+(gateway==null?"unavailable":"active")+" | database="+database.get()+" | cooldown storage="+(sessions.storageHealthy()?"healthy":"FAILED"));sender.sendMessage("Pending transactions: /opdg tx list. Dependencies and configured floors: /adg status. Configuration warning: "+(snapshot.warning().isBlank()?"none recorded":snapshot.warning()));}
            case "admin" -> {if(sender instanceof Player p&&admin!=null)admin.home(p,0);else sender.sendMessage("Player required");}
            case "setup" -> setupCommand(sender,args);
            case "legacy" -> {for(String line:LegacyInventory.inspect(legacyRoot))sender.sendMessage(line);sender.sendMessage("Read-only. For an offline snapshot, see tools/import_legacy.py.");}
            case "shared" -> sharedCommand(sender,args);
            case "private" -> privateCommand(sender,args);
            case "ticket" -> ticketCommand(sender,args);
            case "loot" -> lootCommand(sender,args);
            case "preview" -> preview(sender,args);
            case "create" -> edit(sender,args,true);
            case "reload" -> edit(sender,args,false);
            case "help" -> sender.sendMessage("/adg status | /adg legacy | /adg create <id> <floor> [name] | /adg reload [all|id] | /adg preview | /adg shared | /adg private | /adg ticket | /adg loot | /adg gateway [check|open] | /adg diagnostics | /adg admin | /adg setup | /adg help");
            default -> sender.sendMessage("Unknown subcommand. /adg preview | /adg shared | /adg private | /adg ticket | /adg loot | /adg admin | /adg setup | /adg help");
        }
        return true;
    }
}
