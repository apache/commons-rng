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

/**
 * Performs seed conversions.
 *
 * <p>Note: Legacy converters from version 1.0 use instances of
 * the {@link SeedConverter} interface. Instances are no longer
 * required as no state is used during conversion and converters
 * can use static methods.
 *
 * @since 1.5
 */
final class Conversions {
    /**
     * The fractional part of the golden ratio, phi, scaled to 64-bits and rounded to odd.
     * <pre>
     * phi = (sqrt(5) - 1) / 2) * 2^64
     * </pre>
     * @see <a href="https://en.wikipedia.org/wiki/Golden_ratio">Golden ratio</a>
     */
    private static final long GOLDEN_RATIO = MixFunctions.GOLDEN_RATIO_64;

    /** No instances. */
    private Conversions() {}

    /**
     * Compute the size of an {@code int} array required to hold the specified number of bytes.
     * Allows space for any remaining bytes that do not fit exactly in a 4 byte integer.
     * <pre>
     * n = ceil(size / 4)
     * </pre>
     *
     * @param size the size in bytes (assumed to be positive)
     * @return the size in ints
     */
    static int intSizeFromByteSize(int size) {
        return (size + 3) >>> 2;
    }

    /**
     * Compute the size of an {@code long} array required to hold the specified number of bytes.
     * Allows space for any remaining bytes that do not fit exactly in an 8 byte long.
     * <pre>
     * n = ceil(size / 8)
     * </pre>
     *
     * @param size the size in bytes (assumed to be positive)
     * @return the size in longs
     */
    static int longSizeFromByteSize(int size) {
        return (size + 7) >>> 3;
    }

    /**
     * Compute the size of an {@code int} array required to hold the specified number of longs.
     * Prevents overflow to a negative number when doubling the size.
     * <pre>
     * n = min(size * 2, 2^31 - 1)
     * </pre>
     *
     * @param size the size in longs (assumed to be positive)
     * @return the size in ints
     */
    static int intSizeFromLongSize(int size) {
        // Avoid overflow when doubling the length.
        // If n is negative the signed shift creates a mask with all bits set;
        // otherwise it is zero and n is unchanged after the or operation.
        // The final mask clears the sign bit in the event n did overflow.
        final int n = size << 1;
        return (n | (n >> 31)) & Integer.MAX_VALUE;
    }

    /**
     * Compute the size of an {@code long} array required to hold the specified number of ints.
     * Allows space for an odd int.
     * <pre>
     * n = ceil(size / 2)
     * </pre>
     *
     * @param size the size in ints (assumed to be positive)
     * @return the size in longs
     */
    static int longSizeFromIntSize(int size) {
        return (size + 1) >>> 1;
    }

    /**
     * Creates a {@code long} value from an {@code int}. The conversion
     * is made as if seeding a SplitMix64 RNG and calling nextLong().
     *
     * @param input Input
     * @return a {@code long}.
     */
    static long int2Long(int input) {
        return MixFunctions.stafford13(input + GOLDEN_RATIO);
    }

    /**
     * Creates an {@code int[]} value from an {@code int}. The conversion
     * is made as if seeding a SplitMix64 RNG and calling nextLong() to
     * generate the sequence and filling the ints
     * in little-endian order (least significant byte first).
     *
     * @param input Input
     * @param length Array length
     * @return an {@code int[]}.
     */
    static int[] int2IntArray(int input, int length) {
        return long2IntArray(input, length);
    }

    /**
     * Creates a {@code long[]} value from an {@code int}. The conversion
     * is made as if seeding a SplitMix64 RNG and calling nextLong() to
     * generate the sequence.
     *
     * @param input Input
     * @param length Array length
     * @return a {@code long[]}.
     */
    static long[] int2LongArray(int input, int length) {
        return long2LongArray(input, length);
    }

    /**
     * Creates an {@code int} value from a {@code long}. The conversion
     * is made by xoring the upper and lower bits.
     *
     * @param input Input
     * @return an {@code int}.
     */
    static int long2Int(long input) {
        return (int) input ^ (int) (input >>> 32);
    }

