package com.bermudalocket.nerdydragon;

import org.bukkit.entity.EnderDragon;
import org.bukkit.event.entity.EnderDragonChangePhaseEvent;

// WIP / non-functional
public class Stage implements Comparable<Stage> {

    private final int _id;

    private final String _displayName;

    public Stage(int id, String displayName) {
        _id = id;
        _displayName = displayName;
    }

    public int getId() {
        return _id;
    }

    public void skip() { }

    public void addBuffs(EnderDragon dragon) { }

    // NOT a listener
    public void handlePhaseChange(EnderDragonChangePhaseEvent event) { }

    @Override
    public int compareTo(Stage o) {
        return Integer.compare(_id, o.getId());
    }

}
