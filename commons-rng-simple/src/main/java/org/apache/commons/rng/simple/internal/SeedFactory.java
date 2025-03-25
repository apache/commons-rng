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
    private SeedFactory() {
        // Do nothing
    }

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
    public static int[] createIntArray(int n) {
        // Behaviour from v1.3 is to ensure the first position is non-zero
        return createIntArray(n, 0, Math.min(n, 1));
    }

    /**
     * Creates an array of {@code int} numbers for use as a seed.
     * Optionally ensure a sub-range of the array is not all-zero.
     *
     * <p>This method is package-private for use by {@link NativeSeedType}.
     *
     * @param n Size of the array to create.
     * @param from The start of the not all-zero sub-range (inclusive).
     * @param to The end of the not all-zero sub-range (exclusive).
     * @return an array of {@code n} random numbers.
     * @throws IndexOutOfBoundsException if the sub-range is invalid
     * @since 1.5
     */
    static int[] createIntArray(int n, int from, int to) {
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
        ensureNonZero(seed, from, to);
        return seed;
    }

    /**
     * Creates an array of {@code long} numbers for use as a seed.
     *
     * @param n Size of the array to create.
     * @return an array of {@code n} random numbers.
     */
    public static long[] createLongArray(int n) {
        // Behaviour from v1.3 is to ensure the first position is non-zero
        return createLongArray(n, 0, Math.min(n, 1));
    }

    /**
     * Creates an array of {@code long} numbers for use as a seed.
     * Optionally ensure a sub-range of the array is not all-zero.
     *
     * <p>This method is package-private for use by {@link NativeSeedType}.
     *
     * @param n Size of the array to create.
     * @param from The start of the not all-zero sub-range (inclusive).
     * @param to The end of the not all-zero sub-range (exclusive).
     * @return an array of {@code n} random numbers.
     * @throws IndexOutOfBoundsException if the sub-range is invalid
     * @since 1.5
     */
    static long[] createLongArray(int n, int from, int to) {
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
        ensureNonZero(seed, from, to);
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
    private static void fillIntArray(int[] array, int start, int end) {
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
    private static void fillLongArray(long[] array, int start, int end) {
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
     * randomness. A sub-range can be specified that must not contain all zeros.
     *
     * @param source Source of randomness.
     * @param n Size of the array to create.
     * @param from The start of the not all-zero sub-range (inclusive).
     * @param to The end of the not all-zero sub-range (exclusive).
     * @return an array of {@code n} random numbers.
     */
    static byte[] createByteArray(UniformRandomProvider source,
                                  int n,
                                  int from,
                                  int to) {
        final byte[] seed = new byte[n];
        source.nextBytes(seed);
        ensureNonZero(seed, from, to, source);
        return seed;
    }

    /**
     * Ensure the seed is not all-zero within the specified sub-range.
     *
     * <p>This method will check the sub-range and if all are zero it will fill the range
     * with a random sequence seeded from the default source of random int values. The
     * fill ensures position {@code from} has a non-zero value; and the entire sub-range
     * has a maximum of one zero. The method ensures any length sub-range contains
     * non-zero bits. The output seed is suitable for generators that cannot be seeded
     * with all zeros in the specified sub-range.</p>
     *
     * @param seed Seed array (modified in place).
     * @param from The start of the not all-zero sub-range (inclusive).
     * @param to The end of the not all-zero sub-range (exclusive).
     * @throws IndexOutOfBoundsException if the sub-range is invalid
     * @see #createInt()
     */
    static void ensureNonZero(int[] seed, int from, int to) {
        if (from >= to) {
            return;
        }
        // No check on the range so an IndexOutOfBoundsException will occur if invalid
        for (int i = from; i < to; i++) {
            if (seed[i] != 0) {
                return;
            }
        }
        // Fill with non-zero values using a SplitMix-style PRNG.
        // The range is at least 1.
        // To ensure the first value is not zero requires the input to the mix function
        // to be non-zero. This is ensured if the start is even since the increment is odd.
        int x = createInt() << 1;
        for (int i = from; i < to; i++) {
            x += MixFunctions.GOLDEN_RATIO_32;
            seed[i] = MixFunctions.murmur3(x);
        }
    }

    /**
     * Ensure the seed is not all-zero within the specified sub-range.
     *
     * <p>This method will check the sub-range and if all are zero it will fill the range
     * with a random sequence seeded from the default source of random long values. The
     * fill ensures position {@code from} has a non-zero value; and the entire sub-range
     * has a maximum of one zero. The method ensures any length sub-range contains
     * non-zero bits. The output seed is suitable for generators that cannot be seeded
     * with all zeros in the specified sub-range.</p>
     *
     * @param seed Seed array (modified in place).
     * @param from The start of the not all-zero sub-range (inclusive).
     * @param to The end of the not all-zero sub-range (exclusive).
     * @throws IndexOutOfBoundsException if the sub-range is invalid
     * @see #createLong()
     */
    static void ensureNonZero(long[] seed, int from, int to) {
        if (from >= to) {
            return;
        }
        // No check on the range so an IndexOutOfBoundsException will occur if invalid
        for (int i = from; i < to; i++) {
            if (seed[i] != 0) {
                return;
            }
        }
        // Fill with non-zero values using a SplitMix-style PRNG.
        // The range is at least 1.
        // To ensure the first value is not zero requires the input to the mix function
        // to be non-zero. This is ensured if the start is even since the increment is odd.
        long x = createLong() << 1;
        for (int i = from; i < to; i++) {
            x += MixFunctions.GOLDEN_RATIO_64;
            seed[i] = MixFunctions.stafford13(x);
        }
    }

    /**
     * Ensure the seed is not all-zero within the specified sub-range.
     *
     * <p>This method will check the sub-range and if all are zero it will fill the range
     * with a random sequence seeded from the provided source of randomness. If the range
     * length is above 8 then the first 8 bytes in the range are ensured to not all be
     * zero. If the range length is below 8 then the first byte in the range is ensured to
     * be non-zero. The method ensures any length sub-range contains non-zero bits. The
     * output seed is suitable for generators that cannot be seeded with all zeros in the
     * specified sub-range.</p>
     *
     * @param seed Seed array (modified in place).
     * @param from The start of the not all-zero sub-range (inclusive).
     * @param to The end of the not all-zero sub-range (exclusive).
     * @param source Source of randomness.
     * @throws IndexOutOfBoundsException if the sub-range is invalid
     */
    static void ensureNonZero(byte[] seed, int from, int to, UniformRandomProvider source) {
        if (from >= to) {
            return;
        }
        // No check on the range so an IndexOutOfBoundsException will occur if invalid
        for (int i = from; i < to; i++) {
            if (seed[i] != 0) {
                return;
            }
        }
        // Defend against a faulty source of randomness (which supplied all zero bytes)
        // by filling with non-zero values using a SplitMix-style PRNG seeded from the source.
        // The range is at least 1.
        // To ensure the first value is not zero requires the input to the mix function
        // to be non-zero. This is ensured if the start is even since the increment is odd.
        long x = source.nextLong() << 1;

        // Process in blocks of 8.
        // Get the length without the final 3 bits set for a multiple of 8.
        final int len = (to - from) & ~0x7;
        final int end = from + len;
        int i = from;
        while (i < end) {
            long v = MixFunctions.stafford13(x += MixFunctions.GOLDEN_RATIO_64);
            for (int j = 0; j < 8; j++) {
                seed[i++] = (byte) v;
                v >>>= 8;
            }
        }

        if (i < to) {
            // The final bytes.
            long v = MixFunctions.stafford13(x + MixFunctions.GOLDEN_RATIO_64);
            // Note the special case where no blocks have been processed requires these
            // bytes to be non-zero, i.e. (to - from) < 8. In this case the value 'v' will
            // be non-zero due to the initialisation of 'x' as even.
            // Rotate the value so the least significant byte is non-zero. The rotation
            // in bits is rounded down to the nearest 8-bit block to ensure a byte rotation.
            if (len == 0) {
                v = Long.rotateRight(v, Long.numberOfTrailingZeros(v) & ~0x7);
            }
            while (i < to) {
                seed[i++] = (byte) v;
                v >>>= 8;
            }
        }
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
    static long ensureNonZero(RandomLongSource source,
                              long value) {
        long result = value;
        while (result == 0) {
            result = source.next();
        }
        return result;
    }
}
