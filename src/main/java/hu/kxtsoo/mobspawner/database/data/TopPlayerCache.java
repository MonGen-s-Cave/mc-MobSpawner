package hu.kxtsoo.mobspawner.database.data;

import hu.kxtsoo.mobspawner.MobSpawner;
import hu.kxtsoo.mobspawner.database.DatabaseManager;
import hu.kxtsoo.mobspawner.model.PlayerData;
import org.bukkit.Bukkit;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TopPlayerCache {

    private static final Map<String, List<PlayerData>> topCache = new HashMap<>();

    public static void refreshCache(MobSpawner plugin) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                topCache.put("damage", DatabaseManager.getTopPlayersByDamage(10));
                topCache.put("kills", DatabaseManager.getTopPlayersByKills(10));
            } catch (SQLException e) {
                plugin.getLogger().severe("Failed to refresh top cache: " + e.getMessage());
            }
        });
    }

    public static List<PlayerData> getTopPlayers(String statType) {
        return topCache.getOrDefault(statType, new ArrayList<>());
    }
}
