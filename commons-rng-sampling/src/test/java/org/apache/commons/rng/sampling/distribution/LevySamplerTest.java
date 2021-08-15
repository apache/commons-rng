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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Test for the {@link LevySampler}.
 */
public class LevySamplerTest {
    /**
     * Test the constructor with a negative scale.
     */
    @Test
    public void testConstructorThrowsWithNegativeScale() {
        final UniformRandomProvider rng = RandomSource.SPLIT_MIX_64.create(0L);
        final double location = 1;
        final double scale = -1e-6;
        Assertions.assertThrows(IllegalArgumentException.class,
            () -> LevySampler.of(rng, location, scale));
    }

    /**
     * Test the constructor with a zero scale.
     */
    @Test
    public void testConstructorThrowsWithZeroScale() {
        final UniformRandomProvider rng = RandomSource.SPLIT_MIX_64.create(0L);
        final double location = 1;
        final double scale = 0;
        Assertions.assertThrows(IllegalArgumentException.class,
            () -> LevySampler.of(rng, location, scale));
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
        // Force the underlying ZigguratSampler.NormalizedGaussian to create 0
        final LevySampler s1 = LevySampler.of(
            new SplitMix64(0L) {
                @Override
                public long next() {
                    return 0L;
                }
            }, location, scale);
        Assertions.assertEquals(Double.POSITIVE_INFINITY, s1.sample());

        // Force the underlying ZigguratSampler.NormalizedGaussian to create a large
        // sample in the tail of the distribution.
        // The first two -1,-1 values enters the tail of the distribution.
        // Here an exponential is added to 3.6360066255009455861.
        // The exponential also requires -1,-1 to recurse. Each recursion adds 7.569274694148063
        // to the exponential. A value of 0 stops recursion with a sample of 0.
        // Two exponentials are required: x and y.
        // The exponential is multiplied by 0.27502700159745347 to create x.
        // The condition 2y >= x^x must be true to return x.
        // Create x = 4 * 7.57 and y = 16 * 7.57
        final long[] sequence = {
            // Sample the Gaussian tail
            -1, -1,
            // Exponential x = 4 * 7.57... * 0.275027001597525
            -1, -1, -1, -1, -1, -1, -1, -1, 0,
            // Exponential y = 16 * 7.57...
            -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 0,
        };
        final LevySampler s2 = LevySampler.of(
            new SplitMix64(0L) {
                private int i;
                @Override
                public long next() {
                    if (i++ < sequence.length) {
                        return sequence[i - 1];
                    }
                    return super.next();
                }
            }, location, scale);
        // The tail of the zigguart should be approximately s=11.963
        final double s = 4 * 7.569274694148063 * 0.27502700159745347 + 3.6360066255009455861;
        // expected is 1/s^2 = 0.006987
        // So the sampler never achieves the lower bound of zero.
        // It requires an extreme deviate from the Gaussian.
        final double expected = 1 / (s * s);
        Assertions.assertEquals(expected, s2.sample());
    }
}
