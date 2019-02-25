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
import org.apache.commons.rng.simple.RandomSource;
import org.junit.Assert;
import org.junit.Test;

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
        final UniformRandomProvider rng = RandomSource.create(RandomSource.SPLIT_MIX_64);
        final GeometricSampler sampler = new GeometricSampler(rng, 1);
        // All samples should be 0
        for (int i = 0; i < 10; i++) {
            Assert.assertEquals("p=1 should have 0 for all samples", 0, sampler.sample());
        }
    }

    /**
     * Test the edge case where the probability of success is 1 since it uses a different
     * {@link Object#toString()} method to the normal case tested elsewhere.
     */
    @Test
    public void testProbabilityOfSuccessIsOneSamplerToString() {
        final UniformRandomProvider unusedRng = RandomSource.create(RandomSource.SPLIT_MIX_64);
        final GeometricSampler sampler = new GeometricSampler(unusedRng, 1);
        Assert.assertTrue("Missing 'Geometric' from toString",
            sampler.toString().contains("Geometric"));
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
        final UniformRandomProvider rng = RandomSource.create(RandomSource.SPLIT_MIX_64);
        final GeometricSampler sampler = new GeometricSampler(rng, Double.MIN_VALUE);
        // All samples should be max value
        for (int i = 0; i < 10; i++) {
            Assert.assertEquals("p=(almost 0) should have Integer.MAX_VALUE for all samples",
                Integer.MAX_VALUE, sampler.sample());
        }
    }

    /**
     * Test probability of success {@code >1} is not allowed.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testProbabilityOfSuccessAboveOneThrows() {
        final UniformRandomProvider unusedRng = RandomSource.create(RandomSource.SPLIT_MIX_64);
        final double probabilityOfSuccess = Math.nextUp(1.0);
        @SuppressWarnings("unused")
        final GeometricSampler sampler = new GeometricSampler(unusedRng, probabilityOfSuccess);
    }

    /**
     * Test probability of success {@code 0} is not allowed.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testProbabilityOfSuccessIsZeroThrows() {
        final UniformRandomProvider unusedRng = RandomSource.create(RandomSource.SPLIT_MIX_64);
        final double probabilityOfSuccess = 0;
        @SuppressWarnings("unused")
        final GeometricSampler sampler = new GeometricSampler(unusedRng, probabilityOfSuccess);
    }
}
