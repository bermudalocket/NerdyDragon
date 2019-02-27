package com.bermudalocket.nerdydragon;

import com.bermudalocket.nerdydragon.commands.ExecutorBase;
import com.bermudalocket.nerdydragon.commands.FightCommand;
import com.bermudalocket.nerdydragon.commands.LeaderboardCommand;
import com.bermudalocket.nerdydragon.commands.PluginStateCommand;
import com.bermudalocket.nerdydragon.commands.ReloadCommand;
import com.destroystokyo.paper.event.entity.EntityRemoveFromWorldEvent;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.plugin.java.JavaPlugin;

// ------------------------------------------------------------------------
/**
 * The main plugin and event-handling class.
 */
public class NerdyDragon extends JavaPlugin implements Listener {

    // ------------------------------------------------------------------------
    /**
     * This plugin.
     */
    public static NerdyDragon PLUGIN;

    public static Configuration CONFIG;

    public static Leaderboard LEADERBOARD;

    // ------------------------------------------------------------------------
    /**
     * The current fight, or null if one does not exist. Note that the current
     * fight might have been completed since the last restart.
     */
    private EnderDragonFight _currentFight;

    // ------------------------------------------------------------------------
    /**
     * @see JavaPlugin#onEnable().
     */
    public void onEnable() {
        PLUGIN = this;

        saveDefaultConfig();
        CONFIG = new Configuration();
        CONFIG.reload();

        LEADERBOARD = new Leaderboard();

        getServer().getPluginManager().registerEvents(this, this);

        registerCommand(new ReloadCommand());
        registerCommand(new FightCommand());
        registerCommand(new PluginStateCommand());
        registerCommand(new LeaderboardCommand());

        checkForExistingFight();
    }

    // ------------------------------------------------------------------------
    /**
     * Checks config for a serialized fight and loads it if present.
     */
    private void checkForExistingFight() {
        ConfigurationSection savedFight = getConfig().getConfigurationSection("saved-fight");
        if (savedFight != null) {
            Thread.newThread(5, () -> _currentFight = new EnderDragonFight(savedFight));
        }
    }

    // ------------------------------------------------------------------------
    /**
     * @see JavaPlugin#onDisable().
     */
    public void onDisable() {
        ConfigurationSection serialize = Configuration.getOrCreateSection("saved-fight");
        if (_currentFight != null && _currentFight.getStage() != FightStage.FINISHED) {
            _currentFight.save(serialize);
        } else {
            getConfig().set("saved-fight", null);
            saveConfig();
        }
    }

    // ------------------------------------------------------------------------
    /**
     * Returns the current fight, or null if one does not exist.
     *
     * @return the current fight, or null if one does not exist.
     */
    public EnderDragonFight getCurrentFight() {
        return _currentFight;
    }

    // ------------------------------------------------------------------------
    /**
     * Clears the current fight, freeing it from memory.
     */
    void clearCurrentFight() {
        _currentFight = null;
    }

    // ------------------------------------------------------------------------
    /**
     * Registers the command-handling and tab-completion for the given command.
     *
     * @param executorBase the command to register.
     */
    private void registerCommand(ExecutorBase executorBase) {
        PluginCommand command = getServer().getPluginCommand(executorBase.getName());
        command.setExecutor(executorBase);
        command.setTabCompleter(executorBase);
    }

    // ------------------------------------------------------------------------
    /**
     * Catch dragon spawns.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntitySpawn(CreatureSpawnEvent e) {
        if (!NerdyDragon.CONFIG.ENABLED || e.getLocation().getWorld() != Util.WORLD_THE_END) {
            return;
        }
        if (e.getEntityType() == EntityType.ENDER_DRAGON) {
            if (_currentFight != null && _currentFight.getStage() != FightStage.FINISHED) {
                _currentFight.updateDragon((EnderDragon) e.getEntity());
                return;
            }
            Thread.newThread(() -> _currentFight = new EnderDragonFight((EnderDragon) e.getEntity()));
        }
    }

    // ------------------------------------------------------------------------
    /**
     * Log players placing crystals.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerPlaceCrystal(PlayerInteractEvent e) {
        if (e.getMaterial() == Material.END_CRYSTAL && e.getAction() == Action.RIGHT_CLICK_BLOCK) {
            Player player = e.getPlayer();
            if (player.getWorld() == Util.WORLD_THE_END && e.getClickedBlock().getType() == Material.BEDROCK) {
                log(player.getName() + " placed a crystal at " + Util.locationToOrderedTriple(e.getClickedBlock().getLocation()));
            }
        }
    }

    // ------------------------------------------------------------------------
    /**
     * Prevents the dragon from being unloaded, which seems to cause silliness.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDragonUnload(EntityRemoveFromWorldEvent e) {
        if (e.getEntityType() != EntityType.ENDER_DRAGON) {
            return;
        }
        log("Dragon is attempting to unload (" + e.getEntity().getUniqueId().toString() + ")");
        if (((EnderDragon) e.getEntity()).getHealth() > 0) {
            final Location location = e.getEntity().getLocation().clone();
            Thread.newThread(() -> {
                if (!location.isChunkLoaded()) {
                    log("Reloading chunk...");
                    location.getChunk().load();
                }
            });
        }
    }

    // ------------------------------------------------------------------------
    /**
     * A logging convenience method, used instead of {@link java.util.logging.Logger}
     * for colorizing this plugin's name in console.
     *
     * @param message the message to log.
     */
    public static void log(String message) {
        System.out.println(PREFIX + message);
    }

    // ------------------------------------------------------------------------
    /**
     * Sends the player a message and logs it to console.
     *
     * @param player the player.
     * @param message the message.
     */
    public static void message(Player player, String message) {
        player.sendMessage(PREFIX + message);
        log("Sent " + player.getName() + " a message: " + message);
    }

    // ------------------------------------------------------------------------
    /**
     * Sends the CommandSender a message and logs it to console.
     *
     * @param sender the sender.
     * @param message the message.
     */
    public static void message(CommandSender sender, String message) {
        sender.sendMessage(PREFIX + message);
        log("Sent " + sender.getName() + " a message: " + message);
    }

    // ------------------------------------------------------------------------
    /**
     * A pre-formatted log prefix for this plugin.
     */
    public static final String PREFIX = String.format("%s[%sNerdyDragon%s] %s",
                                                      ChatColor.DARK_GRAY,
                                                      ChatColor.DARK_PURPLE,
                                                      ChatColor.DARK_GRAY,
                                                      ChatColor.GRAY);

}
