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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import java.util.Arrays;
import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.stat.correlation.Covariance;
import org.apache.commons.math3.stat.descriptive.moment.Mean;
import org.apache.commons.rng.UniformRandomProvider;
import org.apache.commons.rng.core.source64.SplitMix64;
import org.apache.commons.rng.sampling.RandomAssert;
import org.apache.commons.rng.simple.RandomSource;

/**
 * Test for {@link DirichletSampler}.
 */
class DirichletSamplerTest {
    @Test
    void testDistributionThrowsWithInvalidNumberOfCategories() {
        final UniformRandomProvider rng = RandomSource.SPLIT_MIX_64.create(0L);
        Assertions.assertThrows(IllegalArgumentException.class,
            () -> DirichletSampler.of(rng, 1.0));
    }

    @Test
    void testDistributionThrowsWithZeroConcentration() {
        final UniformRandomProvider rng = RandomSource.SPLIT_MIX_64.create(0L);
        Assertions.assertThrows(IllegalArgumentException.class,
            () -> DirichletSampler.of(rng, 1.0, 0.0));
    }

    @Test
    void testDistributionThrowsWithNaNConcentration() {
        final UniformRandomProvider rng = RandomSource.SPLIT_MIX_64.create(0L);
        Assertions.assertThrows(IllegalArgumentException.class,
            () -> DirichletSampler.of(rng, 1.0, Double.NaN));
    }

    @Test
    void testDistributionThrowsWithInfiniteConcentration() {
        final UniformRandomProvider rng = RandomSource.SPLIT_MIX_64.create(0L);
        Assertions.assertThrows(IllegalArgumentException.class,
            () -> DirichletSampler.of(rng, 1.0, Double.POSITIVE_INFINITY));
    }

