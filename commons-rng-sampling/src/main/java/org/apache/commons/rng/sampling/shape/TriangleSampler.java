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
 * Generate points <a href="https://mathworld.wolfram.com/TrianglePointPicking.html">
 * uniformly distributed within a triangle</a>.
 *
 * <ul>
 *  <li>
 *   Uses the algorithm described in:
 *   <blockquote>
 *    Turk, G. <i>Generating random points in triangles</i>. Glassner, A. S. (ed) (1990).<br>
 *    <strong>Graphic Gems</strong> Academic Press, pp. 24-28.
 *   </blockquote>
 *  </li>
 * </ul>
 *
 * <p>Sampling uses:</p>
 *
 * <ul>
 *   <li>{@link UniformRandomProvider#nextDouble()}
 * </ul>
 *
 * @since 1.4
 */
public abstract class TriangleSampler implements SharedStateObjectSampler<double[]> {
    /** The dimension for 2D sampling. */
    private static final int TWO_D = 2;
    /** The dimension for 3D sampling. */
    private static final int THREE_D = 3;
    /** The source of randomness. */
    private final UniformRandomProvider rng;

    // The following code defines a plane as the set of all points r:
    // r = r0 + sv + tw
    // where s and t range over all real numbers, v and w are given linearly independent
    // vectors defining the plane, and r0 is an arbitrary (but fixed) point in the plane.
    //
    // Sampling from a triangle (a,b,c) is performed when:
    // s and t are in [0, 1] and s+t <= 1;
    // r0 is one triangle vertex (a);
    // and v (b-a) and w (c-a) are vectors from the other two vertices to r0.
    //
    // For robustness with large value coordinates the point r is computed without
    // the requirement to compute v and w which can overflow:
    //
    // a + s(b-a) + t(c-a) == a + sb - sa + tc - ta
    //                     == a(1 - s - t) + sb + tc
    //
    // Assuming the uniform deviates are from the 2^53 dyadic rationals in [0, 1) if s+t <= 1
    // then 1 - (s+t) is exact. Sampling is then done using:
    //
    // if (s + t <= 1):
    //    p = a(1 - (s + t)) + sb + tc
    // else:
    //    p = a(1 - (1 - s) - (1 - t)) + (1 - s)b + (1 - t)c
    //    p = a(s - 1 + t) + (1 - s)b + (1 - t)c
    //
    // Note do not simplify (1 - (1 - s) - (1 - t)) to (s + t - 1) as s+t > 1 and has potential
    // loss of a single bit of randomness due to rounding. An exact sum is s - 1 + t.

    /**
     * Sample uniformly from a triangle in 2D. This is an non-array based specialisation of
     * {@link TriangleSamplerND} for performance.
     */
    private static class TriangleSampler2D extends TriangleSampler {
        /** The x component of vertex a. */
        private final double ax;
        /** The y component of vertex a. */
        private final double ay;
        /** The x component of vertex b. */
        private final double bx;
        /** The y component of vertex b. */
        private final double by;
        /** The x component of vertex c. */
        private final double cx;
        /** The y component of vertex c. */
        private final double cy;

        /**
         * @param rng Source of randomness.
         * @param a The first vertex.
         * @param b The second vertex.
         * @param c The third vertex.
         */
        TriangleSampler2D(UniformRandomProvider rng, double[] a, double[] b, double[] c) {
            super(rng);
            ax = a[0];
            ay = a[1];
            bx = b[0];
            by = b[1];
            cx = c[0];
            cy = c[1];
        }

        /**
         * @param rng Generator of uniformly distributed random numbers
         * @param source Source to copy.
         */
        TriangleSampler2D(UniformRandomProvider rng, TriangleSampler2D source) {
            super(rng);
            ax = source.ax;
            ay = source.ay;
            bx = source.bx;
            by = source.by;
            cx = source.cx;
            cy = source.cy;
        }

        @Override
        public double[] createSample(double p1msmt, double s, double t) {
            return new double[] {p1msmt * ax + s * bx + t * cx,
                                 p1msmt * ay + s * by + t * cy};
        }

        @Override
        public TriangleSampler withUniformRandomProvider(UniformRandomProvider rng) {
            return new TriangleSampler2D(rng, this);
        }
    }

    /**
     * Sample uniformly from a triangle in 3D. This is an non-array based specialisation of
     * {@link TriangleSamplerND} for performance.
     */
    private static class TriangleSampler3D extends TriangleSampler {
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
        /** The x component of vertex c. */
        private final double cx;
        /** The y component of vertex c. */
        private final double cy;
        /** The z component of vertex c. */
        private final double cz;

        /**
         * @param rng Source of randomness.
         * @param a The first vertex.
         * @param b The second vertex.
         * @param c The third vertex.
         */
        TriangleSampler3D(UniformRandomProvider rng, double[] a, double[] b, double[] c) {
            super(rng);
            ax = a[0];
            ay = a[1];
            az = a[2];
            bx = b[0];
            by = b[1];
            bz = b[2];
            cx = c[0];
            cy = c[1];
            cz = c[2];
        }

        /**
         * @param rng Generator of uniformly distributed random numbers
         * @param source Source to copy.
         */
        TriangleSampler3D(UniformRandomProvider rng, TriangleSampler3D source) {
            super(rng);
            ax = source.ax;
            ay = source.ay;
            az = source.az;
            bx = source.bx;
            by = source.by;
            bz = source.bz;
            cx = source.cx;
            cy = source.cy;
            cz = source.cz;
        }

        @Override
        public double[] createSample(double p1msmt, double s, double t) {
            return new double[] {p1msmt * ax + s * bx + t * cx,
                                 p1msmt * ay + s * by + t * cy,
                                 p1msmt * az + s * bz + t * cz};
        }

        @Override
        public TriangleSampler withUniformRandomProvider(UniformRandomProvider rng) {
            return new TriangleSampler3D(rng, this);
        }
    }

    /**
     * Sample uniformly from a triangle in ND.
     */
    private static class TriangleSamplerND extends TriangleSampler {
        /** The first vertex. */
        private final double[] a;
        /** The second vertex. */
        private final double[] b;
        /** The third vertex. */
        private final double[] c;

        /**
         * @param rng Source of randomness.
         * @param a The first vertex.
         * @param b The second vertex.
         * @param c The third vertex.
         */
        TriangleSamplerND(UniformRandomProvider rng, double[] a, double[] b, double[] c) {
            super(rng);
            // Defensive copy
            this.a = a.clone();
            this.b = b.clone();
            this.c = c.clone();
        }

        /**
         * @param rng Generator of uniformly distributed random numbers
         * @param source Source to copy.
         */
        TriangleSamplerND(UniformRandomProvider rng, TriangleSamplerND source) {
            super(rng);
            // Shared state is immutable
            a = source.a;
            b = source.b;
            c = source.c;
        }

        @Override
        public double[] createSample(double p1msmt, double s, double t) {
            final double[] x = new double[a.length];
            for (int i = 0; i < x.length; i++) {
                x[i] = p1msmt * a[i] + s * b[i] + t * c[i];
            }
            return x;
        }

        @Override
        public TriangleSampler withUniformRandomProvider(UniformRandomProvider rng) {
            return new TriangleSamplerND(rng, this);
        }
    }

    /**
     * @param rng Source of randomness.
     */
    TriangleSampler(UniformRandomProvider rng) {
        this.rng = rng;
    }

    /**
     * @return a random Cartesian coordinate within the triangle.
     */
    @Override
    public double[] sample() {
        final double s = rng.nextDouble();
        final double t = rng.nextDouble();
        final double spt = s + t;
        if (spt > 1) {
            // Transform: s1 = 1 - s; t1 = 1 - t.
            // Compute: 1 - s1 - t1
            // Do not assume (1 - (1-s) - (1-t)) is (s + t - 1), i.e. (spt - 1.0),
            // to avoid loss of a random bit due to rounding when s + t > 1.
            // An exact sum is (s - 1 + t).
            return createSample(s - 1.0 + t, 1.0 - s, 1.0 - t);
        }
        // Here s + t is exact so can be subtracted to make 1 - s - t
        return createSample(1.0 - spt, s, t);
    }

    /**
     * Creates the sample given the random variates {@code s} and {@code t} in the
     * interval {@code [0, 1]} and {@code s + t <= 1}. The sum {@code 1 - s - t} is provided.
     * The sample can be obtained from the triangle abc using:
     * <pre>
     * p = a(1 - s - t) + sb + tc
     * </pre>
     *
     * @param p1msmt plus 1 minus s minus t (1 - s - t)
     * @param s the first variate s
     * @param t the second variate t
     * @return the sample
     */
    protected abstract double[] createSample(double p1msmt, double s, double t);

    /** {@inheritDoc} */
    // Redeclare the signature to return a TriangleSampler not a SharedStateObjectSampler<double[]>
    @Override
    public abstract TriangleSampler withUniformRandomProvider(UniformRandomProvider rng);

    /**
     * Create a triangle sampler with vertices {@code a}, {@code b} and {@code c}.
     * Sampled points are uniformly distributed within the triangle.
     *
     * <p>Sampling is supported in dimensions of 2 or above. Samples will lie in the
     * plane (2D Euclidean space) defined by using the three triangle vertices to
     * create two vectors starting at a point in the plane and orientated in
     * different directions along the plane.
     *
     * <p>No test for collinear points is performed. If the points are collinear the sampling
     * distribution is undefined.
     *
     * @param rng Source of randomness.
     * @param a The first vertex.
     * @param b The second vertex.
     * @param c The third vertex.
     * @return the sampler
     * @throws IllegalArgumentException If the vertices do not have the same
     * dimension; the dimension is less than 2; or vertices have non-finite coordinates
     */
    public static TriangleSampler of(UniformRandomProvider rng,
                                     double[] a,
                                     double[] b,
                                     double[] c) {
        final int dimension = a.length;
        if (dimension != b.length || dimension != c.length) {
            throw new IllegalArgumentException(
                    new StringBuilder("Mismatch of vertex dimensions: ").append(dimension).append(',')
                                                                        .append(b.length).append(',')
                                                                        .append(c.length).toString());
        }
        // Detect non-finite vertices
        Coordinates.requireFinite(a, "Vertex a");
        Coordinates.requireFinite(b, "Vertex b");
        Coordinates.requireFinite(c, "Vertex c");
        // Low dimension specialisations
        if (dimension == TWO_D) {
            return new TriangleSampler2D(rng, a, b, c);
        } else if (dimension == THREE_D) {
            return new TriangleSampler3D(rng, a, b, c);
        } else if (dimension > THREE_D) {
            return new TriangleSamplerND(rng, a, b, c);
        }
        // Less than 2D
        throw new IllegalArgumentException(
                "Unsupported dimension: " + dimension);
    }
}
