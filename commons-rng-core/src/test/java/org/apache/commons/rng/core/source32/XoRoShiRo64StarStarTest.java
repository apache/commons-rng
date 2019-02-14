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
package org.apache.commons.rng.core.source32;

import org.apache.commons.rng.core.RandomAssert;
import org.apache.commons.rng.core.util.NumberFactory;
import org.junit.Assert;
import org.junit.Test;

public class XoRoShiRo64StarStarTest {
    @Test
    public void testReferenceCode() {
        /*
         * Data from running the executable compiled from the author's C code:
         *   http://xoshiro.di.unimi.it/xoroshiro64starstar.c
         */
        final int[] seed = {
            0x012de1ba, 0xa5a818b8,
        };

        final int[] expectedSequence = {
            0x7ac00b42, 0x1f638399, 0x09e4aea4, 0x05cbbd64,
            0x1c967b7b, 0x1cf852fd, 0xc666f4e8, 0xeea9f1ae,
            0xca0fa6bc, 0xa65d0905, 0xa69afc95, 0x34965e62,
            0xdd4f04a9, 0xff1c9342, 0x638ff769, 0x03419ca0,
            0xb46e6dfd, 0xf7555b22, 0x8cab4e68, 0x5a44b6ee,
            0x4e5e1eed, 0xd03c5963, 0x782d05ed, 0x41bda3e3,
            0xd1d65005, 0x88f43a8a, 0xfffe02ea, 0xb326624a,
            0x1ec0034c, 0xb903d8df, 0x78454bd7, 0xaec630f8,
            0x2a0c9a3a, 0xc2594988, 0xe71e767e, 0x4e0e1ddc,
            0xae945004, 0xf178c293, 0xa04081d6, 0xdd9c062f,
        };

        RandomAssert.assertEquals(expectedSequence, new XoRoShiRo64StarStar(seed));
    }

    @Test
    public void testConstructorWithZeroSeed() {
        // This is allowed even though the generator is non-functional
        final int size = 2;
        final XoRoShiRo64StarStar rng = new XoRoShiRo64StarStar(new int[size]);
        for (int i = size * 2; i-- != 0; ) {
            Assert.assertEquals("Expected the generator to be broken", 0, rng.nextInt());
        }
    }

    @Test
    public void testConstructorWithoutFullLengthSeed() {
        // Hit the case when the input seed is self-seeded when not full length
        new XoRoShiRo64StarStar(new int[] { 0x012de1ba });
    }

    @Test
    public void testElementConstructor() {
        final int[] seed = {
            0x012de1ba, 0xa5a818b8,
        };
        final XoRoShiRo64StarStar rng1 = new XoRoShiRo64StarStar(seed);
        final XoRoShiRo64StarStar rng2 = new XoRoShiRo64StarStar(seed[0], seed[1]);
        for (int i = seed.length * 2; i-- != 0; ) {
            Assert.assertEquals(rng1.nextInt(), rng2.nextInt());
        }
    }

    @Test
    public void testLongElementConstructor() {
        final int[] seed = {
            0x012de1ba, 0xa5a818b8,
        };
        final XoRoShiRo64StarStar rng1 = new XoRoShiRo64StarStar(seed);
        final XoRoShiRo64StarStar rng2 = new XoRoShiRo64StarStar(NumberFactory.makeLong(seed[0], seed[1]));
        for (int i = seed.length * 2; i-- != 0; ) {
            Assert.assertEquals(rng1.nextInt(), rng2.nextInt());
        }
    }
}
