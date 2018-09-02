package com.bermudalocket.nerdydragon;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

final class LootTable {

    /**
     * A mapping from the ItemStack of a custom drop to its drop rate
     *
     * f : ItemStack customDrop -> Double dropRate
     */
    private final HashMap<ItemStack, Double> _lootTable = new HashMap<>();

    /**
     * A static Random object.
     */
    private static final Random RANDOM = new Random();

    /**
     * Adds a given item stack to the loot table with the given probability.
     *
     * @param itemStack the item.
     * @param probability the probability.
     */
    void addLoot(ItemStack itemStack, double probability) {
        _lootTable.put(itemStack, probability);
    }

    /**
     * Runs a probability filter and returns drops.
     *
     * @return a set of random drops.
     */
    Set<ItemStack> getLoot() {
        return _lootTable.entrySet().stream()
                                    .filter(e -> e.getValue() >= RANDOM.nextDouble())
                                    .map(Map.Entry::getKey)
                                    .collect(Collectors.toSet());
    }

    /**
     * Returns all custom drops with drop rates.
     *
     * @return all custom drops with drop rates.
     */
    HashMap<ItemStack, Double> getTable() {
        return _lootTable;
    }

    /**
     * Returns all custom drops without drop rates.
     *
     * @return all custom drops without drop rates.
     */
    Set<ItemStack> getAllLoot() {
        return _lootTable.keySet();
    }

    /**
     * Loads saved loot from serialized config.
     *
     * @param configurationSection the configuration section to load from.
     */
    void load(ConfigurationSection configurationSection) {
        _lootTable.clear();
        for (String id : configurationSection.getKeys(false)) {
            ItemStack itemStack = configurationSection.getItemStack(id);
            if (itemStack != null && itemStack.getType() != Material.AIR) {
                double probability = configurationSection.getDouble(id + ".probability");
                _lootTable.put(itemStack, probability);
                NerdyDragon.log("Successfully loaded loot: (" + probability + "% drop chance) " + itemStack);
            } else {
                NerdyDragon.log("Failed to load loot: (" + id + ") " + itemStack);
            }
        }
    }

    /**
     * Saves loot table to configuration.
     *
     * @param configurationSection the configuration section.
     */
    void save(ConfigurationSection configurationSection) {
        NerdyDragon.log("Saving loot table...");
        for (ItemStack itemStack : _lootTable.keySet()) {
            double probability = _lootTable.get(itemStack);
            String key = "custom-drops." + UUID.randomUUID();
            configurationSection.set(key, itemStack);
            configurationSection.set(key + ".probability", probability);
            NerdyDragon.log("-> " + itemStack + "(" + probability + ")");
        }
        NerdyDragon.log("Saved!");
    }

}