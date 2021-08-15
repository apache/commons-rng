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

import java.util.Arrays;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import org.apache.commons.math3.stat.inference.ChiSquareTest;

import org.apache.commons.rng.UniformRandomProvider;
import org.apache.commons.rng.sampling.RandomAssert;
import org.apache.commons.rng.simple.RandomSource;

/**
 * Test for {@link TetrahedronSampler}.
 */
public class TetrahedronSamplerTest {
    /**
     * Test invalid vertex dimensions (i.e. not 3D coordinates).
     */
    @Test
    public void testInvalidDimensionThrows() {
        final UniformRandomProvider rng = RandomSource.SPLIT_MIX_64.create(0L);
        final double[] ok = new double[3];
        final double[] bad = new double[2];
        final double[][] c = {ok, ok, ok, ok};
        for (int i = 0; i < c.length; i++) {
            c[i] = bad;
            try {
                TetrahedronSampler.of(rng, c[0], c[1], c[2], c[3]);
                Assertions.fail(String.format("Did not detect invalid dimension for vertex: %d", i));
            } catch (IllegalArgumentException ex) {
                // Expected
            }
            c[i] = ok;
        }
    }

    /**
     * Test non-finite vertices.
     */
    @Test
    public void testNonFiniteVertexCoordinates() {
        final UniformRandomProvider rng = RandomSource.SPLIT_MIX_64.create(0L);
        // A valid tetrahedron
        final double[][] c = new double[][] {
            {1, 1, 1}, {1, -1, 1}, {-1, 1, 1}, {1, 1, -1}
        };
        Assertions.assertNotNull(TetrahedronSampler.of(rng, c[0], c[1], c[2], c[3]));
        final double[] bad = {Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NaN};
        for (int i = 0; i < c.length; i++) {
            for (int j = 0; j < c[0].length; j++) {
                for (final double d : bad) {
                    final double value = c[i][j];
                    c[i][j] = d;
                    try {
                        TetrahedronSampler.of(rng, c[0], c[1], c[2], c[3]);
                        Assertions.fail(String.format("Did not detect non-finite coordinate: %d,%d = %s", i, j, d));
                    } catch (IllegalArgumentException ex) {
                        // Expected
                    }
                    c[i][j] = value;
                }
            }
        }
    }

    /**
     * Test a tetrahedron with coordinates that are separated by more than
     * {@link Double#MAX_VALUE}.
     */
    @Test
    public void testExtremeValueCoordinates() {
        // Object seed so use Long not long
        final Long seed = 876543L;
        // Create a valid tetrahedron that can be scaled
        final double[][] c1 = new double[][] {
            {1, 1, 1}, {1, -1, -1}, {-1, -1, 1}, {-1, 1, -1}
        };
        final double[][] c2 = new double[4][3];
        // Extremely large value for scaling. Use a power of 2 for exact scaling.
        final double scale = 0x1.0p1023;
        for (int i = 0; i < c1.length; i++) {
            // Scale the second tetrahedron
            for (int j = 0; j < 3; j++) {
                c2[i][j] = c1[i][j] * scale;
            }
        }
        // Show the tetrahedron is too big to compute vectors between points.
        Assertions.assertEquals(Double.NEGATIVE_INFINITY, c2[2][0] - c2[0][0],
            "Expect vector c - a to be infinite in the x dimension");
        Assertions.assertEquals(Double.NEGATIVE_INFINITY, c2[2][1] - c2[3][1],
            "Expect vector c - d to be infinite in the y dimension");
        Assertions.assertEquals(Double.POSITIVE_INFINITY, c2[2][2] - c2[1][2],
            "Expect vector c - b to be infinite in the z dimension");

        final TetrahedronSampler sampler1 = TetrahedronSampler.of(
                RandomSource.XO_RO_SHI_RO_128_PP.create(seed), c1[0], c1[1], c1[2], c1[3]);
        final TetrahedronSampler sampler2 = TetrahedronSampler.of(
                RandomSource.XO_RO_SHI_RO_128_PP.create(seed), c2[0], c2[1], c2[2], c2[3]);

        for (int n = 0; n < 10; n++) {
            final double[] a = sampler1.sample();
            final double[] b = sampler2.sample();
            for (int i = 0; i < a.length; i++) {
                a[i] *= scale;
            }
            Assertions.assertArrayEquals(a, b);
        }
    }

