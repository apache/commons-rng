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
package org.apache.commons.rng.sampling.distribution;

import java.math.BigInteger;
import java.util.Arrays;
import org.apache.commons.rng.UniformRandomProvider;

/**
 * Distribution sampler that uses the Fast Loaded Dice Roller (FLDR). It can be used to
 * sample from {@code n} values each with an associated relative weight. If all unique items
 * are assigned the same weight it is more efficient to use the {@link DiscreteUniformSampler}.
 *
 * <p>Given a list {@code L} of {@code n} positive numbers,
 * where {@code L[i]} represents the relative weight of the {@code i}th side, FLDR returns
 * integer {@code i} with relative probability {@code L[i]}.
 *
 * <p>FLDR produces <em>exact</em> samples from the specified probability distribution.
 * <ul>
 *   <li>For integer weights, the probability of returning {@code i} is precisely equal to the
 *   rational number {@code L[i] / m}, where {@code m} is the sum of {@code L}.
 *   <li>For floating-points weights, each weight {@code L[i]} is converted to the
 *   corresponding rational number {@code p[i] / q[i]} where {@code p[i]} is a positive integer and
 *   {@code q[i]} is a power of 2. The rational weights are then normalized (exactly) to sum to unity.
 * </ul>
 *
 * <p>Note that if <em>exact</em> samples are not required then an alternative sampler that
 * ignores very small relative weights may have improved sampling performance.
 *
 * <p>This implementation is based on the algorithm in:
 *
 * <blockquote>
 *  Feras A. Saad, Cameron E. Freer, Martin C. Rinard, and Vikash K. Mansinghka.
 *  The Fast Loaded Dice Roller: A Near-Optimal Exact Sampler for Discrete Probability
 *  Distributions. In AISTATS 2020: Proceedings of the 23rd International Conference on
 *  Artificial Intelligence and Statistics, Proceedings of Machine Learning Research 108,
 *  Palermo, Sicily, Italy, 2020.
 * </blockquote>
 *
 * <p>Sampling uses {@link UniformRandomProvider#nextInt()} as the source of random bits.
 *
 * @see <a href="https://arxiv.org/abs/2003.03830">Saad et al (2020)
 * Proceedings of the 23rd International Conference on Artificial Intelligence and Statistics,
 * PMLR 108:1036-1046.</a>
 * @since 1.5
 */
