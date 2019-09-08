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
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.ArrayList;
import java.util.Collection;

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
        final String[][] c = {{"0", "1"}, {"0", "2"}, {"0", "3"}, {"0", "4"},
                              {"1", "2"}, {"1", "3"}, {"1", "4"},
                              {"2", "3"}, {"2", "4"},
                              {"3", "4"}};
        final long[] observed = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
        final double[] expected = {100, 100, 100, 100, 100, 100, 100, 100, 100, 100};

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

    @Test(expected = IllegalArgumentException.class)
    public void testSamplePrecondition1() {
        // Must fail for sample size > collection size.
        final List<String> list = new ArrayList<String>();
        list.add("one");
        ListSampler.sample(rng, list, 2);
    }

    @Test(expected = IllegalArgumentException.class)
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

        final List<Integer> arrayList = new ArrayList<Integer>(orig);

        ListSampler.shuffle(rng, arrayList);
        // Ensure that at least one entry has moved.
        Assert.assertTrue("ArrayList", compare(orig, arrayList, 0, orig.size(), false));

        final List<Integer> linkedList = new LinkedList<Integer>(orig);

        ListSampler.shuffle(rng, linkedList);
        // Ensure that at least one entry has moved.
        Assert.assertTrue("LinkedList", compare(orig, linkedList, 0, orig.size(), false));
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

    /**
     * Test shuffle matches {@link PermutationSampler#shuffle(UniformRandomProvider, int[])}.
     * The implementation may be different but the result is a Fisher-Yates shuffle so the
     * output order should match.
     */
    @Test
    public void testShuffleMatchesPermutationSamplerShuffle() {
        final List<Integer> orig = new ArrayList<Integer>();
        for (int i = 0; i < 10; i++) {
            orig.add((i + 1) * rng.nextInt());
        }

        assertShuffleMatchesPermutationSamplerShuffle(new ArrayList<Integer>(orig));
        assertShuffleMatchesPermutationSamplerShuffle(new LinkedList<Integer>(orig));
    }

    /**
     * Test shuffle matches {@link PermutationSampler#shuffle(UniformRandomProvider, int[], int, boolean)}.
     * The implementation may be different but the result is a Fisher-Yates shuffle so the
     * output order should match.
     */
    @Test
    public void testShuffleMatchesPermutationSamplerShuffleDirectional() {
        final List<Integer> orig = new ArrayList<Integer>();
        for (int i = 0; i < 10; i++) {
            orig.add((i + 1) * rng.nextInt());
        }

        assertShuffleMatchesPermutationSamplerShuffle(new ArrayList<Integer>(orig), 4, true);
        assertShuffleMatchesPermutationSamplerShuffle(new ArrayList<Integer>(orig), 4, false);
        assertShuffleMatchesPermutationSamplerShuffle(new LinkedList<Integer>(orig), 4, true);
        assertShuffleMatchesPermutationSamplerShuffle(new LinkedList<Integer>(orig), 4, false);
    }

    /**
     * This test hits the edge case when a LinkedList is small enough that the algorithm
     * using a RandomAccess list is faster than the one with an iterator.
     */
    @Test
    public void testShuffleWithSmallLinkedList() {
        final int size = 3;
        final List<Integer> orig = new ArrayList<Integer>();
        for (int i = 0; i < size; i++) {
            orig.add((i + 1) * rng.nextInt());
        }

        // When the size is small there is a chance that the list has no entries that move.
        // E.g. The number of permutations of 3 items is only 6 giving a 1/6 chance of no change.
        // So repeat test that the small shuffle matches the PermutationSampler.
        // 10 times is (1/6)^10 or 1 in 60,466,176 of no change.
        for (int i = 0; i < 10; i++) {
            assertShuffleMatchesPermutationSamplerShuffle(new LinkedList<Integer>(orig), size - 1, true);
        }
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

    /**
     * Assert the shuffle matches {@link PermutationSampler#shuffle(UniformRandomProvider, int[])}.
     *
     * @param list Array whose entries will be shuffled (in-place).
     */
    private static void assertShuffleMatchesPermutationSamplerShuffle(List<Integer> list) {
        final int[] array = new int[list.size()];
        ListIterator<Integer> it = list.listIterator();
        for (int i = 0; i < array.length; i++) {
            array[i] = it.next();
        }

        // Identical RNGs
        final long seed = RandomSource.createLong();
        final UniformRandomProvider rng1 = RandomSource.create(RandomSource.SPLIT_MIX_64, seed);
        final UniformRandomProvider rng2 = RandomSource.create(RandomSource.SPLIT_MIX_64, seed);

        ListSampler.shuffle(rng1, list);
        PermutationSampler.shuffle(rng2, array);

        final String msg = "Type=" + list.getClass().getSimpleName();
        it = list.listIterator();
        for (int i = 0; i < array.length; i++) {
            Assert.assertEquals(msg, array[i], it.next().intValue());
        }
    }
    /**
     * Assert the shuffle matches {@link PermutationSampler#shuffle(UniformRandomProvider, int[], int, boolean)}.
     *
     * @param list Array whose entries will be shuffled (in-place).
     * @param start Index at which shuffling begins.
     * @param towardHead Shuffling is performed for index positions between
     * {@code start} and either the end (if {@code false}) or the beginning
     * (if {@code true}) of the array.
     */
    private static void assertShuffleMatchesPermutationSamplerShuffle(List<Integer> list,
                                                                    int start,
                                                                    boolean towardHead) {
        final int[] array = new int[list.size()];
        ListIterator<Integer> it = list.listIterator();
        for (int i = 0; i < array.length; i++) {
            array[i] = it.next();
        }

        // Identical RNGs
        final long seed = RandomSource.createLong();
        final UniformRandomProvider rng1 = RandomSource.create(RandomSource.SPLIT_MIX_64, seed);
        final UniformRandomProvider rng2 = RandomSource.create(RandomSource.SPLIT_MIX_64, seed);

        ListSampler.shuffle(rng1, list, start, towardHead);
        PermutationSampler.shuffle(rng2, array, start, towardHead);

        final String msg = String.format("Type=%s start=%d towardHead=%b",
                list.getClass().getSimpleName(), start, towardHead);
        it = list.listIterator();
        for (int i = 0; i < array.length; i++) {
            Assert.assertEquals(msg, array[i], it.next().intValue());
        }
    }
}
