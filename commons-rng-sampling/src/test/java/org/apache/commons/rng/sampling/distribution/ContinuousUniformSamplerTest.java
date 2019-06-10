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
        final UniformRandomProvider rng = RandomSource.create(RandomSource.SPLIT_MIX_64);
        testSampleInRange(rng, low, high);
        testSampleInRange(rng, high, low);
    }

    private static void testSampleInRange(UniformRandomProvider rng,
                                          double low, double high) {
        ContinuousUniformSampler sampler = new ContinuousUniformSampler(rng, low, high);
        final double min = Math.min(low,  high);
        final double max = Math.max(low,  high);
        for (int i = 0; i < 10; i++) {
            final double value = sampler.sample();
            Assert.assertTrue("Value not in range", value >= min && value <= max);
        }
    }

    /**
     * Test the SharedStateSampler implementation.
     */
    @Test
    public void testSharedStateSampler() {
        final UniformRandomProvider rng1 = RandomSource.create(RandomSource.SPLIT_MIX_64, 0L);
        final UniformRandomProvider rng2 = RandomSource.create(RandomSource.SPLIT_MIX_64, 0L);
        final double low = 1.23;
        final double high = 4.56;
        final ContinuousUniformSampler sampler1 =
            new ContinuousUniformSampler(rng1, low, high);
        final ContinuousUniformSampler sampler2 = sampler1.withUniformRandomProvider(rng2);
        RandomAssert.assertProduceSameSequence(sampler1, sampler2);
    }
}
