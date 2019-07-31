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
 * <p>This sampler is suitable for {@code mean >= 40}.</p>
 *
 * <p>Sampling uses:</p>
 *
 * <ul>
 *   <li>{@link UniformRandomProvider#nextLong()}
 *   <li>{@link UniformRandomProvider#nextDouble()}
 * </ul>
 *
 * @since 1.1
 */
public class LargeMeanPoissonSampler
    implements SharedStateDiscreteSampler {
    /** Upper bound to avoid truncation. */
    private static final double MAX_MEAN = 0.5 * Integer.MAX_VALUE;
    /** Class to compute {@code log(n!)}. This has no cached values. */
    private static final InternalUtils.FactorialLog NO_CACHE_FACTORIAL_LOG;
    /** Used when there is no requirement for a small mean Poisson sampler. */
    private static final SharedStateDiscreteSampler NO_SMALL_MEAN_POISSON_SAMPLER =
        new SharedStateDiscreteSampler() {
            @Override
            public SharedStateDiscreteSampler withUniformRandomProvider(UniformRandomProvider rng) {
                // No requirement for RNG
                return this;
            }

            @Override
            public int sample() {
                // No Poisson sample
                return 0;
            }
        };

    static {
        // Create without a cache.
        NO_CACHE_FACTORIAL_LOG = FactorialLog.create();
    }

    /** Underlying source of randomness. */
    private final UniformRandomProvider rng;
    /** Exponential. */
    private final SharedStateContinuousSampler exponential;
    /** Gaussian. */
    private final SharedStateContinuousSampler gaussian;
    /** Local class to compute {@code log(n!)}. This may have cached values. */
    private final InternalUtils.FactorialLog factorialLog;

    // Working values

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
     * Algorithm constant: {@code a1 / aSum}.
     * <ul>
     *  <li>{@code a1 = Math.sqrt(Math.PI * twolpd) * Math.exp(c1)}</li>
     *  <li>{@code aSum = a1 + a2 + 1}</li>
     * </ul>
     */
    private final double p1;
    /**
     * Algorithm constant: {@code a2 / aSum}.
     * <ul>
     *  <li>{@code a2 = (twolpd / delta) * Math.exp(-delta * (1 + delta) / twolpd)}</li>
     *  <li>{@code aSum = a1 + a2 + 1}</li>
     * </ul>
     */
    private final double p2;
    /** Algorithm constant: {@code 1 / (8 * lambda)}. */
    private final double c1;

    /** The internal Poisson sampler for the lambda fraction. */
    private final SharedStateDiscreteSampler smallMeanPoissonSampler;

    /**
     * @param rng Generator of uniformly distributed random numbers.
     * @param mean Mean.
     * @throws IllegalArgumentException if {@code mean < 1} or
     * {@code mean > 0.5 *} {@link Integer#MAX_VALUE}.
     */
    public LargeMeanPoissonSampler(UniformRandomProvider rng,
                                   double mean) {
        if (mean < 1) {
            throw new IllegalArgumentException("mean is not >= 1: " + mean);
        }
        // The algorithm is not valid if Math.floor(mean) is not an integer.
        if (mean > MAX_MEAN) {
            throw new IllegalArgumentException("mean " + mean + " > " + MAX_MEAN);
        }
        this.rng = rng;

        gaussian = new ZigguratNormalizedGaussianSampler(rng);
        exponential = AhrensDieterExponentialSampler.of(rng, 1);
        // Plain constructor uses the uncached function.
        factorialLog = NO_CACHE_FACTORIAL_LOG;

        // Cache values used in the algorithm
        lambda = Math.floor(mean);
        logLambda = Math.log(lambda);
        logLambdaFactorial = getFactorialLog((int) lambda);
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
        final double lambdaFractional = mean - lambda;
        smallMeanPoissonSampler = (lambdaFractional < Double.MIN_VALUE) ?
            NO_SMALL_MEAN_POISSON_SAMPLER : // Not used.
            KempSmallMeanPoissonSampler.of(rng, lambdaFractional);
    }

    /**
     * Instantiates a sampler using a precomputed state.
     *
     * @param rng              Generator of uniformly distributed random numbers.
     * @param state            The state for {@code lambda = (int)Math.floor(mean)}.
     * @param lambdaFractional The lambda fractional value
     *                         ({@code mean - (int)Math.floor(mean))}.
     * @throws IllegalArgumentException
     *                         if {@code lambdaFractional < 0 || lambdaFractional >= 1}.
     */
    LargeMeanPoissonSampler(UniformRandomProvider rng,
                            LargeMeanPoissonSamplerState state,
                            double lambdaFractional) {
        if (lambdaFractional < 0 || lambdaFractional >= 1) {
            throw new IllegalArgumentException(
                    "lambdaFractional must be in the range 0 (inclusive) to 1 (exclusive): " + lambdaFractional);
        }
        this.rng = rng;

        gaussian = new ZigguratNormalizedGaussianSampler(rng);
        exponential = AhrensDieterExponentialSampler.of(rng, 1);
        // Plain constructor uses the uncached function.
        factorialLog = NO_CACHE_FACTORIAL_LOG;

        // Use the state to initialise the algorithm
        lambda = state.getLambdaRaw();
        logLambda = state.getLogLambda();
        logLambdaFactorial = state.getLogLambdaFactorial();
        delta = state.getDelta();
        halfDelta = state.getHalfDelta();
        twolpd = state.getTwolpd();
        p1 = state.getP1();
        p2 = state.getP2();
        c1 = state.getC1();

        // The algorithm requires a Poisson sample from the remaining lambda fraction.
        smallMeanPoissonSampler = (lambdaFractional < Double.MIN_VALUE) ?
            NO_SMALL_MEAN_POISSON_SAMPLER : // Not used.
            KempSmallMeanPoissonSampler.of(rng, lambdaFractional);
    }

    /**
     * @param rng Generator of uniformly distributed random numbers.
     * @param source Source to copy.
     */
    private LargeMeanPoissonSampler(UniformRandomProvider rng,
                                    LargeMeanPoissonSampler source) {
        this.rng = rng;

        gaussian = source.gaussian.withUniformRandomProvider(rng);
        exponential = source.exponential.withUniformRandomProvider(rng);
        // Reuse the cache
        factorialLog = source.factorialLog;

        lambda = source.lambda;
        logLambda = source.logLambda;
        logLambdaFactorial = source.logLambdaFactorial;
        delta = source.delta;
        halfDelta = source.halfDelta;
        twolpd = source.twolpd;
        p1 = source.p1;
        p2 = source.p2;
        c1 = source.c1;

        // Share the state of the small sampler
        smallMeanPoissonSampler = source.smallMeanPoissonSampler.withUniformRandomProvider(rng);
    }

    /** {@inheritDoc} */
    @Override
    public int sample() {
        // This will never be null. It may be a no-op delegate that returns zero.
        final int y2 = smallMeanPoissonSampler.sample();

        double x;
        double y;
        double v;
        int a;
        double t;
        double qr;
        double qa;
        while (true) {
            final double u = rng.nextDouble();
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
            if (v < y * logLambda - getFactorialLog((int) (y + lambda)) + logLambdaFactorial) {
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
    private double getFactorialLog(int n) {
        return factorialLog.value(n);
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return "Large Mean Poisson deviate [" + rng.toString() + "]";
    }

    /** {@inheritDoc} */
    @Override
    public SharedStateDiscreteSampler withUniformRandomProvider(UniformRandomProvider rng) {
        return new LargeMeanPoissonSampler(rng, this);
    }

    /**
     * Creates a new Poisson distribution sampler.
     *
     * @param rng Generator of uniformly distributed random numbers.
     * @param mean Mean.
     * @return the sampler
     * @throws IllegalArgumentException if {@code mean < 1} or {@code mean > 0.5 *}
     * {@link Integer#MAX_VALUE}.
     */
    public static SharedStateDiscreteSampler of(UniformRandomProvider rng,
                                                double mean) {
        return new LargeMeanPoissonSampler(rng, mean);
    }
    /**
     * Gets the initialisation state of the sampler.
     *
     * <p>The state is computed using an integer {@code lambda} value of
     * {@code lambda = (int)Math.floor(mean)}.
     *
     * <p>The state will be suitable for reconstructing a new sampler with a mean
     * in the range {@code lambda <= mean < lambda+1} using
     * {@link #LargeMeanPoissonSampler(UniformRandomProvider, LargeMeanPoissonSamplerState, double)}.
     *
     * @return the state
     */
    LargeMeanPoissonSamplerState getState() {
        return new LargeMeanPoissonSamplerState(lambda, logLambda, logLambdaFactorial,
                delta, halfDelta, twolpd, p1, p2, c1);
    }

    /**
     * Encapsulate the state of the sampler. The state is valid for construction of
     * a sampler in the range {@code lambda <= mean < lambda+1}.
     *
     * <p>This class is immutable.
     *
     * @see #getLambda()
     */
    static final class LargeMeanPoissonSamplerState {
        /** Algorithm constant {@code lambda}. */
        private final double lambda;
        /** Algorithm constant {@code logLambda}. */
        private final double logLambda;
        /** Algorithm constant {@code logLambdaFactorial}. */
        private final double logLambdaFactorial;
        /** Algorithm constant {@code delta}. */
        private final double delta;
        /** Algorithm constant {@code halfDelta}. */
        private final double halfDelta;
        /** Algorithm constant {@code twolpd}. */
        private final double twolpd;
        /** Algorithm constant {@code p1}. */
        private final double p1;
        /** Algorithm constant {@code p2}. */
        private final double p2;
        /** Algorithm constant {@code c1}. */
        private final double c1;

        /**
         * Creates the state.
         *
         * <p>The state is valid for construction of a sampler in the range
         * {@code lambda <= mean < lambda+1} where {@code lambda} is an integer.
         *
         * @param lambda the lambda
         * @param logLambda the log lambda
         * @param logLambdaFactorial the log lambda factorial
         * @param delta the delta
         * @param halfDelta the half delta
         * @param twolpd the two lambda plus delta
         * @param p1 the p1 constant
         * @param p2 the p2 constant
         * @param c1 the c1 constant
         */
        LargeMeanPoissonSamplerState(double lambda, double logLambda,
                double logLambdaFactorial, double delta, double halfDelta, double twolpd,
                double p1, double p2, double c1) {
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
         * Get the lambda value for the state.
         *
         * <p>Equal to {@code floor(mean)} for a Poisson sampler.
         * @return the lambda value
         */
        int getLambda() {
            return (int) getLambdaRaw();
        }

        /**
         * @return algorithm constant {@code lambda}
         */
        double getLambdaRaw() {
            return lambda;
        }

        /**
         * @return algorithm constant {@code logLambda}
         */
        double getLogLambda() {
            return logLambda;
        }

        /**
         * @return algorithm constant {@code logLambdaFactorial}
         */
        double getLogLambdaFactorial() {
            return logLambdaFactorial;
        }

        /**
         * @return algorithm constant {@code delta}
         */
        double getDelta() {
            return delta;
        }

        /**
         * @return algorithm constant {@code halfDelta}
         */
        double getHalfDelta() {
            return halfDelta;
        }

        /**
         * @return algorithm constant {@code twolpd}
         */
        double getTwolpd() {
            return twolpd;
        }

        /**
         * @return algorithm constant {@code p1}
         */
        double getP1() {
            return p1;
        }

        /**
         * @return algorithm constant {@code p2}
         */
        double getP2() {
            return p2;
        }

        /**
         * @return algorithm constant {@code c1}
         */
        double getC1() {
            return c1;
        }
    }
}
