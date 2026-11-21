package corelinks.henry;

import corelinks.henry.commands.*;
import corelinks.henry.listeners.*;
import corelinks.henry.managers.*;
import corelinks.henry.util.ConfigManager;
import org.bukkit.plugin.java.JavaPlugin;

public class HTeleport extends JavaPlugin {

    private ConfigManager configManager;
    private SpawnManager spawnManager;
    private WarpManager warpManager;
    private DeathManager deathManager;
    private TeleportManager teleportManager;
    private TpaManager tpaManager;
    private HomeManager homeManager;
    private RtpManager rtpManager;

    @Override
    public void onEnable() {
        // Khởi tạo Config Manager
        this.configManager = new ConfigManager(this);

        // Khởi tạo các Manager
        this.spawnManager = new SpawnManager(this, configManager);
        this.warpManager = new WarpManager(this, configManager);
        this.deathManager = new DeathManager(this, configManager);
        this.teleportManager = new TeleportManager(this, configManager);
        this.tpaManager = new TpaManager(this, configManager, teleportManager);
        this.homeManager = new HomeManager(this, configManager);
        this.rtpManager = new RtpManager(this, configManager, teleportManager);

        // Đăng ký Commands và Listeners
        registerCommands();
        registerListeners();

        getLogger().info("hTeleport v" + getDescription().getVersion() + " đã kích hoạt thành công!"
                + (isFolia() ? " (chế độ Folia)" : ""));
    }

    @Override
    public void onDisable() {
        // Dữ liệu được lưu bất đồng bộ (gom lại vài trăm ms) nên phải ghi nốt mọi thay đổi còn dang dở
        if (homeManager != null) homeManager.flush();
        if (warpManager != null) warpManager.flush();
        if (deathManager != null) deathManager.flush();
        if (spawnManager != null) spawnManager.flush();

        getLogger().info("hTeleport đã hủy kích hoạt!");
    }

    // Chỉ dùng để hiển thị log: mọi tác vụ lập lịch đã dùng API chuẩn của Paper nên chạy được cả trên Paper lẫn Folia
    private static boolean isFolia() {
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    private void registerCommands() {
        getCommand("setspawn").setExecutor(new SetSpawnCommand(spawnManager, configManager));
        getCommand("spawn").setExecutor(new SpawnCommand(spawnManager, teleportManager, configManager));

        WarpCommand warpCommand = new WarpCommand(warpManager, teleportManager, configManager);
        getCommand("warp").setExecutor(warpCommand);
        getCommand("warp").setTabCompleter(warpCommand);

        getCommand("back").setExecutor(new BackCommand(deathManager, teleportManager, configManager));

        TpaCommand tpaCommand = new TpaCommand(tpaManager, configManager);
        getCommand("tpa").setExecutor(tpaCommand);
        getCommand("tpa").setTabCompleter(tpaCommand);

        TpHereCommand tpHereCommand = new TpHereCommand(tpaManager, configManager);
        getCommand("tphere").setExecutor(tpHereCommand);
        getCommand("tphere").setTabCompleter(tpHereCommand);

        getCommand("tpall").setExecutor(new TpAllCommand(teleportManager, configManager));
        getCommand("tpaccept").setExecutor(new TpAcceptCommand(tpaManager, configManager));
        getCommand("tpdeny").setExecutor(new TpDenyCommand(tpaManager, configManager));
        getCommand("tpcancel").setExecutor(new TpCancelCommand(tpaManager, configManager));

        // Lệnh Home & Management
        HomeCommand homeCommand = new HomeCommand(this, homeManager, teleportManager, configManager);
        getCommand("home").setExecutor(homeCommand);
        getCommand("home").setTabCompleter(homeCommand);

        SetHomeCommand setHomeCommand = new SetHomeCommand(homeManager, configManager);
        getCommand("sethome").setExecutor(setHomeCommand);
        getCommand("sethome").setTabCompleter(setHomeCommand);

        DelHomeCommand delHomeCommand = new DelHomeCommand(homeManager, configManager);
        getCommand("delhome").setExecutor(delHomeCommand);
        getCommand("delhome").setTabCompleter(delHomeCommand);

        getCommand("homes").setExecutor(homeCommand);

        // Đăng ký lệnh RTP
        RtpCommand rtpCommand = new RtpCommand(rtpManager, configManager);
        getCommand("rtp").setExecutor(rtpCommand);
        getCommand("rtp").setTabCompleter(rtpCommand);

        // Lệnh Plugin Admin (/htlp help, /htlp reload)
        HTeleportCommand hTeleportCommand = new HTeleportCommand(this, configManager);
        getCommand("hteleport").setExecutor(hTeleportCommand);
        getCommand("hteleport").setTabCompleter(hTeleportCommand);
    }

    private void registerListeners() {
        getServer().getPluginManager().registerEvents(
                new PlayerListener(this, spawnManager, homeManager, deathManager, teleportManager, configManager),
                this
        );

        getServer().getPluginManager().registerEvents(
                new HomeListener(this, homeManager, teleportManager, configManager),
                this
        );
    }

    // Getters
    public ConfigManager getConfigManager() {
        return configManager;
    }

    public HomeManager getHomeManager() {
        return homeManager;
    }

    public TeleportManager getTeleportManager() {
        return teleportManager;
    }

    public SpawnManager getSpawnManager() {
        return spawnManager;
    }

    public WarpManager getWarpManager() {
        return warpManager;
    }

    public RtpManager getRtpManager() {
        return rtpManager;
    }
}