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

import java.math.BigInteger;
import java.util.SplittableRandom;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import java.util.function.LongSupplier;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;
import org.apache.commons.rng.LongJumpableUniformRandomProvider;
import org.apache.commons.rng.RestorableUniformRandomProvider;
import org.apache.commons.rng.core.RandomAssert;
import org.apache.commons.rng.core.util.NumberFactory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Base test for LXM generators. These generators all use the same basic steps:
 * <pre>
 * s = state of LCG
 * t = state of XBG
 *
 * generate():
 *    z <- mix(combine(w upper bits of s, w upper bits of t))
 *    s <- LCG state update
 *    t <- XBG state update
 * </pre>
 *
 * <p>This base class provides a composite generator of an LCG and XBG
 * both using w = 64-bits. Tests for a RNG implementation must define
 * a factory for constructing a reference composite LXM generator. Implementations
 * for the 64-bit and 128-bit LCG used in the LXM family are provided.
 * Implementations for the XBG are provided based on version in the Commons RNG core library.
 *
 * <p>It is assumed the XBG generator is a {@link LongJumpableUniformRandomProvider}.
 * The composite generator requires the sub-generators to provide a jump and long jump
 * equivalent operation. This is performed by advancing/rewinding the LCG and XBG the same number
 * of cycles. In practice this is performed using:
 * <ul>
 * <li>A large jump of the XBG that wraps the state of the LCG.
 * <li>Leaving the XBG state unchanged and advancing the LCG. This effectively
 * rewinds the state of the LXM by the period of the XBG multiplied by the number of
 * cycles advanced by the LCG.
 * </ul>
 *
 * <p>The paper by Steele and Vigna suggest advancing the LCG to take advantage of the fast
 * update step of the LCG. This is particularly true for the 64-bit LCG variants. If
 * the LCG and XBG sub-generators support jump/longJump then the composite can then be
 * used to test arbitrary combinations of calls to: generate the next long value; and jump
 * operations. This is not possible using the reference implementations of the LXM family
 * in JDK 17 which do not implement jumping (instead providing a split operation to create
 * new RNGs).
 *
 * <p>The test assumes the following conditions:
 * <ul>
 * <li>The seed length equals the state size of the LCG and XBG generators.
 * <li>The LXM generator seed is the seed for the LCG appended with the seed for the XBG.
 * <li>The LCG seed in order is [additive parameter, initial state]. The additive parameter
 * must be odd for a maximum period LCG and the test asserts the final bit of the add parameter
 * from the seed is effectively ignored as it is forced to be odd in the constructor.
 * <li>If the XBG seed is all zeros, the LXM generator is partially functional. It will output
 * non-zero values but the sequence may be statistically weak and the period is that of the LCG.
 * This cannot be practically tested but non-zero output is verified for an all zero seed.
 * </ul>
 *
 * <p>Note: The seed order prioritises the additive parameter first. This parameter can be used to
 * create independent streams of generators. It allows constructing a generator from a single long
 * value, where the rest of the state is filled with a procedure generating non-zero values,
 * which will be independent from a second generator constructed with a different single additive
 * parameter. The difference must be in the high 63-bits of the seed value for 64-bit LCG variants.
 * A suitable filling procedure from an initial seed value is provided in the Commons RNG Simple
 * module.
 *
 * <p>The class uses a per-class lifecycle. This allows abstract methods to be overridden
 * in implementing classes to control test behaviour.
 */
@TestInstance(Lifecycle.PER_CLASS)
abstract class AbstractLXMTest {
    /**
     * A function to mix two long values.
     * Implements the combine and mix functions of the LXM generator.
     */
    interface Mix {
        /**
         * Mix the values.
         *
         * @param a Value a
         * @param b Value b
         * @return the result
         */
        long apply(long a, long b);
    }

    /**
     * A jumpable sub-generator. This can be a linear congruential generator (LCG)
     * or xor-based generator (XBG) when used in the LXM family.
     *
     * <p>For simplicity the steps of obtaining the upper bits of the state
     * and updating the state are combined to a single operation.
     */
    interface SubGen {
        /**
         * Return the upper 64-bits of the current state and update the state.
         *
         * @return the upper 64-bits of the old state
         */
        long stateAndUpdate();

        /**
         * Create a copy and then advance the state in a single jump; the copy is returned.
         *
         * @return the copy
         */
        SubGen copyAndJump();

        /**
         * Create a copy and then advance the state in a single long jump; the copy is returned.
         *
         * @return the copy
         */
        SubGen copyAndLongJump();
    }

    /**
     * Mix the sum using the star star function.
     *
     * @param a Value a
     * @param b Value b
     * @return the result
     */
    static long mixStarStar(long a, long b) {
        return Long.rotateLeft((a + b) * 5, 7) * 9;
    }

    /**
     * Mix the sum using the lea64 function.
     *
     * @param a Value a
     * @param b Value b
     * @return the result
     */
    static long mixLea64(long a, long b) {
        return LXMSupport.lea64(a + b);
    }

    /**
     * A 64-bit linear congruential generator.
     */
    static class LCG64 implements SubGen {
        /** Multiplier. */
        private static final long M = LXMSupport.M64;
        /** Additive parameter (must be odd). */
        private final long a;
        /** State. */
        private long s;
        /** Power of 2 for the jump. */
        private final int jumpPower;
        /** Power of 2 for the long jump. */
        private final int longJumpPower;

