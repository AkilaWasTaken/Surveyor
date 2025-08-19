package network.akila.surveyor.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Records one player's vote on a poll option.
 */
@SuppressWarnings("unused")
public class Vote {
    private Long id;
    private final long pollId;
    private final UUID playerUuid;
    private final int optionIndex;
    private final Instant createdAt;

    public Vote(Long id, long pollId, UUID playerUuid, int optionIndex, Instant createdAt) {
        if (optionIndex < 0 || optionIndex > 5)
            throw new IllegalArgumentException("optionIndex must be between 0 and 5");
        this.id = id;
        this.pollId = pollId;
        this.playerUuid = Objects.requireNonNull(playerUuid, "playerUuid");
        this.optionIndex = optionIndex;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
    }

    public Vote(long pollId, UUID playerUuid, int optionIndex, Instant createdAt) {
        this(null, pollId, playerUuid, optionIndex, createdAt);
    }

    //getters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public long getPollId() {
        return pollId;
    }

    public UUID getPlayerUuid() {
        return playerUuid;
    }

    public int getOptionIndex() {
        return optionIndex;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    @Override
    public String toString() {
        return "Vote{id=" + id +
                ", pollId=" + pollId +
                ", playerUuid=" + playerUuid +
                ", optionIndex=" + optionIndex +
                ", createdAt=" + createdAt + '}';
    }
}