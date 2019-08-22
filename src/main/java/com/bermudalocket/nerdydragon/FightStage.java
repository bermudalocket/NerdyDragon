/*
 * Copyright (c) 2019 bermudalocket. All rights reserved.
 * Unauthorized copying or distribution of this item without permission of the author is prohibited.
 * Proprietary and Confidential
 * Written by bermudalocket, 2019.
 */
package com.bermudalocket.nerdydragon;

import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.boss.BarColor;

// ------------------------------------------------------------------------
/**
 * Isn't it stunning?
 */
public enum FightStage {

         // 1      2    3   4  5  6   7    8    9     10    11    12            13               14  15
   FINISHED(0,     0,   0,  0, 0, 0,  0,   0,   0,     0,    0,    0, Color.PURPLE,  BarColor.GREEN, ""),
      FIRST(0,     4,   1,  0, 9, 0,  0,   1,   1,  0.01,    0,  1.0, Color.PURPLE,  BarColor.GREEN, ChatColor.GREEN + "Stage I"),
     SECOND(0.60,  4,   1, 10, 1, 1,  2,   5,  15,  0.20, 0.12, 0.25, Color.PURPLE, BarColor.YELLOW, ChatColor.GOLD + "Stage II"),
      THIRD(0.30,  7,   3,  7, 2, 2,  4,  14,  40,  0.40, 0.30, 0.55,   Color.LIME,    BarColor.RED, ChatColor.RED + "Stage III"),
     FOURTH(0,     10,  6,  3, 3, 3,  6,  25,  75,  0.75, 0.45, 0.85,  Color.BLACK, BarColor.PURPLE, ChatColor.DARK_RED + "S" + ChatColor.MAGIC + "" + ChatColor.DARK_RED + "t" + ChatColor.RESET + "" + ChatColor.DARK_RED + "age " + ChatColor.MAGIC + "IV");

    public double DRAGON_HP_LOW_BOUND; // 1
    public int MAX_EXTRA_POTION_DUR; // 2
    public int MAX_EXTRA_FIREBALLS; // 3
    public int FIREBALL_TICK_INCREMENT; // 4
    public int MAX_EFFECTS; // 5
    public int MAX_ENDERMITES; // 6
    public int MAX_REINF_PER_CLUSTER; // 7
    public int MIN_PHANTOM_SIZE; // 8
    public int MAX_PHANTOM_SIZE; // 9
    public double REINFORCEMENT_CHANCE; // 10
    double POTION_EFFECT_CHANCE; // 11
    public double LEAVE_PORTAL_CHANCE; // 12
    public Color FLAME_COLOR; // 13
    public BarColor BOSS_BAR_COLOR; // 14
    public String DISPLAY_NAME; // 15

    FightStage(double dragonHpLowBound, int maxExtraPotionDuration, int maxFireballs,
               int fireballTickIncrement, int maxEffects, int maxEndermites, int maxPhantoms,
               int minPhantomSize, int maxPhantomSize, double phantomChance, double dragonRecoilEffectChance,
               double leavePortalChance, Color flameColor, BarColor bossBarColor, String displayName) {
        MAX_EXTRA_POTION_DUR = maxExtraPotionDuration;
        LEAVE_PORTAL_CHANCE = leavePortalChance;

        DRAGON_HP_LOW_BOUND = dragonHpLowBound;

        MAX_EXTRA_FIREBALLS = maxFireballs;
        FIREBALL_TICK_INCREMENT = fireballTickIncrement;

        MAX_ENDERMITES = maxEndermites;

        MAX_REINF_PER_CLUSTER = maxPhantoms;
        MIN_PHANTOM_SIZE = minPhantomSize;
        MAX_PHANTOM_SIZE = maxPhantomSize;
        REINFORCEMENT_CHANCE = phantomChance;

        POTION_EFFECT_CHANCE = dragonRecoilEffectChance;
        MAX_EFFECTS = maxEffects;

        DISPLAY_NAME = displayName;
        FLAME_COLOR = flameColor;
        BOSS_BAR_COLOR = bossBarColor;
    }

    public static FightStage getNext(FightStage stage) {
        switch (stage) {
            case FIRST:
                return SECOND;

            case SECOND:
                return THIRD;

            case THIRD:
                return FOURTH;

            default:
            case FOURTH:
                return FINISHED;
        }
    }

}