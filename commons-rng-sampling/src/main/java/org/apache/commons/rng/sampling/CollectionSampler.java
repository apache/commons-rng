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

import java.util.Collection;
import java.util.List;
import java.util.ArrayList;

import org.apache.commons.rng.UniformRandomProvider;

/**
 * Sampling from a {@link Collection}.
 *
 * @param <T> Type of items in the collection.
 *
 * This class also contains utilities for shuffling a generic {@link List}.
 *
 * @since 1.0
 */
public class CollectionSampler<T> {
    /** Collection to be sampled from. */
    private final ArrayList<T> items;
    /** Permutation. */
    private final PermutationSampler permutation;
    /** Size of returned list. */
    private final int size;

    /**
     * Creates a sampler.
     *
     * The {@link #sample()} method will generate a collection of
     * size {@code k} whose entries are selected randomly, without
     * repetition, from the items in the given {@code collection}.
     *
     * @param rng Generator of uniformly distributed random numbers.
     * @param collection Collection to be sampled.
     * A (shallow) copy will be stored in the created instance.
     * @param k Size of the permutation.
     * @throws IllegalArgumentException if {@code k <= 0} or
     * {@code k > collection.size()}.
     */
    public CollectionSampler(UniformRandomProvider rng,
                             Collection<T> collection,
                             int k) {
        permutation = new PermutationSampler(rng, collection.size(), k);
        items = new ArrayList<T>(collection);
        size = k;
    }

    /**
     * Creates a list of objects selected randomly, without repetition,
     * from the collection provided at
     * {@link #CollectionSampler(UniformRandomProvider,Collection,int)
     * construction}.
     *
     * <p>
     * Sampling is without replacement; but if the source collection
     * contains identical objects, the sample may include repeats.
     * </p>
     * <p>
     * There is no guarantee that the concrete type of the returned
     * collection is the same as the source collection.
     * </p>
     *
     * @return a random sample.
     */
    public Collection<T> sample() {
        final ArrayList<T> result = new ArrayList<T>(size);
        final int[] index = permutation.sample();

        for (int i = 0; i < size; i++) {
            result.add(items.get(index[i]));
        }

        return result;
    }
    /**
     * Shuffles the entries of the given array.
     *
     * @see #shuffle(List,int,boolean,UniformRandomProvider)
     *
     * @param <S> Type of the list items.
     * @param list List whose entries will be shuffled (in-place).
     * @param rng Random number generator.
     */
    public static <S> void shuffle(List<S> list,
                                   UniformRandomProvider rng) {
        shuffle(list, 0, false, rng);
    }

    /**
     * Shuffles the entries of the given array, using the
     * <a href="http://en.wikipedia.org/wiki/Fisher–Yates_shuffle#The_modern_algorithm">
     * Fisher-Yates</a> algorithm.
     * The {@code start} and {@code pos} parameters select which part
     * of the array is randomized and which is left untouched.
     *
     * @param <S> Type of the list items.
     * @param list List whose entries will be shuffled (in-place).
     * @param start Index at which shuffling begins.
     * @param towardHead Shuffling is performed for index positions between
     * {@code start} and either the end (if {@code false}) or the beginning
     * (if {@code true}) of the array.
     * @param rng Random number generator.
     */
    public static <S> void shuffle(List<S> list,
                                   int start,
                                   boolean towardHead,
                                   UniformRandomProvider rng) {
        final int len = list.size();
        final int[] indices = PermutationSampler.natural(len);
        PermutationSampler.shuffle(indices, start, towardHead, rng);

        final ArrayList<S> items = new ArrayList<S>(list);
        for (int i = 0; i < len; i++) {
            list.set(i, items.get(indices[i]));
        }
    }
}
