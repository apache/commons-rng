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

/**
 * Generate points uniformly distributed within a n-dimension box (hyperrectangle).
 *
 * <p>Sampling uses:</p>
 *
 * <ul>
 *   <li>{@link UniformRandomProvider#nextDouble()}
 * </ul>
 *
 * @see <a href="https://en.wikipedia.org/wiki/Hyperrectangle">Hyperrectangle (Wikipedia)</a>
 * @since 1.4
 */
public abstract class BoxSampler implements SharedStateObjectSampler<double[]> {
    /** The dimension for 2D sampling. */
    private static final int TWO_D = 2;
    /** The dimension for 3D sampling. */
    private static final int THREE_D = 3;
    /** The source of randomness. */
    private final UniformRandomProvider rng;

    // The following code defines a point within the range ab:
    // p = (1 - u)a + ub, u in [0, 1]
    //
    // This is the same method used in the
    // o.a.c.rng.sampling.distribution.ContinuousUniformSampler but extended to N-dimensions.

    /**
     * Sample uniformly from a box in 2D. This is an non-array based specialisation of
     * {@link BoxSamplerND} for performance.
     */
    private static class BoxSampler2D extends BoxSampler {
        /** The x component of bound a. */
        private final double ax;
        /** The y component of bound a. */
        private final double ay;
        /** The x component of bound b. */
        private final double bx;
        /** The y component of bound b. */
        private final double by;

        /**
         * @param rng Source of randomness.
         * @param a Bound a.
         * @param b Bound b.
         */
        BoxSampler2D(UniformRandomProvider rng, double[] a, double[] b) {
            super(rng);
            ax = a[0];
            ay = a[1];
            bx = b[0];
            by = b[1];
        }

        /**
         * @param rng Source of randomness.
         * @param source Source to copy.
         */
        BoxSampler2D(UniformRandomProvider rng, BoxSampler2D source) {
            super(rng);
            ax = source.ax;
            ay = source.ay;
            bx = source.bx;
            by = source.by;
        }

        @Override
        public double[] sample() {
            return new double[] {createSample(ax, bx),
                                 createSample(ay, by)};
        }

        @Override
        public BoxSampler withUniformRandomProvider(UniformRandomProvider rng) {
            return new BoxSampler2D(rng, this);
        }
    }

    /**
     * Sample uniformly from a box in 3D. This is an non-array based specialisation of
     * {@link BoxSamplerND} for performance.
     */
    private static class BoxSampler3D extends BoxSampler {
        /** The x component of bound a. */
        private final double ax;
        /** The y component of bound a. */
        private final double ay;
        /** The z component of bound a. */
        private final double az;
        /** The x component of bound b. */
        private final double bx;
        /** The y component of bound b. */
        private final double by;
        /** The z component of bound b. */
        private final double bz;

        /**
         * @param rng Source of randomness.
         * @param a Bound a.
         * @param b Bound b.
         */
        BoxSampler3D(UniformRandomProvider rng, double[] a, double[] b) {
            super(rng);
            ax = a[0];
            ay = a[1];
            az = a[2];
            bx = b[0];
            by = b[1];
            bz = b[2];
        }

        /**
         * @param rng Source of randomness.
         * @param source Source to copy.
         */
        BoxSampler3D(UniformRandomProvider rng, BoxSampler3D source) {
            super(rng);
            ax = source.ax;
            ay = source.ay;
            az = source.az;
            bx = source.bx;
            by = source.by;
            bz = source.bz;
        }

        @Override
        public double[] sample() {
            return new double[] {createSample(ax, bx),
                                 createSample(ay, by),
                                 createSample(az, bz)};
        }

        @Override
        public BoxSampler withUniformRandomProvider(UniformRandomProvider rng) {
            return new BoxSampler3D(rng, this);
        }
    }

    /**
     * Sample uniformly from a box in ND.
     */
    private static class BoxSamplerND extends BoxSampler {
        /** Bound a. */
        private final double[] a;
        /** Bound b. */
        private final double[] b;

        /**
         * @param rng Source of randomness.
         * @param a Bound a.
         * @param b Bound b.
         */
        BoxSamplerND(UniformRandomProvider rng, double[] a, double[] b) {
            super(rng);
            // Defensive copy
            this.a = a.clone();
            this.b = b.clone();
        }

        /**
         * @param rng Source of randomness.
         * @param source Source to copy.
         */
        BoxSamplerND(UniformRandomProvider rng, BoxSamplerND source) {
            super(rng);
            // Shared state is immutable
            a = source.a;
            b = source.b;
        }

        @Override
        public double[] sample() {
            final double[] x = new double[a.length];
            for (int i = 0; i < x.length; i++) {
                x[i] = createSample(a[i], b[i]);
            }
            return x;
        }

        @Override
        public BoxSampler withUniformRandomProvider(UniformRandomProvider rng) {
            return new BoxSamplerND(rng, this);
        }
    }

    /**
     * @param rng Source of randomness.
     */
    BoxSampler(UniformRandomProvider rng) {
        this.rng = rng;
    }

    /**
     * @return a random Cartesian coordinate within the box.
     */
    @Override
    public abstract double[] sample();

    /**
     * Creates the sample between bound a and b.
     *
     * @param a Bound a
     * @param b Bound b
     * @return the sample
     */
    double createSample(double a, double b) {
        final double u = rng.nextDouble();
        return (1.0 - u) * a + u * b;
    }

    /** {@inheritDoc} */
    // Redeclare the signature to return a BoxSampler not a SharedStateObjectSampler<double[]>
    @Override
    public abstract BoxSampler withUniformRandomProvider(UniformRandomProvider rng);

    /**
     * Create a box sampler with bounds {@code a} and {@code b}.
     * Sampled points are uniformly distributed within the box defined by the bounds.
     *
     * <p>Sampling is supported in dimensions of 2 or above. Single dimension sampling
     * can be performed using a {@link LineSampler}.
     *
     * <p>Note: There is no requirement that {@code a <= b}. The samples will be uniformly
     * distributed in the range {@code a} to {@code b} for each dimension.
     *
     * @param rng Source of randomness.
     * @param a Bound a.
     * @param b Bound b.
     * @return the sampler
     * @throws IllegalArgumentException If the bounds do not have the same
     * dimension; the dimension is less than 2; or bounds have non-finite coordinates.
     */
    public static BoxSampler of(UniformRandomProvider rng,
                                double[] a,
                                double[] b) {
        final int dimension = a.length;
        if (dimension != b.length) {
            throw new IllegalArgumentException(
                new StringBuilder("Mismatch of box dimensions: ").append(dimension).append(',')
                                                                 .append(b.length).toString());
        }
        // Detect non-finite bounds
        Coordinates.requireFinite(a, "Bound a");
        Coordinates.requireFinite(b, "Bound b");
        // Low dimension specialisations
        if (dimension == TWO_D) {
            return new BoxSampler2D(rng, a, b);
        } else if (dimension == THREE_D) {
            return new BoxSampler3D(rng, a, b);
        } else if (dimension > THREE_D) {
            return new BoxSamplerND(rng, a, b);
        }
        // Less than 2D
        throw new IllegalArgumentException("Unsupported dimension: " + dimension);
    }
}
