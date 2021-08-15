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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;

/**
 * Tests for {@link BaseProvider}.
 *
 * This class should only contain unit tests that cover code paths not
 * exercised elsewhere.  Those code paths are most probably only taken
 * in case of a wrong implementation (and would therefore fail other
 * tests too).
 */
public class BaseProviderTest {
    @Test
    public void testStateSizeTooSmall() {
        final DummyGenerator dummy = new DummyGenerator();
        final int size = dummy.getStateSize();
        Assumptions.assumeTrue(size > 0);
        final RandomProviderDefaultState state = new RandomProviderDefaultState(new byte[size - 1]);
        Assertions.assertThrows(IllegalStateException.class, () -> dummy.restoreState(state));
    }

    @Test
    public void testStateSizeTooLarge() {
        final DummyGenerator dummy = new DummyGenerator();
        final int size = dummy.getStateSize();
        final RandomProviderDefaultState state = new RandomProviderDefaultState(new byte[size + 1]);
        Assertions.assertThrows(IllegalStateException.class, () -> dummy.restoreState(state));
    }

    @Test
    public void testFillStateInt() {
        final int[] state = new int[10];
        final int[] seed = {1, 2, 3};

        for (int i = 0; i < state.length; i++) {
            Assertions.assertEquals(0, state[i]);
        }

        new DummyGenerator().fillState(state, seed);
        for (int i = 0; i < seed.length; i++) {
            Assertions.assertEquals(seed[i], state[i]);
        }
        for (int i = seed.length; i < state.length; i++) {
            Assertions.assertNotEquals(0, state[i]);
        }
    }

    @Test
    public void testFillStateLong() {
        final long[] state = new long[10];
        final long[] seed = {1, 2, 3};

        for (int i = 0; i < state.length; i++) {
            Assertions.assertEquals(0, state[i]);
        }

        new DummyGenerator().fillState(state, seed);
        for (int i = 0; i < seed.length; i++) {
            Assertions.assertEquals(seed[i], state[i]);
        }
        for (int i = seed.length; i < state.length; i++) {
            Assertions.assertNotEquals(0, state[i]);
        }
    }

    /**
     * Dummy class for checking the behaviorof the IntProvider. Tests:
     * <ul>
     *  <li>an incomplete implementation</li>
     *  <li>{@code fillState} methods with "protected" access</li>
     * </ul>
     */
    static class DummyGenerator extends org.apache.commons.rng.core.source32.IntProvider {
        /** {@inheritDoc} */
        @Override
        public int next() {
            return 4; // https://www.xkcd.com/221/
        }

        /**
         * Gets the state size. This captures the state size of the IntProvider.
         *
         * @return the state size
         */
        int getStateSize() {
            return getStateInternal().length;
        }

        // Missing overrides of "setStateInternal" and "getStateInternal".

        /** {@inheritDoc} */
        @Override
        public void fillState(int[] state, int[] seed) {
            super.fillState(state, seed);
        }

        /** {@inheritDoc} */
        @Override
        public void fillState(long[] state, long[] seed) {
            super.fillState(state, seed);
        }
    }
}
