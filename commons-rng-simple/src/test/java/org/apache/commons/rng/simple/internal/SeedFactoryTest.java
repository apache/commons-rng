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

import java.util.Map;
import java.util.HashMap;
import org.junit.Assert;
import org.junit.Test;
import org.apache.commons.rng.core.source64.RandomLongSource;
import org.apache.commons.rng.core.util.NumberFactory;

/**
 * Tests for {@link SeedFactory}.
 */
public class SeedFactoryTest {
    @Test
    public void testCreateLong() {
        final Map<Long, Integer> values = new HashMap<Long, Integer>();

        final int n = 100000;
        for (int i = 0; i < n; i++) {
            final long v = SeedFactory.createLong();

            Integer count = values.get(v);
            if (count == null) {
                count = 0;
            }
            values.put(v, count + 1);
        }

        // Check that all seeds are different.
        assertDifferentValues(values);
    }

    @Test
    public void testCreateLongArray() {
        final Map<Long, Integer> values = new HashMap<Long, Integer>();

        final int n = 100000;
        final long[] array = SeedFactory.createLongArray(n);
        Assert.assertEquals(n, array.length);

        for (long v : array) {
            Integer count = values.get(v);
            if (count == null) {
                count = 0;
            }
            values.put(v, count + 1);
        }

        // Check that all seeds are different.
        assertDifferentValues(values);
    }

    @Test
    public void testCreateIntArray() {
        final Map<Long, Integer> values = new HashMap<Long, Integer>();

        for (int i = 0; i < 50000; i++) {
            final int[] a = SeedFactory.createIntArray(2);
            final long v = NumberFactory.makeLong(a[0], a[1]);
            Integer count = values.get(v);
            if (count == null) {
                count = 0;
            }
            values.put(v, count + 1);
        }

        // Check that all pairs in are different.
        assertDifferentValues(values);
    }

    /**
     * Asserts that all the keys in given {@code map} have their
     * value equal to 1.
     *
     * @param map Map to counts.
     */
    private static <T> void assertDifferentValues(Map<T, Integer> map) {
        final StringBuilder sb = new StringBuilder();

        int duplicates = 0;
        for (Map.Entry<T, Integer> entry : map.entrySet()) {
            final int count = entry.getValue();
            if (count <= 0) {
                throw new IllegalStateException();
            }

            if (count > 1) {
                duplicates += count - 1;
                sb.append(entry.getKey() + ": " + count + "\n");
            }
        }

        if (duplicates > 0) {
            Assert.fail(duplicates + " duplicates\n" + sb);
        }
    }

    @Test
    public void testCreateIntArrayWithCompleteBlockSize() {
        // Block size is 8 for int
        assertCreateIntArray(8);
    }

    @Test
    public void testCreateIntArrayWithIncompleteBlockSize() {
        // Block size is 8 for int
        assertCreateIntArray(8 + 1);
    }

    /**
     * Checks that the int array values can be placed into 2 bins with
     * approximately equal number of counts.
     * The test uses the expectation from a fixed-step "random walk".
     *
     * @param n The size of the array.
     */
    private static void assertCreateIntArray(int n) {
        final int[] array = SeedFactory.createIntArray(n);
        Assert.assertEquals("Incorrect array length", n, array.length);
        // The bit count should be 50%.
        int bitCount = 0;
        for (final int i : array) {
            bitCount += Integer.bitCount(i);
        }
        final int numberOfBits = n * Integer.SIZE;
        assertMonobit(bitCount, numberOfBits);
    }

    @Test
    public void testCreateLongArrayWithCompleteBlockSize() {
        // Block size is 4 for long
        assertCreateLongArray(4);
    }

    @Test
    public void testCreateLongArrayWithIncompleteBlockSize() {
        // Block size is 4 for long
        assertCreateLongArray(4 + 1);
    }

    /**
     * Checks that the long array values can be placed into 2 bins with
     * approximately equal number of counts.
     * The test uses the expectation from a fixed-step "random walk".
     *
     * @param n The size of the array.
     */
    private static void assertCreateLongArray(int n) {
        final long[] array = SeedFactory.createLongArray(n);
        Assert.assertEquals("Incorrect array length", n, array.length);
        // The bit count should be 50%.
        int bitCount = 0;
        for (final long i : array) {
            bitCount += Long.bitCount(i);
        }
        final int numberOfBits = n * Long.SIZE;
        assertMonobit(bitCount, numberOfBits);
    }

