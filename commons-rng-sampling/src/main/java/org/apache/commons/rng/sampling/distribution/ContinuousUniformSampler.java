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
     * The bounds can be optionally excluded.
     *
     * @param rng Generator of uniformly distributed random numbers.
     * @param lo Lower bound.
     * @param hi Higher bound.
     * @param excludeBounds Set to {@code true} to use the open interval {@code (lower, upper)}.
     * @return the sampler
     * @since 1.4
     */
    public static SharedStateContinuousSampler of(UniformRandomProvider rng,
                                                  double lo,
                                                  double hi,
                                                  boolean excludeBounds) {
        return excludeBounds ?
            new OpenIntervalContinuousUniformSampler(rng, lo, hi) :
            new ContinuousUniformSampler(rng, lo, hi);
    }
}