        /**
         * Create an instance with a jump of 1 cycle and long jump of 2^32 cycles.
         *
         * @param a Additive parameter (set to odd)
         * @param s State
         */
        LCG64(long a, long s) {
            this(a, s, 0, 32);
        }

        /**
         * @param a Additive parameter (set to odd)
         * @param s State
         * @param jumpPower Jump size as a power of 2
         * @param longJumpPower Long jump size as a power of 2
         */
        LCG64(long a, long s, int jumpPower, int longJumpPower) {
            this.a = a | 1;
            this.s = s;
            this.jumpPower = jumpPower;
            this.longJumpPower = longJumpPower;
        }

        @Override
        public long stateAndUpdate() {
            final long s0 = s;
            s = M * s + a;
            return s0;
        }

        @Override
        public SubGen copyAndJump() {
            final SubGen copy = new LCG64(a, s, jumpPower, longJumpPower);
            s = LXMSupportTest.lcgAdvancePow2(s, M, a, jumpPower);
            return copy;
        }

        @Override
        public SubGen copyAndLongJump() {
            final SubGen copy = new LCG64(a, s, jumpPower, longJumpPower);
            s = LXMSupportTest.lcgAdvancePow2(s, M, a, longJumpPower);
            return copy;
        }
    }

    /**
     * A 128-bit linear congruential generator.
     */
    static class LCG128 implements SubGen {
        /** Low half of 128-bit multiplier. The upper half is 1. */
        private static final long ML = LXMSupport.M128L;
        /** High bits of additive parameter. */
        private final long ah;
        /** Low bits of additive parameter (must be odd). */
        private final long al;
        /** High bits of state. */
        private long sh;
        /** Low bits of state. */
        private long sl;
        /** Power of 2 for the jump. */
        private final int jumpPower;
        /** Power of 2 for the long jump. */
        private final int longJumpPower;

        /**
         * Create an instance with a jump of 1 cycle and long jump of 2^64 cycles.
         *
         * @param ah High bits of additive parameter
         * @param al Low bits of additive parameter (set to odd)
         * @param sh High bits of the 128-bit state
         * @param sl Low bits of the 128-bit state
         */
        LCG128(long ah, long al, long sh, long sl) {
            this(ah, al, sh, sl, 0, 64);
        }

        /**
         * @param ah High bits of additive parameter
         * @param al Low bits of additive parameter (set to odd)
         * @param sh High bits of the 128-bit state
         * @param sl Low bits of the 128-bit state
         * @param jumpPower Jump size as a power of 2
         * @param longJumpPower Long jump size as a power of 2
         */
        LCG128(long ah, long al, long sh, long sl, int jumpPower, int longJumpPower) {
            this.ah = ah;
            this.al = al | 1;
            this.sh = sh;
            this.sl = sl;
            this.jumpPower = jumpPower;
            this.longJumpPower = longJumpPower;
        }

        @Override
        public long stateAndUpdate() {
            final long s0 = sh;
            // The LCG is, in effect, "s = m * s + a" where m = ((1LL << 64) + ML)
            final long u = ML * sl;
            // High half
            sh = (ML * sh) + LXMSupport.unsignedMultiplyHigh(ML, sl) + sl + ah;
            // Low half
            sl = u + al;
            // Carry propagation
            if (Long.compareUnsigned(sl, u) < 0) {
                ++sh;
            }
            return s0;
        }

        @Override
        public SubGen copyAndJump() {
            final SubGen copy = new LCG128(ah, al, sh, sl, jumpPower, longJumpPower);
            final long nsl = LXMSupportTest.lcgAdvancePow2(sl, ML, al, jumpPower);
            sh = LXMSupportTest.lcgAdvancePow2High(sh, sl, 1, ML, ah, al, jumpPower);
            sl = nsl;
            return copy;
        }

        @Override
        public SubGen copyAndLongJump() {
            final SubGen copy = new LCG128(ah, al, sh, sl, jumpPower, longJumpPower);
            final long nsl = LXMSupportTest.lcgAdvancePow2(sl, ML, al, longJumpPower);
            sh = LXMSupportTest.lcgAdvancePow2High(sh, sl, 1, ML, ah, al, longJumpPower);
            sl = nsl;
            return copy;
        }
    }

    /**
     * A xor-based generator using XoRoShiRo128.
     *
     * <p>The generator can be configured to perform jumps or simply return a copy.
     * The LXM generator would jump by advancing only the LCG state.
     */
    static class XBGXoRoShiRo128 extends AbstractXoRoShiRo128 implements SubGen {
        /** Jump flag. */
        private final boolean jump;

        /**
         * Create an instance with jumping enabled.
         *
         * @param seed seed
         */
        XBGXoRoShiRo128(long[] seed) {
            super(seed);
            jump = true;
        }

        /**
         * @param seed0 seed element 0
         * @param seed1 seed element 1
         * @param jump Set to true to perform jumping, otherwise a jump returns a copy
         */
        XBGXoRoShiRo128(long seed0, long seed1, boolean jump) {
            super(seed0, seed1);
            this.jump = jump;
        }

        /**
         * Copy constructor.
         *
         * @param source the source to copy
         */
        XBGXoRoShiRo128(XBGXoRoShiRo128 source) {
            // There is no super-class copy constructor so just construct
            // from the RNG state.
            // Warning:
            // This will not copy the cached state from 'source'
            // which matters if tests use nextInt or nextBoolean.
            // Currently tests only target the long output.
            this(source.state0, source.state1, source.jump);
        }

