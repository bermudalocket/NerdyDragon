package com.bermudalocket.nerdydragon.commands;

import com.bermudalocket.nerdydragon.NerdyDragon;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

public class LeaderboardCommand extends ExecutorBase {

    public LeaderboardCommand() {
        super("nd-leaderboard", "statistics", "top", "help");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0 || (args.length == 1 && args[0].equalsIgnoreCase("help"))) {
            return false;
        }

        if ((args.length == 1 || args.length == 2) && args[0].equalsIgnoreCase("statistics")) {
            if (args.length == 1) {
                msg(sender, NerdyDragon.LEADERBOARD.getStatistics(null));
                return true;
            }
            if (args[1].equalsIgnoreCase("solo")) {
                msg(sender, NerdyDragon.LEADERBOARD.getStatistics(true));
            } else if (args[1].equalsIgnoreCase("group")) {
                msg(sender, NerdyDragon.LEADERBOARD.getStatistics(false));
            } else {
                msg(sender, NerdyDragon.LEADERBOARD.getStatistics(null));
            }
            return true;
        }

        if ((args.length == 2 || args.length == 3) && args[0].equalsIgnoreCase("top")) {
            int n = 5;
            try {
                n = Integer.valueOf(args[2]);
            } catch (Exception e) { }
            msg(sender, "--------------------------------------");
            if (args[1].equalsIgnoreCase("solo")) {
                NerdyDragon.LEADERBOARD.getTop(n, true).forEach(s -> msg(sender, s));
            } else if (args[1].equalsIgnoreCase("group")) {
                NerdyDragon.LEADERBOARD.getTop(n, false).forEach(s -> msg(sender, s));
            } else {
                NerdyDragon.LEADERBOARD.getTop(n, null).forEach(s -> msg(sender, s));
            }
            return true;
        }

        return false;
    }

    private static final HashSet<String> STATISTICS_SUBCOMMANDS = new HashSet<>(Arrays.asList("all", "group", "solo"));

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args == null || args.length == 0 || (args.length == 1 && args[0].equals(""))) {
            completions.addAll(_subcommands);
        } else if (args.length == 1) {
            String arg = args[0];
            _subcommands.stream().filter(s -> s.startsWith(arg)).forEach(completions::add);
        } else if (args.length == 2) {
            String arg = args[0];
            if (arg.equalsIgnoreCase("statistics") || arg.equalsIgnoreCase("top")) {
                if ("".equals(args[1])) {
                    return new ArrayList<>(STATISTICS_SUBCOMMANDS);
                }
                _subcommands.stream().filter(s -> s.startsWith(args[0])).forEach(completions::add);
            }
        } else if (args.length == 3) {
            String arg = args[0];
            if (arg.equalsIgnoreCase("top") && STATISTICS_SUBCOMMANDS.contains(args[1])) {
                if ("".equals(args[2])) {
                    return Collections.singletonList("[quantity = 5]");
                }
            }
        }
        return completions;
    }

}
