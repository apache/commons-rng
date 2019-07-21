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
import org.junit.Assert;
import org.junit.Test;


/**
 * This test checks the {@link PoissonSampler} can be created
 * from a saved state.
 */
public class PoissonSamplerTest {
    /**
     * Test the SharedStateSampler implementation with a mean below 40.
     */
    @Test
    public void testSharedStateSamplerWithSmallMean() {
        testSharedStateSampler(34.5);
    }

    /**
     * Test the SharedStateSampler implementation with a mean above 40.
     */
    @Test
    public void testSharedStateSamplerWithLargeMean() {
        testSharedStateSampler(67.8);
    }

    /**
     * Test the SharedStateSampler implementation.
     *
     * @param mean Mean.
     */
    private static void testSharedStateSampler(double mean) {
        final UniformRandomProvider rng1 = RandomSource.create(RandomSource.SPLIT_MIX_64, 0L);
        final UniformRandomProvider rng2 = RandomSource.create(RandomSource.SPLIT_MIX_64, 0L);
        // Use instance constructor not factory constructor to exercise 1.X public API
        final SharedStateDiscreteSampler sampler1 =
            new PoissonSampler(rng1, mean);
        final SharedStateDiscreteSampler sampler2 = sampler1.withUniformRandomProvider(rng2);
        RandomAssert.assertProduceSameSequence(sampler1, sampler2);
    }

    /**
     * Test the toString method. This is added to ensure coverage as the factory constructor
     * used in other tests does not create an instance of the wrapper class.
     */
    @Test
    public void testToString() {
        final UniformRandomProvider rng = RandomSource.create(RandomSource.SPLIT_MIX_64, 0L);
        Assert.assertTrue(new PoissonSampler(rng, 1.23).toString().toLowerCase().contains("poisson"));
    }
}
