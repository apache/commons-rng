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
         * Reference data from JDK 17:
         * java.util.random.RandomGeneratorFactory.of("L128X1024MixRandom").create(seed)
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
                    0x847907737f670a2cL, 0xcb60793f261c7702L, 0x4d0623076241212cL, 0x1a771f1c4849561dL,
                    0xe9214a30656c6621L, 0xce317a274f194d16L, 0x0274414662435445L, 0x1475285b682c1901L,
                    0x1c370d7a6759381aL, 0x752d7d727966663aL, 0x2e2d131f10190254L, 0x7f0f0f5032053132L,
                    0x1f617a47474b144dL, 0x0539411a037e693dL, 0x373801350060224cL, 0x01393b2c1b122104L,
                    0x7808676475605545L, 0xec3764576637392dL, 0x9c22290673241d03L, 0xcb4031176e673e07L,
                },
                new long[] {
                    0x16a64b86bb4f77e8L, 0x08652ab51a725fe6L, 0x352b20ab3eb0e6e8L, 0xd3dfb9080cd0fff3L,
                    0x679bb8834b8c39edL, 0x1db09e09b224efdbL, 0x15ad68e0b42df0ccL, 0xf0b3d174dee2f4d3L,
                    0x10c2523ca4a4c906L, 0x07fb1f38ebb06949L, 0xe17296e0a4551815L, 0x4e767b11277bd5b7L,
                    0xd2861b50ab58769eL, 0x9fda6327293b6edbL, 0xa700b1b39c091649L, 0x3fdb8ab4d3181817L,
                    0xe8e650db502c46f1L, 0xfc6983d3b5499938L, 0x15ffd7b60dbbc81eL, 0xf21885310202f639L,
                    0x416731e4a25fe54eL, 0xc96a09a45aafdbabL, 0xceebcd6c3eb7f410L, 0x6965dd7f2e5be202L,
                    0xb135778200cebda5L, 0x323946de66ea8efcL, 0xd99f8550d934b1a7L, 0x7d54c10751e1943fL,
                    0xfa58243c8f26168aL, 0x4f9503a63b956ac1L, 0xd717de76262d1689L, 0x2dae4df5fd85d281L,
                    0xab587b41f01c52abL, 0x3a0ec6c60fbf40f2L, 0x85b92bcf3709aa15L, 0x81c19f123efa68fbL,
                    0xac06a72f4dbc17edL, 0xd98df600b51e4f48L, 0x009871f979161bf1L, 0xb63b478960527bd5L,
                }),
            Arguments.of(
                new long[] {
                    0x3d4306735a152d71L, 0xc1503e7e2f76750bL, 0x726865045a421d0aL, 0x905a1c0f777c280fL,
                    0xab0b405e243f6655L, 0x7220783e26481705L, 0x3f1a2f071f22787dL, 0x2d624a5a5a1a487cL,
                    0x643935551a0e1744L, 0x5a663f673a506b3aL, 0x76186d1e7a081e14L, 0xf64a453f6f2d5d4dL,
                    0xec27754b5a230725L, 0x852e03364b2d2535L, 0xb71f004f51605102L, 0x0a0c7d68300d575bL,
                    0x9943777910780256L, 0x383b43005c1c4a77L, 0x9923163f2e246e2eL, 0x2e7a657e1079281bL,
                },
                new long[] {
                    0x43843e06d6aa8933L, 0x760b374cdcc21a2eL, 0xf02694e16403b8d2L, 0x3c14fd3a09551e59L,
                    0xcd9acc1bf34253d8L, 0xa2677f96de7f389fL, 0xedaec653655a85f2L, 0x30e1bc7dd9e931fdL,
                    0x9ee8ae96e94e61beL, 0x6949cf0a241bbd7bL, 0x72ed6630513dfed0L, 0x9bd0ebfc89db4d8dL,
                    0xdd6f0a8f70451e3dL, 0x59e9fb17058f1fe2L, 0xd29197fffc0ce21dL, 0x424d6b309b44d7deL,
                    0x8ce459db11a1abbfL, 0xf7f56ebd0a9f8578L, 0xe54de673fd1a7d54L, 0x11bd4054dd8f2ea0L,
                    0xb15c3202a4eecb51L, 0xc93e7c2a0c44487bL, 0xf505494bc0e60deaL, 0xdb73777f97262200L,
                    0xada728f47bff2975L, 0x501d543141ac9285L, 0xe14f70683e9442a1L, 0x6d44ceeccf039483L,
                    0x4bf0401aeca9de03L, 0xba0bd837fb8850a7L, 0xf3ef477cee53d8a1L, 0x84866359a6dcdbceL,
                    0xf38284481dfa4ab6L, 0x9b1c6c1bf3ceee63L, 0x4254e6ac337f55fbL, 0x3f5b499c46a13ddfL,
                    0xf6d4ea2708c600caL, 0x4c7a9c3c986f4a7aL, 0x4a7aa1e3fe17b370L, 0x03e457ba810546f8L,
                }),
            Arguments.of(
                new long[] {
                    0x5a3436133e6d4878L, 0xbd471a486c233413L, 0x3a1e141f643a1a15L, 0x35584a2c3a514d6dL,
                    0x15462b005234675cL, 0x100c3674063b4d2bL, 0xc67f0c760c2a4149L, 0x36530758667a4017L,
                    0x0f527d1a20512f38L, 0x224d0430615c4009L, 0x857d5f576c20290eL, 0x247d23184508793fL,
                    0xed530606415c5d7aL, 0x194e7a0103414b12L, 0x2e03337c393e080bL, 0x736a171f506e5350L,
                    0x0c59070650497402L, 0x287b01437241213fL, 0xf60e374d567a3f6cL, 0xa9635e395b2f5f03L,
                },
                new long[] {
                    0x6e4abb493293e3cfL, 0x57534af73dcd1d1eL, 0x0c66796854cb1e56L, 0xe3c3b560cf82b3baL,
                    0xab53f3c3ca41c10dL, 0x278f0028e279a327L, 0xcca0f86cdb902d14L, 0x442046c504f378a2L,
                    0xf799e6907cfff304L, 0xd5d703efa5e39620L, 0xd54f7043ee6c03d5L, 0xc7713f5af1ead63dL,
                    0x857d603adc723d19L, 0x42a1147bc4844e0fL, 0x204993b253531d7cL, 0x67cc7cae796e3297L,
                    0x7e17cc851367c8b3L, 0xae6d089adc64d157L, 0x54bf513c06041c2fL, 0x5975f68e8b1c3cbbL,
                    0x30551255b5e791dcL, 0xb5500a471d2bf585L, 0x5eafdd741ed469beL, 0x34ddf80f26fbd921L,
                    0x45704fee22f2c0a0L, 0x4735f0ec33fdf033L, 0x6864735d1bbe507eL, 0x47627355d302620cL,
                    0xe99b2b58c414399cL, 0x7d8a28cba60e5938L, 0xc29fd6a62a43fdf6L, 0x4715e2b8c3637eedL,
                    0xfdd17d771ccc525dL, 0xeb99d1815b304ef1L, 0x9679eb1d3b133e68L, 0xcabcd9a42c445c99L,
                    0x479d66e6c85c98beL, 0xba9516550452d729L, 0x299e54b50cebe420L, 0x8fde3ca654cd399dL,
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
