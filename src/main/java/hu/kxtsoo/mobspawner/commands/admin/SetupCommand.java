package hu.kxtsoo.mobspawner.commands.admin;

import dev.triumphteam.cmd.bukkit.annotation.Permission;
import dev.triumphteam.cmd.core.BaseCommand;
import dev.triumphteam.cmd.core.annotation.Command;
import dev.triumphteam.cmd.core.annotation.SubCommand;
import hu.kxtsoo.mobspawner.guis.SetupGUI;
import hu.kxtsoo.mobspawner.manager.SetupModeManager;
import org.bukkit.entity.Player;

@Command("mobspawner")
@Permission("mcave.admin")
public class SetupCommand extends BaseCommand {

    private final SetupGUI setupGUI;
    private final SetupModeManager setupModeManager;

    public SetupCommand(SetupGUI setupGUI, SetupModeManager setupModeManager) {
        this.setupGUI = setupGUI;
        this.setupModeManager = setupModeManager;
    }

    @SubCommand("setup")
    public void openSetupMenu(Player player) {
        if (setupModeManager.isInSetupMode(player)) {
            setupModeManager.toggleSetupMode(player);
        } else {
            setupGUI.openMenu(player);
        }
    }
}
