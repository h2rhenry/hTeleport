// src/main/java/corelinks/henry/commands/TpAllCommand.java
package corelinks.henry.commands;

import corelinks.henry.managers.TeleportManager;
import corelinks.henry.util.ColorUtils;
import corelinks.henry.util.ConfigManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class TpAllCommand implements CommandExecutor {

    private final TeleportManager teleportManager;
    private final ConfigManager configManager;

    public TpAllCommand(TeleportManager teleportManager, ConfigManager configManager) {
        this.teleportManager = teleportManager;
        this.configManager = configManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ColorUtils.colorize(configManager.getMessage("player-only")));
            return true;
        }

        if (!player.hasPermission("hTeleport.tpall")) {
            player.sendMessage(ColorUtils.colorize(configManager.getMessage("no-permission")));
            return true;
        }

        Location targetLocation = player.getLocation();
        player.sendMessage(ColorUtils.colorize(configManager.getMessage("tpall-initiated")));

        for (Player target : Bukkit.getOnlinePlayers()) {
            if (target.getUniqueId().equals(player.getUniqueId())) continue;

            target.sendMessage(ColorUtils.colorize(configManager.getMessage("tpall-teleported")
                    .replace("%player%", player.getName())));
            teleportManager.teleport(target, targetLocation, "tpall", null);
        }

        return true;
    }
}