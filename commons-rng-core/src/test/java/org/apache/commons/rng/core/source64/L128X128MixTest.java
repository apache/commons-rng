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
 * Test for {@link L128X128Mix}.
 */
class L128X128MixTest extends AbstractLXMTest {

    /**
     * Factory to create a composite LXM generator that is equivalent
     * to the RNG under test.
     */
    private static final class Factory implements LXMGeneratorFactory {
        static final Factory INSTANCE = new Factory();

        @Override
        public int lcgSeedSize() {
            return 4;
        }

        @Override
        public int xbgSeedSize() {
            return 2;
        }

        @Override
        public LXMGenerator create(long[] seed) {
            return new LXMGenerator(getMix(),
                                    new LCG128(seed[0], seed[1], seed[2], seed[3]),
                                    new XBGXoRoShiRo128(seed[4], seed[5], false));
        }
    }

    @Override
    LXMGeneratorFactory getFactory() {
        return Factory.INSTANCE;
    }

    @Override
    LongJumpableUniformRandomProvider create(long[] seed) {
        return new L128X128Mix(seed);
    }

    @Override
    Stream<Arguments> getReferenceData() {
        /*
         * Reference data from JDK 19:
         * java.util.random.RandomGeneratorFactory.of("L128X128MixRandom").create(seed)
         *
         * Full byte[] seed created using SecureRandom.nextBytes. The seed was converted
         * to long[] by filling the long bits sequentially starting at the most significant
         * byte matching the method used by the JDK, e.g.
         * long[] result = new long[seed.length / Long.BYTES];
         * ByteBuffer.wrap(seed).order(ByteOrder.BIG_ENDIAN).asLongBuffer().get(result);
         *
         * Note: Seed order: LCG addition; LCG state; XBG state.
         */
        return Stream.of(
            Arguments.of(
                new long[] {
                    0xf7a78c13fc329c64L, 0xef8c948e0494a150L, 0xac4b477c6908b1bdL, 0x3b98735f99c554c8L,
                    0x702659bd934b4909L, 0xfd71d0bb15bc255dL,
                },
                new long[] {
                    0xb8a3befcc9d12da1L, 0x0aa25f8df2b6d30fL, 0x377d33c3a36a02ebL, 0xd7c7fe74dbc32741L,
                    0x758ee8a262f1a31fL, 0x22d8616b5ffba248L, 0xc63691898f00d2b3L, 0xe730156a52a30750L,
                    0x2bc09f92096f83a5L, 0x788e002353be1ddcL, 0x77b06a59be8cce16L, 0xf999cd4ce13b3604L,
                    0xec9a14c9327ff9e9L, 0x01b27bc5bad453c5L, 0x168617c059484a23L, 0xa14908b9013b0bf4L,
                    0xf86d3652e36ae2fbL, 0x6a6dd1eb42453991L, 0xd1c2a605c8657763L, 0x5eb946fd92b2149eL,
                    0xe2c7bd97792c9a54L, 0x7a1072a987220290L, 0x95509bfdfbe5dfd6L, 0xd773ef0b1a11ea4bL,
                    0x23b820058b149fbcL, 0xfa0c47fb90be1869L, 0xe0deb44ae92cfa4fL, 0x74c3e743907845c3L,
                    0x24d2b874702ce165L, 0x07aaeb42bfa6cdabL, 0xd2c4f8831c34953cL, 0x5864cac991d89733L,
                    0x2d18205aadd00e16L, 0xba6358375a1c78c1L, 0x544e9dc9a710bbf6L, 0x6f43b974aeff36c9L,
                    0x0bc71cf292068347L, 0x2ae0f87e3276731cL, 0x5c8b0866ded3fb5aL, 0x4ef10c7dcae51e25L,
                }),
            Arguments.of(
                new long[] {
                    0x4545d46b1b673815L, 0x0806c96b523a302cL, 0x82f2829259399f1eL, 0x1aaed35434ddf7c3L,
                    0xcf18a9ed54609638L, 0x7340b6851a41457eL,
                },
                new long[] {
                    0x813e05de773584dbL, 0x75fbaf767fd4e4d7L, 0x1160b629ad35e420L, 0xf8850cae917f8dffL,
                    0xe0457f00e2ace49dL, 0xec8d69e7e8ee29bfL, 0x51d9fbf6e213ca8aL, 0x899dba9a21163ce8L,
                    0x2e7b28807b274195L, 0x6aa548f1b93007ceL, 0x1211481ba5634f60L, 0x8d84948c5d7721f9L,
                    0x3934e7b0be28c84cL, 0x22a8842d6e92540cL, 0x4e5543d059af300bL, 0xa3be91fbe4a31b65L,
                    0x8555bcb95a292ae8L, 0xd7c0761ca7f6e51fL, 0x9bc2e7f8bc4a5c35L, 0x6ecba9e5da68d11bL,
                    0xb6dd609ff75da3e6L, 0xb419b5841e8242e3L, 0x10affb57349d69d6L, 0xa2468a067cd111c8L,
                    0xca06bc18b07a23f6L, 0x6c35acec00214ff7L, 0x948af68673ceb096L, 0x3ffaef071fa7d66dL,
                    0xd50d3eb49a99557aL, 0x59a5d62382db6922L, 0x5b2711614a95aa11L, 0x57721649868077bcL,
                    0xc0ab75a00b5c9a54L, 0xd5a855baab2f9e73L, 0xcd0b779a4b00bbfcL, 0x20017121200dc1ddL,
                    0x5054ac4a75515202L, 0x7eff78db2ba3327dL, 0x3d2ddad31a4c073bL, 0xd766756cde66e520L,
                }),
            Arguments.of(
                new long[] {
                    0xca3ad0c9fdb80553L, 0x9a1d5d6b9970d990L, 0x325734fae5d66ea8L, 0x20e5991e069f70dfL,
                    0x97e5e144d0cae5beL, 0xf778cc0ff3bbe5d0L,
                },
                new long[] {
                    0xf4662b85cbe7d457L, 0xe960eff7cce91220L, 0x501e265cfdd3d9ddL, 0x112ecbd65f94d068L,
                    0xaef96e7cb7cef806L, 0xfc6d69cb2f17c7ffL, 0xabbe0e5a0a756ee6L, 0x574210c4bfd26204L,
                    0xd56d82c7d5af5844L, 0x6cba6352115d13b3L, 0x3fb3cc3c4cb673d2L, 0x5b14ce737608823dL,
                    0xc3d042947b4d9d09L, 0x15be86cd65bf7a76L, 0x72896127eb90f17bL, 0xeacd7326935ea13dL,
                    0xfbc85eb3a5cfb667L, 0x298b9763221c4d8fL, 0x284d146047d37be0L, 0xf99aae51c8fe4377L,
                    0xc4dbc790201f47ffL, 0x39e0363f4e66f8b0L, 0x21e0903dbe177124L, 0x4e3ce16e5a9b4a34L,
                    0x502969d272b05192L, 0x55f395dd777066b5L, 0xcaf3bbac8c86090dL, 0x3450b10eff592cf7L,
                    0x8edbbaa7fb43dd23L, 0x8cc4624b087758a2L, 0x4de23a236e734d75L, 0x96f82ab35df96018L,
                    0x744fca3a57366d80L, 0x3c30b7f058c689d6L, 0x54d8c98deccd6bafL, 0xaa31a308717c5a79L,
                    0x00a5cbf1228ff424L, 0x7faf65d94b81faf6L, 0x4d5624366334513eL, 0x1613c5c23ee26a7eL,
                }));
    }