    /**
     * Creates an {@code int[]} value from a {@code long}. The conversion
     * is made as if seeding a SplitMix64 RNG and calling nextLong() to
     * generate the sequence and filling the ints
     * in little-endian order (least significant byte first).
     *
     * <p>A special case is made to avoid an array filled with zeros for
     * the initial 2 positions. It is still possible to create a zero in
     * position 0. However any RNG with an int[] native type is expected to
     * require at least 2 int values.
     *
     * @param input Input
     * @param length Array length
     * @return an {@code int[]}.
     */
    static int[] long2IntArray(long input, int length) {
        // Special case to avoid creating a zero-filled array of length 2.
        long v = input == -GOLDEN_RATIO ? ~input : input;
        final int[] output = new int[length];
        // Process pairs
        final int n = length & ~0x1;
        for (int i = 0; i < n; i += 2) {
            final long x = MixFunctions.stafford13(v += GOLDEN_RATIO);
            output[i] = (int) x;
            output[i + 1] = (int) (x >>> 32);
        }
        // Final value
        if (n < length) {
            output[n] = (int) MixFunctions.stafford13(v + GOLDEN_RATIO);
        }
        return output;
    }

    /**
     * Creates a {@code long[]} value from a {@code long}. The conversion
     * is made as if seeding a SplitMix64 RNG and calling nextLong() to
     * generate the sequence.
     *
     * @param input Input
     * @param length Array length
     * @return a {@code long}.
     */
    static long[] long2LongArray(long input, int length) {
        long v = input;
        final long[] output = new long[length];
        for (int i = 0; i < length; i++) {
            output[i] = MixFunctions.stafford13(v += GOLDEN_RATIO);
        }
        return output;
    }

    /**
     * Creates an {@code int} value from a sequence of ints. The conversion
     * is made by combining all the longs with a xor operation.
     *
     * @param input Input bytes
     * @return an {@code int}.
     */
    static int intArray2Int(int[] input) {
        int output = 0;
        for (final int i : input) {
            output ^= i;
        }
        return output;
    }

    /**
     * Creates a {@code long} value from a sequence of ints. The conversion
     * is made as if converting to a {@code long[]} array by filling the longs
     * in little-endian order (least significant byte first), then combining
     * all the longs with a xor operation.
     *
     * @param input Input bytes
     * @return a {@code long}.
     */
    static long intArray2Long(int[] input) {
        long output = 0;

        final int n = input.length;
        // xor in the bits to a long in little-endian order
        for (int i = 0; i < n; i++) {
            // i              = int index
            // i >> 1         = long index
            // i & 0x1        = int number in the long  [0, 1]
            // (i & 0x1) << 5 = little-endian byte shift to the long {0, 32}
            output ^= (input[i] & 0xffffffffL) << ((i & 0x1) << 5);
        }

        return output;
    }

    /**
     * Creates a {@code long[]} value from a sequence of ints. The longs are
     * filled in little-endian order (least significant byte first).
     *
     * @param input Input ints
     * @param length Output array length
     * @return a {@code long[]}.
     */
    static long[] intArray2LongArray(int[] input, int length) {
        final long[] output = new long[length];

        // Overflow-safe minimum using long
        final int n = (int) Math.min(input.length, length * 2L);
        // Little-endian fill
        for (int i = 0; i < n; i++) {
            // i              = int index
            // i >> 1         = long index
            // i & 0x1        = int number in the long  [0, 1]
            // (i & 0x1) << 5 = little-endian byte shift to the long {0, 32}
            output[i >> 1] |= (input[i] & 0xffffffffL) << ((i & 0x1) << 5);
        }

        return output;
    }

    /**
     * Creates an {@code int} value from a sequence of longs. The conversion
     * is made as if combining all the longs with a xor operation, then folding
     * the long high and low parts using a xor operation.
     *
     * @param input Input longs
     * @return an {@code int}.
     */
    static int longArray2Int(long[] input) {
        return Conversions.long2Int(longArray2Long(input));
    }

    /**
     * Creates a {@code long} value from a sequence of longs. The conversion
     * is made by combining all the longs with a xor operation.
     *
     * @param input Input longs
     * @return a {@code long}.
     */
    static long longArray2Long(long[] input) {
        long output = 0;
        for (final long i : input) {
            output ^= i;
        }
        return output;
    }

