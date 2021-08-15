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
import org.junit.Test;

import org.apache.commons.math3.stat.inference.ChiSquareTest;

import org.apache.commons.rng.UniformRandomProvider;
import org.apache.commons.rng.sampling.RandomAssert;
import org.apache.commons.rng.simple.RandomSource;

/**
 * Test for {@link TriangleSampler}.
 */
public class TriangleSamplerTest {
    // Precomputed 3D and 4D rotation matrices for forward and backward transforms
    // created using the matlab function by jodag:
    // https://stackoverflow.com/questions/50337642/how-to-calculate-a-rotation-matrix-in-n-dimensions-given-the-point-to-rotate-an

    /** 3D rotation around the vector [1; 2; 3] by pi/4. */
    private static final double[][] F3 = {
            {0.728027725387508, -0.525104821111919, 0.440727305612110},
            {0.608788597915763, 0.790790557990391, -0.063456571298848},
            {-0.315201640406345, 0.314507901710379, 0.895395278995195}};
    /** 3D rotation around the vector [1; 2; 3] by -pi/4. */
    private static final double[][] R3 = {
            {0.728027725387508, 0.608788597915762, -0.315201640406344},
            {-0.525104821111919, 0.790790557990391, 0.314507901710379},
            {0.440727305612110, -0.063456571298848, 0.895395278995195}};
    /** 4D rotation around the orthogonal vectors [1 0; 0 1; 1 0; 0 1] by pi/4. */
    private static final double[][] F4 = {
            {0.853553390593274, -0.353553390593274, 0.146446609406726, 0.353553390593274},
            {0.353553390593274, 0.853553390593274, -0.353553390593274, 0.146446609406726},
            {0.146446609406726, 0.353553390593274, 0.853553390593274, -0.353553390593274},
            {-0.353553390593274, 0.146446609406726, 0.353553390593274, 0.853553390593274}};
    /** 4D rotation around the orthogonal vectors [1 0; 0 1; 1 0; 0 1] by -pi/4. */
    private static final double[][] R4 = {
            {0.853553390593274, 0.353553390593274, 0.146446609406726, -0.353553390593274},
            {-0.353553390593274, 0.853553390593274, 0.353553390593274, 0.146446609406726},
            {0.146446609406726, -0.353553390593274, 0.853553390593274, 0.353553390593274},
            {0.353553390593274, 0.146446609406726, -0.353553390593274, 0.853553390593274}};

    /**
     * Test the sampling assumptions used to transform coordinates outside the triangle
     * back inside the triangle.
     */
    @Test
    public void testSamplingAssumptions() {
        // The separation between the 2^53 dyadic rationals in the interval [0, 1)
        final double delta = 0x1.0p-53;
        double s = 0.5;
        double t = 0.5 + delta;
        // This value cannot be exactly represented and is rounded
        final double spt = s + t;
        // Test that (1 - (1-s) - (1-t)) is not equal to (s + t - 1).
        // This is due to the rounding to store s + t as a double.
        final double expected = 1 - (1 - s) - (1 - t);
        Assertions.assertNotEquals(expected, spt - 1);
        Assertions.assertNotEquals(expected, s + t - 1);
        // For any uniform deviate u in [0, 1], u - 1 is exact, thus s - 1 is exact
        // and s - 1 + t is exact.
        Assertions.assertEquals(expected, s - 1 + t);

        // Test that a(1 - s - t) + sb + tc does not overflow is s+t = 1
        final double max = Double.MAX_VALUE;
        s -= delta;
        final UniformRandomProvider rng = RandomSource.XO_RO_SHI_RO_128_PP.create();
        for (int n = 0; n < 100; n++) {
            Assertions.assertNotEquals(Double.POSITIVE_INFINITY, (1 - s - t) * max + s * max + t * max);
            s = rng.nextDouble();
            t = 1.0 - s;
        }
    }

    /**
     * Test an unsupported dimension.
     */
    @Test
    public void testInvalidDimensionThrows() {
        final UniformRandomProvider rng = RandomSource.SPLIT_MIX_64.create(0L);
        Assertions.assertThrows(IllegalArgumentException.class,
            () -> TriangleSampler.of(rng, new double[1], new double[1], new double[1]));
    }

