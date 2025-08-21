package network.akila.surveyor.gui;

import fr.mrmicky.fastinv.FastInv;
import fr.mrmicky.fastinv.ItemBuilder;
import net.kyori.adventure.text.Component;
import network.akila.surveyor.Surveyor;
import network.akila.surveyor.listener.ChatOnceListener;
import network.akila.surveyor.model.Poll;
import network.akila.surveyor.service.ConfigService;
import network.akila.surveyor.service.PollService;
import network.akila.surveyor.util.DurationParser;
import network.akila.surveyor.util.Utils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Wizard GUI for creating polls.
 */
public class CreatePollWizard extends FastInv {

    /* Core */

    private final PollService service;
    private final Instant closesAt;
    private final String question;
    private final Consumer<Poll> afterCreate;

    private boolean shuffleOptions = false;

    /* Layout */

    private final int slotQuestion, slotCloses, slotShuffle, slotCancel, slotHelp, slotCustom;
    private final int[] presetSlots;
    private final List<Integer> frameSlots;

    /* Materials */

    private final Material fillerMat, cancelMat, helpMat, customMat, shuffleMat;

    /* Formatting */

    private final DateTimeFormatter dateFmt;

    private final String qName, qLineTmpl;
    private final int qWrapWidth;

    private final String closesName, closesAbsTmpl, closesRelSuffix,
            closesRelPositiveTmpl, closesRelNegative;

    private final String shuffleNameTmpl, shuffleStateOn, shuffleStateOff;
    private final List<String> shuffleLore;

    private final String cancelName;
    private final List<String> cancelLore;

    private final String helpName;
    private final List<String> helpLore;

    private final String customName;
    private final List<String> customLore;

    /* Presets */

    private final String presetItemNameTmpl, presetHeaderTmpl,
            presetEntryTmpl, presetTrimMsg, presetHintPreview, presetHintInstant;

    private final List<PresetDef> presets;

    /* Messages */

    private final String msgCustomPrompt, msgCustomNone, msgCreated;

    /* Constructor */

