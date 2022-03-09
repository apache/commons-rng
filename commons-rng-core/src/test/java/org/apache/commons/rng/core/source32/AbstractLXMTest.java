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
package org.apache.commons.rng.core.source32;

import java.util.SplittableRandom;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import java.util.function.IntSupplier;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.apache.commons.rng.LongJumpableUniformRandomProvider;
import org.apache.commons.rng.RestorableUniformRandomProvider;
import org.apache.commons.rng.core.RandomAssert;
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
 * both using w = 32-bits. Tests for a RNG implementation must define
 * a factory for constructing a reference composite LXM generator. Implementations
 * for the 32-bit LCG used in the LXM family are provided.
 * Implementations for the XBG are provided based on version in the Commons RNG core library.
 *
 * <p>It is assumed the XBG generator may be a {@link LongJumpableUniformRandomProvider}.
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
 * <p>The paper by Steele and Vigna suggest advancing the LCG to take advantage of
 * the fast update step of the LCG. If the LCG and XBG sub-generators support
 * jump/longJump then the composite can then be used to test arbitrary
 * combinations of calls to: generate the next long value; and jump operations.
 * This is not possible using the reference implementations of the LXM family in
 * JDK 17 which do not implement jumping (instead providing a split operation to
 * create new RNGs).
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
 * parameter. The difference must be in the high 31-bits of the seed value. A suitable filling
 * procedure from an initial seed value is provided in the Commons RNG Simple module.
 *
 * <p>The class uses a per-class lifecycle. This allows abstract methods to be overridden
 * in implementing classes to control test behaviour.
 */
@TestInstance(Lifecycle.PER_CLASS)
abstract class AbstractLXMTest {
    /**
     * A function to mix two int values.
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
        int apply(int a, int b);
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
         * Return the upper 32-bits of the current state and update the state.
         *
         * @return the upper 32-bits of the old state
         */
        int stateAndUpdate();

        /**
         * Create a copy and then advance the state in a single jump; the copy is returned.
         *
         * @return the copy
         */
        SubGen copyAndJump();

