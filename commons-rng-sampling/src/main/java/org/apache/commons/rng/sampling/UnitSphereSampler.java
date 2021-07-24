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

package org.apache.commons.rng.sampling;

import org.apache.commons.rng.UniformRandomProvider;
import org.apache.commons.rng.sampling.distribution.NormalizedGaussianSampler;
import org.apache.commons.rng.sampling.distribution.ZigguratSampler;

/**
 * Generate vectors <a href="http://mathworld.wolfram.com/SpherePointPicking.html">
 * isotropically located on the surface of a sphere</a>.
 *
 * <p>Sampling in 2 or more dimensions uses:</p>
 *
 * <ul>
 *   <li>{@link UniformRandomProvider#nextLong()}
 *   <li>{@link UniformRandomProvider#nextDouble()}
 * </ul>
 *
 * <p>Sampling in 1D uses the sign bit from {@link UniformRandomProvider#nextInt()} to set
 * the direction of the vector.
 *
 * @since 1.1
 */
public class UnitSphereSampler implements SharedStateObjectSampler<double[]> {
    /** The dimension for 1D sampling. */
    private static final int ONE_D = 1;
    /** The dimension for 2D sampling. */
    private static final int TWO_D = 2;
    /** The dimension for 3D sampling. */
    private static final int THREE_D = 3;
    /**
     * The mask to extract the second bit from an integer
     * (naming starts at bit 1 for the least significant bit).
     * The masked integer will have a value 0 or 2.
     */
    private static final int MASK_BIT_2 = 0x2;

    /** The internal sampler optimised for the dimension. */
    private final UnitSphereSampler delegate;

    /**
     * Sample uniformly from the ends of a 1D unit line.
     */
    private static class UnitSphereSampler1D extends UnitSphereSampler {
        /** The source of randomness. */
        private final UniformRandomProvider rng;

        /**
         * @param rng Source of randomness.
         */
        UnitSphereSampler1D(UniformRandomProvider rng) {
            this.rng = rng;
        }

        @Override
        public double[] sample() {
            // Either:
            // 1 - 0 = 1
            // 1 - 2 = -1
            // Use the sign bit
            return new double[] {1.0 - ((rng.nextInt() >>> 30) & MASK_BIT_2)};
        }

        @Override
        public UnitSphereSampler withUniformRandomProvider(UniformRandomProvider rng) {
            return new UnitSphereSampler1D(rng);
        }
    }

    /**
     * Sample uniformly from a 2D unit circle.
     * This is a 2D specialisation of the UnitSphereSamplerND.
     */
    private static class UnitSphereSampler2D extends UnitSphereSampler {
        /** Sampler used for generating the individual components of the vectors. */
        private final NormalizedGaussianSampler sampler;

        /**
         * @param rng Source of randomness.
         */
        UnitSphereSampler2D(UniformRandomProvider rng) {
            sampler = ZigguratSampler.NormalizedGaussian.of(rng);
        }

        @Override
        public double[] sample() {
            final double x = sampler.sample();
            final double y = sampler.sample();
            final double sum = x * x + y * y;

            if (sum == 0) {
                // Zero-norm vector is discarded.
                return sample();
            }

            final double f = 1.0 / Math.sqrt(sum);
            return new double[] {x * f, y * f};
        }

        @Override
        public UnitSphereSampler withUniformRandomProvider(UniformRandomProvider rng) {
            return new UnitSphereSampler2D(rng);
        }
    }

    /**
     * Sample uniformly from a 3D unit sphere.
     * This is a 3D specialisation of the UnitSphereSamplerND.
     */
    private static class UnitSphereSampler3D extends UnitSphereSampler {
        /** Sampler used for generating the individual components of the vectors. */
        private final NormalizedGaussianSampler sampler;

        /**
         * @param rng Source of randomness.
         */
        UnitSphereSampler3D(UniformRandomProvider rng) {
            sampler = ZigguratSampler.NormalizedGaussian.of(rng);
        }

