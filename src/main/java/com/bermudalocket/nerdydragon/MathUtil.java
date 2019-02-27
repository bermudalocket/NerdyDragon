package com.bermudalocket.nerdydragon;

import com.bermudalocket.nerdydragon.util.OrderedPair;

import java.util.Collection;
import java.util.Random;

public class MathUtil {

    // ------------------------------------------------------------------------
    /**
     * A persistent Random object.
     */
    private static final Random RANDOM = new Random();

    // ------------------------------------------------------------------------
    /**
     * Returns true if a uniformly-distributed random double is less than or
     * equal to the given probability; false otherwise.
     *
     * @param probability the probability in the range [0, 1].
     * @return true if a uniformly-distributed random double is less than or
     *         equal to the given probability; false otherwise.
     * @throws IllegalArgumentException if probability is not in [0, 1].
     */
    public static boolean cdf(double probability) {
        if (probability < 0 || probability > 1) {
            throw new IllegalArgumentException();
        }
        return RANDOM.nextDouble() <= probability;
    }

    // ------------------------------------------------------------------------
    /**
     * Returns a random integer from the interval of integers [min, max].
     *
     * @param min the interval minimum.
     * @param max the interval maximum.
     * @return a random integer from the interval of integers [min, max].
     */
    public static int random(int min, int max) {
        return min + RANDOM.nextInt(1 + max - min);
    }

    // ------------------------------------------------------------------------
    /**
     * Returns a random integer from the interval of integers [0, max].
     *
     * @param max the interval maximum.
     * @return a random integer from the interval of integers [min, max].
     */
    public static int random(int max) {
        return random(0, max);
    }

    // ------------------------------------------------------------------------
    /**
     * Returns a uniformly-distributed random double in [0, 1).
     *
     * @return a uniformly-distributed random double in [0, 1).
     */
    public static double nextDouble() {
        return RANDOM.nextDouble();
    }

    // ------------------------------------------------------------------------
    /**
     * Returns a normally distributed random double with mean and standard
     * deviation given.
     *
     * @param mean the mean.
     * @param stdev the standard deviation.
     * @return a normally distributed random double.
     */
    public static double gaussian(double mean, double stdev) {
        return mean + stdev*RANDOM.nextGaussian();
    }

    public static int floor(double value) {
        return (int) java.lang.Math.floor(value);
    }

    public static double abs(double value) {
        return value < 0 ? -1*value : value;
    }

    // ------------------------------------------------------------------------
    /**
     * Returns a random object from the given collection.
     *
     * @param collection the collection.
     * @return a random object from the given collection.
     */
    public static <T> T getRandomObject(Collection<T> collection) {
        if (collection == null || collection.size() == 0) {
            return null;
        }
        int N = collection.size();
        int n = RANDOM.nextInt(N);
        int i = 0;
        for (T t : collection) {
            if (i == n) {
                return t;
            }
            i++;
        }
        return null;
    }

    // ------------------------------------------------------------------------
    /**
     * Returns an ordered pair of random coordinates within a square radius.
     *
     * @param radius the radius.
     * @return a random coordinate.
     */
    public static OrderedPair<Integer> getRandomCoordinates(int radius) {
        int a = random(2 * radius) - radius;
        int b = random(2 * radius) - radius;
        return new OrderedPair<>(a, b);
    }

    public static double round(double value) {
        return java.lang.Math.round(value);
    }

}
