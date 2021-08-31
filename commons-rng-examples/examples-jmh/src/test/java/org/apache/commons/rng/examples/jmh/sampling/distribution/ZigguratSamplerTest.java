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
package org.apache.commons.rng.examples.jmh.sampling.distribution;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.Arrays;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.apache.commons.math3.distribution.AbstractRealDistribution;
import org.apache.commons.math3.distribution.ExponentialDistribution;
import org.apache.commons.math3.distribution.NormalDistribution;
import org.apache.commons.math3.stat.inference.ChiSquareTest;
import org.apache.commons.rng.RestorableUniformRandomProvider;
import org.apache.commons.rng.UniformRandomProvider;
import org.apache.commons.rng.sampling.distribution.ContinuousSampler;
import org.apache.commons.rng.simple.RandomSource;

/**
 * Test for ziggurat samplers in the {@link ZigguratSamplerPerformance} class.
 *
 * <p>This test is copied from the {@code commons-rng-sampling} module to ensure all implementations
 * correctly sample from the distribution.
 */
class ZigguratSamplerTest {

    /**
     * The seed for the RNG used in the distribution sampling tests.
     *
     * <p>This has been chosen to allow the test to pass with all generators.
     * Set to null test with a random seed.
     *
     * <p>Note that the p-value of the chi-square test is 0.001. There are multiple assertions
     * per test and multiple samplers. The total number of chi-square tests is above 100
     * and failure of a chosen random seed on a few tests is common. When using a random
     * seed re-run the test multiple times. Systematic failure of the same sampler
     * should be investigated further.
     */
    private static final Long SEED = 0xd1342543de82ef95L;

    /**
     * Create arguments with the name of the factory.
     *
     * @param name Name of the factory
     * @param factory Factory to create the sampler
     * @return the arguments
     */
    private static Arguments args(String name) {
        // Create the factory.
        // Here we delegate to the static method used to create all the samplers for testing.
        final Function<UniformRandomProvider, ContinuousSampler> factory =
            rng -> ZigguratSamplerPerformance.Sources.createSampler(name, rng);
        return Arguments.of(name, factory);
    }

    /**
     * Create a stream of constructors of a Gaussian sampler.
     *
     * <p>Note: This method exists to allow this test to be duplicated in the examples JMH
     * module where many implementations are tested.
     *
     * @return the stream of constructors
     */
    private static Stream<Arguments> gaussianSamplers() {
        // Test all but MOD_GAUSSIAN (tested in the common-rng-sampling module)
        return Stream.of(
            args(ZigguratSamplerPerformance.GAUSSIAN_128),
            args(ZigguratSamplerPerformance.GAUSSIAN_256),
            args(ZigguratSamplerPerformance.MOD_GAUSSIAN2),
            args(ZigguratSamplerPerformance.MOD_GAUSSIAN_SIMPLE_OVERHANGS),
            args(ZigguratSamplerPerformance.MOD_GAUSSIAN_INLINING),
            args(ZigguratSamplerPerformance.MOD_GAUSSIAN_INLINING_SHIFT),
            args(ZigguratSamplerPerformance.MOD_GAUSSIAN_INLINING_SIMPLE_OVERHANGS),
            args(ZigguratSamplerPerformance.MOD_GAUSSIAN_INT_MAP),
            args(ZigguratSamplerPerformance.MOD_GAUSSIAN_E_MAX_TABLE),
            args(ZigguratSamplerPerformance.MOD_GAUSSIAN_E_MAX_2),
            args(ZigguratSamplerPerformance.MOD_GAUSSIAN_512));
    }

    /**
     * Create a stream of constructors of an exponential sampler.
     *
     * <p>Note: This method exists to allow this test to be duplicated in the examples JMH
     * module where many implementations are tested.
     *
     * @return the stream of constructors
     */
    private static Stream<Arguments> exponentialSamplers() {
        // Test all but MOD_EXPONENTIAL (tested in the common-rng-sampling module)
        return Stream.of(
                args(ZigguratSamplerPerformance.EXPONENTIAL),
                args(ZigguratSamplerPerformance.MOD_EXPONENTIAL2),
                args(ZigguratSamplerPerformance.MOD_EXPONENTIAL_SIMPLE_OVERHANGS),
                args(ZigguratSamplerPerformance.MOD_EXPONENTIAL_INLINING),
                args(ZigguratSamplerPerformance.MOD_EXPONENTIAL_LOOP),
                args(ZigguratSamplerPerformance.MOD_EXPONENTIAL_LOOP2),
                args(ZigguratSamplerPerformance.MOD_EXPONENTIAL_RECURSION),
                args(ZigguratSamplerPerformance.MOD_EXPONENTIAL_INT_MAP),
                args(ZigguratSamplerPerformance.MOD_EXPONENTIAL_E_MAX_TABLE),
                args(ZigguratSamplerPerformance.MOD_EXPONENTIAL_E_MAX_2),
                args(ZigguratSamplerPerformance.MOD_EXPONENTIAL_512));
    }

