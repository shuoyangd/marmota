package edu.jhu.marmota.util;

/**
 * This class is used to quickly create classes comparable by their integer index, which enables them to be sorted.
 * For more complicated application the better practice should be defining a new comparator yourself.
 *
 * @author shuoyang
 */
public class Indexed<T> implements Comparable<Indexed<T>> {

    final private int index;
    final private T e;

    public Indexed (int index, T e) {
        this.index = index;
        this.e = e;
    }

    public T getE () {
        return e;
    }

    @Override
    public int compareTo(Indexed<T> o) {
        return index - o.index;
    }
}

