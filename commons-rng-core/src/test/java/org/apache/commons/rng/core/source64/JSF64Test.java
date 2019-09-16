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

public class JSF64Test {
    @Test
    public void testReferenceCode() {
        /*
         * Tested with respect to the original reference:
         * See : https://burtleburtle.net/bob/rand/smallprng.html
         */
        final long[] expectedSequence = {
            0xb2eb2f629a2818c2L, 0xe6c4df3bd8e4a0c8L, 0x2b3ab71e4e888b46L, 0x12a6088f5960738dL,
            0x95715b21fcb1a7d9L, 0x7acafc3916723b0fL, 0x3a0c5f8c4caff822L, 0x9b47b7a1e9784699L,
            0x9c399839261a024fL, 0x56a2fa6eaa7a62aaL, 0xca6995ea5baeb8daL, 0x56cad0c4dee9cbb9L,
            0xbb5df57850f117a5L, 0x147a41dad6a87b7bL, 0xf9225f2aa6485812L, 0x812b9d2c9b99aaa0L,
            0x266ad947cac0acfcL, 0x19bcfc1b69831866L, 0xc5486e1cfa0eca28L, 0x80ca1802e7dd04b7L,
            0x003addd1e44ff095L, 0xb9eaa245ce7c040bL, 0xe607e64b31a6e9b4L, 0x1553718b8013007bL,
            0x86dcd29120fd807bL, 0xeb5b8ec5d73dc39eL, 0x3c26147f6b7ff7d7L, 0xe0b994497bf55bb5L,
            0x24fb3dc33de779c6L, 0x022aba70fc48e04aL, 0xbcf938e19b81f27fL, 0x9022bd08a8ac7511L,
            0x79ad91f7404ecef1L, 0x291858706a2286dbL, 0xf395681f493eb602L, 0xf85ed536da160b93L,
            0x5dd685454dd0d913L, 0x150e7b8f99b10f7dL, 0xcd1c0b519cc69c05L, 0xca92e08bf2676077L,
        };
        RandomAssert.assertEquals(expectedSequence, new JSF64(0x012de1babb3c4104L));
    }
}