        /**
         * Create a copy and then advance the state in a single int jump; the copy is returned.
         *
         * @return the copy
         */
        SubGen copyAndLongJump();
    }

    /**
     * Mix the sum using the lea32 function.
     *
     * @param a Value a
     * @param b Value b
     * @return the result
     */
    static int mixLea32(int a, int b) {
        return LXMSupport.lea32(a + b);
    }

    /**
     * A 32-bit linear congruential generator.
     */
    static class LCG32 implements SubGen {
        /** Multiplier. */
        private static final int M = LXMSupport.M32;
        /** Additive parameter (must be odd). */
        private final int a;
        /** State. */
        private int s;
        /** Power of 2 for the jump. */
        private final int jumpPower;
        /** Power of 2 for the long jump. */
        private final int longJumpPower;

        /**
         * Create an instance with a jump of 1 cycle and int jump of 2^32 cycles.
         *
         * @param a Additive parameter (set to odd)
         * @param s State
         */
        LCG32(int a, int s) {
            this(a, s, 0, 16);
        }

        /**
         * @param a Additive parameter (set to odd)
         * @param s State
         * @param jumpPower Jump size as a power of 2
         * @param longJumpPower Long jump size as a power of 2
         */
        LCG32(int a, int s, int jumpPower, int longJumpPower) {
            this.a = a | 1;
            this.s = s;
            this.jumpPower = jumpPower;
            this.longJumpPower = longJumpPower;
        }

        @Override
        public int stateAndUpdate() {
            final int s0 = s;
            s = M * s + a;
            return s0;
        }

        @Override
        public SubGen copyAndJump() {
            final SubGen copy = new LCG32(a, s, jumpPower, longJumpPower);
            s = LXMSupportTest.lcgAdvancePow2(s, M, a, jumpPower);
            return copy;
        }

        @Override
        public SubGen copyAndLongJump() {
            final SubGen copy = new LCG32(a, s, jumpPower, longJumpPower);
            s = LXMSupportTest.lcgAdvancePow2(s, M, a, longJumpPower);
            return copy;
        }
    }

    /**
     * A xor-based generator using XoRoShiRo64.
     *
     * <p>The generator does not support jumps and simply returns a copy.
     */
    static class XBGXoRoShiRo64 extends AbstractXoRoShiRo64 implements SubGen {
        /**
         * Create an instance (note that jumping is not supported).
         *
         * @param seed seed
         */
        XBGXoRoShiRo64(int[] seed) {
            super(seed);
        }

        /**
         * @param seed0 seed element 0
         * @param seed1 seed element 1
         */
        XBGXoRoShiRo64(int seed0, int seed1) {
            super(seed0, seed1);
        }

        /**
         * Copy constructor.
         *
         * @param source the source to copy
         */
        XBGXoRoShiRo64(XBGXoRoShiRo64 source) {
            // There is no super-class copy constructor so just construct
            // from the RNG state.
            // Warning:
            // This will not copy the cached state from source.
            // This only matters if tests use nextBoolean.
            this(source.state0, source.state1);
        }

        @Override
        public int stateAndUpdate() {
            final int s0 = state0;
            next();
            return s0;
        }

        @Override
        public SubGen copyAndJump() {
            // No jump function
            return new XBGXoRoShiRo64(this);
        }

        @Override
        public SubGen copyAndLongJump() {
            // No jump function
            return new XBGXoRoShiRo64(this);
        }

        @Override
        protected int nextOutput() {
            // Not used
            return 0;
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
     * <p>w is assumed to be 32-bits.
     */
    static class LXMGenerator extends IntProvider implements LongJumpableUniformRandomProvider {
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
        public int next() {
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
        LXMGenerator create(int[] seed);

        /**
         * Gets the mix implementation. This is used to test initial output of the generator.
         * The default implementation is {@link AbstractLXMTest#mixLea32(int, int)}.
         *
         * @return the mix
         */
        default Mix getMix() {
            return AbstractLXMTest::mixLea32;
        }
    }

    /**
     * Test the LCG implementations. These tests should execute only once, and not
     * for each instance of the abstract outer class (i.e. the test is not {@code @Nested}).
     *
     * <p>Note: The LCG implementation is not present in the main RNG core package and
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
        /** A count {@code k} where a jump of {@code 2^k} will wrap the LCG state. */
        private static final int NO_JUMP = -1;
        /** A count {@code k=2} for a jump of {@code 2^k}, or 4 cycles. */
        private static final int JUMP = 2;
        /** A count {@code k=4} for a long jump of {@code 2^k}, or 16 cycles. */
        private static final int LONG_JUMP = 4;

        @RepeatedTest(value = 10)
        void testLCG32DefaultJump() {
            final SplittableRandom rng = new SplittableRandom();
            final int state = rng.nextInt();
            final int add = rng.nextInt();
            final SubGen lcg1 = new LCG32(add, state);
            final SubGen lcg2 = new LCG32(add, state, 0, 16);
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
        void testLCG32() {
            final SplittableRandom rng = new SplittableRandom();
            final int state = rng.nextInt();
            final int add = rng.nextInt();
            int s = state;
            final int a = add | 1;
            final SubGen lcg = new LCG32(add, state, NO_JUMP, NO_JUMP);
            for (int j = 0; j < 10; j++) {
                Assertions.assertEquals(s, lcg.stateAndUpdate(),
                    () -> String.format("seed %d,%d", state, add));
                s = LXMSupport.M32 * s + a;
            }
        }

        @RepeatedTest(value = 10)
        void testLCG32Jump() {
            final SplittableRandom rng = new SplittableRandom();
            final int state = rng.nextInt();
            final int add = rng.nextInt();
            final Supplier<String> msg = () -> String.format("seed %d,%d", state, add);
            int s = state;
            final int a = add | 1;
            final SubGen lcg = new LCG32(add, state, JUMP, LONG_JUMP);

            final SubGen copy1 = lcg.copyAndJump();
            for (int j = 1 << JUMP; j-- != 0;) {
                Assertions.assertEquals(s, copy1.stateAndUpdate(), msg);
                s = LXMSupport.M32 * s + a;
            }
            Assertions.assertEquals(s, lcg.stateAndUpdate(), msg);
            s = LXMSupport.M32 * s + a;

            final SubGen copy2 = lcg.copyAndLongJump();
            for (int j = 1 << LONG_JUMP; j-- != 0;) {
                Assertions.assertEquals(s, copy2.stateAndUpdate(), msg);
                s = LXMSupport.M32 * s + a;
            }
            Assertions.assertEquals(s, lcg.stateAndUpdate(), msg);
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
        void testXBGXoRoShiRo64NoJump() {
            final SplittableRandom r = new SplittableRandom();
            assertNoJump(new XBGXoRoShiRo64(r.nextInt(), r.nextInt()));
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

        // Note: These test are duplicates of the XBGTest in the source64 package
        // which does have jumpable XBG sub-generators. The tests work even when
        // the jump is not implemented and is a simple copy operation.

        @RepeatedTest(value = 5)
        void testXBGXoRoShiRo64Jump() {
            assertJumpAndCycle(ThreadLocalRandom.current().nextLong(), 2, XBGXoRoShiRo64::new, SubGen::copyAndJump);
        }

        @RepeatedTest(value = 5)
        void testXBGXoRoShiRo64LongJump() {
            assertJumpAndCycle(ThreadLocalRandom.current().nextLong(), 2, XBGXoRoShiRo64::new, SubGen::copyAndLongJump);
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
                Function<int[], SubGen> factory, UnaryOperator<SubGen> jump) {
            final SplittableRandom r = new SplittableRandom(testSeed);
            final int[] seed = r.ints(seedSize).toArray();
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
    abstract LongJumpableUniformRandomProvider create(int[] seed);

    /**
     * Gets a stream of reference data. Each Argument consist of the int array seed and
     * the int array of the expected output from the LXM generator.
     * <pre>
     * int[] seed;
     * int[] expected;
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
    final void testReferenceData(int[] seed, int[] expected) {
        RandomAssert.assertEquals(expected, create(seed));
    }

    /**
     * Test the reference data with the composite LXM generator. This ensures the composite
     * correctly implements the LXM algorithm.
     */
    @ParameterizedTest
    @MethodSource(value = "getReferenceData")
    final void testReferenceDataWithComposite(int[] seed, int[] expected) {
        RandomAssert.assertEquals(expected, getFactory().create(seed));
    }

    /**
     * Test initial output is the mix of the most significant bits of the LCG and XBG state.
     * This test ensures the LXM algorithm applies the mix function to the current state
     * before state update. Thus the initial output should be a direct mix of the seed state.
     */
    @RepeatedTest(value = 5)
    final void testInitialOutput() {
        final int[] seed = createRandomSeed();
        // Upper 32 bits of LCG state
        final int s = seed[getFactory().lcgSeedSize() / 2];
        // Upper 32 bits of XBG state
        final int t = seed[getFactory().lcgSeedSize()];
        Assertions.assertEquals(getFactory().getMix().apply(s, t), create(seed).nextInt());
    }

    @Test
    final void testConstructorWithZeroSeedIsPartiallyFunctional() {
        // Note: It is impractical to demonstrate the RNG is partially functional:
        // output quality may be statistically poor and the period is that of the LCG.
        // The test demonstrates the RNG is not totally non-functional with a zero seed
        // which is not the case for a standard XBG.
        final int seedSize = getFactory().seedSize();
        RandomAssert.assertNextLongNonZeroOutput(create(new int[seedSize]), 0, seedSize);
    }

    @ParameterizedTest
    @MethodSource(value = "getReferenceData")
    final void testConstructorWithoutFullLengthSeed(int[] seed) {
        // Hit the case when the input seed is self-seeded when not full length
        final int seedSize = getFactory().seedSize();
        RandomAssert.assertNextIntNonZeroOutput(create(new int[] {seed[0]}),
                seedSize, seedSize);
    }

    @RepeatedTest(value = 5)
    final void testConstructorIgnoresFinalAddParameterSeedBit() {
        final int[] seed1 = createRandomSeed();
        final int[] seed2 = seed1.clone();
        final int seedSize = getFactory().seedSize();
        // seed1 unset final add parameter bit; seed2 set final bit
        final int index = getFactory().lcgSeedSize() / 2 - 1;
        seed1[index] &= -1 << 1;
        seed2[index] |= 1;
        Assertions.assertEquals(1, seed1[index] ^ seed2[index]);
        final LongJumpableUniformRandomProvider rng1 = create(seed1);
        final LongJumpableUniformRandomProvider rng2 = create(seed2);
        RandomAssert.assertNextLongEquals(seedSize * 2, rng1, rng2);
    }

    @ParameterizedTest
    @MethodSource(value = "getReferenceData")
    final void testJump(int[] seed, int[] expected) {
        final int[] expectedAfter = createExpectedSequence(seed, expected.length, false);
        RandomAssert.assertJumpEquals(expected, expectedAfter, create(seed));
    }

    @ParameterizedTest
    @MethodSource(value = "getReferenceData")
    final void testLongJump(int[] seed, int[] expected) {
        final int[] expectedAfter = createExpectedSequence(seed, expected.length, true);
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
        final int[] seed = createRandomSeed(r::nextInt);
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
                Assertions.assertEquals(rng2.nextInt(), copy.nextInt(),
                    () -> String.format("[%d] Incorrect trailing copy, seed=%d", index, testSeed));
                // Advance 1 in parallel
                rng1.nextInt();
            }
            // Catch up 2; it should match 1
            jump.apply(rng2);
            for (int j = 0; j < cycles; j++) {
                Assertions.assertEquals(rng1.nextInt(), rng2.nextInt(),
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
        final LongJumpableUniformRandomProvider rng1 = create(createRandomSeed(r::nextInt));
        final LongJumpableUniformRandomProvider rng2 = create(createRandomSeed(r::nextInt));
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
    private int[] createExpectedSequence(int[] seed, int length, boolean longJump) {
        final LXMGenerator rng = getFactory().create(seed);
        if (longJump) {
            rng.longJump();
        } else {
            rng.jump();
        }
        return IntStream.generate(rng::nextInt).limit(length).toArray();
    }

    /**
     * Creates a random seed of the correct length. Used when the seed can be anything.
     *
     * @return the seed
     */
    private int[] createRandomSeed() {
        return createRandomSeed(new SplittableRandom()::nextInt);
    }

    /**
     * Creates a random seed of the correct length using the provided generator.
     * Used when the seed can be anything.
     *
     * @param gen the generator
     * @return the seed
     */
    private int[] createRandomSeed(IntSupplier gen) {
        return IntStream.generate(gen).limit(getFactory().seedSize()).toArray();
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
