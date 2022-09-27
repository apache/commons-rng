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
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Test for the {@link TSampler}.
 */
class TSamplerTest {
    /**
     * Test the constructor with an invalid degrees of freedom.
     */
    @ParameterizedTest
    @ValueSource(doubles = {0, -1, Double.NaN})
    void testConstructorThrowsWithBadDegreesOfFreedom(double degreesOfFreedom) {
        final UniformRandomProvider rng = RandomAssert.seededRNG();
        Assertions.assertThrows(IllegalArgumentException.class,
            () -> TSampler.of(rng, degreesOfFreedom));
    }

    /**
     * Test the SharedStateSampler implementation.
     */
    @ParameterizedTest
    @ValueSource(doubles = {4.56, 1e16})
    void testSharedStateSampler(double degreesOfFreedom) {
        final UniformRandomProvider rng1 = RandomAssert.seededRNG();
        final UniformRandomProvider rng2 = RandomAssert.seededRNG();
        final TSampler sampler1 = TSampler.of(rng1, degreesOfFreedom);
        final TSampler sampler2 = sampler1.withUniformRandomProvider(rng2);
        RandomAssert.assertProduceSameSequence(sampler1, sampler2);
    }

    /**
     * Test extremely large degrees of freedom is approximated using a normal distribution.
     */
    @Test
    void testExtremelyLargeDegreesOfFreedom() {
        final UniformRandomProvider rng1 = RandomAssert.seededRNG();
        final UniformRandomProvider rng2 = RandomAssert.seededRNG();
        final double degreesOfFreedom = 1e16;
        final ContinuousSampler sampler1 = TSampler.of(rng1, degreesOfFreedom);
        final ContinuousSampler sampler2 = ZigguratSampler.NormalizedGaussian.of(rng2);
        RandomAssert.assertProduceSameSequence(sampler1, sampler2);
    }
}
