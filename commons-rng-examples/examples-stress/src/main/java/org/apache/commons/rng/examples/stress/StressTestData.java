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
package org.apache.commons.rng.examples.stress;

import org.apache.commons.rng.UniformRandomProvider;
import org.apache.commons.rng.simple.RandomSource;

/**
 * Encapsulate the data needed to create and test a random generator. This includes:
 *
 * <ul>
 *   <li>An identifier for the random source
 *   <li>The random source
 *   <li>The constructor arguments
 *   <li>The number of trials for each random source
 * </ul>
 */
class StressTestData {
    /** The identifier. */
    private final String id;
    /** The random source. */
    private final RandomSource randomSource;
    /** The arguments used to construct the random source. */
    private final Object[] args;
    /** The number of trials. */
    private final int trials;

    /**
     * Creates a new instance.
     *
     * @param randomSource The random source.
     * @param args The arguments used to construct the random source (can be {@code null}).
     */
    StressTestData(RandomSource randomSource,
                   Object[] args) {
        // The default ID is defined by the enum order.
        this(Integer.toString(randomSource.ordinal() + 1),
             randomSource,
             args,
             // Ignore by default (trials = 0) any source that has arguments
             args == null ? 1 : 0);
    }

    /**
     * Creates a new instance.
     *
     * @param id The identifier.
     * @param randomSource The random source.
     * @param args The arguments used to construct the random source (can be
     * {@code null}).
     * @param trials The number of trials.
     */
    StressTestData(String id,
                   RandomSource randomSource,
                   Object[] args,
                   int trials) {
        this.id = id;
        this.randomSource = randomSource;
        this.args = args;
        this.trials = trials;
    }

    /**
     * Create a new instance with the given ID prefix prepended to the current ID.
     *
     * @param idPrefix The ID prefix.
     * @return the stress test data
     */
    StressTestData withIDPrefix(String idPrefix) {
        return new StressTestData(idPrefix + id, randomSource, args, trials);
    }

    /**
     * Create a new instance with the given number of trials.
     *
     * @param numberOfTrials The number of trials.
     * @return the stress test data
     */
    StressTestData withTrials(int numberOfTrials) {
        return new StressTestData(id, randomSource, args, numberOfTrials);
    }

    /**
     * Creates the random generator.
     *
     * <p>It is recommended the seed is generated using {@link RandomSource#createSeed()}.</p>
     *
     * @param seed the seed (use {@code null} to automatically create a seed)
     * @return the uniform random provider
     */
    UniformRandomProvider createRNG(byte[] seed) {
        return randomSource.create(seed, args);
    }

    /**
     * Gets the identifier.
     *
     * @return the id
     */
    String getId() {
        return id;
    }

    /**
     * Gets the random source.
     *
     * @return the random source
     */
    RandomSource getRandomSource() {
        return randomSource;
    }

    /**
     * Gets the arguments used to construct the random source.
     *
     * @return the arguments
     */
    Object[] getArgs() {
        return args;
    }

    /**
     * Gets the number of trials.
     *
     * @return the number of trials
     */
    int getTrials() {
        return trials;
    }
}
