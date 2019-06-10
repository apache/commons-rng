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
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

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
@RunWith(value = Parameterized.class)
public class JumpableProvidersParametricTest {
    /** The size of the state for the IntProvider. */
    private static final int intProviderStateSize;
    /** The size of the state for the LongProvider. */
    private static final int longProviderStateSize;

    static {
        intProviderStateSize = new State32Generator().getStateSize();
        longProviderStateSize = new State64Generator().getStateSize();
    }

    /** RNG under test. */
    private final JumpableUniformRandomProvider generator;

    /**
     * Initializes generator instance.
     *
     * @param rng RNG to be tested.
     */
    public JumpableProvidersParametricTest(JumpableUniformRandomProvider rng) {
        generator = rng;
    }

    /**
     * Gets the list of Jumpable generators.
     *
     * @return the list
     */
    @Parameters(name = "{index}: data={0}")
    public static Iterable<JumpableUniformRandomProvider[]> getList() {
        return ProvidersList.listJumpable();
    }

    /**
     * Gets the function using the {@link JumpableUniformRandomProvider#jump()} method.
     *
     * @return the jump function
     */
    private TestJumpFunction getJumpFunction() {
        return new TestJumpFunction() {
            @Override
            public UniformRandomProvider jump() {
                return generator.jump();
            }
        };
    }

    /**
     * Gets the function using the {@link LongJumpableUniformRandomProvider#longJump()} method.
     *
     * @return the jump function
     */
    private TestJumpFunction getLongJumpFunction() {
        Assume.assumeTrue("No long jump function", generator instanceof LongJumpableUniformRandomProvider);

        final LongJumpableUniformRandomProvider rng = (LongJumpableUniformRandomProvider) generator;
        return new TestJumpFunction() {
            @Override
            public UniformRandomProvider jump() {
                return rng.longJump();
            }
        };
    }

    /**
     * Test that the random generator returned from the jump is a new instance of the same class.
     */
    @Test
    public void testJumpReturnsACopy() {
        assertJumpReturnsACopy(getJumpFunction());
    }

    /**
     * Test that the random generator returned from the long jump is a new instance of the same class.
     */
    @Test
    public void testLongJumpReturnsACopy() {
        assertJumpReturnsACopy(getLongJumpFunction());
    }

    /**
     * Assert that the random generator returned from the jump function is a new instance of the same class.
     *
     * @param jumpFunction Jump function to test.
     */
    private void assertJumpReturnsACopy(TestJumpFunction jumpFunction) {
        final UniformRandomProvider copy = jumpFunction.jump();
        Assert.assertNotSame("The copy instance should be a different object", generator, copy);
        Assert.assertEquals("The copy instance should be the same class", generator.getClass(), copy.getClass());
    }

    /**
     * Test that the random generator state of the copy instance returned from the jump
     * matches the input state.
     */
    @Test
    public void testJumpCopyMatchesPreJumpState() {
        assertCopyMatchesPreJumpState(getJumpFunction());
    }

    /**
     * Test that the random generator state of the copy instance returned from the long jump
     * matches the input state.
     */
    @Test
    public void testLongJumpCopyMatchesPreJumpState() {
        assertCopyMatchesPreJumpState(getLongJumpFunction());
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
     */
    private void assertCopyMatchesPreJumpState(TestJumpFunction jumpFunction) {
        Assume.assumeTrue("Not a restorable RNG", generator instanceof RestorableUniformRandomProvider);

        for (int repeats = 0; repeats < 2; repeats++) {
            // Exercise the generator.
            // This calls nextInt() once so the default implementation of LongProvider
            // should have cached a state for nextInt() in one of the two repeats.
            // Calls nextBoolean() to ensure a cached state in one of the two repeats.
            generator.nextInt();
            generator.nextBoolean();

            final RandomProviderState preJumpState = ((RestorableUniformRandomProvider) generator).saveState();
            Assume.assumeTrue("Not a recognised state", preJumpState instanceof RandomProviderDefaultState);

            final UniformRandomProvider copy = jumpFunction.jump();

            final RandomProviderState copyState = ((RestorableUniformRandomProvider) copy).saveState();
            final RandomProviderDefaultState expected = (RandomProviderDefaultState) preJumpState;
            final RandomProviderDefaultState actual = (RandomProviderDefaultState) copyState;
            Assert.assertArrayEquals("The copy instance state should match the state of the original",
                expected.getState(), actual.getState());
        }
    }

    /**
     * Test that a jump resets the state of the default implementation of a generator in
     * {@link IntProvider} and {@link LongProvider}.
     */
    @Test
    public void testJumpResetsDefaultState() {
        if (generator instanceof IntProvider) {
            assertJumpResetsDefaultState(getJumpFunction(), intProviderStateSize);
        } else if (generator instanceof LongProvider) {
            assertJumpResetsDefaultState(getJumpFunction(), longProviderStateSize);
        }
    }

    /**
     * Test that a long jump resets the state of the default implementation of a generator in
     * {@link IntProvider} and {@link LongProvider}.
     */
    @Test
    public void testLongJumpResetsDefaultState() {
        if (generator instanceof IntProvider) {
            assertJumpResetsDefaultState(getLongJumpFunction(), intProviderStateSize);
        } else if (generator instanceof LongProvider) {
            assertJumpResetsDefaultState(getLongJumpFunction(), longProviderStateSize);
        }
    }

    /**
     * Assert the jump resets the specified number of bytes of the state. The bytes are
     * checked from the end of the saved state.
     *
     * <p>This is intended to check the default state of the base implementation of
     * {@link IntProvider} and {@link LongProvider} is reset.</p>
     *
     * @param jumpFunction Jump function to test.
     * @param stateSize State size.
     */
    private void assertJumpResetsDefaultState(TestJumpFunction jumpFunction, int stateSize) {
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

            Assume.assumeTrue("Implementation has removed default state", actual.length >= stateSize);

            // The implementation requires that any sub-class state is prepended to the
            // state thus the default state is at the end.
            final byte[] defaultState = Arrays.copyOfRange(actual, actual.length - stateSize, actual.length);
            Assert.assertArrayEquals("The jump should reset the default state to zero", expected, defaultState);
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
