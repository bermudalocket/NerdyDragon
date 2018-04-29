package com.bermudalocket.nerdydragon;

import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

class DropsManager {

    /**
     * A mapping from the ItemStack of a custom drop to its drop rate
     *
     * f : ItemStack customDrop -> Double dropRate
     */
    private HashMap<ItemStack, Double> _customDrops = new HashMap<>();

    private Random _random = new Random();

    /**
     * Enters a new pair into the custom drop mapping
     *
     * @param itemStack the ItemStack representation of this custom drop
     * @param dropRate  the drop rate for this custom drop
     */
    void loadNewItem(ItemStack itemStack, double dropRate) {
        _customDrops.put(itemStack, dropRate);
    }

    /**
     * Runs a probability filter and returns drops
     *
     * @return a set of random drops
     */
    Set<ItemStack> getRandomDrops() {
        return _customDrops.entrySet().stream()
                .filter(e -> e.getValue() >= _random.nextDouble())
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
    }

    /**
     * Returns a copy of the custom drops HashMap
     *
     * @return a HashMap of all custom drops and their drop rates
     */
    HashMap<ItemStack, Double> getAllDropsInfo() {
        return _customDrops;
    }

    /**
     * Returns a Set of all custom drops *without drop rates*
     *
     * @return a Set of all custom drops without their drop rates
     */
    Set<ItemStack> getAllDrops() {
        return _customDrops.keySet();
    }

    /**
     * Clears the current HashMap
     */
    void clear() {
        _customDrops.clear();
    }

}
