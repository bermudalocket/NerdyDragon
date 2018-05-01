package com.bermudalocket.nerdydragon;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import static com.bermudalocket.nerdydragon.NerdyDragon.CONFIGURATION;
import static com.bermudalocket.nerdydragon.NerdyDragon.DROPS_MANAGER;

public class Commands implements CommandExecutor {

    /**
     * A convenience field which stores the colorized [NerdyDragon] prefix for chat messages
     */
    private static final String prefix = String.format("%s[%sNerdyDragon%s] ",
            ChatColor.GRAY, ChatColor.AQUA, ChatColor.GRAY);

    /**
     * Registers the plugin's commands in the constructor
     */
    Commands() {
        NerdyDragon.PLUGIN.getCommand("nerdydragon-list").setExecutor(this);
        NerdyDragon.PLUGIN.getCommand("nerdydragon-peek").setExecutor(this);
        NerdyDragon.PLUGIN.getCommand("nerdydragon-toggle").setExecutor(this);
        NerdyDragon.PLUGIN.getCommand("nerdydragon-reload").setExecutor(this);
    }

    @Override
    public boolean onCommand(CommandSender commandSender, Command cmd, String s, String[] strings) {

        // ---------------------------------------------------------------------
        // /nerdydragon-list
        // ---------------------------------------------------------------------
        if (cmd.getName().equalsIgnoreCase("nerdydragon-list")) {
            commandSender.sendMessage(prefix + "*** ALL CURRENTLY ACTIVE DROPS ***");
            DROPS_MANAGER.getAllDropsInfo().forEach((i, d) -> {
                String itemName = (i.hasItemMeta()) ? i.getItemMeta().getDisplayName()
                        : i.getType().toString();
                commandSender.sendMessage(prefix + "* " + itemName + " (rate: " + d + ")");
            });
            return true;
        }

        // ---------------------------------------------------------------------
        // /nerdydragon-peek
        // ---------------------------------------------------------------------
        if (cmd.getName().equalsIgnoreCase("nerdydragon-peek")) {
            if (!(commandSender instanceof Player)) {
                commandSender.sendMessage(prefix + "Sorry, kind of hard to give items to console.");
                return true;
            }

            Player p = (Player) commandSender;
            DROPS_MANAGER.getAllDrops().forEach(p.getInventory()::addItem);
            commandSender.sendMessage(prefix + "All drops are now in your inventory. " +
                    "If any are missing, try making more space and try again.");
            return true;
        }

        // ---------------------------------------------------------------------
        // /nerdydragon-toggle
        // ---------------------------------------------------------------------
        if (cmd.getName().equalsIgnoreCase("nerdydragon-toggle")) {
            commandSender.sendMessage(prefix + "New plugin state: "
                    + NerdyDragon.PLUGIN.toggleState());
            return true;
        }

        // ---------------------------------------------------------------------
        // /nerdydragon-reload
        // ---------------------------------------------------------------------
        if (cmd.getName().equalsIgnoreCase("nerdydragon-reload")) {
            CONFIGURATION.reload();
            commandSender.sendMessage(prefix + "NerdyDragon reloaded!");
            return true;
        }

        return false;
    }

}
