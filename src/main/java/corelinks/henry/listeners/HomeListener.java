package corelinks.henry.listeners;

import corelinks.henry.HTeleport;
import corelinks.henry.gui.ConfirmHolder;
import corelinks.henry.gui.HomeGUI;
import corelinks.henry.gui.HomeHolder;
import corelinks.henry.managers.HomeManager;
import corelinks.henry.managers.TeleportManager;
import corelinks.henry.util.ColorUtils;
import corelinks.henry.util.ConfigManager;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

public class HomeListener implements Listener {

    private final HTeleport plugin;
    private final HomeManager homeManager;
    private final TeleportManager teleportManager;
    private final ConfigManager configManager;

    public HomeListener(HTeleport plugin, HomeManager homeManager, TeleportManager teleportManager, ConfigManager configManager) {
        this.plugin = plugin;
        this.homeManager = homeManager;
        this.teleportManager = teleportManager;
        this.configManager = configManager;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        // Xử lý Menu Home
        if (event.getInventory().getHolder() instanceof HomeHolder) {
            event.setCancelled(true);
            int slot = event.getRawSlot();

            // Tìm Home ID tương ứng từ slot được click
            int homeId = HomeGUI.getHomeIdFromSlot(plugin, slot);
            if (homeId == -1) return;

            int maxHomes = homeManager.getMaxHomes(player);

            if (homeId > maxHomes) {
                player.sendMessage(ColorUtils.colorize(configManager.getMessage("home-no-permission-slot")
                        .replace("%id%", String.valueOf(homeId))));
                return;
            }

            Location homeLoc = homeManager.getHome(player.getUniqueId(), homeId);

            if (event.isLeftClick()) {
                if (homeLoc == null) {
                    // Tạo Home mới
                    homeManager.setHome(player.getUniqueId(), homeId, player.getLocation());
                    player.sendMessage(ColorUtils.colorize(configManager.getMessage("home-set-success")
                            .replace("%id%", String.valueOf(homeId))));
                    HomeGUI.openHomeMenu(plugin, player);
                } else {
                    // Dịch chuyển
                    player.closeInventory();
                    teleportManager.teleport(player, homeLoc, "home", null);
                }
            } else if (event.isRightClick()) {
                if (homeLoc != null) {
                    // Mở Menu xác nhận xóa
                    HomeGUI.openConfirmMenu(plugin, player, homeId);
                }
            }
            return;
        }

        // Xử lý Menu Xác Nhận Xóa
        if (event.getInventory().getHolder() instanceof ConfirmHolder confirmHolder) {
            event.setCancelled(true);
            int slot = event.getRawSlot();
            int homeId = confirmHolder.homeId();

            FileConfiguration guiConfig = plugin.getConfigManager().getGuiConfig();
            int confirmSlot = guiConfig.getInt("confirm-menu.confirm-button.slot", 11);
            int cancelSlot = guiConfig.getInt("confirm-menu.cancel-button.slot", 15);

            if (slot == confirmSlot) {
                // Xác nhận xóa
                homeManager.deleteHome(player.getUniqueId(), homeId);
                player.sendMessage(ColorUtils.colorize(configManager.getMessage("home-delete-success")
                        .replace("%id%", String.valueOf(homeId))));
                HomeGUI.openHomeMenu(plugin, player);
            } else if (slot == cancelSlot) {
                // Hủy bỏ
                HomeGUI.openHomeMenu(plugin, player);
            }
        }
    }
}