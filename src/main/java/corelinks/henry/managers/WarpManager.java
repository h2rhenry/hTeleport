// src/main/java/corelinks/henry/managers/WarpManager.java
package corelinks.henry.managers;

import corelinks.henry.HTeleport;
import corelinks.henry.models.Warp;
import corelinks.henry.util.ConfigManager;
import corelinks.henry.util.YamlStore;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class WarpManager {

    private final HTeleport plugin;
    private final ConfigManager configManager;
    private final YamlStore store;
    private final Map<String, Warp> warps = new ConcurrentHashMap<>();

    public WarpManager(HTeleport plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.store = new YamlStore(plugin, "warps.yml");
        loadWarps();
    }

    public void createWarp(String name, Location location, String permission) {
        Warp warp = new Warp(name.toLowerCase(), location, permission);
        warps.put(name.toLowerCase(), warp);
        saveWarpToConfig(warp);
    }

    public boolean deleteWarp(String name) {
        String lower = name.toLowerCase();
        if (warps.remove(lower) == null) return false;

        store.update(config -> config.set("warps." + lower, null));
        return true;
    }

    public boolean setPermission(String name, String permission) {
        Warp warp = getWarp(name);
        if (warp == null) return false;
        warp.setPermission(permission);
        saveWarpToConfig(warp);
        return true;
    }

    public boolean removePermission(String name) {
        Warp warp = getWarp(name);
        if (warp == null) return false;
        warp.setPermission(null);
        saveWarpToConfig(warp);
        return true;
    }

    public Warp getWarp(String name) {
        return warps.get(name.toLowerCase());
    }

    public Collection<Warp> getAllWarps() {
        return Collections.unmodifiableCollection(warps.values());
    }

    /** Ghi ngay dữ liệu chưa lưu ra đĩa (gọi khi tắt plugin). */
    public void flush() {
        store.flush();
    }

    private void saveWarpToConfig(Warp warp) {
        String path = "warps." + warp.getName() + ".";
        Location loc = warp.getLocation();
        String permission = warp.getPermission();

        store.update(config -> {
            config.set(path + "world", loc.getWorld().getName());
            config.set(path + "x", loc.getX());
            config.set(path + "y", loc.getY());
            config.set(path + "z", loc.getZ());
            config.set(path + "yaw", loc.getYaw());
            config.set(path + "pitch", loc.getPitch());
            config.set(path + "permission", permission);
        });
    }

    private void loadWarps() {
        warps.clear();
        store.read(config -> {
            ConfigurationSection section = config.getConfigurationSection("warps");
            if (section == null) return;

            for (String key : section.getKeys(false)) {
                String path = "warps." + key + ".";
                String worldName = config.getString(path + "world");
                World world = Bukkit.getWorld(worldName);
                if (world == null) continue;

                double x = config.getDouble(path + "x");
                double y = config.getDouble(path + "y");
                double z = config.getDouble(path + "z");
                float yaw = (float) config.getDouble(path + "yaw");
                float pitch = (float) config.getDouble(path + "pitch");
                String perm = config.getString(path + "permission", null);

                Location loc = new Location(world, x, y, z, yaw, pitch);
                warps.put(key.toLowerCase(), new Warp(key, loc, perm));
            }
        });
    }
}
