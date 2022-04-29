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
package org.apache.commons.rng;

/**
 * Support for {@link UniformRandomProvider} default methods.
 *
 * @since 1.5
 */
final class UniformRandomProviderSupport {
    /** Message for an invalid stream size. */
    private static final String INVALID_STREAM_SIZE = "Invalid stream size: ";
    /** Message for an invalid upper bound (must be positive, finite and above zero). */
    private static final String INVALID_UPPER_BOUND = "Upper bound must be above zero: ";
    /** Message format for an invalid range for lower inclusive and upper exclusive. */
    private static final String INVALID_RANGE = "Invalid range: [%s, %s)";
    /** 2^32. */
    private static final long POW_32 = 1L << 32;

    /** No instances. */
    private UniformRandomProviderSupport() {}

    /**
     * Validate the stream size.
     *
     * @param size Stream size.
     * @throws IllegalArgumentException if {@code size} is negative.
     */
    static void validateStreamSize(long size) {
        if (size < 0) {
            throw new IllegalArgumentException(INVALID_STREAM_SIZE + size);
        }
    }

    /**
     * Validate the upper bound.
     *
     * @param bound Upper bound (exclusive) on the random number to be returned.
     * @throws IllegalArgumentException if {@code bound} is equal to or less than zero.
     */
    static void validateUpperBound(int bound) {
        if (bound <= 0) {
            throw new IllegalArgumentException(INVALID_UPPER_BOUND + bound);
        }
    }

    /**
     * Validate the upper bound.
     *
     * @param bound Upper bound (exclusive) on the random number to be returned.
     * @throws IllegalArgumentException if {@code bound} is equal to or less than zero.
     */
    static void validateUpperBound(long bound) {
        if (bound <= 0) {
            throw new IllegalArgumentException(INVALID_UPPER_BOUND + bound);
        }
    }

    /**
     * Validate the upper bound.
     *
     * @param bound Upper bound (exclusive) on the random number to be returned.
     * @throws IllegalArgumentException if {@code bound} is equal to or less than zero, or
     * is not finite
     */
    static void validateUpperBound(float bound) {
        // Negation of logic will detect NaN
        if (!(bound > 0 && bound <= Float.MAX_VALUE)) {
            throw new IllegalArgumentException(INVALID_UPPER_BOUND + bound);
        }
    }

    /**
     * Validate the upper bound.
     *
     * @param bound Upper bound (exclusive) on the random number to be returned.
     * @throws IllegalArgumentException if {@code bound} is equal to or less than zero, or
     * is not finite
     */
    static void validateUpperBound(double bound) {
        // Negation of logic will detect NaN
        if (!(bound > 0 && bound <= Double.MAX_VALUE)) {
            throw new IllegalArgumentException(INVALID_UPPER_BOUND + bound);
        }
    }

    /**
     * Validate the range between the specified {@code origin} (inclusive) and the
     * specified {@code bound} (exclusive).
     *
     * @param origin Lower bound on the random number to be returned.
     * @param bound Upper bound (exclusive) on the random number to be returned.
     * @throws IllegalArgumentException if {@code origin} is greater than or equal to
     * {@code bound}.
     */
    static void validateRange(int origin, int bound) {
        if (origin >= bound) {
            throw new IllegalArgumentException(String.format(INVALID_RANGE, origin, bound));
        }
    }

    /**
     * Validate the range between the specified {@code origin} (inclusive) and the
     * specified {@code bound} (exclusive).
     *
     * @param origin Lower bound on the random number to be returned.
     * @param bound Upper bound (exclusive) on the random number to be returned.
     * @throws IllegalArgumentException if {@code origin} is greater than or equal to
     * {@code bound}.
     */
    static void validateRange(long origin, long bound) {
        if (origin >= bound) {
            throw new IllegalArgumentException(String.format(INVALID_RANGE, origin, bound));
        }
    }

    /**
     * Validate the range between the specified {@code origin} (inclusive) and the
     * specified {@code bound} (exclusive).
     *
     * @param origin Lower bound on the random number to be returned.
     * @param bound Upper bound (exclusive) on the random number to be returned.
     * @throws IllegalArgumentException if {@code origin} is not finite, or {@code bound}
     * is not finite, or {@code origin} is greater than or equal to {@code bound}.
     */
    static void validateRange(double origin, double bound) {
        if (origin >= bound || !Double.isFinite(origin) || !Double.isFinite(bound)) {
            throw new IllegalArgumentException(String.format(INVALID_RANGE, origin, bound));
        }
    }

