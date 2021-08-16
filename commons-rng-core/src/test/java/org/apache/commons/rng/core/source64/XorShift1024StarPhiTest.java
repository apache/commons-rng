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

class XorShift1024StarPhiTest {
    /** The size of the array SEED. */
    private static final int SEED_SIZE = 16;

    /*
     * Data from running the executable compiled from the author's C code:
     *   http://xorshift.di.unimi.it/xorshift1024star.c
     */

    private static final long[] SEED = {
        0x012de1babb3c4104L, 0xa5a818b8fc5aa503L, 0xb124ea2b701f4993L, 0x18e0374933d8c782L,
        0x2af8df668d68ad55L, 0x76e56f59daa06243L, 0xf58c016f0f01e30fL, 0x8eeafa41683dbbf4L,
        0x7bf121347c06677fL, 0x4fd0c88d25db5ccbL, 0x99af3be9ebe0a272L, 0x94f2b33b74d0bdcbL,
        0x24b5d9d7a00a3140L, 0x79d983d781a34a3cL, 0x582e4a84d595f5ecL, 0x7316fe8b0f606d20L,
    };

    private static final long[] EXPECTED_SEQUENCE = {
        0xc9351be6ae9af4bbL, 0x2696a1a51e3040cbL, 0xdcbbc38b838b4be8L, 0xc989eee03351a25cL,
        0xc4ad829b653ada72L, 0x1cff4000cc0118dfL, 0x988f3aaf7bfb2852L, 0x3a621d4d5fb27bf2L,
        0x0153d81cf33ff4a7L, 0x8a1b5adb974750c1L, 0x182799e238df6de2L, 0x92d9bda951cd6377L,
        0x601f077d2a659728L, 0x90536cc64ad5bc49L, 0x5d99d9e84e3d7fa9L, 0xfc66f4610240613aL,
        0x0ff67da640cdd6b6L, 0x973c7a6afbb41751L, 0x5089cb5236ac1b5bL, 0x7ed6edc1e4d7e261L,
        0x3e37630df0c00b63L, 0x49ec234a0d03bcc4L, 0x2bcbe2fa4b80fa33L, 0xbaafc47b960baefaL,
        0x1855fa47be98c84fL, 0x8d59cb57e34a73e0L, 0x256b15bb001bf641L, 0x28ad378895f5615dL,
        0x865547335ba2a571L, 0xfefe4c356e154585L, 0xeb87f7a74e076680L, 0x990d2f5d1e60b914L,
        0x3bf0f6864688af2fL, 0x8c6304df9b945d58L, 0x63bc09c335b63666L, 0x1038139f53734ad2L,
        0xf41b58faf5680868L, 0xa50ba830813c163bL, 0x7dc1ca503ae39817L, 0xea3d0f2f37f5ce95L,
    };

    private static final long[] EXPECTED_SEQUENCE_AFTER_JUMP = {
        0x07afdab6d38bddd2L, 0x7bb8f1495d6aaa58L, 0x00f583e9eb57bf12L, 0x694e972ba626f7b6L,
        0x0f7017b991b531dbL, 0x702fc8e6791b530cL, 0xf62322eab2db2526L, 0xe6bdc2cffeeae3ffL,
        0xa0cb7cb78c2e3918L, 0x53fa4d1ee744ee74L, 0x5f0bcdebaf4b4af0L, 0xec23017af16e9040L,
        0x2d1119530d4f4e06L, 0x75b721c9942eea60L, 0x6aab166dbc9d553bL, 0xcfa59b433d647154L,
        0x687a4f5aa4bfd161L, 0xcc954692756486f1L, 0xcfa57a42ae5e8285L, 0xe290ecfae5d74436L,
        0x0adb47de60db796fL, 0xee4e161668406ee0L, 0xe7f5beb82ec63004L, 0x5ee1ed818be7d7c0L,
        0xb1aa1517d646c3c3L, 0x31ab29451adf8b7dL, 0x2612a880abf60efdL, 0xa7679f88450d8d9cL,
        0xee3b07f323a85a69L, 0xac7e0039bb81e9a5L, 0x5b454710e237ab6dL, 0xdd6c4ac09653d161L,
        0xc03e09a39f5e8c24L, 0x4a8352f76f7c3e94L, 0xebeb6e56c67fc377L, 0x3726d52cb3cda75bL,
        0x3f6c00e368b97361L, 0xc49b806ba04c8ef4L, 0xf396608b186fdf27L, 0xe0c7f13ba319779fL,
    };

    @Test
    void testReferenceCode() {
        RandomAssert.assertEquals(EXPECTED_SEQUENCE, new XorShift1024StarPhi(SEED));
    }

    @Test
    void testConstructorWithZeroSeedIsNonFunctional() {
        RandomAssert.assertNextIntZeroOutput(new XorShift1024StarPhi(new long[SEED_SIZE]), 2 * SEED_SIZE);
    }

    @Test
    void testConstructorWithSingleBitSeedIsFunctional() {
        RandomAssert.assertLongArrayConstructorWithSingleBitSeedIsFunctional(XorShift1024StarPhi.class, SEED_SIZE);
    }

    @Test
    void testJump() {
        RandomAssert.assertJumpEquals(EXPECTED_SEQUENCE, EXPECTED_SEQUENCE_AFTER_JUMP, new XorShift1024StarPhi(SEED));
    }
}