    @ParameterizedTest
    @MethodSource(value = "getReferenceData")
    void testElementConstructor(long[] seed, long[] expected) {
        final L128X128Mix rng1 = new L128X128Mix(seed);
        final L128X128Mix rng2 = new L128X128Mix(seed[0], seed[1], seed[2], seed[3], seed[4], seed[5]);
        RandomAssert.assertNextLongEquals(seed.length * 2, rng1, rng2);
    }

    /**
     * Test split with zero bits from the source. This should be robust to escape the state
     * of all zero bits that will create an invalid state for the xor-based generator (XBG).
     */
    @Test
    void testSplitWithZeroBits() {
        final UniformRandomProvider zeroSource = () -> 0;
        final long[] seed = new long[Factory.INSTANCE.seedSize()];
        // Here we copy the split which sets the LCG increment to odd
        seed[(Factory.INSTANCE.lcgSeedSize() / 2) - 1] = 1;
        final SplittableUniformRandomProvider rng1 = new L128X128Mix(seed);
        final SplittableUniformRandomProvider rng2 = rng1.split(zeroSource);
        RandomAssert.assertNextLongNotEquals(seed.length * 2, rng1, rng2);

        // Since we know how the zero seed is amended
        long z = 0;
        for (int i = Factory.INSTANCE.lcgSeedSize(); i < seed.length; i++) {
            seed[i] = LXMSupport.lea64(z);
            z += LXMSupport.GOLDEN_RATIO_64;
        }
        final SplittableUniformRandomProvider rng3 = new L128X128Mix(seed);
        final SplittableUniformRandomProvider rng4 = rng1.split(zeroSource);
        RandomAssert.assertNextLongEquals(seed.length * 2, rng3, rng4);
    }
}
