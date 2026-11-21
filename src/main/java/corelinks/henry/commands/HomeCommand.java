// src/main/java/corelinks/henry/commands/HomeCommand.java
package corelinks.henry.commands;

import corelinks.henry.HTeleport;
import corelinks.henry.gui.HomeGUI;
import corelinks.henry.managers.HomeManager;
import corelinks.henry.managers.TeleportManager;
import corelinks.henry.util.ColorUtils;
import corelinks.henry.util.ConfigManager;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class HomeCommand implements CommandExecutor, TabCompleter {

    private final HTeleport plugin;
    private final HomeManager homeManager;
    private final TeleportManager teleportManager;
    private final ConfigManager configManager;

    public HomeCommand(HTeleport plugin, HomeManager homeManager, TeleportManager teleportManager, ConfigManager configManager) {
        this.plugin = plugin;
        this.homeManager = homeManager;
        this.teleportManager = teleportManager;
        this.configManager = configManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ColorUtils.colorize(configManager.getMessage("player-only")));
            return true;
        }

        // 1. Không tham số -> Mở GUI
        if (args.length == 0) {
            HomeGUI.openHomeMenu(plugin, player);
            return true;
        }

        // 2. /home list hoặc /homes
        if (args[0].equalsIgnoreCase("list") || command.getName().equalsIgnoreCase("homes")) {
            sendHomeList(player);
            return true;
        }

        // 3. /home <id> -> Teleport thẳng
        int homeId;
        try {
            homeId = Integer.parseInt(args[0]);
        } catch (NumberFormatException e) {
            player.sendMessage(ColorUtils.colorize(configManager.getMessage("home-invalid-id")
                    .replace("%max%", String.valueOf(homeManager.getGlobalMax()))));
            return true;
        }

        Location loc = homeManager.getHome(player.getUniqueId(), homeId);
        if (loc == null) {
            player.sendMessage(ColorUtils.colorize(configManager.getMessage("home-not-found")
                    .replace("%id%", String.valueOf(homeId))));
            return true;
        }

        // Home nằm ngoài giới hạn hiện tại (ví dụ bị hạ permission) -> bị khóa, giống như trong GUI
        if (homeId > homeManager.getMaxHomes(player)) {
            player.sendMessage(ColorUtils.colorize(configManager.getMessage("home-no-permission-slot")
                    .replace("%id%", String.valueOf(homeId))));
            return true;
        }

        teleportManager.teleport(player, loc, "home", null);
        return true;
    }

    private void sendHomeList(Player player) {
        int max = homeManager.getMaxHomes(player);
        int count = 0;
        List<String> items = new ArrayList<>();

        for (int i : homeManager.getHomeIds(player.getUniqueId())) {
            Location loc = homeManager.getHome(player.getUniqueId(), i);
            if (loc != null) {
                count++;
                String item = configManager.getRawMessage("home-list-item")
                        .replace("%id%", String.valueOf(i))
                        .replace("%world%", loc.getWorld().getName())
                        .replace("%x%", String.valueOf((int) loc.getX()))
                        .replace("%y%", String.valueOf((int) loc.getY()))
                        .replace("%z%", String.valueOf((int) loc.getZ()));
                items.add(item);
            }
        }

        if (count == 0) {
            player.sendMessage(ColorUtils.colorize(configManager.getMessage("home-list-empty")));
            return;
        }

        String header = configManager.getRawMessage("home-list-header")
                .replace("%count%", String.valueOf(count))
                .replace("%max%", String.valueOf(max));
        player.sendMessage(ColorUtils.colorize(header));

        for (String item : items) {
            player.sendMessage(ColorUtils.colorize(item));
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1 && sender instanceof Player player) {
            List<String> suggestions = new ArrayList<>(List.of("list"));
            for (int i : homeManager.getHomeIds(player.getUniqueId())) {
                suggestions.add(String.valueOf(i));
            }
            return suggestions.stream().filter(s -> s.startsWith(args[0])).toList();
        }
        return List.of();
    }
}