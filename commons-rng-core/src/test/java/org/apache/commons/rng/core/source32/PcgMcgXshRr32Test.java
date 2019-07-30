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

public class PcgMcgXshRr32Test {
    @Test
    public void testReferenceCode() {
        /*
         * Tested with respect to pcg_engines::mcg_xsh_rr_64_32 of the C++ implementation.
         * See : http://www.pcg-random.org/download.html#cpp-implementation
         */
        final int[] expectedSequence = {
            0x25bc3e38, 0xb0693d58, 0x155b98f0, 0x047e13d7,
            0xcfb227b3, 0x66601632, 0x71c6e68b, 0x16e2d4a7,
            0x65412358, 0x6d39102c, 0x545cebed, 0x577695ef,
            0xc851c202, 0x743d50b6, 0xe1876a24, 0x274ae9e1,
            0x4087af7b, 0xd4738e89, 0x6ae6e6cf, 0xf8716a43,
            0x933ed380, 0x3edb0d15, 0xa3716e23, 0x2d5f81f2,
            0x5a921ac5, 0x795ec1cf, 0x42595831, 0x55a39a40,
            0x9a21afda, 0xc03fa331, 0x9192fd98, 0x87eb7041,
            0x2d9e338e, 0xf924d873, 0xf8c6a7a7, 0x2dfe78bf,
            0xd443c0a9, 0xe567f8ed, 0xa4e09491, 0x3c91d8fd,
        };
        RandomAssert.assertEquals(expectedSequence, new PcgMcgXshRr32(0x012de1babb3c4104L));
    }
}
