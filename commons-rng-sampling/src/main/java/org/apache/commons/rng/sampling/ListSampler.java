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

import java.util.List;
import java.util.ListIterator;
import java.util.RandomAccess;
import java.util.ArrayList;

import org.apache.commons.rng.UniformRandomProvider;

/**
 * Sampling from a {@link List}.
 *
 * <p>This class also contains utilities for shuffling a {@link List} in-place.</p>
 *
 * @since 1.0
 */
public final class ListSampler {
    /**
     * The size threshold for using the random access algorithm
     * when the list does not implement java.util.RandomAccess.
     */
    private static final int RANDOM_ACCESS_SIZE_THRESHOLD = 4;

    /**
     * Class contains only static methods.
     */
    private ListSampler() {}

    /**
     * Generates a list of size {@code k} whose entries are selected
     * randomly, without repetition, from the items in the given
     * {@code collection}.
     *
     * <p>
     * Sampling is without replacement; but if the source collection
     * contains identical objects, the sample may include repeats.
     * </p>
     *
     * <p>
     * Sampling uses {@link UniformRandomProvider#nextInt(int)}.
     * </p>
     *
     * @param <T> Type of the list items.
     * @param rng Generator of uniformly distributed random numbers.
     * @param collection List to be sampled from.
     * @param k Size of the returned sample.
     * @throws IllegalArgumentException if {@code k <= 0} or
     * {@code k > collection.size()}.
     * @return a shuffled sample from the source collection.
     */
    public static <T> List<T> sample(UniformRandomProvider rng,
                                     List<T> collection,
                                     int k) {
        final int n = collection.size();
        final PermutationSampler p = new PermutationSampler(rng, n, k);
        final List<T> result = new ArrayList<T>(k);
        final int[] index = p.sample();

        for (int i = 0; i < k; i++) {
            result.add(collection.get(index[i]));
        }

        return result;
    }

    /**
     * Shuffles the entries of the given array, using the
     * <a href="http://en.wikipedia.org/wiki/Fisher-Yates_shuffle#The_modern_algorithm">
     * Fisher-Yates</a> algorithm.
     *
     * <p>
     * Sampling uses {@link UniformRandomProvider#nextInt(int)}.
     * </p>
     *
     * @param <T> Type of the list items.
     * @param rng Random number generator.
     * @param list List whose entries will be shuffled (in-place).
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static <T> void shuffle(UniformRandomProvider rng,
                                   List<T> list) {
        if (list instanceof RandomAccess || list.size() < RANDOM_ACCESS_SIZE_THRESHOLD) {
            // Shuffle list in-place
            for (int i = list.size(); i > 1; i--) {
                swap(list, i - 1, rng.nextInt(i));
            }
        } else {
            // Shuffle as an array
            final Object[] array = list.toArray();
            for (int i = array.length; i > 1; i--) {
                swap(array, i - 1, rng.nextInt(i));
            }

            // Copy back. Use raw types.
            final ListIterator it = list.listIterator();
            for (final Object item : array) {
                it.next();
                it.set(item);
            }
        }
    }

    /**
     * Shuffles the entries of the given array, using the
     * <a href="http://en.wikipedia.org/wiki/Fisher-Yates_shuffle#The_modern_algorithm">
     * Fisher-Yates</a> algorithm.
     *
     * <p>
     * The {@code start} and {@code pos} parameters select which part
     * of the array is randomized and which is left untouched.
     * </p>
     *
     * <p>
     * Sampling uses {@link UniformRandomProvider#nextInt(int)}.
     * </p>
     *
     * @param <T> Type of the list items.
     * @param rng Random number generator.
     * @param list List whose entries will be shuffled (in-place).
     * @param start Index at which shuffling begins.
     * @param towardHead Shuffling is performed for index positions between
     * {@code start} and either the end (if {@code false}) or the beginning
     * (if {@code true}) of the array.
     */
    public static <T> void shuffle(UniformRandomProvider rng,
                                   List<T> list,
                                   int start,
                                   boolean towardHead) {
        // Shuffle in-place as a sub-list.
        if (towardHead) {
            shuffle(rng, list.subList(0, start + 1));
        } else {
            shuffle(rng, list.subList(start, list.size()));
        }
    }

    /**
     * Swaps the two specified elements in the list.
     *
     * @param <T> Type of the list items.
     * @param list List.
     * @param i First index.
     * @param j Second index.
     */
    private static <T> void swap(List<T> list, int i, int j) {
        final T tmp = list.get(i);
        list.set(i, list.get(j));
        list.set(j, tmp);
    }

    /**
     * Swaps the two specified elements in the array.
     *
     * @param array Array.
     * @param i First index.
     * @param j Second index.
     */
    private static void swap(Object[] array, int i, int j) {
        final Object tmp = array[i];
        array[i] = array[j];
        array[j] = tmp;
    }
}
