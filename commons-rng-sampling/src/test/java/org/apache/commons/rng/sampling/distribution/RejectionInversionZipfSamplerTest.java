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
 * Test for the {@link RejectionInversionZipfSampler}. The tests hit edge cases for the sampler.
 */
public class RejectionInversionZipfSamplerTest {
    /**
     * Test the constructor with a bad number of elements.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testConstructorThrowsWithZeroNumberOfElements() {
        final RestorableUniformRandomProvider rng =
            RandomSource.SPLIT_MIX_64.create(0L);
        final int numberOfElements = 0;
        final double exponent = 1;
        RejectionInversionZipfSampler.of(rng, numberOfElements, exponent);
    }

    /**
     * Test the constructor with a bad exponent.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testConstructorThrowsWithZeroExponent() {
        final RestorableUniformRandomProvider rng =
            RandomSource.SPLIT_MIX_64.create(0L);
        final int numberOfElements = 1;
        final double exponent = 0;
        RejectionInversionZipfSampler.of(rng, numberOfElements, exponent);
    }

    /**
     * Test the SharedStateSampler implementation.
     */
    @Test
    public void testSharedStateSampler() {
        final UniformRandomProvider rng1 = RandomSource.SPLIT_MIX_64.create(0L);
        final UniformRandomProvider rng2 = RandomSource.SPLIT_MIX_64.create(0L);
        final int numberOfElements = 7;
        final double exponent = 1.23;
        final SharedStateDiscreteSampler sampler1 =
            RejectionInversionZipfSampler.of(rng1, numberOfElements, exponent);
        final SharedStateDiscreteSampler sampler2 = sampler1.withUniformRandomProvider(rng2);
        RandomAssert.assertProduceSameSequence(sampler1, sampler2);
    }
}
