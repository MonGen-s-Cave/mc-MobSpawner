package hu.kxtsoo.mobspawner.database.data;

public class PlayerStat {
    private final String uuid;
    private final double value;

    public PlayerStat(String uuid, double value) {
        this.uuid = uuid;
        this.value = value;
    }

    public String getUuid() {
        return uuid;
    }

    public double getValue() {
        return value;
    }
}
