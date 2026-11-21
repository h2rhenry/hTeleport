// src/main/java/corelinks/henry/managers/DeathManager.java
package corelinks.henry.managers;

import corelinks.henry.HTeleport;
import corelinks.henry.util.ConfigManager;
import corelinks.henry.util.YamlStore;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class DeathManager {

    private final HTeleport plugin;
    private final ConfigManager configManager;
    private final YamlStore store;
    private final Map<UUID, Location> deathLocations = new ConcurrentHashMap<>();

    public DeathManager(HTeleport plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.store = new YamlStore(plugin, "deaths.yml");
        loadDeaths();
    }

    public void setDeathLocation(UUID playerUuid, Location location) {
        Location stored = location.clone();
        deathLocations.put(playerUuid, stored);

        String path = "deaths." + playerUuid + ".";
        store.update(config -> {
            config.set(path + "world", stored.getWorld().getName());
            config.set(path + "x", stored.getX());
            config.set(path + "y", stored.getY());
            config.set(path + "z", stored.getZ());
            config.set(path + "yaw", stored.getYaw());
            config.set(path + "pitch", stored.getPitch());
        });
    }

    public Location getDeathLocation(UUID playerUuid) {
        Location loc = deathLocations.get(playerUuid);
        return loc != null ? loc.clone() : null;
    }

    public void clearDeathLocation(UUID playerUuid) {
        deathLocations.remove(playerUuid);
        store.update(config -> config.set("deaths." + playerUuid, null));
    }

    /** Ghi ngay dữ liệu chưa lưu ra đĩa (gọi khi tắt plugin). */
    public void flush() {
        store.flush();
    }

    private void loadDeaths() {
        store.read(config -> {
            ConfigurationSection section = config.getConfigurationSection("deaths");
            if (section == null) return;

            for (String uuidStr : section.getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(uuidStr);
                    String path = "deaths." + uuidStr + ".";
                    String worldName = config.getString(path + "world");
                    World world = Bukkit.getWorld(worldName);
                    if (world == null) continue;

                    double x = config.getDouble(path + "x");
                    double y = config.getDouble(path + "y");
                    double z = config.getDouble(path + "z");
                    float yaw = (float) config.getDouble(path + "yaw");
                    float pitch = (float) config.getDouble(path + "pitch");

                    deathLocations.put(uuid, new Location(world, x, y, z, yaw, pitch));
                } catch (IllegalArgumentException ignored) {}
            }
        });
    }
}
