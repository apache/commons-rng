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
package org.apache.commons.rng.core.util;

import java.util.Arrays;

/**
 * Utility for creating number types from one or two {@code int} values
 * or one {@code long} value, or a sequence of bytes.
 */
public final class NumberFactory {
    /**
     * The multiplier to convert the least significant 24-bits of an {@code int} to a {@code float}.
     * See {@link #makeFloat(int)}.
     *
     * <p>This is equivalent to 1.0f / (1 << 24).
     */
    private static final float FLOAT_MULTIPLIER = 0x1.0p-24f;
    /**
     * The multiplier to convert the least significant 53-bits of a {@code long} to a {@code double}.
     * See {@link #makeDouble(long)} and {@link #makeDouble(int, int)}.
     *
     * <p>This is equivalent to 1.0 / (1L << 53).
     */
    private static final double DOUBLE_MULTIPLIER = 0x1.0p-53d;
    /** Lowest byte mask. */
    private static final long LONG_LOWEST_BYTE_MASK = 0xffL;
    /** Number of bytes in a {@code long}. */
    private static final int LONG_SIZE = 8;
    /** Lowest byte mask. */
    private static final int INT_LOWEST_BYTE_MASK = 0xff;
    /** Number of bytes in a {@code int}. */
    private static final int INT_SIZE = 4;
    /** A mask to convert an {@code int} to an unsigned integer stored as a {@code long}. */
    private static final long INT_TO_UNSIGNED_BYTE_MASK = 0xffffffffL;
    /** The exception message prefix when a number is not strictly positive. */
    private static final String NOT_STRICTLY_POSITIVE = "Must be strictly positive: ";

    /**
     * Class contains only static methods.
     */
    private NumberFactory() {}

    /**
     * @param v Number.
     * @return a boolean.
     *
     * @deprecated Since version 1.2. Method has become obsolete following
     * <a href="https://issues.apache.org/jira/browse/RNG-57">RNG-57</a>.
     */
    @Deprecated
    public static boolean makeBoolean(int v) {
        return (v >>> 31) != 0;
    }

    /**
     * @param v Number.
     * @return a boolean.
     *
     * @deprecated Since version 1.2. Method has become obsolete following
     * <a href="https://issues.apache.org/jira/browse/RNG-57">RNG-57</a>.
     */
    @Deprecated
    public static boolean makeBoolean(long v) {
        return (v >>> 63) != 0;
    }

    /**
     * @param v Number.
     * @return a {@code double} value in the interval {@code [0, 1]}.
     */
    public static double makeDouble(long v) {
        // Require the least significant 53-bits so shift the higher bits across
        return (v >>> 11) * DOUBLE_MULTIPLIER;
    }

    /**
     * @param v Number (high order bits).
     * @param w Number (low order bits).
     * @return a {@code double} value in the interval {@code [0, 1]}.
     */
    public static double makeDouble(int v,
                                    int w) {
        // Require the least significant 53-bits from a long.
        // Join the most significant 26 from v with 27 from w.
        final long high = ((long) (v >>> 6)) << 27;  // 26-bits remain
        final int low = w >>> 5;                     // 27-bits remain
        return (high | low) * DOUBLE_MULTIPLIER;
    }

    /**
     * @param v Number.
     * @return a {@code float} value in the interval {@code [0, 1]}.
     */
    public static float makeFloat(int v) {
        // Require the least significant 24-bits so shift the higher bits across
        return (v >>> 8) * FLOAT_MULTIPLIER;
    }

    /**
     * @param v Number (high order bits).
     * @param w Number (low order bits).
     * @return a {@code long} value.
     */
    public static long makeLong(int v,
                                int w) {
        return (((long) v) << 32) | (w & 0xffffffffL);
    }

    /**
     * Creates an {@code int} from a {@code long}.
     *
     * @param v Number.
     * @return an {@code int} value made from the "xor" of the
     * {@link #extractHi(long) high order bits} and
     * {@link #extractLo(long) low order bits} of {@code v}.
     *
     * @deprecated Since version 1.2. Method has become obsolete following
     * <a href="https://issues.apache.org/jira/browse/RNG-57">RNG-57</a>.
     */
    @Deprecated
    public static int makeInt(long v) {
        return extractHi(v) ^ extractLo(v);
    }

