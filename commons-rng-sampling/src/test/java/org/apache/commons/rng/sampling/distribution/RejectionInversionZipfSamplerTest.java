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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assertions;

/**
 * Test for the {@link RejectionInversionZipfSampler}. The tests hit edge cases for the sampler.
 */
class RejectionInversionZipfSamplerTest {
    /**
     * Test the constructor with a bad number of elements.
     */
    @Test
    void testConstructorThrowsWithZeroNumberOfElements() {
        final RestorableUniformRandomProvider rng =
            RandomSource.SPLIT_MIX_64.create(0L);
        final int numberOfElements = 0;
        final double exponent = 1;
        Assertions.assertThrows(IllegalArgumentException.class,
            () -> RejectionInversionZipfSampler.of(rng, numberOfElements, exponent));
    }

    /**
     * Test the constructor with a bad exponent.
     */
    @Test
    void testConstructorThrowsWithNegativeExponent() {
        final RestorableUniformRandomProvider rng =
            RandomSource.SPLIT_MIX_64.create(0L);
        final int numberOfElements = 1;
        final double exponent = Math.nextDown(0);
        Assertions.assertThrows(IllegalArgumentException.class,
            () -> RejectionInversionZipfSampler.of(rng, numberOfElements, exponent));
    }

    /**
     * Test the SharedStateSampler implementation.
     */
    @Test
    void testSharedStateSampler() {
        testSharedStateSampler(7, 1.23);
    }

    /**
     * Special case: Test the SharedStateSampler implementation with a zero exponent.
     */
    @Test
    void testSharedStateSamplerWithZeroExponent() {
        testSharedStateSampler(7, 0);
    }

    /**
     * Test the SharedStateSampler implementation for the specified parameters.
     *
     * @param numberOfElements Number of elements
     * @param exponent Exponent
     */
    private static void testSharedStateSampler(int numberOfElements, double exponent) {
        final UniformRandomProvider rng1 = RandomSource.SPLIT_MIX_64.create(0L);
        final UniformRandomProvider rng2 = RandomSource.SPLIT_MIX_64.create(0L);
        // Use instance constructor not factory constructor to exercise 1.X public API
        final RejectionInversionZipfSampler sampler1 =
            new RejectionInversionZipfSampler(rng1, numberOfElements, exponent);
        final SharedStateDiscreteSampler sampler2 = sampler1.withUniformRandomProvider(rng2);
        RandomAssert.assertProduceSameSequence(sampler1, sampler2);
    }

    /**
     * Test the toString method. This is added to ensure coverage as the factory constructor
     * used in other tests does not create an instance of the wrapper class.
     */
    @Test
    void testToString() {
        final UniformRandomProvider rng = RandomSource.SPLIT_MIX_64.create(0L);
        Assertions.assertTrue(new RejectionInversionZipfSampler(rng, 10, 2.0).toString()
                .toLowerCase().contains("zipf"));
    }
}
