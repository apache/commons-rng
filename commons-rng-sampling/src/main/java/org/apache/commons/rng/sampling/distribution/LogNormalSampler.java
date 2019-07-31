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
 * Sampling from a log-normal distribution.
 *
 * @since 1.1
 */
public class LogNormalSampler implements SharedStateContinuousSampler {
    /** Scale. */
    private final double scale;
    /** Shape. */
    private final double shape;
    /** Gaussian sampling. */
    private final NormalizedGaussianSampler gaussian;

    /**
     * @param gaussian N(0,1) generator.
     * @param scale Scale of the log-normal distribution.
     * @param shape Shape of the log-normal distribution.
     * @throws IllegalArgumentException if {@code scale < 0} or {@code shape <= 0}.
     */
    public LogNormalSampler(NormalizedGaussianSampler gaussian,
                            double scale,
                            double shape) {
        if (scale < 0) {
            throw new IllegalArgumentException("scale is not positive: " + scale);
        }
        if (shape <= 0) {
            throw new IllegalArgumentException("shape is not strictly positive: " + shape);
        }
        this.scale = scale;
        this.shape = shape;
        this.gaussian = gaussian;
    }

    /**
     * @param rng Generator of uniformly distributed random numbers.
     * @param source Source to copy.
     */
    private LogNormalSampler(UniformRandomProvider rng,
                             LogNormalSampler source) {
        this.scale = source.scale;
        this.shape = source.shape;
        this.gaussian = InternalUtils.newNormalizedGaussianSampler(source.gaussian, rng);
    }

    /** {@inheritDoc} */
    @Override
    public double sample() {
        return Math.exp(scale + shape * gaussian.sample());
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return "Log-normal deviate [" + gaussian.toString() + "]";
    }

    /**
     * {@inheritDoc}
     *
     * <p>Note: This function is available if the underlying {@link NormalizedGaussianSampler}
     * is a {@link org.apache.commons.rng.sampling.SharedStateSampler SharedStateSampler}.
     * Otherwise a run-time exception is thrown.</p>
     *
     * @throws UnsupportedOperationException if the underlying sampler is not a
     * {@link org.apache.commons.rng.sampling.SharedStateSampler SharedStateSampler} or
     * does not return a {@link NormalizedGaussianSampler} when sharing state.
     */
    @Override
    public SharedStateContinuousSampler withUniformRandomProvider(UniformRandomProvider rng) {
        return new LogNormalSampler(rng, this);
    }

    /**
     * Create a new log-normal distribution sampler.
     *
     * <p>Note: The shared-state functionality is available if the {@link NormalizedGaussianSampler}
     * is a {@link org.apache.commons.rng.sampling.SharedStateSampler SharedStateSampler}.
     * Otherwise a run-time exception will be thrown when the sampler is used to share state.</p>
     *
     * @param gaussian N(0,1) generator.
     * @param scale Scale of the log-normal distribution.
     * @param shape Shape of the log-normal distribution.
     * @return the sampler
     * @throws IllegalArgumentException if {@code scale < 0} or {@code shape <= 0}.
     * @see #withUniformRandomProvider(UniformRandomProvider)
     */
    public static SharedStateContinuousSampler of(NormalizedGaussianSampler gaussian,
                                                  double scale,
                                                  double shape) {
        return new LogNormalSampler(gaussian, scale, shape);
    }
}
