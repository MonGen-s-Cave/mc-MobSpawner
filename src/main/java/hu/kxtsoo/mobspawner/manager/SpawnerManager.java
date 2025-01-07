package hu.kxtsoo.mobspawner.manager;

import dev.dejvokep.boostedyaml.YamlDocument;
import hu.kxtsoo.mobspawner.database.DatabaseManager;
import hu.kxtsoo.mobspawner.model.Mob;
import hu.kxtsoo.mobspawner.model.Mob.MobLevel;
import hu.kxtsoo.mobspawner.model.Spawner;
import hu.kxtsoo.mobspawner.util.ChatUtil;
import hu.kxtsoo.mobspawner.util.ConfigUtil;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.sql.SQLException;
import java.util.*;

public class SpawnerManager {
    private final JavaPlugin plugin;
    private final ConfigUtil configUtil;
    private final MobManager mobManager;

    private final List<Spawner> spawners = new ArrayList<>();
    private final Map<Location, Spawner> activeSpawners = new HashMap<>();
    private final Map<Location, SchedulerManager.Task> spawnerTasks = new HashMap<>();

    public SpawnerManager(JavaPlugin plugin, ConfigUtil configUtil, MobManager mobManager) {
        this.plugin = plugin;
        this.configUtil = configUtil;
        this.mobManager = mobManager;

        loadSpawners();
        startSpawnerTasks();
    }

    public void loadSpawners() {
        try {
            List<Spawner> loadedSpawners = DatabaseManager.loadSpawners();
            spawners.clear();
            spawners.addAll(loadedSpawners);
            for (Spawner spawner : loadedSpawners) {
                activeSpawners.put(spawner.getLocation(), spawner);
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Error loading spawners: " + e.getMessage());
        }
    }

    private void startSpawnerTasks() {
        for (Spawner spawner : spawners) {
            startSpawnerTask(spawner);
        }
    }

    public void startSpawnerTask(Spawner spawner) {
        SchedulerManager.Task task = SchedulerManager.runTimer(() -> {
            Location location = spawner.getLocation();
            if (location.getWorld() == null) {
                stopSpawnerTask(location);
                return;
            }

            YamlDocument spawnerConfig = configUtil.getSpawnerConfig(spawner.getName());
            if (spawnerConfig == null) {
                plugin.getLogger().warning("No configuration found for spawner: " + spawner.getName());
                return;
            }

            boolean chunkRequiredLoaded = spawnerConfig.getBoolean("spawner.conditions.require-chunk-loaded", true);
            boolean playerCheckEnabled = spawnerConfig.getBoolean("spawner.conditions.player-radius-check.enabled", false);
            int playerRadius = spawnerConfig.getInt("spawner.conditions.player-radius-check.radius", 5);

            if (chunkRequiredLoaded && !location.isChunkLoaded()) {
                return;
            }

            if ((playerCheckEnabled && !isPlayerNearby(location, playerRadius)) ||
                    getNearbyMobsCount(location, spawner.getRadius(), spawner.getMobType()) >= spawner.getMaxMobs()) {
                return;
            }

            SchedulerManager.runAsync(() -> {
                try {
                    int totalMobs = DatabaseManager.getMobCountForSpawner(spawner.getName(), location);
                    if (totalMobs < spawner.getTotalMaxMobs()) {
                        SchedulerManager.run(() -> spawnMob(spawner));
                    }
                } catch (SQLException e) {
                    plugin.getLogger().severe("Error checking global mob limit: " + e.getMessage());
                }
            });
        }, 0, spawner.getSpawnRate() * 20L);

        spawnerTasks.put(spawner.getLocation(), task);
    }

    private boolean isPlayerNearby(Location location, int radius) {
        if (location.getWorld() == null) {
            plugin.getLogger().warning("[MobSpawner] World is null for location: " + location);
            return false;
        }

        return location.getWorld().getPlayers().stream()
                .anyMatch(player -> {
                    double distanceSquared = player.getLocation().distanceSquared(location);
                    return distanceSquared <= radius * radius;
                });
    }


    private int getNearbyMobsCount(Location location, int radius, String spawnerName) {
        return (int) location.getWorld().getNearbyEntities(location, radius, radius, radius).stream()
                .filter(entity -> entity instanceof LivingEntity)
                .filter(entity -> entity.getType() == EntityType.valueOf(spawnerName.toUpperCase()))
                .filter(entity -> isWithinRadius(entity.getLocation(), location, radius))
                .count();
    }

    private boolean isWithinRadius(Location mobLocation, Location spawnerLocation, int radius) {
        return mobLocation.getWorld().equals(spawnerLocation.getWorld()) &&
                mobLocation.distance(spawnerLocation) <= radius;
    }

    private void spawnMob(Spawner spawner) {
        SchedulerManager.runAsync(() -> {
            try {
                int totalMobs = DatabaseManager.getMobCountForSpawner(spawner.getName(), spawner.getLocation());
                if (totalMobs >= spawner.getTotalMaxMobs()) {
                    return;
                }

                SchedulerManager.run(() -> {
                    try {
                        Mob mob = getMobFromConfig(spawner.getMobType());
                        if (mob == null) {
                            return;
                        }

                        MobLevel mobLevel = mob.getRandomLevel();
                        if (mobLevel == null) {
                            return;
                        }

                        LivingEntity entity = (LivingEntity) spawner.getLocation().getWorld()
                                .spawnEntity(spawner.getLocation(), EntityType.valueOf(spawner.getMobType().toUpperCase()));

                        configureMobAttributes(entity, mobLevel);

                        SchedulerManager.runAsync(() -> {
                            try {
                                DatabaseManager.saveMob(
                                        entity.getUniqueId().toString(),
                                        spawner.getName(),
                                        spawner.getLocation(),
                                        mob.getType(),
                                        mobLevel.getLevel()
                                );
                            } catch (SQLException e) {
                                plugin.getLogger().severe("Error saving mob to database: " + e.getMessage());
                            }
                        });

                    } catch (Exception e) {
                        plugin.getLogger().severe("Error during mob spawning: " + e.getMessage());
                    }
                });
            } catch (SQLException e) {
                plugin.getLogger().severe("Error checking global mob limit: " + e.getMessage());
            }
        });
    }

    private Mob getMobFromConfig(String mobType) {
        YamlDocument mobConfig = configUtil.getMobConfig(mobType);
        if (mobConfig == null) {
            return null;
        }

        if (!mobConfig.contains("mob.levels")) {
            return null;
        }

        var levelsSection = mobConfig.getSection("mob.levels");
        if (levelsSection == null) {
            return null;
        }

        Mob mob = new Mob(mobType);

        try {
            levelsSection.getRoutesAsStrings(false).forEach(levelKey -> {
                String basePath = "mob.levels." + levelKey;

                int level = Integer.parseInt(levelKey);
                int health = mobConfig.getInt(basePath + ".health");
                int damage = mobConfig.getInt(basePath + ".damage");
                int armor = mobConfig.getInt(basePath + ".armor");
                double spawnChance = mobConfig.getDouble(basePath + ".spawn-chance", 100.0);
                String displayName = mobConfig.getString(basePath + ".display-name", "Mob");
                String belowName = mobConfig.getString(basePath + ".below-name", "");

                ItemStack helmet = configUtil.getItemStackFromConfig(mobConfig, basePath + ".equipment.helmet");
                double helmetDropChance = mobConfig.getDouble(basePath + ".equipment.helmet.drop-chance", 0.0);

                ItemStack weapon = configUtil.getItemStackFromConfig(mobConfig, basePath + ".equipment.weapon");
                double weaponDropChance = mobConfig.getDouble(basePath + ".equipment.weapon.drop-chance", 0.0);

                List<PotionEffect> effects = new ArrayList<>();
                if (mobConfig.contains(basePath + ".effects")) {
                    var effectsSection = mobConfig.getSection(basePath + ".effects");
                    if (effectsSection != null) {
                        effectsSection.getRoutesAsStrings(false).forEach(effectPath -> {
                            String effectName = mobConfig.getString(basePath + ".effects." + effectPath + ".effect");
                            int effectLevel = mobConfig.getInt(basePath + ".effects." + effectPath + ".level") - 1;
                            int duration = mobConfig.getInt(basePath + ".effects." + effectPath + ".duration") * 20;
                            effects.add(new PotionEffect(Objects.requireNonNull(PotionEffectType.getByName(effectName.toUpperCase())), duration, effectLevel));
                        });
                    }
                }

                List<String> rewards = mobConfig.getStringList(basePath + ".rewards");

                mob.addLevel(level, health, damage, armor, spawnChance, displayName, helmet, helmetDropChance, weapon, weaponDropChance, effects, rewards, belowName);
            });
        } catch (Exception e) {
            plugin.getLogger().severe("Error loading mob levels: " + e.getMessage());
        }

        return mob;
    }

    private void configureMobAttributes(LivingEntity entity, MobLevel mobLevel) {
        if (entity.getAttribute(Attribute.GENERIC_MAX_HEALTH) != null) {
            entity.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(mobLevel.getHealth());
            entity.setHealth(mobLevel.getHealth());
        }

        if (mobLevel.getName() != null && !mobLevel.getName().isEmpty()) {
            updateDisplayName(entity, mobLevel);
        }

        if (entity.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE) != null) {
            entity.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).setBaseValue(mobLevel.getDamage());
        }

