// src/main/java/corelinks/henry/commands/BackCommand.java
package corelinks.henry.commands;

import corelinks.henry.managers.DeathManager;
import corelinks.henry.managers.TeleportManager;
import corelinks.henry.util.ColorUtils;
import corelinks.henry.util.ConfigManager;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class BackCommand implements CommandExecutor {

    private final DeathManager deathManager;
    private final TeleportManager teleportManager;
    private final ConfigManager configManager;

    public BackCommand(DeathManager deathManager, TeleportManager teleportManager, ConfigManager configManager) {
        this.deathManager = deathManager;
        this.teleportManager = teleportManager;
        this.configManager = configManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ColorUtils.colorize(configManager.getMessage("player-only")));
            return true;
        }

        if (!player.hasPermission("hTeleport.back")) {
            player.sendMessage(ColorUtils.colorize(configManager.getMessage("no-permission")));
            return true;
        }

        Location deathLoc = deathManager.getDeathLocation(player.getUniqueId());
        if (deathLoc == null) {
            player.sendMessage(ColorUtils.colorize(configManager.getMessage("back-no-location")));
            return true;
        }

        teleportManager.teleport(player, deathLoc, "back", () -> deathManager.clearDeathLocation(player.getUniqueId()));
        return true;
    }
}