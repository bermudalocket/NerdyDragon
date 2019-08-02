package com.bermudalocket.nerdydragon;

import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import org.apache.commons.lang.time.DurationFormatUtils;
import org.bukkit.ChatColor;

import java.io.*;
import java.util.*;
import java.util.function.Predicate;

// ------------------------------------------------------------------------
/**
 * A class handling all leaderboard functionality, including querying and i/o.
 */
public class Leaderboard {

    private static final HashSet<FightRecord> RECORDS = new HashSet<>();

    public enum Context {

        SOLO(record -> record.getParticipants().size() == 1),
        TEAM(record -> record.getParticipants().size() > 1),
        ALL(record -> true);

        private final Predicate<FightRecord> predicate;

        Context(Predicate<FightRecord> predicate) {
            this.predicate = predicate;
        }

        public boolean matches(FightRecord record) {
            return this.predicate.test(record);
        }

        public static Context fromString(String string) {
            try {
                return Context.valueOf(string);
            } catch (Exception e) {
                return null;
            }
        }

    }

    // ------------------------------------------------------------------------
    /**
     * Constructor.
     */
    @SuppressWarnings("ResultOfMethodCallIgnored")
    public Leaderboard(String dataPath) {
        File recordsDir = new File(dataPath + "/fight-records/");
        if (!recordsDir.exists()) {
            recordsDir.mkdir();
        }
        try {
            for (File recordFile : Objects.requireNonNull(recordsDir.listFiles())) {
                FileInputStream fileIn = new FileInputStream(recordFile);
                ObjectInputStream in = new ObjectInputStream(fileIn);
                RECORDS.add((FightRecord) in.readObject());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Set<FightRecord> getFightsWithin(Context context) {
        HashSet<FightRecord> result = new HashSet<>();
        for (FightRecord record : RECORDS) {
            if (context.matches(record)) {
                result.add(record);
            }
        }
        return result;
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
     * Returns basic statistics, currently mean and standard deviation. If solo
     * is null, all fights will be considered; if solo is true, only solo fights
     * will be considered; and if solo is false, only group fights will be
     * considered.
     *
     * @param solo true for solo, false for group, null for all.
     * @return a string of basic statistics.
     */
    public String getDurationStatistics(Context context) {
        Set<FightRecord> result = this.getFightsWithin(context);
        if (result.isEmpty()) {
            return "There are no fights matching that criteria. :(";
        }

        long meanSum = 0;
        for (FightRecord record : result) {
            if (context.matches(record)) {
                meanSum += record.getRawDuration();
            }
        }
        long mean = meanSum / result.size();

        long squareDeviationSum = 0;
        for (FightRecord record : result) {
            squareDeviationSum += Math.pow(record.getRawDuration() - mean, 2);
        }
        long standardDeviation = Math.round(Math.sqrt(squareDeviationSum / result.size()));

        return String.format(
            "The mean is %s and the standard deviation is %s",
            emph(DurationFormatUtils.formatDuration(mean, Util.getHMSFormat(mean))),
            emph(DurationFormatUtils.formatDuration(standardDeviation, Util.getHMSFormat(standardDeviation)))
        );
    }

    public LinkedHashSet<BaseComponent> getTop(int n, Context context) {
        Set<FightRecord> result = this.getFightsWithin(context);
        if (result.isEmpty()) {
            return new LinkedHashSet<>(Collections.singletonList(
                new TextComponent("There are no fights matching that criteria.")
            ));
        }

        TreeSet<FightRecord> treeSet = new TreeSet<>(
            Comparator.comparingLong(FightRecord::getRawDuration)
        );
        treeSet.addAll(result);

        LinkedHashSet<BaseComponent> chatLines = new LinkedHashSet<>();
        int position = 1;
        for (FightRecord record : treeSet) {
            TextComponent text = new TextComponent("#" + position + ". ");
            text.addExtra(record.getDescriptor());
            chatLines.add(text);
            position++;
            if (position > n) {
                break;
            }
        }
        return chatLines;
    }

    public void add(FightRecord record) {
        UUID uuid = record.getAssociatedFightUUID();
        File pluginDir = NerdyDragon.PLUGIN.getDataFolder();
        try {
            FileOutputStream fileOut = new FileOutputStream(pluginDir.toString() + "/fight-records/" + uuid.toString());
            ObjectOutputStream out = new ObjectOutputStream(fileOut);
            out.writeObject(record);
            out.flush();
            out.close();
        } catch (IOException e) {
            NerdyDragon.log("Failed to create new record file. Printing stack trace...");
            e.printStackTrace();
        }
    }

}