    /**
     * Creates an {@code int} from a {@code long}, using the high order bits.
     *
     * <p>The returned value is such that if</p>
     * <pre><code>
     *  vL = extractLo(v);
     *  vH = extractHi(v);
     * </code></pre>
     *
     * <p>then {@code v} is equal to {@link #makeLong(int,int) makeLong(vH, vL)}.</p>
     *
     * @param v Number.
     * @return an {@code int} value made from the most significant bits
     * of {@code v}.
     */
    public static int extractHi(long v) {
        return (int) (v >>> 32);
    }

    /**
     * Creates an {@code int} from a {@code long}, using the low order bits.
     *
     * <p>The returned value is such that if</p>
     *
     * <pre><code>
     *  vL = extractLo(v);
     *  vH = extractHi(v);
     * </code></pre>
     *
     * <p>then {@code v} is equal to {@link #makeLong(int,int) makeLong(vH, vL)}.</p>
     *
     * @param v Number.
     * @return an {@code int} value made from the least significant bits
     * of {@code v}.
     */
    public static int extractLo(long v) {
        return (int) v;
    }

    /**
     * Splits a {@code long} into 8 bytes.
     *
     * @param v Value.
     * @return the bytes that compose the given value (least-significant
     * byte first).
     */
    public static byte[] makeByteArray(long v) {
        final byte[] b = new byte[LONG_SIZE];

        for (int i = 0; i < LONG_SIZE; i++) {
            final int shift = i * 8;
            b[i] = (byte) ((v >>> shift) & LONG_LOWEST_BYTE_MASK);
        }

        return b;
    }

    /**
     * Creates a {@code long} from 8 bytes.
     *
     * @param input Input.
     * @return the value that correspond to the given bytes assuming
     * that the is ordered in increasing byte significance (i.e. the
     * first byte in the array is the least-siginficant).
     * @throws IllegalArgumentException if {@code input.length != 8}.
     */
    public static long makeLong(byte[] input) {
        checkSize(LONG_SIZE, input.length);

        long v = 0;
        for (int i = 0; i < LONG_SIZE; i++) {
            final int shift = i * 8;
            v |= (((long) input[i]) & LONG_LOWEST_BYTE_MASK) << shift;
        }

        return v;
    }

    /**
     * Splits an array of {@code long} values into a sequence of bytes.
     * This method calls {@link #makeByteArray(long)} for each element of
     * the {@code input}.
     *
     * @param input Input.
     * @return an array of bytes.
     */
    public static byte[] makeByteArray(long[] input) {
        final int size = input.length * LONG_SIZE;
        final byte[] b = new byte[size];

        for (int i = 0; i < input.length; i++) {
            final byte[] current = makeByteArray(input[i]);
            System.arraycopy(current, 0, b, i * LONG_SIZE, LONG_SIZE);
        }

        return b;
    }

    /**
     * Creates an array of {@code long} values from a sequence of bytes.
     * This method calls {@link #makeLong(byte[])} for each subsequence
     * of 8 bytes.
     *
     * @param input Input.
     * @return an array of {@code long}.
     * @throws IllegalArgumentException if {@code input.length} is not
     * a multiple of 8.
     */
    public static long[] makeLongArray(byte[] input) {
        final int size = input.length;
        final int num = size / LONG_SIZE;
        checkSize(num * LONG_SIZE, size);

        final long[] output = new long[num];
        for (int i = 0; i < num; i++) {
            final int from = i * LONG_SIZE;
            final byte[] current = Arrays.copyOfRange(input, from, from + LONG_SIZE);
            output[i] = makeLong(current);
        }

        return output;
    }

    /**
     * Splits an {@code int} into 4 bytes.
     *
     * @param v Value.
     * @return the bytes that compose the given value (least-significant
     * byte first).
     */
    public static byte[] makeByteArray(int v) {
        final byte[] b = new byte[INT_SIZE];

        for (int i = 0; i < INT_SIZE; i++) {
            final int shift = i * 8;
            b[i] = (byte) ((v >>> shift) & INT_LOWEST_BYTE_MASK);
        }

        return b;
    }

