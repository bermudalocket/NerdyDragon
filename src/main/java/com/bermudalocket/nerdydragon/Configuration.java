package com.bermudalocket.nerdydragon;

import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.EnderCrystal;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Handles the plugin configuration.
 */
public class Configuration {

    /**
     * If this plugin's hook (CreatureSpawnEvent in main class) is active.
     */
    public boolean ENABLED;

    public static boolean NERF_DROPS = true;

    public static double DROP_NERF_RATE = 0.6;

    /**
     * The set of worlds in which the dragon death sound will be played.
     */
    private HashSet<UUID> MIRROR_DRAGON_DEATH_WORLDS = new HashSet<>();

    /**
     * The locations at which the End Crystals spawn at the start of a dragon fight.
     */
    HashSet<Location> ENDER_CRYSTAL_PILLAR_LOCATIONS = new HashSet<>();

    // ------------------------------------------------------------------------
    /**
     * Constructor.
     */
    public Configuration() {
        reload();
    }

    // ------------------------------------------------------------------------
    /**
     * Reloads the plugin configuration.
     */
    public void reload() {
        NerdyDragon.log("Reloading configuration.");
        NerdyDragon.PLUGIN.saveDefaultConfig();
        NerdyDragon.PLUGIN.reloadConfig();
        FileConfiguration config = NerdyDragon.PLUGIN.getConfig();

        ENABLED = config.getBoolean("enabled", true);

        NERF_DROPS = config.getBoolean("nerf-drops", true);
        DROP_NERF_RATE = config.getDouble("nerf-rate", 0.6);

        MIRROR_DRAGON_DEATH_WORLDS = config.getStringList("mirror-dragon-death-sound")
                                           .stream()
                                           .map(Bukkit::getWorld)
                                           .filter(Objects::nonNull)
                                           .map(World::getUID)
                                           .collect(Collectors.toCollection(HashSet::new));

        for (String locString : config.getStringList("ender-crystal-pillar-locations")) {
            String[] parts = locString.split(",");
            int x = Integer.parseInt(parts[0]);
            int y = Integer.parseInt(parts[1]);
            int z = Integer.parseInt(parts[2]);
            NerdyDragon.log("Loaded regeneration point: (" + x + ", " + y + ", " + z + ")");
            ENDER_CRYSTAL_PILLAR_LOCATIONS.add(new Location(Util.WORLD_THE_END, x, y, z));
        }

        NerdyDragon.log("Configuration successfully reloaded.");
    }

    // ------------------------------------------------------------------------
    /**
     * Soft-enables/disables the plugin, i.e. the main CreatureSpawnEvent
     * listener in the main plugin class will short-circuit.
     */
    public void setEnabled(boolean enabled) {
        ENABLED = enabled;
        NerdyDragon.PLUGIN.getConfig().set("enabled", enabled);
        NerdyDragon.PLUGIN.saveConfig();
    }

    // ------------------------------------------------------------------------
    /**
     * Returns an immutable set of the dragon fight loot.
     * TODO - make configurable
     *
     * @return the loot.
     */
    public ArrayList<ItemStack> getLoot() {
        ItemStack wings = new ItemStack(Material.ELYTRA, 1);
        ItemMeta meta = wings.getItemMeta();
        meta.setLore(Collections.singletonList(ChatColor.DARK_PURPLE + "I Survived The Dragon Fight!"));
        wings.setItemMeta(meta);
        return new ArrayList<>(Arrays.asList(wings, new ItemStack(Material.DRAGON_HEAD, 1)));
    }

    // ------------------------------------------------------------------------
    /**
     * Saves the spawn locations of the crystals atop the end pillars for
     * regeneration.
     *
     * @param locations the locations to save.
     */
    void saveEnderCrystalPillarLocations(HashSet<EnderCrystal> locations) {
        ENDER_CRYSTAL_PILLAR_LOCATIONS.clear();
        FileConfiguration config = NerdyDragon.PLUGIN.getConfig();
        List<String> stringList = new ArrayList<>();
        for (EnderCrystal crystal : locations) {
            Location location = crystal.getLocation();
            ENDER_CRYSTAL_PILLAR_LOCATIONS.add(location);
            String coordString = location.getBlockX() + "," + location.getBlockY() + "," + location.getBlockZ();
            NerdyDragon.log("Saved regeneration point: (" + coordString + ")");
            stringList.add(coordString);
        }
        config.set("ender-crystal-pillar-locations", stringList);
        NerdyDragon.PLUGIN.saveConfig();
    }

    // ------------------------------------------------------------------------
    /**
     * Returns a set of worlds in which the dragon death sound should be played.
     *
     * @return a set of worlds in which the dragon death sound should be played.
     */
    HashSet<World> getMirrorWorlds() {
        return MIRROR_DRAGON_DEATH_WORLDS.stream()
            .map(Bukkit::getWorld)
            .filter(Objects::nonNull)
            .collect(Collectors.toCollection(HashSet::new));
    }

    // ------------------------------------------------------------------------
    /**
     * Returns the requested configuration section. If the section does not
     * exist, it is created.
     *
     * @param key the name of the section.
     * @return the configuration section.
     */
    static ConfigurationSection getOrCreateSection(String key) {
        FileConfiguration config = NerdyDragon.PLUGIN.getConfig();
        ConfigurationSection section = config.getConfigurationSection(key);
        if (section == null) {
            section = config.createSection(key);
        }
        return section;
    }

}
