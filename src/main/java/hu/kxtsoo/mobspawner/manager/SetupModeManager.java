package hu.kxtsoo.mobspawner.manager;

import dev.dejvokep.boostedyaml.YamlDocument;
import hu.kxtsoo.mobspawner.database.DatabaseManager;
import hu.kxtsoo.mobspawner.model.Spawner;
import hu.kxtsoo.mobspawner.util.ChatUtil;
import hu.kxtsoo.mobspawner.util.ConfigUtil;
import org.bukkit.*;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.sql.SQLException;
import java.util.*;

public class SetupModeManager {

    private final JavaPlugin plugin;
    private final ConfigUtil configUtil;
    private final SpawnerManager spawnerManager;

    private final Map<Player, Boolean> setupModePlayers = new HashMap<>();
    private final Map<Player, ItemStack[]> savedInventories = new HashMap<>();
    private final Map<Player, GameMode> savedGameModes = new HashMap<>();
    private final Map<Player, String> selectedSpawner = new HashMap<>();
    private final Set<Player> viewAllMode = new HashSet<>();
    private final Map<Player, List<BukkitRunnable>> visibilityTasks = new HashMap<>();
    private final Map<Location, BukkitRunnable> spawnerEffectTasks = new HashMap<>();

    public SetupModeManager(JavaPlugin plugin, ConfigUtil configUtil, SpawnerManager spawnerManager) {
        this.plugin = plugin;
        this.configUtil = configUtil;
        this.spawnerManager = spawnerManager;
    }

    public void toggleSetupMode(Player player) {
        if (isInSetupMode(player)) {
            deactivateSetupMode(player);
        } else {
            activateSetupMode(player);
        }
    }

