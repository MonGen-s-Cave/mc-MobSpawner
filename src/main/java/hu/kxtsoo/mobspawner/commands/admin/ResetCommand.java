package hu.kxtsoo.mobspawner.commands.admin;

import dev.triumphteam.cmd.bukkit.annotation.Permission;
import dev.triumphteam.cmd.core.BaseCommand;
import dev.triumphteam.cmd.core.annotation.Command;
import dev.triumphteam.cmd.core.annotation.SubCommand;
import dev.triumphteam.cmd.core.annotation.Suggestion;
import hu.kxtsoo.mobspawner.database.DatabaseManager;
import hu.kxtsoo.mobspawner.manager.SchedulerManager;
import hu.kxtsoo.mobspawner.util.ConfigUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.sql.SQLException;
import java.util.UUID;

@Command("mobspawner")
@Permission("mobspawner.admin")
public class ResetCommand extends BaseCommand {

    private final ConfigUtil configUtil;

    public ResetCommand(ConfigUtil configUtil) {
        this.configUtil = configUtil;
    }

    @SubCommand("reset")
    @Permission("mobspawner.admin.reset")
    public void resetStats(CommandSender sender, @Suggestion("reset_types") String statType, @Suggestion("online_players_with_*") String target) {
        if (!sender.hasPermission("mobspawner.admin.reset")) {
            sender.sendMessage(configUtil.getMessage("messages.no-permission"));
            return;
        }

        if (target.equals("*")) {
            resetAllPlayersStats(sender, statType);
        } else {
            Player targetPlayer = Bukkit.getPlayerExact(target);
            if (targetPlayer == null) {
                sender.sendMessage(configUtil.getMessage("messages.player-not-found").replace("%player%", target));
                return;
            }
            UUID playerUUID = targetPlayer.getUniqueId();
            resetSinglePlayerStats(sender, statType, playerUUID, target);
        }
    }

    private void resetSinglePlayerStats(CommandSender sender, String statType, UUID playerUUID, String playerName) {
        switch (statType.toLowerCase()) {
            case "damage":
                resetDamage(sender, playerUUID, playerName);
                break;
            case "kills":
                resetKills(sender, playerUUID, playerName);
                break;
            case "*":
                resetAllStats(sender, playerUUID, playerName);
                break;
            default:
                sender.sendMessage(configUtil.getMessage("messages.invalid-reset-type"));
                break;
        }
    }

    private void resetDamage(CommandSender sender, UUID playerUUID, String playerName) {
        SchedulerManager.runAsync(() -> {
            try {
                DatabaseManager.resetPlayerDamage(playerUUID.toString());
                SchedulerManager.run(() ->
                        sender.sendMessage(configUtil.getMessage("messages.reset.damage").replace("%player%", playerName))
                );
            } catch (SQLException e) {
                e.printStackTrace();
                SchedulerManager.run(() ->
                        sender.sendMessage(configUtil.getMessage("messages.database-error"))
                );
            }
        });
    }

    private void resetKills(CommandSender sender, UUID playerUUID, String playerName) {
        SchedulerManager.runAsync(() -> {
            try {
                DatabaseManager.resetPlayerKills(playerUUID.toString());
                SchedulerManager.run(() ->
                        sender.sendMessage(configUtil.getMessage("messages.reset.kills").replace("%player%", playerName))
                );
            } catch (SQLException e) {
                e.printStackTrace();
                SchedulerManager.run(() ->
                        sender.sendMessage(configUtil.getMessage("messages.database-error"))
                );
            }
        });
    }

    private void resetAllStats(CommandSender sender, UUID playerUUID, String playerName) {
        SchedulerManager.runAsync(() -> {
            try {
                DatabaseManager.resetPlayerStats(playerUUID.toString());
                SchedulerManager.run(() ->
                        sender.sendMessage(configUtil.getMessage("messages.reset.all").replace("%player%", playerName))
                );
            } catch (SQLException e) {
                e.printStackTrace();
                SchedulerManager.run(() ->
                        sender.sendMessage(configUtil.getMessage("messages.database-error"))
                );
            }
        });
    }

    private void resetAllPlayersStats(CommandSender sender, String statType) {
        SchedulerManager.runAsync(() -> {
            try {
                switch (statType.toLowerCase()) {
                    case "damage":
                        DatabaseManager.resetAllPlayersDamage();
                        SchedulerManager.run(() ->
                                sender.sendMessage(configUtil.getMessage("messages.reset.all-players-damage"))
                        );
                        break;
                    case "kills":
                        DatabaseManager.resetAllPlayersKills();
                        SchedulerManager.run(() ->
                                sender.sendMessage(configUtil.getMessage("messages.reset.all-players-kills"))
                        );
                        break;
                    case "*":
                        DatabaseManager.resetAllPlayersStats();
                        SchedulerManager.run(() ->
                                sender.sendMessage(configUtil.getMessage("messages.reset.all-players"))
                        );
                        break;
                    default:
                        SchedulerManager.run(() ->
                                sender.sendMessage(configUtil.getMessage("messages.invalid-reset-type"))
                        );
                        break;
                }
            } catch (SQLException e) {
                e.printStackTrace();
                SchedulerManager.run(() ->
                        sender.sendMessage(configUtil.getMessage("messages.database-error"))
                );
            }
        });
    }
}
