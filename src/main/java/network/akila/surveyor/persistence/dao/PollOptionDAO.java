package network.akila.surveyor.persistence.dao;

import com.zaxxer.hikari.HikariDataSource;
import network.akila.surveyor.model.PollOption;
import network.akila.surveyor.persistence.enums.DbType;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Handles persistence of PollOption objects.
 * Responsible for saving and loading the answer choices for each poll.
 */
@SuppressWarnings("unused")
public class PollOptionDAO {

    private final HikariDataSource ds;
    private final DbType dbType;

    public PollOptionDAO(HikariDataSource ds, DbType dbType) {
        this.ds = ds;
        this.dbType = dbType;
        initSchema();
    }

    private void initSchema() {
        try (Connection c = ds.getConnection(); Statement st = c.createStatement()) {
            if (dbType == DbType.SQLITE) {
                st.execute("""
                            CREATE TABLE IF NOT EXISTS poll_options (
                              poll_id INTEGER NOT NULL,
                              opt_index INTEGER NOT NULL,
                              text TEXT NOT NULL,
                              PRIMARY KEY (poll_id, opt_index),
                              FOREIGN KEY (poll_id) REFERENCES polls(id) ON DELETE CASCADE
                            )
                        """);
            } else {
                st.execute("""
                            CREATE TABLE IF NOT EXISTS poll_options (
                              poll_id BIGINT NOT NULL,
                              opt_index INT NOT NULL,
                              text TEXT NOT NULL,
                              PRIMARY KEY (poll_id, opt_index),
                              CONSTRAINT fk_poll_options_poll FOREIGN KEY (poll_id) REFERENCES polls(id) ON DELETE CASCADE
                            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                        """);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to init poll_options schema", e);
        }
    }

    public void insertOptions(long pollId, List<String> options) throws SQLException {
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement("INSERT INTO poll_options(poll_id, opt_index, text) VALUES (?, ?, ?)")) {
            for (int i = 0; i < options.size(); i++) {
                ps.setLong(1, pollId);
                ps.setInt(2, i);
                ps.setString(3, options.get(i));
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    public List<PollOption> findByPollId(long pollId) throws SQLException {
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT opt_index, text FROM poll_options WHERE poll_id = ? ORDER BY opt_index")) {
            ps.setLong(1, pollId);
            try (ResultSet rs = ps.executeQuery()) {
                List<PollOption> opts = new ArrayList<>();
                while (rs.next()) {
                    opts.add(new PollOption(rs.getInt("opt_index"), rs.getString("text")));
                }
                return opts;
            }
        }
    }

}