    public CreatePollWizard(PollService service, Instant closesAt, String question, Consumer<Poll> afterCreate) {
        super(
                Math.max(1, Surveyor.getInstance().menus().getInt("create-wizard.rows", 5)) * 9,
                Surveyor.getInstance().menus().getString("create-wizard.title", "Create Poll: Pick Answers")
        );

        this.service = service;
        this.closesAt = closesAt;
        this.question = question;
        this.afterCreate = afterCreate;

        ConfigService.ConfigFile menus = Surveyor.getInstance().menus();
        ConfigService.ConfigFile messages = Surveyor.getInstance().messages();

        /* DateTime */
        this.dateFmt = DateTimeFormatter.ofPattern(
                menus.getString("create-wizard.datetime.pattern", "yyyy-MM-dd HH:mm"),
                Locale.ROOT
        ).withZone(ZoneId.systemDefault());

        /* Slots */
        this.slotQuestion = menus.getInt("create-wizard.slots.question", 10);
        this.slotCloses = menus.getInt("create-wizard.slots.closes", 19);
        this.slotShuffle = menus.getInt("create-wizard.slots.shuffle", 28);
        this.slotCancel = menus.getInt("create-wizard.slots.cancel", 40);
        this.slotHelp = menus.getInt("create-wizard.slots.help", 41);
        this.slotCustom = menus.getInt("create-wizard.slots.custom", 39);

        List<Integer> presetList = menus.raw().getIntegerList("create-wizard.slots.presets");
        if (presetList.isEmpty()) presetList = Arrays.asList(12, 13, 14, 21, 22, 23);
        this.presetSlots = presetList.stream().mapToInt(Integer::intValue).toArray();

        List<Integer> frame = menus.raw().getIntegerList("create-wizard.slots.frame");
        if (frame.isEmpty()) {
            frame = Arrays.asList(
                    // top row
                    0, 1, 2, 3, 4, 5, 6, 7, 8,
                    // bottom row
                    36, 37, 38, 39, 40, 41, 42, 43, 44,
                    // sides
                    9, 18, 27, 17, 26, 35
            );
        }
        this.frameSlots = frame;

        /* Materials */
        this.fillerMat = mat(menus.getString("create-wizard.items.filler", "GRAY_STAINED_GLASS_PANE"), Material.GRAY_STAINED_GLASS_PANE);
        this.cancelMat = mat(menus.getString("create-wizard.items.cancel.material", "BARRIER"), Material.BARRIER);
        this.helpMat = mat(menus.getString("create-wizard.items.help.material", "BOOK"), Material.BOOK);
        this.customMat = mat(menus.getString("create-wizard.items.custom.material", "NAME_TAG"), Material.NAME_TAG);
        this.shuffleMat = mat(menus.getString("create-wizard.items.shuffle.material", "LEVER"), Material.LEVER);

        /* Question */
        this.qName = menus.getString("create-wizard.items.question.name", "<aqua><b>Question</b></aqua>");
        this.qLineTmpl = menus.getString("create-wizard.items.question.line", "<gray>{question}");
        this.qWrapWidth = Math.max(12, menus.getInt("create-wizard.items.question.wrapWidth", 40));

        /* Closes */
        this.closesName = menus.getString("create-wizard.items.closes.name", "<yellow><b>Closes At</b></yellow>");
        this.closesAbsTmpl = menus.getString("create-wizard.items.closes.absolute", "<gray>{time}");
        this.closesRelSuffix = menus.getString("create-wizard.items.closes.relativeSuffix", " left");
        this.closesRelPositiveTmpl = menus.getString("create-wizard.items.closes.relativePositive", "<gray>{time}</gray>");
        this.closesRelNegative = menus.getString("create-wizard.items.closes.relativeNegative", "<red>expired</red>");

        /* Shuffle */
        this.shuffleNameTmpl = menus.getString("create-wizard.items.shuffle.name",
                "<light_purple>Shuffle Options: </light_purple>{state}");
        this.shuffleStateOn = menus.getString("create-wizard.items.shuffle.state.on", "<green>ON</green>");
        this.shuffleStateOff = menus.getString("create-wizard.items.shuffle.state.off", "<red>OFF</red>");
        this.shuffleLore = splitLines(menus.getString("create-wizard.items.shuffle.lore",
                "<gray>Click to toggle</gray>\n<dark_gray>Randomizes order on create</dark_gray>"));

        /* Controls */
        this.cancelName = menus.getString("create-wizard.items.cancel.name", "<red><b>Cancel</b></red>");
        this.cancelLore = splitLines(menus.getString("create-wizard.items.cancel.lore", "<gray>Close without creating.</gray>"));

        this.helpName = menus.getString("create-wizard.items.help.name", "<white><b>Help</b></white>");
        this.helpLore = splitLines(menus.getString("create-wizard.items.help.lore",
                "<gray>• Left-click a preset → Preview & Confirm</gray>\n" +
                        "<gray>• <gold>Shift-Left-click</gold> → Instant Create</gray>\n" +
                        "<gray>• <light_purple>Shuffle</light_purple> → randomize options</gray>\n" +
                        "<gray>• <green>Custom</green> → type your own</gray>"));

        this.customName = menus.getString("create-wizard.items.custom.name", "<green><b>Custom Options</b></green>");
        this.customLore = splitLines(menus.getString("create-wizard.items.custom.lore",
                "<gray>Click to enter options via chat.</gray>\n" +
                        "<dark_gray>Format: A, B, C, D</dark_gray>\n" +
                        "<dark_gray>Max 6 options; extra are trimmed.</dark_gray>"));

        /* Presets */
        this.presetItemNameTmpl = menus.getString("create-wizard.items.preset.name", "<white>{title}</white>");
        this.presetHeaderTmpl = menus.getString("create-wizard.items.preset.header", "<gray>{title}");
        this.presetEntryTmpl = menus.getString("create-wizard.items.preset.entry", "<gray> {index}. <white>{text}");
        this.presetTrimMsg = menus.getString("create-wizard.items.preset.trim", "<red>(Will trim to 6 options)</red>");
        this.presetHintPreview = menus.getString("create-wizard.items.preset.hint.preview", "<gray>Left-click: <white>Preview & Confirm</white>");
        this.presetHintInstant = menus.getString("create-wizard.items.preset.hint.instant", "<gold>Shift-Left-click</gold><gray>: <white>Instant Create</white>");

        List<Map<?, ?>> rawPresets = menus.raw().getMapList("create-wizard.presets");
        this.presets = parsePresets(rawPresets);

        /* Messages */
        this.msgCustomPrompt = messages.getString("create.custom.prompt",
                "<gray>Type your options (comma-separated). Example: <white>Yes, No, Maybe</white>");
        this.msgCustomNone = messages.getString("create.custom.none",
                "<red>No options detected. Reopening wizard…</red>");
        this.msgCreated = messages.getString("create.created",
                "<green>Created poll <white>#{id}</white>: <white>{question}</white></green>");

        /* Draw UI */
        drawFrame();
        drawPreviewPane();
        drawControls();
        drawPresets();
    }

