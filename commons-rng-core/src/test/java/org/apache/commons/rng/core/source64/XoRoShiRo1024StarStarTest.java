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
import org.junit.jupiter.api.Test;

public class XoRoShiRo1024StarStarTest {
    /** The size of the array SEED. */
    private static final int SEED_SIZE = 16;

    /*
     * Data from running the executable compiled from the author's C code:
     *   http://xoshiro.di.unimi.it/xoroshiro1024starstar.c
     */

    private static final long[] SEED = {
        0x012de1babb3c4104L, 0xa5a818b8fc5aa503L, 0xb124ea2b701f4993L, 0x18e0374933d8c782L,
        0x2af8df668d68ad55L, 0x76e56f59daa06243L, 0xf58c016f0f01e30fL, 0x8eeafa41683dbbf4L,
        0x7bf121347c06677fL, 0x4fd0c88d25db5ccbL, 0x99af3be9ebe0a272L, 0x94f2b33b74d0bdcbL,
        0x24b5d9d7a00a3140L, 0x79d983d781a34a3cL, 0x582e4a84d595f5ecL, 0x7316fe8b0f606d20L,
    };

    private static final long[] EXPECTED_SEQUENCE = {
        0x462c422df780c48eL, 0xbe94d15abff76d8aL, 0xb4dbef0e8d88ef2eL, 0xdfa2836db33bfc43L,
        0x2a4965b718a2e4f1L, 0xce2042d1aa74d50dL, 0xa6febfa96d04f58dL, 0xb16b1ce69018ab5dL,
        0xd9a067d3c7a7d9ffL, 0xe6c40f3b3e470500L, 0x54c0b9c458ae5b94L, 0xfba57390e5542333L,
        0x9e1670e4da0647b0L, 0x118cacc5ae1d413cL, 0x855f38d9f9975117L, 0xb1d8ae900456c302L,
        0x30d6603089b0b5a5L, 0x9b0a5cdd71d36a73L, 0x22598c5adb8a49e3L, 0xbe240590cf3abae3L,
        0x9c474364766386e4L, 0x675f099732c21ff2L, 0x432308deff79f4ccL, 0x18106f6cbcbb93d5L,
        0x5f87dd27193d4bf5L, 0xd540713e20f70062L, 0xa8e03c5477d99848L, 0xc01f257b1ad88046L,
        0x67522ec1327b3994L, 0x4c05d92051d406faL, 0xa03daf3fcd37a5ccL, 0x821445c6408c9722L,
        0xf7bbbffc2db460bdL, 0x5b42694c4af4d5caL, 0x408899b212aec78eL, 0x8cf109d6952df65eL,
        0xb7e8d62389e997cdL, 0xf4d82497338d8c89L, 0x7f53cea4f43609b9L, 0xa0ecb8e0fa98f352L,
    };

    private static final long[] EXPECTED_SEQUENCE_AFTER_JUMP = {
        0x5b636f163fc729e7L, 0x707beb36c524d1bdL, 0xa04aa6338804faa5L, 0xa05a8b17c27aadbaL,
        0xaa753a7fb6657575L, 0xe22cf9d201f9ee0cL, 0x922b4f8e56fae8bdL, 0xb460d44f4dac553aL,
        0x6b6a2771b70f8dc9L, 0x9ba789987085a9eeL, 0x5d3a89d1d38e935eL, 0x4d8cf26eba3ceb11L,
        0x17773c0482382356L, 0x7348d4e4e6ff6ea7L, 0x21f79b80d74d2e00L, 0xea221a71545c67b9L,
        0x49aeedb5ca88a5e0L, 0xdf91aaef5bca531bL, 0x91ad24c1bc05880aL, 0x1b4c2a769c2a0602L,
        0xfb45e555e943d087L, 0x2fa45261b0447ae8L, 0xc7df5ba87f846bebL, 0x0ee7351cbc43681eL,
        0x475aefe9ee1743ceL, 0x277a4780d6ac2a37L, 0xbc88b9e78bad80beL, 0x3cc1034ce7df5ca4L,
        0x1f10fcc26ef5376aL, 0x09b9a2233e5bc52fL, 0xe074e46d4d117fecL, 0x5303bc71643cb162L,
        0xbd148f7c799e6bb3L, 0xb562ea36a5c9ad62L, 0xbb0ed30e291a901fL, 0x0be93a50e3ad0c4aL,
        0x08a3a364587f88a9L, 0xc799a663b02566efL, 0x69d258c77cf97b43L, 0x8e14b0ffaf0f6bb2L,
    };

