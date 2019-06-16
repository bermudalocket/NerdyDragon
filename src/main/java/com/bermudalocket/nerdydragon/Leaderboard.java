package com.bermudalocket.nerdydragon;

import org.apache.commons.lang.time.DurationFormatUtils;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Collectors;

// ------------------------------------------------------------------------
/**
 * A class handling all leaderboard functionality, including querying and i/o.
 */
public class Leaderboard {

    /**
     * A reference to "../plugins/NerdyDragon/leaderboard.yml".
     */
    private final File LEADERBOARD_FILE;

    /**
     * Calendar object used for converting timestamps.
     */
    private static final Calendar CALENDAR = Calendar.getInstance();

    /**
     * Date formatting object used for converting timestamps.
     */
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("E MMM d y hh:mm:ss a");

    /**
     * Round decimals to the nearest hundredth.
     */
    private static final DecimalFormat PERCENT_FORMAT = new DecimalFormat("#.##");

    // ------------------------------------------------------------------------
    /**
     * Constructor.
     */
    Leaderboard() {
        LEADERBOARD_FILE = new File(NerdyDragon.PLUGIN.getDataFolder().getPath() + "/leaderboard.yml");
    }

    // ------------------------------------------------------------------------
    /**
     * Returns a freshly-loaded YamlConfiguration instance.
     *
     * @return a freshly-loaded YamlConfiguration instance.
     */
    private YamlConfiguration getYAML() {
        return YamlConfiguration.loadConfiguration(LEADERBOARD_FILE);
    }

    // ------------------------------------------------------------------------
    /**
     * Returns the YAML parent key for the given fight UUID, of the form
     * "fight-history.[UUID]".
     *
     * @param fightId
     * @return
     */
    private static String getKey(UUID fightId) {
        return "fight-history." + fightId.toString();
    }

    // ------------------------------------------------------------------------
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

    // ------------------------------------------------------------------------
    /**
     * Returns the time the fight began as a Unix timestamp.
     *
     * @param fightId the fight UUID.
     * @param yaml the YAML instance.
     * @return the time the fight began as a Unix timestamp.
     */
    private long getTimeStarted(UUID fightId, FileConfiguration yaml) {
        return yaml.getLong(getKey(fightId) + ".time-started", 0);
    }

    // ------------------------------------------------------------------------
    /**
     * Returns the duration of the fight in milliseconds.
     *
     * @param fightId the fight UUID.
     * @param yaml the YAML instance.
     * @return the duration of the fight in milliseconds.
     */
    private long getDuration(UUID fightId, FileConfiguration yaml) {
        return yaml.getLong(getKey(fightId) + ".duration", 0);
    }

    // ------------------------------------------------------------------------
    /**
     * Returns a set of player('s names) involved in the given fight.
     *
     * @param fightId the fight UUID.
     * @param yaml the YAML instance.
     * @return a set of player('s names) involved in the given fight.
     */
    private Set<String> getPlayers(UUID fightId, FileConfiguration yaml) {
        return yaml.getConfigurationSection(getKey(fightId) + ".players")
                   .getKeys(false);
    }

    // ------------------------------------------------------------------------
    /**
     * Returns the overall percentage of damage done to the dragon by the given
     * player in the given fight. Note this is returned as a String with the
     * numeric value rounded to the nearest hundredth.
     *
     * @param fightId the fight UUID.
     * @param player the player's name.
     * @param yaml the YAML instance.
     * @return the overall percentage of damage done by the player.
     */
    private String getPlayerDamagePercent(UUID fightId, String player, FileConfiguration yaml) {
        double damagePct = yaml.getDouble(getKey(fightId) + ".players." + player, 0);
        return PERCENT_FORMAT.format(damagePct);
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

    // ------------------------------------------------------------------------
    /**
     * Returns basic statistics, currently mean and standard deviation. If solo
     * is null, all fights will be considered; if solo is true, only solo fights
     * will be considered; and if solo is false, only group fights will be
     * considered.
     *
     * @param solo true for solo, false for group, null for all.
     * @return a string of basic statistics.
     */
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
        double squaredDev = fights.stream()
            .mapToDouble(fightId -> Math.pow(getDuration(fightId, yaml) - mean, 2))
            .sum() / fights.size();
        long stDev = Math.round(Math.sqrt(squaredDev)); // won't be called often

        return String.format("The mean is %s and the standard deviation is %s",
            emph(DurationFormatUtils.formatDuration(mean, Util.getHMSFormat(mean))),
            emph(DurationFormatUtils.formatDuration(stDev, Util.getHMSFormat(stDev))));
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
            results.add("#" + i + ". " + emph(DurationFormatUtils.formatDuration(duration, Util.getHMSFormat(duration))) + " by " + playersString + " on " + emph(longToDate(time)));
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
        thisFight.set("time-started", fight._timeStarted);
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