    /**
     * Test the distribution of points in three dimensions. 6 tetrahedra are used to create
     * a box. The distribution should be uniform inside the box.
     */
    @Test
    public void testDistribution() {
        // Create the lower and upper limits of the box
        final double lx = -1;
        final double ly = -2;
        final double lz = 1;
        final double ux = 3;
        final double uy = 4;
        final double uz = 5;
        // Create vertices abcd and efgh for the lower and upper rectangles
        // (in the XY plane) of the box
        final double[] a = {lx, ly, lz};
        final double[] b = {ux, ly, lz};
        final double[] c = {ux, uy, lz};
        final double[] d = {lx, uy, lz};
        final double[] e = {lx, ly, uz};
        final double[] f = {ux, ly, uz};
        final double[] g = {ux, uy, uz};
        final double[] h = {lx, uy, uz};

        // Assign bins
        final int bins = 10;
        // Samples should be a multiple of 6 (due to combining 6 equal volume tetrahedra)
        final int samplesPerBin = 12;
        // Scale factors to assign x,y,z to a bin
        final double sx = bins / (ux - lx);
        final double sy = bins / (uy - ly);
        final double sz = bins / (uz - lz);

        // Compute factor to allocate bin index:
        // index = x + y * binsX + z * binsX * binsY
        final int binsXy = bins * bins;

        // Expect a uniform distribution (this is rescaled by the ChiSquareTest)
        final double[] expected = new double[binsXy * bins];
        Arrays.fill(expected, 1);

        // Increase the loops and use a null seed (i.e. randomly generated) to verify robustness
        final UniformRandomProvider rng = RandomSource.XO_SHI_RO_512_PP.create(0xaabbccddeeffL);

        // Cut the box into 6 equal volume tetrahedra by cutting the box in half three times,
        // cutting diagonally through each of the three pairs of opposing faces. In this way,
        // the tetrahedra all share one of the main diagonals of the box (d-f).
        // See the cuts used for the marching tetrahedra algorithm:
        // https://en.wikipedia.org/wiki/Marching_tetrahedra
        final TetrahedronSampler[] samplers = {TetrahedronSampler.of(rng, d, f, b, c),
                                               TetrahedronSampler.of(rng, d, f, c, g),
                                               TetrahedronSampler.of(rng, d, f, g, h),
                                               TetrahedronSampler.of(rng, d, f, h, e),
                                               TetrahedronSampler.of(rng, d, f, e, a),
                                               TetrahedronSampler.of(rng, d, f, a, b)};
        // To determine the sample is inside the correct tetrahedron it is projected to the
        // 4 faces of the tetrahedron along the face normals. The distance should be negative
        // when the face normals are orientated outwards.
        final Tetrahedron[] tetrahedrons = {new Tetrahedron(d, f, b, c),
                                            new Tetrahedron(d, f, c, g),
                                            new Tetrahedron(d, f, g, h),
                                            new Tetrahedron(d, f, h, e),
                                            new Tetrahedron(d, f, e, a),
                                            new Tetrahedron(d, f, a, b)};

        final int samples = expected.length * samplesPerBin;
        for (int n = 0; n < 1; n++) {
            // Assign each coordinate to a region inside the combined box
            final long[] observed = new long[expected.length];
            // Equal volume tetrahedra so sample from each one
            for (int i = 0; i < samples; i += 6) {
                for (int j = 0; j < 6; j++) {
                    addObservation(samplers[j].sample(), observed, bins, binsXy,
                                   lx, ly, lz, sx, sy, sz, tetrahedrons[j]);
                }
            }
            final double p = new ChiSquareTest().chiSquareTest(expected, observed);
            Assertions.assertFalse(p < 0.001, () -> "p-value too small: " + p);
        }
    }

