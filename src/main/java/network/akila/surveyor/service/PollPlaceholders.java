package network.akila.surveyor.service;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import network.akila.surveyor.model.Poll;
import network.akila.surveyor.model.PollOption;
import network.akila.surveyor.util.DurationParser;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * PlaceholderAPI expansion.
 * <p>
 * General:
 * %surveyor_active_polls%
 * <p>
 * Per poll (numeric id):
 * %surveyor_poll_question_<id>%
 * %surveyor_poll_status_<id>%           → "active" | "closed"
 * %surveyor_poll_votes_<id>%            → total votes across all options
 * %surveyor_poll_closes_in_<id>%        → (e.g. 1d2h30m) or "ended"/"none"
 * <p>
 * Options (0-based index):
 * %surveyor_poll_option_<id>_<index>%
 * %surveyor_poll_option_votes_<id>_<index>%
 * <p>
 * Player-specific:
 * %surveyor_has_voted_<id>%             → "yes"/"no"
 * %surveyor_my_vote_index_<id>%         → -1 if none
 * %surveyor_my_vote_text_<id>%          → "none" if none
 */
public class PollPlaceholders extends PlaceholderExpansion {

    private final PollService pollService;

    private final Map<String, String> cache = new ConcurrentHashMap<>();
    private volatile long lastRefreshMs = 0L;
    private static final long CACHE_TTL_MS = 1_000L;

    public PollPlaceholders(PollService pollService) {
        this.pollService = pollService;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "surveyor";
    }

    @Override
    public @NotNull String getAuthor() {
        return "Akila";
    }

    @Override
    public @NotNull String getVersion() {
        return "1.0.0";
    }

    @Override
    public String onPlaceholderRequest(Player player, @NotNull String identifier) {
        long now = System.currentTimeMillis();
        if (now - lastRefreshMs > CACHE_TTL_MS) {
            cache.clear();
            lastRefreshMs = now;
        }
        return cache.computeIfAbsent(identifier, key -> compute(player, key));
    }

    private String compute(Player player, String idf) {
        if ("active_polls".equals(idf)) {
            int active = (int) pollService.findAll().stream()
                    .filter(p -> !pollService.isClosed(p.getId()))
                    .count();
            return String.valueOf(active);
        }

        if (idf.startsWith("poll_question_")) {
            Long id = parseLongTail(idf, "poll_question_");
            if (id == null) return "N/A";
            Optional<Poll> p = pollService.find(id);
            return p.map(Poll::getQuestion).orElse("N/A");
        }

        if (idf.startsWith("poll_status_")) {
            Long id = parseLongTail(idf, "poll_status_");
            if (id == null) return "N/A";
            return pollService.isClosed(id) ? "closed" : "active";
        }

        if (idf.startsWith("poll_votes_")) {
            Long id = parseLongTail(idf, "poll_votes_");
            if (id == null) return "0";
            int total = 0;
            for (int c : pollService.optionCounts(id)) total += c;
            return String.valueOf(total);
        }

        if (idf.startsWith("poll_closes_in_")) {
            Long id = parseLongTail(idf, "poll_closes_in_");
            if (id == null) return "N/A";
            Optional<Poll> p = pollService.find(id);
            if (p.isEmpty()) return "N/A";
            Instant closesAt = p.get().getClosesAt();
            if (closesAt == null) return "none";
            Duration d = Duration.between(Instant.now(), closesAt);
            if (d.isNegative() || d.isZero()) return "ended";
            return DurationParser.format(d);
        }

        if (idf.startsWith("poll_option_")) {
            String[] parts = idf.split("_");
            if (parts.length == 4) {
                Long id = parseLong(parts[2]);
                Integer idx = parseInt(parts[3]);
                if (id == null || idx == null || idx < 0) return "N/A";
                List<PollOption> opts = pollService.options(id);
                return (idx < opts.size()) ? opts.get(idx).getText() : "N/A";
            }
        }

        if (idf.startsWith("poll_option_votes_")) {
            String[] parts = idf.split("_");
            if (parts.length == 5) {
                Long id = parseLong(parts[3]);
                Integer idx = parseInt(parts[4]);
                if (id == null || idx == null || idx < 0) return "0";
                return String.valueOf(pollService.countVotes(id, idx));
            }
        }

        if (idf.startsWith("has_voted_")) {
            if (player == null) return "no";
            Long id = parseLongTail(idf, "has_voted_");
            if (id == null) return "no";
            return pollService.hasVoted(id, player.getUniqueId()) ? "yes" : "no";
        }

        if (idf.startsWith("my_vote_index_")) {
            if (player == null) return "-1";
            Long id = parseLongTail(idf, "my_vote_index_");
            if (id == null) return "-1";
            return String.valueOf(pollService.getVote(id, player.getUniqueId()).orElse(-1));
        }

        if (idf.startsWith("my_vote_text_")) {
            if (player == null) return "none";
            Long id = parseLongTail(idf, "my_vote_text_");
            if (id == null) return "none";
            Optional<Integer> idxOpt = pollService.getVote(id, player.getUniqueId());
            if (idxOpt.isEmpty() || idxOpt.get() < 0) return "none";
            int idx = idxOpt.get();
            List<PollOption> opts = pollService.options(id);
            return (idx < opts.size()) ? opts.get(idx).getText() : "none";
        }

        return null;
    }

    // Helpers

    private Long parseLongTail(String full, String prefix) {
        try {
            return Long.parseLong(full.substring(prefix.length()));
        } catch (Exception e) {
            return null;
        }
    }

    private Long parseLong(String s) {
        try {
            return Long.parseLong(s);
        } catch (Exception e) {
            return null;
        }
    }

    private Integer parseInt(String s) {
        try {
            return Integer.parseInt(s);
        } catch (Exception e) {
            return null;
        }
    }
}
