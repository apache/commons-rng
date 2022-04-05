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
package org.apache.commons.rng.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Arrays;
import java.util.SplittableRandom;

import org.apache.commons.rng.core.source64.SplitMix64;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;

/**
 * Tests for {@link BaseProvider}.
 *
 * This class should only contain unit tests that cover code paths not
 * exercised elsewhere.  Those code paths are most probably only taken
 * in case of a wrong implementation (and would therefore fail other
 * tests too).
 */
class BaseProviderTest {
    @Test
    void testStateSizeTooSmall() {
        final DummyGenerator dummy = new DummyGenerator();
        final int size = dummy.getStateSize();
        Assumptions.assumeTrue(size > 0);
        final RandomProviderDefaultState state = new RandomProviderDefaultState(new byte[size - 1]);
        Assertions.assertThrows(IllegalStateException.class, () -> dummy.restoreState(state));
    }

    @Test
    void testStateSizeTooLarge() {
        final DummyGenerator dummy = new DummyGenerator();
        final int size = dummy.getStateSize();
        final RandomProviderDefaultState state = new RandomProviderDefaultState(new byte[size + 1]);
        Assertions.assertThrows(IllegalStateException.class, () -> dummy.restoreState(state));
    }

    @Test
    void testFillStateInt() {
        final int[] state = new int[10];
        final int[] seed = {1, 2, 3};

        for (int i = 0; i < state.length; i++) {
            Assertions.assertEquals(0, state[i]);
        }

        new DummyGenerator().fillState(state, seed);
        for (int i = 0; i < seed.length; i++) {
            Assertions.assertEquals(seed[i], state[i]);
        }
        for (int i = seed.length; i < state.length; i++) {
            Assertions.assertNotEquals(0, state[i]);
        }
    }

    @Test
    void testFillStateLong() {
        final long[] state = new long[10];
        final long[] seed = {1, 2, 3};

        for (int i = 0; i < state.length; i++) {
            Assertions.assertEquals(0, state[i]);
        }

        new DummyGenerator().fillState(state, seed);
        for (int i = 0; i < seed.length; i++) {
            Assertions.assertEquals(seed[i], state[i]);
        }
        for (int i = seed.length; i < state.length; i++) {
            Assertions.assertNotEquals(0, state[i]);
        }
    }

