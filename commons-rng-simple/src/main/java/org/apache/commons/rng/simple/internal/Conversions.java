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
     * The fractional part of the the golden ratio, phi, scaled to 64-bits and rounded to odd.
     * <pre>
     * phi = (sqrt(5) - 1) / 2) * 2^64
     * </pre>
     * @see <a href="https://en.wikipedia.org/wiki/Golden_ratio">Golden ratio</a>
     */
    private static final long GOLDEN_RATIO = 0x9e3779b97f4a7c15L;

    /** No instances. */
    private Conversions() {}

    /**
     * Perform variant 13 of David Stafford's 64-bit mix function.
     * This is the mix function used in the {@link SplitMix64} RNG.
     *
     * <p>This is ranked first of the top 14 Stafford mixers.
     *
     * @param x the input value
     * @return the output value
     * @see <a href="http://zimbry.blogspot.com/2011/09/better-bit-mixing-improving-on.html">Better
     *      Bit Mixing - Improving on MurmurHash3&#39;s 64-bit Finalizer.</a>
     */
    private static long stafford13(long x) {
        x = (x ^ (x >>> 30)) * 0xbf58476d1ce4e5b9L;
        x = (x ^ (x >>> 27)) * 0x94d049bb133111ebL;
        return x ^ (x >>> 31);
    }

    /**
     * Creates a {@code long} value from an {@code int}. The conversion
     * is made as if seeding a SplitMix64 RNG and calling nextLong().
     *
     * @param input Input
     * @return a {@code long}.
     */
    static long int2long(int input) {
        return stafford13(input + GOLDEN_RATIO);
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
    static int[] int2intArray(int input, int length) {
        return long2intArray(input, length);
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
    static long[] int2longArray(int input, int length) {
        return long2longArray(input, length);
    }

    /**
     * Creates an {@code int} value from a {@code long}. The conversion
     * is made by xoring the upper and lower bits.
     *
     * @param input Input
     * @return an {@code int}.
     */
    static int long2int(long input) {
        return (int) input ^ (int) (input >>> 32);
    }

    /**
     * Creates an {@code int[]} value from a {@code long}. The conversion
     * is made as if seeding a SplitMix64 RNG and calling nextLong() to
     * generate the sequence and filling the ints
     * in little-endian order (least significant byte first).
     *
     * @param input Input
     * @param length Array length
     * @return an {@code int[]}.
     */
    static int[] long2intArray(long input, int length) {
        long v = input;
        final int[] output = new int[length];
        // Process pairs
        final int n = length & ~0x1;
        for (int i = 0; i < n; i += 2) {
            long x = stafford13(v += GOLDEN_RATIO);
            output[i] = (int) x;
            output[i + 1] = (int) (x >>> 32);
        }
        // Final value
        if (n < length) {
            output[n] = (int) stafford13(v + GOLDEN_RATIO);
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
    static long[] long2longArray(long input, int length) {
        long v = input;
        final long[] output = new long[length];
        for (int i = 0; i < length; i++) {
            output[i] = stafford13(v += GOLDEN_RATIO);
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
}