    /**
     * Adds the observation. Coordinates are mapped using the offsets, scaled and
     * then cast to an integer bin.
     *
     * <pre>
     * binx = (int) ((x - lx) * sx)
     * </pre>
     *
     * @param v the sample (3D coordinate xyz)
     * @param observed the observations
     * @param binsX the numbers of bins in the x dimension
     * @param binsXy the numbers of bins in the combined x and y dimensions
     * @param lx the lower limit to convert the x coordinate to the x bin
     * @param ly the lower limit to convert the y coordinate to the y bin
     * @param lz the lower limit to convert the z coordinate to the z bin
     * @param sx the scale to convert the x coordinate to the x bin
     * @param sy the scale to convert the y coordinate to the y bin
     * @param sz the scale to convert the z coordinate to the z bin
     * @param tetrahedron the tetrahedron the sample should be within
     */
    // CHECKSTYLE: stop ParameterNumberCheck
    private static void addObservation(double[] v, long[] observed,
                                       int binsX, int binsXy,
                                       double lx, double ly, double lz,
                                       double sx, double sy, double sz,
                                       Tetrahedron tetrahedron) {
        Assertions.assertEquals(3, v.length);
        // Test the point is inside the correct tetrahedron
        Assertions.assertTrue(tetrahedron.contains(v), "Not inside the tetrahedron");
        final double x = v[0];
        final double y = v[1];
        final double z = v[2];
        // Add to the correct bin after using the offset
        final int binx = (int) ((x - lx) * sx);
        final int biny = (int) ((y - ly) * sy);
        final int binz = (int) ((z - lz) * sz);
        observed[binz * binsXy + biny * binsX + binx]++;
    }
    // CHECKSTYLE: resume ParameterNumberCheck

    /**
     * Test the SharedStateSampler implementation.
     */
    @Test
    public void testSharedStateSampler() {
        final UniformRandomProvider rng1 = RandomSource.SPLIT_MIX_64.create(0L);
        final UniformRandomProvider rng2 = RandomSource.SPLIT_MIX_64.create(0L);
        final double[] c1 = createCoordinate(-1);
        final double[] c2 = createCoordinate(2);
        final double[] c3 = createCoordinate(-3);
        final double[] c4 = createCoordinate(4);
        final TetrahedronSampler sampler1 = TetrahedronSampler.of(rng1, c1, c2, c3, c4);
        final TetrahedronSampler sampler2 = sampler1.withUniformRandomProvider(rng2);
        RandomAssert.assertProduceSameSequence(sampler1, sampler2);
    }

    /**
     * Test the input vectors are copied and not used by reference.
     */
    @Test
    public void testChangedInputCoordinates() {
        final UniformRandomProvider rng1 = RandomSource.SPLIT_MIX_64.create(0L);
        final UniformRandomProvider rng2 = RandomSource.SPLIT_MIX_64.create(0L);
        final double[] c1 = createCoordinate(1);
        final double[] c2 = createCoordinate(2);
        final double[] c3 = createCoordinate(-3);
        final double[] c4 = createCoordinate(-4);
        final TetrahedronSampler sampler1 = TetrahedronSampler.of(rng1, c1, c2, c3, c4);
        // Check the input vectors are copied and not used by reference.
        // Change them in place and create a new sampler. It should have different output
        // translated by the offset.
        final double offset = 10;
        for (int i = 0; i < 3; i++) {
            c1[i] += offset;
            c2[i] += offset;
            c3[i] += offset;
            c4[i] += offset;
        }
        final TetrahedronSampler sampler2 = TetrahedronSampler.of(rng2, c1, c2, c3, c4);
        for (int n = 0; n < 5; n++) {
            final double[] s1 = sampler1.sample();
            final double[] s2 = sampler2.sample();
            Assertions.assertEquals(3, s1.length);
            Assertions.assertEquals(3, s2.length);
            for (int i = 0; i < 3; i++) {
                Assertions.assertEquals(s1[i] + offset, s2[i], 1e-10);
            }
        }
    }

