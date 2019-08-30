/*
 * Copyright (c) 2019 bermudalocket. All rights reserved.
 * Unauthorized copying or distribution of this item without permission of the author is prohibited.
 * Proprietary and Confidential
 * Created by bermudalocket on 8/29/2019 at 9:40:21.
 * Last modified 8/29/19, 9:33 PM.
 */

package com.bermudalocket.nerdydragon.leaderboard;

import com.bermudalocket.nerdydragon.NerdyDragon;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.configuration.serialization.SerializableAs;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@SerializableAs("FightRecord")
public class FightRecord implements ConfigurationSerializable {

    private UUID uuid;
    private long timeStarted;
    private long duration;
    private final Map<String, Double> participants = new HashMap<>();

    public FightRecord(UUID uuid, long timeStarted, long duration) {
        this.uuid = uuid;
        this.timeStarted = timeStarted;
        this.duration = duration;
    }

    @SuppressWarnings("unused")
    public FightRecord(Map<String, Object> data) {
        try {
            this.uuid = UUID.fromString((String) data.get("uuid"));
            this.timeStarted = (Long) data.get("timeStarted");
            this.duration = (Long) data.get("duration");
            Map<String, Double> participants = (HashMap<String, Double>) data.get("participants");
            participants.forEach(this.participants::put);
        } catch (Exception e) {
            NerdyDragon.log("Failed to deserialize fight record " + data.get("uuid"));
        }
    }

    public String getId() {
        return this.uuid.toString();
    }

    public long getTimeStarted() {
        return this.timeStarted;
    }

    public long getDuration() {
        return duration;
    }

    public Set<String> getParticipants() {
        return new HashSet<>(this.participants.keySet());
    }

    public double getParticipantDamagePercent(String participantName) {
        return this.participants.get(participantName);
    }

    public int getParticipantCount() {
        return this.participants.size();
    }

    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> data = new HashMap<>();
        data.put("uuid", this.uuid.toString());
        data.put("timeStarted", this.timeStarted);
        data.put("duration", this.duration);
        participants.forEach((name, percentDamage) -> data.put("participant." + name, percentDamage));
        return data;
    }

}