        @Override
        public long stateAndUpdate() {
            final long s0 = state0;
            next();
            return s0;
        }

        @Override
        public SubGen copyAndJump() {
            return (SubGen) (jump ? super.jump() : copy());
        }

        @Override
        public SubGen copyAndLongJump() {
            return (SubGen) (jump ? super.longJump() : copy());
        }

        @Override
        protected long nextOutput() {
            // Not used
            return 0;
        }

        @Override
        protected XBGXoRoShiRo128 copy() {
            return new XBGXoRoShiRo128(this);
        }
    }

    /**
     * A xor-based generator using XoShiRo256.
     *
     * <p>The generator can be configured to perform jumps or simply return a copy.
     * The LXM generator would jump by advancing only the LCG state.
     */
    static class XBGXoShiRo256 extends AbstractXoShiRo256 implements SubGen {
        /** Jump flag. */
        private final boolean jump;

        /**
         * Create an instance with jumping enabled.
         *
         * @param seed seed
         */
        XBGXoShiRo256(long[] seed) {
            super(seed);
            jump = true;
        }

        /**
         * @param seed0 seed element 0
         * @param seed1 seed element 1
         * @param seed2 seed element 2
         * @param seed3 seed element 3
         * @param jump Set to true to perform jumping, otherwise a jump returns a copy
         */
        XBGXoShiRo256(long seed0, long seed1, long seed2, long seed3, boolean jump) {
            super(seed0, seed1, seed2, seed3);
            this.jump = jump;
        }

        /**
         * Copy constructor.
         *
         * @param source the source to copy
         */
        XBGXoShiRo256(XBGXoShiRo256 source) {
            super(source);
            jump = source.jump;
        }

        @Override
        public long stateAndUpdate() {
            final long s0 = state0;
            next();
            return s0;
        }

        @Override
        public SubGen copyAndJump() {
            return (SubGen) (jump ? super.jump() : copy());
        }

        @Override
        public SubGen copyAndLongJump() {
            return (SubGen) (jump ? super.longJump() : copy());
        }

        @Override
        protected long nextOutput() {
            // Not used
            return 0;
        }

        @Override
        protected XBGXoShiRo256 copy() {
            return new XBGXoShiRo256(this);
        }
    }

    /**
     * A xor-based generator using XoRoShiRo1024.
     *
     * <p>The generator can be configured to perform jumps or simply return a copy.
     * The LXM generator would jump by advancing only the LCG state.
     *
     * <p>Note: To avoid changes to the parent class this test class uses the save/restore
     * function to initialise the index. The rng state update can be managed by the
     * super-class and this class holds a reference to the most recent s0 value.
     */
    static class XBGXoRoShiRo1024 extends AbstractXoRoShiRo1024 implements SubGen {
        /** Jump flag. */
        private final boolean jump;
        /** The most recent s0 variable passed to {@link #transform(long, long)}. */
        private long state0;

        /**
         * Create an instance with jumping enabled.
         *
         * @param seed 16-element seed
         */
        XBGXoRoShiRo1024(long[] seed) {
            this(seed, true);
        }

        /**
         * @param seed 16-element seed
         * @param jump Set to true to perform jumping, otherwise a jump returns a copy
         */
        XBGXoRoShiRo1024(long[] seed, boolean jump) {
            super(seed);
            this.jump = jump;
            // Ensure the first state returned corresponds to state[0].
            // This requires setting:
            // index = state.length - 1
            // To avoid changing the private visibility of super-class state variables
            // this is done using the save/restore function. It avoids any issues
            // with using reflection on the 'index' field but must be maintained
            // inline with the save/restore logic of the super-class.
            final byte[] s = super.getStateInternal();
            final byte[][] c = splitStateInternal(s, 17 * Long.BYTES);
            final long[] tmp = NumberFactory.makeLongArray(c[0]);
            // Here: index = state.length - 1
            tmp[16] = 15;
            c[0] = NumberFactory.makeByteArray(tmp);
            super.setStateInternal(composeStateInternal(c[0], c[1]));
        }

        /**
         * Copy constructor.
         *
         * @param source the source to copy
         */
        XBGXoRoShiRo1024(XBGXoRoShiRo1024 source) {
            super(source);
            jump = source.jump;
        }

        @Override
        public long stateAndUpdate() {
            next();
            return state0;
        }

        @Override
        public SubGen copyAndJump() {
            return (SubGen) (jump ? super.jump() : copy());
        }

        @Override
        public SubGen copyAndLongJump() {
            return (SubGen) (jump ? super.longJump() : copy());
        }

        @Override
        protected long transform(long s0, long s15) {
            this.state0 = s0;
            // No transformation required.
            return 0;
        }

        @Override
        protected XBGXoRoShiRo1024 copy() {
            return new XBGXoRoShiRo1024(this);
        }
    }

    /**
     * A composite LXM generator. Implements:
     * <pre>
     * s = state of LCG
     * t = state of XBG
     *
     * generate():
     *    z <- mix(combine(w upper bits of s, w upper bits of t))
     *    s <- LCG state update
     *    t <- XBG state update
     * </pre>
     * <p>w is assumed to be 64-bits.
     */
    static class LXMGenerator extends LongProvider implements LongJumpableUniformRandomProvider {
        /** Mix implementation. */
        private final Mix mix;
        /** LCG implementation. */
        private final SubGen lcg;
        /** XBG implementation. */
        private final SubGen xbg;

