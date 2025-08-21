package network.akila.surveyor.gui;

import fr.mrmicky.fastinv.FastInv;
import net.kyori.adventure.text.Component;
import network.akila.surveyor.Surveyor;
import network.akila.surveyor.model.Poll;
import network.akila.surveyor.service.ConfigService;
import network.akila.surveyor.service.PollService;
import network.akila.surveyor.util.DurationParser;
import network.akila.surveyor.util.Utils;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * GUI: lists all active (and closed) polls
 */
public class ActivePollsView extends FastInv {

    /* --- Core */
    private final PollService service;
    private final DateTimeFormatter dateFmt;
    private final Duration closingSoonWindow;

    /* --- Layout */
    private final int contentSlots;
    private final int pageSize;
    private final int slotPrev, slotFilter, slotSort, slotPage, slotClose, slotNext;

    /* --- Materials */
    private final Material matPrev, matNext, matFilter, matSort, matPage, matClose;
    private final Material matCardOpen, matCardClosed;

    /* --- Text */
    private final String txtCardName;
    private final List<String> cardLoreTmpl;
    private final String txtNone, txtStatusOpen, txtStatusSoon, txtStatusClosed;
    private final String txtCtaOpen, txtCtaClosed;
    private final String txtPrevName, txtPrevLore, txtNextName, txtNextLore;
    private final String txtFilterName, txtFilterLore, txtSortName, txtSortLore;
    private final String txtPageName, txtPageLore, txtCloseName, txtCloseLore;
    private final String lblFilterAll, lblFilterOpen, lblFilterClosed;
    private final String lblSortNewest, lblSortClosingSoon, lblSortOldest;
    private final String emptyTitleMini;
    private final List<String> emptyLoreMini;

    /* --- State --- */
    private int page = 0;
    private Filter filter = Filter.OPEN;
    private Sort sort = Sort.NEWEST;
    private List<Poll> viewData = List.of();

