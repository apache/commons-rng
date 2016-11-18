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
 * Box-Muller algorithm</a> for sampling from a Log normal distribution.
 */
public class BoxMullerLogNormalSampler
    extends SamplerBase
    implements ContinuousSampler {
    /** Scale. */
    private final double scale;
    /** Shape. */
    private final double shape;
    /** Gaussian sampling. */
    private final BoxMullerGaussianSampler gaussian;

    /**
     * @param rng Generator of uniformly distributed random numbers.
     * @param scale Scale of the Log normal distribution.
     * @param shape Shape of the Log normal distribution.
     */
    public BoxMullerLogNormalSampler(UniformRandomProvider rng,
                                     double scale,
                                     double shape) {
        super(null); // Not used.
        this.scale = scale;
        this.shape = shape;
        gaussian = new BoxMullerGaussianSampler(rng, 0, 1);
    }

    /** {@inheritDoc} */
    @Override
    public double sample() {
        return Math.exp(scale + shape * gaussian.sample());
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return "Box-Muller Log Normal [" + gaussian.toString() + "]";
    }
}
