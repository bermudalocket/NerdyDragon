package com.bermudalocket.nerdydragon.commands;

import com.bermudalocket.nerdydragon.Configuration;
import com.bermudalocket.nerdydragon.NerdyDragon;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

public class ReloadCommand extends ExecutorBase {

    public ReloadCommand() {
        super("nd-reload", "help");
    }

    @Override
    public boolean onCommand(CommandSender commandSender, Command cmd, String s, String[] args) {
        if (args.length == 1 && args[0].equalsIgnoreCase("help")) {
            return false;
        }
        NerdyDragon.CONFIG.reload();
        NerdyDragon.message(commandSender, "NerdyDragon reloaded!");
        return true;
    }

}