    public ActivePollsView(PollService service) {
        super(rowsFromMenus() * 9, titleFromMenus());
        this.service = Objects.requireNonNull(service, "PollService required");

        ConfigService.ConfigFile menus = Surveyor.getInstance().menus();

        /* Rows & content area */
        int rows = rowsFromMenus();
        this.contentSlots = (rows - 1) * 9;
        this.pageSize = contentSlots;
        int footerBase = (rows - 1) * 9;

        /* Footer slots */
        this.slotPrev = menus.getInt("active-polls.footer.slots.prev", footerBase);
        this.slotFilter = menus.getInt("active-polls.footer.slots.filter", footerBase + 1);
        this.slotSort = menus.getInt("active-polls.footer.slots.sort", footerBase + 2);
        this.slotPage = menus.getInt("active-polls.footer.slots.page", footerBase + 4);
        this.slotClose = menus.getInt("active-polls.footer.slots.close", footerBase + 7);
        this.slotNext = menus.getInt("active-polls.footer.slots.next", footerBase + 8);

        /* Materials */
        this.matPrev = mat(menus.getString("active-polls.footer.items.prev.material", "ARROW"), Material.ARROW);
        this.matNext = mat(menus.getString("active-polls.footer.items.next.material", "ARROW"), Material.ARROW);
        this.matFilter = mat(menus.getString("active-polls.footer.items.filter.material", "COMPARATOR"), Material.COMPARATOR);
        this.matSort = mat(menus.getString("active-polls.footer.items.sort.material", "COMPASS"), Material.COMPASS);
        this.matPage = mat(menus.getString("active-polls.footer.items.page.material", "MAP"), Material.MAP);
        this.matClose = mat(menus.getString("active-polls.footer.items.close.material", "BARRIER"), Material.BARRIER);
        this.matCardOpen = mat(menus.getString("active-polls.items.card.material_open", "PAPER"), Material.PAPER);
        this.matCardClosed = mat(menus.getString("active-polls.items.card.material_closed", "BOOK"), Material.BOOK);

        /* Formatting */
        this.dateFmt = DateTimeFormatter.ofPattern(
                menus.getString("active-polls.datetime.pattern", "yyyy-MM-dd HH:mm"),
                Locale.ROOT
        ).withZone(ZoneId.systemDefault());

        this.txtCardName = menus.getString("active-polls.items.card.name", "<white>{question}</white>");

        this.cardLoreTmpl = menus.getStringList("active-polls.items.card.lore").isEmpty()
                ? List.of(
                "<gray>ID:</gray> <white>{id}</white>",
                "<gray>Created:</gray> <white>{created}</white>",
                "<gray>Closes:</gray> <white>{closes}</white>{relative}",
                "<gray>Status:</gray> {status}",
                "",
                "{cta}"
        )
                : menus.getStringList("active-polls.items.card.lore");

        /* Text values */
        this.txtNone = menus.getString("active-polls.text.none", "—");
        this.txtStatusOpen = menus.getString("active-polls.text.status.open", "<green>Open</green>");
        this.txtStatusSoon = menus.getString("active-polls.text.status.soon", "<gold>Closing Soon</gold>");
        this.txtStatusClosed = menus.getString("active-polls.text.status.closed", "<red>Closed</red>");
        this.txtCtaOpen = menus.getString("active-polls.text.cta.open", "<green>Click to view & vote</green>");
        this.txtCtaClosed = menus.getString("active-polls.text.cta.closed", "<gray>Voting disabled</gray>");

        this.txtPrevName = menus.getString("active-polls.footer.items.prev.name", "<white><b>Previous</b></white>");
        this.txtPrevLore = menus.getString("active-polls.footer.items.prev.lore", "<gray>Go to previous page</gray>");
        this.txtNextName = menus.getString("active-polls.footer.items.next.name", "<white><b>Next</b></white>");
        this.txtNextLore = menus.getString("active-polls.footer.items.next.lore", "<gray>Go to next page</gray>");
        this.txtFilterName = menus.getString("active-polls.footer.items.filter.name", "<yellow><b>Filter:</b> <white>{filter}</white></yellow>");
        this.txtFilterLore = menus.getString("active-polls.footer.items.filter.lore", "<gray>All / Open / Closed</gray>\n<green>Click to cycle</green>");
        this.txtSortName = menus.getString("active-polls.footer.items.sort.name", "<yellow><b>Sort:</b> <white>{sort}</white></yellow>");
        this.txtSortLore = menus.getString("active-polls.footer.items.sort.lore", "<gray>Newest / Closing Soon / Oldest</gray>\n<green>Click to cycle</green>");
        this.txtPageName = menus.getString("active-polls.footer.items.page.name", "<aqua><b>Page {page} / {pages}</b></aqua>");
        this.txtPageLore = menus.getString("active-polls.footer.items.page.lore", "<dark_gray>Use arrows to navigate</dark_gray>");
        this.txtCloseName = menus.getString("active-polls.footer.items.close.name", "<red><b>Close</b></red>");
        this.txtCloseLore = menus.getString("active-polls.footer.items.close.lore", "<gray>Exit this menu</gray>");

        this.lblFilterAll = menus.getString("active-polls.labels.filter.all", "All");
        this.lblFilterOpen = menus.getString("active-polls.labels.filter.open", "Open");
        this.lblFilterClosed = menus.getString("active-polls.labels.filter.closed", "Closed");

        this.lblSortNewest = menus.getString("active-polls.labels.sort.newest", "Newest");
        this.lblSortClosingSoon = menus.getString("active-polls.labels.sort.closingSoon", "Closing Soon");
        this.lblSortOldest = menus.getString("active-polls.labels.sort.oldest", "Oldest");

        this.emptyTitleMini = menus.getString("active-polls.empty.title", "<gray><b>No polls found</b></gray>");
        this.emptyLoreMini = Arrays.asList(menus.getString("active-polls.empty.lore", "<dark_gray>Try a different filter.</dark_gray>").split("\n"));

        long soonHours = menus.getLong("active-polls.behavior.closingSoonHours", 6L);
        this.closingSoonWindow = Duration.ofHours(Math.max(1, soonHours));

        render();
    }

    /* Rendering */

