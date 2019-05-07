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
 * Sampler for the <a href="https://en.wikipedia.org/wiki/Binomial_distribution">Binomial
 * distribution</a> using an optimised look-up table.
 *
 * <ul>
 *  <li>
 *   A Binomial process is simulated using pre-tabulated probabilities, as
 *   described in George Marsaglia, Wai Wan Tsang, Jingbo Wang (2004) Fast Generation of
 *   Discrete Random Variables. Journal of Statistical Software. Vol. 11, Issue. 3, pp. 1-11.
 *  </li>
 * </ul>
 *
 * <p>The sampler will fail on construction if the distribution cannot be computed. This
 * occurs when {@code trials} is large and probability of success is close to {@code 0.5}.
 * The exact failure condition is:</p>
 *
 * <pre>
 * {@code Math.exp(trials * Math.log(Math.min(p, 1 - p))) < Double.MIN_VALUE}
 * </pre>
 *
 * <p>In this case the distribution can be approximated using a limiting distributions
 * of either a Poisson or a Normal distribution as appropriate.</p>
 *
 * <p>Note: The algorithm ignores any observation where for a sample size of
 * 2<sup>31</sup> the expected number of occurrences is {@code < 0.5}.</p>
 *
 * <p>Sampling uses 1 call to {@link UniformRandomProvider#nextInt()}. Storage
 * requirements depend on the probabilities and are capped at 2<sup>17</sup> bytes, or 131
 * kB.</p>
 *
 * @see <a href="http://dx.doi.org/10.18637/jss.v011.i03">Margsglia, et al (2004) JSS Vol.
 * 11, Issue 3</a>
 * @since 1.3
 */
public class MarsagliaTsangWangBinomialSampler implements DiscreteSampler {
    /**
     * The value 2<sup>30</sup> as an {@code int}.</p>
     */
    private static final int INT_30 = 1 << 30;
    /**
     * The value 2<sup>16</sup> as an {@code int}.</p>
     */
    private static final int INT_16 = 1 << 16;
    /**
     * The value 2<sup>31</sup> as an {@code double}.</p>
     */
    private static final double DOUBLE_31 = 1L << 31;

    /** The delegate. */
    private final DiscreteSampler delegate;

    /**
     * Return a fixed result for the Binomial distribution.
     */
    private static class FixedResultDiscreteSampler implements DiscreteSampler {
        /** The result. */
        private final int result;

        /**
         * @param result Result.
         */
        FixedResultDiscreteSampler(int result) {
            this.result = result;
        }

        @Override
        public int sample() {
            return result;
        }

        @Override
        public String toString() {
            return "Binomial deviate";
        }
    }

    /**
     * Return an inversion result for the Binomial distribution. This assumes the
     * following:
     *
     * <pre>
     * Binomial(n, p) = 1 - Binomial(n, 1 - p)
     * </pre>
     */
    private static class InversionBinomialDiscreteSampler implements DiscreteSampler {
        /** The number of trials. */
        private final int trials;
        /** The Binomial distribution sampler. */
        private final DiscreteSampler sampler;

        /**
         * @param trials Number of trials.
         * @param sampler Binomial distribution sampler.
         */
        InversionBinomialDiscreteSampler(int trials, DiscreteSampler sampler) {
            this.trials = trials;
            this.sampler = sampler;
        }

        @Override
        public int sample() {
            return trials - sampler.sample();
        }

        @Override
        public String toString() {
            return sampler.toString();
        }
    }

    /**
     * Create a new instance.
     *
     * @param rng Generator of uniformly distributed random numbers.
     * @param trials Number of trials.
     * @param p Probability of success.
     * @throws IllegalArgumentException if {@code trials < 0} or {@code trials >= 2^16},
     * {@code p} is not in the range {@code [0-1]}, or the probability distribution cannot
     * be computed.
     */
    public MarsagliaTsangWangBinomialSampler(UniformRandomProvider rng, int trials, double p) {
        if (trials < 0) {
            throw new IllegalArgumentException("Trials is not positive: " + trials);
        }
        if (p < 0 || p > 1) {
            throw new IllegalArgumentException("Probability is not in range [0,1]: " + p);
        }

        // Handle edge cases
        if (p == 0) {
            delegate = new FixedResultDiscreteSampler(0);
            return;
        }
        if (p == 1) {
            delegate = new FixedResultDiscreteSampler(trials);
            return;
        }

        // A simple check using the supported index size.
        if (trials >= INT_16) {
            throw new IllegalArgumentException("Unsupported number of trials: " + trials);
        }

        // The maximum supported value for Math.exp is approximately -744.
        // This occurs when trials is large and p is close to 1.
        // Handle this by using an inversion: generate j=Binomial(n,1-p), return n-j
        final boolean inversion = p > 0.5;
        if (inversion) {
            p = 1 - p;
        }

        // Check if the distribution can be computed
        final double p0 = Math.exp(trials * Math.log(1 - p));
        if (p0 < Double.MIN_VALUE) {
            throw new IllegalArgumentException("Unable to compute distribution");
        }

        // First find size of probability array
        double t = p0;
        final double h = p / (1 - p);
        // Find first probability
        int begin = 0;
        if (t * DOUBLE_31 < 1) {
            // Somewhere after p(0)
            // Note:
            // If this loop is entered p(0) is < 2^-31.
            // This has been tested at the extreme for p(0)=Double.MIN_VALUE and either
            // p=0.5 or trials=2^16-1 and does not fail to find the beginning.
            for (int i = 1; i <= trials; i++) {
                t *= (trials + 1 - i) * h / i;
                if (t * DOUBLE_31 >= 1) {
                    begin = i;
                    break;
                }
            }
        }
        // Find last probability
        int end = trials;
        for (int i = begin + 1; i <= trials; i++) {
            t *= (trials + 1 - i) * h / i;
            if (t * DOUBLE_31 < 1) {
                end = i - 1;
                break;
            }
        }
        final int size = end - begin + 1;
        final int offset = begin;

        // Then assign probability values as 30-bit integers
        final int[] prob = new int[size];
        t = p0;
        for (int i = 1; i <= begin; i++) {
            t *= (trials + 1 - i) * h / i;
        }
        int sum = toUnsignedInt30(t);
        prob[0] = sum;
        for (int i = begin + 1; i <= end; i++) {
            t *= (trials + 1 - i) * h / i;
            prob[i - begin] = toUnsignedInt30(t);
            sum += prob[i - begin];
        }

        // If the sum is < 2^30 add the remaining sum to the mode (floor((n+1)p))).
        final int mode = (int) ((trials + 1) * p) - offset;
        prob[mode] += Math.max(0, INT_30 - sum);

        final MarsagliaTsangWangDiscreteSampler sampler = new MarsagliaTsangWangDiscreteSampler(rng, prob, offset);

        if (inversion) {
            delegate = new InversionBinomialDiscreteSampler(trials, sampler);
        } else {
            delegate = sampler;
        }
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
        return "Binomial " + delegate.toString();
    }
}
