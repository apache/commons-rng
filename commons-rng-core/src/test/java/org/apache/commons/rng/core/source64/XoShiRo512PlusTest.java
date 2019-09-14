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

public class XoShiRo512PlusTest {
    /** The size of the array SEED. */
    private static final int SEED_SIZE = 8;

    /*
     * Data from running the executable compiled from the author's C code:
     *   http://xoshiro.di.unimi.it/xoshiro512plus.c
     */

    private static final long[] SEED = {
        0x012de1babb3c4104L, 0xa5a818b8fc5aa503L, 0xb124ea2b701f4993L, 0x18e0374933d8c782L,
        0x2af8df668d68ad55L, 0x76e56f59daa06243L, 0xf58c016f0f01e30fL, 0x8eeafa41683dbbf4L,
    };

    private static final long[] EXPECTED_SEQUENCE = {
        0xb252cbe62b5b8a97L, 0xa4aaec677f60aaa2L, 0x1c8bd694b50fd00eL, 0x02753e0294233973L,
        0xbfec0be86d152e2dL, 0x5b9cd7265f320e98L, 0xf8ec45eccc703724L, 0x83fcbefa0359b3c1L,
        0xbd27fcdb7e79265dL, 0x88934227d8bf3cf0L, 0x99e1e79384f40371L, 0xe7e7fd0af2014912L,
        0xebdd19cbcd35745dL, 0x218994e1747243eeL, 0x80628718e5d310daL, 0x88ba1395debd989cL,
        0x72e025c0928c6f55L, 0x51400eaa050bbb0aL, 0x72542ad3e7fe29e9L, 0x3a3355b9dcb9c8b0L,
        0x2f6618f3df6126f4L, 0x34307608d886d40fL, 0x34f5a22e98fe3375L, 0x558f6560d08b9ec3L,
        0xae78928bcb041d6cL, 0xe7afe32a7caf4587L, 0x22dcfb5ca129d4bdL, 0x7c5a41864a6f2cf6L,
        0xbe1186add0fe46a7L, 0xd019fabc10dc96a5L, 0xafa642ef6837d342L, 0xdc4924811f62cf03L,
        0xdeb486ccebccf747L, 0xd827b16c9189f637L, 0xf1aab3c3c690a71dL, 0x6551214a7f04a2a5L,
        0x44b8edb239f2a141L, 0xb840cb37cfbeab59L, 0x0e9558adc0987ca2L, 0xc60442d5ff290606L,
    };

    private static final long[] EXPECTED_SEQUENCE_AFTER_JUMP = {
        0xda8c2f51c0a12fedL, 0xcc63a107350b8d8bL, 0x3730965c235cdc8bL, 0xdff53b55412bf8d4L,
        0xa75dca084c1a404eL, 0x395c5e03c3d51b84L, 0x0c57783cfb429d7bL, 0x402c0857310c0804L,
        0x5fb34f057266575aL, 0x196b0694db94ee83L, 0x31ce1b0c4d40a337L, 0x1f21143738a48e84L,
        0x4c00bde2f7d7184fL, 0xaad1564500e3b773L, 0xba2729da2d1bb5d7L, 0xcd1e33914dd13ac3L,
        0xf98130cc1b0053baL, 0x44eb6a48353a1e5eL, 0x490ae7ce04dfeda3L, 0xb553106ef217b297L,
        0xc073ae69eb507d0bL, 0xa056894e1deea79cL, 0xef69db4765dc1479L, 0x8575bf9f4686ab44L,
        0x35cbaf0c0fb38f4bL, 0xfa30396425d7f722L, 0x312f12282a479019L, 0x40d7ae6d2b24254eL,
        0xa370e089e50b34d6L, 0x5f364b5ae2a36a00L, 0x0a923136b57bb730L, 0x8e46536fce01229cL,
        0x3b2c38bc61116bb6L, 0x3f933d48f4a99c53L, 0x6e0b6ef2a27cd0ddL, 0xb53409e3aa42274aL,
        0xb0389318ac95388fL, 0x12c69799c7c33350L, 0x7e37dbd8210b480cL, 0xf3ab8bf173c83485L,
    };

    @Test
    public void testReferenceCode() {
        RandomAssert.assertEquals(EXPECTED_SEQUENCE, new XoShiRo512Plus(SEED));
    }

    @Test
    public void testConstructorWithZeroSeedIsNonFunctional() {
        RandomAssert.assertNextIntZeroOutput(new XoShiRo512Plus(new long[SEED_SIZE]), 2 * SEED_SIZE);
    }

    @Test
    public void testConstructorWithSingleBitSeedIsFunctional() {
        RandomAssert.assertLongArrayConstructorWithSingleBitSeedIsFunctional(XoShiRo512Plus.class, SEED_SIZE);
    }

    @Test
    public void testConstructorWithoutFullLengthSeed() {
        // Hit the case when the input seed is self-seeded when not full length
        RandomAssert.assertNextLongNonZeroOutput(new XoShiRo512Plus(new long[] {SEED[0]}),
                SEED_SIZE, SEED_SIZE);
    }

    @Test
    public void testElementConstructor() {
        final XoShiRo512Plus rng1 = new XoShiRo512Plus(SEED);
        final XoShiRo512Plus rng2 = new XoShiRo512Plus(SEED[0], SEED[1], SEED[2], SEED[3],
                                                       SEED[4], SEED[5], SEED[6], SEED[7]);
        RandomAssert.assertNextLongEquals(SEED.length * 2, rng1, rng2);
    }

    @Test
    public void testJump() {
        RandomAssert.assertJumpEquals(EXPECTED_SEQUENCE, EXPECTED_SEQUENCE_AFTER_JUMP, new XoShiRo512Plus(SEED));
    }
}
