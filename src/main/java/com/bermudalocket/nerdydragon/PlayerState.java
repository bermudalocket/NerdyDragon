package com.bermudalocket.nerdydragon;

import org.bukkit.entity.Player;

import java.util.UUID;

public class PlayerState {

    private final UUID uuid;

    private boolean didMuteDragon = false;

    public PlayerState(Player player) {
        this.uuid = player.getUniqueId();
    }

    public boolean matches(Player player) {
        return this.uuid.equals(player.getUniqueId());
    }

    public boolean didMuteDragon() {
        return this.didMuteDragon;
    }

    public void toggleDragonMute() {
        this.didMuteDragon = !this.didMuteDragon;
    }

}
