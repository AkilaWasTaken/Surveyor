package network.akila.surveyor.command;

import net.kyori.adventure.text.Component;
import network.akila.surveyor.Surveyor;
import network.akila.surveyor.gui.ActivePollsView;
import network.akila.surveyor.gui.CreatePollWizard;
import network.akila.surveyor.model.PollOption;
import network.akila.surveyor.service.ConfigService;
import network.akila.surveyor.service.PollService;
import network.akila.surveyor.util.DurationParser;
import network.akila.surveyor.util.Utils;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.incendo.cloud.bukkit.CloudBukkitCapabilities;
import org.incendo.cloud.execution.ExecutionCoordinator;
import org.incendo.cloud.paper.LegacyPaperCommandManager;
import org.incendo.cloud.parser.standard.LongParser;
import org.incendo.cloud.parser.standard.StringParser;
import org.incendo.cloud.suggestion.Suggestion;
import org.incendo.cloud.suggestion.SuggestionProvider;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * All poll commands.
 */
public final class PollCommands {

    private final PollService service;
    private final LegacyPaperCommandManager<CommandSender> manager;

    private final String msgOnlyPlayers;
    private final String msgOnlyPlayersCreate;
    private final String msgInvalidDuration;
    private final String msgNotFound;
    private final String msgClosedOk;
    private final String msgRemovedOk;
    private final String msgResultsHeader;
    private final String msgResultsRow;

    private static final List<String> DURATION_SAMPLES =
            List.of("15m", "30m", "45m", "1h", "2h", "6h", "12h", "1d", "2d");

    private static final SuggestionProvider<CommandSender> DURATION_PROVIDER =
            (ctx, input) -> CompletableFuture.completedFuture(
                    DURATION_SAMPLES.stream()
                            .map(Suggestion::suggestion)
                            .toList()
            );


    public PollCommands(final Surveyor plugin, final PollService service) {
        this.service = Objects.requireNonNull(service);
        ConfigService.ConfigFile messages = Surveyor.getInstance().messages();

        // messages.yml
        this.msgOnlyPlayers = messages.getString("cmd.only_players", "<red>Only players can use this.</red>");
        this.msgOnlyPlayersCreate = messages.getString("cmd.only_players_create", "<red>Only players can create polls.</red>");
        this.msgInvalidDuration = messages.getString("cmd.invalid_duration", "<red>Invalid duration:</red> <white>{duration}</white>");
        this.msgNotFound = messages.getString("cmd.not_found", "<red>Poll #{id} not found.</red>");
        this.msgClosedOk = messages.getString("cmd.closed_ok", "<green>Closed poll #{id}.</green>");
        this.msgRemovedOk = messages.getString("cmd.removed_ok", "<green>Removed poll #{id}.</green>");
        this.msgResultsHeader = messages.getString("cmd.results.header",
                "<aqua><b>Results for poll #{id}</b></aqua> <gray>-</gray> <white>{question}</white>");
        this.msgResultsRow = messages.getString("cmd.results.row",
                "<gray>-</gray> <white>{text}</white><gray>:</gray> <green>{count}</green>");

        this.manager = LegacyPaperCommandManager.createNative(
                plugin,
                ExecutionCoordinator.simpleCoordinator()
        );

        if (manager.hasCapability(CloudBukkitCapabilities.NATIVE_BRIGADIER)) {
            manager.registerBrigadier();
        } else if (manager.hasCapability(CloudBukkitCapabilities.ASYNCHRONOUS_COMPLETION)) {
            manager.registerAsynchronousCompletions();
        }

        registerRoot("poll");
        registerRoot("polls");
    }

    private void registerRoot(String root) {
        SuggestionProvider<CommandSender> pollIdProvider =
                (ctx, input) -> CompletableFuture.completedFuture(
                        service.findAll().stream()
                                .map(p -> Suggestion.suggestion(String.valueOf(p.getId())))
                                .collect(Collectors.toList())
                );

        // /poll
        manager.command(
                manager.commandBuilder(root)
                        .handler(ctx -> {
                            CommandSender sender = ctx.sender();
                            if (sender instanceof Player player) {
                                new ActivePollsView(service).open(player);
                            } else {
                                sendMini(sender, msgOnlyPlayers);
                            }
                        })
        );

        // /poll help
        manager.command(
                manager.commandBuilder(root)
                        .literal("help")
                        .handler(ctx -> sendHelp(ctx.sender(), root))
        );

        // /poll create <duration> <question...>
        manager.command(
                manager.commandBuilder(root)
                        .literal("create")
                        .required("duration", StringParser.stringParser(), DURATION_PROVIDER)
                        .required("question", StringParser.greedyStringParser())
                        .handler(ctx -> {
                            CommandSender sender = ctx.sender();
                            if (!(sender instanceof Player player)) {
                                sendMini(sender, msgOnlyPlayersCreate);
                                return;
                            }

                            final String durationLiteral = ctx.get("duration");
                            final Duration duration;
                            try {
                                duration = DurationParser.parse(durationLiteral);
                                if (duration.isZero() || duration.isNegative()) throw new IllegalArgumentException();
                            } catch (IllegalArgumentException ex) {
                                sendMini(sender, msgInvalidDuration.replace("{duration}", durationLiteral));
                                sendInlineCreateUsage(sender, root);
                                return;
                            }

                            final String question = ctx.get("question");
                            final Instant closesAt = Instant.now().plus(duration);

                            new CreatePollWizard(
                                    service,
                                    closesAt,
                                    question,
                                    poll -> new ActivePollsView(service).open(player)
                            ).open(player);
                        })
        );

        // /poll close <pollId>
        manager.command(
                manager.commandBuilder(root)
                        .literal("close")
                        .required("pollId", LongParser.longParser(), pollIdProvider)
                        .handler(ctx -> {
                            final long id = ctx.get("pollId");
                            final boolean exists = service.findAll().stream().anyMatch(p -> p.getId() == id);
                            if (!exists) {
                                sendMini(ctx.sender(), msgNotFound.replace("{id}", String.valueOf(id)));
                                return;
                            }
                            service.close(id);
                            sendMini(ctx.sender(), msgClosedOk.replace("{id}", String.valueOf(id)));
                        })
        );

        // /poll remove <pollId>
        manager.command(
                manager.commandBuilder(root)
                        .literal("remove")
                        .required("pollId", LongParser.longParser(), pollIdProvider)
                        .handler(ctx -> {
                            final long id = ctx.get("pollId");
                            final boolean exists = service.findAll().stream().anyMatch(p -> p.getId() == id);
                            if (!exists) {
                                sendMini(ctx.sender(), msgNotFound.replace("{id}", String.valueOf(id)));
                                return;
                            }
                            service.remove(id);
                            sendMini(ctx.sender(), msgRemovedOk.replace("{id}", String.valueOf(id)));
                        })
        );

        // /poll results <pollId>
        manager.command(
                manager.commandBuilder(root)
                        .literal("results")
                        .required("pollId", LongParser.longParser(), pollIdProvider)
                        .handler(ctx -> {
                            final long id = ctx.get("pollId");
                            service.find(id).ifPresentOrElse(poll -> {
                                sendMini(ctx.sender(), msgResultsHeader
                                        .replace("{id}", String.valueOf(id))
                                        .replace("{question}", poll.getQuestion()));

                                final List<PollOption> opts = service.options(id);
                                for (int i = 0; i < opts.size(); i++) {
                                    final int count = service.countVotes(id, i);
                                    sendMini(ctx.sender(), msgResultsRow
                                            .replace("{text}", opts.get(i).getText())
                                            .replace("{count}", String.valueOf(count)));
                                }
                            }, () -> sendMini(ctx.sender(), msgNotFound.replace("{id}", String.valueOf(id))));
                        })
        );
    }

