package com.bermudalocket.nerdydragon;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

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
            // material -> Material
            // (upon error, this custom-drop will be skipped)
            // ---------------------------------------------------------------------
            String tryItemMaterial = (String) itemMap.get("material");
            try {
                material = Material.valueOf(tryItemMaterial);
            } catch (Exception e) {
                logger.warning("Configuration error: material (" + tryItemMaterial + ") is not a Material.");
                continue;
            }

            // ---------------------------------------------------------------------
            // drop-qty -> Integer
            // (upon error, this custom-drop will be skipped)
            // ---------------------------------------------------------------------
            dropQty = 0;
            try {
                dropQty = (Integer) itemMap.get("drop-qty");
                if (dropQty < 0) throw new IllegalStateException();
            } catch (Exception e) {
                logger.warning("Configuration error: drop qty " + dropQty + " is not an integer.");
                continue;
            }

            // ---------------------------------------------------------------------
            // name-color -> ChatColor
            // (upon error, this custom-drop will be skipped)
            // ---------------------------------------------------------------------
            String tryNameColor = (String) itemMap.get("name-color");
            try {
                itemName = String.format("%s%s%s", ChatColor.valueOf(tryNameColor), itemMap.get("name"), ChatColor.RESET);
            } catch (Exception e) {
                logger.warning("Configuration error: color " + tryNameColor + " is not a ChatColor.");
                continue;
            }

            // ---------------------------------------------------------------------
            // enchant -> Enchantment
            // (upon error, this custom-drop will default to NO enchantment)
            // ---------------------------------------------------------------------
            String tryEnchant = (String) itemMap.get("enchant");
            enchantment = Enchantment.getByName(tryEnchant);

            // ---------------------------------------------------------------------
            // drop-rate -> Double
            // (upon error, this custom-drop will be skipped)
            // ---------------------------------------------------------------------
            dropRate = 0d;
            try {
                dropRate = (Double) itemMap.get("drop-rate");
                if (dropRate < 0 || dropRate > 1) throw new IllegalStateException();
            } catch (NumberFormatException nFE) {
                logger.warning("Configuration error: drop rate " + dropRate + " is not a Double in [0.0, 1.0].");
                continue;
            }

            // ---------------------------------------------------------------------
            // lore -> List<String>
            // (this is a little fucky)
            // ---------------------------------------------------------------------
            List<?> tryLore = (List<?>) itemMap.get("lore");

            // create the item stack and grab its item meta
            ItemStack itemStack = new ItemStack(material, dropQty);
            ItemMeta itemMeta = itemStack.getItemMeta();

            tryLore.stream().map(Object::toString).map(this::format).forEach(itemLore::add);
            if (!itemLore.isEmpty()) itemMeta.setLore(itemLore);

            itemMeta.setDisplayName(itemName);

            itemStack.setItemMeta(itemMeta);

            if (enchantment != null) itemStack.addUnsafeEnchantment(enchantment, 1);

            // send the item stack to the drops manager to be saved
            DROPS_MANAGER.loadNewItem(itemStack, dropRate);
            logger.info("Added " + itemName + " to drops list.");

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

}
