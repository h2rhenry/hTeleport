package corelinks.henry.managers;

import corelinks.henry.HTeleport;
import corelinks.henry.util.ColorUtils;
import corelinks.henry.util.ConfigManager;
import corelinks.henry.util.SchedulerUtil;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

public class RtpManager {

    private final HTeleport plugin;
    private final ConfigManager configManager;
    private final TeleportManager teleportManager;
    private final Random random = new Random();

    // Danh sách mặc định, chỉ dùng khi config.yml cũ chưa có mục "rtp.blacklist-blocks"
    private static final List<String> DEFAULT_BLACKLIST = List.of(
            "LAVA", "WATER", "MAGMA_BLOCK", "CACTUS", "CAMPFIRE", "SOUL_CAMPFIRE",
            "FIRE", "SOUL_FIRE", "VOID_AIR", "BEDROCK", "WITHER_ROSE", "POWDER_SNOW"
    );

    public RtpManager(HTeleport plugin, ConfigManager configManager, TeleportManager teleportManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.teleportManager = teleportManager;
    }

    // Đọc danh sách block cấm từ config.yml (rtp.blacklist-blocks), được gọi lại mỗi lần RTP
    // nên khi admin sửa config và /htlp reload thì áp dụng ngay, không cần khởi động lại plugin.
    private Set<Material> loadBlacklist() {
        List<String> names = plugin.getConfig().getStringList("rtp.blacklist-blocks");
        if (names.isEmpty()) {
            names = DEFAULT_BLACKLIST;
        }

        Set<Material> result = new HashSet<>();
        for (String name : names) {
            Material material = Material.matchMaterial(name.trim());
            if (material != null) {
                result.add(material);
            } else {
                plugin.getLogger().warning("[RTP] Không nhận diện được block '" + name
                        + "' trong rtp.blacklist-blocks, đã bỏ qua mục này.");
            }
        }
        return result;
    }

    // RTP ngay trong thế giới hiện tại của người chơi (hành vi mặc định, dùng khi gõ /rtp không kèm tham số)
    public void executeRtp(Player player) {
        executeRtp(player, player.getWorld());
    }

    // RTP tới một thế giới chỉ định, phục vụ lệnh /rtp <world>
    public void executeRtp(Player player, World targetWorld) {
        if (teleportManager.isOnCooldown(player, "rtp")) {
            long remaining = teleportManager.getRemainingCooldown(player, "rtp");
            String msg = configManager.getMessage("cooldown").replace("%cooldown%", String.valueOf(remaining));
            player.sendMessage(ColorUtils.colorize(msg));
            return;
        }

        String worldPath = "rtp.worlds." + targetWorld.getName();

        if (!plugin.getConfig().contains(worldPath)) {
            player.sendMessage(ColorUtils.colorize(configManager.getMessage("rtp-not-allowed-world")));
            return;
        }

        player.sendMessage(ColorUtils.colorize(configManager.getMessage("rtp-searching")));

        int minRadius = plugin.getConfig().getInt(worldPath + ".min-radius", 100);
        int maxRadius = plugin.getConfig().getInt(worldPath + ".max-radius", 5000);
        int centerX = plugin.getConfig().getInt(worldPath + ".center-x", 0);
        int centerZ = plugin.getConfig().getInt(worldPath + ".center-z", 0);
        int maxAttempts = plugin.getConfig().getInt("rtp.max-attempts", 20);
        Set<Material> blacklist = loadBlacklist();

        // Giới hạn độ cao quét dọc theo cột (tùy chọn, mặc định dùng luôn giới hạn của thế giới).
        // "max-y" nếu đặt sẽ ép điểm bắt đầu quét không vượt quá giá trị này (hữu ích để
        // bỏ qua trần bedrock của Nether cho nhanh), "min-y" là điểm dừng quét phía dưới.
        int worldMinHeight = targetWorld.getMinHeight();
        int worldMaxHeight = targetWorld.getMaxHeight();
        int configuredMinY = plugin.getConfig().getInt(worldPath + ".min-y", worldMinHeight);
        int minY = Math.max(configuredMinY, worldMinHeight);
        Integer maxYCap = plugin.getConfig().contains(worldPath + ".max-y")
                ? plugin.getConfig().getInt(worldPath + ".max-y")
                : null;

        // Bắt đầu chuỗi tìm kiếm bất đồng bộ (Async)
        findSafeLocationAsync(targetWorld, minRadius, maxRadius, centerX, centerZ, maxAttempts, 0, blacklist, minY, maxYCap, worldMaxHeight)
                .thenAccept(safeLoc -> SchedulerUtil.runOnEntity(plugin, player, () -> {
                    if (safeLoc != null) {
                        // Chuyển lại về luồng đang sở hữu người chơi (luồng chính trên Paper, luồng region trên Folia)
                        // và sử dụng TeleportManager để đếm ngược
                        // (teleportAsync của Paper tự xử lý việc dịch chuyển liên thế giới)
                        teleportManager.teleport(player, safeLoc, "rtp", () -> playSuccessEffect(player));
                    } else {
                        player.sendMessage(ColorUtils.colorize(configManager.getMessage("rtp-not-found")
                                .replace("%attempts%", String.valueOf(maxAttempts))));
                    }
                }));
    }

    // Lấy danh sách tên các thế giới đã cấu hình cho RTP (dùng cho tab-complete)
    public Set<String> getConfiguredWorldNames() {
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("rtp.worlds");
        if (section == null) return Collections.emptySet();
        return section.getKeys(false);
    }

