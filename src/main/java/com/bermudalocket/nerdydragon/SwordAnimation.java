package com.bermudalocket.nerdydragon;

import com.google.common.collect.Lists;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.ArmorStand;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.List;

public class SwordAnimation {

    private static final float SEPARATOR = Math.round(Math.PI / 8f);
    private static final float RAD_PER_SEC = 1.5F;
    private static final float RAD_PER_TICK = RAD_PER_SEC / 20F;

    private Location center;
    private double radius;

    private List<ArmorStand> swords;

    int tick = 0;

    private static final Vector X_UNIT_VEC = new Vector(1, 0, 0);

    public SwordAnimation(Location center, double radius) {
        this.center = center;
        this.radius = radius;
        swords = Lists.newArrayList();
    }

    public void start() {
        for (double angle = 0; angle < Math.PI * 2; angle += SEPARATOR) {
            spawnStand(angle);
        }
        Bukkit.getScheduler().runTaskTimer(NerdyDragon.PLUGIN, () -> {
            tick++;
            for (int i = 0; i < swords.size(); i++) {
                ArmorStand armorStand = swords.get(i);
                Location nextLoc = getLocationInCircle(RAD_PER_TICK * tick + SEPARATOR * i);
                armorStand.setVelocity(X_UNIT_VEC);
                armorStand.teleport(nextLoc);
            }
        }, 0L, 1L);
    }

    private void spawnStand(double angle) {
        Location loc = getLocationInCircle(angle);
        ArmorStand stand = loc.getWorld().spawn(loc, ArmorStand.class);
        stand.setVisible(false);
        stand.setHelmet(new ItemStack(Material.DIAMOND_SWORD));
        swords.add(stand);
    }

    private Location getLocationInCircle(double angle) {
        double x = center.getX() + radius * Math.cos(angle);
        double z = center.getZ() + radius * Math.sin(angle);
        return new Location(center.getWorld(), x, center.getY(), z);
    }

}