    /**
     * Creates a {@code int[]} value from a sequence of longs. The ints are
     * filled in little-endian order (least significant byte first).
     *
     * @param input Input longs
     * @param length Output array length
     * @return an {@code int[]}.
     */
    static int[] longArray2IntArray(long[] input, int length) {
        final int[] output = new int[length];

        // Overflow-safe minimum using long
        final int n = (int) Math.min(input.length * 2L, length);
        // Little-endian fill
        // Alternate low/high 32-bits from each long
        for (int i = 0; i < n; i++) {
            // i              = int index
            // i >> 1         = long index
            // i & 0x1        = int number in the long  [0, 1]
            // (i & 0x1) << 5 = little-endian long shift to the int {0, 32}
            output[i] = (int)((input[i >> 1]) >>> ((i & 0x1) << 5));
        }

        return output;
    }

    /**
     * Creates an {@code int} value from a sequence of bytes. The conversion
     * is made as if converting to a {@code int[]} array by filling the ints
     * in little-endian order (least significant byte first), then combining
     * all the ints with a xor operation.
     *
     * @param input Input bytes
     * @return an {@code int}.
     */
    static int byteArray2Int(byte[] input) {
        int output = 0;

        final int n = input.length;
        // xor in the bits to an int in little-endian order
        for (int i = 0; i < n; i++) {
            // i              = byte index
            // i >> 2         = integer index
            // i & 0x3        = byte number in the integer  [0, 3]
            // (i & 0x3) << 3 = little-endian byte shift to the integer {0, 8, 16, 24}
            output ^= (input[i] & 0xff) << ((i & 0x3) << 3);
        }

        return output;
    }

    /**
     * Creates an {@code int[]} value from a sequence of bytes. The ints are
     * filled in little-endian order (least significant byte first).
     *
     * @param input Input bytes
     * @param length Output array length
     * @return a {@code int[]}.
     */
    static int[] byteArray2IntArray(byte[] input, int length) {
        final int[] output = new int[length];

        // Overflow-safe minimum using long
        final int n = (int) Math.min(input.length, length * (long) Integer.BYTES);
        // Little-endian fill
        for (int i = 0; i < n; i++) {
            // i              = byte index
            // i >> 2         = integer index
            // i & 0x3        = byte number in the integer  [0, 3]
            // (i & 0x3) << 3 = little-endian byte shift to the integer {0, 8, 16, 24}
            output[i >> 2] |= (input[i] & 0xff) << ((i & 0x3) << 3);
        }

        return output;
    }

    /**
     * Creates a {@code long} value from a sequence of bytes. The conversion
     * is made as if converting to a {@code long[]} array by filling the longs
     * in little-endian order (least significant byte first), then combining
     * all the longs with a xor operation.
     *
     * @param input Input bytes
     * @return a {@code long}.
     */
    static long byteArray2Long(byte[] input) {
        long output = 0;

        final int n = input.length;
        // xor in the bits to a long in little-endian order
        for (int i = 0; i < n; i++) {
            // i              = byte index
            // i >> 3         = long index
            // i & 0x7        = byte number in the long  [0, 7]
            // (i & 0x7) << 3 = little-endian byte shift to the long {0, 8, 16, 24, 32, 36, 40, 48, 56}
            output ^= (input[i] & 0xffL) << ((i & 0x7) << 3);
        }

        return output;
    }

    /**
     * Creates a {@code long[]} value from a sequence of bytes. The longs are
     * filled in little-endian order (least significant byte first).
     *
     * @param input Input bytes
     * @param length Output array length
     * @return a {@code long[]}.
     */
    static long[] byteArray2LongArray(byte[] input, int length) {
        final long[] output = new long[length];

        // Overflow-safe minimum using long
        final int n = (int) Math.min(input.length, length * (long) Long.BYTES);
        // Little-endian fill
        for (int i = 0; i < n; i++) {
            // i              = byte index
            // i >> 3         = long index
            // i & 0x7        = byte number in the long  [0, 7]
            // (i & 0x7) << 3 = little-endian byte shift to the long {0, 8, 16, 24, 32, 36, 40, 48, 56}
            output[i >> 3] |= (input[i] & 0xffL) << ((i & 0x7) << 3);
        }

        return output;
    }
}
