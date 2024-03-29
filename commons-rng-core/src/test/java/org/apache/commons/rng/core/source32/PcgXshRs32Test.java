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
import org.junit.jupiter.api.Test;

class PcgXshRs32Test {
    @Test
    void testReferenceCode() {
        /*
         * Tested with respect to pcg_engines::setseq_xsh_rs_64_32(x, y) from the C++ implementation.
         * See : http://www.pcg-random.org/download.html#cpp-implementation
         */
        final int[] expectedSequence = {
            0xba4138b8, 0xd329a393, 0x75d68d3f, 0xbb7572ca,
            0x7a48d2f2, 0xcb3c1e37, 0xc1374a97, 0x7c2c5bfa,
            0x8a1c8695, 0x30db4fea, 0x95f9a901, 0x72ebfa48,
            0x6a284dbf, 0x0ef11286, 0x37330e11, 0xfeb53893,
            0x77e3adda, 0x64dc86bd, 0xc8d762d7, 0xbf3fb80c,
            0x732dfd12, 0x6088e86d, 0xbc4e79e5, 0x56ece5b1,
            0xe706ac72, 0xee798018, 0xef73de74, 0x3de1f966,
            0x7a36db53, 0x1e921eb2, 0x55e35484, 0x2577c6f2,
            0x0a006e21, 0x8cb811b7, 0x5f26c916, 0x3990837f,
            0x15f2983d, 0x546ccb4a, 0x4eda8716, 0xb8666a25,
        };
        RandomAssert.assertEquals(expectedSequence, new PcgXshRs32(new long[] {
            0x012de1babb3c4104L, 0xc8161b4202294965L
        }));
    }

    @Test
    void testReferenceCodeFixedIncrement() {
        /*
         * Tested with respect to pcg_engines::setseq_xsh_rs_64_32(x) from the C++ implementation.
         * See : http://www.pcg-random.org/download.html#cpp-implementation
         */
        final int[] expectedSequence = {
            0x5ab2ddd9, 0x215c476c, 0x83c34b11, 0xe2c5e213,
            0x37979624, 0x303cf5b5, 0xbf2a146e, 0xb0692351,
            0x49b00de3, 0xd9ded67c, 0x298e2bb9, 0xa20d2287,
            0xa067cd33, 0x5c10d395, 0x1f8d8bd5, 0x4306b6bc,
            0x97a3e50b, 0x992e0604, 0x8a982b33, 0x4baa6604,
            0xefd995eb, 0x0f341c29, 0x080bce32, 0xb22b3de2,
            0x5fbf47ff, 0x7fc928bf, 0x075a5871, 0x174a0c48,
            0x72458b67, 0xa869a8c1, 0x64857577, 0xed28377c,
            0x3ce86b48, 0xa855af8b, 0x6a051d88, 0x23b06c33,
            0xb3e4afc1, 0xa848c3e4, 0x79f969a6, 0x670e2acb,
        };
        RandomAssert.assertEquals(expectedSequence, new PcgXshRs32(0x012de1babb3c4104L));
    }
}
