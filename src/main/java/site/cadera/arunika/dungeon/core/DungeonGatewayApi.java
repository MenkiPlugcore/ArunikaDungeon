package site.cadera.arunika.dungeon.core;
import java.util.*;
/** Read-only service contract for the future CdrWaystone bridge. No teleport or ticket mutation. */
public interface DungeonGatewayApi {
    int apiVersion();
    boolean entryEnabled();
    List<DungeonCatalog.Floor> floors();
    Optional<SessionRegistry.Session> session(UUID player);
    default Optional<String> queuedFloor(UUID player){return Optional.empty();}
    record SharedFloorStatus(String floor,String state,long seconds,List<UUID> queued,Set<UUID> active,Set<UUID> living){public SharedFloorStatus{queued=List.copyOf(queued);active=Set.copyOf(active);living=Set.copyOf(living);}}
    record PrivatePartyStatus(UUID id,String floor,UUID leader,Set<UUID> members,Set<UUID> ready,boolean queued){public PrivatePartyStatus{members=Set.copyOf(members);ready=Set.copyOf(ready);}}
    default List<PrivatePartyStatus> privateStatus(){return List.of();}
    /** Main-thread only; staging status, not an authorization to teleport. */
    default List<SharedFloorStatus> sharedStatus(){return List.of();}
}
