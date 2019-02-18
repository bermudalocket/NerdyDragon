package com.bermudalocket.nerdydragon;

import org.bukkit.Bukkit;

public class Thread {

    // ------------------------------------------------------------------------
    /**
     * Schedules a new thread delayed by a single tick.
     *
     * @param runnable the runnable.
     */
    public static void newThread(Runnable runnable) {
        Bukkit.getScheduler().runTaskLater(NerdyDragon.PLUGIN, runnable, 1);
    }

    // ------------------------------------------------------------------------
    /**
     * Schedules a new thread delayed by the given number of seconds.
     *
     * @param delay the delay, in sec.
     * @param runnable the runnable.
     */
    public static void newThread(int delay, Runnable runnable) {
        Bukkit.getScheduler().runTaskLater(NerdyDragon.PLUGIN, runnable, 20*delay);
    }

    // ------------------------------------------------------------------------
    /**
     * Schedules a new thread delayed by a random number of seconds.
     *
     * @param minDelay the minimum delay, in sec.
     * @param maxDelay the maximum delay, in sec.
     * @param runnable the runnable.
     */
    public static void newThread(int minDelay, int maxDelay, Runnable runnable) {
        newThread(MathUtil.random(minDelay, maxDelay), runnable);
    }

    // ------------------------------------------------------------------------
    /**
     * Schedules a random number of copies of the given runnable with a delay of
     * delayStep ticks between each copy.
     *
     * @param minRepeats minimum number of times to repeat the runnable.
     * @param maxRepeats maximum number of times to repeat the runnable.
     * @param delayStep number of ticks between each copy.
     * @param runnable the runnable to be scheduled.
     */
    public static void newRepeatedThread(int minRepeats, int maxRepeats, int delayStep, Runnable runnable) {
        if (minRepeats <= 0 || minRepeats > maxRepeats || delayStep <= 0) {
            throw new IllegalArgumentException();
        }
        newRepeatedThread(MathUtil.random(minRepeats, maxRepeats), delayStep, runnable);
    }

    // ------------------------------------------------------------------------
    /**
     * Schedules a given number of copies of the given runnable with a delay of
     * delayStep ticks between each copy.
     *
     * @param repeats number of times to repeat the runnable.
     * @param delayStep number of ticks between each copy.
     * @param runnable the runnable to be scheduled.
     */
    public static void newRepeatedThread(int repeats, int delayStep, Runnable runnable) {
        if (repeats <= 0 || delayStep <= 0) {
            throw new IllegalArgumentException();
        }
        for (int i = 1; i <= repeats; i++) {
            Bukkit.getScheduler().runTaskLater(NerdyDragon.PLUGIN, runnable, i*delayStep);
        }
    }

}
