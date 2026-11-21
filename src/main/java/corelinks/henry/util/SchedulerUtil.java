// src/main/java/corelinks/henry/util/SchedulerUtil.java
package corelinks.henry.util;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;

/**
 * Tiện ích lập lịch tương thích Folia.
 *
 * Folia chia thế giới thành nhiều "region" chạy song song trên nhiều luồng khác nhau, nên
 * KHÔNG được đụng vào Player/Block của region khác từ luồng hiện tại. Toàn bộ API bên dưới là
 * API chuẩn của Paper (Entity/Region Scheduler), vì vậy cùng một đoạn code chạy được trên cả
 * Paper (mọi thứ nằm trên luồng chính) lẫn Folia mà không cần thư viện hay reflection.
 */
public final class SchedulerUtil {

    private SchedulerUtil() {}

    /**
     * Chạy tác vụ trên luồng đang sở hữu entity (Player).
     * Nếu luồng hiện tại đã là luồng đó thì chạy ngay (không tốn thêm tick), ngược lại
     * đẩy vào Entity Scheduler. Nếu entity đã bị gỡ (người chơi thoát) tác vụ sẽ bị bỏ qua.
     */
    public static void runOnEntity(Plugin plugin, Entity entity, Runnable task) {
        if (Bukkit.isOwnedByCurrentRegion(entity)) {
            task.run();
            return;
        }
        entity.getScheduler().run(plugin, scheduled -> task.run(), null);
    }

    /**
     * Chạy tác vụ trên luồng đang sở hữu chunk (chunkX, chunkZ) của world.
     * Dùng khi cần đọc block/độ cao địa hình.
     */
    public static void runAtChunk(Plugin plugin, World world, int chunkX, int chunkZ, Runnable task) {
        if (Bukkit.isOwnedByCurrentRegion(world, chunkX, chunkZ)) {
            task.run();
            return;
        }
        Bukkit.getRegionScheduler().run(plugin, world, chunkX, chunkZ, scheduled -> task.run());
    }
}
