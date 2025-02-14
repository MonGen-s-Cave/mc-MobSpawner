package hu.kxtsoo.mobspawner.database;

import hu.kxtsoo.mobspawner.database.data.PlayerStat;
import hu.kxtsoo.mobspawner.model.Mob;
import hu.kxtsoo.mobspawner.model.PlayerData;
import hu.kxtsoo.mobspawner.model.Spawner;
import org.bukkit.Location;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public interface DatabaseInterface {
    void initialize() throws SQLException;

    void createTables() throws SQLException;

    // Spawner handling
    void saveSpawner(String spawnerName, String ownerUuid, String worldName, double x, double y, double z) throws SQLException;
    List<Spawner> loadSpawners() throws SQLException;
    void removeSpawner(Location location) throws SQLException;

    // Mob handling
    void saveMob(String mobUuid, String spawnerName, Location location, String mobType, int mobLevel) throws SQLException;
    int getMobCountForSpawner(String spawnerName, Location location) throws SQLException;
    void removeMob(String mobUuid) throws SQLException;
    String getSpawnerNameForMob(String mobUuid) throws SQLException;
    Location getSpawnerLocationForMob(String mobUuid) throws SQLException;
    Mob.MobLevel getMobLevelByUUID(String mobUuid) throws SQLException;
    List<String> getMobUUIDsForSpawner(Location spawnerLocation) throws SQLException;
    List<String> getAllMobUUIDs() throws SQLException;
    List<String> getMobUUIDsBySpawnerType(String spawnerType) throws SQLException;
    void clearAllMobs() throws SQLException;
    void clearMobsBySpawnerType(String spawnerType) throws SQLException;
    void clearMobsBySpawnerLocation(Location location) throws SQLException;

    // PlayerData handling
    PlayerData getPlayerData(String playerUuid) throws SQLException;
    void savePlayerData(PlayerData playerData) throws SQLException;
    List<PlayerStat> getTopPlayerStat(String statType, int limit) throws SQLException;

    void resetPlayerDamage(String playerUuid) throws SQLException;
    void resetPlayerKills(String playerUuid) throws SQLException;
    void resetPlayerStats(String playerUuid) throws SQLException;
    void resetAllPlayersDamage() throws SQLException;
    void resetAllPlayersKills() throws SQLException;
    void resetAllPlayersStats() throws SQLException;

    Connection getConnection() throws SQLException;

    void close() throws SQLException;
}