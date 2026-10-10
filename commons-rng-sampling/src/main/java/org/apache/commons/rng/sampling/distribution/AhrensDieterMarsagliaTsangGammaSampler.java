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

import java.util.function.DoubleBinaryOperator;
import java.util.function.DoubleUnaryOperator;
import org.apache.commons.rng.UniformRandomProvider;

/**
 * Sampling from the <a href="https://mathworld.wolfram.com/GammaDistribution.html">gamma distribution</a>.
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
 *   <li>{@link UniformRandomProvider#nextDouble()} (both algorithms)</li>
 *   <li>{@link UniformRandomProvider#nextLong()} (only for {@code alpha >= 1})</li>
 * </ul>
 *
 * @see <a href="https://mathworld.wolfram.com/GammaDistribution.html">MathWorld Gamma distribution</a>
 * @see <a href="https://en.wikipedia.org/wiki/Gamma_distribution">Wikipedia Gamma distribution</a>
 * @since 1.0
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
         * Constructs a new instance.
         *
         * @param rng Generator of uniformly distributed random numbers.
         * @param alpha Alpha parameter of the distribution.
         * @param theta Theta parameter of the distribution.
         * @throws IllegalArgumentException if {@code alpha <= 0} or {@code theta <= 0},
         * or if {@code alpha} or {@code theta} are not finite
         */
        BaseGammaSampler(UniformRandomProvider rng,
                         double alpha,
                         double theta) {
            // Validation before java.lang.Object constructor exits prevents partially initialized object
            this(InternalUtils.requireStrictlyPositiveFinite(alpha, "alpha"),
                 InternalUtils.requireStrictlyPositiveFinite(theta, "theta"),
                 rng);
        }

        /**
         * Constructs a new instance.
         *
         * @param alpha Alpha parameter of the distribution.
         * @param theta Theta parameter of the distribution.
         * @param rng Generator of uniformly distributed random numbers.
         */
        private BaseGammaSampler(double alpha,
                                 double theta,
                                 UniformRandomProvider rng) {
            this.rng = rng;
            this.alpha = alpha;
            this.theta = theta;
        }

        /**
         * Constructs a new instance.
         *
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
    private static final class AhrensDieterGammaSampler
        extends BaseGammaSampler {

        /** Inverse of "alpha". */
        private final double oneOverAlpha;
        /** Optimization (see code). */
        private final double bGSOptim;
        /** Output mapping function for step 2. Accepts u and x. */
        private final DoubleBinaryOperator out2;
        /** Output mapping function for step 3. Accepts x. */
        private final DoubleUnaryOperator out3;

        /**
         * Constructs a new instance.
         *
         * @param rng Generator of uniformly distributed random numbers.
         * @param alpha Alpha parameter of the distribution.
         * @param theta Theta parameter of the distribution.
         * @param logSamples Return the natural logarithm of the sample.
         * @throws IllegalArgumentException if {@code alpha <= 0} or {@code theta <= 0},
         * or if {@code alpha} or {@code theta} are not finite
         */
        AhrensDieterGammaSampler(UniformRandomProvider rng,
                                 double alpha,
                                 double theta,
                                 boolean logSamples) {
            super(rng, alpha, theta);
            oneOverAlpha = 1 / alpha;
            bGSOptim = 1 + alpha / Math.E;
            if (logSamples) {
                final double logTheta = Math.log(theta);
                // RNG-195: Support log gamma samples for the DirichletSampler
                // x      = ((1 + alpha / Math.E) * u)^1/alpha
                // log(x) = log((1 + alpha / Math.E) * u) * 1/alpha
                //        = (log(1 + alpha / Math.E) + log(u)) * 1/alpha
                // alpha < 2^-106 the log1p term (~2^-107) cannot be added to log(u).
                final double logBSGOptim = Math.log1p(alpha / Math.E);
                out2 = (u, x) -> logTheta + (logBSGOptim + Math.log(u)) * oneOverAlpha;
                out3 = x -> logTheta + Math.log(x);
            } else {
                // RNG-195: Support for the existing gamma sampler behaviour is unchanged
                out2 = (u, x) -> theta * x;
                out3 = x -> theta * x;
            }
        }

        /**
         * Constructs a new instance.
         *
         * @param rng Generator of uniformly distributed random numbers.
         * @param source Source to copy.
         */
        AhrensDieterGammaSampler(UniformRandomProvider rng,
                                 AhrensDieterGammaSampler source) {
            super(rng, source);
            oneOverAlpha = source.oneOverAlpha;
            bGSOptim = source.bGSOptim;
            out2 = source.out2;
            out3 = source.out3;
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
                    // Always taken when alpha < 2^-52 since 1 + alpha / E = 1.
                    // RNG-195: Support for the existing gamma sampler behaviour is unchanged.
                    // This does not use exp(log(p) * 1/alpha) or log1p variants to recycle
                    // the log term. Output can differ by a large relative error.
                    // In the main use case for real samples using pow is faster than exp+log.
                    final double x = Math.pow(p, oneOverAlpha);
                    final double u2 = rng.nextDouble();

                    if (u2 > Math.exp(-x)) {
                        // Reject.
                        continue;
                    }
                    return out2.applyAsDouble(u, x);
                }

                // Step 3:
                // -log(((1+alpha/E) - p) / alpha)
                // -log((1+alpha/E) - p) + log(alpha)
                // At limit alpha ~ 1
                // max(1 + alpha / E) = 1.368, p > 1
                // (1 + alpha / Math.E) - p < 0.368 and -> 0 as alpha reduces.
                // No use for log1p.
                // alpha ~ 1 sample range:
                // -log(2^-52) = 36.04
                // -log(1 / E) = 1
                final double x = -Math.log((bGSOptim - p) * oneOverAlpha);
                final double u2 = rng.nextDouble();

                if (u2 <= Math.pow(x, alpha - 1)) {
                    return out3.applyAsDouble(x);
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
    private static final class MarsagliaTsangGammaSampler
        extends BaseGammaSampler {

        /** 1/3. */
        private static final double ONE_THIRD = 1d / 3;

        /** Optimization (see code). */
        private final double dOptim;
        /** Optimization (see code). */
        private final double cOptim;
        /** Gaussian sampling. */
        private final NormalizedGaussianSampler gaussian;
        /** Output mapping function for step 1. Accepts v and Gaussian sample x. */
        private final DoubleBinaryOperator out1;
        /** Output mapping function for step 2. Accepts v and log(v). */
        private final DoubleBinaryOperator out2;

        /**
         * Constructs a new instance.
         *
         * @param rng Generator of uniformly distributed random numbers.
         * @param alpha Alpha parameter of the distribution.
         * @param theta Theta parameter of the distribution.
         * @param logSamples Return the natural logarithm of the sample.
         * @throws IllegalArgumentException if {@code alpha <= 0} or {@code theta <= 0},
         * or if {@code alpha} or {@code theta} are not finite
         */
        MarsagliaTsangGammaSampler(UniformRandomProvider rng,
                                   double alpha,
                                   double theta,
                                   boolean logSamples) {
            super(rng, alpha, theta);
            gaussian = ZigguratSampler.NormalizedGaussian.of(rng);
            // Note: As alpha -> large:
            // dOptim -> alpha
            // cOptim -> 0
            // samples -> alpha
            dOptim = alpha - ONE_THIRD;
            cOptim = ONE_THIRD / Math.sqrt(dOptim);
            if (logSamples) {
                final double c = Math.log(theta) + Math.log(dOptim);
                out1 = (v, x) -> c + 3 * Math.log1p(cOptim * x);
                out2 = (v, lv) -> c + lv;
            } else {
                // RNG-195: Support for the existing gamma sampler behaviour is unchanged
                final double c = theta * dOptim;
                out1 = (v, x) -> c * v;
                out2 = (v, lv) -> c * v;
            }
        }

        /**
         * Constructs a new instance.
         *
         * @param rng Generator of uniformly distributed random numbers.
         * @param source Source to copy.
         */
        MarsagliaTsangGammaSampler(UniformRandomProvider rng,
                                   MarsagliaTsangGammaSampler source) {
            super(rng, source);
            gaussian = ZigguratSampler.NormalizedGaussian.of(rng);
            dOptim = source.dOptim;
            cOptim = source.cOptim;
            out1 = source.out1;
            out2 = source.out2;
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
                    return out1.applyAsDouble(v, x);
                }

                // Support for large alpha where 1 + cOptim * x is 1.
                // log(v) = log((1 + cOptim * x)^3)
                // Ensures log output is consistent on both output pathways.
                final double lv = 3 * Math.log1p(cOptim * x);
                if (Math.log(u) < 0.5 * x2 + dOptim * (1 - v + lv)) {
                    return out2.applyAsDouble(v, lv);
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
     * @throws IllegalArgumentException if {@code alpha <= 0} or {@code theta <= 0},
     * or if {@code alpha} or {@code theta} are not finite
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

    /**
     * {@inheritDoc}
     *
     * @since 1.3
     */
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
     * @throws IllegalArgumentException if {@code alpha <= 0} or {@code theta <= 0},
     * or if {@code alpha} or {@code theta} are not finite
     * @since 1.3
     */
    public static SharedStateContinuousSampler of(UniformRandomProvider rng,
                                                  double alpha,
                                                  double theta) {
        return of(rng, alpha, theta, false);
    }

    /**
     * Creates a new gamma distribution sampler.
     *
     * <p><Strong>This method is intentionally not part of the public API</strong>.
     *
     * @param rng Generator of uniformly distributed random numbers.
     * @param alpha Alpha parameter of the distribution (this is a shape parameter).
     * @param theta Theta parameter of the distribution (this is a scale parameter).
     * @param logSamples Return samples using the natural logarithm of the sample.
     * @return the sampler
     * @throws IllegalArgumentException if {@code alpha <= 0} or {@code theta <= 0},
     * or if {@code alpha} or {@code theta} are not finite
     * @since 1.8
     */
    // package-private for use in the DirichletSampler
    static SharedStateContinuousSampler of(UniformRandomProvider rng,
                                           double alpha,
                                           double theta,
                                           boolean logSamples) {
        // Each sampler should check the input arguments.
        return alpha < 1 ?
                new AhrensDieterGammaSampler(rng, alpha, theta, logSamples) :
                new MarsagliaTsangGammaSampler(rng, alpha, theta, logSamples);
    }
}
