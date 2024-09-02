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
package org.apache.commons.rng.examples.jmh.sampling;

import java.util.function.BiConsumer;
import org.apache.commons.math3.stat.inference.ChiSquareTest;
import org.apache.commons.rng.UniformRandomProvider;
import org.apache.commons.rng.sampling.PermutationSampler;
import org.apache.commons.rng.simple.RandomSource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Test for array shuffle samplers in the {@link ArrayShuffleBenchmark} class.
 */
class ArrayShuffleBenchmarkTest {

    /**
     * The seed for the RNG used in the sampling tests.
     *
     * <p>This has been chosen to allow the test to pass with all generators.
     * Set to null test with a random seed. When using a random
     * seed re-run the test multiple times. Systematic failure of the same test
     * should be investigated further.
     */
    private static final Long SEED = 0xd1342543de82ef95L;

    @ParameterizedTest
    @CsvSource({
        "42, 257",
        "1356, 8073",
    })
    void testBoundedRandom2(int range1, int range2) {
        Assertions.assertTrue((long) range1 * range2 < 1L << 31, "Product must be less than 2^31");

        final int samples = 1000000;
        final int bins = 8;
        final long[][] observed = new long[bins][bins];
        final UniformRandomProvider rng = RandomSource.XO_SHI_RO_128_PP.create(SEED);
        final int[] productBound = {range1 * range2};
        final int width1 = (int) Math.ceil((double) range1 / bins);
        final int width2 = (int) Math.ceil((double) range2 / bins);
        for (int i = 0; i < samples; i++) {
            final int[] indices = ArrayShuffleBenchmark.randomBounded2(range1, range2, productBound, rng);
            final int index1 = indices[0] / width1;
            final int index2 = indices[1] / width2;
            observed[index1][index2]++;
        }

        final double p = new ChiSquareTest().chiSquareTest(observed);
        Assertions.assertFalse(p < 1e-3, () -> "p-value too small: " + p);
    }

    @ParameterizedTest
    @CsvSource({
        "13, 12257",
        "4242, 9899",
    })
    void testBoundedRandom2a(int range1, int range2) {
        Assertions.assertTrue((long) range1 * range2 < 1L << 31, "Product must be less than 2^31");

        final int samples = 1000;
        final UniformRandomProvider rng1 = RandomSource.XO_SHI_RO_128_PP.create(SEED);
        final UniformRandomProvider rng2 = RandomSource.XO_SHI_RO_128_PP.create(SEED);
        final int[] productBound = {range1 * range2};
        int bound = productBound[0];
        final int[] indices2 = new int[2];
        for (int i = 0; i < samples; i++) {
            final int[] indices1 = ArrayShuffleBenchmark.randomBounded2(range1, range2, productBound, rng1);
            bound = ArrayShuffleBenchmark.randomBounded2a(range1, range2, bound, rng2, indices2);
            Assertions.assertEquals(indices1[0], indices2[0], "index0");
            Assertions.assertEquals(indices1[1], indices2[1], "index1");
            Assertions.assertEquals(productBound[0], bound, "bound");
        }
    }

    @ParameterizedTest
    @CsvSource({
        "257",
        "8073",
    })
    void testShuffle1(int length) {
        assertShuffle(length, ArrayShuffleBenchmark::shuffle1);
    }

    @ParameterizedTest
    @CsvSource({
        "257",
        "8073",
        // Above the bounded random threshold of 2^15
        "57548",
    })
    void testShuffle2(int length) {
        assertShuffle(length, ArrayShuffleBenchmark::shuffle2);
    }

    @ParameterizedTest
    @CsvSource({
        "257",
        "8073",
        // Above the bounded random threshold of 2^15
        "57548",
    })
    void testShuffle3(int length) {
        assertShuffle(length, ArrayShuffleBenchmark::shuffle3);
    }

    @ParameterizedTest
    @CsvSource({
        "89",
        "257",
        "8073",
        // Above the bounded random threshold of 2^15
        "57548",
    })
    void testShuffle4(int length) {
        assertShuffle(length, ArrayShuffleBenchmark::shuffle4);
    }

    private static void assertShuffle(int length, BiConsumer<UniformRandomProvider, int[]> fun) {
        final int[] array = PermutationSampler.natural(length);
        final UniformRandomProvider rng = RandomSource.XO_SHI_RO_128_PP.create(SEED);
        final int samples = 1000000 / length;
        final int bins = 8;
        final long[][] observed = new long[bins][bins];
        final int width = (int) Math.ceil((double) length / bins);
        for (int j = 0; j < samples; j++) {
            fun.accept(rng, array);
            for (int i = 0; i < length; i++) {
                observed[i / width][array[i] / width]++;
            }
        }
        final double p = new ChiSquareTest().chiSquareTest(observed);
        Assertions.assertFalse(p < 1e-3, () -> "p-value too small: " + p);
    }

    @ParameterizedTest
    @CsvSource({
        "257",
        "8073",
    })
    void testShuffle1a(int length) {
        final int[] a = PermutationSampler.natural(length);
        final int[] b = a.clone();
        final RandomSource source = RandomSource.XO_RO_SHI_RO_128_PP;
        final byte[] seed = source.createSeed();
        final UniformRandomProvider rng1 = source.create(seed);
        final UniformRandomProvider rng2 = source.create(seed);
        final int samples = 10;
        for (int j = 0; j < samples; j++) {
            ArrayShuffleBenchmark.shuffle1(rng1, a);
            ArrayShuffleBenchmark.shuffle1(rng2, b);
            Assertions.assertArrayEquals(a, b);
        }
    }

    @ParameterizedTest
    @CsvSource({
        "257",
        "8073",
    })
    void testShuffle2a(int length) {
        final int[] a = PermutationSampler.natural(length);
        final int[] b = a.clone();
        final int[] c = a.clone();
        final RandomSource source = RandomSource.XO_RO_SHI_RO_128_PP;
        final byte[] seed = source.createSeed();
        final UniformRandomProvider rng1 = source.create(seed);
        final UniformRandomProvider rng2 = source.create(seed);
        final UniformRandomProvider rng3 = source.create(seed);
        final int samples = 10;
        for (int j = 0; j < samples; j++) {
            ArrayShuffleBenchmark.shuffle2(rng1, a);
            ArrayShuffleBenchmark.shuffle2a(rng2, b);
            ArrayShuffleBenchmark.shuffle2(rng3, c, 0, c.length);
            Assertions.assertArrayEquals(a, b, "shuffle2a");
            Assertions.assertArrayEquals(a, c, "shuffle2 range");
        }
    }
}
