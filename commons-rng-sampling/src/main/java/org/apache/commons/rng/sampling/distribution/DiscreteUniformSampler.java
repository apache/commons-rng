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

import org.apache.commons.rng.UniformRandomProvider;

/**
 * Discrete uniform distribution sampler.
 *
 * <p>Sampling uses {@link UniformRandomProvider#nextInt}.</p>
 *
 * <p>When the range is a power of two the number of calls is 1 per sample.
 * Otherwise a rejection algorithm is used to ensure uniformity. In the worst
 * case scenario where the range spans half the range of an {@code int}
 * (2<sup>31</sup> + 1) the expected number of calls is 2 per sample.</p>
 *
 * <p>This sampler can be used as a replacement for {@link UniformRandomProvider#nextInt}
 * with appropriate adjustment of the upper bound to be inclusive and will outperform that
 * method when the range is not a power of two. The advantage is gained by pre-computation
 * of the rejection threshold.</p>
 *
 * <p>The sampling algorithm is described in:</p>
 *
 * <blockquote>
 *  Lemire, D (2019). <i>Fast Random Integer Generation in an Interval.</i>
 *  <strong>ACM Transactions on Modeling and Computer Simulation</strong> 29 (1).
 * </blockquote>
 *
 * <p>The number of {@code int} values required per sample follows a geometric distribution with
 * a probability of success p of {@code 1 - ((2^32 % n) / 2^32)}. This requires on average 1/p random
 * {@code int} values per sample.</p>
 *
 * @see <a href="https://arxiv.org/abs/1805.10941">Fast Random Integer Generation in an Interval</a>
 *
 * @since 1.0
 */
