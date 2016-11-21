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
package org.apache.commons.rng.sampling;

import java.util.Set;
import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;

import org.junit.Assert;
import org.junit.Test;

import org.apache.commons.math3.stat.inference.ChiSquareTest;
import org.apache.commons.math3.util.MathArrays;

import org.apache.commons.rng.UniformRandomProvider;
import org.apache.commons.rng.simple.RandomSource;

/**
 * Tests for {@link PermutationSampler}.
 */
public class PermutationSamplerTest {
    private final UniformRandomProvider rng = RandomSource.create(RandomSource.ISAAC, 1232343456L);
    private final ChiSquareTest chiSquareTest = new ChiSquareTest();

    @Test
    public void testSampleTrivial() {
        final int n = 6;
        final int k = 3;
        final PermutationSampler sampler = new PermutationSampler(RandomSource.create(RandomSource.KISS),
                                                                  6, 3);
        final int[] random = sampler.sample();
        SAMPLE: for (int s : random) {
            for (int i = 0; i < n; i++) {
                if (i == s) {
                    continue SAMPLE;
                }
            }
            Assert.fail("number " + s + " not in array");
        }
    }

    @Test
    public void testSampleChiSquareTest() {
        final int[][] p = { { 0, 1, 2 }, { 0, 2, 1 },
                            { 1, 0, 2 }, { 1, 2, 0 },
                            { 2, 0, 1 }, { 2, 1, 0 } };
        final int len = p.length; 
        final long[] observed = new long[len];
        final int numSamples = 6000;
        final double numExpected = numSamples / (double) len;
        final double[] expected = new double[len];
        Arrays.fill(expected, numExpected);

        final PermutationSampler sampler = new PermutationSampler(rng, 3, 3);
        for (int i = 0; i < numSamples; i++) {
            observed[findPerm(p, sampler.sample())]++;
        }

        // Pass if we cannot reject null hypothesis that distributions are the same.
        Assert.assertFalse(chiSquareTest.chiSquareTest(expected, observed, 0.001));
    }
    
    @Test
    public void testSampleBoundaryCase() {
        // Check size = 1 boundary case.
        final PermutationSampler sampler = new PermutationSampler(rng, 1, 1);
        final int[] perm = sampler.sample();
        Assert.assertEquals(1, perm.length);
        Assert.assertEquals(0, perm[0]);
    }

    @Test(expected=IllegalArgumentException.class)
    public void testSamplePrecondition1() {
        // Must fail for k > n.
        new PermutationSampler(rng, 2, 3);
    }

    @Test(expected=IllegalArgumentException.class)
    public void testSamplePrecondition2() {
        // Must fail for n = 0.
        new PermutationSampler(rng, 0, 0);
    }

    @Test(expected=IllegalArgumentException.class)
    public void testSamplePrecondition3() {
        // Must fail for k < n < 0.
        new PermutationSampler(rng, -1, 0);
    }

    @Test(expected=IllegalArgumentException.class)
    public void testSamplePrecondition4() {
        // Must fail for k < n < 0.
        new PermutationSampler(rng, 1, -1);
    }

    @Test
    public void testNatural() {
        final int n = 4;
        final int[] expected = {0, 1, 2, 3};

        final int[] natural = PermutationSampler.natural(n);
        for (int i = 0; i < n; i++) {
            Assert.assertEquals(expected[i], natural[i]);
        }
    }

    @Test
    public void testNaturalZero() {
        final int[] natural = PermutationSampler.natural(0);
        Assert.assertEquals(0, natural.length);
    }

    @Test
    public void testShuffleNoDuplicates() {
        final int n = 100;
        final int[] orig = PermutationSampler.natural(n);
        PermutationSampler.shuffle(rng, orig);

        // Test that all (unique) entries exist in the shuffled array.
        final int[] count = new int[n];
        for (int i = 0; i < n; i++) {
            count[orig[i]] += 1;
        }

        for (int i = 0; i < n; i++) {
            Assert.assertEquals(1, count[i]);
        }
    }

    @Test
    public void testShuffleTail() {
        final int[] orig = new int[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 };
        final int[] list = orig.clone();
        final int start = 4;
        PermutationSampler.shuffle(rng, list, start, false);

        // Ensure that all entries below index "start" did not move.
        for (int i = 0; i < start; i++) {
            Assert.assertEquals(orig[i], list[i]);
        }

        // Ensure that at least one entry has moved.
        boolean ok = false;
        for (int i = start; i < orig.length - 1; i++) {
            if (orig[i] != list[i]) {
                ok = true;
                break;
            }
        }
        Assert.assertTrue(ok);
    }

    @Test
    public void testShuffleHead() {
        final int[] orig = new int[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 };
        final int[] list = orig.clone();
        final int start = 4;
        PermutationSampler.shuffle(rng, list, start, true);

        // Ensure that all entries above index "start" did not move.
        for (int i = start + 1; i < orig.length; i++) {
            Assert.assertEquals(orig[i], list[i]);
        }

        // Ensure that at least one entry has moved.
        boolean ok = false;
        for (int i = 0; i <= start; i++) {
            if (orig[i] != list[i]) {
                ok = true;
                break;
            }
        }
        Assert.assertTrue(ok);
    }

    //// Support methods.

    private int findPerm(int[][] p,
                         int[] samp) {
        for (int i = 0; i < p.length; i++) {
            boolean good = true;
            for (int j = 0; j < samp.length; j++) {
                if (samp[j] != p[i][j]) {
                    good = false;
                }
            }
            if (good) {
                return i;
            }
        }

        Assert.fail("Permutation not found");
        return -1;
    }
}
