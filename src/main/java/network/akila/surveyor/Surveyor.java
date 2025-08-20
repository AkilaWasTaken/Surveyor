package network.akila.surveyor;

import com.zaxxer.hikari.HikariDataSource;
import fr.mrmicky.fastinv.FastInvManager;
import network.akila.surveyor.command.PollCommands;
import network.akila.surveyor.config.ConfigBootstrap;
import network.akila.surveyor.listener.ChatOnceListener;
import network.akila.surveyor.persistence.DatabaseProvider;
import network.akila.surveyor.persistence.dao.PollDAO;
import network.akila.surveyor.persistence.dao.PollOptionDAO;
import network.akila.surveyor.persistence.dao.VoteDAO;
import network.akila.surveyor.persistence.enums.DbType;
import network.akila.surveyor.service.ConfigService;
import network.akila.surveyor.service.ConfigService.ConfigFile;
import network.akila.surveyor.service.PollPlaceholders;
import network.akila.surveyor.service.PollService;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Logger;

@SuppressWarnings("unused")
public final class Surveyor extends JavaPlugin {

    private static Surveyor instance;
    private DatabaseProvider databaseProvider;
    private ConfigService configService;
    private PollService pollService;

    public static Surveyor getInstance() {
        return instance;
    }

    @Override
    public void onEnable() {
        FastInvManager.register(this);
        instance = this;
        Logger log = getLogger();

        this.configService = ConfigBootstrap
                .registerAll(new ConfigService(this))
                .loadAll();
        this.configService.saveAll();

        try {
            initDatabase();
        } catch (Exception e) {
            getLogger().severe("Failed to initialize database: " + e.getMessage());
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        if (databaseProvider != null) {
            databaseProvider.close();
        }
    }

    private void initDatabase() {
        ConfigFile cfg = configService.file("config.yml");
        final String typeStr = cfg.getString("database.type", "SQLITE").toUpperCase();
        final DbType dbType = DbType.valueOf(typeStr);

        switch (dbType) {
            case SQLITE: {
                String fileName = cfg.getString("database.sqlite.file", "polls.db");
                this.databaseProvider = DatabaseProvider.forSqlite(getDataFolder().toPath(), fileName);
                break;
            }
            case MYSQL: {
                String host = cfg.getString("database.mysql.host", "localhost");
                int port = cfg.getInt("database.mysql.port", 3306);
                String database = cfg.getString("database.mysql.database", "surveyor");
                String user = cfg.getString("database.mysql.user", "root");
                String pass = cfg.getString("database.mysql.password", "");
                String params = cfg.getString("database.mysql.params", "useSSL=true&useUnicode=true&characterEncoding=utf8");
                this.databaseProvider = DatabaseProvider.forMysql(host, port, database, user, pass, params);
                break;
            }
            default:
                throw new IllegalStateException("Unsupported DbType: " + dbType);
        }

        HikariDataSource dataSource = databaseProvider.getDataSource();
        if (dataSource == null) {
            throw new IllegalStateException("DataSource was not initialized");
        }

        DbType type = databaseProvider.getDbType();
        PollDAO pollDAO = new PollDAO(dataSource, type);
        PollOptionDAO pollOptionDAO = new PollOptionDAO(dataSource, type);
        VoteDAO voteDAO = new VoteDAO(dataSource, type);

        pollService = new PollService(pollDAO, pollOptionDAO, voteDAO);

        ChatOnceListener.init(this);
        new PollCommands(this, pollService);

        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new PollPlaceholders(pollService).register();
            getLogger().info("Surveyor poll placeholders registered with PlaceholderAPI.");
        } else {
            getLogger().warning("PlaceholderAPI not found! Poll placeholders will be disabled.");
        }
    }

    public PollService getPollService() {
        return pollService;
    }

    public ConfigService.ConfigFile menus() {
        return configService.file(ConfigBootstrap.MENUS_YML);
    }

    public ConfigService.ConfigFile messages() {
        return configService.file(ConfigBootstrap.MESSAGES_YML);
    }

    public ConfigService.ConfigFile config() {
        return configService.file(ConfigBootstrap.CONFIG_YML);
    }
}
