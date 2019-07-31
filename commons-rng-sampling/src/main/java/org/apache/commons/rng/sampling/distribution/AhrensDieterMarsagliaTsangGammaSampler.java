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
 * Sampling from the <a href="http://mathworld.wolfram.com/GammaDistribution.html">gamma distribution</a>.
 * <ul>
 *  <li>
 *   For {@code 0 < alpha < 1}:
 *   <blockquote>
 *    Ahrens, J. H. and Dieter, U.,
 *    <i>Computer methods for sampling from gamma, beta, Poisson and binomial distributions,</i>
 *    Computing, 12, 223-246, 1974.
 *   </blockquote>
 *  </li>
 *  <li>
 *  For {@code alpha >= 1}:
 *   <blockquote>
 *   Marsaglia and Tsang, <i>A Simple Method for Generating
 *   Gamma Variables.</i> ACM Transactions on Mathematical Software,
 *   Volume 26 Issue 3, September, 2000.
 *   </blockquote>
 *  </li>
 * </ul>
 *
 * <p>Sampling uses:</p>
 *
 * <ul>
 *   <li>{@link UniformRandomProvider#nextDouble()} (both algorithms)
 *   <li>{@link UniformRandomProvider#nextLong()} (only for {@code alpha >= 1})
 * </ul>
 *
 * @since 1.0
 * @see <a href="http://mathworld.wolfram.com/GammaDistribution.html">MathWorld Gamma distribution</a>
 * @see <a href="https://en.wikipedia.org/wiki/Gamma_distribution">Wikipedia Gamma distribution</a>
 */
public class AhrensDieterMarsagliaTsangGammaSampler
    extends SamplerBase
    implements SharedStateContinuousSampler {
    /** The appropriate gamma sampler for the parameters. */
    private final SharedStateContinuousSampler delegate;

    /**
     * Base class for a sampler from the Gamma distribution.
     */
    private abstract static class BaseGammaSampler
        implements SharedStateContinuousSampler {

        /** Underlying source of randomness. */
        protected final UniformRandomProvider rng;
        /** The alpha parameter. This is a shape parameter. */
        protected final double alpha;
        /** The theta parameter. This is a scale parameter. */
        protected final double theta;

        /**
         * @param rng Generator of uniformly distributed random numbers.
         * @param alpha Alpha parameter of the distribution.
         * @param theta Theta parameter of the distribution.
         * @throws IllegalArgumentException if {@code alpha <= 0} or {@code theta <= 0}
         */
        BaseGammaSampler(UniformRandomProvider rng,
                         double alpha,
                         double theta) {
            if (alpha <= 0) {
                throw new IllegalArgumentException("alpha is not strictly positive: " + alpha);
            }
            if (theta <= 0) {
                throw new IllegalArgumentException("theta is not strictly positive: " + theta);
            }
            this.rng = rng;
            this.alpha = alpha;
            this.theta = theta;
        }

        /**
         * @param rng Generator of uniformly distributed random numbers.
         * @param source Source to copy.
         */
        BaseGammaSampler(UniformRandomProvider rng,
                         BaseGammaSampler source) {
            this.rng = rng;
            this.alpha = source.alpha;
            this.theta = source.theta;
        }

        /** {@inheritDoc} */
        @Override
        public String toString() {
            return "Ahrens-Dieter-Marsaglia-Tsang Gamma deviate [" + rng.toString() + "]";
        }
    }

    /**
     * Class to sample from the Gamma distribution when {@code 0 < alpha < 1}.
     *
     * <blockquote>
     *  Ahrens, J. H. and Dieter, U.,
     *  <i>Computer methods for sampling from gamma, beta, Poisson and binomial distributions,</i>
     *  Computing, 12, 223-246, 1974.
     * </blockquote>
     */
    private static class AhrensDieterGammaSampler
        extends BaseGammaSampler {

        /** Inverse of "alpha". */
        private final double oneOverAlpha;
        /** Optimization (see code). */
        private final double bGSOptim;

        /**
         * @param rng Generator of uniformly distributed random numbers.
         * @param alpha Alpha parameter of the distribution.
         * @param theta Theta parameter of the distribution.
         * @throws IllegalArgumentException if {@code alpha <= 0} or {@code theta <= 0}
         */
        AhrensDieterGammaSampler(UniformRandomProvider rng,
                                 double alpha,
                                 double theta) {
            super(rng, alpha, theta);
            oneOverAlpha = 1 / alpha;
            bGSOptim = 1 + alpha / Math.E;
        }

        /**
         * @param rng Generator of uniformly distributed random numbers.
         * @param source Source to copy.
         */
        AhrensDieterGammaSampler(UniformRandomProvider rng,
                                 AhrensDieterGammaSampler source) {
            super(rng, source);
            oneOverAlpha = source.oneOverAlpha;
            bGSOptim = source.bGSOptim;
        }

        @Override
        public double sample() {
            // [1]: p. 228, Algorithm GS.

            while (true) {
                // Step 1:
                final double u = rng.nextDouble();
                final double p = bGSOptim * u;

                if (p <= 1) {
                    // Step 2:
                    final double x = Math.pow(p, oneOverAlpha);
                    final double u2 = rng.nextDouble();

                    if (u2 > Math.exp(-x)) {
                        // Reject.
                        continue;
                    }
                    return theta * x;
                }

                // Step 3:
                final double x = -Math.log((bGSOptim - p) * oneOverAlpha);
                final double u2 = rng.nextDouble();

                if (u2 <= Math.pow(x, alpha - 1)) {
                    return theta * x;
                }
                // Reject and continue.
            }
        }

        @Override
        public SharedStateContinuousSampler withUniformRandomProvider(UniformRandomProvider rng) {
            return new AhrensDieterGammaSampler(rng, this);
        }
    }

    /**
     * Class to sample from the Gamma distribution when the {@code alpha >= 1}.
     *
     * <blockquote>
     *  Marsaglia and Tsang, <i>A Simple Method for Generating
     *  Gamma Variables.</i> ACM Transactions on Mathematical Software,
     *  Volume 26 Issue 3, September, 2000.
     * </blockquote>
     */
    private static class MarsagliaTsangGammaSampler
        extends BaseGammaSampler {

        /** 1/3. */
        private static final double ONE_THIRD = 1d / 3;

        /** Optimization (see code). */
        private final double dOptim;
        /** Optimization (see code). */
        private final double cOptim;
        /** Gaussian sampling. */
        private final NormalizedGaussianSampler gaussian;

        /**
         * @param rng Generator of uniformly distributed random numbers.
         * @param alpha Alpha parameter of the distribution.
         * @param theta Theta parameter of the distribution.
         * @throws IllegalArgumentException if {@code alpha <= 0} or {@code theta <= 0}
         */
        MarsagliaTsangGammaSampler(UniformRandomProvider rng,
                                   double alpha,
                                   double theta) {
            super(rng, alpha, theta);
            gaussian = new ZigguratNormalizedGaussianSampler(rng);
            dOptim = alpha - ONE_THIRD;
            cOptim = ONE_THIRD / Math.sqrt(dOptim);
        }

        /**
         * @param rng Generator of uniformly distributed random numbers.
         * @param source Source to copy.
         */
        MarsagliaTsangGammaSampler(UniformRandomProvider rng,
                                   MarsagliaTsangGammaSampler source) {
            super(rng, source);
            gaussian = new ZigguratNormalizedGaussianSampler(rng);
            dOptim = source.dOptim;
            cOptim = source.cOptim;
        }

        @Override
        public double sample() {
            while (true) {
                final double x = gaussian.sample();
                final double oPcTx = 1 + cOptim * x;
                final double v = oPcTx * oPcTx * oPcTx;

                if (v <= 0) {
                    continue;
                }

                final double x2 = x * x;
                final double u = rng.nextDouble();

                // Squeeze.
                if (u < 1 - 0.0331 * x2 * x2) {
                    return theta * dOptim * v;
                }

                if (Math.log(u) < 0.5 * x2 + dOptim * (1 - v + Math.log(v))) {
                    return theta * dOptim * v;
                }
            }
        }

        @Override
        public SharedStateContinuousSampler withUniformRandomProvider(UniformRandomProvider rng) {
            return new MarsagliaTsangGammaSampler(rng, this);
        }
    }

    /**
     * This instance delegates sampling. Use the factory method
     * {@link #of(UniformRandomProvider, double, double)} to create an optimal sampler.
     *
     * @param rng Generator of uniformly distributed random numbers.
     * @param alpha Alpha parameter of the distribution (this is a shape parameter).
     * @param theta Theta parameter of the distribution (this is a scale parameter).
     * @throws IllegalArgumentException if {@code alpha <= 0} or {@code theta <= 0}
     */
    public AhrensDieterMarsagliaTsangGammaSampler(UniformRandomProvider rng,
                                                  double alpha,
                                                  double theta) {
        super(null);
        delegate = of(rng, alpha, theta);
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
        // Direct return of the optimised sampler
        return delegate.withUniformRandomProvider(rng);
    }

    /**
     * Creates a new gamma distribution sampler.
     *
     * @param rng Generator of uniformly distributed random numbers.
     * @param alpha Alpha parameter of the distribution (this is a shape parameter).
     * @param theta Theta parameter of the distribution (this is a scale parameter).
     * @return the sampler
     * @throws IllegalArgumentException if {@code alpha <= 0} or {@code theta <= 0}
     */
    public static SharedStateContinuousSampler of(UniformRandomProvider rng,
                                                  double alpha,
                                                  double theta) {
        // Each sampler should check the input arguments.
        return alpha < 1 ?
                new AhrensDieterGammaSampler(rng, alpha, theta) :
                new MarsagliaTsangGammaSampler(rng, alpha, theta);
    }
}
