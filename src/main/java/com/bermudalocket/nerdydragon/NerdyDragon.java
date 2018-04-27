package com.bermudalocket.nerdydragon;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;

public class NerdyDragon extends JavaPlugin implements Listener {

    static NerdyDragon PLUGIN;

    private static Configuration CONFIGURATION = new Configuration();

    static DropsManager DROPS_MANAGER = new DropsManager();

    public void onEnable() {
        PLUGIN = this;

        getServer().getPluginManager().registerEvents(this, this);

        saveDefaultConfig();
        CONFIGURATION.reload();
    }

    // *************************************************************************

    @EventHandler
    public void onEntityDeath(EntityDeathEvent e) {
        if (!e.getEntityType().equals(EntityType.ENDER_DRAGON)) return;

        Player player = e.getEntity().getKiller();
        if (player != null && player.isOnline()) {
            DROPS_MANAGER.getRandomDrops().forEach(i -> giveCustomDrop(player, i));
        } else {
            DROPS_MANAGER.getRandomDrops().forEach(i -> naturallyDropCustomDrop(Configuration.DEFAULT_WORLD, i));
        }
    }

    // *************************************************************************

    private void giveCustomDrop(Player player, ItemStack itemStack) {
        if (!player.isOnline()) return;

        PlayerInventory playerInventory = player.getInventory();

        // playerInventory#addItem returns a HashMap containing any items that
        // couldn't be added, which is pretty nice
        HashMap<Integer, ItemStack> result = playerInventory.addItem(itemStack);

        // if the result is empty, everything went well; if not, let's drop those naturally
        if (result.isEmpty()) {
            PLUGIN.getLogger().info("Successfully gave a custom dragon drop ("
                   + itemStack.getItemMeta().getDisplayName() + ") to " + player.getName());
        } else {
            // naturally drop at the player's feet (this is distinct from naturallyDropCustomDrop)
            result.values().forEach(i -> {
                player.getWorld().dropItemNaturally(player.getLocation(), i);
                PLUGIN.getLogger().info(player.getName() + "'s inventory is full! Dropping "
                        + itemStack.getItemMeta().getDisplayName() + " naturally at their position.");
            });
        }
    }

    private void naturallyDropCustomDrop(World world, ItemStack itemStack) {
        if (world == null) return;

        // let's drop the item naturally in the default world
        Location dropLoc = world.getSpawnLocation();

        for (int i = -6; i <= 6; i++) {
            for (int j = -6; j <= 6; j++){

                Material blockType = world.getHighestBlockAt(dropLoc).getType();
                if (blockType != Material.FIRE && blockType != Material.END_GATEWAY) {
                    DROPS_MANAGER.getRandomDrops().forEach(customDrop -> {
                        world.dropItemNaturally(dropLoc, customDrop);
                        PLUGIN.getLogger().info("Player logged out? Dropping "
                                + customDrop.getItemMeta().getDisplayName() + " at ("
                                + dropLoc.getX() + ","
                                + dropLoc.getY() + ","
                                + dropLoc.getZ() + ")");
                        });
                }

            } // for j (z)
        } // for i (x)

    }

} // NerdyDragon
