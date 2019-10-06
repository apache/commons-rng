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
 * Sampling from a <a href="http://en.wikipedia.org/wiki/Beta_distribution">beta distribution</a>.
 *
 * <p>Uses Cheng's algorithms for beta distribution sampling:</p>
 *
 * <blockquote>
 * <pre>
 * R. C. H. Cheng,
 * "Generating beta variates with nonintegral shape parameters",
 * Communications of the ACM, 21, 317-322, 1978.
 * </pre>
 * </blockquote>
 *
 * <p>Sampling uses {@link UniformRandomProvider#nextDouble()}.</p>
 *
 * @since 1.0
 */
public class ChengBetaSampler
    extends SamplerBase
    implements SharedStateContinuousSampler {
    /** The appropriate beta sampler for the parameters. */
    private final SharedStateContinuousSampler delegate;

    /**
     * Base class to implement Cheng's algorithms for the beta distribution.
     */
    private abstract static class BaseChengBetaSampler
            implements SharedStateContinuousSampler {
        /**
         * Flag set to true if {@code a} is the beta distribution alpha shape parameter.
         * Otherwise {@code a} is the beta distribution beta shape parameter.
         */
        protected final boolean aIsAlphaShape;
        /**
         * First shape parameter.
         * The meaning of this is dependent on the {@code aIsAlphaShape} flag.
         */
        protected final double a;
        /**
         * Second shape parameter.
         * The meaning of this is dependent on the {@code aIsAlphaShape} flag.
         */
        protected final double b;
        /** Underlying source of randomness. */
        protected final UniformRandomProvider rng;
        /**
         * The algorithm alpha factor. This is not the beta distribution alpha shape parameter.
         * It is the sum of the two shape parameters ({@code a + b}.
         */
        protected final double alpha;
        /** The logarithm of the alpha factor. */
        protected final double logAlpha;

        /**
         * @param rng Generator of uniformly distributed random numbers.
         * @param aIsAlphaShape true if {@code a} is the beta distribution alpha shape parameter.
         * @param a Distribution first shape parameter.
         * @param b Distribution second shape parameter.
         */
        BaseChengBetaSampler(UniformRandomProvider rng, boolean aIsAlphaShape, double a, double b) {
            this.rng = rng;
            this.aIsAlphaShape = aIsAlphaShape;
            this.a = a;
            this.b = b;
            alpha = a + b;
            logAlpha = Math.log(alpha);
        }

        /**
         * @param rng Generator of uniformly distributed random numbers.
         * @param source Source to copy.
         */
        private BaseChengBetaSampler(UniformRandomProvider rng,
                                     BaseChengBetaSampler source) {
            this.rng = rng;
            aIsAlphaShape = source.aIsAlphaShape;
            a = source.a;
            b = source.b;
            // Compute algorithm factors.
            alpha = source.alpha;
            logAlpha = source.logAlpha;
        }

        /** {@inheritDoc} */
        @Override
        public String toString() {
            return "Cheng Beta deviate [" + rng.toString() + "]";
        }
    }

    /**
     * Computes one sample using Cheng's BB algorithm, when beta distribution {@code alpha} and
     * {@code beta} shape parameters are both larger than 1.
     */
    private static class ChengBBBetaSampler extends BaseChengBetaSampler {
        /** The algorithm beta factor. This is not the beta distribution beta shape parameter. */
        private final double beta;
        /** The algorithm gamma factor. */
        private final double gamma;

        /**
         * @param rng Generator of uniformly distributed random numbers.
         * @param aIsAlphaShape true if {@code a} is the beta distribution alpha shape parameter.
         * @param a min(alpha, beta) shape parameter.
         * @param b max(alpha, beta) shape parameter.
         */
        ChengBBBetaSampler(UniformRandomProvider rng, boolean aIsAlphaShape, double a, double b) {
            super(rng, aIsAlphaShape, a, b);
            beta = Math.sqrt((alpha - 2) / (2 * a * b - alpha));
            gamma = a + 1 / beta;
        }

        /**
         * @param rng Generator of uniformly distributed random numbers.
         * @param source Source to copy.
         */
        private ChengBBBetaSampler(UniformRandomProvider rng,
                                   ChengBBBetaSampler source) {
            super(rng, source);
            // Compute algorithm factors.
            beta = source.beta;
            gamma = source.gamma;
        }

        @Override
        public double sample() {
            double r;
            double w;
            double t;
            do {
                final double u1 = rng.nextDouble();
                final double u2 = rng.nextDouble();
                final double v = beta * (Math.log(u1) - Math.log1p(-u1));
                w = a * Math.exp(v);
                final double z = u1 * u1 * u2;
                r = gamma * v - 1.3862944;
                final double s = a + r - w;
                if (s + 2.609438 >= 5 * z) {
                    break;
                }

                t = Math.log(z);
                if (s >= t) {
                    break;
                }
            } while (r + alpha * (logAlpha - Math.log(b + w)) < t);

            w = Math.min(w, Double.MAX_VALUE);

            return aIsAlphaShape ? w / (b + w) : b / (b + w);
        }

        @Override
        public SharedStateContinuousSampler withUniformRandomProvider(UniformRandomProvider rng) {
            return new ChengBBBetaSampler(rng, this);
        }
    }

    /**
     * Computes one sample using Cheng's BC algorithm, when at least one of beta distribution
     * {@code alpha} or {@code beta} shape parameters is smaller than 1.
     */
    private static class ChengBCBetaSampler extends BaseChengBetaSampler {
        /** 1/2. */
        private static final double ONE_HALF = 1d / 2;
        /** 1/4. */
        private static final double ONE_QUARTER = 1d / 4;

        /** The algorithm beta factor. This is not the beta distribution beta shape parameter. */
        private final double beta;
        /** The algorithm delta factor. */
        private final double delta;
        /** The algorithm k1 factor. */
        private final double k1;
        /** The algorithm k2 factor. */
        private final double k2;

        /**
         * @param rng Generator of uniformly distributed random numbers.
         * @param aIsAlphaShape true if {@code a} is the beta distribution alpha shape parameter.
         * @param a max(alpha, beta) shape parameter.
         * @param b min(alpha, beta) shape parameter.
         */
        ChengBCBetaSampler(UniformRandomProvider rng, boolean aIsAlphaShape, double a, double b) {
            super(rng, aIsAlphaShape, a, b);
            // Compute algorithm factors.
            beta = 1 / b;
            delta = 1 + a - b;
            k1 = delta * (0.0138889 + 0.0416667 * b) / (a * beta - 0.777778);
            k2 = 0.25 + (0.5 + 0.25 / delta) * b;
        }

        /**
         * @param rng Generator of uniformly distributed random numbers.
         * @param source Source to copy.
         */
        private ChengBCBetaSampler(UniformRandomProvider rng,
                                   ChengBCBetaSampler source) {
            super(rng, source);
            beta = source.beta;
            delta = source.delta;
            k1 = source.k1;
            k2 = source.k2;
        }

        @Override
        public double sample() {
            double w;
            while (true) {
                final double u1 = rng.nextDouble();
                final double u2 = rng.nextDouble();
                final double y = u1 * u2;
                final double z = u1 * y;
                if (u1 < ONE_HALF) {
                    if (0.25 * u2 + z - y >= k1) {
                        continue;
                    }
                } else {
                    if (z <= ONE_QUARTER) {
                        final double v = beta * (Math.log(u1) - Math.log1p(-u1));
                        w = a * Math.exp(v);
                        break;
                    }

                    if (z >= k2) {
                        continue;
                    }
                }

                final double v = beta * (Math.log(u1) - Math.log1p(-u1));
                w = a * Math.exp(v);
                if (alpha * (logAlpha - Math.log(b + w) + v) - 1.3862944 >= Math.log(z)) {
                    break;
                }
            }

            w = Math.min(w, Double.MAX_VALUE);

            return aIsAlphaShape ? w / (b + w) : b / (b + w);
        }

        @Override
        public SharedStateContinuousSampler withUniformRandomProvider(UniformRandomProvider rng) {
            return new ChengBCBetaSampler(rng, this);
        }
    }

    /**
     * Creates a sampler instance.
     *
     * @param rng Generator of uniformly distributed random numbers.
     * @param alpha Distribution first shape parameter.
     * @param beta Distribution second shape parameter.
     * @throws IllegalArgumentException if {@code alpha <= 0} or {@code beta <= 0}
     */
    public ChengBetaSampler(UniformRandomProvider rng,
                            double alpha,
                            double beta) {
        super(null);
        delegate = of(rng, alpha, beta);
    }

    /** {@inheritDoc} */
    @Override
    public double sample() {
        return delegate.sample();
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return delegate.toString();
    }

    /** {@inheritDoc} */
    @Override
    public SharedStateContinuousSampler withUniformRandomProvider(UniformRandomProvider rng) {
        return delegate.withUniformRandomProvider(rng);
    }

    /**
     * Creates a new beta distribution sampler.
     *
     * @param rng Generator of uniformly distributed random numbers.
     * @param alpha Distribution first shape parameter.
     * @param beta Distribution second shape parameter.
     * @return the sampler
     * @throws IllegalArgumentException if {@code alpha <= 0} or {@code beta <= 0}
     */
    public static SharedStateContinuousSampler of(UniformRandomProvider rng,
                                                  double alpha,
                                                  double beta) {
        if (alpha <= 0) {
            throw new IllegalArgumentException("alpha is not strictly positive: " + alpha);
        }
        if (beta <= 0) {
            throw new IllegalArgumentException("beta is not strictly positive: " + beta);
        }

        // Choose the algorithm.
        final double a = Math.min(alpha, beta);
        final double b = Math.max(alpha, beta);
        final boolean aIsAlphaShape = a == alpha;

        return a > 1 ?
            // BB algorithm
            new ChengBBBetaSampler(rng, aIsAlphaShape, a, b) :
            // The BC algorithm is deliberately invoked with reversed parameters
            // as the argument order is: max(alpha,beta), min(alpha,beta).
            // Also invert the 'a is alpha' flag.
            new ChengBCBetaSampler(rng, !aIsAlphaShape, b, a);
    }
}
