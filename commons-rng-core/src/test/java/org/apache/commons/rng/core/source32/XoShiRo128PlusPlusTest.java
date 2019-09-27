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

public class XoShiRo128PlusPlusTest {
    /** The size of the array seed. */
    private static final int SEED_SIZE = 4;

    /*
     * Data from running the executable compiled from the author's C code:
     *   http://xoshiro.di.unimi.it/xoshiro128plusplus.c
     */

    private static final int[] SEED = {
        0x012de1ba, 0xa5a818b8, 0xb124ea2b, 0x18e03749,
    };

    private static final int[] EXPECTED_SEQUENCE = {
        0x083a6347, 0xaf13e949, 0xc170e7f6, 0x1fff4fb2,
        0x683f45ee, 0x0447edcf, 0x42e85ced, 0xaf636b74,
        0xb0087a5e, 0x75bf2669, 0xd1bce8bd, 0x421fc05e,
        0x1c6c405f, 0x14ddbffd, 0xaeacb705, 0x977ae584,
        0x2ac01aac, 0xc474ec71, 0xe0888022, 0xf94bc227,
        0x32775b57, 0x44142b05, 0x525f6d9b, 0xa2721e61,
        0x1bfe5c72, 0x17be23c2, 0x3231cc54, 0x8776866e,
        0x9ede2587, 0x0f7f144e, 0xb6f2ff9d, 0x1556365b,
        0x9e68aef3, 0x254010c3, 0x0b885bdd, 0x7c3f26bb,
        0xc8266de6, 0xcd2e6587, 0x0cbec249, 0xa69b37ba,
    };

    private static final int[] EXPECTED_SEQUENCE_AFTER_JUMP  = {
        0x4485d85f, 0x43f4d8a7, 0xe21ea064, 0x3eddd57d,
        0x44f6149a, 0xde7e1c16, 0xa7410410, 0x6360a4a9,
        0x34dab153, 0xfdf089b0, 0xa9b78551, 0xa2136cee,
        0x0ad2126f, 0x67a62b78, 0xa3e8c1fa, 0x19eed39b,
        0x83357624, 0xde015e70, 0xdc670b3b, 0x833dc245,
        0x4d644c84, 0x30e7ea9f, 0x9dd22362, 0x70978ced,
        0xf3d07dbb, 0xfdad08e5, 0x9118ebe0, 0x5dc55edf,
        0xcf9abe08, 0x7a822c3b, 0xa1115ecf, 0x9f8cc327,
        0x452c8954, 0xf920ef83, 0xcff75ece, 0x9622a70d,
        0x6202e501, 0x10ae0703, 0x8b7ee1be, 0xc72c1cf6,
    };

    private static final int[] EXPECTED_SEQUENCE_AFTER_LONG_JUMP  = {
        0x27906b5a, 0xe2ce9fb2, 0xd97f8c4f, 0x7609af7d,
        0x5b91ddd8, 0x4134b769, 0x47f505f2, 0xacc18832,
        0xeea7faf6, 0x50178ca9, 0xc15f4b36, 0xcbd206e6,
        0x4f2273cb, 0xebaabeef, 0x51e6d76f, 0xaf1fdde8,
        0x3cb5ced1, 0x04b42264, 0x2396256f, 0x9c0618ff,
        0x95ecbb0c, 0xc88c952c, 0x820664ab, 0x5d2e6153,
        0xb2003213, 0x71531ff6, 0x99d4bd53, 0xbd15fcc1,
        0x90ad002b, 0x3d37b45d, 0x500b49db, 0x6f81b35f,
        0x533f3bab, 0xe22a25fe, 0x114ca833, 0x4ab45586,
        0x077ca93d, 0xd5cf0025, 0xbe019f55, 0x8ecbe4a8,
    };

    @Test
    public void testReferenceCode() {
        RandomAssert.assertEquals(EXPECTED_SEQUENCE, new XoShiRo128PlusPlus(SEED));
    }

    @Test
    public void testConstructorWithZeroSeedIsNonFunctional() {
        RandomAssert.assertNextIntZeroOutput(new XoShiRo128PlusPlus(new int[SEED_SIZE]), 2 * SEED_SIZE);
    }

    @Test
    public void testConstructorWithSingleBitSeedIsFunctional() {
        RandomAssert.assertIntArrayConstructorWithSingleBitSeedIsFunctional(XoShiRo128PlusPlus.class, SEED_SIZE);
    }

    @Test
    public void testConstructorWithoutFullLengthSeed() {
        // Hit the case when the input seed is self-seeded when not full length
        RandomAssert.assertNextLongNonZeroOutput(new XoShiRo128PlusPlus(new int[] {SEED[0]}),
                SEED_SIZE, SEED_SIZE);
    }

    @Test
    public void testElementConstructor() {
        final XoShiRo128PlusPlus rng1 = new XoShiRo128PlusPlus(SEED);
        final XoShiRo128PlusPlus rng2 = new XoShiRo128PlusPlus(SEED[0], SEED[1], SEED[2], SEED[3]);
        RandomAssert.assertNextIntEquals(SEED.length * 2, rng1, rng2);
    }

    @Test
    public void testJump() {
        RandomAssert.assertJumpEquals(EXPECTED_SEQUENCE, EXPECTED_SEQUENCE_AFTER_JUMP, new XoShiRo128PlusPlus(SEED));
    }

    @Test
    public void testLongJump() {
        RandomAssert.assertLongJumpEquals(EXPECTED_SEQUENCE, EXPECTED_SEQUENCE_AFTER_LONG_JUMP, new XoShiRo128PlusPlus(SEED));
    }
}
