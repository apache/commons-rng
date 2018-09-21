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
 * @since 1.0
 */
public class InverseTransformParetoSampler
    extends SamplerBase
    implements ContinuousSampler {
    /** Scale. */
    private final double scale;
    /** Shape. */
    private final double shape;
    /** Underlying source of randomness. */
    private final UniformRandomProvider rng;

    /**
     * @param rng Generator of uniformly distributed random numbers.
     * @param scale Scale of the distribution.
     * @param shape Shape of the distribution.
     */
    public InverseTransformParetoSampler(UniformRandomProvider rng,
                                         double scale,
                                         double shape) {
        super(null);
        this.rng = rng;
        this.scale = scale;
        this.shape = shape;
    }

    /** {@inheritDoc} */
    @Override
    public double sample() {
        return scale / Math.pow(rng.nextDouble(), 1 / shape);
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return "[Inverse method for Pareto distribution " + rng.toString() + "]";
    }
}
