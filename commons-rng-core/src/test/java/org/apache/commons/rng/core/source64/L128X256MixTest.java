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
 * Test for {@link L128X256Mix}.
 */
class L128X256MixTest extends AbstractLXMTest {

    /**
     * Factory to create a composite LXM generator that is equivalent
     * to the RNG under test.
     */
    private static class Factory implements LXMGeneratorFactory {
        static final Factory INSTANCE = new Factory();

        @Override
        public int lcgSeedSize() {
            return 4;
        }

        @Override
        public int xbgSeedSize() {
            return 4;
        }

        @Override
        public LXMGenerator create(long[] seed) {
            return new LXMGenerator(getMix(),
                                    new LCG128(seed[0], seed[1], seed[2], seed[3]),
                                    new XBGXoShiRo256(seed[4], seed[5], seed[6], seed[7], false));
        }
    }

    @Override
    LXMGeneratorFactory getFactory() {
        return Factory.INSTANCE;
    }

    @Override
    LongJumpableUniformRandomProvider create(long[] seed) {
        return new L128X256Mix(seed);
    }

    @Override
    Stream<Arguments> getReferenceData() {
        /*
         * Reference data from JDK 17:
         * java.util.random.RandomGeneratorFactory.of("L128X256MixRandom").create(seed)
         * Full seed expanded from a single long value using the same method in the
         * OpenJDK source (v17) and recorded in the order used by the Commons RNG code.
         *
         * Note: This is a different from the other LXMTest instances due to a constructor
         * bug in JDK 17 L128X256MixRandom. The result is an initial state for the LCG
         * that is always 1L.
         * See: https://bugs.java.com/bugdatabase/view_bug.do?bug_id=JDK-8283083
         */
        return Stream.of(
            Arguments.of(
                new long[] {
                    0xaac2f67d761dadc6L, 0xe584ab0136fa95fcL, 0x0000000000000000L, 0x0000000000000001L,
                    0xa56f550e4455ad6bL, 0xfa8c6a4c0b4f87c1L, 0x97aa5a6091689f0cL, 0x97774f9a7b01252aL,
                },
                new long[] {
                    0x0f6839e2df51d066L, 0x63828bfa952b7223L, 0x1449a6518fc6698aL, 0xf0d255739e8a95a9L,
                    0xdc2277916582ab84L, 0x855172869dc4ad71L, 0x9f1e38cd53f3aeceL, 0x8f0cfee1ed210171L,
                    0xdb86b178e98ef8d0L, 0x6e53a3629d1485f7L, 0x033da8ec17c6256cL, 0x94a44e70d72cd494L,
                    0x71a2171e09f1503dL, 0x6355ff323a49300eL, 0x4d4f7e37beec3a76L, 0x5391b119c23afe81L,
                    0x21a28a3f83d71acaL, 0xe157ce29ed8a468dL, 0xfbcbeab9dfa54c0aL, 0x5bd0072c9751b499L,
                    0x1f3ea8c1456a2cd3L, 0x656f6535b5a3d4d4L, 0xba7d0ee7b7cf61b8L, 0x8c26ad19a4660e2aL,
                    0x9ca6f47a205fdcdeL, 0xa2ae34ee95f8d89eL, 0xb6de34b282c7b220L, 0xf8c1d88a8d284430L,
                    0x4df71c7f08c3ad4bL, 0x9c7a7168f01b7905L, 0x93252635246315e0L, 0xcaaa648c929f8b87L,
                    0x9d5f169be9b8050dL, 0xa24bb43e098948daL, 0x47d1147f27e102eeL, 0xcf80915231874ea4L,
                    0xfb5a2832f261afc7L, 0xe4ed459ebe0e8d4dL, 0xa5f9df78cfed42d3L, 0x1cc368014904ab73L,
                }),
            Arguments.of(
                new long[] {
                    0x6e1a741ae7ec03ebL, 0xf5363e7b44211d57L, 0x0000000000000000L, 0x0000000000000001L,
                    0x0114edb0b4bbfa18L, 0x83154f7914d38972L, 0x8d932b636513ae0eL, 0xa0bb24e85d97c9fcL,
                },
                new long[] {
                    0x360b1762f284ac1aL, 0xc7e08eb6c5264259L, 0x60eabd90a111f141L, 0xbd3d95bdc05e96fdL,
                    0xc12366f63a5cb505L, 0x30141aab158cf2deL, 0xefb1f62f804c4c02L, 0x198e4d8fdceed3f7L,
                    0x178f01697a119a29L, 0x5ab2081161a38a08L, 0x0e02978d9d84e577L, 0x17286dbb65ec4b83L,
                    0x15e5ccb7a1f5085aL, 0x11fdb06b66597f8cL, 0x4b8057b570a377c3L, 0xb274608cf0c9ec10L,
                    0x9f220c4df7966a96L, 0x15bc1babf827161dL, 0x7ed132f78f8be153L, 0x38522a2d55b16e76L,
                    0x70f56b472b9a589cL, 0x0c16b3de606a20b3L, 0x1e691d63b1e01619L, 0xe43b1de605c2efd8L,
                    0x6fd7144c0a6b2f7eL, 0x9d2c424422c1e228L, 0xc3fdd9daf170c845L, 0xb5416500b7597222L,
                    0x9d5beb65e35c57a4L, 0x3610afc9f107b341L, 0x26487e11ebcf8709L, 0x7e3f0ea4bbbe0d7bL,
                    0xfeaa2d257997c2edL, 0xcf8974fcac8abcfaL, 0x1ae3d7733f7d9cffL, 0xdeb446665fe7c07eL,
                    0x58a66ab0134febd5L, 0xcda459bb0c024431L, 0x109e6cfa2953268cL, 0xa74609ee30817c67L,
                }),
            Arguments.of(
                new long[] {
                    0x75576ea649c68bdfL, 0x988200da91af3658L, 0x0000000000000000L, 0x0000000000000001L,
                    0xb610c4732da06560L, 0x60dfa764b8ae927dL, 0x24867e0eecc8eb78L, 0x952f3d5dc17c0cf2L,
                },
                new long[] {
                    0x7c2bf5f25669c985L, 0x65f5ce67c1250090L, 0x131fb13c3f2b0af0L, 0xab6f976bc3783faeL,
                    0xbac9b9bac68c1bb2L, 0x969a2438afc9eb6cL, 0x7e652efa93fc93b9L, 0xf1c6ccbd1333ea5bL,
                    0x2a25a2c25ad454e8L, 0x002ed102a1146657L, 0x8f482f8aed7b4b41L, 0x1cc0b65ed2ce02c1L,
                    0xad2c3f04da92abb0L, 0x3783c7d4f5bff2d8L, 0x8f1ddf96128c3d3eL, 0x8e10ceca6da015eeL,
                    0x6bc2a9963a300b32L, 0xe7283e10d87a55eeL, 0x7737c78b6497ed8dL, 0x509181e37cbf2e52L,
                    0x135e66e65ec985baL, 0xb191dd61abb669c7L, 0x82551e94692b0058L, 0x6acab0125b911923L,
                    0x7ad6ebb617bfd61eL, 0xa2854d9a0b1fb89cL, 0xb5088284b426fe15L, 0x74621d1fc29f0ad7L,
                    0xea5cf8ac302b12deL, 0xa8222d387b40a5a7L, 0x2dd23fd77aab83e0L, 0xd88147e1c98e69beL,
                    0x7aaba7d84838fd21L, 0x933440a3dd8ecf5aL, 0xc6fe6e8cdb7d09e7L, 0xa00cab23b7a20207L,
                    0xb769003d4abbef6bL, 0x119e829d8cdb859eL, 0x4cd41960d97c80a6L, 0xfb65ffaaf81144d3L,
                }));
    }

    @ParameterizedTest
    @MethodSource(value = "getReferenceData")
    void testElementConstructor(long[] seed, long[] expected) {
        final L128X256Mix rng1 = new L128X256Mix(seed);
        final L128X256Mix rng2 = new L128X256Mix(seed[0], seed[1], seed[2], seed[3],
                                                 seed[4], seed[5], seed[6], seed[7]);
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
        final SplittableUniformRandomProvider rng1 = new L128X256Mix(seed);
        final SplittableUniformRandomProvider rng2 = rng1.split(zeroSource);
        RandomAssert.assertNextLongNotEquals(seed.length * 2, rng1, rng2);

        // Since we know how the zero seed is amended
        long z = 0;
        for (int i = Factory.INSTANCE.lcgSeedSize(); i < seed.length; i++) {
            seed[i] = LXMSupport.lea64(z);
            z += LXMSupport.GOLDEN_RATIO_64;
        }
        final SplittableUniformRandomProvider rng3 = new L128X256Mix(seed);
        final SplittableUniformRandomProvider rng4 = rng1.split(zeroSource);
        RandomAssert.assertNextLongEquals(seed.length * 2, rng3, rng4);
    }
}
