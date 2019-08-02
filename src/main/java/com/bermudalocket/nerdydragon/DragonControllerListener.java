package com.bermudalocket.nerdydragon;

import java.util.function.Consumer;

/**
 * API
 */
public interface DragonControllerListener {

    /**
     * Context target is a Player
     */
    Consumer<EnderDragonFight> onDragonFireball(DragonActionContext context);

    /**
     * Context target is an AreaEffectCloud
     */
    Consumer<EnderDragonFight> onFireballImpact(DragonActionContext context);

}
