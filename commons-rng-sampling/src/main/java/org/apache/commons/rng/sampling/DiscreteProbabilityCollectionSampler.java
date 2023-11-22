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
public class DiscreteProbabilityCollectionSampler<T> implements SharedStateObjectSampler<T> {
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
        this(toList(collection),
             createSampler(rng, toProbabilities(collection)));
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
        this(copyList(collection),
             createSampler(rng, collection, probabilities));
    }

    /**
     * @param items Collection to be sampled.
     * @param sampler Sampler for the probabilities.
     */
    private DiscreteProbabilityCollectionSampler(List<T> items,
                                                 SharedStateDiscreteSampler sampler) {
        this.items = items;
        this.sampler = sampler;
    }

    /**
     * Picks one of the items from the collection passed to the constructor.
     *
     * @return a random sample.
     */
    @Override
    public T sample() {
        return items.get(sampler.sample());
    }

    /**
     * {@inheritDoc}
     *
     * @since 1.3
     */
    @Override
    public DiscreteProbabilityCollectionSampler<T> withUniformRandomProvider(UniformRandomProvider rng) {
        return new DiscreteProbabilityCollectionSampler<>(items, sampler.withUniformRandomProvider(rng));
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

    /**
     * Creates the sampler of the enumerated probability distribution.
     *
     * @param <T> Type of items in the collection.
     * @param rng Generator of uniformly distributed random numbers.
     * @param collection Collection to be sampled.
     * @param probabilities Probability associated to each item.
     * @return the sampler
     * @throws IllegalArgumentException if the number
     * of items in the {@code collection} is not equal to the number of
     * provided {@code probabilities}.
     */
    private static <T> SharedStateDiscreteSampler createSampler(UniformRandomProvider rng,
                                                                List<T> collection,
                                                                double[] probabilities) {
        if (probabilities.length != collection.size()) {
            throw new IllegalArgumentException("Size mismatch: " +
                                               probabilities.length + " != " +
                                               collection.size());
        }
        return GuideTableDiscreteSampler.of(rng, probabilities);
    }

    // Validation methods exist to raise an exception before invocation of the
    // private constructor; this mitigates Finalizer attacks
    // (see SpotBugs CT_CONSTRUCTOR_THROW).

    /**
     * Extract the items.
     *
     * @param <T> Type of items in the collection.
     * @param collection Collection.
     * @return the items
     * @throws IllegalArgumentException if {@code collection} is empty.
     */
    private static <T> List<T> toList(Map<T, Double> collection) {
        if (collection.isEmpty()) {
            throw new IllegalArgumentException(EMPTY_COLLECTION);
        }
        return new ArrayList<>(collection.keySet());
    }

    /**
     * Extract the probabilities.
     *
     * @param <T> Type of items in the collection.
     * @param collection Collection.
     * @return the probabilities
     */
    private static <T> double[] toProbabilities(Map<T, Double> collection) {
        final int size = collection.size();
        final double[] probabilities = new double[size];
        int count = 0;
        for (final Double e : collection.values()) {
            final double probability = e;
            if (probability < 0 ||
                Double.isInfinite(probability) ||
                Double.isNaN(probability)) {
                throw new IllegalArgumentException("Invalid probability: " +
                                                   probability);
            }
            probabilities[count++] = probability;
        }
        return probabilities;
    }

    /**
     * Create a (shallow) copy of the collection.
     *
     * @param <T> Type of items in the collection.
     * @param collection Collection.
     * @return the copy
     * @throws IllegalArgumentException if {@code collection} is empty.
     */
    private static <T> List<T> copyList(List<T> collection) {
        if (collection.isEmpty()) {
            throw new IllegalArgumentException(EMPTY_COLLECTION);
        }
        return new ArrayList<>(collection);
    }
}
