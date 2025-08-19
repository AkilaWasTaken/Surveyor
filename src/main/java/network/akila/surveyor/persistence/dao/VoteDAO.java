package network.akila.surveyor.persistence.dao;

import com.zaxxer.hikari.HikariDataSource;
import network.akila.surveyor.model.Vote;
import network.akila.surveyor.persistence.enums.DbType;

import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Handles persistence of Vote objects.
 * Responsible for saving votes and retrieving them by poll.
 */
@SuppressWarnings("unused")
public class VoteDAO {

    private final HikariDataSource ds;
    private final DbType dbType;

    public VoteDAO(HikariDataSource ds, DbType dbType) {
        this.ds = ds;
        this.dbType = dbType;
        initSchema();
    }

    private void initSchema() {
        try (Connection c = ds.getConnection(); Statement st = c.createStatement()) {
            if (dbType == DbType.SQLITE) {
                st.execute("""
                            PRAGMA foreign_keys = ON;
                        """);
                st.execute("""
                            CREATE TABLE IF NOT EXISTS votes (
                              poll_id     INTEGER NOT NULL,
                              player_uuid TEXT    NOT NULL,
                              opt_index   INTEGER NOT NULL,
                              created_at  INTEGER NOT NULL,
                              PRIMARY KEY (poll_id, player_uuid),
                              FOREIGN KEY (poll_id) REFERENCES polls(id) ON DELETE CASCADE
                            )
                        """);
            } else {
                st.execute("""
                            CREATE TABLE IF NOT EXISTS votes (
                              poll_id     BIGINT      NOT NULL,
                              player_uuid VARCHAR(36) NOT NULL,
                              opt_index   INT         NOT NULL,
                              created_at  BIGINT      NOT NULL,
                              PRIMARY KEY (poll_id, player_uuid),
                              CONSTRAINT fk_votes_poll FOREIGN KEY (poll_id) REFERENCES polls(id) ON DELETE CASCADE
                            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                        """);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to init votes schema", e);
        }
    }

    public void upsertVote(Vote vote) throws SQLException {
        String sql;
        if (dbType == DbType.SQLITE) {
            // SQLite
            sql = """
                    INSERT INTO votes(poll_id, player_uuid, opt_index, created_at)
                    VALUES (?, ?, ?, ?)
                    ON CONFLICT(poll_id, player_uuid)
                    DO UPDATE SET opt_index=excluded.opt_index, created_at=excluded.created_at
                    """;
        } else {
            // MySQL
            sql = """
                    INSERT INTO votes(poll_id, player_uuid, opt_index, created_at)
                    VALUES (?, ?, ?, ?)
                    ON DUPLICATE KEY UPDATE opt_index=VALUES(opt_index), created_at=VALUES(created_at)
                    """;
        }

        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, vote.getPollId());
            ps.setString(2, vote.getPlayerUuid().toString());
            ps.setInt(3, vote.getOptionIndex());
            ps.setLong(4, vote.getCreatedAt().toEpochMilli());
            ps.executeUpdate();
        }
    }

    public List<Vote> findByPoll(long pollId) throws SQLException {
        String sql = "SELECT player_uuid, opt_index, created_at FROM votes WHERE poll_id = ?";
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, pollId);
            try (ResultSet rs = ps.executeQuery()) {
                List<Vote> list = new ArrayList<>();
                while (rs.next()) {
                    UUID uuid = UUID.fromString(rs.getString("player_uuid"));
                    int optionIndex = rs.getInt("opt_index");
                    Instant createdAt = Instant.ofEpochMilli(rs.getLong("created_at"));
                    list.add(new Vote(pollId, uuid, optionIndex, createdAt));
                }
                return list;
            }
        }
    }

    public int countVotes(long pollId, int optionIndex) throws SQLException {
        String sql = "SELECT COUNT(*) FROM votes WHERE poll_id=? AND opt_index=?";
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, pollId);
            ps.setInt(2, optionIndex);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    public boolean hasVoted(long pollId, UUID player) throws SQLException {
        String sql = "SELECT 1 FROM votes WHERE poll_id=? AND player_uuid=? LIMIT 1";
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, pollId);
            ps.setString(2, player.toString());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public void deleteByPoll(long pollId) throws SQLException {
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement("DELETE FROM votes WHERE poll_id=?")) {
            ps.setLong(1, pollId);
            ps.executeUpdate();
        }
    }
}
