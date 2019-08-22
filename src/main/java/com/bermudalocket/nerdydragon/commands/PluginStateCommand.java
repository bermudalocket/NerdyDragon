/*
 * Copyright (c) 2019 bermudalocket. All rights reserved.
 * Unauthorized copying or distribution of this item without permission of the author is prohibited.
 * Proprietary and Confidential
 * Written by bermudalocket, 2019.
 */
package com.bermudalocket.nerdydragon.commands;

import com.bermudalocket.nerdydragon.NerdyDragon;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

public class PluginStateCommand extends ExecutorBase {

    // ------------------------------------------------------------------------
    /**
     * Constructor.
     */
    public PluginStateCommand() {
        super("nd-toggle", "help");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length >= 1 && args[0].equalsIgnoreCase("help")) {
            return false;
        }
        if (NerdyDragon.CONFIG.ENABLED) {
            NerdyDragon.CONFIG.setEnabled(false);
            msg(sender, "Plugin soft-disabled.");
        } else {
            NerdyDragon.CONFIG.setEnabled(true);
            msg(sender, "Plugin enabled!");
        }
        return true;
    }

}