    private static final long[] EXPECTED_SEQUENCE_AFTER_LONG_JUMP = {
        0x29d4cc1fe4502216L, 0x81fe29054d484511L, 0xcf4e2f1f65dc852aL, 0x05993959c8d9d985L,
        0xb57512c9299013c2L, 0xd2b8a57fc03dd67aL, 0x76ab1e45f4b8ff83L, 0xd276813593cac49bL,
        0xc2f796572548f66fL, 0x7bad52ec5018d885L, 0x7d679c0324cdb96cL, 0xae500f725e175105L,
        0x869fde40ae42de70L, 0x2b8d995a2dfbac20L, 0x32391cf98dcc6fe7L, 0xd5ef6a4b2900cbb5L,
        0x0f6dc088c163aba4L, 0x5f072870f2394255L, 0x0915668ce75ca5bfL, 0xee97505819af2f8aL,
        0x0e0aaeb44813209bL, 0x1aa3e857d2a250f2L, 0x8fc5f60afad20c9eL, 0x3be2b0549452d2e8L,
        0x7ec70fe0db81c3a3L, 0x97941a6b3d611110L, 0x0ad410b1b1a0a1edL, 0x24574ac5004bd459L,
        0x46cd9e7cb86a283dL, 0xae3164713f141638L, 0x36a3e58636929c8dL, 0x5db41ba175a678a2L,
        0x54052dfa35a50c8aL, 0xf24bd17fad5e7174L, 0xc5c51f1cbc2f8a21L, 0x34b1ca5c1769f076L,
        0xbb5924f5674d6712L, 0x9309c6aa3701f4c8L, 0x1885c9eedd107655L, 0x55f4a50aeda95998L,
    };

    @Test
    public void testReferenceCode() {
        RandomAssert.assertEquals(EXPECTED_SEQUENCE, new XoRoShiRo1024StarStar(SEED));
    }

    @Test
    public void testConstructorWithZeroSeedIsNonFunctional() {
        RandomAssert.assertNextIntZeroOutput(new XoRoShiRo1024StarStar(new long[SEED_SIZE]), 2 * SEED_SIZE);
    }

    @Test
    public void testConstructorWithSingleBitSeedIsFunctional() {
        RandomAssert.assertLongArrayConstructorWithSingleBitSeedIsFunctional(XoRoShiRo1024StarStar.class, SEED_SIZE);
    }

    @Test
    public void testConstructorWithoutFullLengthSeed() {
        // Hit the case when the input seed is self-seeded when not full length
        RandomAssert.assertNextLongNonZeroOutput(new XoRoShiRo1024StarStar(new long[] {SEED[0]}),
                SEED_SIZE, SEED_SIZE);
    }

    @Test
    public void testJump() {
        RandomAssert.assertJumpEquals(EXPECTED_SEQUENCE, EXPECTED_SEQUENCE_AFTER_JUMP, new XoRoShiRo1024StarStar(SEED));
    }

    @Test
    public void testLongJump() {
        RandomAssert.assertLongJumpEquals(EXPECTED_SEQUENCE, EXPECTED_SEQUENCE_AFTER_LONG_JUMP, new XoRoShiRo1024StarStar(SEED));
    }
}