    /**
     * Checks if the sub-range from fromIndex (inclusive) to fromIndex + size (exclusive) is
     * within the bounds of range from 0 (inclusive) to length (exclusive).
     *
     * <p>This function provides the functionality of
     * {@code java.utils.Objects.checkFromIndexSize} introduced in JDK 9. The
     * <a href="https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/Objects.html#checkFromIndexSize(int,int,int)">Objects</a>
     * javadoc has been reproduced for reference.
     *
     * <p>The sub-range is defined to be out of bounds if any of the following inequalities
     * is true:
     * <ul>
     * <li>{@code fromIndex < 0}
     * <li>{@code size < 0}
     * <li>{@code fromIndex + size > length}, taking into account integer overflow
     * <li>{@code length < 0}, which is implied from the former inequalities
     * </ul>
     *
     * <p>Note: This is not an exact implementation of the functionality of
     * {@code Objects.checkFromIndexSize}. The following changes have been made:
     * <ul>
     * <li>The method signature has been changed to avoid the return of {@code fromIndex};
     * this value is not used within this package.
     * <li>No checks are made for {@code length < 0} as this is assumed to be derived from
     * an array length.
     * </ul>
     *
     * @param fromIndex the lower-bound (inclusive) of the sub-interval
     * @param size the size of the sub-range
     * @param length the upper-bound (exclusive) of the range
     * @throws IndexOutOfBoundsException if the sub-range is out of bounds
     */
    static void validateFromIndexSize(int fromIndex, int size, int length) {
        // check for any negatives (assume 'length' is positive array length),
        // or overflow safe length check given the values are all positive
        // remaining = length - fromIndex
        if ((fromIndex | size) < 0 || size > length - fromIndex) {
            throw new IndexOutOfBoundsException(
                // Note: %<d is 'relative indexing' to re-use the last argument
                String.format("Range [%d, %<d + %d) out of bounds for length %d",
                    fromIndex, size, length));
        }
    }

    /**
     * Generates random bytes and places them into a user-supplied array.
     *
     * <p>The array is filled with bytes extracted from random {@code long} values. This
     * implies that the number of random bytes generated may be larger than the length of
     * the byte array.
     *
     * @param source Source of randomness.
     * @param bytes Array in which to put the generated bytes. Cannot be null.
     * @param start Index at which to start inserting the generated bytes.
     * @param len Number of bytes to insert.
     */
    static void nextBytes(UniformRandomProvider source,
                          byte[] bytes, int start, int len) {
        // Index of first insertion plus multiple of 8 part of length
        // (i.e. length with 3 least significant bits unset).
        final int indexLoopLimit = start + (len & 0x7ffffff8);

        // Start filling in the byte array, 8 bytes at a time.
        int index = start;
        while (index < indexLoopLimit) {
            final long random = source.nextLong();
            bytes[index++] = (byte) random;
            bytes[index++] = (byte) (random >>> 8);
            bytes[index++] = (byte) (random >>> 16);
            bytes[index++] = (byte) (random >>> 24);
            bytes[index++] = (byte) (random >>> 32);
            bytes[index++] = (byte) (random >>> 40);
            bytes[index++] = (byte) (random >>> 48);
            bytes[index++] = (byte) (random >>> 56);
        }

        // Index of last insertion + 1
        final int indexLimit = start + len;

        // Fill in the remaining bytes.
        if (index < indexLimit) {
            long random = source.nextLong();
            for (;;) {
                bytes[index++] = (byte) random;
                if (index == indexLimit) {
                    break;
                }
                random >>>= 8;
            }
        }
    }

    /**
     * Generates an {@code int} value between 0 (inclusive) and the specified value
     * (exclusive).
     *
     * @param source Source of randomness.
     * @param n Bound on the random number to be returned. Must be strictly positive.
     * @return a random {@code int} value between 0 (inclusive) and {@code n} (exclusive).
     */
    static int nextInt(UniformRandomProvider source,
                       int n) {
        // Lemire (2019): Fast Random Integer Generation in an Interval
        // https://arxiv.org/abs/1805.10941
        long m = (source.nextInt() & 0xffffffffL) * n;
        long l = m & 0xffffffffL;
        if (l < n) {
            // 2^32 % n
            final long t = POW_32 % n;
            while (l < t) {
                m = (source.nextInt() & 0xffffffffL) * n;
                l = m & 0xffffffffL;
            }
        }
        return (int) (m >>> 32);
    }

