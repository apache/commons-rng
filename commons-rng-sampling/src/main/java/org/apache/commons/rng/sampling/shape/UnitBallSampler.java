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

package org.apache.commons.rng.sampling.shape;

import org.apache.commons.rng.UniformRandomProvider;
import org.apache.commons.rng.sampling.SharedStateObjectSampler;
import org.apache.commons.rng.sampling.distribution.ContinuousSampler;
import org.apache.commons.rng.sampling.distribution.NormalizedGaussianSampler;
import org.apache.commons.rng.sampling.distribution.ZigguratSampler;

/**
 * Generate coordinates <a href="http://mathworld.wolfram.com/BallPointPicking.html">
 * uniformly distributed within the unit n-ball</a>.
 *
 * <p>Sampling uses:</p>
 *
 * <ul>
 *   <li>{@link UniformRandomProvider#nextLong()}
 *   <li>{@link UniformRandomProvider#nextDouble()} (only for dimensions above 2)
 * </ul>
 *
 * @since 1.4
 */
public abstract class UnitBallSampler implements SharedStateObjectSampler<double[]> {
    /** The dimension for 1D sampling. */
    private static final int ONE_D = 1;
    /** The dimension for 2D sampling. */
    private static final int TWO_D = 2;
    /** The dimension for 3D sampling. */
    private static final int THREE_D = 3;
    /**
     * The multiplier to convert the least significant 53-bits of a {@code long} to a {@code double}.
     * Taken from o.a.c.rng.core.utils.NumberFactory.
     *
     * <p>This is equivalent to 1.0 / (1L << 53).
     */
    private static final double DOUBLE_MULTIPLIER = 0x1.0p-53d;

    /**
     * Sample uniformly from a 1D unit line.
     */
    private static class UnitBallSampler1D extends UnitBallSampler {
        /** The source of randomness. */
        private final UniformRandomProvider rng;

        /**
         * @param rng Source of randomness.
         */
        UnitBallSampler1D(UniformRandomProvider rng) {
            this.rng = rng;
        }

        @Override
        public double[] sample() {
            return new double[] {makeSignedDouble(rng.nextLong())};
        }

        @Override
        public UnitBallSampler withUniformRandomProvider(UniformRandomProvider rng) {
            return new UnitBallSampler1D(rng);
        }
    }

    /**
     * Sample uniformly from a 2D unit disk.
     */
    private static class UnitBallSampler2D extends UnitBallSampler {
        /** The source of randomness. */
        private final UniformRandomProvider rng;

        /**
         * @param rng Source of randomness.
         */
        UnitBallSampler2D(UniformRandomProvider rng) {
            this.rng = rng;
        }

        @Override
        public double[] sample() {
            // Generate via rejection method of a circle inside a square of edge length 2.
            // This should compute approximately 2^2 / pi = 1.27 square positions per sample.
            double x;
            double y;
            do {
                x = makeSignedDouble(rng.nextLong());
                y = makeSignedDouble(rng.nextLong());
            } while (x * x + y * y > 1.0);
            return new double[] {x, y};
        }

        @Override
        public UnitBallSampler withUniformRandomProvider(UniformRandomProvider rng) {
            return new UnitBallSampler2D(rng);
        }
    }

    /**
     * Sample uniformly from a 3D unit ball. This is an non-array based specialisation of
     * {@link UnitBallSamplerND} for performance.
     */
    private static class UnitBallSampler3D extends UnitBallSampler {
        /** The standard normal distribution. */
        private final NormalizedGaussianSampler normal;
        /** The exponential distribution (mean=1). */
        private final ContinuousSampler exp;

        /**
         * @param rng Source of randomness.
         */
        UnitBallSampler3D(UniformRandomProvider rng) {
            normal = ZigguratSampler.NormalizedGaussian.of(rng);
            // Require an Exponential(mean=2).
            // Here we use mean = 1 and scale the output later.
            exp = ZigguratSampler.Exponential.of(rng);
        }

        @Override
        public double[] sample() {
            final double x = normal.sample();
            final double y = normal.sample();
            final double z = normal.sample();
            // Include the exponential sample. It has mean 1 so multiply by 2.
            final double sum = exp.sample() * 2 + x * x + y * y + z * z;
            // Note: Handle the possibility of a zero sum and invalid inverse
            if (sum == 0) {
                return sample();
            }
            final double f = 1.0 / Math.sqrt(sum);
            return new double[] {x * f, y * f, z * f};
        }

        @Override
        public UnitBallSampler withUniformRandomProvider(UniformRandomProvider rng) {
            return new UnitBallSampler3D(rng);
        }
    }

    /**
     * Sample using ball point picking.
     * @see <a href="https://mathworld.wolfram.com/BallPointPicking.html">Ball point picking</a>
     */
    private static class UnitBallSamplerND extends UnitBallSampler {
        /** The dimension. */
        private final int dimension;
        /** The standard normal distribution. */
        private final NormalizedGaussianSampler normal;
        /** The exponential distribution (mean=1). */
        private final ContinuousSampler exp;

        /**
         * @param rng Source of randomness.
         * @param dimension Space dimension.
         */
        UnitBallSamplerND(UniformRandomProvider rng, int dimension) {
            this.dimension  = dimension;
            normal = ZigguratSampler.NormalizedGaussian.of(rng);
            // Require an Exponential(mean=2).
            // Here we use mean = 1 and scale the output later.
            exp = ZigguratSampler.Exponential.of(rng);
        }

        @Override
        public double[] sample() {
            final double[] sample = new double[dimension];
            // Include the exponential sample. It has mean 1 so multiply by 2.
            double sum = exp.sample() * 2;
            for (int i = 0; i < dimension; i++) {
                final double x = normal.sample();
                sum += x * x;
                sample[i] = x;
            }
            // Note: Handle the possibility of a zero sum and invalid inverse
            if (sum == 0) {
                return sample();
            }
            final double f = 1.0 / Math.sqrt(sum);
            for (int i = 0; i < dimension; i++) {
                sample[i] *= f;
            }
            return sample;
        }

        @Override
        public UnitBallSampler withUniformRandomProvider(UniformRandomProvider rng) {
            return new UnitBallSamplerND(rng, dimension);
        }
    }

    /**
     * @return a random Cartesian coordinate within the unit n-ball.
     */
    @Override
    public abstract double[] sample();

    /** {@inheritDoc} */
    // Redeclare the signature to return a UnitBallSampler not a SharedStateObjectSampler<double[]>
    @Override
    public abstract UnitBallSampler withUniformRandomProvider(UniformRandomProvider rng);

    /**
     * Create a unit n-ball sampler for the given dimension.
     * Sampled points are uniformly distributed within the unit n-ball.
     *
     * <p>Sampling is supported in dimensions of 1 or above.
     *
     * @param rng Source of randomness.
     * @param dimension Space dimension.
     * @return the sampler
     * @throws IllegalArgumentException If {@code dimension <= 0}
     */
    public static UnitBallSampler of(UniformRandomProvider rng,
                                     int dimension) {
        if (dimension <= 0) {
            throw new IllegalArgumentException("Dimension must be strictly positive");
        } else if (dimension == ONE_D) {
            return new UnitBallSampler1D(rng);
        } else if (dimension == TWO_D) {
            return new UnitBallSampler2D(rng);
        } else if (dimension == THREE_D) {
            return new UnitBallSampler3D(rng);
        }
        return new UnitBallSamplerND(rng, dimension);
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