    /**
     * Test the checkIndex method. This is not used by any RNG implementations
     * as it has been superseded by the equivalent of JDK 9 Objects.checkFromIndexSize.
     */
    @Test
    void testCheckIndex() {
        final BaseProvider rng = new BaseProvider() {
            @Override
            public void nextBytes(byte[] bytes) { /* noop */ }
            @Override
            public void nextBytes(byte[] bytes, int start, int len) { /* noop */ }
            @Override
            public int nextInt() {
                return 0;
            }
            @Override
            public long nextLong() {
                return 0;
            }
            @Override
            public boolean nextBoolean() {
                return false;
            }
            @Override
            public float nextFloat() {
                return 0;
            }
            @Override
            public double nextDouble() {
                return 0;
            }
        };
        // Arguments are (min, max, index)
        // index must be in [min, max]
        rng.checkIndex(-10, 5, 0);
        rng.checkIndex(-10, 5, -10);
        rng.checkIndex(-10, 5, 5);
        rng.checkIndex(Integer.MIN_VALUE, Integer.MAX_VALUE, Integer.MIN_VALUE);
        rng.checkIndex(Integer.MIN_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE);
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> rng.checkIndex(-10, 5, -11));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> rng.checkIndex(-10, 5, 6));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> rng.checkIndex(-10, 5, Integer.MIN_VALUE));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> rng.checkIndex(-10, 5, Integer.MAX_VALUE));
    }

    /**
     * Test a seed can be extended to a required size by filling with a SplitMix64 generator.
     */
    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2, 4, 5, 6, 7, 8, 9})
    void testExpandSeedLong(int length) {
        // The seed does not matter.
        // Create random seeds that are smaller or larger than length.
        final SplittableRandom rng = new SplittableRandom();
        for (long[] seed : new long[][] {
            {},
            rng.longs(1).toArray(),
            rng.longs(2).toArray(),
            rng.longs(3).toArray(),
            rng.longs(4).toArray(),
            rng.longs(5).toArray(),
            rng.longs(6).toArray(),
            rng.longs(7).toArray(),
            rng.longs(8).toArray(),
            rng.longs(9).toArray(),
        }) {
            Assertions.assertArrayEquals(expandSeed(length, seed),
                                         BaseProvider.extendSeed(seed, length));
        }
    }

    /**
     * Expand the seed to the minimum specified length using a {@link SplitMix64} generator
     * seeded with {@code seed[0]}, or zero if the seed length is zero.
     *
     * @param length the length
     * @param seed the seed
     * @return the seed
     */
    private static long[] expandSeed(int length, long... seed) {
        if (seed.length < length) {
            final long[] s = Arrays.copyOf(seed, length);
            final SplitMix64 rng = new SplitMix64(s[0]);
            for (int i = seed.length; i < length; i++) {
                s[i] = rng.nextLong();
            }
            return s;
        }
        return seed;
    }

    /**
     * Test a seed can be extended to a required size by filling with a SplitMix64-style
     * generator using MurmurHash3's 32-bit mix function.
     *
     * <p>There is no reference RNG for this output. The test uses fixed output computed
     * from the reference c++ function in smhasher.
     */
    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2, 4, 5, 6, 7, 8, 9})
    void testExpandSeedInt(int length) {
        // Reference output from the c++ function fmix32(uint32_t) in smhasher.
        // https://github.com/aappleby/smhasher/blob/master/src/MurmurHash3.cpp
        final int seedA = 0x012de1ba;
        final int[] valuesA = {
            0x2f66c8b6, 0x256c0269, 0x054ef409, 0x402425ba, 0x78ebf590, 0x76bea1db,
            0x8bf5dcbe, 0x104ecdd4, 0x43cfc87e, 0xa33c7643, 0x4d210f56, 0xfa12093d,
        };
        // Values from a seed of zero
        final int[] values0 = {
            0x92ca2f0e, 0x3cd6e3f3, 0x1b147dcc, 0x4c081dbf, 0x487981ab, 0xdb408c9d,
            0x78bc1b8f, 0xd83072e5, 0x65cbdd54, 0x1f4b8cef, 0x91783bb0, 0x0231739b,
        };

        // Create a random seed that is larger than the maximum length;
        // start with the initial value
        final int[] data = new SplittableRandom().ints(10).toArray();
        data[0] = seedA;

        for (int i = 0; i <= 9; i++) {
            final int seedLength = i;
            // Truncate the random seed
            final int[] seed = Arrays.copyOf(data, seedLength);
            // Create the expected output length.
            // If too short it should be extended with values from the reference output
            final int[] expected = Arrays.copyOf(seed, Math.max(seedLength, length));
            if (expected.length == 0) {
                // Edge case for zero length
                Assertions.assertArrayEquals(new int[0],
                                             BaseProvider.extendSeed(seed, length));
                continue;
            }
            // Extend the truncated seed using the reference output.
            // This may be seeded with zero or the non-zero initial value.
            final int[] source = expected[0] == 0 ? values0 : valuesA;
            System.arraycopy(source, 0, expected, seedLength, expected.length - seedLength);
            Assertions.assertArrayEquals(expected,
                                         BaseProvider.extendSeed(seed, length),
                                         () -> String.format("%d -> %d", seedLength, length));
        }
    }

    /**
     * Dummy class for checking the behaviorof the IntProvider. Tests:
     * <ul>
     *  <li>an incomplete implementation</li>
     *  <li>{@code fillState} methods with "protected" access</li>
     * </ul>
     */
    static class DummyGenerator extends org.apache.commons.rng.core.source32.IntProvider {
        /** {@inheritDoc} */
        @Override
        public int next() {
            return 4; // https://www.xkcd.com/221/
        }

        /**
         * Gets the state size. This captures the state size of the IntProvider.
         *
         * @return the state size
         */
        int getStateSize() {
            return getStateInternal().length;
        }

        // Missing overrides of "setStateInternal" and "getStateInternal".

        /** {@inheritDoc} */
        @Override
        public void fillState(int[] state, int[] seed) {
            super.fillState(state, seed);
        }

        /** {@inheritDoc} */
        @Override
        public void fillState(long[] state, long[] seed) {
            super.fillState(state, seed);
        }
    }
}
