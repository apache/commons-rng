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
 * Test for {@link L128X256Mix}.
 */
class L128X256MixTest extends AbstractLXMTest {

    /**
     * Factory to create a composite LXM generator that is equivalent
     * to the RNG under test.
     */
    private static final class Factory implements LXMGeneratorFactory {
        static final Factory INSTANCE = new Factory();

        @Override
        public int lcgSeedSize() {
            return 4;
        }

        @Override
        public int xbgSeedSize() {
            return 4;
        }

        @Override
        public LXMGenerator create(long[] seed) {
            return new LXMGenerator(getMix(),
                                    new LCG128(seed[0], seed[1], seed[2], seed[3]),
                                    new XBGXoShiRo256(seed[4], seed[5], seed[6], seed[7], false));
        }
    }

    @Override
    LXMGeneratorFactory getFactory() {
        return Factory.INSTANCE;
    }

    @Override
    LongJumpableUniformRandomProvider create(long[] seed) {
        return new L128X256Mix(seed);
    }

    @Override
    Stream<Arguments> getReferenceData() {
        /*
         * Reference data from JDK 21:
         * java.util.random.RandomGeneratorFactory.of("L128X256MixRandom").create(seed)
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
                    0xaa05bb6a00e362a1L, 0x3ae176af61091150L, 0x4bf580302146b227L, 0x39db3019e6d00068L,
                    0xf3c7901acb51e7efL, 0x5257d2d89424a228L, 0x039b4e3bf0867638L, 0xca0e544f6f4e4c46L,
                },
                new long[] {
                    0x6d866f06c3b33a81L, 0xd9b432396d9f0095L, 0x5c6f57e856fc8312L, 0x71672e7d6c147c4dL,
                    0x90d6b6e290b94b16L, 0x115558add3a2ec66L, 0xc05eaa84d8bd9e8fL, 0xe28dd6fac10304b3L,
                    0xd4e655f8e6188df0L, 0x80ce7cbe29d2e193L, 0x1d412157be774c4aL, 0x6e5a2de7f921cf6dL,
                    0x17aecce35169ffa6L, 0x46cc5d574a1a63fcL, 0x31659705536905b3L, 0x4bcbf2a430dbf65cL,
                    0x66941d88b416536cL, 0x7c6f181beb0de730L, 0x95563ca139a29259L, 0x24de25bfd5f5d4faL,
                    0xac294de79355b668L, 0x282c6865fba13198L, 0xd583a9e4736a7dcfL, 0x40229c3b0d0027bbL,
                    0xc1a1a81f5f2d9bdfL, 0x78c35b56820ac017L, 0xc66efcbfb91ef941L, 0x4a9cc983926c7333L,
                    0x4060f5cb47ac3d7eL, 0x3d4e509e45306b8bL, 0x3eed027aa5aa3cc6L, 0xa00321247d435dc4L,
                    0x38d72a9a1fcf24e1L, 0x0f89ee0b709e1906L, 0x77e692a51693bc5aL, 0x5670e87e6eb4ed15L,
                    0x8e7eda4a370271a9L, 0xd6c157bfea09abb8L, 0x7cb846fbd89f7692L, 0xabbceb32f3a82c7cL,
                }),
            Arguments.of(
                new long[] {
                    0x5f7e521c22234349L, 0x43275c2d8b864eb6L, 0xc18ec1872a4938eeL, 0xbd46918698ebf64bL,
                    0x1d829f8bd1e502d2L, 0x2511e535216ae474L, 0x68a83824109be8ddL, 0x10d74550098027d0L,
                },
                new long[] {
                    0x22730b6721455cb2L, 0x5e1e9fe035857ac3L, 0xe96d04b39876f333L, 0xb72871f1b6aba971L,
                    0x75cf8331a6bb9e60L, 0x9acced05bcf2854dL, 0x2a0033ca609c9048L, 0x338936e9e4e483b9L,
                    0xf2fc06537f3eaebdL, 0x68f8db1b77873c0dL, 0xc7ec6c67f6b4fc6eL, 0x1fc2e76a0f68b788L,
                    0x6dc97c9f3a2fbfc6L, 0xa026cebbc9432391L, 0x2a4183ec9d4678f3L, 0x243a6a5c0067aa72L,
                    0x46a59c0eb7959104L, 0x659b1a3f61c0ed7cL, 0x215caa9463a58b1aL, 0x254642a0520c8651L,
                    0xfb8634626a39d3a1L, 0xce83d95c7775133aL, 0x5dc27d715f6409d3L, 0x2e0b2ae3ecce0077L,
                    0xb956dd21789a3963L, 0x6b916866fdae1e35L, 0x01cc314d11493e62L, 0x0391b5b718f24a82L,
                    0xdb43acbe82f439a4L, 0x40444da3ee29194aL, 0x4dee6494dcb90928L, 0xd2dbeadf078860baL,
                    0xb3ff03cefda75edcL, 0x7ea4a0af88d3e41fL, 0x71211cc08790829bL, 0xbc5532a27297928aL,
                    0x37fda61f54ed7256L, 0x276acf3bc0b732faL, 0x6939ac6cdfec6adaL, 0x4587a691d391980fL,
                }),
            Arguments.of(
                new long[] {
                    0x3d7e770d98625f7bL, 0x73f5ac0ecc6b3713L, 0x1c473ef51bb47466L, 0x265be88aae7ec15eL,
                    0x482c4848dc89175eL, 0x075324b45865bedbL, 0x7241d87ef0316e43L, 0x9ee973225e2917b4L,
                },
                new long[] {
                    0xc06d5019b8415ec6L, 0x3216f586ad0219c4L, 0xdc6256174ae52d3aL, 0x6f73525557a40362L,
                    0xc67a16801f3c7849L, 0x35fac0e26f949436L, 0xc05132d08e0ca160L, 0x86a1eea71bced849L,
                    0x5ceef8251b695656L, 0x270b44cccb03ea55L, 0xe12d74a068c6c004L, 0x6530827a464cf047L,
                    0xcac534cb43447a0eL, 0xd87210aaf51d8274L, 0x62ff0d309884417fL, 0x941a3bee4409ec4fL,
                    0x67ad1b4bb5ddd1c9L, 0x4da0f22244aaeb90L, 0x642bfdaade36f0ccL, 0x0cdbb174cdb5e231L,
                    0x02a5f954605f202fL, 0x934d28a02207ae81L, 0xa9b80d5be091e2cdL, 0x0fe7f1aae2165deeL,
                    0xf8565707f4f9f799L, 0xb272ac31b5dde485L, 0x8e862ec5e1023e23L, 0x8476fdc87da839f6L,
                    0xa87ec083c6985febL, 0x5eb0fc4fd32da92fL, 0xbbdc64a8ef4d92aeL, 0x5c4a1d7f814116dbL,
                    0x527cb12521766a5cL, 0x2ca8099919917dd4L, 0x679529f3069a615bL, 0xeea6131fa046d78aL,
                    0x65a704f76940a905L, 0xd1e4deb695b6bfd0L, 0x94b57b055a1f8263L, 0xedeb40c2ed28380eL,
                }));
    }

    @ParameterizedTest
    @MethodSource(value = "getReferenceData")
    void testElementConstructor(long[] seed, long[] expected) {
        final L128X256Mix rng1 = new L128X256Mix(seed);
        final L128X256Mix rng2 = new L128X256Mix(seed[0], seed[1], seed[2], seed[3],
                                                 seed[4], seed[5], seed[6], seed[7]);
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
        final SplittableUniformRandomProvider rng1 = new L128X256Mix(seed);
        final SplittableUniformRandomProvider rng2 = rng1.split(zeroSource);
        RandomAssert.assertNextLongNotEquals(seed.length * 2, rng1, rng2);

        // Since we know how the zero seed is amended
        long z = 0;
        for (int i = Factory.INSTANCE.lcgSeedSize(); i < seed.length; i++) {
            seed[i] = LXMSupport.lea64(z);
            z += LXMSupport.GOLDEN_RATIO_64;
        }
        final SplittableUniformRandomProvider rng3 = new L128X256Mix(seed);
        final SplittableUniformRandomProvider rng4 = rng1.split(zeroSource);
        RandomAssert.assertNextLongEquals(seed.length * 2, rng3, rng4);
    }
}
