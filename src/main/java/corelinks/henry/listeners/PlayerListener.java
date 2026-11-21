package corelinks.henry.listeners;

import corelinks.henry.HTeleport;
import corelinks.henry.managers.DeathManager;
import corelinks.henry.managers.HomeManager;
import corelinks.henry.managers.SpawnManager;
import corelinks.henry.managers.TeleportManager;
import corelinks.henry.util.ColorUtils;
import corelinks.henry.util.ConfigManager;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

import java.util.List;

public class PlayerListener implements Listener {

    private final HTeleport plugin;
    private final SpawnManager spawnManager;
    private final HomeManager homeManager;
    private final DeathManager deathManager;
    private final TeleportManager teleportManager;
    private final ConfigManager configManager;

    public PlayerListener(HTeleport plugin, SpawnManager spawnManager, HomeManager homeManager,
                          DeathManager deathManager, TeleportManager teleportManager, ConfigManager configManager) {
        this.plugin = plugin;
        this.spawnManager = spawnManager;
        this.homeManager = homeManager;
        this.deathManager = deathManager;
        this.teleportManager = teleportManager;
        this.configManager = configManager;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // 1. Đặt người chơi tại Spawn khi tham gia máy chủ lần đầu (chậm 3 tick để tránh bị Vanilla ghi đè)
        // Entity Scheduler: chạy đúng luồng của người chơi (Folia) và tự bỏ qua nếu họ đã thoát.
        if (!player.hasPlayedBefore()) {
            Location spawnLoc = spawnManager.getSpawn();
            if (spawnLoc != null) {
                player.getScheduler().runDelayed(plugin, task -> player.teleportAsync(spawnLoc), null, 3L);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();

        // 1. Chỉ can thiệp nếu KHÔNG có Giường hoặc Neo hồi sinh
        if (!event.isBedSpawn() && !event.isAnchorSpawn()) {

            // 2. Lấy danh sách ID home kiểu int (đã sort tăng dần)
            List<Integer> homes = homeManager.getHomeIds(player.getUniqueId());

            if (!homes.isEmpty()) {
                int smallestHomeId = homes.get(0); // Phần tử đầu tiên luôn là ID nhỏ nhất
                Location homeLoc = homeManager.getHome(player.getUniqueId(), smallestHomeId);

                if (homeLoc != null) {
                    event.setRespawnLocation(homeLoc);
                    return;
                }
            }

            // 3. Nếu không có Home nào, đưa về Spawn của server
            Location spawnLoc = spawnManager.getSpawn();
            if (spawnLoc != null) {
                event.setRespawnLocation(spawnLoc);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        deathManager.setDeathLocation(player.getUniqueId(), player.getLocation());
        player.sendMessage(ColorUtils.colorize(configManager.getMessage("death-recorded")));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (!teleportManager.isPending(player.getUniqueId())) return;

        TeleportManager.PendingTeleport pending = teleportManager.getPending(player.getUniqueId());
        if (pending == null || !pending.cancelOnMove()) return;

        Location from = pending.startLocation();
        Location to = event.getTo();

        if (from.getWorld() != to.getWorld() || from.distanceSquared(to) > 0.01) {
            teleportManager.cancelPendingTeleport(player.getUniqueId(), true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player player) {
            teleportManager.handleDamageCancellation(player);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        teleportManager.cancelPendingTeleport(event.getPlayer().getUniqueId(), false);
    }
}