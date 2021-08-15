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

import org.junit.jupiter.api.Assertions;
import org.junit.Test;
import org.apache.commons.rng.simple.RandomSource;
import java.util.Arrays;
import org.apache.commons.math3.stat.inference.ChiSquareTest;
import org.apache.commons.rng.UniformRandomProvider;
import org.apache.commons.rng.core.source64.SplitMix64;

/**
 * Test for {@link UnitSphereSampler}.
 */
public class UnitSphereSamplerTest {
    /** 2 pi */
    private static final double TWO_PI = 2 * Math.PI;

    /**
     * Test a non-positive dimension.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testInvalidDimensionThrows() {
        // Use instance constructor not factory constructor to exercise 1.X public API
        new UnitSphereSampler(0, null);
    }

    /**
     * Test a non-positive dimension.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testInvalidDimensionThrowsWithFactoryConstructor() {
        UnitSphereSampler.of(null, 0);
    }

    /**
     * Test the distribution of points in one dimension.
     */
    @Test
    public void testDistribution1D() {
        testDistribution1D(false);
    }

    /**
     * Test the distribution of points in one dimension with the factory constructor.
     */
    @Test
    public void testDistribution1DWithFactoryConstructor() {
        testDistribution1D(true);
    }

    /**
     * Test the distribution of points in one dimension.
     * RNG-130: All samples should be 1 or -1.
     */
    private static void testDistribution1D(boolean factoryConstructor) {
        final UniformRandomProvider rng = RandomSource.XO_RO_SHI_RO_128_PP.create(0x1a2b3cL);
        final UnitSphereSampler generator = createUnitSphereSampler(1, rng, factoryConstructor);
        final int samples = 10000;
        // Count the negatives.
        int count = 0;
        for (int i = 0; i < samples; i++) {
            // Test the deprecated method once in the test suite.
            @SuppressWarnings("deprecation")
            final double[] v = generator.nextVector();
            Assertions.assertEquals(1, v.length);
            final double d = v[0];
            if (d == -1.0) {
                count++;
            } else if (d != 1.0) {
                // RNG-130: All samples should be 1 or -1.
                Assertions.fail("Invalid unit length: " + d);
            }
        }
        // Test the number of negatives is approximately 50%
        assertMonobit(count, samples);
    }

    /**
     * Assert that the number of 1 bits is approximately 50%. This is based upon a fixed-step "random
     * walk" of +1/-1 from zero.
     *
     * <p>The test is equivalent to the NIST Monobit test with a fixed p-value of 0.01. The number of
     * bits is recommended to be above 100.</p>
     *
     * @see <a href="https://csrc.nist.gov/publications/detail/sp/800-22/rev-1a/final">Bassham, et al
     *      (2010) NIST SP 800-22: A Statistical Test Suite for Random and Pseudorandom Number
     *      Generators for Cryptographic Applications. Section 2.1.</a>
     *
     * @param bitCount The bit count.
     * @param numberOfBits Number of bits.
     */
    private static void assertMonobit(int bitCount, int numberOfBits) {
        // Convert the bit count into a number of +1/-1 steps.
        final double sum = 2.0 * bitCount - numberOfBits;
        // The reference distribution is Normal with a standard deviation of sqrt(n).
        // Check the absolute position is not too far from the mean of 0 with a fixed
        // p-value of 0.01 taken from a 2-tailed Normal distribution. Computation of
        // the p-value requires the complimentary error function.
        final double absSum = Math.abs(sum);
        final double max = Math.sqrt(numberOfBits) * 2.576;
        Assertions.assertTrue(absSum <= max,
            () -> "Walked too far astray: " + absSum + " > " + max +
                  " (test will fail randomly about 1 in 100 times)");
    }

    /**
     * Test the distribution of points in two dimensions.
     */
    @Test
    public void testDistribution2D() {
        testDistribution2D(false);
    }

    /**
     * Test the distribution of points in two dimensions with the factory constructor.
     */
    @Test
    public void testDistribution2DWithFactoryConstructor() {
        testDistribution2D(true);
    }