    /**
     * Generates an {@code int} value between the specified {@code origin} (inclusive) and
     * the specified {@code bound} (exclusive).
     *
     * @param source Source of randomness.
     * @param origin Lower bound on the random number to be returned.
     * @param bound Upper bound (exclusive) on the random number to be returned. Must be
     * above {@code origin}.
     * @return a random {@code int} value between {@code origin} (inclusive) and
     * {@code bound} (exclusive).
     */
    static int nextInt(UniformRandomProvider source,
                       int origin, int bound) {
        final int n = bound - origin;
        if (n > 0) {
            return nextInt(source, n) + origin;
        }
        // Range too large to fit in a positive integer.
        // Use simple rejection.
        int v = source.nextInt();
        while (v < origin || v >= bound) {
            v = source.nextInt();
        }
        return v;
    }

    /**
     * Generates an {@code long} value between 0 (inclusive) and the specified value
     * (exclusive).
     *
     * @param source Source of randomness.
     * @param n Bound on the random number to be returned. Must be strictly positive.
     * @return a random {@code long} value between 0 (inclusive) and {@code n}
     * (exclusive).
     */
    static long nextLong(UniformRandomProvider source,
                         long n) {
        long bits;
        long val;
        do {
            bits = source.nextLong() >>> 1;
            val  = bits % n;
        } while (bits - val + (n - 1) < 0);

        return val;
    }

    /**
     * Generates a {@code long} value between the specified {@code origin} (inclusive) and
     * the specified {@code bound} (exclusive).
     *
     * @param source Source of randomness.
     * @param origin Lower bound on the random number to be returned.
     * @param bound Upper bound (exclusive) on the random number to be returned. Must be
     * above {@code origin}.
     * @return a random {@code long} value between {@code origin} (inclusive) and
     * {@code bound} (exclusive).
     */
    static long nextLong(UniformRandomProvider source,
                         long origin, long bound) {
        final long n = bound - origin;
        if (n > 0) {
            return nextLong(source, n) + origin;
        }
        // Range too large to fit in a positive integer.
        // Use simple rejection.
        long v = source.nextLong();
        while (v < origin || v >= bound) {
            v = source.nextLong();
        }
        return v;
    }

    /**
     * Generates a {@code float} value between 0 (inclusive) and the specified value
     * (exclusive).
     *
     * @param source Source of randomness.
     * @param bound Bound on the random number to be returned. Must be strictly positive.
     * @return a random {@code float} value between 0 (inclusive) and {@code bound}
     * (exclusive).
     */
    static float nextFloat(UniformRandomProvider source,
                           float bound) {
        float v = source.nextFloat() * bound;
        if (v >= bound) {
            // Correct rounding
            v = Math.nextDown(bound);
        }
        return v;
    }

    /**
     * Generates a {@code float} value between the specified {@code origin} (inclusive)
     * and the specified {@code bound} (exclusive).
     *
     * @param source Source of randomness.
     * @param origin Lower bound on the random number to be returned. Must be finite.
     * @param bound Upper bound (exclusive) on the random number to be returned. Must be
     * above {@code origin} and finite.
     * @return a random {@code float} value between {@code origin} (inclusive) and
     * {@code bound} (exclusive).
     */
    static float nextFloat(UniformRandomProvider source,
                           float origin, float bound) {
        float v = source.nextFloat();
        // This expression allows (bound - origin) to be infinite
        // origin + (bound - origin) * v
        // == origin - origin * v + bound * v
        v = (1f - v) * origin + v * bound;
        if (v >= bound) {
            // Correct rounding
            v = Math.nextDown(bound);
        }
        return v;
    }

    /**
     * Generates a {@code double} value between 0 (inclusive) and the specified value
     * (exclusive).
     *
     * @param source Source of randomness.
     * @param bound Bound on the random number to be returned. Must be strictly positive.
     * @return a random {@code double} value between 0 (inclusive) and {@code bound}
     * (exclusive).
     */
    static double nextDouble(UniformRandomProvider source,
                             double bound) {
        double v = source.nextDouble() * bound;
        if (v >= bound) {
            // Correct rounding
            v = Math.nextDown(bound);
        }
        return v;
    }

    /**
     * Generates a {@code double} value between the specified {@code origin} (inclusive)
     * and the specified {@code bound} (exclusive).
     *
     * @param source Source of randomness.
     * @param origin Lower bound on the random number to be returned. Must be finite.
     * @param bound Upper bound (exclusive) on the random number to be returned. Must be
     * above {@code origin} and finite.
     * @return a random {@code double} value between {@code origin} (inclusive) and
     * {@code bound} (exclusive).
     */
    static double nextDouble(UniformRandomProvider source,
                             double origin, double bound) {
        double v = source.nextDouble();
        // This expression allows (bound - origin) to be infinite
        // origin + (bound - origin) * v
        // == origin - origin * v + bound * v
        v = (1f - v) * origin + v * bound;
        if (v >= bound) {
            // Correct rounding
            v = Math.nextDown(bound);
        }
        return v;
    }
}
