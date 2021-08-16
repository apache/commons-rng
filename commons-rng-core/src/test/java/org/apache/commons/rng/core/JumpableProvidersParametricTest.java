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
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Arrays;

import org.apache.commons.rng.JumpableUniformRandomProvider;
import org.apache.commons.rng.LongJumpableUniformRandomProvider;
import org.apache.commons.rng.RandomProviderState;
import org.apache.commons.rng.RestorableUniformRandomProvider;
import org.apache.commons.rng.UniformRandomProvider;
import org.apache.commons.rng.core.source32.IntProvider;
import org.apache.commons.rng.core.source64.LongProvider;

/**
 * Tests which all {@link JumpableUniformRandomProvider} generators must pass.
 */
class JumpableProvidersParametricTest {
    /** The size of the state for the IntProvider. */
    private static final int INT_PROVIDER_STATE_SIZE;
    /** The size of the state for the LongProvider. */
    private static final int LONG_PROVIDER_STATE_SIZE;

    static {
        INT_PROVIDER_STATE_SIZE = new State32Generator().getStateSize();
        LONG_PROVIDER_STATE_SIZE = new State64Generator().getStateSize();
    }

    /**
     * Gets the list of Jumpable generators.
     *
     * @return the list
     */
    private static Iterable<JumpableUniformRandomProvider> getJumpableProviders() {
        return ProvidersList.listJumpable();
    }

    /**
     * Gets the function using the {@link LongJumpableUniformRandomProvider#longJump()} method.
     * If the RNG is not long jumpable then this will raise an exception to skip the test.
     *
     * @param generator RNG under test.
     * @return the jump function
     */
    private static TestJumpFunction getLongJumpFunction(JumpableUniformRandomProvider generator) {
        Assumptions.assumeTrue(generator instanceof LongJumpableUniformRandomProvider, "No long jump function");
        final LongJumpableUniformRandomProvider rng2 = (LongJumpableUniformRandomProvider) generator;
        return rng2::jump;
    }

    /**
     * Test that the random generator returned from the jump is a new instance of the same class.
     */
    @ParameterizedTest
    @MethodSource("getJumpableProviders")
    void testJumpReturnsACopy(JumpableUniformRandomProvider generator) {
        assertJumpReturnsACopy(generator::jump, generator);
    }

    /**
     * Test that the random generator returned from the long jump is a new instance of the same class.
     */
    @ParameterizedTest
    @MethodSource("getJumpableProviders")
    void testLongJumpReturnsACopy(JumpableUniformRandomProvider generator) {
        assertJumpReturnsACopy(getLongJumpFunction(generator), generator);
    }

    /**
     * Assert that the random generator returned from the jump function is a new instance of the same class.
     *
     * @param jumpFunction Jump function to test.
     * @param generator RNG under test.
     */
    private static void assertJumpReturnsACopy(TestJumpFunction jumpFunction,
                                               JumpableUniformRandomProvider generator) {
        final UniformRandomProvider copy = jumpFunction.jump();
        Assertions.assertNotSame(generator, copy, "The copy instance should be a different object");
        Assertions.assertEquals(generator.getClass(), copy.getClass(), "The copy instance should be the same class");
    }

    /**
     * Test that the random generator state of the copy instance returned from the jump
     * matches the input state.
     */
    @ParameterizedTest
    @MethodSource("getJumpableProviders")
    void testJumpCopyMatchesPreJumpState(JumpableUniformRandomProvider generator) {
        assertCopyMatchesPreJumpState(generator::jump, generator);
    }

    /**
     * Test that the random generator state of the copy instance returned from the long jump
     * matches the input state.
     */
    @ParameterizedTest
    @MethodSource("getJumpableProviders")
    void testLongJumpCopyMatchesPreJumpState(JumpableUniformRandomProvider generator) {
        assertCopyMatchesPreJumpState(getLongJumpFunction(generator), generator);
    }

