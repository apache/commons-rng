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
import org.apache.commons.rng.core.source64.SplitMix64;
import org.apache.commons.rng.sampling.RandomAssert;
import org.apache.commons.rng.simple.RandomSource;
import org.junit.Assert;
import org.junit.Test;

/**
 * Test for the {@link ContinuousUniformSampler}.
 */
public class ContinuousUniformSamplerTest {
    /**
     * Test that the sampler algorithm does not require high to be above low.
     */
    @Test
    public void testNoRestrictionOnOrderOfLowAndHighParameters() {
        final double low = 3.18;
        final double high = 5.23;
        final UniformRandomProvider rng = RandomSource.SPLIT_MIX_64.create();
        testSampleInRange(rng, low, high);
        testSampleInRange(rng, high, low);
    }

    private static void testSampleInRange(UniformRandomProvider rng,
                                          double low, double high) {
        final SharedStateContinuousSampler sampler = ContinuousUniformSampler.of(rng, low, high);
        final double min = Math.min(low,  high);
        final double max = Math.max(low,  high);
        for (int i = 0; i < 10; i++) {
            final double value = sampler.sample();
            Assert.assertTrue("Value not in range", value >= min && value <= max);
        }
    }

    /**
     * Test the sampler excludes the bounds when the underlying generator returns long values
     * that produce the limit of the uniform double output.
     */
    @Test
    public void testExcludeBounds() {
        // A broken RNG that will return in an alternating sequence from 0 up or -1 down.
        // This is either zero bits or all the bits
        final UniformRandomProvider rng = new SplitMix64(0L) {
            private long l1;
            private long l2;
            @Override
            public long nextLong() {
                long x;
                if (l1 > l2) {
                    l2++;
                    // Descending sequence: -1, -2, -3, ...
                    x = -l2;
                } else {
                    // Ascending sequence: 0, 1, 2, ...
                    x = l1++;
                }
                // Shift by 11 bits to reverse the shift performed when computing the next
                // double from a long.
                return x << 11;
            }
        };
        final double low = 3.18;
        final double high = 5.23;
        final SharedStateContinuousSampler sampler =
            ContinuousUniformSampler.of(rng, low, high, true);
        // Test the sampler excludes the end points
        for (int i = 0; i < 10; i++) {
            final double value = sampler.sample();
            Assert.assertTrue("Value not in range", value >= low && value <= high);
        }
    }

    /**
     * Test the SharedStateSampler implementation.
     */
    @Test
    public void testSharedStateSampler() {
        testSharedStateSampler(false);
        testSharedStateSampler(true);
    }

    /**
     * Test the SharedStateSampler implementation.
     *
     * @param excludedBounds Set to true to exclude the bounds.
     */
    private static void testSharedStateSampler(boolean excludedBounds) {
        final UniformRandomProvider rng1 = RandomSource.SPLIT_MIX_64.create(0L);
        final UniformRandomProvider rng2 = RandomSource.SPLIT_MIX_64.create(0L);
        final double low = 1.23;
        final double high = 4.56;
        final SharedStateContinuousSampler sampler1 =
            ContinuousUniformSampler.of(rng1, low, high, excludedBounds);
        final SharedStateContinuousSampler sampler2 = sampler1.withUniformRandomProvider(rng2);
        RandomAssert.assertProduceSameSequence(sampler1, sampler2);
    }

    /**
     * Test the sampler implementation with bounds excluded matches that with bounds included
     * when the generator does not produce the limit of the uniform double output.
     */
    @Test
    public void testSamplerWithBoundsExcluded() {
        // SplitMix64 only returns zero once in the output. Seeded with zero it outputs zero
        // at the end of the period.
        final UniformRandomProvider rng1 = RandomSource.SPLIT_MIX_64.create(0L);
        final UniformRandomProvider rng2 = RandomSource.SPLIT_MIX_64.create(0L);
        final double low = 1.23;
        final double high = 4.56;
        final SharedStateContinuousSampler sampler1 =
            ContinuousUniformSampler.of(rng1, low, high, false);
        final SharedStateContinuousSampler sampler2 =
            ContinuousUniformSampler.of(rng2, low, high, true);
        RandomAssert.assertProduceSameSequence(sampler1, sampler2);
    }
}
