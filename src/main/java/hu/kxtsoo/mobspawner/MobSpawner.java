package hu.kxtsoo.mobspawner;

import hu.kxtsoo.mobspawner.database.DatabaseManager;
import hu.kxtsoo.mobspawner.hooks.HookManager;
import hu.kxtsoo.mobspawner.hooks.impl.PlaceholderAPIHandler;
import hu.kxtsoo.mobspawner.listener.MobHealthListener;
import hu.kxtsoo.mobspawner.listener.PlayerInteractListener;
import hu.kxtsoo.mobspawner.listener.MobDeathListener;
import hu.kxtsoo.mobspawner.guis.SetupGUI;
import hu.kxtsoo.mobspawner.manager.CommandManager;
import hu.kxtsoo.mobspawner.manager.MobManager;
import hu.kxtsoo.mobspawner.manager.SetupModeManager;
import hu.kxtsoo.mobspawner.manager.SpawnerManager;
import hu.kxtsoo.mobspawner.util.ConfigUtil;
import hu.kxtsoo.mobspawner.util.UpdateChecker;
import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.SQLException;
import java.util.logging.Level;

public final class MobSpawner extends JavaPlugin {

    private static MobSpawner instance;
    private ConfigUtil configUtil;
    private SpawnerManager spawnerManager;
    private MobManager mobManager;
    private SetupModeManager setupModeManager;
    private SetupGUI setupGUI;
    private HookManager hookManager;

    @Override
    public void onEnable() {
        instance = this;

        int pluginId = 23963;
        new Metrics(this, pluginId);

        configUtil = new ConfigUtil(this);
        configUtil.setupConfig();
        reloadConfig();

        try {
            DatabaseManager.initialize(configUtil, this);
        } catch (SQLException e) {
            getLogger().log(Level.SEVERE, "Failed to initialize database", e);
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        mobManager = new MobManager(this, configUtil);

        hookManager = new HookManager(this, configUtil);
        hookManager.registerHooks();

        spawnerManager = new SpawnerManager(this, configUtil, mobManager);
        setupModeManager = new SetupModeManager(this, configUtil, spawnerManager);
        setupGUI = new SetupGUI(configUtil, setupModeManager);

        getServer().getPluginManager().registerEvents(new MobDeathListener(mobManager, this), this);
        getServer().getPluginManager().registerEvents(new PlayerInteractListener(setupModeManager, configUtil, setupGUI), this);
        getServer().getPluginManager().registerEvents(new MobHealthListener(this), this);

        CommandManager commandManager = new CommandManager(this, configUtil, spawnerManager, setupGUI, setupModeManager);
        commandManager.registerSuggestions();
        commandManager.registerCommands();

       if (getConfig().getBoolean("update-checker.enabled", true)) {
            new UpdateChecker(this, configUtil, 6940);
       }

        long cacheUpdateIntervalMillis = getConfig().getLong("hooks.settings.PlaceholderAPI.update-interval-seconds", 300) * 1000;
        new PlaceholderAPIHandler(this, cacheUpdateIntervalMillis).register();

        String orange = "\u001B[38;2;206;96;18m";
        String reset = "\u001B[0m";
        String yellow = "\u001B[33m";
        String software = getServer().getName();
        String version = getServer().getVersion();

        System.out.println(" ");
        System.out.println(orange + "   __  __   ___   ____  ____   ____    _ __        __ _   _  _____  ____" + reset);
        System.out.println(orange + "   |  \\/  | / _ \\ | __ )/ ___| |  _ \\  / \\\\ \\      / /| \\ | || ____||  _ \\" + reset);
        System.out.println(orange + "   | |\\/| || | | ||  _ \\\\___ \\ | |_) |/ _ \\\\ \\ /\\ / / |  \\| ||  _|  | |_) |" + reset);
        System.out.println(orange + "   | |  | || |_| || |_) |___) ||  __// ___ \\\\ V  V /  | |\\  || |___ |  _ < " + reset);
        System.out.println(orange + "   |_|  |_| \\___/ |____/|____/ |_|  /_/   \\_\\\\_/\\_/   |_| \\_||_____||_| \\_\\" + reset);
        System.out.println(" ");
        System.out.println(orange + "   The plugin successfully started." + reset);
        System.out.println(orange + "   mc-MobSpawner " + software + " " + version + reset);
        System.out.println(yellow + "   Discord @ dc.mongenscave.com" + reset);
        System.out.println(" ");

    }

    @Override
    public void onDisable() {
        try {
            DatabaseManager.close();
        } catch (SQLException e) {
            getLogger().log(Level.SEVERE, "Failed to close database", e);
        }
    }

    public static MobSpawner getInstance() {
        return instance;
    }

    public ConfigUtil getConfigUtil() {
        return configUtil;
    }

    public SpawnerManager getSpawnerManager() {
        return spawnerManager;
    }

    public MobManager getMobManager() {
        return mobManager;
    }
}
