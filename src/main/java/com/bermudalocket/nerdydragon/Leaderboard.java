package com.bermudalocket.nerdydragon;

import org.apache.commons.lang.time.DurationFormatUtils;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class Leaderboard {

    private final File LEADERBOARD_FILE;

    /**
     * Calendar object used for converting timestamps.
     */
    private static final Calendar CALENDAR = Calendar.getInstance();

    /**
     * Date formatting object used for converting timestamps.
     */
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("E MMM d y hh:mm:ss a");

    Leaderboard() {
        LEADERBOARD_FILE = new File(NerdyDragon.PLUGIN.getDataFolder().getPath() + "/leaderboard.yml");
    }

    private YamlConfiguration getYAML() {
        return YamlConfiguration.loadConfiguration(LEADERBOARD_FILE);
    }

    private static String getKey(UUID fightId) {
        return "fight-history." + fightId.toString();
    }

    /**
     * Returns all fights in the fight history. If type is true, this will return
     * solo fights only. If type is false, this will return group fights only. If
     * type is null, this will return both solo and group fights.
     */
    private Set<UUID> getFights(FileConfiguration yaml, Boolean type) {
        Predicate<UUID> predicate;
        if (type == null) {
            predicate = (fightId) -> getPlayers(fightId, yaml).size() >= 1;
        } else if (type) {
            predicate = (fightId) -> getPlayers(fightId, yaml).size() == 1;
        } else {
            predicate = (fightId) -> getPlayers(fightId, yaml).size() > 1;
        }
        return yaml.getConfigurationSection("fight-history")
            .getKeys(false)
            .stream()
            .map(UUID::fromString)
            .filter(predicate)
            .collect(Collectors.toSet());
    }

    private long getTimeStarted(UUID fightId, FileConfiguration yaml) {
        return yaml.getLong(getKey(fightId) + ".time-started", 0);
    }

    private long getDuration(UUID fightId, FileConfiguration yaml) {
        return yaml.getLong(getKey(fightId) + ".duration", 0);
    }

    private Set<String> getPlayers(UUID fightId, FileConfiguration yaml) {
        return yaml.getConfigurationSection(getKey(fightId) + ".players")
                   .getKeys(false);
    }

    private double getPlayerDamagePercent(UUID fightId, String player, FileConfiguration yaml) {
        return yaml.getDouble(getKey(fightId) + ".players." + player, 0);
    }

    // ------------------------------------------------------------------------
    /**
     * Emphasizes the given message.
     */
    private static String emph(String msg) {
        return ChatColor.DARK_PURPLE + msg + ChatColor.GRAY;
    }

    // ------------------------------------------------------------------------
    /**
     * Turns the given timestamp into a string following the form described by
     * DATE_FORMAT.
     *
     * @param time the timestamp.
     * @return a string following the form described by DATE_FORMAT.
     */
    private static String longToDate(Long time) {
        CALENDAR.setTimeInMillis(time);
        return DATE_FORMAT.format(CALENDAR.getTime());
    }

    public String getStatistics(Boolean solo) {
        // mean duration
        YamlConfiguration yaml = getYAML();
        Set<UUID> fights = getFights(yaml, solo);
        if (fights.size() == 0) {
            return "There are no fights matching that criteria.";
        }
        long mean = fights.stream()
            .mapToLong(fightId -> getDuration(fightId, yaml))
            .sum() / fights.size();
        long squaredDev = fights.stream()
            .mapToLong(fightId -> (getDuration(fightId, yaml) - mean)^2)
            .sum() / fights.size();
        long stDev = Math.round(Math.sqrt(squaredDev)); // won't be called often

        return String.format("The mean is %s and the standard deviation is %s",
            emph(DurationFormatUtils.formatDuration(mean, getHMSFormat(mean))),
            emph(DurationFormatUtils.formatDuration(stDev, getHMSFormat(stDev))));
    }

    static String getHMSFormat(long value) {
        return (value > 60*60*1000 ? "H'h' " : "") + "m'm' s's'";
    }

    public LinkedHashSet<String> getTop(int n, Boolean solo) {
        YamlConfiguration yaml = getYAML();
        LinkedHashSet<String> results = new LinkedHashSet<>();
        TreeMap<Long, UUID> sortMap = new TreeMap<>();
        for (UUID fightId : getFights(yaml, solo)) {
            sortMap.put(getDuration(fightId, yaml), fightId);
        }
        if (sortMap.isEmpty()) {
            results.add("There are no fights matching that criteria.");
            return results;
        }
        if (n > sortMap.size()) {
            n = sortMap.size();
        }
        for (int i = 1; i <= n; i++) {
            Map.Entry<Long, UUID> next = sortMap.pollFirstEntry();
            UUID fightId = next.getValue();
            long duration = next.getKey();
            long time = getTimeStarted(fightId, yaml);
            String playersString = getPlayers(fightId, yaml)
                .stream()
                .map(player -> ChatColor.DARK_PURPLE + player + ChatColor.GRAY + " (" + getPlayerDamagePercent(fightId, player, yaml) + "%)")
                .collect(Collectors.joining(", "));
            results.add("#" + i + ". " + emph(DurationFormatUtils.formatDuration(duration, getHMSFormat(duration))) + " by " + playersString + " on " + emph(longToDate(time)));
        }
        return results;
    }

    void add(EnderDragonFight fight, long duration) {
        FileConfiguration yaml = YamlConfiguration.loadConfiguration(LEADERBOARD_FILE);
        ConfigurationSection section = yaml.getConfigurationSection("fight-history");
        if (section == null) {
            section = yaml.createSection("fight-history");
        }
        ConfigurationSection thisFight = section.createSection(fight.getUUID().toString());
        thisFight.set("time-started", fight.TIME_STARTED);
        thisFight.set("duration", duration);
        ConfigurationSection players = thisFight.createSection("players");
        for (OfflinePlayer offlinePlayer : fight.getAttackers()) {
            UUID uuid = offlinePlayer.getUniqueId();
            players.set(offlinePlayer.getName(), DragonHelper.getDamageRatio(fight.getDamage(uuid), fight.getDragon()));
        }
        try {
            yaml.save(LEADERBOARD_FILE);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