    /**
     * Test the distribution of points in two dimensions.
     * Obtains polar coordinates and checks the angle distribution is uniform.
     */
    private static void testDistribution2D(boolean factoryConstructor) {
        final UniformRandomProvider rng = RandomSource.XOR_SHIFT_1024_S_PHI.create(17399225432L);
        final UnitSphereSampler generator = createUnitSphereSampler(2, rng, factoryConstructor);

        // In 2D, angles with a given vector should be uniformly distributed.
        final int angleBins = 200;
        final long[] observed = new long[angleBins];
        final int steps = 100000;
        for (int i = 0; i < steps; ++i) {
            final double[] v = generator.sample();
            Assertions.assertEquals(2, v.length);
            Assertions.assertEquals(1.0, length(v), 1e-10);
            // Get the polar angle bin from xy
            final int angleBin = angleBin(angleBins, v[0], v[1]);
            observed[angleBin]++;
        }

        final double[] expected = new double[observed.length];
        Arrays.fill(expected, (double) steps / observed.length);
        final double p = new ChiSquareTest().chiSquareTest(expected, observed);
        Assertions.assertFalse(p < 0.01, () -> "p-value too small: " + p);
    }

    /**
     * Test the distribution of points in three dimensions.
     */
    @Test
    public void testDistribution3D() {
        testDistribution3D(false);
    }

    /**
     * Test the distribution of points in three dimensions with the factory constructor.
     */
    @Test
    public void testDistribution3DWithFactoryConstructor() {
        testDistribution3D(true);
    }

    /**
     * Test the distribution of points in three dimensions.
     * Obtains spherical coordinates and checks the distribution is uniform.
     */
    private static void testDistribution3D(boolean factoryConstructor) {
        final UniformRandomProvider rng = RandomSource.XO_SHI_RO_256_PP.create(0xabcdefL);
        final UnitSphereSampler generator = createUnitSphereSampler(3, rng, factoryConstructor);

        // Get 3D spherical coordinates. Assign to a bin.
        //
        // polar angle (theta) in [0, 2pi)
        // azimuthal angle (phi) in [0, pi)
        //
        // theta = arctan(y/x) and is uniformly distributed
        // phi = arccos(z); and cos(phi) is uniformly distributed
        final int angleBins = 20;
        final int depthBins = 10;
        final long[] observed = new long[angleBins * depthBins];
        final int steps = 1000000;
        for (int i = 0; i < steps; ++i) {
            final double[] v = generator.sample();
            Assertions.assertEquals(3, v.length);
            Assertions.assertEquals(1.0, length(v), 1e-10);
            // Get the polar angle bin from xy
            final int angleBin = angleBin(angleBins, v[0], v[1]);
            // Map cos(phi) = z from [-1, 1) to [0, 1) then assign a bin
            final int depthBin = (int) (depthBins * (v[2] + 1) / 2);
            observed[depthBin * angleBins + angleBin]++;
        }

        final double[] expected = new double[observed.length];
        Arrays.fill(expected, (double) steps / observed.length);
        final double p = new ChiSquareTest().chiSquareTest(expected, observed);
        Assertions.assertFalse(p < 0.01, () -> "p-value too small: " + p);
    }

    /**
     * Test the distribution of points in four dimensions.
     */
    @Test
    public void testDistribution4D() {
        testDistribution4D(false);
    }

    /**
     * Test the distribution of points in four dimensions with the factory constructor.
     */
    @Test
    public void testDistribution4DWithFactoryConstructor() {
        testDistribution4D(true);
    }

