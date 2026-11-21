// src/main/java/corelinks/henry/util/YamlStore.java
package corelinks.henry.util;

import corelinks.henry.HTeleport;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.logging.Level;

/**
 * Kho dữ liệu YAML an toàn đa luồng (homes.yml, warps.yml, deaths.yml, spawn.yml).
 *
 * - File chỉ được đọc 1 lần khi khởi động và giữ trong RAM; mọi thay đổi chỉ sửa trên bản RAM.
 * - Việc ghi ra ổ đĩa được gom lại (debounce) và chạy ở luồng bất đồng bộ, nên không bao giờ
 *   chặn tick của server, và nhiều người chơi ở nhiều region Folia cùng ghi dữ liệu một lúc
 *   cũng không thể làm mất dữ liệu hay hỏng file.
 * - Ghi file theo kiểu "ghi ra file tạm rồi đổi tên", nên server sập giữa chừng cũng không hỏng file.
 */
public final class YamlStore {

    /** Thời gian gom các thay đổi liên tiếp thành 1 lần ghi đĩa. */
    private static final long SAVE_DELAY_MS = 500L;

    private final HTeleport plugin;
    private final File file;
    private final YamlConfiguration config;

    private final Object dataLock = new Object();
    private final Object diskLock = new Object();
    private final AtomicBoolean savePending = new AtomicBoolean(false);

    public YamlStore(HTeleport plugin, String fileName) {
        this.plugin = plugin;

        File dataFolder = plugin.getDataFolder();
        if (!dataFolder.exists() && !dataFolder.mkdirs()) {
            plugin.getLogger().warning("Could not create plugin data folder.");
        }

        this.file = new File(dataFolder, fileName);
        // Nếu file chưa tồn tại, loadConfiguration trả về cấu hình rỗng
        this.config = YamlConfiguration.loadConfiguration(file);
    }

    /** Đọc dữ liệu (không kích hoạt lưu file). */
    public void read(Consumer<FileConfiguration> reader) {
        synchronized (dataLock) {
            reader.accept(config);
        }
    }

    /** Sửa dữ liệu trong RAM và lên lịch lưu file bất đồng bộ. */
    public void update(Consumer<FileConfiguration> mutator) {
        synchronized (dataLock) {
            mutator.accept(config);
        }
        scheduleSave();
    }

    private void scheduleSave() {
        if (!savePending.compareAndSet(false, true)) return;

        try {
            plugin.getServer().getAsyncScheduler()
                    .runDelayed(plugin, task -> flush(), SAVE_DELAY_MS, TimeUnit.MILLISECONDS);
        } catch (RuntimeException e) {
            // Plugin đang tắt nên không thể lập lịch -> lưu ngay lập tức
            flush();
        }
    }

    /** Ghi ngay dữ liệu hiện tại ra đĩa. Được gọi khi tắt plugin để không mất dữ liệu chưa lưu. */
    public void flush() {
        savePending.set(false);

        // Chụp dữ liệu BÊN TRONG diskLock để thứ tự ghi luôn khớp thứ tự chụp
        // (tránh bản cũ ghi đè lên bản mới khi 2 lần lưu chạy chồng nhau).
        synchronized (diskLock) {
            String data;
            synchronized (dataLock) {
                data = config.saveToString();
            }
            writeAtomically(data);
        }
    }

    private void writeAtomically(String data) {
        Path target = file.toPath();
        Path temp = target.resolveSibling(file.getName() + ".tmp");
        try {
            Files.writeString(temp, data, StandardCharsets.UTF_8);
            try {
                Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save " + file.getName(), e);
        }
    }
}
