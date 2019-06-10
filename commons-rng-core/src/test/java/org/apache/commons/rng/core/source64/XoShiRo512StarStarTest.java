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
    /** The size of the array SEED. */
    private static final int SEED_SIZE = 8;

    /*
     * Data from running the executable compiled from the author's C code:
     *   http://xoshiro.di.unimi.it/xoshiro512starstar.c
     */

    private static final long[] SEED = {
        0x012de1babb3c4104L, 0xa5a818b8fc5aa503L, 0xb124ea2b701f4993L, 0x18e0374933d8c782L,
        0x2af8df668d68ad55L, 0x76e56f59daa06243L, 0xf58c016f0f01e30fL, 0x8eeafa41683dbbf4L,
    };

    private static final long[] EXPECTED_SEQUENCE = {
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

    private static final long[] EXPECTED_SEQUENCE_AFTER_JUMP = {
        0x49738e6a1dc6cfe4L, 0x20d43df324825b53L, 0x901311368abdf6ffL, 0x5b6a89ae30e497cfL,
        0xf39563a09132cd18L, 0x31617c165823312dL, 0xf661249f2e13379aL, 0x83182e7f15ea1d74L,
        0x2138679c77623fe3L, 0x659ea7f79918b8edL, 0x95d1cd36efefe94aL, 0xdca9a32638633311L,
        0x29e61a381e2d6cd7L, 0xe77e58a6f50d890eL, 0x2bd956f2c3efb142L, 0xf7ac562caac34825L,
        0x211db4255861df4bL, 0x0631b9f14d2c23f5L, 0x3fd2f6b40055cee4L, 0x07303eb9aba24a7bL,
        0x3edb03a893e24a16L, 0x82d9065956477f0aL, 0x4ff3574b1ae46dd4L, 0xaaf0f48c44668055L,
        0x2e778b8218a3fe10L, 0xe29fe50ff75a3fd4L, 0x5ab758438572ba1cL, 0x94bb2aca7d056186L,
        0x7699c4588a722dc4L, 0x131fd3c8d477a06aL, 0xc87f6eb21f8d91b8L, 0x5a273a9c0a89e24fL,
        0xa4466c28e147be23L, 0x877e97af45d42bbcL, 0x07f0c9e5e1fb133fL, 0x3a2f2422d2a9af03L,
        0x2f1f49ee5e1d5207L, 0xafca316bd4672318L, 0x91e9df4fe8cb3cddL, 0x8857a767c3a1d387L,
    };

    @Test
    public void testReferenceCode() {
        RandomAssert.assertEquals(EXPECTED_SEQUENCE, new XoShiRo512StarStar(SEED));
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
        new XoShiRo512StarStar(new long[] {SEED[0]});
    }

    @Test
    public void testElementConstructor() {
        final XoShiRo512StarStar rng1 = new XoShiRo512StarStar(SEED);
        final XoShiRo512StarStar rng2 = new XoShiRo512StarStar(SEED[0], SEED[1], SEED[2], SEED[3],
                                                               SEED[4], SEED[5], SEED[6], SEED[7]);
        RandomAssert.assertNextLongEquals(SEED.length * 2, rng1, rng2);
    }

    @Test
    public void testJump() {
        RandomAssert.assertJumpEquals(EXPECTED_SEQUENCE, EXPECTED_SEQUENCE_AFTER_JUMP, new XoShiRo512StarStar(SEED));
    }
}