        /**
         * Create a new instance.
         * The jump and long jump of the LCG are assumed to appropriately match those of the XBG.
         * This can be achieved by an XBG jump that wraps the period of the LCG; or advancing
         * the LCG and leaving the XBG state unchanged, effectively rewinding the LXM generator.
         *
         * @param mix Mix implementation
         * @param lcg LCG implementation
         * @param xbg XBG implementation
         */
        LXMGenerator(Mix mix, SubGen lcg, SubGen xbg) {
            this.lcg = lcg;
            this.xbg = xbg;
            this.mix = mix;
        }

        @Override
        public long next() {
            return mix.apply(lcg.stateAndUpdate(), xbg.stateAndUpdate());
        }

        @Override
        public LXMGenerator jump() {
            return new LXMGenerator(mix, lcg.copyAndJump(), xbg.copyAndJump());
        }

        @Override
        public LXMGenerator longJump() {
            return new LXMGenerator(mix, lcg.copyAndLongJump(), xbg.copyAndLongJump());
        }
    }

    /**
     * A factory for creating LXMGenerator objects.
     */
    interface LXMGeneratorFactory {
        /**
         * Return the size of the LCG long seed array.
         *
         * @return the LCG seed size
         */
        int lcgSeedSize();

        /**
         * Return the size of the XBG long seed array.
         *
         * @return the XBG seed size
         */
        int xbgSeedSize();

        /**
         * Return the size of the long seed array.
         * The default implementation adds the size of the LCG and XBG seed.
         *
         * @return the seed size
         */
        default int seedSize() {
            return lcgSeedSize() + xbgSeedSize();
        }

        /**
         * Creates a new LXMGenerator.
         *
         * <p>Tests using the LXMGenerator assume the seed is a composite containing the
         * LCG seed and then the XBG seed.
         *
         * @param seed the seed
         * @return the generator
         */
        LXMGenerator create(long[] seed);

        /**
         * Gets the mix implementation. This is used to test initial output of the generator.
         * The default implementation is {@link AbstractLXMTest#mixLea64(long, long)}.
         *
         * @return the mix
         */
        default Mix getMix() {
            return AbstractLXMTest::mixLea64;
        }
    }

    /**
     * Test the LCG implementations. These tests should execute only once, and not
     * for each instance of the abstract outer class (i.e. the test is not {@code @Nested}).
     *
     * <p>Note: The LCG implementations are not present in the main RNG core package and
     * this test ensures an LCG update of:
     * <pre>
     * s = m * s + a
     *
     * s = state
     * m = multiplier
     * a = addition
     * </pre>
     *
     * <p>A test is made to ensure the LCG can perform jump and long jump operations using
     * small jumps that can be verified by an equal number of single state updates.
     */
    static class LCGTest {
        /** 2^63. */
        private static final BigInteger TWO_POW_63 = BigInteger.ONE.shiftLeft(63);
        /** 65-bit multiplier for the 128-bit LCG. */
        private static final BigInteger M = BigInteger.ONE.shiftLeft(64).add(toUnsignedBigInteger(LXMSupport.M128L));
        /** 2^128. Used as the modulus for the 128-bit LCG. */
        private static final BigInteger MOD = BigInteger.ONE.shiftLeft(128);

        /** A count {@code k} where a jump of {@code 2^k} will wrap the LCG state. */
        private static final int NO_JUMP = -1;
        /** A count {@code k=2} for a jump of {@code 2^k}, or 4 cycles. */
        private static final int JUMP = 2;
        /** A count {@code k=4} for a long jump of {@code 2^k}, or 16 cycles. */
        private static final int LONG_JUMP = 4;

        @RepeatedTest(value = 10)
        void testLCG64DefaultJump() {
            final SplittableRandom rng = new SplittableRandom();
            final long state = rng.nextLong();
            final long add = rng.nextLong();
            final SubGen lcg1 = new LCG64(add, state);
            final SubGen lcg2 = new LCG64(add, state, 0, 32);
            for (int j = 0; j < 10; j++) {
                Assertions.assertEquals(lcg1.stateAndUpdate(), lcg2.stateAndUpdate(),
                    () -> String.format("seed %d,%d", state, add));
            }
            lcg1.copyAndJump();
            lcg2.copyAndJump();
            for (int j = 0; j < 10; j++) {
                Assertions.assertEquals(lcg1.stateAndUpdate(), lcg2.stateAndUpdate(),
                    () -> String.format("seed %d,%d", state, add));
            }
            lcg1.copyAndLongJump();
            lcg2.copyAndLongJump();
            for (int j = 0; j < 10; j++) {
                Assertions.assertEquals(lcg1.stateAndUpdate(), lcg2.stateAndUpdate(),
                    () -> String.format("seed %d,%d", state, add));
            }
        }

        @RepeatedTest(value = 10)
        void testLCG64() {
            final SplittableRandom rng = new SplittableRandom();
            final long state = rng.nextLong();
            final long add = rng.nextLong();
            long s = state;
            final long a = add | 1;
            final SubGen lcg = new LCG64(add, state, NO_JUMP, NO_JUMP);
            for (int j = 0; j < 10; j++) {
                Assertions.assertEquals(s, lcg.stateAndUpdate(),
                    () -> String.format("seed %d,%d", state, add));
                s = LXMSupport.M64 * s + a;
            }
        }

