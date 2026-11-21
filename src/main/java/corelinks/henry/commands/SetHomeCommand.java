// src/main/java/corelinks/henry/commands/SetHomeCommand.java
package corelinks.henry.commands;

import corelinks.henry.HTeleport;
import corelinks.henry.managers.HomeManager;
import corelinks.henry.util.ColorUtils;
import corelinks.henry.util.ConfigManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class SetHomeCommand implements CommandExecutor, TabCompleter {

    private final HomeManager homeManager;
    private final ConfigManager configManager;

    public SetHomeCommand(HomeManager homeManager, ConfigManager configManager) {
        this.homeManager = homeManager;
        this.configManager = configManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ColorUtils.colorize(configManager.getMessage("player-only")));
            return true;
        }

        String globalMax = String.valueOf(homeManager.getGlobalMax());

        if (args.length < 1) {
            player.sendMessage(ColorUtils.colorize(configManager.getMessage("home-usage-sethome")
                    .replace("%max%", globalMax)));
            return true;
        }

        int homeId;
        try {
            homeId = Integer.parseInt(args[0]);
        } catch (NumberFormatException e) {
            player.sendMessage(ColorUtils.colorize(configManager.getMessage("home-invalid-id")
                    .replace("%max%", globalMax)));
            return true;
        }

        // Giới hạn tuyệt đối lấy từ home.max-homes trong config.yml
        if (homeId < 1 || homeId > homeManager.getGlobalMax()) {
            player.sendMessage(ColorUtils.colorize(configManager.getMessage("home-invalid-id")
                    .replace("%max%", globalMax)));
            return true;
        }

        int maxHomes = homeManager.getMaxHomes(player);
        if (homeId > maxHomes) {
            player.sendMessage(ColorUtils.colorize(configManager.getMessage("home-no-permission-slot")
                    .replace("%id%", String.valueOf(homeId))));
            return true;
        }

        homeManager.setHome(player.getUniqueId(), homeId, player.getLocation());
        player.sendMessage(ColorUtils.colorize(configManager.getMessage("home-set-success")
                .replace("%id%", String.valueOf(homeId))));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1 && sender instanceof Player player) {
            int max = homeManager.getMaxHomes(player);
            List<String> suggestions = new ArrayList<>();
            for (int i = 1; i <= max; i++) {
                suggestions.add(String.valueOf(i));
            }
            return suggestions.stream().filter(s -> s.startsWith(args[0])).toList();
        }
        return List.of();
    }
}