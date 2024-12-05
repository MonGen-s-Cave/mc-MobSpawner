package hu.kxtsoo.mobspawner.commands.admin;

import dev.triumphteam.cmd.bukkit.annotation.Permission;
import dev.triumphteam.cmd.core.BaseCommand;
import dev.triumphteam.cmd.core.annotation.Command;
import dev.triumphteam.cmd.core.annotation.SubCommand;
import hu.kxtsoo.mobspawner.database.DatabaseManager;
import hu.kxtsoo.mobspawner.util.ConfigUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

@Command("mobspawner")
@Permission("mobspawner.admin")
public class KillAllCommand extends BaseCommand {
    private final ConfigUtil configUtil;

    public KillAllCommand(ConfigUtil configUtil) {
        this.configUtil = configUtil;
    }

    @SubCommand("killall")
    @Permission("mobspawner.admin.killall")
    public void killAllMobs(CommandSender sender, String spawnerType) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(configUtil.getMessage("messages.only-player"));
            return;
        }

        Player player = (Player) sender;

        if (spawnerType.equals("*")) {
            killAllMobsGlobally(player);
        } else {
            killMobsBySpawnerType(player, spawnerType);
        }
    }

    private void killAllMobsGlobally(Player player) {
        try {
            List<String> mobUUIDs = DatabaseManager.getAllMobUUIDs();

            for (String mobUUID : mobUUIDs) {
                LivingEntity entity = (LivingEntity) Bukkit.getEntity(UUID.fromString(mobUUID));
                if (entity != null) {
                    entity.remove();
                }
            }

            DatabaseManager.clearAllMobs();
            player.sendMessage(configUtil.getMessage("messages.killall-command.deleted-all-mobs"));
        } catch (SQLException e) {
            e.printStackTrace();
            player.sendMessage(configUtil.getMessage("messages.database-error"));
        }
    }

    private void killMobsBySpawnerType(Player player, String spawnerType) {
        try {
            List<String> mobUUIDs = DatabaseManager.getMobUUIDsBySpawnerType(spawnerType);

            for (String mobUUID : mobUUIDs) {
                LivingEntity entity = (LivingEntity) Bukkit.getEntity(UUID.fromString(mobUUID));
                if (entity != null) {
                    entity.remove();
                }
            }

            DatabaseManager.clearMobsBySpawnerType(spawnerType);
            player.sendMessage(configUtil.getMessage("messages.killall-command.deleted-mobs"));
        } catch (SQLException e) {
            e.printStackTrace();
            player.sendMessage(configUtil.getMessage("messages.database-error"));
        }
    }
}