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
}
