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
import org.junit.Test;

/**
 * Test for the {@link ChengBetaSampler}. The tests hit edge cases for the sampler.
 */
public class ChengBetaSamplerTest {
    /**
     * Test the constructor with a bad alpha.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testConstructorThrowsWithZeroAlpha() {
        final RestorableUniformRandomProvider rng =
            RandomSource.create(RandomSource.SPLIT_MIX_64);
        final double alpha = 0;
        final double beta = 1;
        @SuppressWarnings("unused")
        final ChengBetaSampler sampler =
            new ChengBetaSampler(rng, alpha, beta);
    }

    /**
     * Test the constructor with a bad beta.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testConstructorThrowsWithZeroBeta() {
        final RestorableUniformRandomProvider rng =
            RandomSource.create(RandomSource.SPLIT_MIX_64);
        final double alpha = 1;
        final double beta = 0;
        @SuppressWarnings("unused")
        final ChengBetaSampler sampler =
            new ChengBetaSampler(rng, alpha, beta);
    }

    /**
     * Test the SharedStateSampler implementation.
     */
    @Test
    public void testSharedStateSampler() {
        final UniformRandomProvider rng1 = RandomSource.create(RandomSource.SPLIT_MIX_64, 0L);
        final UniformRandomProvider rng2 = RandomSource.create(RandomSource.SPLIT_MIX_64, 0L);
        final double alpha = 1.23;
        final double beta = 4.56;
        final ChengBetaSampler sampler1 =
            new ChengBetaSampler(rng1, alpha, beta);
        final ChengBetaSampler sampler2 = sampler1.withUniformRandomProvider(rng2);
        RandomAssert.assertProduceSameSequence(sampler1, sampler2);
    }
}
