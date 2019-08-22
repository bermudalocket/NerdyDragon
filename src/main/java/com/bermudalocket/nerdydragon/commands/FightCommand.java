/*
 * Copyright (c) 2019 bermudalocket. All rights reserved.
 * Unauthorized copying or distribution of this item without permission of the author is prohibited.
 * Proprietary and Confidential
 * Written by bermudalocket, 2019.
 */
package com.bermudalocket.nerdydragon.commands;

import com.bermudalocket.nerdydragon.CrystalRunnable;
import com.bermudalocket.nerdydragon.EnderDragonFight;
import com.bermudalocket.nerdydragon.FightStage;
import com.bermudalocket.nerdydragon.NerdyDragon;
import com.bermudalocket.nerdydragon.Util;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EnderCrystal;

public class FightCommand extends ExecutorBase {

    public FightCommand() {
        super("nd-fight", "help", "butcher", "butcher-all", "debug", "skip", "stop");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0 || (args.length == 1 && args[0].equalsIgnoreCase("help"))) {
            return false;
        }

        EnderDragonFight fight = NerdyDragon.FIGHT;
        if (fight == null) {
            sender.sendMessage(ChatColor.RED + "A fight does not currently exist.");
            return true;
        }

        String arg = args[0];

        if (arg.equalsIgnoreCase("help")) {
            return false;
        } else if (arg.equalsIgnoreCase("skip")) {
            fight.skipStage();
            msg(sender, "Skipping stage.");
        } else if (arg.equalsIgnoreCase("butcher")) {
            fight.removeReinforcements(false);
            msg(sender, "Removing reinforcements.");
        } else if (arg.equalsIgnoreCase("butcher-all")) {
            fight.removeReinforcements(true);
            msg(sender, "Removing all entities associated with this fight.");
        } else if (arg.equalsIgnoreCase("stop")) {
            if (fight.getStage() != FightStage.FINISHED) {
                if (fight.getStage() == FightStage.FIRST) {
                    fight.skipStage();
                }
                fight.endFight(true);
                msg(sender, "Stopped!");
            } else {
                msg(sender, "The most recent fight has already finished.");
            }
        } else if (arg.equalsIgnoreCase("debug")) {
            CrystalRunnable runnable = fight.getCrystalRunnable();
            msg(sender, "The UUID of this fight is " + fight.getUUID().toString() + ".");
            msg(sender, "The fight is in stage " + fight.getStage().toString() + ".");
            msg(sender, "Crystal runnable is " + (runnable.isRunning() ? "running" : "stopped"));
            msg(sender, "There are currently " + runnable.getCrystals().size() + " crystals being tracked: ");
            int i = 1;
            for (EnderCrystal crystal : runnable.getCrystals()) {
                msg(sender, i + ". " + Util.locationToOrderedTriple(crystal.getLocation()));
                i++;
            }
        }
        return true;
    }

}
