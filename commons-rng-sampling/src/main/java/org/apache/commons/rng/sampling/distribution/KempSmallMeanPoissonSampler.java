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
 *   For small means, a Poisson process is simulated using uniform deviates, as
 *   described in Kemp, A, W, (1981) Efficient Generation of Logarithmically Distributed
 *   Pseudo-Random Variables. Journal of the Royal Statistical Society. Vol. 30, No. 3, pp. 249-253.
 *  </li>
 * </ul>
 *
 * <p>This sampler is suitable for {@code mean < 40}.
 * For large means, {@link LargeMeanPoissonSampler} should be used instead.</p>
 *
 * <p>Note: The algorithm uses a recurrence relation to compute the Poisson probability and
 * a rolling summation for the cumulative probability. When the mean is large the
 * initial probability (Math.exp(-mean)) is zero and an exception is raised by the constructor.</p>
 *
 * <p>Sampling uses 1 call to {@link UniformRandomProvider#nextDouble()}.</p>
 *
 * @since 1.3
 * @see <a href="https://www.jstor.org/stable/2346348">Kemp, A.W. (1981) JRSS Vol. 30, pp. 249-253</a>
 */
public class KempSmallMeanPoissonSampler
    implements DiscreteSampler {
    /** The value of p=0.5. */
    private static final double ONE_HALF = 0.5;
    /**
     * The threshold that defines the cumulative probability for the long tail of the
     * Poisson distribution. Above this threshold the recurrence relation that computes the
     * next probability must check that the p-value is not zero.
     */
    private static final double LONG_TAIL_THRESHOLD = 0.999;

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
     * Pre-compute the cumulative probability for all samples up to and including x.
     * This is F(x) = sum of p(X<=x).
     *
     * <p>The value is computed at approximately 50% allowing the algorithm to choose to start
     * at value (x+1) approximately half of the time.
     */
    private final double fx;
    /**
     * Store the value (x+1) corresponding to the next value after the cumulative probability is
     * above 50%.
     */
    private final int x1;
    /**
     * Store the probability value p(x+1), allowing the algorithm to start from the point n+1.
     */
    private final double px1;

    /**
     * Create a new instance.
     *
     * <p>This is valid for means as large as approximately {@code 744}.</p>
     *
     * @param rng  Generator of uniformly distributed random numbers.
     * @param mean Mean.
     * @throws IllegalArgumentException if {@code mean <= 0} or {@code Math.exp(-mean) == 0}.
     */
    public KempSmallMeanPoissonSampler(UniformRandomProvider rng,
                                       double mean) {
        if (mean <= 0) {
            throw new IllegalArgumentException("mean is not strictly positive: " + mean);
        }

        this.rng = rng;
        p0 = Math.exp(-mean);
        this.mean = mean;

        // Pre-compute a hedge value for the cumulative probability at approximately 50%.
        // This is only done when p0 is less than the long tail threshold.
        // The result is that the rolling probability computation should never hit the
        // long tail where p reaches zero.
        if (p0 <= LONG_TAIL_THRESHOLD) {
            // Check the edge case for no probability
            if (p0 == 0) {
                throw new IllegalArgumentException("No probability for mean " + mean);
            }

            double p = p0;
            int x = 0;
            // Sum is cumulative probability F(x) = sum p(X<=x)
            double sum = p;
            while (sum < ONE_HALF) {
                // Compute the next probability using a recurrence relation.
                // p(x+1) = p(x) * mean / (x+1)
                p *= mean / ++x;
                sum += p;
            }
            fx = sum;
            x1 = x + 1;
            px1 = p * mean / x1;
        } else {
            // Always start at zero.
            // Note: If NaN is input as the mean this path is executed and the sample is always zero.
            fx = 0;
            x1 = 0;
            px1 = p0;
        }
    }

    /** {@inheritDoc} */
    @Override
    public int sample() {
        // Note on the algorithm:
        // - X is the unknown sample deviate (the output of the algorithm)
        // - x is the current value from distribution
        // - p is the probability of the current value x, p(X=x)
        // - u is effectively the cumulative probability that the sample X
        //   is equal or above the current value x, p(X>=x)
        // So if p(X>=x) > p(X=x) the sample must be above x, otherwise it is x
        final double u = rng.nextDouble();

        if (u <= fx) {
            // Sample from the lower half of the distribution starting at zero
            return sampleBeforeLongTail(u, 0, p0);
        }

        // Sample from the upper half of the distribution starting at cumulative probability fx.
        // This is reached when u > fx and sample X > x.

        // If below the long tail threshold then omit the check on the asymptote of p -> zero
        if (u <= LONG_TAIL_THRESHOLD) {
            return sampleBeforeLongTail(u - fx, x1, px1);
        }

        return sampleWithinLongTail(u - fx, x1, px1);
    }

    /**
     * Compute the sample assuming it is <strong>not</strong> in the long tail of the distribution.
     *
     * <p>This avoids a check on the next probability value assuming that the cumulative probability
     * is at a level where the long tail of the Poisson distribution will not be reached.
     *
     * @param u the remaining cumulative probability (p(X>x))
     * @param x the current sample value X
     * @param p the current probability of the sample (p(X=x))
     * @return the sample X
     */
    private int sampleBeforeLongTail(double u, int x, double p) {
        while (u > p) {
            // Update the remaining cumulative probability
            u -= p;
            // Compute the next probability using a recurrence relation.
            // p(x+1) = p(x) * mean / (x+1)
            p *= mean / ++x;
            // The algorithm listed in Kemp (1981) does not check that the rolling probability
            // is positive (non-zero). This is omitted here on the assumption that the cumulative
            // probability will not be in the long tail where the probability asymptotes to zero.
        }
        return x;
    }

    /**
     * Compute the sample assuming it is in the long tail of the distribution.
     *
     * <p>This requires a check on the next probability value which is expected to asymptote to zero.
     *
     * @param u the remaining cumulative probability
     * @param x the current sample value X
     * @param p the current probability of the sample (p(X=x))
     * @return the sample X
     */
    private int sampleWithinLongTail(double u, int x, double p) {
        while (u > p) {
            // Update the remaining cumulative probability
            u -= p;
            // Compute the next probability using a recurrence relation.
            // p(x+1) = p(x) * mean / (x+1)
            p *= mean / ++x;
            // The algorithm listed in Kemp (1981) does not check that the rolling probability
            // is positive. This check is added to ensure no errors when the limit of the summation
            // 1 - sum(p(x)) is above 0 due to cumulative error in floating point arithmetic when
            // in the long tail of the distribution.
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
}
