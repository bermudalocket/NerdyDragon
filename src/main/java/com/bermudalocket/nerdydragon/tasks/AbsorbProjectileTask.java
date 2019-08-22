/*
 * Copyright (c) 2019 bermudalocket. All rights reserved.
 * Unauthorized copying or distribution of this item without permission of the author is prohibited.
 * Proprietary and Confidential
 * Written by bermudalocket, 2019.
 */
package com.bermudalocket.nerdydragon.tasks;

import com.bermudalocket.nerdydragon.DragonHelper;
import com.bermudalocket.nerdydragon.EnderDragonFight;
import com.bermudalocket.nerdydragon.Util;
import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.entity.AreaEffectCloud;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public class AbsorbProjectileTask extends AbstractFightTask {

    private final EntityDamageByEntityEvent _event;

    private boolean _absorbed = false;

    public AbsorbProjectileTask(EnderDragonFight fight, EntityDamageByEntityEvent event) {
        super(fight, true);
        _event = event;
    }

    public boolean isAbsorbed() {
        return _absorbed;
    }

    @Override
    public void run() {
        Projectile projectile = (Projectile) _event.getDamager();
        if (!(projectile.getShooter() instanceof Player)) {
            return;
        }
        EnderDragon dragon = getDragon();
        if (dragon.getHealth() - _event.getFinalDamage() <= 0) {
            return;
        }
        _absorbed = true;
        DragonHelper.healDragon(dragon, _event.getFinalDamage());
        AreaEffectCloud cloud = (AreaEffectCloud) Util.WORLD_THE_END.spawnEntity(dragon.getLocation(), EntityType.AREA_EFFECT_CLOUD);
        cloud.setParticle(Particle.REDSTONE, new Particle.DustOptions(Color.LIME, 1));
        cloud.setDuration(10);
        cloud.setRadius(5.0f);
        _event.setDamage(0.0);
    }

}
