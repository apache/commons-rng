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
         * Tested with respect to pcg_engines::setseq_rxs_m_xs_64_64(x, y) of the C++ implementation.
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

    @Test
    public void testReferenceCodeFixedIncrement() {
        /*
         * Tested with respect to pcg_engines::setseq_rxs_m_xs_64_64(x) of the C++ implementation.
         * See : http://www.pcg-random.org/download.html#cpp-implementation
         */
        final long[] expectedSequence = {
            0xa5ace6c92c5fa6c7L, 0xac02118387228764L, 0xa6e796e49dc36e00L, 0x4713f32552134368L,
            0xa2ad36cb4e6b7cc9L, 0x6bbce7db898fa11dL, 0x134cb18300fe9eb0L, 0x3f705c0d635cbc23L,
            0x4bd7531b62a59b62L, 0x413cc95f3c3e9952L, 0xbc77749b270d987cL, 0xd2c74089bc6489f5L,
            0xc2debc07a31bb1a8L, 0x5163cfcc77ebd4fbL, 0x6f41b5621cba1b2dL, 0x72dd618ae82f792fL,
            0x76888898287eeaa2L, 0xf5c7de46ad2739a0L, 0xc9d63bfe7b405a66L, 0xefc0161a3119efd0L,
            0xbc7a7e23220b53c8L, 0x6efb5e3e2b510988L, 0xe70ce3d64ed4ee82L, 0x3e5c15687252a94dL,
            0x95530066e3a7f3a6L, 0x6c9a303ab74d9a21L, 0x93ff7e36cf46cdeaL, 0xd5173d3428745856L,
            0x4fb30e4c6e8bf68eL, 0x6466bbcaf078ad4fL, 0x846768c1bd451c96L, 0xd9c7d6b4aabce95dL,
            0x4f789941d453a26fL, 0x802e40798afb9cf9L, 0x4f9fd27240f2303bL, 0xb9c25cd3e029e4eaL,
            0x7d8ecedb3334b077L, 0x9011d404cf44b7c7L, 0xe6f26367c52b12a6L, 0xcbfb6a1dd20f2df4L,
        };
        RandomAssert.assertEquals(expectedSequence, new PcgRxsMXs64(0x012de1babb3c4104L));
    }
}
