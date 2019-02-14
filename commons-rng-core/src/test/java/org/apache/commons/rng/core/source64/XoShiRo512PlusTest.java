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
import org.junit.Assert;
import org.junit.Test;

public class XoShiRo512PlusTest {
    @Test
    public void testReferenceCode() {
        /*
         * Data from running the executable compiled from the author's C code:
         *   http://xoshiro.di.unimi.it/xoshiro512plus.c
         */
        final long[] seed = {
            0x012de1babb3c4104L, 0xa5a818b8fc5aa503L, 0xb124ea2b701f4993L, 0x18e0374933d8c782L,
            0x2af8df668d68ad55L, 0x76e56f59daa06243L, 0xf58c016f0f01e30fL, 0x8eeafa41683dbbf4L,
        };

        final long[] expectedSequence = {
            0xb252cbe62b5b8a97L, 0xa4aaec677f60aaa2L, 0x1c8bd694b50fd00eL, 0x02753e0294233973L,
            0xbfec0be86d152e2dL, 0x5b9cd7265f320e98L, 0xf8ec45eccc703724L, 0x83fcbefa0359b3c1L,
            0xbd27fcdb7e79265dL, 0x88934227d8bf3cf0L, 0x99e1e79384f40371L, 0xe7e7fd0af2014912L,
            0xebdd19cbcd35745dL, 0x218994e1747243eeL, 0x80628718e5d310daL, 0x88ba1395debd989cL,
            0x72e025c0928c6f55L, 0x51400eaa050bbb0aL, 0x72542ad3e7fe29e9L, 0x3a3355b9dcb9c8b0L,
            0x2f6618f3df6126f4L, 0x34307608d886d40fL, 0x34f5a22e98fe3375L, 0x558f6560d08b9ec3L,
            0xae78928bcb041d6cL, 0xe7afe32a7caf4587L, 0x22dcfb5ca129d4bdL, 0x7c5a41864a6f2cf6L,
            0xbe1186add0fe46a7L, 0xd019fabc10dc96a5L, 0xafa642ef6837d342L, 0xdc4924811f62cf03L,
            0xdeb486ccebccf747L, 0xd827b16c9189f637L, 0xf1aab3c3c690a71dL, 0x6551214a7f04a2a5L,
            0x44b8edb239f2a141L, 0xb840cb37cfbeab59L, 0x0e9558adc0987ca2L, 0xc60442d5ff290606L,
        };

        RandomAssert.assertEquals(expectedSequence, new XoShiRo512Plus(seed));
    }

    @Test
    public void testConstructorWithZeroSeed() {
        // This is allowed even though the generator is non-functional
        final int size = 8;
        final XoShiRo512Plus rng = new XoShiRo512Plus(new long[size]);
        for (int i = size * 2; i-- != 0; ) {
            Assert.assertEquals("Expected the generator to be broken", 0L, rng.nextLong());
        }
    }

    @Test
    public void testConstructorWithoutFullLengthSeed() {
        // Hit the case when the input seed is self-seeded when not full length
        new XoShiRo512Plus(new long[] { 0x012de1babb3c4104L });
    }

    @Test
    public void testElementConstructor() {
        final long[] seed = {
            0x012de1babb3c4104L, 0xa5a818b8fc5aa503L, 0xb124ea2b701f4993L, 0x18e0374933d8c782L,
            0x2af8df668d68ad55L, 0x76e56f59daa06243L, 0xf58c016f0f01e30fL, 0x8eeafa41683dbbf4L,
        };
        final XoShiRo512Plus rng1 = new XoShiRo512Plus(seed);
        final XoShiRo512Plus rng2 = new XoShiRo512Plus(seed[0], seed[1], seed[2], seed[3],
                                                       seed[4], seed[5], seed[6], seed[7]);
        for (int i = seed.length * 2; i-- != 0; ) {
            Assert.assertEquals(rng1.nextLong(), rng2.nextLong());
        }
    }
}
