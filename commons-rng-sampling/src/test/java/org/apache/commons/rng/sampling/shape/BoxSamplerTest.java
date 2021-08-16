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
import org.apache.commons.rng.sampling.UnitSphereSampler;
import org.apache.commons.rng.simple.RandomSource;

/**
 * Test for {@link BoxSampler}.
 */
class BoxSamplerTest {
    /**
     * Test an unsupported dimension.
     */
    @Test
    void testInvalidDimensionThrows() {
        final UniformRandomProvider rng = RandomSource.SPLIT_MIX_64.create(0L);
        Assertions.assertThrows(IllegalArgumentException.class,
            () -> BoxSampler.of(rng, new double[1], new double[1]));
    }

    /**
     * Test a dimension mismatch between vertices.
     */
    @Test
    void testDimensionMismatchThrows() {
        final UniformRandomProvider rng = RandomSource.SPLIT_MIX_64.create(0L);
        final double[] c2 = new double[2];
        final double[] c3 = new double[3];
        for (double[][] c : new double[][][] {
            {c2, c3},
            {c3, c2},
        }) {
            Assertions.assertThrows(IllegalArgumentException.class,
                () -> BoxSampler.of(rng, c[0], c[1]),
                () -> String.format("Did not detect dimension mismatch: %d,%d",
                    c[0].length, c[1].length));
        }
    }

    /**
     * Test non-finite vertices.
     */
    @Test
    void testNonFiniteVertexCoordinates() {
        final UniformRandomProvider rng = RandomSource.SPLIT_MIX_64.create(0L);
        // A valid box
        final double[][] c = new double[][] {
            {0, 1, 2}, {-1, 2, 3}
        };
        Assertions.assertNotNull(BoxSampler.of(rng, c[0],  c[1]));
        final double[] bad = {Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NaN};
        for (int i = 0; i < c.length; i++) {
            final int ii = i;
            for (int j = 0; j < c[0].length; j++) {
                final int jj = j;
                for (final double d : bad) {
                    final double value = c[i][j];
                    c[i][j] = d;
                    Assertions.assertThrows(IllegalArgumentException.class,
                        () -> BoxSampler.of(rng, c[0], c[1]),
                        () -> String.format("Did not detect non-finite coordinate: %d,%d = %s", ii, jj, d));
                    c[i][j] = value;
                }
            }
        }
    }

    /**
     * Test a box with coordinates that are separated by more than
     * {@link Double#MAX_VALUE} in 2D.
     */
    @Test
    void testExtremeValueCoordinates2D() {
        testExtremeValueCoordinates(2);
    }

    /**
     * Test a box with coordinates that are separated by more than
     * {@link Double#MAX_VALUE} in 3D.
     */
    @Test
    void testExtremeValueCoordinates3D() {
        testExtremeValueCoordinates(3);
    }

    /**
     * Test a box with coordinates that are separated by more than
     * {@link Double#MAX_VALUE} in 4D.
     */
    @Test
    void testExtremeValueCoordinates4D() {
        testExtremeValueCoordinates(4);
    }