    public void activateSetupMode(Player player) {
        if (setupModePlayers.containsKey(player)) return;

        YamlDocument config = configUtil.getConfig();

        if (config.getBoolean("setup-mode.settings.restore-inventory", true)) {
            savedInventories.put(player, player.getInventory().getContents());
        }

        String gameMode = config.getString("setup-mode.settings.gamemode", "CREATIVE");
        try {
            player.setGameMode(GameMode.valueOf(gameMode.toUpperCase()));
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Invalid gamemode configuration: " + gameMode);
            player.setGameMode(GameMode.CREATIVE);
        }

        if (config.getBoolean("setup-mode.settings.night-vision", true)) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, Integer.MAX_VALUE, 0, false, false, false));
        }

        if (config.getBoolean("setup-mode.settings.flight", true)) {
            player.setAllowFlight(true);
            player.setFlying(true);
        }

        player.getInventory().clear();
        config.getSection("setup-mode.items").getRoutesAsStrings(false).forEach(itemKey -> {
            String basePath = "setup-mode.items." + itemKey;
            int slot = config.getInt(basePath + ".slot", -1);
            String type = config.getString(basePath + ".type", "STONE");
            String name = config.getString(basePath + ".name", "");
            List<String> lore = config.getStringList(basePath + ".lore");

            Material material = Material.matchMaterial(type.toUpperCase());
            if (material == null) {
                plugin.getLogger().warning("Invalid item type: " + type + " (" + itemKey + ")");
                return;
            }

            ItemStack item = new ItemStack(material);
            var meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(ChatUtil.colorizeHex(name));
                meta.setLore(ChatUtil.colorizeHex(lore));
                item.setItemMeta(meta);
            }

            if (slot >= 0 && slot < player.getInventory().getSize()) {
                player.getInventory().setItem(slot, item);
            } else {
                plugin.getLogger().warning("Invalid slot: " + slot + " (" + itemKey + ")");
            }
        });

        setupModePlayers.put(player, true);
        player.sendMessage(configUtil.getMessage("messages.setup-mode.activated"));
    }

    public void deactivateSetupMode(Player player) {
        if (!setupModePlayers.containsKey(player)) return;

        YamlDocument config = configUtil.getConfig();

        if (config.getBoolean("setup-mode.settings.restore-inventory", true)) {
            player.getInventory().clear();
            if (savedInventories.containsKey(player)) {
                player.getInventory().setContents(savedInventories.remove(player));
            }
        }

        if (config.getBoolean("setup-mode.settings.night-vision", true)) {
            player.removePotionEffect(PotionEffectType.NIGHT_VISION);
        }

        if (config.getBoolean("setup-mode.settings.flight", true)) {
            player.setAllowFlight(false);
            player.setFlying(false);
        }

        if (savedGameModes.containsKey(player)) {
            player.setGameMode(savedGameModes.remove(player));
        }

        if (viewAllMode.contains(player)) {
            viewAllMode.remove(player);
            spawnerEffectTasks.forEach((location, task) -> task.cancel());
            spawnerEffectTasks.clear();
        }

        setupModePlayers.remove(player);
        player.sendMessage(configUtil.getMessage("messages.setup-mode.deactivated"));
    }

    public void placeSpawner(Player player, Location location) {
        if (!isInSetupMode(player)) return;

        String selectedSpawnerType = getSelectedSpawner(player);
        if (selectedSpawnerType == null) {
            player.sendActionBar(configUtil.getMessage("messages.setup-mode.items-actionbar.spawner-not-selected"));
            return;
        }

        YamlDocument spawnerConfig = configUtil.getSpawnerConfig(selectedSpawnerType);
        if (spawnerConfig == null) {
            plugin.getLogger().warning("Spawner config not found for: " + selectedSpawnerType);
            return;
        }

        String type = spawnerConfig.getString("spawner.type", "VISIBLE").toUpperCase();
        int spawnRate = spawnerConfig.getInt("spawner.spawn-rate", 30);
        int maxMobs = spawnerConfig.getInt("spawner.conditions.max-mobs", 10);
        String mobType = spawnerConfig.getString("mob.type", "ZOMBIE");
        int mobLevel = spawnerConfig.getInt("mob-level", 1);
        String mobCustomName = spawnerConfig.getString("mob-custom-name", null);
        int radius = spawnerConfig.getInt("spawner.conditions.radius", 5);
        int totalMaxMobs = spawnerConfig.getInt("spawner.conditions.total-max-mobs", 10);

        Spawner spawner = new Spawner(selectedSpawnerType, type, spawnRate, maxMobs, location, mobType, mobLevel, mobCustomName, radius, totalMaxMobs);

        try {
            DatabaseManager.saveSpawner(
                    selectedSpawnerType,
                    player.getUniqueId().toString(),
                    location.getWorld().getName(),
                    location.getX(),
                    location.getY(),
                    location.getZ()
            );

            spawnerManager.getActiveSpawners().put(location, spawner);
            spawnerManager.getSpawners().add(spawner);
            spawnerManager.startSpawnerTask(spawner);

            player.sendActionBar(configUtil.getMessage("messages.setup-mode.items-actionbar.spawner-placed").replace("%spawner%", selectedSpawnerType));
        } catch (SQLException e) {
            plugin.getLogger().severe("Error saving spawner: " + e.getMessage());
        }

        if (viewAllMode.contains(player)) {
            YamlDocument config = configUtil.getConfig();
            String mode = config.getString("setup-mode.settings.spawner-visibility.mode", "PARTICLE").toUpperCase();

            if ("PARTICLE".equalsIgnoreCase(mode)) {
                BukkitRunnable task = createSpawnerParticleTask(location, player);
                task.runTaskTimer(plugin, 0, 10L);
                visibilityTasks.computeIfAbsent(player, k -> new ArrayList<>()).add(task);
            } else if ("MATERIAL".equalsIgnoreCase(mode)) {
                String materialType = config.getString("setup-mode.settings.spawner-visibility.type", "STONE").toUpperCase();
                try {
                    Material material = Material.valueOf(materialType);
                    player.sendBlockChange(location, material.createBlockData());
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Invalid material type in config: " + materialType);
                }
            }
        }
    }

    public boolean removeSpawner(Player player, Location location) {
        if (!isInSetupMode(player)) return false;

        try {
            boolean removed = spawnerManager.getActiveSpawners().remove(location) != null;

            if (removed) {
                removeSpawnerParticles(location);

                List<String> mobUUIDs = DatabaseManager.getMobUUIDsForSpawner(location);

                for (String mobUUID : mobUUIDs) {
                    LivingEntity entity = (LivingEntity) Bukkit.getEntity(UUID.fromString(mobUUID));
                    if (entity != null) {
                        entity.remove();
                    }
                }

                DatabaseManager.removeSpawner(location);
                spawnerManager.removeSpawner(location);

                player.sendBlockChange(location, Material.AIR.createBlockData());
                player.sendActionBar(configUtil.getMessage("messages.setup-mode.items-actionbar.spawner-removed"));

                if (viewAllMode.contains(player)) {
                    refreshGlobalSpawnerParticles(player);
                }

                return true;
            } else {
                player.sendActionBar(configUtil.getMessage("messages.setup-mode.items-actionbar.no-spawner-there"));
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Error deleting spawner: " + e.getMessage());
        }

        return false;
    }

    public void toggleSpawnerVisibility(Player player) {
        if (!isInSetupMode(player)) return;

        YamlDocument config = configUtil.getConfig();
        String mode = config.getString("setup-mode.settings.spawner-visibility.mode", "PARTICLE").toUpperCase();
        String type = config.getString("setup-mode.settings.spawner-visibility.type", "WAX_ON").toUpperCase();

        if (viewAllMode.contains(player)) {
            viewAllMode.remove(player);

            player.sendActionBar(ChatUtil.colorizeHex(configUtil.getMessage("messages.setup-mode.items-actionbar.visibility-deactivated")));

            spawnerEffectTasks.forEach((location, task) -> task.cancel());
            spawnerEffectTasks.clear();
        } else {
            viewAllMode.add(player);
            player.sendActionBar(ChatUtil.colorizeHex(configUtil.getMessage("messages.setup-mode.items-actionbar.visibility-activated")));

            spawnerManager.getActiveSpawners().forEach((location, spawner) -> {
                if ("PARTICLE".equalsIgnoreCase(mode)) {
                    BukkitRunnable task = createSpawnerParticleTask(location, player);
                    task.runTaskTimer(plugin, 0, 10L);
                    spawnerEffectTasks.put(location, task);
                } else if ("MATERIAL".equalsIgnoreCase(mode)) {
                    try {
                        Material material = Material.valueOf(type);
                        player.sendBlockChange(location, material.createBlockData());
                    } catch (IllegalArgumentException e) {
                        plugin.getLogger().warning("Invalid material type: " + type);
                    }
                }
            });
        }
    }

    private void refreshGlobalSpawnerParticles(Player player) {
        if (!visibilityTasks.containsKey(player)) return;

        visibilityTasks.get(player).forEach(BukkitRunnable::cancel);
        visibilityTasks.remove(player);

        createGlobalSpawnerParticleTask(player);
    }

    private void createGlobalSpawnerParticleTask(Player player) {
        BukkitRunnable task = new BukkitRunnable() {
            @Override
            public void run() {
                if (!isInSetupMode(player) || !viewAllMode.contains(player)) {
                    cancel();
                    return;
                }

                String type = configUtil.getConfig().getString("setup-mode.settings.spawner-visibility.type", "FLAME").toUpperCase();
                Particle particle;

                try {
                    particle = Particle.valueOf(type);
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Invalid particle: " + type);
                    cancel();
                    return;
                }

                spawnerManager.getActiveSpawners().values().forEach(spawner -> {
                    Location location = spawner.getLocation();
                    double x = location.getX();
                    double y = location.getY();
                    double z = location.getZ();

                    for (double t = 0; t <= 1; t += 0.1) {
                        player.spawnParticle(particle, x + t, y, z, 0);
                        player.spawnParticle(particle, x, y, z + t, 0);
                        player.spawnParticle(particle, x + t, y, z + 1, 0);
                        player.spawnParticle(particle, x + 1, y, z + t, 0);
                        player.spawnParticle(particle, x + t, y + 1, z, 0);
                        player.spawnParticle(particle, x, y + 1, z + t, 0);
                        player.spawnParticle(particle, x + t, y + 1, z + 1, 0);
                        player.spawnParticle(particle, x + 1, y + 1, z + t, 0);
                    }

                    for (double t = 0; t <= 1; t += 0.1) {
                        player.spawnParticle(particle, x, y + t, z, 0);
                        player.spawnParticle(particle, x + 1, y + t, z, 0);
                        player.spawnParticle(particle, x, y + t, z + 1, 0);
                        player.spawnParticle(particle, x + 1, y + t, z + 1, 0);
                    }
                });
            }
        };

        task.runTaskTimer(plugin, 0, 10L);
        visibilityTasks.computeIfAbsent(player, k -> new ArrayList<>()).add(task);
    }

    private BukkitRunnable createSpawnerParticleTask(Location location, Player player) {
        return new BukkitRunnable() {
            @Override
            public void run() {
                if (!isInSetupMode(player) || !viewAllMode.contains(player)) {
                    cancel();
                    return;
                }

                String type = configUtil.getConfig().getString("setup-mode.settings.spawner-visibility.type", "FLAME").toUpperCase();
                try {
                    Particle particle = Particle.valueOf(type);

                    double x = location.getX();
                    double y = location.getY();
                    double z = location.getZ();

                    for (double t = 0; t <= 1; t += 0.1) {
                        player.spawnParticle(particle, x + t, y, z, 0);
                        player.spawnParticle(particle, x, y, z + t, 0);
                        player.spawnParticle(particle, x + t, y, z + 1, 0);
                        player.spawnParticle(particle, x + 1, y, z + t, 0);
                        player.spawnParticle(particle, x + t, y + 1, z, 0);
                        player.spawnParticle(particle, x, y + 1, z + t, 0);
                        player.spawnParticle(particle, x + t, y + 1, z + 1, 0);
                        player.spawnParticle(particle, x + 1, y + 1, z + t, 0);
                    }

                    for (double t = 0; t <= 1; t += 0.1) {
                        player.spawnParticle(particle, x, y + t, z, 0);
                        player.spawnParticle(particle, x + 1, y + t, z, 0);
                        player.spawnParticle(particle, x, y + t, z + 1, 0);
                        player.spawnParticle(particle, x + 1, y + t, z + 1, 0);
                    }
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Invalid particle: " + type);
                    cancel();
                }
            }
        };
    }

    private void removeSpawnerParticles(Location location) {
        BukkitRunnable task = spawnerEffectTasks.remove(location);
        if (task != null) {
            task.cancel();
        }
    }

    public boolean isInSetupMode(Player player) {
        return setupModePlayers.getOrDefault(player, false);
    }

    public String getSelectedSpawner(Player player) {
        return selectedSpawner.getOrDefault(player, null);
    }

    public void setSelectedSpawner(Player player, String spawnerType) {
        selectedSpawner.put(player, spawnerType);
    }

    public Spawner getSpawnerAt(Location location) {
        return spawnerManager.getActiveSpawners().get(location);
    }
}