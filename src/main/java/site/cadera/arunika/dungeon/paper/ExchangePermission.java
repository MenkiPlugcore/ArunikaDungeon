package site.cadera.arunika.dungeon.paper;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.node.Node;
import site.cadera.arunika.dungeon.exchange.ExchangeCatalog.Grant;
/** Optional LuckPerms API adapter; only used after explicit allowlist and duplicate check. */
final class ExchangePermission {
 static boolean available(UUID player,Grant grant){var user=LuckPermsProvider.get().getUserManager().getUser(player);return user!=null&&!user.getNodes().stream().anyMatch(n->n.getKey().equalsIgnoreCase(grant.node())&&!n.hasExpired())&&!user.getCachedData().getPermissionData().checkPermission(grant.node()).asBoolean();}
 static CompletableFuture<Void> grant(UUID player,Grant grant){var manager=LuckPermsProvider.get().getUserManager();var user=manager.getUser(player);if(user==null||!available(player,grant))throw new IllegalStateException("Permission already exists or user unavailable");var node=Node.builder(grant.node()).value(true).expiry(Duration.ofSeconds(grant.seconds())).build();if(!user.data().add(node).wasSuccessful())throw new IllegalStateException("Permission not added");return manager.saveUser(user);}
}
