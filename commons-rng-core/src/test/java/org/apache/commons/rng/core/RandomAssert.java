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

package org.apache.commons.rng.core;

import org.junit.jupiter.api.Assertions;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import org.apache.commons.rng.JumpableUniformRandomProvider;
import org.apache.commons.rng.LongJumpableUniformRandomProvider;
import org.apache.commons.rng.UniformRandomProvider;

/**
 * Utility class for testing random generators.
 */
public final class RandomAssert {
    /**
     * Class contains only static methods.
     */
    private RandomAssert() {}

    /**
     * Assert that the random generator produces the expected output using
     * {@link UniformRandomProvider#nextInt()}.
     *
     * @param expected Expected output.
     * @param rng Random generator.
     */
    public static void assertEquals(int[] expected, UniformRandomProvider rng) {
        assertEquals(expected, rng, "Value at position ");
    }

    /**
     * Assert that the random generator produces the expected output using
     * {@link UniformRandomProvider#nextLong()}.
     *
     * @param expected Expected output.
     * @param rng Random generator.
     */
    public static void assertEquals(long[] expected, UniformRandomProvider rng) {
        assertEquals(expected, rng, "Value at position ");
    }

    /**
     * Assert that the random generator produces the expected output using
     * {@link UniformRandomProvider#nextInt()}.
     * The message prefix is prepended to the array index for the assertion message.
     *
     * @param expected Expected output.
     * @param rng Random generator.
     * @param messagePrefix Message prefix.
     */
    private static void assertEquals(int[] expected, UniformRandomProvider rng, String messagePrefix) {
        for (int i = 0; i < expected.length; i++) {
            final int index = i;
            Assertions.assertEquals(expected[i], rng.nextInt(), () -> messagePrefix + index);
        }
    }

    /**
     * Assert that the random generator produces the expected output using
     * {@link UniformRandomProvider#nextLong()}.
     * The message prefix is prepended to the array index for the assertion message.
     *
     * @param expected Expected output.
     * @param rng Random generator.
     * @param messagePrefix Message prefix.
     */
    private static void assertEquals(long[] expected, UniformRandomProvider rng, String messagePrefix) {
        for (int i = 0; i < expected.length; i++) {
            final int index = i;
            Assertions.assertEquals(expected[i], rng.nextLong(), () -> messagePrefix + index);
        }
    }

    /**
     * Assert that the random generator produces a <strong>different</strong> output using
     * {@link UniformRandomProvider#nextLong()} to the expected output.
     *
     * @param expected Expected output.
     * @param rng Random generator.
     */
    public static void assertNotEquals(long[] expected, UniformRandomProvider rng) {
        for (int i = 0; i < expected.length; i++) {
            if (expected[i] != rng.nextLong()) {
                return;
            }
        }
        Assertions.fail("Expected a different nextLong output");
    }

    /**
     * Assert that the random generator satisfies the contract of the
     * {@link JumpableUniformRandomProvider#jump()} function.
     *
     * <ul>
     *  <li>The jump returns a copy instance. This is asserted to be a different object
     *      of the same class type as the input.
     *  <li>The copy instance outputs the expected sequence for the current state of the input generator.
     *      This is asserted using the {@code expectedBefore} sequence.
     *  <li>The input instance outputs the expected sequence for an advanced state.
     *      This is asserted using the {@code expectedAfter} sequence.
     * <ul>
     *
     * @param expectedBefore Expected output before the jump.
     * @param expectedAfter Expected output after the jump.
     * @param rng Random generator.
     */
    public static void assertJumpEquals(int[] expectedBefore,
                                        int[] expectedAfter,
                                        JumpableUniformRandomProvider rng) {
        final UniformRandomProvider copy = rng.jump();
        Assertions.assertNotSame(rng, copy, "The copy instance should be a different object");
        Assertions.assertEquals(rng.getClass(), copy.getClass(), "The copy instance should be the same class");
        assertEquals(expectedBefore, copy, "Pre-jump value at position ");
        assertEquals(expectedAfter, rng, "Post-jump value at position ");
    }

    /**
     * Assert that the random generator satisfies the contract of the
     * {@link JumpableUniformRandomProvider#jump()} function.
     *
     * <ul>
     *  <li>The jump returns a copy instance. This is asserted to be a different object
     *      of the same class type as the input.
     *  <li>The copy instance outputs the expected sequence for the current state of the input generator.
     *      This is asserted using the {@code expectedBefore} sequence.
     *  <li>The input instance outputs the expected sequence for an advanced state.
     *      This is asserted using the {@code expectedAfter} sequence.
     * <ul>
     *
     * @param expectedBefore Expected output before the jump.
     * @param expectedAfter Expected output after the jump.
     * @param rng Random generator.
     */
    public static void assertJumpEquals(long[] expectedBefore,
                                        long[] expectedAfter,
                                        JumpableUniformRandomProvider rng) {
        final UniformRandomProvider copy = rng.jump();
        Assertions.assertNotSame(rng, copy, "The copy instance should be a different object");
        Assertions.assertEquals(rng.getClass(), copy.getClass(), "The copy instance should be the same class");
        assertEquals(expectedBefore, copy, "Pre-jump value at position ");
        assertEquals(expectedAfter, rng, "Post-jump value at position ");
    }

