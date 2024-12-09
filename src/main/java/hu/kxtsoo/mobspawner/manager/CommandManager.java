package hu.kxtsoo.mobspawner.manager;

import dev.triumphteam.cmd.bukkit.BukkitCommandManager;
import dev.triumphteam.cmd.core.suggestion.SuggestionKey;
import hu.kxtsoo.mobspawner.commands.admin.KillAllCommand;
import hu.kxtsoo.mobspawner.commands.admin.ReloadCommand;
import hu.kxtsoo.mobspawner.commands.admin.SetupCommand;
import hu.kxtsoo.mobspawner.guis.SetupGUI;
import hu.kxtsoo.mobspawner.model.Spawner;
import hu.kxtsoo.mobspawner.util.ConfigUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

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

    public void registerSuggestions() {
        commandManager.registerSuggestion(SuggestionKey.of("online_players"), (sender, context) -> {
            if (sender instanceof Player) {
                return Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName)
                        .toList();
            }
            return List.of();
        });

        commandManager.registerSuggestion(SuggestionKey.of("killall_types"), (sender, context) -> {
            List<String> spawnerNames = spawnerManager.getSpawners().stream()
                    .map(Spawner::getName)
                    .toList();

            List<String> suggestions = new ArrayList<>(spawnerNames);
            suggestions.add("*");

            return suggestions;
        });
    }

    public void registerCommands() {
        commandManager.registerCommand(new SetupCommand(setupGUI, setupModeManager));
        commandManager.registerCommand(new KillAllCommand(configUtil));
        commandManager.registerCommand(new ReloadCommand(configUtil));
    }
}