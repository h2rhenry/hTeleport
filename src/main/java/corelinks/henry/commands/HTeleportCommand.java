// src/main/java/corelinks/henry/commands/HTeleportCommand.java
package corelinks.henry.commands;

import corelinks.henry.HTeleport;
import corelinks.henry.util.ColorUtils;
import corelinks.henry.util.ConfigManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.List;

public class HTeleportCommand implements CommandExecutor, TabCompleter {

    private final HTeleport plugin;
    private final ConfigManager configManager;

    public HTeleportCommand(HTeleport plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("hTeleport.admin")) {
                sender.sendMessage(ColorUtils.colorize(configManager.getMessage("no-permission")));
                return true;
            }

            configManager.reloadConfigs();
            sender.sendMessage(ColorUtils.colorize(configManager.getMessage("reload-success")));
            return true;
        }

        // Mặc định hoặc /htlp help
        sendHelp(sender);
        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(ColorUtils.colorize(configManager.getRawMessage("help-header")));
        sender.sendMessage(ColorUtils.colorize(configManager.getRawMessage("help-home")));
        String maxHomes = String.valueOf(plugin.getHomeManager().getGlobalMax());
        sender.sendMessage(ColorUtils.colorize(configManager.getRawMessage("help-sethome").replace("%max%", maxHomes)));
        sender.sendMessage(ColorUtils.colorize(configManager.getRawMessage("help-delhome").replace("%max%", maxHomes)));
        sender.sendMessage(ColorUtils.colorize(configManager.getRawMessage("help-spawn")));
        sender.sendMessage(ColorUtils.colorize(configManager.getRawMessage("help-warp")));
        sender.sendMessage(ColorUtils.colorize(configManager.getRawMessage("help-tpa")));
        sender.sendMessage(ColorUtils.colorize(configManager.getRawMessage("help-back")));

        if (sender.hasPermission("hTeleport.admin")) {
            sender.sendMessage(ColorUtils.colorize(configManager.getRawMessage("help-setspawn")));
            sender.sendMessage(ColorUtils.colorize(configManager.getRawMessage("help-reload")));
        }

        sender.sendMessage(ColorUtils.colorize(configManager.getRawMessage("help-footer")));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> sub = new ArrayList<>(List.of("help"));
            if (sender.hasPermission("hTeleport.admin")) {
                sub.add("reload");
            }
            return sub.stream().filter(s -> s.startsWith(args[0].toLowerCase())).toList();
        }
        return List.of();
    }
}