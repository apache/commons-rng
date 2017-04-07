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
 *   described <a href="http://mathaa.epfl.ch/cours/PMMI2001/interactive/rng7.htm">here</a>.
 *   The Poisson process (and hence, the returned value) is bounded by 1000 * mean.
 *  </li>
 *  <li>
 *   For large means, we use the rejection algorithm described in
 *   <blockquote>
 *    Devroye, Luc. (1981).<i>The Computer Generation of Poisson Random Variables</i><br>
 *    <strong>Computing</strong> vol. 26 pp. 197-207.
 *   </blockquote>
 *  </li>
 * </ul>
 */
public class PoissonSampler
    extends SamplerBase
    implements DiscreteSampler {
    /** Value for switching sampling algorithm. */
    private static final double PIVOT = 40;
    /** Mean of the distribution. */
    private final double mean;
    /** Exponential. */
    private final ContinuousSampler exponential;
    /** Gaussian. */
    private final NormalizedGaussianSampler gaussian;
    /** {@code log(n!)}. */
    private final InternalUtils.FactorialLog factorialLog;

    /**
     * @param rng Generator of uniformly distributed random numbers.
     * @param mean Mean.
     * @throws IllegalArgumentException if {@code mean <= 0}.
     */
    public PoissonSampler(UniformRandomProvider rng,
                          double mean) {
        super(rng);
        if (mean <= 0) {
            throw new IllegalArgumentException(mean + " <= " + 0);
        }

        this.mean = mean;

        gaussian = new MarsagliaNormalizedGaussianSampler(rng);
        exponential = new AhrensDieterExponentialSampler(rng, 1);
        factorialLog = mean < PIVOT ?
            null : // Not used.
            InternalUtils.FactorialLog.create().withCache((int) Math.min(mean, 2 * PIVOT));
    }

    /** {@inheritDoc} */
    @Override
    public int sample() {
        return (int) Math.min(nextPoisson(mean), Integer.MAX_VALUE);
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return "Poisson deviate [" + super.toString() + "]";
    }

    /**
     * @param meanPoisson Mean.
     * @return the next sample.
     */
    private long nextPoisson(double meanPoisson) {
        if (meanPoisson < PIVOT) {
            double p = Math.exp(-meanPoisson);
            long n = 0;
            double r = 1;

            while (n < 1000 * meanPoisson) {
                r *= nextDouble();
                if (r >= p) {
                    n++;
                } else {
                    break;
                }
            }
            return n;
        } else {
            final double lambda = Math.floor(meanPoisson);
            final double lambdaFractional = meanPoisson - lambda;
            final double logLambda = Math.log(lambda);
            final double logLambdaFactorial = factorialLog((int) lambda);
            final long y2 = lambdaFractional < Double.MIN_VALUE ? 0 : nextPoisson(lambdaFractional);
            final double delta = Math.sqrt(lambda * Math.log(32 * lambda / Math.PI + 1));
            final double halfDelta = delta / 2;
            final double twolpd = 2 * lambda + delta;
            final double a1 = Math.sqrt(Math.PI * twolpd) * Math.exp(1 / (8 * lambda));
            final double a2 = (twolpd / delta) * Math.exp(-delta * (1 + delta) / twolpd);
            final double aSum = a1 + a2 + 1;
            final double p1 = a1 / aSum;
            final double p2 = a2 / aSum;
            final double c1 = 1 / (8 * lambda);

            double x;
            double y;
            double v;
            int a;
            double t;
            double qr;
            double qa;
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
                    } else {
                        x = delta + (twolpd / delta) * exponential.sample();
                        y = Math.ceil(x);
                        v = -exponential.sample() - delta * (x + 1) / twolpd;
                    }
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
            return y2 + (long) y;
        }
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
}
