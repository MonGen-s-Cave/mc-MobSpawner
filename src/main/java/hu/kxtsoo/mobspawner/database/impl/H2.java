package hu.kxtsoo.mobspawner.database.impl;

import hu.kxtsoo.mobspawner.database.DatabaseInterface;
import hu.kxtsoo.mobspawner.database.data.PlayerStat;
import hu.kxtsoo.mobspawner.model.Mob;
import hu.kxtsoo.mobspawner.model.PlayerData;
import hu.kxtsoo.mobspawner.model.Spawner;
import org.bukkit.Location;
import org.bukkit.plugin.java.JavaPlugin;
import org.h2.jdbc.JdbcConnection;

import java.io.File;
import java.sql.*;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public class H2 implements DatabaseInterface {

    private final JavaPlugin plugin;
    private Connection connection;

    public H2(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void initialize() throws SQLException {
        try {
            File dataFolder = plugin.getDataFolder();
            if (!dataFolder.exists()) {
                dataFolder.mkdirs();
            }

            String url = "jdbc:h2:" + new File(dataFolder, "data").getAbsolutePath() + ";mode=MySQL";
            Properties props = new Properties();
            connection = new JdbcConnection(url, props, null, null, false);

            connection.setAutoCommit(true);

            createTables();
        } catch (SQLException e) {
            throw new RuntimeException("Could not connect to the H2 database", e);
        }
    }

    @Override
    public Connection getConnection() {
        return connection;
    }

    @Override
    public void createTables() throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS mobspawners_spawners (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "spawner_name VARCHAR(255) NOT NULL, " +
                    "owner_uuid VARCHAR(36) NOT NULL, " +
                    "placed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                    "world_name VARCHAR(255) NOT NULL, " +
                    "x DOUBLE NOT NULL, " +
                    "y DOUBLE NOT NULL, " +
                    "z DOUBLE NOT NULL" +
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
    public List<String> getAllMobUUIDs() throws SQLException {
        return List.of();
    }

    @Override
    public List<String> getMobUUIDsBySpawnerType(String spawnerType) throws SQLException {
        return List.of();
    }

    @Override
    public void clearAllMobs() throws SQLException {

    }

    @Override
    public void clearMobsBySpawnerType(String spawnerType) throws SQLException {

    }

    @Override
    public void clearMobsBySpawnerLocation(Location location) throws SQLException {

    }

    @Override
    public PlayerData getPlayerData(String playerUuid) throws SQLException {
        return null;
    }

    @Override
    public void savePlayerData(PlayerData playerData) throws SQLException {

    }

    @Override
    public List<PlayerStat> getTopPlayerStat(String statType, int limit) throws SQLException {
        return List.of();
    }


    @Override
    public void close() throws SQLException {
        if (connection != null && !connection.isClosed()) {
            connection.close();
        }
    }
}