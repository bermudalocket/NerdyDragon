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

public class Util {

    public static final World WORLD_THE_END = Bukkit.getWorld("world_the_end");

    public static final Location END_SPAWN = new Location(WORLD_THE_END, 0, 55, 0);

    // ------------------------------------------------------------------------
    /**
     * Weakly compares two locations, returning true if their block (integer)
     * coordinates are equal.
     *
     * @param a
     * @param b
     * @return
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
     * @param player the player.
     * @return the given player's head as an ItemStack.
     */
    @SuppressWarnings("deprecation")
    public static ItemStack getPlayerHead(String player) {
        ItemStack itemStack = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta skullMeta = (SkullMeta) itemStack.getItemMeta();
        skullMeta.setOwningPlayer(Bukkit.getOfflinePlayer(player));
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
    }

    private static final String METADATA_KEY = "dragon-fight";

    private static final HashSet<String> ADMINS = new HashSet<>(Arrays.asList(
        "pez252", "ttsci", "defiex",
        "cujobear", "Flumper", "kumquatmay",
        "bermudalocket", "totemo"
    ));

}