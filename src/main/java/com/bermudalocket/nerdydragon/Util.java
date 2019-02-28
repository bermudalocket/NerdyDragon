package com.bermudalocket.nerdydragon;

import nu.nerd.entitymeta.EntityMeta;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.Arrays;
import java.util.HashSet;
import java.util.UUID;

// ------------------------------------------------------------------------
/**
 * A utilities class containing global methods that aren't better placed
 * elsewhere.
 */
public class Util {

    // ------------------------------------------------------------------------
    /**
     * Determines and returns the most appropriate HMS format for the given
     * long, assuming the long is in milliseconds. If the value of the long
     * represents a duration of more than an hour, "Hms" will be returned;
     * otherwise, "ms" will be returned.
     *
     * @param value the timestamp value in milliseconds.
     * @return the most appropriate HMS format.
     */
    public static String getHMSFormat(long value) {
        return (value > 60*60*1000 ? "H'h' " : "") + "m'm' s's'";
    }

    // ------------------------------------------------------------------------
    /**
     * Weakly compares two locations, returning true if their block (integer)
     * coordinates are equal.
     *
     * @param a the first location.
     * @param b the second location.
     * @return true if the locations are weakly comparable.
     */
    public static boolean weaklyCompareLocations(Location a, Location b) {
        return a.getBlockX() == b.getBlockX()
            && a.getBlockY() == b.getBlockY()
            && a.getBlockZ() == b.getBlockZ();
    }

    // ------------------------------------------------------------------------
    /**
     * Returns the head of a random admin.
     *
     * @return the head of a random admin.
     */
    public static ItemStack getRandomAdminHead() {
        String randomAdmin = MathUtil.getRandomObject(ADMINS);
        return getPlayerHead(randomAdmin);
    }

    // ------------------------------------------------------------------------
    /**
     * Transforms a location object to an ordered triple of the form (x, y, z)
     *
     * @param loc The location to transform
     * @return The transformed ordered triple
     */
    public static String locationToOrderedTriple(Location loc) {
        return "(" + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ() + ")";
    }

    // ------------------------------------------------------------------------
    /**
     * Returns true if the given player is flying or gliding with an elytra.
     *
     * @param player the player.
     * @return true if the given player is flying or gliding with an elytra.
     */
    public static boolean isFlying(Player player) {
        if (player == null || !player.isOnline() || player.isOnGround()) {
            return false;
        }
        EntityEquipment equipment = player.getEquipment();
        if (equipment != null) {
            ItemStack chestplateSlot = equipment.getChestplate();
            if (chestplateSlot != null && chestplateSlot.getType() == Material.ELYTRA) {
                return player.isGliding();
            }
        }
        return false;
    }

    // ------------------------------------------------------------------------
    /**
     * Finds the player, if one exists, in the given EntityDamageByEntityEvent.
     *
     * @param e the event.
     * @return the player involved, if one exists; otherwise null.
     */
    static Player getPlayerFromDamageEvent(EntityDamageByEntityEvent e) {
        if (e.getDamager() instanceof Player) {
            return (Player) e.getDamager();
        } else if (e.getDamager() instanceof Projectile) {
            Projectile projectile = (Projectile) e.getDamager();
            if (projectile.getShooter() instanceof Player) {
                return (Player) projectile.getShooter();
            }
        }
        return null;
    }

    // ------------------------------------------------------------------------
    /**
     * Returns the given player's head as an ItemStack.
     *
     * @param playerUUID the UUID of the player.
     * @return the given player's head as an ItemStack.
     */
    public static ItemStack getPlayerHead(String playerUUID) {
        UUID uuid = UUID.fromString(playerUUID);
        ItemStack itemStack = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta skullMeta = (SkullMeta) itemStack.getItemMeta();
        skullMeta.setOwningPlayer(Bukkit.getOfflinePlayer(uuid));
        itemStack.setItemMeta(skullMeta);
        return itemStack;
    }

    // ------------------------------------------------------------------------
    /**
     * Returns true if the entity has been spawned by this fight.
     *
     * @param entity the entity.
     * @return true if the entity has been spawned by this fight.
     */
    public static boolean isReinforcement(Entity entity) {
        if (entity == null) {
            return false;
        }
        Object meta = EntityMeta.api().get(entity, NerdyDragon.PLUGIN, METADATA_KEY);
        return meta != null && "true".equalsIgnoreCase((String) meta);
    }

    // ------------------------------------------------------------------------
    /**
     * Tags the given entity with this fight's UUID as metadata.
     *
     * @param entity the entity to tag.
     */
    static void tagEntityWithMetadata(Entity entity) {
        EntityMeta.api().set(entity, NerdyDragon.PLUGIN, METADATA_KEY, "true");
        // yes yes i know that's a string
        // i had... issues
    }

    /**
     * A reference to The End, used as a default-world fallback.
     */
    public static final World WORLD_THE_END = Bukkit.getWorld("world_the_end");

    /**
     * This plugin's EntityMeta metadata key.
     */
    private static final String METADATA_KEY = "dragon-fight";

    /**
     * A set of admin names from which Vex heads are randomly chosen.
     */
    private static final HashSet<String> ADMINS = new HashSet<>(Arrays.asList(
        "1f5abb89-9f4f-4571-8599-a56ef4982840", // pez252
        "70346d9c-14dd-472c-89aa-e2cd1e223f61", // ttsci
        "5466a9cf-a22a-4574-b30f-aaa6aebd712e", // defiex
        "7a9c5824-a3b5-4105-8b1c-90ae1e2acd7e", // cujobear
        "e3675cb9-ff31-49b2-bb7c-47fbb021ec16", // flumper
        "d908f8ff-07ed-4e8d-a8b5-e4275866812b", // kumquatmay
        "e3501dfb-9513-47c8-9e55-965f88325ff7", // bermudalocket
        "8a2182fb-bc2f-440f-87d0-a889c7832e78" // totemo
    ));

}