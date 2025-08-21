package network.akila.surveyor.service;

import network.akila.surveyor.model.Poll;
import network.akila.surveyor.model.PollOption;
import network.akila.surveyor.model.Vote;
import network.akila.surveyor.persistence.dao.PollDAO;
import network.akila.surveyor.persistence.dao.PollOptionDAO;
import network.akila.surveyor.persistence.dao.VoteDAO;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Operations for polls.
 * Uses DAOs to create polls, record votes, and query results.
 */
@SuppressWarnings("unused")
public class PollService {
    private final PollDAO polls;
    private final PollOptionDAO options;
    private final VoteDAO votes;

    public PollService(PollDAO polls, PollOptionDAO options, VoteDAO votes) {
        this.polls = polls;
        this.options = options;
        this.votes = votes;
    }

    public CompletableFuture<Poll> create(String question, Instant closesAt, List<String> optionTexts) {
        return polls.createPoll(question, closesAt, optionTexts);
    }

    public CompletableFuture<Optional<Poll>> find(long id) {
        return polls.findById(id);
    }

    public CompletableFuture<List<Poll>> findAll() {
        return polls.findAll();
    }

    public CompletableFuture<Void> close(long id) {
        return polls.setManuallyClosed(id, true);
    }

    public CompletableFuture<Void> remove(long id) {
        return polls.delete(id);
    }

    public CompletableFuture<Void> vote(long pollId, UUID player, int optionIndex) {
        return polls.findById(pollId).thenCompose(optPoll -> {
            Poll p = optPoll.orElseThrow(() -> new IllegalArgumentException("Poll not found: " + pollId));

            boolean closed = p.isManuallyClosed() ||
                    (p.getClosesAt() != null && !Instant.now().isBefore(p.getClosesAt()));
            if (closed) throw new IllegalStateException("Poll is closed");

            return hasVoted(pollId, player).thenCompose(already -> {
                if (already) throw new IllegalStateException("You have already voted");
                return votes.upsert(pollId, player, optionIndex);
            });
        });
    }

    public CompletableFuture<Boolean> hasVoted(long pollId, UUID player) {
        return votes.hasVoted(pollId, player);
    }

    public CompletableFuture<Optional<Integer>> getVote(long pollId, UUID player) {
        return votes.findByPoll(pollId)
                .thenApply(list -> list.stream()
                        .filter(v -> v.getPlayerUuid().equals(player))
                        .map(Vote::getOptionIndex)
                        .findFirst());
    }

    public CompletableFuture<Boolean> isClosed(long pollId) {
        return polls.findById(pollId).thenApply(optPoll -> {
            Poll p = optPoll.orElse(null);
            if (p == null) return true;
            if (p.isManuallyClosed()) return true;
            return p.getClosesAt() != null && !Instant.now().isBefore(p.getClosesAt());
        });
    }

    public CompletableFuture<Integer> countVotes(long pollId, int optionIndex) {
        return votes.countVotes(pollId, optionIndex);
    }

    public CompletableFuture<int[]> optionCounts(long pollId) {
        return options.findByPollId(pollId).thenCompose(opts -> {
            CompletableFuture<?>[] futures = new CompletableFuture[opts.size()];
            int[] counts = new int[opts.size()];

            for (int i = 0; i < opts.size(); i++) {
                final int idx = i;
                futures[i] = votes.countVotes(pollId, idx)
                        .thenAccept(count -> counts[idx] = count);
            }

            return CompletableFuture.allOf(futures).thenApply(v -> counts);
        });
    }

    public CompletableFuture<List<PollOption>> options(long pollId) {
        return options.findByPollId(pollId);
    }
}
