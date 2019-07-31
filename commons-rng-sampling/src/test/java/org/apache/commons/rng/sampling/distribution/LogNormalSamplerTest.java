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

import org.apache.commons.rng.RestorableUniformRandomProvider;
import org.apache.commons.rng.UniformRandomProvider;
import org.apache.commons.rng.sampling.RandomAssert;
import org.apache.commons.rng.sampling.SharedStateSampler;
import org.apache.commons.rng.simple.RandomSource;
import org.junit.Test;

/**
 * Test for the {@link LogNormalSampler}. The tests hit edge cases for the sampler.
 */
public class LogNormalSamplerTest {
    /**
     * Test the constructor with a bad shape.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testConstructorThrowsWithNegativeScale() {
        final RestorableUniformRandomProvider rng =
            RandomSource.create(RandomSource.SPLIT_MIX_64);
        final NormalizedGaussianSampler gauss = new ZigguratNormalizedGaussianSampler(rng);
        final double scale = -1e-6;
        final double shape = 1;
        LogNormalSampler.of(gauss, scale, shape);
    }

    /**
     * Test the constructor with a bad shape.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testConstructorThrowsWithZeroShape() {
        final RestorableUniformRandomProvider rng =
            RandomSource.create(RandomSource.SPLIT_MIX_64);
        final NormalizedGaussianSampler gauss = new ZigguratNormalizedGaussianSampler(rng);
        final double scale = 1;
        final double shape = 0;
        LogNormalSampler.of(gauss, scale, shape);
    }

    /**
     * Test the SharedStateSampler implementation.
     */
    @Test
    public void testSharedStateSampler() {
        final UniformRandomProvider rng1 = RandomSource.create(RandomSource.SPLIT_MIX_64, 0L);
        final UniformRandomProvider rng2 = RandomSource.create(RandomSource.SPLIT_MIX_64, 0L);
        final NormalizedGaussianSampler gauss = new ZigguratNormalizedGaussianSampler(rng1);
        final double scale = 1.23;
        final double shape = 4.56;
        final SharedStateContinuousSampler sampler1 =
            LogNormalSampler.of(gauss, scale, shape);
        final SharedStateContinuousSampler sampler2 = sampler1.withUniformRandomProvider(rng2);
        RandomAssert.assertProduceSameSequence(sampler1, sampler2);
    }

    /**
     * Test the SharedStateSampler implementation throws if the underlying sampler is
     * not a SharedStateSampler.
     */
    @Test(expected = UnsupportedOperationException.class)
    public void testSharedStateSamplerThrowsIfUnderlyingSamplerDoesNotShareState() {
        final UniformRandomProvider rng2 = RandomSource.create(RandomSource.SPLIT_MIX_64, 0L);
        final NormalizedGaussianSampler gauss = new NormalizedGaussianSampler() {
            @Override
            public double sample() {
                return 0;
            }
        };
        final double scale = 1.23;
        final double shape = 4.56;
        final SharedStateContinuousSampler sampler1 =
            LogNormalSampler.of(gauss, scale, shape);
        sampler1.withUniformRandomProvider(rng2);
    }

    /**
     * Test the SharedStateSampler implementation throws if the underlying sampler is
     * a SharedStateSampler that returns an incorrect type.
     */
    @Test(expected = UnsupportedOperationException.class)
    public void testSharedStateSamplerThrowsIfUnderlyingSamplerReturnsWrongSharedState() {
        final UniformRandomProvider rng2 = RandomSource.create(RandomSource.SPLIT_MIX_64, 0L);
        final NormalizedGaussianSampler gauss = new BadSharedStateNormalizedGaussianSampler();
        final double scale = 1.23;
        final double shape = 4.56;
        final SharedStateContinuousSampler sampler1 =
            LogNormalSampler.of(gauss, scale, shape);
        sampler1.withUniformRandomProvider(rng2);
    }

    /**
     * Test class to return an incorrect sampler from the SharedStateSampler method.
     *
     * <p>Note that due to type erasure the type returned by the SharedStateSampler is not
     * available at run-time and the LogNormalSampler has to assume it is the correct type.</p>
     */
    private static class BadSharedStateNormalizedGaussianSampler
            implements NormalizedGaussianSampler, SharedStateSampler<Integer> {
        @Override
        public double sample() {
            return 0;
        }

        @Override
        public Integer withUniformRandomProvider(UniformRandomProvider rng) {
            // Something that is not a NormalizedGaussianSampler
            return Integer.valueOf(44);
        }
    }
}
