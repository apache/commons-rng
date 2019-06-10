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

public class XoShiRo256PlusTest {
    /** The size of the array SEED. */
    private static final int SEED_SIZE = 4;

    /*
     * Data from running the executable compiled from the author's C code:
     *   http://xoshiro.di.unimi.it/xoshiro256plus.c
     */

    private static final long[] SEED = {
        0x012de1babb3c4104L, 0xa5a818b8fc5aa503L, 0xb124ea2b701f4993L, 0x18e0374933d8c782L,
    };

    private static final long[] EXPECTED_SEQUENCE = {
        0x1a0e1903ef150886L, 0x08b605f47abc5d75L, 0xd82176096ac9be31L, 0x8fbf2af9b4fa5405L,
        0x9ab074b448171964L, 0xfd68cc83ab4360aaL, 0xf431f7c0c8dc6f2bL, 0xc04430be08212638L,
        0xc1ad670648f1da03L, 0x3eb70d38796ba24aL, 0x0e474d0598251ed2L, 0xf9b6b3b56482566bL,
        0x3d11e529ae07a7c8L, 0x3b195f84f4db17e7L, 0x09d62e817b8223e2L, 0x89dc4db9cd625509L,
        0x52e04793fe977846L, 0xc052428d6d7d17cdL, 0x6fd6f8da306b10efL, 0x64a7996ba5cc80aaL,
        0x03abf59b95a1ef20L, 0xc5a82fc3cfb50234L, 0x0d401229eabb2d39L, 0xb537b249f70bd18aL,
        0x1af1b703753fcf4dL, 0xb84648c1945d9ccbL, 0x1d321bea673e1f66L, 0x93d4445b268f305fL,
        0xc046cfa36d89a312L, 0x8cc2d55bbf778790L, 0x1d668b0a3d329cc7L, 0x81b6d533dfcf82deL,
        0x9ca1c49a18537b16L, 0x68e55c4054e0cb72L, 0x06ed1956cb69afc6L, 0x4871e696449da910L,
        0xcfbd7a145066d46eL, 0x10131cb15004b62dL, 0x489c91a322bca3b6L, 0x8ec95fa9bef73e66L,
    };

    private static final long[] EXPECTED_SEQUENCE_AFTER_JUMP = {
        0x894cd8014fa285abL, 0x9737ec9aba91e4b6L, 0xf53b956d74db413aL, 0x6fb0350e20edef6bL,
        0x1babe425f938088fL, 0x04f33708a2103773L, 0x03a3f59f511629dfL, 0x9d7323fd9cc8f542L,
        0x8df0f8083323117bL, 0x9097a2cc69730c34L, 0x54e01393f7e1c5f6L, 0x14971cb42dce9e33L,
        0x6ee4f7da32d287feL, 0x36124f300901b735L, 0x71726514f0341ccaL, 0xbdd6ff5845590a93L,
        0x75982d4223903b23L, 0x75e88dbec205937cL, 0x82fa1ef5ed2d3ff5L, 0x49983b880a0758b8L,
        0x8d3d74acd90595ccL, 0x1176ea450c32b01fL, 0xfddac8dca767aee0L, 0xbd8226c3f021dcfdL,
        0xf95c1aead608a5f9L, 0xc7bd37c9a128d4f2L, 0x8abc94eb440371ceL, 0x4f86410df47f6928L,
        0xd2d3479afe5730feL, 0xa7e02f6550aa6668L, 0x5f8b8630e9f5814eL, 0xe8c605350467cdccL,
        0xecc91d6be68b5d11L, 0xbe9382f9d9e9e205L, 0xb512c7b80ca731f1L, 0x5125f56b47a89007L,
        0x6d98bfd64342222bL, 0x7244f91ff7efc522L, 0xbcf26439fef5555fL, 0xce657c86d81455ceL,
    };

    @Test
    public void testReferenceCode() {
        RandomAssert.assertEquals(EXPECTED_SEQUENCE, new XoShiRo256Plus(SEED));
    }

    @Test
    public void testConstructorWithZeroSeedIsNonFunctional() {
        RandomAssert.assertNextIntZeroOutput(new XoShiRo256Plus(new long[SEED_SIZE]), 2 * SEED_SIZE);
    }

    @Test
    public void testConstructorWithSingleBitSeedIsFunctional() {
        RandomAssert.assertLongArrayConstructorWithSingleBitSeedIsFunctional(XoShiRo256Plus.class, SEED_SIZE);
    }

    @Test
    public void testConstructorWithoutFullLengthSeed() {
        // Hit the case when the input seed is self-seeded when not full length
        new XoShiRo256Plus(new long[] {SEED[0]});
    }

    @Test
    public void testElementConstructor() {
        final XoShiRo256Plus rng1 = new XoShiRo256Plus(SEED);
        final XoShiRo256Plus rng2 = new XoShiRo256Plus(SEED[0], SEED[1], SEED[2], SEED[3]);
        RandomAssert.assertNextLongEquals(SEED.length * 2, rng1, rng2);
    }

    @Test
    public void testJump() {
        RandomAssert.assertJumpEquals(EXPECTED_SEQUENCE, EXPECTED_SEQUENCE_AFTER_JUMP, new XoShiRo256Plus(SEED));
    }
}
