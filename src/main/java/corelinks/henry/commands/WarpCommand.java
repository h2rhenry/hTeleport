// src/main/java/corelinks/henry/commands/WarpCommand.java
package corelinks.henry.commands;

import corelinks.henry.managers.TeleportManager;
import corelinks.henry.managers.WarpManager;
import corelinks.henry.models.Warp;
import corelinks.henry.util.ColorUtils;
import corelinks.henry.util.ConfigManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class WarpCommand implements CommandExecutor, TabCompleter {

    private final WarpManager warpManager;
    private final TeleportManager teleportManager;
    private final ConfigManager configManager;

    public WarpCommand(WarpManager warpManager, TeleportManager teleportManager, ConfigManager configManager) {
        this.warpManager = warpManager;
        this.teleportManager = teleportManager;
        this.configManager = configManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendUsage(sender);
            return true;
        }

        String sub = args[0].toLowerCase();

        switch (sub) {
            case "create" -> handleCreate(sender, args);
            case "delete" -> handleDelete(sender, args);
            case "setperm" -> handleSetPerm(sender, args);
            case "delperm" -> handleDelPerm(sender, args);
            case "list" -> handleList(sender);
            default -> handleTeleport(sender, args[0]);
        }

        return true;
    }

    private void handleCreate(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ColorUtils.colorize(configManager.getMessage("player-only")));
            return;
        }
        if (!player.hasPermission("hTeleport.admin")) {
            player.sendMessage(ColorUtils.colorize(configManager.getMessage("no-permission")));
            return;
        }
        if (args.length < 2) {
            player.sendMessage(ColorUtils.colorize("&cUsage: /warp create <name> [permission]"));
            return;
        }

        String name = args[1];
        String perm = args.length >= 3 ? args[2] : null;

        warpManager.createWarp(name, player.getLocation(), perm);
        player.sendMessage(ColorUtils.colorize(configManager.getMessage("warp-created").replace("%warp%", name)));
    }

    private void handleDelete(CommandSender sender, String[] args) {
        if (!sender.hasPermission("hTeleport.admin")) {
            sender.sendMessage(ColorUtils.colorize(configManager.getMessage("no-permission")));
            return;
        }
        if (args.length < 2) {
            sender.sendMessage(ColorUtils.colorize("&cUsage: /warp delete <name>"));
            return;
        }

        String name = args[1];
        if (warpManager.deleteWarp(name)) {
            sender.sendMessage(ColorUtils.colorize(configManager.getMessage("warp-deleted").replace("%warp%", name)));
        } else {
            sender.sendMessage(ColorUtils.colorize(configManager.getMessage("warp-not-found").replace("%warp%", name)));
        }
    }

    private void handleSetPerm(CommandSender sender, String[] args) {
        if (!sender.hasPermission("hTeleport.admin")) {
            sender.sendMessage(ColorUtils.colorize(configManager.getMessage("no-permission")));
            return;
        }
        if (args.length < 3) {
            sender.sendMessage(ColorUtils.colorize("&cUsage: /warp setperm <name> <permission>"));
            return;
        }

        String name = args[1];
        String perm = args[2];

        if (warpManager.setPermission(name, perm)) {
            sender.sendMessage(ColorUtils.colorize(configManager.getMessage("warp-perm-set")
                    .replace("%warp%", name).replace("%perm%", perm)));
        } else {
            sender.sendMessage(ColorUtils.colorize(configManager.getMessage("warp-not-found").replace("%warp%", name)));
        }
    }

    private void handleDelPerm(CommandSender sender, String[] args) {
        if (!sender.hasPermission("hTeleport.admin")) {
            sender.sendMessage(ColorUtils.colorize(configManager.getMessage("no-permission")));
            return;
        }
        if (args.length < 2) {
            sender.sendMessage(ColorUtils.colorize("&cUsage: /warp delperm <name>"));
            return;
        }

        String name = args[1];
        if (warpManager.removePermission(name)) {
            sender.sendMessage(ColorUtils.colorize(configManager.getMessage("warp-perm-removed").replace("%warp%", name)));
        } else {
            sender.sendMessage(ColorUtils.colorize(configManager.getMessage("warp-not-found").replace("%warp%", name)));
        }
    }

    private void handleList(CommandSender sender) {
        var warps = warpManager.getAllWarps();
        if (warps.isEmpty()) {
            sender.sendMessage(ColorUtils.colorize(configManager.getMessage("warp-list-empty")));
            return;
        }

        sender.sendMessage(ColorUtils.colorize(configManager.getMessage("warp-list-header")));
        for (Warp warp : warps) {
            String permHint = warp.hasPermission() ? "&7(Perm: &f" + warp.getPermission() + "&7)" : "&7(Public)";
            String item = configManager.getRawMessage("warp-list-item")
                    .replace("%warp%", warp.getName())
                    .replace("%perm_hint%", permHint);
            sender.sendMessage(ColorUtils.colorize(item));
        }
    }

    private void handleTeleport(CommandSender sender, String warpName) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ColorUtils.colorize(configManager.getMessage("player-only")));
            return;
        }

        Warp warp = warpManager.getWarp(warpName);
        if (warp == null) {
            player.sendMessage(ColorUtils.colorize(configManager.getMessage("warp-not-found").replace("%warp%", warpName)));
            return;
        }

        if (warp.hasPermission() && !player.hasPermission(warp.getPermission()) && !player.hasPermission("hTeleport.admin")) {
            player.sendMessage(ColorUtils.colorize(configManager.getMessage("warp-no-perm").replace("%warp%", warp.getName())));
            return;
        }

        teleportManager.teleport(player, warp.getLocation(), "warp", null);
    }

    private void sendUsage(CommandSender sender) {
        sender.sendMessage(ColorUtils.colorize("&b/warp <name> &7- Teleport to a warp"));
        sender.sendMessage(ColorUtils.colorize("&b/warp list &7- List available warps"));
        if (sender.hasPermission("hTeleport.admin")) {
            sender.sendMessage(ColorUtils.colorize("&b/warp create <name> [perm] &7- Create a warp"));
            sender.sendMessage(ColorUtils.colorize("&b/warp delete <name> &7- Delete a warp"));
            sender.sendMessage(ColorUtils.colorize("&b/warp setperm <name> <perm> &7- Set warp permission"));
            sender.sendMessage(ColorUtils.colorize("&b/warp delperm <name> &7- Remove warp permission"));
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            List<String> subs = new ArrayList<>(List.of("list"));
            if (sender.hasPermission("hTeleport.admin")) {
                subs.addAll(Arrays.asList("create", "delete", "setperm", "delperm"));
            }
            warpManager.getAllWarps().forEach(w -> subs.add(w.getName()));
            return filterCompletions(subs, args[0]);
        } else if (args.length == 2 && (args[0].equalsIgnoreCase("delete") || args[0].equalsIgnoreCase("setperm") || args[0].equalsIgnoreCase("delperm"))) {
            List<String> names = new ArrayList<>();
            warpManager.getAllWarps().forEach(w -> names.add(w.getName()));
            return filterCompletions(names, args[1]);
        }
        return completions;
    }

    private List<String> filterCompletions(List<String> list, String prefix) {
        return list.stream()
                .filter(s -> s.toLowerCase().startsWith(prefix.toLowerCase()))
                .toList();
    }
}