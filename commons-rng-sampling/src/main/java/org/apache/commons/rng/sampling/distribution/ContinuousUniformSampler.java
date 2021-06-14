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
 * Sampling from a uniform distribution.
 *
 * <p>Sampling uses {@link UniformRandomProvider#nextDouble()}.</p>
 *
 * @since 1.0
 */
public class ContinuousUniformSampler
    extends SamplerBase
    implements SharedStateContinuousSampler {
    /** The minimum ULP gap for the open interval when the doubles have the same sign. */
    private static final int MIN_ULP_SAME_SIGN = 2;
    /** The minimum ULP gap for the open interval when the doubles have the opposite sign. */
    private static final int MIN_ULP_OPPOSITE_SIGN = 3;

    /** Lower bound. */
    private final double lo;
    /** Higher bound. */
    private final double hi;
    /** Underlying source of randomness. */
    private final UniformRandomProvider rng;

    /**
     * Specialization to sample from an open interval {@code (lo, hi)}.
     */
    private static class OpenIntervalContinuousUniformSampler extends ContinuousUniformSampler {
        /**
         * @param rng Generator of uniformly distributed random numbers.
         * @param lo Lower bound.
         * @param hi Higher bound.
         */
        OpenIntervalContinuousUniformSampler(UniformRandomProvider rng, double lo, double hi) {
            super(rng, lo, hi);
        }

        @Override
        public double sample() {
            final double x = super.sample();
            // Due to rounding using a variate u in the open interval (0,1) with the original
            // algorithm may generate a value at the bound. Thus the bound is explicitly tested
            // and the sample repeated if necessary.
            if (x == getHi() || x == getLo()) {
                return sample();
            }
            return x;
        }

        @Override
        public SharedStateContinuousSampler withUniformRandomProvider(UniformRandomProvider rng) {
            return new OpenIntervalContinuousUniformSampler(rng, getLo(), getHi());
        }
    }

    /**
     * @param rng Generator of uniformly distributed random numbers.
     * @param lo Lower bound.
     * @param hi Higher bound.
     */
    public ContinuousUniformSampler(UniformRandomProvider rng,
                                    double lo,
                                    double hi) {
        super(null);
        this.rng = rng;
        this.lo = lo;
        this.hi = hi;
    }

    /** {@inheritDoc} */
    @Override
    public double sample() {
        final double u = rng.nextDouble();
        return u * hi + (1 - u) * lo;
    }

    /**
     * Gets the lower bound. This is deliberately scoped as package private.
     *
     * @return the lower bound
     */
    double getLo() {
        return lo;
    }

    /**
     * Gets the higher bound. This is deliberately scoped as package private.
     *
     * @return the higher bound
     */
    double getHi() {
        return hi;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return "Uniform deviate [" + rng.toString() + "]";
    }

    /**
     * {@inheritDoc}
     *
     * @since 1.3
     */
    @Override
    public SharedStateContinuousSampler withUniformRandomProvider(UniformRandomProvider rng) {
        return new ContinuousUniformSampler(rng, lo, hi);
    }

    /**
     * Creates a new continuous uniform distribution sampler.
     *
     * @param rng Generator of uniformly distributed random numbers.
     * @param lo Lower bound.
     * @param hi Higher bound.
     * @return the sampler
     * @since 1.3
     */
    public static SharedStateContinuousSampler of(UniformRandomProvider rng,
                                                  double lo,
                                                  double hi) {
        return new ContinuousUniformSampler(rng, lo, hi);
    }

    /**
     * Creates a new continuous uniform distribution sampler.
     *
     * <p>The bounds can be optionally excluded to sample from the open interval
     * {@code (lower, upper)}. In this case if the bounds have the same sign the
     * open interval must contain at least 1 double value between the limits; if the
     * bounds have opposite signs the open interval must contain at least 2 double values
     * between the limits excluding {@code -0.0}. Thus the interval {@code (-x,x)} will
     * raise an exception when {@code x} is {@link Double#MIN_VALUE}.
     *
     * @param rng Generator of uniformly distributed random numbers.
     * @param lo Lower bound.
     * @param hi Higher bound.
     * @param excludeBounds Set to {@code true} to use the open interval
     * {@code (lower, upper)}.
     * @return the sampler
     * @throws IllegalArgumentException If the open interval is invalid.
     * @since 1.4
     */
    public static SharedStateContinuousSampler of(UniformRandomProvider rng,
                                                  double lo,
                                                  double hi,
                                                  boolean excludeBounds) {
        if (excludeBounds) {
            if (!validateOpenInterval(lo, hi)) {
                throw new IllegalArgumentException("Invalid open interval (" +
                                                    lo + "," + hi + ")");
            }
            return new OpenIntervalContinuousUniformSampler(rng, lo, hi);
        }
        return new ContinuousUniformSampler(rng, lo, hi);
    }

    /**
     * Check that the open interval is valid. It must contain at least one double value
     * between the limits if the signs are the same, or two double values between the limits
     * if the signs are different (excluding {@code -0.0}).
     *
     * @param lo Lower bound.
     * @param hi Higher bound.
     * @return false is the interval is invalid
     */
    private static boolean validateOpenInterval(double lo, double hi) {
        // Get the raw bit representation.
        long bitsx = Double.doubleToRawLongBits(lo);
        long bitsy = Double.doubleToRawLongBits(hi);
        // xor will set the sign bit if the signs are different
        if ((bitsx ^ bitsy) < 0) {
            // Opposite signs. Drop the sign bit to represent the count of
            // bits above +0.0. When combined this should be above 2.
            // This prevents the range (-Double.MIN_VALUE, Double.MIN_VALUE)
            // which cannot be sampled unless the uniform deviate u=0.5.
            // (MAX_VALUE has all bits set except the most significant sign bit.)
            bitsx &= Long.MAX_VALUE;
            bitsy &= Long.MAX_VALUE;
            if (lessThanUnsigned(bitsx + bitsy, MIN_ULP_OPPOSITE_SIGN)) {
                return false;
            }
        } else {
            // Same signs, subtraction will count the ULP difference.
            // This should be above 1.
            if (Math.abs(bitsx - bitsy) < MIN_ULP_SAME_SIGN) {
                return false;
            }
        }
        return true;
    }

    /**
     * Compares two {@code long} values numerically treating the values as unsigned
     * to test if the first value is less than the second value.
     *
     * <p>See Long.compareUnsigned(long, long) in JDK 1.8.
     *
     * @param x the first value
     * @param y the second value
     * @return true if {@code x < y}
     */
    private static boolean lessThanUnsigned(long x, long y) {
        return x + Long.MIN_VALUE < y + Long.MIN_VALUE;
    }
}
