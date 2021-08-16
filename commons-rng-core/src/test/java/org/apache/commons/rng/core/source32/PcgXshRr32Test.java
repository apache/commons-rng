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

class PcgXshRr32Test {
    @Test
    void testReferenceCode() {
        /*
         * Tested with respect to pcg_engines::setseq_xsh_rr_64_32(x, y) of the C++ implementation.
         * See : http://www.pcg-random.org/download.html#cpp-implementation
         */
        final int[] expectedSequence = {
            0xe860dd24, 0x15d339c0, 0xd9f75c46, 0x00efabb7,
            0xa625e97f, 0xcdeae599, 0x6304e667, 0xbc81be11,
            0x2b8ea285, 0x8e186699, 0xac552be9, 0xd1ae72e5,
            0x5b953ad4, 0xa061dc1b, 0x526006e7, 0xf5a6c623,
            0xfcefea93, 0x3a1964d2, 0xd6f03237, 0xf3e493f7,
            0x0c733750, 0x34a73582, 0xc4f8807b, 0x92b741ca,
            0x0d38bf9c, 0xc39ee6ad, 0xdc24857b, 0x7ba8f7d8,
            0x377a2618, 0x92d83d3f, 0xd22a957a, 0xb6724af4,
            0xe116141a, 0xf465fe45, 0xa95f35bb, 0xf0398d4d,
            0xe880af3e, 0xc2951dfd, 0x984ec575, 0x8679addb,
        };
        RandomAssert.assertEquals(expectedSequence, new PcgXshRr32(new long[] {
            0x012de1babb3c4104L, 0xc8161b4202294965L
        }));
    }

    @Test
    void testReferenceCodeFixedIncrement() {
        /*
         * Tested with respect to pcg_engines::setseq_xsh_rr_64_32(x) of the C++ implementation.
         * See : http://www.pcg-random.org/download.html#cpp-implementation
         */
        final int[] expectedSequence = {
            0x0d2d5291, 0x45df90aa, 0xc60f3fb7, 0x06694f16,
            0x29563e6f, 0x42f46063, 0xf2be5583, 0x30360e91,
            0x36385531, 0xddd36cd9, 0x5f4a6535, 0x644d10c0,
            0xaca075d7, 0x33781706, 0x4e1f9f34, 0x0676e286,
            0xaca5eeb2, 0x7315cc93, 0xa6dfefe2, 0xd480e065,
            0xda9da26f, 0xda0f27b7, 0x045c0844, 0x22acfa0f,
            0xcd7ecd75, 0xb97fd692, 0xac96dd03, 0xf59c7174,
            0x488947fe, 0x64a3d543, 0x90963884, 0x4adee0bb,
            0x993cf7c0, 0x8545b3f2, 0x409b542d, 0x6bf0a247,
            0xfd59f9b4, 0x8f50b06e, 0x1bbcf6f5, 0xe1fdd29c,
        };
        RandomAssert.assertEquals(expectedSequence, new PcgXshRr32(0x012de1babb3c4104L));
    }
}
