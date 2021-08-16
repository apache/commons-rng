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

class XoShiRo256PlusPlusTest {
    /** The size of the array SEED. */
    private static final int SEED_SIZE = 4;

    /*
     * Data from running the executable compiled from the author's C code:
     *   http://xoshiro.di.unimi.it/xoshiro256plusplus.c
     */

    private static final long[] SEED = {
        0x012de1babb3c4104L, 0xa5a818b8fc5aa503L, 0xb124ea2b701f4993L, 0x18e0374933d8c782L,
    };

    private static final long[] EXPECTED_SEQUENCE = {
        0x83256c3efe494810L, 0xb6a32c7a2f427e87L, 0xea4a4faa5f25c89cL, 0xbc7eccdda31316ccL,
        0x13fd0f7150d989c6L, 0x547138cbae221c4bL, 0x9a2ed08e202ccdd4L, 0x71c76beffd5ffaf7L,
        0x4a82a53f9bf0e159L, 0x82b8fee551e226f6L, 0xc8a7cb2002fbabd2L, 0xe9fd4b8e8420b6caL,
        0xc4fee10ff73a4513L, 0xbeee1386595fd5bbL, 0x1ca9ea9f7af81173L, 0x1182e0f6515e7a82L,
        0x92f033c288c73349L, 0xf929b1c910f5d6fdL, 0xd74f135a22456c58L, 0x5db7c5f1bab2ba95L,
        0x35bd2e90555e90ffL, 0x82f57164f0a873e8L, 0xfe8c06ad1f4322a3L, 0xb5830910972042f4L,
        0x01b098fccc86e9a1L, 0x0a401cbff79d2968L, 0xaee758e14fc8b6c4L, 0x9b69b1669c551c7bL,
        0xc424e07de89d8003L, 0x2f54c2bc2413685cL, 0x18ed020132f7fd78L, 0x1296df883b21ddb7L,
        0x08ce35eb04245592L, 0x5b379c8c2a13dd1fL, 0xd5d72bff230d0038L, 0xe8fa9e75a5b11653L,
        0x01cda02ee361fc5dL, 0x458c1ba437db0e66L, 0x653d400afec2f1a5L, 0xce41edefbfc16d19L,
    };

    private static final long[] EXPECTED_SEQUENCE_AFTER_JUMP = {
        0x097817815ff7650eL, 0xe3a4a1ba31fe551cL, 0xd7edac1f857eb72eL, 0xeaf103ab604c1f33L,
        0x274bf134459253efL, 0xb2c037e04c354378L, 0x42f0d50f53d01cfaL, 0x170b7b0992291c41L,
        0xab2f51abd6144ce5L, 0xb1b29ec57f17c875L, 0xeee5b6846ef8731fL, 0xc93a9a89f885741bL,
        0xeb2c0ef80f38209aL, 0x8b11ae67b295bf5dL, 0x102ff04bf5f6a514L, 0x011491b4c8d59849L,
        0xd949e7c675fa13e0L, 0xf03f2b6bb95f63dfL, 0x0394a28de3853a88L, 0xc6bf4defddf657b3L,
        0x137edf82bc6da988L, 0xdd3da2a860fe7aaaL, 0xd562a0fceb57f6cfL, 0x7ad187b77b82ed41L,
        0xcee517096675814bL, 0x9cb90e61cd248c36L, 0x08e7d20e76147477L, 0xeb3be1e1fa580f29L,
        0xed5803c04e7f457dL, 0x57736d360cf9b389L, 0xf98478390475f655L, 0x077fe75d911a51d2L,
        0x0acd5c157adfb636L, 0x4e17872ccc3d84b0L, 0x9e31d8f0e2943738L, 0xaaee22c711ec0602L,
        0x48998bdfa462be6dL, 0xff066ab2d6250e56L, 0xe319e9c2ab1ad697L, 0x2b14627b78929b4aL,
    };

    private static final long[] EXPECTED_SEQUENCE_AFTER_LONG_JUMP = {
        0x0a6b9e34dbf9c7caL, 0x2214a78c97dc2187L, 0x9f44832a1e1ba9c6L, 0x49f5a7c5d1618518L,
        0xbca9ab8bed062466L, 0x21da06a34efaf84aL, 0xfc22b0fe3dda8c05L, 0x9b85b0b6cfc93dafL,
        0x252992459938a6c2L, 0x57e8cc4f4dd5316cL, 0x7942b8174c68b3f7L, 0x0687eb4b9902d56cL,
        0x76c25e3f67a134b1L, 0x9b4a908b5fc601e4L, 0x28019ff6b951f9baL, 0xb23ce81de5d3e05dL,
        0x44908a02c9cacabcL, 0xf527591a7870712dL, 0x4c8647740ac64a92L, 0xbf51a8298a198b8dL,
        0x2a899624fd875afdL, 0xbd9806ef5269c9bfL, 0xd75990e2678307c8L, 0x2984ae7ce86bcbdcL,
        0x948a1a74b031559cL, 0xf560ffba9a474a3cL, 0x8c82a4075190e936L, 0x1735d50cb3619419L,
        0x488716879f534f96L, 0x359aadcdd6a5d5adL, 0x314e31dac415bc2bL, 0x066cb902db3e6bb9L,
        0xd4d8c53a90d7480dL, 0x721670f7e471039fL, 0x0d7193e03567d61cL, 0xdb6d987c352b9fd0L,
        0x3c22102b526bff0cL, 0x24836afdeaab511eL, 0x9f098d3918b5fd3cL, 0x1f6c643f8a0b8c1dL,
    };

    @Test
    void testReferenceCode() {
        RandomAssert.assertEquals(EXPECTED_SEQUENCE, new XoShiRo256PlusPlus(SEED));
    }

    @Test
    void testConstructorWithZeroSeedIsNonFunctional() {
        RandomAssert.assertNextIntZeroOutput(new XoShiRo256PlusPlus(new long[SEED_SIZE]), 2 * SEED_SIZE);
    }

    @Test
    void testConstructorWithSingleBitSeedIsFunctional() {
        RandomAssert.assertLongArrayConstructorWithSingleBitSeedIsFunctional(XoShiRo256PlusPlus.class, SEED_SIZE);
    }

    @Test
    void testConstructorWithoutFullLengthSeed() {
        // Hit the case when the input seed is self-seeded when not full length
        RandomAssert.assertNextLongNonZeroOutput(new XoShiRo256PlusPlus(new long[] {SEED[0]}),
                SEED_SIZE, SEED_SIZE);
    }

    @Test
    void testElementConstructor() {
        final XoShiRo256PlusPlus rng1 = new XoShiRo256PlusPlus(SEED);
        final XoShiRo256PlusPlus rng2 = new XoShiRo256PlusPlus(SEED[0], SEED[1], SEED[2], SEED[3]);
        RandomAssert.assertNextLongEquals(SEED.length * 2, rng1, rng2);
    }

    @Test
    void testJump() {
        RandomAssert.assertJumpEquals(EXPECTED_SEQUENCE, EXPECTED_SEQUENCE_AFTER_JUMP, new XoShiRo256PlusPlus(SEED));
    }

    @Test
    void testLongJump() {
        RandomAssert.assertLongJumpEquals(EXPECTED_SEQUENCE, EXPECTED_SEQUENCE_AFTER_LONG_JUMP, new XoShiRo256PlusPlus(SEED));
    }
}
