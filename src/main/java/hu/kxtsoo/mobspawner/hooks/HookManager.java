package hu.kxtsoo.mobspawner.hooks;

import hu.kxtsoo.mobspawner.MobSpawner;
import hu.kxtsoo.mobspawner.hooks.impl.PlaceholderAPIHandler;
import hu.kxtsoo.mobspawner.util.ConfigUtil;
import org.bukkit.Bukkit;

public class HookManager {

    private final MobSpawner plugin;
    private final ConfigUtil configUtil;

    private PlaceholderAPIHandler placeholderAPIHandler;

    public HookManager(MobSpawner plugin, ConfigUtil configUtil) {
        this.plugin = plugin;
        this.configUtil = configUtil;
    }

    public void registerHooks() {
        if (configUtil.getHooks().getBoolean("hooks.register.PlaceholderAPI", true) &&
                Bukkit.getPluginManager().getPlugin("PlaceholderAPI").isEnabled()) {

            long cacheUpdateIntervalMillis = configUtil.getHooks().getLong("hooks.settings.PlaceholderAPI.update-interval-seconds", 300L) * 1000;
            placeholderAPIHandler = new PlaceholderAPIHandler(plugin, cacheUpdateIntervalMillis);
            placeholderAPIHandler.register();

            plugin.getLogger().info("\u001B[32m[Hook] PlaceholderAPI successfully enabled.\u001B[0m");
        }
    }

    public PlaceholderAPIHandler getPlaceholderAPIHandler() {
        return placeholderAPIHandler;
    }
}