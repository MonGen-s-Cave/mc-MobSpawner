package hu.kxtsoo.mobspawner.model;

import org.bukkit.Location;

public class Spawner {
    private final String name;
    private final String type;
    private final int spawnRate;
    private final int maxMobs;
    private final Location location;
    private final String mobType;
    private final int mobLevel;
    private final String mobCustomName;
    private final int radius;
    private final int totalMaxMobs;

    public Spawner(String name, String type, int spawnRate, int maxMobs, Location location, String mobType, int mobLevel, String mobCustomName, int radius, int totalMaxMobs) {
        this.name = name;
        this.type = type;
        this.spawnRate = spawnRate;
        this.maxMobs = maxMobs;
        this.location = location;
        this.mobType = mobType;
        this.mobLevel = mobLevel;
        this.mobCustomName = mobCustomName;
        this.radius = radius;
        this.totalMaxMobs = totalMaxMobs;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public int getSpawnRate() {
        return spawnRate;
    }

    public Location getLocation() {
        return location;
    }

    public String getMobType() {
        return mobType;
    }

    public int getMobLevel() {
        return mobLevel;
    }

    public String getMobCustomName() {
        return mobCustomName;
    }

    public int getMaxMobs() {
        return maxMobs;
    }

    public int getRadius() {
        return radius;
    }

    public int getTotalMaxMobs() {
        return totalMaxMobs;
    }
}