    /**
     * Creates an {@code int} from 4 bytes.
     *
     * @param input Input.
     * @return the value that correspond to the given bytes assuming
     * that the is ordered in increasing byte significance (i.e. the
     * first byte in the array is the least-siginficant).
     * @throws IllegalArgumentException if {@code input.length != 4}.
     */
    public static int makeInt(byte[] input) {
        checkSize(INT_SIZE, input.length);

        int v = 0;
        for (int i = 0; i < INT_SIZE; i++) {
            final int shift = i * 8;
            v |= (((int) input[i]) & INT_LOWEST_BYTE_MASK) << shift;
        }

        return v;
    }

    /**
     * Splits an array of {@code int} values into a sequence of bytes.
     * This method calls {@link #makeByteArray(int)} for each element of
     * the {@code input}.
     *
     * @param input Input.
     * @return an array of bytes.
     */
    public static byte[] makeByteArray(int[] input) {
        final int size = input.length * INT_SIZE;
        final byte[] b = new byte[size];

        for (int i = 0; i < input.length; i++) {
            final byte[] current = makeByteArray(input[i]);
            System.arraycopy(current, 0, b, i * INT_SIZE, INT_SIZE);
        }

        return b;
    }

    /**
     * Creates an array of {@code int} values from a sequence of bytes.
     * This method calls {@link #makeInt(byte[])} for each subsequence
     * of 4 bytes.
     *
     * @param input Input. Length must be a multiple of 4.
     * @return an array of {@code int}.
     * @throws IllegalArgumentException if {@code input.length} is not
     * a multiple of 4.
     */
    public static int[] makeIntArray(byte[] input) {
        final int size = input.length;
        final int num = size / INT_SIZE;
        checkSize(num * INT_SIZE, size);

        final int[] output = new int[num];
        for (int i = 0; i < num; i++) {
            final int from = i * INT_SIZE;
            final byte[] current = Arrays.copyOfRange(input, from, from + INT_SIZE);
            output[i] = makeInt(current);
        }

        return output;
    }

    /**
     * Generates an {@code int} value between 0 (inclusive) and the
     * specified value (exclusive) using a bit source of randomness.
     *
     * <p>This is equivalent to {@code floor(n * u)} where floor rounds down to the nearest
     * integer and {@code u} is a uniform random deviate in the range {@code [0,1)}. The
     * equivalent unsigned integer arithmetic is:</p>
     *
     * <pre>{@code (int) (n * (v & 0xffffffffL) / 2^32)}</pre>
     *
     * @param v Value to use as a source of randomness.
     * @param n Bound on the random number to be returned. Must be positive.
     * @return a random {@code int} value between 0 (inclusive) and {@code n}
     * (exclusive).
     * @throws IllegalArgumentException if {@code n} is negative.
     * @since 1.3
     */
    public static int makeIntInRange(int v, int n) {
        if (n <= 0) {
            throw new IllegalArgumentException(NOT_STRICTLY_POSITIVE + n);
        }
        // This computes the rounded fraction n * v / 2^32.
        // If v is an unsigned integer in the range 0 to 2^32 -1 then v / 2^32
        // is a uniform deviate in the range [0,1).
        // This is possible using unsigned integer arithmetic with long.
        // A division by 2^32 is a bit shift of 32.
        return (int) ((n * (v & INT_TO_UNSIGNED_BYTE_MASK)) >>> 32);
    }

