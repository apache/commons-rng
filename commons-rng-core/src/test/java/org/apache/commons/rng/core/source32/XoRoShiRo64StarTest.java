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

public class XoRoShiRo64StarTest {
    @Test
    public void testReferenceCode() {
        /*
         * Data from running the executable compiled from the author's C code:
         *   http://xoshiro.di.unimi.it/xoroshiro64star.c
         */
        final int[] seed = {
            0x012de1ba, 0xa5a818b8,
        };

        final int[] expectedSequence = {
            0xd72accde, 0x29cbd26c, 0xa00fd44a, 0xa4d612c8,
            0xf9c7572b, 0xce94c084, 0x47a3d7ee, 0xb64aa982,
            0x67a9b2a4, 0x0c3d61a8, 0x8f70f7fa, 0xd1edbd63,
            0xac954b3a, 0xd7fe941e, 0xaa38e658, 0x019ecf61,
            0xcded7d7c, 0xd6588891, 0x4414454a, 0xb3c3a124,
            0x4a16fcfe, 0x3fb393c2, 0x4d8d14d6, 0x3a02c906,
            0x0c82f080, 0x174186c4, 0x1199966b, 0x12b83d6a,
            0xe697999e, 0x9df4d2f4, 0x5a5a0879, 0xc44ad6b4,
            0x96a9adc3, 0x4603c20f, 0x3171ca57, 0x66e349c9,
            0xa77dba19, 0xbe4f279d, 0xf5cd3402, 0x1962933d,
        };

        RandomAssert.assertEquals(expectedSequence, new XoRoShiRo64Star(seed));
    }

    @Test
    public void testConstructorWithZeroSeed() {
        // This is allowed even though the generator is non-functional
        final int size = 2;
        final XoRoShiRo64Star rng = new XoRoShiRo64Star(new int[size]);
        for (int i = size * 2; i-- != 0; ) {
            Assert.assertEquals("Expected the generator to be broken", 0, rng.nextInt());
        }
    }

    @Test
    public void testConstructorWithoutFullLengthSeed() {
        // Hit the case when the input seed is self-seeded when not full length
        new XoRoShiRo64Star(new int[] { 0x012de1ba });
    }

    @Test
    public void testElementConstructor() {
        final int[] seed = {
            0x012de1ba, 0xa5a818b8,
        };
        final XoRoShiRo64Star rng1 = new XoRoShiRo64Star(seed);
        final XoRoShiRo64Star rng2 = new XoRoShiRo64Star(seed[0], seed[1]);
        for (int i = seed.length * 2; i-- != 0; ) {
            Assert.assertEquals(rng1.nextInt(), rng2.nextInt());
        }
    }

    @Test
    public void testLongElementConstructor() {
        final int[] seed = {
            0x012de1ba, 0xa5a818b8,
        };
        final XoRoShiRo64Star rng1 = new XoRoShiRo64Star(seed);
        final XoRoShiRo64Star rng2 = new XoRoShiRo64Star(NumberFactory.makeLong(seed[0], seed[1]));
        for (int i = seed.length * 2; i-- != 0; ) {
            Assert.assertEquals(rng1.nextInt(), rng2.nextInt());
        }
    }
}
