package network.akila.surveyor.config;

import network.akila.surveyor.service.ConfigService;

import java.util.Arrays;

/**
 * Config bootstrap used to validate configs.
 */
public final class ConfigBootstrap {
    private ConfigBootstrap() {
    }

    public static ConfigService registerAll(ConfigService svc) {
        return svc
                .register(CONFIG_YML, ConfigBootstrap::seedCore)
                .register(MENUS_YML, ConfigBootstrap::seedMenus)
                .register(MESSAGES_YML, ConfigBootstrap::seedMessages);
    }

    public static final String CONFIG_YML = "config.yml";
    public static final String MENUS_YML = "menus.yml";
    public static final String MESSAGES_YML = "messages.yml";

    // config.yml
    private static void seedCore(ConfigService.ConfigFile c) {
        c.setIfMissing("database.type", "SQLITE");

        c.setIfMissing("database.sqlite.file", "polls.db");

        c.setIfMissing("database.mysql.host", "localhost");
        c.setIfMissing("database.mysql.port", 3306);
        c.setIfMissing("database.mysql.database", "surveyor");
        c.setIfMissing("database.mysql.user", "root");
        c.setIfMissing("database.mysql.password", "");
        c.setIfMissing("database.mysql.params", "useSSL=true&useUnicode=true&characterEncoding=utf8");
    }

