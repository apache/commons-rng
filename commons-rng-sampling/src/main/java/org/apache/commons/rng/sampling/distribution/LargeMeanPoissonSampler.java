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
import org.apache.commons.rng.sampling.distribution.InternalUtils.FactorialLog;

/**
 * Sampler for the <a href="http://mathworld.wolfram.com/PoissonDistribution.html">Poisson distribution</a>.
 *
 * <ul>
 *  <li>
 *   For large means, we use the rejection algorithm described in
 *   <blockquote>
 *    Devroye, Luc. (1981).<i>The Computer Generation of Poisson Random Variables</i><br>
 *    <strong>Computing</strong> vol. 26 pp. 197-207.
 *   </blockquote>
 *  </li>
 * </ul>
 *
 * This sampler is suitable for {@code mean >= 40}.
 */
public class LargeMeanPoissonSampler
    extends SamplerBase
    implements DiscreteSampler {

    /** Class to compute {@code log(n!)}. This has no cached values. */
    private static final InternalUtils.FactorialLog NO_CACHE_FACTORIAL_LOG;

    static {
        // Create without a cache.
        NO_CACHE_FACTORIAL_LOG = FactorialLog.create();
    }

    /** Exponential. */
    private final ContinuousSampler exponential;
    /** Gaussian. */
    private final ContinuousSampler gaussian;
    /** Local class to compute {@code log(n!)}. This may have cached values. */
    private final InternalUtils.FactorialLog factorialLog;

    // Working values

    /** Algorithm constant: {@code Math.floor(mean)}. */
    private final double lambda;
    /** Algorithm constant: {@code mean - lambda}. */
    private final double lambdaFractional;
    /** Algorithm constant: {@code Math.log(lambda)}. */
    private final double logLambda;
    /** Algorithm constant: {@code factorialLog((int) lambda)}. */
    private final double logLambdaFactorial;
    /** Algorithm constant: {@code Math.sqrt(lambda * Math.log(32 * lambda / Math.PI + 1))}. */
    private final double delta;
    /** Algorithm constant: {@code delta / 2}. */
    private final double halfDelta;
    /** Algorithm constant: {@code 2 * lambda + delta}. */
    private final double twolpd;
    /**
     * Algorithm constant: {@code a1 / aSum} with
     * <ul>
     *  <li>{@code a1 = Math.sqrt(Math.PI * twolpd) * Math.exp(c1)}</li>
     *  <li>{@code aSum = a1 + a2 + 1}</li>
     * </ul>
     */
    private final double p1;
    /**
     * Algorithm constant: {@code a2 / aSum} with
     * <ul>
     *  <li>{@code a2 = (twolpd / delta) * Math.exp(-delta * (1 + delta) / twolpd)}</li>
     *  <li>{@code aSum = a1 + a2 + 1}</li>
     * </ul>
     */
    private final double p2;
    /** Algorithm constant: {@code 1 / (8 * lambda)}. */
    private final double c1;

    /** The internal Poisson sampler for the lambda fraction. */
    private final DiscreteSampler smallMeanPoissonSampler;

    /**
     * Encapsulate the state of the sampler. The state is valid for construction of
     * a sampler in the range {@code lambda <= mean < lambda+1}.
     */
    static class LargeMeanPoissonSamplerState {
        /** Algorithm constant: {@code Math.floor(mean)}. */
        private final double lambda;
        /** Algorithm constant: {@code Math.log(lambda)}. */
        private final double logLambda;
        /** Algorithm constant: {@code factorialLog((int) lambda)}. */
        private final double logLambdaFactorial;
        /** Algorithm constant: {@code Math.sqrt(lambda * Math.log(32 * lambda / Math.PI + 1))}. */
        private final double delta;
        /** Algorithm constant: {@code delta / 2}. */
        private final double halfDelta;
        /** Algorithm constant: {@code 2 * lambda + delta}. */
        private final double twolpd;
        /**
         * Algorithm constant: {@code a1 / aSum} with
         * <ul>
         *  <li>{@code a1 = Math.sqrt(Math.PI * twolpd) * Math.exp(c1)}</li>
         *  <li>{@code aSum = a1 + a2 + 1}</li>
         * </ul>
         */
        private final double p1;
        /**
         * Algorithm constant: {@code a2 / aSum} with
         * <ul>
         *  <li>{@code a2 = (twolpd / delta) * Math.exp(-delta * (1 + delta) / twolpd)}</li>
         *  <li>{@code aSum = a1 + a2 + 1}</li>
         * </ul>
         */
        private final double p2;
        /** Algorithm constant: {@code 1 / (8 * lambda)}. */
        private final double c1;

        /**
         * Instantiates a new large mean poisson sampler state for
         * the range {@code lambda <= mean < lambda+1}.
         *
         * @param lambda             {@code Math.floor(mean)}
         * @param logLambda          {@code Math.log(lambda)}
         * @param logLambdaFactorial {@code factorialLog((int) lambda)}
         * @param delta              {@code Math.sqrt(lambda * Math.log(32 * lambda / Math.PI + 1))}
         * @param halfDelta          {@code delta / 2}
         * @param twolpd             {@code 2 * lambda + delta}
         * @param p1                 {@code a1 / aSum}
         * @param p2                 {@code a2 / aSum}
         * @param c1                 {@code 1 / (8 * lambda)}
         */
        private LargeMeanPoissonSamplerState(double lambda,
                                             double logLambda,
                                             double logLambdaFactorial,
                                             double delta,
                                             double halfDelta,
                                             double twolpd,
                                             double p1,
                                             double p2,
                                             double c1) {
            this.lambda = lambda;
            this.logLambda = logLambda;
            this.logLambdaFactorial = logLambdaFactorial;
            this.delta = delta;
            this.halfDelta = halfDelta;
            this.twolpd = twolpd;
            this.p1 = p1;
            this.p2 = p2;
            this.c1 = c1;
        }

        /**
         * Creates the state. The state is valid for construction of a sampler in the
         * range {@code n <= mean < n+1}.
         *
         * @param n the value n ({@code floor(mean)})
         * @return the state
         * @throws IllegalArgumentException if {@code n < 0}.
         */
        static LargeMeanPoissonSamplerState create(int n) {
            if (n < 0) {
                throw new IllegalArgumentException(n + " < " + 0);
            }
            final double lambda = n;
            final double logLambda = Math.log(lambda);
            final double logLambdaFactorial = NO_CACHE_FACTORIAL_LOG.value(n);
            final double delta = Math.sqrt(lambda * Math.log(32 * lambda / Math.PI + 1));
            final double halfDelta = delta / 2;
            final double twolpd = 2 * lambda + delta;
            final double c1 = 1 / (8 * lambda);
            final double a1 = Math.sqrt(Math.PI * twolpd) * Math.exp(c1);
            final double a2 = (twolpd / delta) * Math.exp(-delta * (1 + delta) / twolpd);
            final double aSum = a1 + a2 + 1;
            final double p1 = a1 / aSum;
            final double p2 = a2 / aSum;
            return new LargeMeanPoissonSamplerState(lambda, logLambda, logLambdaFactorial, delta, halfDelta, twolpd, p1,
                    p2, c1);
        }

        /**
         * Get the lambda value for the state. Equal to {@code floor(mean)}.
         * @return {@code floor(mean)}
         */
        int getLambda() {
            return (int) lambda;
        }
    }

    /**
     * @param rng  Generator of uniformly distributed random numbers.
     * @param mean Mean.
     * @throws IllegalArgumentException if {@code mean <= 0}.
     */
    public LargeMeanPoissonSampler(UniformRandomProvider rng,
                                   double mean) {
        super(rng);
        if (mean <= 0) {
            throw new IllegalArgumentException(mean + " <= " + 0);
        }

        gaussian = new ZigguratNormalizedGaussianSampler(rng);
        exponential = new AhrensDieterExponentialSampler(rng, 1);
        // Plain constructor uses the uncached function.
        factorialLog = NO_CACHE_FACTORIAL_LOG;

        // Cache values used in the algorithm
        lambda = Math.floor(mean);
        lambdaFractional = mean - lambda;
        logLambda = Math.log(lambda);
        logLambdaFactorial = factorialLog((int) lambda);
        delta = Math.sqrt(lambda * Math.log(32 * lambda / Math.PI + 1));
        halfDelta = delta / 2;
        twolpd = 2 * lambda + delta;
        c1 = 1 / (8 * lambda);
        final double a1 = Math.sqrt(Math.PI * twolpd) * Math.exp(c1);
        final double a2 = (twolpd / delta) * Math.exp(-delta * (1 + delta) / twolpd);
        final double aSum = a1 + a2 + 1;
        p1 = a1 / aSum;
        p2 = a2 / aSum;

        // The algorithm requires a Poisson sample from the remaining lambda fraction.
        smallMeanPoissonSampler = (lambdaFractional < Double.MIN_VALUE) ?
            null : // Not used.
            new SmallMeanPoissonSampler(rng, lambdaFractional);
    }

    /**
     * Instantiates a sampler using a precomputed state.
     *
     * @param rng              Generator of uniformly distributed random numbers.
     * @param state            The state for {@code (int)Math.floor(mean)}.
     * @param lambdaFractional The lambda fractional value
     *                         ({@code mean - (int)Math.floor(mean))}.
     * @throws IllegalArgumentException
     *                         if {@code lambdaFractional < 0 || lambdaFractional >= 1}.
     */
    LargeMeanPoissonSampler(UniformRandomProvider rng,
                            LargeMeanPoissonSamplerState state,
                            double lambdaFractional) {
        super(rng);
        if (lambdaFractional < 0 || lambdaFractional >= 1) {
            throw new IllegalArgumentException(
                    "lambdaFractional must be in the range 0 (inclusive) to 1 (exclusive): " + lambdaFractional);
        }

        gaussian = new ZigguratNormalizedGaussianSampler(rng);
        exponential = new AhrensDieterExponentialSampler(rng, 1);
        // Plain constructor uses the uncached function.
        factorialLog = NO_CACHE_FACTORIAL_LOG;

        // Use the state to initialise the algorithm
        this.lambda = state.lambda;
        this.logLambda = state.logLambda;
        this.logLambdaFactorial = state.logLambdaFactorial;
        this.delta = state.delta;
        this.halfDelta = state.halfDelta;
        this.twolpd = state.twolpd;
        this.p1 = state.p1;
        this.p2 = state.p2;
        this.c1 = state.c1;
        this.lambdaFractional = lambdaFractional;

        // The algorithm requires a Poisson sample from the remaining lambda fraction.
        smallMeanPoissonSampler = (lambdaFractional < Double.MIN_VALUE) ?
            null : // Not used.
            new SmallMeanPoissonSampler(rng, lambdaFractional);
    }

    /** {@inheritDoc} */
    @Override
    public int sample() {

        final int y2 = (smallMeanPoissonSampler == null) ?
            0 : // No lambda fraction
            smallMeanPoissonSampler.sample();

        double x = 0;
        double y = 0;
        double v = 0;
        int a = 0;
        double t = 0;
        double qr = 0;
        double qa = 0;
        while (true) {
            final double u = nextDouble();
            if (u <= p1) {
                final double n = gaussian.sample();
                x = n * Math.sqrt(lambda + halfDelta) - 0.5d;
                if (x > delta || x < -lambda) {
                    continue;
                }
                y = x < 0 ? Math.floor(x) : Math.ceil(x);
                final double e = exponential.sample();
                v = -e - 0.5 * n * n + c1;
            } else {
                if (u > p1 + p2) {
                    y = lambda;
                    break;
                }
                x = delta + (twolpd / delta) * exponential.sample();
                y = Math.ceil(x);
                v = -exponential.sample() - delta * (x + 1) / twolpd;
            }
            a = x < 0 ? 1 : 0;
            t = y * (y + 1) / (2 * lambda);
            if (v < -t && a == 0) {
                y = lambda + y;
                break;
            }
            qr = t * ((2 * y + 1) / (6 * lambda) - 1);
            qa = qr - (t * t) / (3 * (lambda + a * (y + 1)));
            if (v < qa) {
                y = lambda + y;
                break;
            }
            if (v > qr) {
                continue;
            }
            if (v < y * logLambda - factorialLog((int) (y + lambda)) + logLambdaFactorial) {
                y = lambda + y;
                break;
            }
        }

        return (int) Math.min(y2 + (long) y, Integer.MAX_VALUE);
    }

    /**
     * Compute the natural logarithm of the factorial of {@code n}.
     *
     * @param n Argument.
     * @return {@code log(n!)}
     * @throws IllegalArgumentException if {@code n < 0}.
     */
    private double factorialLog(int n) {
        return factorialLog.value(n);
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return "Large Mean Poisson deviate [" + super.toString() + "]";
    }
}
