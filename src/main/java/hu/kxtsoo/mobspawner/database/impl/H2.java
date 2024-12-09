package hu.kxtsoo.mobspawner.database.impl;

import hu.kxtsoo.mobspawner.MobSpawner;
import hu.kxtsoo.mobspawner.database.DatabaseInterface;
import hu.kxtsoo.mobspawner.database.data.PlayerStat;
import hu.kxtsoo.mobspawner.model.Mob;
import hu.kxtsoo.mobspawner.model.PlayerData;
import hu.kxtsoo.mobspawner.model.Spawner;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.plugin.java.JavaPlugin;
import org.h2.jdbc.JdbcConnection;

import java.io.File;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class H2 implements DatabaseInterface {

    private final JavaPlugin plugin;
    private Connection connection;

    public H2(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void initialize() {
        try {
            File dataFolder = plugin.getDataFolder();
            if (!dataFolder.exists()) {
                dataFolder.mkdirs();
            }

            connection = new JdbcConnection("jdbc:h2:./" + MobSpawner.getInstance().getDataFolder() + "/data;mode=MySQL", new Properties(), null, null, false);
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

    public void createTables() throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS mobspawner_spawners (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "spawner_name VARCHAR(255) NOT NULL, " +
                    "owner_uuid VARCHAR(36) NOT NULL, " +
                    "placed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                    "world_name VARCHAR(255) NOT NULL, " +
                    "x DOUBLE NOT NULL, " +
                    "y DOUBLE NOT NULL, " +
                    "z DOUBLE NOT NULL, " +
                    "UNIQUE(spawner_name, world_name, x, y, z)" +
                    ");");

            statement.executeUpdate("CREATE TABLE IF NOT EXISTS mobspawner_mobs (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "mob_uuid VARCHAR(36) NOT NULL, " +
                    "spawner_name VARCHAR(255) NOT NULL, " +
                    "spawner_world VARCHAR(255) NOT NULL, " +
                    "spawner_x DOUBLE NOT NULL, " +
                    "spawner_y DOUBLE NOT NULL, " +
                    "spawner_z DOUBLE NOT NULL, " +
                    "mob_type VARCHAR(50) NOT NULL, " +
                    "mob_level INT NOT NULL, " +
                    "spawn_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                    "FOREIGN KEY (spawner_name, spawner_world, spawner_x, spawner_y, spawner_z) " +
                    "REFERENCES mobspawner_spawners(spawner_name, world_name, x, y, z) ON DELETE CASCADE" +
                    ");");

            statement.executeUpdate("CREATE TABLE IF NOT EXISTS mobspawner_players (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "uuid VARCHAR(36) UNIQUE NOT NULL, " +
                    "mobs_killed INT DEFAULT 0, " +
                    "damage_dealt BIGINT DEFAULT 0" +
                    ");");
        }
    }

    @Override
    public void saveSpawner(String spawnerName, String ownerUuid, String worldName, double x, double y, double z) throws SQLException {
        String query = "INSERT INTO mobspawner_spawners (spawner_name, owner_uuid, world_name, x, y, z) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setString(1, spawnerName);
            ps.setString(2, ownerUuid);
            ps.setString(3, worldName);
            ps.setDouble(4, x);
            ps.setDouble(5, y);
            ps.setDouble(6, z);
            ps.executeUpdate();
        }
    }

    @Override
    public List<Spawner> loadSpawners() throws SQLException {
        List<Spawner> spawners = new ArrayList<>();
        String query = "SELECT spawner_name, owner_uuid, world_name, x, y, z FROM mobspawner_spawners";

        try (PreparedStatement ps = connection.prepareStatement(query);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String spawnerName = rs.getString("spawner_name");
                String worldName = rs.getString("world_name");
                double x = rs.getDouble("x");
                double y = rs.getDouble("y");
                double z = rs.getDouble("z");

                Location location = new Location(Bukkit.getWorld(worldName), x, y, z);
                spawners.add(new Spawner(spawnerName, "", 0, 0, location, "", 0, "", 0, 0));
            }
        }
        return spawners;
    }

    @Override
    public void removeSpawner(Location location) throws SQLException {
        String query = "DELETE FROM mobspawner_spawners WHERE world_name = ? AND x = ? AND y = ? AND z = ?";
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setString(1, location.getWorld().getName());
            ps.setDouble(2, location.getX());
            ps.setDouble(3, location.getY());
            ps.setDouble(4, location.getZ());
            ps.executeUpdate();
        }
    }

    @Override
    public void saveMob(String mobUuid, String spawnerName, Location location, String mobType, int mobLevel) throws SQLException {
        String query = "INSERT INTO mobspawner_mobs (mob_uuid, spawner_name, spawner_world, spawner_x, spawner_y, spawner_z, mob_type, mob_level) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setString(1, mobUuid);
            ps.setString(2, spawnerName);
            ps.setString(3, location.getWorld().getName());
            ps.setDouble(4, location.getX());
            ps.setDouble(5, location.getY());
            ps.setDouble(6, location.getZ());
            ps.setString(7, mobType);
            ps.setInt(8, mobLevel);
            ps.executeUpdate();
        }
    }

    @Override
    public int getMobCountForSpawner(String spawnerName, Location location) throws SQLException {
        String query = "SELECT COUNT(*) AS mob_count FROM mobspawner_mobs " +
                "WHERE spawner_name = ? AND spawner_world = ? AND spawner_x = ? AND spawner_y = ? AND spawner_z = ?";
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setString(1, spawnerName);
            ps.setString(2, location.getWorld().getName());
            ps.setDouble(3, location.getX());
            ps.setDouble(4, location.getY());
            ps.setDouble(5, location.getZ());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("mob_count");
                }
            }
        }
        return 0;
    }

    @Override
    public void removeMob(String mobUuid) throws SQLException {
        String query = "DELETE FROM mobspawner_mobs WHERE mob_uuid = ?";
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setString(1, mobUuid);
            ps.executeUpdate();
        }
    }

    @Override
    public String getSpawnerNameForMob(String mobUuid) throws SQLException {
        String query = "SELECT spawner_name FROM mobspawner_mobs WHERE mob_uuid = ?";
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setString(1, mobUuid);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("spawner_name");
                }
            }
        }
        return null;
    }

    @Override
    public Location getSpawnerLocationForMob(String mobUuid) throws SQLException {
        String query = "SELECT spawner_world, spawner_x, spawner_y, spawner_z FROM mobspawner_mobs WHERE mob_uuid = ?";
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setString(1, mobUuid);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String worldName = rs.getString("spawner_world");
                    double x = rs.getDouble("spawner_x");
                    double y = rs.getDouble("spawner_y");
                    double z = rs.getDouble("spawner_z");
                    return new Location(plugin.getServer().getWorld(worldName), x, y, z);
                }
            }
        }
        return null;
    }

    @Override
    public PlayerData getPlayerData(String playerUuid) throws SQLException {
        String query = "SELECT mobs_killed, damage_dealt FROM mobspawner_players WHERE uuid = ?";
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setString(1, playerUuid);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int mobsKilled = rs.getInt("mobs_killed");
                    long damageDealt = rs.getLong("damage_dealt");
                    return new PlayerData(playerUuid, mobsKilled, damageDealt);
                }
            }
        }
        return new PlayerData(playerUuid, 0, 0);
    }

    public void savePlayerData(PlayerData playerData) throws SQLException {
        String query = "MERGE INTO mobspawner_players (uuid, mobs_killed, damage_dealt) " +
                "KEY (uuid) VALUES (?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setString(1, playerData.getUuid());
            ps.setInt(2, playerData.getMobsKilled());
            ps.setLong(3, playerData.getDamageDealt());
            ps.executeUpdate();
        }
    }

    @Override
    public List<PlayerStat> getTopPlayerStat(String statType, int limit) throws SQLException {
        String query;
        switch (statType) {
            case "damage":
                query = "SELECT uuid, damage_dealt AS stat_value FROM mobspawner_players ORDER BY damage_dealt DESC LIMIT ?";
                break;
            case "kills":
                query = "SELECT uuid, mobs_killed AS stat_value FROM mobspawner_players ORDER BY mobs_killed DESC LIMIT ?";
                break;
            default:
                throw new IllegalArgumentException("Invalid stat type: " + statType);
        }

        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, limit);
            try (ResultSet rs = stmt.executeQuery()) {
                List<PlayerStat> topPlayers = new ArrayList<>();
                while (rs.next()) {
                    topPlayers.add(new PlayerStat(rs.getString("uuid"), rs.getDouble("stat_value")));
                }
                return topPlayers;
            }
        }
    }

    @Override
    public Mob.MobLevel getMobLevelByUUID(String mobUuid) throws SQLException {
        String query = "SELECT mob_type, mob_level FROM mobspawner_mobs WHERE mob_uuid = ?";
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setString(1, mobUuid);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String mobType = rs.getString("mob_type");
                    int mobLevel = rs.getInt("mob_level");
                    Mob mob = MobSpawner.getInstance().getMobManager().getMobByType(mobType);
                    return mob != null ? mob.getLevel(mobLevel) : null;
                }
            }
        }
        return null;
    }

    @Override
    public List<String> getMobUUIDsForSpawner(Location spawnerLocation) throws SQLException {
        List<String> mobUUIDs = new ArrayList<>();
        String query = "SELECT mob_uuid FROM mobspawner_mobs WHERE spawner_world = ? AND spawner_x = ? AND spawner_y = ? AND spawner_z = ?";
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setString(1, spawnerLocation.getWorld().getName());
            ps.setDouble(2, spawnerLocation.getX());
            ps.setDouble(3, spawnerLocation.getY());
            ps.setDouble(4, spawnerLocation.getZ());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    mobUUIDs.add(rs.getString("mob_uuid"));
                }
            }
        }
        return mobUUIDs;
    }

    @Override
    public List<String> getAllMobUUIDs() throws SQLException {
        List<String> mobUUIDs = new ArrayList<>();
        String query = "SELECT mob_uuid FROM mobspawner_mobs";
        try (PreparedStatement ps = connection.prepareStatement(query);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                mobUUIDs.add(rs.getString("mob_uuid"));
            }
        }
        return mobUUIDs;
    }

    @Override
    public List<String> getMobUUIDsBySpawnerType(String spawnerType) throws SQLException {
        List<String> mobUUIDs = new ArrayList<>();
        String query = "SELECT mob_uuid FROM mobspawner_mobs WHERE spawner_name = ?";
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setString(1, spawnerType);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    mobUUIDs.add (rs.getString("mob_uuid"));
                }
            }
        }
        return mobUUIDs;
    }

    @Override
    public void clearAllMobs() throws SQLException {
        String query = "DELETE FROM mobspawner_mobs";
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.executeUpdate();
        }
    }

    @Override
    public void clearMobsBySpawnerType(String spawnerType) throws SQLException {
        String query = "DELETE FROM mobspawner_mobs WHERE spawner_name = ?";
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setString(1, spawnerType);
            ps.executeUpdate();
        }
    }

    @Override
    public void clearMobsBySpawnerLocation(Location location) throws SQLException {
        String query = "DELETE FROM mobspawner_mobs WHERE spawner_world = ? AND spawner_x = ? AND spawner_y = ? AND spawner_z = ?";
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setString(1, location.getWorld().getName());
            ps.setDouble(2, location.getX());
            ps.setDouble(3, location.getY());
            ps.setDouble(4, location.getZ());
            ps.executeUpdate();
        }
    }

    @Override
    public void close() throws SQLException {
        if (connection != null && !connection.isClosed()) {
            connection.close();
        }
    }
}