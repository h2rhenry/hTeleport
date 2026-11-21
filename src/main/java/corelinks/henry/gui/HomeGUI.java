package corelinks.henry.gui;

import corelinks.henry.HTeleport;
import corelinks.henry.util.ColorUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class HomeGUI {

    public static void openHomeMenu(HTeleport plugin, Player player) {
        FileConfiguration guiConfig = plugin.getConfigManager().getGuiConfig();

        int rows = Math.min(6, Math.max(1, guiConfig.getInt("home-menu.rows", 4)));
        int globalMax = plugin.getHomeManager().getGlobalMax();
        int maxHomes = plugin.getHomeManager().getMaxHomes(player);
        int currentHomes = plugin.getHomeManager().getHomeIds(player.getUniqueId()).size();

        String title = guiConfig.getString("home-menu.title", "&8DANH SÁCH HOME")
                .replace("%count%", String.valueOf(currentHomes))
                .replace("%max%", String.valueOf(maxHomes));

        Inventory inv = Bukkit.createInventory(new HomeHolder(), rows * 9, ColorUtils.colorize(title));

        // 1. Tạo các Item lót trang trí (Fillers)
        renderFillers(guiConfig, inv, "home-menu.fillers");

        // 2. Render các slot Home
        List<Integer> homeSlots = guiConfig.getIntegerList("home-menu.home-slots");
        for (int i = 0; i < homeSlots.size(); i++) {
            int slot = homeSlots.get(i);
            int homeId = i + 1;

            // Không hiển thị các ô vượt quá số Home tối đa (home.max-homes) vì chúng không bao giờ mở khóa được
            if (homeId > globalMax) break;
            if (slot >= inv.getSize()) continue;

            Location homeLoc = plugin.getHomeManager().getHome(player.getUniqueId(), homeId);

            if (homeId > maxHomes) {
                // Locked Slot
                inv.setItem(slot, buildHomeItem(guiConfig, "home-menu.items.home-locked", homeId, homeLoc));
            } else if (homeLoc == null) {
                // Empty Slot
                inv.setItem(slot, buildHomeItem(guiConfig, "home-menu.items.home-empty", homeId, null));
            } else {
                // Set Slot
                inv.setItem(slot, buildHomeItem(guiConfig, "home-menu.items.home-set", homeId, homeLoc));
            }
        }

        player.openInventory(inv);
    }

    public static void openConfirmMenu(HTeleport plugin, Player player, int homeId) {
        FileConfiguration guiConfig = plugin.getConfigManager().getGuiConfig();

        int rows = Math.min(6, Math.max(1, guiConfig.getInt("confirm-menu.rows", 3)));
        String title = guiConfig.getString("confirm-menu.title", "&8XÁC NHẬN XÓA HOME")
                .replace("%id%", String.valueOf(homeId));

        Inventory inv = Bukkit.createInventory(new ConfirmHolder(homeId), rows * 9, ColorUtils.colorize(title));

        // 1. Render Fillers
        renderFillers(guiConfig, inv, "confirm-menu.fillers");

        // 2. Render nút Xác nhận
        int confirmSlot = guiConfig.getInt("confirm-menu.confirm-button.slot", 11);
        if (confirmSlot < inv.getSize()) {
            inv.setItem(confirmSlot, buildSimpleItem(guiConfig, "confirm-menu.confirm-button", homeId));
        }

        // 3. Render nút Hủy
        int cancelSlot = guiConfig.getInt("confirm-menu.cancel-button.slot", 15);
        if (cancelSlot < inv.getSize()) {
            inv.setItem(cancelSlot, buildSimpleItem(guiConfig, "confirm-menu.cancel-button", homeId));
        }

        player.openInventory(inv);
    }

    // --- Helper Methods ---

    private static void renderFillers(FileConfiguration config, Inventory inv, String path) {
        ConfigurationSection section = config.getConfigurationSection(path);
        if (section == null) return;

        for (String key : section.getKeys(false)) {
            String matName = section.getString(key + ".material", "GRAY_STAINED_GLASS_PANE");
            String name = section.getString(key + ".name", " ");
            List<Integer> slots = section.getIntegerList(key + ".slots");

            Material mat = Material.matchMaterial(matName);
            if (mat == null) mat = Material.GRAY_STAINED_GLASS_PANE;

            ItemStack item = new ItemStack(mat);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.displayName(ColorUtils.colorize(name));
                item.setItemMeta(meta);
            }

            for (int slot : slots) {
                if (slot < inv.getSize()) {
                    inv.setItem(slot, item);
                }
            }
        }
    }

    private static ItemStack buildHomeItem(FileConfiguration config, String path, int homeId, Location loc) {
        String matName = config.getString(path + ".material", "PAPER");
        Material mat = Material.matchMaterial(matName);
        if (mat == null) mat = Material.PAPER;

        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        String name = config.getString(path + ".name", "")
                .replace("%id%", String.valueOf(homeId));
        meta.displayName(ColorUtils.colorize(name));

        List<String> rawLore = config.getStringList(path + ".lore");
        List<Component> formattedLore = new ArrayList<>();

        for (String line : rawLore) {
            String formatted = line.replace("%id%", String.valueOf(homeId));
            if (loc != null) {
                formatted = formatted.replace("%world%", loc.getWorld().getName())
                        .replace("%x%", String.valueOf((int) loc.getX()))
                        .replace("%y%", String.valueOf((int) loc.getY()))
                        .replace("%z%", String.valueOf((int) loc.getZ()));
            }
            formattedLore.add(ColorUtils.colorize(formatted));
        }

        meta.lore(formattedLore);
        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack buildSimpleItem(FileConfiguration config, String path, int homeId) {
        String matName = config.getString(path + ".material", "STONE");
        Material mat = Material.matchMaterial(matName);
        if (mat == null) mat = Material.STONE;

        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        String name = config.getString(path + ".name", "").replace("%id%", String.valueOf(homeId));
        meta.displayName(ColorUtils.colorize(name));

        List<Component> lore = config.getStringList(path + ".lore").stream()
                .map(line -> ColorUtils.colorize(line.replace("%id%", String.valueOf(homeId))))
                .toList();

        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    public static int getHomeIdFromSlot(HTeleport plugin, int slot) {
        FileConfiguration guiConfig = plugin.getConfigManager().getGuiConfig();
        List<Integer> slots = guiConfig.getIntegerList("home-menu.home-slots");
        int index = slots.indexOf(slot);
        if (index == -1) return -1;

        int homeId = index + 1;
        return homeId <= plugin.getHomeManager().getGlobalMax() ? homeId : -1;
    }
}