    // Help
    private void sendHelp(CommandSender sender, String root) {
        final String durations = String.join(", ", DURATION_SAMPLES);

        sendBlank(sender);
        send(sender, "<gradient:#00e1ff:#0066ff><b>❖ Surveyor Polls ❖</b></gradient>");
        send(sender, "<gray>Hover commands for usage • Click to prefill</gray>");
        send(sender, "<dark_gray>────────────────────────────────────</dark_gray>");

        // /<root>
        helpEntry(sender,
                "/" + root,
                "Open the active polls GUI",
                "/" + root,
                "Open the polls interface."
        );

        // /<root> create <duration> <question...>
        helpEntry(sender,
                "/" + root + " create",
                "Start a new poll",
                "/" + root + " create <duration> <question...>",
                "<white>Examples:</white><newline>  <yellow>/" + root + " create 30m</yellow> <white>Your question here</white>"
                        + "<newline>  <yellow>/" + root + " create 1h</yellow> <white>What should we build next?</white>"
                        + "<newline><gray>Durations:</gray> " + durations
        );

        // /<root> close <pollId>
        helpEntry(sender,
                "/" + root + " close",
                "Close a poll",
                "/" + root + " close <pollId>",
                "Closes an active poll by its id."
        );

        // /<root> remove <pollId>
        helpEntry(sender,
                "/" + root + " remove",
                "Remove a poll",
                "/" + root + " remove <pollId>",
                "Permanently removes a poll."
        );

        // /<root> results <pollId>
        helpEntry(sender,
                "/" + root + " results",
                "View results",
                "/" + root + " results <pollId>",
                "Shows vote counts for each option."
        );

        send(sender, "<dark_gray>────────────────────────────────────</dark_gray>");
        send(sender, "<gray>Tip:</gray> <yellow>Use short durations like 30m, 1h, 2d</yellow>");
        sendBlank(sender);
    }

    private void helpEntry(CommandSender sender, String commandLabel, String summary, String usage, String extraHover) {
        final String mm =
                "<green><hover:show_text:'<white>Usage:</white> " + escapeHover(usage) +
                        (extraHover == null || extraHover.isEmpty() ? "" : "<newline>" + escapeHover(extraHover)) +
                        "'><click:suggest_command:" + escapeClick(commandLabel) + ">" +
                        commandLabel + "</click></hover></green> <dark_gray>-</dark_gray> <white>" + summary + "</white>";
        send(sender, mm);
    }

    private String escapeHover(String s) {
        return s.replace('\'', '’');
    }

    private String escapeClick(String s) {
        return s + (s.endsWith(" ") ? "" : " ");
    }

    private void sendInlineCreateUsage(CommandSender sender, String root) {
        final String durations = String.join(", ", DURATION_SAMPLES);
        sendBlank(sender);
        send(sender, "<aqua><b>Usage</b></aqua> <gray>→</gray> <white>/" + root + " create <duration> <question...></white>");
        send(sender, "<gray>Examples:</gray> <yellow>/" + root + " create 30m</yellow> <white>Your question here</white>");
        send(sender, "           <yellow>/" + root + " create 1h</yellow> <white>What should we build next?</white>");
        send(sender, "<gray>Durations:</gray> <white>" + durations + "</white>");
        sendBlank(sender);
    }

    // Utils
    private void sendMini(CommandSender sender, String mini) {
        send(sender, mini);
    }

    private void send(CommandSender sender, String mini) {
        final Component c = Utils.parse(sender instanceof Player p ? p : null, mini);
        sender.sendMessage(c);
    }

    private void sendBlank(CommandSender sender) {
        sender.sendMessage(Component.text(" "));
    }
}
