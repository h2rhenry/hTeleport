package corelinks.henry.managers;

import corelinks.henry.HTeleport;
import corelinks.henry.util.ColorUtils;
import corelinks.henry.util.ConfigManager;
import corelinks.henry.util.SchedulerUtil;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

public class TeleportManager {

    private final HTeleport plugin;
    private final ConfigManager configManager;

    // ConcurrentHashMap: trên Folia, người chơi ở các region khác nhau sẽ truy cập đồng thời
    private final Map<UUID, PendingTeleport> pendingTeleports = new ConcurrentHashMap<>();
    private final Map<UUID, Map<String, Long>> cooldowns = new ConcurrentHashMap<>();

    public TeleportManager(HTeleport plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
    }

    // Kiểm tra bypass Cooldown
    public boolean hasBypassCooldown(Player player, String feature) {
        if (player.isOp() || player.hasPermission("hteleport.admin") || player.hasPermission("hteleport.bypass.cooldown")) {
            return true;
        }
        return player.hasPermission("hteleport.bypass.cooldown." + feature.toLowerCase());
    }

    // Kiểm tra bypass Delay
    public boolean hasBypassDelay(Player player, String feature) {
        if (player.isOp() || player.hasPermission("hteleport.admin") || player.hasPermission("hteleport.bypass.delay")) {
            return true;
        }
        return player.hasPermission("hteleport.bypass.delay." + feature.toLowerCase());
    }

    public boolean isOnCooldown(Player player, String feature) {
        if (hasBypassCooldown(player, feature)) return false;

        Map<String, Long> playerCooldowns = cooldowns.get(player.getUniqueId());
        if (playerCooldowns == null) return false;

        Long expireTime = playerCooldowns.get(feature.toLowerCase());
        if (expireTime == null) return false;

        return System.currentTimeMillis() < expireTime;
    }

    public long getRemainingCooldown(Player player, String feature) {
        Map<String, Long> playerCooldowns = cooldowns.get(player.getUniqueId());
        if (playerCooldowns == null) return 0;

        Long expireTime = playerCooldowns.get(feature.toLowerCase());
        if (expireTime == null) return 0;

        long remaining = (expireTime - System.currentTimeMillis()) / 1000;
        return Math.max(0, remaining);
    }

    public void setCooldown(Player player, String feature) {
        // Bỏ qua không lưu cooldown nếu người chơi có quyền bypass
        if (hasBypassCooldown(player, feature)) return;

        int seconds = plugin.getConfig().getInt(feature + ".cooldown", 0);
        if (seconds <= 0) return;

        cooldowns.computeIfAbsent(player.getUniqueId(), k -> new ConcurrentHashMap<>())
                .put(feature.toLowerCase(), System.currentTimeMillis() + (seconds * 1000L));
    }

    public void teleport(Player player, Location targetLocation, String feature, Runnable onSuccess) {
        // Folia: mỗi người chơi thuộc về một region riêng, chỉ được đọc vị trí / gửi hiệu ứng / lập lịch
        // trên đúng luồng của người đó. Các trường hợp gọi từ người khác (/tpall, /tphere, /tpa...) sẽ được
        // chuyển sang luồng của người chơi cần dịch chuyển; nếu đã đúng luồng thì chạy ngay, không trễ tick.
        SchedulerUtil.runOnEntity(plugin, player, () -> startTeleport(player, targetLocation, feature, onSuccess));
    }

