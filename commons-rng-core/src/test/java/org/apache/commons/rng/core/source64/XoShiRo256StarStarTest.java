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

public class XoShiRo256StarStarTest {
    /** The size of the array SEED. */
    private static final int SEED_SIZE = 4;

    /*
     * Data from running the executable compiled from the author's C code:
     *   http://xoshiro.di.unimi.it/xoshiro256starstar.c
     */

    private static final long[] SEED = {
        0x012de1babb3c4104L, 0xa5a818b8fc5aa503L, 0xb124ea2b701f4993L, 0x18e0374933d8c782L,
    };

    private static final long[] EXPECTED_SEQUENCE = {
        0x462c422df780c48eL, 0xa82f1f6031c183e6L, 0x8a113820e8d2ca8dL, 0x1ac7023a26534958L,
        0xac8e41d0101e109cL, 0x46e34bc13edd63c4L, 0x3a26776adcd665c3L, 0x9ac6c9bea8fc518cL,
        0x1cef0aa07cc738c4L, 0x5136a5f070244b1dL, 0x12e2e12edee691ffL, 0x28942b20799b71b4L,
        0xbe2d5c4267af2469L, 0x9dbec53728b2b9b7L, 0x893cf86611b14a96L, 0x712c226c79f066d6L,
        0x1a8a11ef81d2ac60L, 0x28171739ef8f2f46L, 0x073baa93525f8b1dL, 0xa73c7f3cb93df678L,
        0xae5633ab977a3531L, 0x25314041ba2d047eL, 0x31e6819dea142672L, 0x9479fa694f4c2965L,
        0xde5b771a968472b7L, 0xf0501965d9eeb4a3L, 0xef25a2a8ec90b911L, 0x1f58f71a75392659L,
        0x32d9547188781f3cL, 0x2d13b036ccf65bc0L, 0x289f9cc038dd952fL, 0x6ae2d5231e50824aL,
        0x75651acfb42ab170L, 0x7369aeb4f10056cfL, 0x0297ed632a97cf75L, 0x19f534c778015b72L,
        0x5d1d111c5ff182a8L, 0x861cdfe8e8014b96L, 0x07c6071e08112c83L, 0x15601582dcf4e4feL,
    };

    private static final long[] EXPECTED_SEQUENCE_AFTER_JUMP = {
        0xb416bd07926c6735L, 0x2f91faf5c6c9f79aL, 0x538d25a148318bc1L, 0x79ffea0eb76500e6L,
        0x7b74b0513602a5e1L, 0xdc114ef0bb881ac5L, 0xa0845293613f458bL, 0xb650a96e30f09819L,
        0xbd2aeb7eb2ac1a6aL, 0x724e2d39d21b00baL, 0x4b38be1deb8553caL, 0xd83f40c399601212L,
        0x97aba644588c210aL, 0x5caa9f64a83047b6L, 0x36ade013e70660abL, 0xf1bf69a51790aadaL,
        0x4c25aad6aac062ddL, 0x072e1ab91c2ec7c1L, 0x9343a09f5f5eec61L, 0xdcdd0baaaa38bce7L,
        0x0c0ea0bcd389f16aL, 0x0765633fee36d533L, 0xfba7f80666c43a76L, 0x896323052d851b9cL,
        0x60bdd013e4a0a3f3L, 0x244be4b11b49ca4cL, 0x1513dcbe57a23089L, 0x809e9476dd32f1baL,
        0x09e914013550ced8L, 0x68873250d4a070b9L, 0x0c7709d63a915660L, 0x97014b396d121b71L,
        0xc50b646fe0f40c95L, 0x4edff941823be25cL, 0x5310e5528d79fa55L, 0xb7353ccef26265e9L,
        0x3346bda5d2ac2d7dL, 0xbeab7520abd736f1L, 0x7195e9c9f28eac6aL, 0x64d959048b71d87bL,
    };

    @Test
    public void testReferenceCode() {
        RandomAssert.assertEquals(EXPECTED_SEQUENCE, new XoShiRo256StarStar(SEED));
    }

    @Test
    public void testConstructorWithZeroSeedIsNonFunctional() {
        RandomAssert.assertNextIntZeroOutput(new XoShiRo256StarStar(new long[SEED_SIZE]), 2 * SEED_SIZE);
    }

    @Test
    public void testConstructorWithSingleBitSeedIsFunctional() {
        RandomAssert.assertLongArrayConstructorWithSingleBitSeedIsFunctional(XoShiRo256StarStar.class, SEED_SIZE);
    }

    @Test
    public void testConstructorWithoutFullLengthSeed() {
        // Hit the case when the input seed is self-seeded when not full length
        new XoShiRo256StarStar(new long[] {SEED[0]});
    }

    @Test
    public void testElementConstructor() {
        final XoShiRo256StarStar rng1 = new XoShiRo256StarStar(SEED);
        final XoShiRo256StarStar rng2 = new XoShiRo256StarStar(SEED[0], SEED[1], SEED[2], SEED[3]);
        RandomAssert.assertNextLongEquals(SEED.length * 2, rng1, rng2);
    }

    @Test
    public void testJump() {
        RandomAssert.assertJumpEquals(EXPECTED_SEQUENCE, EXPECTED_SEQUENCE_AFTER_JUMP, new XoShiRo256StarStar(SEED));
    }

    @Test
    public void testJumpUsingState() {
        RandomAssert.assertJumpUsingState(new XoShiRo256StarStar(SEED));
    }
}
