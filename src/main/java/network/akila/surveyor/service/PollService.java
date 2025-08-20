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

    public Poll create(String question, Instant closesAt, List<String> optionTexts) {
        return polls.createPoll(question, closesAt, optionTexts);
    }

    public Optional<Poll> find(long id) {
        return polls.findById(id);
    }

    public List<Poll> findAll() {
        return polls.findAll();
    }

    public void close(long id) {
        polls.setManuallyClosed(id, true);
    }

    public void remove(long id) {
        polls.delete(id);
    }

    public void vote(long pollId, UUID player, int optionIndex) {
        Poll p = polls.findById(pollId).orElseThrow(() -> new IllegalArgumentException("Poll not found: " + pollId));
        boolean closed = p.isManuallyClosed() || (p.getClosesAt() != null && !Instant.now().isBefore(p.getClosesAt()));
        if (closed) throw new IllegalStateException("Poll is closed");
        if (hasVoted(pollId, player)) throw new IllegalStateException("You have already voted");
        votes.upsert(pollId, player, optionIndex);
    }

    public boolean hasVoted(long pollId, UUID player) {
        try {
            return votes.hasVoted(pollId, player);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public Optional<Integer> getVote(long pollId, UUID player) {
        try {
            return votes.findByPoll(pollId).stream()
                    .filter(v -> v.getPlayerUuid().equals(player))
                    .map(Vote::getOptionIndex)
                    .findFirst();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public boolean isClosed(long pollId) {
        Poll p = polls.findById(pollId).orElse(null);
        if (p == null) return true;
        if (p.isManuallyClosed()) return true;
        return p.getClosesAt() != null && !Instant.now().isBefore(p.getClosesAt());
    }

    public int countVotes(long pollId, int optionIndex) {
        try {
            return votes.countVotes(pollId, optionIndex);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public int[] optionCounts(long pollId) {
        try {
            List<PollOption> opts = options.findByPollId(pollId);
            int[] counts = new int[opts.size()];
            for (int i = 0; i < opts.size(); i++) counts[i] = votes.countVotes(pollId, i);
            return counts;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public List<PollOption> options(long pollId) {
        try {
            return options.findByPollId(pollId);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}