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

    @Test
    public void testReferenceCode() {
        /*
         * Data from running the executable compiled from the author's C code:
         *   http://xoshiro.di.unimi.it/xoshiro128plus.c
         */
        final int[] seed = {
            0x012de1ba, 0xa5a818b8, 0xb124ea2b, 0x18e03749,
        };

        final int[] expectedSequence = {
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

        RandomAssert.assertEquals(expectedSequence, new XoShiRo128Plus(seed));
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
        new XoShiRo128Plus(new int[] { 0x012de1ba });
    }

    @Test
    public void testElementConstructor() {
        final int[] seed = {
            0x012de1ba, 0xa5a818b8, 0xb124ea2b, 0x18e03749,
        };
        final XoShiRo128Plus rng1 = new XoShiRo128Plus(seed);
        final XoShiRo128Plus rng2 = new XoShiRo128Plus(seed[0], seed[1], seed[2], seed[3]);
        RandomAssert.assertNextIntEquals(seed.length * 2, rng1, rng2);
    }
}
