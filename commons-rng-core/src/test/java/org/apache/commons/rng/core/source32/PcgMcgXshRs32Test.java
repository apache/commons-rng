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

public class PcgMcgXshRs32Test {
    @Test
    public void testReferenceCode() {
        /*
         * Tested with respect to pcg_engines::mcg_xsh_rs_64_32 of the C++ implementation.
         * See : http://www.pcg-random.org/download.html#cpp-implementation
         */
        final int[] expectedSequence = {
            0xb786f832, 0x6920834f, 0x5b88b399, 0x6b811447,
            0x91230c70, 0x163c83b5, 0x8dd8bba9, 0xb8bcd10a,
            0xe1964b6e, 0x40b9adc8, 0x75fbee87, 0xed3d1e5c,
            0x82cb437b, 0xea94cea8, 0x76b1726a, 0x9275544a,
            0xed015249, 0x9d46c1cc, 0xe6fddd59, 0x487a0912,
            0xa709c922, 0xd15ac2a2, 0xba36e687, 0x3e40b099,
            0x62ae602c, 0xec0ebb27, 0x94246eda, 0xa40c2daa,
            0xd7e0abb5, 0xf8061587, 0x97f2132a, 0x861cfa5e,
            0xc5b2280b, 0x5fc8ec4e, 0xa9e552ed, 0xbf2ee34f,
            0x0a945eb3, 0x9e578662, 0x292cf72c, 0xc7e04668,
        };
        RandomAssert.assertEquals(expectedSequence, new PcgMcgXshRs32(0x012de1babb3c4104L));
    }
}
