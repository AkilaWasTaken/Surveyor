package network.akila.surveyor.listener;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Captures the next chat message from a player.
 */
@SuppressWarnings("unused")
public final class ChatOnceListener implements Listener {

    private static final Map<UUID, Pending> WAITING = new ConcurrentHashMap<>();
    private static Plugin PLUGIN;

    private record Pending(Consumer<String> handler, Runnable onTimeout) {
    }

    private ChatOnceListener() {
    }

    public static void init(Plugin plugin) {
        if (PLUGIN != null) return;
        PLUGIN = plugin;
        Bukkit.getPluginManager().registerEvents(new ChatOnceListener(), plugin);
    }

    public static void capture(Player player, Consumer<String> handler) {
        capture(player, handler, null, null);
    }

    public static void capture(Player player, Consumer<String> handler, Duration timeout, Runnable onTimeout) {
        UUID id = player.getUniqueId();
        WAITING.put(id, new Pending(handler, onTimeout));

        if (timeout != null && !timeout.isNegative() && !timeout.isZero()) {
            long ticks = Math.max(1, timeout.toSeconds() * 20);
            Bukkit.getScheduler().runTaskLater(PLUGIN, () -> {
                Pending removed = WAITING.remove(id);
                if (removed != null && removed.onTimeout != null) {
                    removed.onTimeout.run();
                }
            }, ticks);
        }
    }

    public static void cancel(Player player) {
        WAITING.remove(player.getUniqueId());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onAsyncChat(AsyncChatEvent event) {
        UUID id = event.getPlayer().getUniqueId();
        Pending pending = WAITING.remove(id);
        if (pending == null) return;

        event.setCancelled(true);

        Component msgComponent = event.message();
        String plain = PlainTextComponentSerializer.plainText().serialize(msgComponent);

        Bukkit.getScheduler().runTask(PLUGIN, () -> pending.handler.accept(plain));
    }
}
