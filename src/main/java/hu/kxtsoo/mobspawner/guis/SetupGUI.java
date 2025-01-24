package hu.kxtsoo.mobspawner.guis;

import dev.triumphteam.gui.builder.item.ItemBuilder;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import dev.triumphteam.gui.guis.PaginatedGui;
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

    public PaginatedGui createSetupGUI(Player player) {
        String rawTitle = configUtil.getGuiConfig().getString("setup-menu.title");
        String title;

        int size = configUtil.getGuiConfig().getInt("setup-menu.size", 27);

        List<ItemStack> spawnerItems = configUtil.getSpawnerItems();

        int rows = size / 9;
        int maxItemsPerPage = (rows - 1) * 9;
        int totalPages = (int) Math.ceil((double) spawnerItems.size() / maxItemsPerPage);

        title = getFormattedTitle(rawTitle, 1, totalPages);

        PaginatedGui gui = Gui.paginated()
                .title(Component.text(ChatUtil.colorizeHex(title)))
                .rows(size / 9)
                .disableAllInteractions()
                .create();

        addDecorativeItems(gui, configUtil);
        addNavigationButtons(gui, title, configUtil);
        addCloseButton(gui, configUtil);

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

            gui.addItem(guiItem);
        }

        updateGuiTitle(gui, rawTitle);
        return gui;
    }

    private void addNavigationButtons(PaginatedGui gui, String rawTitle, ConfigUtil configUtil) {
        String nextName = ChatUtil.colorizeHex(configUtil.getGuiConfig().getString("setup-menu.next-button.name"));
        String prevName = ChatUtil.colorizeHex(configUtil.getGuiConfig().getString("setup-menu.previous-button.name"));

        String nextMaterialName = configUtil.getGuiConfig().getString("setup-menu.next-button.material", "ARROW");
        String prevMaterialName = configUtil.getGuiConfig().getString("setup-menu.previous-button.material", "ARROW");

        Material nextMaterial = Material.matchMaterial(nextMaterialName.toUpperCase());
        Material prevMaterial = Material.matchMaterial(prevMaterialName.toUpperCase());

        if (nextMaterial == null || prevMaterial == null) {
            throw new IllegalArgumentException("Invalid material specified for navigation buttons in the config.");
        }

        ItemStack nextItem = new ItemStack(nextMaterial);
        ItemStack prevItem = new ItemStack(prevMaterial);

        nextItem.editMeta(meta -> meta.setDisplayName(nextName));
        prevItem.editMeta(meta -> meta.setDisplayName(prevName));

        List<Integer> nextSlots = configUtil.getGuiConfig().getIntList("setup-menu.next-button.slot");
        List<Integer> prevSlots = configUtil.getGuiConfig().getIntList("setup-menu.previous-button.slot");

        GuiItem nextButton = new GuiItem(nextItem, event -> {
            event.setCancelled(true);
            gui.next();
            updateGuiTitle(gui, rawTitle);
        });

        GuiItem prevButton = new GuiItem(prevItem, event -> {
            event.setCancelled(true);
            gui.previous();
            updateGuiTitle(gui, rawTitle);
        });

        nextSlots.forEach(slot -> gui.setItem(slot, nextButton));
        prevSlots.forEach(slot -> gui.setItem(slot, prevButton));
    }

    private String getFormattedTitle(String rawTitle, int currentPage, int totalPages) {
        return ChatUtil.colorizeHex(rawTitle
                .replace("%current_page%", String.valueOf(currentPage))
                .replace("%total_pages%", String.valueOf(totalPages)));
    }

    private void updateGuiTitle(PaginatedGui gui, String rawTitle) {
        String formattedTitle = getFormattedTitle(rawTitle, gui.getCurrentPageNum(), gui.getPagesNum());
        gui.updateTitle(formattedTitle);
    }

    private void addDecorativeItems(PaginatedGui gui, ConfigUtil configUtil) {
        var decorativeItems = configUtil.getGuiConfig().getSection("setup-menu.decorative-items");
        if (decorativeItems != null) {
            for (String key : decorativeItems.getRoutesAsStrings(false)) {
                var decorationSection = decorativeItems.getSection(key);

                if (decorationSection == null) continue;

                String materialName = decorationSection.getString("material", "STONE");
                Material material;
                try {
                    material = Material.valueOf(materialName.toUpperCase());
                } catch (IllegalArgumentException e) {
                    material = Material.BEDROCK;
                }

                String name = ChatUtil.colorizeHex(decorationSection.getString("name", "&r"));
                List<String> lore = decorationSection.getStringList("lore").stream()
                        .map(ChatUtil::colorizeHex)
                        .toList();

                List<Integer> slots = decorationSection.getIntList("slot");

                ItemStack decorativeItem = new ItemStack(material);
                decorativeItem.editMeta(meta -> {
                    meta.setDisplayName(name);
                    meta.setLore(lore);
                });

                GuiItem guiItem = new GuiItem(decorativeItem, event -> event.setCancelled(true));

                for (int slot : slots) {
                    if (slot >= 0 && slot < gui.getRows() * 9) {
                        gui.setItem(slot, guiItem);
                    }
                }
            }
        }
    }

    private void addCloseButton(PaginatedGui gui, ConfigUtil configUtil) {
        String closeName = ChatUtil.colorizeHex(configUtil.getGuiConfig().getString("setup-menu.close-button.name", "&cClose"));
        String closeMaterialName = configUtil.getGuiConfig().getString("setup-menu.close-button.material", "BARRIER");
        List<Integer> closeSlots = configUtil.getGuiConfig().getIntList("setup-menu.close-button.slot");

        Material closeMaterial = Material.matchMaterial(closeMaterialName.toUpperCase());
        if (closeMaterial == null) {
            closeMaterial = Material.BARRIER;
        }

        ItemStack closeItem = new ItemStack(closeMaterial);
        closeItem.editMeta(meta -> meta.setDisplayName(closeName));

        GuiItem closeButton = new GuiItem(closeItem, event -> {
            event.setCancelled(true);
            event.getWhoClicked().closeInventory();
        });

        for (int slot : closeSlots) {
            if (slot >= 0 && slot < gui.getRows() * 9) {
                gui.setItem(slot, closeButton);
            }
        }
    }

    public void openMenu(Player player) {
        createSetupGUI(player).open(player);
    }
}
