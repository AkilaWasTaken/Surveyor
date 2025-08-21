package network.akila.surveyor.gui;

import fr.mrmicky.fastinv.FastInv;
import fr.mrmicky.fastinv.ItemBuilder;
import net.kyori.adventure.text.Component;
import network.akila.surveyor.Surveyor;
import network.akila.surveyor.model.Poll;
import network.akila.surveyor.model.PollOption;
import network.akila.surveyor.service.ConfigService;
import network.akila.surveyor.service.PollService;
import network.akila.surveyor.util.DurationParser;
import network.akila.surveyor.util.Utils;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * GUI for voting on a poll.
 */
public class PollVoteView extends FastInv {

    /* Core */

    private final PollService service;
    private final long pollId;
    private final Player viewer;

    private Poll poll;
    private List<PollOption> options;
    private boolean pollClosed;

    /* Layout */

    private final int slotQuestion;
    private final int slotCloses;
    private final int slotRefresh;
    private final int slotClose;
    private final int slotHelp;
    private final List<Integer> optionSlots;

    /*  Formatting */

    private final DateTimeFormatter dateFmt;
    private final String qName;
    private final int qWrapWidth;

    private final String closesName;
    private final String closesAbsTmpl;
    private final String closesRelOpenTmpl;
    private final String closesRelClosed;
    private final String closesRelNone;
    private final String txtNone;

    /* Controls*/

    private final String helpName;
    private final List<String> helpLore;

    private final String refreshName;
    private final List<String> refreshLore;

    private final String closeName;
    private final List<String> closeLore;

    /* Options */

    private final String optNameTmpl;
    private final List<String> optLoreClosed;
    private final List<String> optLorePicked;
    private final List<String> optLoreAlreadyVoted;
    private final List<String> optLoreCanVote;

    /* Materials */

    private final Material frameMat;
    private final List<Material> optionMats;

    /* Messages */

    private final String msgNotFound;
    private final String msgClosed;
    private final String msgAlready;
    private final String msgSuccess;
    private final String msgError;
    private final String msgNoOptions;

    /* Init */

