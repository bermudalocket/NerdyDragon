/*
 * Copyright (c) 2019 bermudalocket. All rights reserved.
 * Unauthorized copying or distribution of this item without permission of the author is prohibited.
 * Proprietary and Confidential
 * Written by bermudalocket, 2019.
 */
package com.bermudalocket.nerdydragon.tasks;

import com.bermudalocket.nerdydragon.EnderDragonFight;
import com.bermudalocket.nerdydragon.NerdyDragon;
import org.bukkit.Bukkit;
import org.bukkit.entity.EnderDragon;
import org.bukkit.scheduler.BukkitTask;

public abstract class AbstractFightTask implements Runnable {

    final EnderDragonFight _fight;

    private final BukkitTask _task;

    AbstractFightTask(EnderDragonFight fight, boolean forceSync) {
        _fight = fight;
        if (forceSync) {
            _task = Bukkit.getScheduler().runTask(NerdyDragon.PLUGIN, this);
        } else {
            _task = Bukkit.getScheduler().runTaskAsynchronously(NerdyDragon.PLUGIN, this);
        }
    }

    AbstractFightTask(EnderDragonFight fight, boolean forceSync, int delay, int period) {
        _fight = fight;
        if (forceSync) {
            _task = Bukkit.getScheduler().runTaskTimer(NerdyDragon.PLUGIN, this, delay, period);
        } else {
            _task = Bukkit.getScheduler().runTaskTimerAsynchronously(NerdyDragon.PLUGIN, this, delay, period);
        }
    }

    EnderDragon getDragon() {
        return _fight.getDragon();
    }

}
