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
import org.apache.commons.rng.core.source32.IntProvider;
import org.apache.commons.rng.sampling.RandomAssert;
import org.apache.commons.rng.simple.RandomSource;
import org.junit.Assert;
import org.junit.Test;

/**
 * Test for the {@link MarsagliaNormalizedGaussianSampler}.
 */
public class MarsagliaNormalisedGaussianSamplerTest {
    /**
     * Test the SharedStateSampler implementation.
     */
    @Test
    public void testSharedStateSampler() {
        final UniformRandomProvider rng1 = RandomSource.create(RandomSource.SPLIT_MIX_64, 0L);
        final UniformRandomProvider rng2 = RandomSource.create(RandomSource.SPLIT_MIX_64, 0L);
        final SharedStateContinuousSampler sampler1 =
            MarsagliaNormalizedGaussianSampler.<MarsagliaNormalizedGaussianSampler>of(rng1);
        final SharedStateContinuousSampler sampler2 = sampler1.withUniformRandomProvider(rng2);
        RandomAssert.assertProduceSameSequence(sampler1, sampler2);
    }

    /**
     * Test the edge case where the pair of samples are rejected. This occurs when the distance
     * of the pair is outside the unit circle or lies on the origin.
     */
    @Test
    public void testSamplePairIsRejected() {
        final double value = 0.25;
        final UniformRandomProvider rng = new IntProvider() {
            private int i;

            @Override
            public int next() {
                // Not used
                return 0;
            }

            @Override
            public double nextDouble() {
                i++;
                if (i <= 2) {
                    // First two samples are one.
                    // This is outside the unit circle.
                    return 1.0;
                }
                if (i <= 4) {
                    // Next two samples are 0.5.
                    // The pair lies at the origin.
                    return 0.5;
                }
                return value;
            }
        };

        final MarsagliaNormalizedGaussianSampler sampler = new MarsagliaNormalizedGaussianSampler(rng);

        // Compute as per the algorithm
        final double x = 2 * value - 1;
        final double r2 = x * x + x * x;
        final double expected = x * Math.sqrt(-2 * Math.log(r2) / r2);
        Assert.assertEquals(expected, sampler.sample(), 0);
    }
}
