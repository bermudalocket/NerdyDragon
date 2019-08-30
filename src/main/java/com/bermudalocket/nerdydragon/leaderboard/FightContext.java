/*
 * Copyright (c) 2019 bermudalocket. All rights reserved.
 * Unauthorized copying or distribution of this item without permission of the author is prohibited.
 * Proprietary and Confidential
 * Created by bermudalocket on 8/29/2019 at 9:40:21.
 * Last modified 8/28/19, 7:25 PM.
 */

package com.bermudalocket.nerdydragon.leaderboard;

import java.util.function.Predicate;

public enum FightContext {

    NONE(i -> true),
    TEAM(i -> i > 1),
    SOLO(i -> i == 1);

    private Predicate<Integer> predicate;

    FightContext(Predicate<Integer> predicate) {
        this.predicate = predicate;
    }

    public boolean matches(FightRecord record) {
        return this.predicate.test(record.getParticipantCount());
    }

}