    /**
     * Test a box with coordinates that are separated by more than
     * {@link Double#MAX_VALUE}.
     *
     * @param dimension the dimension
     */
    private static void testExtremeValueCoordinates(int dimension) {
        // Object seed so use Long not long
        final Long seed = 456456L;
        final double[][] c1 = new double[2][dimension];
        final double[][] c2 = new double[2][dimension];
        // Create a valid box that can be scaled
        Arrays.fill(c1[0], -1);
        Arrays.fill(c1[1], 1);
        // Extremely large value for scaling. Use a power of 2 for exact scaling.
        final double scale = 0x1.0p1023;
        for (int i = 0; i < c1.length; i++) {
            // Scale the second box
            for (int j = 0; j < dimension; j++) {
                c2[i][j] = c1[i][j] * scale;
            }
        }
        // Show the box is too big to compute vectors between points.
        Assertions.assertEquals(Double.POSITIVE_INFINITY, c2[1][0] - c2[0][0],
            "Expect vector b - a to be infinite in the x dimension");

        final BoxSampler sampler1 = BoxSampler.of(
            RandomSource.XO_RO_SHI_RO_128_PP.create(seed), c1[0], c1[1]);
        final BoxSampler sampler2 = BoxSampler.of(
            RandomSource.XO_RO_SHI_RO_128_PP.create(seed), c2[0], c2[1]);

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
     * Test the distribution of points in 2D.
     */
    @Test
    void testDistribution2D() {
        testDistributionND(2);
    }

    /**
     * Test the distribution of points in 3D.
     */
    @Test
    void testDistribution3D() {
        testDistributionND(3);
    }

    /**
     * Test the distribution of points in 4D.
     */
    @Test
    void testDistribution4D() {
        testDistributionND(4);
    }

    /**
     * Test the distribution of points in N dimensions. The output coordinates
     * should be uniform in the box.
     *
     * @param dimension the dimension
     */
    private static void testDistributionND(int dimension) {
        final UniformRandomProvider rng = RandomSource.JSF_64.create(0xdabfab);

        final UnitSphereSampler sphere = UnitSphereSampler.of(rng, dimension);
        final double[] a = sphere.sample();
        final double[] b = sphere.sample();

        // Assign bins
        final int bins = 10;
        final int samplesPerBin = 20;

        // To test uniformity within the box assign the position to a bin using:
        // x - a
        // ----- * bins
        // b - a
        // Pre-compute scaling
        final double[] scale = new double[dimension];
        // Precompute the bin offset for each increasing dimension:
        // 1, bins, bins*bins, bins*bins*bins, ...
        final int[] offset = new int[dimension];
        for (int i = 0; i < dimension; i++) {
            scale[i] = 1.0 / (b[i] - a[i]);
            offset[i] = (int) Math.pow(bins, i);
        }

        // Expect a uniform distribution
        final double[] expected = new double[(int) Math.pow(bins, dimension)];
        Arrays.fill(expected, 1.0);

        // Increase the loops and use a null seed (i.e. randomly generated) to verify robustness
        final BoxSampler sampler = BoxSampler.of(rng, a, b);
        final int samples = expected.length * samplesPerBin;
        for (int n = 0; n < 1; n++) {
            // Assign each coordinate to a region inside the box
            final long[] observed = new long[expected.length];
            for (int i = 0; i < samples; i++) {
                final double[] x = sampler.sample();
                Assertions.assertEquals(dimension, x.length);
                int index = 0;
                for (int j = 0; j < dimension; j++) {
                    final double c = (x[j] - a[j]) * scale[j];
                    Assertions.assertTrue(c >= 0.0 && c <= 1.0, "Not within the box");
                    // Get the bin for this dimension (assumes c != 1.0)
                    final int bin = (int) (c * bins);
                    // Add to the final bin index
                    index += bin * offset[j];
                }
                // Assign the uniform deviate to a bin
                observed[index]++;
            }
            final double p = new ChiSquareTest().chiSquareTest(expected, observed);
            Assertions.assertFalse(p < 0.001, () -> "p-value too small: " + p);
        }
    }

    /**
     * Test the SharedStateSampler implementation for 2D.
     */
    @Test
    void testSharedStateSampler2D() {
        testSharedStateSampler(2);
    }

    /**
     * Test the SharedStateSampler implementation for 3D.
     */
    @Test
    void testSharedStateSampler3D() {
        testSharedStateSampler(3);
    }

    /**
     * Test the SharedStateSampler implementation for 4D.
     */
    @Test
    void testSharedStateSampler4D() {
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
        final BoxSampler sampler1 = BoxSampler.of(rng1, c1, c2);
        final BoxSampler sampler2 = sampler1.withUniformRandomProvider(rng2);
        RandomAssert.assertProduceSameSequence(sampler1, sampler2);
    }

    /**
     * Test the input vectors are copied and not used by reference for 2D.
     */
    @Test
    void testChangedInputCoordinates2D() {
        testChangedInputCoordinates(2);
    }

    /**
     * Test the input vectors are copied and not used by reference for 3D.
     */
    @Test
    void testChangedInputCoordinates3D() {
        testChangedInputCoordinates(3);
    }

    /**
     * Test the input vectors are copied and not used by reference for 4D.
     */
    @Test
    void testChangedInputCoordinates4D() {
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
        final BoxSampler sampler1 = BoxSampler.of(rng1, c1, c2);
        // Check the input vectors are copied and not used by reference.
        // Change them in place and create a new sampler. It should have different output
        // translated by the offset.
        final double offset = 10;
        for (int i = 0; i < dimension; i++) {
            c1[i] += offset;
            c2[i] += offset;
        }
        final BoxSampler sampler2 = BoxSampler.of(rng2, c1, c2);
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
}
