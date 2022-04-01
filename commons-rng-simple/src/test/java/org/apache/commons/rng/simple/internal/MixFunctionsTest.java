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
package org.apache.commons.rng.simple.internal;

import java.util.SplittableRandom;
import org.apache.commons.rng.core.source64.SplitMix64;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Tests for {@link MixFunctions}.
 */
class MixFunctionsTest {
    @ParameterizedTest
    @ValueSource(longs = {0, -1, 1, 63812881278371L, -236812734617872L})
    void testStafford13(long seed) {
        // Reference output is from a SplitMix64 generator
        final SplitMix64 rng = new SplitMix64(seed);

        long x = seed;
        for (int i = 0; i < 20; i++) {
            Assertions.assertEquals(rng.nextLong(), MixFunctions.stafford13(x += MixFunctions.GOLDEN_RATIO_64));
        }
    }

    @Test
    void testMurmur3() {
        // Reference output from the c++ function fmix32(uint32_t) in smhasher.
        // https://github.com/aappleby/smhasher/blob/master/src/MurmurHash3.cpp
        final int seedA = 0x012de1ba;
        final int[] valuesA = {
            0x2f66c8b6, 0x256c0269, 0x054ef409, 0x402425ba, 0x78ebf590, 0x76bea1db,
            0x8bf5dcbe, 0x104ecdd4, 0x43cfc87e, 0xa33c7643, 0x4d210f56, 0xfa12093d,
        };

        int x = seedA;
        for (int z : valuesA) {
            Assertions.assertEquals(z, MixFunctions.murmur3(x += MixFunctions.GOLDEN_RATIO_32));
        }

        // Values from a seed of zero
        final int[] values0 = {
            0x92ca2f0e, 0x3cd6e3f3, 0x1b147dcc, 0x4c081dbf, 0x487981ab, 0xdb408c9d,
            0x78bc1b8f, 0xd83072e5, 0x65cbdd54, 0x1f4b8cef, 0x91783bb0, 0x0231739b,
        };

        x = 0;
        for (int z : values0) {
            Assertions.assertEquals(z, MixFunctions.murmur3(x += MixFunctions.GOLDEN_RATIO_32));
        }
    }

    /**
     * Test the reverse of the Stafford 13 mix function.
     */
    @Test
    void testUnmixStafford13() {
        final SplittableRandom rng = new SplittableRandom();
        for (int i = 0; i < 100; i++) {
            final long x = rng.nextLong();
            final long y = MixFunctions.stafford13(x);
            Assertions.assertEquals(x, unmixStafford13(y));
        }
    }

    /**
     * Reverse the Stafford 13 mix function.
     *
     * <p>This can be used to generate specific seed values for a SplitMix-style RNG using
     * the Stafford 13 mix constants, for example in the SeedFactoryTest. It is left in
     * the test source code as a reference to allow computation of test seeds.
     *
     * @param x Argument
     * @return the result
     */
    private static long unmixStafford13(long x) {
        // Multiplicative inverse:
        // https://lemire.me/blog/2017/09/18/computing-the-inverse-of-odd-integers/
        // Invert 0xbf58476d1ce4e5b9L
        final long u1 = 0x96de1b173f119089L;
        // Invert 0x94d049bb133111ebL
        final long u2 = 0x319642b2d24d8ec3L;
        // Inversion of xor right-shift operations exploits the facts:
        // 1. A xor operation can be used to recover itself:
        //   x ^ y = z
        //   z ^ y = x
        //   z ^ x = y
        // 2. During xor right-shift of size n the top n-bits are unchanged.
        // 3. Known values from the top bits can be used to recover the next set of n-bits.
        // Recovery is done by xoring with the shifted argument, then doubling the right shift.
        // This is iterated until all bits are recovered. With initial large shifts only one
        // doubling is required.
        x = x ^ (x >>> 31);
        x ^= x >>> 62;
        x *= u2;
        x = x ^ (x >>> 27);
        x ^= x >>> 54;
        x *= u1;
        x = x ^ (x >>> 30);
        x ^= x >>> 60;
        return x;
    }
}
