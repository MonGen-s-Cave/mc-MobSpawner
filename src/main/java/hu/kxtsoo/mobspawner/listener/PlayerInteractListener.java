package hu.kxtsoo.mobspawner.listener;

import hu.kxtsoo.mobspawner.guis.SetupGUI;
import hu.kxtsoo.mobspawner.manager.SetupModeManager;
import hu.kxtsoo.mobspawner.model.Spawner;
import hu.kxtsoo.mobspawner.util.ChatUtil;
import hu.kxtsoo.mobspawner.util.ConfigUtil;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Collections;
import java.util.List;

public class PlayerInteractListener implements Listener {

    private final SetupModeManager setupModeManager;
    private final ConfigUtil configUtil;
    private final SetupGUI setupGUI;

    public PlayerInteractListener(SetupModeManager setupModeManager, ConfigUtil configUtil, SetupGUI setupGUI) {
        this.setupModeManager = setupModeManager;
        this.configUtil = configUtil;
        this.setupGUI = setupGUI;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (!setupModeManager.isInSetupMode(player)) return;
        if (item == null || item.getType() == Material.AIR) return;

        String displayName = item.getItemMeta() != null && item.getItemMeta().hasDisplayName()
                ? ChatUtil.colorizeHex(item.getItemMeta().getDisplayName())
                : "";

        if (event.getAction() == Action.RIGHT_CLICK_BLOCK || event.getAction() == Action.RIGHT_CLICK_AIR) {
            Block clickedBlock = event.getClickedBlock();

            configUtil.getConfig().getSection("setup-mode.items").getRoutesAsStrings(false).forEach(key -> {
                String basePath = "setup-mode.items." + key;
                String expectedName = ChatUtil.colorizeHex(configUtil.getConfig().getString(basePath + ".name", ""));

                if (displayName.equals(expectedName)) {
                    switch (key) {
                        case "place-spawner":
                            if (clickedBlock == null) {
                                player.sendActionBar(configUtil.getMessage("messages.setup-mode.items-actionbar.not-selected-block"));
                                return;
                            }

                            Location spawnerLocation = clickedBlock.getRelative(event.getBlockFace()).getLocation();

                            if (!spawnerLocation.getBlock().getType().isAir()) {
                                player.sendActionBar(configUtil.getMessage("messages.setup-mode.items-actionbar.claimed-block"));
                                return;
                            }

                            setupModeManager.placeSpawner(player, spawnerLocation);
                            event.setCancelled(true);
                            break;

                        case "remove-spawner":
                            if (clickedBlock == null) {
                                player.sendActionBar(configUtil.getMessage("messages.setup-mode.items-actionbar.no-spawner-there"));
                                return;
                            }

                            if (!setupModeManager.removeSpawner(player, clickedBlock.getLocation())) {
                                player.sendActionBar(configUtil.getMessage("messages.setup-mode.items-actionbar.no-spawner-there"));
                            }

                            event.setCancelled(true);
                            break;

                        case "view-all":
                            setupModeManager.toggleSpawnerVisibility(player);
                            event.setCancelled(true);
                            break;

                        case "spawner-info":
                            if (clickedBlock == null) {
                                player.sendActionBar(configUtil.getMessage("messages.setup-mode.items-actionbar.no-spawner-there"));
                                return;
                            }

                            Location location = clickedBlock.getLocation();
                            Spawner spawner = setupModeManager.getSpawnerAt(location);

                            if (spawner != null) {
                                List<String> infoMessages = Collections.singletonList(configUtil.getMessage("messages.setup-mode.spawn-info"));
                                infoMessages.forEach(line -> {
                                    String formattedLine = line
                                            .replace("%prefix%", configUtil.getMessage("prefix"))
                                            .replace("%type%", spawner.getType())
                                            .replace("%name%", spawner.getName())
                                            .replace("%location%", location.getWorld().getName() + " " +
                                                    location.getBlockX() + ", " + location.getBlockY() + ", " + location.getBlockZ())
                                            .replace("%mob_type%", spawner.getMobType())
                                            .replace("%max_mobs%", String.valueOf(spawner.getMaxMobs()))
                                            .replace("%spawn_rate%", String.valueOf(spawner.getSpawnRate()));
                                    player.sendMessage(ChatUtil.colorizeHex(formattedLine));
                                });
                            } else {
                                player.sendActionBar(configUtil.getMessage("messages.setup-mode.items-actionbar.no-spawner-there"));
                            }
                            event.setCancelled(true);
                            break;

                        case "select-spawner":
                            setupGUI.openMenu(player);
                            event.setCancelled(true);
                            break;

                        case "leave-setup-mode":
                            setupModeManager.deactivateSetupMode(player);
                            event.setCancelled(true);
                            break;

                        default:
                            break;
                    }
                }
            });
        }
    }
}