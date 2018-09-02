package com.bermudalocket.nerdydragon;

import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.logging.Logger;

import static com.bermudalocket.nerdydragon.NerdyDragon.PLUGIN;

/**
 * Handles the plugin configuration.
 */
class Configuration {

    /**
     * The default world, usually world_the_end.
     */
    static World DEFAULT_WORLD;

    /**
     * Defines the radius of the square search area when safely dropping loot.
     */
    static int DEFAULT_DROP_SEARCH_RADIUS;

    /**
     * Reloads the plugin configuration.
     */
    static void reload(boolean saveFirst) {
        NerdyDragon.log("Reloading configuration.");
        if (saveFirst) {
            save();
        }
        PLUGIN.reloadConfig();
        FileConfiguration config = PLUGIN.getConfig();
        Logger logger = PLUGIN.getLogger();

        ConfigurationSection lootSection = getOrCreateSection("custom-drops", config);
        NerdyDragon.PLUGIN.getLootTable().load(lootSection);

        String worldName = config.getString("default-word");
        if (worldName != null) {
            World world = NerdyDragon.PLUGIN.getServer().getWorld(worldName);
            if (world != null) {
                DEFAULT_WORLD = world;
            } else {
                logger.warning("Fatal configuration error: specified default world (" + worldName + ") does not exist.");
                PLUGIN.getServer().getPluginManager().disablePlugin(PLUGIN);
            }
        }

        int radius = config.getInt("default-drop-search-radius", 0);
        DEFAULT_DROP_SEARCH_RADIUS = (radius > 0) ? radius : 4;
        if (radius <= 0) {
            logger.info("Configuration error: specified default drop search radius (" + radius + ") " +
                                "should be an integer greater than 0. Defaulting to 4.");
        }

        NerdyDragon.log("Configuration successfully reloaded.");
    }

    /**
     * Saves the plugin state.
     */
    static void save() {
        ConfigurationSection lootSection = getOrCreateSection("custom-drops", PLUGIN.getConfig());
        NerdyDragon.PLUGIN.getLootTable().save(lootSection);
    }

    /**
     * Returns the requested configuration section. If the section does not exist, it is created.
     *
     * @param section the name of the section.
     * @param parent the parent.
     * @return the configuration section.
     */
    private static ConfigurationSection getOrCreateSection(String section, ConfigurationSection parent) {
        return parent.getConfigurationSection(section) != null ? parent.getConfigurationSection(section)
                                                               : parent.createSection(section);
    }

}
