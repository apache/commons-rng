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
 * Sampling from a <a href="https://en.wikipedia.org/wiki/Pareto_distribution">Pareto distribution</a>.
 *
 * <p>Sampling uses {@link UniformRandomProvider#nextDouble()}.</p>
 *
 * @since 1.0
 */
public class InverseTransformParetoSampler
    extends SamplerBase
    implements SharedStateContinuousSampler {
    /** Scale. */
    private final double scale;
    /** 1 / Shape. */
    private final double oneOverShape;
    /** Underlying source of randomness. */
    private final UniformRandomProvider rng;

    /**
     * @param rng Generator of uniformly distributed random numbers.
     * @param scale Scale of the distribution.
     * @param shape Shape of the distribution.
     * @throws IllegalArgumentException if {@code scale <= 0} or {@code shape <= 0}
     */
    public InverseTransformParetoSampler(UniformRandomProvider rng,
                                         double scale,
                                         double shape) {
        super(null);
        if (scale <= 0) {
            throw new IllegalArgumentException("scale is not strictly positive: " + scale);
        }
        if (shape <= 0) {
            throw new IllegalArgumentException("shape is not strictly positive: " + shape);
        }
        this.rng = rng;
        this.scale = scale;
        this.oneOverShape = 1 / shape;
    }

    /**
     * @param rng Generator of uniformly distributed random numbers.
     * @param source Source to copy.
     */
    private InverseTransformParetoSampler(UniformRandomProvider rng,
                                          InverseTransformParetoSampler source) {
        super(null);
        this.rng = rng;
        scale = source.scale;
        oneOverShape = source.oneOverShape;
    }

    /** {@inheritDoc} */
    @Override
    public double sample() {
        return scale / Math.pow(rng.nextDouble(), oneOverShape);
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return "[Inverse method for Pareto distribution " + rng.toString() + "]";
    }

    /** {@inheritDoc} */
    @Override
    public SharedStateContinuousSampler withUniformRandomProvider(UniformRandomProvider rng) {
        return new InverseTransformParetoSampler(rng, this);
    }

    /**
     * Creates a new Pareto distribution sampler.
     *
     * @param rng Generator of uniformly distributed random numbers.
     * @param scale Scale of the distribution.
     * @param shape Shape of the distribution.
     * @return the sampler
     * @throws IllegalArgumentException if {@code scale <= 0} or {@code shape <= 0}
     */
    public static SharedStateContinuousSampler of(UniformRandomProvider rng,
                                                  double scale,
                                                  double shape) {
        return new InverseTransformParetoSampler(rng, scale, shape);
    }
}
