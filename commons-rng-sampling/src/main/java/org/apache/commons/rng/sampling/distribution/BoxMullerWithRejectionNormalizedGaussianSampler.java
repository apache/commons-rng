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
 * <a href="https://en.wikipedia.org/wiki/Box%E2%80%93Muller_transform">
 * Box-Muller algorithm</a> for sampling from Gaussian distribution with
 * mean 0 and standard deviation 1.
 * This is a variation, suggested in <a href="http://haifux.org/lectures/79/random.pdf">
 * this presentation</a> (page 39), of the algorithm implemented in
 * {@link BoxMullerNormalizedGaussianSampler}.
 *
 * @since 1.1
 */
public class BoxMullerWithRejectionNormalizedGaussianSampler
    extends SamplerBase
    implements NormalizedGaussianSampler {
    /** Next gaussian. */
    private double nextGaussian = Double.NaN;

    /**
     * @param rng Generator of uniformly distributed random numbers.
     */
    public BoxMullerWithRejectionNormalizedGaussianSampler(UniformRandomProvider rng) {
        super(rng);
    }

    /** {@inheritDoc} */
    @Override
    public double sample() {
        final double random;
        if (Double.isNaN(nextGaussian)) {
            // Rejection scheme for selecting a pair that lies within the unit circle.
            SAMPLE: while (true) {
                // Generate a pair of numbers within [-1 , 1).
                final double x = 2 * nextDouble() - 1;
                final double y = 2 * nextDouble() - 1;
                final double r2 = x * x + y * y;

                if (r2 < 1) {
                    // Pair (x, y) is within unit circle.

                    final double r = Math.sqrt(r2);
                    final double alpha = 2 * Math.sqrt(-Math.log(r)) / r;

                    // Return the first element of the generated pair.
                    random = alpha * x;

                    // Keep second element of the pair for next invocation.
                    nextGaussian = alpha * y;
                    break SAMPLE;
                }
                // Pair is not within the unit circle: Generate another one.
            }
        } else {
            // Use the second element of the pair (generated at the
            // previous invocation).
            random = nextGaussian;

            // Both elements of the pair have been used.
            nextGaussian = Double.NaN;
        }

        return random;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return "Box-Muller (with rejection) normalized Gaussian deviate [" + super.toString() + "]";
    }
}