    // menus.yml
    private static void seedMenus(ConfigService.ConfigFile c) {
        c.setIfMissing("active-polls.title", "Active Polls");
        c.setIfMissing("active-polls.rows", 6);
        c.setIfMissing("active-polls.datetime.pattern", "yyyy-MM-dd HH:mm");

        c.setIfMissing("active-polls.items.card.material_open", "PAPER");
        c.setIfMissing("active-polls.items.card.material_closed", "BOOK");
        c.setIfMissing("active-polls.items.card.name", "<white>{question}</white>");
        c.setIfMissing("active-polls.items.card.lore",
                Arrays.asList(
                        "<gray>ID:</gray> <white>{id}</white>",
                        "<gray>Created:</gray> <white>{created}</white>",
                        "<gray>Closes:</gray> <white>{closes}</white>{relative}",
                        "<gray>Status:</gray> {status}",
                        "{cta}"
                ));

        c.setIfMissing("active-polls.text.none", "—");
        c.setIfMissing("active-polls.text.status.open", "<green>Open</green>");
        c.setIfMissing("active-polls.text.status.soon", "<gold>Closing Soon</gold>");
        c.setIfMissing("active-polls.text.status.closed", "<red>Closed</red>");
        c.setIfMissing("active-polls.text.cta.open", "<green>Click to view & vote</green>");
        c.setIfMissing("active-polls.text.cta.closed", "<gray>Voting disabled</gray>");

        c.setIfMissing("active-polls.labels.filter.all", "All");
        c.setIfMissing("active-polls.labels.filter.open", "Open");
        c.setIfMissing("active-polls.labels.filter.closed", "Closed");

        c.setIfMissing("active-polls.labels.sort.newest", "Newest");
        c.setIfMissing("active-polls.labels.sort.closingSoon", "Closing Soon");
        c.setIfMissing("active-polls.labels.sort.oldest", "Oldest");

        c.setIfMissing("active-polls.empty.title", "<gray><b>No polls found</b></gray>");
        c.setIfMissing("active-polls.empty.lore", "<dark_gray>Try a different filter.</dark_gray>");

        c.setIfMissing("active-polls.behavior.closingSoonHours", 6);

        c.setIfMissing("active-polls.footer.slots.prev", 45);
        c.setIfMissing("active-polls.footer.slots.filter", 46);
        c.setIfMissing("active-polls.footer.slots.sort", 47);
        c.setIfMissing("active-polls.footer.slots.page", 49);
        c.setIfMissing("active-polls.footer.slots.close", 52);
        c.setIfMissing("active-polls.footer.slots.next", 53);

        c.setIfMissing("active-polls.footer.items.prev.material", "ARROW");
        c.setIfMissing("active-polls.footer.items.prev.name", "<white><b>Previous</b></white>");
        c.setIfMissing("active-polls.footer.items.prev.lore", "<gray>Go to previous page</gray>");

        c.setIfMissing("active-polls.footer.items.next.material", "ARROW");
        c.setIfMissing("active-polls.footer.items.next.name", "<white><b>Next</b></white>");
        c.setIfMissing("active-polls.footer.items.next.lore", "<gray>Go to next page</gray>");

        c.setIfMissing("active-polls.footer.items.filter.material", "COMPARATOR");
        c.setIfMissing("active-polls.footer.items.filter.name", "<yellow><b>Filter:</b> <white>{filter}</white></yellow>");
        c.setIfMissing("active-polls.footer.items.filter.lore",
                "<gray>All / Open / Closed</gray>\n<green>Click to cycle</green>");

        c.setIfMissing("active-polls.footer.items.sort.material", "COMPASS");
        c.setIfMissing("active-polls.footer.items.sort.name", "<yellow><b>Sort:</b> <white>{sort}</white></yellow>");
        c.setIfMissing("active-polls.footer.items.sort.lore",
                "<gray>Newest / Closing Soon / Oldest</gray>\n<green>Click to cycle</green>");

        c.setIfMissing("active-polls.footer.items.page.material", "MAP");
        c.setIfMissing("active-polls.footer.items.page.name", "<aqua><b>Page {page} / {pages}</b></aqua>");
        c.setIfMissing("active-polls.footer.items.page.lore", "<dark_gray>Use arrows to navigate</dark_gray>");

        c.setIfMissing("active-polls.footer.items.close.material", "BARRIER");
        c.setIfMissing("active-polls.footer.items.close.name", "<red><b>Close</b></red>");
        c.setIfMissing("active-polls.footer.items.close.lore", "<gray>Exit this menu</gray>");

        c.setIfMissing("poll-vote.title", "Poll: {question} {suffix}");
        c.setIfMissing("poll-vote.titleClosedSuffix", "<gray>(<red>closed</red>)</gray>");
        c.setIfMissing("poll-vote.titleOpenSuffix", "<gray>({relative})</gray>");
        c.setIfMissing("poll-vote.rows", 5);
        c.setIfMissing("poll-vote.datetime.pattern", "yyyy-MM-dd HH:mm");

        c.setIfMissing("poll-vote.slots.question", 10);
        c.setIfMissing("poll-vote.slots.closes", 19);
        c.setIfMissing("poll-vote.slots.refresh", 41);
        c.setIfMissing("poll-vote.slots.close", 40);
        c.setIfMissing("poll-vote.slots.help", 39);
        c.setIfMissing("poll-vote.slots.options", Arrays.asList(12, 13, 14, 21, 22, 23));

        c.setIfMissing("poll-vote.items.frame.material", "GRAY_STAINED_GLASS_PANE");

        c.setIfMissing("poll-vote.items.question.name", "<aqua><b>Question</b></aqua>");
        c.setIfMissing("poll-vote.items.question.wrapWidth", 40);

        c.setIfMissing("poll-vote.items.closes.name", "<yellow><b>Closes At</b></yellow>");
        c.setIfMissing("poll-vote.items.closes.absolute", "<gray>{time}</gray>");
        c.setIfMissing("poll-vote.items.closes.relativeOpen", "<gray>{time}</gray>");
        c.setIfMissing("poll-vote.items.closes.relativeClosed", "<red>Closed</red>");
        c.setIfMissing("poll-vote.items.closes.relativeNone", "<dark_gray>(no close time)</dark_gray>");

        c.setIfMissing("poll-vote.items.help.name", "<white><b>Help</b></white>");
        c.setIfMissing("poll-vote.items.help.lore", "<gray>• Click an option to vote</gray>\n<dark_gray>• One vote per player</dark_gray>");

        c.setIfMissing("poll-vote.items.refresh.name", "<white><b>Refresh</b></white>");
        c.setIfMissing("poll-vote.items.refresh.lore", "<gray>Update results & close state</gray>");

        c.setIfMissing("poll-vote.items.close.name", "<red><b>Close</b></red>");
        c.setIfMissing("poll-vote.items.close.lore", "<gray>Exit this menu</gray>");

        c.setIfMissing("poll-vote.items.option.materials",
                Arrays.asList("LIME_CONCRETE", "CYAN_CONCRETE", "LIGHT_BLUE_CONCRETE", "ORANGE_CONCRETE", "PINK_CONCRETE", "PURPLE_CONCRETE"));
        c.setIfMissing("poll-vote.items.option.name", "<white>[{index}] <green>{text}</green>");
        c.setIfMissing("poll-vote.items.option.lore.closed", "<gray>• Votes locked</gray>\n<dark_gray>Others: {others}</dark_gray>");
        c.setIfMissing("poll-vote.items.option.lore.picked", "<green>• You picked this</green>\n<dark_gray>Others: {others}</dark_gray>");
        c.setIfMissing("poll-vote.items.option.lore.alreadyVoted", "<yellow>• Already voted</yellow>\n<dark_gray>Others: {others}</dark_gray>");
        c.setIfMissing("poll-vote.items.option.lore.canVote", "<gray>• Click to vote</gray>\n<dark_gray>Others: {others}</dark_gray>");

        c.setIfMissing("poll-vote.text.none", "—");

        c.setIfMissing("confirm-poll.title", "Confirm Poll");
        c.setIfMissing("confirm-poll.rows", 3);
        c.setIfMissing("confirm-poll.datetime.pattern", "yyyy-MM-dd HH:mm");
        c.setIfMissing("confirm-poll.filler", "GRAY_STAINED_GLASS_PANE");

        c.setIfMissing("confirm-poll.slots.question", 10);
        c.setIfMissing("confirm-poll.slots.closes", 11);
        c.setIfMissing("confirm-poll.slots.options", 12);
        c.setIfMissing("confirm-poll.slots.create", 15);
        c.setIfMissing("confirm-poll.slots.back", 16);

        c.setIfMissing("confirm-poll.items.question.title", "<aqua><b>Question</b></aqua>");
        c.setIfMissing("confirm-poll.items.question.line", "<gray>{question}");
        c.setIfMissing("confirm-poll.items.closes.title", "<yellow><b>Closes At</b></yellow>");
        c.setIfMissing("confirm-poll.items.closes.line", "<gray>{time}</gray>");
        c.setIfMissing("confirm-poll.items.options.title", "<white><b>Options</b></white>");
        c.setIfMissing("confirm-poll.items.options.countSuffix", "<gray>({count})</gray>");
        c.setIfMissing("confirm-poll.items.options.entry", "<gray> {index}. <white>{text}");
        c.setIfMissing("confirm-poll.items.create.name", "<green><b>Create</b></green>");
        c.setIfMissing("confirm-poll.items.create.lore", "<gray>Create this poll.</gray>");
        c.setIfMissing("confirm-poll.items.back.name", "<red><b>Back</b></red>");
        c.setIfMissing("confirm-poll.items.back.lore", "<gray>Go back to presets.</gray>");
        c.setIfMissing("confirm-poll.text.none", "—");

        c.setIfMissing("create-wizard.title", "Create Poll: Pick Answers");
        c.setIfMissing("create-wizard.rows", 5);
        c.setIfMissing("create-wizard.datetime.pattern", "yyyy-MM-dd HH:mm");

        c.setIfMissing("create-wizard.slots.question", 10);
        c.setIfMissing("create-wizard.slots.closes", 19);
        c.setIfMissing("create-wizard.slots.shuffle", 28);
        c.setIfMissing("create-wizard.slots.cancel", 40);
        c.setIfMissing("create-wizard.slots.help", 41);
        c.setIfMissing("create-wizard.slots.custom", 39);

        c.setIfMissing("create-wizard.items.filler.material", "GRAY_STAINED_GLASS_PANE");
        c.setIfMissing("create-wizard.items.question.name", "<aqua><b>Question</b></aqua>");
        c.setIfMissing("create-wizard.items.question.line", "<gray>{question}");
        c.setIfMissing("create-wizard.items.question.wrapWidth", 40);

        c.setIfMissing("create-wizard.items.closes.name", "<yellow><b>Closes At</b></yellow>");
        c.setIfMissing("create-wizard.items.closes.absolute", "<gray>{time}");
        c.setIfMissing("create-wizard.items.closes.relativeSuffix", " left");
        c.setIfMissing("create-wizard.items.closes.relativePositive", "<gray>{time}</gray>");
        c.setIfMissing("create-wizard.items.closes.relativeNegative", "<red>expired</red>");

        c.setIfMissing("create-wizard.items.shuffle.material", "LEVER");
        c.setIfMissing("create-wizard.items.shuffle.name", "<white><b>Shuffle Options:</b> {state}</white>");
        c.setIfMissing("create-wizard.items.shuffle.state.on", "<green>ON</green>");
        c.setIfMissing("create-wizard.items.shuffle.state.off", "<red>OFF</red>");
        c.setIfMissing("create-wizard.items.shuffle.lore", "<gray>Randomize option order for each voter.</gray>");

        c.setIfMissing("create-wizard.items.cancel.material", "BARRIER");
        c.setIfMissing("create-wizard.items.cancel.name", "<red><b>Cancel</b></red>");
        c.setIfMissing("create-wizard.items.cancel.lore", "<gray>Close without creating.</gray>");

        c.setIfMissing("create-wizard.items.help.material", "BOOK");
        c.setIfMissing("create-wizard.items.help.name", "<white><b>Help</b></white>");
        c.setIfMissing("create-wizard.items.help.lore",
                "<gray>• Left-click a preset to preview & confirm</gray>\n<gray>• <gold>Shift-Left-click</gold> to create instantly</gray>");

        c.setIfMissing("create-wizard.items.custom.material", "NAME_TAG");
        c.setIfMissing("create-wizard.items.custom.name", "<green><b>Custom Options</b></green>");
        c.setIfMissing("create-wizard.items.custom.lore", "<gray>Type options in chat, separated by commas</gray>");

        c.setIfMissing("create-wizard.items.preset.name", "<white>{title}</white>");
        c.setIfMissing("create-wizard.items.preset.header", "<gray>{title}");
        c.setIfMissing("create-wizard.items.preset.entry", "<gray> {index}. <white>{text}");
        c.setIfMissing("create-wizard.items.preset.trim", "<red>(Will trim to 6 options)</red>");
        c.setIfMissing("create-wizard.items.preset.hint.preview", "<gray>Left-click: <white>Preview & Confirm</white>");
        c.setIfMissing("create-wizard.items.preset.hint.instant",
                "<gray><gold>Shift-Left-click</gold><gray>: <white>Instant Create</white>");

        c.setIfMissing("create-wizard.presets[0].title", "Yes / No");
        c.setIfMissing("create-wizard.presets[0].material", "LIME_DYE");
        c.setIfMissing("create-wizard.presets[0].options", Arrays.asList("Yes", "No"));

        c.setIfMissing("create-wizard.presets[1].title", "Yes / No / Maybe");
        c.setIfMissing("create-wizard.presets[1].material", "CYAN_DYE");
        c.setIfMissing("create-wizard.presets[1].options", Arrays.asList("Yes", "No", "Maybe"));

        c.setIfMissing("create-wizard.presets[2].title", "A / B / C / D");
        c.setIfMissing("create-wizard.presets[2].material", "LIGHT_BLUE_DYE");
        c.setIfMissing("create-wizard.presets[2].options", Arrays.asList("A", "B", "C", "D"));

        c.setIfMissing("create-wizard.presets[3].title", "1..5 Rating");
        c.setIfMissing("create-wizard.presets[3].material", "ORANGE_DYE");
        c.setIfMissing("create-wizard.presets[3].options", Arrays.asList("1", "2", "3", "4", "5"));

        c.setIfMissing("create-wizard.presets[4].title", "Agree..Disagree");
        c.setIfMissing("create-wizard.presets[4].material", "PINK_DYE");
        c.setIfMissing("create-wizard.presets[4].options",
                Arrays.asList("Strongly Agree", "Agree", "Neutral", "Disagree", "Strongly Disagree"));

        c.setIfMissing("create-wizard.presets[5].title", "Custom placeholders");
        c.setIfMissing("create-wizard.presets[5].material", "PURPLE_DYE");
        c.setIfMissing("create-wizard.presets[5].options",
                Arrays.asList("Option 1", "Option 2", "Option 3"));
    }

