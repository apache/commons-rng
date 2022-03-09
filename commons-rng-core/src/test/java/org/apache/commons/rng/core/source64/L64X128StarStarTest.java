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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Test for {@link L64X128StarStar}.
 */
class L64X128StarStarTest extends AbstractLXMTest {

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

        @Override
        public Mix getMix() {
            return AbstractLXMTest::mixStarStar;
        }
    }

    @Override
    LXMGeneratorFactory getFactory() {
        return Factory.INSTANCE;
    }

    @Override
    LongJumpableUniformRandomProvider create(long[] seed) {
        return new L64X128StarStar(seed);
    }

    @Override
    Stream<Arguments> getReferenceData() {
        /*
         * Reference data from JDK 17:
         * java.util.random.RandomGeneratorFactory.of("L64X128StarStarRandom").create(
         * seed)
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
                    0x90553f5a59676f25L, 0x590a4f291e16303fL, 0x02774b434710415eL, 0x9a6735334f764669L,
                },
                new long[] {
                    0xfa2eda9a8503875eL, 0x40d2997cf3a8faa2L, 0xb782cb5e01cfbfe8L, 0x6f80294d7caccc20L,
                    0x1415b29e01be890fL, 0xa7a184e3ae16d463L, 0xa27d0a5eac6f025cL, 0xa63c4bd5924f9c09L,
                    0x91e045ca04392005L, 0x217117d21492258bL, 0xdcd5e74c08e9a8a6L, 0x9ae1eda89f8f8ce6L,
                    0xb83d6fad06d9bccbL, 0xed96a0a8d3ba2232L, 0xd5a2730465123ec2L, 0x514212d2f65a3e68L,
                    0x62439a2270b47780L, 0xa7a1a451402a983fL, 0xfaa331658e2610b2L, 0x5eb09511024e8962L,
                    0xa98a44c94106f1d6L, 0x6350f0194244c038L, 0xc359b399832fd464L, 0x26a08f51b2caa80aL,
                    0x4063c19cf92c8122L, 0x51a9e422a6cc8536L, 0x50412f9eb97447fdL, 0xd46551a24d3b3644L,
                    0x9cb52774b1d76aeaL, 0x278a1794658b9809L, 0xe1e9901717b9935eL, 0xa7fc2b13259d8e1eL,
                    0x9fe0ff858f44de81L, 0xd6f4f7217503f9b9L, 0xc341d1b3fe50029bL, 0x11b61829e61442d8L,
                    0xabe181bddbc10dddL, 0xc1e84b3cda0c8cadL, 0x9f9ad08d6c2d04a2L, 0x1a69efc926ae734cL,
                }),
            Arguments.of(
                new long[] {
                    0x3b31185c397d2e52L, 0xf01c3f7a25072270L, 0x3125364a71672453L, 0xac720a006d614309L,
                },
                new long[] {
                    0x95e9a605114380feL, 0xc4ca92a0d8f35896L, 0x96a303974cf302d0L, 0xb82c651679cc2fecL,
                    0x0f2d0498c8eb3d69L, 0x0184f46152b5b87bL, 0x3b78c3105abf46b2L, 0xe438cffc7df8d357L,
                    0xc2be744f5e0b57baL, 0xc81dcaecd52aaa2fL, 0x6d7ef235a564cc20L, 0x7fb08ae36375ecf8L,
                    0x81040c0645bf581bL, 0xdbb8f4c78ea8b15fL, 0x13a108fe1d370c74L, 0xc93b297927a6e4aaL,
                    0x197a4fc2cc535c25L, 0x5e800e1bb478ca01L, 0x638d5ac226fbfb4fL, 0xfaee78320b426b13L,
                    0xe13dc9e43f84df48L, 0x392847cd9f4b2233L, 0x48a6e2e4636389d6L, 0xe0e53e0b19cec7beL,
                    0xb6459258787c8829L, 0x83b00e03ec5abffcL, 0xe517275e6afab602L, 0x4febfc2f98abd21fL,
                    0x8bbe2ef1dff25e6cL, 0x2167cbac37bef5b8L, 0xfd047e45396ca725L, 0x170d226283dd4ef6L,
                    0x39690beb723b8d07L, 0x306116b9d06d5509L, 0x3f49cf45882f0f67L, 0xdeca8c6a4b085bf5L,
                    0x0e3b1e52ff7506bfL, 0xc2656d2d8fd459c2L, 0x0e4148af436156afL, 0x806e4553f786cd7dL,
                }),
            Arguments.of(
                new long[] {
                    0x8874316d7b59490aL, 0xff31187b6059744cL, 0xda36353a6766765eL, 0xaf735c18397e6947L,
                },
                new long[] {
                    0xfa05c16dda52a826L, 0xe9c523f07c5cb2b4L, 0x13ae41d65b14107dL, 0xf3e1173f543e509dL,
                    0xc43eb0c67d3495c5L, 0x44edd79d203d8293L, 0xf2fccfe48084fc20L, 0x3c77d17b4850f6fdL,
                    0x83f79eb77a62bb37L, 0x987c4f75be4133e9L, 0x0ee529d1f73a2099L, 0x7c9639d659ae4079L,
                    0x2d191c7096a1ac90L, 0x9e4606d9dccd30d1L, 0x850c3dfc9386ecb1L, 0x77a00fb6cf9fb824L,
                    0xdfe307b7c9faa672L, 0x3c4ee316952584ceL, 0x5feaf6e782fa5602L, 0x23d0b7c0aa85e8f3L,
                    0x66d9f669dffceadcL, 0x4c00536a1a4fd75bL, 0x08e54b8bb6e6c5f9L, 0x3453c08c4b422660L,
                    0x513269dd9e520177L, 0xd86591fb64fdd9dcL, 0xb7753a837e7cf44dL, 0xde8bdc2fd2c8d37fL,
                    0x535f5931cb5acc8dL, 0x4e2d0d0823d2a562L, 0x99a9874c01c4b261L, 0x322dbf9da83b96f7L,
                    0x468fe390218f10d8L, 0xc7c7b4405e830136L, 0x926beae7a25fe30cL, 0x648ad95805c12f29L,
                    0x94ed61e84f718d12L, 0xf57931fcb440dd42L, 0x6759dde6dbe42de8L, 0xe48db87e5f0df1dbL,
                }));
    }

    @ParameterizedTest
    @MethodSource(value = "getReferenceData")
    void testElementConstructor(long[] seed, long[] expected) {
        final L64X128StarStar rng1 = new L64X128StarStar(seed);
        final L64X128StarStar rng2 = new L64X128StarStar(seed[0], seed[1], seed[2], seed[3]);
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
        Assertions.assertThrows(UnsupportedOperationException.class, () -> new L64X128StarStar(seed).nextOutput());
    }
}
