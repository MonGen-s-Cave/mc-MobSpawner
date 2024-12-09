package hu.kxtsoo.mobspawner.util;

import dev.dejvokep.boostedyaml.YamlDocument;
import dev.dejvokep.boostedyaml.dvs.versioning.BasicVersioning;
import dev.dejvokep.boostedyaml.settings.dumper.DumperSettings;
import dev.dejvokep.boostedyaml.settings.general.GeneralSettings;
import dev.dejvokep.boostedyaml.settings.loader.LoaderSettings;
import dev.dejvokep.boostedyaml.settings.updater.UpdaterSettings;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;

public class ConfigUtil {

    private final JavaPlugin plugin;
    private YamlDocument config;
    private YamlDocument messages;
    private YamlDocument guis;
    private YamlDocument hooks;

    private final Map<String, YamlDocument> spawnerConfigs = new HashMap<>();
    private final Map<String, YamlDocument> mobConfigs = new HashMap<>();

    public ConfigUtil(JavaPlugin plugin) {
        this.plugin = plugin;
        setupConfig();
        setupMessages();
        loadSpawnerConfigs();
        loadMobConfigs();
        setupGuis();
        setupHooks();
    }

    public void setupConfig() {
        try {
            File configFile = new File(plugin.getDataFolder(), "config.yml");
            if (!configFile.exists()) {
                plugin.saveResource("config.yml", false);
            }

            config = YamlDocument.create(configFile,
                    Objects.requireNonNull(plugin.getResource("config.yml")),
                    GeneralSettings.builder().setUseDefaults(false).build(),
                    LoaderSettings.DEFAULT, DumperSettings.DEFAULT,
                    UpdaterSettings.builder().setKeepAll(true)
                            .setVersioning(new BasicVersioning("version")).build());

            config.update();
        } catch (IOException ex) {
            plugin.getLogger().severe("Error loading or creating config.yml: " + ex.getMessage());
        }
    }

    public void setupMessages() {
        generateDefaultLocales();

        String locale = config.getString("locale", "en");
        File messagesFile = new File(plugin.getDataFolder() + File.separator + "locale", locale + ".yml");

        if (!messagesFile.exists()) {
            plugin.saveResource("locale" + File.separator + locale + ".yml", false);
        }

        try {
            messages = YamlDocument.create(messagesFile,
                    Objects.requireNonNull(plugin.getResource("locale/" + locale + ".yml")),
                    GeneralSettings.builder().setUseDefaults(false).build(),
                    LoaderSettings.DEFAULT, DumperSettings.DEFAULT,
                    UpdaterSettings.builder()
                            .setVersioning(new BasicVersioning("version"))
                            .setKeepAll(true)
                            .build());

            messages.update();
        } catch (IOException ex) {
            plugin.getLogger().severe("Error loading or creating message files " + ex.getMessage());
        }
    }

    private void generateDefaultLocales() {
        String[] locales = {"en", "hu"};
        for (String locale : locales) {
            File localeFile = new File(plugin.getDataFolder(), "locale" + File.separator + locale + ".yml");
            if (!localeFile.exists()) {
                plugin.saveResource("locale" + File.separator + locale + ".yml", false);
            }
        }
    }


    public String getMessage(String key) {
        Object messageObj = messages.get(key, "Message not found");

        if (messageObj instanceof String) {
            String message = ChatUtil.colorizeHex((String) messageObj);
            String prefix = ChatUtil.colorizeHex(config.getString("prefix", ""));
            if (message.contains("%prefix%")) {
                return message.replace("%prefix%", prefix);
            }
            return message;
        } else if (messageObj instanceof List) {
            List<String> messageList = (List<String>) messageObj;
            String prefix = ChatUtil.colorizeHex(config.getString("prefix", ""));
            messageList = messageList.stream()
                    .map(ChatUtil::colorizeHex)
                    .map(msg -> msg.contains("%prefix%") ? msg.replace("%prefix%", prefix) : msg)
                    .toList();
            return String.join("\n", messageList);
        }

        return "Invalid message format";
    }

    private void generateDefaultConfigs(String folderName, String[] fileNames) {
        File targetFolder = new File(plugin.getDataFolder(), folderName);
        if (!targetFolder.exists()) {
            targetFolder.mkdirs();
        }

        for (String fileName : fileNames) {
            File targetFile = new File(targetFolder, fileName + ".yml");
            if (!targetFile.exists()) {
                plugin.saveResource(folderName + File.separator + fileName + ".yml", false);
            }
        }
    }

    private void loadSpawnerConfigs() {
        String[] spawnerFiles = {"skeleton_spawner", "zombie_spawner", "wither_skeleton_spawner"};
        generateDefaultConfigs("spawners", spawnerFiles);

        File spawnerFolder = new File(plugin.getDataFolder(), "spawners");
        if (!spawnerFolder.exists()) {
            spawnerFolder.mkdirs();
        }

        for (File file : Objects.requireNonNull(spawnerFolder.listFiles())) {
            if (file.isFile() && file.getName().endsWith(".yml")) {
                try {
                    YamlDocument spawnerConfig = YamlDocument.create(file,
                            GeneralSettings.builder().setUseDefaults(false).build(),
                            LoaderSettings.DEFAULT, DumperSettings.DEFAULT,
                            UpdaterSettings.builder().setKeepAll(true).build());
                    spawnerConfigs.put(file.getName().replace(".yml", ""), spawnerConfig);
                } catch (IOException ex) {
                    plugin.getLogger().log(Level.SEVERE, "Error loading spawner config " + file.getName(), ex);
                }
            }
        }
    }

