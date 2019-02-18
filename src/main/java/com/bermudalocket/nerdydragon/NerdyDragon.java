package com.bermudalocket.nerdydragon;

import com.bermudalocket.nerdydragon.commands.ExecutorBase;
import com.bermudalocket.nerdydragon.commands.FightCommand;
import com.bermudalocket.nerdydragon.commands.PluginStateCommand;
import com.bermudalocket.nerdydragon.commands.ReloadCommand;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
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

    // ------------------------------------------------------------------------
    /**
     * The current fight, or null if one does not exist. Note that the current
     * fight might have been completed since the last restart.
     */
    private static EnderDragonFight _currentFight;

    // ------------------------------------------------------------------------
    /**
     * @see JavaPlugin#onEnable().
     */
    public void onEnable() {
        PLUGIN = this;

        saveDefaultConfig();
        Configuration.reload();

        getServer().getPluginManager().registerEvents(this, this);

        registerCommand(new ReloadCommand());
        registerCommand(new FightCommand());
        registerCommand(new PluginStateCommand());

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
    public static EnderDragonFight getCurrentFight() {
        return _currentFight;
    }

    // ------------------------------------------------------------------------
    /**
     * Clears the current fight freeing it from memory.
     */
    static void clearCurrentFight() {
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
        if (!Configuration.ENABLED) {
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
