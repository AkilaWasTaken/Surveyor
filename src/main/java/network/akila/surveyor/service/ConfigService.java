package network.akila.surveyor.service;

import network.akila.surveyor.util.DurationParser;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.util.*;
import java.util.function.Consumer;

/**
 * Central config hub that manages multiple YAML files.
 */
@SuppressWarnings("unused")
public final class ConfigService {

    private final JavaPlugin plugin;
    private final DurationParser durationParser;
    private final Map<String, ConfigFile> files = new LinkedHashMap<>();

    public ConfigService(JavaPlugin plugin, DurationParser durationParser) {
        this.plugin = plugin;
        this.durationParser = durationParser;
    }

    public ConfigService register(String fileName, Consumer<ConfigFile> validatorOrNull) {
        files.put(fileName, new ConfigFile(plugin, fileName, validatorOrNull, durationParser));
        return this;
    }

    public ConfigService loadAll() {
        for (ConfigFile f : files.values()) f.reload();
        return this;
    }

    public void reloadAll() {
        for (ConfigFile f : files.values()) f.reload();
    }

    public void saveAll() {
        for (ConfigFile f : files.values()) f.save();
    }

    public ConfigFile file(String fileName) {
        ConfigFile f = files.get(fileName);
        if (f == null) throw new IllegalArgumentException("Config not registered: " + fileName);
        return f;
    }

    public static final class ConfigFile {
        private final JavaPlugin plugin;
        private final String fileName;
        private final Consumer<ConfigFile> validator;

        private final File file;
        private YamlConfiguration yaml;

        private ConfigFile(JavaPlugin plugin,
                           String fileName,
                           Consumer<ConfigFile> validator,
                           DurationParser durationParser) {
            this.plugin = plugin;
            this.fileName = fileName;
            this.validator = validator;
            this.file = new File(plugin.getDataFolder(), fileName);
        }

        public void reload() {
            if (!file.exists()) {
                if (plugin.getResource(fileName) != null) {
                    plugin.saveResource(fileName, false);
                } else {
                    File folder = plugin.getDataFolder();
                    if (!folder.exists() && !folder.mkdirs()) {
                        plugin.getLogger().warning("Could not create data folder: " + folder.getAbsolutePath());
                    }
                    try {
                        if (!file.createNewFile()) {
                            plugin.getLogger().warning("Could not create " + file.getName());
                        }
                    } catch (IOException e) {
                        throw new RuntimeException("Failed to create " + fileName, e);
                    }
                }
            }
            yaml = new YamlConfiguration();
            try {
                yaml.load(file);
            } catch (IOException | InvalidConfigurationException e) {
                throw new RuntimeException("Failed to load " + fileName, e);
            }
            if (validator != null) validator.accept(this);
        }

        public void save() {
            try {
                yaml.save(file);
            } catch (IOException e) {
                throw new RuntimeException("Failed to save " + fileName, e);
            }
        }

        public FileConfiguration raw() {
            return yaml;
        }

        public String getString(String path, String def) {
            return yaml.getString(path, def);
        }

        public boolean getBool(String path, boolean def) {
            return yaml.getBoolean(path, def);
        }

        public int getInt(String path, int def) {
            return yaml.getInt(path, def);
        }

        public long getLong(String path, long def) {
            return yaml.getLong(path, def);
        }

        public double getDouble(String path, double def) {
            return yaml.getDouble(path, def);
        }

        public List<String> getStringList(String path) {
            return yaml.getStringList(path);
        }

        public Set<String> getKeys(String path) {
            var section = yaml.getConfigurationSection(path);
            return section == null ? Collections.emptySet() : section.getKeys(false);
        }

        public <E extends Enum<E>> E getEnum(String path, Class<E> enumClass, E def) {
            String s = yaml.getString(path);
            if (s == null) return def;
            try {
                return Enum.valueOf(enumClass, s.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ex) {
                return def;
            }
        }

        public Duration getDuration(String path, String defLiteral) {
            String raw = yaml.getString(path, defLiteral);
            return DurationParser.parse(raw);
        }

        public ConfigFile set(String path, Object value) {
            yaml.set(path, value);
            return this;
        }

        public boolean isSet(String path) {
            return yaml.isSet(path);
        }

        public boolean setIfMissing(String path, Object value) {
            if (!yaml.isSet(path)) {
                yaml.set(path, value);
                return true;
            }
            return false;
        }

        public void requireOrSet(String path, Object fallback) {
            Object v = yaml.get(path);
            if (!yaml.isSet(path) || v == null || (v instanceof String && ((String) v).isBlank())) {
                yaml.set(path, fallback);
            }
        }

        public String name() {
            return fileName;
        }

        public File file() {
            return file;
        }
    }
}
