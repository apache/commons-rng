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
package org.apache.commons.rng.integration;

import org.apache.commons.rng.RandomProviderState;
import org.apache.commons.rng.RestorableUniformRandomProvider;
import org.apache.commons.rng.UniformRandomProvider;
import org.apache.commons.rng.simple.RandomSource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@code o.a.c.rng.simple}.
 * Uses classes from all packages.
 *
 * <p>Note: Class is public for the OSGi bundle testing.
 */
public class SimpleTest {
    @Test
    void testCreateSource32() {
        // Tests: core.source32
        final UniformRandomProvider rng = RandomSource.KISS.create();
        final int origin = 3;
        final int bound = 7;
        // Tests: o.a.c.rng (client API)
        final int s = rng.nextInt(origin, bound);
        Assertions.assertTrue(origin <= s && s < bound);
    }

    @Test
    void testCreateSource64() {
        // Tests: core.source64
        final RestorableUniformRandomProvider rng = RandomSource.SPLIT_MIX_64.create();
        // Tests: core; core.util
        RandomProviderState state = rng.saveState();
        final double[] s1 = rng.doubles(10).toArray();
        rng.restoreState(state);
        final double[] s2 = rng.doubles(10).toArray();
        Assertions.assertArrayEquals(s1, s2);
    }
}
