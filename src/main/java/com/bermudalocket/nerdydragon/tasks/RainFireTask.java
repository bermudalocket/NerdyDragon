package com.bermudalocket.nerdydragon.tasks;

import com.bermudalocket.nerdydragon.EnderDragonFight;
import com.bermudalocket.nerdydragon.Thread;
import com.bermudalocket.nerdydragon.Util;
import com.bermudalocket.nerdydragon.util.OrderedPair;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

// ------------------------------------------------------------------------
/**
 * A task performed by the EnderDragon in the final stage of the battle.
 * Causes fireballs to rain down from the sky.
 */
public class RainFireTask extends AbstractFightTask {

    private static final Vector DOWN_VECTOR = new Vector(0, -3.25, 0);

    private static long _lastRan = 0;

    public RainFireTask(EnderDragonFight fight) {
        super(fight, true);
    }

    @Override
    public void run() {
        if (System.currentTimeMillis() - _lastRan <= 25*1000) {
            return;
        }

        _lastRan = System.currentTimeMillis();
        World world = _fight.getWorld();

        Thread.newRepeatedThread(4, 20, () -> {
            for (Player player : _fight.getNearbyPlayers()) {
                player.sendTitle(ChatColor.RED + "INCOMING ATTACK", "", 1, 8, 1);
                _fight.playSound(Sound.BLOCK_NOTE_BLOCK_PLING, 1);
            }
        });

        Thread.newThread(6, () -> {
            _fight.playSound(Sound.ENTITY_WITHER_SPAWN, 0.7f);
            Thread.newRepeatedThread(25, 40, 5, () -> {
                OrderedPair<Integer> coords = Util.getRandomCoordinates(40);
                int x = coords.getA();
                int z = coords.getB();
                Location loc = new Location(world, x, 150, z);
                Fireball fireball = (Fireball) _fight.spawnReinforcement(loc, EntityType.FIREBALL);
                fireball.setYield(3f);
                fireball.setDirection(DOWN_VECTOR);
            });
        });
    }

}
