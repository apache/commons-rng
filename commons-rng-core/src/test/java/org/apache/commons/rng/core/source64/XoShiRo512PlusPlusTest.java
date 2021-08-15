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

public class XoShiRo512PlusPlusTest {
    /** The size of the array SEED. */
    private static final int SEED_SIZE = 8;

    /*
     * Data from running the executable compiled from the author's C code:
     *   http://xoshiro.di.unimi.it/xoshiro512plusplus.c
     */

    private static final long[] SEED = {
        0x012de1babb3c4104L, 0xa5a818b8fc5aa503L, 0xb124ea2b701f4993L, 0x18e0374933d8c782L,
        0x2af8df668d68ad55L, 0x76e56f59daa06243L, 0xf58c016f0f01e30fL, 0x8eeafa41683dbbf4L,
    };

    private static final long[] EXPECTED_SEQUENCE = {
        0x48f140e2854eae38L, 0x88d80a53206851ecL, 0xf1d255641f3ae3b3L, 0x0f4f285abdd594d8L,
        0x143218246037a643L, 0xee38ae788815d8e2L, 0xb034afe60cf3bf1eL, 0x6ebe40961af2ac91L,
        0x5dafb9e849412600L, 0xbf27348ef757e243L, 0x469345ef2d21ee91L, 0x4f2f7b8e0ab1c23cL,
        0xfb6f8d5eeaba7c82L, 0x0d95bd0852a4ae70L, 0xec95e0448516b491L, 0xa6c62460124a0036L,
        0xc206ef6cfb672cd8L, 0xa8339b4fdf8111a5L, 0xa267bc4646c60968L, 0x149f8c339958964fL,
        0xea140efc2121e5faL, 0xbb274991873c8148L, 0xee6c77038d610dbaL, 0xebaab9379277d787L,
        0x3966eb686a44bb24L, 0x54bb61aa4003b069L, 0xcde854e91cc2cb30L, 0x1f8f8f30b896c2e5L,
        0x50b15f5e42bc2517L, 0x2f6695e6dc48d57aL, 0x35a4c31375a7b365L, 0x9927e1adf43ee3c2L,
        0xe99ffb0a8dac464cL, 0x41816340c01c96d6L, 0x8f5ebe34bcadcc8cL, 0x3099b1ac7bba1f73L,
        0x74ad51e9c17ca276L, 0xc8a871271ab601baL, 0x659aa958c902a843L, 0x741516f44b750698L,
    };

    private static final long[] EXPECTED_SEQUENCE_AFTER_JUMP = {
        0x3490d35943b71b6eL, 0x1381f9435a364888L, 0x583be5b08408e06cL, 0x96d9ebeb8441d875L,
        0x33fb5086bca9477dL, 0x54a1318e630e3206L, 0x28d51480f6c0fc26L, 0xfc55a9d294c5d9d3L,
        0x5d2e6bf2d7a8b758L, 0x2cd5f8233e87db70L, 0x1c2b1fe36201510fL, 0xd6178211474796f9L,
        0x5897035554f9057bL, 0x6088bb64660e1d51L, 0x98e07f6c6a499cb9L, 0x98f9bd3445a33f43L,
        0x0c28666f552423bdL, 0xb8f19aa7fdb89c1aL, 0x548713e79008b073L, 0x61d2bbb14a0f3bc1L,
        0x917ec60de3ffb14eL, 0xd1fe8d6f37b9bd70L, 0x162531e606e234edL, 0x4f83a041b08cf330L,
        0xc38ddec5d5179651L, 0x27e89ca3ddf9428bL, 0x14800d12f9ca493aL, 0x0dab376fd3d4d190L,
        0xe1e9f6789280e16dL, 0x39025d8c68e4bf32L, 0x8112964847bb7f20L, 0x9c27cab74ab44154L,
        0xdf2ecc32a56b4f74L, 0x1b520df69792caacL, 0x1bf881006d0bf761L, 0x25b263716b101941L,
        0xd8d54d4602dd3719L, 0x7e6afa6cf20adcdcL, 0x4468886c474bc7bdL, 0x95aabb6847f69f47L,
    };

