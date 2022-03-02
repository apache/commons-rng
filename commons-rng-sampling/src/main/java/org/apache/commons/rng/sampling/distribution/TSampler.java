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

import java.util.function.DoubleUnaryOperator;
import org.apache.commons.rng.UniformRandomProvider;

/**
 * Sampling from a T distribution.
 *
 * <p>Uses Bailey's algorithm for t-distribution sampling:</p>
 *
 * <blockquote>
 * <pre>
 * Bailey, R. W. (1994)
 * "Polar Generation of Random Variates with the t-Distribution."
 * Mathematics of Computation 62, 779-781.
 * </pre>
 * </blockquote>
 *
 * <p>Sampling uses {@link UniformRandomProvider#nextLong()}.</p>
 *
 * @see <a href="https://en.wikipedia.org/wiki/Student%27s_t-distribution">Student's T distribution (wikipedia)</a>
 * @see <a href="https://doi.org/10.2307/2153537">Mathematics of Computation, 62, 779-781</a>
 * @since 1.5
 */
public abstract class TSampler implements SharedStateContinuousSampler {
    /** Threshold for huge degrees of freedom. Above this value the CDF of the t distribution
     * matches the normal distribution. Value is 2/eps (where eps is the machine epsilon)
     * or approximately 9.0e15.  */
    private static final double HUGE_DF = 0x1.0p53;

    /** Source of randomness. */
    private final UniformRandomProvider rng;

    /**
     * Sample from a t-distribution using Bailey's algorithm.
     */
    private static final class StudentsTSampler extends TSampler {
        /** Threshold for large degrees of freedom. */
        private static final double LARGE_DF = 25;
        /** The multiplier to convert the least significant 53-bits of a {@code long} to a
         * uniform {@code double}. */
        private static final double DOUBLE_MULTIPLIER = 0x1.0p-53;

        /** Degrees of freedom. */
        private final double df;
        /** Function to compute pow(x, -2/v) - 1, where v = degrees of freedom. */
        private final DoubleUnaryOperator powm1;

        /**
         * @param rng Generator of uniformly distributed random numbers.
         * @param v Degrees of freedom.
         */
        StudentsTSampler(UniformRandomProvider rng,
                         double v) {
            super(rng);
            df = v;

            // The sampler requires pow(w, -2/v) - 1 with
            // 0 <= w <= 1; Expected(w) = sqrt(0.5).
            // When the exponent is small then pow(x, y) -> 1.
            // This affects large degrees of freedom.
            final double exponent = -2 / v;
            powm1 = v > LARGE_DF ?
                x -> Math.expm1(Math.log(x) * exponent) :
                x -> Math.pow(x, exponent) - 1;
        }

        /**
         * @param rng Generator of uniformly distributed random numbers.
         * @param source Source to copy.
         */
        private StudentsTSampler(UniformRandomProvider rng,
                                 StudentsTSampler source) {
            super(rng);
            df = source.df;
            powm1 = source.powm1;
        }

        /** {@inheritDoc} */
        @Override
        public double sample() {
            // Require u and v in [0, 1] and a random sign.
            // Create u in (0, 1] to avoid generating nan
            // from u*u/w (0/0) or r2*c2 (inf*0).
            final double u = InternalUtils.makeNonZeroDouble(nextLong());
            final double v = makeSignedDouble(nextLong());
            final double w = u * u + v * v;
            if (w > 1) {
                // Rejection frequency = 1 - pi/4 = 0.215.
                // Recursion will generate stack overflow given a broken RNG
                // and avoids an infinite loop.
                return sample();
            }
            // Sidestep a square-root calculation.
            final double c2 = u * u / w;
            final double r2 = df * powm1.applyAsDouble(w);
            // Choose sign at random from the sign of v.
            return Math.copySign(Math.sqrt(r2 * c2), v);
        }

        /** {@inheritDoc} */
        @Override
        public StudentsTSampler withUniformRandomProvider(UniformRandomProvider rng) {
            return new StudentsTSampler(rng, this);
        }

        /**
         * Creates a signed double in the range {@code [-1, 1)}. The magnitude is sampled evenly
         * from the 2<sup>54</sup> dyadic rationals in the range.
         *
         * <p>Note: This method will not return samples for both -0.0 and 0.0.
         *
         * @param bits the bits
         * @return the double
         */
        private static double makeSignedDouble(long bits) {
            // As per o.a.c.rng.core.utils.NumberFactory.makeDouble(long) but using a signed
            // shift of 10 in place of an unsigned shift of 11.
            // Use the upper 54 bits on the assumption they are more random.
            // The sign bit is maintained by the signed shift.
            // The next 53 bits generates a magnitude in the range [0, 2^53) or [-2^53, 0).
            return (bits >> 10) * DOUBLE_MULTIPLIER;
        }
    }

    /**
     * Sample from a t-distribution using a normal distribution.
     * This is used when the degrees of freedom is extremely large (e.g. {@code > 1e16}).
     */
    private static final class NormalTSampler extends TSampler {
        /** Underlying normalized Gaussian sampler. */
        private final NormalizedGaussianSampler sampler;

        /**
         * @param rng Generator of uniformly distributed random numbers.
         */
        NormalTSampler(UniformRandomProvider rng) {
            super(rng);
            this.sampler = ZigguratSampler.NormalizedGaussian.of(rng);
        }

        /** {@inheritDoc} */
        @Override
        public double sample() {
            return sampler.sample();

        }

        /** {@inheritDoc} */
        @Override
        public NormalTSampler withUniformRandomProvider(UniformRandomProvider rng) {
            return new NormalTSampler(rng);
        }
    }

    /**
     * @param rng Generator of uniformly distributed random numbers.
     */
    TSampler(UniformRandomProvider rng) {
        this.rng = rng;
    }

    /** {@inheritDoc} */
    // Redeclare the signature to return a TSampler not a SharedStateContinuousSampler
    @Override
    public abstract TSampler withUniformRandomProvider(UniformRandomProvider rng);

    /**
     * Generates a {@code long} value.
     * Used by algorithm implementations without exposing access to the RNG.
     *
     * @return the next random value
     */
    long nextLong() {
        return rng.nextLong();
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return "Student's t deviate [" + rng.toString() + "]";
    }

    /**
     * Create a new t distribution sampler.
     *
     * @param rng Generator of uniformly distributed random numbers.
     * @param degreesOfFreedom Degrees of freedom.
     * @return the sampler
     * @throws IllegalArgumentException if {@code degreesOfFreedom <= 0}
     */
    public static TSampler of(UniformRandomProvider rng,
                              double degreesOfFreedom) {
        if (degreesOfFreedom > HUGE_DF) {
            return new NormalTSampler(rng);
        } else if (degreesOfFreedom > 0) {
            return new StudentsTSampler(rng, degreesOfFreedom);
        } else {
            // df <= 0 or nan
            throw new IllegalArgumentException(
                "degrees of freedom is not strictly positive: " + degreesOfFreedom);
        }
    }
}
