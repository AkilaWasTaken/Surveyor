package network.akila.surveyor.service;

import network.akila.surveyor.model.Poll;
import network.akila.surveyor.model.PollOption;
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
        votes.upsert(pollId, player, optionIndex);
    }

    public int countVotes(long pollId, int optionIndex) {
        try {
            return votes.countVotes(pollId, optionIndex);
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