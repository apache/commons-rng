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

import java.time.Duration;
import org.apache.commons.rng.UniformRandomProvider;
import org.apache.commons.rng.sampling.RandomAssert;
import org.apache.commons.rng.sampling.distribution.ZetaSampler.RejectionZetaSampler;
import org.apache.commons.rng.simple.RandomSource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Test for the {@link ZetaSampler}. The tests hit edge cases for the sampler.
 */
class ZetaSamplerTest {
    /**
     * Test the constructor with an {@code exponent <= 1}.
     */
    @ParameterizedTest
    @ValueSource(doubles = {-1, 0, 1, Double.NaN})
    void testConstructorThrowsWithBadExponent(double s) {
        final UniformRandomProvider rng = RandomAssert.seededRNG();
        Assertions.assertThrows(IllegalArgumentException.class,
            () -> ZetaSampler.of(rng, s));
    }

    /**
     * Test the SharedStateSampler implementation with exponents that are above and below
     * the threshold to switch the sampler bias from 1 to infinity.
     */
    @ParameterizedTest
    @ValueSource(doubles = {1.01, 1.1, 1.3, 2, 100})
    void testSharedStateSampler(double s) {
        final UniformRandomProvider rng1 = RandomAssert.seededRNG();
        final UniformRandomProvider rng2 = RandomAssert.seededRNG();
        final SharedStateDiscreteSampler sampler1 = ZetaSampler.of(rng1, s);
        final SharedStateDiscreteSampler sampler2 = sampler1.withUniformRandomProvider(rng2);
        RandomAssert.assertProduceSameSequence(sampler1, sampler2);
    }

    /**
     * Test a large exponent biases towards 1.
     *
     * <p>Note: This uses a package-private constructor for the rejection sampler.
     * The test validates that an exponent above 54 does not sample values other
     * than 1. If the exponent is too large (e.g. above 1025) then the sampler will
     * overflow to infinity when computing 2^(a-1). This will result in inf / inf
     * division producing a NaN and infinite recursion. This limits the exponent used
     * by this test.
     *
     * <p>In practice the factory method knows the rejection method will not work
     * and returns a sampler that always returns 1.
     */
    @ParameterizedTest
    @ValueSource(doubles = {54.1, 100})
    void testLargeExponent(double exponent) {
        // x = floor ( U^{-1/(a-1)} )
        // Bias towards 1 has U in (0, 1] to avoid U=0 where x=infinity
        // Threshold at 1.9999... = pow(2^-53, -(1 / (a - 1))
        // a = 1 - Math.log(0x1p-53) / Math.log(Math.nextDown(2.0)) = 54.00000000000001
        // The sample should always be 1
        final int expected = 1;
        UniformRandomProvider rng;
        DiscreteSampler s;
        // u from first long : Can be [0, 1) or (0, 1]
        // v from second long should control rejection
        rng = createRNG(0, -1, 0, -1, 0, -1);
        s = new RejectionZetaSampler(rng, exponent);
        for (int i = 0; i < 3; i++) {
            Assertions.assertEquals(expected, s.sample());
        }
        rng = createRNG(-1, -1, -1, -1, -1, -1);
        s = new RejectionZetaSampler(rng, exponent);
        for (int i = 0; i < 3; i++) {
            Assertions.assertEquals(expected, s.sample());
        }
        // Any RNG
        s = new RejectionZetaSampler(RandomAssert.createRNG(), exponent);
        for (int i = 0; i < 100; i++) {
            Assertions.assertEquals(expected, s.sample());
        }
    }

    /**
     * Test a tiny exponent cannot avoid small samples even though it should
     * bias towards infinity.
     */
    @Test
    void testTinyExponent() {
        final double exponent = Math.nextUp(1.0);
        // x = floor ( U^{-1/(a-1)} )
        // Bias towards infinity has U in [0, 1) to avoid U=1
        // But this cannot avoid small samples when U is close to 1.
        // Math.pow(1.0, -(1/0x1p-52))  = 1
        // Math.pow(Math.nextDown(1.0), -(1/0x1p-52)) = 1.6487212707001282
        // Math.pow(Math.nextDown(Math.nextDown(1.0)), -(1/0x1p-52)) = 2.7182818284590455
        // Math.pow(Math.nextDown(Math.nextDown(Math.nextDown(1.0))), -(1/0x1p-52)) = 4.481689070338066
        final UniformRandomProvider rng = createRNG(
            0, -1,   // u=0.0; v~1.0
            -1, -1,  // u=1.0 - 2^-53; v~1.0

            // Note: These lead to rejection in the current implementation when v is high.
            // So here we lower v to allow the rejection test to pass.
            -2L << 11, -1L >>> 1,  // u=1.0 - 2 * 2^-53; v~0.5
            -3L << 11, -1L >>> 1   // u=1.0 - 3 * 2^-53; v~0.5
        );
        final DiscreteSampler s = ZetaSampler.of(rng, exponent);
        Assertions.assertEquals(Integer.MAX_VALUE, s.sample());
        // Expected samples from values above: 1.645; 2.718; 4.481.
        Assertions.assertEquals(1, s.sample());
        Assertions.assertEquals(2, s.sample());
        Assertions.assertEquals(4, s.sample());

        // Test the sampler can output samples (i.e. do not reject forever with other u in [0, 1).
        final int[][] sample = new int[1][];
        UniformRandomProvider rng2 = RandomSource.L128_X256_MIX.create();
        Assertions.assertTimeoutPreemptively(Duration.ofMillis(100), () ->
            sample[0] = ZetaSampler.of(rng2, exponent).samples(1000).toArray()
        );
        // The survival function of the zeta distribution with tiny s:
        // sf(x=2147483647; s=1 + 2^-52) = 0.9999999999999951
        // We expect the sample to be at the upper bound.
        // Use of a fresh RNG for repeat invocation if the test fails should ensure
        // this passes. Otherwise consider a fixed seed.
        for (final int x : sample[0]) {
            Assertions.assertEquals(Integer.MAX_VALUE, x, "Consider using a fixed RNG seed");
        }
    }

    /**
     * Test the toString method for cases not hit in the rest of the test suite.
     * This test asserts the toString method always contains the string 'zeta'.
     */
    @ParameterizedTest
    @ValueSource(doubles = {1.23, 100})
    void testToString(double exponent) {
        final UniformRandomProvider rng = RandomAssert.seededRNG();
        final String s = ZetaSampler.of(rng, exponent).toString().toLowerCase();
        Assertions.assertTrue(s.contains("zeta"));
    }

    /**
     * Creates the RNG to return the given values from the nextLong() method.
     *
     * @param values Long values
     * @return the RNG
     */
    private static UniformRandomProvider createRNG(long... values) {
        return new UniformRandomProvider() {
            private int i;

            @Override
            public long nextLong() {
                return values[i++];
            }

            @Override
            public double nextDouble() {
                throw new IllegalStateException("nextDouble cannot be trusted to be in [0, 1) and should be ignored");
            }
        };
    }
}
