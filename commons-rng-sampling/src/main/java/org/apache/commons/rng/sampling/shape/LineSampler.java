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

/**
 * Generate points uniformly distributed on a line.
 *
 * <p>Sampling uses:</p>
 *
 * <ul>
 *   <li>{@link UniformRandomProvider#nextDouble()}
 * </ul>
 *
 * @since 1.4
 */
public abstract class LineSampler implements SharedStateSampler<LineSampler> {
    /** The dimension for 1D sampling. */
    private static final int ONE_D = 1;
    /** The dimension for 2D sampling. */
    private static final int TWO_D = 2;
    /** The dimension for 3D sampling. */
    private static final int THREE_D = 3;
    /** The source of randomness. */
    private final UniformRandomProvider rng;

    // The following code defines a point on a line as:
    // p = a + u * (b - a), u in [0, 1]
    //
    // This is rearranged to:
    // p = a + ub - ua
    //   = (1 - u)a + ub
    //
    // This is the same method used in the
    // o.a.c.rng.sampling.distribution.ContinuousUniformSampler but extended to N-dimensions.

    /**
     * Sample uniformly from a line in 1D. This is an non-array based specialisation of
     * {@link LineSamplerND} for performance.
     */
    private static class LineSampler1D extends LineSampler {
        /** The x component of vertex a. */
        private final double ax;
        /** The x component of vertex b. */
        private final double bx;

        /**
         * @param a The first vertex.
         * @param b The second vertex.
         * @param rng Source of randomness.
         */
        LineSampler1D(double[] a, double[] b, UniformRandomProvider rng) {
            super(rng);
            ax = a[0];
            bx = b[0];
        }

        /**
         * @param rng Source of randomness.
         * @param source Source to copy.
         */
        LineSampler1D(UniformRandomProvider rng, LineSampler1D source) {
            super(rng);
            ax = source.ax;
            bx = source.bx;
        }

        @Override
        public double[] createSample(double p1mu, double u) {
            return new double[] {p1mu * ax + u * bx};
        }

        @Override
        public LineSampler withUniformRandomProvider(UniformRandomProvider rng) {
            return new LineSampler1D(rng, this);
        }
    }

    /**
     * Sample uniformly from a line in 2D. This is an non-array based specialisation of
     * {@link LineSamplerND} for performance.
     */
    private static class LineSampler2D extends LineSampler {
        /** The x component of vertex a. */
        private final double ax;
        /** The y component of vertex a. */
        private final double ay;
        /** The x component of vertex b. */
        private final double bx;
        /** The y component of vertex b. */
        private final double by;

        /**
         * @param a The first vertex.
         * @param b The second vertex.
         * @param rng Source of randomness.
         */
        LineSampler2D(double[] a, double[] b, UniformRandomProvider rng) {
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
        LineSampler2D(UniformRandomProvider rng, LineSampler2D source) {
            super(rng);
            ax = source.ax;
            ay = source.ay;
            bx = source.bx;
            by = source.by;
        }

        @Override
        public double[] createSample(double p1mu, double u) {
            return new double[] {p1mu * ax + u * bx,
                                 p1mu * ay + u * by};
        }

        @Override
        public LineSampler withUniformRandomProvider(UniformRandomProvider rng) {
            return new LineSampler2D(rng, this);
        }
    }

    /**
     * Sample uniformly from a line in 3D. This is an non-array based specialisation of
     * {@link LineSamplerND} for performance.
     */
    private static class LineSampler3D extends LineSampler {
        /** The x component of vertex a. */
        private final double ax;
        /** The y component of vertex a. */
        private final double ay;
        /** The z component of vertex a. */
        private final double az;
        /** The x component of vertex b. */
        private final double bx;
        /** The y component of vertex b. */
        private final double by;
        /** The z component of vertex b. */
        private final double bz;

