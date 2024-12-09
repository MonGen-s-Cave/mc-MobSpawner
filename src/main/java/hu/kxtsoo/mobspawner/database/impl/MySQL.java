package hu.kxtsoo.mobspawner.database.impl;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import dev.dejvokep.boostedyaml.YamlDocument;
import hu.kxtsoo.mobspawner.MobSpawner;
import hu.kxtsoo.mobspawner.database.DatabaseInterface;
import hu.kxtsoo.mobspawner.database.data.PlayerStat;
import hu.kxtsoo.mobspawner.model.Mob;
import hu.kxtsoo.mobspawner.model.PlayerData;
import hu.kxtsoo.mobspawner.model.Spawner;
import hu.kxtsoo.mobspawner.util.ConfigUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MySQL implements DatabaseInterface {

    private final ConfigUtil configUtil;
    private final JavaPlugin plugin;
    private HikariDataSource dataSource;

    public MySQL(ConfigUtil configUtil, JavaPlugin plugin) {
        this.configUtil = configUtil;
        this.plugin = plugin;
    }

    @Override
    public void initialize() throws SQLException {
        HikariConfig hikariConfig = new HikariConfig();

        String host = configUtil.getConfig().getString("storage.host", "localhost");
        String port = configUtil.getConfig().getString("storage.port", "3306");
        String database = configUtil.getConfig().getString("storage.name", "database_name");
        String username = configUtil.getConfig().getString("storage.username", "root");
        String password = configUtil.getConfig().getString("storage.password", "");

        String jdbcUrl = "jdbc:mysql://" + host + ":" + port + "/" + database + "?useSSL=false";
        hikariConfig.setJdbcUrl(jdbcUrl);
        hikariConfig.setUsername(username);
        hikariConfig.setPassword(password);

        hikariConfig.setMaximumPoolSize(configUtil.getConfig().getInt("storage.pool.maximumPoolSize", 10));
        hikariConfig.setMinimumIdle(configUtil.getConfig().getInt("storage.pool.minimumIdle", 5));
        hikariConfig.setConnectionTimeout(configUtil.getConfig().getInt("storage.pool.connectionTimeout", 30000));
        hikariConfig.setMaxLifetime(configUtil.getConfig().getInt("storage.pool.maxLifetime", 1800000));
        hikariConfig.setIdleTimeout(configUtil.getConfig().getInt("storage.pool.idleTimeout", 600000));

        dataSource = new HikariDataSource(hikariConfig);
        createTables();
    }

    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    public void createTables() throws SQLException {
        try (Connection connection = getConnection(); Statement statement = connection.createStatement()) {
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS mobspawner_spawners (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "spawner_name VARCHAR(255) NOT NULL, " +
                    "owner_uuid VARCHAR(36) NOT NULL, " +
                    "placed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                    "world_name VARCHAR(255) NOT NULL, " +
                    "x DOUBLE NOT NULL, " +
                    "y DOUBLE NOT NULL, " +
                    "z DOUBLE NOT NULL, " +
                    "UNIQUE KEY `idx_spawner_location` (`spawner_name`, `world_name`, `x`, `y`, `z`)" +
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
                    "CONSTRAINT fk_spawner FOREIGN KEY (spawner_name, spawner_world, spawner_x, spawner_y, spawner_z) " +
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
        try (Connection connection = getConnection();
             PreparedStatement ps = connection.prepareStatement(query)) {
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

        try (Connection connection = getConnection();
             PreparedStatement ps = connection.prepareStatement(query);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String spawnerName = rs.getString("spawner_name");
                String worldName = rs.getString("world_name");
                double x = rs.getDouble("x");
                double y = rs.getDouble("y");
                double z = rs.getDouble("z");

                Location location = new Location(plugin.getServer().getWorld(worldName), x, y, z);

                YamlDocument spawnerConfig = configUtil.getSpawnerConfig(spawnerName);
                if (spawnerConfig == null) {
                    plugin.getLogger().warning("Spawner configuration not found for: " + spawnerName);
                    continue;
                }

                String type = spawnerConfig.getString("spawner.type", "INVISIBLE");
                int spawnRate = spawnerConfig.getInt("spawner.spawn-rate", 30);
                int maxMobs = spawnerConfig.getInt("spawner.conditions.max-mobs", 5);
                String mobType = spawnerConfig.getString("mob.type", "zombie");
                int mobLevel = 1;
                String mobCustomName = "";
                int radius = spawnerConfig.getInt("spawner.conditions.radius", 5);
                int totalMaxMobs = spawnerConfig.getInt("spawner.conditions.total-max-mobs", 10);

                spawners.add(new Spawner(spawnerName, type, spawnRate, maxMobs, location, mobType, mobLevel, mobCustomName, radius, totalMaxMobs));
            }
        }
        return spawners;
    }

    @Override
    public void removeSpawner(Location location) throws SQLException {
        String query = "DELETE FROM mobspawner_spawners WHERE world_name = ? AND x = ? AND y = ? AND z = ?";
        try (Connection connection = getConnection();
             PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setString(1, location.getWorld().getName());
            ps.setDouble(2, location.getX());
            ps.setDouble(3, location.getY());
            ps.setDouble(4, location.getZ());
            ps.executeUpdate();
        }
    }

    public void saveMob(String mobUuid, String spawnerName, Location location, String mobType, int mobLevel) throws SQLException {
        String query = "INSERT INTO mobspawner_mobs (mob_uuid, spawner_name, spawner_world, spawner_x, spawner_y, spawner_z, mob_type, mob_level) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection connection = getConnection();
             PreparedStatement ps = connection.prepareStatement(query)) {
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

    public Mob.MobLevel getMobLevelByUUID(String mobUuid) throws SQLException {
        String query = "SELECT mob_type, mob_level FROM mobspawner_mobs WHERE mob_uuid = ?";
        try (Connection connection = getConnection();
             PreparedStatement ps = connection.prepareStatement(query)) {
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

    public void removeMob(String mobUuid) throws SQLException {
        String query = "DELETE FROM mobspawner_mobs WHERE mob_uuid = ?";
        try (Connection connection = getConnection();
             PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setString(1, mobUuid);
            ps.executeUpdate();
        }
    }

    public int getMobCountForSpawner(String spawnerName, Location location) throws SQLException {
        String query = "SELECT COUNT(*) AS mob_count FROM mobspawner_mobs " +
                "WHERE spawner_name = ? AND spawner_world = ? AND spawner_x = ? AND spawner_y = ? AND spawner_z = ?";
        try (Connection connection = getConnection();
             PreparedStatement ps = connection.prepareStatement(query)) {
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

    public String getSpawnerNameForMob(String mobUuid) throws SQLException {
        String query = "SELECT spawner_name FROM mobspawner_mobs WHERE mob_uuid = ?";
        try (Connection connection = getConnection();
             PreparedStatement ps = connection.prepareStatement(query)) {
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
        try (Connection connection = getConnection();
             PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setString(1, mobUuid);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String worldName = rs.getString("spawner_world");
                    double x = rs.getDouble("spawner_x");
                    double y = rs.getDouble("spawner_y");
                    double z = rs.getDouble("spawner_z");
                    return new Location(Bukkit.getWorld(worldName), x, y, z);
                }
            }
        }
        return null;
    }

    public List<String> getMobUUIDsForSpawner(Location spawnerLocation) throws SQLException {
        List<String> mobUUIDs = new ArrayList<>();
        String query = "SELECT mob_uuid FROM mobspawner_mobs WHERE spawner_world = ? AND spawner_x = ? AND spawner_y = ? AND spawner_z = ?";
        try (Connection connection = getConnection();
             PreparedStatement ps = connection.prepareStatement(query)) {
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

    public PlayerData getPlayerData(String playerUuid) throws SQLException {
        String query = "SELECT mobs_killed, damage_dealt FROM mobspawner_players WHERE uuid = ?";
        try (Connection connection = getConnection();
             PreparedStatement ps = connection.prepareStatement(query)) {
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
        String query = "INSERT INTO mobspawner_players (uuid, mobs_killed, damage_dealt) " +
                "VALUES (?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE mobs_killed = ?, damage_dealt = ?";
        try (Connection connection = getConnection();
             PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setString(1, playerData.getUuid());
            ps.setInt(2, playerData.getMobsKilled());
            ps.setLong(3, playerData.getDamageDealt());
            ps.setInt(4, playerData.getMobsKilled());
            ps.setLong(5, playerData.getDamageDealt());
            ps.executeUpdate();
        }
    }

    public List<PlayerStat> getTopPlayerStat(String statType, int limit) throws SQLException {
        String query;
        switch (statType) {
            case "damage":
                query = "SELECT uuid, damage_dealt AS value FROM mobspawner_players ORDER BY damage_dealt DESC LIMIT ?";
                break;
            case "kills":
                query = "SELECT uuid, mobs_killed AS value FROM mobspawner_players ORDER BY mobs_killed DESC LIMIT ?";
                break;
            default:
                throw new IllegalArgumentException("Invalid stat type: " + statType);
        }

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, limit);
            try (ResultSet rs = stmt.executeQuery()) {
                List<PlayerStat> topPlayers = new ArrayList<>();
                while (rs.next()) {
                    topPlayers.add(new PlayerStat(rs.getString("uuid"), rs.getDouble("value")));
                }
                return topPlayers;
            }
        }
    }

    public List<String> getAllMobUUIDs() throws SQLException {
        String query = "SELECT mob_uuid FROM mobspawner_mobs";
        List<String> mobUUIDs = new ArrayList<>();

        try (Connection connection = getConnection();
             PreparedStatement ps = connection.prepareStatement(query);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                mobUUIDs.add(rs.getString("mob_uuid"));
            }
        }

        return mobUUIDs;
    }

    public List<String> getMobUUIDsBySpawnerType(String spawnerType) throws SQLException {
        String query = "SELECT mob_uuid FROM mobspawner_mobs WHERE spawner_name = ?";
        List<String> mobUUIDs = new ArrayList<>();

        try (Connection connection = getConnection();
             PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setString(1, spawnerType);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    mobUUIDs.add(rs.getString("mob_uuid"));
                }
            }
        }

        return mobUUIDs;
    }

    public void clearAllMobs() throws SQLException {
        String query = "DELETE FROM mobspawner_mobs";

        try (Connection connection = getConnection();
             PreparedStatement ps = connection.prepareStatement(query)) {
            ps.executeUpdate();
        }
    }

    public void clearMobsBySpawnerType(String spawnerType) throws SQLException {
        String query = "DELETE FROM mobspawner_mobs WHERE spawner_name = ?";

        try (Connection connection = getConnection();
             PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setString(1, spawnerType);
            ps.executeUpdate();
        }
    }

    public void clearMobsBySpawnerLocation(Location location) throws SQLException {
        String query = "DELETE FROM mobspawner_mobs WHERE spawner_world = ? AND spawner_x = ? AND spawner_y = ? AND spawner_z = ?";

        try (Connection connection = getConnection();
             PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setString(1, location.getWorld().getName());
            ps.setDouble(2, location.getX());
            ps.setDouble(3, location.getY());
            ps.setDouble(4, location.getZ());
            ps.executeUpdate();
        }
    }

    @Override
    public void close() {
        if (dataSource != null) {
            dataSource.close();
        }
    }
}