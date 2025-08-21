package network.akila.surveyor.gui;

import fr.mrmicky.fastinv.FastInv;
import fr.mrmicky.fastinv.ItemBuilder;
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
 * GUI that lets a player confirm or cancel poll creation.
 */
public class ConfirmPollGUI extends FastInv {

    private final String question;
    private final Instant closesAt;
    private final List<String> options;
    private final Consumer<Boolean> onClose;

    /* Formatting */
    private final DateTimeFormatter dateFmt;

    /* Layout slots */
    private final int slotQuestion;
    private final int slotCloses;
    private final int slotOptions;
    private final int slotCreate;
    private final int slotBack;

    /* Item materials */
    private final Material fillerMat;
    private final Material createMat;
    private final Material backMat;

    /* Text values */
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

        /* Time format */
        this.dateFmt = DateTimeFormatter.ofPattern(
                menus.getString("confirm-poll.datetime.pattern", "yyyy-MM-dd HH:mm"),
                Locale.ROOT
        ).withZone(ZoneId.systemDefault());

        /* Layout */
        this.slotQuestion = menus.getInt("confirm-poll.slots.question", 10);
        this.slotCloses = menus.getInt("confirm-poll.slots.closes", 11);
        this.slotOptions = menus.getInt("confirm-poll.slots.options", 12);
        this.slotCreate = menus.getInt("confirm-poll.slots.create", 15);
        this.slotBack = menus.getInt("confirm-poll.slots.back", 16);

        /* Materials */
        this.fillerMat = material(menus.getString("confirm-poll.filler", "LIGHT_GRAY_STAINED_GLASS_PANE"), Material.LIGHT_GRAY_STAINED_GLASS_PANE);
        this.createMat = material(menus.getString("confirm-poll.items.create.material", "LIME_CONCRETE"), Material.LIME_CONCRETE);
        this.backMat = material(menus.getString("confirm-poll.items.back.material", "RED_CONCRETE"), Material.RED_CONCRETE);

        /* Texts */
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

    /* Config Helpers */

    private static int rows() {
        return Math.max(1, Surveyor.getInstance().menus().getInt("confirm-poll.rows", 3));
    }

    private static String title() {
        return Surveyor.getInstance().menus().getString("confirm-poll.title", "Confirm Poll");
    }

    private static Material material(String name, Material def) {
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

    /* Drawing */

    private void draw() {
        fillWithFiller();
        drawQuestion();
        drawClosesAt();
        drawOptions();
        drawConfirmButton();
        drawBackButton();
    }

    private void fillWithFiller() {
        ItemStack filler = new ItemBuilder(fillerMat).name(" ").build();
        for (int i = 0; i < getInventory().getSize(); i++) {
            setItem(i, filler);
        }
    }

    private void drawQuestion() {
        String line = qLineTmpl.replace("{question}", question);
        setItem(slotQuestion, componentItem(Material.WRITABLE_BOOK, qTitle, List.of(line)));
    }

    private void drawClosesAt() {
        String closesAbs = (closesAt == null) ? txtNone : dateFmt.format(closesAt);
        String line = cLineTmpl.replace("{time}", closesAbs);
        setItem(slotCloses, componentItem(Material.CLOCK, cTitle, List.of(line)));
    }

    private void drawOptions() {
        String countSuffix = oCountSuffixTmpl.replace("{count}", String.valueOf(options.size()));
        List<String> lore = new ArrayList<>();
        int idx = 1;
        for (String opt : options) {
            lore.add(oEntryTmpl
                    .replace("{index}", String.valueOf(idx++))
                    .replace("{text}", opt));
        }
        setItem(slotOptions, componentItem(Material.PAPER, oTitleBase + " " + countSuffix, lore));
    }

    private void drawConfirmButton() {
        setItem(slotCreate, componentItem(createMat, createName, createLore), e -> {
            if (e.getWhoClicked() instanceof Player p) {
                p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1.2f);
                p.closeInventory();
                onClose.accept(true);
            }
        });
    }

    private void drawBackButton() {
        setItem(slotBack, componentItem(backMat, backName, backLore), e -> {
            if (e.getWhoClicked() instanceof Player p) {
                p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 0.9f);
                p.closeInventory();
                onClose.accept(false);
            }
        });
    }

    /* Item builder */

    private ItemStack componentItem(Material mat, String nameMini, List<String> loreMini) {
        ItemStack stack = new ItemStack(mat);
        ItemMeta meta = stack.getItemMeta();

        meta.displayName(Utils.parse(null, nameMini));

        if (loreMini != null && !loreMini.isEmpty()) {
            meta.lore(Utils.parse(null, loreMini));
        }

        stack.setItemMeta(meta);
        return stack;
    }
}