        @RepeatedTest(value = 10)
        void testLCG64Jump() {
            final SplittableRandom rng = new SplittableRandom();
            final long state = rng.nextLong();
            final long add = rng.nextLong();
            final Supplier<String> msg = () -> String.format("seed %d,%d", state, add);
            long s = state;
            final long a = add | 1;
            final SubGen lcg = new LCG64(add, state, JUMP, LONG_JUMP);

            final SubGen copy1 = lcg.copyAndJump();
            for (int j = 1 << JUMP; j-- != 0;) {
                Assertions.assertEquals(s, copy1.stateAndUpdate(), msg);
                s = LXMSupport.M64 * s + a;
            }
            Assertions.assertEquals(s, lcg.stateAndUpdate(), msg);
            s = LXMSupport.M64 * s + a;

            final SubGen copy2 = lcg.copyAndLongJump();
            for (int j = 1 << LONG_JUMP; j-- != 0;) {
                Assertions.assertEquals(s, copy2.stateAndUpdate(), msg);
                s = LXMSupport.M64 * s + a;
            }
            Assertions.assertEquals(s, lcg.stateAndUpdate(), msg);
        }

        @RepeatedTest(value = 10)
        void testLCG128DefaultJump() {
            final SplittableRandom rng = new SplittableRandom();
            final long stateh = rng.nextLong();
            final long statel = rng.nextLong();
            final long addh = rng.nextLong();
            final long addl = rng.nextLong();
            final SubGen lcg1 = new LCG128(addh, addl, stateh, statel);
            final SubGen lcg2 = new LCG128(addh, addl, stateh, statel, 0, 64);
            for (int j = 0; j < 10; j++) {
                Assertions.assertEquals(lcg1.stateAndUpdate(), lcg2.stateAndUpdate(),
                    () -> String.format("seed %d,%d,%d,%d", stateh, statel, addh, addl));
            }
            lcg1.copyAndJump();
            lcg2.copyAndJump();
            for (int j = 0; j < 10; j++) {
                Assertions.assertEquals(lcg1.stateAndUpdate(), lcg2.stateAndUpdate(),
                    () -> String.format("seed %d,%d,%d,%d", stateh, statel, addh, addl));
            }
            lcg1.copyAndLongJump();
            lcg2.copyAndLongJump();
            for (int j = 0; j < 10; j++) {
                Assertions.assertEquals(lcg1.stateAndUpdate(), lcg2.stateAndUpdate(),
                    () -> String.format("seed %d,%d,%d,%d", stateh, statel, addh, addl));
            }
        }

        @RepeatedTest(value = 10)
        void testLCG128() {
            final SplittableRandom rng = new SplittableRandom();
            final long stateh = rng.nextLong();
            final long statel = rng.nextLong();
            final long addh = rng.nextLong();
            final long addl = rng.nextLong();
            BigInteger s = toUnsignedBigInteger(stateh).shiftLeft(64).add(toUnsignedBigInteger(statel));
            final BigInteger a = toUnsignedBigInteger(addh).shiftLeft(64).add(toUnsignedBigInteger(addl | 1));
            final SubGen lcg = new LCG128(addh, addl, stateh, statel, NO_JUMP, NO_JUMP);
            for (int j = 0; j < 10; j++) {
                Assertions.assertEquals(s.shiftRight(64).longValue(), lcg.stateAndUpdate(),
                        () -> String.format("seed %d,%d,%d,%d", stateh, statel, addh, addl));
                s = M.multiply(s).add(a).mod(MOD);
            }
        }

        @RepeatedTest(value = 10)
        void testLCG128Jump() {
            final SplittableRandom rng = new SplittableRandom();
            final long stateh = rng.nextLong();
            final long statel = rng.nextLong();
            final long addh = rng.nextLong();
            final long addl = rng.nextLong();
            final Supplier<String> msg = () -> String.format("seed %d,%d,%d,%d", stateh, statel, addh, addl);
            BigInteger s = toUnsignedBigInteger(stateh).shiftLeft(64).add(toUnsignedBigInteger(statel));
            final BigInteger a = toUnsignedBigInteger(addh).shiftLeft(64).add(toUnsignedBigInteger(addl | 1));
            final SubGen lcg = new LCG128(addh, addl, stateh, statel, JUMP, LONG_JUMP);

            final SubGen copy1 = lcg.copyAndJump();
            for (int j = 1 << JUMP; j-- != 0;) {
                Assertions.assertEquals(s.shiftRight(64).longValue(), copy1.stateAndUpdate(), msg);
                s = M.multiply(s).add(a).mod(MOD);
            }
            Assertions.assertEquals(s.shiftRight(64).longValue(), lcg.stateAndUpdate(), msg);
            s = M.multiply(s).add(a).mod(MOD);

            final SubGen copy2 = lcg.copyAndLongJump();
            for (int j = 1 << LONG_JUMP; j-- != 0;) {
                Assertions.assertEquals(s.shiftRight(64).longValue(), copy2.stateAndUpdate(), msg);
                s = M.multiply(s).add(a).mod(MOD);
            }
            Assertions.assertEquals(s.shiftRight(64).longValue(), lcg.stateAndUpdate(), msg);
        }

        /**
         * Create a big integer treating the value as unsigned.
         *
         * @param v Value
         * @return the big integer
         */
        private static BigInteger toUnsignedBigInteger(long v) {
            return v < 0 ?
                TWO_POW_63.add(BigInteger.valueOf(v & Long.MAX_VALUE)) :
                BigInteger.valueOf(v);
        }
    }

