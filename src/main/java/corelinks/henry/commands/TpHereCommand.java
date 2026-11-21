// src/main/java/corelinks/henry/commands/TpHereCommand.java
package corelinks.henry.commands;

import corelinks.henry.managers.TpaManager;
import corelinks.henry.models.TpaRequest;
import corelinks.henry.util.ColorUtils;
import corelinks.henry.util.ConfigManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class TpHereCommand implements CommandExecutor, TabCompleter {

    private final TpaManager tpaManager;
    private final ConfigManager configManager;

    public TpHereCommand(TpaManager tpaManager, ConfigManager configManager) {
        this.tpaManager = tpaManager;
        this.configManager = configManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ColorUtils.colorize(configManager.getMessage("player-only")));
            return true;
        }

        if (!player.hasPermission("hTeleport.tphere")) {
            player.sendMessage(ColorUtils.colorize(configManager.getMessage("no-permission")));
            return true;
        }

        if (args.length < 1) {
            player.sendMessage(ColorUtils.colorize("&cUsage: /tphere <player>"));
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null || !target.isOnline()) {
            player.sendMessage(ColorUtils.colorize(configManager.getMessage("player-not-found").replace("%player%", args[0])));
            return true;
        }

        tpaManager.sendRequest(player, target, TpaRequest.Type.TPHERE);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> names = new ArrayList<>();
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (!p.getName().equalsIgnoreCase(sender.getName())) {
                    names.add(p.getName());
                }
            }
            return names.stream().filter(n -> n.toLowerCase().startsWith(args[0].toLowerCase())).toList();
        }
        return List.of();
    }
}