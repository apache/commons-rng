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

import org.apache.commons.rng.UniformRandomProvider;
import org.junit.Test;

import org.junit.Assert;

/**
 * Test for the {@link MarsagliaTsangWangBinomialSampler}. The tests hit edge cases for
 * the sampler.
 */
public class MarsagliaTsangWangBinomialSamplerTest {
    @Test(expected = IllegalArgumentException.class)
    public void testConstructorThrowsWithTrialsBelow0() {
        final UniformRandomProvider rng = new FixedRNG(0);
        final int trials = -1;
        final double p = 0.5;
        @SuppressWarnings("unused")
        final MarsagliaTsangWangBinomialSampler sampler = new MarsagliaTsangWangBinomialSampler(rng, trials, p);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorThrowsWithTrialsAboveMax() {
        final UniformRandomProvider rng = new FixedRNG(0);
        final int trials = 1 << 16; // 2^16
        final double p = 0.5;
        @SuppressWarnings("unused")
        final MarsagliaTsangWangBinomialSampler sampler = new MarsagliaTsangWangBinomialSampler(rng, trials, p);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorThrowsWithProbabilityBelow0() {
        final UniformRandomProvider rng = new FixedRNG(0);
        final int trials = 1;
        final double p = -0.5;
        @SuppressWarnings("unused")
        final MarsagliaTsangWangBinomialSampler sampler = new MarsagliaTsangWangBinomialSampler(rng, trials, p);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorThrowsWithProbabilityAbove1() {
        final UniformRandomProvider rng = new FixedRNG(0);
        final int trials = 1;
        final double p = 1.5;
        @SuppressWarnings("unused")
        final MarsagliaTsangWangBinomialSampler sampler = new MarsagliaTsangWangBinomialSampler(rng, trials, p);
    }

    /**
     * Test the constructor with distribution parameters that create a very small p(0)
     * with a high probability of success.
     */
    @Test
    public void testSamplerWithSmallestP0ValueAndHighestProbabilityOfSuccess() {
        final UniformRandomProvider rng = new FixedRNG(0xffffffff);
        // p(0) = Math.exp(trials * Math.log(1-p))
        // p(0) will be smaller as Math.log(1-p) is more negative, which occurs when p is
        // larger.
        // Since the sampler uses inversion the largest value for p is 0.5.
        // At the extreme for p = 0.5:
        // trials = Math.log(p(0)) / Math.log(1-p)
        // = Math.log(Double.MIN_VALUE) / Math.log(0.5)
        // = 1074
        final int trials = (int) Math.floor(Math.log(Double.MIN_VALUE) / Math.log(0.5));
        final double p = 0.5;
        // Validate set-up
        Assert.assertEquals("Invalid test set-up for p(0)", Double.MIN_VALUE, getP0(trials, p), 0);
        Assert.assertEquals("Invalid test set-up for p(0)", 0, getP0(trials + 1, p), 0);

        // This will throw if the table does not sum to 2^30
        final MarsagliaTsangWangBinomialSampler sampler = new MarsagliaTsangWangBinomialSampler(rng, trials, p);
        sampler.sample();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorThrowsWhenP0IsZero() {
        final UniformRandomProvider rng = new FixedRNG(0);
        // As above but increase the trials so p(0) should be zero
        final int trials = 1 + (int) Math.floor(Math.log(Double.MIN_VALUE) / Math.log(0.5));
        final double p = 0.5;
        // Validate set-up
        Assert.assertEquals("Invalid test set-up for p(0)", 0, getP0(trials, p), 0);
        @SuppressWarnings("unused")
        final MarsagliaTsangWangBinomialSampler sampler = new MarsagliaTsangWangBinomialSampler(rng, trials, p);
    }

    /**
     * Test the constructor with distribution parameters that create a very small p(0)
     * with a high number of trials.
     */
    @Test
    public void testSamplerWithLargestTrialsAndSmallestProbabilityOfSuccess() {
        final UniformRandomProvider rng = new FixedRNG(0xffffffff);
        // p(0) = Math.exp(trials * Math.log(1-p))
        // p(0) will be smaller as Math.log(1-p) is more negative, which occurs when p is
        // larger.
        // Since the sampler uses inversion the largest value for p is 0.5.
        // At the extreme for trials = 2^16-1:
        // p = 1 - Math.exp(Math.log(p(0)) / trials)
        // = 1 - Math.exp(Math.log(Double.MIN_VALUE) / trials)
        // = 0.011295152668039599
        final int trials = (1 << 16) - 1;
        double p = 1 - Math.exp(Math.log(Double.MIN_VALUE) / trials);

        // Validate set-up
        Assert.assertEquals("Invalid test set-up for p(0)", Double.MIN_VALUE, getP0(trials, p), 0);

        // Search for larger p until Math.nextAfter(p, 1) produces 0
        double upper = p * 2;
        Assert.assertEquals("Invalid test set-up for p(0)", 0, getP0(trials, upper), 0);

        double lower = p;
        while (Double.doubleToRawLongBits(lower) + 1 < Double.doubleToRawLongBits(upper)) {
            final double mid = (upper + lower) / 2;
            if (getP0(trials, mid) == 0) {
                upper = mid;
            } else {
                lower = mid;
            }
        }
        p = lower;

        // Re-validate
        Assert.assertEquals("Invalid test set-up for p(0)", Double.MIN_VALUE, getP0(trials, p), 0);
        Assert.assertEquals("Invalid test set-up for p(0)", 0, getP0(trials, Math.nextAfter(p, 1)), 0);

        final MarsagliaTsangWangBinomialSampler sampler = new MarsagliaTsangWangBinomialSampler(rng, trials, p);
        // This will throw if the table does not sum to 2^30
        sampler.sample();
    }

    /**
     * Gets the p(0) value.
     *
     * @param trials the trials
     * @param probabilityOfSuccess the probability of success
     * @return the p(0) value
     */
    private static double getP0(int trials, double probabilityOfSuccess) {
        return Math.exp(trials * Math.log(1 - probabilityOfSuccess));
    }

    @Test
    public void testSamplerWithProbability0() {
        final UniformRandomProvider rng = new FixedRNG(0);
        final int trials = 1000000;
        final double p = 0;
        final MarsagliaTsangWangBinomialSampler sampler = new MarsagliaTsangWangBinomialSampler(rng, trials, p);
        for (int i = 0; i < 5; i++) {
            Assert.assertEquals(0, sampler.sample());
        }
        // Hit the toString() method
        Assert.assertTrue(sampler.toString().contains("Binomial"));
    }

    @Test
    public void testSamplerWithProbability1() {
        final UniformRandomProvider rng = new FixedRNG(0);
        final int trials = 1000000;
        final double p = 1;
        final MarsagliaTsangWangBinomialSampler sampler = new MarsagliaTsangWangBinomialSampler(rng, trials, p);
        for (int i = 0; i < 5; i++) {
            Assert.assertEquals(trials, sampler.sample());
        }
        // Hit the toString() method
        Assert.assertTrue(sampler.toString().contains("Binomial"));
    }

    /**
     * Test the sampler with a large number of trials. This tests the sampler can create the
     * Binomial distribution for a large size when a limiting distribution (e.g. the Normal distribution)
     * could be used instead.
     */
    @Test
    public void testSamplerWithLargeNumberOfTrials() {
        final UniformRandomProvider rng = new FixedRNG(0xffffffff);
        final int trials = 65000;
        final double p = 0.01;
        final MarsagliaTsangWangBinomialSampler sampler = new MarsagliaTsangWangBinomialSampler(rng, trials, p);
        // This will throw if the table does not sum to 2^30
        sampler.sample();
    }

    /**
     * Test the sampler with a probability of 0.5. This should hit the edge case in the loop to
     * search for the last probability of the Binomial distribution.
     */
    @Test
    public void testSamplerWithProbability0_5() {
        final UniformRandomProvider rng = new FixedRNG(0xffffffff);
        final int trials = 10;
        final double p = 0.5;
        final MarsagliaTsangWangBinomialSampler sampler = new MarsagliaTsangWangBinomialSampler(rng, trials, p);
        // This will throw if the table does not sum to 2^30
        sampler.sample();
    }

    /**
     * A RNG returning a fixed value.
     */
    private static class FixedRNG implements UniformRandomProvider {
        /** The value. */
        private final int value;

        /**
         * @param value the value
         */
        FixedRNG(int value) {
            this.value = value;
        }

        @Override
        public int nextInt() {
            return value;
        }

        public void nextBytes(byte[] bytes) {}
        public void nextBytes(byte[] bytes, int start, int len) {}
        public int nextInt(int n) { return 0; }
        public long nextLong() { return 0; }
        public long nextLong(long n) { return 0; }
        public boolean nextBoolean() { return false; }
        public float nextFloat() { return 0; }
        public double nextDouble() { return 0; }
    }
}
