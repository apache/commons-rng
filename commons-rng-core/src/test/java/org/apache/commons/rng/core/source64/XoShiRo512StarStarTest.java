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

public class XoShiRo512StarStarTest {
    /** The size of the array seed. */
    private static final int SEED_SIZE = 8;

    @Test
    public void testReferenceCode() {
        /*
         * Data from running the executable compiled from the author's C code:
         *   http://xoshiro.di.unimi.it/xoshiro512starstar.c
         */
        final long[] seed = {
            0x012de1babb3c4104L, 0xa5a818b8fc5aa503L, 0xb124ea2b701f4993L, 0x18e0374933d8c782L,
            0x2af8df668d68ad55L, 0x76e56f59daa06243L, 0xf58c016f0f01e30fL, 0x8eeafa41683dbbf4L,
        };

        final long[] expectedSequence = {
            0x462c422df780c48eL, 0xa82f1f6031c183e6L, 0x60559add0e1e369aL, 0xf956a2b900083a8dL,
            0x0e5c039df1576573L, 0x2f35cef71b14aa24L, 0x5809ea8aa1d5a045L, 0x3e695e3189ccf9bdL,
            0x1eb940ee4bcb1a08L, 0x78b72a0927bd9257L, 0xe1a8e8d6dc64600bL, 0x3993bff6e1378a4bL,
            0x439161ee3b5d5cc8L, 0xac6ca2359fe7f321L, 0xc4238c5785d320e2L, 0x75cf64526530aed5L,
            0x679241ffc120e2b1L, 0xded30a8f20b24c73L, 0xff8ac62cff0deb9bL, 0xe63a25973df23c45L,
            0x74742f9096c56401L, 0xc573afa2368288acL, 0x9b1048cf2daf9f9dL, 0xe7d9720c2f51ca5fL,
            0x38a21e1f7a441cedL, 0x78835d75a9bd17a6L, 0xeb64167a723de35fL, 0x9455dd663e40620cL,
            0x88693a769f203ed1L, 0xea5f0997a281cffcL, 0x2662b83f835f3273L, 0x5e90efde2150ed04L,
            0xd481b14551c8f8d9L, 0xf2e4d714a0ab22d7L, 0xdfb1a8f0637a2013L, 0x8cd8d8c353640028L,
            0xb4ce3b66785e0cc6L, 0xa51386e09b6ab734L, 0xfeac4151ac4a3f8dL, 0x0e5679853ab5180bL,
        };

        RandomAssert.assertEquals(expectedSequence, new XoShiRo512StarStar(seed));
    }

    @Test
    public void testConstructorWithZeroSeedIsNonFunctional() {
        RandomAssert.assertNextIntZeroOutput(new XoShiRo512StarStar(new long[SEED_SIZE]), 2 * SEED_SIZE);
    }

    @Test
    public void testConstructorWithSingleBitSeedIsFunctional() {
        RandomAssert.assertLongArrayConstructorWithSingleBitSeedIsFunctional(XoShiRo512StarStar.class, SEED_SIZE);
    }

    @Test
    public void testConstructorWithoutFullLengthSeed() {
        // Hit the case when the input seed is self-seeded when not full length
        new XoShiRo512StarStar(new long[] { 0x012de1babb3c4104L });
    }

    @Test
    public void testElementConstructor() {
        final long[] seed = {
            0x012de1babb3c4104L, 0xa5a818b8fc5aa503L, 0xb124ea2b701f4993L, 0x18e0374933d8c782L,
            0x2af8df668d68ad55L, 0x76e56f59daa06243L, 0xf58c016f0f01e30fL, 0x8eeafa41683dbbf4L,
        };
        final XoShiRo512StarStar rng1 = new XoShiRo512StarStar(seed);
        final XoShiRo512StarStar rng2 = new XoShiRo512StarStar(seed[0], seed[1], seed[2], seed[3],
                                                               seed[4], seed[5], seed[6], seed[7]);
        RandomAssert.assertNextLongEquals(seed.length * 2, rng1, rng2);
    }
}
