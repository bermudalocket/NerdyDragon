package com.bermudalocket.nerdydragon.util;

// ------------------------------------------------------------------------

import com.google.common.base.Preconditions;

/**
 * A class representing a simple ordered pair.
 */
public class OrderedPair<T> {

    private final T _a, _b;

    public OrderedPair(T a, T b) {
        _a = a;
        _b = b;
    }

    public OrderedPair(T[] ts) {
        Preconditions.checkArgument(ts.length == 2);
        _a = ts[0];
        _b = ts[1];
    }

    public T getA() { return _a; }
    public T getB() { return _b; }

}
