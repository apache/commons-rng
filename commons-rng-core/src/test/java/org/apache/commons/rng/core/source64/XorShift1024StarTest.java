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

class XorShift1024StarTest {
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
        0xd85e9fc0855614cdL, 0xaf4965c9c1ac6a3dL, 0x067da398791111d8L, 0x2771c41db58d7644L,
        0xf71a471e1ac2b03eL, 0x953449ae275f7409L, 0x8aa570c72de0af5eL, 0xae59db2acdae32beL,
        0x3d46f316b8f97301L, 0x72dc8399b7a70957L, 0xf5624d788b3b6f4eL, 0xb7a79275f6c0e7b1L,
        0xf79354208377d498L, 0x0e5d2f2ac2b4f28fL, 0x0f8f57edc8aa802fL, 0x5e918ea72ece0c36L,
        0xeeb8dbdb00ac7a5aL, 0xf16f88dfef0d6047L, 0x1244c29e0e0d8d2dL, 0xaa94f1cc42691eb7L,
        0xd06425dd329e5de5L, 0x968b1c2e016f159cL, 0x6aadff7055065295L, 0x3bce2efcb0d00876L,
        0xb28d5b69ad8fb719L, 0x1e4040c451376920L, 0x6b0801a8a00de7d7L, 0x891ba2cbe2a4675bL,
        0x6355008481852527L, 0x7a47bcd9960126f3L, 0x07f72fcd4ebe3580L, 0x4658b29c126840ccL,
        0xdc7b36d3037c7539L, 0x9e30aab0410122e8L, 0x7215126e0fce932aL, 0xda63f12a489fc8deL,
        0x769997671b2a0158L, 0xfa9cd84e0ffc174dL, 0x34df1cd959dca211L, 0xccea41a33ec1f763L,
    };

    private static final long[] EXPECTED_SEQUENCE_AFTER_JUMP = {
        0x65d54201f5acfddeL, 0x6fde36eb1f6a0de8L, 0x4bb549d952f83a9eL, 0x8a450adce576715aL,
        0xb031eca9597292adL, 0xf37e0e06b835d614L, 0x26bfeba6b6a6066aL, 0xd3e5bc247e11b3e9L,
        0x17efb0eaa881e128L, 0x17b53d5adf9c0c6cL, 0xcf40e97397cf3b90L, 0xaa9c9cc35ba0f5c0L,
        0x531285c99a04b28aL, 0x868bac37b6520ea0L, 0x8532f89936f0c04dL, 0x46d686a34dafce8cL,
        0xca15a5750af797b7L, 0x83cc94eb061c67a7L, 0xea90f3d41c3da1f3L, 0x18b74852ce1150daL,
        0x193e6b8e68eea0f9L, 0x6ac72b7aa768f620L, 0xf302fae8b5f1705cL, 0x7cfbedcfa8626240L,
        0x46a5a5fb00c3ee85L, 0xde6d0648b997303bL, 0x56ae91e7655900bbL, 0xab5031aa9f399904L,
        0x8a97889ade2a276fL, 0x1b9faa106ffce5d3L, 0xab648350c8f58ecbL, 0x713bfb795f4397b7L,
        0x2935361aa09ab73cL, 0xb5b425a0382d3f4cL, 0x8253cb35cdc787b1L, 0x9bad73436171212dL,
        0x1d9c3fd0fcf525b7L, 0x5c0b302408af77ecL, 0x8e92e2d9a37b8481L, 0x8f3c06c39295f749L,
    };

    @Test
    void testReferenceCode() {
        RandomAssert.assertEquals(EXPECTED_SEQUENCE, new XorShift1024Star(SEED));
    }

    @Test
    void testConstructorWithZeroSeedIsNonFunctional() {
        RandomAssert.assertNextIntZeroOutput(new XorShift1024Star(new long[SEED_SIZE]), 2 * SEED_SIZE);
    }

    @Test
    void testConstructorWithSingleBitSeedIsFunctional() {
        RandomAssert.assertLongArrayConstructorWithSingleBitSeedIsFunctional(XorShift1024Star.class, SEED_SIZE);
    }

    @Test
    void testJump() {
        RandomAssert.assertJumpEquals(EXPECTED_SEQUENCE, EXPECTED_SEQUENCE_AFTER_JUMP, new XorShift1024Star(SEED));
    }
}
