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
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Test for the {@link SmallMeanPoissonSampler}. The tests hit edge cases for the sampler.
 */
class SmallMeanPoissonSamplerTest {
    /**
     * Test the constructor with a bad mean.
     */
    @Test
    void testConstructorThrowsWithMeanThatSetsProbabilityP0ToZero() {
        final UniformRandomProvider rng =
            RandomSource.SPLIT_MIX_64.create(0L);
        final double p0 = Double.MIN_VALUE;
        // Note: p0 = Math.exp(-mean) => mean = -Math.log(p0).
        // Add to the limit on the mean to cause p0 to be zero.
        final double mean = -Math.log(p0) + 1;
        Assertions.assertThrows(IllegalArgumentException.class,
            () -> SmallMeanPoissonSampler.of(rng, mean));
    }

    /**
     * Test the constructor with a bad mean.
     */
    @Test
    void testConstructorThrowsWithZeroMean() {
        final UniformRandomProvider rng =
            RandomSource.SPLIT_MIX_64.create(0L);
        final double mean = 0;
        Assertions.assertThrows(IllegalArgumentException.class,
            () -> SmallMeanPoissonSampler.of(rng, mean));
    }

    /**
     * Test the sample is bounded to 1000 * mean.
     */
    @ParameterizedTest
    @ValueSource(doubles = {0.5, 1, 1.5, 2.2})
    void testSampleUpperBounds(double mean) {
        // If the nextDouble() is always ~1 then the sample will hit the upper bounds.
        // nextLong() returns -1; nextDouble returns Math.nextDown(1.0).
        final UniformRandomProvider rng = () -> -1;
        final SharedStateDiscreteSampler sampler = SmallMeanPoissonSampler.of(rng, mean);
        final int expected = (int) Math.ceil(1000 * mean);
        Assertions.assertEquals(expected, sampler.sample());
    }

    /**
     * Test the SharedStateSampler implementation.
     */
    @Test
    void testSharedStateSampler() {
        final UniformRandomProvider rng1 = RandomSource.SPLIT_MIX_64.create(0L);
        final UniformRandomProvider rng2 = RandomSource.SPLIT_MIX_64.create(0L);
        final double mean = 1.23;
        final SharedStateDiscreteSampler sampler1 =
            SmallMeanPoissonSampler.of(rng1, mean);
        final SharedStateDiscreteSampler sampler2 = sampler1.withUniformRandomProvider(rng2);
        RandomAssert.assertProduceSameSequence(sampler1, sampler2);
    }
}
