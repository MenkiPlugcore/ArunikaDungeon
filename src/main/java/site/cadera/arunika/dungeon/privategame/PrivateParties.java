package site.cadera.arunika.dungeon.privategame;
import java.util.*;
import site.cadera.arunika.dungeon.core.SessionRegistry;

/** Main-thread service; one queue, per-floor FIFO with independent dispatch. */
public final class PrivateParties {
 public record View(UUID id,String floor,UUID leader,Set<UUID> members,Set<UUID> ready,boolean queued){public View{members=Set.copyOf(members);ready=Set.copyOf(ready);}}
 public interface Port {
  boolean eligible(UUID player,String floor); default boolean invitable(UUID player,String floor){return eligible(player,floor);} boolean arenaReady(String floor); boolean ticketReady(UUID leader,TicketLedger.Ticket ticket) throws Exception;
  void start(SessionRegistry.Session session,TicketLedger.Ticket ticket)throws Exception;
  default void notify(UUID player,String message){}
 }
 private static final class Party {final UUID id=UUID.randomUUID();final String floor;UUID leader;final Set<UUID> members=new LinkedHashSet<>(),ready=new HashSet<>();long deadline;TicketLedger.Ticket ticket;Party(String floor,UUID leader,long now){this.floor=floor;this.leader=leader;members.add(leader);deadline=now+600_000;}}
 private record Invite(UUID party,long expiry){}
 private final SessionRegistry registry;private final Map<String,Integer> limits;private final Port port;
 private final Map<UUID,Party> parties=new LinkedHashMap<>();private final Map<UUID,Invite> invites=new HashMap<>();private final LinkedHashSet<UUID> queue=new LinkedHashSet<>();
 public PrivateParties(SessionRegistry registry,Map<String,Integer> limits,Port port){this.registry=registry;this.limits=Map.copyOf(limits);this.port=port;if(limits.values().stream().anyMatch(n->n<1||n>5))throw new IllegalArgumentException("private player limit");}
 public boolean empty(){return parties.isEmpty();}
 private Party party(UUID player){return registry.party(player).map(parties::get).orElseThrow(()->new IllegalStateException("No private party"));}
 private Party leader(UUID player){Party p=party(player);if(!p.leader.equals(player))throw new IllegalStateException("Leader only");return p;}
 private void change(Party p,long now){queue.remove(p.id);p.ticket=null;p.ready.clear();p.deadline=now+600_000;}
 private void unlocked(Party p){if(queue.contains(p.id))throw new IllegalStateException("Party queued; cancel first");}
 public View create(String floor,UUID player,long now){if(!limits.containsKey(floor)||!port.eligible(player,floor))throw new IllegalStateException("Private floor unavailable");Party p=new Party(floor,player,now);registry.joinParty(p.id,player);parties.put(p.id,p);invites.remove(player);return view(p);}
 public void invite(UUID owner,UUID target,long now){Party p=leader(owner);unlocked(p);if(p.members.size()>=limits.get(p.floor)||p.members.contains(target)||registry.party(target).isPresent()||registry.participant(target).isPresent()||registry.queuedFloor(target).isPresent()||!port.invitable(target,p.floor))throw new IllegalStateException("Target unavailable");Invite last=invites.get(target);if(last!=null&&last.expiry>now)throw new IllegalStateException("Target already invited");invites.put(target,new Invite(p.id,now+60_000));}
 public View accept(UUID player,long now){Invite i=invites.remove(player);Party p=i==null?null:parties.get(i.party);if(p==null||i.expiry<=now||p.members.size()>=limits.get(p.floor)||!port.eligible(player,p.floor))throw new IllegalStateException("Invitation unavailable");unlocked(p);registry.joinParty(p.id,player);p.members.add(player);change(p,now);return view(p);}
 public boolean decline(UUID player){return invites.remove(player)!=null;}
 public void ready(UUID player,boolean yes,long now){Party p=party(player);unlocked(p);if(yes)p.ready.add(player);else p.ready.remove(player);p.deadline=now+600_000;}
 public void kick(UUID owner,UUID target,long now){Party p=leader(owner);unlocked(p);if(owner.equals(target)||!p.members.remove(target))throw new IllegalStateException("Invalid member");registry.leaveParty(p.id,target);change(p,now);}
 public void transfer(UUID owner,UUID target,long now){Party p=leader(owner);unlocked(p);if(!p.members.contains(target))throw new IllegalStateException("Invalid leader");p.leader=target;change(p,now);}
 public void cancel(UUID owner,long now){change(leader(owner),now);}
 public void leave(UUID player,long now){Optional<UUID> id=registry.party(player);if(id.isEmpty())return;Party p=parties.get(id.get());if(p==null)return;if(p.leader.equals(player)){remove(p);return;}p.members.remove(player);registry.leaveParty(p.id,player);change(p,now);}
 private void remove(Party p){queue.remove(p.id);parties.remove(p.id);p.members.forEach(u->registry.leaveParty(p.id,u));invites.values().removeIf(i->i.party.equals(p.id));}
 public void clear(){for(Party p:new ArrayList<>(parties.values()))remove(p);invites.clear();}
 public View status(UUID player){return view(party(player));}
 public List<View> views(){return parties.values().stream().map(this::view).toList();}
 private View view(Party p){return new View(p.id,p.floor,p.leader,p.members,p.ready,queue.contains(p.id));}
 public void enqueue(UUID owner,TicketLedger.Ticket ticket,long now)throws Exception{Party p=leader(owner);unlocked(p);if(!p.ready.equals(p.members)||p.members.stream().anyMatch(u->!port.eligible(u,p.floor))||!port.ticketReady(owner,ticket))throw new IllegalStateException("All members must be ready and leader needs valid ticket");p.ticket=ticket;p.deadline=now+600_000;queue.add(p.id);}
 public void tick(long now,boolean performance){invites.values().removeIf(i->i.expiry<=now);for(Party p:new ArrayList<>(parties.values())){if(p.deadline<=now){remove(p);continue;}for(UUID u:new ArrayList<>(p.members)){if(!parties.containsKey(p.id))break;if(!port.eligible(u,p.floor))leave(u,now);}}if(!performance)return;
  Set<String> attempted=new HashSet<>();for(UUID id:new ArrayList<>(queue)){Party p=parties.get(id);if(p==null||!attempted.add(p.floor)||!port.arenaReady(p.floor))continue;SessionRegistry.Session s;
   try{if(!port.ticketReady(p.leader,p.ticket)){change(p,now);port.notify(p.leader,"Ticket unavailable; ready again.");continue;}}catch(Exception e){change(p,now);port.notify(p.leader,"Ticket validation failed: "+e.getMessage());continue;}
   try{s=registry.reserveParty(p.floor,p.id,p.leader,p.members);}catch(IllegalStateException busy){continue;}
   queue.remove(id);parties.remove(id);invites.values().removeIf(i->i.party.equals(id));
   try{port.start(s,p.ticket);}catch(Exception|LinkageError e){registry.close(s.id());for(UUID u:p.members)port.notify(u,"Private start failed: "+e.getMessage());}
  }
 }
}