    private CompletableFuture<Location> findSafeLocationAsync(World world, int min, int max, int cx, int cz,
                                                                int maxAttempts, int currentAttempt, Set<Material> blacklist,
                                                                int minY, Integer maxYCap, int worldMaxHeight) {
        if (currentAttempt >= maxAttempts) {
            return CompletableFuture.completedFuture(null);
        }

        CompletableFuture<Location> future = new CompletableFuture<>();

        int x = cx + generateRandomCoordinate(min, max);
        int z = cz + generateRandomCoordinate(min, max);
        int chunkX = x >> 4;
        int chunkZ = z >> 4;

        // Sử dụng Paper API để load Chunk Async an toàn, không giật server
        world.getChunkAtAsync(chunkX, chunkZ).thenAccept(chunk ->
                // Folia: chỉ được đọc block trên luồng của region đang sở hữu chunk này.
                // (Nếu callback đã chạy đúng luồng đó thì chạy ngay, không tốn thêm tick)
                SchedulerUtil.runAtChunk(plugin, world, chunkX, chunkZ, () -> {
                    try {
                        Location safeLoc = scanColumnForSafeSpot(world, x, z, minY, maxYCap, worldMaxHeight, blacklist);

                        if (safeLoc != null) {
                            future.complete(safeLoc);
                        } else {
                            // Đệ quy thử lại tọa độ (x, z) khác nếu cả cột không tìm được vị trí an toàn
                            findSafeLocationAsync(world, min, max, cx, cz, maxAttempts, currentAttempt + 1, blacklist, minY, maxYCap, worldMaxHeight)
                                    .thenAccept(future::complete);
                        }
                    } catch (Throwable t) {
                        plugin.getLogger().log(Level.WARNING, "[RTP] Lỗi khi quét vị trí an toàn tại " + x + ", " + z, t);
                        future.complete(null);
                    }
                })
        ).exceptionally(ex -> {
            // Không load được chunk -> báo "không tìm thấy" thay vì để người chơi chờ mãi
            plugin.getLogger().log(Level.WARNING, "[RTP] Không thể load chunk " + chunkX + ", " + chunkZ, ex);
            future.complete(null);
            return null;
        });

        return future;
    }

    // Quét dọc từ trên xuống dưới tại 1 cột (x, z) để tìm khoảng trống an toàn đầu tiên.
    // Khác với cách cũ (chỉ lấy đúng block cao nhất), cách này xuyên qua được các block bị
    // blacklist (ví dụ trần bedrock ở Nether) để tìm đúng hang/khoảng trống thật bên dưới.
    private Location scanColumnForSafeSpot(World world, int x, int z, int minY, Integer maxYCap, int worldMaxHeight, Set<Material> blacklist) {
        int highestY = world.getHighestBlockYAt(x, z);
        int startY = (maxYCap != null) ? Math.min(highestY, maxYCap) : highestY;
        startY = Math.min(startY, worldMaxHeight - 3); // Chừa chỗ cho 2 block khí phía trên

        for (int y = startY; y >= minY; y--) {
            Block ground = world.getBlockAt(x, y, z);
            Block air1 = ground.getRelative(0, 1, 0);
            Block air2 = ground.getRelative(0, 2, 0);

            if (isSafe(ground, air1, air2, blacklist)) {
                return new Location(world, x + 0.5, y + 1, z + 0.5);
            }
        }
        return null;
    }

    private int generateRandomCoordinate(int min, int max) {
        int range = max - min;
        int result = random.nextInt(range + 1) + min;
        return random.nextBoolean() ? result : -result;
    }

    private boolean isSafe(Block ground, Block air1, Block air2, Set<Material> blacklist) {
        if (!ground.getType().isSolid()) return false;
        if (blacklist.contains(ground.getType())) return false;
        if (!air1.getType().isAir() || !air2.getType().isAir()) return false;
        return true;
    }

    // Phát âm thanh + hạt (particle) tại vị trí người chơi khi RTP thành công, cấu hình tại
    // "rtp.success-effect" trong config.yml. Được gọi qua callback onSuccess của TeleportManager
    // nên chỉ chạy đúng 1 lần, ngay sau khi người chơi đã dịch chuyển xong.
    private void playSuccessEffect(Player player) {
        String path = "rtp.success-effect";

        if (!plugin.getConfig().getBoolean(path + ".enabled", true)) return;

        Location loc = player.getLocation();

        String soundName = plugin.getConfig().getString(path + ".sound", "");
        if (soundName != null && !soundName.isBlank() && !soundName.equalsIgnoreCase("none")) {
            try {
                Sound sound = Sound.valueOf(soundName.trim().toUpperCase());
                float volume = (float) plugin.getConfig().getDouble(path + ".volume", 1.0);
                float pitch = (float) plugin.getConfig().getDouble(path + ".pitch", 1.0);
                player.playSound(loc, sound, volume, pitch);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("[RTP] Âm thanh '" + soundName + "' trong rtp.success-effect.sound không hợp lệ.");
            }
        }

        String particleName = plugin.getConfig().getString(path + ".particle", "");
        if (particleName != null && !particleName.isBlank() && !particleName.equalsIgnoreCase("none")) {
            try {
                Particle particle = Particle.valueOf(particleName.trim().toUpperCase());
                int count = plugin.getConfig().getInt(path + ".particle-count", 50);
                double offsetX = plugin.getConfig().getDouble(path + ".particle-offset-x", 0.5);
                double offsetY = plugin.getConfig().getDouble(path + ".particle-offset-y", 1.0);
                double offsetZ = plugin.getConfig().getDouble(path + ".particle-offset-z", 0.5);
                double speed = plugin.getConfig().getDouble(path + ".particle-speed", 0.05);

                player.getWorld().spawnParticle(particle, loc.clone().add(0, 1, 0), count, offsetX, offsetY, offsetZ, speed);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("[RTP] Particle '" + particleName + "' trong rtp.success-effect.particle không hợp lệ.");
            }
        }
    }
}
