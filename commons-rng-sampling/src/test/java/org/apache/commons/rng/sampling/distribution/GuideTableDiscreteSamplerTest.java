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
package org.apache.commons.rng.sampling.distribution;

import org.apache.commons.math3.distribution.BinomialDistribution;
import org.apache.commons.math3.distribution.PoissonDistribution;
import org.apache.commons.math3.stat.inference.ChiSquareTest;
import org.apache.commons.rng.UniformRandomProvider;
import org.apache.commons.rng.sampling.RandomAssert;
import org.apache.commons.rng.simple.RandomSource;
import org.junit.jupiter.api.Assertions;
import org.junit.Test;

/**
 * Test for the {@link GuideTableDiscreteSampler}.
 */
public class GuideTableDiscreteSamplerTest {
    @Test
    public void testConstructorThrowsWithNullProbabilites() {
        assertConstructorThrows(null, 1.0);
    }

    @Test
    public void testConstructorThrowsWithZeroLengthProbabilites() {
        assertConstructorThrows(new double[0], 1.0);
    }

    @Test
    public void testConstructorThrowsWithNegativeProbabilites() {
        assertConstructorThrows(new double[] {-1, 0.1, 0.2}, 1.0);
    }

    @Test
    public void testConstructorThrowsWithNaNProbabilites() {
        assertConstructorThrows(new double[] {0.1, Double.NaN, 0.2}, 1.0);
    }

    @Test
    public void testConstructorThrowsWithInfiniteProbabilites() {
        assertConstructorThrows(new double[] {0.1, Double.POSITIVE_INFINITY, 0.2}, 1.0);
    }

    @Test
    public void testConstructorThrowsWithInfiniteSumProbabilites() {
        assertConstructorThrows(new double[] {Double.MAX_VALUE, Double.MAX_VALUE}, 1.0);
    }

    @Test
    public void testConstructorThrowsWithZeroSumProbabilites() {
        assertConstructorThrows(new double[4], 1.0);
    }

    @Test
    public void testConstructorThrowsWithZeroAlpha() {
        assertConstructorThrows(new double[] {0.5, 0.5}, 0.0);
    }

    @Test
    public void testConstructorThrowsWithNegativeAlpha() {
        assertConstructorThrows(new double[] {0.5, 0.5}, -1.0);
    }

    /**
     * Assert the factory constructor throws and {@link IllegalArgumentException}.
     *
     * @param probabilities the probabilities
     * @param alpha the alpha
     */
    private static void assertConstructorThrows(double[] probabilities, double alpha) {
        final UniformRandomProvider rng = RandomSource.SPLIT_MIX_64.create(0L);
        Assertions.assertThrows(IllegalArgumentException.class,
            () -> GuideTableDiscreteSampler.of(rng, probabilities, alpha));
    }

    @Test
    public void testToString() {
        final UniformRandomProvider rng = RandomSource.SPLIT_MIX_64.create();
        final SharedStateDiscreteSampler sampler = GuideTableDiscreteSampler.of(rng, new double[] {0.5, 0.5}, 1.0);
        Assertions.assertTrue(sampler.toString().toLowerCase().contains("guide table"));
    }

    /**
     * Test sampling from a binomial distribution.
     */
    @Test
    public void testBinomialSamples() {
        final int trials = 67;
        final double probabilityOfSuccess = 0.345;
        final BinomialDistribution dist = new BinomialDistribution(null, trials, probabilityOfSuccess);
        final double[] expected = new double[trials + 1];
        for (int i = 0; i < expected.length; i++) {
            expected[i] = dist.probability(i);
        }
        checkSamples(expected, 1.0);
    }

    /**
     * Test sampling from a Poisson distribution.
     */
    @Test
    public void testPoissonSamples() {
        final double mean = 3.14;
        final PoissonDistribution dist = new PoissonDistribution(null, mean,
            PoissonDistribution.DEFAULT_EPSILON, PoissonDistribution.DEFAULT_MAX_ITERATIONS);
        final int maxN = dist.inverseCumulativeProbability(1 - 1e-6);
        final double[] expected = new double[maxN];
        for (int i = 0; i < expected.length; i++) {
            expected[i] = dist.probability(i);
        }
        checkSamples(expected, 1.0);
    }

    /**
     * Test sampling from a non-uniform distribution of probabilities (these sum to 1).
     */
    @Test
    public void testNonUniformSamplesWithProbabilities() {
        final double[] expected = {0.1, 0.2, 0.3, 0.1, 0.3};
        checkSamples(expected, 1.0);
    }

