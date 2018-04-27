package com.bermudalocket.nerdydragon;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Optional;

public class NerdyDragon extends JavaPlugin implements Listener {

    /**
     * The plugin accessible as a singleton
     */
    static NerdyDragon PLUGIN;

    /**
     * The configuration handler accessible as a singleton
     */
    static Configuration CONFIGURATION = new Configuration();

    /**
     * The drops manager accessible as a singleton
     */
    static DropsManager DROPS_MANAGER = new DropsManager();

    /**
     * @see org.bukkit.plugin.java.JavaPlugin#onEnable()
     */
    public void onEnable() {
        PLUGIN = this;

        getServer().getPluginManager().registerEvents(this, this);

        saveDefaultConfig();
        CONFIGURATION.reload();

        new Commands();
    }

    // -------------------------------------------------------------------------

    /**
     * Handle custom drops upon Ender Dragon death
     *
     * @param e the EntityDeathEvent
     */
    @EventHandler
    public void onEntityDeath(EntityDeathEvent e) {
        if (!e.getEntityType().equals(EntityType.ENDER_DRAGON)) return;

        if (!_state) {
            getLogger().info("Ender Dragon death detected but the plugin is toggled off.");
            return;
        }

        Player player = e.getEntity().getKiller();
        if (player != null && player.isOnline())
            DROPS_MANAGER.getRandomDrops().forEach(i -> giveCustomDrop(player, i));
        else
            DROPS_MANAGER.getRandomDrops().forEach(this::randomlyDropInWorld);
    }

    // -------------------------------------------------------------------------

    /**
     * Gives the specified custom drop to the specified player.
     *
     * @param player    The player whom will receive the custom drop
     * @param itemStack The custom drop
     */
    private void giveCustomDrop(Player player, ItemStack itemStack) {
        String itemName = (itemStack.hasItemMeta()) ? itemStack.getItemMeta().getDisplayName()
                : itemStack.getType().toString();
        PlayerInventory playerInventory = player.getInventory();
        World world = player.getWorld();

        // PlayerInventory#addItem returns a HashMap containing any items that failed to add.
        // If the result is empty, everything was added to the player's inventory. If not,
        // we'll have to drop those naturally.

        HashMap<Integer, ItemStack> result = playerInventory.addItem(itemStack);

        if (result.isEmpty()) {
            getLogger().info("Gave a custom dragon drop (" + itemName + ") to " + player.getName());
        } else {
            Location dropLoc = (Configuration.FAILSAFE_DROP_IN_PORTAL) ? world.getSpawnLocation()
                    : player.getLocation();
            result.values().forEach(i -> {
                world.dropItemNaturally(dropLoc, i);
                getLogger().info(player.getName() + "'s inventory is full! Dropping " + itemName
                        + " at (" + dropLoc.toString() + ").");
            });
        }
    }

    /**
     * Randomly drops the specified ItemStack in the default world.
     *
     * @param itemStack the ItemStack to drop
     */
    private void randomlyDropInWorld(ItemStack itemStack) {
        World world = Configuration.DEFAULT_WORLD;
        Location spawnLoc = world.getSpawnLocation();
        if (Configuration.FAILSAFE_DROP_IN_PORTAL)
            world.dropItemNaturally(spawnLoc, itemStack);
        else
            world.dropItem(getSafeDropLoc(spawnLoc).orElse(spawnLoc), itemStack);
    }

    /**
     * Finds a safe drop location around the given location.
     *
     * @return an optional Location
     */
    private Optional<Location> getSafeDropLoc(Location aboutLoc) {
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
     * Toggles the state of the plugin's activity.
     *
     * @return the new state
     */
    boolean toggleState() {
        _state = !_state;
        return _state;
    }

    /**
     * A HashSet of all the "unsafe" blocks we don't want to be dropping items onto.
     */
    private static HashSet<Material> _omitBlocks = new HashSet<>(Arrays.asList(
            Material.STRUCTURE_VOID, Material.FIRE, Material.ENDER_PORTAL, Material.END_GATEWAY));

    /**
     * The current plugin state
     */
    private static Boolean _state = true;

} // NerdyDragon