        if (entity.getAttribute(Attribute.GENERIC_ARMOR) != null) {
            entity.getAttribute(Attribute.GENERIC_ARMOR).setBaseValue(mobLevel.getArmor());
        }

        if (mobLevel.getHelmet() != null) {
            entity.getEquipment().setHelmet(mobLevel.getHelmet());
            entity.getEquipment().setHelmetDropChance((float) mobLevel.getHelmetDropChance());
        }

        if (mobLevel.getWeapon() != null) {
            entity.getEquipment().setItemInMainHand(mobLevel.getWeapon());
            entity.getEquipment().setItemInMainHandDropChance((float) mobLevel.getWeaponDropChance());
        }

        mobLevel.getEffects().forEach(entity::addPotionEffect);
    }

    private void updateDisplayName(LivingEntity entity, Mob.MobLevel mobLevel) {
        if (entity == null || mobLevel == null || mobLevel.getName() == null) return;

        double currentHealth = Math.max(0, entity.getHealth());
        double maxHealth = entity.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();

        String displayName = mobLevel.getName()
                .replace("%current_health%", String.format("%.1f", currentHealth))
                .replace("%max_health%", String.format("%.1f", maxHealth));

        entity.setCustomName(ChatUtil.colorizeHex(displayName));
        entity.setCustomNameVisible(true);
    }

    public Map<Location, Spawner> getActiveSpawners() {
        return activeSpawners;
    }

    public List<Spawner> getSpawners() {
        return new ArrayList<>(spawners);
    }

    public void stopSpawnerTask(Location location) {
        SchedulerManager.Task task = spawnerTasks.remove(location);
        if (task != null) {
            task.cancel();
        }
    }

    public void removeSpawner(Location location) {
        stopSpawnerTask(location);

        Spawner spawner = activeSpawners.remove(location);
        if (spawner != null) {
            spawners.removeIf(existingSpawner -> existingSpawner.getLocation().equals(location));
        }
    }
}