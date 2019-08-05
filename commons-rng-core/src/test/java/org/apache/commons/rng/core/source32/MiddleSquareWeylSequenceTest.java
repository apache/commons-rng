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

public class MiddleSquareWeylSequenceTest {
    @Test
    public void testReferenceCode() {
        /*
         * The data was generated using the following program based on the author's C code:
         *     https://mswsrng.wixsite.com/rand
         *
         * #include <stdio.h>
         * #include <stdint.h>
         *
         * uint64_t x, w, s;
         *
         * inline static uint32_t msws() {
         *     x *= x; x += (w += s); return x = (x>>32) | (x<<32);
         * }
         *
         * int main() {
         *     x = 0x012de1babb3c4104;
         *     w = 0xc8161b4202294965;
         *     s = 0xb5ad4eceda1ce2a9;
         *     for (int i=0; i<10; i++) {
         *         for (int j=0; j<4; j++) {
         *             printf("0x%08x, ", msws());
         *         }
         *         printf("\n");
         *     }
         * }
         */
        final long[] seed = {0x012de1babb3c4104L, 0xc8161b4202294965L, 0xb5ad4eceda1ce2a9L};

        final int[] expectedSequence = {
            0xe7f4010b, 0x37bdb1e7, 0x05d8934f, 0x22970c75,
            0xe7432a9f, 0xd157c60f, 0x26e9b5ae, 0x3dd91250,
            0x8dbf85f1, 0x99e3aa17, 0xcb90322b, 0x29a007e2,
            0x25a431fb, 0xcc612768, 0x510db5cd, 0xeb0aec2f,
            0x05f88c18, 0xcdb79066, 0x5222c513, 0x9075045c,
            0xf11a0e0e, 0x0106ab1d, 0xe2546700, 0xdf0a7656,
            0x170e7908, 0x17a7b775, 0x98d69720, 0x74da3b78,
            0x410ea18e, 0x4f708277, 0x471853e8, 0xa2cd2587,
            0x16238d96, 0x57653154, 0x7ecbf9c8, 0xc5dd75bf,
            0x32ed82a2, 0x4700e664, 0xb0ad77c9, 0xfb87df7b,
        };

        RandomAssert.assertEquals(expectedSequence, new MiddleSquareWeylSequence(seed));
    }

    /**
     * Test the self-seeding functionality. The seeding of the generator requires a high complexity
     * increment for the Weyl sequence. The standard test for the statistical quality of the
     * generator uses a good increment. This test ensures the generator can be created with a seed
     * smaller than the seed size without exception; the statistical quality of the output is not
     * assessed and expected to be poor.
     */
    @Test
    public void testSelfSeeding() {
        // Ensure this does not throw. The output quality will be poor.
        final MiddleSquareWeylSequence rng = new MiddleSquareWeylSequence(new long[0]);
        rng.nextInt();
    }

    /**
     * Test nextLong() returns two nextInt() values joined together. This tests the custom
     * nextLong() routine in the implementation that overrides the default.
     */
    @Test
    public void testNextLong() {
        final long[] seed = {0x012de1babb3c4104L, 0xc8161b4202294965L, 0xb5ad4eceda1ce2a9L};
        final MiddleSquareWeylSequence rng1 = new MiddleSquareWeylSequence(seed);
        final MiddleSquareWeylSequence rng2 = new MiddleSquareWeylSequence(seed);
        for (int i = 0; i < 50; i++) {
            Assert.assertEquals(NumberFactory.makeLong(rng1.nextInt(), rng1.nextInt()),
                                rng2.nextLong());
        }
    }
}
