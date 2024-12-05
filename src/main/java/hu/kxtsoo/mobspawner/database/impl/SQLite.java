package hu.kxtsoo.mobspawner.database.impl;

import hu.kxtsoo.mobspawner.database.DatabaseInterface;
import hu.kxtsoo.mobspawner.model.Mob;
import hu.kxtsoo.mobspawner.model.PlayerData;
import hu.kxtsoo.mobspawner.model.Spawner;
import org.bukkit.Location;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.sql.*;
import java.util.List;

public class SQLite implements DatabaseInterface {

    private final JavaPlugin plugin;
    private Connection connection;

    public SQLite(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void initialize() throws SQLException {
        File dataFolder = plugin.getDataFolder();
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }

        String url = "jdbc:sqlite:" + new File(dataFolder, "database.db").getAbsolutePath();
        connection = DriverManager.getConnection(url);

        createTables();
    }

    @Override
    public Connection getConnection() {
        return connection;
    }

    @Override
    public void createTables() throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS mobspawners_spawners (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "spawner_name TEXT NOT NULL, " +
                    "owner_uuid TEXT NOT NULL, " +
                    "placed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                    "world_name TEXT NOT NULL, " +
                    "x REAL NOT NULL, " +
                    "y REAL NOT NULL, " +
                    "z REAL NOT NULL" +
                    ");");
        }
    }

    @Override
    public void saveSpawner(String spawnerName, String ownerUuid, String worldName, double x, double y, double z) throws SQLException {

    }

    @Override
    public List<Spawner> loadSpawners() throws SQLException {
        return List.of();
    }

    @Override
    public void removeSpawner(Location location) throws SQLException {

    }

    @Override
    public void saveMob(String mobUuid, String spawnerName, Location location, String mobType, int mobLevel) throws SQLException {

    }

    @Override
    public int getMobCountForSpawner(String spawnerName, Location location) throws SQLException {
        return 0;
    }

    @Override
    public void removeMob(String mobUuid) throws SQLException {

    }

    @Override
    public String getSpawnerNameForMob(String mobUuid) throws SQLException {
        return "";
    }

    @Override
    public Location getSpawnerLocationForMob(String mobUuid) throws SQLException {
        return null;
    }

    @Override
    public Mob.MobLevel getMobLevelByUUID(String mobUuid) throws SQLException {
        return null;
    }

    @Override
    public List<String> getMobUUIDsForSpawner(Location spawnerLocation) throws SQLException {
        return List.of();
    }

    @Override
    public PlayerData getPlayerData(String playerUuid) throws SQLException {
        return null;
    }

    @Override
    public void savePlayerData(PlayerData playerData) throws SQLException {

    }

    @Override
    public List<PlayerData> getTopPlayersByDamage(int limit) throws SQLException {
        return List.of();
    }

    @Override
    public List<PlayerData> getTopPlayersByKills(int limit) throws SQLException {
        return List.of();
    }

    @Override
    public void close() throws SQLException {
        if (connection != null && !connection.isClosed()) {
            connection.close();
        }
    }
}