    /**
     * Test the distribution of points in four dimensions.
     * Checks the surface of the 3-sphere can be used to generate uniform samples within a circle.
     */
    private static void testDistribution4D(boolean factoryConstructor) {
        final UniformRandomProvider rng = RandomSource.XO_SHI_RO_512_PP.create(0x9876543210L);
        final UnitSphereSampler generator = createUnitSphereSampler(4, rng, factoryConstructor);

        // No uniform distribution of spherical coordinates for a 3-sphere.
        // https://en.wikipedia.org/wiki/N-sphere#Spherical_coordinates
        // Here we exploit the fact that the uniform distribution of a (n+1)-sphere
        // when discarding two coordinates is uniform within a n-ball.
        // Thus any two coordinates of the 4 are uniform within a circle.
        // Here we separately test pairs (0, 1) and (2, 3).
        // Note: We cannot create a single bin from the two bins in each circle as they are
        // not independent. A point close to the edge in one circle requires a point close to
        // the centre in the other circle to create a unit radius from all 4 coordinates.
        // This test exercises the N-dimension sampler and demonstrates the vectors obey
        // properties of the (n+1)-sphere.

        // Divide the circle into layers of concentric rings and an angle.
        final int layers = 10;
        final int angleBins = 20;

        // Compute the radius for each layer to have the same area
        // (i.e. incrementally larger concentric circles must increase area by a constant).
        // r = sqrt(fraction * maxArea / pi)
        // Unit circle has area pi so we just use sqrt(fraction).
        final double[] r = new double[layers];
        for (int i = 1; i < layers; i++) {
            r[i - 1] = Math.sqrt((double) i / layers);
        }
        // The final radius should be 1.0.
        r[layers - 1] = 1.0;

        final long[] observed1 = new long[layers * angleBins];
        final long[] observed2 = new long[observed1.length];
        final int steps = 1000000;
        for (int i = 0; i < steps; ++i) {
            final double[] v = generator.sample();
            Assertions.assertEquals(4, v.length);
            Assertions.assertEquals(1.0, length(v), 1e-10);
            // Circle 1
            observed1[circleBin(angleBins, r, v[0], v[1])]++;
            // Circle 2
            observed2[circleBin(angleBins, r, v[2], v[3])]++;
        }

        final double[] expected = new double[observed1.length];
        Arrays.fill(expected, (double) steps / observed1.length);
        final ChiSquareTest chi = new ChiSquareTest();
        final double p1 = chi.chiSquareTest(expected, observed1);
        Assertions.assertFalse(p1 < 0.01, () -> "Circle 1 p-value too small: " + p1);
        final double p2 = chi.chiSquareTest(expected, observed2);
        Assertions.assertFalse(p2 < 0.01, () -> "Circle 2 p-value too small: " + p2);
    }

    /**
     * Compute a bin inside the circle using the polar angle theta and the radius thresholds.
     *
     * @param angleBins the number of angle bins
     * @param r the radius bin thresholds (ascending order)
     * @param x the x coordinate
     * @param y the y coordinate
     * @return the bin
     */
    private static int circleBin(int angleBins, double[] r, double x, double y) {
        final int angleBin = angleBin(angleBins, x, y);
        final int radiusBin = radiusBin(r, x, y);
        return radiusBin * angleBins + angleBin;
    }

    /**
     * Compute an angle bin from the xy vector. The bin will represent the range [0, 2pi).
     *
     * @param angleBins the number of angle bins
     * @param x the x coordinate
     * @param y the y coordinate
     * @return the bin
     */
    private static int angleBin(int angleBins, double x, double y) {
        final double angle = Math.atan2(y, x);
        // Map [-pi, pi) to [0, 1) then assign a bin
        return (int) (angleBins * (angle + Math.PI) / TWO_PI);
    }

    /**
     * Compute a radius bin from the xy vector. The bin is assigned if the length of the vector
     * is above the threshold of the bin.
     *
     * @param r the radius bin thresholds (ascending order)
     * @param x the x coordinate
     * @param y the y coordinate
     * @return the bin
     */
    private static int radiusBin(double[] r, double x, double y) {
        final double length = Math.sqrt(x * x + y * y);

        // Note: The bin should be uniformly distributed.
        // A test using a custom binary search (avoiding double NaN checks)
        // shows the simple loop over a small number of bins is comparable in speed.
        // The loop is preferred for simplicity. A binary search may be better
        // if the number of bins is increased.
        for (int layer = 0; layer < r.length; layer++) {
            if (length <= r[layer]) {
                return layer;
            }
        }
        // Unreachable if the xy component is from a vector of length <= 1
        throw new AssertionError("Invalid sample length: " + length);
    }

    /**
     * Test infinite recursion occurs with a bad provider in 2D.
     */
    @Test(expected = StackOverflowError.class)
    public void testBadProvider2D() {
        testBadProvider(2);
    }

    /**
     * Test infinite recursion occurs with a bad provider in 3D.
     */
    @Test(expected = StackOverflowError.class)
    public void testBadProvider3D() {
        testBadProvider(3);
    }

    /**
     * Test infinite recursion occurs with a bad provider in 4D.
     */
    @Test(expected = StackOverflowError.class)
    public void testBadProvider4D() {
        testBadProvider(4);
    }

