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

import org.apache.commons.rng.RandomProviderState;
import org.apache.commons.rng.RestorableUniformRandomProvider;
import org.apache.commons.rng.UniformRandomProvider;
import org.apache.commons.rng.sampling.RandomAssert;
import org.apache.commons.rng.sampling.distribution.LargeMeanPoissonSampler.LargeMeanPoissonSamplerState;
import org.apache.commons.rng.simple.RandomSource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * This test checks the {@link LargeMeanPoissonSampler} can be created
 * from a saved state.
 */
public class LargeMeanPoissonSamplerTest {

    // Edge cases for construction

    /**
     * Test the constructor with a bad mean.
     */
    @Test
    public void testConstructorThrowsWithMeanLargerThanUpperBound() {
        final RestorableUniformRandomProvider rng =
                RandomSource.SPLIT_MIX_64.create(0L);
        final double mean = Integer.MAX_VALUE / 2 + 1;
        Assertions.assertThrows(IllegalArgumentException.class,
            () -> LargeMeanPoissonSampler.of(rng, mean));
    }

    /**
     * Test the constructor with a mean below 1.
     */
    @Test
    public void testConstructorThrowsWithMeanBelow1() {
        final RestorableUniformRandomProvider rng =
                RandomSource.SPLIT_MIX_64.create(0L);
        final double mean = Math.nextDown(1);
        Assertions.assertThrows(IllegalArgumentException.class,
            () -> LargeMeanPoissonSampler.of(rng, mean));
    }

    /**
     * Test the constructor using the state with a negative fractional mean.
     */
    @Test
    public void testConstructorThrowsWithStateAndNegativeFractionalMean() {
        final RestorableUniformRandomProvider rng =
                RandomSource.SPLIT_MIX_64.create(0L);
        final LargeMeanPoissonSamplerState state = new LargeMeanPoissonSampler(rng, 1).getState();
        Assertions.assertThrows(IllegalArgumentException.class,
            () -> new LargeMeanPoissonSampler(rng, state, -0.1));
    }

    /**
     * Test the constructor with a non-fractional mean.
     */
    @Test
    public void testConstructorThrowsWithStateAndNonFractionalMean() {
        final RestorableUniformRandomProvider rng =
                RandomSource.SPLIT_MIX_64.create(0L);
        final LargeMeanPoissonSamplerState state = new LargeMeanPoissonSampler(rng, 1).getState();
        Assertions.assertThrows(IllegalArgumentException.class,
            () -> new LargeMeanPoissonSampler(rng, state, 1.1));
    }

    /**
     * Test the constructor with fractional mean of 1.
     */
    @Test
    public void testConstructorThrowsWithStateAndFractionalMeanOne() {
        final RestorableUniformRandomProvider rng =
                RandomSource.SPLIT_MIX_64.create(0L);
        final LargeMeanPoissonSamplerState state = new LargeMeanPoissonSampler(rng, 1).getState();
        Assertions.assertThrows(IllegalArgumentException.class,
            () -> new LargeMeanPoissonSampler(rng, state, 1));
    }

    // Sampling tests

    /**
     * Test the {@link LargeMeanPoissonSampler} returns the same samples when it
     * is created using the saved state.
     */
    @Test
    public void testCanComputeSameSamplesWhenConstructedWithState() {
        // Two identical RNGs
        final RestorableUniformRandomProvider rng1 =
                RandomSource.MWC_256.create();
        final RandomProviderState state = rng1.saveState();
        final RestorableUniformRandomProvider rng2 =
                RandomSource.MWC_256.create();
        rng2.restoreState(state);

        // The sampler is suitable for mean > 40
        for (int i = 40; i < 44; i++) {
            // Test integer mean (no SmallMeanPoissonSampler required)
            testPoissonSamples(rng1, rng2, i);
            // Test non-integer mean (SmallMeanPoissonSampler required)
            testPoissonSamples(rng1, rng2, i + 0.5);
        }
    }

    /**
     * Test the {@link LargeMeanPoissonSampler} returns the same samples when it
     * is created using the saved state. The random providers must be
     * identical (including state).
     *
     * @param rng1  the first random provider
     * @param rng2  the second random provider
     * @param mean  the mean
     */
    private static void testPoissonSamples(
            final RestorableUniformRandomProvider rng1,
            final RestorableUniformRandomProvider rng2,
            double mean) {
        final LargeMeanPoissonSampler s1 = new LargeMeanPoissonSampler(rng1, mean);
        final int n = (int) Math.floor(mean);
        final double lambdaFractional = mean - n;
        final LargeMeanPoissonSamplerState state1 = s1.getState();
        final LargeMeanPoissonSampler s2 = new LargeMeanPoissonSampler(rng2, state1, lambdaFractional);
        final LargeMeanPoissonSamplerState state2 = s2.getState();
        Assertions.assertEquals(state1.getLambda(), state2.getLambda(), "State lambdas are not equal");
        Assertions.assertNotSame(state1, state2, "States are the same object");
        RandomAssert.assertProduceSameSequence(s1, s2);
    }

    /**
     * Test the SharedStateSampler implementation.
     */
    @Test
    public void testSharedStateSamplerWithFractionalMean() {
        testSharedStateSampler(34.5);
    }

    /**
     * Test the SharedStateSampler implementation with the edge case when there is no
     * small mean sampler (i.e. no fraction part to the mean).
     */
    @Test
    public void testSharedStateSamplerWithIntegerMean() {
        testSharedStateSampler(34.0);
    }

    /**
     * Test the SharedStateSampler implementation.
     *
     * @param mean Mean.
     */
    private static void testSharedStateSampler(double mean) {
        final UniformRandomProvider rng1 = RandomSource.SPLIT_MIX_64.create(0L);
        final UniformRandomProvider rng2 = RandomSource.SPLIT_MIX_64.create(0L);
        final SharedStateDiscreteSampler sampler1 =
            LargeMeanPoissonSampler.of(rng1, mean);
        final SharedStateDiscreteSampler sampler2 = sampler1.withUniformRandomProvider(rng2);
        RandomAssert.assertProduceSameSequence(sampler1, sampler2);
    }
}
