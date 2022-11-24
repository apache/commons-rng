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
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Test for the {@link InverseTransformParetoSampler}. The tests hit edge cases for the sampler.
 */
class InverseTransformParetoSamplerTest {
    /**
     * The multiplier to convert the least significant 53-bits of a {@code long} to a {@code double}.
     * This is the smallest non-zero value output from nextDouble.
     */
    private static final double U = 0x1.0p-53d;

    /**
     * Test the constructor with a bad scale.
     */
    @Test
    void testConstructorThrowsWithZeroScale() {
        final UniformRandomProvider rng = RandomAssert.seededRNG();
        final double scale = 0;
        final double shape = 1;
        Assertions.assertThrows(IllegalArgumentException.class,
            () -> InverseTransformParetoSampler.of(rng, scale, shape));
    }

    /**
     * Test the constructor with a bad shape.
     */
    @Test
    void testConstructorThrowsWithZeroShape() {
        final UniformRandomProvider rng = RandomAssert.seededRNG();
        final double scale = 1;
        final double shape = 0;
        Assertions.assertThrows(IllegalArgumentException.class,
            () -> InverseTransformParetoSampler.of(rng, scale, shape));
    }

    /**
     * Test the SharedStateSampler implementation with a shape above and below 1.
     */
    @ParameterizedTest
    @CsvSource({
        "1.23, 4.56",
        "1.23, 0.56",
    })
    void testSharedStateSampler(double scale, double shape) {
        final UniformRandomProvider rng1 = RandomAssert.seededRNG();
        final UniformRandomProvider rng2 = RandomAssert.seededRNG();
        final SharedStateContinuousSampler sampler1 =
            InverseTransformParetoSampler.of(rng1, scale, shape);
        final SharedStateContinuousSampler sampler2 = sampler1.withUniformRandomProvider(rng2);
        RandomAssert.assertProduceSameSequence(sampler1, sampler2);
    }

    /**
     * Test a large shape is sampled using inversion of p in [0, 1).
     */
    @ParameterizedTest
    @CsvSource({
        "12.34, Infinity",
        "1.23, Infinity",
        "0.1, Infinity",
        // Also valid when shape is finite.
        // Limit taken from o.a.c.statistics.ParetoDistributionTest
        "12.34, 6.6179136542552806e17",
        "1.23, 6.6179136542552806e17",
        "0.1, 6.6179136542552806e17",
    })
    void testLargeShape(double scale, double shape) {
        // Assert the inversion using the power function.
        // Note here the first argument to pow is (1 - p)
        final double oneOverShape = 1 / shape;
        // Note that inverse CDF(p=1) should be infinity (upper bound).
        // However this requires a check on the value of p and the
        // inversion returns the lower bound when 1 / shape = 0 due
        // to pow(0, 0) == 1
        final double p1expected = oneOverShape == 0 ? scale : Double.POSITIVE_INFINITY;
        Assertions.assertEquals(p1expected, scale / Math.pow(0, oneOverShape));
        Assertions.assertEquals(scale, scale / Math.pow(U, oneOverShape));
        Assertions.assertEquals(scale, scale / Math.pow(1 - U, oneOverShape));
        Assertions.assertEquals(scale, scale / Math.pow(1, oneOverShape));

        // Sampling should be as if p in [0, 1) so avoiding an infinite sample for
        // large finite shape when p=1
        assertSampler(scale, shape, scale);
    }

    /**
     * Test a tiny shape is sampled using inversion of p in (0, 1].
     */
    @ParameterizedTest
    @CsvSource({
        "12.34, 4.9e-324",
        "1.23, 4.9e-324",
        "0.1, 4.9e-324",
        // Also valid when 1 / shape is finite.
        // Limit taken from o.a.c.statistics.ParetoDistributionTest
        "12.34, 7.456765604783329e-20",
        "1.23, 7.456765604783329e-20",
        "0.1, 7.456765604783329e-20",
    })
    void testTinyShape(double scale, double shape) {
        // Assert the inversion using the power function.
        // Note here the first argument to pow is (1 - p)
        final double oneOverShape = 1 / shape;
        Assertions.assertEquals(Double.POSITIVE_INFINITY, scale / Math.pow(0, oneOverShape));
        Assertions.assertEquals(Double.POSITIVE_INFINITY, scale / Math.pow(U, oneOverShape));
        Assertions.assertEquals(Double.POSITIVE_INFINITY, scale / Math.pow(1 - U, oneOverShape));
        // Note that inverse CDF(p=0) should be scale (lower bound).
        // However this requires a check on the value of p and the
        // inversion returns NaN due to pow(1, inf) == NaN.
        final double p0expected = oneOverShape == Double.POSITIVE_INFINITY ? Double.NaN : scale;
        Assertions.assertEquals(p0expected, scale / Math.pow(1, oneOverShape));

        // Sampling should be as if p in (0, 1] so avoiding a NaN sample for
        // infinite 1 / shape when p=0
        assertSampler(scale, shape, Double.POSITIVE_INFINITY);
    }

    /**
     * Assert the sampler produces the expected sample value irrespective of the values from the RNG.
     *
     * @param scale Distribution scale
     * @param shape Distribution shape
     * @param expected Expected sample value
     */
    private static void assertSampler(double scale, double shape, double expected) {
        // Extreme random numbers using no bits or all bits, then combinations
        // that may be used to generate a double from the lower or upper 53-bits
        final long[] values = {0, -1, 1, 1L << 11, -2, -2L << 11};
        final UniformRandomProvider rng = createRNG(values);
        ContinuousSampler s = InverseTransformParetoSampler.of(rng, scale, shape);
        for (final long l : values) {
            Assertions.assertEquals(expected, s.sample(), () -> "long bits = " + l);
        }
        // Any random number
        s = InverseTransformParetoSampler.of(RandomAssert.createRNG(), scale, shape);
        for (int i = 0; i < 100; i++) {
            Assertions.assertEquals(expected, s.sample());
        }
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