    /**
     * Test the edge case where the normalisation sum to divide by is always zero.
     * This test requires generation of Gaussian samples with the value 0.
     * The sample eventually fails due to infinite recursion.
     * See RNG-55.
     *
     * @param dimension the dimension
     */
    private static void testBadProvider(final int dimension) {
        // A provider that will create zero valued Gaussian samples
        // from the ZigguratNormalizedGaussianSampler.
        final UniformRandomProvider bad = new SplitMix64(0L) {
            @Override
            public long nextLong() {
                return 0;
            }
        };

        UnitSphereSampler.of(bad, dimension).sample();
    }

    /**
     * Test the edge case where the normalisation sum to divide by is zero for 2D.
     */
    @Test
    public void testInvalidInverseNormalisation2D() {
        testInvalidInverseNormalisationND(2);
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
     * See RNG-55.
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

        final double[] vector = UnitSphereSampler.of(bad, dimension).sample();
        Assertions.assertEquals(dimension, vector.length);
        Assertions.assertEquals(1.0, length(vector), 1e-10);
    }

    /**
     * Test to demonstrate that using floating-point equality of the norm squared with
     * zero is valid. Any norm squared after zero should produce a valid scaling factor.
     */
    @Test
    public void testNextNormSquaredAfterZeroIsValid() {
        // The sampler explicitly handles length == 0 using recursion.
        // Anything above zero should be valid.
        final double normSq = Math.nextUp(0.0);
        // Map to the scaling factor
        final double f = 1 / Math.sqrt(normSq);
        // As long as this is finite positive then the sampler is valid
        Assertions.assertTrue(f > 0 && f <= Double.MAX_VALUE);
    }

    /**
     * Test the SharedStateSampler implementation for 1D.
     */
    @Test
    public void testSharedStateSampler1D() {
        testSharedStateSampler(1, false);
    }

    /**
     * Test the SharedStateSampler implementation for 2D.
     */
    @Test
    public void testSharedStateSampler2D() {
        testSharedStateSampler(2, false);
    }

    /**
     * Test the SharedStateSampler implementation for 3D.
     */
    @Test
    public void testSharedStateSampler3D() {
        testSharedStateSampler(3, false);
    }

    /**
     * Test the SharedStateSampler implementation for 4D.
     */
    @Test
    public void testSharedStateSampler4D() {
        testSharedStateSampler(4, false);
    }

    /**
     * Test the SharedStateSampler implementation for 1D using the factory constructor.
     */
    @Test
    public void testSharedStateSampler1DWithFactoryConstructor() {
        testSharedStateSampler(1, true);
    }

    /**
     * Test the SharedStateSampler implementation for 2D using the factory constructor.
     */
    @Test
    public void testSharedStateSampler2DWithFactoryConstructor() {
        testSharedStateSampler(2, true);
    }

    /**
     * Test the SharedStateSampler implementation for 3D using the factory constructor.
     */
    @Test
    public void testSharedStateSampler3DWithFactoryConstructor() {
        testSharedStateSampler(3, true);
    }

    /**
     * Test the SharedStateSampler implementation for 4D using the factory constructor.
     */
    @Test
    public void testSharedStateSampler4DWithFactoryConstructor() {
        testSharedStateSampler(4, true);
    }

    /**
     * Test the SharedStateSampler implementation for the given dimension.
     *
     * @param dimension the dimension
     * @param factoryConstructor true to use the factory constructor
     */
    private static void testSharedStateSampler(int dimension, boolean factoryConstructor) {
        final UniformRandomProvider rng1 = RandomSource.SPLIT_MIX_64.create(0L);
        final UniformRandomProvider rng2 = RandomSource.SPLIT_MIX_64.create(0L);
        final UnitSphereSampler sampler1 = createUnitSphereSampler(dimension, rng1, factoryConstructor);
        final UnitSphereSampler sampler2 = sampler1.withUniformRandomProvider(rng2);
        RandomAssert.assertProduceSameSequence(sampler1, sampler2);
    }

    /**
     * Creates a UnitSphereSampler.
     *
     * @param dimension the dimension
     * @param rng the source of randomness
     * @param factoryConstructor true to use the factory constructor
     * @return the sampler
     */
    private static UnitSphereSampler createUnitSphereSampler(int dimension, UniformRandomProvider rng,
            boolean factoryConstructor) {
        return factoryConstructor ?
                UnitSphereSampler.of(rng, dimension) : new UnitSphereSampler(dimension, rng);
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
}
