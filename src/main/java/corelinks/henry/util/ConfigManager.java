package corelinks.henry.util;

import corelinks.henry.HTeleport;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;

public class ConfigManager {

    private final HTeleport plugin;
    // volatile: các region của Folia đọc tin nhắn/GUI song song, còn /htlp reload sẽ thay thế đối tượng này
    private volatile FileConfiguration messagesConfig;
    private volatile FileConfiguration guiConfig;
    private File messagesFile;

    public ConfigManager(HTeleport plugin) {
        this.plugin = plugin;
        plugin.saveDefaultConfig();
        loadMessages();
        loadGui();
    }

    // Phương thức tải/khởi tạo file messages.yml
    public void loadMessages() {
        messagesFile = new File(plugin.getDataFolder(), "messages.yml");
        if (!messagesFile.exists()) {
            plugin.saveResource("messages.yml", false);
        }
        messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);
    }

    // Tải gui.yml vào RAM (dùng lại cho mọi lần mở menu/click thay vì đọc file từ đĩa liên tục)
    public void loadGui() {
        guiConfig = createYamlFile("gui.yml");
    }

    public FileConfiguration getGuiConfig() {
        if (guiConfig == null) {
            loadGui();
        }
        return guiConfig;
    }

    // Phương thức Reload lại toàn bộ config, messages và gui
    public void reloadConfigs() {
        plugin.reloadConfig();
        loadMessages();
        loadGui();
    }

    // Lấy chuỗi raw từ messages.yml
    public String getRawMessage(String path) {
        if (messagesConfig == null) {
            loadMessages();
        }
        return messagesConfig.getString(path, "&cMissing message: " + path);
    }

    public String getMessage(String path) {
        return getRawMessage(path);
    }

    // Quản lý các file YAML khác (homes.yml, warps.yml...)
    public FileConfiguration createYamlFile(String fileName) {
        File file = new File(plugin.getDataFolder(), fileName);
        if (!file.exists()) {
            try {
                if (plugin.getResource(fileName) != null) {
                    plugin.saveResource(fileName, false);
                } else {
                    file.createNewFile();
                }
            } catch (IOException e) {
                plugin.getLogger().severe("Could not create " + fileName);
            }
        }
        return YamlConfiguration.loadConfiguration(file);
    }

    public void saveYamlFile(FileConfiguration config, String fileName) {
        File file = new File(plugin.getDataFolder(), fileName);
        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save " + fileName);
        }
    }
}