    @Test
    void testSymmetricDistributionThrowsWithInvalidNumberOfCategories() {
        final UniformRandomProvider rng = RandomSource.SPLIT_MIX_64.create(0L);
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> DirichletSampler.symmetric(rng, 1, 1.0));
    }

    @Test
    void testSymmetricDistributionThrowsWithZeroConcentration() {
        final UniformRandomProvider rng = RandomSource.SPLIT_MIX_64.create(0L);
        Assertions.assertThrows(IllegalArgumentException.class,
            () -> DirichletSampler.symmetric(rng, 2, 0.0));
    }

    @Test
    void testSymmetricDistributionThrowsWithNaNConcentration() {
        final UniformRandomProvider rng = RandomSource.SPLIT_MIX_64.create(0L);
        Assertions.assertThrows(IllegalArgumentException.class,
            () -> DirichletSampler.symmetric(rng, 2, Double.NaN));
    }

    @Test
    void testSymmetricDistributionThrowsWithInfiniteConcentration() {
        final UniformRandomProvider rng = RandomSource.SPLIT_MIX_64.create(0L);
        Assertions.assertThrows(IllegalArgumentException.class,
            () -> DirichletSampler.symmetric(rng, 2, Double.POSITIVE_INFINITY));
    }

    /**
     * Create condition so that all samples are zero and it is impossible to normalise the
     * samples to sum to 1. These should be ignored and the sample is repeated until
     * normalisation is possible.
     */
    @Test
    void testInvalidSampleIsIgnored() {
        // An RNG implementation which should create zero samples from the underlying
        // exponential sampler for an initial sequence.
        final UniformRandomProvider rng = new SplitMix64(0L) {
            private int i;

            @Override
            public long next() {
                return i++ < 10 ? 0L : super.next();
            }
        };

        // Alpha=1 will use an exponential sampler
        final DirichletSampler sampler = DirichletSampler.symmetric(rng, 2, 1.0);
        assertSample(2, sampler.sample());
    }

    @Test
    void testSharedStateSampler() {
        final RandomSource randomSource = RandomSource.XO_RO_SHI_RO_128_PP;
        final byte[] seed = randomSource.createSeed();
        final UniformRandomProvider rng1 = randomSource.create(seed);
        final UniformRandomProvider rng2 = randomSource.create(seed);
        final DirichletSampler sampler1 = DirichletSampler.of(rng1, 1, 2, 3);
        final DirichletSampler sampler2 = sampler1.withUniformRandomProvider(rng2);
        RandomAssert.assertProduceSameSequence(sampler1, sampler2);
    }

    @Test
    void testSharedStateSamplerForSymmetricCase() {
        final RandomSource randomSource = RandomSource.XO_RO_SHI_RO_128_PP;
        final byte[] seed = randomSource.createSeed();
        final UniformRandomProvider rng1 = randomSource.create(seed);
        final UniformRandomProvider rng2 = randomSource.create(seed);
        final DirichletSampler sampler1 = DirichletSampler.symmetric(rng1, 2, 1.5);
        final DirichletSampler sampler2 = sampler1.withUniformRandomProvider(rng2);
        RandomAssert.assertProduceSameSequence(sampler1, sampler2);
    }

    @Test
    void testSymmetricCaseMatchesGeneralCase() {
        final RandomSource randomSource = RandomSource.XO_RO_SHI_RO_128_PP;
        final byte[] seed = randomSource.createSeed();
        final UniformRandomProvider rng1 = randomSource.create(seed);
        final UniformRandomProvider rng2 = randomSource.create(seed);
        final int k = 3;
        final double[] alphas = new double[k];
        for (final double alpha : new double[] {0.5, 1.0, 1.5}) {
            Arrays.fill(alphas, alpha);
            final DirichletSampler sampler1 = DirichletSampler.symmetric(rng1, k, alpha);
            final DirichletSampler sampler2 = DirichletSampler.of(rng2, alphas);
            RandomAssert.assertProduceSameSequence(sampler1, sampler2);
        }
    }

    /**
     * Test the toString method. This is added to ensure coverage.
     */
    @Test
    void testToString() {
        final UniformRandomProvider rng = RandomSource.SPLIT_MIX_64.create(0L);
        final DirichletSampler sampler1 = DirichletSampler.symmetric(rng, 2, 1.0);
        final DirichletSampler sampler2 = DirichletSampler.of(rng, 0.5, 1, 1.5);
        Assertions.assertTrue(sampler1.toString().toLowerCase().contains("dirichlet"));
        Assertions.assertTrue(sampler2.toString().toLowerCase().contains("dirichlet"));
    }

    @Test
    void testSampling1() {
        assertSamples(1, 2, 3);
    }

    @Test
    void testSampling2() {
        assertSamples(1, 1, 1);
    }

    @Test
    void testSampling3() {
        assertSamples(0.5, 1, 1.5);
    }

    @Test
    void testSampling4() {
        assertSamples(1, 3);
    }

    @Test
    void testSampling5() {
        assertSamples(1, 2, 3, 4);
    }

    /**
     * Assert samples from the distribution. The variates are tested against the expected
     * mean and covariance for the given concentration parameters.
     *
     * @param alpha Concentration parameters.
     */
    private static void assertSamples(double... alpha) {
        // No fixed seed. Failed tests will be repeated by the JUnit test runner.
        final UniformRandomProvider rng = RandomAssert.createRNG();
        final DirichletSampler sampler = DirichletSampler.of(rng, alpha);
        final int k = alpha.length;
        final double[][] samples = new double[100000][];
        for (int i = 0; i < samples.length; i++) {
            final double[] x = sampler.sample();
            assertSample(k, x);
            samples[i] = x;
        }

        // Computation of moments:
        // https://en.wikipedia.org/wiki/Dirichlet_distribution#Moments

        // Compute the sum of the concentration parameters: alpha0
        double alpha0 = 0;
        for (int i = 0; i < k; i++) {
            alpha0 += alpha[i];
        }

        // Use a moderate tolerance.
        // Differences are usually observed in the 3rd significant figure.
        final double relativeTolerance = 5e-2;

        // Mean = alpha[i] / alpha0
        final double[] means = getColumnMeans(samples);
        for (int i = 0; i < k; i++) {
            final double mean = alpha[i] / alpha0;
            Assertions.assertEquals(mean, means[i], mean * relativeTolerance, "Mean");
        }

        // Variance = alpha_i (alpha_0 - alpha_i) / alpha_0^2 (alpha_0 + 1)
        // Covariance = -alpha_i * alpha_j / alpha_0^2 (alpha_0 + 1)
        final double[][] covars = getCovariance(samples);
        final double denom = alpha0 * alpha0 * (alpha0 + 1);
        for (int i = 0; i < k; i++) {
            final double var = alpha[i] * (alpha0 - alpha[i]) / denom;
            Assertions.assertEquals(var, covars[i][i], var * relativeTolerance, "Variance");
            for (int j = i + 1; j < k; j++) {
                final double covar = -alpha[i] * alpha[j] / denom;
                Assertions.assertEquals(covar, covars[i][j], Math.abs(covar) * relativeTolerance, "Covariance");
            }
        }
    }

    /**
     * Assert the sample has the correct length and sums to 1.
     *
     * @param k Expected number of categories.
     * @param x Sample.
     */
    private static void assertSample(int k, double[] x) {
        Assertions.assertEquals(k, x.length, "Number of categories");
        // There are always at least 2 categories
        double sum = x[0] + x[1];
        // Sum the rest
        for (int i = 2; i < x.length; i++) {
            sum += x[i];
        }
        Assertions.assertEquals(1.0, sum, 1e-10, "Invalid sum");
    }

    /**
     * Gets the column means. This is done using the same method as the means in the
     * Apache Commons Math Covariance class by using the Mean class.
     *
     * @param data the data
     * @return the column means
     */
    private static double[] getColumnMeans(double[][] data) {
        final Array2DRowRealMatrix m = new Array2DRowRealMatrix(data, false);
        final Mean mean = new Mean();
        final double[] means = new double[m.getColumnDimension()];
        for (int i = 0; i < means.length; i++) {
            means[i] = mean.evaluate(m.getColumn(i));
        }
        return means;
    }

    /**
     * Gets the covariance.
     *
     * @param data the data
     * @return the covariance
     */
    private static double[][] getCovariance(double[][] data) {
        final Array2DRowRealMatrix m = new Array2DRowRealMatrix(data, false);
        return new Covariance(m).getCovarianceMatrix().getData();
    }
}