    // -------------------------------------------------------------------------
    // All code below here is copied from:
    // commons-rng-sampling/src/test/java/org/apache/commons/rng/sampling/distribution/ZigguratSamplerTest.java
    // -------------------------------------------------------------------------

    /**
     * Creates the gaussian distribution.
     *
     * @return the distribution
     */
    private static AbstractRealDistribution createGaussianDistribution() {
        return new NormalDistribution(null, 0.0, 1.0);
    }

    /**
     * Creates the exponential distribution.
     *
     * @return the distribution
     */
    private static AbstractRealDistribution createExponentialDistribution() {
        return new ExponentialDistribution(null, 1.0);
    }

    /**
     * Test Gaussian samples using a large number of bins based on uniformly spaced
     * quantiles. Added for RNG-159.
     *
     * @param name Name of the sampler
     * @param factory Factory to create the sampler
     */
    @ParameterizedTest(name = "{index} => {0}")
    @MethodSource("gaussianSamplers")
    void testGaussianSamplesWithQuantiles(String name, Function<UniformRandomProvider, ContinuousSampler> factory) {
        final int bins = 2000;
        final AbstractRealDistribution dist = createGaussianDistribution();
        final double[] quantiles = new double[bins];
        for (int i = 0; i < bins; i++) {
            quantiles[i] = dist.inverseCumulativeProbability((i + 1.0) / bins);
        }
        testSamples(quantiles, factory, ZigguratSamplerTest::createGaussianDistribution,
            // Test positive and negative ranges to check symmetry.
            // Smallest layer is 0.292 (convex region)
            new double[] {0, 0.2},
            new double[] {-0.35, -0.1},
            // Around the mean
            new double[] {-0.1, 0.1},
            new double[] {-0.4, 0.6},
            // Inflection point at x=1
            new double[] {-1.1, -0.9},
            // A concave region
            new double[] {2.1, 2.5},
            // Tail = 3.64
            new double[] {2.5, 8});
    }

    /**
     * Test Gaussian samples using a large number of bins uniformly spaced in a range.
     * Added for RNG-159.
     *
     * @param name Name of the sampler
     * @param factory Factory to create the sampler
     */
    @ParameterizedTest(name = "{index} => {0}")
    @MethodSource("gaussianSamplers")
    void testGaussianSamplesWithUniformValues(String name, Function<UniformRandomProvider, ContinuousSampler> factory) {
        final int bins = 2000;
        final double[] values = new double[bins];
        final double minx = -8;
        final double maxx = 8;
        // Bin width = 16 / 2000 = 0.008
        for (int i = 0; i < bins; i++) {
            values[i] = minx + (maxx - minx) * (i + 1.0) / bins;
        }
        // Ensure upper bound is the support limit
        values[bins - 1] = Double.POSITIVE_INFINITY;
        testSamples(values, factory, ZigguratSamplerTest::createGaussianDistribution,
            // Test positive and negative ranges to check symmetry.
            // Smallest layer is 0.292 (convex region)
            new double[] {0, 0.2},
            new double[] {-0.35, -0.1},
            // Around the mean
            new double[] {-0.1, 0.1},
            new double[] {-0.4, 0.6},
            // Inflection point at x=1
            new double[] {-1.01, -0.99},
            new double[] {0.98, 1.03},
            // A concave region
            new double[] {1.03, 1.05},
            // Tail = 3.64
            new double[] {3.6, 3.8},
            new double[] {3.7, 8});
    }

    /**
     * Test exponential samples using a large number of bins based on uniformly spaced quantiles.
     *
     * @param name Name of the sampler
     * @param factory Factory to create the sampler
     */
    @ParameterizedTest(name = "{index} => {0}")
    @MethodSource("exponentialSamplers")
    void testExponentialSamplesWithQuantiles(String name, Function<UniformRandomProvider, ContinuousSampler> factory) {
        final int bins = 2000;
        final AbstractRealDistribution dist = createExponentialDistribution();
        final double[] quantiles = new double[bins];
        for (int i = 0; i < bins; i++) {
            quantiles[i] = dist.inverseCumulativeProbability((i + 1.0) / bins);
        }
        testSamples(quantiles, factory, ZigguratSamplerTest::createExponentialDistribution,
            // Smallest layer is 0.122
            new double[] {0, 0.1},
            new double[] {0.05, 0.15},
            // Around the mean
            new double[] {0.9, 1.1},
            // Tail = 7.57
            new double[] {1.5, 12});
    }