    private void startTeleport(Player player, Location targetLocation, String feature, Runnable onSuccess) {
        if (!player.isOnline()) return;

        UUID uuid = player.getUniqueId();

        if (isOnCooldown(player, feature)) {
            long remaining = getRemainingCooldown(player, feature);
            String msg = configManager.getMessage("cooldown").replace("%cooldown%", String.valueOf(remaining));
            player.sendMessage(ColorUtils.colorize(msg));
            return;
        }

        cancelPendingTeleport(uuid, false);

        // Lấy delay (Nếu có quyền bypass delay thì gán bằng 0)
        int delay = hasBypassDelay(player, feature) ? 0 : plugin.getConfig().getInt(feature + ".delay", 0);

        boolean cancelOnMove = plugin.getConfig().getBoolean(feature + ".cancel-on-move", true);
        boolean cancelOnDamage = plugin.getConfig().getBoolean(feature + ".cancel-on-damage", true);
        String cdSoundStr = plugin.getConfig().getString(feature + ".countdown-sound", "");
        String tpSoundStr = plugin.getConfig().getString(feature + ".teleport-sound", "");

        // Dịch chuyển tức thì nếu delay <= 0
        if (delay <= 0) {
            executeTeleport(player, targetLocation, tpSoundStr, feature, onSuccess);
            return;
        }

        Location startLoc = player.getLocation().clone();
        AtomicInteger remainingSeconds = new AtomicInteger(delay);

        // Entity Scheduler: tác vụ đếm ngược luôn chạy trên đúng luồng của người chơi (kể cả khi họ
        // di chuyển sang region khác), và tự động dừng khi người chơi thoát. Độ trễ đầu tối thiểu là 1 tick.
        ScheduledTask task = player.getScheduler().runAtFixedRate(plugin, scheduled -> {
            if (!player.isOnline()) {
                cancelPendingTeleport(uuid, false);
                return;
            }

            if (remainingSeconds.get() <= 0) {
                cancelPendingTeleport(uuid, false);
                executeTeleport(player, targetLocation, tpSoundStr, feature, onSuccess);
                return;
            }

            String actionMsg = configManager.getRawMessage("actionbar-countdown")
                    .replace("%delay%", String.valueOf(remainingSeconds.get()));
            player.sendActionBar(ColorUtils.colorize(actionMsg));

            playSound(player, cdSoundStr);

            remainingSeconds.decrementAndGet();
        }, () -> pendingTeleports.remove(uuid), 1L, 20L);

        // task == null nghĩa là người chơi đã bị gỡ khỏi thế giới -> không cần chờ đếm ngược nữa
        if (task != null) {
            pendingTeleports.put(uuid, new PendingTeleport(task, startLoc, cancelOnMove, cancelOnDamage));
        }
    }

    private void executeTeleport(Player player, Location targetLoc, String soundName, String feature, Runnable onSuccess) {
        player.teleportAsync(targetLoc).thenAccept(success -> {
            if (!Boolean.TRUE.equals(success)) return;

            // Future có thể hoàn thành ở luồng khác -> quay về luồng sở hữu người chơi trước khi phát
            // âm thanh, gửi tin nhắn và chạy callback (callback có thể đụng tới vị trí/thế giới của người chơi)
            SchedulerUtil.runOnEntity(plugin, player, () -> {
                playSound(player, soundName);
                player.sendMessage(ColorUtils.colorize(configManager.getMessage("teleport-success")));
                setCooldown(player, feature);
                if (onSuccess != null) {
                    onSuccess.run();
                }
            });
        }).exceptionally(ex -> {
            plugin.getLogger().log(Level.WARNING, "Không thể dịch chuyển người chơi " + player.getName(), ex);
            return null;
        });
    }

    public boolean isPending(UUID uuid) {
        return pendingTeleports.containsKey(uuid);
    }

    public PendingTeleport getPending(UUID uuid) {
        return pendingTeleports.get(uuid);
    }

    public void cancelPendingTeleport(UUID uuid, boolean notify) {
        PendingTeleport pending = pendingTeleports.remove(uuid);
        if (pending != null) {
            pending.task().cancel();
            if (notify) {
                Player p = Bukkit.getPlayer(uuid);
                if (p != null) {
                    p.sendMessage(ColorUtils.colorize(configManager.getMessage("teleport-cancelled-move")));
                }
            }
        }
    }

    public void handleDamageCancellation(Player player) {
        UUID uuid = player.getUniqueId();
        PendingTeleport pending = pendingTeleports.get(uuid);
        if (pending != null && pending.cancelOnDamage() && pendingTeleports.remove(uuid, pending)) {
            pending.task().cancel();
            player.sendMessage(ColorUtils.colorize(configManager.getMessage("teleport-cancelled-damage")));
        }
    }

    private void playSound(Player player, String soundName) {
        if (soundName == null || soundName.isBlank() || soundName.equalsIgnoreCase("none")) return;
        try {
            Sound sound = Sound.valueOf(soundName.toUpperCase());
            player.playSound(player.getLocation(), sound, 1.0f, 1.0f);
        } catch (IllegalArgumentException ignored) {}
    }

    public record PendingTeleport(ScheduledTask task, Location startLocation, boolean cancelOnMove, boolean cancelOnDamage) {}
}