    /**
     * Test the XBG implementations. These tests should execute only once, and not
     * for each instance of the abstract outer class (i.e. the test is not {@code @Nested}).
     *
     * <p>Note: The XBG implementation are extensions of RNGs already verified against
     * reference implementations. These tests ensure that the upper bits of the state is
     * identical after {@code n} cycles then a jump; or a jump then {@code n} cycles.
     * This is the functionality required for a jumpable XBG generator.
     */
    static class XBGTest {

        @RepeatedTest(value = 5)
        void testXBGXoRoShiRo128NoJump() {
            final SplittableRandom r = new SplittableRandom();
            assertNoJump(new XBGXoRoShiRo128(r.nextLong(), r.nextLong(), false));
        }

        @RepeatedTest(value = 5)
        void testXBGXoShiRo256NoJump() {
            final SplittableRandom r = new SplittableRandom();
            assertNoJump(new XBGXoShiRo256(r.nextLong(), r.nextLong(), r.nextLong(), r.nextLong(), false));
        }

        @RepeatedTest(value = 5)
        void testXBGXoRoShiRo1024NoJump() {
            final SplittableRandom r = new SplittableRandom();
            assertNoJump(new XBGXoRoShiRo1024(r.longs(16).toArray(), false));
        }

        void assertNoJump(SubGen g) {
            final SubGen g1 = g.copyAndJump();
            Assertions.assertNotSame(g, g1);
            for (int i = 0; i < 10; i++) {
                Assertions.assertEquals(g.stateAndUpdate(), g1.stateAndUpdate());
            }
            final SubGen g2 = g.copyAndLongJump();
            Assertions.assertNotSame(g, g2);
            for (int i = 0; i < 10; i++) {
                Assertions.assertEquals(g.stateAndUpdate(), g2.stateAndUpdate());
            }
        }

        @RepeatedTest(value = 5)
        void testXBGXoRoShiRo128Jump() {
            assertJumpAndCycle(ThreadLocalRandom.current().nextLong(), 2, XBGXoRoShiRo128::new, SubGen::copyAndJump);
        }

        @RepeatedTest(value = 5)
        void testXBGXoRoShiRo128LongJump() {
            assertJumpAndCycle(ThreadLocalRandom.current().nextLong(), 2, XBGXoRoShiRo128::new, SubGen::copyAndLongJump);
        }

        @RepeatedTest(value = 5)
        void testXBGXoShiRo256Jump() {
            assertJumpAndCycle(ThreadLocalRandom.current().nextLong(), 4, XBGXoShiRo256::new, SubGen::copyAndJump);
        }

        @RepeatedTest(value = 5)
        void testXBGXShiRo256LongJump() {
            assertJumpAndCycle(ThreadLocalRandom.current().nextLong(), 4, XBGXoShiRo256::new, SubGen::copyAndLongJump);
        }

        @RepeatedTest(value = 5)
        void testXBGXoRoShiRo1024Jump() {
            assertJumpAndCycle(ThreadLocalRandom.current().nextLong(), 16, XBGXoRoShiRo1024::new, SubGen::copyAndJump);
        }

        @RepeatedTest(value = 5)
        void testXBGXoRoShiRo1024LongJump() {
            assertJumpAndCycle(ThreadLocalRandom.current().nextLong(), 16, XBGXoRoShiRo1024::new, SubGen::copyAndLongJump);
        }

        /**
         * Assert alternating jump and cycle of the XBG.
         *
         * <p>This test verifies one generator can jump and advance {@code n} steps
         * while the other generator advances {@code n} steps then jumps. The two should
         * be in the same state. The copy left behind by the first jump should match the
         * state of the other generator as it cycles.
         *
         * @param testSeed A seed for the test
         * @param seedSize The seed size
         * @param factory Factory to create the XBG
         * @param jump XBG jump function
         */
        private static void assertJumpAndCycle(long testSeed, int seedSize,
                Function<long[], SubGen> factory, UnaryOperator<SubGen> jump) {
            final SplittableRandom r = new SplittableRandom(testSeed);
            final long[] seed = r.longs(seedSize).toArray();
            final int[] steps = createSteps(r, seedSize);
            final int cycles = 2 * seedSize;

            final SubGen rng1 = factory.apply(seed);
            final SubGen rng2 = factory.apply(seed);

            // Try jumping and stepping with a number of steps up to the seed size
            for (int i = 0; i < steps.length; i++) {
                final int step = steps[i];
                final int index = i;
                // Jump 1
                final SubGen copy = jump.apply(rng1);
                // Advance 2; it should match the copy
                for (int j = 0; j < step; j++) {
                    Assertions.assertEquals(rng2.stateAndUpdate(), copy.stateAndUpdate(),
                        () -> String.format("[%d] Incorrect trailing copy, seed=%d", index, testSeed));
                    // Advance in parallel
                    rng1.stateAndUpdate();
                }
                // Catch up 2; it should match 1
                jump.apply(rng2);
                for (int j = 0; j < cycles; j++) {
                    Assertions.assertEquals(rng1.stateAndUpdate(), rng2.stateAndUpdate(),
                        () -> String.format("[%d] Incorrect after catch up jump, seed=%d", index, testSeed));
                }
            }
        }
    }

    /////////////////////////////////////
    // Tests for the LXM implementation
    /////////////////////////////////////

    /**
     * Gets the factory to create a composite LXM generator.
     * The generator is used to provide reference output for the generator under test.
     *
     * @return the factory
     */
    abstract LXMGeneratorFactory getFactory();

