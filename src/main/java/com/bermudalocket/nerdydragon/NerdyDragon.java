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
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.plugin.java.JavaPlugin;

// ------------------------------------------------------------------------
/**
 * The main plugin and event-handling class.
 */
public class NerdyDragon extends JavaPlugin implements Listener {

    /**
     * This plugin.
     */
    public static NerdyDragon PLUGIN;

    /**
     * This plugin's configuration.
     */
    public static Configuration CONFIG;

    /**
     * Manages the leaderboard: the getting and saving of fight history and
     * statistics.
     */
    public static Leaderboard LEADERBOARD;

    /**
     * The current fight or null if one does not exist.
     *
     * It doesn't really make much sense to NOT make this a global variable
     * since it's not the intention of this plugin to facilitate multiple
     * dragon fight instances. The NMS code also only allows for one instance
     * as well.
     */
    public static EnderDragonFight FIGHT;

    // ------------------------------------------------------------------------
    /**
     * @see JavaPlugin#onEnable().
     */
    public void onEnable() {
        PLUGIN = this;
        CONFIG = new Configuration();
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
            Thread.newThread(5, () -> FIGHT = new EnderDragonFight(savedFight));
        }
    }

    // ------------------------------------------------------------------------
    /**
     * @see JavaPlugin#onDisable().
     */
    public void onDisable() {
        ConfigurationSection serialize = Configuration.getOrCreateSection("saved-fight");
        if (FIGHT != null && FIGHT.getStage() != FightStage.FINISHED) {
            FIGHT.save(serialize);
        } else {
            getConfig().set("saved-fight", null);
            saveConfig();
        }
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
            log("[DRAGON SPAWN] Caught EnderDragon spawn at " + Util.locationToOrderedTriple(e.getLocation()));
            if (FIGHT != null && FIGHT.getStage() != FightStage.FINISHED) {
                log("[DRAGON SPAWN] Fight in progress. Attempting to update the dragon without crashing and burning...");
                FIGHT.updateDragon((EnderDragon) e.getEntity());
                log("[DRAGON SPAWN] ... hopefully that worked.");
                return;
            }
            Thread.newThread(() -> FIGHT = new EnderDragonFight((EnderDragon) e.getEntity()));
        }
    }

    // ------------------------------------------------------------------------
    /**
     * Catch and log all dragon deaths.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent e) {
        if (!NerdyDragon.CONFIG.ENABLED || e.getEntity().getWorld() != Util.WORLD_THE_END) {
            return;
        }
        if (e.getEntityType() == EntityType.ENDER_DRAGON) {
            log("[DRAGON DEATH] Dragon has UUID " + e.getEntity().getUniqueId().toString() +
                ", killer " + e.getEntity().getKiller());
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
        if (((EnderDragon) e.getEntity()).getHealth() > 0) {
            log("Dragon is attempting to unload (" + e.getEntity().getUniqueId().toString() + ")");
            final Location location = e.getEntity().getLocation().clone();
            Thread.newThread(() -> {
                if (!location.isChunkLoaded()) {
                    location.getChunk().setForceLoaded(true);
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

    /**
     * A pre-formatted log prefix for this plugin.
     */
    public static final String PREFIX = String.format("%s[%sNerdyDragon%s] %s",
                                                      ChatColor.DARK_GRAY,
                                                      ChatColor.DARK_PURPLE,
                                                      ChatColor.DARK_GRAY,
                                                      ChatColor.GRAY);

}
