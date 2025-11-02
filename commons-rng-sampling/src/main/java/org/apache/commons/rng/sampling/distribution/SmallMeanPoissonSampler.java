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
 * Sampler for the <a href="https://mathworld.wolfram.com/PoissonDistribution.html">Poisson distribution</a>.
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
 * </ul>
 *
 * <p>This sampler is suitable for {@code mean < 40}.
 * For large means, {@link LargeMeanPoissonSampler} should be used instead.</p>
 *
 * <p>Sampling uses {@link UniformRandomProvider#nextDouble()} and requires on average
 * {@code mean + 1} deviates per sample.</p>
 *
 * @since 1.1
 */
public class SmallMeanPoissonSampler
    implements SharedStateDiscreteSampler {
    /**
     * Pre-compute {@code Math.exp(-mean)}.
     * Note: This is the probability of the Poisson sample {@code P(n=0)}.
     */
    private final double p0;
    /** Pre-compute {@code 1000 * mean} as the upper limit of the sample. */
    private final int limit;
    /** Underlying source of randomness. */
    private final UniformRandomProvider rng;

    /**
     * Create an instance.
     *
     * @param rng  Generator of uniformly distributed random numbers.
     * @param mean Mean.
     * @throws IllegalArgumentException if {@code mean <= 0} or {@code Math.exp(-mean) == 0}
     */
    public SmallMeanPoissonSampler(UniformRandomProvider rng,
                                   double mean) {
        this(rng, mean, computeP0(mean));
    }

    /**
     * Instantiates a new small mean poisson sampler.
     *
     * @param rng  Generator of uniformly distributed random numbers.
     * @param mean Mean.
     * @param p0 {@code Math.exp(-mean)}.
     */
    private SmallMeanPoissonSampler(UniformRandomProvider rng,
                                    double mean,
                                    double p0) {
        this.rng = rng;
        this.p0 = p0;
        // The returned sample is bounded by 1000 * mean
        limit = (int) Math.ceil(1000 * mean);
    }

    /**
     * @param rng Generator of uniformly distributed random numbers.
     * @param source Source to copy.
     */
    private SmallMeanPoissonSampler(UniformRandomProvider rng,
                                    SmallMeanPoissonSampler source) {
        this.rng = rng;
        p0 = source.p0;
        limit = source.limit;
    }

    /** {@inheritDoc} */
    @Override
    public int sample() {
        int n = 0;
        double r = 1;

        while (n < limit) {
            r *= rng.nextDouble();
            if (r >= p0) {
                n++;
            } else {
                break;
            }
        }
        return n;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return "Small Mean Poisson deviate [" + rng.toString() + "]";
    }

    /**
     * {@inheritDoc}
     *
     * @since 1.3
     */
    @Override
    public SharedStateDiscreteSampler withUniformRandomProvider(UniformRandomProvider rng) {
        return new SmallMeanPoissonSampler(rng, this);
    }

    /**
     * Creates a new sampler for the Poisson distribution.
     *
     * @param rng Generator of uniformly distributed random numbers.
     * @param mean Mean of the distribution.
     * @return the sampler
     * @throws IllegalArgumentException if {@code mean <= 0} or {@code Math.exp(-mean) == 0}.
     * @since 1.3
     */
    public static SharedStateDiscreteSampler of(UniformRandomProvider rng,
                                                double mean) {
        return new SmallMeanPoissonSampler(rng, mean);
    }

    /**
     * Compute {@code Math.exp(-mean)}.
     *
     * <p>This method exists to raise an exception before invocation of the
     * private constructor; this mitigates Finalizer attacks
     * (see SpotBugs CT_CONSTRUCTOR_THROW).
     *
     * @param mean Mean.
     * @return the mean
     * @throws IllegalArgumentException if {@code mean <= 0} or {@code Math.exp(-mean) == 0}
     */
    private static double computeP0(double mean) {
        InternalUtils.requireStrictlyPositive(mean, "mean");
        final double p0 = Math.exp(-mean);
        if (p0 > 0) {
            return p0;
        }
        // This excludes NaN values for the mean
        throw new IllegalArgumentException("No p(x=0) probability for mean: " + mean);
    }
}
