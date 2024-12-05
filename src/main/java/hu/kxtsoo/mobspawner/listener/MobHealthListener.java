package hu.kxtsoo.mobspawner.listener;

import hu.kxtsoo.mobspawner.MobSpawner;
import hu.kxtsoo.mobspawner.database.DatabaseManager;
import hu.kxtsoo.mobspawner.model.Mob;
import hu.kxtsoo.mobspawner.model.PlayerData;
import hu.kxtsoo.mobspawner.util.ChatUtil;
import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;

import java.sql.SQLException;
import java.util.Objects;

public class MobHealthListener implements Listener {

    private final MobSpawner plugin;

    public MobHealthListener(MobSpawner plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof LivingEntity entity)) return;

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                Mob.MobLevel mobLevel = DatabaseManager.getMobLevelByUUID(entity.getUniqueId().toString());
                if (mobLevel == null) return;

                if (event instanceof EntityDamageByEntityEvent damageByEntityEvent) {
                    if (damageByEntityEvent.getDamager() instanceof Player player) {
                        double damage = event.getDamage();

                        try {
                            PlayerData playerData = DatabaseManager.getPlayerData(player.getUniqueId().toString());
                            playerData.incrementDamageDealt((long) damage);
                            DatabaseManager.savePlayerData(playerData);
                        } catch (SQLException e) {
                            plugin.getLogger().severe("[mc-MobSpawner] Failed to save player damage for UUID: " + player.getUniqueId());
                        }
                    }
                }

                Bukkit.getScheduler().runTask(plugin, () -> updateDisplayName(entity, mobLevel));
            } catch (SQLException e) {
                plugin.getLogger().severe("[mc-MobSpawner] Failed to fetch mob level for UUID: " + entity.getUniqueId());
            }
        });
    }

    @EventHandler
    public void onEntityHeal(EntityRegainHealthEvent event) {
        if (!(event.getEntity() instanceof LivingEntity entity)) return;

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                Mob.MobLevel mobLevel = DatabaseManager.getMobLevelByUUID(entity.getUniqueId().toString());
                if (mobLevel == null) return;

                Bukkit.getScheduler().runTask(plugin, () -> updateDisplayName(entity, mobLevel));
            } catch (SQLException e) {
                plugin.getLogger().severe("[mc-MobSpawner] Failed to fetch mob level for UUID: " + entity.getUniqueId());
            }
        });
    }

    private void updateDisplayName(LivingEntity entity, Mob.MobLevel mobLevel) {
        if (entity == null || mobLevel == null) return;

        double currentHealth = Math.max(0, entity.getHealth());
        double maxHealth = Objects.requireNonNull(entity.getAttribute(Attribute.GENERIC_MAX_HEALTH)).getValue();

        String updatedName = mobLevel.getName()
                .replace("%current_health%", String.format("%.1f", currentHealth))
                .replace("%max_health%", String.format("%.1f", maxHealth));

        entity.setCustomName(ChatUtil.colorizeHex(updatedName));
        entity.setCustomNameVisible(true);
    }
}