    private void loadMobConfigs() {
        String[] mobFiles = {"skeleton", "zombie", "wither_skeleton"};
        generateDefaultConfigs("mobs", mobFiles);

        File mobFolder = new File(plugin.getDataFolder(), "mobs");
        if (!mobFolder.exists()) {
            mobFolder.mkdirs();
        }

        for (File file : Objects.requireNonNull(mobFolder.listFiles())) {
            if (file.isFile() && file.getName().endsWith(".yml")) {
                try {
                    YamlDocument mobConfig = YamlDocument.create(file,
                            GeneralSettings.builder().setUseDefaults(false).build(),
                            LoaderSettings.DEFAULT, DumperSettings.DEFAULT,
                            UpdaterSettings.builder().setKeepAll(true).build());
                    mobConfigs.put(file.getName().replace(".yml", ""), mobConfig);
                } catch (IOException ex) {
                    plugin.getLogger().log(Level.SEVERE, "Error loading mob config " + file.getName(), ex);
                }
            }
        }
    }

    public void setupGuis() {
        File guisFile = new File(plugin.getDataFolder(), "guis.yml");

        if (!guisFile.exists()) {
            plugin.saveResource("guis.yml", false);
        }

        try {
            guis = YamlDocument.create(guisFile,
                    Objects.requireNonNull(plugin.getResource("guis.yml")),
                    GeneralSettings.builder().setUseDefaults(false).build(),
                    LoaderSettings.DEFAULT, DumperSettings.DEFAULT,
                    UpdaterSettings.builder().setKeepAll(true)
                            .setVersioning(new BasicVersioning("version")).build());

            guis.update();
        } catch (IOException ex) {
            plugin.getLogger().severe("Error loading or creating guis.yml: " + ex.getMessage());
        }
    }

    public void setupHooks() {
        try {
            File hooksFile = new File(plugin.getDataFolder(), "hooks.yml");
            if (!hooksFile.exists()) {
                plugin.saveResource("hooks.yml", false);
            }

            hooks = YamlDocument.create(hooksFile,
                    Objects.requireNonNull(plugin.getResource("hooks.yml")),
                    GeneralSettings.builder().setUseDefaults(false).build(),
                    LoaderSettings.DEFAULT, DumperSettings.DEFAULT,
                    UpdaterSettings.builder().setKeepAll(true)
                            .setVersioning(new BasicVersioning("version")).build());

            hooks.update();
        } catch (IOException ex) {
            plugin.getLogger().severe("Error loading or creating hooks.yml: " + ex.getMessage());
        }
    }

    public ItemStack getItemStackFromConfig(YamlDocument config, String path) {
        if (!config.contains(path)) {
            return null;
        }

        String materialName = config.getString(path + ".type", "STONE");
        Material material = Material.matchMaterial(materialName);
        if (material == null) {
            plugin.getLogger().warning("Invalid material: " + materialName + " at path: " + path);
            return null;
        }

        int amount = config.getInt(path + ".amount", 1);
        ItemStack itemStack = new ItemStack(material, amount);

        ItemMeta meta = itemStack.getItemMeta();
        if (meta != null) {
            if (config.contains(path + ".name")) {
                meta.setDisplayName(ChatUtil.colorizeHex(config.getString(path + ".name")));
            }

            if (config.contains(path + ".lore")) {
                List<String> lore = config.getStringList(path + ".lore");
                meta.setLore(ChatUtil.colorizeHex(lore));
            }

            itemStack.setItemMeta(meta);
        }

        return itemStack;
    }

    public YamlDocument getConfig() { return config;
    }

    public Map<String, YamlDocument> getSpawnerConfigs() {
        return spawnerConfigs;
    }

    public Map<String, YamlDocument> getMobConfigs() {
        return mobConfigs;
    }

    public YamlDocument getSpawnerConfig(String spawnerName) {
        return spawnerConfigs.get(spawnerName);
    }

    public YamlDocument getMobConfig(String mobName) {
        return mobConfigs.get(mobName);
    }
    public YamlDocument getGuiConfig() {
        return guis;
    }
    public YamlDocument getHooks() {
        return hooks;
    }

    public List<ItemStack> getSpawnerItems() {
        List<ItemStack> spawnerItems = new ArrayList<>();

        for (String spawnerName : spawnerConfigs.keySet()) {
            String materialName = guis.getString("setup-menu.spawner-item.type", "SPAWNER");
            Material material = Material.matchMaterial(materialName.toUpperCase());
            if (material == null) {
                plugin.getLogger().warning("Invalid material in guis.yml for spawner items. Using default Spawner.");
                material = Material.SPAWNER;
            }

            ItemStack item = new ItemStack(material);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(ChatUtil.colorizeHex(spawnerName));
                meta.setLore(Collections.singletonList(ChatUtil.colorizeHex("&7Click here to select the spawner")));
                item.setItemMeta(meta);
            }
            spawnerItems.add(item);
        }

        return spawnerItems;
    }

    public void reloadConfigs() {
        try {
            config.reload();
            messages.reload();
            for (YamlDocument doc : spawnerConfigs.values()) {
                doc.reload();
            }
            for (YamlDocument doc : mobConfigs.values()) {
                doc.reload();
            }
            guis.reload();
        } catch (IOException ex) {
            plugin.getLogger().log(Level.SEVERE, "Error reloading configuration files", ex);
        }
    }
}