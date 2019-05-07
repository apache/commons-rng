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

/**
 * Test for the {@link MarsagliaTsangWangSmallMeanPoissonSampler}. The tests hit edge
 * cases for the sampler.
 */
public class MarsagliaTsangWangSmallMeanPoissonSamplerTest {
    /**
     * Test the constructor with a bad mean.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testConstructorThrowsWithMeanLargerThanUpperBound() {
        final UniformRandomProvider rng = new FixedRNG(0);
        final double mean = 1025;
        @SuppressWarnings("unused")
        final MarsagliaTsangWangSmallMeanPoissonSampler sampler = new MarsagliaTsangWangSmallMeanPoissonSampler(rng,
            mean);
    }

    /**
     * Test the constructor with a bad mean.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testConstructorThrowsWithZeroMean() {
        final UniformRandomProvider rng = new FixedRNG(0);
        final double mean = 0;
        @SuppressWarnings("unused")
        final MarsagliaTsangWangSmallMeanPoissonSampler sampler = new MarsagliaTsangWangSmallMeanPoissonSampler(rng,
            mean);
    }

    /**
     * Test the constructor with the maximum mean.
     */
    @Test
    public void testConstructorWithMaximumMean() {
        final UniformRandomProvider rng = new FixedRNG(0);
        final double mean = 1024;
        @SuppressWarnings("unused")
        final MarsagliaTsangWangSmallMeanPoissonSampler sampler = new MarsagliaTsangWangSmallMeanPoissonSampler(rng,
            mean);
    }

    /**
     * Test the constructor with a small mean that hits the edge case where the
     * probability sum is not 2^30.
     */
    @Test
    public void testConstructorWithSmallMean() {
        final UniformRandomProvider rng = new FixedRNG(0xffffffff);
        final double mean = 0.25;
        final MarsagliaTsangWangSmallMeanPoissonSampler sampler = new MarsagliaTsangWangSmallMeanPoissonSampler(rng,
            mean);
        // This will throw if the table does not sum to 2^30
        sampler.sample();
    }

    /**
     * Test the constructor with a medium mean that is at the switch point for how the probability
     * distribution is computed.
     */
    @Test
    public void testConstructorWithMediumMean() {
        final UniformRandomProvider rng = new FixedRNG(0xffffffff);
        final double mean = 21.4;
        final MarsagliaTsangWangSmallMeanPoissonSampler sampler = new MarsagliaTsangWangSmallMeanPoissonSampler(rng,
            mean);
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
