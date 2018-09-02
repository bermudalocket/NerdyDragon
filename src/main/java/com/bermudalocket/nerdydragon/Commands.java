package com.bermudalocket.nerdydragon;

import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

// ----------------------------------------------------------------------------------------------------------
/**
 * The command handling class.
 */
public class Commands implements CommandExecutor {

    // ------------------------------------------------------------------------------------------------------
    /**
     * Reference to the plugin's prefix.
     */
    private static final String PREFIX = NerdyDragon.getPrefix();

    // ------------------------------------------------------------------------------------------------------
    /**
     * Registers the plugin's commands in the constructor.
     */
    Commands() {
        NerdyDragon.PLUGIN.getCommand("nd-list").setExecutor(this);
        NerdyDragon.PLUGIN.getCommand("nd-peek").setExecutor(this);
        NerdyDragon.PLUGIN.getCommand("nd-toggle").setExecutor(this);
        NerdyDragon.PLUGIN.getCommand("nd-reload").setExecutor(this);
        NerdyDragon.PLUGIN.getCommand("nd-add").setExecutor(this);
    }

    /**
     * @see org.bukkit.command.CommandExecutor#onCommand(CommandSender, Command, String, String[]).
     */
    @Override
    public boolean onCommand(CommandSender commandSender, Command cmd, String s, String[] args) {

        // --------------------------------------------------------------------------------------------------
        // /nd-add
        // --------------------------------------------------------------------------------------------------
        if (cmd.getName().equalsIgnoreCase("nd-add")) {
            if (commandSender instanceof Player) {
                Player player = (Player) commandSender;
                ItemStack itemStack = player.getEquipment().getItemInMainHand();
                try {
                    double probability = Double.valueOf(args[0]);
                    if (itemStack != null && itemStack.getType() != Material.AIR) {
                        NerdyDragon.PLUGIN.getLootTable().addLoot(itemStack, probability);
                    } else {
                        player.sendMessage(PREFIX + "There's nothing in your hand!");
                    }
                } catch (Exception e) {
                    player.sendMessage(PREFIX + "You must specify a probability between 0.0 and 1.0!");
                }
            } else {
                commandSender.sendMessage(PREFIX + "You must be in-game to do that!");
            }
            return true;
        }

        // --------------------------------------------------------------------------------------------------
        // /nd-list
        // --------------------------------------------------------------------------------------------------
        if (cmd.getName().equalsIgnoreCase("nd-list")) {
            commandSender.sendMessage(PREFIX + "*** ALL CURRENTLY ACTIVE DROPS ***");
            int n = 1;
            LootTable lootTable = NerdyDragon.PLUGIN.getLootTable();
            for (Map.Entry<ItemStack, Double> entry : lootTable.getTable().entrySet()) {
                String item = Util.getFormattedName(entry.getKey());
                double probability = entry.getValue();
                String msg = PREFIX + " " + n + ") " + item + " (probability: " + probability + "/1.0)";
                commandSender.sendMessage(msg);
            }
            return true;
        }

        // --------------------------------------------------------------------------------------------------
        // /nd-peek
        // --------------------------------------------------------------------------------------------------
        if (cmd.getName().equalsIgnoreCase("nd-peek")) {
            if (commandSender instanceof Player) {
                Player player = (Player) commandSender;
                LootTable lootTable = NerdyDragon.PLUGIN.getLootTable();
                lootTable.getAllLoot().forEach(player.getInventory()::addItem);
                player.sendMessage(PREFIX + "All drops are now in your inventory. If any are missing, make" +
                                              " more space and try again.");
            } else {
                commandSender.sendMessage(PREFIX + "You must be in-game to do that!");
            }
            return true;
        }

        // --------------------------------------------------------------------------------------------------
        // /nd-toggle
        // --------------------------------------------------------------------------------------------------
        if (cmd.getName().equalsIgnoreCase("nd-toggle")) {
            commandSender.sendMessage(PREFIX + "NerdyDragon is now "
                                        + (NerdyDragon.PLUGIN.toggleState() ? "enabled" : "disabled") + ".");
            return true;
        }

        // --------------------------------------------------------------------------------------------------
        // /nd-reload
        // --------------------------------------------------------------------------------------------------
        if (cmd.getName().equalsIgnoreCase("nd-reload")) {
            Configuration.reload(true);
            commandSender.sendMessage(PREFIX + "NerdyDragon reloaded!");
            return true;
        }

        return false;
    }

}
