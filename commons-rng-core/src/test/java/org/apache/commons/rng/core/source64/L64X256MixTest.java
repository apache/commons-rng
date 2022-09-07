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
    private static class Factory implements LXMGeneratorFactory {
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
         * Reference data from JDK 17:
         * java.util.random.RandomGeneratorFactory.of("L64X256MixRandom").create(seed)
         *
         * Full byte[] seed created using SecureRandom.nextBytes. The seed was converted
         * to long[] by filling the long bits sequentially starting at the most
         * significant byte matching the method used by JDK 17. A sign extension bug in
         * byte[] conversion required all byte indices (i % 8 != 0) to be non-negative.
         * See: https://bugs.java.com/bugdatabase/view_bug.do?bug_id=JDK-8282144
         *
         * Note: Seed order: LCG addition; LCG state; XBG state.
         */
        return Stream.of(
            Arguments.of(
                new long[] {
                    0x2249232b631c5a33L, 0x6231564c7b67752eL,
                    0x5b241a23475c136bL, 0x611f57177c451714L, 0xe51a4670417c4929L, 0x83495b4512573b69L,
                },
                new long[] {
                    0x3b2581b0f7d47a44L, 0xae08c7439c0d9db8L, 0x667d577e8f0e7107L, 0xb127662052ba23dbL,
                    0x283b9f648abff77dL, 0x8a79c3f4079291a7L, 0x0d569424123c789bL, 0x606769bee07389b4L,
                    0x3ae004db1868e7d9L, 0x87cc2ca9fc7652acL, 0xc8f6be5980ad6a86L, 0x9bd9dcdbefb172beL,
                    0x11e9c9fbca8109dcL, 0xd5e93ef6b9b2869fL, 0xafc5ad654ddadfeaL, 0xac02c6fd5734a004L,
                    0x7bb48191736c2ee2L, 0xef3267674750942fL, 0x408d1e4a979ece93L, 0x28df1363bfbdc13cL,
                    0x8e9018d64bb35ccaL, 0x1429a15115d3e447L, 0xa48b9d1f30d5c5dfL, 0x7341fddce66d825fL,
                    0x1dcda0c69b88240fL, 0x5e3e88b0fa0cf62eL, 0xb31b0fd05fb0ceffL, 0x27f6cfa05d602b21L,
                    0x2aa55a37ec2dd1a9L, 0xb9ecd3176de5aba0L, 0x0b60a5d210e80bfeL, 0xec3ae0690b5d36d0L,
                    0xd36827e33c2610afL, 0x443701f20b0b55b9L, 0x9c44bc2e173606efL, 0x682bbfa8c8a76b12L,
                    0xcbb50c8f008184d4L, 0x9a8f4dc4ace9dab7L, 0x635dbc0bba0acf56L, 0x2be7233ba0af3755L,
                }),
            Arguments.of(
                new long[] {
                    0x5a6a3e055b336673L, 0x1c48321c3f450a4bL,
                    0x573d392b6d444672L, 0x391a0b7e7256257eL, 0xe3553c7338183a4bL, 0xb3453524327b474eL,
                },
                new long[] {
                    0x497c9968fb30b55aL, 0x88b701c7c8a5ce87L, 0xc647de6f7c945818L, 0x15455e0da4e225cbL,
                    0x05a8121c5ac13fb6L, 0xc7a2840d44d85358L, 0x035682005f927ac5L, 0x5c02d2bc69806329L,
                    0x357b8b489e7dd1ceL, 0x31a3b0cb1e7ab647L, 0xc430e7726d89bfa5L, 0x7ff2b9985ce138b6L,
                    0xc9d6a64623ceee4eL, 0x66d9f22310afd121L, 0x4947bd5dffa51e31L, 0x37643d00b4886c04L,
                    0x87f629915e305f6fL, 0x219bcd75cbc0285aL, 0xe4c1297b5211a25aL, 0x40e1183164eac5ebL,
                    0x09d9430c9b1b6987L, 0x992314f2bcda24f9L, 0x089ae6cd57c799c2L, 0xa00587bdd329cba0L,
                    0xcb8b06012ff9b1daL, 0x42e9337d8d2cecd4L, 0x614bd447dff91cf6L, 0xa8544ea46a034e04L,
                    0x92ff39c62c2d86efL, 0x2c1906c4b204a841L, 0xaa703346273b7e65L, 0x4c56a1e60ac062e2L,
                    0x699d07dcece2f142L, 0xebec46c7fcf2854cL, 0xd474b44306a4ffa0L, 0xc2ec5ad575f8f2a1L,
                    0x9465c39accfc4119L, 0x3848d247b91666baL, 0x746b4f3707e3d8b7L, 0x95a6a969a95f06bbL,
                }),
            Arguments.of(
                new long[] {
                    0x54400165483f2e3fL, 0x9a15215939207347L,
                    0xf2356277031d321eL, 0x84424261090c5f65L, 0x6971605d2e512524L, 0xda2440670153601fL,
                },
                new long[] {
                    0x8b1d7efc7fc06c1eL, 0x66d5acbd0eb82506L, 0x1ae1b0593b508681L, 0xb5257bc2cd864b10L,
                    0x3b3ce25dba52b1ecL, 0xeba1c30537df5fb8L, 0x6de3e0b4727488f6L, 0x5cc1339198afa780L,
                    0x53d7e6a0df40863dL, 0x29a12cf9852cd019L, 0x5a0063a3c5fb8edcL, 0x83877b4357d7bba9L,
                    0x31927eacbf38d044L, 0x4b5b4edb1239316cL, 0xb2972e6b23a169d2L, 0x8a1b11d5aeb8e987L,
                    0x9a43065171f7e6a3L, 0x0d1429e90174c8d3L, 0x569bbb67381c91faL, 0x54873f0e39ce40d6L,
                    0x069ba230fdb29a2aL, 0xd3cbd59ae9ac652aL, 0xf82cbb27a9150de1L, 0x0013470c201fceaaL,
                    0xf8d1b0b50662e153L, 0x57dcaa907fd596d4L, 0x400e840591ba1d54L, 0x5f7735e9f6904c01L,
                    0x7a34f1dc7b3229a5L, 0x7aa5499d4f712524L, 0x0784bc58e37fec70L, 0x93770a8e29acb2d2L,
                    0x9fcc1756e91b4c28L, 0xacfb91b7aac01d92L, 0x3ed26f0091a74012L, 0x3daffd2afe73d286L,
                    0x4199ca7739b26126L, 0xd96d30747880bb7bL, 0x479432cba5b5f450L, 0x4b0aac6c79480b96L,
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
