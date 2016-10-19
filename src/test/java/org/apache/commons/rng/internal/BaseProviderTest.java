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
package org.apache.commons.rng.internal;

import org.junit.Test;

import org.apache.commons.rng.RestorableUniformRandomProvider;
import org.apache.commons.rng.RandomSource;

/**
 * Tests for {@link BaseProvider}.
 *
 * This class should only contain unit tests that cover code paths not
 * exercised elsewhere.  Those code paths are most probably only taken
 * in case of a wrong implementation (and would therefore fail other
 * tests too).
 */
public class BaseProviderTest {
    @Test(expected=UnsupportedOperationException.class)
    public void testMissingGetStateInternal() {
        final RestorableUniformRandomProvider rng = new DummyGenerator();
        rng.saveState();
    }

    @Test(expected=UnsupportedOperationException.class)
    public void testMissingSetStateInternal() {
        final RestorableUniformRandomProvider rng = new DummyGenerator();
        rng.restoreState(new RandomSource.State(new byte[1]));
    }
}

/**
 * Dummy class for checking the behaviour of an incomplete implementation.
 */
class DummyGenerator extends org.apache.commons.rng.internal.source32.IntProvider {
    /** {@inheritDoc} */
    @Override
    public int next() {
        return 4; // https://www.xkcd.com/221/
    }

    // Missing overrides of "setStateInternal" and "getStateInternal".
}