    public PollVoteView(PollService service, long pollId, Player viewer) {
        super(
                Math.max(3, Surveyor.getInstance().menus().getInt("poll-vote.rows", 5)) * 9,
                computeTitle(Surveyor.getInstance().menus(), service, pollId)
        );
        this.service = service;
        this.pollId = pollId;
        this.viewer = viewer;

        ConfigService.ConfigFile menus = Surveyor.getInstance().menus();
        ConfigService.ConfigFile messages = Surveyor.getInstance().messages();

        // Layout
        this.slotQuestion = menus.getInt("poll-vote.slots.question", 10);
        this.slotCloses = menus.getInt("poll-vote.slots.closes", 19);
        this.slotRefresh = menus.getInt("poll-vote.slots.refresh", 41);
        this.slotClose = menus.getInt("poll-vote.slots.close", 40);
        this.slotHelp = menus.getInt("poll-vote.slots.help", 39);
        List<Integer> opts = menus.raw().getIntegerList("poll-vote.slots.options");
        if (opts.isEmpty()) opts = Arrays.asList(12, 13, 14, 21, 22, 23);
        this.optionSlots = opts;

        // Materials
        this.frameMat = materialOr("poll-vote.items.frame.material", "GRAY_STAINED_GLASS_PANE", Material.GRAY_STAINED_GLASS_PANE);
        List<String> optMatNames = menus.getStringList("poll-vote.items.option.materials");
        if (optMatNames.isEmpty()) {
            optMatNames = Arrays.asList("LIME_CONCRETE", "CYAN_CONCRETE", "LIGHT_BLUE_CONCRETE", "ORANGE_CONCRETE", "PINK_CONCRETE", "PURPLE_CONCRETE");
        }
        this.optionMats = optMatNames.stream().map(n -> materialOr(n, n, Material.PAPER)).collect(Collectors.toList());

        // Formatting
        String pat = menus.getString("poll-vote.datetime.pattern", "yyyy-MM-dd HH:mm");
        this.dateFmt = DateTimeFormatter.ofPattern(pat, Locale.ROOT).withZone(ZoneId.systemDefault());

        this.qName = menus.getString("poll-vote.items.question.name", "<aqua><b>Question</b></aqua>");
        this.qWrapWidth = Math.max(12, menus.getInt("poll-vote.items.question.wrapWidth", 40));

        this.closesName = menus.getString("poll-vote.items.closes.name", "<yellow><b>Closes At</b></yellow>");
        this.closesAbsTmpl = menus.getString("poll-vote.items.closes.absolute", "<gray>{time}</gray>");
        this.closesRelOpenTmpl = menus.getString("poll-vote.items.closes.relativeOpen", "<gray>{time}</gray>");
        this.closesRelClosed = menus.getString("poll-vote.items.closes.relativeClosed", "<red>Closed</red>");
        this.closesRelNone = menus.getString("poll-vote.items.closes.relativeNone", "<dark_gray>(no close time)</dark_gray>");
        this.txtNone = menus.getString("poll-vote.text.none", "—");

        // Controls
        this.helpName = menus.getString("poll-vote.items.help.name", "<white><b>Help</b></white>");
        this.helpLore = splitLines(menus.getString("poll-vote.items.help.lore",
                "<gray>• Click an option to vote</gray>\n<dark_gray>• One vote per player</dark_gray>"));

        this.refreshName = menus.getString("poll-vote.items.refresh.name", "<white><b>Refresh</b></white>");
        this.refreshLore = splitLines(menus.getString("poll-vote.items.refresh.lore", "<gray>Click to refresh results/options.</gray>"));

        this.closeName = menus.getString("poll-vote.items.close.name", "<red><b>Close</b></red>");
        this.closeLore = splitLines(menus.getString("poll-vote.items.close.lore", "<gray>Close this menu.</gray>"));

        // Options
        this.optNameTmpl = menus.getString("poll-vote.items.option.name", "<white>[{index}] <green>{text}</green>");
        this.optLoreClosed = splitLines(menus.getString("poll-vote.items.option.lore.closed",
                "<red>Poll closed.</red>\n<gray>You can’t vote.</gray>\n<gray>Votes (others):</gray> <white>{others}</white>"));
        this.optLorePicked = splitLines(menus.getString("poll-vote.items.option.lore.picked",
                "<green>You voted this.</green>\n<gray>Votes (others):</gray> <white>{others}</white>"));
        this.optLoreAlreadyVoted = splitLines(menus.getString("poll-vote.items.option.lore.alreadyVoted",
                "<gray>You already voted.</gray>\n<gray>Votes (others):</gray> <white>{others}</white>"));
        this.optLoreCanVote = splitLines(menus.getString("poll-vote.items.option.lore.canVote",
                "<gray>Click to vote for:</gray>\n<white>{text}</white>\n \n<gray>Votes (others):</gray> <white>{others}</white>"));

        // Messages
        this.msgNotFound = messages.getString("vote.not_found", "<red>Poll not found.</red>");
        this.msgClosed = messages.getString("vote.closed", "<red>This poll is closed. You can no longer vote.</red>");
        this.msgAlready = messages.getString("vote.already", "<red>You already voted for <white>[{index}] {text}</white></red>");
        this.msgSuccess = messages.getString("vote.success", "<green>You voted for <white>[{index}] {text}</white></green>");
        this.msgError = messages.getString("vote.error", "<red>Could not record your vote. Try again.</red>");
        this.msgNoOptions = messages.getString("vote.no_options", "<gray>No options</gray>");

        draw();
    }

    /* Draw */

    private void draw() {
        this.poll = service.find(pollId).join().orElse(null);
        if (poll == null) {
            setCenterError(msgNotFound);
            return;
        }

        this.options = service.options(pollId).join();
        this.pollClosed = poll.getClosesAt() != null && Instant.now().isAfter(poll.getClosesAt());

        drawFrame();
        drawInfo();
        drawControls();
        drawOptions();
    }

