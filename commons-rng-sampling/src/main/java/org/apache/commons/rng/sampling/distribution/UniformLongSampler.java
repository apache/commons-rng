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
 * Discrete uniform distribution sampler generating values of type {@code long}.
 *
 * <p>Sampling uses {@link UniformRandomProvider#nextLong}.</p>
 *
 * <p>When the range is a power of two the number of calls is 1 per sample.
 * Otherwise a rejection algorithm is used to ensure uniformity. In the worst
 * case scenario where the range spans half the range of a {@code long}
 * (2<sup>63</sup> + 1) the expected number of calls is 2 per sample.</p>
 *
 * @since 1.4
 */
public abstract class UniformLongSampler implements SharedStateLongSampler {
    /** Underlying source of randomness. */
    protected final UniformRandomProvider rng;

    /**
     * Discrete uniform distribution sampler when the sample value is fixed.
     */
    private static class FixedUniformLongSampler extends UniformLongSampler {
        /** The value. */
        private final long value;

        /**
         * @param value The value.
         */
        FixedUniformLongSampler(long value) {
            // No requirement for the RNG
            super(null);
            this.value = value;
        }

        @Override
        public long sample() {
            return value;
        }

        @Override
        public String toString() {
            // No RNG to include in the string
            return "Uniform deviate [X=" + value + "]";
        }

        @Override
        public UniformLongSampler withUniformRandomProvider(UniformRandomProvider rng) {
            // No requirement for the RNG
            return this;
        }
    }

    /**
     * Discrete uniform distribution sampler when the range is a power of 2 and greater than 1.
     * This sampler assumes the lower bound of the range is 0.
     *
     * <p>Note: This cannot be used when the range is 1 (2^0) as the shift would be 64-bits
     * which is ignored by the shift operator.</p>
     */
    private static class PowerOf2RangeUniformLongSampler extends UniformLongSampler {
        /** Bit shift to apply to the long sample. */
        private final int shift;

        /**
         * @param rng Generator of uniformly distributed random numbers.
         * @param range Maximum range of the sample (exclusive).
         * Must be a power of 2 greater than 2^0.
         */
        PowerOf2RangeUniformLongSampler(UniformRandomProvider rng,
                                        long range) {
            super(rng);
            this.shift = Long.numberOfLeadingZeros(range) + 1;
        }

        /**
         * @param rng Generator of uniformly distributed random numbers.
         * @param source Source to copy.
         */
        PowerOf2RangeUniformLongSampler(UniformRandomProvider rng,
                                        PowerOf2RangeUniformLongSampler source) {
            super(rng);
            this.shift = source.shift;
        }

        @Override
        public long sample() {
            // Use a bit shift to favour the most significant bits.
            return rng.nextLong() >>> shift;
        }

        @Override
        public UniformLongSampler withUniformRandomProvider(UniformRandomProvider rng) {
            return new PowerOf2RangeUniformLongSampler(rng, this);
        }
    }

    /**
     * Discrete uniform distribution sampler when the range is small
     * enough to fit in a positive long.
     * This sampler assumes the lower bound of the range is 0.
     */
    private static class SmallRangeUniformLongSampler extends UniformLongSampler {
        /** Maximum range of the sample (exclusive). */
        private final long n;

        /**
         * @param rng Generator of uniformly distributed random numbers.
         * @param range Maximum range of the sample (exclusive).
         */
        SmallRangeUniformLongSampler(UniformRandomProvider rng,
                                     long range) {
            super(rng);
            this.n = range;
        }

        @Override
        public long sample() {
            // Rejection algorithm copied from o.a.c.rng.core.BaseProvider
            // to avoid the (n <= 0) conditional.
            // See the JDK javadoc for java.util.Random.nextInt(int) for
            // a description of the algorithm.
            long bits;
            long val;
            do {
                bits = rng.nextLong() >>> 1;
                val  = bits % n;
            } while (bits - val + (n - 1) < 0);
            return val;
        }

        @Override
        public UniformLongSampler withUniformRandomProvider(UniformRandomProvider rng) {
            return new SmallRangeUniformLongSampler(rng, n);
        }
    }

    /**
     * Discrete uniform distribution sampler when the range between lower and upper is too large
     * to fit in a positive long.
     */
    private static class LargeRangeUniformLongSampler extends UniformLongSampler {
        /** Lower bound. */
        private final long lower;
        /** Upper bound. */
        private final long upper;

        /**
         * @param rng Generator of uniformly distributed random numbers.
         * @param lower Lower bound (inclusive) of the distribution.
         * @param upper Upper bound (inclusive) of the distribution.
         */
        LargeRangeUniformLongSampler(UniformRandomProvider rng,
                                     long lower,
                                     long upper) {
            super(rng);
            this.lower = lower;
            this.upper = upper;
        }

        @Override
        public long sample() {
            // Use a simple rejection method.
            // This is used when (upper-lower) >= Long.MAX_VALUE.
            // This will loop on average 2 times in the worst case scenario
            // when (upper-lower) == Long.MAX_VALUE.
            while (true) {
                final long r = rng.nextLong();
                if (r >= lower &&
                    r <= upper) {
                    return r;
                }
            }
        }

        @Override
        public UniformLongSampler withUniformRandomProvider(UniformRandomProvider rng) {
            return new LargeRangeUniformLongSampler(rng, lower, upper);
        }
    }

    /**
     * Adds an offset to an underlying discrete sampler.
     */
    private static class OffsetUniformLongSampler extends UniformLongSampler {
        /** The offset. */
        private final long offset;
        /** The long sampler. */
        private final UniformLongSampler sampler;

        /**
         * @param offset The offset for the sample.
         * @param sampler The discrete sampler.
         */
        OffsetUniformLongSampler(long offset,
                                 UniformLongSampler sampler) {
            // No requirement for the RNG
            super(null);
            this.offset = offset;
            this.sampler = sampler;
        }

        @Override
        public long sample() {
            return offset + sampler.sample();
        }

        @Override
        public String toString() {
            return sampler.toString();
        }

        @Override
        public UniformLongSampler withUniformRandomProvider(UniformRandomProvider rng) {
            return new OffsetUniformLongSampler(offset, sampler.withUniformRandomProvider(rng));
        }
    }

    /**
     * @param rng Generator of uniformly distributed random numbers.
     */
    UniformLongSampler(UniformRandomProvider rng) {
        this.rng = rng;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return "Uniform deviate [" + rng.toString() + "]";
    }

    /** {@inheritDoc} */
    // Redeclare the signature to return a UniformLongSampler not a SharedStateLongSampler
    @Override
    public abstract UniformLongSampler withUniformRandomProvider(UniformRandomProvider rng);

    /**
     * Creates a new discrete uniform distribution sampler.
     *
     * @param rng Generator of uniformly distributed random numbers.
     * @param lower Lower bound (inclusive) of the distribution.
     * @param upper Upper bound (inclusive) of the distribution.
     * @return the sampler
     * @throws IllegalArgumentException if {@code lower > upper}.
     */
    public static UniformLongSampler of(UniformRandomProvider rng,
                                        long lower,
                                        long upper) {
        if (lower > upper) {
            throw new IllegalArgumentException(lower  + " > " + upper);
        }

        // Choose the algorithm depending on the range

        // Edge case for no range.
        // This must be done first as the methods to handle lower == 0
        // do not handle upper == 0.
        if (upper == lower) {
            return new FixedUniformLongSampler(lower);
        }

        // Algorithms to ignore the lower bound if it is zero.
        if (lower == 0) {
            return createZeroBoundedSampler(rng, upper);
        }

        final long range = (upper - lower) + 1;
        // Check power of 2 first to handle range == 2^63.
        if (isPowerOf2(range)) {
            return new OffsetUniformLongSampler(lower,
                                                new PowerOf2RangeUniformLongSampler(rng, range));
        }
        if (range <= 0) {
            // The range is too wide to fit in a positive long (larger
            // than 2^63); use a simple rejection method.
            // Note: if range == 0 then the input is [Long.MIN_VALUE, Long.MAX_VALUE].
            // No specialisation exists for this and it is handled as a large range.
            return new LargeRangeUniformLongSampler(rng, lower, upper);
        }
        // Use a sample from the range added to the lower bound.
        return new OffsetUniformLongSampler(lower,
                                            new SmallRangeUniformLongSampler(rng, range));
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
    private static UniformLongSampler createZeroBoundedSampler(UniformRandomProvider rng,
                                                               long upper) {
        // Note: Handle any range up to 2^63 (which is negative as a signed
        // 64-bit long but handled as a power of 2)
        final long range = upper + 1;
        return isPowerOf2(range) ?
            new PowerOf2RangeUniformLongSampler(rng, range) :
            new SmallRangeUniformLongSampler(rng, range);
    }

    /**
     * Checks if the value is a power of 2.
     *
     * <p>This returns {@code true} for the value {@code Long.MIN_VALUE} which can be
     * handled as an unsigned long of 2^63.</p>
     *
     * @param value Value.
     * @return {@code true} if a power of 2
     */
    private static boolean isPowerOf2(final long value) {
        return value != 0 && (value & (value - 1)) == 0;
    }
}
