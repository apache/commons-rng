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
 * Sampler for the <a href="http://mathworld.wolfram.com/PoissonDistribution.html">Poisson
 * distribution</a>.
 *
 * <ul>
 *   <li>
 *     Kemp, A, W, (1981) Efficient Generation of Logarithmically Distributed
 *     Pseudo-Random Variables. Journal of the Royal Statistical Society. Vol. 30, No. 3, pp.
 *     249-253.
 *   </li>
 * </ul>
 *
 * <p>This sampler is suitable for {@code mean < 40}. For large means,
 * {@link LargeMeanPoissonSampler} should be used instead.</p>
 *
 * <p>Note: The algorithm uses a recurrence relation to compute the Poisson probability
 * and a rolling summation for the cumulative probability. When the mean is large the
 * initial probability (Math.exp(-mean)) is zero and an exception is raised by the
 * constructor.</p>
 *
 * <p>Sampling uses 1 call to {@link UniformRandomProvider#nextDouble()}. This method provides
 * an alternative to the {@link SmallMeanPoissonSampler} for slow generators of {@code double}.</p>
 *
 * @see <a href="https://www.jstor.org/stable/2346348">Kemp, A.W. (1981) JRSS Vol. 30, pp.
 * 249-253</a>
 * @since 1.3
 */
public final class KempSmallMeanPoissonSampler
    implements SharedStateDiscreteSampler {
    /** Underlying source of randomness. */
    private final UniformRandomProvider rng;
    /**
     * Pre-compute {@code Math.exp(-mean)}.
     * Note: This is the probability of the Poisson sample {@code p(x=0)}.
     */
    private final double p0;
    /**
     * The mean of the Poisson sample.
     */
    private final double mean;

    /**
     * @param rng Generator of uniformly distributed random numbers.
     * @param p0 Probability of the Poisson sample {@code p(x=0)}.
     * @param mean Mean.
     */
    private KempSmallMeanPoissonSampler(UniformRandomProvider rng,
                                        double p0,
                                        double mean) {
        this.rng = rng;
        this.p0 = p0;
        this.mean = mean;
    }

    /** {@inheritDoc} */
    @Override
    public int sample() {
        // Note on the algorithm:
        // - X is the unknown sample deviate (the output of the algorithm)
        // - x is the current value from the distribution
        // - p is the probability of the current value x, p(X=x)
        // - u is effectively the cumulative probability that the sample X
        //   is equal or above the current value x, p(X>=x)
        // So if p(X>=x) > p(X=x) the sample must be above x, otherwise it is x
        double u = rng.nextDouble();
        int x = 0;
        double p = p0;
        while (u > p) {
            u -= p;
            // Compute the next probability using a recurrence relation.
            // p(x+1) = p(x) * mean / (x+1)
            p *= mean / ++x;
            // The algorithm listed in Kemp (1981) does not check that the rolling probability
            // is positive. This check is added to ensure no errors when the limit of the summation
            // 1 - sum(p(x)) is above 0 due to cumulative error in floating point arithmetic.
            if (p == 0) {
                return x;
            }
        }
        return x;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return "Kemp Small Mean Poisson deviate [" + rng.toString() + "]";
    }

    /** {@inheritDoc} */
    @Override
    public SharedStateDiscreteSampler withUniformRandomProvider(UniformRandomProvider rng) {
        return new KempSmallMeanPoissonSampler(rng, p0, mean);
    }

    /**
     * Creates a new sampler for the Poisson distribution.
     *
     * @param rng Generator of uniformly distributed random numbers.
     * @param mean Mean of the distribution.
     * @return the sampler
     * @throws IllegalArgumentException if {@code mean <= 0} or
     * {@code Math.exp(-mean) == 0}.
     */
    public static SharedStateDiscreteSampler of(UniformRandomProvider rng,
                                                double mean) {
        if (mean <= 0) {
            throw new IllegalArgumentException("Mean is not strictly positive: " + mean);
        }

        final double p0 = Math.exp(-mean);

        // Probability must be positive. As mean increases then p(0) decreases.
        if (p0 > 0) {
            return new KempSmallMeanPoissonSampler(rng, p0, mean);
        }

        // This catches the edge case of a NaN mean
        throw new IllegalArgumentException("No probability for mean: " + mean);
    }
}
