package network.akila.surveyor.model;

import java.util.Objects;

/**
 * A single answer choice in a poll.
 */
@SuppressWarnings("unused")
public class PollOption {
    private final int index;
    private String text;
    private int votes;

    public PollOption(int index, String text) {
        if (index < 0 || index > 5) throw new IllegalArgumentException("index must be between 0 and 5");
        this.index = index;
        this.text = Objects.requireNonNull(text, "text");
    }

    // getters
    public int getIndex() {
        return index;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = Objects.requireNonNull(text, "text");
    }

    public int getVotes() {
        return votes;
    }

    public void setVotes(int votes) {
        this.votes = Math.max(0, votes);
    }

    public void incrementVotes() {
        this.votes++;
    }

    @Override
    public String toString() {
        return "PollOption{index=" + index + ", text='" + text + "', votes=" + votes + '}';
    }
}