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

import java.util.Arrays;

import org.apache.commons.rng.UniformRandomProvider;

/**
 * Class for representing permutations of a sequence of integers.
 *
 * <p>This class also contains utilities for shuffling an {@code int[]} array in-place.
 *
 * @see <a href="https://en.wikipedia.org/wiki/Permutation">Permutation definition</a>
 */
public class PermutationSampler {
    /** Domain of the permutation. */
    private final int[] domain;
    /** Size of the permutation. */
    private final int size;
    /** RNG. */
    private final UniformRandomProvider rng;

    /**
     * Creates a generator of permutations.
     *
     * <p>The {@link #sample()} method will generate an integer array of
     * length {@code k} whose entries are selected randomly, without
     * repetition, from the integers 0, 1, ..., {@code n}-1 (inclusive).
     * The returned array represents a permutation of {@code n} taken
     * {@code k}.
     *
     * @param rng Generator of uniformly distributed random numbers.
     * @param n Domain of the permutation.
     * @param k Size of the permutation.
     * @throws IllegalArgumentException if {@code n < 0} or {@code k <= 0}
     * or {@code k > n}.
     */
    public PermutationSampler(UniformRandomProvider rng,
                              int n,
                              int k) {
        if (n < 0) {
            throw new IllegalArgumentException("n < 0 : n=" + n);
        }
        if (k <= 0) {
            throw new IllegalArgumentException("k <= 0 : k=" + k);
        }
        if (k > n) {
            throw new IllegalArgumentException("k > n : k=" + k + ", n=" + n);
        }

        domain = natural(n);
        size = k;
        this.rng = rng;
    }

    /**
     * @return a random permutation.
     *
     * @see #PermutationSampler(UniformRandomProvider,int,int)
     */
    public int[] sample() {
        shuffle(rng, domain);
        return Arrays.copyOf(domain, size);
    }

    /**
     * Shuffles the entries of the given array.
     *
     * @see #shuffle(UniformRandomProvider,int[],int,boolean)
     *
     * @param rng Random number generator.
     * @param list Array whose entries will be shuffled (in-place).
     */
    public static void shuffle(UniformRandomProvider rng,
                               int[] list) {
        shuffle(rng, list, list.length - 1, true);
    }

    /**
     * Shuffles the entries of the given array, using the
     * <a href="http://en.wikipedia.org/wiki/Fisher-Yates_shuffle#The_modern_algorithm">
     * Fisher-Yates</a> algorithm.
     * The {@code start} and {@code towardHead} parameters select which part
     * of the array is randomized and which is left untouched.
     *
     * @param rng Random number generator.
     * @param list Array whose entries will be shuffled (in-place).
     * @param start Index at which shuffling begins.
     * @param towardHead Shuffling is performed for index positions between
     * {@code start} and either the end (if {@code false}) or the beginning
     * (if {@code true}) of the array.
     */
    public static void shuffle(UniformRandomProvider rng,
                               int[] list,
                               int start,
                               boolean towardHead) {
        if (towardHead) {
            // Visit all positions from start to 0.
            // Do not visit 0 to avoid a swap with itself.
            for (int i = start; i > 0; i--) {
                // Swap index with any position down to 0
                swap(list, i, rng.nextInt(i + 1));
            }
        } else {
            // Visit all positions from the end to start.
            // Start is not visited to avoid a swap with itself.
            for (int i = list.length - 1; i > start; i--) {
                // Swap index with any position down to start.
                // Note: i - start + 1 is the number of elements remaining.
                swap(list, i, rng.nextInt(i - start + 1) + start);
            }
        }
    }

    /**
     * Swaps the two specified elements in the specified array.
     *
     * @param array the array
     * @param i the first index
     * @param j the second index
     */
    private static void swap(int[] array, int i, int j) {
        final int tmp = array[i];
        array[i] = array[j];
        array[j] = tmp;
    }

    /**
     * Creates an array representing the natural number {@code n}.
     *
     * @param n Natural number.
     * @return an array whose entries are the numbers 0, 1, ..., {@code n}-1.
     * If {@code n == 0}, the returned array is empty.
     */
    public static int[] natural(int n) {
        final int[] a = new int[n];
        for (int i = 0; i < n; i++) {
            a[i] = i;
        }
        return a;
    }
}
