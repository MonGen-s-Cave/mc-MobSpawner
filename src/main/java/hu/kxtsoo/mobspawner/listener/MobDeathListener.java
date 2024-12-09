package hu.kxtsoo.mobspawner.listener;

import hu.kxtsoo.mobspawner.database.DatabaseManager;
import hu.kxtsoo.mobspawner.manager.MobManager;
import hu.kxtsoo.mobspawner.model.Mob;
import hu.kxtsoo.mobspawner.model.PlayerData;
import hu.kxtsoo.mobspawner.util.ChatUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
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

        Mob.MobLevel mobLevel;
        try {
            mobLevel = DatabaseManager.getMobLevelByUUID(entity.getUniqueId().toString());
        } catch (SQLException e) {
            plugin.getLogger().severe("Error fetching mob level from database: " + e.getMessage());
            return;
        }

        if (mobLevel == null) return;

        Player killer = entity.getKiller();
        executeRewardCommands(mobLevel.getRewards(), killer, mobLevel.getName());

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                DatabaseManager.removeMob(entity.getUniqueId().toString());

                PlayerData playerData = DatabaseManager.getPlayerData(killer.getUniqueId().toString());
                playerData.setMobsKilled(playerData.getMobsKilled() + 1);
                DatabaseManager.savePlayerData(playerData);
            } catch (SQLException e) {
                plugin.getLogger().severe("Error removing mob from database: " + e.getMessage());
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
}