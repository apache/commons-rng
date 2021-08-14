/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.commons.rng.simple.internal;

import java.security.SecureRandom;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.rng.core.util.NumberFactory;
import org.apache.commons.rng.UniformRandomProvider;
import org.apache.commons.rng.core.source64.RandomLongSource;
import org.apache.commons.rng.core.source64.SplitMix64;
import org.apache.commons.rng.core.source64.XoRoShiRo1024PlusPlus;

/**
 * Utilities related to seeding.
 *
 * <p>
 * This class provides methods to generate random seeds (single values
 * or arrays of values, of {@code int} or {@code long} types) that can
 * be passed to the {@link org.apache.commons.rng.simple.RandomSource
 * methods that create a generator instance}.
 * <br>
 * Although the seed-generating methods defined in this class will likely
 * return different values for all calls, there is no guarantee that the
 * produced seed will result always in a "good" sequence of numbers (even
 * if the generator initialized with that seed is good).
 * <br>
 * There is <i>no guarantee</i> that sequences will not overlap.
 * </p>
 *
 * @since 1.0
 */
public final class SeedFactory {
    /** Size of the state array of "XoRoShiRo1024PlusPlus". */
    private static final int XO_RO_SHI_RO_1024_STATE_SIZE = 16;
    /** Size of block to fill in an {@code int[]} seed per synchronized operation. */
    private static final int INT_ARRAY_BLOCK_SIZE = 8;
    /** Size of block to fill in a {@code long[]} seed per synchronized operation. */
    private static final int LONG_ARRAY_BLOCK_SIZE = 4;

    /**
     * The lock to own when using the seed generator. This lock is unfair and there is no
     * particular access order for waiting threads.
     *
     * <p>This is used as an alternative to {@code synchronized} statements to guard access
     * to the seed generator.</p>
     */
    private static final ReentrantLock LOCK = new ReentrantLock(false);

    /** Generator with a long period. */
    private static final UniformRandomProvider SEED_GENERATOR;

    static {
        // Use a secure RNG so that different instances (e.g. in multiple JVM
        // instances started in rapid succession) will have different seeds.
        final SecureRandom seedGen = new SecureRandom();
        final byte[] bytes = new byte[8 * XO_RO_SHI_RO_1024_STATE_SIZE];
        seedGen.nextBytes(bytes);
        final long[] seed = NumberFactory.makeLongArray(bytes);
        // The XoRoShiRo1024PlusPlus generator cannot recover from an all zero seed and
        // will produce low quality initial output if initialized with some zeros.
        // Ensure it is non zero at all array positions using a SplitMix64
        // generator (this is insensitive to a zero seed so can use the first seed value).
        final SplitMix64 rng = new SplitMix64(seed[0]);
        for (int i = 0; i < seed.length; i++) {
            seed[i] = ensureNonZero(rng, seed[i]);
        }

        SEED_GENERATOR = new XoRoShiRo1024PlusPlus(seed);
    }

    /**
     * Class contains only static methods.
     */
    private SeedFactory() {}

    /**
     * Creates an {@code int} number for use as a seed.
     *
     * @return a random number.
     */
    public static int createInt() {
        LOCK.lock();
        try {
            return SEED_GENERATOR.nextInt();
        } finally {
            LOCK.unlock();
        }
    }

    /**
     * Creates a {@code long} number for use as a seed.
     *
     * @return a random number.
     */
    public static long createLong() {
        LOCK.lock();
        try {
            return SEED_GENERATOR.nextLong();
        } finally {
            LOCK.unlock();
        }
    }

    /**
     * Creates an array of {@code int} numbers for use as a seed.
     *
     * @param n Size of the array to create.
     * @return an array of {@code n} random numbers.
     */
    public static int[] createIntArray(final int n) {
        final int[] seed = new int[n];
        // Compute the size that can be filled with complete blocks
        final int blockSize = INT_ARRAY_BLOCK_SIZE * (n / INT_ARRAY_BLOCK_SIZE);
        int i = 0;
        while (i < blockSize) {
            final int end = i + INT_ARRAY_BLOCK_SIZE;
            fillIntArray(seed, i, end);
            i = end;
        }
        // Final fill only if required
        if (i != n) {
            fillIntArray(seed, i, n);
        }
        ensureNonZero(seed);
        return seed;
    }

    /**
     * Creates an array of {@code long} numbers for use as a seed.
     *
     * @param n Size of the array to create.
     * @return an array of {@code n} random numbers.
     */
    public static long[] createLongArray(final int n) {
        final long[] seed = new long[n];
        // Compute the size that can be filled with complete blocks
        final int blockSize = LONG_ARRAY_BLOCK_SIZE * (n / LONG_ARRAY_BLOCK_SIZE);
        int i = 0;
        while (i < blockSize) {
            final int end = i + LONG_ARRAY_BLOCK_SIZE;
            fillLongArray(seed, i, end);
            i = end;
        }
        // Final fill only if required
        if (i != n) {
            fillLongArray(seed, i, n);
        }
        ensureNonZero(seed);
        return seed;
    }

