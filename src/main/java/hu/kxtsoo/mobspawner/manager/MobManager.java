package hu.kxtsoo.mobspawner.manager;

import hu.kxtsoo.mobspawner.model.Mob;
import hu.kxtsoo.mobspawner.util.ConfigUtil;
import dev.dejvokep.boostedyaml.YamlDocument;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

public class MobManager {
    private final JavaPlugin plugin;
    private final ConfigUtil configUtil;
    private final Map<String, Mob> mobs = new HashMap<>();

    public MobManager(JavaPlugin plugin, ConfigUtil configUtil) {
        this.plugin = plugin;
        this.configUtil = configUtil;
        loadMobs();
    }

    private void loadMobs() {
        for (String mobName : configUtil.getMobConfigs().keySet()) {
            YamlDocument mobConfig = configUtil.getMobConfig(mobName);
            Mob mob = new Mob(mobName);

            for (String levelKey : mobConfig.getSection("mob.levels").getRoutesAsStrings(false)) {
                int level = Integer.parseInt(levelKey);
                int health = mobConfig.getInt("mob.levels." + level + ".health", 20);
                int damage = mobConfig.getInt("mob.levels." + level + ".damage", 5);
                int armor = mobConfig.getInt("mob.levels." + level + ".armor", 0);
                double spawnChance = mobConfig.getDouble("mob.levels." + level + ".spawn-chance", 100.0);
                String name = mobConfig.getString("mob.levels." + level + ".display-name", "Mob");
                String belowname = mobConfig.getString("mob.levels." + level + ".below-name");

                ItemStack helmet = configUtil.getItemStackFromConfig(mobConfig, "mob.levels." + level + ".equipment.helmet");
                double helmetDropChance = mobConfig.getDouble("mob.levels." + level + ".equipment.helmet-drop-chance", 0.0);
                ItemStack weapon = configUtil.getItemStackFromConfig(mobConfig, "mob.levels." + level + ".equipment.weapon");
                double weaponDropChance = mobConfig.getDouble("mob.levels." + level + ".equipment.weapon-drop-chance", 0.0);

                List<PotionEffect> effects = loadPotionEffects(mobConfig, "mob.levels." + level + ".effects");
                List<String> rewards = mobConfig.getStringList("mob.levels." + level + ".rewards");

                mob.addLevel(level, health, damage, armor, spawnChance, name, helmet, helmetDropChance, weapon, weaponDropChance, effects, rewards, belowname);
            }
            mobs.put(mobName, mob);
        }
    }

    private List<PotionEffect> loadPotionEffects(YamlDocument mobConfig, String path) {
        List<PotionEffect> effects = new ArrayList<>();
        List<Map<String, Object>> effectList = (List<Map<String, Object>>) mobConfig.get(path);

        if (effectList != null) {
            for (Map<String, Object> effectMap : effectList) {
                String effectType = (String) effectMap.get("effect");
                int level = (int) effectMap.getOrDefault("level", 1);
                int duration = (int) effectMap.getOrDefault("duration", 99999);

                PotionEffectType potionEffectType = PotionEffectType.getByName(effectType.toUpperCase());
                if (potionEffectType != null) {
                    effects.add(new PotionEffect(potionEffectType, duration, level - 1));
                }
            }
        }
        return effects;
    }

    public Mob getMobByType(String type) {
        if (type == null || type.isEmpty()) {
            plugin.getLogger().warning("[mc-MobSpawner] Mob type is null or empty.");
            return null;
        }
        return mobs.get(type.toLowerCase());
    }
}