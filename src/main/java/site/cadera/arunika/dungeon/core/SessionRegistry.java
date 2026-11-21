package site.cadera.arunika.dungeon.core;

import java.time.*;
import java.util.*;

/** In-memory foundation: no world, ticket or loot side effects. All mutations are serialized. */
public final class SessionRegistry {
    public enum Mode { SHARED, PRIVATE }
    public enum State { RESERVED, ACTIVE, LOOT }
    public record Session(UUID id, String floor, Mode mode, UUID leader, Set<UUID> participants, State state, Instant expiresAt) {
        public Session { participants = Set.copyOf(participants); }
    }
    private final Set<String> floors;
    private final int privatePlayers, privateSlots;
    private final Duration cooldown, reservationTimeout;
    private final Clock clock;
    private final Map<UUID,Session> sessions = new HashMap<>();
    private final Map<UUID,UUID> byPlayer = new HashMap<>();
    private final Map<String,Instant> sharedCooldown = new HashMap<>();
    private final Map<UUID,String> queued = new HashMap<>();
    private final Map<UUID,UUID> parties = new HashMap<>();
    @FunctionalInterface public interface CooldownSink { void save(Map<String,Instant> values) throws java.io.IOException; }
    private CooldownSink sink = values -> {};
    private boolean storageFailed;
    public synchronized void setCooldownSink(CooldownSink sink){this.sink=Objects.requireNonNull(sink);}
    public synchronized boolean storageHealthy(){return !storageFailed;}
    public synchronized void enqueue(String floor,UUID player){Objects.requireNonNull(player);if(!floors.contains(floor)||storageFailed||byPlayer.containsKey(player)||parties.containsKey(player)||queued.putIfAbsent(player,floor)!=null)throw new IllegalStateException("Already registered or floor unavailable");}
    public synchronized boolean dequeue(String floor,UUID player){return queued.remove(player,floor);}
    public synchronized Optional<String> queuedFloor(UUID player){return Optional.ofNullable(queued.get(player));}
    public synchronized void joinParty(UUID party,UUID player){Objects.requireNonNull(party);Objects.requireNonNull(player);if(byPlayer.containsKey(player)||queued.containsKey(player)||parties.putIfAbsent(player,party)!=null)throw new IllegalStateException("Player occupied");}
    public synchronized void leaveParty(UUID party,UUID player){parties.remove(player,party);}
    public synchronized Optional<UUID> party(UUID player){return Optional.ofNullable(parties.get(player));}
    public synchronized Session reserveParty(String floor,UUID party,UUID leader,Set<UUID> members){
        if(members.stream().anyMatch(p->!party.equals(parties.get(p))))throw new IllegalStateException("Missing party membership");
        members.forEach(parties::remove);
        try{return reserve(floor,Mode.PRIVATE,leader,members);}catch(RuntimeException e){members.forEach(p->parties.put(p,party));throw e;}
    }
    public synchronized Session reserveQueued(String floor,UUID leader,Set<UUID> members){
        if(members.stream().anyMatch(p->!floor.equals(queued.get(p))))throw new IllegalStateException("Missing queue registration");
        members.forEach(queued::remove);
        try{return reserve(floor,Mode.SHARED,leader,members);}catch(RuntimeException e){members.forEach(p->queued.put(p,floor));throw e;}
    }
    public SessionRegistry(Set<String> floors, int privatePlayers, int privateSlots, Duration cooldown, Duration reservationTimeout, Clock clock) {
        this.floors = Set.copyOf(floors); this.clock = Objects.requireNonNull(clock);
        if (floors.isEmpty() || privatePlayers < 1 || privateSlots < 1 || cooldown.isNegative() || reservationTimeout.isNegative() || reservationTimeout.isZero()) throw new IllegalArgumentException("invalid session settings");
        this.privatePlayers=privatePlayers; this.privateSlots=privateSlots; this.cooldown=cooldown; this.reservationTimeout=reservationTimeout;
    }
    public synchronized Session reserve(String floor, Mode mode, UUID leader, Set<UUID> participants) {
        Objects.requireNonNull(mode); Objects.requireNonNull(leader);
        Set<UUID> members=Set.copyOf(participants); expire();
        if (!floors.contains(floor) || members.isEmpty() || !members.contains(leader)) throw new IllegalArgumentException("invalid floor or party");
        if (members.stream().anyMatch(p->byPlayer.containsKey(p)||queued.containsKey(p)||parties.containsKey(p))) throw new IllegalStateException("participant already reserved");
        if (mode==Mode.PRIVATE && (members.size()>privatePlayers || sessions.values().stream().filter(s->s.mode()==mode).count()>=privateSlots)) throw new IllegalStateException("private capacity");
        if (mode==Mode.SHARED && (storageFailed || remaining(floor).compareTo(Duration.ZERO)>0 || sessions.values().stream().anyMatch(s->s.mode()==mode && s.floor().equals(floor)))) throw new IllegalStateException("shared floor busy");
        UUID id=UUID.randomUUID(); Session s=new Session(id,floor,mode,leader,members,State.RESERVED,clock.instant().plus(reservationTimeout));
        sessions.put(id,s); members.forEach(p->byPlayer.put(p,id)); return s;
    }
    public synchronized Session activate(UUID id) { expire(); return transition(id,State.RESERVED,State.ACTIVE); }
    /** Only the first valid boss defeat transitions to LOOT and starts Shared cooldown. */
    public synchronized Session bossDefeated(UUID id) {
        Session s=transition(id,State.ACTIVE,State.LOOT);
        if (s.mode()==Mode.SHARED) {
            sharedCooldown.put(s.floor(),clock.instant().plus(cooldown));
            try{sink.save(Map.copyOf(sharedCooldown));}catch(java.io.IOException|RuntimeException e){storageFailed=true;throw new IllegalStateException("Cooldown persistence failed; Shared locked",e);}
        } return s;
    }
    private Session transition(UUID id, State from, State to) {
        Session old=sessions.get(id); if(old==null || old.state()!=from) throw new IllegalStateException("invalid transition");
        Session next=new Session(id,old.floor(),old.mode(),old.leader(),old.participants(),to,old.expiresAt()); sessions.put(id,next); return next;
    }
    public synchronized boolean close(UUID id) {
        Session s=sessions.remove(id); if(s==null)return false; s.participants().forEach(p->byPlayer.remove(p,id)); return true;
    }
    public synchronized void expire() {
        Instant now=clock.instant(); sessions.values().stream().filter(s->s.state()==State.RESERVED && !s.expiresAt().isAfter(now)).map(Session::id).toList().forEach(this::close);
        sharedCooldown.values().removeIf(t->!t.isAfter(now));
    }
    public synchronized Duration remaining(String floor) { Instant until=sharedCooldown.get(floor); Instant now=clock.instant(); return until==null || !until.isAfter(now)?Duration.ZERO:Duration.between(now,until); }
    public synchronized Optional<Session> participant(UUID player) { return Optional.ofNullable(byPlayer.get(player)).map(sessions::get); }
    public synchronized Optional<Session> find(UUID id){return Optional.ofNullable(sessions.get(id));}
    public synchronized List<Session> snapshot(){return List.copyOf(sessions.values());}
    public synchronized Map<String,Instant> cooldownSnapshot(){expire();return Map.copyOf(sharedCooldown);}
    public synchronized void restoreCooldowns(Map<String,Instant> values){if(!sessions.isEmpty())throw new IllegalStateException("active sessions");for(var e:values.entrySet())if(e.getKey().matches("[a-z][a-z0-9_]{0,47}")&&e.getValue().isAfter(clock.instant()))sharedCooldown.put(e.getKey(),e.getValue());}
    public synchronized void clear(){sessions.clear();byPlayer.clear();queued.clear();parties.clear();sharedCooldown.clear();}
}
