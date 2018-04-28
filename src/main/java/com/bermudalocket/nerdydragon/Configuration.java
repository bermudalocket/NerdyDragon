package com.bermudalocket.nerdydragon;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.bermudalocket.nerdydragon.NerdyDragon.DROPS_MANAGER;

/**
 * Handles the plugin configuration.
 */
class Configuration {

    static World DEFAULT_WORLD;

    static int DEFAULT_DROP_SEARCH_RADIUS;

    static Boolean FAILSAFE_DROP_IN_PORTAL;

    /**
     * Reloads the plugin configuration.
     */
    void reload() {
        NerdyDragon.PLUGIN.reloadConfig();
        FileConfiguration config = NerdyDragon.PLUGIN.getConfig();
        Logger logger = NerdyDragon.PLUGIN.getLogger();
        List<Map<?, ?>> customDrops = config.getMapList("custom-drops");

        // clear out the current drops to avoid duplicates
        DROPS_MANAGER.clear();

        // ---------------------------------------------------------------------
        // upon-fail-drop-in-portal -> FAILSAFE_DROP_IN_PORTAL
        // (upon error, will default to true)
        // ---------------------------------------------------------------------
        FAILSAFE_DROP_IN_PORTAL = config.getBoolean("upon-fail-drop-in-portal", true);

        // ---------------------------------------------------------------------
        // default-world -> DEFAULT_WORLD
        // (upon error, the plugin will be disabled)
        // ---------------------------------------------------------------------
        String tryWorld = config.getString("default-world");
        World DEFAULT_WORLD = NerdyDragon.PLUGIN.getServer().getWorld(tryWorld);
        if (DEFAULT_WORLD == null) {
            logger.warning("Fatal configuration error: specified default world (" + tryWorld + ") does not exist.");
            NerdyDragon.PLUGIN.getServer().getPluginManager().disablePlugin(NerdyDragon.PLUGIN);
        }

        // ---------------------------------------------------------------------
        // default-drop-search-radius -> DEFAULT_DROP_SEARCH_RADIUS
        // (upon error, the default radius will be set to 4 blocks)
        // ---------------------------------------------------------------------
        int tryRadius = config.getInt("default-drop-search-radius");
        if (tryRadius <= 0) {
            tryRadius = 4;
            logger.info("Configuration error: specified default drop search radius (" + tryRadius + ") " +
                    "should be an integer greater than 0. Defaulting to 4.");
        }
        DEFAULT_DROP_SEARCH_RADIUS = tryRadius;

        // ---------------------------------------------------------------------
        // custom-drops
        // ---------------------------------------------------------------------
        for (Map<?, ?> itemMap : customDrops) {

            // store information for the current custom drop
            String itemName;
            ArrayList<String> itemLore = new ArrayList<>();
            Material material;
            Enchantment enchantment;
            double dropRate;
            int dropQty;



            // ---------------------------------------------------------------------
            // name -> String
            // (upon error, this custom-drop will be skipped)
            // ---------------------------------------------------------------------
            Optional<String> tryName = Optional.ofNullable(itemMap.get("name"))
                                                .map(Object::toString)
                                                .map(this::format);
            if (!tryName.isPresent()) {
                logger.warning("Configuration error: name (" + tryName.orElse("N/A") + " is invalid.");
                continue;
            }

            // ---------------------------------------------------------------------
            // material -> Material
            // (upon error, this custom-drop will be skipped)
            // ---------------------------------------------------------------------
            Optional<Material> tryMaterial = Optional.ofNullable(itemMap.get("material"))
                                                    .map(Object::toString)
                                                    .map(Material::matchMaterial);
            if (!tryMaterial.isPresent()) {
                logger.warning("Configuration error: material for " + tryName + " is not a Material.");
                continue;
            }

            // ---------------------------------------------------------------------
            // drop-qty -> Integer
            // (upon error, this custom-drop will be skipped)
            // ---------------------------------------------------------------------
            Optional<Integer> tryQty = Optional.ofNullable(itemMap.get("drop-qty"))
                                                .map(Object::toString)
                                                .map(Integer::parseInt);
            try {
                if (tryQty.orElseThrow(IllegalArgumentException::new) < 0) throw new IllegalStateException();
            } catch (Exception e) {
                logger.warning("Configuration error: drop qty " + tryQty + " is not an integer.");
                continue;
            }

            // ---------------------------------------------------------------------
            // enchant -> Enchantment
            // (upon error, this custom-drop will default to ARROW_INFINITE)
            // ---------------------------------------------------------------------
            Optional<Enchantment> tryEnchant = Optional.ofNullable(itemMap.get("enchant"))
                                                        .map(Object::toString)
                                                        .map(Enchantment::getByName);

            // ---------------------------------------------------------------------
            // drop-rate -> Double
            // (upon error, this custom-drop will be skipped)
            // ---------------------------------------------------------------------
            Optional<Double> tryRate = Optional.ofNullable(itemMap.get("drop-rate"))
                                                .map(Object::toString)
                                                .map(Double::parseDouble);
            try {
                tryRate.filter(d -> d <= 1d && d >= 0d).orElseThrow(IllegalArgumentException::new);
            } catch (Exception e) {
                logger.warning("Configuration error: drop rate " + tryRate + " is not a Double in [0.0, 1.0].");
                continue;
            }

            // ---------------------------------------------------------------------
            // lore -> List<String>
            // (this is a little iffy)
            // ---------------------------------------------------------------------
            Optional<List<String>> tryLore = Optional.ofNullable(itemMap.get("lore"))
                                                .map(Configuration::objToList);
            tryLore.map(List::stream).map(s -> s.map(this::format)).map(s -> s.collect(Collectors.toList()));

            // ---------------------------------------------------------------------
            // create the item stack
            // ---------------------------------------------------------------------
            ItemStack itemStack = new ItemStack(tryMaterial.get(), tryQty.get());

            ItemMeta itemMeta = itemStack.getItemMeta();
            itemMeta.setLore(tryLore.orElse(null));
            itemMeta.setDisplayName(tryName.get());
            itemStack.setItemMeta(itemMeta);

            itemStack.addUnsafeEnchantment(tryEnchant.orElse(Enchantment.ARROW_INFINITE), 1);

            // send the item stack to the drops manager to be saved
            DROPS_MANAGER.loadNewItem(itemStack, tryRate.get());
            logger.info("Added " + tryName.get() + " to drops list.");
            logger.info("Lore: " + tryLore.get());

        } // for custom-drop

    } // reload

    /**
     * Translates the character & into the section symbol for color coding
     *
     * @param string The string to transform
     * @return The transformed string
     */
    private String format(String string) {
        return ChatColor.translateAlternateColorCodes("&".charAt(0), string);
    }

    private static List<String> objToList(Object o) {
        return (List<String>) o;
    }

}