    /**
     * Test the tetrahedron contains predicate.
     */
    @Test
    public void testTetrahedronContains() {
        final double[][] c1 = new double[][] {
            {1, 1, 1}, {1, -1, -1}, {-1, -1, 1}, {-1, 1, -1}
        };
        final Tetrahedron tetrahedron = new Tetrahedron(c1[0], c1[1], c1[2], c1[3]);
        // Testing points on the vertices, edges or faces are subject to floating point error
        final double epsilon = 1e-14;
        // Vertices
        for (int i = 0; i < 4; i++) {
            Assertions.assertTrue(tetrahedron.contains(c1[i], epsilon));
        }
        // Edge
        Assertions.assertTrue(tetrahedron.contains(new double[] {1, 0, 0}, epsilon));
        Assertions.assertTrue(tetrahedron.contains(new double[] {0.5, 0.5, 1}, epsilon));
        // Just inside the edge
        Assertions.assertTrue(tetrahedron.contains(new double[] {1 - 1e-10, 0, 0}));
        Assertions.assertTrue(tetrahedron.contains(new double[] {0.5, 0.5, 1 - 1e-10}));
        // Just outside the edge
        Assertions.assertFalse(tetrahedron.contains(new double[] {1, 0, 1e-10}, epsilon));
        Assertions.assertFalse(tetrahedron.contains(new double[] {0.5, 0.5 + 1e-10, 1}, epsilon));
        // Face
        double x = 1.0 / 3;
        Assertions.assertTrue(tetrahedron.contains(new double[] {x, -x, x}, epsilon));
        Assertions.assertTrue(tetrahedron.contains(new double[] {-x, -x, -x}, epsilon));
        Assertions.assertTrue(tetrahedron.contains(new double[] {x, x, -x}, epsilon));
        Assertions.assertTrue(tetrahedron.contains(new double[] {-x, x, x}, epsilon));
        // Just outside the face
        x += 1e-10;
        Assertions.assertFalse(tetrahedron.contains(new double[] {x, -x, x}, epsilon));
        Assertions.assertFalse(tetrahedron.contains(new double[] {-x, -x, -x}, epsilon));
        Assertions.assertFalse(tetrahedron.contains(new double[] {x, x, -x}, epsilon));
        Assertions.assertFalse(tetrahedron.contains(new double[] {-x, x, x}, epsilon));
        // Inside
        Assertions.assertTrue(tetrahedron.contains(new double[] {0, 0, 0}));
        Assertions.assertTrue(tetrahedron.contains(new double[] {0.5, 0.25, -0.1}));
        // Outside
        Assertions.assertFalse(tetrahedron.contains(new double[] {0, 20, 0}));
        Assertions.assertFalse(tetrahedron.contains(new double[] {-20, 0, 0}));
        Assertions.assertFalse(tetrahedron.contains(new double[] {6, 6, 4}));
    }

    /**
     * Creates the coordinate of length 3 filled with
     * the given value and the dimension index: x + i.
     *
     * @param x the value for index 0
     * @return the coordinate
     */
    private static double[] createCoordinate(double x) {
        final double[] coord = new double[3];
        for (int i = 0; i < 3; i++) {
            coord[0] = x + i;
        }
        return coord;
    }

    /**
     * Class to test if a point is inside the tetrahedron.
     *
     * <p>Computes the outer pointing face normals for the tetrahedron. A point is inside
     * if the point lies below each of the face planes of the shape.
     *
     * @see <a href="https://mathworld.wolfram.com/Point-PlaneDistance.html">Point-Plane distance</a>
     */
    private static class Tetrahedron {
        /** The face normals. */
        private final double[][] n;
        /** The distance of each face from the origin. */
        private final double[] d;

