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

import org.junit.Test;
import org.apache.commons.rng.RestorableUniformRandomProvider;
import org.apache.commons.rng.UniformRandomProvider;
import org.apache.commons.rng.sampling.RandomAssert;
import org.apache.commons.rng.simple.RandomSource;

/**
 * Test for {@link ZigguratSampler}.
 */
public class ZigguratSamplerTest {
    /**
     * Test the exponential constructor with a bad mean.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testExponentialConstructorThrowsWithZeroMean() {
        final RestorableUniformRandomProvider rng = RandomSource.SPLIT_MIX_64.create(0L);
        final double mean = 0;
        ZigguratSampler.Exponential.of(rng, mean);
    }

    /**
     * Test the exponential SharedStateSampler implementation.
     */
    @Test
    public void testExponentialSharedStateSampler() {
        final UniformRandomProvider rng1 = RandomSource.SPLIT_MIX_64.create(0L);
        final UniformRandomProvider rng2 = RandomSource.SPLIT_MIX_64.create(0L);
        final ZigguratSampler.Exponential sampler1 = ZigguratSampler.Exponential.of(rng1);
        final ZigguratSampler.Exponential sampler2 = sampler1.withUniformRandomProvider(rng2);
        RandomAssert.assertProduceSameSequence(sampler1, sampler2);
    }

    /**
     * Test the exponential SharedStateSampler implementation with a mean.
     */
    @Test
    public void testExponentialSharedStateSamplerWithMean() {
        final UniformRandomProvider rng1 = RandomSource.SPLIT_MIX_64.create(0L);
        final UniformRandomProvider rng2 = RandomSource.SPLIT_MIX_64.create(0L);
        final double mean = 1.23;
        final ZigguratSampler.Exponential sampler1 = ZigguratSampler.Exponential.of(rng1, mean);
        final ZigguratSampler.Exponential sampler2 = sampler1.withUniformRandomProvider(rng2);
        RandomAssert.assertProduceSameSequence(sampler1, sampler2);
    }

    /**
     * Test the Gaussian SharedStateSampler implementation.
     */
    @Test
    public void testGaussianSharedStateSampler() {
        final UniformRandomProvider rng1 = RandomSource.SPLIT_MIX_64.create(0L);
        final UniformRandomProvider rng2 = RandomSource.SPLIT_MIX_64.create(0L);
        final ZigguratSampler.NormalizedGaussian sampler1 = ZigguratSampler.NormalizedGaussian.of(rng1);
        final ZigguratSampler.NormalizedGaussian sampler2 = sampler1.withUniformRandomProvider(rng2);
        RandomAssert.assertProduceSameSequence(sampler1, sampler2);
    }
}
