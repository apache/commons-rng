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
 * Sampling from the <a href="http://mathworld.wolfram.com/GammaDistribution.html">Gamma distribution</a>.
 * <ul>
 *  <li>
 *   For {@code 0 < shape < 1}:
 *   <blockquote>
 *    Ahrens, J. H. and Dieter, U.,
 *    <i>Computer methods for sampling from gamma, beta, Poisson and binomial distributions,</i>
 *    Computing, 12, 223-246, 1974.
 *   </blockquote>
 *  </li>
 *  <li>
 *  For {@code shape >= 1}:
 *   <blockquote>
 *   Marsaglia and Tsang, <i>A Simple Method for Generating
 *   Gamma Variables.</i> ACM Transactions on Mathematical Software,
 *   Volume 26 Issue 3, September, 2000.
 *   </blockquote>
 *  </li>
 * </ul>
 */
public class AhrensDieterMarsagliaTsangGammaSampler
    extends SamplerBase
    implements ContinuousSampler {
    /** The shape parameter. */
    private final double theta;
    /** The alpha parameter. */
    private final double alpha;
    /** Gaussian sampling. */
    private final NormalizedGaussianSampler gaussian;

    /**
     * @param rng Generator of uniformly distributed random numbers.
     * @param alpha Alpha parameter of the distribution.
     * @param theta Theta parameter of the distribution.
     */
    public AhrensDieterMarsagliaTsangGammaSampler(UniformRandomProvider rng,
                                                  double alpha,
                                                  double theta) {
        super(rng);
        this.alpha = alpha;
        this.theta = theta;
        gaussian = new MarsagliaNormalizedGaussianSampler(rng);
    }

    /** {@inheritDoc} */
    @Override
    public double sample() {
        if (theta < 1) {
            // [1]: p. 228, Algorithm GS.

            while (true) {
                // Step 1:
                final double u = nextDouble();
                final double bGS = 1 + theta / Math.E;
                final double p = bGS * u;

                if (p <= 1) {
                    // Step 2:

                    final double x = Math.pow(p, 1 / theta);
                    final double u2 = nextDouble();

                    if (u2 > Math.exp(-x)) {
                        // Reject.
                        continue;
                    } else {
                        return alpha * x;
                    }
                } else {
                    // Step 3:

                    final double x = -1 * Math.log((bGS - p) / theta);
                    final double u2 = nextDouble();

                    if (u2 > Math.pow(x, theta - 1)) {
                        // Reject.
                        continue;
                    } else {
                        return alpha * x;
                    }
                }
            }
        }

        // Now theta >= 1.

        final double d = theta - 0.333333333333333333;
        final double c = 1 / (3 * Math.sqrt(d));

        while (true) {
            final double x = gaussian.sample();
            final double v = (1 + c * x) * (1 + c * x) * (1 + c * x);

            if (v <= 0) {
                continue;
            }

            final double x2 = x * x;
            final double u = nextDouble();

            // Squeeze.
            if (u < 1 - 0.0331 * x2 * x2) {
                return alpha * d * v;
            }

            if (Math.log(u) < 0.5 * x2 + d * (1 - v + Math.log(v))) {
                return alpha * d * v;
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return "Ahrens-Dieter-Marsaglia-Tsang Gamma deviate [" + super.toString() + "]";
    }
}
