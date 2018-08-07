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
import org.apache.commons.rng.sampling.distribution.LargeMeanPoissonSampler.LargeMeanPoissonSamplerState;
import org.apache.commons.rng.simple.RandomSource;
import org.junit.Assert;
import org.junit.Test;

/**
 * This test checks the {@link LargeMeanPoissonSampler} using the 
 * {@link LargeMeanPoissonSamplerState}.
 */
public class LargeMeanPoissonSamplerTest {

    // Edge cases for construction

    /**
     * Test the constructor with a bad mean.
     */
    @Test(expected=IllegalArgumentException.class)
    public void testConstructorThrowsWithZeroMean() {
        final RestorableUniformRandomProvider rng =
                RandomSource.create(RandomSource.SPLIT_MIX_64);
        @SuppressWarnings("unused")
        LargeMeanPoissonSampler sampler = new LargeMeanPoissonSampler(rng, 0);
    }

    /**
     * Test the state cannot be created with a negative n.
     */
    @Test(expected=IllegalArgumentException.class)
    public void testStateCreationThrowsWithNegativeN() {
        @SuppressWarnings("unused")
        LargeMeanPoissonSamplerState state = LargeMeanPoissonSamplerState.create(-1);
    }

    /**
     * Test the constructor with a negative fractional mean.
     */
    @Test(expected=IllegalArgumentException.class)
    public void testConstructorThrowsWithNegativeFractionalMean() {
        final RestorableUniformRandomProvider rng =
                RandomSource.create(RandomSource.SPLIT_MIX_64);
        LargeMeanPoissonSamplerState state = LargeMeanPoissonSamplerState.create(0);
        @SuppressWarnings("unused")
        LargeMeanPoissonSampler sampler = new LargeMeanPoissonSampler(rng, state, -0.1);
    }

    /**
     * Test the constructor with a non-fractional mean.
     */
    @Test(expected=IllegalArgumentException.class)
    public void testConstructorThrowsWithNonFractionalMean() {
        final RestorableUniformRandomProvider rng =
                RandomSource.create(RandomSource.SPLIT_MIX_64);
        LargeMeanPoissonSamplerState state = LargeMeanPoissonSamplerState.create(0);
        @SuppressWarnings("unused")
        LargeMeanPoissonSampler sampler = new LargeMeanPoissonSampler(rng, state, 1.1);
    }

    /**
     * Test the constructor with fractional mean of 1.
     */
    @Test(expected=IllegalArgumentException.class)
    public void testConstructorThrowsWithFractionalMeanOne() {
        final RestorableUniformRandomProvider rng =
                RandomSource.create(RandomSource.SPLIT_MIX_64);
        LargeMeanPoissonSamplerState state = LargeMeanPoissonSamplerState.create(0);
        @SuppressWarnings("unused")
        LargeMeanPoissonSampler sampler = new LargeMeanPoissonSampler(rng, state, 1);
    }

    // Sampling tests

    /**
     * Test the {@link LargeMeanPoissonSampler} returns the same samples when it
     * is created using the {@link LargeMeanPoissonSamplerState}.
     */
    @Test
    public void testCanComputeSameSamplesWhenConstructedWithState() {
        // Two identical RNGs
        final RestorableUniformRandomProvider rng1 =
                RandomSource.create(RandomSource.WELL_19937_C);
        final RandomProviderState state = rng1.saveState();
        final RestorableUniformRandomProvider rng2 =
                RandomSource.create(RandomSource.WELL_19937_C);
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
     * Test poisson samples are the same from the {@link PoissonSampler}
     * and {@link PoissonSamplerCache}. The random providers must be
     * identical (including state).
     *
     * @param rng1  the first random provider
     * @param rng2  the second random provider
     * @param cache the cache
     * @param mean  the mean
     */
    private static void testPoissonSamples(
            final RestorableUniformRandomProvider rng1,
            final RestorableUniformRandomProvider rng2,
            double mean) {
        final DiscreteSampler s1 = new LargeMeanPoissonSampler(rng1, mean);
        int n = (int) Math.floor(mean);
        double lambdaFractional = mean - n;
        LargeMeanPoissonSamplerState state = LargeMeanPoissonSamplerState.create(n);
        final DiscreteSampler s2 = new LargeMeanPoissonSampler(rng2, state, lambdaFractional);
        for (int j = 0; j < 10; j++)
            Assert.assertEquals(s1.sample(), s2.sample());
    }
}
