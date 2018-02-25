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
    public void testShuffle() {
        final List<Integer> orig = new ArrayList<Integer>();
        for (int i = 0; i < 10; i++) {
            orig.add((i + 1) * rng.nextInt());
        }
        final List<Integer> list = new ArrayList<Integer>(orig);

        ListSampler.shuffle(rng, list);
        // Ensure that at least one entry has moved.
        Assert.assertTrue(compare(orig, list, 0, orig.size(), false));
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
        Assert.assertTrue(compare(orig, list, 0, start, true));

        // Ensure that at least one entry has moved.
        Assert.assertTrue(compare(orig, list, start, orig.size(), false));
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
        Assert.assertTrue(compare(orig, list, start + 1, orig.size(), true));

        // Ensure that at least one entry has moved.
        Assert.assertTrue(compare(orig, list, 0, start + 1, false));
    }

    //// Support methods.

    /**
     * If {@code same == true}, return {@code true} if all entries are
     * the same; if {@code same == false}, return {@code true} if at
     * least one entry is different.
     */
    private <T> boolean compare(List<T> orig,
                                List<T> list,
                                int start,
                                int end,
                                boolean same) {
        for (int i = start; i < end; i++) {
            if (!orig.get(i).equals(list.get(i))) {
                return same ? false : true;
            }
        }
        return same ? true : false;
    }

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
