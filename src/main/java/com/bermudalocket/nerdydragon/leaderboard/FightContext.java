/*
 * Copyright (c) 2019 bermudalocket. All rights reserved.
 * Unauthorized copying or distribution of this item without permission of the author is prohibited.
 * Proprietary and Confidential
 * Created by bermudalocket on 8/29/2019 at 10:11:0.
 * Last modified 8/29/19, 9:44 PM.
 */

package com.bermudalocket.nerdydragon.leaderboard;

import java.util.Optional;
import java.util.function.Predicate;

public enum FightContext {

    NONE(i -> true, ""),
    TEAM(i -> i > 1, "group"),
    SOLO(i -> i == 1, "solo");

    private Predicate<Integer> predicate;
    String descriptor;

    FightContext(Predicate<Integer> predicate, String descriptor) {
        this.predicate = predicate;
        this.descriptor = descriptor;
    }

    public boolean matches(FightRecord record) {
        return this.predicate.test(record.getParticipantCount());
    }

    public static Optional<FightContext> fromDescriptor(String descriptor) {
        for (FightContext context : values()) {
            if (context.descriptor.equalsIgnoreCase(descriptor)) {
                return Optional.of(context);
            }
        }
        return Optional.empty();
    }

}
