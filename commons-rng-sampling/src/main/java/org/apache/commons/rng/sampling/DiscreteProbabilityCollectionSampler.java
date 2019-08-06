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
import java.util.Map;
import java.util.ArrayList;

import org.apache.commons.rng.UniformRandomProvider;
import org.apache.commons.rng.sampling.distribution.GuideTableDiscreteSampler;
import org.apache.commons.rng.sampling.distribution.SharedStateDiscreteSampler;

/**
 * Sampling from a collection of items with user-defined
 * <a href="http://en.wikipedia.org/wiki/Probability_distribution#Discrete_probability_distribution">
 * probabilities</a>.
 * Note that if all unique items are assigned the same probability,
 * it is much more efficient to use {@link CollectionSampler}.
 *
 * <p>Sampling uses {@link UniformRandomProvider#nextDouble()}.</p>
 *
 * @param <T> Type of items in the collection.
 *
 * @since 1.1
 */
public class DiscreteProbabilityCollectionSampler<T>
    implements SharedStateSampler<DiscreteProbabilityCollectionSampler<T>> {
    /** The error message for an empty collection. */
    private static final String EMPTY_COLLECTION = "Empty collection";
    /** Collection to be sampled from. */
    private final List<T> items;
    /** Sampler for the probabilities. */
    private final SharedStateDiscreteSampler sampler;

    /**
     * Creates a sampler.
     *
     * @param rng Generator of uniformly distributed random numbers.
     * @param collection Collection to be sampled, with the probabilities
     * associated to each of its items.
     * A (shallow) copy of the items will be stored in the created instance.
     * The probabilities must be non-negative, but zero values are allowed
     * and their sum does not have to equal one (input will be normalized
     * to make the probabilities sum to one).
     * @throws IllegalArgumentException if {@code collection} is empty, a
     * probability is negative, infinite or {@code NaN}, or the sum of all
     * probabilities is not strictly positive.
     */
    public DiscreteProbabilityCollectionSampler(UniformRandomProvider rng,
                                                Map<T, Double> collection) {
        if (collection.isEmpty()) {
            throw new IllegalArgumentException(EMPTY_COLLECTION);
        }

        // Extract the items and probabilities
        final int size = collection.size();
        items = new ArrayList<T>(size);
        final double[] probabilities = new double[size];

        int count = 0;
        for (final Map.Entry<T, Double> e : collection.entrySet()) {
            items.add(e.getKey());
            probabilities[count++] = e.getValue();
        }

        // Delegate sampling
        sampler = createSampler(rng, probabilities);
    }

    /**
     * Creates a sampler.
     *
     * @param rng Generator of uniformly distributed random numbers.
     * @param collection Collection to be sampled.
     * A (shallow) copy of the items will be stored in the created instance.
     * @param probabilities Probability associated to each item of the
     * {@code collection}.
     * The probabilities must be non-negative, but zero values are allowed
     * and their sum does not have to equal one (input will be normalized
     * to make the probabilities sum to one).
     * @throws IllegalArgumentException if {@code collection} is empty or
     * a probability is negative, infinite or {@code NaN}, or if the number
     * of items in the {@code collection} is not equal to the number of
     * provided {@code probabilities}.
     */
    public DiscreteProbabilityCollectionSampler(UniformRandomProvider rng,
                                                List<T> collection,
                                                double[] probabilities) {
        if (collection.isEmpty()) {
            throw new IllegalArgumentException(EMPTY_COLLECTION);
        }
        final int len = probabilities.length;
        if (len != collection.size()) {
            throw new IllegalArgumentException("Size mismatch: " +
                                               len + " != " +
                                               collection.size());
        }
        // Shallow copy the list
        items = new ArrayList<T>(collection);
        // Delegate sampling
        sampler = createSampler(rng, probabilities);
    }

    /**
     * @param rng Generator of uniformly distributed random numbers.
     * @param source Source to copy.
     */
    private DiscreteProbabilityCollectionSampler(UniformRandomProvider rng,
                                                 DiscreteProbabilityCollectionSampler<T> source) {
        this.items = source.items;
        this.sampler = source.sampler.withUniformRandomProvider(rng);
    }

    /**
     * Picks one of the items from the collection passed to the constructor.
     *
     * @return a random sample.
     */
    public T sample() {
        return items.get(sampler.sample());
    }

    /** {@inheritDoc} */
    @Override
    public DiscreteProbabilityCollectionSampler<T> withUniformRandomProvider(UniformRandomProvider rng) {
        return new DiscreteProbabilityCollectionSampler<T>(rng, this);
    }

    /**
     * Creates the sampler of the enumerated probability distribution.
     *
     * @param rng Generator of uniformly distributed random numbers.
     * @param probabilities Probability associated to each item.
     * @return the sampler
     */
    private static SharedStateDiscreteSampler createSampler(UniformRandomProvider rng,
                                                            double[] probabilities) {
        return GuideTableDiscreteSampler.of(rng, probabilities);
    }
}