    /**
     * Assert that the random generator satisfies the contract of the
     * {@link LongJumpableUniformRandomProvider#longJump()} function.
     *
     * <ul>
     *  <li>The long jump returns a copy instance. This is asserted to be a different object
     *      of the same class type as the input.
     *  <li>The copy instance outputs the expected sequence for the current state of the input generator.
     *      This is asserted using the {@code expectedBefore} sequence.
     *  <li>The input instance outputs the expected sequence for an advanced state.
     *      This is asserted using the {@code expectedAfter} sequence.
     * <ul>
     *
     * @param expectedBefore Expected output before the long jump.
     * @param expectedAfter Expected output after the long jump.
     * @param rng Random generator.
     */
    public static void assertLongJumpEquals(int[] expectedBefore,
                                            int[] expectedAfter,
                                            LongJumpableUniformRandomProvider rng) {
        final UniformRandomProvider copy = rng.longJump();
        Assertions.assertNotSame(rng, copy, "The copy instance should be a different object");
        Assertions.assertEquals(rng.getClass(), copy.getClass(), "The copy instance should be the same class");
        assertEquals(expectedBefore, copy, "Pre-jump value at position ");
        assertEquals(expectedAfter, rng, "Post-jump value at position ");
    }

    /**
     * Assert that the random generator satisfies the contract of the
     * {@link LongJumpableUniformRandomProvider#longJump()} function.
     *
     * <ul>
     *  <li>The long jump returns a copy instance. This is asserted to be a different object
     *      of the same class type as the input.
     *  <li>The copy instance outputs the expected sequence for the current state of the input generator.
     *      This is asserted using the {@code expectedBefore} sequence.
     *  <li>The input instance outputs the expected sequence for an advanced state.
     *      This is asserted using the {@code expectedAfter} sequence.
     * <ul>
     *
     * @param expectedBefore Expected output before the long jump.
     * @param expectedAfter Expected output after the long jump.
     * @param rng Random generator.
     */
    public static void assertLongJumpEquals(long[] expectedBefore,
                                            long[] expectedAfter,
                                            LongJumpableUniformRandomProvider rng) {
        final UniformRandomProvider copy = rng.longJump();
        Assertions.assertNotSame(rng, copy, "The copy instance should be a different object");
        Assertions.assertEquals(rng.getClass(), copy.getClass(), "The copy instance should be the same class");
        assertEquals(expectedBefore, copy, "Pre-jump value at position ");
        assertEquals(expectedAfter, rng, "Post-jump value at position ");
    }

    /**
     * Assert that the two random generators produce the same output for
     * {@link UniformRandomProvider#nextInt()} over the given number of cycles.
     *
     * @param cycles Number of cycles.
     * @param rng1 Random generator 1.
     * @param rng2 Random generator 2.
     */
    public static void assertNextIntEquals(int cycles, UniformRandomProvider rng1, UniformRandomProvider rng2) {
        for (int i = 0; i < cycles; i++) {
            final int index = i;
            Assertions.assertEquals(rng1.nextInt(), rng2.nextInt(),
                () -> String.format("%s vs %s: Value at position %d", rng1, rng2, index));
        }
    }

    /**
     * Assert that the two random generators produce the same output for
     * {@link UniformRandomProvider#nextLong()} over the given number of cycles.
     *
     * @param cycles Number of cycles.
     * @param rng1 Random generator 1.
     * @param rng2 Random generator 2.
     */
    public static void assertNextLongEquals(int cycles, UniformRandomProvider rng1, UniformRandomProvider rng2) {
        for (int i = 0; i < cycles; i++) {
            final int index = i;
            Assertions.assertEquals(rng1.nextLong(), rng2.nextLong(),
                () -> String.format("%s vs %s: Value at position %d", rng1, rng2, index));
        }
    }

    /**
     * Assert that the two random generators produce a different output for
     * {@link UniformRandomProvider#nextInt()} over the given number of cycles.
     *
     * @param cycles Number of cycles.
     * @param rng1 Random generator 1.
     * @param rng2 Random generator 2.
     */
    public static void assertNextIntNotEquals(int cycles, UniformRandomProvider rng1, UniformRandomProvider rng2) {
        for (int i = 0; i < cycles; i++) {
            if (rng1.nextInt() != rng2.nextInt()) {
                return;
            }
        }
        Assertions.fail(
            () -> String.format("%s vs %s: %d cycles of nextInt has same output", rng1, rng2, cycles));
    }