    /**
     * Draw the border frame
     */
    private void drawFrame() {
        ItemStack pane = new ItemBuilder(frameMat).name(" ").build();
        int size = getInventory().getSize();
        int cols = 9;

        // top and bottom rows
        for (int i = 0; i < cols; i++) setItem(i, pane);
        int lastRow = size - cols;
        for (int i = lastRow; i < size; i++) setItem(i, pane);

        // left + right columns
        for (int r = 1; r < (size / cols) - 1; r++) {
            setItem(r * cols, pane);
            setItem(r * cols + (cols - 1), pane);
        }
    }

    /**
     * Draw question + closes-at cards
     */
    private void drawInfo() {
        setItem(slotQuestion, componentItem(Material.WRITABLE_BOOK,
                qName,
                wrapMini("<gray>" + poll.getQuestion() + "</gray>", qWrapWidth)
        ));

        String abs = (poll.getClosesAt() == null) ? txtNone : dateFmt.format(poll.getClosesAt());
        String rel;
        if (poll.getClosesAt() == null) {
            rel = closesRelNone;
        } else if (pollClosed) {
            rel = closesRelClosed;
        } else {
            String compact = DurationParser.format(Duration.between(Instant.now(), poll.getClosesAt()));
            rel = closesRelOpenTmpl.replace("{time}", compact);
        }

        setItem(slotCloses, componentItem(Material.CLOCK, closesName, Arrays.asList(
                closesAbsTmpl.replace("{time}", abs),
                rel
        )));
    }

    /** Draw refresh/close/help buttons */
    private void drawControls() {
        setItem(slotRefresh, componentItem(Material.SPYGLASS, refreshName, refreshLore), e -> {
            if (e.getWhoClicked() instanceof Player p) {
                p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1.2f);
                new PollVoteView(service, pollId, p).open(p);
            }
        });

