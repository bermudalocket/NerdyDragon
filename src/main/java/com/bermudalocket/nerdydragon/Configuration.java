package com.bermudalocket.nerdydragon;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;

import static com.bermudalocket.nerdydragon.NerdyDragon.DROPS_MANAGER;

/**
 * Handles the plugin configuration.
 */
class Configuration {

	static World DEFAULT_WORLD;

	/**
	 * Reloads the plugin configuration.
	 */
	void reload() {
		FileConfiguration config = NerdyDragon.PLUGIN.getConfig();
		Logger logger = NerdyDragon.PLUGIN.getLogger();
		List<Map<?, ?>> customDrops = config.getMapList("custom-drops");

		String tryWorld = config.getString("default-world");
		World DEFAULT_WORLD = NerdyDragon.PLUGIN.getServer().getWorld(tryWorld);
		if (DEFAULT_WORLD == null) {
			logger.info("Configuration error: specified default world (" + tryWorld + ") does not exist");
		}

		for (Map<?, ?> itemMap : customDrops) {

			// store information for the current custom drop
			String itemName;
			LinkedHashSet<String> itemLore = new LinkedHashSet<>();
			Material material;
			Optional<Enchantment> enchantment;
			double dropRate;
			int dropQty;

			String tryItemMaterial = (String) itemMap.get("material");
			try {
				material = Material.valueOf(tryItemMaterial);
			} catch (Exception e) {
				logger.warning("Configuration error: material (" + tryItemMaterial + ") is not a Material.");
				continue;
			}

			String tryDropQty = (String) itemMap.get("drop-qty");
			try {
				dropQty = Integer.valueOf(tryDropQty);
				if (dropQty < 0) throw new IllegalStateException();
			} catch (NumberFormatException nFE) {
				logger.warning("Configuration error: drop qty " + tryDropQty + " is not an integer.");
				dropQty = 1;
			} catch (IllegalStateException iSE) {
				logger.warning("Configuration error: drop rate " + tryDropQty + " is not a positive integer.");
				dropQty = 1;
			}

			String tryNameColor = (String) itemMap.get("name-color");
			try {
				itemName = String.format("%s%s%s", ChatColor.valueOf(tryNameColor), (String) itemMap.get("name"), ChatColor.RESET);
			} catch (Exception e) {
				itemName = (String) itemMap.get("name");
				logger.warning("Configuration error: color " + tryNameColor + " is not a ChatColor.");
			}

			String tryEnchant = (String) itemMap.get("enchant");
			enchantment = Optional.ofNullable(Enchantment.getByName(tryEnchant));

			String tryDropRate = (String) itemMap.get("drop-rate");
			try {
				dropRate = Double.valueOf(tryDropRate);
				if (dropRate < 0 || dropRate > 1) throw new IllegalStateException();
			} catch (NumberFormatException nFE) {
				logger.warning("Configuration error: drop rate " + tryDropRate + " is not a Double.");
				dropRate = 1d;
			} catch (IllegalStateException iSE) {
				logger.warning("Configuration error: drop rate " + tryDropRate + " is not a Double in [0,1].");
				dropRate = 1d;
			}

			// this should be ok...
			List<String> tryLore = (List<String>) itemMap.get("lore");
			tryLore.stream().map(this::format).forEach(itemLore::add);

			// create the item stack and grab its item meta
			ItemStack itemStack = new ItemStack(material, dropQty);
			ItemMeta itemMeta = itemStack.getItemMeta();

			itemStack.addEnchantment(enchantment.orElse(Enchantment.ARROW_INFINITE), 1);

			itemMeta.setDisplayName(itemName);
			itemMeta.getLore().addAll(itemLore);
			itemStack.setItemMeta(itemMeta);

			// send the item stack to the drops manager to be saved
			DROPS_MANAGER.loadNewItem(itemStack, dropRate);
		}
	}

	private String format(String string) {
		return ChatColor.translateAlternateColorCodes("&".charAt(0), string);
	}

}
