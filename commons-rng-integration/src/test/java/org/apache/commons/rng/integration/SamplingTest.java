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

import java.util.BitSet;
import org.apache.commons.rng.UniformRandomProvider;
import org.apache.commons.rng.sampling.PermutationSampler;
import org.apache.commons.rng.sampling.distribution.DiscreteUniformSampler;
import org.apache.commons.rng.sampling.shape.BoxSampler;
import org.apache.commons.rng.simple.RandomSource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@code o.a.c.rng.sampling}.
 * Uses classes from all packages.
 *
 * <p>Note: Class is public for the OSGi bundle testing.
 */
public class SamplingTest {
    @Test
    void testSampling() {
        final UniformRandomProvider rng = RandomSource.KISS.create();
        final int n = 10;
        final int k = 3;
        final int[] s = new PermutationSampler(rng, n, k).sample();

        Assertions.assertEquals(k, s.length);
        BitSet set = new BitSet(n);
        for (final int x : s) {
            Assertions.assertTrue(0 <= x && x < n);
            set.set(x);
        }
        Assertions.assertEquals(k, set.cardinality());
    }

    @Test
    void testSamplingDistribution() {
        final UniformRandomProvider rng = RandomSource.KISS.create();
        final int lower = 3;
        final int upper = 5;
        final int s = DiscreteUniformSampler.of(rng, lower, upper).sample();
        Assertions.assertTrue(lower <= s && s <= upper);
    }

    @Test
    void testSamplingShape() {
        final UniformRandomProvider rng = RandomSource.KISS.create();
        final double[] a = {3, 4, 5};
        final double[] b = {6, 7, 8};
        final double[] s = BoxSampler.of(rng, a, b).sample();

        Assertions.assertEquals(a.length, s.length);
        for (int i = 0; i < a.length; i++) {
            Assertions.assertTrue(a[i] <= s[i] && s[i] <= b[i]);
        }
    }
}
