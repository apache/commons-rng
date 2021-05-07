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
import org.apache.commons.rng.sampling.SharedStateSampler;
import org.apache.commons.rng.sampling.distribution.NormalizedGaussianSampler;
import org.apache.commons.rng.sampling.distribution.ZigguratNormalizedGaussianSampler;

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
public abstract class UnitBallSampler implements SharedStateSampler<UnitBallSampler> {
    /** The dimension for 1D sampling. */
    private static final int ONE_D = 1;
    /** The dimension for 2D sampling. */
    private static final int TWO_D = 2;
    /** The dimension for 3D sampling. */
    private static final int THREE_D = 3;
    /** The mask to extract the lower 53-bits from a long. */
    private static final long LOWER_53_BITS = -1L >>> 11;

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
        /** The normal distribution. */
        private final NormalizedGaussianSampler normal;

        /**
         * @param rng Source of randomness.
         */
        UnitBallSampler3D(UniformRandomProvider rng) {
            normal = new ZigguratNormalizedGaussianSampler(rng);
        }

        @Override
        public double[] sample() {
            // Discard 2 samples from the coordinate but include in the sum
            final double x0 = normal.sample();
            final double x1 = normal.sample();
            final double x = normal.sample();
            final double y = normal.sample();
            final double z = normal.sample();
            final double sum = x0 * x0 + x1 * x1 + x * x + y * y + z * z;
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
     * Sample uniformly from a unit n-ball.
     * Take a random point on the (n+1)-dimensional hypersphere and drop two coordinates.
     * Remember that the (n+1)-hypersphere is the unit sphere of R^(n+2), i.e. the surface
     * of the (n+2)-dimensional ball.
     * @see <a href="https://mathoverflow.net/questions/309567/sampling-a-uniformly-distributed-point-inside-a-hypersphere">
     * Sampling a uniformly distributed point INSIDE a hypersphere?</a>
     */
    private static class UnitBallSamplerND extends UnitBallSampler {
        /** The dimension. */
        private final int dimension;
        /** The normal distribution. */
        private final NormalizedGaussianSampler normal;

        /**
         * @param dimension Space dimension.
         * @param rng Source of randomness.
         */
        UnitBallSamplerND(int dimension, UniformRandomProvider rng) {
            this.dimension  = dimension;
            normal = new ZigguratNormalizedGaussianSampler(rng);
        }

        @Override
        public double[] sample() {
            final double[] sample = new double[dimension];
            // Discard 2 samples from the coordinate but include in the sum
            final double x0 = normal.sample();
            final double x1 = normal.sample();
            double sum = x0 * x0 + x1 * x1;
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
            return new UnitBallSamplerND(dimension, rng);
        }
    }

    /**
     * @return a random Cartesian coordinate within the unit n-ball.
     */
    public abstract double[] sample();

    /**
     * Create a unit n-ball sampler for the given dimension.
     * Sampled points are uniformly distributed within the unit n-ball.
     *
     * <p>Sampling is supported in dimensions of 1 or above.
     *
     * @param dimension Space dimension.
     * @param rng Source of randomness.
     * @return the sampler
     * @throws IllegalArgumentException If {@code dimension <= 0}
     */
    public static UnitBallSampler of(int dimension,
                                     UniformRandomProvider rng) {
        if (dimension <= 0) {
            throw new IllegalArgumentException("Dimension must be strictly positive");
        } else if (dimension == ONE_D) {
            return new UnitBallSampler1D(rng);
        } else if (dimension == TWO_D) {
            return new UnitBallSampler2D(rng);
        } else if (dimension == THREE_D) {
            return new UnitBallSampler3D(rng);
        }
        return new UnitBallSamplerND(dimension, rng);
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
        // Use the upper 54 bits on the assumption they are more random.
        // The sign bit generates a value of 0 or 1 for subtraction.
        // The next 53 bits generates a positive number in the range [0, 1).
        // [0, 1) - (0 or 1) => [-1, 1)
        return (((bits >>> 10) & LOWER_53_BITS) * 0x1.0p-53d) - (bits >>> 63);
    }
}