        /**
         * Create an instance.
         *
         * @param v1 The first vertex.
         * @param v2 The second vertex.
         * @param v3 The third vertex.
         * @param v4 The fourth vertex.
         */
        Tetrahedron(double[] v1, double[] v2, double[] v3, double[] v4) {
            // Compute the centre of each face
            final double[][] x = new double[][] {
                centre(v1, v2, v3),
                centre(v2, v3, v4),
                centre(v3, v4, v1),
                centre(v4, v1, v2)
            };

            // Compute the normal for each face
            n = new double[][] {
                normal(v1, v2, v3),
                normal(v2, v3, v4),
                normal(v3, v4, v1),
                normal(v4, v1, v2)
            };

            // Given the plane:
            // 0 = ax + by + cz + d
            // Where abc is the face normal and d is the distance of the plane from the origin.
            // Compute d:
            // d = -(ax + by + cz)
            d = new double[] {
                -dot(n[0], x[0]),
                -dot(n[1], x[1]),
                -dot(n[2], x[2]),
                -dot(n[3], x[3]),
            };

            // Compute the distance of the other vertex from each face plane.
            // When below the distance should be negative. Orient each normal so this is true.
            //
            // This distance D of a point xyz to the plane is:
            // D = ax + by + cz + d
            // Above plane:
            // ax + by + cz + d > 0
            // ax + by + cz > -d
            final double[][] other = {v4, v1, v2, v3};
            for (int i = 0; i < 4; i++) {
                if (dot(n[i], other[i]) > -d[i]) {
                    // Swap orientation
                    n[i][0] = -n[i][0];
                    n[i][1] = -n[i][1];
                    n[i][2] = -n[i][2];
                    d[i] = -d[i];
                }
            }
        }

        /**
         * Compute the centre of the triangle face.
         *
         * @param a The first vertex.
         * @param b The second vertex.
         * @param c The third vertex.
         * @return the centre
         */
        private static double[] centre(double[] a, double[] b, double[] c) {
            return new double[] {
                (a[0] + b[0] + c[0]) / 3,
                (a[1] + b[1] + c[1]) / 3,
                (a[2] + b[2] + c[2]) / 3
            };
        }

        /**
         * Compute the normal of the triangle face.
         *
         * @param a The first vertex.
         * @param b The second vertex.
         * @param c The third vertex.
         * @return the normal
         */
        private static double[] normal(double[] a, double[] b, double[] c) {
            final double[] v1 = subtract(b, a);
            final double[] v2 = subtract(c, a);
            // Cross product
            final double[] normal = {
                v1[1] * v2[2] - v1[2] * v2[1],
                v1[2] * v2[0] - v1[0] * v2[2],
                v1[0] * v2[1] - v1[1] * v2[0]
            };
            // Normalise
            final double scale = 1.0 / Math.sqrt(dot(normal, normal));
            normal[0] *= scale;
            normal[1] *= scale;
            normal[2] *= scale;
            return normal;
        }

        /**
         * Compute the dot product of vector {@code a} and {@code b}.
         *
         * <pre>
         * a.b = a.x * b.x + a.y * b.y + a.z * b.z
         * </pre>
         *
         * @param a the first vector
         * @param b the second vector
         * @return the dot product
         */
        private static double dot(double[] a, double[] b) {
            return a[0] * b[0] + a[1] * b[1] + a[2] * b[2];
        }

        /**
         * Subtract the second term from the first: {@code a - b}.
         *
         * @param a The first term.
         * @param b The second term.
         * @return the vector {@code a - b}
         */
        private static double[] subtract(double[] a, double[] b) {
            return new double[] {
                a[0] - b[0],
                a[1] - b[1],
                a[2] - b[2]
            };
        }

        /**
         * Check whether or not the tetrahedron contains the given point.
         *
         * @param x the coordinate
         * @return true if inside the tetrahedron
         */
        boolean contains(double[] x) {
            // Must be below all the face planes
            for (int i = 0; i < 4; i++) {
                // This distance D of a point xyz to the plane is:
                // D = ax + by + cz + d
                // Above plane:
                // ax + by + cz + d > 0
                // ax + by + cz > -d
                if (dot(n[i], x) > -d[i]) {
                    return false;
                }
            }
            return true;
        }

        /**
         * Check whether or not the tetrahedron contains the given point
         * within the given absolute epsilon.
         *
         * @param x the coordinate
         * @param epsilon the epsilon
         * @return true if inside the tetrahedron
         */
        boolean contains(double[] x, double epsilon) {
            for (int i = 0; i < 4; i++) {
                // As above but with an epsilon above zero
                if (dot(n[i], x) > epsilon - d[i]) {
                    return false;
                }
            }
            return true;
        }
    }
}
