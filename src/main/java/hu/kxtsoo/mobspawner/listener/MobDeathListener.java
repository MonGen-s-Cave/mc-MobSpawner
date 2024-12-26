package hu.kxtsoo.mobspawner.listener;

import com.destroystokyo.paper.event.entity.EntityRemoveFromWorldEvent;
import hu.kxtsoo.mobspawner.database.DatabaseManager;
import hu.kxtsoo.mobspawner.manager.MobManager;
import hu.kxtsoo.mobspawner.model.Mob;
import hu.kxtsoo.mobspawner.model.PlayerData;
import hu.kxtsoo.mobspawner.util.ChatUtil;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.SQLException;
import java.util.List;
import java.util.Random;

public class MobDeathListener implements Listener {

    private final MobManager mobManager;
    private final JavaPlugin plugin;
    private final Random random = new Random();

    public MobDeathListener(MobManager mobManager, JavaPlugin plugin) {
        this.mobManager = mobManager;
        this.plugin = plugin;
    }

    @EventHandler
    public void onMobDeath(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                Mob.MobLevel mobLevel = DatabaseManager.getMobLevelByUUID(entity.getUniqueId().toString());
                if (mobLevel == null) return;

                Player killer = entity.getKiller();
                if (killer == null) return;

                Bukkit.getScheduler().runTask(plugin, () -> executeRewardCommands(mobLevel.getRewards(), killer, mobLevel.getName()));

                try {
                    DatabaseManager.removeMob(entity.getUniqueId().toString());

                    PlayerData playerData = DatabaseManager.getPlayerData(killer.getUniqueId().toString());
                    playerData.setMobsKilled(playerData.getMobsKilled() + 1);
                    DatabaseManager.savePlayerData(playerData);
                } catch (SQLException e) {
                    plugin.getLogger().severe("Error updating database for mob death: " + e.getMessage());
                }

            } catch (SQLException e) {
                plugin.getLogger().severe("Error fetching mob level from database: " + e.getMessage());
            }
        });
    }

    private void executeRewardCommands(List<String> rewards, Player player, String mobName) {
        if (player == null) return;

        for (String reward : rewards) {
            String actionType = "[command]";
            double chance = 100.0;
            String command = reward;

            if (reward.matches("\\[(command|message|actionbar)] \\d+% .*")) {
                String[] parts = reward.split(" ", 3);
                actionType = parts[0];
                chance = Double.parseDouble(parts[1].replace("%", "").trim());
                command = parts[2];
            } else if (reward.startsWith("[command]") || reward.startsWith("[message]") || reward.startsWith("[actionbar]")) {
                actionType = reward.split(" ")[0];
                command = reward.replace(actionType, "").trim();
            }

            command = command.replace("%player%", player.getName()).replace("%mob%", mobName);

            if (random.nextDouble() * 100 <= chance) {
                switch (actionType) {
                    case "[command]":
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
                        break;
                    case "[message]":
                        player.sendMessage(ChatUtil.colorizeHex(command));
                        break;
                    case "[actionbar]":
                        player.sendActionBar(ChatUtil.colorizeHex(command));
                        break;
                    default:
                        plugin.getLogger().warning("Unknown reward type: " + actionType);
                }
            }
        }
    }

    @EventHandler
    public void onEntityRemove(EntityRemoveFromWorldEvent event) {
        String mobUuid = event.getEntity().getUniqueId().toString();

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                if (DatabaseManager.getAllMobUUIDs().contains(mobUuid)) {
                    DatabaseManager.removeMob(mobUuid);
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("Failed to remove mob from database asynchronously: " + mobUuid);
                e.printStackTrace();
            }
        });
    }

    @EventHandler
    public void onChunkUnload(ChunkUnloadEvent event) {
        Chunk chunk = event.getChunk();

        for (org.bukkit.entity.Entity entity : chunk.getEntities()) {
            if (entity instanceof LivingEntity) {
                String mobUuid = entity.getUniqueId().toString();

                Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                    try {
                        if (DatabaseManager.getAllMobUUIDs().contains(mobUuid)) {
                            DatabaseManager.removeMob(mobUuid);
                        }
                    } catch (SQLException e) {
                        plugin.getLogger().severe("Error handling chunk unload for UUID: " + mobUuid);
                        e.printStackTrace();
                    }
                });
            }
        }
    }
}