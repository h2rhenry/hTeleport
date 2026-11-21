package corelinks.henry.commands;

import corelinks.henry.managers.RtpManager;
import corelinks.henry.util.ColorUtils;
import corelinks.henry.util.ConfigManager;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class RtpCommand implements CommandExecutor, TabCompleter {

    private final RtpManager rtpManager;
    private final ConfigManager configManager;

    public RtpCommand(RtpManager rtpManager, ConfigManager configManager) {
        this.rtpManager = rtpManager;
        this.configManager = configManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ColorUtils.colorize(configManager.getMessage("player-only")));
            return true;
        }

        // /rtp -> RTP ngay trong thế giới hiện tại (hành vi cũ)
        if (args.length == 0) {
            rtpManager.executeRtp(player);
            return true;
        }

        // /rtp <world> -> RTP thẳng tới thế giới được chỉ định
        World targetWorld = Bukkit.getWorld(args[0]);
        if (targetWorld == null) {
            player.sendMessage(ColorUtils.colorize(configManager.getMessage("rtp-world-not-found")
                    .replace("%world%", args[0])));
            return true;
        }

        rtpManager.executeRtp(player, targetWorld);
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, String @NotNull [] args) {
        if (args.length == 1) {
            List<String> names = new ArrayList<>(rtpManager.getConfiguredWorldNames());
            return names.stream()
                    .filter(name -> name.toLowerCase().startsWith(args[0].toLowerCase()))
                    .toList();
        }
        return List.of();
    }
}
