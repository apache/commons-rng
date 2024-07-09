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
 *
 * <p>Sampling uses:</p>
 *
 * <ul>
 *   <li>{@link UniformRandomProvider#nextDouble()}
 *   <li>{@link UniformRandomProvider#nextLong()}
 * </ul>
 *
 * @since 1.1
 */
public class BoxMullerNormalizedGaussianSampler
    implements NormalizedGaussianSampler, SharedStateContinuousSampler {
    /** Next gaussian. */
    private double nextGaussian = Double.NaN;
    /** Underlying source of randomness. */
    private final UniformRandomProvider rng;

    /**
     * Create an instance.
     *
     * @param rng Generator of uniformly distributed random numbers.
     */
    public BoxMullerNormalizedGaussianSampler(UniformRandomProvider rng) {
        this.rng = rng;
    }

    /** {@inheritDoc} */
    @Override
    public double sample() {
        final double random;
        if (Double.isNaN(nextGaussian)) {
            // Generate a pair of Gaussian numbers.

            // Avoid zero for the uniform deviate y.
            // The extreme tail of the sample is:
            // y = 2^-53
            // r = 8.57167
            final double x = rng.nextDouble();
            final double y = InternalUtils.makeNonZeroDouble(rng.nextLong());
            final double alpha = 2 * Math.PI * x;
            final double r = Math.sqrt(-2 * Math.log(y));

            // Return the first element of the generated pair.
            random = r * Math.cos(alpha);

            // Keep second element of the pair for next invocation.
            nextGaussian = r * Math.sin(alpha);
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
        return "Box-Muller normalized Gaussian deviate [" + rng.toString() + "]";
    }

    /**
     * {@inheritDoc}
     *
     * @since 1.3
     */
    @Override
    public SharedStateContinuousSampler withUniformRandomProvider(UniformRandomProvider rng) {
        return new BoxMullerNormalizedGaussianSampler(rng);
    }

    /**
     * Create a new normalised Gaussian sampler.
     *
     * @param <S> Sampler type.
     * @param rng Generator of uniformly distributed random numbers.
     * @return the sampler
     * @since 1.3
     */
    @SuppressWarnings("unchecked")
    public static <S extends NormalizedGaussianSampler & SharedStateContinuousSampler> S
            of(UniformRandomProvider rng) {
        return (S) new BoxMullerNormalizedGaussianSampler(rng);
    }
}
