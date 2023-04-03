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

/**
 * Utility for creating number types from one or two {@code int} values
 * or one {@code long} value, or a sequence of bytes.
 */
public final class NumberFactory {
    /**
     * The multiplier to convert the least significant 24-bits of an {@code int} to a {@code float}.
     * See {@link #makeFloat(int)}.
     *
     * <p>This is equivalent to {@code 1.0f / (1 << 24)}.
     */
    private static final float FLOAT_MULTIPLIER = 0x1.0p-24f;
    /**
     * The multiplier to convert the least significant 53-bits of a {@code long} to a {@code double}.
     * See {@link #makeDouble(long)} and {@link #makeDouble(int, int)}.
     *
     * <p>This is equivalent to {@code 1.0 / (1L << 53)}.
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

    /**
     * Class contains only static methods.
     */
    private NumberFactory() {}

    /**
     * Creates a {@code boolean} from an {@code int} value.
     *
     * @param v Number.
     * @return a boolean.
     *
     * @deprecated Since version 1.2. Method has become obsolete following
     * <a href="https://issues.apache.org/jira/browse/RNG-57">RNG-57</a>.
     */
    @Deprecated
    public static boolean makeBoolean(int v) {
        return v < 0;
    }

    /**
     * Creates a {@code boolean} from a {@code long} value.
     *
     * @param v Number.
     * @return a boolean.
     *
     * @deprecated Since version 1.2. Method has become obsolete following
     * <a href="https://issues.apache.org/jira/browse/RNG-57">RNG-57</a>.
     */
    @Deprecated
    public static boolean makeBoolean(long v) {
        return v < 0;
    }

    /**
     * Creates a {@code double} from a {@code long} value.
     *
     * @param v Number.
     * @return a {@code double} value in the interval {@code [0, 1]}.
     */
    public static double makeDouble(long v) {
        // Require the least significant 53-bits so shift the higher bits across
        return (v >>> 11) * DOUBLE_MULTIPLIER;
    }

    /**
     * Creates a {@code double} from two {@code int} values.
     *
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
     * Creates a {@code float} from an {@code int} value.
     *
     * @param v Number.
     * @return a {@code float} value in the interval {@code [0, 1]}.
     */
    public static float makeFloat(int v) {
        // Require the least significant 24-bits so shift the higher bits across
        return (v >>> 8) * FLOAT_MULTIPLIER;
    }

    /**
     * Creates a {@code long} from two {@code int} values.
     *
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
        putLong(v, b, 0);
        return b;
    }

    /**
     * Puts the {@code long} into the buffer starting at the given position.
     * Adds 8 bytes (least-significant first).
     *
     * @param v      Value.
     * @param buffer the buffer.
     * @param index  the index.
     */
    private static void putLong(long v,
                                byte[] buffer,
                                int index) {
        buffer[index    ] = (byte) (v         & LONG_LOWEST_BYTE_MASK);
        buffer[index + 1] = (byte)((v >>>  8) & LONG_LOWEST_BYTE_MASK);
        buffer[index + 2] = (byte)((v >>> 16) & LONG_LOWEST_BYTE_MASK);
        buffer[index + 3] = (byte)((v >>> 24) & LONG_LOWEST_BYTE_MASK);
        buffer[index + 4] = (byte)((v >>> 32) & LONG_LOWEST_BYTE_MASK);
        buffer[index + 5] = (byte)((v >>> 40) & LONG_LOWEST_BYTE_MASK);
        buffer[index + 6] = (byte)((v >>> 48) & LONG_LOWEST_BYTE_MASK);
        buffer[index + 7] = (byte) (v >>> 56);
    }

    /**
     * Creates a {@code long} from 8 bytes.
     *
     * @param input Input.
     * @return the value that correspond to the given bytes assuming
     * that the order is in increasing byte significance (i.e. the
     * first byte in the array is the least-significant).
     * @throws IllegalArgumentException if {@code input.length != 8}.
     */
    public static long makeLong(byte[] input) {
        checkSize(LONG_SIZE, input.length);
        return getLong(input, 0);
    }

    /**
     * Gets the {@code long} from the buffer starting at the given position.
     * Uses 8 bytes (least-significant first).
     *
     * @param input the input bytes.
     * @param index the index.
     * @return the value that correspond to the given bytes assuming
     * that the order is in increasing byte significance (i.e. the
     * first byte in the array is the least-significant).
     */
    private static long getLong(byte[] input,
                                int index) {
        return (input[index    ] & LONG_LOWEST_BYTE_MASK)       |
               (input[index + 1] & LONG_LOWEST_BYTE_MASK) <<  8 |
               (input[index + 2] & LONG_LOWEST_BYTE_MASK) << 16 |
               (input[index + 3] & LONG_LOWEST_BYTE_MASK) << 24 |
               (input[index + 4] & LONG_LOWEST_BYTE_MASK) << 32 |
               (input[index + 5] & LONG_LOWEST_BYTE_MASK) << 40 |
               (input[index + 6] & LONG_LOWEST_BYTE_MASK) << 48 |
               (input[index + 7] & LONG_LOWEST_BYTE_MASK) << 56;
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
            putLong(input[i], b, i * LONG_SIZE);
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
            output[i] = getLong(input, i * LONG_SIZE);
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
        putInt(v, b, 0);
        return b;
    }

    /**
     * Puts the {@code int} into the buffer starting at the given position.
     * Adds 4 bytes (least-significant first).
     *
     * @param v      the value.
     * @param buffer the buffer.
     * @param index  the index.
     */
    private static void putInt(int v,
                               byte[] buffer,
                               int index) {
        buffer[index    ] = (byte) (v         & INT_LOWEST_BYTE_MASK);
        buffer[index + 1] = (byte)((v >>>  8) & INT_LOWEST_BYTE_MASK);
        buffer[index + 2] = (byte)((v >>> 16) & INT_LOWEST_BYTE_MASK);
        buffer[index + 3] = (byte) (v >>> 24);
    }
    /**
     * Creates an {@code int} from 4 bytes.
     *
     * @param input Input.
     * @return the value that correspond to the given bytes assuming
     * that the order is in increasing byte significance (i.e. the
     * first byte in the array is the least-significant).
     * @throws IllegalArgumentException if {@code input.length != 4}.
     */
    public static int makeInt(byte[] input) {
        checkSize(INT_SIZE, input.length);
        return getInt(input, 0);
    }

    /**
     * Gets the {@code int} from the buffer starting at the given position.
     * Uses 4 bytes (least-significant first).
     *
     * @param input the input bytes.
     * @param index the index.
     * @return the value that correspond to the given bytes assuming
     * that the order is in increasing byte significance (i.e. the
     * first byte in the array is the least-significant).
     */
    private static int getInt(byte[] input,
                              int index) {
        return (input[index    ] & INT_LOWEST_BYTE_MASK)       |
               (input[index + 1] & INT_LOWEST_BYTE_MASK) <<  8 |
               (input[index + 2] & INT_LOWEST_BYTE_MASK) << 16 |
               (input[index + 3] & INT_LOWEST_BYTE_MASK) << 24;
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
            putInt(input[i], b, i * INT_SIZE);
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
            output[i] = getInt(input, i * INT_SIZE);
        }

        return output;
    }

    /**
     * @param expected Expected value.
     * @param actual Actual value.
     * @throws IllegalArgumentException if {@code expected != actual}.
     */
    private static void checkSize(int expected,
                                  int actual) {
        if (expected != actual) {
            throw new IllegalArgumentException("Array size: Expected " + expected +
                                               " but was " + actual);
        }
    }
}