public class DiscreteUniformSampler
    extends SamplerBase
    implements SharedStateDiscreteSampler {

    /** The appropriate uniform sampler for the parameters. */
    private final SharedStateDiscreteSampler delegate;

    /**
     * Base class for a sampler from a discrete uniform distribution. This contains the
     * source of randomness.
     */
    private abstract static class AbstractDiscreteUniformSampler
            implements SharedStateDiscreteSampler {
        /** Underlying source of randomness. */
        protected final UniformRandomProvider rng;

        /**
         * @param rng Generator of uniformly distributed random numbers.
         */
        AbstractDiscreteUniformSampler(UniformRandomProvider rng) {
            this.rng = rng;
        }

        @Override
        public String toString() {
            return "Uniform deviate [" + rng.toString() + "]";
        }
    }

    /**
     * Discrete uniform distribution sampler when the sample value is fixed.
     */
    private static class FixedDiscreteUniformSampler
            extends AbstractDiscreteUniformSampler {
        /** The value. */
        private final int value;

        /**
         * @param rng Generator of uniformly distributed random numbers.
         * @param value The value.
         */
        FixedDiscreteUniformSampler(UniformRandomProvider rng,
                                    int value) {
            super(rng);
            this.value = value;
        }

        @Override
        public int sample() {
            return value;
        }

        @Override
        public SharedStateDiscreteSampler withUniformRandomProvider(UniformRandomProvider rng) {
            // No requirement for the RNG
            return this;
        }
    }

    /**
     * Discrete uniform distribution sampler when the range is a power of 2 and greater than 1.
     * This sampler assumes the lower bound of the range is 0.
     *
     * <p>Note: This cannot be used when the range is 1 (2^0) as the shift would be 32-bits
     * which is ignored by the shift operator.</p>
     */
    private static class PowerOf2RangeDiscreteUniformSampler
            extends AbstractDiscreteUniformSampler {
        /** Bit shift to apply to the integer sample. */
        private final int shift;

        /**
         * @param rng Generator of uniformly distributed random numbers.
         * @param range Maximum range of the sample (exclusive).
         * Must be a power of 2 greater than 2^0.
         */
        PowerOf2RangeDiscreteUniformSampler(UniformRandomProvider rng,
                                            int range) {
            super(rng);
            this.shift = Integer.numberOfLeadingZeros(range) + 1;
        }

        /**
         * @param rng Generator of uniformly distributed random numbers.
         * @param source Source to copy.
         */
        PowerOf2RangeDiscreteUniformSampler(UniformRandomProvider rng,
                                            PowerOf2RangeDiscreteUniformSampler source) {
            super(rng);
            this.shift = source.shift;
        }

        @Override
        public int sample() {
            // Use a bit shift to favour the most significant bits.
            // Note: The result would be the same as the rejection method used in the
            // small range sampler when there is no rejection threshold.
            return rng.nextInt() >>> shift;
        }

        @Override
        public SharedStateDiscreteSampler withUniformRandomProvider(UniformRandomProvider rng) {
            return new PowerOf2RangeDiscreteUniformSampler(rng, this);
        }
    }

    /**
     * Discrete uniform distribution sampler when the range is small
     * enough to fit in a positive integer.
     * This sampler assumes the lower bound of the range is 0.
     *
     * <p>Implements the algorithm of Lemire (2019).</p>
     *
     * @see <a href="https://arxiv.org/abs/1805.10941">Fast Random Integer Generation in an Interval</a>
     */
    private static class SmallRangeDiscreteUniformSampler
            extends AbstractDiscreteUniformSampler {
        /** Maximum range of the sample (exclusive). */
        private final long n;

        /**
         * The level below which samples are rejected based on the fraction remainder.
         *
         * <p>Any remainder below this denotes that there are still floor(2^32 / n) more
         * observations of this sample from the interval [0, 2^32), where n is the range.</p>
         */
        private final long threshold;

        /**
         * @param rng Generator of uniformly distributed random numbers.
         * @param range Maximum range of the sample (exclusive).
         */
        SmallRangeDiscreteUniformSampler(UniformRandomProvider rng,
                                         int range) {
            super(rng);
            // Handle range as an unsigned 32-bit integer
            this.n = range & 0xffffffffL;
            // Compute 2^32 % n
            threshold = (1L << 32) % n;
        }

        /**
         * @param rng Generator of uniformly distributed random numbers.
         * @param source Source to copy.
         */
        SmallRangeDiscreteUniformSampler(UniformRandomProvider rng,
                                         SmallRangeDiscreteUniformSampler source) {
            super(rng);
            this.n = source.n;
            this.threshold = source.threshold;
        }

        @Override
        public int sample() {
            // Rejection method using multiply by a fraction:
            // n * [0, 2^32 - 1)
            //     -------------
            //         2^32
            // The result is mapped back to an integer and will be in the range [0, n).
            // Note this is comparable to range * rng.nextDouble() but with compensation for
            // non-uniformity due to round-off.
            long result;
            do {
                // Compute 64-bit unsigned product of n * [0, 2^32 - 1).
                // The upper 32-bits contains the sample value in the range [0, n), i.e. result / 2^32.
                // The lower 32-bits contains the remainder, i.e. result % 2^32.
                result = n * (rng.nextInt() & 0xffffffffL);

                // Test the sample uniformity.
                // Samples are observed on average (2^32 / n) times at a frequency of either
                // floor(2^32 / n) or ceil(2^32 / n).
                // To ensure all samples have a frequency of floor(2^32 / n) reject any results with
                // a remainder < (2^32 % n), i.e. the level below which denotes that there are still
                // floor(2^32 / n) more observations of this sample.
            } while ((result & 0xffffffffL) < threshold);
            // Divide by 2^32 to get the sample.
            return (int)(result >>> 32);
        }

        @Override
        public SharedStateDiscreteSampler withUniformRandomProvider(UniformRandomProvider rng) {
            return new SmallRangeDiscreteUniformSampler(rng, this);
        }
    }

    /**
     * Discrete uniform distribution sampler when the range between lower and upper is too large
     * to fit in a positive integer.
     */
    private static class LargeRangeDiscreteUniformSampler
            extends AbstractDiscreteUniformSampler {
        /** Lower bound. */
        private final int lower;
        /** Upper bound. */
        private final int upper;

        /**
         * @param rng Generator of uniformly distributed random numbers.
         * @param lower Lower bound (inclusive) of the distribution.
         * @param upper Upper bound (inclusive) of the distribution.
         */
        LargeRangeDiscreteUniformSampler(UniformRandomProvider rng,
                                         int lower,
                                         int upper) {
            super(rng);
            this.lower = lower;
            this.upper = upper;
        }

        @Override
        public int sample() {
            // Use a simple rejection method.
            // This is used when (upper-lower) >= Integer.MAX_VALUE.
            // This will loop on average 2 times in the worst case scenario
            // when (upper-lower) == Integer.MAX_VALUE.
            while (true) {
                final int r = rng.nextInt();
                if (r >= lower &&
                    r <= upper) {
                    return r;
                }
            }
        }

        @Override
        public SharedStateDiscreteSampler withUniformRandomProvider(UniformRandomProvider rng) {
            return new LargeRangeDiscreteUniformSampler(rng, lower, upper);
        }
    }

    /**
     * Adds an offset to an underlying discrete sampler.
     */
    private static class OffsetDiscreteUniformSampler
            extends AbstractDiscreteUniformSampler {
        /** The offset. */
        private final int offset;
        /** The discrete sampler. */
        private final SharedStateDiscreteSampler sampler;

        /**
         * @param offset The offset for the sample.
         * @param sampler The discrete sampler.
         */
        OffsetDiscreteUniformSampler(int offset,
                                     SharedStateDiscreteSampler sampler) {
            super(null);
            this.offset = offset;
            this.sampler = sampler;
        }

        @Override
        public int sample() {
            return offset + sampler.sample();
        }

        /** {@inheritDoc} */
        @Override
        public String toString() {
            return sampler.toString();
        }

        @Override
        public SharedStateDiscreteSampler withUniformRandomProvider(UniformRandomProvider rng) {
            return new OffsetDiscreteUniformSampler(offset, sampler.withUniformRandomProvider(rng));
        }
    }

    /**
     * This instance delegates sampling. Use the factory method
     * {@link #of(UniformRandomProvider, int, int)} to create an optimal sampler.
     *
     * @param rng Generator of uniformly distributed random numbers.
     * @param lower Lower bound (inclusive) of the distribution.
     * @param upper Upper bound (inclusive) of the distribution.
     * @throws IllegalArgumentException if {@code lower > upper}.
     */
    public DiscreteUniformSampler(UniformRandomProvider rng,
                                  int lower,
                                  int upper) {
        super(null);
        delegate = of(rng, lower, upper);
    }

    /** {@inheritDoc} */
    @Override
    public int sample() {
        return delegate.sample();
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return delegate.toString();
    }

    /** {@inheritDoc} */
    @Override
    public SharedStateDiscreteSampler withUniformRandomProvider(UniformRandomProvider rng) {
        // Direct return of the optimised sampler
        return delegate.withUniformRandomProvider(rng);
    }

    /**
     * Creates a new discrete uniform distribution sampler.
     *
     * @param rng Generator of uniformly distributed random numbers.
     * @param lower Lower bound (inclusive) of the distribution.
     * @param upper Upper bound (inclusive) of the distribution.
     * @return the sampler
     * @throws IllegalArgumentException if {@code lower > upper}.
     */
    public static SharedStateDiscreteSampler of(UniformRandomProvider rng,
                                                int lower,
                                                int upper) {
        if (lower > upper) {
            throw new IllegalArgumentException(lower  + " > " + upper);
        }

        // Choose the algorithm depending on the range

        // Edge case for no range.
        // This must be done first as the methods to handle lower == 0
        // do not handle upper == 0.
        if (upper == lower) {
            return new FixedDiscreteUniformSampler(rng, lower);
        }

        // Algorithms to ignore the lower bound if it is zero.
        if (lower == 0) {
            return createZeroBoundedSampler(rng, upper);
        }

        final int range = (upper - lower) + 1;
        // Check power of 2 first to handle range == 2^31.
        if (isPowerOf2(range)) {
            return new OffsetDiscreteUniformSampler(lower,
                                                    new PowerOf2RangeDiscreteUniformSampler(rng, range));
        }
        if (range <= 0) {
            // The range is too wide to fit in a positive int (larger
            // than 2^31); use a simple rejection method.
            // Note: if range == 0 then the input is [Integer.MIN_VALUE, Integer.MAX_VALUE].
            // No specialisation exists for this and it is handled as a large range.
            return new LargeRangeDiscreteUniformSampler(rng, lower, upper);
        }
        // Use a sample from the range added to the lower bound.
        return new OffsetDiscreteUniformSampler(lower,
                                                new SmallRangeDiscreteUniformSampler(rng, range));
    }

    /**
     * Create a new sampler for the range {@code 0} inclusive to {@code upper} inclusive.
     *
     * <p>This can handle any positive {@code upper}.
     *
     * @param rng Generator of uniformly distributed random numbers.
     * @param upper Upper bound (inclusive) of the distribution. Must be positive.
     * @return the sampler
     */
    private static AbstractDiscreteUniformSampler createZeroBoundedSampler(UniformRandomProvider rng,
                                                                           int upper) {
        // Note: Handle any range up to 2^31 (which is negative as a signed
        // 32-bit integer but handled as a power of 2)
        final int range = upper + 1;
        return isPowerOf2(range) ?
            new PowerOf2RangeDiscreteUniformSampler(rng, range) :
            new SmallRangeDiscreteUniformSampler(rng, range);
    }

    /**
     * Checks if the value is a power of 2.
     *
     * <p>This returns {@code true} for the value {@code Integer.MIN_VALUE} which can be
     * handled as an unsigned integer of 2^31.</p>
     *
     * @param value Value.
     * @return {@code true} if a power of 2
     */
    private static boolean isPowerOf2(final int value) {
        return value != 0 && (value & (value - 1)) == 0;
    }
}
