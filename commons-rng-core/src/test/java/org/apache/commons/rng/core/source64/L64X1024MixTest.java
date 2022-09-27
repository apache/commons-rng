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

import java.util.Arrays;
import java.util.stream.Stream;
import org.apache.commons.rng.LongJumpableUniformRandomProvider;
import org.apache.commons.rng.SplittableUniformRandomProvider;
import org.apache.commons.rng.UniformRandomProvider;
import org.apache.commons.rng.core.RandomAssert;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.provider.Arguments;

/**
 * Test for {@link L64X1024Mix}.
 */
class L64X1024MixTest extends AbstractLXMTest {

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
            return 16;
        }

        @Override
        public LXMGenerator create(long[] seed) {
            return new LXMGenerator(getMix(),
                                    new LCG64(seed[0], seed[1]),
                                    new XBGXoRoShiRo1024(Arrays.copyOfRange(seed, 2, 18), false));
        }
    }

    @Override
    LXMGeneratorFactory getFactory() {
        return Factory.INSTANCE;
    }

    @Override
    LongJumpableUniformRandomProvider create(long[] seed) {
        return new L64X1024Mix(seed);
    }

    @Override
    Stream<Arguments> getReferenceData() {
        /*
         * Reference data from JDK 19:
         * java.util.random.RandomGeneratorFactory.of("L64X1024MixRandom").create(seed)
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
                    0x6d03aad14e58f8beL, 0xd4c213de342c1162L, 0xd597eafd698d6175L, 0xe97dcf3d06517a81L,
                    0x8fea00579bc8811fL, 0x6309bcb2aea9a4deL, 0x7ac44e80035abac9L, 0xa675b5664574acfaL,
                    0x6f0b919d4411bd4bL, 0xc4385eca083e11f4L, 0xae4da78f6ee015d7L, 0x5ed8e1cfcf602e2cL,
                    0x9adebb2f8ee338e8L, 0x7c42477aca7bf3e5L, 0xd5c934704ecce9c8L, 0xa7fcd1cc6f3cc3d9L,
                    0x4fcda8f3c0f0e9b9L, 0xf5a0bef115769b05L,
                },
                new long[] {
                    0xdf77d6d9db757098L, 0x0769611d12b166c7L, 0x708c4e0c0b172d54L, 0x85a2116d903b3110L,
                    0x3907a7d0cd664028L, 0xab1cf1a27126232aL, 0xf5a0ffc53b0e881cL, 0x0dc4aacd8d4ab80fL,
                    0x6378f240f93e410eL, 0xf4b3f7453749afbbL, 0xda50ef44cffd1ac0L, 0xe4b8986625162c7bL,
                    0x9cf06807071e3298L, 0x5601d712f9216a74L, 0xe5f63284ff4cdb0eL, 0xa4f750918672c1f1L,
                    0x0c282fc046e88796L, 0x02a3c158d433bb9cL, 0x3a4a103208803a12L, 0x3c7c9892ecbe1435L,
                    0x53fe51d586d0a9c9L, 0x0b9d8f499ac3b7adL, 0xa60744cd33c8237eL, 0xfcd11a8401f66410L,
                    0x27a1a8ef0083950eL, 0x83a43fca0b04f357L, 0xea1a6ad13d53f526L, 0xd46548f3c38cbfa8L,
                    0x66510fffc2efd464L, 0x0fa0ea2a25a26e12L, 0x95b947b27fc1e38dL, 0xae48ee91936b4a0cL,
                    0x3b6a6fe3aa245f36L, 0x4d172e1c972b6029L, 0xf56228f6cfaa19f8L, 0x132c2d231740e656L,
                    0x7b414119880f0602L, 0x7b04530dee6f280dL, 0x570efef48afa46c1L, 0x1526790b50b1c5f0L,
                }),
            Arguments.of(
                new long[] {
                    0x4e5d2758950ce2f6L, 0x26eb64e6f6ce2f4cL, 0x67dbaeb866eb5146L, 0x2e53f5d363ccc0caL,
                    0xb082d1f77b99b907L, 0x3d739ee76d3218a5L, 0x2e94a38e16245b39L, 0x13412e76abeca10cL,
                    0x5e5454bc36fbec04L, 0x1bb9d711b459e3c6L, 0x21e532cad599a4b6L, 0xee674b9fa2375928L,
                    0xd4142c6ed43d6ad9L, 0x90fb3eb966686cbdL, 0x668de128668949acL, 0xf90dd2df394ec382L,
                    0x912c0e71f4ed3031L, 0xf0c5b9f6cce6f885L,
                },
                new long[] {
                    0xe6f95d595a4af8f8L, 0x0c17ba67cad911c8L, 0x404d3881e064f874L, 0x8d327b13d44d02ddL,
                    0xbb2d324c29f355ddL, 0x50330bbc22e58c05L, 0x273b48550711ac35L, 0x52c7c59b6e5eb50dL,
                    0xc569a2c2b5b750b4L, 0x48faa3df570c128bL, 0xa23d12062036f287L, 0xd9503f17c8551f7cL,
                    0x0981a8e88e96ed3cL, 0x02334ba7bd2c7a50L, 0xc2130c5adb86f369L, 0x752eb27167b77d0cL,
                    0xbdf62fff413892b4L, 0x4fc6ce43dbb707baL, 0x2be070d3f3ac6a21L, 0x92ba5a0c83bf80f1L,
                    0xfcfd5662258e8ff9L, 0xe24f65da07bbd2b5L, 0x76c40dd4fd3c0f46L, 0x6a89d17f8ce1591bL,
                    0x8e4aab3bcf6e44d4L, 0x75af39a30c384e90L, 0x6cbe6c1c78cf01c9L, 0x5843c4c0cdd76fbeL,
                    0x23e4b707d2e89314L, 0xbb5d3050cdc05ddbL, 0x7856e971b410dabcL, 0xc525d7da4da96e56L,
                    0x6cac7c7d4baca7e5L, 0x03e6c6839e74d3aaL, 0x0ac2f86811ef3634L, 0xc958a175552a7795L,
                    0xbe2887523e1c4024L, 0x57fc2e99d06003c9L, 0xd6ad5ef42ecbb49bL, 0xa3b621ec83b1871fL,
                }),
            Arguments.of(
                new long[] {
                    0x2adf28f2d3abfe32L, 0xc30b0861ed8bd4c4L, 0x282546a94af8b08fL, 0xbfba98ae6a0461c3L,
                    0xba8d7237f5194544L, 0x030e02b1530cf49eL, 0x7da2ef20c7469ecdL, 0x449858a06567c5feL,
                    0x5efd6bd4c4f2b24aL, 0x6ad10da447eb6b22L, 0x411402a4cf9eeeffL, 0x16d11360e0712af1L,
                    0xb1fa3007f9f4588aL, 0x5054dcb198cd5032L, 0x73b7545983150dcdL, 0x8f61e32fb7b252ffL,
                    0x51ca7eec0f4430efL, 0x24d1f99aabf6a75cL,
                },
                new long[] {
                    0x6e7ff191c0830aacL, 0x4b3752f3792d8ec8L, 0xcda3452b26f49744L, 0x72790a252653f225L,
                    0x9f8ea6be4ff231efL, 0x2a95cbe1c40b0fe9L, 0xd8e7239fe90daa8dL, 0x8828dc21a1ec9635L,
                    0x3b0f343e895c642bL, 0x8b935a728e21d134L, 0xdec84687f9fedec0L, 0xfbf6be15ef1f2b97L,
                    0x9a46a2ddbbf6d697L, 0x0bb2e54396b32d7cL, 0x80dbac0072f34633L, 0x343fee693c38db85L,
                    0x14f6fa0287518153L, 0xc09a84f9a1b26197L, 0x09b257e0d0be4784L, 0xd13870d9410703c6L,
                    0x4a5428ab4f0a16edL, 0xe089e6e6cf4ff84aL, 0x41473f06b79df0f3L, 0xc8620e2f443fd831L,
                    0x170e9539f618fd88L, 0x8eb8e70ce5b18844L, 0xbd429d12f6886994L, 0xa50726552d16cd8dL,
                    0xe6534e392bef0bf2L, 0x77e41ca7e1e3bdc1L, 0x08d24ebe01774c6dL, 0x42aae0536ce8daaeL,
                    0x4d2790f2f934b5e0L, 0x9f3102e25874a2c4L, 0x4d7a5c745eba29bdL, 0xdd951065bc1b84b7L,
                    0x9ba99966e4e55ae5L, 0x24c22788322d0a63L, 0x7e4553bacad3a606L, 0x717d7d11b6610246L,
                }));
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
        final SplittableUniformRandomProvider rng1 = new L64X1024Mix(seed);
        final SplittableUniformRandomProvider rng2 = rng1.split(zeroSource);
        RandomAssert.assertNextLongNotEquals(seed.length * 2, rng1, rng2);

        // Since we know how the zero seed is amended
        long z = 0;
        for (int i = Factory.INSTANCE.lcgSeedSize(); i < seed.length; i++) {
            seed[i] = LXMSupport.lea64(z);
            z += LXMSupport.GOLDEN_RATIO_64;
        }
        final SplittableUniformRandomProvider rng3 = new L64X1024Mix(seed);
        final SplittableUniformRandomProvider rng4 = rng1.split(zeroSource);
        RandomAssert.assertNextLongEquals(seed.length * 2, rng3, rng4);
    }
}
