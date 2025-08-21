package network.akila.surveyor.persistence;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import network.akila.surveyor.persistence.enums.DbType;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Provides a database connection pool using HikariCP.
 * Supports SQLite and MySQL.
 */
@SuppressWarnings("unused")
public class DatabaseProvider {

    private final DbType dbType;
    private final HikariDataSource ds;
    private final ExecutorService executor;

    public DatabaseProvider(DbType dbType, HikariDataSource ds) {
        this.dbType = dbType;
        this.ds = ds;
        this.executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    }

    public static DatabaseProvider forSqlite(Path dataFolder, String fileName) {
        HikariConfig cfg = new HikariConfig();
        cfg.setJdbcUrl("jdbc:sqlite:" + dataFolder.resolve(fileName).toAbsolutePath());
        cfg.setMaximumPoolSize(10);
        cfg.setConnectionTestQuery("SELECT 1");
        cfg.setPoolName("Surveyor");
        return new DatabaseProvider(DbType.SQLITE, new HikariDataSource(cfg));
    }

    public static DatabaseProvider forMysql(
            String host, int port, String db,
            String user, String pass, String params
    ) {
        HikariConfig cfg = new HikariConfig();
        cfg.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + db + (params != null ? "?" + params : ""));
        cfg.setUsername(user);
        cfg.setPassword(pass);
        cfg.setMaximumPoolSize(10);
        cfg.setConnectionTestQuery("SELECT 1");
        cfg.setPoolName("Surveyor");
        cfg.addDataSourceProperty("cachePrepStmts", "true");
        cfg.addDataSourceProperty("prepStmtCacheSize", "250");
        cfg.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        return new DatabaseProvider(DbType.MYSQL, new HikariDataSource(cfg));
    }

    public DbType getDbType() {
        return dbType;
    }

    public Connection getConnection() throws SQLException {
        return ds.getConnection();
    }

    public HikariDataSource getDataSource() {
        return ds;
    }

    public ExecutorService getExecutor() {
        return executor;
    }

    public void close() {
        if (ds != null && !ds.isClosed()) {
            ds.close();
        }
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
        }
    }
}