    // messages.yml
    private static void seedMessages(ConfigService.ConfigFile c) {
        c.setIfMissing("vote.not_found", "<red>Poll not found.</red>");
        c.setIfMissing("vote.closed", "<red>This poll is closed. You can no longer vote.</red>");
        c.setIfMissing("vote.already", "<red>You already voted for <white>[{index}] {text}</white></red>");
        c.setIfMissing("vote.success", "<green>You voted for <white>[{index}] {text}</white></green>");
        c.setIfMissing("vote.error", "<red>Could not record your vote. Try again.</red>");
        c.setIfMissing("vote.no_options", "<gray>No options</gray>");

        c.setIfMissing("create.custom.prompt",
                "<gray>Type your options (comma-separated). Example: <white>Yes, No, Maybe</white>");
        c.setIfMissing("create.custom.none", "<red>No options detected. Reopening wizard…</red>");
        c.setIfMissing("create.created", "<green>Created poll <white>#{id}</white>: <white>{question}</white></green>");

        c.setIfMissing("cmd.only_players", "<red>Only players can use this.</red>");
        c.setIfMissing("cmd.only_players_create", "<red>Only players can create polls.</red>");
        c.setIfMissing("cmd.invalid_duration", "<red>Invalid duration:</red> <white>{duration}</white>");
        c.setIfMissing("cmd.not_found", "<red>Poll #{id} not found.</red>");
        c.setIfMissing("cmd.closed_ok", "<green>Closed poll #{id}.</green>");
        c.setIfMissing("cmd.removed_ok", "<green>Removed poll #{id}.</green>");

        c.setIfMissing("cmd.results.header",
                "<aqua><b>Results for poll #{id}</b></aqua> <gray>-</gray> <white>{question}</white>");
        c.setIfMissing("cmd.results.row", "<gray>-</gray> <white>{text}</white><gray>:</gray> <green>{count}</green>");
    }
}
