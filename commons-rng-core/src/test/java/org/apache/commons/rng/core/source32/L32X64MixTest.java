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

import java.util.stream.Stream;
import org.apache.commons.rng.LongJumpableUniformRandomProvider;
import org.apache.commons.rng.SplittableUniformRandomProvider;
import org.apache.commons.rng.UniformRandomProvider;
import org.apache.commons.rng.core.RandomAssert;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Test for {@link L32X64Mix}.
 */
class L32X64MixTest extends AbstractLXMTest {

    /**
     * Factory to create a composite LXM generator that is equivalent
     * to the RNG under test.
     */
    private static final class Factory implements LXMGeneratorFactory {
        static final Factory INSTANCE = new Factory();

        @Override
        public int lcgSeedSize() {
            return 2;
        }

        @Override
        public int xbgSeedSize() {
            return 2;
        }

        @Override
        public LXMGenerator create(int[] seed) {
            return new LXMGenerator(getMix(),
                                    new LCG32(seed[0], seed[1]),
                                    new XBGXoRoShiRo64(seed[2], seed[3]));
        }
    }

    @Override
    LXMGeneratorFactory getFactory() {
        return Factory.INSTANCE;
    }

    @Override
    LongJumpableUniformRandomProvider create(int[] seed) {
        return new L32X64Mix(seed);
    }

    @Override
    Stream<Arguments> getReferenceData() {
        /*
         * Reference data from JDK 17:
         * java.util.random.RandomGeneratorFactory.of("L32X64MixRandom").create(seed)
         *
         * Full byte[] seed created using SecureRandom.nextBytes. The seed was converted
         * to int[] by filling the long bits sequentially starting at the most
         * significant byte matching the method used by JDK 17. A sign extension bug in
         * byte[] conversion required all byte indices (i % 4 != 0) to be non-negative.
         * See: https://bugs.java.com/bugdatabase/view_bug.do?bug_id=JDK-8282144
         *
         * Note: Seed order: LCG addition; LCG state; XBG state.
         */
        return Stream.of(
            Arguments.of(
                new int[] {
                    0x5a16253f, 0xd449657e, 0x5b46012d, 0x1d504d64,
                },
                new int[] {
                    0xa6cc8f9b, 0xb9d3f0e3, 0xb8861d42, 0x9f8001a2,
                    0xaf1eea5b, 0x4e3bc947, 0x1c6378b8, 0x54ccc942,
                    0x4629da71, 0x0126a7e7, 0x6038f89c, 0xff72cb5c,
                    0x3853ae58, 0xc86ccf16, 0x02d32147, 0x78afed22,
                    0x432194a4, 0xf79809aa, 0x8c115507, 0x646b0445,
                    0xed63cc9c, 0x4e1d7b55, 0x699cf21d, 0x7d2adf74,
                    0xc2613634, 0x5ac9c96f, 0x1f2e59d2, 0x4c9878fb,
                    0x49c7a137, 0xd277cdf8, 0x369dd190, 0x1edeb2dc,
                    0x10bd09bd, 0x86e5140c, 0xe6bcb04c, 0x67f97775,
                    0x8f979b43, 0x36dfa266, 0x3acf5157, 0xe69aac08,
                }),
            Arguments.of(
                new int[] {
                    0xa3512c5b, 0x970f3100, 0xfc22313b, 0xa71f183a,
                },
                new int[] {
                    0x971dc6d7, 0xd8200b05, 0x0093d467, 0x74f11b96,
                    0x8945aa8f, 0x88a3d635, 0xb1fcb212, 0x66ed8543,
                    0xd5970056, 0x7b8b2d89, 0xd2d3957b, 0xc4ecc777,
                    0xf8e295c3, 0x4ffb4a29, 0x509fc6b0, 0x9c5b1965,
                    0x5008c757, 0x3a19410a, 0xc3992498, 0x0c4a8f22,
                    0x3f6abfd8, 0xe943ac45, 0xb632fcd3, 0x3d1f3201,
                    0xb17bc9f5, 0x902a9b9c, 0x39a76388, 0xd8c856a0,
                    0x7422cf86, 0x3f776592, 0xef70a122, 0x053b628e,
                    0x0dec8c4e, 0x5b0d80c9, 0xf58d19fc, 0x741f1361,
                    0x01b747f7, 0x6b325a32, 0x714ed761, 0xf1facf8e,
                }),
            Arguments.of(
                new int[] {
                    0x17767167, 0x1b6e286c, 0xe84a2f12, 0x637a5034,
                },
                new int[] {
                    0xd69fd4c1, 0x134945f8, 0x34c2fd4e, 0xd152b08f,
                    0xb5a7e2bd, 0xd8afab0d, 0xed15f9d2, 0x6baba06e,
                    0x9a1e8bcd, 0x8b392fbd, 0xb1e1cad9, 0x929f06d9,
                    0xc9467860, 0x3d492690, 0x84fd7d06, 0xb576b0f9,
                    0x860eac4c, 0xd3b7aa5b, 0x3ff4dd13, 0xfea00d01,
                    0x025143a0, 0xf1c5885a, 0x3041a8be, 0xbfbfb908,
                    0x186cd9cc, 0x1771875b, 0x9cc65fe2, 0xc9189702,
                    0x6063b09d, 0x20f0286b, 0x6094b83d, 0x8c3fe8f5,
                    0x2bf720eb, 0xe229e207, 0xe33f93db, 0x443b3849,
                    0x79cc70a8, 0xcbf0de84, 0xe57c6367, 0xe555ee21,
                }));
    }

    @ParameterizedTest
    @MethodSource(value = "getReferenceData")
    void testElementConstructor(int[] seed, int[] expected) {
        final L32X64Mix rng1 = new L32X64Mix(seed);
        final L32X64Mix rng2 = new L32X64Mix(seed[0], seed[1], seed[2], seed[3]);
        RandomAssert.assertNextLongEquals(seed.length * 2, rng1, rng2);
    }

    /**
     * Test split with zero bits from the source. This should be robust to escape the state
     * of all zero bits that will create an invalid state for the xor-based generator (XBG).
     */
    @Test
    void testSplitWithZeroBits() {
        final UniformRandomProvider zeroSource = () -> 0;
        final int[] seed = new int[Factory.INSTANCE.seedSize()];
        // Here we copy the split which sets the LCG increment to odd
        seed[(Factory.INSTANCE.lcgSeedSize() / 2) - 1] = 1;
        final SplittableUniformRandomProvider rng1 = new L32X64Mix(seed);
        final SplittableUniformRandomProvider rng2 = rng1.split(zeroSource);
        RandomAssert.assertNextIntNotEquals(seed.length * 2, rng1, rng2);

        // Since we know how the zero seed is amended
        int z = 0;
        for (int i = Factory.INSTANCE.lcgSeedSize(); i < seed.length; i++) {
            seed[i] = LXMSupport.lea32(z);
            z += LXMSupport.GOLDEN_RATIO_32;
        }
        final SplittableUniformRandomProvider rng3 = new L32X64Mix(seed);
        final SplittableUniformRandomProvider rng4 = rng1.split(zeroSource);
        RandomAssert.assertNextIntEquals(seed.length * 2, rng3, rng4);
    }
}
