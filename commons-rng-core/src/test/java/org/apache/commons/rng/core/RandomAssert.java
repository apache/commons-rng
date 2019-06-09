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

import org.junit.Assert;
import org.junit.Assume;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import org.apache.commons.rng.JumpableUniformRandomProvider;
import org.apache.commons.rng.RandomProviderState;
import org.apache.commons.rng.RestorableUniformRandomProvider;
import org.apache.commons.rng.UniformRandomProvider;

/**
 * Utility class for testing random generators.
 */
public class RandomAssert {
    /**
     * Assert that the random generator produces the expected output.
     *
     * @param expected Expected output.
     * @param rng Random generator.
     */
    public static void assertEquals(int[] expected, UniformRandomProvider rng) {
        assertEquals("Value at position ", expected, rng);
    }

    /**
     * Assert that the random generator produces the expected output.
     *
     * @param expected Expected output.
     * @param rng Random generator.
     */
    public static void assertEquals(long[] expected, UniformRandomProvider rng) {
        assertEquals("Value at position ", expected, rng);
    }

    /**
     * Assert that the random generator produces the expected output.
     * The message prefix is prepended to the array index for the assertion message.
     *
     * @param messagePrefix Message prefix.
     * @param expected Expected output.
     * @param rng Random generator.
     */
    private static void assertEquals(String messagePrefix, int[] expected, UniformRandomProvider rng) {
        for (int i = 0; i < expected.length; i++) {
            Assert.assertEquals(messagePrefix + i, expected[i], rng.nextInt());
        }
    }

    /**
     * Assert that the random generator produces the expected output.
     * The message prefix is prepended to the array index for the assertion message.
     *
     * @param messagePrefix Message prefix.
     * @param expected Expected output.
     * @param rng Random generator.
     */
    private static void assertEquals(String messagePrefix, long[] expected, UniformRandomProvider rng) {
        for (int i = 0; i < expected.length; i++) {
            Assert.assertEquals(messagePrefix + i, expected[i], rng.nextLong());
        }
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
        Assert.assertNotSame("The copy instance should be a different object", rng, copy);
        Assert.assertEquals("The copy instance should be the same class", rng.getClass(), copy.getClass());
        assertEquals("Pre-jump value at position ", expectedBefore, copy);
        assertEquals("Post-jump value at position ", expectedAfter, rng);
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
        Assert.assertNotSame("The copy instance should be a different object", rng, copy);
        Assert.assertEquals("The copy instance should be the same class", rng.getClass(), copy.getClass());
        assertEquals("Pre-jump value at position ", expectedBefore, copy);
        assertEquals("Post-jump value at position ", expectedAfter, rng);
    }

    /**
     * Assert that the random generator state of the copy instance returned from the
     * jump function matches the input state.
     *
     * <p>The generator must be a {@link RestorableUniformRandomProvider} and return
     * an instance of {@link RandomProviderDefaultState}.</p>
     *
     * <p>The input generator is sampled using methods in the
     * {@link UniformRandomProvider} interface, the state is saved and a jump is
     * performed. The states from the pre-jump generator and the returned copy
     * instance must match.</p>
     *
     * <p>This test targets any cached state of the default implementation of a generator
     * in {@link org.apache.commons.rng.core.source32.IntProvider IntProvider} and
     * {@link org.apache.commons.rng.core.source64.LongProvider LongProvider}
     * such as the state cached for the nextBoolean() and nextInt() functions.</p>
     *
     * @param rng Random generator.
     */
    public static void assertJumpUsingState(JumpableUniformRandomProvider rng) {
        Assume.assumeTrue("Not a restorable RNG", rng instanceof RestorableUniformRandomProvider);

        // Exercise the generator. 
        // This calls nextInt() an odd number of times so the default
        // implementation of LongProvider should have cached a state for nextInt().
        for (int i = 0; i < 3; i++) {
            rng.nextInt();
            rng.nextLong();
            rng.nextBoolean();
        }

        final RandomProviderState preJumpState = ((RestorableUniformRandomProvider) rng).saveState();
        Assume.assumeTrue("Not a recognised state", preJumpState instanceof RandomProviderDefaultState);

        final UniformRandomProvider copy = rng.jump();
        Assert.assertNotSame("The copy instance should be a different object", rng, copy);

        final RandomProviderState copyState = ((RestorableUniformRandomProvider)copy).saveState();
        final RandomProviderDefaultState expected = (RandomProviderDefaultState) preJumpState;
        final RandomProviderDefaultState actual = (RandomProviderDefaultState) copyState;
        Assert.assertArrayEquals(expected.getState(), actual.getState());
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
            Assert.assertEquals("Value at position " + i, rng1.nextInt(), rng2.nextInt());
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
            Assert.assertEquals("Value at position " + i, rng1.nextLong(), rng2.nextLong());
        }
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
            Assert.assertEquals("Expected the generator to output zeros", 0, rng.nextInt());
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
            Assert.assertEquals("Expected the generator to output zeros", 0, rng.nextLong());
        }
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
        Assert.fail("No non-zero output after " + (warmupCycles + testCycles) + " cycles");
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
        try {
            // Find the int[] constructor
            final Constructor<T> constructor = type.getConstructor(int[].class);
            final int[] seed = new int[size];
            for (int i = 0; i < size; i++) {
                seed[i] = 1;
                for (int j = 0; j < 32; j++) {
                    final UniformRandomProvider rng = constructor.newInstance(seed);
                    RandomAssert.assertNextLongNonZeroOutput(rng, 2 * size, 2 * size);
                    // Eventually rolls-over to reset to zero
                    seed[i] <<= 1;
                }
                Assert.assertEquals("Seed element was not reset", 0, seed[i]);
            }
        } catch (IllegalAccessException ex) {
            Assert.fail(ex.getMessage());
        } catch (NoSuchMethodException ex) {
            Assert.fail(ex.getMessage());
        } catch (InstantiationException ex) {
            Assert.fail(ex.getMessage());
        } catch (InvocationTargetException ex) {
            Assert.fail(ex.getMessage());
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
        try {
            // Find the long[] constructor
            final Constructor<T> constructor = type.getConstructor(long[].class);
            final long[] seed = new long[size];
            for (int i = 0; i < size; i++) {
                seed[i] = 1;
                for (int j = 0; j < 64; j++) {
                    final UniformRandomProvider rng = constructor.newInstance(seed);
                    RandomAssert.assertNextLongNonZeroOutput(rng, 2 * size, 2 * size);
                    // Eventually rolls-over to reset to zero
                    seed[i] <<= 1;
                }
                Assert.assertEquals("Seed element was not reset", 0L, seed[i]);
            }
        } catch (IllegalAccessException ex) {
            Assert.fail(ex.getMessage());
        } catch (NoSuchMethodException ex) {
            Assert.fail(ex.getMessage());
        } catch (InstantiationException ex) {
            Assert.fail(ex.getMessage());
        } catch (InvocationTargetException ex) {
            Assert.fail(ex.getMessage());
        }
    }
}
