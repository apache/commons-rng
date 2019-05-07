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
 * distribution</a> using an optimised look-up table.
 *
 * <ul>
 *  <li>
 *   A Poisson process is simulated using pre-tabulated probabilities, as described
 *   in George Marsaglia, Wai Wan Tsang, Jingbo Wang (2004) Fast Generation of Discrete
 *   Random Variables. Journal of Statistical Software. Vol. 11, Issue. 3, pp. 1-11.
 *  </li>
 * </ul>
 *
 * <p>This sampler is suitable for {@code mean <= 1024}. Larger means accumulate errors
 * when tabulating the Poisson probability. For large means, {@link LargeMeanPoissonSampler}
 * should be used instead.</p>
 *
 * <p>Note: The algorithm ignores any observation where for a sample size of
 * 2<sup>31</sup> the expected number of occurrences is {@code < 0.5}.</p>
 *
 * <p>Sampling uses 1 call to {@link UniformRandomProvider#nextInt()}. Storage requirements
 * depend on the tabulated probability values. Example storage requirements are listed below.</p>
 *
 * <pre>
 * mean      table size     kB
 * 0.25      882            0.88
 * 0.5       1135           1.14
 * 1         1200           1.20
 * 2         1451           1.45
 * 4         1955           1.96
 * 8         2961           2.96
 * 16        4410           4.41
 * 32        6115           6.11
 * 64        8499           8.50
 * 128       11528          11.53
 * 256       15935          31.87
 * 512       20912          41.82
 * 1024      30614          61.23
 * </pre>
 *
 * <p>Note: Storage changes to 2 bytes per index when {@code mean=256}.</p>
 *
 * @since 1.3
 * @see <a href="http://dx.doi.org/10.18637/jss.v011.i03">Margsglia, et al (2004) JSS Vol.
 * 11, Issue 3</a>
 */
public class MarsagliaTsangWangSmallMeanPoissonSampler implements DiscreteSampler {
    /**
     * The value 2<sup>30</sup> as an {@code int}.</p>
     */
    private static final int INT_30 = 1 << 30;
    /**
     * The value 2<sup>31</sup> as an {@code double}.</p>
     */
    private static final double DOUBLE_31 = 1L << 31;
    /**
     * Upper bound to avoid exceeding the table sizes.
     *
     * <p>The number of possible values of the distribution should not exceed 2^16.</p>
     *
     * <p>The original source code provided in Marsaglia, et al (2004) has no explicit
     * limit but the code fails at mean >= 1941 as the transform to compute p(x=mode)
     * produces infinity. Use a conservative limit of 1024.</p>
     */
    private static final double MAX_MEAN = 1024;

    /** The delegate. */
    private final DiscreteSampler delegate;

    /**
     * Create a new instance.
     *
     * @param rng Generator of uniformly distributed random numbers.
     * @param mean Mean.
     * @throws IllegalArgumentException if {@code mean <= 0} or {@code mean > 1024}.
     */
    public MarsagliaTsangWangSmallMeanPoissonSampler(UniformRandomProvider rng, double mean) {
        if (mean <= 0) {
            throw new IllegalArgumentException("mean is not strictly positive: " + mean);
        }
        // The algorithm is not valid if Math.floor(mean) is not an integer.
        if (mean > MAX_MEAN) {
            throw new IllegalArgumentException("mean " + mean + " > " + MAX_MEAN);
        }

        // Probabilities are 30-bit integers, assumed denominator 2^30
        int[] prob;
        // This is the minimum sample value: prob[x - offset] = p(x)
        int offset;

        // Generate P's from 0 if mean < 21.4
        if (mean < 21.4) {
            final double p0 = Math.exp(-mean);

            // Recursive update of Poisson probability until the value is too small
            // p(x + 1) = p(x) * mean / (x + 1)
            double p = p0;
            int i;
            for (i = 1; p * DOUBLE_31 >= 1; i++) {
                p *= mean / i;
            }

            // Fill P as (30-bit integers)
            offset = 0;
            final int size = i - 1;
            prob = new int[size];

            p = p0;
            prob[0] = toUnsignedInt30(p);
            // The sum must exceed 2^30. In edges cases this is false due to round-off.
            int sum = prob[0];
            for (i = 1; i < prob.length; i++) {
                p *= mean / i;
                prob[i] = toUnsignedInt30(p);
                sum += prob[i];
            }

            // If the sum is < 2^30 add the remaining sum to the mode (floor(mean)).
            prob[(int) mean] += Math.max(0, INT_30 - sum);
        } else {
            // If mean >= 21.4, generate from largest p-value up, then largest down.
            // The largest p-value will be at the mode (floor(mean)).

            // Find p(x=mode)
            final int mode = (int) mean;
            // This transform is stable until mean >= 1941 where p will result in Infinity
            // before the divisor i is large enough to start reducing the product (i.e. i > c).
            final double c = mean * Math.exp(-mean / mode);
            double p = 1.0;
            int i;
            for (i = 1; i <= mode; i++) {
                p *= c / i;
            }
            final double pX = p;
            // Note this will exit when i overflows to negative so no check on the range
            for (i = mode + 1; p * DOUBLE_31 >= 1; i++) {
                p *= mean / i;
            }
            final int last = i - 2;
            p = pX;
            int j = -1;
            for (i = mode - 1; i >= 0; i--) {
                p *= (i + 1) / mean;
                if (p * DOUBLE_31 < 1) {
                    j = i;
                    break;
                }
            }

            // Fill P as (30-bit integers)
            offset = j + 1;
            final int size = last - offset + 1;
            prob = new int[size];

            p = pX;
            prob[mode - offset] = toUnsignedInt30(p);
            // The sum must exceed 2^30. In edges cases this is false due to round-off.
            int sum = prob[mode - offset];
            for (i = mode + 1; i <= last; i++) {
                p *= mean / i;
                prob[i - offset] = toUnsignedInt30(p);
                sum += prob[i - offset];
            }
            p = pX;
            for (i = mode - 1; i >= offset; i--) {
                p *= (i + 1) / mean;
                prob[i - offset] = toUnsignedInt30(p);
                sum += prob[i - offset];
            }

            // If the sum is < 2^30 add the remaining sum to the mode
            prob[mode - offset] += Math.max(0, INT_30 - sum);
        }

        delegate = new MarsagliaTsangWangDiscreteSampler(rng, prob, offset);
    }

    /**
     * Convert the probability to an unsigned integer in the range [0,2^30].
     *
     * @param p the probability
     * @return the integer
     */
    private static int toUnsignedInt30(double p) {
        return (int) (p * INT_30 + 0.5);
    }

    /** {@inheritDoc} */
    @Override
    public int sample() {
        return delegate.sample();
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return "Small Mean Poisson " + delegate.toString();
    }
}
