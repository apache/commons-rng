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
package org.apache.commons.rng.simple;

import java.util.Random;

import org.apache.commons.rng.UniformRandomProvider;
import org.apache.commons.rng.core.source64.LongProvider;
import org.junit.Assert;
import org.junit.Test;

/**
 * Tests for the {@link JDKRandomWrapper} class.
 */
public class JDKRandomWrapperTest {
    /**
     * Test all the methods shared by Random and UniformRandomProvider are equivalent.
     */
    @Test
    public void testJDKRandomEquivalence() {
        // Initialize.
        final long seed = RandomSource.createLong();
        final Random rng1 = new Random(seed);
        final UniformRandomProvider rng2 = new JDKRandomWrapper(new Random(seed));
        checkSameSequence(rng1, rng2);
    }

    /**
     * Ensure that both generators produce the same sequences.
     *
     * @param rng1 RNG.
     * @param rng2 RNG.
     */
    private static void checkSameSequence(Random rng1,
                                          UniformRandomProvider rng2) {
        for (int i = 0; i < 4; i++) {
            Assert.assertEquals(rng1.nextInt(),
                                rng2.nextInt());
        }
        for (int i = 0; i < 7; i++) {
            Assert.assertEquals(rng1.nextLong(),
                                rng2.nextLong());
        }
        for (int i = 0; i < 9; i++) {
            Assert.assertEquals(rng1.nextFloat(),
                                rng2.nextFloat(),
                                0f);
        }
        for (int i = 0; i < 12; i++) {
            Assert.assertEquals(rng1.nextDouble(),
                                rng2.nextDouble(),
                                0d);
        }
        for (int i = 0; i < 18; i++) {
            Assert.assertEquals(rng1.nextBoolean(),
                                rng2.nextBoolean());
        }
        for (int i = 0; i < 19; i++) {
            final int max = i + 123456;
            Assert.assertEquals(rng1.nextInt(max),
                                rng2.nextInt(max));
        }

        final int len = 233;
        final byte[] store1 = new byte[len];
        final byte[] store2 = new byte[len];
        rng1.nextBytes(store1);
        rng2.nextBytes(store2);
        for (int i = 0; i < len; i++) {
            Assert.assertEquals(store1[i],
                                store2[i]);
        }
    }

    /**
     * Test {@link UniformRandomProvider#nextLong(long)} matches that from the core
     * BaseProvider implementation.
     */
    @Test
    public void testNextLongInRange() {
        final long seed = RandomSource.createLong();
        // This will use the RNG core BaseProvider implementation.
        // Use a LongProvider to directly use the Random::nextLong method
        // which is different from IntProvider::nextLong.
        final UniformRandomProvider rng1 = new LongProvider() {
            private final Random random = new Random(seed);

            @Override
            public long next() {
                return random.nextLong();
            }
        };
        final UniformRandomProvider rng2 = new JDKRandomWrapper(new Random(seed));

        // Test cases
        // 1              : Smallest range
        // 256            : Integer power of 2
        // 56757          : Integer range
        // 1L << 32       : Non-integer power of 2
        // (1L << 62) + 1 : Worst case for rejection rate for the algorithm.
        //                  Reject probability is approximately 0.5 thus the test hits
        //                  all code paths.
        for (final long max : new long[] {1, 256, 56757, 1L << 32, (1L << 62) + 1}) {
            for (int i = 0; i < 10; i++) {
                Assert.assertEquals(rng1.nextLong(max),
                                    rng2.nextLong(max));
            }
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNextLongInRangeThrows() {
        final UniformRandomProvider rng1 = new JDKRandomWrapper(new Random(5675767L));
        rng1.nextLong(0);
    }

    /**
     * Test the bytes created by {@link UniformRandomProvider#nextBytes(byte[], int, int)} matches
     * {@link Random#nextBytes(byte[])}.
     */
    @Test
    public void testNextByteInRange() {
        final long seed = RandomSource.createLong();
        final Random rng1 = new Random(seed);
        final UniformRandomProvider rng2 = new JDKRandomWrapper(new Random(seed));

        checkSameBytes(rng1, rng2, 1, 0, 1);
        checkSameBytes(rng1, rng2, 100, 0, 100);
        checkSameBytes(rng1, rng2, 100, 10, 90);
        checkSameBytes(rng1, rng2, 245, 67, 34);
    }

    /**
     * Ensure that the bytes produced in a sub-range of a byte array by
     * {@link UniformRandomProvider#nextBytes(byte[], int, int)} match the bytes created
     * by the JDK {@link Random#nextBytes(byte[])}.
     *
     * @param rng1 JDK Random.
     * @param rng2 RNG.
     * @param size Size of byte array.
     * @param start Index at which to start inserting the generated bytes.
     * @param len Number of bytes to insert.
     */
    private static void checkSameBytes(Random rng1,
                                       UniformRandomProvider rng2,
                                       int size, int start, int length) {
        final byte[] store1 = new byte[length];
        final byte[] store2 = new byte[size];
        rng1.nextBytes(store1);
        rng2.nextBytes(store2, start, length);
        for (int i = 0; i < length; i++) {
            Assert.assertEquals(store1[i],
                                store2[i + start]);
        }
    }
}
