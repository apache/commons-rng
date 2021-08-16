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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link RandomSource}.
 */
class RandomSourceTest {
    @Test
    void testCreateInt() {
        final int n = 4;
        for (int i = 0; i < n; i++) {
            // Can fail, but unlikely given the range.
            Assertions.assertNotEquals(RandomSource.createInt(),
                                       RandomSource.createInt());
        }
    }

    @Test
    void testCreateLong() {
        final int n = 6;
        for (int i = 0; i < n; i++) {
            // Can fail, but unlikely given the range.
            Assertions.assertNotEquals(RandomSource.createLong(),
                                       RandomSource.createLong());
        }
    }

    @Test
    void testCreateIntArray() {
        final int n = 13;
        final int[] seed = RandomSource.createIntArray(n);
        Assertions.assertEquals(n, seed.length);

        for (int i = 1; i < n; i++) {
            // Can fail, but unlikely given the range.
            Assertions.assertNotEquals(seed[i - 1], seed[i]);
        }
    }

    @Test
    void testCreateLongArray() {
        final int n = 9;
        final long[] seed = RandomSource.createLongArray(n);
        Assertions.assertEquals(n, seed.length);

        for (int i = 1; i < n; i++) {
            // Can fail, but unlikely given the range.
            Assertions.assertNotEquals(seed[i - 1], seed[i]);
        }
    }

    @Test
    void testIsJumpable() {
        Assertions.assertFalse(RandomSource.JDK.isJumpable(), "JDK is not Jumpable");
        Assertions.assertTrue(RandomSource.XOR_SHIFT_1024_S_PHI.isJumpable(), "XOR_SHIFT_1024_S_PHI is Jumpable");
        Assertions.assertTrue(RandomSource.XO_SHI_RO_256_SS.isJumpable(), "XO_SHI_RO_256_SS is Jumpable");
    }

    @Test
    void testIsLongJumpable() {
        Assertions.assertFalse(RandomSource.JDK.isLongJumpable(), "JDK is not LongJumpable");
        Assertions.assertFalse(RandomSource.XOR_SHIFT_1024_S_PHI.isLongJumpable(), "XOR_SHIFT_1024_S_PHI is not LongJumpable");
        Assertions.assertTrue(RandomSource.XO_SHI_RO_256_SS.isLongJumpable(), "XO_SHI_RO_256_SS is LongJumpable");
    }
}
