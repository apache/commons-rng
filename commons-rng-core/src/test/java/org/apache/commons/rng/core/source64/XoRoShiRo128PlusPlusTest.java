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
import org.junit.jupiter.api.Assertions;

public class XoRoShiRo128PlusPlusTest {
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
        0xf61550e8874b8eafL, 0x125015fce911e8f6L, 0xff0e6030e39af1a4L, 0xd5738fc2a502673bL,
        0xef48cdcbefd84325L, 0xb60462c014133da1L, 0xa62c6d8b9f87cd81L, 0x52fd609a347198ebL,
        0x3c717475e803bf09L, 0x1b6e66b21504a677L, 0x528f64243db486f4L, 0x3676015c33fbf0faL,
        0x3e05f2ea0216a127L, 0x373343bb4159fa59L, 0xc375c54ebe2f9097L, 0x52d85b22744e0574L,
        0x055dd7e34e687524L, 0xb749afc4bc4ed98aL, 0x31b972f93d117746L, 0xc0e13329779abc15L,
        0xee52ec4b4ddc0091L, 0xc756c7dd1d6796d6L, 0x3ce47f42e211c63eL, 0xa635aa7ce5d06101L,
        0xe8054178cbb492c1L, 0x3cc3ad122e7da816L, 0x0cbad73cdacab8fdL, 0x20aa1cbc64638b31L,
        0x3bce572cfe3bc776L, 0xcc81e41637090cd8L, 0x69cc93e599f51181L, 0x2d5c9a4e509f984dL,
        0xf4f3bf08ff627f92L, 0x3430e0a0e8670235L, 0x75a856b68968f466L, 0xdee1dbbb374913d7L,
        0x9736e33202fbe05bL, 0x4bea0cc1151902a4L, 0x9fe7fd9d8de47d13L, 0xf011332584a1c7abL,
    };

    private static final long[] EXPECTED_SEQUENCE_AFTER_JUMP = {
        0xd0247ffd625d34bbL, 0x5247d11117b07db9L, 0xb9aec11eefe737e1L, 0xa88d6ac4c2d7f480L,
        0x876e38fc5bcd7f89L, 0x14aece3dfd7a6ce9L, 0x6a7d54fb077757c9L, 0x4b295874fd6e600aL,
        0x7efc54a81a770ffbL, 0xba3e5563f7fe8921L, 0x2239e13c7e891f63L, 0xb55fe548136c7487L,
        0x9c25f74da0d8ee56L, 0xf0394d67496e5365L, 0x66825f2ecdf8dd31L, 0x4556f285a8ddf1e5L,
        0xe36263a4651b8713L, 0xe71d97594e1fcbf4L, 0x85e29bc51ade5500L, 0x29072f022acf4424L,
        0x5a6ad132ebe0e3acL, 0x27636b07bd1d0589L, 0x4bea854864d0b254L, 0x9067b3ebd340445cL,
        0x8efb0e541cf4748dL, 0x77ac4cf8b2c52130L, 0x3e6d153c3fd9d216L, 0x133f5b6cb6113ae4L,
        0xb412087ed3fc5f2bL, 0xaf452866aed4f35fL, 0x18f5416acb6e5e7cL, 0x98efa7dd2825dd3eL,
        0x4741ee086b0c21b8L, 0x83bee0e266ace631L, 0x1365249d567dd250L, 0xe249e3e3531ce4e5L,
        0xc731814bfb74fb0dL, 0x7e787fed6a4867a6L, 0xd5dcb4243d5e99b5L, 0x814f448f602da27aL,
    };

    private static final long[] EXPECTED_SEQUENCE_AFTER_LONG_JUMP = {
        0x6df051506844df28L, 0x82b35c7bdbfaf5d1L, 0x4777df68763da907L, 0xcce5e8b9296d60a4L,
        0xbc6c9ad115aeca29L, 0xc07d256e39f80780L, 0xf8ff8370f70e7a3cL, 0x7febc84171245a56L,
        0x81d2666d50e5025aL, 0x6bb05ddc3eba6542L, 0x9ee3a1141dec8267L, 0x699d68881fa2a2b3L,
        0xd4a22412b7a931f1L, 0x806e870d51a5de43L, 0xf3c9392aca6b5165L, 0x0336ef0f76d4bbe3L,
        0xa64aee05112a7bafL, 0x00d4e91ecda4e9efL, 0x72d6f199a63bae80L, 0x3e3fe49a3e9d3c16L,
        0x8d01430dcfbbaf88L, 0x7c251ea9025494beL, 0x6f9d95802f2b73d4L, 0x77940cc33cb6d44bL,
        0xa2923dd106c5a575L, 0x7a40e2608ecc4156L, 0xabab85f8f1f0ac30L, 0x7d46384e68e42393L,
        0xe51135892364edabL, 0x30a6d989c2e1ce8cL, 0xe3a5ca50ef05e784L, 0xb166f894ee83ee6cL,
        0x8034b69518bda95aL, 0xbf47c48eb7a25d81L, 0x6661fd8cc91ba9cfL, 0x0bbc9b1b8dd65082L,
        0xd15af2be53778c09L, 0x5167f22075802d3cL, 0x76c2c76784ba1941L, 0x9e79d12a7bb75038L,
    };

    @Test
    public void testReferenceCode() {
        RandomAssert.assertEquals(EXPECTED_SEQUENCE, new XoRoShiRo128PlusPlus(SEED));
    }

    @Test
    public void testConstructorWithZeroSeedIsNonFunctional() {
        RandomAssert.assertNextIntZeroOutput(new XoRoShiRo128PlusPlus(new long[SEED_SIZE]), 2 * SEED_SIZE);
    }

    @Test
    public void testConstructorWithSingleBitSeedIsFunctional() {
        RandomAssert.assertLongArrayConstructorWithSingleBitSeedIsFunctional(XoRoShiRo128PlusPlus.class, SEED_SIZE);
    }

    @Test
    public void testConstructorWithoutFullLengthSeed() {
        // Hit the case when the input seed is self-seeded when not full length
        RandomAssert.assertNextLongNonZeroOutput(new XoRoShiRo128PlusPlus(new long[] {SEED[0]}),
                SEED_SIZE, SEED_SIZE);
    }

    @Test
    public void testElementConstructor() {
        final XoRoShiRo128PlusPlus rng1 = new XoRoShiRo128PlusPlus(SEED);
        final XoRoShiRo128PlusPlus rng2 = new XoRoShiRo128PlusPlus(SEED[0], SEED[1]);
        RandomAssert.assertNextLongEquals(SEED.length * 2, rng1, rng2);
    }

    @Test
    public void testJump() {
        RandomAssert.assertJumpEquals(EXPECTED_SEQUENCE, EXPECTED_SEQUENCE_AFTER_JUMP, new XoRoShiRo128PlusPlus(SEED));
    }

    @Test
    public void testLongJump() {
        RandomAssert.assertLongJumpEquals(EXPECTED_SEQUENCE, EXPECTED_SEQUENCE_AFTER_LONG_JUMP, new XoRoShiRo128PlusPlus(SEED));
    }

    /**
     * This PlusPlus algorithm uses a different state update step. It overrides next() directly
     * and the abstract nextOutput() method should not be used. This test checks the method
     * throws an exception if used.
     */
    @Test
    public void testNextOutputThrows() {
        Assertions.assertThrows(UnsupportedOperationException.class, () -> new XoRoShiRo128PlusPlus(SEED).nextOutput());
    }
}
