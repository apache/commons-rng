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
package org.apache.commons.rng.core.source64;

/**
 * Utility for working with positive integer jump distances represented
 * as 64-bit integers.
 *
 * @since 1.7
 */
final class LongJumpDistances {
    /** 2^63. */
    private static final double TWO_POW_63 = 0x1.0p63;
    /** The value of the exponent for NaN when the exponent is
     * extracted and scaled to account for multiplying the
     * floating-point 52-bit mantissa to an integer. 1024 - 52 = 972. */
    private static final int NAN_EXP = 972;
    /** Mask to extract the 52-bit mantissa from a long representation of a double. */
    private static final long MANTISSA_MASK = 0x000f_ffff_ffff_ffffL;
    /** Small shift of a 53-bit significand where it remains within the same long. */
    private static final int SMALL_SHIFT = 11;

    /** Class contains only static methods. */
    private LongJumpDistances() {}

    /**
     * Validate the jump {@code distance} is valid within the given {@code period}
     * of the cycle.
     *
     * @param distance Jump distance.
     * @param period Cycle period.
     * @throws IllegalArgumentException if {@code distance} is negative,
     * or is greater than the {@code period}.
     */
    static void validateJump(double distance, double period) {
        // Logic negation will detect NaN
        if (!(distance < period && distance >= 0)) {
            throw new IllegalArgumentException(
                String.format("Invalid jump distance: %s (Period: %s)", distance, period));
        }
    }

    /**
     * Validate the jump distance of 2<sup>{@code logDistance}</sup> is valid within the
     * given cycle period of 2<sup>{@code logPeriod}</sup>.
     *
     * <p>Note: Negative {@code logDistance} is allowed as the distance is {@code >= 0}.
     *
     * @param logDistance Base-2 logarithm of the distance to jump forward with the state cycle.
     * @param logPeriod Base-2 logarithm of the cycle period.
     * @throws IllegalArgumentException if {@code logDistance >= logPeriod}.
     */
    static void validateJumpPowerOfTwo(int logDistance, int logPeriod) {
        if (logDistance >= logPeriod) {
            throw new IllegalArgumentException(
                String.format("Invalid jump distance: 2^%d (Period: 2^%d)", logDistance, logPeriod));
        }
    }

    /**
     * Write the {@code value} to an unsigned integer representation. Any fractional part
     * of the floating-point value is discarded. The integer part of the value must be
     * positive or an exception is raised.
     *
     * <p>The 53-bit significand of the {@code value} is converted to an integer, shifted
     * to align with a 64-bit boundary and written to the provided {@code result}, ordered
     * with the least significant bits first.
     *
     * <p>No bounds checking is performed. The {@code result} array is assumed to have a
     * length of at least 1 to store an unshifted 53-bit significand. The required array
     * size for values larger than 2^53 can be computed using:
     *
     * <pre>
     * 1 + Math.getExponent(value) / 64
     * </pre>
     *
     * <p>Parts of the array not used by the significand are unchanged. It is recommended
     * to use a newly created array for the result.
     *
     * <p>Note: For jump distances validated as less than the period of an output cycle
     * the result array length can be preallocated based on the maximum possible length.
     *
     * @param value Value
     * @param result the integer representation
     * @throws IllegalArgumentException if the {@code value} is negative or non-finite.
     */
    static void writeUnsignedInteger(double value, long[] result) {
        if (value < TWO_POW_63) {
            // Case for representable integers
            final long a = (long) value;
            if (a < 0) {
                throw new IllegalArgumentException(
                    "Integer value is negative: " + value);
            }
            result[0] = a;
            return;
        }

        // Large positive integer, infinity or NaN.
        // Extract significand (with implicit leading 1 bit) and exponent.
        // Note: sign is +1; exponent is non-zero.
        // See IEEE 754 format conversion description in
        // Double.longBitsToDouble.
        final long bits = Double.doubleToRawLongBits(value);
        final int exponent = (int) (bits >> 52) - 1075;

        // Check if finite
        if (exponent == NAN_EXP) {
            throw new IllegalArgumentException(
                "Double value is not finite: " + value);
        }

        final long significand = (bits & MANTISSA_MASK) | (1L << 52);

        // value == significand * 2**exponent

        // Return the offset to the first long, and then the shifted significand.
        // offset = exponent / 64
        final int offset = exponent >> 6;

        // Compute the remaining shift from the offset exponent (in [0, 63]).
        final int shift = exponent - (offset << 6);

        // Write the low and high parts of the 53-bit significand
        // across the 2 output long values.
        // Note the opposite of a left shift is an unsigned right shift
        // where only the lowest 6 bits of the shift are used:
        // x << n : x >>> (64 - n) == x >>> -n
        result[offset] = significand << shift;
        if (shift > SMALL_SHIFT) {
            result[offset + 1] = significand >>> -shift;
        }
    }
}
