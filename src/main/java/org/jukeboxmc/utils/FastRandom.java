package org.jukeboxmc.utils;


import java.util.Random;

/**
 * @author geNAZt
 * @version 1.0
 */
public final class FastRandom extends Random {

    private static final ThreadLocal<FastRandom> FAST_RANDOM_THREAD_LOCAL = new ThreadLocal<>();

    private long seed;

    public FastRandom() {
        this( System.nanoTime() );
    }

    public FastRandom( long seed ) {
        this.seed = seed;
    }

    @Override
    public void setSeed( long seed ) {
        this.seed = seed;
    }

    @Override
    protected int next( int nbits ) {
        long x = this.seed;
        x ^= ( x << 21 );
        x ^= ( x >>> 35 );
        x ^= ( x << 4 );
        this.seed = x;
        x &= ( ( 1L << nbits ) - 1 );

        return (int) x;
    }

    public static FastRandom current() {
        FastRandom fastRandom = FAST_RANDOM_THREAD_LOCAL.get();
        if ( fastRandom == null ) {
            fastRandom = new FastRandom();
            FAST_RANDOM_THREAD_LOCAL.set( fastRandom );
        }

        return fastRandom;
    }

}
