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

import java.util.function.LongToDoubleFunction;
import org.apache.commons.rng.UniformRandomProvider;

/**
 * Sampling from a zeta distribution.
 *
 * <p>Note that the zeta distribution has an upper limit of positive infinity. This
 * implementation is clipped to {@link Integer#MAX_VALUE}. As the exponent \( s \to 1 \)
 * sampling will become biased to 2<sup>31</sup> - 1 as the distribution density is
 * truncated by the {@code int} limit. When the exponent is large the density
 * of the zeta distribution is concentrated at 1. In {@code double} precision this
 * limits the rejection method to {@code s <= 54}; larger {@code s} will always sample 1.
 *
 * <p>Rejection sampling method for a zeta distribution adapted from:
 * <blockquote>
 *   Luc Devroye (1986)
 *   <i>"Non-uniform random variate generation",</i><br>
 *   <strong>Springer New York, NY</strong> pp 550-552.
 * </blockquote>
 *
 * <p>Sampling uses {@link UniformRandomProvider#nextLong()}.</p>
 *
 * @see <a href="https://en.wikipedia.org/wiki/Zeta_distribution">Zeta distribution (Wikipedia)</a>
 * @since 1.4
 */
public final class ZetaSampler {
    /** The minimum exponent. The exponent must be above 1. */
    private static final double MIN_EXPONENT = Math.nextUp(1.0);
    /**
     * The maximum exponent for the rejection sampler.
     * Note that if the sampler uses u in (0, 1] to bias towards 1 then the sample
     * x = floor ( U^{-1/(a-1)} ) is always 1 when a is large.
     * The threshold for a is 54 if u=2^-53.
     * Note the zeta distribution CDF(x=1; s=54) = 1.0 in {@code double} precision.
     */
    private static final double REJECTION_EXPONENT = 54;

    /**
     * Sample from the zeta distribution using a rejection method.
     * Package-private for testing.
     */
    static final class RejectionZetaSampler implements SharedStateDiscreteSampler {
        /**
         * The threshold to bias the extreme sample to 1 or infinity. Change the
         * extreme sample of the zeta distribution using the midpoint of the support
         * domain, i.e. x = 2^31 / 2; cdf(x; a) = sf(x; a) ~ 0.5.
         */
        private static final double THRESHOLD = 1.0324376395045163;
        /** ln(2). */
        private static final double LN2 = Math.log(2.0);

        /** Source of randomness. */
        private final UniformRandomProvider rng;
        /** a - 1. */
        private final double am1;
        /** Reciprocal of (a - 1) = 1 / (a - 1). */
        private final double ram1;
        /** 2^(a-1) / (2^(a-1) - 1). */
        private final double bObm1;
        /** Function to compute u in [0, 1]. */
        private final LongToDoubleFunction nextU;

        /**
         * Create an instance.
         *
         * @param rng Source of randomness.
         * @param a Exponent of the zeta distribution ({@code a > 1}).
         */
        RejectionZetaSampler(UniformRandomProvider rng, double a) {
            this.rng = rng;
            am1 = a - 1;
            ram1 = 1 / am1;
            // b = 2^(a-1)
            // This will not overflow within the usable range of the algorithm.
            // We never expect infinity / infinity = NaN.
            final double b = Math.exp(LN2 * am1);
            final double bm1 = Math.expm1(LN2 * am1);
            bObm1 = b / bm1;
            // Note:
            // u in [0, 1]
            // u == 0 : x == inf
            // u == 1 : x == 1
            // When a -> 1 then bias to infinity; otherwise bias to 1.
            nextU = a <= THRESHOLD ?
                // u in [0, 1)
                InternalUtils::makeDouble :
                // u in (0, 1]
                InternalUtils::makeNonZeroDouble;
        }

        /**
         * Copy constructor.
         *
         * @param rng Source of randomness.
         * @param source Source to copy.
         */
        private RejectionZetaSampler(UniformRandomProvider rng, RejectionZetaSampler source) {
            this.rng = rng;
            am1 = source.am1;
            ram1 = source.ram1;
            bObm1 = source.bObm1;
            nextU = source.nextU;
        }

        @Override
        public int sample() {
            double u;
            double v;
            double x;
            double t;
            double tm1;
            for (;;) {
                // Generate iid uniform [0, 1] random variate U, V.
                // Sampling only uses nextLong.
                u = nextU.applyAsDouble(rng.nextLong());
                v = InternalUtils.makeDouble(rng.nextLong());

                // X = floor ( U^{-1/(a-1)} ) , X in [1, inf]
                x = Math.floor(Math.pow(u, -ram1));

                // T = (1 + 1/x)^(a-1)
                // This can create T ~ 1 for large X so
                // avoid precision loss using exp(log(1 + 1/x) * (a-1)).
                t = Math.log1p(1 / x) * am1;
                tm1 = Math.expm1(t);
                t = Math.exp(t);

                // Until:
                //    T-1    T
                // VX --- <= -
                //    b-1    b

                // Note: If X==infinity then T==1.
                // Avoid infinity * (T - 1) == NaN by rearrangement:
                //     b      T
                // VX --- <= ---
                //    b-1    T-1

                if (v * x * bObm1 <= t / tm1) {
                    // Truncates x >= 2^31 to integer max
                    return (int) x;
                }
            }
        }

        /** {@inheritDoc} */
        @Override
        public String toString() {
            return "Zeta deviate [" + rng.toString() + "]";
        }

        @Override
        public SharedStateDiscreteSampler withUniformRandomProvider(UniformRandomProvider rng) {
            return new RejectionZetaSampler(rng, this);
        }
    }

    /**
     * Sample from the zeta distribution when the density is entirely concentrated at x=1.
     */
    private static final class LargeExponentZetaSampler implements SharedStateDiscreteSampler {
        /** The single instance. */
        static final LargeExponentZetaSampler INSTANCE = new LargeExponentZetaSampler();

        @Override
        public int sample() {
            return 1;
        }

        @Override
        public String toString() {
            return "Zeta(x=1) deviate";
        }

        @Override
        public SharedStateDiscreteSampler withUniformRandomProvider(UniformRandomProvider rng) {
            // No requirement for a new instance
            return this;
        }
    }

    /** Class contains only static methods. */
    private ZetaSampler() {}

    /**
     * Creates a new zeta distribution sampler.
     *
     * <p>When {@code s > 54} the sample will always be 1. See the {@linkplain ZetaSampler
     * class-level} documentation for details.
     *
     * @param rng Generator of uniformly distributed random numbers.
     * @param exponent Exponent.
     * @return the sampler
     * @throws IllegalArgumentException if {@code exponent <= 1} or is non-finite.
     */
    public static SharedStateDiscreteSampler of(UniformRandomProvider rng,
                                                double exponent) {
        InternalUtils.requireRangeClosed(MIN_EXPONENT, Double.POSITIVE_INFINITY, exponent, "exponent");
        return exponent <= REJECTION_EXPONENT ?
            new RejectionZetaSampler(rng, exponent) :
            LargeExponentZetaSampler.INSTANCE;
    }
}