    private void render() {
        getInventory().clear();
        List<Poll> all = new ArrayList<>(service.findAll().join());
        this.viewData = applySort(applyFilter(all));

        int totalPages = Math.max(1, (int) Math.ceil(viewData.size() / (double) pageSize));
        page = Math.max(0, Math.min(page, totalPages - 1));

        drawGrid(totalPages);
        drawFooter(totalPages);
    }

    private void drawGrid(int totalPages) {
        int start = page * pageSize;
        int end = Math.min(start + pageSize, viewData.size());
        List<Poll> slice = viewData.subList(start, end);

        if (slice.isEmpty()) {
            int center = ((getInventory().getSize() / 9 - 1) / 2) * 9 + 4;
            setItem(center, basicItem(matCardOpen, emptyTitleMini, emptyLoreMini));
            return;
        }

        int slot = 0;
        for (Poll poll : slice) {
            if (slot >= contentSlots) break;

            ItemStack card = makePollCard(poll);
            boolean closed = isClosed(poll);

            if (!closed) {
                final long pollId = poll.getId();
                setItem(slot, card, e -> {
                    if (e.getWhoClicked() instanceof org.bukkit.entity.Player player) {
                        new PollVoteView(service, pollId, player).open(player);
                    }
                });
            } else {
                setItem(slot, card);
            }

            slot++;
        }
    }


    private void drawFooter(int totalPages) {
        /* Prev */
        if (totalPages > 1 && page > 0) {
            setItem(slotPrev, chip(matPrev, txtPrevName, List.of(txtPrevLore)), e -> {
                page--;
                render();
            });
        }

        /* Filter */
        String filterLabel = switch (filter) {
            case ALL -> lblFilterAll;
            case OPEN -> lblFilterOpen;
            case CLOSED -> lblFilterClosed;
        };
        setItem(slotFilter, chip(matFilter, txtFilterName.replace("{filter}", filterLabel),
                Arrays.asList(txtFilterLore.split("\n"))), e -> {
            filter = filter.next();
            page = 0;
            render();
        });

        /* Sort */
        String sortLabel = switch (sort) {
            case NEWEST -> lblSortNewest;
            case CLOSING_SOON -> lblSortClosingSoon;
            case OLDEST -> lblSortOldest;
        };
        setItem(slotSort, chip(matSort, txtSortName.replace("{sort}", sortLabel),
                Arrays.asList(txtSortLore.split("\n"))), e -> {
            sort = sort.next();
            page = 0;
            render();
        });

        /* Page */
        setItem(slotPage, chip(matPage,
                txtPageName.replace("{page}", String.valueOf(page + 1)).replace("{pages}", String.valueOf(totalPages)),
                List.of(txtPageLore)));

        /* Close */
        setItem(slotClose, chip(matClose, txtCloseName, List.of(txtCloseLore)), e -> e.getWhoClicked().closeInventory());

        /* Next */
        if (totalPages > 1 && page + 1 < totalPages) {
            setItem(slotNext, chip(matNext, txtNextName, List.of(txtNextLore)), e -> {
                page++;
                render();
            });
        }
    }

    /* Card creation */

    private ItemStack makePollCard(Poll p) {
        boolean closed = isClosed(p);
        boolean closingSoon = !closed && isClosingSoon(p, closingSoonWindow);

        Material icon = closed ? matCardClosed : matCardOpen;
        String createdStr = p.getCreatedAt() == null ? txtNone : dateFmt.format(p.getCreatedAt());

        final String closesStr;
        final String relativeMini;
        if (p.getClosesAt() != null) {
            closesStr = dateFmt.format(p.getClosesAt());
            String rel = relative(p.getClosesAt());
            relativeMini = (rel != null && !rel.isBlank())
                    ? " <dark_gray>(" + rel + ")</dark_gray>"
                    : "";
        } else {
            closesStr = txtNone;
            relativeMini = "";
        }

        String statusMini = closed ? txtStatusClosed : (closingSoon ? txtStatusSoon : txtStatusOpen);
        String ctaMini = closed ? txtCtaClosed : txtCtaOpen;

        List<String> loreMini = cardLoreTmpl.stream()
                .map(line -> line
                        .replace("{id}", String.valueOf(p.getId()))
                        .replace("{created}", createdStr)
                        .replace("{closes}", closesStr)
                        .replace("{relative}", relativeMini)
                        .replace("{status}", statusMini)
                        .replace("{cta}", ctaMini)
                )
                .toList();

        ItemStack card = basicItem(icon, txtCardName.replace("{question}", escapeMini(p.getQuestion())), loreMini);
        if (closingSoon) addGlow(card);

        return card;
    }


