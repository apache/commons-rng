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
 * Sampling from a Lévy distribution.
 *
 * @see <a href="https://en.wikipedia.org/wiki/L%C3%A9vy_distribution">Lévy distribution</a>
 * @since 1.4
 */
public final class LevySampler implements SharedStateContinuousSampler {
    /** Gaussian sampler. */
    private final NormalizedGaussianSampler gaussian;
    /** Location. */
    private final double location;
    /** Scale. */
    private final double scale;
    /** RNG (used for the toString() method). */
    private final UniformRandomProvider rng;

    /**
     * @param rng Generator of uniformly distributed random numbers.
     * @param location Location of the Lévy distribution.
     * @param scale Scale of the Lévy distribution.
     */
    private LevySampler(UniformRandomProvider rng,
                        double location,
                        double scale) {
        this.gaussian = ZigguratSampler.NormalizedGaussian.of(rng);
        this.location = location;
        this.scale = scale;
        this.rng = rng;
    }

    /**
     * @param rng Generator of uniformly distributed random numbers.
     * @param source Source to copy.
     */
    private LevySampler(UniformRandomProvider rng,
                        LevySampler source) {
        this.gaussian = ZigguratSampler.NormalizedGaussian.of(rng);
        this.location = source.location;
        this.scale = source.scale;
        this.rng = rng;
    }

    /** {@inheritDoc} */
    @Override
    public double sample() {
        final double n = gaussian.sample();
        return scale / (n * n) + location;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return "Lévy deviate [" + rng.toString() + "]";
    }

    /** {@inheritDoc} */
    @Override
    public LevySampler withUniformRandomProvider(UniformRandomProvider rng) {
        return new LevySampler(rng, this);
    }

    /**
     * Create a new Lévy distribution sampler.
     *
     * @param rng Generator of uniformly distributed random numbers.
     * @param location Location of the Lévy distribution.
     * @param scale Scale of the Lévy distribution.
     * @return the sampler
     * @throws IllegalArgumentException if {@code scale <= 0}
     */
    public static LevySampler of(UniformRandomProvider rng,
                                 double location,
                                 double scale) {
        if (scale <= 0) {
            throw new IllegalArgumentException("scale is not strictly positive: " + scale);
        }
        return new LevySampler(rng, location, scale);
    }
}
