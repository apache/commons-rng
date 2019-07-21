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
package org.apache.commons.rng.sampling.distribution;

import org.apache.commons.rng.UniformRandomProvider;

/**
 * Sampler for the <a href="http://mathworld.wolfram.com/PoissonDistribution.html">Poisson distribution</a>.
 *
 * <ul>
 *  <li>
 *   For small means, a Poisson process is simulated using uniform deviates, as described in
 *   <blockquote>
 *    Knuth (1969). <i>Seminumerical Algorithms</i>. The Art of Computer Programming,
 *    Volume 2. Chapter 3.4.1.F.3 Important integer-valued distributions: The Poisson distribution.
 *    Addison Wesley.
 *   </blockquote>
 *   The Poisson process (and hence, the returned value) is bounded by {@code 1000 * mean}.
 *  </li>
 *  <li>
 *   For large means, we use the rejection algorithm described in
 *   <blockquote>
 *    Devroye, Luc. (1981). <i>The Computer Generation of Poisson Random Variables</i><br>
 *    <strong>Computing</strong> vol. 26 pp. 197-207.
 *   </blockquote>
 *  </li>
 * </ul>
 *
 * <p>Sampling uses:</p>
 *
 * <ul>
 *   <li>{@link UniformRandomProvider#nextDouble()}
 *   <li>{@link UniformRandomProvider#nextLong()} (large means only)
 * </ul>
 *
 * @since 1.0
 */
public class PoissonSampler
    extends SamplerBase
    implements SharedStateDiscreteSampler {

    /**
     * Value for switching sampling algorithm.
     *
     * <p>Package scope for the {@link PoissonSamplerCache}.
     */
    static final double PIVOT = 40;
    /** The internal Poisson sampler. */
    private final SharedStateDiscreteSampler poissonSamplerDelegate;

    /**
     * This instance delegates sampling. Use the factory method
     * {@link #of(UniformRandomProvider, double)} to create an optimal sampler.
     *
     * @param rng Generator of uniformly distributed random numbers.
     * @param mean Mean.
     * @throws IllegalArgumentException if {@code mean <= 0} or
     * {@code mean >} {@link Integer#MAX_VALUE}.
     */
    public PoissonSampler(UniformRandomProvider rng,
                          double mean) {
        super(null);

        // Delegate all work to specialised samplers.
        poissonSamplerDelegate = of(rng, mean);
    }

    /** {@inheritDoc} */
    @Override
    public int sample() {
        return poissonSamplerDelegate.sample();
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return poissonSamplerDelegate.toString();
    }

    /** {@inheritDoc} */
    @Override
    public SharedStateDiscreteSampler withUniformRandomProvider(UniformRandomProvider rng) {
        // Direct return of the optimised sampler
        return poissonSamplerDelegate.withUniformRandomProvider(rng);
    }

    /**
     * Creates a new Poisson distribution sampler.
     *
     * @param rng Generator of uniformly distributed random numbers.
     * @param mean Mean.
     * @return the sampler
     * @throws IllegalArgumentException if {@code mean <= 0} or {@code mean >}
     * {@link Integer#MAX_VALUE}.
     */
    public static SharedStateDiscreteSampler of(UniformRandomProvider rng,
                                                double mean) {
        // Each sampler should check the input arguments.
        return mean < PIVOT ?
            SmallMeanPoissonSampler.of(rng, mean) :
            LargeMeanPoissonSampler.of(rng, mean);
    }
}
