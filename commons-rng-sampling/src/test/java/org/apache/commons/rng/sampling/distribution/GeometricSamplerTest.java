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

/**
 * Test for the {@link GeometricSampler}. The tests hit edge cases for the sampler.
 */
public class GeometricSamplerTest {
    /**
     * Test the edge case where the probability of success is 1. This is a valid geometric
     * distribution where the sample should always be 0.
     */
    @Test
    public void testProbabilityOfSuccessIsOneGeneratesZeroForSamples() {
        final UniformRandomProvider rng = RandomSource.SPLIT_MIX_64.create(0L);
        final SharedStateDiscreteSampler sampler = GeometricSampler.of(rng, 1);
        // All samples should be 0
        for (int i = 0; i < 10; i++) {
            Assertions.assertEquals(0, sampler.sample(), "p=1 should have 0 for all samples");
        }
    }

    /**
     * Test to demonstrate that any probability of success under one produces a valid
     * mean for the exponential distribution.
     */
    @Test
    public void testProbabilityOfSuccessUnderOneIsValid() {
        // The sampler explicitly handles probabilityOfSuccess == 1 as an edge case.
        // Anything under it should be valid for sampling from an ExponentialDistribution.
        final double probabilityOfSuccess = Math.nextDown(1);
        // Map to the mean
        final double exponentialMean = 1.0 / (-Math.log1p(-probabilityOfSuccess));
        // As long as this is finite positive then the sampler is valid
        Assertions.assertTrue(exponentialMean > 0 && exponentialMean <= Double.MAX_VALUE);
        // The internal exponential sampler validates the mean so demonstrate creating a
        // geometric sampler does not throw.
        final UniformRandomProvider rng = RandomSource.SPLIT_MIX_64.create(0L);
        GeometricSampler.of(rng, probabilityOfSuccess);
    }

    /**
     * Test the edge case where the probability of success is 1 since it uses a different
     * {@link Object#toString()} method to the normal case tested elsewhere.
     */
    @Test
    public void testProbabilityOfSuccessIsOneSamplerToString() {
        final UniformRandomProvider unusedRng = RandomSource.SPLIT_MIX_64.create(0L);
        final SharedStateDiscreteSampler sampler = GeometricSampler.of(unusedRng, 1);
        Assertions.assertTrue(sampler.toString().contains("Geometric"),
            "Missing 'Geometric' from toString");
    }

    /**
     * Test the edge case where the probability of success is nearly 0. This is a valid geometric
     * distribution but the sample is clipped to max integer value because the underlying
     * exponential has a mean of positive infinity (effectively the sample is from a truncated
     * distribution).
     *
     * <p>This test can be changed in future if a lower bound limit for the probability of success
     * is introduced.
     */
    @Test
    public void testProbabilityOfSuccessIsAlmostZeroGeneratesMaxValueForSamples() {
        final UniformRandomProvider rng = RandomSource.SPLIT_MIX_64.create(0L);
        final SharedStateDiscreteSampler sampler = GeometricSampler.of(rng, Double.MIN_VALUE);
        // All samples should be max value
        for (int i = 0; i < 10; i++) {
            Assertions.assertEquals(Integer.MAX_VALUE, sampler.sample(),
                "p=(almost 0) should have Integer.MAX_VALUE for all samples");
        }
    }

    /**
     * Test probability of success {@code >1} is not allowed.
     */
    @Test
    public void testProbabilityOfSuccessAboveOneThrows() {
        final UniformRandomProvider unusedRng = RandomSource.SPLIT_MIX_64.create(0L);
        final double probabilityOfSuccess = Math.nextUp(1.0);
        Assertions.assertThrows(IllegalArgumentException.class,
            () -> GeometricSampler.of(unusedRng, probabilityOfSuccess));
    }

    /**
     * Test probability of success {@code 0} is not allowed.
     */
    @Test
    public void testProbabilityOfSuccessIsZeroThrows() {
        final UniformRandomProvider unusedRng = RandomSource.SPLIT_MIX_64.create(0L);
        final double probabilityOfSuccess = 0;
        Assertions.assertThrows(IllegalArgumentException.class,
            () -> GeometricSampler.of(unusedRng, probabilityOfSuccess));
    }

    /**
     * Test the SharedStateSampler implementation.
     */
    @Test
    public void testSharedStateSampler() {
        testSharedStateSampler(0.5);
    }

    /**
     * Test the SharedStateSampler implementation with the edge case when the probability of
     * success is {@code 1.0}.
     */
    @Test
    public void testSharedStateSamplerWithProbabilityOfSuccessOne() {
        testSharedStateSampler(1.0);
    }

    /**
     * Test the SharedStateSampler implementation.
     *
     * @param probabilityOfSuccess Probability of success.
     */
    private static void testSharedStateSampler(double probabilityOfSuccess) {
        final UniformRandomProvider rng1 = RandomSource.SPLIT_MIX_64.create(0L);
        final UniformRandomProvider rng2 = RandomSource.SPLIT_MIX_64.create(0L);
        final SharedStateDiscreteSampler sampler1 =
            GeometricSampler.of(rng1, probabilityOfSuccess);
        final SharedStateDiscreteSampler sampler2 = sampler1.withUniformRandomProvider(rng2);
        RandomAssert.assertProduceSameSequence(sampler1, sampler2);
    }
}
