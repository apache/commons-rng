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

import org.junit.Assert;
import org.junit.Test;

/**
 * Tests for {@link RandomSource}.
 */
public class RandomSourceTest {
    @Test
    public void testCreateInt() {
        final int n = 4;
        for (int i = 0; i < n; i++) {
            // Can fail, but unlikely given the range.
            Assert.assertNotEquals(RandomSource.createInt(),
                                   RandomSource.createInt());
        }
    }

    @Test
    public void testCreateLong() {
        final int n = 6;
        for (int i = 0; i < n; i++) {
            // Can fail, but unlikely given the range.
            Assert.assertNotEquals(RandomSource.createLong(),
                                   RandomSource.createLong());
        }
    }

    @Test
    public void testCreateIntArray() {
        final int n = 13;
        final int[] seed = RandomSource.createIntArray(n);
        Assert.assertEquals(n, seed.length);

        for (int i = 1; i < n; i++) {
            // Can fail, but unlikely given the range.
            Assert.assertNotEquals(seed[i - 1], seed[i]);
        }
    }

    @Test
    public void testCreateLongArray() {
        final int n = 9;
        final long[] seed = RandomSource.createLongArray(n);
        Assert.assertEquals(n, seed.length);

        for (int i = 1; i < n; i++) {
            // Can fail, but unlikely given the range.
            Assert.assertNotEquals(seed[i - 1], seed[i]);
        }
    }
}
