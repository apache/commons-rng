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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import org.apache.commons.rng.UniformRandomProvider;
import org.apache.commons.rng.core.source32.IntProvider;
import org.apache.commons.rng.core.source64.LongProvider;
import org.apache.commons.rng.core.util.NumberFactory;
import org.junit.Assert;
import org.junit.Test;

/**
 * Tests the cached UniformRandomProvider correctly return values.
 */
public class CachedUniformRandomProviderFactoryTest {

    /**
     * Provide an empty implementation of UniformRandomProvider.
     */
    static class EmptyUniformRandomProvider implements UniformRandomProvider {
        @Override
        public void nextBytes(byte[] bytes) { /* Do nothing */ }
        @Override
        public void nextBytes(byte[] bytes, int start, int len) { /* Do nothing */ }
        @Override
        public int nextInt() { return 0; }
        @Override
        public int nextInt(int n) { return 0; }
        @Override
        public long nextLong() { return 0; }
        @Override
        public long nextLong(long n) { return 0; }
        @Override
        public boolean nextBoolean() { return false; }
        @Override
        public float nextFloat() { return 0; }
        @Override
        public double nextDouble() { return 0; }
    }

    @Test
    public void testUnsupportedUniformRandomProviderIsNotWrapped() {
        // Create an unsupported implementation
        final UniformRandomProvider rng = new EmptyUniformRandomProvider();
        final UniformRandomProvider rng2 = CachedUniformRandomProviderFactory.wrap(rng);
        Assert.assertSame(rng, rng2);
    }

    @Test
    public void testWrappedUniformRandomProviderIsNotWrapped() {
        // Create an supported implementation
        final UniformRandomProvider rng = new IntProvider() {
            @Override
            public int next() {
                return 0;
            }
        };
        // Wrap first time
        final UniformRandomProvider rng2 = CachedUniformRandomProviderFactory.wrap(rng);
        Assert.assertNotSame("Single wrapped", rng, rng2);
        // Do not double wrap
        final UniformRandomProvider rng3 = CachedUniformRandomProviderFactory.wrap(rng2);
        Assert.assertSame("Double wrapped", rng2, rng3);
    }

    @Test
    public void testIntProviderNextBoolean() {
        final Random random = new Random();
        final int size = 32;
        // Test all possible number of bits
        for (int bitsPerRepeat = 0; bitsPerRepeat <= size; bitsPerRepeat++) {
            // Test an IntProvider that returns the given number of bits.
            final int bits = bitsPerRepeat;
            final UniformRandomProvider rng = new IntProvider() {
                @Override
                public int next() {
                    // Generate a value with the correct number of bits set
                    final List<Boolean> list = createList(bits, size);
                    Collections.shuffle(list, random);
                    int v = 0;
                    for (int i = 0; i < size; i++) {
                        // An set bit corresponds to true
                        if (list.get(i)) {
                            v |= (1 << i);
                        }
                    }
                    return v;
                }
            };
            // Zero bits will be counted as true the bitsPerRepeat
            // starts at zero an increases
            testNextBoolean(rng, bitsPerRepeat, size);
        }
    }

    @Test
    public void testLongProviderNextBoolean() {
        final Random random = new Random();
        final int size = 64;
        // Test all possible number of bits
        for (int bitsPerRepeat = 0; bitsPerRepeat <= size; bitsPerRepeat++) {
            // Test an IntProvider that returns the given number of bits.
            final int bits = bitsPerRepeat;
            final UniformRandomProvider rng = new LongProvider() {
                @Override
                public long next() {
                    // Generate a value with the correct number of bits set
                    final List<Boolean> list = createList(bits, size);
                    Collections.shuffle(list, random);
                    long v = 0;
                    for (int i = 0; i < size; i++) {
                        // An set bit corresponds to true
                        if (list.get(i)) {
                            v |= (1L << i);
                        }
                    }
                    return v;
                }
            };
            // Zero bits will be counted as true the bitsPerRepeat
            // starts at zero an increases
            testNextBoolean(rng, bitsPerRepeat, size);
        }
    }

    /**
     * Create a list of the given size with the specified number of bits set as
     * 'true' values.
     *
     * @param bits the bits
     * @param size the size
     * @return the list
     */
    private static List<Boolean> createList(int bits,
                                            int size) {
        final ArrayList<Boolean> list = new ArrayList<Boolean>(size);
        for (int i = 0; i < bits; i++) {
            list.add(true);
        }
        while (list.size() < size) {
            list.add(false);
        }
        return list;
    }

    /**
     * Test nextBoolean() by calling repeatedly and checking the
     * correct number of 'true' boolean bits were returned per repeat.
     *
     * @param source the source
     * @param trueBitsPerRepeat the bits per repeat that are 'true'
     * @param totalBitsPerRepeat the total bits in each repeat (must be a factor of 32)
     */
    private static void testNextBoolean(UniformRandomProvider source,
                                        int trueBitsPerRepeat,
                                        int totalBitsPerRepeat) {
        final UniformRandomProvider rng = CachedUniformRandomProviderFactory.wrap(source);
        int count = 0;
        final int total = 64 * 2; // Factor of 64
        for (int i = 0; i < total; i++) {
            if (rng.nextBoolean()) {
                count++;
            }
        }
        final int repeats = total / totalBitsPerRepeat;
        final int expected = trueBitsPerRepeat * repeats;
        Assert.assertEquals("Incorrect number of 'true' bits", expected, count);
    }

    @Test
    public void testLongProviderNextInt() {
        // Generate some random ints and pack them into longs
        final int[] expected = new int[50];
        final long[] values = new long[expected.length / 2];
        final Random random = new Random();
        for (int i = 0, j = 0; i < expected.length; i += 2, j++) {
            final int i1 = random.nextInt();
            final int i2 = random.nextInt();
            expected[i] = i1;
            expected[i + 1] = i2;
            values[j] = NumberFactory.makeLong(i1, i2);
        }

        // Test a LongProvider that returns the given sequence values
        final UniformRandomProvider source = new LongProvider() {
            int count = 0;
            @Override
            public long next() {
                return values[count++];
            }
        };

        final int[] actual = new int[expected.length];
        final UniformRandomProvider rng = CachedUniformRandomProviderFactory.wrap(source);
        for (int i = 0; i < expected.length; i ++) {
            actual[i] = rng.nextInt();
        }
        Assert.assertArrayEquals("Invalid int sequence", expected, actual);
    }
}
