package hu.kxtsoo.mobspawner.hooks.impl;

import hu.kxtsoo.mobspawner.MobSpawner;
import hu.kxtsoo.mobspawner.database.DatabaseManager;
import hu.kxtsoo.mobspawner.database.data.PlayerStat;
import hu.kxtsoo.mobspawner.database.data.ToplistCache;
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
    private final ToplistCache topListCache;

    public PlaceholderAPIHandler(MobSpawner plugin, long cacheUpdateIntervalMillis) {
        this.plugin = plugin;
        this.configUtil = plugin.getConfigUtil();
        this.topListCache = new ToplistCache(cacheUpdateIntervalMillis);
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
        Pattern pattern = Pattern.compile("top_(damage|kills)_(\\d+)_(name|value)");
        Matcher matcher = pattern.matcher(params);

        if (matcher.matches()) {
            String statType = matcher.group(1);
            int rank = Integer.parseInt(matcher.group(2));
            boolean isName = matcher.group(3).equals("name");

            return getTopPlayerStat(statType, rank, isName);
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

    private @NotNull String getTopPlayerStat(String sortBy, int rank, boolean isName) {
        List<PlayerStat> topPlayers = sortBy.equalsIgnoreCase("kills")
                ? topListCache.getTopKills()
                : topListCache.getTopDamage();

        if (rank > 0 && rank <= topPlayers.size()) {
            PlayerStat playerStat = topPlayers.get(rank - 1);
            if (isName) {
                String playerName = Bukkit.getOfflinePlayer(UUID.fromString(playerStat.getUuid())).getName();
                return playerName != null && !playerName.isEmpty() ? playerName : "---";
            } else {
                return String.valueOf((int) Math.round(playerStat.getValue()));
            }
        }

        return configUtil.getHooks().getString("hooks.settings.PlaceholderAPI.empty-placeholder", "---");
    }

//    private @NotNull String getTopPlayerStat(String sortBy, int rank, boolean isName) {
//        try {
//            List<PlayerStat> topPlayers = DatabaseManager.getTopPlayerStat(sortBy, rank);
//
//            if (rank > 0 && rank <= topPlayers.size()) {
//                PlayerStat playerStat = topPlayers.get(rank - 1);
//                if (isName) {
//                    String playerName = Bukkit.getOfflinePlayer(UUID.fromString(playerStat.getUuid())).getName();
//                    return playerName != null && !playerName.isEmpty() ? playerName : "---";
//                } else {
//                    return String.valueOf((int) Math.round(playerStat.getValue()));
//                }
//            }
//        } catch (SQLException e) {
//            e.printStackTrace();
//        }
//
//        return configUtil.getHooks().getString("hooks.settings.PlaceholderAPI.empty-placeholder", "---");
//    }
}