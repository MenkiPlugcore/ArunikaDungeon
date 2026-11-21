package site.cadera.arunika.dungeon.core;
import java.time.*;
import java.util.*;
import java.nio.file.*;
import java.util.concurrent.*;
public final class CoreTest {
    static int tests;
    static void check(boolean b,String name){if(!b)throw new AssertionError(name);System.out.println("PASS "+name);tests++;}
    static void rejects(Runnable r,String name){try{r.run();}catch(IllegalArgumentException|IllegalStateException e){check(true,name);return;}throw new AssertionError(name);}
    static class MutableClock extends Clock {Instant t=Instant.EPOCH;public Instant instant(){return t;}public ZoneId getZone(){return ZoneOffset.UTC;}public Clock withZone(ZoneId z){return this;}void advance(long secs){t=t.plusSeconds(secs);}}
    static SessionRegistry registry(MutableClock c){return new SessionRegistry(Set.of("queen_bee","goblin"),5,3,Duration.ofMinutes(30),Duration.ofSeconds(60),c);}
    public static void main(String[] args)throws Exception{
        var c=new MutableClock();var r=registry(c);var p=UUID.randomUUID();var q=UUID.randomUUID();
        Set<UUID> raid=new HashSet<>();raid.add(p);for(int i=0;i<100;i++)raid.add(UUID.randomUUID());
        var s=r.reserve("queen_bee",SessionRegistry.Mode.SHARED,p,raid);check(s.participants().size()==101,"shared no participant cap");
        check(r.find(s.id()).orElseThrow().equals(s),"session lookup by ID");
        check(r.find(UUID.randomUUID()).isEmpty(),"unknown session ID absent");
        rejects(()->StagingPolicy.requireBattle(true,true),"legacy spawner conflict blocks battle");
        rejects(()->StagingPolicy.requireBattle(false,false),"missing Mythic blocks battle");
        StagingPolicy.requireBattle(false,true);check(true,"isolated staging dependency gate accepts");
        rejects(()->r.reserve("goblin",SessionRegistry.Mode.PRIVATE,p,Set.of(p)),"cross-mode participant lock");
        rejects(()->r.reserve("queen_bee",SessionRegistry.Mode.SHARED,q,Set.of(q)),"one shared session per floor");
        var pr=r.reserve("queen_bee",SessionRegistry.Mode.PRIVATE,q,Set.of(q));check(pr.mode()==SessionRegistry.Mode.PRIVATE,"same floor private and shared independent");
        r.activate(s.id());r.activate(pr.id());check(r.remaining("queen_bee").isZero(),"no cooldown before boss death");
        r.bossDefeated(s.id());check(r.remaining("queen_bee").equals(Duration.ofMinutes(30)),"cooldown starts on boss death");
        rejects(()->r.bossDefeated(s.id()),"duplicate boss transition rejected");
        check(r.remaining("goblin").isZero(),"independent floor cooldown");r.close(s.id());
        rejects(()->r.reserve("queen_bee",SessionRegistry.Mode.SHARED,p,Set.of(p)),"cooldown blocks next shared");
        r.bossDefeated(pr.id());r.close(pr.id());check(r.remaining("queen_bee").equals(Duration.ofMinutes(30)),"private boss does not alter shared cooldown");
        c.advance(1800);r.expire();check(r.remaining("queen_bee").isZero(),"cooldown expiry");
        var reservation=r.reserve("queen_bee",SessionRegistry.Mode.PRIVATE,p,Set.of(p));c.advance(60);r.expire();check(r.participant(p).isEmpty(),"reservation expiry releases participant");
        rejects(()->r.activate(reservation.id()),"expired reservation cannot activate");
        rejects(()->r.reserve("goblin",SessionRegistry.Mode.PRIVATE,p,raid),"private party limit");
        for(int i=0;i<3;i++){var u=UUID.randomUUID();r.reserve("goblin",SessionRegistry.Mode.PRIVATE,u,Set.of(u));}
        rejects(()->r.reserve("goblin",SessionRegistry.Mode.PRIVATE,p,Set.of(p)),"private session budget");r.clear();
        check(r.snapshot().isEmpty()&&r.participant(p).isEmpty(),"cleanup clears index");
        rejects(()->r.reserve("missing",SessionRegistry.Mode.SHARED,p,Set.of(p)),"unknown floor rejected");
        rejects(()->r.reserve("goblin",SessionRegistry.Mode.SHARED,p,Set.of(q)),"leader must belong to party");
        var pool=Executors.newFixedThreadPool(2);try{Callable<Boolean> attempt=()->{try{r.reserve("goblin",SessionRegistry.Mode.SHARED,p,Set.of(p));return true;}catch(IllegalStateException ex){return false;}};var a=pool.submit(attempt);var b=pool.submit(attempt);check(a.get()!=b.get(),"concurrent duplicate reservation blocked");}finally{pool.shutdownNow();}
        var prior=new SessionRegistry(Set.of("goblin"),5,3,Duration.ofMinutes(30),Duration.ofSeconds(60),c);prior.restoreCooldowns(Map.of("queen_bee",c.instant().plusSeconds(1800)));check(prior.cooldownSnapshot().containsKey("queen_bee"),"temporarily removed floor cooldown preserved");var restored=registry(c);restored.restoreCooldowns(prior.cooldownSnapshot());check(restored.remaining("queen_bee").equals(Duration.ofMinutes(30)),"re-added floor cannot bypass cooldown");
        check(DungeonCatalog.INITIAL.size()==5,"five proposed floor descriptors");
        if(args.length>0){var path=Path.of(args[0]);check(LegacyInventory.countFloors(path)==10,"legacy archive floor count");check(LegacyInventory.inspect(path).stream().anyMatch(x->x.equals("ArunikaDungeonExchange/exchange.db: present")),"legacy database detected");}
        System.out.println("TOTAL "+tests+" PASS");
    }
}
