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

package org.apache.commons.rng.simple.internal;

import org.apache.commons.math3.stat.inference.ChiSquareTest;
import org.apache.commons.rng.UniformRandomProvider;
import org.apache.commons.rng.core.source64.SplitMix64;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.util.Arrays;

/**
 * Tests for the {@link SeedUtils}.
 */
class SeedUtilsTest {
    /**
     * Test the int hex permutation has 8 unique hex digits per permutation.
     * A uniformity test is performed on to check each hex digits is used evenly at each
     * character position.
     */
    @Test
    void testCreateIntHexPermutation() {
        final UniformRandomProvider rng = new SplitMix64(-567435247L);
        final long[][] samples = new long[8][16];
        for (int i = 0; i < 1000; i++) {
            int sample = SeedUtils.createIntHexPermutation(rng);
            int observed = 0;
            for (int j = 0; j < 8; j++) {
                final int digit = sample & 0xf;
                Assertions.assertEquals(0, observed & (1 << digit), "Duplicate digit in sample");
                observed |= 1 << digit;
                samples[j][digit]++;
                sample >>>= 4;
            }
        }

        final ChiSquareTest chiSquareTest = new ChiSquareTest();
        final double[] expected = new double[16];
        Arrays.fill(expected, 1.0 / 16);
        // Pass if we cannot reject null hypothesis that distributions are the same.
        for (int j = 0; j < 8; j++) {
            Assertions.assertFalse(chiSquareTest.chiSquareTest(expected, samples[j], 0.001),
                "Not uniform in digit " + j);
        }
    }

    /**
     * Test the long hex permutation has 8 unique hex digits per permutation in the upper and
     * lower 32-bits.
     * A uniformity test is performed on to check each hex digits is used evenly at each
     * character position.
     */
    @Test
    void testCreateLongHexPermutation() {
        final UniformRandomProvider rng = new SplitMix64(34645768L);
        final long[][] samples = new long[16][16];
        for (int i = 0; i < 1000; i++) {
            long sample = SeedUtils.createLongHexPermutation(rng);
            // Check lower 32-bits
            long observed = 0;
            for (int j = 0; j < 8; j++) {
                final int digit = (int) (sample & 0xfL);
                Assertions.assertEquals(0, observed & (1 << digit), "Duplicate digit in lower sample");
                observed |= 1 << digit;
                samples[j][digit]++;
                sample >>>= 4;
            }
            // Check upper 32-bits
            observed = 0;
            for (int j = 8; j < 16; j++) {
                final int digit = (int) (sample & 0xfL);
                Assertions.assertEquals(0, observed & (1 << digit), "Duplicate digit in upper sample");
                observed |= 1 << digit;
                samples[j][digit]++;
                sample >>>= 4;
            }
        }

        final ChiSquareTest chiSquareTest = new ChiSquareTest();
        final double[] expected = new double[16];
        Arrays.fill(expected, 1.0 / 16);
        // Pass if we cannot reject null hypothesis that distributions are the same.
        for (int j = 0; j < 16; j++) {
            Assertions.assertFalse(chiSquareTest.chiSquareTest(expected, samples[j], 0.001),
                "Not uniform in digit " + j);
        }
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, Integer.MAX_VALUE})
    void testIntSizeFromByteSize(int size) {
        Assertions.assertEquals((int) Math.ceil((double) size / Integer.BYTES), SeedUtils.intSizeFromByteSize(size));
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, Integer.MAX_VALUE})
    void testLongSizeFromByteSize(int size) {
        Assertions.assertEquals((int) Math.ceil((double) size / Long.BYTES), SeedUtils.longSizeFromByteSize(size));
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, Integer.MAX_VALUE})
    void testIntSizeFromLongSize(int size) {
        Assertions.assertEquals((int) Math.min(size * 2L, Integer.MAX_VALUE), SeedUtils.intSizeFromLongSize(size));
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2, 3, 4, 5, Integer.MAX_VALUE})
    void testLongSizeFromIntSize(int size) {
        Assertions.assertEquals((int) Math.ceil((double) size / 2), SeedUtils.longSizeFromIntSize(size));
    }
}
