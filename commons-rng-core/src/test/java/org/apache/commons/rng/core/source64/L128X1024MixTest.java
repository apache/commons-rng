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
 * Test for {@link L128X1024Mix}.
 */
class L128X1024MixTest extends AbstractLXMTest {

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
            return 16;
        }

        @Override
        public LXMGenerator create(long[] seed) {
            return new LXMGenerator(getMix(),
                                    new LCG128(seed[0], seed[1], seed[2], seed[3]),
                                    new XBGXoRoShiRo1024(Arrays.copyOfRange(seed, 4, 20), false));
        }
    }

    @Override
    LXMGeneratorFactory getFactory() {
        return Factory.INSTANCE;
    }

    @Override
    LongJumpableUniformRandomProvider create(long[] seed) {
        return new L128X1024Mix(seed);
    }

    @Override
    Stream<Arguments> getReferenceData() {
        /*
         * Reference data from JDK 19:
         * java.util.random.RandomGeneratorFactory.of("L128X1024MixRandom").create(seed)
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
                    0x6b58aa6ffc337cacL, 0x7554ffc31477c792L, 0xadb09543e412c559L, 0xaec37b44eedbebebL,
                    0x1b5f8ce14fd365aaL, 0xdd9583b20d633d9aL, 0x047b8ee58629d33dL, 0x1455a9f0a1b5072cL,
                    0x22176a92ad7fc152L, 0xdd5787b7217de80fL, 0xf2a39d303e489c65L, 0xc639427ce47fdf8fL,
                    0x719635b081f3f914L, 0xa697379a497e7e39L, 0x9e7ae3aa56a287ccL, 0x8b4abd91a352faa5L,
                    0x7b5f405155546a12L, 0x5b4043b9f1140d8fL, 0x2cb19f45483f80fbL, 0x0b6e5226e1c85467L,
                },
                new long[] {
                    0x5fa2cf23d60cf67cL, 0xc31c2b6451aa2fbaL, 0xd2af323c077ee4d4L, 0x213bed298b3ad585L,
                    0xdcce8c3e6f63d6c6L, 0x3dfc05a23b884532L, 0xbcbfe3860bef2eaaL, 0xf656a285e4ebbce1L,
                    0x8f9505806a414372L, 0xbcff459696a30316L, 0xa700a8d63166a0aeL, 0x6407b055a4f0b3c7L,
                    0xe77333b14d15309aL, 0x70a9155fd1c1961bL, 0xdfc035a8c748ae7eL, 0x534f5830fe5297caL,
                    0xca187d9d2eab2437L, 0xcc3a1e20a36fa6abL, 0xf8f4960737866c0cL, 0x7eab49fd524b840dL,
                    0x9937413f08979628L, 0x017565584f1da654L, 0x13c76265c74cc0a6L, 0x54de60aea30cedecL,
                    0xe80ad8897a74b5a8L, 0xcd1555f1abbf30acL, 0x015d519e2a4c7d53L, 0xad32c13b44c39ffbL,
                    0xcd6a12650b3753f8L, 0x1486ea548da30363L, 0x98987c807f9660d0L, 0x6bb14b89a9643040L,
                    0x8ee4d97ec0bced99L, 0x3b9e0ee8a39a959aL, 0x5565cd513ab34ef9L, 0xd81ed95e235db404L,
                    0x9167e57917401421L, 0x30808415959689a4L, 0x1b868f963e2be44cL, 0x70f98922267d2397L,
                }),
            Arguments.of(
                new long[] {
                    0x72831df890850049L, 0x14d5fad4e42f9302L, 0xde0b08b519b5ed35L, 0xb074db75c9a488eeL,
                    0x9b32cb087f31bb65L, 0xc47709977d2f66bdL, 0x801689b1d45ccec6L, 0x17b9a964bd7c2914L,
                    0xf4691614d2280435L, 0x8e891da749b9afc6L, 0xe452871043428ec9L, 0x3412688e450d367cL,
                    0xb47eaadd6c656be4L, 0x4e4c09a9d9055ec9L, 0x60e549d120c759caL, 0xb1e0ebc749dc7df4L,
                    0xf479ceebad6b798aL, 0xb71d9e212eb1d1c1L, 0x28f0025d7748addaL, 0x6063774bbe188331L,
                },
                new long[] {
                    0x237759030495d6bdL, 0xa1085516169c5723L, 0xf2938d898f81a4dcL, 0x3f2472fa6229549fL,
                    0x7b5c7d86db5cd897L, 0x2a3e9a1a243ab91fL, 0xad94f5d540a46bccL, 0x7cf02d130867296eL,
                    0x9ad5f8a5e883b2b2L, 0xc37ac97fff9ba5bdL, 0xf1d836c448e74e62L, 0xdface229fa5babe0L,
                    0x83fac2cce3a51eeeL, 0xfde459054fb9a650L, 0xf2289c9c7a3ce827L, 0x6096deea7085c16fL,
                    0xa7542a7e16638f15L, 0x0f38a3642d12078aL, 0x46f08e525547a329L, 0x6ffec9295514502cL,
                    0xef0ae056313ac76aL, 0x0972748d1e10ce75L, 0x45d76330939da7abL, 0x49a51c499b44c48fL,
                    0xcda6ac64fcb7e891L, 0xe08859838fb0fec5L, 0xbd1e49427e9460d3L, 0x69ab2b69ce6a6aacL,
                    0xc4cf49487adaf18fL, 0xf18641ff23c6d099L, 0xe25d8d4195a84f10L, 0x85602ba0e16337cfL,
                    0x118f4640b8f3550bL, 0xae642cb3a6d01717L, 0x5d3f27a3d6534f66L, 0xbf4ed49c67cd4a4cL,
                    0x1400824879988551L, 0xad2a6637c4512f53L, 0xf424063005575699L, 0xb29f63cbd1acb668L,
                }),
            Arguments.of(
                new long[] {
                    0x1362c961e1630fbeL, 0xa15f80cdcb56460fL, 0x54a55905815c1d21L, 0x3ae5f0c7bb27dddcL,
                    0x829c93e6c7c7025bL, 0xb0213f4c376814c1L, 0xba8cbf51f44bb2a5L, 0xeb34355868993b1fL,
                    0x08f3cda12137f730L, 0x4059c90010dc3b11L, 0xf7268debee731db0L, 0xba1e6900e52effb0L,
                    0x9901ff51f11fa35bL, 0x28d5e83d8aee75ceL, 0xf38c8641eb866c6dL, 0x9cfa85edc14efaa0L,
                    0x9e55a1737705d52bL, 0x15a494ad0cf92e68L, 0x74b70ce02553f7ddL, 0x1270569fe023553eL,
                },
                new long[] {
                    0x6959d677d00dd35dL, 0x06faab2179b4dbe7L, 0x40ffae141e27250dL, 0x77fdf94f98ee84bfL,
                    0x981562b903976493L, 0x54036193c77a82fbL, 0x2cf0959da786b980L, 0x7b2481c1a56508dcL,
                    0x319b4e1e4fc56d20L, 0xc54d3ad54cfd1499L, 0xbb32cf5518dd6be9L, 0xaa4e32f37f7a2586L,
                    0xf67aacc627b86195L, 0x88980c4616943de3L, 0xb837ab119857807dL, 0x0cfbc2544dbf48b8L,
                    0xe8b9884fa0ae4e34L, 0x489aaa0d0627a22cL, 0x5443cebef68ea0a6L, 0x5ca4968dfd31b40aL,
                    0x74cfc71092585a27L, 0x0bda495018792daeL, 0x654affcb7eedad76L, 0xfcb97ca8b72c4382L,
                    0x89b6d82c5e526ef0L, 0x1d7c02820d6de8d5L, 0x69fe9b84565391a2L, 0xecca48b5a815e5c5L,
                    0x8f1503297cd02494L, 0x440ee5546a18b02cL, 0xf45519c36b910074L, 0x43dc905fa1732ccbL,
                    0x27440d7f50533e2eL, 0xab553d44b699875bL, 0x6162c477b201aabbL, 0x5a1addc6a8151187L,
                    0x33e56bf53aacefb4L, 0x108e02cfdb44c699L, 0xc985a4c385edf80dL, 0x4384e8799019d966L,
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
        final SplittableUniformRandomProvider rng1 = new L128X1024Mix(seed);
        final SplittableUniformRandomProvider rng2 = rng1.split(zeroSource);
        RandomAssert.assertNextLongNotEquals(seed.length * 2, rng1, rng2);

        // Since we know how the zero seed is amended
        long z = 0;
        for (int i = Factory.INSTANCE.lcgSeedSize(); i < seed.length; i++) {
            seed[i] = LXMSupport.lea64(z);
            z += LXMSupport.GOLDEN_RATIO_64;
        }
        final SplittableUniformRandomProvider rng3 = new L128X1024Mix(seed);
        final SplittableUniformRandomProvider rng4 = rng1.split(zeroSource);
        RandomAssert.assertNextLongEquals(seed.length * 2, rng3, rng4);
    }
}