    /**
     * Assert that the two random generators produce a different output for
     * {@link UniformRandomProvider#nextLong()} over the given number of cycles.
     *
     * @param cycles Number of cycles.
     * @param rng1 Random generator 1.
     * @param rng2 Random generator 2.
     */
    public static void assertNextLongNotEquals(int cycles, UniformRandomProvider rng1, UniformRandomProvider rng2) {
        for (int i = 0; i < cycles; i++) {
            if (rng1.nextLong() != rng2.nextLong()) {
                return;
            }
        }
        Assertions.fail(
            () -> String.format("%s vs %s: %d cycles of nextLong has same output", rng1, rng2, cycles));
    }

    /**
     * Assert that the random generator produces zero output for
     * {@link UniformRandomProvider#nextInt()} over the given number of cycles.
     * This is used to test a poorly seeded generator cannot generate random output.
     *
     * @param rng Random generator.
     * @param cycles Number of cycles.
     */
    public static void assertNextIntZeroOutput(UniformRandomProvider rng, int cycles) {
        for (int i = 0; i < cycles; i++) {
            Assertions.assertEquals(0, rng.nextInt(), "Expected the generator to output zeros");
        }
    }

    /**
     * Assert that the random generator produces zero output for
     * {@link UniformRandomProvider#nextLong()} over the given number of cycles.
     * This is used to test a poorly seeded generator cannot generate random output.
     *
     * @param rng Random generator.
     * @param cycles Number of cycles.
     */
    public static void assertNextLongZeroOutput(UniformRandomProvider rng, int cycles) {
        for (int i = 0; i < cycles; i++) {
            Assertions.assertEquals(0, rng.nextLong(), "Expected the generator to output zeros");
        }
    }

    /**
     * Assert that following a set number of warm-up cycles the random generator produces
     * at least one non-zero output for {@link UniformRandomProvider#nextInt()} over the
     * given number of test cycles. This is used to test a poorly seeded generator can recover
     * internal state to generate "random" output.
     *
     * @param rng Random generator.
     * @param warmupCycles Number of warm-up cycles.
     * @param testCycles Number of test cycles.
     */
    public static void assertNextIntNonZeroOutput(UniformRandomProvider rng,
                                                  int warmupCycles, int testCycles) {
        for (int i = 0; i < warmupCycles; i++) {
            rng.nextInt();
        }
        for (int i = 0; i < testCycles; i++) {
            if (rng.nextInt() != 0) {
                return;
            }
        }
        Assertions.fail("No non-zero output after " + (warmupCycles + testCycles) + " cycles");
    }

    /**
     * Assert that following a set number of warm-up cycles the random generator produces
     * at least one non-zero output for {@link UniformRandomProvider#nextLong()} over the
     * given number of test cycles. This is used to test a poorly seeded generator can recover
     * internal state to generate "random" output.
     *
     * @param rng Random generator.
     * @param warmupCycles Number of warm-up cycles.
     * @param testCycles Number of test cycles.
     */
    public static void assertNextLongNonZeroOutput(UniformRandomProvider rng,
                                                   int warmupCycles, int testCycles) {
        for (int i = 0; i < warmupCycles; i++) {
            rng.nextLong();
        }
        for (int i = 0; i < testCycles; i++) {
            if (rng.nextLong() != 0L) {
                return;
            }
        }
        Assertions.fail("No non-zero output after " + (warmupCycles + testCycles) + " cycles");
    }

    /**
     * Assert that following a set number of warm-up cycles the random generator produces
     * at least one non-zero output for {@link UniformRandomProvider#nextLong()} over the
     * given number of test cycles.
     *
     * <p>Helper function to add the seed element and bit that was non zero to the fail message.
     *
     * @param rng Random generator.
     * @param warmupCycles Number of warm-up cycles.
     * @param testCycles Number of test cycles.
     * @param seedElement Seed element for the message.
     * @param bit Seed bit for the message.
     */
    private static void assertNextLongNonZeroOutputFromSingleBitSeed(
        UniformRandomProvider rng, int warmupCycles, int testCycles, int seedElement, int bit) {
        try {
            assertNextLongNonZeroOutput(rng, warmupCycles, testCycles);
        } catch (final AssertionError ex) {
            Assertions.fail("No non-zero output after " + (warmupCycles + testCycles) + " cycles. " +
                        "Seed element [" + seedElement + "], bit=" + bit);
        }
    }