    /**
     * Creates a new instance of the RNG under test.
     *
     * @param seed the seed
     * @return the RNG
     */
    abstract LongJumpableUniformRandomProvider create(long[] seed);

    /**
     * Gets a stream of reference data. Each Argument consist of the long array seed and
     * the long array of the expected output from the LXM generator.
     * <pre>
     * long[] seed;
     * long[] expected;
     * return Stream.of(Arguments.of(seed, expected));
     * </pre>
     *
     * @return the reference data
     */
    abstract Stream<Arguments> getReferenceData();

    /**
     * Test the reference data with the LXM generator.
     * The reference data should be created with a reference implementation of the
     * LXM algorithm, for example the implementations in JDK 17.
     */
    @ParameterizedTest
    @MethodSource(value = "getReferenceData")
    final void testReferenceData(long[] seed, long[] expected) {
        RandomAssert.assertEquals(expected, create(seed));
    }

    /**
     * Test the reference data with the composite LXM generator. This ensures the composite
     * correctly implements the LXM algorithm.
     */
    @ParameterizedTest
    @MethodSource(value = "getReferenceData")
    final void testReferenceDataWithComposite(long[] seed, long[] expected) {
        RandomAssert.assertEquals(expected, getFactory().create(seed));
    }

    /**
     * Test initial output is the mix of the most significant bits of the LCG and XBG state.
     * This test ensures the LXM algorithm applies the mix function to the current state
     * before state update. Thus the initial output should be a direct mix of the seed state.
     */
    @RepeatedTest(value = 5)
    final void testInitialOutput() {
        final long[] seed = createRandomSeed();
        // Upper 64 bits of LCG state
        final long s = seed[getFactory().lcgSeedSize() / 2];
        // Upper 64 bits of XBG state
        final long t = seed[getFactory().lcgSeedSize()];
        Assertions.assertEquals(getFactory().getMix().apply(s, t), create(seed).nextLong());
    }

    @Test
    final void testConstructorWithZeroSeedIsPartiallyFunctional() {
        // Note: It is impractical to demonstrate the RNG is partially functional:
        // output quality may be statistically poor and the period is that of the LCG.
        // The test demonstrates the RNG is not totally non-functional with a zero seed
        // which is not the case for a standard XBG.
        final int seedSize = getFactory().seedSize();
        RandomAssert.assertNextLongNonZeroOutput(create(new long[seedSize]), 0, seedSize);
    }

    @ParameterizedTest
    @MethodSource(value = "getReferenceData")
    final void testConstructorWithoutFullLengthSeed(long[] seed) {
        // Hit the case when the input seed is self-seeded when not full length
        final int seedSize = getFactory().seedSize();
        RandomAssert.assertNextLongNonZeroOutput(create(new long[] {seed[0]}),
                seedSize, seedSize);
    }

    @RepeatedTest(value = 5)
    final void testConstructorIgnoresFinalAddParameterSeedBit() {
        final long[] seed1 = createRandomSeed();
        final long[] seed2 = seed1.clone();
        final int seedSize = getFactory().seedSize();
        // seed1 unset final add parameter bit; seed2 set final bit
        final int index = getFactory().lcgSeedSize() / 2 - 1;
        seed1[index] &= -1L << 1;
        seed2[index] |= 1;
        Assertions.assertEquals(1, seed1[index] ^ seed2[index]);
        final LongJumpableUniformRandomProvider rng1 = create(seed1);
        final LongJumpableUniformRandomProvider rng2 = create(seed2);
        RandomAssert.assertNextLongEquals(seedSize * 2, rng1, rng2);
    }

    @ParameterizedTest
    @MethodSource(value = "getReferenceData")
    final void testJump(long[] seed, long[] expected) {
        final long[] expectedAfter = createExpectedSequence(seed, expected.length, false);
        RandomAssert.assertJumpEquals(expected, expectedAfter, create(seed));
    }

    @ParameterizedTest
    @MethodSource(value = "getReferenceData")
    final void testLongJump(long[] seed, long[] expected) {
        final long[] expectedAfter = createExpectedSequence(seed, expected.length, true);
        RandomAssert.assertLongJumpEquals(expected, expectedAfter, create(seed));
    }

    @RepeatedTest(value = 5)
    final void testJumpAndOutput() {
        assertJumpAndOutput(false, ThreadLocalRandom.current().nextLong());
    }

    @RepeatedTest(value = 5)
    final void testLongJumpAndOutput() {
        assertJumpAndOutput(true, ThreadLocalRandom.current().nextLong());
    }

