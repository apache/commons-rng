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
import java.util.function.DoubleUnaryOperator;
import org.junit.jupiter.api.Assertions;
import org.junit.Test;

import org.apache.commons.math3.stat.inference.ChiSquareTest;

import org.apache.commons.rng.UniformRandomProvider;
import org.apache.commons.rng.core.source64.SplitMix64;
import org.apache.commons.rng.sampling.RandomAssert;
import org.apache.commons.rng.simple.RandomSource;

/**
 * Test for {@link UnitBallSampler}.
 */
public class UnitBallSamplerTest {
    /**
     * Test a non-positive dimension.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testInvalidDimensionThrows() {
        final UniformRandomProvider rng = RandomSource.SPLIT_MIX_64.create(0L);
        UnitBallSampler.of(rng, 0);
    }

    /**
     * Test the distribution of points in one dimension.
     */
    @Test
    public void testDistribution1D() {
        testDistributionND(1);
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
     * Test the distribution of points in five dimensions.
     */
    @Test
    public void testDistribution5D() {
        testDistributionND(5);
    }

    /**
     * Test the distribution of points in six dimensions.
     */
    @Test
    public void testDistribution6D() {
        testDistributionND(6);
    }

    /**
     * Test the distribution of points in n dimensions. The output coordinates
     * should be uniform in the unit n-ball. The unit n-ball is divided into inner
     * n-balls. The radii of the internal n-balls are varied to ensure that successive layers
     * have the same volume. This assigns each coordinate to an inner n-ball layer and an
     * orthant using the sign bits of the coordinates. The number of samples in each bin
     * should be the same.
     *
     * @see <a href="https://en.wikipedia.org/wiki/Volume_of_an_n-ball">Volume of an n-ball</a>
     * @see <a href="https://en.wikipedia.org/wiki/Orthant">Orthant</a>
     */
    private static void testDistributionND(int dimension) {
        // The number of inner layers and samples has been selected by trial and error using
        // random seeds and multiple runs to ensure correctness of the test (i.e. it fails with
        // approximately the fraction expected for the test p-value).
        // A fixed seed is used to make the test suite robust.
        final int layers = 10;
        final int samplesPerBin = 20;
        final int orthants = 1 << dimension;

        // Compute the radius for each layer to have the same volume.
        final double volume = createVolumeFunction(dimension).applyAsDouble(1);
        final DoubleUnaryOperator radius = createRadiusFunction(dimension);
        final double[] r = new double[layers];
        for (int i = 1; i < layers; i++) {
            r[i - 1] = radius.applyAsDouble(volume * ((double) i / layers));
        }
        // The final radius should be 1.0. Any coordinates with a radius above 1
        // should fail so explicitly set the value as 1.
        r[layers - 1] = 1.0;

        // Expect a uniform distribution
        final double[] expected = new double[layers * orthants];
        final int samples = samplesPerBin * expected.length;
        Arrays.fill(expected, (double) samples / layers);

        // Increase the loops and use a null seed (i.e. randomly generated) to verify robustness
        final UniformRandomProvider rng = RandomSource.XO_SHI_RO_512_PP.create(0xa1b2c3d4L);
        final UnitBallSampler sampler = UnitBallSampler.of(rng, dimension);
        for (int loop = 0; loop < 1; loop++) {
            // Assign each coordinate to a layer inside the ball and an orthant using the sign
            final long[] observed = new long[layers * orthants];
            NEXT:
            for (int i = 0; i < samples; i++) {
                final double[] v = sampler.sample();
                final double length = length(v);
                for (int layer = 0; layer < layers; layer++) {
                    if (length <= r[layer]) {
                        final int orthant = orthant(v);
                        observed[layer * orthants + orthant]++;
                        continue NEXT;
                    }
                }
                // Radius above 1
                Assertions.fail("Invalid sample length: " + length);
            }
            final double p = new ChiSquareTest().chiSquareTest(expected, observed);
            Assertions.assertFalse(p < 0.001, () -> "p-value too small: " + p);
        }
    }

    /**
     * Test the edge case where the normalisation sum to divide by is zero for 3D.
     */
    @Test
    public void testInvalidInverseNormalisation3D() {
        testInvalidInverseNormalisationND(3);
    }

    /**
     * Test the edge case where the normalisation sum to divide by is zero for 4D.
     */
    @Test
    public void testInvalidInverseNormalisation4D() {
        testInvalidInverseNormalisationND(4);
    }

    /**
     * Test the edge case where the normalisation sum to divide by is zero.
     * This test requires generation of Gaussian samples with the value 0.
     */
    private static void testInvalidInverseNormalisationND(final int dimension) {
        // Create a provider that will create a bad first sample but then recover.
        // This checks recursion will return a good value.
        final UniformRandomProvider bad = new SplitMix64(0x1a2b3cL) {
            private int count = -2 * dimension;

            @Override
            public long nextLong() {
                // Return enough zeros to create Gaussian samples of zero for all coordinates.
                return count++ < 0 ? 0 : super.nextLong();
            }
        };

        final double[] vector = UnitBallSampler.of(bad, dimension).sample();
        Assertions.assertEquals(dimension, vector.length);
        // A non-zero coordinate should occur with a SplitMix which returns 0 only once.
        Assertions.assertNotEquals(0.0, length(vector));
    }

    /**
     * Test the SharedStateSampler implementation for 1D.
     */
    @Test
    public void testSharedStateSampler1D() {
        testSharedStateSampler(1);
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
        final UnitBallSampler sampler1 = UnitBallSampler.of(rng1, dimension);
        final UnitBallSampler sampler2 = sampler1.withUniformRandomProvider(rng2);
        RandomAssert.assertProduceSameSequence(sampler1, sampler2);
    }

    /**
     * @return the length (L2-norm) of given vector.
     */
    private static double length(double[] vector) {
        double total = 0;
        for (double d : vector) {
            total += d * d;
        }
        return Math.sqrt(total);
    }

    /**
     * Assign an orthant to the vector using the sign of each component.
     * The i<sup>th</sup> bit is set in the orthant for the i<sup>th</sup> component
     * if the component is negative.
     *
     * @return the orthant in the range [0, vector.length)
     * @see <a href="https://en.wikipedia.org/wiki/Orthant">Orthant</a>
     */
    private static int orthant(double[] vector) {
        int orthant = 0;
        for (int i = 0; i < vector.length; i++) {
            if (vector[i] < 0) {
                orthant |= 1 << i;
            }
        }
        return orthant;
    }

    /**
     * Check the n-ball volume functions can map the radius to the volume and back.
     * These functions are used to divide the n-ball into uniform volume bins to test sampling
     * within the n-ball.
     */
    @Test
    public void checkVolumeFunctions() {
        final double[] radii = {0, 0.1, 0.25, 0.5, 0.75, 1.0};
        for (int n = 1; n <= 6; n++) {
            final DoubleUnaryOperator volume = createVolumeFunction(n);
            final DoubleUnaryOperator radius = createRadiusFunction(n);
            for (final double r : radii) {
                Assertions.assertEquals(r, radius.applyAsDouble(volume.applyAsDouble(r)), 1e-10);
            }
        }
    }

    /**
     * Creates a function to compute the volume of a ball of the given dimension
     * from the radius.
     *
     * @param dimension the dimension
     * @return the volume function
     * @see <a href="https://en.wikipedia.org/wiki/Volume_of_an_n-ball">Volume of an n-ball</a>
     */
    private static DoubleUnaryOperator createVolumeFunction(final int dimension) {
        if (dimension == 1) {
            return r -> r * 2;
        } else if (dimension == 2) {
            return r -> Math.PI * r * r;
        } else if (dimension == 3) {
            final double factor = 4 * Math.PI / 3;
            return r -> factor * Math.pow(r, 3);
        } else if (dimension == 4) {
            final double factor = Math.PI * Math.PI / 2;
            return r -> factor * Math.pow(r, 4);
        } else if (dimension == 5) {
            final double factor = 8 * Math.PI * Math.PI / 15;
            return r -> factor * Math.pow(r, 5);
        } else if (dimension == 6) {
            final double factor = Math.pow(Math.PI, 3) / 6;
            return r -> factor * Math.pow(r, 6);
        }
        throw new IllegalStateException("Unsupported dimension: " + dimension);
    }

    /**
     * Creates a function to compute the radius of a ball of the given dimension
     * from the volume.
     *
     * @param dimension the dimension
     * @return the radius function
     * @see <a href="https://en.wikipedia.org/wiki/Volume_of_an_n-ball">Volume of an n-ball</a>
     */
    private static DoubleUnaryOperator createRadiusFunction(final int dimension) {
        if (dimension == 1) {
            return v -> v * 0.5;
        } else if (dimension == 2) {
            return v -> Math.sqrt(v / Math.PI);
        } else if (dimension == 3) {
            final double factor = 3.0 / (4 * Math.PI);
            return v -> Math.cbrt(v * factor);
        } else if (dimension == 4) {
            final double factor = 2.0 / (Math.PI * Math.PI);
            return v -> Math.pow(v * factor, 0.25);
        } else if (dimension == 5) {
            final double factor = 15.0 / (8 * Math.PI * Math.PI);
            return v -> Math.pow(v * factor, 0.2);
        } else if (dimension == 6) {
            final double factor = 6.0 / Math.pow(Math.PI, 3);
            return v -> Math.pow(v * factor, 1.0 / 6);
        }
        throw new IllegalStateException("Unsupported dimension: " + dimension);
    }
}