    /**
     * Assert that the random generator created using an {@code int[]} seed with a
     * single bit set is functional. This is tested using the
     * {@link #assertNextLongNonZeroOutput(UniformRandomProvider, int, int)} using
     * two times the seed size as the warm-up and test cycles.
     *
     * @param type Class of the generator.
     * @param size Seed size.
     */
    public static <T extends UniformRandomProvider> void
        assertIntArrayConstructorWithSingleBitSeedIsFunctional(Class<T> type, int size) {
        assertIntArrayConstructorWithSingleBitInPoolIsFunctional(type, 32 * size);
    }

    /**
     * Assert that the random generator created using an {@code int[]} seed with a
     * single bit set is functional. This is tested using the
     * {@link #assertNextLongNonZeroOutput(UniformRandomProvider, int, int)} using
     * two times the seed size as the warm-up and test cycles.
     *
     * <p>The seed size is determined from the size of the bit pool. Bits are set for every position
     * in the pool from most significant first. If the pool size is not a multiple of 32 then the
     * remaining lower significant bits of the last seed position are not tested.</p>
     *
     * @param type Class of the generator.
     * @param k Number of bits in the pool (not necessarily a multiple of 32).
     */
    public static <T extends UniformRandomProvider> void
        assertIntArrayConstructorWithSingleBitInPoolIsFunctional(Class<T> type, int k) {
        try {
            // Find the int[] constructor
            final Constructor<T> constructor = type.getConstructor(int[].class);
            final int size = (k + 31) / 32;
            final int[] seed = new int[size];
            int remaining = k;
            for (int i = 0; i < seed.length; i++) {
                seed[i] = Integer.MIN_VALUE;
                for (int j = 0; j < 32; j++) {
                    final UniformRandomProvider rng = constructor.newInstance(seed);
                    RandomAssert.assertNextLongNonZeroOutputFromSingleBitSeed(rng, 2 * size, 2 * size, i, 31 - j);
                    // Eventually reset to zero
                    seed[i] >>>= 1;
                    // Terminate when the entire bit pool has been tested
                    if (--remaining == 0) {
                        return;
                    }
                }
                Assertions.assertEquals(0, seed[i], "Seed element was not reset");
            }
        } catch (IllegalAccessException | NoSuchMethodException | InstantiationException |
                InvocationTargetException ex) {
            Assertions.fail(ex.getMessage());
        }
    }

    /**
     * Assert that the random generator created using a {@code long[]} seed with a
     * single bit set is functional. This is tested using the
     * {@link #assertNextLongNonZeroOutput(UniformRandomProvider, int, int)} using two times the seed
     * size as the warm-up and test cycles.
     *
     * @param type Class of the generator.
     * @param size Seed size.
     */
    public static <T extends UniformRandomProvider> void
        assertLongArrayConstructorWithSingleBitSeedIsFunctional(Class<T> type, int size) {
        assertLongArrayConstructorWithSingleBitInPoolIsFunctional(type, 64 * size);
    }

    /**
     * Assert that the random generator created using an {@code long[]} seed with a
     * single bit set is functional. This is tested using the
     * {@link #assertNextLongNonZeroOutput(UniformRandomProvider, int, int)} using
     * two times the seed size as the warm-up and test cycles.
     *
     * <p>The seed size is determined from the size of the bit pool. Bits are set for every position
     * in the pool from most significant first. If the pool size is not a multiple of 64 then the
     * remaining lower significant bits of the last seed position are not tested.</p>
     *
     * @param type Class of the generator.
     * @param k Number of bits in the pool (not necessarily a multiple of 64).
     */
    public static <T extends UniformRandomProvider> void
        assertLongArrayConstructorWithSingleBitInPoolIsFunctional(Class<T> type, int k) {
        try {
            // Find the long[] constructor
            final Constructor<T> constructor = type.getConstructor(long[].class);
            final int size = (k + 63) / 64;
            final long[] seed = new long[size];
            int remaining = k;
            for (int i = 0; i < seed.length; i++) {
                seed[i] = Long.MIN_VALUE;
                for (int j = 0; j < 64; j++) {
                    final UniformRandomProvider rng = constructor.newInstance(seed);
                    RandomAssert.assertNextLongNonZeroOutputFromSingleBitSeed(rng, 2 * size, 2 * size, i, 63 - j);
                    // Eventually reset to zero
                    seed[i] >>>= 1;
                    // Terminate when the entire bit pool has been tested
                    if (--remaining == 0) {
                        return;
                    }
                }
                Assertions.assertEquals(0L, seed[i], "Seed element was not reset");
            }
        } catch (IllegalAccessException | NoSuchMethodException | InstantiationException |
                InvocationTargetException ex) {
            Assertions.fail(ex.getMessage());
        }
    }
}
