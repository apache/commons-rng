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
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

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
                                    new LCG64(seed[16], seed[17]),
                                    new XBGXoRoShiRo1024(seed, false));
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
         * Reference data from JDK 17:
         * java.util.random.RandomGeneratorFactory.of("L64X1024MixRandom").create(seed)
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
                    0xd926667a2d47376fL, 0x4d6c207f3970190cL, 0x47674b1d727a1864L, 0xe9690738771f7564L,
                    0x7b5772414c1d4911L, 0x4f1b6709796d554dL, 0x6830481709626056L, 0x0079145654746432L,
                    0xd80557672c7b0f16L, 0x6c006c063728501aL, 0x224d1c2231223135L, 0xe54b38267c774412L,
                    0x5e434374462a697cL, 0x5e523f08696d4154L, 0xec6e770542517a7aL, 0xc80f7b222063171fL,
                    0x9a4072143201313bL, 0xca03226f3b01261dL,
                },
                new long[] {
                    0x9e4cec0bbde71682L, 0x0c5743461d7efdb6L, 0xf53dd523366d85a4L, 0x71e8c10d41c40334L,
                    0x3f220a99cfeada3aL, 0xfaf2e47b4b07ed4eL, 0x14ed83a98896ba31L, 0x9edebf98b36b1b9fL,
                    0x0a9ba0f7b3ab6175L, 0x978afd6e4072b35fL, 0x692ad061d1a892c2L, 0x33ca5f76cab2d07cL,
                    0x92c276ba933acc76L, 0xccba20e15efb8c73L, 0xd18002eecfb7f704L, 0x73c69198abd748a5L,
                    0xcf97c9abcba58ce5L, 0x190c808fd9122ec2L, 0x51723fbb1e14b8f7L, 0x42e6ce0bc68aecd8L,
                    0x3ac053dbfd27dc96L, 0x095303a8d8c740b3L, 0xeb6838e053ce700bL, 0x35c5c9d6bda511e2L,
                    0xdc00af14c0ea9505L, 0x260353d68b8b6bf2L, 0xb8303edce2102ac0L, 0xc579aa4cd2c3bfe3L,
                    0x36ba5233d4209182L, 0x11dfc73f40dd439dL, 0x697816afda914386L, 0x70a24741fb461579L,
                    0x461c7f4f4e3e13aeL, 0xe451b43cc3faec68L, 0xdc1d659897a21cffL, 0xebe38fd616677617L,
                    0x62572f9da023db27L, 0x227a328aaa96b26fL, 0x26b55d72e7cb3f28L, 0x820f8a339d14eb0aL,
                }),
            Arguments.of(
                new long[] {
                    0x39446556365d1b21L, 0xb33d473545745f3aL, 0x276f081368096368L, 0x3518237203624325L,
                    0xcd5308535c656b50L, 0x4d49137b5c026935L, 0xba536d2941171771L, 0x3364082c1712430bL,
                    0xbc3e613e47150259L, 0xa75441394924054cL, 0xed2b66352a560b1fL, 0xd022471f3504144aL,
                    0x2b604f50263a156cL, 0x1f12170a76035534L, 0xce294a2c74753b19L, 0x5e3f685b0b245661L,
                    0xdb741039765b1e3aL, 0x5b464417077b2524L,
                },
                new long[] {
                    0x00354c35c9ce8debL, 0x160325bf7614c724L, 0x31ab2503f939a58dL, 0x2b6d719d0d0b5ea3L,
                    0xeaaa842ca62dc0b3L, 0x20eb6b2b74baedbeL, 0x9efeb411f9c6e328L, 0xa22ae76c0cfcf624L,
                    0x4b66f8525cb6faecL, 0xc880b839d82ef494L, 0xfec62366978e29abL, 0x44a492a7b145becbL,
                    0x1801a2bf4c9f47efL, 0x8489c117b90c73d3L, 0xa7dd633e622e3437L, 0x84941696c38d9d23L,
                    0xa98faa3e6aaacdcfL, 0x19d59224a50095d0L, 0xe9c7239cd575c1c5L, 0xebdc11c43dbfef89L,
                    0xe9873bce2133d419L, 0x1ee76f7efa7e7e64L, 0x7ad7abe737d78323L, 0xb636edf4f1690b89L,
                    0xb03b4051ae7773a1L, 0x0115d01af9a3bdc6L, 0x8a0a958882934ec1L, 0x4d1f35f737ac69fdL,
                    0xc232a55d6a13c97cL, 0x5238ad0760bfe5bcL, 0xeabdf873c076270bL, 0x517ac4dd13562203L,
                    0x67f7c356484ba8b3L, 0x3bbef4723003e48bL, 0xf139f49d7f7a8dd8L, 0xdf75073a5f0387a8L,
                    0x2288c8ad617b8076L, 0x36fe63b9fb97c59dL, 0xe1570ec1967a5186L, 0x6b945ae90b4d7f40L,
                }),
            Arguments.of(
                new long[] {
                    0xb85e5b5412716228L, 0xb041795a1e13265aL, 0x23310f54405f0721L, 0xc006072e53321602L,
                    0xe00b775c5a122b02L, 0x465652350f4a6a07L, 0xad132729177f7718L, 0xd96122397040196fL,
                    0x5a7602557020575fL, 0xd70904063e100a7fL, 0x51172f7872206f0dL, 0xa939510a4259666eL,
                    0xbd300b4e75437a55L, 0x390b776402611b6bL, 0x653137325e691747L, 0x8067514f2a4d4d7bL,
                    0xbc755d3c47642146L, 0xb12c333a355c7c1eL,
                },
                new long[] {
                    0x54c1ce77f29f4f0eL, 0x01e8dccd6c287771L, 0xdca04ce508edb108L, 0x43eaaafc30d568c7L,
                    0x6c127d5ac0c817a0L, 0x4a087f20c482a0b9L, 0x6e0f52f0f6661d53L, 0x2325216bedf5df1dL,
                    0xaec61a8e97399dc8L, 0xb3915e80a05167c6L, 0x7da9dd81fc46eeb2L, 0x50ae26a8ec38edd0L,
                    0x8a972cdcb0998498L, 0x3bf00b045c323ddaL, 0x9be071a64da0bf2dL, 0xf522fe5cafeb8a68L,
                    0xacab485bf32ebfe6L, 0x16376a49fb6f0183L, 0xa3ae978f843cc96fL, 0xadf7ec319dae447eL,
                    0x2fa3d25318594f29L, 0x9393b88f73fd1cb4L, 0x114c0987ff63717bL, 0xd682ac09b1c4ddc8L,
                    0x483f5de1559da9a6L, 0xa94da376a7273489L, 0xd0c128feb97c394cL, 0x81291d7e85be4923L,
                    0x250013089d41fc4eL, 0x3a20e0abfd4fc7f1L, 0x6f36b9287c1cc333L, 0x648fa8168da53117L,
                    0xfc9ed78756025597L, 0x8f45c16fb8da57f5L, 0x2e03559e30bde857L, 0xd6e491f8c8c22546L,
                    0xaa84a5cf5b0668caL, 0xecb643d0e758e7edL, 0xe6eba4065ff373abL, 0xb80a1412a869cef7L,
                }));
    }

    /**
     * This algorithm overrides next() directly to allow parallel pipelining of the
     * output generation and the sub-generator update (a design feature of the LXM family).
     * The abstract transform() method should not be used. This test checks the method
     * throws an exception if used.
     */
    @ParameterizedTest
    @MethodSource(value = "getReferenceData")
    void testTransformThrows(long[] seed) {
        Assertions.assertThrows(UnsupportedOperationException.class, () -> new L64X1024Mix(seed).transform(123, 456));
    }
}
