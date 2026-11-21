// src/main/java/corelinks/henry/commands/TpDenyCommand.java
package corelinks.henry.commands;

import corelinks.henry.managers.TpaManager;
import corelinks.henry.util.ColorUtils;
import corelinks.henry.util.ConfigManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class TpDenyCommand implements CommandExecutor {

    private final TpaManager tpaManager;
    private final ConfigManager configManager;

    public TpDenyCommand(TpaManager tpaManager, ConfigManager configManager) {
        this.tpaManager = tpaManager;
        this.configManager = configManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ColorUtils.colorize(configManager.getMessage("player-only")));
            return true;
        }

        if (!player.hasPermission("hTeleport.tpdeny")) {
            player.sendMessage(ColorUtils.colorize(configManager.getMessage("no-permission")));
            return true;
        }

        tpaManager.denyRequest(player);
        return true;
    }
}