    /**
     * Test exponential samples using a large number of bins uniformly spaced in a range.
     *
     * @param name Name of the sampler
     * @param factory Factory to create the sampler
     */
    @ParameterizedTest(name = "{index} => {0}")
    @MethodSource("exponentialSamplers")
    void testExponentialSamplesWithUniformValues(String name, Function<UniformRandomProvider, ContinuousSampler> factory) {
        final int bins = 2000;
        final double[] values = new double[bins];
        final double minx = 0;
        // Enter the tail of the distribution
        final double maxx = 12;
        // Bin width = 12 / 2000 = 0.006
        for (int i = 0; i < bins; i++) {
            values[i] = minx + (maxx - minx) * (i + 1.0) / bins;
        }
        // Ensure upper bound is the support limit
        values[bins - 1] = Double.POSITIVE_INFINITY;

        testSamples(values, factory,  ZigguratSamplerTest::createExponentialDistribution,
            // Smallest layer is 0.122
            new double[] {0, 0.1},
            new double[] {0.05, 0.15},
            // Around the mean
            new double[] {0.9, 1.1},
            // Tail = 7.57
            new double[] {7.5, 7.7},
            new double[] {7.7, 12});
    }

    /**
     * Test samples using the provided bins. Values correspond to the bin upper
     * limit. It is assumed the values span most of the distribution. Additional
     * tests are performed using a region of the distribution sampled.
     *
     * @param values Bin upper limits
     * @param factory Factory to create the sampler
     * @param distribution The distribution under test
     * @param ranges Ranges of the distribution to test
     */
    private static void testSamples(double[] values,
                                    Function<UniformRandomProvider, ContinuousSampler> factory,
                                    Supplier<AbstractRealDistribution> distribution,
                                    double[]... ranges) {
        final int bins = values.length;

        final int samples = 10000000;
        final long[] observed = new long[bins];
        final RestorableUniformRandomProvider rng = RandomSource.XO_SHI_RO_128_PP.create(SEED);
        final ContinuousSampler sampler = factory.apply(rng);
        for (int i = 0; i < samples; i++) {
            final double x = sampler.sample();
            final int index = findIndex(values, x);
            observed[index]++;
        }

        // Compute expected
        final AbstractRealDistribution dist = distribution.get();
        final double[] expected = new double[bins];
        double x0 = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < bins; i++) {
            final double x1 = values[i];
            expected[i] = dist.probability(x0, x1);
            x0 = x1;
        }

        final double significanceLevel = 0.001;

        final double lowerBound = dist.getSupportLowerBound();

        final ChiSquareTest chiSquareTest = new ChiSquareTest();
        // Pass if we cannot reject null hypothesis that the distributions are the same.
        final double pValue = chiSquareTest.chiSquareTest(expected, observed);
        Assertions.assertFalse(pValue < 0.001,
            () -> String.format("(%s <= x < %s) Chi-square p-value = %s",
                                lowerBound, values[bins - 1], pValue));

        // Test regions of the ziggurat.
        for (final double[] range : ranges) {
            final int min = findIndex(values, range[0]);
            final int max = findIndex(values, range[1]);
            // Must have a range of 2
            if (max - min + 1 < 2) {
                // This will probably occur if the quantiles test uses too small a range
                // for the tail. The tail is so far into the CDF that a single bin is
                // often used to represent it.
                Assertions.fail("Invalid range: " + Arrays.toString(range));
            }
            final long[] observed2 = Arrays.copyOfRange(observed, min, max + 1);
            final double[] expected2 = Arrays.copyOfRange(expected, min, max + 1);
            final double pValueB = chiSquareTest.chiSquareTest(expected2, observed2);
            Assertions.assertFalse(pValueB < significanceLevel,
                () -> String.format("(%s <= x < %s) Chi-square p-value = %s",
                                    min == 0 ? lowerBound : values[min - 1], values[max], pValueB));
        }
    }

    /**
     * Find the index of the value in the data such that:
     * <pre>
     * data[index - 1] <= x < data[index]
     * </pre>
     *
     * <p>This is a specialised binary search that assumes the bounds of the data are the
     * extremes of the support, and the upper support is infinite. Thus an index cannot
     * be returned as equal to the data length.
     *
     * @param data the data
     * @param x the value
     * @return the index
     */
    private static int findIndex(double[] data, double x) {
        int low = 0;
        int high = data.length - 1;

        // Bracket so that low is just above the value x
        while (low <= high) {
            final int mid = (low + high) >>> 1;
            final double midVal = data[mid];

            if (x < midVal) {
                // Reduce search range
                high = mid - 1;
            } else {
                // Set data[low] above the value
                low = mid + 1;
            }
        }
        // Verify the index is correct
        Assertions.assertTrue(x < data[low]);
        if (low != 0) {
            Assertions.assertTrue(x >= data[low - 1]);
        }
        return low;
    }
}
