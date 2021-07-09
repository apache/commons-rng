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
 * Test for the {@link LevySampler}.
 */
public class LevySamplerTest {
    /**
     * Test the constructor with a negative scale.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testConstructorThrowsWithNegativeScale() {
        final UniformRandomProvider rng = RandomSource.SPLIT_MIX_64.create(0L);
        final double location = 1;
        final double scale = -1e-6;
        LevySampler.of(rng, location, scale);
    }

    /**
     * Test the constructor with a zero scale.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testConstructorThrowsWithZeroScale() {
        final UniformRandomProvider rng = RandomSource.SPLIT_MIX_64.create(0L);
        final double location = 1;
        final double scale = 0;
        LevySampler.of(rng, location, scale);
    }

    /**
     * Test the SharedStateSampler implementation.
     */
    @Test
    public void testSharedStateSampler() {
        final UniformRandomProvider rng1 = RandomSource.SPLIT_MIX_64.create(0L);
        final UniformRandomProvider rng2 = RandomSource.SPLIT_MIX_64.create(0L);
        final double location = 4.56;
        final double scale = 1.23;
        final LevySampler sampler1 = LevySampler.of(rng1, location, scale);
        final LevySampler sampler2 = sampler1.withUniformRandomProvider(rng2);
        RandomAssert.assertProduceSameSequence(sampler1, sampler2);
    }

    /**
     * Test the support of the standard distribution is {@code [0, inf)}.
     */
    @Test
    public void testSupport() {
        final double location = 0.0;
        final double scale = 1.0;
        // Force the underlying ZigguratNormalizedGaussianSampler to create 0
        final LevySampler s1 = LevySampler.of(
            new SplitMix64(0L) {
                @Override
                public long next() {
                    return 0L;
                }
            }, location, scale);
        Assert.assertEquals(Double.POSITIVE_INFINITY, s1.sample(), 0.0);

        // Force the underlying ZigguratNormalizedGaussianSampler to create the largest value.
        // This is 14.11
        final LevySampler s2 = LevySampler.of(
            new SplitMix64(0L) {
                private int i;
                @Override
                public long next() {
                    i++;
                    if (i == 1) {
                        // Set the first value to ensure we sample the tail of the ziggurat.
                        // The lowest 7 bits are zero to select rectangle 0 from the ziggurat.
                        return (Long.MAX_VALUE << 7) & Long.MAX_VALUE;
                    }
                    if (i == 2) {
                        // Set the second value to generate y as the largest value possible by
                        // ensuring Math.log is called with a small value.
                        return 0L;
                    }
                    // The next value generates x which must be set to the largest value x which
                    // satisfies the condition:
                    // 2y >= x^2
                    return 1377L << 11;
                }
            }, location, scale);
        // The tail of the zigguart should be s=12.014118700751192
        // expected is 1/s^2 = 0.006928132149804786
        // This is as close to zero as the sampler can get.
        final double expected = 0.006928132149804786;
        Assert.assertEquals(expected, s2.sample(), 0.0);
    }
}
