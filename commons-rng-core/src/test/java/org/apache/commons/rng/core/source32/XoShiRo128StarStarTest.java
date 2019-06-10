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

    /*
     * Data from running the executable compiled from the author's C code:
     *   http://xoshiro.di.unimi.it/xoshiro128starstar.c
     */

    private static final int[] SEED = {
        0x012de1ba, 0xa5a818b8, 0xb124ea2b, 0x18e03749,
    };

    private static final int[] EXPECTED_SEQUENCE = {
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

    private static final int[] EXPECTED_SEQUENCE_AFTER_JUMP  = {
        0xab597786, 0x9e11647e, 0x147c0f8e, 0xfe86b079,
        0x8cda108b, 0x461f24ad, 0x00204529, 0xdfa240cf,
        0xcc697f88, 0x11a49521, 0x5e6eb377, 0xe7dad980,
        0xc522beed, 0x54049c0d, 0x5409c7de, 0x22148129,
        0x2d7fc96e, 0x288d7114, 0xfb18c495, 0x689c4e2b,
        0xd7219504, 0xb2d81d4d, 0xe66c9680, 0xeec149b3,
        0x82fad922, 0x49e59804, 0x7be8f245, 0xbc193d57,
        0xa542f0d5, 0x07474d51, 0xff3c7b3d, 0x2eda9beb,
        0xca7657a3, 0xcf58554d, 0x5fe25af7, 0x5beb7d19,
        0x58339082, 0x6f7ac9ed, 0xf07faa96, 0x7348dcbf,
    };

    @Test
    public void testReferenceCode() {
        RandomAssert.assertEquals(EXPECTED_SEQUENCE, new XoShiRo128StarStar(SEED));
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
        new XoShiRo128StarStar(new int[] {SEED[0]});
    }

    @Test
    public void testElementConstructor() {
        final XoShiRo128StarStar rng1 = new XoShiRo128StarStar(SEED);
        final XoShiRo128StarStar rng2 = new XoShiRo128StarStar(SEED[0], SEED[1], SEED[2], SEED[3]);
        RandomAssert.assertNextIntEquals(SEED.length * 2, rng1, rng2);
    }

    @Test
    public void testJump() {
        RandomAssert.assertJumpEquals(EXPECTED_SEQUENCE, EXPECTED_SEQUENCE_AFTER_JUMP, new XoShiRo128StarStar(SEED));
    }
}
