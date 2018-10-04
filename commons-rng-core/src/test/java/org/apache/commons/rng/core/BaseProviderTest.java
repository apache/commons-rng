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

import org.junit.Test;
import org.junit.Assert;

/**
 * Tests for {@link BaseProvider}.
 *
 * This class should only contain unit tests that cover code paths not
 * exercised elsewhere.  Those code paths are most probably only taken
 * in case of a wrong implementation (and would therefore fail other
 * tests too).
 */
public class BaseProviderTest {
    @Test(expected=IllegalStateException.class)
    public void testWrongStateSize() {
        new DummyGenerator().restoreState(new RandomProviderDefaultState(new byte[1]));
    }

    @Test
    public void testFillStateInt() {
        final int[] state = new int[10];
        final int[] seed = {1, 2, 3};

        for (int i = 0; i < state.length; i++) {
            Assert.assertEquals(0, state[i]);
        }

        new DummyGenerator().fillState(state, seed);
        for (int i = 0; i < seed.length; i++) {
            Assert.assertEquals(seed[i], state[i]);
        }
        for (int i = seed.length; i < state.length; i++) {
            Assert.assertNotEquals(0, state[i]);
        }
    }

    @Test
    public void testFillStateLong() {
        final long[] state = new long[10];
        final long[] seed = {1, 2, 3};

        for (int i = 0; i < state.length; i++) {
            Assert.assertEquals(0, state[i]);
        }

        new DummyGenerator().fillState(state, seed);
        for (int i = 0; i < seed.length; i++) {
            Assert.assertEquals(seed[i], state[i]);
        }
        for (int i = seed.length; i < state.length; i++) {
            Assert.assertNotEquals(0, state[i]);
        }
    }

    /**
     * Dummy class for checking the behaviour of
     * <ul>
     *  <li>an incomplete implementation</li>
     *  <li>{@code fillState} methods with "protected" access</li>
     * </ul>
     */
    class DummyGenerator extends org.apache.commons.rng.core.source32.IntProvider {
        /** {@inheritDoc} */
        @Override
        public int next() {
            return 4; // https://www.xkcd.com/221/
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
