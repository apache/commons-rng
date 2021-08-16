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

class XoRoShiRo128StarStarTest {
    /** The size of the array SEED. */
    private static final int SEED_SIZE = 2;

    /*
     * Data from running the executable compiled from the author's C code:
     *   http://xorshift.di.unimi.it/xorshift1024star.c
     */

    private static final long[] SEED = {
        0x012de1babb3c4104L, 0xa5a818b8fc5aa503L
    };

    private static final long[] EXPECTED_SEQUENCE = {
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

    private static final long[] EXPECTED_SEQUENCE_AFTER_JUMP = {
        0xbfc83ba4fd4868deL, 0x3f3eed826e632fddL, 0x9dff0e4eeebb7d32L, 0x686d1cc4eabb9535L,
        0xc091c73d7fbf9d0fL, 0x1042f833a9e605c8L, 0x525ef7b2826c2f23L, 0xe550c19eaaf09c9cL,
        0x091b0069cde91826L, 0x0cd7b89fce47625dL, 0x51f7596cfccd79d2L, 0xf36137464d58d44aL,
        0xc41979c626c4523cL, 0x6fe485d7bc4c268bL, 0x70d606b170542c89L, 0xb2a1ebcaa0b1411eL,
        0x4fec797dbbdeac3eL, 0x55c95cc08792d434L, 0x10e8fa257142e24aL, 0x6a7203a3c55725dfL,
        0x4c5606f2b8a65adbL, 0x5fc275ff7c58e74fL, 0x52272729918e8ac7L, 0xd495b534244c4ad2L,
        0x74be6afb953ce5aeL, 0xe61156ee003e41c9L, 0x5098c77788c9bf25L, 0xde679d7898225baaL,
        0x3568366f6aefe488L, 0x2afe2420f9f9dbccL, 0x1ca0bcb15f1d1accL, 0xf1677c09f3df59e1L,
        0x45dadcac4957d8d0L, 0xc46265d2810fe48eL, 0xa9234384080ec825L, 0x4a4e054ced918e13L,
        0xa9b19565638b0e2bL, 0x755bf81250abd6c1L, 0x2b8ba00b2714bf8fL, 0x5e165740374e0fa6L,
    };

    private static final long[] EXPECTED_SEQUENCE_AFTER_LONG_JUMP = {
        0x77781c11728ccab7L, 0x53ebc2b248baf6bfL, 0x891c834850b9a60aL, 0x3d75b3f460049bc4L,
        0x0ce2e3bec7efd177L, 0xcb3eac404f71e15dL, 0x4d1dd0a773be7287L, 0x2ef0fbefdd874642L,
        0x063a585e07c58b9dL, 0xd5b5174e803858efL, 0x40dfddff6a4743efL, 0xfa4e62e3fb9b2de2L,
        0x66fa3013cb6c4ca0L, 0x5b53086fea56344fL, 0x2ed92fdc75a14297L, 0x0cf92e9456f1e047L,
        0x5455e58538979accL, 0xb4226b69b40f5c89L, 0xe6de8c807f5fdf89L, 0xf9c898bc7c8815cbL,
        0x1c0363d696d06dccL, 0x4d7765ac48833302L, 0x84cf12882a284c7eL, 0x1d19d839c41f08b7L,
        0xec30f743f1fdf8beL, 0x61a7536651dadd56L, 0xff456aeab1bc73cfL, 0x671390bb0e2dcad4L,
        0xb51b533c7dc9cbccL, 0xa78c13a798610f10L, 0xb1cc573c29364864L, 0x23fe45909c9fcd47L,
        0xd17b8d91ad355118L, 0x1dd7b85f071efd1bL, 0x518bf93d445908ccL, 0x31a212d5f4cc8458L,
        0x62b7ffe46b91633eL, 0x079af812760a6633L, 0x596c95fed05028e7L, 0x6277ec5c12ea9677L,
    };

    @Test
    void testReferenceCode() {
        RandomAssert.assertEquals(EXPECTED_SEQUENCE, new XoRoShiRo128StarStar(SEED));
    }

    @Test
    void testConstructorWithZeroSeedIsNonFunctional() {
        RandomAssert.assertNextIntZeroOutput(new XoRoShiRo128StarStar(new long[SEED_SIZE]), 2 * SEED_SIZE);
    }

    @Test
    void testConstructorWithSingleBitSeedIsFunctional() {
        RandomAssert.assertLongArrayConstructorWithSingleBitSeedIsFunctional(XoRoShiRo128StarStar.class, SEED_SIZE);
    }

    @Test
    void testConstructorWithoutFullLengthSeed() {
        // Hit the case when the input seed is self-seeded when not full length
        RandomAssert.assertNextLongNonZeroOutput(new XoRoShiRo128StarStar(new long[] {SEED[0]}),
                SEED_SIZE, SEED_SIZE);
    }

    @Test
    void testElementConstructor() {
        final XoRoShiRo128StarStar rng1 = new XoRoShiRo128StarStar(SEED);
        final XoRoShiRo128StarStar rng2 = new XoRoShiRo128StarStar(SEED[0], SEED[1]);
        RandomAssert.assertNextLongEquals(SEED.length * 2, rng1, rng2);
    }

    @Test
    void testJump() {
        RandomAssert.assertJumpEquals(EXPECTED_SEQUENCE, EXPECTED_SEQUENCE_AFTER_JUMP, new XoRoShiRo128StarStar(SEED));
    }

    @Test
    void testLongJump() {
        RandomAssert.assertLongJumpEquals(EXPECTED_SEQUENCE, EXPECTED_SEQUENCE_AFTER_LONG_JUMP, new XoRoShiRo128StarStar(SEED));
    }
}
