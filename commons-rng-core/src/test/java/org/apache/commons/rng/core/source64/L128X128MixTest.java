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
    private static class Factory implements LXMGeneratorFactory {
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
         * Reference data from JDK 17:
         * java.util.random.RandomGeneratorFactory.of("L128X128MixRandom").create(seed)
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
                    0x3a297b7429761628L, 0x9d21645f201b7021L, 0xba324e3c4d146537L, 0x16364205433a7113L,
                    0xf60962173e0c5742L, 0x3e7f5f502633755fL,
                },
                new long[] {
                    0x1ea7a5960faef34dL, 0xe77f9f6290dc93f2L, 0x74aefd78ce4030fdL, 0xc081b6e4c3595c02L,
                    0x3b1edc17aa95bcb4L, 0xc14bf25e77010cb2L, 0xe7395dfccaa9dbcaL, 0xbebad53c986c6f58L,
                    0x8f8a7583d30ec2e4L, 0xe2c541e3be5ea9b0L, 0x3bd9827396ed52fcL, 0xf21e25d719195bd6L,
                    0x133fdd3ec02e5349L, 0xe4173fa34456f84cL, 0xf749b087f88e3cefL, 0xd078170802484e3cL,
                    0x05f06c0da0b2fa8bL, 0x42b7c01bcf46c009L, 0xeae2728936c06176L, 0x71b6f995578a341dL,
                    0x1dc20c1eb7dde50bL, 0x4dfe3a2dc06884f9L, 0x53f8a64ef6620772L, 0x09f05fe07be6c5aaL,
                    0x019dd2cec64bd224L, 0xc91122977e5795a1L, 0x5ca628e227a9c59cL, 0x1c45e0b0ba328638L,
                    0x431beb396ed3d853L, 0x270db91f7a43193cL, 0xba22d45ffa952da8L, 0xa03026f48d0a16eaL,
                    0xd59e56fa28a9fd0bL, 0x5d3e3178bf2d1bddL, 0xd4a01fc739524149L, 0x08d25b8c425dcbedL,
                    0x6b53b7544bd12866L, 0x4c1ba8e9baacd1d4L, 0x6a2c03ce0e64be9eL, 0xaef1188c7b49e967L,
                }),
            Arguments.of(
                new long[] {
                    0xab05120b1d68141aL, 0xaa16702f314b0958L, 0x48712e101702612cL, 0x52564c1c0c462c6aL,
                    0x02442c63271c2e65L, 0xa8256d670f6a0f50L,
                },
                new long[] {
                    0xb26e759a441f5966L, 0xc754e012f0e5b695L, 0x6f63edc0e02b9339L, 0xe3b4a2f074e68065L,
                    0x50e3133fee680fb1L, 0x4806d74486f1047aL, 0x636a4bb9b1588135L, 0xa38a1d12165a1a5eL,
                    0x1c93a08893b5d460L, 0xe5f12c095908d68bL, 0xc8209614bdf5c3aaL, 0x269514b48092e05cL,
                    0xf53376fb46c4068fL, 0x1c29c163c83808a1L, 0x4e061b72e2558d90L, 0x764dbe734460cd58L,
                    0x951eed50f2688936L, 0x90c431c196e7caa2L, 0x546c18e2ae8449cbL, 0xfa53c4db8f104337L,
                    0x6101109a7906c635L, 0xd60391b908c7689aL, 0x2edba3f7777b0fceL, 0xc58ec62f686cfafeL,
                    0xb46e6b9a63f7a5c8L, 0x0882126f0efd1c8cL, 0xb0a74862abb8fdceL, 0xeb73ae25e8d0cb13L,
                    0x6d2cabda24960b3fL, 0xa066055ef4918eb3L, 0x5e41aee4befadce1L, 0x8d0188693ebc3e89L,
                    0x42b7171e98c7ba4bL, 0x6cf4919164a36dedL, 0xd90883fa3b33a4daL, 0x01ac3a18f4020a05L,
                    0x1ec9164000c2c5c3L, 0xa90a377959d611baL, 0x4719833abe706dc0L, 0xf9a18b8597be90c8L,
                }),
            Arguments.of(
                new long[] {
                    0x68034679056f6362L, 0xe666227323110c1cL, 0xaa502d6b5e676c61L, 0xe94d1d266d030b23L,
                    0x50253d7d19161d66L, 0x5f64221100172276L,
                },
                new long[] {
                    0xcf85d748550aaf08L, 0xa1903ba42f5c24a0L, 0x64df004d9a06460aL, 0xfbf4062ca343346cL,
                    0x5bdf77cdfdac6cf8L, 0x5e16cbd71a4020a3L, 0x3835ab4dde607646L, 0xa1ccb3ec0909470cL,
                    0x3d6f31f8aedb785fL, 0x8214004c996c8bf6L, 0xc6c775f354564e3cL, 0x6054e091e54a7278L,
                    0xd7a4b3827266b0fdL, 0x78ef85c8c29cc270L, 0x1e92d5e38336284bL, 0xd2d7939efd300d8aL,
                    0xa3b4a28536af8e04L, 0x5f1510ef2c9611dbL, 0xacbffd845e921744L, 0xeb46cad5d2c25cd9L,
                    0xd449bc48ce4c0288L, 0x738064dc34ed21a9L, 0xac85af76c0d4ae3eL, 0xf04885858cce92f8L,
                    0x3288572bff8677d4L, 0xc58371958117ecd1L, 0x018a1d0d5f6d6bb7L, 0xe29afe1c6f54208fL,
                    0x724c19235787dfb1L, 0x1d47fde2388156eaL, 0xb6316593e1cc5ac8L, 0x15f8c8c913938180L,
                    0x0d38390376df87feL, 0x950b50b0b4af2525L, 0x1d2d86b7b52aeabbL, 0x8331694e910a3705L,
                    0xc962bf20dc7ae05bL, 0x07b911ecb2b6e5f8L, 0x2e814094668c24eaL, 0xc259ec3c9a99750aL,
                }));
    }

    @ParameterizedTest
    @MethodSource(value = "getReferenceData")
    void testElementConstructor(long[] seed, long[] expected) {
        final L128X128Mix rng1 = new L128X128Mix(seed);
        final L128X128Mix rng2 = new L128X128Mix(seed[0], seed[1], seed[2], seed[3], seed[4], seed[5]);
        RandomAssert.assertNextLongEquals(seed.length * 2, rng1, rng2);
    }
}
