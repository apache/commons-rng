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
         * Reference data from JDK 21:
         * java.util.random.RandomGeneratorFactory.of("L32X64MixRandom").create(seed)
         *
         * Full byte[] seed created using SecureRandom.nextBytes. The seed was converted
         * to int[] by filling the int bits sequentially starting at the most significant
         * byte matching the method used by the JDK, e.g.
         * int[] result = new int[seed.length / Integer.BYTES];
         * ByteBuffer.wrap(seed).order(ByteOrder.BIG_ENDIAN).asIntBuffer().get(result);
         *
         * Note: Seed order: LCG addition; LCG state; XBG state.
         */
        return Stream.of(
            Arguments.of(
                new int[] {
                    0x1382067f, 0x11271e0a, 0x1c409978, 0x61f837c8,
                },
                new int[] {
                    0x34d44b72, 0xc750d8e4, 0x2584b161, 0xe9271d96,
                    0x3f538c0c, 0x4ecdf9e5, 0x8cdd4e81, 0xa6a5535b,
                    0x44879020, 0xcc14afbc, 0x66072188, 0x1c60e8b4,
                    0x9d659300, 0x8b34c635, 0x80487423, 0xa4207e37,
                    0xa8f83eb4, 0xffba89ba, 0x2436ecfd, 0x09d00955,
                    0x4f3e22b9, 0x75fc441a, 0xbae0e225, 0x063ba447,
                    0x73c4496d, 0xf4f0c0d1, 0xa4c4d47e, 0xf5950ecf,
                    0x37c08cae, 0x722310f2, 0xd57ddaed, 0x7810a797,
                    0x3a999e58, 0x38d902dd, 0x267c802a, 0x566b7c3f,
                    0xae527bee, 0x288233c4, 0x5e852aa1, 0xcaaa9452,
                }),
            Arguments.of(
                new int[] {
                    0x8dce7821, 0xd395b8fb, 0x882ad86d, 0x6c42301b,
                },
                new int[] {
                    0xf1e9b7b5, 0x39fd7dc3, 0xc2d24932, 0x9bc0e33c,
                    0x92a9f679, 0xf4b5aaa9, 0x9e2c545d, 0x0d7e55b7,
                    0x4698f8a8, 0x2b73b6a5, 0x4add4011, 0xe9177c21,
                    0x58444b3a, 0xbb61e0bf, 0x18092a31, 0xc89ba939,
                    0xd8538403, 0x1731b7d4, 0x3f625aa5, 0x831d5bd0,
                    0xa752451a, 0xd3e023db, 0x7c6e3e1e, 0xdc0445a3,
                    0xcede9123, 0x2387454a, 0xed63408a, 0x45443d1d,
                    0xa430e2f3, 0x5ae09548, 0xc2b07e8a, 0xbc6165c3,
                    0xdd42ff11, 0x335bd3f2, 0x11914bfa, 0x99e552a0,
                    0x061dab2b, 0x51dfcf44, 0x39686726, 0x851cf916,
                }),
            Arguments.of(
                new int[] {
                    0x2d5f37ab, 0xc3dd28d9, 0x026acc94, 0x2bbc5847,
                },
                new int[] {
                    0x48e4f692, 0xff3c6a41, 0xaf27d478, 0xbb3ef921,
                    0x9c84c6c0, 0x8d5e754d, 0x2c531e3a, 0x9da71b2f,
                    0xfc96959f, 0x63dccae3, 0x06e52284, 0xe88e132b,
                    0xabddb514, 0x4fb33005, 0x8459ab3e, 0xf4f953cc,
                    0xfab97ed8, 0x266fd572, 0x40583e15, 0xe19b5e70,
                    0x2220fa4c, 0xc39f35d0, 0xecb2a582, 0xa0f61e5a,
                    0xbdeb66f3, 0x2009eda4, 0x64af4f0c, 0xa3002588,
                    0x0d540eb8, 0x2e7680a5, 0xbcf8ac04, 0x5c487b38,
                    0x601ef06d, 0xe45bd1ee, 0xb1f7d8d2, 0x91f48864,
                    0x0d39f6df, 0x3584ed75, 0xec73b815, 0xf5a3e5e1,
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
