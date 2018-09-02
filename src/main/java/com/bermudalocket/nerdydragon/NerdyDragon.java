package com.bermudalocket.nerdydragon;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.function.Consumer;

// ----------------------------------------------------------------------------------------------------------
/**
 * The main plugin and event-handling class.
 */
public class NerdyDragon extends JavaPlugin implements Listener {

    // ------------------------------------------------------------------------------------------------------
    /**
     * This plugin.
     */
    static NerdyDragon PLUGIN;

    /**
     * The Ender Dragon's new loot table.
     */
    private final LootTable _lootTable = new LootTable();

    /**
     * The current plugin state.
     */
    private static Boolean STATE;

    // ------------------------------------------------------------------------------------------------------
    /**
     * @see JavaPlugin#onEnable().
     */
    public void onEnable() {
        PLUGIN = this;

        getServer().getPluginManager().registerEvents(this, this);

        saveDefaultConfig();
        Configuration.reload(false);

        new Commands();
    }

    /**
     * @see JavaPlugin#onDisable().
     */
    public void onDisable() {
        Configuration.save();
    }

    // ------------------------------------------------------------------------------------------------------
    // Event Handlers
    // ------------------------------------------------------------------------------------------------------
    /**
     * Handle custom drops upon Ender Dragon death
     *
     * @param e the EntityDeathEvent
     */
    @EventHandler
    protected void onEntityDeath(EntityDeathEvent e) {
        if (e.getEntityType() == EntityType.ENDER_DRAGON) {
            Player player = e.getEntity().getKiller();
            if (player != null) {
                if (STATE) {
                    Consumer<ItemStack> consumer = getConsumer(player);
                    _lootTable.getLoot().forEach(consumer);
                } else {
                    log(player.getName() + " killed the Ender Dragon but the plugin is soft disabled.");
                }
            } else {
                log("The dragon was killed but the killer is null!");
            }
        }
    }

    /**
     * Returns a (loot) consumer depending on the player's online status.
     *
     * @param player the player.
     * @return the consumer.
     */
    private Consumer<ItemStack> getConsumer(Player player) {
        return (player.isOnline()) ? item -> givePlayerLoot(player, item) : this::safelyDropInWorld;
    }

    // ------------------------------------------------------------------------------------------------------
    // Loot
    // ------------------------------------------------------------------------------------------------------
    /**
     * Returns the loot table.
     *
     * @return the loot table.
     */
    LootTable getLootTable() {
        return _lootTable;
    }

    /**
     * Gives the specified custom drop to the specified player.
     *
     * @param player the player.
     * @param itemStack the custom drop.
     */
    private void givePlayerLoot(Player player, ItemStack itemStack) {
        String itemName = (itemStack.hasItemMeta()) ? itemStack.getItemMeta().getDisplayName()
                                                    : itemStack.getType().toString();
        PlayerInventory playerInventory = player.getInventory();

        // PlayerInventory#addItem returns a HashMap containing any items that failed to add
        HashMap<Integer, ItemStack> result = playerInventory.addItem(itemStack);
        if (result.isEmpty()) {
            Util.notifyPlayer(player, "You received a(n) " + ChatColor.WHITE + itemName + ChatColor.GOLD +
                                        " in your inventory!");
        } else {
            for (ItemStack item : result.values()) {
                Location dropLoc = player.getLocation();
                player.getWorld().dropItemNaturally(dropLoc, item).setInvulnerable(true);
                Util.notifyPlayer(player, "Your inventory was full! Your " + ChatColor.WHITE + itemName +
                                            ChatColor.GOLD + " has been dropped at " +
                                            Util.locationToOrderedTriple(dropLoc));
            }
        }
    }

    /**
     * Safely drops the specified ItemStack in the default world specified in config.
     *
     * @param itemStack the ItemStack to drop
     */
    private void safelyDropInWorld(ItemStack itemStack) {
        World world = Configuration.DEFAULT_WORLD;
        Location spawnLoc = world.getSpawnLocation();
        world.dropItem(Util.getSafeDropLoc(spawnLoc), itemStack);
    }

    // ------------------------------------------------------------------------------------------------------
    // Misc
    // ------------------------------------------------------------------------------------------------------
    /**
     * A logging convenience method, used instead of {@link java.util.logging.Logger} for colorizing this
     * plugin's name in console.
     *
     * @param message the message to log.
     */
    static void log(String message) {
        System.out.println(PREFIX + message);
    }

    /**
     * Toggles the state of the plugin's activity.
     *
     * @return the new state
     */
    boolean toggleState() {
        STATE = !STATE;
        return STATE;
    }

    /**
     * Returns this plugin's message prefix.
     *
     * @return the prefix.
     */
    static String getPrefix() {
        return PREFIX;
    }

    // ------------------------------------------------------------------------------------------------------
    /**
     * A pre-formatted log prefix for this plugin.
     */
    private static final String PREFIX = String.format("%s[%s%sNerdyDragon%s%s] %s[%sLOG%s] %s",
            ChatColor.DARK_GRAY, ChatColor.DARK_RED, ChatColor.BOLD, ChatColor.RESET, ChatColor.DARK_GRAY,
            ChatColor.DARK_GRAY, ChatColor.DARK_RED, ChatColor.DARK_GRAY, ChatColor.DARK_GRAY);

}
