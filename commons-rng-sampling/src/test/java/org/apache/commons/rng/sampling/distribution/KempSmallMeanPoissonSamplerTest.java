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
     * <p>p0 = Math.exp(-mean) => mean = -Math.log(p0).
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
        KempSmallMeanPoissonSampler sampler = new KempSmallMeanPoissonSampler(dummyRng, mean);
    }

    /**
     * Test the constructor with zero mean.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testConstructorThrowsWithZeroMean() {
        final double mean = 0;
        @SuppressWarnings("unused")
        KempSmallMeanPoissonSampler sampler = new KempSmallMeanPoissonSampler(dummyRng, mean);
    }

    /**
     * Test the constructor with a negative mean.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testConstructorThrowsWithNegativeMean() {
        final double mean = -1;
        @SuppressWarnings("unused")
        KempSmallMeanPoissonSampler sampler = new KempSmallMeanPoissonSampler(dummyRng, mean);
    }

    /**
     * Test the constructor with a NaN mean.
     */
    @Test
    public void testConstructorWithNaNMean() {
        final double mean = Double.NaN;
        // The sampler is allowed but the sample will be zero no matter what the uniform
        // random deviate returns.
        Assert.assertEquals("Sample should be zero with cumulative probability 0",
            0, new KempSmallMeanPoissonSampler(new FixedRNG(0), mean).sample());
        Assert.assertEquals("Sample should be zero with cumulative probability 1",
            0, new KempSmallMeanPoissonSampler(new FixedRNG(1), mean).sample());
    }

    /**
     * Test the cumulative summation at the upper bound on the mean is close to zero.
     */
    @Test
    public void testSummationAtUpperBound() {
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
    }

    /**
     * Test the sampler functions at a low mean. The mean is chosen so that the hedge at 50%
     * cumulative probability is not used.
     */
    @Test
    public void testSamplerAtLowMean() {
        // Set the initial probability above the long tail threshold value (0.999)
        final double p0 = 0.9995;
        final double mean = -Math.log(p0);

        // Test some ranges for the cumulative probability
        final PoissonDistribution pd = new PoissonDistribution(null, mean,
            PoissonDistribution.DEFAULT_EPSILON, PoissonDistribution.DEFAULT_MAX_ITERATIONS);

        // Lower bound should be zero
        testSample(mean, 0, 0, 0);

        // When the mean is small the Poisson is highly skewed and the range of the achievable
        // cumulative probability is small.
        // Just test reasonable samples stating from 1.
        double p = pd.cumulativeProbability(0);
        for (int i = 1; i < 5; i++) {
            final double lastP = p;
            p = pd.cumulativeProbability(i);
            testSample(mean, (p + lastP) / 2, i, i);
        }
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

        // Lower bound should be zero
        testSample(mean, 0, 0, 0);

        // Upper bound should exceed 99.99% of the range
        testSample(mean, 1, pd.inverseCumulativeProbability(0.9999), Integer.MAX_VALUE);

        // A sample from within the cumulative probability should be within the expected range
        for (int i = 1; i < 10; i++) {
            final double p = i * 0.1;
            final int lower = pd.inverseCumulativeProbability(p - 0.01);
            final int upper = pd.inverseCumulativeProbability(p + 0.01);
            testSample(mean, p, lower, upper);
        }
    }

    /**
     * Test a sample from the Poisson distribution at the given cumulative probability.
     *
     * @param mean the mean
     * @param cumulativeProbability the cumulative probability
     * @param lower the expected lower limit
     * @param upper the expected upper limit
     */
    private static void testSample(double mean, double cumulativeProbability, int lower, int upper) {
        final UniformRandomProvider rng = new FixedRNG(cumulativeProbability);
        final KempSmallMeanPoissonSampler sampler = new KempSmallMeanPoissonSampler(rng, mean);
        final int sample = sampler.sample();
        Assert.assertTrue("Sampler is not above realistic lower limit: " + lower, sample >= lower);
        Assert.assertTrue("Sampler is not below realistic upper limit: " + upper, sample <= upper);
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

        public long nextLong(long n) { return 0; }
        public long nextLong() { return 0; }
        public int nextInt(int n) { return 0; }
        public int nextInt() { return 0; }
        public float nextFloat() { return 0; }
        public void nextBytes(byte[] bytes, int start, int len) {}
        public void nextBytes(byte[] bytes) {}
        public boolean nextBoolean() { return false; }
    }
}