    /**
     * Generates an {@code long} value between 0 (inclusive) and the
     * specified value (exclusive) using a bit source of randomness.
     *
     * <p>This is equivalent to {@code floor(n * u)} where floor rounds down to the nearest
     * long integer and {@code u} is a uniform random deviate in the range {@code [0,1)}. The
     * equivalent {@link java.math.BigInteger BigInteger} arithmetic is:</p>
     *
     * <pre><code>
     * // Construct big-endian byte representation from the long
     * byte[] bytes = new byte[8];
     * for(int i = 0; i &lt; 8; i++) {
     *     bytes[7 - i] = (byte)((v >>> (i * 8)) & 0xff);
     * }
     * BigInteger unsignedValue = new BigInteger(1, bytes);
     * return BigInteger.valueOf(n).multiply(unsignedValue).shiftRight(64).longValue();
     * </code></pre>
     *
     * <p>Notes:<p/>
     *
     * <ul>
     *  <li>The algorithm does not use {@link java.math.BigInteger BigInteger} and is optimised for
     *      128-bit arithmetic.
     *  <li>The algorithm uses the most significant bits of the source of randomness to construct
     *      the output.
     * </ul>
     *
     * @param v Value to use as a source of randomness.
     * @param n Bound on the random number to be returned. Must be positive.
     * @return a random {@code long} value between 0 (inclusive) and {@code n}
     * (exclusive).
     * @throws IllegalArgumentException if {@code n} is negative.
     * @since 1.3
     */
    public static long makeLongInRange(long v, long n) {
        if (n <= 0) {
            throw new IllegalArgumentException(NOT_STRICTLY_POSITIVE + n);
        }

        // This computes the rounded fraction n * v / 2^64.
        // If v is an unsigned integer in the range 0 to 2^64 -1 then v / 2^64
        // is a uniform deviate in the range [0,1).
        // This computation is possible using the 2s-complement integer arithmetic in Java
        // which is unsigned.
        //
        // Note: This adapts the multiply and carry idea in BigInteger arithmetic.
        // This is based on the following observation about the upper and lower bits of an
        // unsigned big-endian integer:
        //   ab * xy
        // =  b *  y
        // +  b * x0
        // + a0 *  y
        // + a0 * x0
        // = b * y
        // + b * x * 2^32
        // + a * y * 2^32
        // + a * x * 2^64
        //
        // A division by 2^64 is a bit shift of 64. So we must compute the equivalent of the
        // 128-bit results of multiplying two unsigned 64-bit numbers and return only the upper
        // 64-bits.
        final long a = n >>> 32;
        final long b = n & INT_TO_UNSIGNED_BYTE_MASK;
        final long x = v >>> 32;
        if (a == 0) {
            // Fast computation with long.
            // Use the upper bits from the source of randomness so the result is the same
            // as the full computation.
            return (b * x) >>> 32;
        }
        final long y = v & INT_TO_UNSIGNED_BYTE_MASK;
        if (b == 0) {
            // Fast computation with long.
            // Note: This will catch power of 2 edge cases with large n but ensure the most
            // significant bits are used rather than returning: v & (n - 1)
            // Cannot overflow at the maximum values.
            return ((a * y) >>> 32) +
                    (a * x);
        }

        // Note:
        // The result of two unsigned 32-bit integers multiplied together cannot overflow 64 bits.
        // The carry is the upper 32-bits of the 64-bit result; this is obtained by bit shift.
        // This algorithm thus computes the small numbers multiplied together and then sums
        // the carry on to the result for the next power 2^32.
        // This is a diagram of the bit cascade (using a 4 byte representation):
        //           byby byby
        //      bxbx bxbx 0000
        //      ayay ayay 0000
        // axax axax 0000 0000

        // First step cannot overflow since:
        // (0xffffffff * 0xffffffffl) >>> 32) + (0xffffffff * 0xffffffffL)
        // ((2^32-1) * (2^32-1) / 2^32) + (2^32-1) * (2^32-1)
        // ~ 2^32-1                     + (2^64 - 2^33 + 1)
        final long bx = ((b * y) >>> 32) +
                         (b * x);
        final long ay = a * y;

        // Sum the lower and upper 32-bits separately to control overflow
        final long carry = ((bx & INT_TO_UNSIGNED_BYTE_MASK) +
                            (ay & INT_TO_UNSIGNED_BYTE_MASK)) >>> 32;

        return carry +
              (bx >>> 32) +
              (ay >>> 32) +
               a * x;
    }

    /**
     * @param expected Expected value.
     * @param actual Actual value.
     * @throw IllegalArgumentException if {@code expected != actual}.
     */
    private static void checkSize(int expected,
                                  int actual) {
        if (expected != actual) {
            throw new IllegalArgumentException("Array size: Expected " + expected +
                                               " but was " + actual);
        }
    }
}