    /* UI */

    private void drawFrame() {
        ItemStack pane = new ItemBuilder(fillerMat).name(" ").build();
        for (int s : frameSlots) setItem(s, pane);
    }

    private void drawPreviewPane() {
        List<String> qLore = wrapMini(qLineTmpl.replace("{question}", question), qWrapWidth);
        setItem(slotQuestion, componentItem(Material.WRITABLE_BOOK, qName, qLore));

        String abs = dateFmt.format(closesAt);
        Duration until = Duration.between(Instant.now(), closesAt);
        String relMini = until.isNegative()
                ? closesRelNegative
                : closesRelPositiveTmpl.replace("{time}", DurationParser.format(until) + closesRelSuffix);

        setItem(slotCloses, componentItem(
                Material.CLOCK, closesName, List.of(
                        closesAbsTmpl.replace("{time}", abs),
                        relMini
                )
        ));

        String shName = shuffleNameTmpl.replace("{state}", shuffleOptions ? shuffleStateOn : shuffleStateOff);
        setItem(slotShuffle, componentItem(shuffleMat, shName, shuffleLore), e -> {
            shuffleOptions = !shuffleOptions;
            drawPreviewPane();
            if (e.getWhoClicked() instanceof Player p) {
                p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1.2f);
            }
        });
    }

    private void drawControls() {
        setItem(slotCancel, componentItem(cancelMat, cancelName, cancelLore),
                e -> e.getWhoClicked().closeInventory());

        setItem(slotHelp, componentItem(helpMat, helpName, helpLore));

        setItem(slotCustom, componentItem(customMat, customName, customLore), e -> {
            if (!(e.getWhoClicked() instanceof Player player)) return;
            player.closeInventory();

            player.sendMessage(Utils.parse(player, msgCustomPrompt));
            ChatOnceListener.capture(player, line -> {
                List<String> opts = Arrays.stream(line.split(","))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .collect(Collectors.toList());
                if (opts.isEmpty()) {
                    player.sendMessage(Utils.parse(player, msgCustomNone));
                    Bukkit.getScheduler().runTask(Surveyor.getInstance(),
                            () -> new CreatePollWizard(service, closesAt, question, afterCreate).open(player));
                    return;
                }
                createViaFlow(player, opts, false);
            });
        });
    }

    private void drawPresets() {
        for (int i = 0; i < presets.size() && i < presetSlots.length; i++) {
            PresetDef p = presets.get(i);
            addPreset(presetSlots[i], mat(p.material, Material.PAPER), p.title, p.options);
        }
    }

    private void addPreset(int slot, Material icon, String title, List<String> options) {
        boolean willTrim = options.size() > 6;
        List<String> lore = new ArrayList<>();

        lore.add(presetHeaderTmpl.replace("{title}", title));
        lore.add(" ");

        int idx = 1;
        for (String o : options.subList(0, Math.min(options.size(), 6))) {
            lore.add(presetEntryTmpl.replace("{index}", String.valueOf(idx++)).replace("{text}", o));
        }
        if (willTrim) {
            lore.add(" ");
            lore.add(presetTrimMsg);
        }
        lore.add(" ");
        lore.add(presetHintPreview);
        lore.add(presetHintInstant);

        ItemStack stack = componentItem(icon,
                presetItemNameTmpl.replace("{title}", title),
                lore);

        setItem(slot, stack, e -> {
            if (!(e.getWhoClicked() instanceof Player player)) return;
            List<String> trimmed = options.size() > 6 ? options.subList(0, 6) : options;
            boolean instant = e.getClick() == ClickType.SHIFT_LEFT;
            createViaFlow(player, trimmed, instant);
        });
    }

    /* Flow */

    private void createViaFlow(Player player, List<String> options, boolean instant) {
        List<String> finalOptions = new ArrayList<>(options);
        if (shuffleOptions) Collections.shuffle(finalOptions);

        if (instant) {
            doCreate(player, finalOptions);
            return;
        }

        new ConfirmPollGUI(question, closesAt, finalOptions, confirmed -> {
            if (confirmed) {
                doCreate(player, finalOptions);
            } else {
                new CreatePollWizard(service, closesAt, question, afterCreate).open(player);
            }
        }).open(player);
    }

    private void doCreate(Player player, List<String> options) {
        Poll poll = service.create(question, closesAt, options).join();
        player.playSound(player.getLocation(), Sound.UI_TOAST_IN, 1f, 1f);

        String createdMsg = msgCreated
                .replace("{id}", String.valueOf(poll.getId()))
                .replace("{question}", poll.getQuestion());
        player.sendMessage(Utils.parse(player, createdMsg));

        if (afterCreate != null) afterCreate.accept(poll);
    }

    /* Helpers */

    private List<PresetDef> parsePresets(List<Map<?, ?>> rawPresets) {
        if (rawPresets.isEmpty()) {
            return new ArrayList<>(List.of(
                    new PresetDef("Yes / No", "LIME_DYE", Arrays.asList("Yes", "No")),
                    new PresetDef("Yes / No / Maybe", "CYAN_DYE", Arrays.asList("Yes", "No", "Maybe")),
                    new PresetDef("A / B / C / D", "LIGHT_BLUE_DYE", Arrays.asList("A", "B", "C", "D")),
                    new PresetDef("1..5 Rating", "ORANGE_DYE", Arrays.asList("1", "2", "3", "4", "5")),
                    new PresetDef("Agree..Disagree", "PINK_DYE",
                            Arrays.asList("Strongly Agree", "Agree", "Neutral", "Disagree", "Strongly Disagree")),
                    new PresetDef("Custom placeholders", "PURPLE_DYE",
                            Arrays.asList("Option 1", "Option 2", "Option 3"))
            ));
        }

        List<PresetDef> parsed = new ArrayList<>(rawPresets.size());
        for (Map<?, ?> m : rawPresets) {
            String title = String.valueOf(m.containsKey("title") ? m.get("title") : "Preset");
            String material = String.valueOf(m.containsKey("material") ? m.get("material") : "PAPER");

            List<String> opts;
            Object optsObj = m.get("options");
            if (optsObj instanceof List<?> list) {
                opts = list.stream().map(String::valueOf).collect(Collectors.toList());
            } else {
                opts = List.of("A", "B", "C");
            }
            parsed.add(new PresetDef(title, material, opts));
        }
        return parsed;
    }

    private static Material mat(String name, Material def) {
        try {
            return Material.valueOf(name.toUpperCase(Locale.ROOT));
        } catch (Exception ignored) {
            return def;
        }
    }

    private ItemStack componentItem(Material mat, String nameMini, List<String> loreMini) {
        ItemStack stack = new ItemStack(mat);
        ItemMeta meta = stack.getItemMeta();

        if (nameMini != null) {
            Component name = Utils.parse(null, nameMini);
            meta.displayName(name);
        }

        if (loreMini != null && !loreMini.isEmpty()) {
            meta.lore(Utils.parse(null, loreMini));
        }

        stack.setItemMeta(meta);
        return stack;
    }

    private static List<String> wrapMini(String mini, int width) {
        String plain = mini.replaceAll("<[^>]+>", "");
        List<String> lines = new ArrayList<>();
        String[] words = plain.split(" ");
        StringBuilder current = new StringBuilder();

        for (String w : words) {
            if ((current.length() + w.length() + 1) > width) {
                lines.add("<gray>" + current);
                current = new StringBuilder();
            }
            if (!current.isEmpty()) current.append(' ');
            current.append(w);
        }

        if (!current.isEmpty()) lines.add("<gray>" + current);
        return lines;
    }

    private static List<String> splitLines(String raw) {
        if (raw == null || raw.isBlank()) return List.of();
        return Arrays.asList(raw.split("\n"));
    }

    /* Preset Class */

    private record PresetDef(String title, String material, List<String> options) {
    }
}
