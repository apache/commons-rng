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

public class PcgRxsMXs64Test {
    @Test
    public void testReferenceCode() {
        /*
         * Tested with respect to pcg_engines::setseq_rxs_m_xs_64_64 of the C++ implementation.
         * See : http://www.pcg-random.org/download.html#cpp-implementation
         */
        final long[] expectedSequence = {
            0xc147f2291fa40ccfL, 0x8edbcbf8a5f49877L, 0x61e05a1d5213f0b4L, 0xc039f9369032e638L,
            0x95146e605b2e4a96L, 0x5480af6332262d03L, 0x7cbfb3a67a714557L, 0x5c9f0a25eba41575L,
            0x6e23dba403318decL, 0x7b230e581b829dbcL, 0x0617d61457cce844L, 0x661c9bd85d60eb09L,
            0x48acc16a0113d8f1L, 0xd7c6a4b516ccf126L, 0x85e72ab4e1819cfbL, 0x578cfcc9d7f55036L,
            0xcfc87f2b332b581dL, 0x50db098980bb8bf4L, 0xe50bab9f780884d5L, 0xbcd91f1aa5240febL,
            0xf45c3398207f4f55L, 0x390a6132c0e31e56L, 0x0594d18864e34a6aL, 0x10268ae6df979e24L,
            0x69bc35b3539195eeL, 0x4931c30cf8c9342fL, 0x63bc344007b30e4aL, 0x6da4c43e42bdf49eL,
            0x9a52bbf48ebbcd52L, 0xd7e0040b82efa84fL, 0xd601c8e7917a1db9L, 0xb905ecaf6864e799L,
            0x32877df7625ae7b5L, 0xa3dc41742161f87dL, 0x9556e15438c1aca1L, 0xb2c890ac0e32cd37L,
            0xf1d53427ff980d09L, 0x0e227593be626d22L, 0x0fcbdacbf19d6ae1L, 0xe425b9f0345bd813L,
        };
        RandomAssert.assertEquals(expectedSequence, new PcgRxsMXs64(new long[] {
            0x012de1babb3c4104L, 0xc8161b4202294965L
        }));
    }
}
