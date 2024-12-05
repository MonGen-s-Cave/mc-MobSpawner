package hu.kxtsoo.mobspawner.model;

public class PlayerData {
    private final String uuid;
    private int mobsKilled;
    private long damageDealt;

    public PlayerData(String uuid, int mobsKilled, long damageDealt) {
        this.uuid = uuid;
        this.mobsKilled = mobsKilled;
        this.damageDealt = damageDealt;
    }

    public String getUuid() {
        return uuid;
    }

    public int getMobsKilled() {
        return mobsKilled;
    }

    public void setMobsKilled(int mobsKilled) {
        this.mobsKilled = mobsKilled;
    }

    public long getDamageDealt() {
        return damageDealt;
    }

    public void setDamageDealt(long damageDealt) {
        this.damageDealt = damageDealt;
    }

    public void incrementMobsKilled() {
        this.mobsKilled++;
    }

    public void incrementDamageDealt(long damage) {
        this.damageDealt += damage;
    }
}