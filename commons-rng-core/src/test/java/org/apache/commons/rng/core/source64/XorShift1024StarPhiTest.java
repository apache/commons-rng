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

public class XorShift1024StarPhiTest {
    /** The size of the array seed. */
    private static final int SEED_SIZE = 16;

    @Test
    public void testReferenceCode() {
        /*
         * Data from running the executable compiled from the author's C code:
         *   http://xorshift.di.unimi.it/xorshift1024star.c
         */
        final long[] seed = {
            0x012de1babb3c4104L, 0xa5a818b8fc5aa503L, 0xb124ea2b701f4993L, 0x18e0374933d8c782L,
            0x2af8df668d68ad55L, 0x76e56f59daa06243L, 0xf58c016f0f01e30fL, 0x8eeafa41683dbbf4L,
            0x7bf121347c06677fL, 0x4fd0c88d25db5ccbL, 0x99af3be9ebe0a272L, 0x94f2b33b74d0bdcbL,
            0x24b5d9d7a00a3140L, 0x79d983d781a34a3cL, 0x582e4a84d595f5ecL, 0x7316fe8b0f606d20L,
        };

        final long[] expectedSequence = {
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

        RandomAssert.assertEquals(expectedSequence, new XorShift1024StarPhi(seed));
    }

    @Test
    public void testConstructorWithZeroSeedIsNonFunctional() {
        RandomAssert.assertNextIntZeroOutput(new XorShift1024StarPhi(new long[SEED_SIZE]), 2 * SEED_SIZE);
    }

    @Test
    public void testConstructorWithSingleBitSeedIsFunctional() {
        RandomAssert.assertLongArrayConstructorWithSingleBitSeedIsFunctional(XorShift1024StarPhi.class, SEED_SIZE);
    }
}