    /**
     * Fill the array between {@code start} inclusive and {@code end} exclusive from the
     * seed generator. The lock is used to guard access to the generator.
     *
     * @param array Array data.
     * @param start Start (inclusive).
     * @param end End (exclusive).
     */
    private static void fillIntArray(final int[] array, final int start, final int end) {
        LOCK.lock();
        try {
            for (int i = start; i < end; i++) {
                array[i] = SEED_GENERATOR.nextInt();
            }
        } finally {
            LOCK.unlock();
        }
    }

    /**
     * Fill the array between {@code start} inclusive and {@code end} exclusive from the
     * seed generator. The lock is used to guard access to the generator.
     *
     * @param array Array data.
     * @param start Start (inclusive).
     * @param end End (exclusive).
     */
    private static void fillLongArray(final long[] array, final int start, final int end) {
        LOCK.lock();
        try {
            for (int i = start; i < end; i++) {
                array[i] = SEED_GENERATOR.nextLong();
            }
        } finally {
            LOCK.unlock();
        }
    }

    /**
     * Creates an array of {@code byte} numbers for use as a seed using the supplied source of
     * randomness. The result will not be all zeros.
     *
     * @param source Source of randomness.
     * @param n Size of the array to create.
     * @return an array of {@code n} random numbers.
     */
    static byte[] createByteArray(final UniformRandomProvider source,
                                  final int n) {
        final byte[] seed = new byte[n];
        source.nextBytes(seed);
        // If the seed is zero it is assumed the input source RNG is either broken
        // or the seed is small and it was zero by chance. Revert to the built-in
        // source of randomness to ensure it is non-zero.
        ensureNonZero(seed);
        return seed;
    }

    /**
     * Ensure the seed is non-zero at the first position in the array.
     *
     * <p>This method will replace a zero at index 0 in the array with
     * a non-zero random number. The method ensures any length seed
     * contains non-zero bits. The output seed is suitable for generators
     * that cannot be seeded with all zeros.</p>
     *
     * @param seed Seed array (modified in place).
     * @see #createInt()
     */
    static void ensureNonZero(final int[] seed) {
        // Zero occurs 1 in 2^32
        if (seed.length != 0 && seed[0] == 0) {
            do {
                seed[0] = createInt();
            } while (seed[0] == 0);
        }
    }

    /**
     * Ensure the seed is non-zero at the first position in the array.
     *
     * <p>This method will replace a zero at index 0 in the array with
     * a non-zero random number. The method ensures any length seed
     * contains non-zero bits. The output seed is suitable for generators
     * that cannot be seeded with all zeros.</p>
     *
     * @param seed Seed array (modified in place).
     * @see #createLong()
     */
    static void ensureNonZero(final long[] seed) {
        // Zero occurs 1 in 2^64
        if (seed.length != 0 && seed[0] == 0) {
            do {
                seed[0] = createLong();
            } while (seed[0] == 0);
        }
    }

    /**
     * Ensure the seed is not zero at all positions in the array.
     *
     * <p>This method will check all positions in the array and if all
     * are zero it will replace index 0 in the array with
     * a non-zero random number. The method ensures any length seed
     * contains non-zero bits. The output seed is suitable for generators
     * that cannot be seeded with all zeros.</p>
     *
     * @param seed Seed array (modified in place).
     * @see #createInt()
     */
    private static void ensureNonZero(final byte[] seed) {
        // Since zero occurs 1 in 2^8 for a single byte this checks the entire array for zeros.
        if (seed.length != 0 && isAllZero(seed)) {
            do {
                seed[0] = (byte) createInt();
            } while (seed[0] == 0);
        }
    }

    /**
     * Test if each position in the array is zero.
     *
     * @param array Array data.
     * @return true if all position are zero
     */
    private static boolean isAllZero(final byte[] array) {
        for (final byte value : array) {
            if (value != 0) {
                return false;
            }
        }
        return true;
    }

    /**
     * Ensure the value is non-zero.
     *
     * <p>This method will replace a zero with a non-zero random number from the random source.</p>
     *
     * @param source Source of randomness.
     * @param value Value.
     * @return {@code value} if non-zero; else a new random number
     */
    static long ensureNonZero(final RandomLongSource source,
                              final long value) {
        long result = value;
        while (result == 0) {
            result = source.next();
        }
        return result;
    }
}