public abstract class FastLoadedDiceRollerDiscreteSampler
    implements SharedStateDiscreteSampler {
    /**
     * The maximum size of an array.
     *
     * <p>This value is taken from the limit in Open JDK 8 {@code java.util.ArrayList}.
     * It allows VMs to reserve some header words in an array.
     */
    private static final int MAX_ARRAY_SIZE = Integer.MAX_VALUE - 8;
    /** The maximum biased exponent for a finite double.
     * This is offset by 1023 from {@code Math.getExponent(Double.MAX_VALUE)}. */
    private static final int MAX_BIASED_EXPONENT = 2046;
    /** Size of the mantissa of a double. Equal to 52 bits. */
    private static final int MANTISSA_SIZE = 52;
    /** Mask to extract the 52-bit mantissa from a long representation of a double. */
    private static final long MANTISSA_MASK = 0x000f_ffff_ffff_ffffL;
    /** BigInteger representation of {@link Long#MAX_VALUE}. */
    private static final BigInteger MAX_LONG = BigInteger.valueOf(Long.MAX_VALUE);
    /** The maximum offset that will avoid loss of bits for a left shift of a 53-bit value.
     * The value will remain positive for any shift {@code <=} this value. */
    private static final int MAX_OFFSET = 10;
    /** Initial value for no leaf node label. */
    private static final int NO_LABEL = Integer.MAX_VALUE;
    /** Name of the sampler. */
    private static final String SAMPLER_NAME = "Fast Loaded Dice Roller";

    /**
     * Class to handle the edge case of observations in only one category.
     */
    private static final class FixedValueDiscreteSampler extends FastLoadedDiceRollerDiscreteSampler {
        /** The sample value. */
        private final int sampleValue;

        /**
         * @param sampleValue Sample value.
         */
        FixedValueDiscreteSampler(int sampleValue) {
            this.sampleValue = sampleValue;
        }

        @Override
        public int sample() {
            return sampleValue;
        }

        @Override
        public FastLoadedDiceRollerDiscreteSampler withUniformRandomProvider(UniformRandomProvider rng) {
            return this;
        }

        @Override
        public String toString() {
            return SAMPLER_NAME;
        }
    }

    /**
     * Class to implement the FLDR sample algorithm.
     */
    private static final class FLDRSampler extends FastLoadedDiceRollerDiscreteSampler {
        /** Empty boolean source. This is the location of the sign-bit after 31 right shifts on
         * the boolean source. */
        private static final int EMPTY_BOOL_SOURCE = 1;

        /** Underlying source of randomness. */
        private final UniformRandomProvider rng;
        /** Number of categories. */
        private final int n;
        /** Number of levels in the discrete distribution generating (DDG) tree.
         * Equal to {@code ceil(log2(m))} where {@code m} is the sum of observations. */
        private final int k;
        /** Number of leaf nodes at each level. */
        private final int[] h;
        /** Stores the leaf node labels in increasing order. Named {@code H} in the FLDR paper. */
        private final int[] lH;

        /**
         * Provides a bit source for booleans.
         *
         * <p>A cached value from a call to {@link UniformRandomProvider#nextInt()}.
         *
         * <p>Only stores 31-bits when full as 1 bit has already been consumed.
         * The sign bit is a flag that shifts down so the source eventually equals 1
         * when all bits are consumed and will trigger a refill.
         */
        private int booleanSource = EMPTY_BOOL_SOURCE;

        /**
         * Creates a sampler.
         *
         * <p>The input parameters are not validated and must be correctly computed tables.
         *
         * @param rng Generator of uniformly distributed random numbers.
         * @param n Number of categories
         * @param k Number of levels in the discrete distribution generating (DDG) tree.
         * Equal to {@code ceil(log2(m))} where {@code m} is the sum of observations.
         * @param h Number of leaf nodes at each level.
         * @param lH Stores the leaf node labels in increasing order.
         */
        FLDRSampler(UniformRandomProvider rng,
                    int n,
                    int k,
                    int[] h,
                    int[] lH) {
            this.rng = rng;
            this.n = n;
            this.k = k;
            // Deliberate direct storage of input arrays
            this.h = h;
            this.lH = lH;
        }

        /**
         * Creates a copy with a new source of randomness.
         *
         * @param rng Generator of uniformly distributed random numbers.
         * @param source Source to copy.
         */
        private FLDRSampler(UniformRandomProvider rng,
                            FLDRSampler source) {
            this.rng = rng;
            this.n = source.n;
            this.k = source.k;
            this.h = source.h;
            this.lH = source.lH;
        }

        /** {@inheritDoc} */
        @Override
        public int sample() {
            // ALGORITHM 5: SAMPLE
            int c = 0;
            int d = 0;
            for (;;) {
                // b = flip()
                // d = 2 * d + (1 - b)
                d = (d << 1) + flip();
                if (d < h[c]) {
                    // z = H[d][c]
                    final int z = lH[d * k + c];
                    // assert z != NO_LABEL
                    if (z < n) {
                        return z;
                    }
                    d = 0;
                    c = 0;
                } else {
                    d = d - h[c];
                    c++;
                }
            }
        }

        /**
         * Provides a source of boolean bits.
         *
         * <p>Note: This replicates the boolean cache functionality of
         * {@code o.a.c.rng.core.source32.IntProvider}. The method has been simplified to return
         * an {@code int} value rather than a {@code boolean}.
         *
         * @return the bit (0 or 1)
         */
        private int flip() {
            int bits = booleanSource;
            if (bits == 1) {
                // Refill
                bits = rng.nextInt();
                // Store a refill flag in the sign bit and the unused 31 bits, return lowest bit
                booleanSource = Integer.MIN_VALUE | (bits >>> 1);
                return bits & 0x1;
            }
            // Shift down eventually triggering refill, return current lowest bit
            booleanSource = bits >>> 1;
            return bits & 0x1;
        }

        /** {@inheritDoc} */
        @Override
        public String toString() {
            return SAMPLER_NAME + " [" + rng.toString() + "]";
        }

        /** {@inheritDoc} */
        @Override
        public FastLoadedDiceRollerDiscreteSampler withUniformRandomProvider(UniformRandomProvider rng) {
            return new FLDRSampler(rng, this);
        }
    }

    /** Package-private constructor. */
    FastLoadedDiceRollerDiscreteSampler() {
        // Intentionally empty
    }

    /** {@inheritDoc} */
    // Redeclare the signature to return a FastLoadedDiceRollerSampler not a SharedStateLongSampler
    @Override
    public abstract FastLoadedDiceRollerDiscreteSampler withUniformRandomProvider(UniformRandomProvider rng);

    /**
     * Creates a sampler.
     *
     * <p>Note: The discrete distribution generating (DDG) tree requires {@code (n + 1) * k} entries
     * where {@code n} is the number of categories, {@code k == ceil(log2(m))} and {@code m}
     * is the sum of the observed frequencies. An exception is raised if this cannot be allocated
     * as a single array.
     *
     * <p>For reference the sum is limited to {@link Long#MAX_VALUE} and the value {@code k} to 63.
     * The number of categories is limited to approximately {@code ((2^31 - 1) / k) = 34,087,042}
     * when the sum of frequencies is large enough to create k=63.
     *
     * @param rng Generator of uniformly distributed random numbers.
     * @param frequencies Observed frequencies of the discrete distribution.
     * @return the sampler
     * @throws IllegalArgumentException if {@code frequencies} is null or empty, a
     * frequency is negative, the sum of all frequencies is either zero or
     * above {@link Long#MAX_VALUE}, or the size of the discrete distribution generating tree
     * is too large.
     */
    public static FastLoadedDiceRollerDiscreteSampler of(UniformRandomProvider rng,
                                                         long[] frequencies) {
        final long m = sum(frequencies);

        // Obtain indices of non-zero frequencies
        final int[] indices = indicesOfNonZero(frequencies);

        // Edge case for 1 non-zero weight. This also handles edge case for 1 observation
        // (as log2(m) == 0 will break the computation of the DDG tree).
        if (indices.length == 1) {
            return new FixedValueDiscreteSampler(indexOfNonZero(frequencies));
        }

        return createSampler(rng, frequencies, indices, m);
    }

    /**
     * Creates a sampler.
     *
     * <p>Weights are converted to rational numbers {@code p / q} where {@code q} is a power of 2.
     * The numerators {@code p} are scaled to use a common denominator before summing.
     *
     * <p>All weights are used to create the sampler. Weights with a small magnitude relative
     * to the largest weight can be excluded using the constructor method with the
     * relative magnitude parameter {@code alpha} (see {@link #of(UniformRandomProvider, double[], int)}).
     *
     * @param rng Generator of uniformly distributed random numbers.
     * @param weights Weights of the discrete distribution.
     * @return the sampler
     * @throws IllegalArgumentException if {@code weights} is null or empty, a
     * weight is negative, infinite or {@code NaN}, the sum of all weights is zero, or the size
     * of the discrete distribution generating tree is too large.
     * @see #of(UniformRandomProvider, double[], int)
     */
    public static FastLoadedDiceRollerDiscreteSampler of(UniformRandomProvider rng,
                                                         double[] weights) {
        return of(rng, weights, 0);
    }

    /**
     * Creates a sampler.
     *
     * <p>Weights are converted to rational numbers {@code p / q} where {@code q} is
     * a power of 2. The numerators {@code p} are scaled to use a common
     * denominator before summing.
     *
     * <p>Note: The discrete distribution generating (DDG) tree requires
     * {@code (n + 1) * k} entries where {@code n} is the number of categories,
     * {@code k == ceil(log2(m))} and {@code m} is the sum of the weight numerators
     * {@code q}. An exception is raised if this cannot be allocated as a single
     * array.
     *
     * <p>For reference the value {@code k} is equal to or greater than the ratio of
     * the largest to the smallest weight expressed as a power of 2. For
     * {@code Double.MAX_VALUE / Double.MIN_VALUE} this is ~2098. The value
     * {@code k} increases with the sum of the weight numerators. A number of
     * weights in excess of 1,000,000 with values equal to {@link Double#MAX_VALUE}
     * would be required to raise an exception when the minimum weight is
     * {@link Double#MIN_VALUE}.
     *
     * <p>Weights with a small magnitude relative to the largest weight can be
     * excluded using the relative magnitude parameter {@code alpha}. This will set
     * any weight to zero if the magnitude is approximately 2<sup>alpha</sup>
     * <em>smaller</em> than the largest weight. This comparison is made using only
     * the exponent of the input weights. The {@code alpha} parameter is ignored if
     * not above zero. Note that a small {@code alpha} parameter will exclude more
     * weights than a large {@code alpha} parameter.
     *
     * <p>The alpha parameter can be used to exclude categories that
     * have a very low probability of occurrence and will improve the construction
     * performance of the sampler. The effect on sampling performance depends on
     * the relative weights of the excluded categories; typically a high {@code alpha}
     * is used to exclude categories that would be visited with a very low probability
     * and the sampling performance is unchanged.
     *
     * <p><b>Implementation Note</b>
     *
     * <p>This method creates a sampler with <em>exact</em> samples from the
     * specified probability distribution. It is recommended to use this method:
     * <ul>
     *  <li>if the weights are computed, for example from a probability mass function; or
     *  <li>if the weights sum to an infinite value.
     * </ul>
     *
     * <p>If the weights are computed from empirical observations then it is
     * recommended to use the factory method
     * {@link #of(UniformRandomProvider, long[]) accepting frequencies}. This
     * requires the total number of observations to be representable as a long
     * integer.
     *
     * <p>Note that if all weights are scaled by a power of 2 to be integers, and
     * each integer can be represented as a positive 64-bit long value, then the
     * sampler created using this method will match the output from a sampler
     * created with the scaled weights converted to long values for the factory
     * method {@link #of(UniformRandomProvider, long[]) accepting frequencies}. This
     * assumes the sum of the integer values does not overflow.
     *
     * <p>It should be noted that the conversion of weights to rational numbers has
     * a performance overhead during construction (sampling performance is not
     * affected). This may be avoided by first converting them to integer values
     * that can be summed without overflow. For example by scaling values by
     * {@code 2^62 / sum} and converting to long by casting or rounding.
     *
     * <p>This approach may increase the efficiency of construction. The resulting
     * sampler may no longer produce <em>exact</em> samples from the distribution.
     * In particular any weights with a converted frequency of zero cannot be
     * sampled.
     *
     * @param rng Generator of uniformly distributed random numbers.
     * @param weights Weights of the discrete distribution.
     * @param alpha Alpha parameter.
     * @return the sampler
     * @throws IllegalArgumentException if {@code weights} is null or empty, a
     * weight is negative, infinite or {@code NaN}, the sum of all weights is zero,
     * or the size of the discrete distribution generating tree is too large.
     * @see #of(UniformRandomProvider, long[])
     */
    public static FastLoadedDiceRollerDiscreteSampler of(UniformRandomProvider rng,
                                                         double[] weights,
                                                         int alpha) {
        final int n = checkWeightsNonZeroLength(weights);

        // Convert floating-point double to a relative weight
        // using a shifted integer representation
        final long[] frequencies = new long[n];
        final int[] offsets = new int[n];
        convertToIntegers(weights, frequencies, offsets, alpha);

        // Obtain indices of non-zero weights
        final int[] indices = indicesOfNonZero(frequencies);

        // Edge case for 1 non-zero weight.
        if (indices.length == 1) {
            return new FixedValueDiscreteSampler(indexOfNonZero(frequencies));
        }

        final BigInteger m = sum(frequencies, offsets, indices);

        // Use long arithmetic if possible. This occurs when the weights are similar in magnitude.
        if (m.compareTo(MAX_LONG) <= 0) {
            // Apply the offset
            for (int i = 0; i < n; i++) {
                frequencies[i] <<= offsets[i];
            }
            return createSampler(rng, frequencies, indices, m.longValue());
        }

        return createSampler(rng, frequencies, offsets, indices, m);
    }

    /**
     * Sum the frequencies.
     *
     * @param frequencies Frequencies.
     * @return the sum
     * @throws IllegalArgumentException if {@code frequencies} is null or empty, a
     * frequency is negative, or the sum of all frequencies is either zero or above
     * {@link Long#MAX_VALUE}
     */
    private static long sum(long[] frequencies) {
        // Validate
        if (frequencies == null || frequencies.length == 0) {
            throw new IllegalArgumentException("frequencies must contain at least 1 value");
        }

        // Sum the values.
        // Combine all the sign bits in the observations and the intermediate sum in a flag.
        long m = 0;
        long signFlag = 0;
        for (final long o : frequencies) {
            m += o;
            signFlag |= o | m;
        }

        // Check for a sign-bit.
        if (signFlag < 0) {
            // One or more observations were negative, or the sum overflowed.
            for (final long o : frequencies) {
                if (o < 0) {
                    throw new IllegalArgumentException("frequencies must contain positive values: " + o);
                }
            }
            throw new IllegalArgumentException("Overflow when summing frequencies");
        }
        if (m == 0) {
            throw new IllegalArgumentException("Sum of frequencies is zero");
        }
        return m;
    }

    /**
     * Convert the floating-point weights to relative weights represented as
     * integers {@code value * 2^exponent}. The relative weight as an integer is:
     *
     * <pre>
     * BigInteger.valueOf(value).shiftLeft(exponent)
     * </pre>
     *
     * <p>Note that the weights are created using a common power-of-2 scaling
     * operation so the minimum exponent is zero.
     *
     * <p>A positive {@code alpha} parameter is used to set any weight to zero if
     * the magnitude is approximately 2<sup>alpha</sup> <em>smaller</em> than the
     * largest weight. This comparison is made using only the exponent of the input
     * weights.
     *
     * @param weights Weights of the discrete distribution.
     * @param values Output floating-point mantissas converted to 53-bit integers.
     * @param exponents Output power of 2 exponent.
     * @param alpha Alpha parameter.
     * @throws IllegalArgumentException if a weight is negative, infinite or
     * {@code NaN}, or the sum of all weights is zero.
     */
    private static void convertToIntegers(double[] weights, long[] values, int[] exponents, int alpha) {
        int maxExponent = Integer.MIN_VALUE;
        for (int i = 0; i < weights.length; i++) {
            final double weight = weights[i];
            // Ignore zero.
            // When creating the integer value later using bit shifts the result will remain zero.
            if (weight == 0) {
                continue;
            }
            final long bits = Double.doubleToRawLongBits(weight);

            // For the IEEE 754 format see Double.longBitsToDouble(long).

            // Extract the exponent (with the sign bit)
            int exp = (int) (bits >>> MANTISSA_SIZE);
            // Detect negative, infinite or NaN.
            // Note: Negative values sign bit will cause the exponent to be too high.
            if (exp > MAX_BIASED_EXPONENT) {
                throw new IllegalArgumentException("Invalid weight: " + weight);
            }
            long mantissa;
            if (exp == 0) {
                // Sub-normal number:
                mantissa = (bits & MANTISSA_MASK) << 1;
                // Here we convert to a normalised number by counting the leading zeros
                // to obtain the number of shifts of the most significant bit in
                // the mantissa that is required to get a 1 at position 53 (i.e. as
                // if it were a normal number with assumed leading bit).
                final int shift = Long.numberOfLeadingZeros(mantissa << 11);
                mantissa <<= shift;
                exp -= shift;
            } else {
                // Normal number. Add the implicit leading 1-bit.
                mantissa = (bits & MANTISSA_MASK) | (1L << MANTISSA_SIZE);
            }

            // Here the floating-point number is equal to:
            // mantissa * 2^(exp-1075)

            values[i] = mantissa;
            exponents[i] = exp;
            maxExponent = Math.max(maxExponent, exp);
        }

        // No exponent indicates that all weights are zero
        if (maxExponent == Integer.MIN_VALUE) {
            throw new IllegalArgumentException("Sum of weights is zero");
        }

        filterWeights(values, exponents, alpha, maxExponent);
        scaleWeights(values, exponents);
    }

    /**
     * Filters small weights using the {@code alpha} parameter.
     * A positive {@code alpha} parameter is used to set any weight to zero if
     * the magnitude is approximately 2<sup>alpha</sup> <em>smaller</em> than the
     * largest weight. This comparison is made using only the exponent of the input
     * weights.
     *
     * @param values 53-bit values.
     * @param exponents Power of 2 exponent.
     * @param alpha Alpha parameter.
     * @param maxExponent Maximum exponent.
     */
    private static void filterWeights(long[] values, int[] exponents, int alpha, int maxExponent) {
        if (alpha > 0) {
            // Filter weights. This must be done before the values are shifted so
            // the exponent represents the approximate magnitude of the value.
            for (int i = 0; i < exponents.length; i++) {
                if (maxExponent - exponents[i] > alpha) {
                    values[i] = 0;
                }
            }
        }
    }

    /**
     * Scale the weights represented as integers {@code value * 2^exponent} to use a
     * minimum exponent of zero. The values are scaled to remove any common trailing zeros
     * in their representation. This ultimately reduces the size of the discrete distribution
     * generating (DGG) tree.
     *
     * @param values 53-bit values.
     * @param exponents Power of 2 exponent.
     */
    private static void scaleWeights(long[] values, int[] exponents) {
        // Find the minimum exponent and common trailing zeros.
        int minExponent = Integer.MAX_VALUE;
        for (int i = 0; i < exponents.length; i++) {
            if (values[i] != 0) {
                minExponent = Math.min(minExponent, exponents[i]);
            }
        }
        // Trailing zeros occur when the original weights have a representation with
        // less than 52 binary digits, e.g. {1.5, 0.5, 0.25}.
        int trailingZeros = Long.SIZE;
        for (int i = 0; i < values.length && trailingZeros != 0; i++) {
            trailingZeros = Math.min(trailingZeros, Long.numberOfTrailingZeros(values[i]));
        }
        // Scale by a power of 2 so the minimum exponent is zero.
        for (int i = 0; i < exponents.length; i++) {
            exponents[i] -= minExponent;
        }
        // Remove common trailing zeros.
        if (trailingZeros != 0) {
            for (int i = 0; i < values.length; i++) {
                values[i] >>>= trailingZeros;
            }
        }
    }

    /**
     * Sum the integers at the specified indices.
     * Integers are represented as {@code value * 2^exponent}.
     *
     * @param values 53-bit values.
     * @param exponents Power of 2 exponent.
     * @param indices Indices to sum.
     * @return the sum
     */
    private static BigInteger sum(long[] values, int[] exponents, int[] indices) {
        BigInteger m = BigInteger.ZERO;
        for (final int i : indices) {
            m = m.add(toBigInteger(values[i], exponents[i]));
        }
        return m;
    }

    /**
     * Convert the value and left shift offset to a BigInteger.
     * It is assumed the value is at most 53-bits. This allows optimising the left
     * shift if it is below 11 bits.
     *
     * @param value 53-bit value.
     * @param offset Left shift offset (must be positive).
     * @return the BigInteger
     */
    private static BigInteger toBigInteger(long value, int offset) {
        // Ignore zeros. The sum method uses indices of non-zero values.
        if (offset <= MAX_OFFSET) {
            // Assume (value << offset) <= Long.MAX_VALUE
            return BigInteger.valueOf(value << offset);
        }
        return BigInteger.valueOf(value).shiftLeft(offset);
    }

    /**
     * Creates the sampler.
     *
     * <p>It is assumed the frequencies are all positive and the sum does not
     * overflow.
     *
     * @param rng Generator of uniformly distributed random numbers.
     * @param frequencies Observed frequencies of the discrete distribution.
     * @param indices Indices of non-zero frequencies.
     * @param m Sum of the frequencies.
     * @return the sampler
     */
    private static FastLoadedDiceRollerDiscreteSampler createSampler(UniformRandomProvider rng,
                                                                     long[] frequencies,
                                                                     int[] indices,
                                                                     long m) {
        // ALGORITHM 5: PREPROCESS
        // a == frequencies
        // m = sum(a)
        // h = leaf node count
        // H = leaf node label (lH)

        final int n = frequencies.length;

        // k = ceil(log2(m))
        final int k = 64 - Long.numberOfLeadingZeros(m - 1);
        // r = a(n+1) = 2^k - m
        final long r = (1L << k) - m;

        // Note:
        // A sparse matrix can often be used for H, as most of its entries are empty.
        // This implementation uses a 1D array for efficiency at the cost of memory.
        // This is limited to approximately ((2^31 - 1) / k), e.g. 34087042 when the sum of
        // observations is large enough to create k=63.
        // This could be handled using a 2D array. In practice a number of categories this
        // large is not expected and is currently not supported.
        final int[] h = new int[k];
        final int[] lH = new int[checkArraySize((n + 1L) * k)];
        Arrays.fill(lH, NO_LABEL);

        int d;
        for (int j = 0; j < k; j++) {
            final int shift = (k - 1) - j;
            final long bitMask = 1L << shift;

            d = 0;
            for (final int i : indices) {
                // bool w ← (a[i] >> (k − 1) − j)) & 1
                // h[j] = h[j] + w
                // if w then:
                if ((frequencies[i] & bitMask) != 0) {
                    h[j]++;
                    // H[d][j] = i
                    lH[d * k + j] = i;
                    d++;
                }
            }
            // process a(n+1) without extending the input frequencies array by 1
            if ((r & bitMask) != 0) {
                h[j]++;
                lH[d * k + j] = n;
            }
        }

        return new FLDRSampler(rng, n, k, h, lH);
    }

    /**
     * Creates the sampler. Frequencies are represented as a 53-bit value with a
     * left-shift offset.
     * <pre>
     * BigInteger.valueOf(value).shiftLeft(offset)
     * </pre>
     *
     * <p>It is assumed the frequencies are all positive.
     *
     * @param rng Generator of uniformly distributed random numbers.
     * @param frequencies Observed frequencies of the discrete distribution.
     * @param offsets Left shift offsets (must be positive).
     * @param indices Indices of non-zero frequencies.
     * @param m Sum of the frequencies.
     * @return the sampler
     */
    private static FastLoadedDiceRollerDiscreteSampler createSampler(UniformRandomProvider rng,
                                                                     long[] frequencies,
                                                                     int[] offsets,
                                                                     int[] indices,
                                                                     BigInteger m) {
        // Repeat the logic from createSampler(...) using extended arithmetic to test the bits

        // ALGORITHM 5: PREPROCESS
        // a == frequencies
        // m = sum(a)
        // h = leaf node count
        // H = leaf node label (lH)

        final int n = frequencies.length;

        // k = ceil(log2(m))
        final int k = m.subtract(BigInteger.ONE).bitLength();
        // r = a(n+1) = 2^k - m
        final BigInteger r = BigInteger.ONE.shiftLeft(k).subtract(m);

        final int[] h = new int[k];
        final int[] lH = new int[checkArraySize((n + 1L) * k)];
        Arrays.fill(lH, NO_LABEL);

        int d;
        for (int j = 0; j < k; j++) {
            final int shift = (k - 1) - j;

            d = 0;
            for (final int i : indices) {
                // bool w ← (a[i] >> (k − 1) − j)) & 1
                // h[j] = h[j] + w
                // if w then:
                if (testBit(frequencies[i], offsets[i], shift)) {
                    h[j]++;
                    // H[d][j] = i
                    lH[d * k + j] = i;
                    d++;
                }
            }
            // process a(n+1) without extending the input frequencies array by 1
            if (r.testBit(shift)) {
                h[j]++;
                lH[d * k + j] = n;
            }
        }

        return new FLDRSampler(rng, n, k, h, lH);
    }

    /**
     * Test the logical bit of the shifted integer representation.
     * The value is assumed to have at most 53-bits of information. The offset
     * is assumed to be positive. This is functionally equivalent to:
     * <pre>
     * BigInteger.valueOf(value).shiftLeft(offset).testBit(n)
     * </pre>
     *
     * @param value 53-bit value.
     * @param offset Left shift offset.
     * @param n Index of bit to test.
     * @return true if the bit is 1
     */
    private static boolean testBit(long value, int offset, int n) {
        if (n < offset) {
            // All logical trailing bits are zero
            return false;
        }
        // Test if outside the 53-bit value (note that the implicit 1 bit
        // has been added to the 52-bit mantissas for 'normal' floating-point numbers).
        final int bit = n - offset;
        return bit <= MANTISSA_SIZE && (value & (1L << bit)) != 0;
    }

    /**
     * Check the weights have a non-zero length.
     *
     * @param weights Weights.
     * @return the length
     */
    private static int checkWeightsNonZeroLength(double[] weights) {
        if (weights == null || weights.length == 0) {
            throw new IllegalArgumentException("weights must contain at least 1 value");
        }
        return weights.length;
    }

    /**
     * Create the indices of non-zero values.
     *
     * @param values Values.
     * @return the indices
     */
    private static int[] indicesOfNonZero(long[] values) {
        int n = 0;
        final int[] indices = new int[values.length];
        for (int i = 0; i < values.length; i++) {
            if (values[i] != 0) {
                indices[n++] = i;
            }
        }
        return Arrays.copyOf(indices, n);
    }

    /**
     * Find the index of the first non-zero frequency.
     *
     * @param frequencies Frequencies.
     * @return the index
     * @throws IllegalStateException if all frequencies are zero.
     */
    static int indexOfNonZero(long[] frequencies) {
        for (int i = 0; i < frequencies.length; i++) {
            if (frequencies[i] != 0) {
                return i;
            }
        }
        throw new IllegalStateException("All frequencies are zero");
    }

    /**
     * Check the size is valid for a 1D array.
     *
     * @param size Size
     * @return the size as an {@code int}
     * @throws IllegalArgumentException if the size is too large for a 1D array.
     */
    static int checkArraySize(long size) {
        if (size > MAX_ARRAY_SIZE) {
            throw new IllegalArgumentException("Unable to allocate array of size: " + size);
        }
        return (int) size;
    }
}
