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
import org.junit.Test;

public class SFC32Test {
    @Test
    public void testReferenceCode() {
        /*
         * Tested with respect to PractRand::RNGs::Raw::sfc32 of the C++ implementation (v0.94).
         * See : http://pracrand.sourceforge.net/
         */
        final int[] expectedSequence = {
            0x89b5c414, 0x7ee57639, 0xdbe18f7b, 0x94aa0162,
            0xa22bff0a, 0x21c91fb8, 0x2c6fd6fe, 0xcda90d13,
            0x019684ca, 0xe5b400c0, 0x459d8590, 0x3aec0a1e,
            0x254dac77, 0xbe10ae80, 0x9ac27819, 0xd17d10c6,
            0x71a69026, 0x4bb2bdda, 0x70853646, 0xda28f272,
            0x879200d9, 0x8c2f8b5b, 0x8a87cb78, 0x27ffdced,
            0x988a2b7b, 0xf220ef9b, 0x13b8984f, 0x345d1732,
            0x8f5bc6fc, 0x092b09ff, 0x046bd2b0, 0xa5a99fc5,
            0x19400604, 0xb76e7394, 0x037addd5, 0xe916ed79,
            0x94f10dc6, 0xf2ecb45e, 0x69834355, 0xb814aeb2,
        };
        RandomAssert.assertEquals(expectedSequence, new SFC32(new int[] {
            0xbb3c4104, 0x02294965, 0xda1ce2a9
        }));
    }
}