    /* Filtering & Sorting */

    private enum Filter {
        ALL, OPEN, CLOSED;

        Filter next() {
            return values()[(ordinal() + 1) % values().length];
        }
    }

    private enum Sort {
        NEWEST, CLOSING_SOON, OLDEST;

        Sort next() {
            return values()[(ordinal() + 1) % values().length];
        }
    }

    private List<Poll> applyFilter(List<Poll> src) {
        Instant now = Instant.now();
        return switch (filter) {
            case ALL -> src;
            case OPEN ->
                    src.stream().filter(p -> !p.isManuallyClosed() && (p.getClosesAt() == null || now.isBefore(p.getClosesAt()))).toList();
            case CLOSED ->
                    src.stream().filter(p -> p.isManuallyClosed() || (p.getClosesAt() != null && !now.isBefore(p.getClosesAt()))).toList();
        };
    }

    private List<Poll> applySort(List<Poll> src) {
        Comparator<Poll> comp = switch (sort) {
            case NEWEST ->
                    Comparator.comparing(Poll::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())).reversed();
            case CLOSING_SOON ->
                    Comparator.comparing(Poll::getClosesAt, Comparator.nullsLast(Comparator.naturalOrder()));
            case OLDEST -> Comparator.comparing(Poll::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder()));
        };
        return src.stream().sorted(comp).toList();
    }

    /* Helpers */

    private static int rowsFromMenus() {
        return Math.max(2, Surveyor.getInstance().menus().getInt("active-polls.rows", 6));
    }

    private static String titleFromMenus() {
        return Surveyor.getInstance().menus().getString("active-polls.title", "Active Polls");
    }

    private boolean isClosed(Poll p) {
        if (p.isManuallyClosed()) return true;
        return p.getClosesAt() != null && !Instant.now().isBefore(p.getClosesAt());
    }
    private boolean isClosingSoon(Poll p, Duration window) {
        if (p.getClosesAt() == null || isClosed(p)) return false;
        Duration until = Duration.between(Instant.now(), p.getClosesAt());
        return !until.isNegative() && until.compareTo(window) <= 0;
    }
    private String relative(Instant when) {
        if (when == null) return null;
        Duration d = Duration.between(Instant.now(), when);
        if (d.isZero()) return "now";
        boolean past = d.isNegative();
        Duration abs = past ? d.negated() : d;
        return past ? DurationParser.format(abs) + " ago" : "in " + DurationParser.format(abs);
    }

    private ItemStack chip(Material mat, String nameMini, List<String> loreMini) {
        return buildItem(mat, nameMini, loreMini);
    }

    private ItemStack basicItem(Material mat, String nameMini, List<String> loreMini) {
        return buildItem(mat, nameMini, loreMini);
    }

    private ItemStack buildItem(Material mat, String nameMini, List<String> loreMini) {
        ItemStack s = new ItemStack(mat);
        ItemMeta m = s.getItemMeta();
        m.displayName(Utils.parse(nameMini));
        List<Component> lore = Utils.parse(loreMini);
        if (!lore.isEmpty()) m.lore(lore);
        m.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS);
        s.setItemMeta(m);
        return s;
    }
    private void addGlow(ItemStack stack) {
        ItemMeta meta = stack.getItemMeta();
        meta.addEnchant(Enchantment.INFINITY, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        stack.setItemMeta(meta);
    }

    private String escapeMini(String s) {
        return s == null ? "" : s.replace("<", "&lt;").replace(">", "&gt;");
    }

    private static Material mat(String name, Material def) {
        try {
            return Material.valueOf(name.toUpperCase(Locale.ROOT));
        } catch (Exception e) {
            return def;
        }
    }
}
