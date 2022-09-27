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
 * Test for {@link L64X128Mix}.
 */
class L64X128MixTest extends AbstractLXMTest {

    /**
     * Factory to create a composite LXM generator that is equivalent
     * to the RNG under test.
     */
    private static class Factory implements LXMGeneratorFactory {
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
        public LXMGenerator create(long[] seed) {
            return new LXMGenerator(getMix(),
                                    new LCG64(seed[0], seed[1]),
                                    new XBGXoRoShiRo128(seed[2], seed[3], false));
        }
    }

    @Override
    LXMGeneratorFactory getFactory() {
        return Factory.INSTANCE;
    }

    @Override
    LongJumpableUniformRandomProvider create(long[] seed) {
        return new L64X128Mix(seed);
    }

    @Override
    Stream<Arguments> getReferenceData() {
        /*
         * Reference data from JDK 19:
         * java.util.random.RandomGeneratorFactory.of("L64X128MixRandom").create(seed)
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
                    0xa2fc3db3faf20b60L, 0x0ca17f844355c30bL, 0x966393c3c699b9c4L, 0x26d0b369e961d05dL,
                },
                new long[] {
                    0x431e95bfddd868f1L, 0x11d41649fc250a2bL, 0x386107fb4229f3c6L, 0x58809538283ec2beL,
                    0x792d8502b636fa57L, 0xdfdd635a8d4b513bL, 0x639e33d9d46709a4L, 0xac064fae27c58ae2L,
                    0xf503c18f9b81e221L, 0xc8e9239b41d75ac2L, 0x305035c93feb8da0L, 0xcb639286b0b77172L,
                    0x134cb8dea2744fe3L, 0x849f85c7979eaaa8L, 0x7ec51bffd104b0b8L, 0xc9fb22d1f0ce8392L,
                    0xdcbedf16be02f77dL, 0x8779acb6ffd5cb89L, 0x24be000a509e92ebL, 0x73b09559615c1540L,
                    0x25f9fba5d4818f13L, 0x171607e2956fef8dL, 0x8950dc3f40d5f8cfL, 0xd11e7c72b36f1c72L,
                    0xe687a38bb9887225L, 0x078daf1a151ba805L, 0x2aeb69f8638fda77L, 0xff50d66233a5078dL,
                    0x9ed224880f7564a9L, 0x298f140cc162145dL, 0xeb2ba17c17e97b8fL, 0x59d785fd7c80a45bL,
                    0x45afefc04e164b8fL, 0x6282afa6f7a13d16L, 0x59a5232c189d6c95L, 0x7c36546951518872L,
                    0x7973a0bdd8a7d494L, 0x711c54e35cc6d30aL, 0xc931dd22fcc5e6a9L, 0x079e4436817465d5L,
                }),
            Arguments.of(
                new long[] {
                    0x36d2370e6207bab2L, 0xc4753d31834da409L, 0xecef76bb60e6f8daL, 0xb6b63fbeb3710b5eL,
                },
                new long[] {
                    0x4a3b46e4c46330c6L, 0x1edb14ee973103cbL, 0x82ac31b5cfd9b725L, 0xbaa145314d88b5c1L,
                    0xced95922c6f6fdcfL, 0xdba6a91b0fb3ecc1L, 0x3d479c0083bc99e8L, 0x4935d8779f92155cL,
                    0xb375363b0f724142L, 0x2a849a0a4d3328bbL, 0x13d9384867b9e73dL, 0x692a97c3730699c7L,
                    0x1110586b1ba6aaf7L, 0x8b49b5c42008a6b4L, 0xb099f56ded714e73L, 0xe47202801ea2edeeL,
                    0x90a3d0c6c4cd6616L, 0x69e27ce6f839bffeL, 0xd5f1dacba40dfa28L, 0xcbc3b8207d22ab3cL,
                    0xbcfebfe31140ebcfL, 0xc5ade4d27d907a6eL, 0xa4c576fd470abdacL, 0x9a06630004e2434cL,
                    0xe5c91ffe2e649403L, 0x79e8cde35d8a01c3L, 0xfe6e6a2eac80c265L, 0x5a906723692cf576L,
                    0xef004a00e60db421L, 0xa15ee20151bb95f6L, 0x12cb5e8a4c7adaceL, 0x80172ad9c0ed3661L,
                    0x492c31dc6ad30a4dL, 0x0ac761509dd595efL, 0x9349f08282fd74d8L, 0xd2dcbdbb42c1cfddL,
                    0xcc21040c1e9d367bL, 0x62b448f73d61c8a4L, 0x567ef895e846cebdL, 0x8238e9fbccabbb44L,
                }),
            Arguments.of(
                new long[] {
                    0xc705d150b09c1f94L, 0xa828a2ba98ac9c61L, 0x343daac9fd050fdbL, 0xce65de67934aebadL,
                },
                new long[] {
                    0x86cbfd59d51c2b12L, 0x2015e74ab836e71fL, 0x76c59d63993224fbL, 0x5dfd3a311e5df341L,
                    0xeb1654b961c05168L, 0xb0085df8d9127280L, 0xc9ec3f9c1ce14534L, 0x44cae5abad7cc428L,
                    0xaac545985ef4a83dL, 0xbdffb30f032cc9aaL, 0x8309e6733ddef867L, 0x84b62aa79a9de881L,
                    0x8ac5790bda006d61L, 0x2d66e83835309543L, 0x9a69810f00faad5eL, 0xfb52e0c7e2387ac6L,
                    0xb7edafdec4002910L, 0x18b3feeac49b7e4eL, 0x43a4f03145408ca5L, 0x9dc51ffbaca77d03L,
                    0xbb7ad1a943a1daceL, 0xc41841a02d67ceeaL, 0xb88033e5005f190eL, 0x79a40a956ed7d916L,
                    0xc58bad848a86fee9L, 0x3a311ee2f4d9e712L, 0xe4b32160ddf8e17cL, 0x723d7ac41d2496aaL,
                    0x96f96a92d33bc637L, 0x724b68f3e1d969e7L, 0x80edfd99dfc0ef39L, 0x7924f19f4d8bfb4eL,
                    0x4fd956126608af2dL, 0xf81eb1b489993b23L, 0x05d2514191a38239L, 0x819dda6b5dd8eb2bL,
                    0xb31d2eba66041796L, 0x196d3972d36f36b9L, 0x9ee7893afecfd4eeL, 0x2359a24a67a8e98bL,
                }));
    }

    @ParameterizedTest
    @MethodSource(value = "getReferenceData")
    void testElementConstructor(long[] seed, long[] expected) {
        final L64X128Mix rng1 = new L64X128Mix(seed);
        final L64X128Mix rng2 = new L64X128Mix(seed[0], seed[1], seed[2], seed[3]);
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
        final SplittableUniformRandomProvider rng1 = new L64X128Mix(seed);
        final SplittableUniformRandomProvider rng2 = rng1.split(zeroSource);
        RandomAssert.assertNextLongNotEquals(seed.length * 2, rng1, rng2);

        // Since we know how the zero seed is amended
        long z = 0;
        for (int i = Factory.INSTANCE.lcgSeedSize(); i < seed.length; i++) {
            seed[i] = LXMSupport.lea64(z);
            z += LXMSupport.GOLDEN_RATIO_64;
        }
        final SplittableUniformRandomProvider rng3 = new L64X128Mix(seed);
        final SplittableUniformRandomProvider rng4 = rng1.split(zeroSource);
        RandomAssert.assertNextLongEquals(seed.length * 2, rng3, rng4);
    }
}
