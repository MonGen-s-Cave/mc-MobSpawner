package hu.kxtsoo.mobspawner.hooks.impl;

import hu.kxtsoo.mobspawner.MobSpawner;
import hu.kxtsoo.mobspawner.database.DatabaseManager;
import hu.kxtsoo.mobspawner.database.data.TopPlayerCache;
import hu.kxtsoo.mobspawner.model.PlayerData;
import hu.kxtsoo.mobspawner.util.ConfigUtil;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PlaceholderAPIHandler extends PlaceholderExpansion {

    private final MobSpawner plugin;
    private final ConfigUtil configUtil;

    public PlaceholderAPIHandler(MobSpawner plugin) {
        this.plugin = plugin;
        this.configUtil = plugin.getConfigUtil();
    }

    @Override
    public @NotNull String getIdentifier() {
        return "MobSpawner";
    }

    @NotNull
    @Override
    public String getAuthor() {
        return plugin.getDescription().getAuthors().toString();
    }

    @NotNull
    @Override
    public String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean canRegister() {
        return true;
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public @Nullable String onPlaceholderRequest(Player player, @NotNull String params) {
        if (player == null) {
            return "";
        }

        Pattern pattern = Pattern.compile("top_(damage|kills)_(\\d+)_(name|value)");
        Matcher matcher = pattern.matcher(params);

        if (matcher.matches()) {
            String statType = matcher.group(1);
            int rank = Integer.parseInt(matcher.group(2));
            boolean isName = matcher.group(3).equals("name");

            return getTopPlayerStatFromCache(statType, rank, isName);
        }

        String playerUuid = player.getUniqueId().toString();
        return switch (params.toLowerCase()) {
            case "mob_damage" -> {
                try {
                    yield String.valueOf(DatabaseManager.getPlayerData(playerUuid).getDamageDealt());
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            }
            case "mob_kills" -> {
                try {
                    yield String.valueOf(DatabaseManager.getPlayerData(playerUuid).getMobsKilled());
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            }
            default -> null;
        };
    }

    private @Nullable String getTopPlayerStatFromCache(String statType, int rank, boolean isName) {
        List<PlayerData> topPlayers = TopPlayerCache.getTopPlayers(statType);

        if (rank > 0 && rank <= topPlayers.size()) {
            PlayerData data = topPlayers.get(rank - 1);
            if (isName) {
                String playerName = Bukkit.getOfflinePlayer(UUID.fromString(data.getUuid())).getName();
                System.out.println(playerName + Bukkit.getOfflinePlayer(UUID.fromString(data.getUuid())));
                return playerName != null && !playerName.isEmpty()
                        ? playerName
                        : configUtil.getHooks().getString("hooks.settings.PlaceholderAPI.empty-placeholder", "---");
            } else {
                return "damage".equals(statType)
                        ? String.valueOf(data.getDamageDealt())
                        : String.valueOf(data.getMobsKilled());
            }
        }
        return configUtil.getHooks().getString("hooks.settings.PlaceholderAPI.empty-placeholder", "---");
    }
}