    /**
     * Assert that the random generator state of the copy instance returned from the jump
     * function matches the input state.
     *
     * <p>The generator must be a {@link RestorableUniformRandomProvider} and return an
     * instance of {@link RandomProviderDefaultState}.</p>
     *
     * <p>The input generator is sampled using methods in the
     * {@link UniformRandomProvider} interface, the state is saved and a jump is
     * performed. The states from the pre-jump generator and the returned copy instance
     * must match.</p>
     *
     * <p>This test targets any cached state of the default implementation of a generator
     * in {@link IntProvider} and {@link LongProvider} such as the state cached for the
     * nextBoolean() and nextInt() functions.</p>
     *
     * @param jumpFunction Jump function to test.
     * @param generator RNG under test.
     */
    private static void assertCopyMatchesPreJumpState(TestJumpFunction jumpFunction,
                                                      JumpableUniformRandomProvider generator) {
        Assumptions.assumeTrue(generator instanceof RestorableUniformRandomProvider, "Not a restorable RNG");

        for (int repeats = 0; repeats < 2; repeats++) {
            // Exercise the generator.
            // This calls nextInt() once so the default implementation of LongProvider
            // should have cached a state for nextInt() in one of the two repeats.
            // Calls nextBoolean() to ensure a cached state in one of the two repeats.
            generator.nextInt();
            generator.nextBoolean();

            final RandomProviderState preJumpState = ((RestorableUniformRandomProvider) generator).saveState();
            Assumptions.assumeTrue(preJumpState instanceof RandomProviderDefaultState, "Not a recognised state");

            final UniformRandomProvider copy = jumpFunction.jump();

            final RandomProviderState copyState = ((RestorableUniformRandomProvider) copy).saveState();
            final RandomProviderDefaultState expected = (RandomProviderDefaultState) preJumpState;
            final RandomProviderDefaultState actual = (RandomProviderDefaultState) copyState;
            Assertions.assertArrayEquals(expected.getState(), actual.getState(),
                "The copy instance state should match the state of the original");
        }
    }

    /**
     * Test that a jump resets the state of the default implementation of a generator in
     * {@link IntProvider} and {@link LongProvider}.
     */
    @ParameterizedTest
    @MethodSource("getJumpableProviders")
    void testJumpResetsDefaultState(JumpableUniformRandomProvider generator) {
        assertJumpResetsDefaultState(generator::jump, generator);
    }

    /**
     * Test that a long jump resets the state of the default implementation of a generator in
     * {@link IntProvider} and {@link LongProvider}.
     */
    @ParameterizedTest
    @MethodSource("getJumpableProviders")
    void testLongJumpResetsDefaultState(JumpableUniformRandomProvider generator) {
        assertJumpResetsDefaultState(getLongJumpFunction(generator), generator);
    }

    /**
     * Assert the jump resets the specified number of bytes of the state. The bytes are
     * checked from the end of the saved state.
     *
     * <p>This is intended to check the default state of the base implementation of
     * {@link IntProvider} and {@link LongProvider} is reset.</p>
     *
     * @param jumpFunction Jump function to test.
     * @param generator RNG under test.
     */
    private static void assertJumpResetsDefaultState(TestJumpFunction jumpFunction,
                                                     JumpableUniformRandomProvider generator) {
        int stateSize;
        if (generator instanceof IntProvider) {
            stateSize = INT_PROVIDER_STATE_SIZE;
        } else if (generator instanceof LongProvider) {
            stateSize = LONG_PROVIDER_STATE_SIZE;
        } else {
            throw new AssertionError("Unsupported RNG");
        }
        final byte[] expected = new byte[stateSize];
        for (int repeats = 0; repeats < 2; repeats++) {
            // Exercise the generator.
            // This calls nextInt() once so the default implementation of LongProvider
            // should have cached a state for nextInt() in one of the two repeats.
            // Calls nextBoolean() to ensure a cached state in one of the two repeats.
            generator.nextInt();
            generator.nextBoolean();

            jumpFunction.jump();

            // An Int/LongProvider so must be a RestorableUniformRandomProvider
            final RandomProviderState postJumpState = ((RestorableUniformRandomProvider) generator).saveState();
            final byte[] actual = ((RandomProviderDefaultState) postJumpState).getState();

            Assumptions.assumeTrue(actual.length >= stateSize, "Implementation has removed default state");

            // The implementation requires that any sub-class state is prepended to the
            // state thus the default state is at the end.
            final byte[] defaultState = Arrays.copyOfRange(actual, actual.length - stateSize, actual.length);
            Assertions.assertArrayEquals(expected, defaultState,
                "The jump should reset the default state to zero");
        }
    }

    /**
     * Dummy class for checking the state size of the IntProvider.
     */
    static class State32Generator extends IntProvider {
        /** {@inheritDoc} */
        @Override
        public int next() {
            return 0;
        }

        /**
         * Gets the state size. This captures the state size of the IntProvider.
         *
         * @return the state size
         */
        int getStateSize() {
            return getStateInternal().length;
        }
    }

    /**
     * Dummy class for checking the state size of the LongProvider.
     */
    static class State64Generator extends LongProvider {
        /** {@inheritDoc} */
        @Override
        public long next() {
            return 0;
        }

        /**
         * Gets the state size. This captures the state size of the LongProvider.
         *
         * @return the state size
         */
        int getStateSize() {
            return getStateInternal().length;
        }
    }

    /**
     * Specify the jump operation to test.
     *
     * <p>This allows testing {@link JumpableUniformRandomProvider} or
     * {@link LongJumpableUniformRandomProvider}.</p>
     */
    interface TestJumpFunction {
        /**
         * Perform the jump and return a pre-jump copy.
         *
         * @return the pre-jump copy.
         */
        UniformRandomProvider jump();
    }
}
