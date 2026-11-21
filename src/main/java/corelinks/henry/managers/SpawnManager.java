// src/main/java/corelinks/henry/managers/SpawnManager.java
package corelinks.henry.managers;

import corelinks.henry.HTeleport;
import corelinks.henry.util.ConfigManager;
import corelinks.henry.util.YamlStore;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

public class SpawnManager {

    private final HTeleport plugin;
    private final ConfigManager configManager;
    private final YamlStore store;
    // volatile: được đọc từ nhiều luồng region của Folia
    private volatile Location spawnLocation;

    public SpawnManager(HTeleport plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.store = new YamlStore(plugin, "spawn.yml");
        loadSpawn();
    }

    public void setSpawn(Location location) {
        Location stored = location.clone();
        this.spawnLocation = stored;

        store.update(config -> {
            config.set("spawn.world", stored.getWorld().getName());
            config.set("spawn.x", stored.getX());
            config.set("spawn.y", stored.getY());
            config.set("spawn.z", stored.getZ());
            config.set("spawn.yaw", stored.getYaw());
            config.set("spawn.pitch", stored.getPitch());
        });
    }

    public Location getSpawn() {
        Location loc = spawnLocation;
        return loc != null ? loc.clone() : null;
    }

    public boolean hasSpawn() {
        return spawnLocation != null;
    }

    /** Ghi ngay dữ liệu chưa lưu ra đĩa (gọi khi tắt plugin). */
    public void flush() {
        store.flush();
    }

    private void loadSpawn() {
        store.read(config -> {
            if (!config.contains("spawn.world")) return;

            String worldName = config.getString("spawn.world");
            World world = Bukkit.getWorld(worldName);
            if (world == null) return;

            double x = config.getDouble("spawn.x");
            double y = config.getDouble("spawn.y");
            double z = config.getDouble("spawn.z");
            float yaw = (float) config.getDouble("spawn.yaw");
            float pitch = (float) config.getDouble("spawn.pitch");

            this.spawnLocation = new Location(world, x, y, z, yaw, pitch);
        });
    }
}
