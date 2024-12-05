package hu.kxtsoo.mobspawner.manager;

import dev.triumphteam.cmd.bukkit.BukkitCommandManager;
import hu.kxtsoo.mobspawner.commands.admin.SetupCommand;
import hu.kxtsoo.mobspawner.guis.SetupGUI;
import hu.kxtsoo.mobspawner.util.ConfigUtil;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

public class CommandManager {
    private final BukkitCommandManager<CommandSender> commandManager;
    private ConfigUtil configUtil;
    private final SpawnerManager spawnerManager;
    private final SetupGUI setupGUI;
    private final SetupModeManager setupModeManager;

    public CommandManager(JavaPlugin plugin, ConfigUtil configUtil, SpawnerManager spawnerManager, SetupGUI setupGUI, SetupModeManager setupModeManager) {
        this.commandManager = BukkitCommandManager.create(plugin);
        this.configUtil = configUtil;
        this.spawnerManager = spawnerManager;
        this.setupGUI = setupGUI;
        this.setupModeManager = setupModeManager;
    }

    public void registerSuggestions() {}

    public void registerCommands() {
        commandManager.registerCommand(new SetupCommand(setupGUI, setupModeManager));
    }
}