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
import org.apache.commons.rng.simple.RandomSource;
import org.junit.Assert;
import org.junit.Test;

/**
 * Test for the {@link AhrensDieterMarsagliaTsangGammaSampler}. The tests hit edge cases for the sampler.
 */
public class AhrensDieterMarsagliaTsangGammaSamplerTest {
    /**
     * Test the constructor with a bad alpha.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testConstructorThrowsWithZeroAlpha() {
        final RestorableUniformRandomProvider rng =
            RandomSource.create(RandomSource.SPLIT_MIX_64, 0L);
        final double alpha = 0;
        final double theta = 1;
        AhrensDieterMarsagliaTsangGammaSampler.of(rng, alpha, theta);
    }

    /**
     * Test the constructor with a bad theta.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testConstructorThrowsWithZeroTheta() {
        final RestorableUniformRandomProvider rng =
            RandomSource.create(RandomSource.SPLIT_MIX_64, 0L);
        final double alpha = 1;
        final double theta = 0;
        AhrensDieterMarsagliaTsangGammaSampler.of(rng, alpha, theta);
    }

    /**
     * Test the SharedStateSampler implementation.
     */
    @Test
    public void testSharedStateSamplerWithAlphaBelowOne() {
        testSharedStateSampler(0.5, 3.456);
    }

    /**
     * Test the SharedStateSampler implementation.
     */
    @Test
    public void testSharedStateSamplerWithAlphaAboveOne() {
        testSharedStateSampler(3.5, 3.456);
    }

    /**
     * Test the SharedStateSampler implementation.
     *
     * @param alpha Alpha.
     * @param theta Theta.
     */
    private static void testSharedStateSampler(double alpha, double theta) {
        final UniformRandomProvider rng1 = RandomSource.create(RandomSource.SPLIT_MIX_64, 0L);
        final UniformRandomProvider rng2 = RandomSource.create(RandomSource.SPLIT_MIX_64, 0L);
        // Use instance constructor not factory constructor to exercise 1.X public API
        final AhrensDieterMarsagliaTsangGammaSampler sampler1 =
            new AhrensDieterMarsagliaTsangGammaSampler(rng1, alpha, theta);
        final SharedStateContinuousSampler sampler2 = sampler1.withUniformRandomProvider(rng2);
        RandomAssert.assertProduceSameSequence(sampler1, sampler2);
    }

    /**
     * Test the toString method. This is added to ensure coverage as the factory constructor
     * used in other tests does not create an instance of the wrapper class.
     */
    @Test
    public void testToString() {
        final UniformRandomProvider rng = RandomSource.create(RandomSource.SPLIT_MIX_64, 0L);
        Assert.assertTrue(new AhrensDieterMarsagliaTsangGammaSampler(rng, 1.0, 2.0).toString()
                .toLowerCase().contains("gamma"));
    }
}
