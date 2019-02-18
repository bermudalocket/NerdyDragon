package com.bermudalocket.nerdydragon.commands;

import com.bermudalocket.nerdydragon.Configuration;
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
        if (Configuration.ENABLED) {
            Configuration.setEnabled(false);
            msg(sender, "Plugin soft-disabled.");
        } else {
            Configuration.setEnabled(true);
            msg(sender, "Plugin enabled!");
        }
        return true;
    }

}
