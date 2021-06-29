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

        // Force the underlying ZigguratNormalizedGaussianSampler to create +inf
        final LevySampler s2 = LevySampler.of(
            new SplitMix64(0L) {
                @Override
                public long next() {
                    return (Long.MAX_VALUE << 7) & Long.MAX_VALUE;
                }
                @Override
                public double nextDouble() {
                    return 0.0;
                }
            }, location, scale);
        Assert.assertEquals(0.0, s2.sample(), 0.0);
    }
}
