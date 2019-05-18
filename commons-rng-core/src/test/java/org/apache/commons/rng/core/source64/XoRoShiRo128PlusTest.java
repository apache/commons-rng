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

public class XoRoShiRo128PlusTest {
    /** The size of the array seed. */
    private static final int SEED_SIZE = 2;

    @Test
    public void testReferenceCode() {
        /*
         * Data from running the executable compiled from the author's C code:
         *   http://xoshiro.di.unimi.it/xoroshiro128plus.c
         */
        final long[] seed = {
            0x012de1babb3c4104L, 0xa5a818b8fc5aa503L,
        };

        final long[] expectedSequence = {
            0xa6d5fa73b796e607L, 0xd419031a381fea2eL, 0x28938b88b4972f52L, 0x032793a0d51c1a27L,
            0x50001cd69cc5b006L, 0x44bbf571167cb7f0L, 0x172f6a2f093b2befL, 0xe642c831f1e4f7bfL,
            0xcec4e4b5d448032aL, 0xc0164992807cbd59L, 0xb96ff06c68515410L, 0x5288e0312a0aae72L,
            0x79a891c387d3be2eL, 0x6c52f6f710db553bL, 0x2ce6f6b1946862b3L, 0x87eb1e1b24b47f11L,
            0x9f7c3511c5f23bcfL, 0x3254897533dcd1abL, 0x89d56ad217fbd1adL, 0x70f6b269f815f6e6L,
            0xe8ee60efadfdb8c4L, 0x09286db69fdd232bL, 0xf440882651fc19e8L, 0x6356fea018cc26cdL,
            0xf692282b43fcb0c2L, 0xef3f084929119babL, 0x355efbf5bedeb114L, 0x6cf5089c2acc96ddL,
            0x819c19e480f0bfd1L, 0x414d12ff4082e261L, 0xc9a33a52545dd374L, 0x4675247e6fe89b3cL,
            0x069f2e55cea155baL, 0x1e8d1dcf349746b8L, 0xdf32e487bdd74523L, 0xa544710cae2ad7cdL,
            0xf5ac505e74fe049dL, 0xf039e289da4cdf7eL, 0x0a6fbebe9122529cL, 0x880c51e0915031a3L,
        };

        RandomAssert.assertEquals(expectedSequence, new XoRoShiRo128Plus(seed));
    }

    @Test
    public void testConstructorWithZeroSeedIsNonFunctional() {
        RandomAssert.assertNextIntZeroOutput(new XoRoShiRo128Plus(new long[SEED_SIZE]), 2 * SEED_SIZE);
    }

    @Test
    public void testConstructorWithSingleBitSeedIsFunctional() {
        RandomAssert.assertLongArrayConstructorWithSingleBitSeedIsFunctional(XoRoShiRo128Plus.class, SEED_SIZE);
    }

    @Test
    public void testConstructorWithoutFullLengthSeed() {
        // Hit the case when the input seed is self-seeded when not full length
        new XoRoShiRo128Plus(new long[] { 0x012de1babb3c4104L });
    }

    @Test
    public void testElementConstructor() {
        final long[] seed = {
            0x012de1babb3c4104L, 0xa5a818b8fc5aa503L,
        };
        final XoRoShiRo128Plus rng1 = new XoRoShiRo128Plus(seed);
        final XoRoShiRo128Plus rng2 = new XoRoShiRo128Plus(seed[0], seed[1]);
        RandomAssert.assertNextLongEquals(seed.length * 2, rng1, rng2);
    }
}
