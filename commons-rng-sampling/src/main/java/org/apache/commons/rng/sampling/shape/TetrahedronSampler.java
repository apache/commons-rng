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
 * Generate points uniformly distributed within a
 * <a href="https://en.wikipedia.org/wiki/Tetrahedron">tetrahedron</a>.
 *
 * <ul>
 *  <li>
 *   Uses the algorithm described in:
 *   <blockquote>
 *    Rocchini, C. and Cignoni, P. (2001)<br>
 *    <i>Generating Random Points in a Tetrahedron</i>.<br>
 *    <strong>Journal of Graphics Tools</strong> 5(4), pp. 9-12.
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
 * @see <a href="https://doi.org/10.1080/10867651.2000.10487528">
 *   Rocchini, C. &amp; Cignoni, P. (2001) Journal of Graphics Tools 5, pp. 9-12</a>
 * @since 1.4
 */
public class TetrahedronSampler implements SharedStateObjectSampler<double[]> {
    /** The dimension for 3D sampling. */
    private static final int THREE_D = 3;
    /** The name of vertex a. */
    private static final String VERTEX_A = "Vertex a";
    /** The name of vertex b. */
    private static final String VERTEX_B = "Vertex b";
    /** The name of vertex c. */
    private static final String VERTEX_C = "Vertex c";
    /** The name of vertex d. */
    private static final String VERTEX_D = "Vertex d";

    /** The first vertex. */
    private final double[] a;
    /** The second vertex. */
    private final double[] b;
    /** The third vertex. */
    private final double[] c;
    /** The fourth vertex. */
    private final double[] d;
    /** The source of randomness. */
    private final UniformRandomProvider rng;

    /**
     * @param rng Source of randomness.
     * @param a The first vertex.
     * @param b The second vertex.
     * @param c The third vertex.
     * @param d The fourth vertex.
     */
    TetrahedronSampler(UniformRandomProvider rng, double[] a, double[] b, double[] c, double[] d) {
        // Defensive copy
        this.a = a.clone();
        this.b = b.clone();
        this.c = c.clone();
        this.d = d.clone();
        this.rng = rng;
    }

    /**
     * @param rng Generator of uniformly distributed random numbers
     * @param source Source to copy.
     */
    TetrahedronSampler(UniformRandomProvider rng, TetrahedronSampler source) {
        // Shared state is immutable
        a = source.a;
        b = source.b;
        c = source.c;
        d = source.d;
        this.rng = rng;
    }

    /**
     * @return a random Cartesian point within the tetrahedron.
     */
    @Override
    public double[] sample() {
        double s = rng.nextDouble();
        double t = rng.nextDouble();
        final double u = rng.nextDouble();
        // Care is taken to ensure the 3 deviates remain in the 2^53 dyadic rationals in [0, 1).
        // The following are exact for all the 2^53 dyadic rationals:
        // 1 - u; u in [0, 1]
        // u - 1; u in [0, 1]
        // u + 1; u in [-1, 0]
        // u + v; u in [-1, 0], v in [0, 1]
        // u + v; u, v in [0, 1], u + v <= 1

        // Cut and fold with the plane s + t = 1
        if (s + t > 1) {
            // (s, t, u) = (1 - s, 1 - t, u)                if s + t > 1
            s = 1 - s;
            t = 1 - t;
        }
        // Now s + t <= 1.
        // Cut and fold with the planes t + u = 1 and s + t + u = 1.
        final double tpu = t + u;
        final double sptpu = s + tpu;
        if (sptpu > 1) {
            if (tpu > 1) {
                // (s, t, u) = (s, 1 - u, 1 - s - t)        if t + u > 1
                // 1 - s - (1-u) - (1-s-t) == u - 1 + t
                return createSample(u - 1 + t, s, 1 - u, 1 - s - t);
            }
            // (s, t, u) = (1 - t - u, t, s + t + u - 1)    if t + u <= 1
            // 1 - (1-t-u) - t - (s+t+u-1) == 1 - s - t
            return createSample(1 - s - t, 1 - tpu, t, s - 1 + tpu);
        }
        return createSample(1 - sptpu, s, t, u);
    }

    /**
     * Creates the sample given the random variates {@code s}, {@code t} and {@code u} in the
     * interval {@code [0, 1]} and {@code s + t + u <= 1}. The sum {@code 1 - s - t - u} is
     * provided. The sample can be obtained from the tetrahedron {@code abcd} using:
     *
     * <pre>
     * p = (1 - s - t - u)a + sb + tc + ud
     * </pre>
     *
     * @param p1msmtmu plus 1 minus s minus t minus u ({@code 1 - s - t - u})
     * @param s the first variate s
     * @param t the second variate t
     * @param u the third variate u
     * @return the sample
     */
    private double[] createSample(double p1msmtmu, double s, double t, double u) {
        // From the barycentric coordinates s,t,u create the point by moving along
        // vectors ab, ac and ad.
        // Here we do not compute the vectors and use the original vertices:
        // p = a + s(b-a) + t(c-a) + u(d-a)
        //   = (1-s-t-u)a + sb + tc + ud
        return new double[] {p1msmtmu * a[0] + s * b[0] + t * c[0] + u * d[0],
                             p1msmtmu * a[1] + s * b[1] + t * c[1] + u * d[1],
                             p1msmtmu * a[2] + s * b[2] + t * c[2] + u * d[2]};
    }

    /** {@inheritDoc} */
    @Override
    public TetrahedronSampler withUniformRandomProvider(UniformRandomProvider rng) {
        return new TetrahedronSampler(rng, this);
    }

    /**
     * Create a tetrahedron sampler with vertices {@code a}, {@code b}, {@code c} and {@code d}.
     * Sampled points are uniformly distributed within the tetrahedron.
     *
     * <p>No test for a volume is performed. If the vertices are coplanar the sampling
     * distribution is undefined.
     *
     * @param rng Source of randomness.
     * @param a The first vertex.
     * @param b The second vertex.
     * @param c The third vertex.
     * @param d The fourth vertex.
     * @return the sampler
     * @throws IllegalArgumentException If the vertices do not have length 3;
     * or vertices have non-finite coordinates
     */
    public static TetrahedronSampler of(UniformRandomProvider rng,
                                        double[] a,
                                        double[] b,
                                        double[] c,
                                        double[] d) {
        // Must be 3D
        Coordinates.requireLength(a, THREE_D, VERTEX_A);
        Coordinates.requireLength(b, THREE_D, VERTEX_B);
        Coordinates.requireLength(c, THREE_D, VERTEX_C);
        Coordinates.requireLength(d, THREE_D, VERTEX_D);
        // Detect non-finite vertices
        Coordinates.requireFinite(a, VERTEX_A);
        Coordinates.requireFinite(b, VERTEX_B);
        Coordinates.requireFinite(c, VERTEX_C);
        Coordinates.requireFinite(d, VERTEX_D);
        return new TetrahedronSampler(rng, a, b, c, d);
    }
}
