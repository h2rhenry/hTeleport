// src/main/java/corelinks/henry/managers/TpaManager.java
package corelinks.henry.managers;

import corelinks.henry.HTeleport;
import corelinks.henry.models.TpaRequest;
import corelinks.henry.util.ColorUtils;
import corelinks.henry.util.ConfigManager;
import corelinks.henry.util.SchedulerUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class TpaManager {

    private final HTeleport plugin;
    private final ConfigManager configManager;
    private final TeleportManager teleportManager;

    // Target UUID -> Pending Incoming Request
    // (ConcurrentHashMap: người gửi, người nhận và bộ đếm hết hạn có thể chạy ở các luồng region khác nhau của Folia)
    private final Map<UUID, TpaRequest> incomingRequests = new ConcurrentHashMap<>();
    // Sender UUID -> Target UUID
    private final Map<UUID, UUID> outgoingRequests = new ConcurrentHashMap<>();

    public TpaManager(HTeleport plugin, ConfigManager configManager, TeleportManager teleportManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.teleportManager = teleportManager;
    }

    public void sendRequest(Player sender, Player target, TpaRequest.Type type) {
        if (sender.getUniqueId().equals(target.getUniqueId())) {
            sender.sendMessage(ColorUtils.colorize(configManager.getMessage("tpa-self")));
            return;
        }

        cancelOutgoing(sender.getUniqueId(), false);

        TpaRequest request = new TpaRequest(sender.getUniqueId(), target.getUniqueId(), type);
        incomingRequests.put(target.getUniqueId(), request);
        outgoingRequests.put(sender.getUniqueId(), target.getUniqueId());

        String feature = type == TpaRequest.Type.TPA ? "tpa" : "tphere";
        int expiry = plugin.getConfig().getInt(feature + ".request-expiry", 60);

        // Global Region Scheduler: tác vụ hẹn giờ không gắn với người chơi hay vị trí cụ thể nào.
        // Độ trễ tối thiểu là 1 tick nên chặn trường hợp request-expiry <= 0 trong config.
        Bukkit.getGlobalRegionScheduler().runDelayed(plugin, task -> {
            // Chỉ hết hạn nếu request này vẫn còn nguyên (chưa được chấp nhận / từ chối / hủy / thay thế)
            if (!incomingRequests.remove(target.getUniqueId(), request)) return;
            outgoingRequests.remove(sender.getUniqueId(), target.getUniqueId());

            Player s = Bukkit.getPlayer(sender.getUniqueId());
            Player t = Bukkit.getPlayer(target.getUniqueId());

            if (s != null) {
                s.sendMessage(ColorUtils.colorize(configManager.getMessage("request-expired-sender")
                        .replace("%player%", target.getName())));
            }
            if (t != null) {
                t.sendMessage(ColorUtils.colorize(configManager.getMessage("request-expired-target")
                        .replace("%player%", sender.getName())));
            }
        }, Math.max(1L, expiry * 20L));

        // Notify Sender
        String sentKey = type == TpaRequest.Type.TPA ? "tpa-sent" : "tphere-sent";
        sender.sendMessage(ColorUtils.colorize(configManager.getMessage(sentKey)
                .replace("%player%", target.getName())));

        // Notify Target with interactive component
        String recvKey = type == TpaRequest.Type.TPA ? "tpa-received" : "tphere-received";
        Component baseMsg = ColorUtils.colorize(configManager.getMessage(recvKey)
                .replace("%player%", sender.getName()));

        // Đổi /htlp:tpaccept thành /tpaccept
        Component acceptBtn = ColorUtils.colorize(configManager.getRawMessage("tpa-accept-button"))
                .clickEvent(ClickEvent.runCommand("/tpaccept"))
                .hoverEvent(HoverEvent.showText(ColorUtils.colorize(configManager.getRawMessage("tpa-accept-hover"))));

        Component space = Component.text(" ");

        // Đổi /htlp:tpdeny thành /tpdeny
        Component denyBtn = ColorUtils.colorize(configManager.getRawMessage("tpa-deny-button"))
                .clickEvent(ClickEvent.runCommand("/tpdeny"))
                .hoverEvent(HoverEvent.showText(ColorUtils.colorize(configManager.getRawMessage("tpa-deny-hover"))));

        target.sendMessage(baseMsg.append(acceptBtn).append(space).append(denyBtn));
    }

    public void acceptRequest(Player target) {
        TpaRequest request = incomingRequests.remove(target.getUniqueId());
        if (request == null) {
            target.sendMessage(ColorUtils.colorize(configManager.getMessage("no-pending-request")));
            return;
        }

        outgoingRequests.remove(request.getSender());
        Player sender = Bukkit.getPlayer(request.getSender());

        if (sender == null || !sender.isOnline()) {
            target.sendMessage(ColorUtils.colorize(configManager.getMessage("player-not-found")
                    .replace("%player%", "Sender")));
            return;
        }

        target.sendMessage(ColorUtils.colorize(configManager.getMessage("request-accepted-target")
                .replace("%player%", sender.getName())));
        sender.sendMessage(ColorUtils.colorize(configManager.getMessage("request-accepted-sender")
                .replace("%player%", target.getName())));

        if (request.getType() == TpaRequest.Type.TPA) {
            // Đang chạy trên luồng của target nên đọc được vị trí target; việc dịch chuyển sender
            // sẽ tự được chuyển sang luồng của sender bên trong TeleportManager.
            teleportManager.teleport(sender, target.getLocation(), "tpa", null);
        } else {
            // Folia: chỉ được đọc vị trí của sender trên luồng của sender (khác luồng với target)
            SchedulerUtil.runOnEntity(plugin, sender, () -> {
                Location senderLocation = sender.getLocation();
                teleportManager.teleport(target, senderLocation, "tphere", null);
            });
        }
    }

    public void denyRequest(Player target) {
        TpaRequest request = incomingRequests.remove(target.getUniqueId());
        if (request == null) {
            target.sendMessage(ColorUtils.colorize(configManager.getMessage("no-pending-request")));
            return;
        }

        outgoingRequests.remove(request.getSender());
        Player sender = Bukkit.getPlayer(request.getSender());

        target.sendMessage(ColorUtils.colorize(configManager.getMessage("request-denied-target")
                .replace("%player%", sender != null ? sender.getName() : "Unknown")));

        if (sender != null) {
            sender.sendMessage(ColorUtils.colorize(configManager.getMessage("request-denied-sender")
                    .replace("%player%", target.getName())));
        }
    }

    public void cancelOutgoing(UUID senderUuid, boolean notify) {
        UUID targetUuid = outgoingRequests.remove(senderUuid);
        if (targetUuid == null) {
            if (notify) {
                Player sender = Bukkit.getPlayer(senderUuid);
                if (sender != null) {
                    sender.sendMessage(ColorUtils.colorize(configManager.getMessage("no-outgoing-request")));
                }
            }
            return;
        }

        incomingRequests.remove(targetUuid);
        Player sender = Bukkit.getPlayer(senderUuid);
        Player target = Bukkit.getPlayer(targetUuid);

        if (notify && sender != null) {
            sender.sendMessage(ColorUtils.colorize(configManager.getMessage("request-cancelled-sender")
                    .replace("%player%", target != null ? target.getName() : "Target")));
        }
        if (target != null) {
            target.sendMessage(ColorUtils.colorize(configManager.getMessage("request-cancelled-target")
                    .replace("%player%", sender != null ? sender.getName() : "Sender")));
        }
    }
}