    /**
     * Test sampling from a non-uniform distribution of probabilities with an alpha smaller than
     * the default.
     */
    @Test
    public void testNonUniformSamplesWithProbabilitiesWithSmallAlpha() {
        final double[] expected = {0.1, 0.2, 0.3, 0.1, 0.3};
        checkSamples(expected, 0.1);
    }

    /**
     * Test sampling from a non-uniform distribution of probabilities with an alpha larger than
     * the default.
     */
    @Test
    public void testNonUniformSamplesWithProbabilitiesWithLargeAlpha() {
        final double[] expected = {0.1, 0.2, 0.3, 0.1, 0.3};
        checkSamples(expected, 10.0);
    }

    /**
     * Test sampling from a non-uniform distribution of observations (i.e. the sum is not 1 as per
     * probabilities).
     */
    @Test
    public void testNonUniformSamplesWithObservations() {
        final double[] expected = {1, 2, 3, 1, 3};
        checkSamples(expected, 1.0);
    }

    /**
     * Test sampling from a non-uniform distribution of probabilities (these sum to 1).
     * Extra zero-values are added.
     */
    @Test
    public void testNonUniformSamplesWithZeroProbabilities() {
        final double[] expected = {0.1, 0, 0.2, 0.3, 0.1, 0.3, 0};
        checkSamples(expected, 1.0);
    }

    /**
     * Test sampling from a non-uniform distribution of observations (i.e. the sum is not 1 as per
     * probabilities). Extra zero-values are added.
     */
    @Test
    public void testNonUniformSamplesWithZeroObservations() {
        final double[] expected = {1, 2, 3, 0, 1, 3, 0};
        checkSamples(expected, 1.0);
    }

    /**
     * Test sampling from a uniform distribution. This is an edge case where there
     * are no probabilities less than the mean.
     */
    @Test
    public void testUniformSamplesWithNoObservationLessThanTheMean() {
        final double[] expected = {2, 2, 2, 2, 2, 2};
        checkSamples(expected, 1.0);
    }

    /**
     * Check the distribution of samples match the expected probabilities.
     *
     * <p>If the expected probability is zero then this should never be sampled. The non-zero
     * probabilities are compared to the sample distribution using a Chi-square test.</p>
     *
     * @param probabilies the probabilities
     * @param alpha the alpha
     */
    private static void checkSamples(double[] probabilies, double alpha) {
        final UniformRandomProvider rng = RandomSource.JSF_64.create();
        final SharedStateDiscreteSampler sampler = GuideTableDiscreteSampler.of(rng, probabilies, alpha);

        final int numberOfSamples = 10000;
        final long[] samples = new long[probabilies.length];
        for (int i = 0; i < numberOfSamples; i++) {
            samples[sampler.sample()]++;
        }

        // Handle a test with some zero-probability observations by mapping them out.
        // The results is the Chi-square test is performed using only the non-zero probabilities.
        int mapSize = 0;
        for (int i = 0; i < probabilies.length; i++) {
            if (probabilies[i] != 0) {
                mapSize++;
            }
        }

        final double[] expected = new double[mapSize];
        final long[] observed = new long[mapSize];
        for (int i = 0; i < probabilies.length; i++) {
            if (probabilies[i] == 0) {
                Assertions.assertEquals(0, samples[i], "No samples expected from zero probability");
            } else {
                // This can be added for the Chi-square test
                --mapSize;
                expected[mapSize] = probabilies[i];
                observed[mapSize] = samples[i];
            }
        }

        final ChiSquareTest chiSquareTest = new ChiSquareTest();
        // Pass if we cannot reject null hypothesis that the distributions are the same.
        Assertions.assertFalse(chiSquareTest.chiSquareTest(expected, observed, 0.001));
    }

    /**
     * Test the SharedStateSampler implementation.
     */
    @Test
    public void testSharedStateSampler() {
        final UniformRandomProvider rng1 = RandomSource.SPLIT_MIX_64.create(0L);
        final UniformRandomProvider rng2 = RandomSource.SPLIT_MIX_64.create(0L);
        final double[] probabilities = {0.1, 0, 0.2, 0.3, 0.1, 0.3, 0};
        final SharedStateDiscreteSampler sampler1 =
            GuideTableDiscreteSampler.of(rng1, probabilities);
        final SharedStateDiscreteSampler sampler2 = sampler1.withUniformRandomProvider(rng2);
        RandomAssert.assertProduceSameSequence(sampler1, sampler2);
    }
}
