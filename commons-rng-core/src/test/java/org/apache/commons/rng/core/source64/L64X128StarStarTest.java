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
 * Test for {@link L64X128StarStar}.
 */
class L64X128StarStarTest extends AbstractLXMTest {

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
            return 2;
        }

        @Override
        public LXMGenerator create(long[] seed) {
            return new LXMGenerator(getMix(),
                                    new LCG64(seed[0], seed[1]),
                                    new XBGXoRoShiRo128(seed[2], seed[3], false));
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
         * Reference data from JDK 19:
         * java.util.random.RandomGeneratorFactory.of("L64X128StarStarRandom").create(seed)
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
                    0xaee231d184911ad8L, 0x4520532a5549a510L, 0x71a6eb6f79366919L, 0x2083c44d4810bd6fL,
                },
                new long[] {
                    0x830084a6413e9d08L, 0x9d3ad3573722e868L, 0xd64138313ff25b4fL, 0x345eda927a84a375L,
                    0x7c20a3612b0c8fbeL, 0xab227b85595fb299L, 0x0c8a0567f166ea51L, 0xf5983403c41db8c3L,
                    0x4eb1b4dd8d7663b0L, 0xb488ddd3113af675L, 0xd0a7849e601e7a14L, 0x881dd54f67e038a3L,
                    0xf0f052f1c25c5140L, 0x50a63f651e394656L, 0x0e5c4b325f8e2e5fL, 0xf7768e8d250a834aL,
                    0x6dd2857fab5822c8L, 0x261a1503b1799ed1L, 0xdc635cdfcb6b928eL, 0xfa41d9e29c8c35c9L,
                    0xca56e6ec0e924af1L, 0xc9de942825743c7aL, 0xe975cbf2a1d60758L, 0x671044bedafa32deL,
                    0x3b35dc7e2e583e6cL, 0x7cb03e41feecfc85L, 0x67972bdd63a30d60L, 0xf75b8f37d0b4717bL,
                    0x0d833c09bba62056L, 0xfca3a08b9be878c3L, 0x4da8c219bd7fc0f9L, 0xeebabf6885853096L,
                    0x65555aba641a0653L, 0x6a9f5c913a9ed878L, 0x99fbda7113e1354dL, 0x198cbfc3c00a0ca2L,
                    0x7387aedc8b8b69d4L, 0x26c47d85244685c1L, 0x28065c272a6b0f55L, 0x6de39d4fca78f723L,
                }),
            Arguments.of(
                new long[] {
                    0x2c157477eb19cba9L, 0x26048e417b3694adL, 0x659d21b0cf0f005bL, 0x02bb00bb4faa6188L,
                },
                new long[] {
                    0xb5f6cb871d993745L, 0x8261e85c8528b091L, 0x801adfbdd328d6a9L, 0x8e9089dc51c29de2L,
                    0x2b0be99918898d84L, 0x97c205687d17d03cL, 0xf92e93c9b2f96d37L, 0xdc8f743d166b8bffL,
                    0x99db57d3bd595c68L, 0xc93392d2fb587e46L, 0xc62445f3b2ed9294L, 0xe4252d2ec389dbe6L,
                    0x369edad9d9b5651bL, 0xb2e0f52955f8a1bfL, 0xc9a1b6980b2c8ea8L, 0x40e4aa609c3bb19cL,
                    0xfaede9c7002b035cL, 0xc00b77d1b7989058L, 0x243164c89302811dL, 0xcf47804a33ca4f9dL,
                    0x15a1a6fc769a1a06L, 0x8be1729b907194ceL, 0xb943ef109365dec3L, 0x35070a31e43a522bL,
                    0x7a29a9150d6a72b9L, 0x9c67a5bbb72d971bL, 0xf2b927381613bb56L, 0xa317e83d3332ed5aL,
                    0x62f06a382579291cL, 0x065063eaf4967efdL, 0x4e0dbd771d7e9957L, 0xa1179b58c6d3a335L,
                    0x30419a0f7114b910L, 0x144eb4c373807ac2L, 0xd0904239bc2793deL, 0xe426ba924a2099f2L,
                    0xd013d7a457218234L, 0xe7ed5726a3141b1cL, 0xe5be2e06a8e623afL, 0xa9963ae2497e0c14L,
                }),
            Arguments.of(
                new long[] {
                    0xb84e2fb2f137f0d3L, 0x51a02981dca47a14L, 0xe176a5f236f46230L, 0xb841ae1033bb4ba1L,
                },
                new long[] {
                    0x813bb3b8ef5bfe77L, 0x93d1da33cc598a2cL, 0x641bdf12d424ed18L, 0xe9002256c8326374L,
                    0xbfa7b1f649eebe70L, 0xa2a50c9b58d5b8a8L, 0x102d51f8895a250cL, 0x7f645e87d4c4aeb9L,
                    0xc63a82d7347d97c1L, 0x93b0e513a347fdc5L, 0x9fbe539e9b811777L, 0xf0197ca328174abfL,
                    0x9e0417be835df367L, 0x7a87a1e461c874ffL, 0x913ec6fbe78ff63dL, 0x62d24838cff9bd28L,
                    0x19009f9566b627cbL, 0xf30e7ecc3cd4f372L, 0x8513d9f72faaeea6L, 0x02190ed9019ae371L,
                    0xe1760616b1fd4efeL, 0x58a0960d45583134L, 0x377c9c9e0872d8dbL, 0x4cf6ec0f1ecfb91bL,
                    0x54910aebb60facc2L, 0x7637e8286fc51437L, 0x15ed477ae483d7caL, 0x7acbbd2a4d1f2901L,
                    0x9fc5ddd926500fb6L, 0x16c98971886a86aeL, 0xc61dcc20296a9228L, 0x90b4583569802b04L,
                    0x8ffeb5b929c2d785L, 0xf35e8bfdccc70e97L, 0xc427ca7da8efc0d9L, 0x3d8fe39fcaf4b913L,
                    0xd350ca35762a802cL, 0x340852a8b058cf12L, 0x073fa1debb4f835fL, 0xfacc317ecd1f047bL,
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
     * Test split with zero bits from the source. This should be robust to escape the state
     * of all zero bits that will create an invalid state for the xor-based generator (XBG).
     */
    @Test
    void testSplitWithZeroBits() {
        final UniformRandomProvider zeroSource = () -> 0;
        final long[] seed = new long[Factory.INSTANCE.seedSize()];
        // Here we copy the split which sets the LCG increment to odd
        seed[(Factory.INSTANCE.lcgSeedSize() / 2) - 1] = 1;
        final SplittableUniformRandomProvider rng1 = new L64X128StarStar(seed);
        final SplittableUniformRandomProvider rng2 = rng1.split(zeroSource);
        RandomAssert.assertNextLongNotEquals(seed.length * 2, rng1, rng2);

        // Since we know how the zero seed is amended
        long z = 0;
        for (int i = Factory.INSTANCE.lcgSeedSize(); i < seed.length; i++) {
            seed[i] = LXMSupport.lea64(z);
            z += LXMSupport.GOLDEN_RATIO_64;
        }
        final SplittableUniformRandomProvider rng3 = new L64X128StarStar(seed);
        final SplittableUniformRandomProvider rng4 = rng1.split(zeroSource);
        RandomAssert.assertNextLongEquals(seed.length * 2, rng3, rng4);
    }
}
