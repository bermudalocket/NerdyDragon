package com.bermudalocket.nerdydragon;

import com.sun.istack.internal.NotNull;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.Entity;

import javax.annotation.Nullable;

public class DragonActionContext {

    private final EnderDragon enderDragon;

    private final Entity target;

    private final FightStage stage;

    public DragonActionContext(@NotNull EnderDragon dragon, @Nullable Entity target, FightStage stage) {
        this.enderDragon = dragon;
        this.target = target;
        this.stage = stage;
    }

    @NotNull
    public EnderDragon getDragon() {
        return this.enderDragon;
    }

    @Nullable
    public Entity getTarget() {
        return this.target;
    }

    @NotNull
    public FightStage getStage() {
        return this.stage;
    }

}