        setItem(slotClose, componentItem(Material.BARRIER, closeName, closeLore), e -> {
            if (e.getWhoClicked() instanceof Player p) {
                p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 0.9f);
                p.closeInventory();
            }
        });

        setItem(slotHelp, componentItem(Material.BOOK, helpName, helpLore));
    }

    /** Draw all option buttons */
    private void drawOptions() {
        if (options == null || options.isEmpty()) {
            setCenterError(msgNoOptions);
            return;
        }

        UUID uuid = viewer.getUniqueId();
        Optional<Integer> myVoteOpt = service.getVote(pollId, uuid).join();

        for (int i = 0; i < options.size() && i < optionSlots.size() && i < 6; i++) {
            final int index = i;
            final PollOption option = options.get(i);

            int totalVotes = safeJoin(service.countVotes(pollId, index), 0);
            int others = totalVotes - (myVoteOpt.isPresent() && myVoteOpt.get() == index ? 1 : 0);
            if (others < 0) others = 0;

            Material mat = optionMats.get(i % optionMats.size());
            String name = optNameTmpl
                    .replace("{index}", String.valueOf(index + 1))
                    .replace("{text}", escapeMini(option.getText()));

            List<String> lore;
            if (pollClosed) {
                lore = replaceVars(optLoreClosed, option.getText(), others);
            } else if (myVoteOpt.isPresent()) {
                boolean picked = myVoteOpt.get() == index;
                lore = replaceVars(picked ? optLorePicked : optLoreAlreadyVoted, option.getText(), others);
            } else {
                lore = replaceVars(optLoreCanVote, option.getText(), others);
            }

            setItem(optionSlots.get(i), componentItem(mat, name, lore), e -> handleVoteClick((Player) e.getWhoClicked(), index, option));
        }
    }

    /* Voting Logic */

    private void handleVoteClick(Player p, int index, PollOption option) {
        Poll latest = service.find(pollId).join().orElse(null);
        if (latest == null) {
            Utils.send(p, msgNotFound);
            return;
        }

        if (latest.getClosesAt() != null && Instant.now().isAfter(latest.getClosesAt())) {
            p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1f, 0.7f);
            Utils.send(p, msgClosed);
            return;
        }

        Optional<Integer> already = service.getVote(pollId, p.getUniqueId()).join();
        if (already.isPresent()) {
            int chosen = already.get();
            String chosenText = (chosen >= 0 && chosen < options.size()) ? options.get(chosen).getText() : "?";
            Utils.send(p, msgAlready
                    .replace("{index}", String.valueOf(chosen + 1))
                    .replace("{text}", chosenText)
            );
            p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1f, 0.7f);
            return;
        }

        try {
            service.vote(pollId, p.getUniqueId(), index);
            p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1.2f);
            Utils.send(p, msgSuccess
                    .replace("{index}", String.valueOf(index + 1))
                    .replace("{text}", option.getText())
            );
            new PollVoteView(service, pollId, p).open(p);
        } catch (RuntimeException ex) {
            Utils.send(p, msgError);
            p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1f, 0.7f);
        }
    }

    /* Helpers */

    private static String computeTitle(ConfigService.ConfigFile menus, PollService service, long pollId) {
        String tmpl = menus.getString("poll-vote.title", "Poll: {question} {suffix}");
        String suffixClosed = menus.getString("poll-vote.titleClosedSuffix", "<gray>(<red>closed</red>)</gray>");
        String suffixOpen = menus.getString("poll-vote.titleOpenSuffix", "<gray>({relative})</gray>");

        return service.find(pollId).join().map(p -> {
            String q = trim(p.getQuestion());
            String suffix;
            if (p.getClosesAt() == null) suffix = "";
            else if (Instant.now().isAfter(p.getClosesAt())) suffix = " " + suffixClosed;
            else {
                String rel = DurationParser.format(Duration.between(Instant.now(), p.getClosesAt()));
                suffix = " " + suffixOpen.replace("{relative}", rel);
            }
            String title = tmpl.replace("{question}", q).replace("{suffix}", suffix);
            Component c = Utils.parse(title);
            return net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection().serialize(c);
        }).orElse("Poll: N/A");
    }

    private void setCenterError(String mini) {
        int center = 22;
        setItem(center, new ItemBuilder(Material.BARRIER)
                .name(asLegacy(viewer, mini))
                .build());
    }

    private ItemStack componentItem(Material mat, String nameMini, List<String> loreMini) {
        ItemStack stack = new ItemStack(mat);
        ItemMeta meta = stack.getItemMeta();
        meta.displayName(Utils.parse(viewer, nameMini));
        if (loreMini != null && !loreMini.isEmpty()) {
            meta.lore(Utils.parse(viewer, loreMini));
        }
        stack.setItemMeta(meta);
        return stack;
    }

    private static List<String> wrapMini(String mini, int width) {
        String stripped = mini.replaceAll("<[^>]+>", "");
        List<String> lines = new ArrayList<>();
        String[] words = stripped.split("\\s+");
        StringBuilder line = new StringBuilder();
        for (String w : words) {
            if ((line.length() + w.length() + 1) > width) {
                lines.add("<gray>" + line);
                line.setLength(0);
            }
            if (!line.isEmpty()) line.append(' ');
            line.append(w);
        }
        if (!line.isEmpty()) lines.add("<gray>" + line);
        return lines;
    }

    private static List<String> splitLines(String multi) {
        if (multi == null || multi.isBlank()) return List.of();
        return Arrays.asList(multi.split("\n"));
    }

    private static String trim(String s) {
        return (s == null || s.length() <= 26) ? (s == null ? "" : s) : s.substring(0, Math.max(0, 26 - 3)) + "...";
    }

    private static Material materialOr(String pathOrName, String defName, Material def) {
        try {
            return Material.valueOf(pathOrName.toUpperCase(Locale.ROOT));
        } catch (Exception ignore) {
            try {
                return Material.valueOf(defName.toUpperCase(Locale.ROOT));
            } catch (Exception ignore2) {
                return def;
            }
        }
    }

    private static String escapeMini(String s) {
        return s == null ? "" : s.replace("<", "\\<");
    }

    private static List<String> replaceVars(List<String> lore, String text, int others) {
        return lore.stream()
                .map(l -> l.replace("{text}", text).replace("{others}", String.valueOf(others)))
                .collect(Collectors.toList());
    }

    private static <T> T safeJoin(java.util.concurrent.CompletableFuture<T> f, T fallback) {
        try {
            return f.join();
        } catch (Exception e) {
            return fallback;
        }
    }

    private static String asLegacy(Player viewer, String mini) {
        return net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection()
                .serialize(Utils.parse(viewer, mini));
    }
}
