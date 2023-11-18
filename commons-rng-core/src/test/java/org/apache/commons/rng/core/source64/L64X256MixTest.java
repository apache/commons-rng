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
 * Test for {@link L64X256Mix}.
 */
class L64X256MixTest extends AbstractLXMTest {

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
            return 4;
        }

        @Override
        public LXMGenerator create(long[] seed) {
            return new LXMGenerator(getMix(),
                                    new LCG64(seed[0], seed[1]),
                                    new XBGXoShiRo256(seed[2], seed[3], seed[4], seed[5], false));
        }
    }

    @Override
    LXMGeneratorFactory getFactory() {
        return Factory.INSTANCE;
    }

    @Override
    LongJumpableUniformRandomProvider create(long[] seed) {
        return new L64X256Mix(seed);
    }

    @Override
    Stream<Arguments> getReferenceData() {
        /*
         * Reference data from JDK 19:
         * java.util.random.RandomGeneratorFactory.of("L64X256MixRandom").create(seed)
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
                    0x38ad02a4977cfc85L, 0xac591a702af353a9L, 0xc18ee497ebfa3a67L, 0xc43728a4135dbf81L,
                    0x501873d92a0ffc1eL, 0x21da5d9219a01f1dL,
                },
                new long[] {
                    0x4a8fc9b294a2903bL, 0xe124abe9a71662ecL, 0xfc9ca7d4e1c2b278L, 0x612395c98dfbf451L,
                    0x131004a7cf4033f8L, 0x7d182d5855d1e58eL, 0xba7ccd83d224206dL, 0xc5e270c4ce67cbd3L,
                    0x9123f5d85e0e8746L, 0xc94f94c5880b6537L, 0x827cfda9abc85255L, 0xce74ef6578c18d8cL,
                    0xcf9a3c36d733a4a8L, 0xcf56df43d5ffff86L, 0x9cfb645b362f65a5L, 0xabf301bec6e25ac4L,
                    0xff95d6c7fe60dd77L, 0x69f593186b882b82L, 0x86b35b9cfad7970cL, 0xdbfca7884f3f7f13L,
                    0x2b9eb4307e26e1bbL, 0x31efdc45759d5be5L, 0x28b33884e8407e1dL, 0xec98fe1c2c05f0bcL,
                    0x071969a499ad9c2aL, 0xc85430fc3a9c2649L, 0x978cb0eb9d3a6d16L, 0xc5bf174c7b9aea3bL,
                    0x3da95559360b1c16L, 0x2ef8870aff685d0cL, 0xac7324e58579b175L, 0x6bf70e58e0f2e963L,
                    0xe1114a1cdde29c1eL, 0x1a8b54c562541b08L, 0xa9529fb0c9bb9c3aL, 0x373a68e6f14e6b2dL,
                    0x4e96f36a49fac53cL, 0xeb1fe0f59197fb33L, 0x1e8b4d4daa3f3311L, 0x97ff0d70df1ce349L,
                }),
            Arguments.of(
                new long[] {
                    0x13ca7c4d76bad8f0L, 0x602bf3584414eee4L, 0x3e769cf3ba98660dL, 0x61a95d3d58ed469dL,
                    0x6fc2fc2f5b0921cbL, 0x774d52db376df887L,
                },
                new long[] {
                    0x84c5ab78c40ac14cL, 0x1d616aaae3399f43L, 0xab1a55821b3f4dc1L, 0x8bf9a5c970dbf38bL,
                    0x6fd9452a2dc3da53L, 0x896200532a13b11aL, 0x0c139f2ab4a12593L, 0xb9410309e3750ba9L,
                    0xaa18856fa85517d9L, 0xb36310525d77e950L, 0x0ffa217a0a56025bL, 0x0b948fe6b5d95704L,
                    0x3856404d4cf0ddc8L, 0x7c40f0f8a37b7467L, 0xdee45d9ab52003d0L, 0xfc2d9bd0ea1df302L,
                    0xb9c36154e4c2c927L, 0xc38a3cd9cbd64f86L, 0x80d06dd216397eb2L, 0x94568be37b47a23fL,
                    0xfedaf933d0ff15c2L, 0x207af70fcf265696L, 0x68f117e04406c116L, 0x798da2b4d1d1bc07L,
                    0x664bb84977d2914bL, 0xe95c00e85f03bee5L, 0x2468f9cb7e0c2eefL, 0xa3234bafd0d327cfL,
                    0xa64292c604374ef1L, 0xfde0c87c9e66f469L, 0x040437b68311143dL, 0x793d144161c5a82cL,
                    0xfc8d06776d37b1d1L, 0x643d0d646a26b7a3L, 0xc0cff6fc7f434038L, 0xf8658db199d75217L,
                    0xc34ec05c7964404aL, 0x63e8e02d10e0ca82L, 0x7c18e1e8281ff5acL, 0x82e7a4ef6040d9e1L,
                }),
            Arguments.of(
                new long[] {
                    0x95cef6dd0f721028L, 0x62729b1796f20c67L, 0x3326bb36f96c995fL, 0x7cca88f294ea8828L,
                    0xcd1baff0c8a55c31L, 0xb8b30298127cafc0L,
                },
                new long[] {
                    0x6a6d6b182a973476L, 0xb1c8fe6c748abeb1L, 0x8feda649365ec1a8L, 0xd6fc825213db401eL,
                    0x5abfd540f73ebbe6L, 0x9b5ba437f32b8bd4L, 0xba19cac6c2e0b57dL, 0x81cd25461c2c6869L,
                    0x0a2ca0730e59d25eL, 0xc4a7794c6f1dd5ffL, 0x4acb6cbb120e5980L, 0x27db228fb1e7e793L,
                    0x07857cffa09d2ae4L, 0xb1f1291e2853c4c6L, 0xb3201971b3cb2cc8L, 0xbfb40ab19721f445L,
                    0xb247334ccee1f857L, 0xa13ac1de01fb83b3L, 0x686b00fa750819d5L, 0x2fcd849ed7921c82L,
                    0xda27960205f0704fL, 0xd11a3299f8af4c44L, 0xb1a23ef67d2f567cL, 0xd5ca5a9e0e4977bcL,
                    0x9786640a1bcd0104L, 0x43da7cef69ea3cb3L, 0x3e11acd367e77bdeL, 0xc61349825af6400fL,
                    0xbb6813fb3ce2ef4aL, 0x623c0fbf7b03d348L, 0xf14656bcf51235f1L, 0xd924702149fcd0c1L,
                    0x8d0674793d656071L, 0x9f0520e73fa64387L, 0x9c5abce00783f3faL, 0xe9f65611774feab8L,
                    0x4758118eef111e09L, 0xbfbe22a0b12f1be2L, 0x9ca2ce5f7a74bb75L, 0x80500fe93a8a0938L,
                }));
    }

    @ParameterizedTest
    @MethodSource(value = "getReferenceData")
    void testElementConstructor(long[] seed, long[] expected) {
        final L64X256Mix rng1 = new L64X256Mix(seed);
        final L64X256Mix rng2 = new L64X256Mix(seed[0], seed[1], seed[2], seed[3], seed[4], seed[5]);
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
        final SplittableUniformRandomProvider rng1 = new L64X256Mix(seed);
        final SplittableUniformRandomProvider rng2 = rng1.split(zeroSource);
        RandomAssert.assertNextLongNotEquals(seed.length * 2, rng1, rng2);

        // Since we know how the zero seed is amended
        long z = 0;
        for (int i = Factory.INSTANCE.lcgSeedSize(); i < seed.length; i++) {
            seed[i] = LXMSupport.lea64(z);
            z += LXMSupport.GOLDEN_RATIO_64;
        }
        final SplittableUniformRandomProvider rng3 = new L64X256Mix(seed);
        final SplittableUniformRandomProvider rng4 = rng1.split(zeroSource);
        RandomAssert.assertNextLongEquals(seed.length * 2, rng3, rng4);
    }
}
