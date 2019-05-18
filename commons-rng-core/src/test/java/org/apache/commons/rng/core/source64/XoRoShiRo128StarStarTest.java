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
package org.apache.commons.rng.core.source64;

import org.apache.commons.rng.core.RandomAssert;
import org.junit.Test;

public class XoRoShiRo128StarStarTest {
    /** The size of the array seed. */
    private static final int SEED_SIZE = 2;

    @Test
    public void testReferenceCode() {
        /*
         * Data from running the executable compiled from the author's C code:
         *   http://xoshiro.di.unimi.it/xoroshiro128starstar.c
         */
        final long[] seed = {
            0x012de1babb3c4104L, 0xa5a818b8fc5aa503L,
        };

        final long[] expectedSequence = {
            0x8856e974cbb6da12L, 0xd1704f3601beb952L, 0x368a027941bc61e7L, 0x882b24dcfa3ada58L,
            0xfa8dafb3363143fbL, 0x2eb9417a5dcf7654L, 0xed8722e0a73e975bL, 0x435fff57a631d485L,
            0x954f1ad2377632b8L, 0x9aa2f4dcba28ab71L, 0xaca10369f96ac911L, 0x968088e7277d0369L,
            0x662e442ae32c42b4L, 0xe1cd476f71dd058eL, 0xb462a3c2bbb650f8L, 0x74749215e8c07d08L,
            0x1629f3cb1a671dbbL, 0x3636dcc702eadf55L, 0x97ae682e61cb3f57L, 0xfdf8fc5ea9541f3bL,
            0x2dfdb23d99c34accL, 0x68bef4f41a8f4113L, 0x5cd03dc43f7af892L, 0xdc2184abe0565da1L,
            0x1dfaece40d9f96d0L, 0x7d7b19285818ab71L, 0xedea7fd3a0e47018L, 0x23542ee7ed294823L,
            0x1719f2b97bfc26c4L, 0x2c7b7e288b399818L, 0x49fa00786a1f5ad9L, 0xd97cdfbe81700be2L,
            0x557480baa4d9e5b2L, 0x840a0403c0e85d92L, 0xb4d5c6b2dc19dab2L, 0xdf1b570e3bf1cf1bL,
            0x26d1ac9455ccc75fL, 0xdcc0e5fe06d1e231L, 0x5164b7650568120eL, 0x5fa82f6598483607L,
        };

        RandomAssert.assertEquals(expectedSequence, new XoRoShiRo128StarStar(seed));
    }

    @Test
    public void testConstructorWithZeroSeedIsNonFunctional() {
        RandomAssert.assertNextIntZeroOutput(new XoRoShiRo128StarStar(new long[SEED_SIZE]), 2 * SEED_SIZE);
    }

    @Test
    public void testConstructorWithSingleBitSeedIsFunctional() {
        RandomAssert.assertLongArrayConstructorWithSingleBitSeedIsFunctional(XoRoShiRo128StarStar.class, SEED_SIZE);
    }

    @Test
    public void testConstructorWithoutFullLengthSeed() {
        // Hit the case when the input seed is self-seeded when not full length
        new XoRoShiRo128StarStar(new long[] { 0x012de1babb3c4104L });
    }

    @Test
    public void testElementConstructor() {
        final long[] seed = {
            0x012de1babb3c4104L, 0xa5a818b8fc5aa503L,
        };
        final XoRoShiRo128StarStar rng1 = new XoRoShiRo128StarStar(seed);
        final XoRoShiRo128StarStar rng2 = new XoRoShiRo128StarStar(seed[0], seed[1]);
        RandomAssert.assertNextLongEquals(seed.length * 2, rng1, rng2);
    }
}
