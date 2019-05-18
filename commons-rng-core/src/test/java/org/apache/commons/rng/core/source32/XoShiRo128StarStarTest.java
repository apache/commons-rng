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
package org.apache.commons.rng.core.source32;

import org.apache.commons.rng.core.RandomAssert;
import org.junit.Test;

public class XoShiRo128StarStarTest {
    /** The size of the array seed. */
    private static final int SEED_SIZE = 4;

    @Test
    public void testReferenceCode() {
        /*
         * Data from running the executable compiled from the author's C code:
         *   http://xoshiro.di.unimi.it/xoshiro128starstar.c
         */
        final int[] seed = {
            0x012de1ba, 0xa5a818b8, 0xb124ea2b, 0x18e03749,
        };

        final int[] expectedSequence = {
            0x8856d912, 0xf2a19a86, 0x7693f66d, 0x23516f86,
            0x4895054e, 0xf4503fe6, 0x40e04672, 0x99244e34,
            0xb971815c, 0x3008b82c, 0x0ee73b58, 0x88aad2c6,
            0x7923f2e9, 0xfde55485, 0x7aed95f5, 0xeb8abb59,
            0xca78183a, 0x80ecdd68, 0xfd404b06, 0x248b9c9e,
            0xa2c69c6f, 0x1723b375, 0x879f37b0, 0xe98fd208,
            0x75de84a9, 0x717d6df8, 0x92cd7bc7, 0x46380167,
            0x7f08600b, 0x58566f2b, 0x7f781475, 0xe34ec04d,
            0x6d5ef889, 0xb76ff6d8, 0x501f5df6, 0x4cf70ccb,
            0xd7375b26, 0x457ea1ab, 0x7439e565, 0x355855af,
        };

        RandomAssert.assertEquals(expectedSequence, new XoShiRo128StarStar(seed));
    }

    @Test
    public void testConstructorWithZeroSeedIsNonFunctional() {
        RandomAssert.assertNextIntZeroOutput(new XoShiRo128StarStar(new int[SEED_SIZE]), 2 * SEED_SIZE);
    }

    @Test
    public void testConstructorWithSingleBitSeedIsFunctional() {
        RandomAssert.assertIntArrayConstructorWithSingleBitSeedIsFunctional(XoShiRo128StarStar.class, SEED_SIZE);
    }

    @Test
    public void testConstructorWithoutFullLengthSeed() {
        // Hit the case when the input seed is self-seeded when not full length
        new XoShiRo128StarStar(new int[] { 0x012de1ba });
    }

    @Test
    public void testElementConstructor() {
        final int[] seed = {
            0x012de1ba, 0xa5a818b8, 0xb124ea2b, 0x18e03749,
        };
        final XoShiRo128StarStar rng1 = new XoShiRo128StarStar(seed);
        final XoShiRo128StarStar rng2 = new XoShiRo128StarStar(seed[0], seed[1], seed[2], seed[3]);
        RandomAssert.assertNextIntEquals(seed.length * 2, rng1, rng2);
    }
}
