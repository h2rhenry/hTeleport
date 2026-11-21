package corelinks.henry.managers;

import corelinks.henry.HTeleport;
import corelinks.henry.util.ConfigManager;
import corelinks.henry.util.YamlStore;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class HomeManager {

    // Giá trị dự phòng khi config.yml cũ chưa có mục "home.max-homes"
    private static final int DEFAULT_MAX_HOMES = 21;

    private final HTeleport plugin;
    private final ConfigManager configManager;
    private final YamlStore store;

    // ConcurrentHashMap: Folia có nhiều luồng region cùng đọc/ghi dữ liệu Home
    private final Map<UUID, Map<Integer, Location>> playerHomes = new ConcurrentHashMap<>();

    public HomeManager(HTeleport plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.store = new YamlStore(plugin, "homes.yml");
        loadHomes();
    }

    /**
     * Số Home TỐI ĐA tuyệt đối (home.max-homes trong config.yml).
     * Đây là "trần" áp dụng cho tất cả mọi người, kể cả admin.
     */
    public int getGlobalMax() {
        return Math.max(1, plugin.getConfig().getInt("home.max-homes", DEFAULT_MAX_HOMES));
    }

    /**
     * Số Home người chơi này được phép sở hữu. Lấy mức CAO NHẤT trong các nguồn sau:
     *  1. home.default-limit (mặc định cho người không có permission nào)
     *  2. Các nhóm trong home.permission-limits (permission tuỳ chỉnh trong config.yml)
     *  3. hTeleport.home.<số> (cách cũ, vẫn được hỗ trợ để không hỏng server đang chạy)
     * Admin (hTeleport.admin) luôn được mức tối đa. Kết quả không bao giờ vượt quá home.max-homes.
     */
    public int getMaxHomes(Player player) {
        int globalMax = getGlobalMax();

        if (player.hasPermission("hTeleport.admin")) {
            return globalMax;
        }

        int limit = Math.max(0, plugin.getConfig().getInt("home.default-limit", 1));

        // 1. Các nhóm permission tuỳ chỉnh
        ConfigurationSection groups = plugin.getConfig().getConfigurationSection("home.permission-limits");
        if (groups != null) {
            for (String key : groups.getKeys(false)) {
                String permission = groups.getString(key + ".permission");
                if (permission == null || permission.isBlank()) continue;

                int groupLimit = groups.getInt(key + ".limit", 0);
                if (groupLimit > limit && player.hasPermission(permission.trim())) {
                    limit = groupLimit;
                }
            }
        }

        // 2. Tương thích ngược: hTeleport.home.<số> (chỉ cần kiểm tra các số lớn hơn mức hiện có)
        for (int i = globalMax; i > limit; i--) {
            if (player.hasPermission("hTeleport.home." + i)) {
                limit = i;
                break;
            }
        }

        return Math.min(limit, globalMax);
    }

    public Location getHome(UUID uuid, int homeId) {
        Map<Integer, Location> homes = playerHomes.get(uuid);
        if (homes == null) return null;
        Location loc = homes.get(homeId);
        return loc != null ? loc.clone() : null;
    }

    /**
     * Lấy danh sách toàn bộ ID Home (kiểu int) của người chơi từ RAM,
     * tự động sắp xếp theo thứ tự tăng dần (1, 2, 3...)
     */
    public List<Integer> getHomeIds(UUID uuid) {
        Map<Integer, Location> homes = playerHomes.get(uuid);
        if (homes == null || homes.isEmpty()) {
            return Collections.emptyList();
        }
        List<Integer> ids = new ArrayList<>(homes.keySet());
        Collections.sort(ids);
        return ids;
    }

    /**
     * Lấy bản sao toàn bộ Map Home của người chơi (sắp xếp theo ID)
     */
    public Map<Integer, Location> getHomes(UUID uuid) {
        Map<Integer, Location> homes = playerHomes.get(uuid);
        if (homes == null || homes.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Integer, Location> copy = new TreeMap<>();
        homes.forEach((id, loc) -> copy.put(id, loc.clone()));
        return copy;
    }

    public void setHome(UUID uuid, int homeId, Location location) {
        Location stored = location.clone();
        playerHomes.computeIfAbsent(uuid, k -> new ConcurrentHashMap<>()).put(homeId, stored);

        String path = "homes." + uuid + "." + homeId + ".";
        store.update(config -> {
            config.set(path + "world", stored.getWorld().getName());
            config.set(path + "x", stored.getX());
            config.set(path + "y", stored.getY());
            config.set(path + "z", stored.getZ());
            config.set(path + "yaw", stored.getYaw());
            config.set(path + "pitch", stored.getPitch());
        });
    }

    public void deleteHome(UUID uuid, int homeId) {
        Map<Integer, Location> homes = playerHomes.get(uuid);
        if (homes != null) {
            homes.remove(homeId);
        }

        store.update(config -> config.set("homes." + uuid + "." + homeId, null));
    }

    /** Ghi ngay dữ liệu chưa lưu ra đĩa (gọi khi tắt plugin). */
    public void flush() {
        store.flush();
    }

    private void loadHomes() {
        store.read(config -> {
            ConfigurationSection section = config.getConfigurationSection("homes");
            if (section == null) return;

            for (String uuidStr : section.getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(uuidStr);
                    ConfigurationSection homeSection = config.getConfigurationSection("homes." + uuidStr);
                    if (homeSection == null) continue;

                    Map<Integer, Location> homes = new ConcurrentHashMap<>();
                    for (String homeIdStr : homeSection.getKeys(false)) {
                        int homeId = Integer.parseInt(homeIdStr);
                        String path = "homes." + uuidStr + "." + homeIdStr + ".";

                        String worldName = config.getString(path + "world");
                        World world = Bukkit.getWorld(worldName);
                        if (world == null) continue;

                        double x = config.getDouble(path + "x");
                        double y = config.getDouble(path + "y");
                        double z = config.getDouble(path + "z");
                        float yaw = (float) config.getDouble(path + "yaw");
                        float pitch = (float) config.getDouble(path + "pitch");

                        homes.put(homeId, new Location(world, x, y, z, yaw, pitch));
                    }
                    playerHomes.put(uuid, homes);
                } catch (Exception ignored) {}
            }
        });
    }
}
