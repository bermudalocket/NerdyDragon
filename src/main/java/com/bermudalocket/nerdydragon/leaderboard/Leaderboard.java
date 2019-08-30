/*
 * Copyright (c) 2019 bermudalocket. All rights reserved.
 * Unauthorized copying or distribution of this item without permission of the author is prohibited.
 * Proprietary and Confidential
 * Created by bermudalocket on 8/29/2019 at 9:41:32.
 * Last modified 8/29/19, 9:41 PM.
 */
package com.bermudalocket.nerdydragon.leaderboard;

import com.bermudalocket.nerdydragon.NerdyDragon;
import com.bermudalocket.nerdydragon.Util;
import org.apache.commons.lang.time.DurationFormatUtils;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

// ------------------------------------------------------------------------
/**
 * A class handling all leaderboard functionality, including querying and i/o.
 */
public class Leaderboard {

    private static final Leaderboard INSTANCE = new Leaderboard();

    private final YamlConfiguration yaml;

    private static final Set<FightRecord> FIGHT_RECORDS = new HashSet<>();

    public static Leaderboard getInstance() {
        return INSTANCE;
    }

    private Leaderboard() {
        File file = new File(NerdyDragon.PLUGIN.getDataFolder().getPath() + "/leaderboard.yml");
        this.yaml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection history = this.yaml.getConfigurationSection("fight-history");
        if (history == null) {
            history = this.yaml.createSection("fight-history");
        }
        for (String id : history.getKeys(false)) {
            try {
                FightRecord record = (FightRecord) this.yaml.get("fight-history." + id);
                FIGHT_RECORDS.add(record);
            } catch (Exception e) {
                NerdyDragon.log("Failed to deserialize fight record with id " + id);
            }
        }
    }

    public void save() {
        for (FightRecord record : FIGHT_RECORDS) {
            this.yaml.set("fight-history." + record.getId(), record);
        }
    }

    // ------------------------------------------------------------------------
    /**
     * Adds a fight record to the fight history.
     */
    public void addRecord(FightRecord record) {
        FIGHT_RECORDS.add(record);
    }

    // ------------------------------------------------------------------------
    /**
     * Returns all records matching the given context.
     */
    private Set<FightRecord> getRecords(FightContext context) {
        return FIGHT_RECORDS.stream().filter(context::matches).collect(Collectors.toSet());
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
     * Returns basic statistics, currently mean and standard deviation.
     */
    public String getStatistics(FightContext context) {
        if (context == null) {
            return "There are no fights matching that criteria.";
        }
        Set<FightRecord> fights = this.getRecords(context);
        if (fights.size() == 0) {
            return "There are no fights matching that criteria.";
        }
        long mean = fights.stream()
                          .mapToLong(FightRecord::getDuration)
                          .sum() / fights.size();
        double squaredDev = fights.stream()
                                  .mapToDouble(fight -> Math.pow(fight.getDuration() - mean, 2))
                                  .sum() / fights.size();
        long stDev = Math.round(Math.sqrt(squaredDev)); // won't be called often

        return String.format("The mean is %s and the standard deviation is %s",
            emph(DurationFormatUtils.formatDuration(mean, Util.getHMSFormat(mean))),
            emph(DurationFormatUtils.formatDuration(stDev, Util.getHMSFormat(stDev))));
    }

    public LinkedHashSet<String> getTop(int n, FightContext context) {
        if (n <= 0 || context == null) {
            return new LinkedHashSet<>();
        }
        LinkedHashSet<String> results = new LinkedHashSet<>();
        TreeMap<Long, FightRecord> sortMap = new TreeMap<>();
        this.getRecords(context).forEach(fight -> sortMap.put(fight.getDuration(), fight));
        if (sortMap.isEmpty()) {
            return new LinkedHashSet<>(Collections.singleton("There are no fights matching that criteria."));
        }
        if (n > sortMap.size()) {
            n = sortMap.size(); // clamp
        }
        for (int i = 1; i <= n; i++) {
            Map.Entry<Long, FightRecord> next = sortMap.pollFirstEntry();
            FightRecord record = next.getValue();
            long duration = next.getKey();
            long time = record.getTimeStarted();
            String playersString = record.getParticipants()
                .stream()
                .map(player -> String.format("%s%s%s (%.2f%%)", ChatColor.DARK_PURPLE,
                                                                player,
                                                                ChatColor.GRAY,
                                                                record.getParticipantDamagePercent(player)))
                .collect(Collectors.joining(", "));
            results.add("#" + i + ". " + emph(DurationFormatUtils.formatDuration(duration, Util.getHMSFormat(duration))) + " by " + playersString + " on " + emph(longToDate(time)));
        }
        return results;
    }

    /**
     * Calendar object used for converting timestamps.
     */
    private static final Calendar CALENDAR = Calendar.getInstance();

    /**
     * Date formatting object used for converting timestamps.
     */
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("E MMM d y hh:mm:ss a");

}
