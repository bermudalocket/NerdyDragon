package com.bermudalocket.nerdydragon;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Random;

class Util {

    /**
     * A persistent Random object.
     */
    static final Random RANDOM = new Random();

    /**
     * A HashSet of all the "unsafe" blocks we don't want to be dropping items onto.
     */
    private static HashSet<Material> BLACKLIST = new HashSet<>(Arrays.asList(
            Material.STRUCTURE_VOID, Material.FIRE, Material.ENDER_PORTAL, Material.END_GATEWAY));

    /**
     * Transforms a location object to an ordered triple of the form (x, y, z)
     *
     * @param loc The location to transform
     * @return The transformed ordered triple
     */
    static String locationToOrderedTriple(Location loc) {
        return "(" + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ() + ")";
    }

    /**
     * Convenience method to notify player of the custom drop location
     *
     * @param player The player to notify
     * @param msg    The message to display
     */
    static void notifyPlayer(Player player, String msg) {
        player.playSound(player.getLocation(), Sound.BLOCK_END_PORTAL_FRAME_FILL, 1f, 1f);
        player.sendMessage(ChatColor.GOLD + msg);
        NerdyDragon.log("Sent notification to " + player.getName() + ": " + msg);
    }

    /**
     * Finds a safe drop location around the given location.
     *
     * @return an safe drop Location
     */
    static Location getSafeDropLoc(Location center) {
        World world = center.getWorld();
        int x0 = center.getBlockX();
        int z0 = center.getBlockZ();
        int delta = Configuration.DEFAULT_DROP_SEARCH_RADIUS;

        for (int x = x0 - delta; x <= x0 + delta; x++) {
            for (int z = z0 - delta; z <= z0 + delta; z++) {
                Block nextBlock = world.getHighestBlockAt(x, z);
                if (nextBlock != null && !BLACKLIST.contains(nextBlock.getType())) {
                    return nextBlock.getLocation();
                }
            }
        }
        return center;
    }

    /**
     * Returns either the custom name (if present) or the material type.
     *
     * @param itemStack the item stack.
     * @return either the custom name (if present) or the material type.
     */
    static String getFormattedName(ItemStack itemStack) {
        if (itemStack.hasItemMeta()) {
            ItemMeta itemMeta = itemStack.getItemMeta();
            if (itemMeta.hasDisplayName()) {
                return itemMeta.getDisplayName();
            }
        }
        return itemStack.getType().toString();
    }

}