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

/**
 * Test for the {@link AhrensDieterMarsagliaTsangGammaSampler}. The tests hit edge cases for the sampler.
 */
class AhrensDieterMarsagliaTsangGammaSamplerTest {
    /**
     * Test the constructor with a bad alpha.
     */
    @Test
    void testConstructorThrowsWithZeroAlpha() {
        final UniformRandomProvider rng = RandomAssert.seededRNG();
        final double alpha = 0;
        final double theta = 1;
        Assertions.assertThrows(IllegalArgumentException.class,
            () -> AhrensDieterMarsagliaTsangGammaSampler.of(rng, alpha, theta));
    }

    /**
     * Test the constructor with a bad theta.
     */
    @Test
    void testConstructorThrowsWithZeroTheta() {
        final UniformRandomProvider rng = RandomAssert.seededRNG();
        final double alpha = 1;
        final double theta = 0;
        Assertions.assertThrows(IllegalArgumentException.class,
            () -> AhrensDieterMarsagliaTsangGammaSampler.of(rng, alpha, theta));
    }

    /**
     * Test the SharedStateSampler implementation with {@code alpha < 1}.
     */
    @Test
    void testSharedStateSamplerWithAlphaBelowOne() {
        testSharedStateSampler(0.5, 3.456, false);
        testSharedStateSampler(0.5, 3.456, true);
    }

    /**
     * Test the SharedStateSampler implementation with {@code alpha > 1}.
     */
    @Test
    void testSharedStateSamplerWithAlphaAboveOne() {
        testSharedStateSampler(3.5, 3.456, false);
        testSharedStateSampler(3.5, 3.456, true);
    }

    /**
     * Test the SharedStateSampler implementation.
     *
     * @param alpha Alpha.
     * @param theta Theta.
     * @param logSamples Use log samples.
     */
    private static void testSharedStateSampler(double alpha, double theta, boolean logSamples) {
        final UniformRandomProvider rng1 = RandomAssert.seededRNG();
        final UniformRandomProvider rng2 = RandomAssert.seededRNG();
        final SharedStateContinuousSampler sampler1 = logSamples ?
            AhrensDieterMarsagliaTsangGammaSampler.of(rng1, alpha, theta, true) :
            // Use instance constructor not factory constructor to exercise 1.X public API
            new AhrensDieterMarsagliaTsangGammaSampler(rng1, alpha, theta);
        final SharedStateContinuousSampler sampler2 = sampler1.withUniformRandomProvider(rng2);
        RandomAssert.assertProduceSameSequence(sampler1, sampler2);
    }

    /**
     * Test the log samples implementation with {@code alpha < 1}.
     */
    @Test
    void testLogSamplesWithAlphaBelowOne() {
        // Note: ulp passes with N=1,000,000. N has been lowered for time savings.
        final int n = 1000;
        testLogSamples(0.5, 3.456, n, 25);
        // alpha = 2^-52 is where (1 + alpha/E) == 1 (See RNG-195)
        testLogSamples(0x1p-50, 1.0, n, 50);
        testLogSamples(0x1p-52, 1.0, n, 50);
        testLogSamples(0x1p-54, 1.0, n, 50);
    }

    /**
     * Test the log samples implementation with {@code alpha > 1}.
     */
    @Test
    void testLogSamplesWithAlphaAboveOne() {
        // Note: ulp passes with N=1,000,000. N has been lowered for time savings.
        final int n = 1000;
        testLogSamples(3.5, 3.456, n, 25);
        // As alpha -> large then samples -> alpha
        // Occurs consistently > 3e32 (See RNG-195)
        testLogSamples(1e30, 1.0, n, 50);
        testLogSamples(1e32, 1.0, n, 50);
        testLogSamples(1e34, 1.0, n, 50);
    }

    /**
     * Test the log samples implementation matches the real samples output.
     *
     * @param alpha Alpha.
     * @param theta Theta.
     * @param ulps Units-in-the-last place for matching output.
     */
    private static void testLogSamples(double alpha, double theta, int n, int ulps) {
        // May spuriously fail. Allow reruns to use a different RNG.
        final UniformRandomProvider[] rngs = RandomAssert.createRNG(2);
        final UniformRandomProvider rng1 = rngs[0];
        final UniformRandomProvider rng2 = rngs[1];
        final SharedStateContinuousSampler sampler1 =
            AhrensDieterMarsagliaTsangGammaSampler.of(rng1, alpha, theta, false);
        final SharedStateContinuousSampler sampler2 =
            AhrensDieterMarsagliaTsangGammaSampler.of(rng2, alpha, theta, true);
        for (int i = 0; i < n; i++) {
            // The sequence may not be exactly the same.
            final double x1 = sampler1.sample();
            final double x2 = sampler2.sample();
            // Use exp on the log sample.
            final double s2 = Math.exp(x2);
            Assertions.assertEquals(x1, s2, ulps * Math.ulp(x1),
                () -> String.format("exp %s %s ulp=%d", alpha, theta, (long) ((s2 - x1) / Math.ulp(x1))));
            // Using log on the real sample has issues mapping
            // the domains and relative error can be large when x1 -> 1 (log(x1) -> 0)
            if (x1 < 0.95 && x1 > 1.05) {
                final double s1 = Math.log(x1);
                Assertions.assertEquals(x2, s1, ulps * Math.ulp(x2),
                    () -> String.format("log %s %s ulp=%d", alpha, theta, (long) ((s1 - x2) / Math.ulp(x2))));
            }
        }
    }

    /**
     * Test the toString method. This is added to ensure coverage as the factory constructor
     * used in other tests does not create an instance of the wrapper class.
     */
    @Test
    void testToString() {
        final UniformRandomProvider rng = RandomAssert.seededRNG();
        Assertions.assertTrue(new AhrensDieterMarsagliaTsangGammaSampler(rng, 1.0, 2.0).toString()
                .toLowerCase().contains("gamma"));
    }
}
