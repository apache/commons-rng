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

public class XoRoShiRo1024PlusPlusTest {
    /** The size of the array SEED. */
    private static final int SEED_SIZE = 16;

    /*
     * Data from running the executable compiled from the author's C code:
     *   http://xoshiro.di.unimi.it/xoroshiro1024plusplus.c
     */

    private static final long[] SEED = {
        0x012de1babb3c4104L, 0xa5a818b8fc5aa503L, 0xb124ea2b701f4993L, 0x18e0374933d8c782L,
        0x2af8df668d68ad55L, 0x76e56f59daa06243L, 0xf58c016f0f01e30fL, 0x8eeafa41683dbbf4L,
        0x7bf121347c06677fL, 0x4fd0c88d25db5ccbL, 0x99af3be9ebe0a272L, 0x94f2b33b74d0bdcbL,
        0x24b5d9d7a00a3140L, 0x79d983d781a34a3cL, 0x582e4a84d595f5ecL, 0x7316fe8b0f606d20L,
    };

    private static final long[] EXPECTED_SEQUENCE = {
        0x3b09ad2dbf0fac01L, 0xc94a7fe723f359b9L, 0xe6e1dd32bf791bfcL, 0xb7544ad223abd2d8L,
        0xf30327faf9c86e1bL, 0xe6e5f8e0c3ee32eeL, 0xd0517ea6f0a6dff8L, 0xe7ab840562f624f9L,
        0x46974064298e33c5L, 0xb1c7d3a0a763d025L, 0x516b8571e4870ed4L, 0xc6fb23b5d1e49b84L,
        0x09aa6d82bcab4254L, 0x23719002bbe8f966L, 0x0d67afdb43a5dca4L, 0x297327cb4057c221L,
        0x4b2af2a3dba0da80L, 0x9eda3fa5e098414aL, 0x6806ec1363b1e1b9L, 0x6efe75a6813c59c6L,
        0x335baf867960e5fbL, 0x9f4415ecc8830b7aL, 0xe1c6456883daafbdL, 0x50d175bab6ac665cL,
        0x8122d5175b11d1f5L, 0xb3671ac101492a4bL, 0x658bac8aa044c300L, 0xa652105130589a28L,
        0xf49f0307772db260L, 0x9d18a1bd5200fcbcL, 0x9cd41d5db25d6593L, 0xe34ecdb564717debL,
        0x8affe46f54d83679L, 0x67639a77a4199b87L, 0xa0d788390eaa4b68L, 0x67f84eff59949883L,
        0xc374a0949d9e7c44L, 0xdb3251d6eb8cfc68L, 0x130ac6799fd3b059L, 0x72258b39becdf313L,
    };

    private static final long[] EXPECTED_SEQUENCE_AFTER_JUMP = {
        0x12907f578f2a163cL, 0xc5034828888b0a7dL, 0xb2cc81c062eb12c9L, 0x7cb14f46bc6c47b1L,
        0x5f5641c733b6b7c9L, 0x36eed13f65e22019L, 0xa6e9a258ec194d05L, 0xf5a6cf90bfed11f1L,
        0xe37a7b9b9e8b429eL, 0xa30129f12e7ca354L, 0x6c38ec46d7707a41L, 0xfd8e399d18e8fccaL,
        0xf93f723ea2ba8b80L, 0xef4b6593ecb01139L, 0x774015239c7fd6adL, 0xc868b4129532870aL,
        0x7a77dc9ea4cebdddL, 0x217dd8bd12b281e1L, 0x18dbc96aa091bf40L, 0xfbb8397be69034d7L,
        0xfb686ead6dcadfd7L, 0x25a17990b448429dL, 0x7476a4cef88b1766L, 0xd6b4eccc2b574014L,
        0x89bc48ea54f24968L, 0x9a779e116dd3ac15L, 0xa10f0df74bd66f83L, 0xfbadde536a6ed6b6L,
        0xa9b98fcc285f5920L, 0x07d8a0b3fb1c89baL, 0x413de5a03081fac0L, 0xfa12ec0e83efcdc9L,
        0x84280bbe5242a8c2L, 0xae6da4ff89c29e50L, 0xe611cd4047f50f31L, 0x972cdee05fc6c463L,
        0x69679b42d792ec82L, 0xfb610ac33ca4efd3L, 0xcb78db0ccb62e334L, 0xd7e1ca3dc8db39c4L,
    };

    private static final long[] EXPECTED_SEQUENCE_AFTER_LONG_JUMP = {
        0x9144383654573d60L, 0x6223e80cec1a7284L, 0xc24ec8b470b103e8L, 0xeeae61f7a3220c62L,
        0xafebedd0e1fa737eL, 0x860201c60edbaf3eL, 0x4b0579f618188e95L, 0x767032c20a4553b5L,
        0xcf6ac990528dd14fL, 0x047eed71a8bfdd05L, 0xbc3afb7e47c198ccL, 0x10ee99db24684f0bL,
        0xe28af1c36d0b80c7L, 0xa648390a61f79e01L, 0x19ed67bc53b7604aL, 0x22454886633affaeL,
        0x925cc38296b612f5L, 0x7490feb237c248bcL, 0xea57d10bd26d79adL, 0x31b5fb910df0d3c6L,
        0x49ce7616a038a946L, 0x75bff93a6c5a2bc3L, 0xaf75a56b81a4d784L, 0x41b4b1aa4e59172aL,
        0xb4ec816fd4484d88L, 0xa8ba3b236a03520aL, 0x88f8c9f2776e8182L, 0x8b4096cdb6668ff8L,
        0xaf90a119ab52bab5L, 0x516ebfdce73f105cL, 0x7f04868cc1c439a3L, 0x8cd3eae52205020fL,
        0x505a05e2eaf6af10L, 0x9f16121875db8153L, 0x92032da26a981a00L, 0x1486140057b674b1L,
        0x335ac573af5d92a3L, 0x72a601de5a4547f9L, 0xb3e8d3309e3f1327L, 0x21a6aae8ec8b1966L,
    };

    @Test
    public void testReferenceCode() {
        RandomAssert.assertEquals(EXPECTED_SEQUENCE, new XoRoShiRo1024PlusPlus(SEED));
    }

    @Test
    public void testConstructorWithZeroSeedIsNonFunctional() {
        RandomAssert.assertNextIntZeroOutput(new XoRoShiRo1024PlusPlus(new long[SEED_SIZE]), 2 * SEED_SIZE);
    }

    @Test
    public void testConstructorWithSingleBitSeedIsFunctional() {
        RandomAssert.assertLongArrayConstructorWithSingleBitSeedIsFunctional(XoRoShiRo1024PlusPlus.class, SEED_SIZE);
    }

    @Test
    public void testConstructorWithoutFullLengthSeed() {
        // Hit the case when the input seed is self-seeded when not full length
        RandomAssert.assertNextLongNonZeroOutput(new XoRoShiRo1024PlusPlus(new long[] {SEED[0]}),
                SEED_SIZE, SEED_SIZE);
    }

    @Test
    public void testJump() {
        RandomAssert.assertJumpEquals(EXPECTED_SEQUENCE, EXPECTED_SEQUENCE_AFTER_JUMP, new XoRoShiRo1024PlusPlus(SEED));
    }

    @Test
    public void testLongJump() {
        RandomAssert.assertLongJumpEquals(EXPECTED_SEQUENCE, EXPECTED_SEQUENCE_AFTER_LONG_JUMP, new XoRoShiRo1024PlusPlus(SEED));
    }
}
