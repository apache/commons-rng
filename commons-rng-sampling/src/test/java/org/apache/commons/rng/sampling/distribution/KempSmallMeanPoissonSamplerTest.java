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

import org.apache.commons.math3.distribution.PoissonDistribution;
import org.apache.commons.rng.UniformRandomProvider;
import org.apache.commons.rng.sampling.RandomAssert;
import org.apache.commons.rng.simple.RandomSource;
import org.junit.Assert;
import org.junit.Test;

/**
 * Test for the {@link KempSmallMeanPoissonSampler}. The tests hit edge cases for the
 * sampler and tests it functions at the supported upper bound on the mean.
 */
public class KempSmallMeanPoissonSamplerTest {
    /**
     * The upper limit on the mean.
     *
     * <p>p0 = Math.exp(-mean) => mean = -Math.log(p0). When p0 is {@link Double#MIN_VALUE} the
     * mean is approximately 744.4.</p>
     */
    private static final double SUPPORTED_UPPER_BOUND = -Math.log(Double.MIN_VALUE);

    /** The rng for construction tests. */
    private final UniformRandomProvider dummyRng = new FixedRNG(0);

    /**
     * Test the constructor with a bad mean.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testConstructorThrowsWithMeanLargerThanUpperBound() {
        final double mean = SUPPORTED_UPPER_BOUND + 1;
        @SuppressWarnings("unused")
        SharedStateDiscreteSampler sampler = KempSmallMeanPoissonSampler.of(dummyRng, mean);
    }

    /**
     * Test the constructor with zero mean.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testConstructorThrowsWithZeroMean() {
        final double mean = 0;
        @SuppressWarnings("unused")
        SharedStateDiscreteSampler sampler = KempSmallMeanPoissonSampler.of(dummyRng, mean);
    }

    /**
     * Test the constructor with a negative mean.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testConstructorThrowsWithNegativeMean() {
        final double mean = -1;
        @SuppressWarnings("unused")
        SharedStateDiscreteSampler sampler = KempSmallMeanPoissonSampler.of(dummyRng, mean);
    }

    /**
     * Test the constructor with a NaN mean.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testConstructorWithNaNMean() {
        final double mean = Double.NaN;
        @SuppressWarnings("unused")
        SharedStateDiscreteSampler sampler = KempSmallMeanPoissonSampler.of(dummyRng, mean);
    }

    /**
     * Test the cumulative summation at the upper bound on the mean is close to zero when
     * starting from 1.
     */
    @Test
    public void testSummationFrom1AtUpperBound() {
        final double mean = SUPPORTED_UPPER_BOUND;
        double u = 1;
        int x = 0;
        double p = Math.exp(-mean);
        while (u > p && p != 0) {
            u -= p;
            x = x + 1;
            p = p * mean / x;
        }
        Assert.assertEquals("Summation is not zero", 0, u, 1e-3);
        Assert.assertTrue("Summation is not greater than zero", u > 0);
    }

    /**
     * Test the cumulative summation at the upper bound on the mean is close to one when
     * starting from 0.
     */
    @Test
    public void testSummationTo1AtUpperBound() {
        final double mean = SUPPORTED_UPPER_BOUND;
        double u = 0;
        int x = 0;
        double p = Math.exp(-mean);
        while (p != 0) {
            u += p;
            x = x + 1;
            p = p * mean / x;
        }
        Assert.assertEquals("Summation is not one", 1, u, 1e-3);
        Assert.assertTrue("Summation is not less than one", u < 1);
    }

    /**
     * Test the sampler functions at the upper bound on the mean.
     */
    @Test
    public void testSamplerAtUpperBounds() {
        final double mean = SUPPORTED_UPPER_BOUND;

        // Test some ranges for the cumulative probability
        final PoissonDistribution pd = new PoissonDistribution(null, mean,
            PoissonDistribution.DEFAULT_EPSILON, PoissonDistribution.DEFAULT_MAX_ITERATIONS);

        final FixedRNG rng = new FixedRNG(0);
        final SharedStateDiscreteSampler sampler = KempSmallMeanPoissonSampler.of(rng, mean);

        // Lower bound should be zero
        testSample(rng, sampler, 0, 0, 0);

        // Upper bound should exceed 99.99% of the range
        testSample(rng, sampler, 1, pd.inverseCumulativeProbability(0.9999), Integer.MAX_VALUE);

        // A sample from within the cumulative probability should be within the expected range
        for (int i = 1; i < 10; i++) {
            final double p = i * 0.1;
            final int lower = pd.inverseCumulativeProbability(p - 0.01);
            final int upper = pd.inverseCumulativeProbability(p + 0.01);
            testSample(rng, sampler, p, lower, upper);
        }
    }

    /**
     * Test the SharedStateSampler implementation.
     */
    @Test
    public void testSharedStateSampler() {
        final UniformRandomProvider rng1 = RandomSource.create(RandomSource.SPLIT_MIX_64, 0L);
        final UniformRandomProvider rng2 = RandomSource.create(RandomSource.SPLIT_MIX_64, 0L);
        final double mean = 1.23;
        final SharedStateDiscreteSampler sampler1 =
            KempSmallMeanPoissonSampler.of(rng1, mean);
        final SharedStateDiscreteSampler sampler2 = sampler1.withUniformRandomProvider(rng2);
        RandomAssert.assertProduceSameSequence(sampler1, sampler2);
    }

    /**
     * Test a sample from the Poisson distribution at the given cumulative probability.
     *
     * @param rng the fixed random generator backing the sampler
     * @param sampler the sampler
     * @param cumulativeProbability the cumulative probability
     * @param lower the expected lower limit
     * @param upper the expected upper limit
     */
    private static void testSample(FixedRNG rng, SharedStateDiscreteSampler sampler, double cumulativeProbability,
        int lower, int upper) {
        rng.setValue(cumulativeProbability);
        final int sample = sampler.sample();
        Assert.assertTrue(sample + " sample is not above realistic lower limit: " + lower, sample >= lower);
        Assert.assertTrue(sample + " sample is not below realistic upper limit: " + upper, sample <= upper);
    }

    /**
     * A RNG returning a fixed value.
     */
    private static class FixedRNG implements UniformRandomProvider {
        /** The value. */
        private double value;

        /**
         * @param value the value
         */
        FixedRNG(double value) {
            this.value = value;
        }

        @Override
        public double nextDouble() {
            return value;
        }

        /**
         * @param value the new value
         */
        void setValue(double value) {
            this.value = value;
        }

        // CHECKSTYLE: stop all
        public long nextLong(long n) { return 0; }
        public long nextLong() { return 0; }
        public int nextInt(int n) { return 0; }
        public int nextInt() { return 0; }
        public float nextFloat() { return 0; }
        public void nextBytes(byte[] bytes, int start, int len) {}
        public void nextBytes(byte[] bytes) {}
        public boolean nextBoolean() { return false; }
        // CHECKSTYLE: resume all
    }
}
