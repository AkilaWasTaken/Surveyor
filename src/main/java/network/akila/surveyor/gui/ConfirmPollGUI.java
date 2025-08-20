package network.akila.surveyor.gui;

import fr.mrmicky.fastinv.FastInv;
import fr.mrmicky.fastinv.ItemBuilder;
import net.kyori.adventure.text.Component;
import network.akila.surveyor.Surveyor;
import network.akila.surveyor.service.ConfigService;
import network.akila.surveyor.util.Utils;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

/**
 * GUI used for confirmation of a vote
 */
public class ConfirmPollGUI extends FastInv {

    private final String question;
    private final Instant closesAt;
    private final List<String> options;
    private final Consumer<Boolean> onClose;

    private final DateTimeFormatter dateFmt;

    private final int slotQuestion, slotCloses, slotOptions, slotCreate, slotBack;

    private final Material fillerMat, createMat, backMat;

    private final String qTitle;
    private final String qLineTmpl;
    private final String cTitle;
    private final String cLineTmpl;
    private final String txtNone;
    private final String oTitleBase;
    private final String oCountSuffixTmpl;
    private final String oEntryTmpl;
    private final String createName;
    private final List<String> createLore;
    private final String backName;
    private final List<String> backLore;

    public ConfirmPollGUI(String question, Instant closesAt, List<String> options, Consumer<Boolean> onClose) {
        super(rows() * 9, title());
        this.question = question;
        this.closesAt = closesAt;
        this.options = options;
        this.onClose = onClose;

        ConfigService.ConfigFile menus = Surveyor.getInstance().menus();

        // Datetime
        this.dateFmt = DateTimeFormatter.ofPattern(
                menus.getString("confirm-poll.datetime.pattern", "yyyy-MM-dd HH:mm"),
                Locale.ROOT
        ).withZone(ZoneId.systemDefault());

        // Slots
        this.slotQuestion = menus.getInt("confirm-poll.slots.question", 10);
        this.slotCloses = menus.getInt("confirm-poll.slots.closes", 11);
        this.slotOptions = menus.getInt("confirm-poll.slots.options", 12);
        this.slotCreate = menus.getInt("confirm-poll.slots.create", 15);
        this.slotBack = menus.getInt("confirm-poll.slots.back", 16);

        // Materials
        this.fillerMat = mat(menus.getString("confirm-poll.filler", "LIGHT_GRAY_STAINED_GLASS_PANE"), Material.LIGHT_GRAY_STAINED_GLASS_PANE);
        this.createMat = mat(menus.getString("confirm-poll.items.create.material", "LIME_CONCRETE"), Material.LIME_CONCRETE);
        this.backMat = mat(menus.getString("confirm-poll.items.back.material", "RED_CONCRETE"), Material.RED_CONCRETE);

        // Texts
        this.qTitle = menus.getString("confirm-poll.items.question.name", "<aqua><b>Question</b></aqua>");
        this.qLineTmpl = menus.getString("confirm-poll.items.question.line", "<gray>{question}");

        this.cTitle = menus.getString("confirm-poll.items.closes.name", "<yellow><b>Closes At</b></yellow>");
        this.cLineTmpl = menus.getString("confirm-poll.items.closes.line", "<gray>{time}");
        this.txtNone = menus.getString("confirm-poll.text.none", "—");

        this.oTitleBase = menus.getString("confirm-poll.items.options.name", "<white><b>Options</b></white>");
        this.oCountSuffixTmpl = menus.getString("confirm-poll.items.options.countSuffix", "<gray>({count})</gray>");
        this.oEntryTmpl = menus.getString("confirm-poll.items.options.entry", "<gray> {index}. <white>{text}");

        this.createName = menus.getString("confirm-poll.items.create.name", "<green><b>Create</b></green>");
        this.createLore = splitLines(menus.getString("confirm-poll.items.create.lore", "<gray>Create this poll.</gray>"));

        this.backName = menus.getString("confirm-poll.items.back.name", "<red><b>Back</b></red>");
        this.backLore = splitLines(menus.getString("confirm-poll.items.back.lore", "<gray>Go back to presets.</gray>"));

        draw();
    }

    private static int rows() {
        return Math.max(1, Surveyor.getInstance().menus().getInt("confirm-poll.rows", 3));
    }

    private static String title() {
        return Surveyor.getInstance().menus().getString("confirm-poll.title", "Confirm Poll");
    }

    private static Material mat(String name, Material def) {
        try {
            return Material.valueOf(name.toUpperCase(Locale.ROOT));
        } catch (Exception ignored) {
            return def;
        }
    }

    private static List<String> splitLines(String s) {
        if (s == null || s.isBlank()) return List.of();
        return Arrays.asList(s.split("\n"));
    }

    private void draw() {
        ItemStack filler = new ItemBuilder(fillerMat).name(" ").build();
        for (int i = 0; i < getInventory().getSize(); i++) setItem(i, filler);

        String qLine = qLineTmpl.replace("{question}", question);
        setItem(slotQuestion, componentItem(Material.WRITABLE_BOOK, qTitle, List.of(qLine)));

        String closesAbs = (closesAt == null) ? txtNone : dateFmt.format(closesAt);
        String cLine = cLineTmpl.replace("{time}", closesAbs);
        setItem(slotCloses, componentItem(Material.CLOCK, cTitle, List.of(cLine)));

        String oCount = oCountSuffixTmpl.replace("{count}", String.valueOf(options.size()));
        List<String> optLore = new ArrayList<>(options.size());
        int idx = 1;
        for (String o : options) {
            optLore.add(
                    oEntryTmpl
                            .replace("{index}", String.valueOf(idx++))
                            .replace("{text}", o)
            );
        }
        setItem(slotOptions, componentItem(Material.PAPER, oTitleBase + " " + oCount, optLore));

        setItem(slotCreate, componentItem(createMat, createName, createLore), e -> {
            if (!(e.getWhoClicked() instanceof Player p)) return;
            p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1.2f);
            p.closeInventory();
            onClose.accept(true);
        });

        setItem(slotBack, componentItem(backMat, backName, backLore), e -> {
            if (!(e.getWhoClicked() instanceof Player p)) return;
            p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 0.9f);
            p.closeInventory();
            onClose.accept(false);
        });
    }

    private ItemStack componentItem(Material mat, String nameMini, List<String> loreMini) {
        ItemStack stack = new ItemStack(mat);
        ItemMeta meta = stack.getItemMeta();

        Component name = Utils.parse(null, nameMini);
        meta.displayName(name);

        if (loreMini != null && !loreMini.isEmpty()) {
            meta.lore(Utils.parse(null, loreMini));
        }

        stack.setItemMeta(meta);
        return stack;
    }
}
