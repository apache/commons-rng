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

public class SFC64Test {
    @Test
    public void testReferenceCode() {
        /*
         * Tested with respect to PractRand::RNGs::Raw::sfc64 of the C++ implementation (v0.94).
         * See : http://pracrand.sourceforge.net/
         */
        final long[] expectedSequence = {
            0x383be11f844db7f4L, 0x563e7e24056ad886L, 0x959e56afde1c3f72L, 0x7924b83a8ac40b01L,
            0xe3096acc85876ae6L, 0x9932c32968faf17eL, 0x5df8e164496c717bL, 0x443e63b0f0636d11L,
            0xa0c1255bd56fb4ceL, 0xe9b12d67fbae4394L, 0x87a6b8f68968124bL, 0xe7a29a2c9eb466b6L,
            0xcfbcb67cac7ffb22L, 0x9a77f8d8860be8e5L, 0x51c287e3d450bf11L, 0x9518d0a2cd3f16a3L,
            0x36fdfd2044cbbb67L, 0x94d6e5b7e50ed797L, 0x01c80459dcc9ba5eL, 0x913aa13874b1da2aL,
            0x136f9eb31f816b8dL, 0xbb68f2aba658e9f5L, 0x455f38462bb2e598L, 0x216693ead3d4036dL,
            0x2e697d6093522eeeL, 0x8aa3e5e922c68cecL, 0x55f38b99e6e9fadcL, 0xc3b18937baf48d2fL,
            0xd3a84a0f0781ef03L, 0x0374b8766ea7b9a7L, 0x354736eb92044fc2L, 0x7e78cca53d9bb584L,
            0x6b44e298f16ca140L, 0xf1c7b84c51d8b1d8L, 0x0bee55dd0ea4439dL, 0xd9a26515c0a88471L,
            0xda4c3174cafc57f8L, 0x6193f4b96362eb4bL, 0x207e9a94b58041afL, 0x5451bd65c481d8fcL,
        };
        RandomAssert.assertEquals(expectedSequence, new SFC64(new long[] {
            0x012de1babb3c4104L, 0xc8161b4202294965L, 0xb5ad4eceda1ce2a9L
        }));
    }
}