        /**
         * @param a The first vertex.
         * @param b The second vertex.
         * @param rng Source of randomness.
         */
        LineSampler3D(double[] a, double[] b, UniformRandomProvider rng) {
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
        LineSampler3D(UniformRandomProvider rng, LineSampler3D source) {
            super(rng);
            ax = source.ax;
            ay = source.ay;
            az = source.az;
            bx = source.bx;
            by = source.by;
            bz = source.bz;
        }

        @Override
        public double[] createSample(double p1mu, double u) {
            return new double[] {p1mu * ax + u * bx,
                                 p1mu * ay + u * by,
                                 p1mu * az + u * bz};
        }

        @Override
        public LineSampler withUniformRandomProvider(UniformRandomProvider rng) {
            return new LineSampler3D(rng, this);
        }
    }

    /**
     * Sample uniformly from a line in ND.
     */
    private static class LineSamplerND extends LineSampler {
        /** The first vertex. */
        private final double[] a;
        /** The second vertex. */
        private final double[] b;

        /**
         * @param a The first vertex.
         * @param b The second vertex.
         * @param rng Source of randomness.
         */
        LineSamplerND(double[] a, double[] b, UniformRandomProvider rng) {
            super(rng);
            // Defensive copy
            this.a = a.clone();
            this.b = b.clone();
        }

        /**
         * @param rng Source of randomness.
         * @param source Source to copy.
         */
        LineSamplerND(UniformRandomProvider rng, LineSamplerND source) {
            super(rng);
            // Shared state is immutable
            a = source.a;
            b = source.b;
        }

        @Override
        public double[] createSample(double p1mu, double u) {
            final double[] x = new double[a.length];
            for (int i = 0; i < x.length; i++) {
                x[i] = p1mu * a[i] + u * b[i];
            }
            return x;
        }

        @Override
        public LineSampler withUniformRandomProvider(UniformRandomProvider rng) {
            return new LineSamplerND(rng, this);
        }
    }

    /**
     * @param rng Source of randomness.
     */
    LineSampler(UniformRandomProvider rng) {
        this.rng = rng;
    }

    /**
     * @return a random Cartesian coordinate on the line.
     */
    public double[] sample() {
        final double u = rng.nextDouble();
        return createSample(1.0 - u, u);
    }

    /**
     * Creates the sample given the random variate {@code u} in the
     * interval {@code [0, 1]}. The sum {@code 1 - u} is provided.
     * The sample can be obtained from the line ab using:
     * <pre>
     * p = a(1 - u) + ub
     * </pre>
     *
     * @param p1mu plus 1 minus u (1 - u)
     * @param u the variate u
     * @return the sample
     */
    protected abstract double[] createSample(double p1mu, double u);

    /**
     * Create a line sampler with vertices {@code a} and {@code b}.
     * Sampled points are uniformly distributed on the line segment {@code ab}.
     *
     * <p>Sampling is supported in dimensions of 1 or above.
     *
     * @param a The first vertex.
     * @param b The second vertex.
     * @param rng Source of randomness.
     * @return the sampler
     * @throws IllegalArgumentException If the vertices do not have the same
     * dimension; the dimension is less than 1; or vertices have non-finite coordinates.
     */
    public static LineSampler of(double[] a,
                                 double[] b,
                                 UniformRandomProvider rng) {
        final int dimension = a.length;
        if (dimension != b.length) {
            throw new IllegalArgumentException(
                new StringBuilder("Mismatch of vertex dimensions: ").append(dimension).append(',')
                                                                    .append(b.length).toString());
        }
        // Detect non-finite vertices
        Coordinates.requireFinite(a, "Vertex a");
        Coordinates.requireFinite(b, "Vertex b");
        // Low dimension specialisations
        if (dimension == TWO_D) {
            return new LineSampler2D(a, b, rng);
        } else if (dimension == THREE_D) {
            return new LineSampler3D(a, b, rng);
        } else if (dimension > THREE_D) {
            return new LineSamplerND(a, b, rng);
        } else if (dimension == ONE_D) {
            // Unlikely case of 1D is placed last.
            // Use o.a.c.rng.sampling.distribution.ContinuousUniformSampler for non-array samples.
            return new LineSampler1D(a, b, rng);
        }
        // Less than 1D
        throw new IllegalArgumentException("Unsupported dimension: " + dimension);
    }
}
