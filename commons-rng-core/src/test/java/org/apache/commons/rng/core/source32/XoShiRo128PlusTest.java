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

public class XoShiRo128PlusTest {
    /** The size of the array seed. */
    private static final int SEED_SIZE = 4;

    /*
     * Data from running the executable compiled from the author's C code:
     *   http://xoshiro.di.unimi.it/xoshiro128plus.c
     */

    private static final int[] SEED = {
        0x012de1ba, 0xa5a818b8, 0xb124ea2b, 0x18e03749,
    };

    private static final int[] EXPECTED_SEQUENCE = {
        0x1a0e1903, 0xfde55c35, 0xddb16b2e, 0xab949ac5,
        0xb5519fea, 0xc6a97473, 0x1f0403d9, 0x1bb46995,
        0x79c99a12, 0xe447ebce, 0xa8c31d78, 0x54d8bbe3,
        0x4984a039, 0xb411e932, 0x9c1f2c5e, 0x5f53c469,
        0x7f333552, 0xb368c7a1, 0xa57b8e66, 0xb29a9444,
        0x5c389bfa, 0x8e7d3758, 0xfe17a1bb, 0xcd0aad57,
        0xde83c4bb, 0x1402339d, 0xb557a080, 0x4f828bc9,
        0xde14892d, 0xbba8eaed, 0xab62ebbb, 0x4ad959a4,
        0x3c6ee9c7, 0x4f6a6fd3, 0xd5785eed, 0x1a0227d1,
        0x81314acb, 0xfabdfb97, 0x7e1b7e90, 0x57544e23,
    };

    private static final int[] EXPECTED_SEQUENCE_AFTER_JUMP = {
        0x65ddc942, 0x7e7c4d6b, 0x6745a785, 0x40897788,
        0xfb60ce92, 0x121f2ee0, 0xd000bae8, 0x52b3ebfc,
        0x62fc3720, 0xf880f092, 0x7753c1ab, 0x1e76a627,
        0xe5de31e8, 0xc7b1503f, 0xa5557a66, 0x37b2b2cd,
        0x656dde58, 0xdd5f1b93, 0xba61298b, 0xbd5d1ce2,
        0xea4a5a73, 0x0f10981d, 0xc207a68c, 0x1897adca,
        0x4d729b07, 0xf0115ee0, 0x953d9e4b, 0x3608e61c,
        0x0c14c065, 0xf2ed7579, 0xcd96ef9b, 0xdb62d117,
        0x844e4713, 0x763a8a76, 0x9ad37470, 0x211e4883,
        0xc8682b75, 0xb1831941, 0xf0c50a84, 0x7321dc33,
    };

    @Test
    public void testReferenceCode() {
        RandomAssert.assertEquals(EXPECTED_SEQUENCE, new XoShiRo128Plus(SEED));
    }

    @Test
    public void testConstructorWithZeroSeedIsNonFunctional() {
        RandomAssert.assertNextIntZeroOutput(new XoShiRo128Plus(new int[SEED_SIZE]), 2 * SEED_SIZE);
    }

    @Test
    public void testConstructorWithSingleBitSeedIsFunctional() {
        RandomAssert.assertIntArrayConstructorWithSingleBitSeedIsFunctional(XoShiRo128Plus.class, SEED_SIZE);
    }

    @Test
    public void testConstructorWithoutFullLengthSeed() {
        // Hit the case when the input seed is self-seeded when not full length
        RandomAssert.assertNextLongNonZeroOutput(new XoShiRo128Plus(new int[] {SEED[0]}),
                SEED_SIZE, SEED_SIZE);
    }

    @Test
    public void testElementConstructor() {
        final XoShiRo128Plus rng1 = new XoShiRo128Plus(SEED);
        final XoShiRo128Plus rng2 = new XoShiRo128Plus(SEED[0], SEED[1], SEED[2], SEED[3]);
        RandomAssert.assertNextIntEquals(SEED.length * 2, rng1, rng2);
    }

    @Test
    public void testJump() {
        RandomAssert.assertJumpEquals(EXPECTED_SEQUENCE, EXPECTED_SEQUENCE_AFTER_JUMP, new XoShiRo128Plus(SEED));
    }
}
