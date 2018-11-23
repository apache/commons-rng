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

import org.apache.commons.rng.UniformRandomProvider;

/**
 * Class for representing combinations of a sequence of integers.
 *
 * <p>A combination is a selection of items from a collection, such that (unlike
 * permutations) the order of selection <strong>does not matter</strong>. This
 * sampler can be used to generate a combination in an unspecified order and is
 * faster than the corresponding {@link PermutationSampler}.
 *
 * <p>Note that the sample order is unspecified. For example a sample
 * combination of 2 from 4 may return {@code [0,1]} or {@code [1,0]} as the two are
 * equivalent, and the order of a given combination may change in subsequent samples.
 *
 * <p>The sampler can be used to generate indices to select subsets where the
 * order of the subset is not important.
 *
 * @see <a href="https://en.wikipedia.org/wiki/Combination">Combination
 *      definition</a>
 * @see PermutationSampler
 */
public class CombinationSampler {
    /** Domain of the combination. */
    private final int[] domain;
    /** Size of the combination. */
    private final int size;
    /** The number of steps of a full shuffle to perform. */
    private final int steps;
    /**
     * The position to copy the domain from after a partial shuffle.
     *
     * <p>The copy is either in the range [0 : size] or [domain.length - size :
     * domain.length].
     */
    private final int from;
    /** RNG. */
    private final UniformRandomProvider rng;

    /**
     * Creates a generator of combinations.
     *
     * <p>The {@link #sample()} method will generate an integer array of
     * length {@code k} whose entries are selected randomly, without
     * repetition, from the integers 0, 1, ..., {@code n}-1 (inclusive).
     * The returned array represents a combination of {@code n} taken
     * {@code k}.
     *
     * <p>In contrast to a permutation, the returned array is <strong>not
     * guaranteed</strong> to be in a random order. The {@link #sample()}
     * method returns the array in an unspecified order.
     *
     * <p>If {@code n <= 0} or {@code k <= 0} or {@code k > n} then no combination
     * is required and an exception is raised.
     *
     * @param rng Generator of uniformly distributed random numbers.
     * @param n   Domain of the combination.
     * @param k   Size of the combination.
     * @throws IllegalArgumentException if {@code n <= 0} or {@code k <= 0} or
     *                                  {@code k > n}.
     */
    public CombinationSampler(UniformRandomProvider rng, int n, int k) {
        if (n <= 0) {
            throw new IllegalArgumentException("n <= 0 : n=" + n);
        }
        if (k <= 0) {
            throw new IllegalArgumentException("k <= 0 : k=" + k);
        }
        if (k > n) {
            throw new IllegalArgumentException("k > n : k=" + k + ", n=" + n);
        }

        domain = PermutationSampler.natural(n);
        size = k;
        // The sample can be optimised by only performing the first k or (n - k) steps
        // from a full Fisher-Yates shuffle from the end of the domain to the start.
        // The upper positions will then contain a random sample from the domain. The
        // lower half is then by definition also a random sample (just not in a random order).
        // The sample is then picked using the upper or lower half depending which
        // makes the number of steps smaller.
        if (k <= n / 2) {
            // Upper half
            steps = k;
            from = n - k;
        } else {
            // Lower half
            steps = n - k;
            from = 0;
        }
        this.rng = rng;
    }

    /**
     * Return a combination of {@code k} whose entries are selected randomly,
     * without repetition, from the integers 0, 1, ..., {@code n}-1 (inclusive).
     *
     * <p>The order of the returned array is not guaranteed to be in a random order
     * as the order of a combination <strong>does not matter</strong>.
     *
     * @return a random combination.
     */
    public int[] sample() {
        // Shuffle from the end but limit to a number of steps.
        // The subset C(n, k) is then either those positions that have
        // been sampled, or those that haven't been, depending
        // on the number of steps.
        // Note: if n==k the number of steps is zero and the result
        // is just a clone of the domain.
        for (int i = domain.length - 1,
                j = 0; i > 0 && j < steps; i--, j++) {
            // Swap index i with any position down to 0 (including itself)
            swap(domain, i, rng.nextInt(i + 1));
        }
        final int[] result = new int[size];
        System.arraycopy(domain, from, result, 0, size);
        return result;
    }

    /**
     * Swaps the two specified elements in the specified array.
     *
     * @param array the array
     * @param i     the first index
     * @param j     the second index
     */
    private static void swap(int[] array, int i, int j) {
        final int tmp = array[i];
        array[i] = array[j];
        array[j] = tmp;
    }
}
