package hu.kxtsoo.mobspawner.database.data;

import hu.kxtsoo.mobspawner.database.DatabaseManager;

import java.sql.SQLException;
import java.util.Collections;
import java.util.List;

public class ToplistCache {

    private List<PlayerStat> topKills = Collections.emptyList();
    private List<PlayerStat> topDamage = Collections.emptyList();
    private long lastUpdate = 0;

    private final long updateIntervalMillis;

    public ToplistCache(long updateIntervalMillis) {
        this.updateIntervalMillis = updateIntervalMillis;
    }

    public synchronized List<PlayerStat> getTopKills() {
        updateCacheIfNeeded();
        return topKills;
    }

    public synchronized List<PlayerStat> getTopDamage() {
        updateCacheIfNeeded();
        return topDamage;
    }

    private void updateCacheIfNeeded() {
        if (System.currentTimeMillis() - lastUpdate >= updateIntervalMillis) {
            try {
                topKills = DatabaseManager.getTopPlayerStat("kills", 10);
                topDamage = DatabaseManager.getTopPlayerStat("damage", 10);
                lastUpdate = System.currentTimeMillis();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
}