        @Override
        public double[] sample() {
            final double x = sampler.sample();
            final double y = sampler.sample();
            final double z = sampler.sample();
            final double sum = x * x + y * y + z * z;

            if (sum == 0) {
                // Zero-norm vector is discarded.
                return sample();
            }

            final double f = 1.0 / Math.sqrt(sum);
            return new double[] {x * f, y * f, z * f};
        }

        @Override
        public UnitSphereSampler withUniformRandomProvider(UniformRandomProvider rng) {
            return new UnitSphereSampler3D(rng);
        }
    }

    /**
     * Sample uniformly from a ND unit sphere.
     */
    private static class UnitSphereSamplerND extends UnitSphereSampler {
        /** Space dimension. */
        private final int dimension;
        /** Sampler used for generating the individual components of the vectors. */
        private final NormalizedGaussianSampler sampler;

        /**
         * @param rng Source of randomness.
         * @param dimension Space dimension.
         */
        UnitSphereSamplerND(UniformRandomProvider rng, int dimension) {
            this.dimension = dimension;
            sampler = ZigguratSampler.NormalizedGaussian.of(rng);
        }

        @Override
        public double[] sample() {
            final double[] v = new double[dimension];

            // Pick a point by choosing a standard Gaussian for each element,
            // and then normalize to unit length.
            double sum = 0;
            for (int i = 0; i < dimension; i++) {
                final double x = sampler.sample();
                v[i] = x;
                sum += x * x;
            }

            if (sum == 0) {
                // Zero-norm vector is discarded.
                // Using recursion as it is highly unlikely to generate more
                // than a few such vectors. It also protects against infinite
                // loop (in case a buggy generator is used), by eventually
                // raising a "StackOverflowError".
                return sample();
            }

            final double f = 1 / Math.sqrt(sum);
            for (int i = 0; i < dimension; i++) {
                v[i] *= f;
            }

            return v;
        }

        @Override
        public UnitSphereSampler withUniformRandomProvider(UniformRandomProvider rng) {
            return new UnitSphereSamplerND(rng, dimension);
        }
    }

    /**
     * This instance delegates sampling. Use the factory method
     * {@link #of(UniformRandomProvider, int)} to create an optimal sampler.
     *
     * @param dimension Space dimension.
     * @param rng Generator for the individual components of the vectors.
     * A shallow copy will be stored in this instance.
     * @throws IllegalArgumentException If {@code dimension <= 0}
     * @deprecated Use {@link #of(UniformRandomProvider, int)}.
     */
    @Deprecated
    public UnitSphereSampler(int dimension,
                             UniformRandomProvider rng) {
        delegate = of(rng, dimension);
    }

    /**
     * Private constructor used by sub-class specialisations.
     * In future versions the public constructor should be removed and the class made abstract.
     */
    private UnitSphereSampler() {
        delegate = null;
    }

    /**
     * @return a random normalized Cartesian vector.
     * @since 1.4
     */
    @Override
    public double[] sample() {
        return delegate.sample();
    }

    /**
     * @return a random normalized Cartesian vector.
     * @deprecated Use {@link #sample()}.
     */
    @Deprecated
    public double[] nextVector() {
        return sample();
    }

    /**
     * {@inheritDoc}
     *
     * @since 1.3
     */
    @Override
    public UnitSphereSampler withUniformRandomProvider(UniformRandomProvider rng) {
        return delegate.withUniformRandomProvider(rng);
    }

    /**
     * Create a unit sphere sampler for the given dimension.
     *
     * @param rng Generator for the individual components of the vectors. A shallow
     * copy will be stored in the sampler.
     * @param dimension Space dimension.
     * @return the sampler
     * @throws IllegalArgumentException If {@code dimension <= 0}
     *
     * @since 1.4
     */
    public static UnitSphereSampler of(UniformRandomProvider rng,
                                       int dimension) {
        if (dimension <= 0) {
            throw new IllegalArgumentException("Dimension must be strictly positive");
        } else if (dimension == ONE_D) {
            return new UnitSphereSampler1D(rng);
        } else if (dimension == TWO_D) {
            return new UnitSphereSampler2D(rng);
        } else if (dimension == THREE_D) {
            return new UnitSphereSampler3D(rng);
        }
        return new UnitSphereSamplerND(rng, dimension);
    }
}
