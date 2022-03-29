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
package org.apache.commons.rng.examples.stress;

import org.apache.commons.rng.UniformRandomProvider;
import org.apache.commons.rng.core.source64.SplitMix64;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link RNGUtils}.
 */
class RNGUtilsTest {
    @Test
    void testCreateIntProviderLongThrows() {
        final SplitMix64 rng = new SplitMix64(42);
        Assertions.assertThrows(IllegalArgumentException.class,
            () -> RNGUtils.createIntProvider(rng, Source64Mode.LONG));
    }

    @Test
    void testCreateIntProviderInt() {
        final long seed = 236784264237894L;
        final UniformRandomProvider rng1 = new SplitMix64(seed);
        final UniformRandomProvider rng2 =
            RNGUtils.createIntProvider(new SplitMix64(seed), Source64Mode.INT);
        for (int i = 0; i < 50; i++) {
            Assertions.assertEquals(rng1.nextInt(), rng2.nextInt());
        }
    }

    @Test
    void testCreateIntProviderLoHi() {
        final long seed = 236784264237894L;
        final UniformRandomProvider rng1 = new SplitMix64(seed);
        final UniformRandomProvider rng2 =
            RNGUtils.createIntProvider(new SplitMix64(seed), Source64Mode.LO_HI);
        for (int i = 0; i < 50; i++) {
            final long l = rng1.nextLong();
            final int hi = (int) (l >>> 32);
            final int lo = (int) l;
            Assertions.assertEquals(lo, rng2.nextInt());
            Assertions.assertEquals(hi, rng2.nextInt());
        }
    }

    @Test
    void testCreateIntProviderHiLo() {
        final long seed = 2367234237894L;
        final UniformRandomProvider rng1 = new SplitMix64(seed);
        final UniformRandomProvider rng2 =
            RNGUtils.createIntProvider(new SplitMix64(seed), Source64Mode.HI_LO);
        for (int i = 0; i < 50; i++) {
            final long l = rng1.nextLong();
            final int hi = (int) (l >>> 32);
            final int lo = (int) l;
            Assertions.assertEquals(hi, rng2.nextInt());
            Assertions.assertEquals(lo, rng2.nextInt());
        }
    }

    @Test
    void testCreateIntProviderHi() {
        final long seed = 2367234237894L;
        final UniformRandomProvider rng1 = new SplitMix64(seed);
        final UniformRandomProvider rng2 =
            RNGUtils.createIntProvider(new SplitMix64(seed), Source64Mode.HI);
        for (int i = 0; i < 50; i++) {
            final int hi = (int) (rng1.nextLong() >>> 32);
            Assertions.assertEquals(hi, rng2.nextInt());
        }
    }

    @Test
    void testCreateIntProviderLo() {
        final long seed = 2367234237894L;
        final UniformRandomProvider rng1 = new SplitMix64(seed);
        final UniformRandomProvider rng2 =
            RNGUtils.createIntProvider(new SplitMix64(seed), Source64Mode.LO);
        for (int i = 0; i < 50; i++) {
            final int lo = (int) rng1.nextLong();
            Assertions.assertEquals(lo, rng2.nextInt());
        }
    }

    /**
     * Test that the default source64 mode matches the nextInt implementation for a LongProvider.
     * If this changes then the default mode should be updated. The value is used as
     * the default for the stress test application.
     */
    @Test
    void testCreateIntProviderDefault() {
        final long seed = 236784264237894L;
        final UniformRandomProvider rng1 = new SplitMix64(seed);
        final UniformRandomProvider rng2 =
            RNGUtils.createIntProvider(new SplitMix64(seed), RNGUtils.getSource64Default());
        for (int i = 0; i < 50; i++) {
            final int a = rng1.nextInt();
            final int b = rng1.nextInt();
            Assertions.assertEquals(a, rng2.nextInt());
            Assertions.assertEquals(b, rng2.nextInt());
        }
    }
}
