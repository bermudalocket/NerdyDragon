package com.bermudalocket.nerdydragon;

import org.bukkit.entity.Player;

import java.io.Serializable;
import java.util.UUID;

public class Participant implements Serializable {

    private final UUID uuid;

    private final String name;

    private double damage;

    public Participant(Player player) {
        this.uuid = player.getUniqueId();
        this.name = player.getDisplayName();
    }

    public UUID getUUID() {
        return this.uuid;
    }

    public String getName() {
        return this.name;
    }

    public double getDamage() {
        return this.damage;
    }

    public void addDamage(double damage) {
        this.damage += damage;
    }

}
