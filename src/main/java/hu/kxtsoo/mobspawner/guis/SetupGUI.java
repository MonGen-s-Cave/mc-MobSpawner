package hu.kxtsoo.mobspawner.guis;

import dev.triumphteam.gui.builder.item.ItemBuilder;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import hu.kxtsoo.mobspawner.manager.SetupModeManager;
import hu.kxtsoo.mobspawner.util.ChatUtil;
import hu.kxtsoo.mobspawner.util.ConfigUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class SetupGUI {

    private final ConfigUtil configUtil;
    private final SetupModeManager setupModeManager;

    public SetupGUI(ConfigUtil configUtil, SetupModeManager setupModeManager) {
        this.configUtil = configUtil;
        this.setupModeManager = setupModeManager;
    }

    public Gui createSetupGUI(Player player) {
        String title = configUtil.getGuiConfig().getString("setup-menu.title");
        int size = configUtil.getGuiConfig().getInt("setup-menu.size", 27);
        String fillerType = configUtil.getGuiConfig().getString("setup-menu.filler-item.type", "");
        String fillerName = configUtil.getGuiConfig().getString("setup-menu.filler-item.name", "");

        Gui gui = Gui.gui()
                .title(Component.text(ChatUtil.colorizeHex(title)))
                .rows(size / 9)
                .disableAllInteractions()
                .create();

        if (!fillerType.isEmpty()) {
            Material fillerMaterial = Material.matchMaterial(fillerType.toUpperCase());
            if (fillerMaterial != null) {
                ItemStack fillerItem = new ItemStack(fillerMaterial);
                fillerItem.editMeta(meta -> meta.setDisplayName(ChatUtil.colorizeHex(fillerName)));
                gui.getFiller().fill(ItemBuilder.from(fillerItem).asGuiItem());
            }
        }

        List<ItemStack> spawnerItems = configUtil.getSpawnerItems();
        int slot = 0;

        for (ItemStack spawnerItem : spawnerItems) {
            String spawnerFileName = ChatUtil.colorizeHex(spawnerItem.getItemMeta().getDisplayName());

            var spawnerConfig = configUtil.getSpawnerConfig(spawnerFileName);
            String type = spawnerConfig.getString("spawner.type");
            String mobType = spawnerConfig.getString("mob.type");
            String maxMobs = spawnerConfig.getString("spawner.conditions.max-mobs");
            String spawnRate = spawnerConfig.getString("spawner.spawn-rate");

            if ("VISIBLE".equalsIgnoreCase(type)) {
                type = "ᴠɪꜱɪʙʟᴇ";
            } else if ("INVISIBLE".equalsIgnoreCase(type)) {
                type = "ɪɴᴠɪꜱɪʙʟᴇ";
            }

            String spawnerDisplayName = configUtil.getGuiConfig()
                    .getString("setup-menu.spawner-item.name", "&6&l> %spawner_name%")
                    .replace("%spawner_name%", spawnerFileName);
            String finalType = type;
            List<String> spawnerLore = configUtil.getGuiConfig()
                    .getStringList("setup-menu.spawner-item.lore")
                    .stream()
                    .map(line -> line
                            .replace("%spawner_name%", spawnerFileName)
                            .replace("%type%", finalType)
                            .replace("%mob_type%", mobType)
                            .replace("%max_mobs%", maxMobs)
                            .replace("%spawn_rate%", spawnRate))
                    .map(ChatUtil::colorizeHex)
                    .toList();

            spawnerItem.editMeta(meta -> {
                meta.setDisplayName(ChatUtil.colorizeHex(spawnerDisplayName));
                meta.setLore(spawnerLore);
            });

            GuiItem guiItem = ItemBuilder.from(spawnerItem).asGuiItem(event -> {
                player.closeInventory();
                setupModeManager.setSelectedSpawner(player, spawnerFileName);
                setupModeManager.activateSetupMode(player);
            });

            if (slot < size) {
                gui.setItem(slot, guiItem);
                slot++;
            }
        }

        return gui;
    }

    public void openMenu(Player player) {
        createSetupGUI(player).open(player);
    }
}
