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
import java.util.Collection;
import java.util.Arrays;

import org.junit.Assert;
import org.junit.Test;

import org.apache.commons.math3.stat.inference.ChiSquareTest;

import org.apache.commons.rng.UniformRandomProvider;
import org.apache.commons.rng.simple.RandomSource;

/**
 * Tests for {@link ListSampler}.
 */
public class ListSamplerTest {
    private final UniformRandomProvider rng = RandomSource.create(RandomSource.ISAAC, 6543432321L);
    private final ChiSquareTest chiSquareTest = new ChiSquareTest();

    @Test
    public void testSample() {
        final String[][] c = { { "0", "1" }, { "0", "2" }, { "0", "3" }, { "0", "4" },
                               { "1", "2" }, { "1", "3" }, { "1", "4" },
                               { "2", "3" }, { "2", "4" },
                               { "3", "4" } };
        final long[] observed = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };
        final double[] expected = { 100, 100, 100, 100, 100, 100, 100, 100, 100, 100 };

        final HashSet<String> cPop = new HashSet<String>(); // {0, 1, 2, 3, 4}.
        for (int i = 0; i < 5; i++) {
            cPop.add(Integer.toString(i));
        }

        final List<Set<String>> sets = new ArrayList<Set<String>>(); // 2-sets from 5.
        for (int i = 0; i < 10; i++) {
            final HashSet<String> hs = new HashSet<String>();
            hs.add(c[i][0]);
            hs.add(c[i][1]);
            sets.add(hs);
        }

        for (int i = 0; i < 1000; i++) {
            observed[findSample(sets, ListSampler.sample(rng, new ArrayList<String>(cPop), 2))]++;
        }

        // Pass if we cannot reject null hypothesis that distributions are the same.
        Assert.assertFalse(chiSquareTest.chiSquareTest(expected, observed, 0.001));
    }

    @Test
    public void testSampleWhole() {
        // Sample of size = size of collection must return the same collection.
        final List<String> list = new ArrayList<String>();
        list.add("one");

        final List<String> one = ListSampler.sample(rng, list, 1);
        Assert.assertEquals(1, one.size());
        Assert.assertTrue(one.contains("one"));
    }

    @Test(expected=IllegalArgumentException.class)
    public void testSamplePrecondition1() {
        // Must fail for sample size > collection size.
        final List<String> list = new ArrayList<String>();
        list.add("one");
        ListSampler.sample(rng, list, 2);
    }

    @Test(expected=IllegalArgumentException.class)
    public void testSamplePrecondition2() {
        // Must fail for empty collection.
        final List<String> list = new ArrayList<String>();
        ListSampler.sample(rng, list, 1);
    }

    @Test
    public void testShuffleTail() {
        final List<Integer> orig = new ArrayList<Integer>();
        for (int i = 0; i < 10; i++) {
            orig.add((i + 1) * rng.nextInt());
        }
        final List<Integer> list = new ArrayList<Integer>(orig);

        final int start = 4;
        ListSampler.shuffle(rng, list, start, false);

        // Ensure that all entries below index "start" did not move.
        for (int i = 0; i < start; i++) {
            Assert.assertEquals(orig.get(i), list.get(i));
        }

        // Ensure that at least one entry has moved.
        boolean ok = false;
        for (int i = start; i < orig.size() - 1; i++) {
            if (!orig.get(i).equals(list.get(i))) {
                ok = true;
                break;
            }
        }
        Assert.assertTrue(ok);
    }

    @Test
    public void testShuffleHead() {
        final List<Integer> orig = new ArrayList<Integer>();
        for (int i = 0; i < 10; i++) {
            orig.add((i + 1) * rng.nextInt());
        }
        final List<Integer> list = new ArrayList<Integer>(orig);

        final int start = 4;
        ListSampler.shuffle(rng, list, start, true);

        // Ensure that all entries above index "start" did not move.
        for (int i = start + 1; i < orig.size(); i++) {
            Assert.assertEquals(orig.get(i), list.get(i));
        }

        // Ensure that at least one entry has moved.
        boolean ok = false;
        for (int i = 0; i <= start; i++) {
            if (!orig.get(i).equals(list.get(i))) {
                ok = true;
                break;
            }
        }
        Assert.assertTrue(ok);
    }

    //// Support methods.

    private <T extends Set<String>> int findSample(List<T> u,
                                                   Collection<String> sampList) {
        final String[] samp = sampList.toArray(new String[sampList.size()]);
        for (int i = 0; i < u.size(); i++) {
            final T set = u.get(i);
            final HashSet<String> sampSet = new HashSet<String>();
            for (int j = 0; j < samp.length; j++) {
                sampSet.add(samp[j]);
            }
            if (set.equals(sampSet)) {
                return i;
            }
        }

        Assert.fail("Sample not found: { " +
                    samp[0] + ", " + samp[1] + " }");
        return -1;
    }
}
