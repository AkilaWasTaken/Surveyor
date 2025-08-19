package network.akila.surveyor.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * A poll with up to 6 options.
 */
@SuppressWarnings("unused")
public class Poll {
    private Long id;
    private final String question;
    private final Instant createdAt;
    private final Instant closesAt;
    private boolean manuallyClosed;
    private final List<PollOption> options = new ArrayList<>(6);

    public Poll(Long id, String question, Instant createdAt, Instant closesAt, boolean manuallyClosed, List<PollOption> options) {
        this.id = id;
        this.question = Objects.requireNonNull(question, "question");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
        this.closesAt = Objects.requireNonNull(closesAt, "closesAt");
        this.manuallyClosed = manuallyClosed;
        if (options != null) {
            if (options.size() > 6) throw new IllegalArgumentException("Max 6 options allowed");
            this.options.addAll(options);
        }
    }

    public Poll(String question, Instant createdAt, Instant closesAt) {
        this(null, question, createdAt, closesAt, false, null);
    }

    // getters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getQuestion() {
        return question;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getClosesAt() {
        return closesAt;
    }

    public void setManuallyClosed(boolean manuallyClosed) {
        this.manuallyClosed = manuallyClosed;
    }

    public boolean isManuallyClosed() {
        return manuallyClosed;
    }

    public List<PollOption> getOptions() {
        return Collections.unmodifiableList(options);
    }

    public void addOption(PollOption option) {
        if (options.size() >= 6) throw new IllegalStateException("Cannot add more than 6 options");
        this.options.add(Objects.requireNonNull(option, "option"));
    }

    public boolean isActive(Instant now) {
        return !isClosed(now);
    }

    public boolean isClosed(Instant now) {
        return manuallyClosed || now.isAfter(closesAt);
    }

    @Override
    public String toString() {
        return "Poll{" +
                "id=" + id +
                ", question='" + question + '\'' +
                ", createdAt=" + createdAt +
                ", closesAt=" + closesAt +
                ", manuallyClosed=" + manuallyClosed +
                ", options=" + options +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Poll poll)) return false;
        return id != null && id.equals(poll.id);
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : System.identityHashCode(this);
    }
}
