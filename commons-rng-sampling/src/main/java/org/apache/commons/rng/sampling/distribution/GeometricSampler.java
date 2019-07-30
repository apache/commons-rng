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
 * Sampling from a <a href="https://en.wikipedia.org/wiki/Geometric_distribution">geometric
 * distribution</a>.
 *
 * <p>This distribution samples the number of failures before the first success taking values in the
 * set {@code [0, 1, 2, ...]}.</p>
 *
 * <p>The sample is computed using a related exponential distribution. If \( X \) is an
 * exponentially distributed random variable with parameter \( \lambda \), then
 * \( Y = \left \lfloor X \right \rfloor \) is a geometrically distributed random variable with
 * parameter \( p = 1 − e^\lambda \), with \( p \) the probability of success.</p>
 *
 * <p>This sampler outperforms using the {@link InverseTransformDiscreteSampler} with an appropriate
 * Geometric inverse cumulative probability function.</p>
 *
 * <p>Usage note: As the probability of success (\( p \)) tends towards zero the mean of the
 * distribution (\( \frac{1-p}{p} \)) tends towards infinity and due to the use of {@code int}
 * for the sample this can result in truncation of the distribution.</p>
 *
 * <p>Sampling uses {@link UniformRandomProvider#nextDouble()}.</p>
 *
 * @see <a
 * href="https://en.wikipedia.org/wiki/Geometric_distribution#Related_distributions">Geometric
 * distribution - related distributions</a>
 *
 * @since 1.3
 */
public final class GeometricSampler {
    /**
     * Sample from the geometric distribution when the probability of success is 1.
     */
    private static class GeometricP1Sampler
        implements SharedStateDiscreteSampler {
        /** The single instance. */
        static final GeometricP1Sampler INSTANCE = new GeometricP1Sampler();

        @Override
        public int sample() {
            // When probability of success is 1 the sample is always zero
            return 0;
        }

        @Override
        public String toString() {
            return "Geometric(p=1) deviate";
        }

        @Override
        public SharedStateDiscreteSampler withUniformRandomProvider(UniformRandomProvider rng) {
            // No requirement for a new instance
            return this;
        }
    }

    /**
     * Sample from the geometric distribution by using a related exponential distribution.
     */
    private static class GeometricExponentialSampler
        implements SharedStateDiscreteSampler {
        /** Underlying source of randomness. Used only for the {@link #toString()} method. */
        private final UniformRandomProvider rng;
        /** The related exponential sampler for the geometric distribution. */
        private final SharedStateContinuousSampler exponentialSampler;

        /**
         * @param rng Generator of uniformly distributed random numbers
         * @param probabilityOfSuccess The probability of success (must be in the range
         * {@code [0 < probabilityOfSuccess < 1]})
         */
        GeometricExponentialSampler(UniformRandomProvider rng, double probabilityOfSuccess) {
            this.rng = rng;
            // Use a related exponential distribution:
            // λ = −ln(1 − probabilityOfSuccess)
            // exponential mean = 1 / λ
            // --
            // Note on validation:
            // If probabilityOfSuccess == Math.nextDown(1.0) the exponential mean is >0 (valid).
            // If probabilityOfSuccess == Double.MIN_VALUE the exponential mean is +Infinity
            // and the sample will always be Integer.MAX_VALUE (the distribution is truncated). It
            // is noted in the class Javadoc that the use of a small p leads to truncation so
            // no checks are made for this case.
            final double exponentialMean = 1.0 / (-Math.log1p(-probabilityOfSuccess));
            exponentialSampler = AhrensDieterExponentialSampler.of(rng, exponentialMean);
        }

        /**
         * @param rng Generator of uniformly distributed random numbers
         * @param source Source to copy.
         */
        GeometricExponentialSampler(UniformRandomProvider rng, GeometricExponentialSampler source) {
            this.rng = rng;
            exponentialSampler = source.exponentialSampler.withUniformRandomProvider(rng);
        }

        @Override
        public int sample() {
            // Return the floor of the exponential sample
            return (int) Math.floor(exponentialSampler.sample());
        }

        @Override
        public String toString() {
            return "Geometric deviate [" + rng.toString() + "]";
        }

        @Override
        public SharedStateDiscreteSampler withUniformRandomProvider(UniformRandomProvider rng) {
            return new GeometricExponentialSampler(rng, this);
        }
    }

    /** Class contains only static methods. */
    private GeometricSampler() {}

    /**
     * Creates a new geometric distribution sampler. The samples will be provided in the set
     * {@code k=[0, 1, 2, ...]} where {@code k} indicates the number of failures before the first
     * success.
     *
     * @param rng Generator of uniformly distributed random numbers.
     * @param probabilityOfSuccess The probability of success.
     * @return the sampler
     * @throws IllegalArgumentException if {@code probabilityOfSuccess} is not in the range
     * {@code [0 < probabilityOfSuccess <= 1]})
     */
    public static SharedStateDiscreteSampler of(UniformRandomProvider rng,
                                                double probabilityOfSuccess) {
        if (probabilityOfSuccess <= 0 || probabilityOfSuccess > 1) {
            throw new IllegalArgumentException(
                "Probability of success (p) must be in the range [0 < p <= 1]: " +
                    probabilityOfSuccess);
        }
        return probabilityOfSuccess == 1 ?
            GeometricP1Sampler.INSTANCE :
            new GeometricExponentialSampler(rng, probabilityOfSuccess);
    }
}
