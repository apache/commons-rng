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
import org.apache.commons.rng.sampling.SharedStateSampler;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Test for the {@link LogNormalSampler}. The tests hit edge cases for the sampler.
 */
class LogNormalSamplerTest {
    /**
     * Test the constructor with a bad standard deviation.
     */
    @Test
    void testConstructorThrowsWithZeroShape() {
        final UniformRandomProvider rng = RandomAssert.seededRNG();
        final NormalizedGaussianSampler gauss = ZigguratSampler.NormalizedGaussian.of(rng);
        final double mu = 1;
        final double sigma = 0;
        Assertions.assertThrows(IllegalArgumentException.class,
            () -> LogNormalSampler.of(gauss, mu, sigma));
    }

    /**
     * Test the SharedStateSampler implementation.
     */
    @Test
    void testSharedStateSampler() {
        final UniformRandomProvider rng1 = RandomAssert.seededRNG();
        final UniformRandomProvider rng2 = RandomAssert.seededRNG();
        final NormalizedGaussianSampler gauss = ZigguratSampler.NormalizedGaussian.of(rng1);
        // Test a negative mean is allowed (see RNG-166)
        final double mu = -1.23;
        final double sigma = 4.56;
        final SharedStateContinuousSampler sampler1 =
            LogNormalSampler.of(gauss, mu, sigma);
        final SharedStateContinuousSampler sampler2 = sampler1.withUniformRandomProvider(rng2);
        RandomAssert.assertProduceSameSequence(sampler1, sampler2);
    }

    /**
     * Test the SharedStateSampler implementation throws if the underlying sampler is
     * not a SharedStateSampler.
     */
    @Test
    void testSharedStateSamplerThrowsIfUnderlyingSamplerDoesNotShareState() {
        final UniformRandomProvider rng2 = RandomAssert.seededRNG();
        final NormalizedGaussianSampler gauss = new NormalizedGaussianSampler() {
            @Override
            public double sample() {
                return 0;
            }
        };
        final double mu = 1.23;
        final double sigma = 4.56;
        final SharedStateContinuousSampler sampler1 =
            LogNormalSampler.of(gauss, mu, sigma);
        Assertions.assertThrows(UnsupportedOperationException.class,
            () -> sampler1.withUniformRandomProvider(rng2));
    }

    /**
     * Test the SharedStateSampler implementation throws if the underlying sampler is
     * a SharedStateSampler that returns an incorrect type.
     */
    @Test
    void testSharedStateSamplerThrowsIfUnderlyingSamplerReturnsWrongSharedState() {
        final UniformRandomProvider rng2 = RandomAssert.seededRNG();
        final NormalizedGaussianSampler gauss = new BadSharedStateNormalizedGaussianSampler();
        final double mu = 1.23;
        final double sigma = 4.56;
        final SharedStateContinuousSampler sampler1 =
            LogNormalSampler.of(gauss, mu, sigma);
        Assertions.assertThrows(UnsupportedOperationException.class,
            () -> sampler1.withUniformRandomProvider(rng2));
    }

    /**
     * Test class to return an incorrect sampler from the SharedStateSampler method.
     *
     * <p>Note that due to type erasure the type returned by the SharedStateSampler is not
     * available at run-time and the LogNormalSampler has to assume it is the correct type.</p>
     */
    private static final class BadSharedStateNormalizedGaussianSampler
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
