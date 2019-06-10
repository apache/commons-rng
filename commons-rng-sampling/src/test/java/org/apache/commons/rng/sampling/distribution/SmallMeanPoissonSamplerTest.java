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
import org.apache.commons.rng.sampling.RandomAssert;
import org.apache.commons.rng.simple.RandomSource;
import org.junit.Assert;
import org.junit.Test;

/**
 * Test for the {@link SmallMeanPoissonSampler}. The tests hit edge cases for the sampler.
 */
public class SmallMeanPoissonSamplerTest {
    /**
     * Test the constructor with a bad mean.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testConstructorThrowsWithMeanThatSetsProbabilityP0ToZero() {
        final UniformRandomProvider rng =
            RandomSource.create(RandomSource.SPLIT_MIX_64);
        final double p0 = Double.MIN_VALUE;
        // Note: p0 = Math.exp(-mean) => mean = -Math.log(p0).
        // Add to the limit on the mean to cause p0 to be zero.
        final double mean = -Math.log(p0) + 1;
        @SuppressWarnings("unused")
        SmallMeanPoissonSampler sampler = new SmallMeanPoissonSampler(rng, mean);
    }

    /**
     * Test the constructor with a bad mean.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testConstructorThrowsWithZeroMean() {
        final UniformRandomProvider rng =
            RandomSource.create(RandomSource.SPLIT_MIX_64);
        final double mean = 0;
        @SuppressWarnings("unused")
        SmallMeanPoissonSampler sampler = new SmallMeanPoissonSampler(rng, mean);
    }

    /**
     * Test the sample is bounded to 1000 * mean.
     */
    @Test
    public void testSampleUpperBounds() {
        // If the nextDouble() is always 1 then the sample will hit the upper bounds
        final UniformRandomProvider rng = new UniformRandomProvider() {
            // CHECKSTYLE: stop all
            public long nextLong(long n) { return 0; }
            public long nextLong() { return 0; }
            public int nextInt(int n) { return 0; }
            public int nextInt() { return 0; }
            public float nextFloat() { return 0; }
            public double nextDouble() { return 1;}
            public void nextBytes(byte[] bytes, int start, int len) {}
            public void nextBytes(byte[] bytes) {}
            public boolean nextBoolean() { return false; }
            // CHECKSTYLE: resume all
        };
        for (double mean : new double[] {0.5, 1, 1.5, 2.2}) {
            final SmallMeanPoissonSampler sampler = new SmallMeanPoissonSampler(rng, mean);
            final int expected = (int) Math.ceil(1000 * mean);
            Assert.assertEquals(expected, sampler.sample());
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
        final SmallMeanPoissonSampler sampler1 =
            new SmallMeanPoissonSampler(rng1, mean);
        final SmallMeanPoissonSampler sampler2 = sampler1.withUniformRandomProvider(rng2);
        RandomAssert.assertProduceSameSequence(sampler1, sampler2);
    }
}