    private static final long[] EXPECTED_SEQUENCE_AFTER_LONG_JUMP = {
        0x218b3c94697bfd62L, 0xe2b22013826bfbcbL, 0xca92e100129f73cdL, 0x401313c6b39a0f61L,
        0x595926c839e29f64L, 0x1f95ce075d9009bdL, 0x54f0f8f0f8389f19L, 0x788a0c369131e765L,
        0x8d4a94df5837ac36L, 0xfe01e5ede70ca81eL, 0xc5e3709b662ae8ecL, 0x541f3cd991d5602bL,
        0xcc313c38ccb11f6fL, 0xa97ccc8f19546e0dL, 0x93c3808953be0ea7L, 0x89fdac3da0e315a6L,
        0xdd979f2ae97e8b4aL, 0x74fb917f5bdec895L, 0x2ac64ab3d6672713L, 0x72bc49a21ede60e0L,
        0x9ec32cacadd5e908L, 0x90d7b30c1abe35a2L, 0x332bc85a538edc63L, 0xf91e8b31e02fdf2bL,
        0xed40aee6f7cd0201L, 0x1bbf20497f99c4eaL, 0x390994ea6c7430bfL, 0xcb5c702a5f827df3L,
        0xe21c537cda1abddfL, 0xefdbd8500965fd8bL, 0x9f8c363e8824e1eeL, 0x70bb6b5670253cdcL,
        0x271cdbf6954126dbL, 0xf1942493c16240a7L, 0xfc7ad4185188e39dL, 0x0726ee3787d4b389L,
        0x637d2aac6c82ff5fL, 0xdd00930da7790bf0L, 0xd3b37aa49732fe48L, 0x60a0f58457ed68a0L,
    };

    @Test
    public void testReferenceCode() {
        RandomAssert.assertEquals(EXPECTED_SEQUENCE, new XoShiRo512PlusPlus(SEED));
    }

    @Test
    public void testConstructorWithZeroSeedIsNonFunctional() {
        RandomAssert.assertNextIntZeroOutput(new XoShiRo512PlusPlus(new long[SEED_SIZE]), 2 * SEED_SIZE);
    }

    @Test
    public void testConstructorWithSingleBitSeedIsFunctional() {
        RandomAssert.assertLongArrayConstructorWithSingleBitSeedIsFunctional(XoShiRo512PlusPlus.class, SEED_SIZE);
    }

    @Test
    public void testConstructorWithoutFullLengthSeed() {
        // Hit the case when the input seed is self-seeded when not full length
        RandomAssert.assertNextLongNonZeroOutput(new XoShiRo512PlusPlus(new long[] {SEED[0]}),
                SEED_SIZE, SEED_SIZE);
    }

    @Test
    public void testElementConstructor() {
        final XoShiRo512PlusPlus rng1 = new XoShiRo512PlusPlus(SEED);
        final XoShiRo512PlusPlus rng2 = new XoShiRo512PlusPlus(SEED[0], SEED[1], SEED[2], SEED[3],
                                                               SEED[4], SEED[5], SEED[6], SEED[7]);
        RandomAssert.assertNextLongEquals(SEED.length * 2, rng1, rng2);
    }

    @Test
    public void testJump() {
        RandomAssert.assertJumpEquals(EXPECTED_SEQUENCE, EXPECTED_SEQUENCE_AFTER_JUMP, new XoShiRo512PlusPlus(SEED));
    }

    @Test
    public void testLongJump() {
        RandomAssert.assertLongJumpEquals(EXPECTED_SEQUENCE, EXPECTED_SEQUENCE_AFTER_LONG_JUMP, new XoShiRo512PlusPlus(SEED));
    }
}
