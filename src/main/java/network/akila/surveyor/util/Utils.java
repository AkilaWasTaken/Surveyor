package network.akila.surveyor.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Class for sending nice looking messages.
 * Handles MiniMessage formatting (colors, gradients, rainbow, etc.)
 * PlaceholderAPI Support.
 */

@SuppressWarnings("unused")
public final class Utils {
    private Utils() {
    }

    private static final MiniMessage MM = MiniMessage.miniMessage();

    public static Component parse(Player p, String mini) {
        if (mini == null) return Component.empty();
        return MM.deserialize(applyPAPI(p, mini));
    }

    public static Component parse(String mini) {
        return parse(null, mini);
    }

    public static List<Component> parse(Player p, List<String> lines) {
        if (lines == null) return List.of();
        return lines.stream().map(s -> parse(p, s)).collect(Collectors.toList());
    }

    public static List<Component> parse(List<String> lines) {
        return parse(null, lines);
    }

    public static Component prefixed(Player p, String prefixMini, String msgMini) {
        String s = (prefixMini == null ? "" : prefixMini) + (msgMini == null ? "" : msgMini);
        return parse(p, s);
    }

    public static void send(Player p, String mini) {
        if (p != null) p.sendMessage(parse(p, mini));
    }

    public static void sendPrefixed(Player p, String prefixMini, String msgMini) {
        if (p != null) p.sendMessage(prefixed(p, prefixMini, msgMini));
    }

    public static Component parseWith(Player p, String mini, TagResolver... resolvers) {
        if (mini == null) return Component.empty();
        return MM.deserialize(applyPAPI(p, mini), resolvers);
    }

    public static void actionBar(Player p, String mini) {
        if (p != null) p.sendActionBar(parse(p, mini));
    }

    public static void title(Player p, String titleMini, String subMini, int in, int stay, int out) {
        if (p == null) return;
        p.showTitle(net.kyori.adventure.title.Title.title(
                parse(p, titleMini), parse(p, subMini),
                net.kyori.adventure.title.Title.Times.times(
                        java.time.Duration.ofMillis(in),
                        java.time.Duration.ofMillis(stay),
                        java.time.Duration.ofMillis(out))));
    }

    private static String applyPAPI(Player p, String s) {
        try {
            if (p != null && Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
                Class<?> cls = Class.forName("me.clip.placeholderapi.PlaceholderAPI");
                return (String) cls.getMethod("setPlaceholders", Player.class, String.class).invoke(null, p, s);
            }
        } catch (Throwable ignored) {
        }
        return s;
    }
}
