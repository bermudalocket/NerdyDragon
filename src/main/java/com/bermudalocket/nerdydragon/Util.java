package com.bermudalocket.nerdydragon;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.stream.IntStream;

class Util {

    /**
     * A HashSet of all the "unsafe" blocks we don't want to be dropping items onto.
     */
    private static HashSet<Material> _omitBlocks = new HashSet<>(Arrays.asList(
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
        player.playSound(player.getLocation(), Sound.ENTITY_ENDERDRAGON_GROWL, 1f, 1f);
        player.sendMessage(ChatColor.GOLD + msg);
        NerdyDragon.PLUGIN.getLogger().info("Sent notification to " + player.getName() + ": " + msg);
    }

    /**
     * Finds a safe drop location around the given location.
     *
     * @return an optional Location
     */
    static Optional<Location> getSafeDropLoc(Location aboutLoc) {
        World world = aboutLoc.getWorld();
        int x0 = aboutLoc.getBlockX();
        int z0 = aboutLoc.getBlockZ();
        int delta = Configuration.DEFAULT_DROP_SEARCH_RADIUS;

        for (int x = x0 - delta; x <= x0 + delta; x++) {
            for (int z = z0 - delta; z <= z0 + delta; z++) {
                Block nextBlock = world.getHighestBlockAt(x, z);
                if (nextBlock != null && !_omitBlocks.contains(nextBlock.getType())) {
                    int y = world.getHighestBlockYAt(x, z);
                    return Optional.of(new Location(world, x, y, z));
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Finds the end portal (Material END_GATEWAY) in a given world, if one exists
     *
     * @param world The world in which to search
     * @return The location of an end portal, or the world's spawn if none is found
     */
    static Location findEndPortal(World world) {
        int x0 = 0;
        int z0 = 0;
        HashSet<Block> blockSet = new HashSet<>();

        IntStream.rangeClosed(-5, 5).parallel().forEach(dx -> {
            IntStream.rangeClosed(-5, 5).parallel().forEach(dz -> {
                int x = x0 + dx;
                int z = z0 + dz;
                blockSet.add(world.getHighestBlockAt(x, z));
            });
        });

        return blockSet.stream()
                .parallel()
                .filter(b -> b.getType().equals(Material.ENDER_PORTAL))
                .map(Block::getLocation)
                .findFirst()
                .orElse(world.getSpawnLocation());
    }

}
