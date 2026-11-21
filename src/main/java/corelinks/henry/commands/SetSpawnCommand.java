// src/main/java/corelinks/henry/commands/SetSpawnCommand.java
package corelinks.henry.commands;

import corelinks.henry.managers.SpawnManager;
import corelinks.henry.util.ColorUtils;
import corelinks.henry.util.ConfigManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SetSpawnCommand implements CommandExecutor {

    private final SpawnManager spawnManager;
    private final ConfigManager configManager;

    public SetSpawnCommand(SpawnManager spawnManager, ConfigManager configManager) {
        this.spawnManager = spawnManager;
        this.configManager = configManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ColorUtils.colorize(configManager.getMessage("player-only")));
            return true;
        }

        if (!player.hasPermission("hTeleport.admin")) {
            player.sendMessage(ColorUtils.colorize(configManager.getMessage("no-permission")));
            return true;
        }

        spawnManager.setSpawn(player.getLocation());
        player.sendMessage(ColorUtils.colorize(configManager.getMessage("spawn-set")));
        return true;
    }
}