    /**
     * Assert that the number of 1 bits is approximately 50%. This is based upon a
     * fixed-step "random walk" of +1/-1 from zero.
     *
     * <p>The test is equivalent to the NIST Monobit test with a fixed p-value of 0.0001.
     * The number of bits is recommended to be above 100.</p>
     *
     * @see <A
     * href="https://csrc.nist.gov/publications/detail/sp/800-22/rev-1a/final">Bassham, et
     * al (2010) NIST SP 800-22: A Statistical Test Suite for Random and Pseudorandom
     * Number Generators for Cryptographic Applications. Section 2.1.</a>
     *
     * @param bitCount The bit count.
     * @param numberOfBits Number of bits.
     */
    private static void assertMonobit(int bitCount, int numberOfBits) {
        // Convert the bit count into a number of +1/-1 steps.
        final double sum = 2.0 * bitCount - numberOfBits;
        // The reference distribution is Normal with a standard deviation of sqrt(n).
        // Check the absolute position is not too far from the mean of 0 with a fixed
        // p-value of 0.0001 taken from a 2-tailed Normal distribution. Computation of
        // the p-value requires the complimentary error function.
        // The p-value is set to be equal to a 0.01 with 1 allowed re-run.
        // (Re-runs are not configured for this test.) 
        final double absSum = Math.abs(sum);
        final double max = Math.sqrt(numberOfBits) * 3.891;
        Assert.assertTrue("Walked too far astray: " + absSum +
                          " > " + max +
                          " (test will fail randomly about 1 in 10,000 times)",
                          absSum <= max);
    }

    @Test
    public void testEnsureNonZeroIntArrayIgnoresEmptySeed() {
        final int[] seed = new int[0];
        SeedFactory.ensureNonZero(seed);
    }

    @Test
    public void testEnsureNonZeroIntArrayIgnoresNonZeroPosition0() {
        final int position0 = 123;
        final int[] seed = new int[] {position0, 0, 0, 0};
        final int[] before = seed.clone();
        SeedFactory.ensureNonZero(seed);
        Assert.assertEquals("Non-zero at position 0 should be unmodified", position0, seed[0]);
        for (int i = 1; i < seed.length; i++) {
            Assert.assertEquals("Position above 0 should be unmodified", before[i], seed[i]);
        }
    }

    @Test
    public void testEnsureNonZeroIntArrayUpdatesZeroPosition0() {
        // Test the method replaces position 0 even if the rest of the array is non-zero
        final int[] seed = new int[] {0, 123, 456, 789};
        final int[] before = seed.clone();
        SeedFactory.ensureNonZero(seed);
        Assert.assertNotEquals("Zero at position 0 should be modified", 0, seed[0]);
        for (int i = 1; i < seed.length; i++) {
            Assert.assertEquals("Position above 0 should be unmodified", before[i], seed[i]);
        }
    }

    @Test
    public void testEnsureNonZeroLongArrayIgnoresEmptySeed() {
        final long[] seed = new long[0];
        SeedFactory.ensureNonZero(seed);
    }

    @Test
    public void testEnsureNonZeroLongArrayIgnoresNonZeroPosition0() {
        final long position0 = 123;
        final long[] seed = new long[] {position0, 0, 0, 0};
        final long[] before = seed.clone();
        SeedFactory.ensureNonZero(seed);
        Assert.assertEquals("Non-zero at position 0 should be unmodified", position0, seed[0]);
        for (int i = 1; i < seed.length; i++) {
            Assert.assertEquals("Position above 0 should be unmodified", before[i], seed[i]);
        }
    }

    @Test
    public void testEnsureNonZeroLongArrayUpdatesZeroPosition0() {
        // Test the method replaces position 0 even if the rest of the array is non-zero
        final long[] seed = new long[] {0, 123, 456, 789};
        final long[] before = seed.clone();
        SeedFactory.ensureNonZero(seed);
        Assert.assertNotEquals("Zero at position 0 should be modified", 0, seed[0]);
        for (int i = 1; i < seed.length; i++) {
            Assert.assertEquals("Position above 0 should be unmodified", before[i], seed[i]);
        }
    }

    @Test
    public void testEnsureNonZeroValue() {
        final long expected = 345;
        RandomLongSource source = new RandomLongSource() {
            @Override
            public long next() {
                return expected;
            }
        };
        Assert.assertEquals("Zero should be replaced using the random source",
                expected, SeedFactory.ensureNonZero(source, 0));
        for (final long nonZero : new long[] {Long.MIN_VALUE, -1, 1, 9876654321L, Long.MAX_VALUE}) {
            Assert.assertEquals("Non-zero should be unmodified",
                    nonZero, SeedFactory.ensureNonZero(source, nonZero));
        }
    }
}
