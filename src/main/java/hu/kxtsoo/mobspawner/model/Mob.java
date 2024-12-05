package hu.kxtsoo.mobspawner.model;

import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;

import java.util.*;

public class Mob {
    private final String type;
    private final Map<Integer, MobLevel> levels = new HashMap<>();

    public Mob(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }

    public void addLevel(
            int level, int health, int damage, int armor, double spawnChance, String name,
            ItemStack helmet, double helmetDropChance, ItemStack weapon, double weaponDropChance,
            List<PotionEffect> effects, List<String> rewards, String belowName) {

        if (levels.containsKey(level)) {
            throw new IllegalArgumentException("Level " + level + " already exists for mob type: " + type);
        }

        MobLevel mobLevel = new MobLevel(level, health, damage, armor, spawnChance, name,
                helmet, helmetDropChance, weapon, weaponDropChance,
                effects, rewards, belowName);
        levels.put(level, mobLevel);
    }

    public MobLevel getLevel(int level) {
        return levels.get(level);
    }

    public MobLevel getRandomLevel() {
        double randomValue = new Random().nextDouble() * 100;
        double cumulativeChance = 0.0;

        for (MobLevel mobLevel : levels.values()) {
            cumulativeChance += mobLevel.getSpawnChance();
            if (randomValue <= cumulativeChance) {
                return mobLevel;
            }
        }

        return levels.values().iterator().next();
    }

    public static class MobLevel {
        private final int level;
        private final int health;
        private final int damage;
        private final int armor;
        private final double spawnChance;
        private final String name;
        private final ItemStack helmet;
        private final double helmetDropChance;
        private final ItemStack weapon;
        private final double weaponDropChance;
        private final List<PotionEffect> effects;
        private final List<String> rewards;
        private final String belowName;

        public MobLevel(int level, int health, int damage, int armor, double spawnChance, String name,
                        ItemStack helmet, double helmetDropChance, ItemStack weapon, double weaponDropChance,
                        List<PotionEffect> effects, List<String> rewards, String belowName) {
            this.level = level;
            this.health = health;
            this.damage = damage;
            this.armor = armor;
            this.spawnChance = spawnChance;
            this.name = name;
            this.helmet = helmet;
            this.helmetDropChance = helmetDropChance;
            this.weapon = weapon;
            this.weaponDropChance = weaponDropChance;
            this.effects = effects == null ? new ArrayList<>() : new ArrayList<>(effects);
            this.rewards = rewards == null ? new ArrayList<>() : new ArrayList<>(rewards);
            this.belowName = belowName;
        }

        public int getLevel() {
            return level;
        }

        public int getHealth() {
            return health;
        }

        public int getDamage() {
            return damage;
        }

        public int getArmor() {
            return armor;
        }

        public double getSpawnChance() {
            return spawnChance;
        }

        public String getName() {
            return name;
        }

        public ItemStack getHelmet() {
            return helmet;
        }

        public double getHelmetDropChance() {
            return helmetDropChance;
        }

        public ItemStack getWeapon() {
            return weapon;
        }

        public double getWeaponDropChance() {
            return weaponDropChance;
        }

        public List<PotionEffect> getEffects() {
            return Collections.unmodifiableList(effects);
        }

        public List<String> getRewards() {
            return Collections.unmodifiableList(rewards);
        }

        public String getBelowName() {
            return belowName;
        }
    }
}