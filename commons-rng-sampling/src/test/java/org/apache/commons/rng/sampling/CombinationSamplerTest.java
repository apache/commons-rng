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
package org.apache.commons.rng.sampling;

import java.util.Arrays;

import org.junit.Assert;
import org.junit.Test;

import org.apache.commons.math3.stat.inference.ChiSquareTest;
import org.apache.commons.math3.util.CombinatoricsUtils;
import org.apache.commons.rng.UniformRandomProvider;
import org.apache.commons.rng.simple.RandomSource;

/**
 * Tests for {@link CombinationSampler}.
 */
public class CombinationSamplerTest {
    private final UniformRandomProvider rng = RandomSource.create(RandomSource.XOR_SHIFT_1024_S);

    @Test
    public void testSampleIsInDomain() {
        final int n = 6;
        for (int k = 1; k <= n; k++) {
            final CombinationSampler sampler = new CombinationSampler(rng, n, k);
            final int[] random = sampler.sample();
            for (int s : random) {
                assertIsInDomain(n, s);
            }
        }
    }

    @Test
    public void testUniformWithKlessThanHalfN() {
        final int n = 8;
        final int k = 2;
        assertUniformSamples(n, k);
    }

    @Test
    public void testUniformWithKmoreThanHalfN() {
        final int n = 8;
        final int k = 6;
        assertUniformSamples(n, k);
    }

    @Test
    public void testSampleWhenNequalsKIsNotShuffled() {
        // Check n == k boundary case.
        // This is allowed but the sample is not shuffled.
        for (int n = 1; n < 3; n++) {
            final int k = n;
            final CombinationSampler sampler = new CombinationSampler(rng, n, k);
            final int[] sample = sampler.sample();
            Assert.assertEquals("Incorrect sample length", n, sample.length);
            for (int i = 0; i < n; i++) {
                Assert.assertEquals("Sample was shuffled", i, sample[i]);
            }
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testKgreaterThanNThrows() {
        // Must fail for k > n.
        final int n = 2;
        final int k = 3;
        new CombinationSampler(rng, n, k);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNequalsZeroThrows() {
        // Must fail for n = 0.
        final int n = 0;
        final int k = 3;
        new CombinationSampler(rng, n, k);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testKequalsZeroThrows() {
        // Must fail for k = 0.
        final int n = 2;
        final int k = 0;
        new CombinationSampler(rng, n, k);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNisNegativeThrows() {
        // Must fail for n <= 0.
        final int n = -1;
        final int k = 3;
        new CombinationSampler(rng, n, k);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testKisNegativeThrows() {
        // Must fail for k <= 0.
        final int n = 0;
        final int k = -1;
        new CombinationSampler(rng, n, k);
    }

    /**
     * Test the SharedStateSampler implementation.
     */
    @Test
    public void testSharedStateSampler() {
        final UniformRandomProvider rng1 = RandomSource.create(RandomSource.SPLIT_MIX_64, 0L);
        final UniformRandomProvider rng2 = RandomSource.create(RandomSource.SPLIT_MIX_64, 0L);
        final int n = 17;
        final int k = 3;
        final CombinationSampler sampler1 =
            new CombinationSampler(rng1, n, k);
        final CombinationSampler sampler2 = sampler1.withUniformRandomProvider(rng2);
        RandomAssert.assertProduceSameSequence(
            new RandomAssert.Sampler<int[]>() {
                @Override
                public int[] sample() {
                    return sampler1.sample();
                }
            },
            new RandomAssert.Sampler<int[]>() {
                @Override
                public int[] sample() {
                    return sampler2.sample();
                }
            });
    }

    //// Support methods.

    /**
     * Asserts the sample value is in the range 0 to n-1.
     *
     * @param n     the n
     * @param value the sample value
     */
    private static void assertIsInDomain(int n, int value) {
        if (value < 0 || value >= n) {
            Assert.fail("sample " + value + " not in the domain " + n);
        }
    }

    private void assertUniformSamples(int n, int k) {
        // The C(n, k) should generate a sample of unspecified order.
        // To test this each combination is allocated a unique code
        // based on setting k of the first n-bits in an integer.
        // Codes are positive for all combinations of bits that use k-bits,
        // otherwise they are negative.
        final int totalBitCombinations = 1 << n;
        int[] codeLookup = new int[totalBitCombinations];
        Arrays.fill(codeLookup, -1); // initialise as negative
        int codes = 0;
        for (int i = 0; i < totalBitCombinations; i++) {
            if (Integer.bitCount(i) == k) {
                // This is a valid sample so allocate a code
                codeLookup[i] = codes++;
            }
        }

        // The number of combinations C(n, k) is the binomial coefficient
        Assert.assertEquals("Incorrect number of combination codes",
                CombinatoricsUtils.binomialCoefficient(n, k), codes);

        final long[] observed = new long[codes];
        final int numSamples = 6000;

        final CombinationSampler sampler = new CombinationSampler(rng, n, k);
        for (int i = 0; i < numSamples; i++) {
            observed[findCode(codeLookup, sampler.sample())]++;
        }

        // Chi squared test of uniformity
        final double numExpected = numSamples / (double) codes;
        final double[] expected = new double[codes];
        Arrays.fill(expected, numExpected);
        final ChiSquareTest chiSquareTest = new ChiSquareTest();
        // Pass if we cannot reject null hypothesis that distributions are the same.
        Assert.assertFalse(chiSquareTest.chiSquareTest(expected, observed, 0.001));
    }

    private static int findCode(int[] codeLookup, int[] sample) {
        // Each sample index is used to set a bit in an integer.
        // The resulting bits should be a valid code.
        int bits = 0;
        for (int s : sample) {
            // This shift will be from 0 to n-1 since it is from the
            // domain of size n.
            bits |= 1 << s;
        }
        if (bits >= codeLookup.length) {
            Assert.fail("Bad bit combination: " + Arrays.toString(sample));
        }
        final int code = codeLookup[bits];
        if (code < 0) {
            Assert.fail("Bad bit code: " + Arrays.toString(sample));
        }
        return code;
    }
}
