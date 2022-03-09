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
import org.apache.commons.rng.core.RandomAssert;
import org.junit.jupiter.api.Assertions;
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
                                    new LCG64(seed[2], seed[3]),
                                    new XBGXoRoShiRo128(seed[0], seed[1], false));
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
         * Reference data from JDK 17:
         * java.util.random.RandomGeneratorFactory.of("L64X128MixRandom").create(seed)
         *
         * Full byte[] seed created using SecureRandom.nextBytes. The seed was converted
         * to long[] by filling the long bits sequentially starting at the most
         * significant byte matching the method used by JDK 17. A sign extension bug in
         * byte[] conversion required all byte indices (i % 8 != 0) to be non-negative.
         * See: https://bugs.java.com/bugdatabase/view_bug.do?bug_id=JDK-8282144
         *
         * Note: Seed has been recorded in the order used by Commons RNG.
         * JDK 17 seed order: LCG addition; LCG state; XBG state.
         * Commons RNG seed order: XBG state; LCG state; LCG addition.
         */
        return Stream.of(
            Arguments.of(
                new long[] {
                    0x8827247f034c4e4dL, 0xea5508650a264226L, 0xc1231f1811655906L, 0x413b3d6c52481a65L,
                },
                new long[] {
                    0xb4d113f36ccdd3fdL, 0xf26e7cb5c0390ec4L, 0x973ff8d2debe46b8L, 0x962e44b59daf2169L,
                    0x8c7dd11775e9d55bL, 0x8763b0cafcca658fL, 0x4d748b8631a75017L, 0x17498c7364f32942L,
                    0x2c1738406e82f6efL, 0x02214c1f300ab893L, 0xbbab56a4020b1be9L, 0x7a7b6ae910f98bcfL,
                    0xa37204aa89e828bbL, 0x46580e4586e88e53L, 0x812332dd546fd80aL, 0x747e1c1ef176795fL,
                    0xeb75d2315f29f405L, 0x39c6bad2a694c815L, 0xa00747ccd4d49a88L, 0x4c0b4679cebac0b0L,
                    0x689523cbfb9e5b40L, 0xf4798ac6780a1823L, 0x447c43ed041a52beL, 0x5878850e65d9f209L,
                    0xca89aa2b7b279b30L, 0xe7b98710421b974aL, 0x9cd4a033438fbc3aL, 0xe91dc3036b57e0f4L,
                    0xbb3ccb76d805cf70L, 0xf1b608bc99b17c4eL, 0x48ce8348ba47ff4aL, 0x12bf180fc386fdd3L,
                    0x06fcbf5e8ed7f11bL, 0x951df66a7ff90fffL, 0x25e4ba22b1d2c97dL, 0xa2450ee7e234acbeL,
                    0x206794d092392fb4L, 0x751bc3f1ba29a78fL, 0xcacd6a825354a2a5L, 0x9f807a742ddf3c2eL,
                }),
            Arguments.of(
                new long[] {
                    0xb156162d454f4406L, 0x2b1f603431544f14L, 0x1e140b1f2d047426L, 0x2c41500c4a40464fL,
                },
                new long[] {
                    0x8ec1b80ecbdf6638L, 0x7d427e73692801feL, 0xa7d11c38e77bd3f9L, 0xb70a427f0f7b8242L,
                    0x5b5aafca3afd3e77L, 0xb23cd1296d06029bL, 0x0eda6a18a61f04c7L, 0xa5dbdb8badbb0a19L,
                    0xf482bcdd6bf246ecL, 0xdc3e5161265bd675L, 0x3dbf65ac75444016L, 0xaeabf78ddea24d43L,
                    0x9a3e1aa6e98660ecL, 0x8719933d2a4f9d51L, 0x6a2c183328f2f108L, 0x8d6e6cb61bb7e254L,
                    0x4123c3eb7a5a5e26L, 0x1c2d6e8ce8de78c4L, 0xf2f4d8ca8c529d88L, 0x6fed12ec6d63428fL,
                    0x0c3404aa52cb98e0L, 0xebc4d88cedcd573cL, 0x6dacde15cde2938aL, 0x7faf0fc88902a4f4L,
                    0x2cc7b1ed35a49de7L, 0xedfdb738b0b51f34L, 0xb7403fdcd5623f05L, 0x344dd5453feff140L,
                    0x1cd944bd864e44ecL, 0xf3a728bb7bd944bbL, 0xac9e1c57f53166bfL, 0x77a1167656ff9afbL,
                    0x3bf2621282d1fb4bL, 0x37730904ed22f5e3L, 0x357c76a47629e24dL, 0x0656773b6cac7efeL,
                    0x1bcfa143e2649c7fL, 0x4907de5c28fcc710L, 0x8aff7d708155fdf9L, 0x697687aef87dd832L,
                }),
            Arguments.of(
                new long[] {
                    0xd826310863145d67L, 0x28545c212559142eL, 0xb86e7364396a5f5fL, 0x69472c09433c4245L,
                },
                new long[] {
                    0xfa3f7883551b65c4L, 0x5537e7a29730dba9L, 0x171dfb4cdb51de33L, 0x55857dc286cee0b9L,
                    0x232ae8dcad83fe7aL, 0x650afbab83268f46L, 0xc51c927f43e309d0L, 0x4b4bc38aedddeef9L,
                    0x6c679933128f4adfL, 0x3a94141aeb654cbcL, 0xb9025dc647419e1eL, 0x0b6eeeecddf751aaL,
                    0x3c0cb20257c3d1deL, 0x824ab0e63d512801L, 0xf4f11b8ff9792039L, 0xf0a712041f72b16dL,
                    0x65232d0f0db89a31L, 0xde1dc59d9a3020deL, 0x012629ebd0e82e41L, 0x72bb791550469c08L,
                    0x10acd0a730000863L, 0xf0dbdd652569feb1L, 0xb2a3505a6f8caadcL, 0x38b592bc58ffc6e2L,
                    0xb79c9661f2a726f7L, 0x9fcc0d2b41cfd3ccL, 0x4bac2b22916e7805L, 0x19ddc2276290c8ffL,
                    0x70a42053a130a2bbL, 0x915c2eec9a377a46L, 0x85b81aef329899f5L, 0xeddd96f8c61ae62dL,
                    0xa8f7a4779614ca12L, 0x5ce66eb03e190499L, 0x845f24e22591c5e1L, 0xc26d1b8bd87d8009L,
                    0xe894d1bb0e69824bL, 0x28c4c76880cec271L, 0xb887447c68200a4eL, 0xcb55ad59c43920cbL,
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
     * This algorithm overrides next() directly to allow parallel pipelining of the
     * output generation and the sub-generator update (a design feature of the LXM family).
     * The abstract nextOutput() method should not be used. This test checks the method
     * throws an exception if used.
     */
    @ParameterizedTest
    @MethodSource(value = "getReferenceData")
    void testNextOutputThrows(long[] seed) {
        Assertions.assertThrows(UnsupportedOperationException.class, () -> new L64X128Mix(seed).nextOutput());
    }

    @Test
    void test() {
        for (int i = 1; i < 20; i += 2) {
            new L64X128Mix(42, 43, 0, i).longJump();
        }
    }
}
