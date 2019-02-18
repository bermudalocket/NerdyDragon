package com.bermudalocket.nerdydragon;

import org.bukkit.entity.Player;

public class PlayerData {

    final String _playerName;

    private double _damageInflicted = 0;

    private int _deaths = 0;

    public PlayerData(Player player) {
        _playerName = player.getName();
    }

    public String getName() {
        return _playerName;
    }

    public void addDamage(double damage) {
        _damageInflicted += damage;
    }

    public double getDamageInflicted() {
        return _damageInflicted;
    }

    public void addDeath() {
        _deaths += 1;
    }

    public int getDeaths() {
        return _deaths;
    }

}
