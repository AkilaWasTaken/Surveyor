package network.akila.surveyor.persistence.dao;

import network.akila.surveyor.model.Poll;
import network.akila.surveyor.model.PollOption;
import network.akila.surveyor.persistence.DatabaseProvider;
import network.akila.surveyor.persistence.enums.DbType;

import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

/**
 * Handles persistence of Poll objects.
 * Responsible for creating polls, saving them, and loading them from the database.
 */
@SuppressWarnings("unused")
public class PollDAO {

    private final DatabaseProvider dbProvider;
    private final DbType dbType;
    private final ExecutorService executor;

    public PollDAO(DatabaseProvider dbProvider) {
        this.dbProvider = dbProvider;
        this.dbType = dbProvider.getDbType();
        this.executor = dbProvider.getExecutor();
        initSchema();
    }

    private void initSchema() {
        try (Connection c = dbProvider.getConnection(); Statement st = c.createStatement()) {
            if (dbType == DbType.SQLITE) {
                st.execute("""
                            CREATE TABLE IF NOT EXISTS polls (
                              id INTEGER PRIMARY KEY AUTOINCREMENT,
                              question TEXT NOT NULL,
                              created_at INTEGER NOT NULL,
                              closes_at INTEGER NOT NULL,
                              manually_closed INTEGER NOT NULL DEFAULT 0
                            )
                        """);
            } else {
                st.execute("""
                            CREATE TABLE IF NOT EXISTS polls (
                              id BIGINT NOT NULL AUTO_INCREMENT,
                              question TEXT NOT NULL,
                              created_at BIGINT NOT NULL,
                              closes_at BIGINT NOT NULL,
                              manually_closed TINYINT(1) NOT NULL DEFAULT 0,
                              PRIMARY KEY (id)
                            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                        """);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to init polls schema", e);
        }
    }

    public CompletableFuture<Poll> createPoll(String question, Instant closesAt, List<String> options) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection c = dbProvider.getConnection()) {
                long id;
                Instant now = Instant.now();

                try (PreparedStatement ps = c.prepareStatement(
                        "INSERT INTO polls(question, created_at, closes_at, manually_closed) VALUES (?, ?, ?, ?)",
                        Statement.RETURN_GENERATED_KEYS
                )) {
                    ps.setString(1, question);
                    ps.setLong(2, now.toEpochMilli());
                    ps.setLong(3, closesAt.toEpochMilli());
                    ps.setBoolean(4, false);
                    ps.executeUpdate();

                    try (ResultSet rs = ps.getGeneratedKeys()) {
                        if (!rs.next()) throw new SQLException("No ID generated for poll");
                        id = rs.getLong(1);
                    }
                }

                PollOptionDAO optionDAO = new PollOptionDAO(dbProvider);
                optionDAO.insertOptions(id, options).join();

                List<PollOption> opts = new ArrayList<>();
                for (int i = 0; i < options.size(); i++) {
                    opts.add(new PollOption(i, options.get(i)));
                }

                return new Poll(id, question, now, closesAt, false, opts);
            } catch (SQLException e) {
                throw new RuntimeException("Failed to create poll", e);
            }
        }, executor);
    }

    public CompletableFuture<Optional<Poll>> findById(long id) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection c = dbProvider.getConnection()) {
                Poll poll = null;
                try (PreparedStatement ps = c.prepareStatement("SELECT * FROM polls WHERE id = ?")) {
                    ps.setLong(1, id);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            poll = new Poll(
                                    rs.getLong("id"),
                                    rs.getString("question"),
                                    Instant.ofEpochMilli(rs.getLong("created_at")),
                                    Instant.ofEpochMilli(rs.getLong("closes_at")),
                                    rs.getBoolean("manually_closed"),
                                    new ArrayList<>()
                            );
                        }
                    }
                }
                if (poll == null) return Optional.empty();

                PollOptionDAO optionDAO = new PollOptionDAO(dbProvider);
                List<PollOption> options = optionDAO.findByPollId(id).join();
                for (PollOption opt : options) {
                    poll.addOption(opt);
                }

                return Optional.of(poll);
            } catch (SQLException e) {
                throw new RuntimeException("Failed to load poll " + id, e);
            }
        }, executor);
    }

    public CompletableFuture<List<Poll>> findAll() {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection c = dbProvider.getConnection();
                 PreparedStatement ps = c.prepareStatement("SELECT id FROM polls");
                 ResultSet rs = ps.executeQuery()) {

                List<Poll> list = new ArrayList<>();
                while (rs.next()) {
                    findById(rs.getLong("id")).join().ifPresent(list::add);
                }
                return list;
            } catch (SQLException e) {
                throw new RuntimeException("Failed to list polls", e);
            }
        }, executor);
    }

    public CompletableFuture<Void> setManuallyClosed(long id, boolean closed) {
        return CompletableFuture.runAsync(() -> {
            String sql = "UPDATE polls SET manually_closed=? WHERE id=?";
            try (Connection c = dbProvider.getConnection();
                 PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setBoolean(1, closed);
                ps.setLong(2, id);
                ps.executeUpdate();
            } catch (SQLException e) {
                throw new RuntimeException("Failed to update manually_closed for poll " + id, e);
            }
        }, executor);
    }

    public CompletableFuture<Void> delete(long id) {
        return CompletableFuture.runAsync(() -> {
            String sql = "DELETE FROM polls WHERE id=?";
            try (Connection c = dbProvider.getConnection();
                 PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setLong(1, id);
                ps.executeUpdate();
            } catch (SQLException e) {
                throw new RuntimeException("Failed to delete poll " + id, e);
            }
        }, executor);
    }
}
