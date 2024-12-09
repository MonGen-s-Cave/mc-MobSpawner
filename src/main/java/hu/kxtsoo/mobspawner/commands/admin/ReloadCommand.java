package hu.kxtsoo.mobspawner.commands.admin;

import dev.triumphteam.cmd.bukkit.annotation.Permission;
import dev.triumphteam.cmd.core.BaseCommand;
import dev.triumphteam.cmd.core.annotation.Command;
import dev.triumphteam.cmd.core.annotation.SubCommand;
import hu.kxtsoo.mobspawner.util.ConfigUtil;
import org.bukkit.command.CommandSender;

@Command("mobspawner")
@Permission("mobspawner.admin")
public class ReloadCommand extends BaseCommand {
    private final ConfigUtil configUtil;

    public ReloadCommand(ConfigUtil configUtil) {
        this.configUtil = configUtil;
    }

    @SubCommand("reload")
    @Permission("mobspawner.admin.reload")
    public void reload(CommandSender sender) {
        configUtil.reloadConfigs();
        sender.sendMessage(configUtil.getMessage("messages.reload-command.success"));
    }
}