    /**
     * Assert alternating jump and output from the LXM generator and the
     * reference composite LXM generator.
     *
     * <p>The composite generator uses an LCG that is tested to match
     * output after a jump (see {@link LCGTest} or a series of cycles.
     * The XBG generator is <em>assumed</em> to function similarly.
     * The {@link XBGTest} verifies that the generator is in the same
     * state when using {@code n} steps then a jump; or a jump then
     * {@code n} steps.
     *
     * <p>This test verifies one LXM generator can jump and advance
     * {@code n} steps while the other generator advances {@code n}
     * steps then jumps. The two should be in the same state. The copy
     * left behind by the first jump should match the output of the other
     * generator as it cycles.
     *
     * @param longJump If true use the long jump; otherwise the jump
     * @param testSeed A seed for the test
     */
    private void assertJumpAndOutput(boolean longJump, long testSeed) {
        final SplittableRandom r = new SplittableRandom(testSeed);
        final long[] seed = createRandomSeed(r::nextLong);
        final int[] steps = createSteps(r);
        final int cycles = getFactory().seedSize();

        LongJumpableUniformRandomProvider rng1 = create(seed);
        LongJumpableUniformRandomProvider rng2 = getFactory().create(seed);

        final UnaryOperator<LongJumpableUniformRandomProvider> jump = longJump ?
            x -> (LongJumpableUniformRandomProvider) x.longJump() :
            x -> (LongJumpableUniformRandomProvider) x.jump();

        // Alternate jumping and stepping then stepping and jumping.
        for (int i = 0; i < steps.length; i++) {
            final int step = steps[i];
            final int index = i;
            // Jump 1
            LongJumpableUniformRandomProvider copy = jump.apply(rng1);
            // Advance 2; it should match the copy
            for (int j = 0; j < step; j++) {
                Assertions.assertEquals(rng2.nextLong(), copy.nextLong(),
                    () -> String.format("[%d] Incorrect trailing copy, seed=%d", index, testSeed));
                // Advance 1 in parallel
                rng1.nextLong();
            }
            // Catch up 2; it should match 1
            jump.apply(rng2);
            for (int j = 0; j < cycles; j++) {
                Assertions.assertEquals(rng1.nextLong(), rng2.nextLong(),
                    () -> String.format("[%d] Incorrect after catch up jump, seed=%d", index, testSeed));
            }

            // Swap
            copy = rng1;
            rng1 = rng2;
            rng2 = copy;
        }
    }

    @RepeatedTest(value = 5)
    final void testSaveRestoreAfterJump() {
        assertSaveRestoreAfterJump(false, ThreadLocalRandom.current().nextLong());
    }

    @RepeatedTest(value = 5)
    final void testSaveRestoreAfterLongJump() {
        assertSaveRestoreAfterJump(true, ThreadLocalRandom.current().nextLong());
    }

    /**
     * Assert that the precomputation of the jump coefficients functions
     * with saved/restore.
     *
     * @param longJump If true use the long jump; otherwise the jump
     * @param testSeed A seed for the test
     */
    private void assertSaveRestoreAfterJump(boolean longJump, long testSeed) {
        final SplittableRandom r = new SplittableRandom(testSeed);
        final int cycles = getFactory().seedSize();

        final UnaryOperator<LongJumpableUniformRandomProvider> jump = longJump ?
            x -> (LongJumpableUniformRandomProvider) x.longJump() :
            x -> (LongJumpableUniformRandomProvider) x.jump();

        // Create 2 generators and jump them
        final LongJumpableUniformRandomProvider rng1 = create(createRandomSeed(r::nextLong));
        final LongJumpableUniformRandomProvider rng2 = create(createRandomSeed(r::nextLong));
        jump.apply(rng1);
        jump.apply(rng2);

        // Restore the state from one to the other
        RestorableUniformRandomProvider g1 = (RestorableUniformRandomProvider) rng1;
        RestorableUniformRandomProvider g2 = (RestorableUniformRandomProvider) rng2;
        g2.restoreState(g1.saveState());

        // They should have the same output
        RandomAssert.assertNextLongEquals(cycles, rng1, rng2);

        // They should have the same output after a jump too
        jump.apply(rng1);
        jump.apply(rng2);

        RandomAssert.assertNextLongEquals(cycles, rng1, rng2);
    }

    /**
     * Create the expected sequence after a jump by advancing the XBG and LCG
     * sub-generators. This is done by creating and jumping the composite LXM
     * generator.
     *
     * @param seed the seed
     * @param length the length of the expected sequence
     * @param longJump If true use the long jump; otherwise the jump
     * @return the expected sequence
     */
    private long[] createExpectedSequence(long[] seed, int length, boolean longJump) {
        final LXMGenerator rng = getFactory().create(seed);
        if (longJump) {
            rng.longJump();
        } else {
            rng.jump();
        }
        return LongStream.generate(rng::nextLong).limit(length).toArray();
    }

    /**
     * Creates a random seed of the correct length. Used when the seed can be anything.
     *
     * @return the seed
     */
    private long[] createRandomSeed() {
        return createRandomSeed(new SplittableRandom()::nextLong);
    }

    /**
     * Creates a random seed of the correct length using the provided generator.
     * Used when the seed can be anything.
     *
     * @param gen the generator
     * @return the seed
     */
    private long[] createRandomSeed(LongSupplier gen) {
        return LongStream.generate(gen).limit(getFactory().seedSize()).toArray();
    }

    /**
     * Creates a random permutation of steps in [1, seed size].
     * The seed size is obtained from the LXM generator factory.
     *
     * @param rng Source of randomness
     * @return the steps
     */
    private int[] createSteps(SplittableRandom rng) {
        return createSteps(rng, getFactory().seedSize());
    }

    /**
     * Creates a random permutation of steps in [1, seed size].
     *
     * @param rng Source of randomness
     * @param seedSize Seed size
     * @return the steps
     */
    private static int[] createSteps(SplittableRandom rng, int seedSize) {
        final int[] steps = IntStream.rangeClosed(1, seedSize).toArray();
        // Fisher-Yates shuffle
        for (int i = steps.length; i > 1; i--) {
            final int j = rng.nextInt(i);
            final int tmp = steps[i - 1];
            steps[i - 1] = steps[j];
            steps[j] = tmp;
        }
        return steps;
    }
}
