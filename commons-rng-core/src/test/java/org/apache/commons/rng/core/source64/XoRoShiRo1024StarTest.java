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

class XoRoShiRo1024StarTest {
    /** The size of the array SEED. */
    private static final int SEED_SIZE = 16;

    /*
     * Data from running the executable compiled from the author's C code:
     *   http://xoshiro.di.unimi.it/xoroshiro1024star.c
     */

    private static final long[] SEED = {
        0x012de1babb3c4104L, 0xa5a818b8fc5aa503L, 0xb124ea2b701f4993L, 0x18e0374933d8c782L,
        0x2af8df668d68ad55L, 0x76e56f59daa06243L, 0xf58c016f0f01e30fL, 0x8eeafa41683dbbf4L,
        0x7bf121347c06677fL, 0x4fd0c88d25db5ccbL, 0x99af3be9ebe0a272L, 0x94f2b33b74d0bdcbL,
        0x24b5d9d7a00a3140L, 0x79d983d781a34a3cL, 0x582e4a84d595f5ecL, 0x7316fe8b0f606d20L,
    };

    private static final long[] EXPECTED_SEQUENCE = {
        0x25420431d285b339L, 0x9d50004eab73a9e9L, 0xc62fb621034dc6a6L, 0x8e4f7ec7784c094fL,
        0xfb233a178dddbef9L, 0x538a598a1a751e1dL, 0xb2262ac30427231cL, 0xe782bbf13a51326dL,
        0x503706f497e83711L, 0x8b44a684d34f4676L, 0x10228730591a6a11L, 0x4d21c526cd1ca7c0L,
        0x4497a82cf06b9274L, 0x20324535a7779084L, 0x20f683e744439960L, 0xeca1ed99f04a8802L,
        0xb0710447ff1eab59L, 0x725be21e77ae9cedL, 0x0c53e906e3c75f2dL, 0xee3f9b042ee640cbL,
        0xddeee0b06f807837L, 0xe7a56a2dab4ff707L, 0xc08a9562b73b0e9eL, 0x424e42a62f21f0e0L,
        0x93d9ef07036ace29L, 0x3389f1d557fe67c5L, 0xb6bcecf856c408d0L, 0xed4e342feea1b9c1L,
        0x8444c6a9d792ab55L, 0x0b75319a836a81b5L, 0x846082b4ea38f5bcL, 0xa9e2b47baddca313L,
        0x3f1a966bb7668740L, 0xf556b1a28a741e1dL, 0xfc99602a418d56b3L, 0x50e51136f1ff265bL,
        0x0b1534b756ba6913L, 0x902b3601421c7827L, 0x88a33e48fbabe7f0L, 0xc3fdf390206a509eL,
    };

    private static final long[] EXPECTED_SEQUENCE_AFTER_JUMP = {
        0x3c9956e04f3540b4L, 0x8afdc8db489de8deL, 0xc1b843dfaf80f74fL, 0x8af7f996ca0b43acL,
        0xe9678ee162f4a196L, 0x64fe21412ecd8f67L, 0xa37fc756b9a58e30L, 0x54e4943ea2256f1dL,
        0x462b64934d76c9a9L, 0xb05141b0175a2e75L, 0x6ce6faee43a1f5a1L, 0xaaba2a0d982ddf6fL,
        0x2616ff8ac72434bcL, 0xd17792b5478719a6L, 0x5c5e252ff46288a4L, 0x37d60a9cea333634L,
        0x1d54cf05358aa8fbL, 0x1b789cbbdc91b2daL, 0xc146695af4e7b3f4L, 0x9e18b93eea2800fcL,
        0x8f24b7b08a478060L, 0xf9911ea6c3aee2cdL, 0x0b7c0e4de51be5aeL, 0x5545c9e12996bd73L,
        0x06792d509e39c6bfL, 0x35c552ab8ff34bb0L, 0xb82ba8f4110f1004L, 0xee50f84093a0dc87L,
        0x34428d22a60e61d3L, 0x2e873e2ee49252feL, 0x60b4c7c15833c7c1L, 0xe509268f45253ee3L,
        0xff9324851f9edc6fL, 0xb00abd7d8b3bfaebL, 0x5c33988f7599de27L, 0xd131c3d1d58e79f0L,
        0xbdd56afa4e7d3fb1L, 0x46825a94f62679b6L, 0x14e69a0ae63aa3d1L, 0x7c3bbbc3ab5dcf6bL,
    };

    private static final long[] EXPECTED_SEQUENCE_AFTER_LONG_JUMP = {
        0x844905489c673003L, 0x346119a9080539bbL, 0xd6daab0b3d76dbfcL, 0x779de94809667d8fL,
        0xef431e757eb6dcdcL, 0x4afca5425c9ce0d6L, 0x2d3346e5d4e5b904L, 0xdf8c0c75d449c9b7L,
        0xce8901ef4341c557L, 0x4d9c8ce74919d511L, 0xa0c9b66bf8151b0eL, 0xf27e867e82ba9960L,
        0x7fbe6396e871808bL, 0x05b986f7cd526c2aL, 0x69486c6bd10e5528L, 0xec5b81a2983edceeL,
        0x386c3e25b2c97169L, 0xb93097d01f6f2dc3L, 0x6d1de2eca99e0075L, 0x4fa5bb372448ef65L,
        0x59a7f43fb5667effL, 0x473aef023223d925L, 0x7960ff90dd9faf6aL, 0xd6a52f1010fc621dL,
        0xae0475c0252706feL, 0x04b18a9676b43b1fL, 0x23614d123c1d7981L, 0x90a3a61d347af01dL,
        0xa8bb5a73ed60fa71L, 0x9bf4aebbcef5d4dcL, 0xcb055aa09030c14eL, 0xadb90edea65b9f10L,
        0x18c39d4ad7d7b42bL, 0x77d582b2b6cd5b63L, 0xb1981ca7f4940a72L, 0x3831442ccb482f24L,
        0x290fa8a2be437eb2L, 0xcc6e9e6945d4d357L, 0xb056b5998149a15bL, 0x14d11b85ca684993L,
    };

    @Test
    void testReferenceCode() {
        RandomAssert.assertEquals(EXPECTED_SEQUENCE, new XoRoShiRo1024Star(SEED));
    }

    @Test
    void testConstructorWithZeroSeedIsNonFunctional() {
        RandomAssert.assertNextIntZeroOutput(new XoRoShiRo1024Star(new long[SEED_SIZE]), 2 * SEED_SIZE);
    }

    @Test
    void testConstructorWithSingleBitSeedIsFunctional() {
        RandomAssert.assertLongArrayConstructorWithSingleBitSeedIsFunctional(XoRoShiRo1024Star.class, SEED_SIZE);
    }

    @Test
    void testConstructorWithoutFullLengthSeed() {
        // Hit the case when the input seed is self-seeded when not full length
        RandomAssert.assertNextLongNonZeroOutput(new XoRoShiRo1024Star(new long[] {SEED[0]}),
                SEED_SIZE, SEED_SIZE);
    }

    @Test
    void testJump() {
        RandomAssert.assertJumpEquals(EXPECTED_SEQUENCE, EXPECTED_SEQUENCE_AFTER_JUMP, new XoRoShiRo1024Star(SEED));
    }

    @Test
    void testLongJump() {
        RandomAssert.assertLongJumpEquals(EXPECTED_SEQUENCE, EXPECTED_SEQUENCE_AFTER_LONG_JUMP, new XoRoShiRo1024Star(SEED));
    }
}
