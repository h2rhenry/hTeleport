// src/main/java/corelinks/henry/commands/SpawnCommand.java
package corelinks.henry.commands;

import corelinks.henry.managers.SpawnManager;
import corelinks.henry.managers.TeleportManager;
import corelinks.henry.util.ColorUtils;
import corelinks.henry.util.ConfigManager;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SpawnCommand implements CommandExecutor {

    private final SpawnManager spawnManager;
    private final TeleportManager teleportManager;
    private final ConfigManager configManager;

    public SpawnCommand(SpawnManager spawnManager, TeleportManager teleportManager, ConfigManager configManager) {
        this.spawnManager = spawnManager;
        this.teleportManager = teleportManager;
        this.configManager = configManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ColorUtils.colorize(configManager.getMessage("player-only")));
            return true;
        }

        if (!spawnManager.hasSpawn()) {
            player.sendMessage(ColorUtils.colorize(configManager.getMessage("spawn-not-set")));
            return true;
        }

        Location spawnLoc = spawnManager.getSpawn();
        teleportManager.teleport(player, spawnLoc, "spawn", null);
        return true;
    }
}