    /**
     * Test a dimension mismatch between vertices.
     */
    @Test
    public void testDimensionMismatchThrows() {
        final UniformRandomProvider rng = RandomSource.SPLIT_MIX_64.create(0L);
        final double[] c2 = new double[2];
        final double[] c3 = new double[3];
        for (double[][] c : new double[][][] {
            {c2, c2, c3},
            {c2, c3, c2},
            {c3, c2, c2},
            {c2, c3, c3},
            {c3, c3, c2},
            {c3, c2, c3},
        }) {
            try {
                TriangleSampler.of(rng, c[0], c[1], c[2]);
                Assertions.fail(String.format("Did not detect dimension mismatch: %d,%d,%d",
                        c[0].length, c[1].length, c[2].length));
            } catch (IllegalArgumentException ex) {
                // Expected
            }
        }
    }

    /**
     * Test non-finite vertices.
     */
    @Test
    public void testNonFiniteVertexCoordinates() {
        final UniformRandomProvider rng = RandomSource.SPLIT_MIX_64.create(0L);
        // A valid triangle
        final double[][] c = new double[][] {
            {0, 0, 1}, {2, 1, 0}, {-1, 2, 3}
        };
        Assertions.assertNotNull(TriangleSampler.of(rng, c[0],  c[1],  c[2]));
        final double[] bad = {Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NaN};
        for (int i = 0; i < c.length; i++) {
            for (int j = 0; j < c[0].length; j++) {
                for (final double d : bad) {
                    final double value = c[i][j];
                    c[i][j] = d;
                    try {
                        TriangleSampler.of(rng, c[0], c[1], c[2]);
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
     * Test a triangle with coordinates that are separated by more than
     * {@link Double#MAX_VALUE} in two dimensions.
     */
    @Test
    public void testExtremeValueCoordinates2D() {
        testExtremeValueCoordinates(2);
    }

    /**
     * Test a triangle with coordinates that are separated by more than
     * {@link Double#MAX_VALUE} in three dimensions.
     */
    @Test
    public void testExtremeValueCoordinates3D() {
        testExtremeValueCoordinates(3);
    }

    /**
     * Test a triangle with coordinates that are separated by more than
     * {@link Double#MAX_VALUE} in four dimensions.
     */
    @Test
    public void testExtremeValueCoordinates4D() {
        testExtremeValueCoordinates(4);
    }

    /**
     * Test a triangle with coordinates that are separated by more than
     * {@link Double#MAX_VALUE}.
     *
     * @param dimension the dimension
     */
    private static void testExtremeValueCoordinates(int dimension) {
        // Object seed so use Long not long
        final Long seed = 999666333L;
        final double[][] c1 = new double[3][dimension];
        final double[][] c2 = new double[3][dimension];
        // Create a valid triangle that can be scaled
        c1[0][0] = -1;
        c1[0][1] = -1;
        c1[1][0] = 1;
        c1[1][1] = 1;
        c1[2][0] = 1;
        c1[2][1] = -1;
        // Extremely large value for scaling. Use a power of 2 for exact scaling.
        final double scale = 0x1.0p1023;
        for (int i = 0; i < 3; i++) {
            // Fill the remaining dimensions with 1 or -1
            for (int j = 2; j < dimension; j++) {
                c1[i][j] = 1 - (j & 0x2);
            }
            // Scale the second triangle
            for (int j = 0; j < dimension; j++) {
                c2[i][j] = c1[i][j] * scale;
            }
        }
        // Show the triangle is too big to compute vectors between points.
        Assertions.assertEquals(Double.POSITIVE_INFINITY, c2[2][0] - c2[0][0],
            "Expect vector c - a to be infinite in the x dimension");
        Assertions.assertEquals(Double.NEGATIVE_INFINITY, c2[2][1] - c2[1][1],
            "Expect vector c - b to be infinite in the y dimension");

        final TriangleSampler sampler1 = TriangleSampler.of(
                RandomSource.XO_RO_SHI_RO_128_PP.create(seed), c1[0], c1[1], c1[2]);
        final TriangleSampler sampler2 = TriangleSampler.of(
                RandomSource.XO_RO_SHI_RO_128_PP.create(seed), c2[0], c2[1], c2[2]);

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
     * Test the distribution of points in two dimensions.
     */
    @Test
    public void testDistribution2D() {
        testDistributionND(2);
    }

    /**
     * Test the distribution of points in three dimensions.
     */
    @Test
    public void testDistribution3D() {
        testDistributionND(3);
    }

    /**
     * Test the distribution of points in four dimensions.
     */
    @Test
    public void testDistribution4D() {
        testDistributionND(4);
    }

    /**
     * Test the distribution of points in N dimensions. The output coordinates
     * should be uniform in the triangle.
     *
     * <p>ND is supported by transforming 2D triangles to ND, sampling in ND then
     * transforming back to 2D. The transformed results should have a zero
     * coordinate for indices above 1. Supports 2 to 4 dimensions.
     *
     * @param dimension the dimension in the range [2, 4]
     */
    private static void testDistributionND(int dimension) {
        // Create 3 triangles that form a 3x4 rectangle:
        // b-----c
        // |\   /|
        // | \ / |
        // |  e  |
        // |   \ |
        // |    \|
        // a-----d
        //
        // Sample from them proportional to the area:
        // adb = 4 * 3 / 2 = 6
        // bce = 3 * 2 / 2 = 3
        // cde = 4 * 1.5 / 2 = 3
        // The samples in the ratio 2:1:1 should be uniform within the rectangle.
        final double[] a = {0, 0};
        final double[] b = {0, 4};
        final double[] c = {3, 4};
        final double[] d = {3, 0};
        final double[] e = {1.5, 2};

        // Assign bins
        final int bins = 20;
        final int samplesPerBin = 10;
        // Scale factors to assign x,y to a bin
        final double sx = bins / 3.0;
        final double sy = bins / 4.0;

        // Expect a uniform distribution (this is rescaled by the ChiSquareTest)
        final double[] expected = new double[bins * bins];
        Arrays.fill(expected, 1);

        // Support ND by applying a rotation transform to the 2D triangles to generate
        // n-coordinates. The samples are transformed back and should be uniform in 2D.
        Transform forward;
        Transform reverse;
        if (dimension == 4) {
            forward = new ForwardTransform(F4);
            reverse = new ReverseTransform(R4);
        } else if (dimension == 3) {
            forward = new ForwardTransform(F3);
            reverse = new ReverseTransform(R3);
        } else if (dimension == 2) {
            forward = reverse = new Transform2Dto2D();
        } else {
            throw new AssertionError("Unsupported dimension: " + dimension);
        }

        // Increase the loops and use a null seed (i.e. randomly generated) to verify robustness
        final UniformRandomProvider rng = RandomSource.XO_SHI_RO_512_PP.create(0xfabcab);
        final TriangleSampler sampler1 = TriangleSampler.of(rng, forward.apply(a), forward.apply(d), forward.apply(b));
        final TriangleSampler sampler2 = TriangleSampler.of(rng, forward.apply(b), forward.apply(c), forward.apply(e));
        final TriangleSampler sampler3 = TriangleSampler.of(rng, forward.apply(c), forward.apply(d), forward.apply(e));
        final Triangle triangle1 = new Triangle(a, d, b);
        final Triangle triangle2 = new Triangle(b, c, e);
        final Triangle triangle3 = new Triangle(c, d, e);
        final int samples = expected.length * samplesPerBin;
        for (int n = 0; n < 1; n++) {
            // Assign each coordinate to a region inside the combined rectangle
            final long[] observed = new long[expected.length];
            // Sample according to the area of each triangle (ratio 2:1:1)
            for (int i = 0; i < samples; i += 4) {
                addObservation(reverse.apply(sampler1.sample()), observed, bins, sx, sy, triangle1);
                addObservation(reverse.apply(sampler1.sample()), observed, bins, sx, sy, triangle1);
                addObservation(reverse.apply(sampler2.sample()), observed, bins, sx, sy, triangle2);
                addObservation(reverse.apply(sampler3.sample()), observed, bins, sx, sy, triangle3);
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
     * binx = (int) (x * sx)
     * </pre>
     *
     * @param v the sample (2D coordinate xy)
     * @param observed the observations
     * @param bins the numbers of bins in the x dimension
     * @param sx the scale to convert the x coordinate to the x bin
     * @param sy the scale to convert the y coordinate to the y bin
     * @param triangle the triangle the sample should be within
     */
    private static void addObservation(double[] v, long[] observed,
            int bins, double sx, double sy, Triangle triangle) {
        final double x = v[0];
        final double y = v[1];
        // Test the point is triangle the triangle
        Assertions.assertTrue(triangle.contains(x, y));
        // Add to the correct bin after using the offset
        final int binx = (int) (x * sx);
        final int biny = (int) (y * sy);
        observed[biny * bins + binx]++;
    }

    /**
     * Test the SharedStateSampler implementation for 2D.
     */
    @Test
    public void testSharedStateSampler2D() {
        testSharedStateSampler(2);
    }

    /**
     * Test the SharedStateSampler implementation for 3D.
     */
    @Test
    public void testSharedStateSampler3D() {
        testSharedStateSampler(3);
    }

    /**
     * Test the SharedStateSampler implementation for 4D.
     */
    @Test
    public void testSharedStateSampler4D() {
        testSharedStateSampler(4);
    }

    /**
     * Test the SharedStateSampler implementation for the given dimension.
     */
    private static void testSharedStateSampler(int dimension) {
        final UniformRandomProvider rng1 = RandomSource.SPLIT_MIX_64.create(0L);
        final UniformRandomProvider rng2 = RandomSource.SPLIT_MIX_64.create(0L);
        final double[] c1 = createCoordinate(1, dimension);
        final double[] c2 = createCoordinate(2, dimension);
        final double[] c3 = createCoordinate(-3, dimension);
        final TriangleSampler sampler1 = TriangleSampler.of(rng1, c1, c2, c3);
        final TriangleSampler sampler2 = sampler1.withUniformRandomProvider(rng2);
        RandomAssert.assertProduceSameSequence(sampler1, sampler2);
    }

    /**
     * Test the input vectors are copied and not used by reference for 2D.
     */
    @Test
    public void testChangedInputCoordinates2D() {
        testChangedInputCoordinates(2);
    }

    /**
     * Test the input vectors are copied and not used by reference for 3D.
     */
    @Test
    public void testChangedInputCoordinates3D() {
        testChangedInputCoordinates(3);
    }

    /**
     * Test the input vectors are copied and not used by reference for 4D.
     */
    @Test
    public void testChangedInputCoordinates4D() {
        testChangedInputCoordinates(4);
    }

    /**
     * Test the input vectors are copied and not used by reference for the given
     * dimension.
     *
     * @param dimension the dimension
     */
    private static void testChangedInputCoordinates(int dimension) {
        final UniformRandomProvider rng1 = RandomSource.SPLIT_MIX_64.create(0L);
        final UniformRandomProvider rng2 = RandomSource.SPLIT_MIX_64.create(0L);
        final double[] c1 = createCoordinate(1, dimension);
        final double[] c2 = createCoordinate(2, dimension);
        final double[] c3 = createCoordinate(-3, dimension);
        final TriangleSampler sampler1 = TriangleSampler.of(rng1, c1, c2, c3);
        // Check the input vectors are copied and not used by reference.
        // Change them in place and create a new sampler. It should have different output
        // translated by the offset.
        final double offset = 10;
        for (int i = 0; i < dimension; i++) {
            c1[i] += offset;
            c2[i] += offset;
            c3[i] += offset;
        }
        final TriangleSampler sampler2 = TriangleSampler.of(rng2, c1, c2, c3);
        for (int n = 0; n < 3; n++) {
            final double[] s1 = sampler1.sample();
            final double[] s2 = sampler2.sample();
            Assertions.assertEquals(s1.length, s2.length);
            Assertions.assertFalse(Arrays.equals(s1, s2),
                    "First sampler has used the vertices by reference");
            for (int i = 0; i < dimension; i++) {
                Assertions.assertEquals(s1[i] + offset, s2[i], 1e-10);
            }
        }
    }

    /**
     * Creates the coordinate of length specified by the dimension filled with
     * the given value and the dimension index: x + i.
     *
     * @param x the value for index 0
     * @param dimension the dimension
     * @return the coordinate
     */
    private static double[] createCoordinate(double x, int dimension) {
        final double[] coord = new double[dimension];
        for (int i = 0; i < dimension; i++) {
            coord[0] = x + i;
        }
        return coord;
    }

    /**
     * Test the transform generates 3D coordinates and can reverse them.
     */
    @Test
    public void testTransform3D() {
        testTransformND(3);
    }

    /**
     * Test the transform generates 4D coordinates and can reverse them.
     */
    @Test
    public void testTransform4D() {
        testTransformND(4);
    }

    /**
     * Test the transform generates ND coordinates and can reverse them.
     *
     * @param dimension the dimension
     */
    private static void testTransformND(int dimension) {
        Transform forward;
        Transform reverse;
        if (dimension == 4) {
            forward = new ForwardTransform(F4);
            reverse = new ReverseTransform(R4);
        } else if (dimension == 3) {
            forward = new ForwardTransform(F3);
            reverse = new ReverseTransform(R3);
        } else if (dimension == 2) {
            forward = reverse = new Transform2Dto2D();
        } else {
            throw new AssertionError("Unsupported dimension: " + dimension);
        }

        final UniformRandomProvider rng = RandomSource.SPLIT_MIX_64.create(789L);
        double sum = 0;
        for (int n = 0; n < 10; n++) {
            final double[] a = new double[] {rng.nextDouble(), rng.nextDouble()};
            final double[] b = forward.apply(a);
            Assertions.assertEquals(dimension, b.length);
            for (int i = 2; i < dimension; i++) {
                sum += Math.abs(b[i]);
            }
            final double[] c = reverse.apply(b);
            Assertions.assertArrayEquals(a, c, 1e-10);
        }
        // Check that higher dimension coordinates are generated
        Assertions.assertTrue(sum > 0.5);
    }

    /**
     * Test 3D rotations forward and reverse.
     */
    @Test
    public void testRotations3D() {
        final double[] x = {1, 0.5, 0};
        final double[] y = multiply(F3, x);
        Assertions.assertArrayEquals(new double[] {0.465475314831549, 1.004183876910958, -0.157947689551155}, y, 1e-10);
        Assertions.assertEquals(length(x), length(y), 1e-10);
        final double[] x2 = multiply(R3, y);
        Assertions.assertArrayEquals(x, x2, 1e-10);
    }

    /**
     * Test 4D rotations forward and reverse.
     */
    @Test
    public void testRotations4D() {
        final double[] x = {1, 0.5, 0, 0};
        final double[] y = multiply(F4, x);
        Assertions.assertArrayEquals(
                new double[] {0.676776695296637, 0.780330085889911, 0.323223304703363, -0.280330085889911}, y, 1e-10);
        Assertions.assertEquals(length(x), length(y), 1e-10);
        final double[] x2 = multiply(R4, y);
        Assertions.assertArrayEquals(x, x2, 1e-10);
    }

    /**
     * Matrix multiplication. It is assumed the matrix is square and matches (or exceeds)
     * the vector length.
     *
     * @param m the matrix
     * @param v the vector
     * @return the result
     */
    private static double[] multiply(double[][] matrix, double[] v) {
        final int n = matrix.length;
        final int m = v.length;
        final double[] r = new double[n];
        for (int i = 0; i < n; i++) {
            double sum = 0;
            for (int j = 0; j < m; j++) {
                sum += matrix[i][j] * v[j];
            }
            r[i] = sum;
        }
        return r;
    }

    /**
     * @return the length (L2-norm) of given vector.
     */
    private static double length(double[] vector) {
        double total = 0;
        for (final double d : vector) {
            total += d * d;
        }
        return Math.sqrt(total);
    }

    /**
     * Test the triangle contains predicate.
     */
    @Test
    public void testTriangleContains() {
        final Triangle triangle = new Triangle(1, 2, 3, 1, 0.5, 6);
        // Vertices
        Assertions.assertTrue(triangle.contains(1, 2));
        Assertions.assertTrue(triangle.contains(3, 1));
        Assertions.assertTrue(triangle.contains(0.5, 6));
        // Edge
        Assertions.assertTrue(triangle.contains(0.75, 4));
        // Inside
        Assertions.assertTrue(triangle.contains(1.5, 3));
        // Outside
        Assertions.assertFalse(triangle.contains(0, 20));
        Assertions.assertFalse(triangle.contains(-20, 0));
        Assertions.assertFalse(triangle.contains(6, 6));
        // Just outside
        Assertions.assertFalse(triangle.contains(0.75, 4 - 1e-10));

        // Note:
        // Touching triangles can both have the point triangle.
        // This predicate is not suitable for assigning points uniquely to
        // non-overlapping triangles that share an edge.
        final Triangle triangle2 = new Triangle(1, 2, 3, 1, 0, -2);
        Assertions.assertTrue(triangle.contains(2, 1.5));
        Assertions.assertTrue(triangle2.contains(2, 1.5));
    }

    /**
     * Define a transform on coordinates.
     */
    private interface Transform {
        /**
         * Apply the transform.
         *
         * @param coord the coordinates
         * @return the new coordinates
         */
        double[] apply(double[] coord);
    }

    /**
     * Transform coordinates from 2D to a higher dimension using the rotation matrix.
     */
    private static class ForwardTransform implements Transform {
        private final double[][] r;

        /**
         * @param r the rotation matrix
         */
        ForwardTransform(double[][] r) {
            this.r = r;
        }

        @Override
        public double[] apply(double[] coord) {
            return multiply(r, coord);
        }
    }

    /**
     * Transform coordinates from a higher dimension to 2D using the rotation matrix.
     * The result should be in the 2D plane (i.e. higher dimensions of the transformed vector
     * are asserted to be zero).
     */
    private static class ReverseTransform implements Transform {
        private final double[][] r;
        private final int n;

        /**
         * @param r the rotation matrix
         */
        ReverseTransform(double[][] r) {
            this.r = r;
            n = r.length;
        }

        @Override
        public double[] apply(double[] coord) {
            Assertions.assertEquals(n, coord.length);
            final double[] x = multiply(r, coord);
            // This should reverse the 2D transform and return to the XY plane.
            for (int i = 2; i < x.length; i++) {
                Assertions.assertEquals(0.0, x[i], 1e-14);
            }
            return new double[] {x[0], x[1]};
        }
    }

    /**
     * No-operation transform on 2D input. Asserts the input coordinates are length 2.
     */
    private static class Transform2Dto2D implements Transform {
        @Override
        public double[] apply(double[] coord) {
            Assertions.assertEquals(2, coord.length);
            return coord;
        }
    }

    /**
     * Class to test if a point is inside the triangle.
     *
     * <p>This function has been adapted from a StackOverflow post by Cédric Dufour. It converts the
     * point to unscaled barycentric coordinates (s,t) and tests they are within the triangle.
     * (Scaling would be done by dividing by twice the area of the triangle.)
     *
     * <h2>Warning</h2>
     *
     * <p>This assigns points on the edges as inside irrespective of edge orientation. Thus
     * back-to-back touching triangles can both have the point inside them. A predicate for geometry
     * applications where the point must be within a unique non-overlapping triangle should use
     * a different solution, e.g. assigning new points to the result of a triangulation.
     * For testing sampling within the triangle this predicate is acceptable.
     *
     * @see <a
     * href="https://stackoverflow.com/questions/2049582/how-to-determine-if-a-point-is-in-a-2d-triangle?lq=1">
     * Point in a triangle</a>
     * @see <a href="https://stackoverflow.com/a/34093754">Point inside triangle by Cédric Dufour</a>
     */
    private static class Triangle {
        private final double p2x;
        private final double p2y;
        private final double dX21;
        private final double dY12;
        private final double dY20;
        private final double dX02;
        private final double d;

        /**
         * Create an instance.
         *
         * @param p0 triangle vertex 0
         * @param p1 triangle vertex 1
         * @param p2 triangle vertex 2
         */
        Triangle(double[] p0, double[] p1, double[] p2) {
            this(p0[0], p0[1], p1[0], p1[1], p2[0], p2[1]);
        }
        /**
         * Create an instance.
         *
         * @param p0x triangle vertex 0 x coordinate
         * @param p0y triangle vertex 0 y coordinate
         * @param p1x triangle vertex 1 x coordinate
         * @param p1y triangle vertex 1 y coordinate
         * @param p2x triangle vertex 2 x coordinate
         * @param p2y triangle vertex 2 y coordinate
         */
        Triangle(double p0x, double p0y, double p1x, double p1y, double p2x, double p2y) {
            this.p2x = p2x;
            this.p2y = p2y;
            // Precompute factors
            dX21 = p2x - p1x;
            dY12 = p1y - p2y;
            dY20 = p2y - p0y;
            dX02 = p0x - p2x;
            // d = twice the signed area of the triangle
            d = dY12 * (p0x - p2x) + dX21 * (p0y - p2y);
        }

        /**
         * Check whether or not the triangle contains the given point.
         *
         * @param px the point x coordinate
         * @param py the point y coordinate
         * @return true if inside the triangle
         */
        boolean contains(double px, double py) {
            // Barycentric coordinates:
            // p = p0 + (p1 - p0) * s + (p2 - p0) * t
            // The point p is inside the triangle if 0 <= s <= 1 and 0 <= t <= 1 and s + t <= 1
            //
            // The following solves s and t.
            // Some factors are precomputed.
            final double dX = px - p2x;
            final double dY = py - p2y;
            final double s = dY12 * dX + dX21 * dY;
            final double t = dY20 * dX + dX02 * dY;
            if (d < 0) {
                return s <= 0 && t <= 0 && s + t >= d;
            }
            return s >= 0 && t >= 0 && s + t <= d;
        }
    }
}
