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

public class XoRoShiRo128PlusTest {
    /** The size of the array SEED. */
    private static final int SEED_SIZE = 2;

    /*
     * Data from running the executable compiled from the author's C code:
     *   http://xoshiro.di.unimi.it/xoroshiro128plus.c
     */

    private static final long[] SEED = {
        0x012de1babb3c4104L, 0xa5a818b8fc5aa503L,
    };

    private static final long[] EXPECTED_SEQUENCE = {
        0xa6d5fa73b796e607L, 0xd419031a381fea2eL, 0x28938b88b4972f52L, 0x032793a0d51c1a27L,
        0x50001cd69cc5b006L, 0x44bbf571167cb7f0L, 0x172f6a2f093b2befL, 0xe642c831f1e4f7bfL,
        0xcec4e4b5d448032aL, 0xc0164992807cbd59L, 0xb96ff06c68515410L, 0x5288e0312a0aae72L,
        0x79a891c387d3be2eL, 0x6c52f6f710db553bL, 0x2ce6f6b1946862b3L, 0x87eb1e1b24b47f11L,
        0x9f7c3511c5f23bcfL, 0x3254897533dcd1abL, 0x89d56ad217fbd1adL, 0x70f6b269f815f6e6L,
        0xe8ee60efadfdb8c4L, 0x09286db69fdd232bL, 0xf440882651fc19e8L, 0x6356fea018cc26cdL,
        0xf692282b43fcb0c2L, 0xef3f084929119babL, 0x355efbf5bedeb114L, 0x6cf5089c2acc96ddL,
        0x819c19e480f0bfd1L, 0x414d12ff4082e261L, 0xc9a33a52545dd374L, 0x4675247e6fe89b3cL,
        0x069f2e55cea155baL, 0x1e8d1dcf349746b8L, 0xdf32e487bdd74523L, 0xa544710cae2ad7cdL,
        0xf5ac505e74fe049dL, 0xf039e289da4cdf7eL, 0x0a6fbebe9122529cL, 0x880c51e0915031a3L,
    };

    private static final long[] EXPECTED_SEQUENCE_AFTER_JUMP = {
        0xb715ad9cb572030eL, 0x10634f817a8f69b1L, 0xe871b367a8f9c567L, 0x3096f4ceb23198cdL,
        0xf5b09c8734d26da9L, 0x58ba83f779a2549cL, 0xb6c54c8ea9fc672bL, 0x87bb9766ff20834dL,
        0x2686d4a93b9bb8a7L, 0xababdb798931fbfbL, 0xc9ce83b35259eb39L, 0xe2314c9488d44131L,
        0x5841497fb6fe3a62L, 0x00ecd78f2eba1e81L, 0x4aed6d184b3ada35L, 0xb33681cb7e39c9d0L,
        0x911498e07cb3d3ceL, 0xea92a7dfb98aa971L, 0x5db49f2f3f22321cL, 0x4982bae495d875f4L,
        0xa436167ccfb3a982L, 0x646b0fe680176bb2L, 0x8312610f7e93a41fL, 0xa67a4d13d12cb264L,
        0xf22330689f003fa8L, 0xfa0d7f3712db37beL, 0x409b34496c4a847cL, 0x9ac1b246a47e2a17L,
        0x6006078c0c743c74L, 0x457ef921811029ecL, 0xd6d9f58851583575L, 0xabbdd4ac3c49239bL,
        0xc07444095ef29743L, 0x75a2eb7c74f95d6eL, 0xd0ead6726a91ada9L, 0x9749e96908129418L,
        0x83ae5b12eb343545L, 0x10828fde12decab5L, 0x6952d51f130505b7L, 0x9b6bd81fbe064f54L,
    };

    private static final long[] EXPECTED_SEQUENCE_AFTER_LONG_JUMP = {
        0xf569a89f3ee6fa3dL, 0x3c61867cbcc208a8L, 0x95a83b710aa1a57fL, 0xed5c658383559407L,
        0xbb70b6959a82f3b0L, 0x31eab244213fe7beL, 0xe14bb9d50b6b026fL, 0x80716d04b81d5aaaL,
        0x5451ede574559e11L, 0x1aa1d87fdfb78d52L, 0x38e4a2c33f97870fL, 0xf88a98d8376a95b5L,
        0x5b88ec173f131549L, 0x642af091aab9643aL, 0x92edb9171a703919L, 0xf65bd2ac6b1efd62L,
        0xe10b1da6e5b2efafL, 0xf2abec22c6c4170dL, 0xa5b853b7dee402c1L, 0xe45d2ec5e488cac3L,
        0x64f126a2065224d6L, 0x5e4f64edde60e852L, 0xc8dc8819696ef333L, 0x1a9acfaa4a10f22bL,
        0xd75be7f8487d1c52L, 0x7759ec0ea4b8e698L, 0xc36b934a78c463e2L, 0x2c7257c4228f7948L,
        0x0651b8c869027343L, 0xe66b122fec5ca8a1L, 0xe3f8ad0bcf74642dL, 0x08f46e481eec937aL,
        0xccd1c4419af68187L, 0x93396af913995faeL, 0x95ee2988b39a3534L, 0x7da239ae0dd79948L,
        0xa0fdcf90f2e3ab22L, 0xb2e20b2ce7a83f7aL, 0xd1346542947f8b0dL, 0xa95894552bbb2460L,
    };

    @Test
    public void testReferenceCode() {
        RandomAssert.assertEquals(EXPECTED_SEQUENCE, new XoRoShiRo128Plus(SEED));
    }

    @Test
    public void testConstructorWithZeroSeedIsNonFunctional() {
        RandomAssert.assertNextIntZeroOutput(new XoRoShiRo128Plus(new long[SEED_SIZE]), 2 * SEED_SIZE);
    }

    @Test
    public void testConstructorWithSingleBitSeedIsFunctional() {
        RandomAssert.assertLongArrayConstructorWithSingleBitSeedIsFunctional(XoRoShiRo128Plus.class, SEED_SIZE);
    }

    @Test
    public void testConstructorWithoutFullLengthSeed() {
        // Hit the case when the input seed is self-seeded when not full length
        RandomAssert.assertNextLongNonZeroOutput(new XoRoShiRo128Plus(new long[] {SEED[0]}),
                SEED_SIZE, SEED_SIZE);
    }

    @Test
    public void testElementConstructor() {
        final XoRoShiRo128Plus rng1 = new XoRoShiRo128Plus(SEED);
        final XoRoShiRo128Plus rng2 = new XoRoShiRo128Plus(SEED[0], SEED[1]);
        RandomAssert.assertNextLongEquals(SEED.length * 2, rng1, rng2);
    }

    @Test
    public void testJump() {
        RandomAssert.assertJumpEquals(EXPECTED_SEQUENCE, EXPECTED_SEQUENCE_AFTER_JUMP, new XoRoShiRo128Plus(SEED));
    }

    @Test
    public void testLongJump() {
        RandomAssert.assertLongJumpEquals(EXPECTED_SEQUENCE, EXPECTED_SEQUENCE_AFTER_LONG_JUMP, new XoRoShiRo128Plus(SEED));
    }
}
