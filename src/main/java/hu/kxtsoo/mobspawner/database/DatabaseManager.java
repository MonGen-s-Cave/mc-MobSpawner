package hu.kxtsoo.mobspawner.database;

import hu.kxtsoo.mobspawner.database.impl.H2;
import hu.kxtsoo.mobspawner.database.impl.MySQL;
import hu.kxtsoo.mobspawner.database.impl.SQLite;
import hu.kxtsoo.mobspawner.model.Mob;
import hu.kxtsoo.mobspawner.model.PlayerData;
import hu.kxtsoo.mobspawner.model.Spawner;
import hu.kxtsoo.mobspawner.util.ConfigUtil;
import org.bukkit.Location;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public class DatabaseManager {

    private static DatabaseInterface database;

    public static void initialize(ConfigUtil configUtil, JavaPlugin plugin) throws SQLException {
        String driver = configUtil.getConfig().getString("storage.driver", "h2");
        switch (driver.toLowerCase()) {
            case "sqlite":
                database = new SQLite(plugin);
                database.initialize();
                break;
            case "mysql":
                database = new MySQL(configUtil, plugin);
                database.initialize();
                break;
            case "h2":
                database = new H2(plugin);
                database.initialize();
                break;
            default:
                throw new IllegalArgumentException("Unsupported database driver: " + driver);
        }

        database.createTables();
    }

    public static void saveSpawner(String spawnerName, String ownerUuid, String worldName, double x, double y, double z) throws SQLException {
        database.saveSpawner(spawnerName, ownerUuid, worldName, x, y, z);
    }

    public static List<Spawner> loadSpawners() throws SQLException {
        return database.loadSpawners();
    }

    public static void removeSpawner(Location location) throws SQLException {
        database.removeSpawner(location);
    }

    public static void saveMob(String mobUuid, String spawnerName, Location location, String mobType, int mobLevel) throws SQLException {
        database.saveMob(mobUuid, spawnerName, location, mobType, mobLevel);
    }

    public static int getMobCountForSpawner(String spawnerName, Location location) throws SQLException {
        return database.getMobCountForSpawner(spawnerName, location);
    }

    public static void removeMob(String mobUuid) throws SQLException {
        database.removeMob(mobUuid);
    }

    public static String getSpawnerNameForMob(String mobUuid) throws SQLException {
        return database.getSpawnerNameForMob(mobUuid);
    }

    public static Location getSpawnerLocationForMob(String mobUuid) throws SQLException {
        return database.getSpawnerLocationForMob(mobUuid);
    }

    public static Mob.MobLevel getMobLevelByUUID(String mobUuid) throws SQLException {
        return database.getMobLevelByUUID(mobUuid);
    }

    public static List<String> getMobUUIDsForSpawner(Location spawnerLocation) throws SQLException {
        return database.getMobUUIDsForSpawner(spawnerLocation);
    }

    public static PlayerData getPlayerData(String playerUuid) throws SQLException {
        return database.getPlayerData(playerUuid);
    }

    public static void savePlayerData(PlayerData playerData) throws SQLException {
        database.savePlayerData(playerData);
    }

    public static List<PlayerData> getTopPlayersByDamage(int limit) throws SQLException {
        return database.getTopPlayersByDamage(limit);
    }

    public static List<PlayerData> getTopPlayersByKills(int limit) throws SQLException {
        return database.getTopPlayersByKills(limit);
    }

    public static Connection getConnection() throws SQLException {
        if (database != null) {
            return database.getConnection();
        }
        throw new SQLException("Database is not initialized.");
    }

    public static void close() throws SQLException {
        if (database != null) {
            database.close();
        }
    }
}