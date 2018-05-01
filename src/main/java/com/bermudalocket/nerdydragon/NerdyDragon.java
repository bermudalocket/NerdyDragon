package com.bermudalocket.nerdydragon;

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
     * The current plugin state
     */
    private static Boolean _state = true;

    // -------------------------------------------------------------------------

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

        Optional<Player> player = Optional.ofNullable(e.getEntity().getKiller());

        if (!_state) {
            getLogger().info(player + " killed the Ender Dragon but the plugin is toggled off.");
            return;
        }

        if (player.isPresent() && player.get().isOnline())
            DROPS_MANAGER.getRandomDrops().forEach(i -> giveCustomDrop(player.get(), i));
        else
            DROPS_MANAGER.getRandomDrops().forEach(this::randomlyDropInWorld);
    }

    /**
     * Gives the specified custom drop to the specified player.
     *
     * @param player    The player whom will receive the custom drop
     * @param itemStack The custom drop
     */
    private void giveCustomDrop(Player player, ItemStack itemStack) {
        String itemName = (itemStack.hasItemMeta()) ? itemStack.getItemMeta().getDisplayName() : itemStack.getType().toString();
        PlayerInventory playerInventory = player.getInventory();
        World world = player.getWorld();

        // PlayerInventory#addItem returns a HashMap containing any items that failed to add.
        // If the result is empty, everything was added to the player's inventory. If not,
        // we'll have to drop those naturally.

        HashMap<Integer, ItemStack> result = playerInventory.addItem(itemStack);

        if (result.isEmpty()) {
            Util.notifyPlayer(player, "You received a(n) " + itemName + " in your inventory!");
        } else {
            Location dropLoc = (Configuration.FAILSAFE_DROP_IN_PORTAL) ? Util.findEndPortal(world) : player.getLocation();
            result.values().forEach(i -> {
                world.dropItemNaturally(dropLoc, i);
                Util.notifyPlayer(player, "Your inventory was full! Your " + itemName + " has been dropped at " + Util.locationToOrderedTriple(dropLoc));
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
            world.dropItem(Util.getSafeDropLoc(spawnLoc).orElse(spawnLoc), itemStack);
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

} // NerdyDragon
