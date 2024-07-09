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
 * Distribution sampler that uses the
 * <a href="https://en.wikipedia.org/wiki/Inverse_transform_sampling">
 * inversion method</a>.
 *
 * It can be used to sample any distribution that provides access to its
 * <em>inverse cumulative probability function</em>.
 *
 * <p>Sampling uses {@link UniformRandomProvider#nextDouble()}.</p>
 *
 * <p>Example:</p>
 * <pre><code>
 * import org.apache.commons.math3.distribution.IntegerDistribution;
 * import org.apache.commons.math3.distribution.BinomialDistribution;
 *
 * import org.apache.commons.rng.simple.RandomSource;
 * import org.apache.commons.rng.sampling.distribution.DiscreteSampler;
 * import org.apache.commons.rng.sampling.distribution.InverseTransformDiscreteSampler;
 * import org.apache.commons.rng.sampling.distribution.DiscreteInverseCumulativeProbabilityFunction;
 *
 * // Distribution to sample.
 * final IntegerDistribution dist = new BinomialDistribution(11, 0.56);
 * // Create the sampler.
 * final DiscreteSampler binomialSampler =
 *     InverseTransformDiscreteSampler.of(RandomSource.XO_RO_SHI_RO_128_PP.create(),
 *                                        new DiscreteInverseCumulativeProbabilityFunction() {
 *                                            public int inverseCumulativeProbability(double p) {
 *                                                return dist.inverseCumulativeProbability(p);
 *                                            }
 *                                        });
 *
 * // Generate random deviate.
 * int random = binomialSampler.sample();
 * </code></pre>
 *
 * @since 1.0
 */
public class InverseTransformDiscreteSampler
    extends SamplerBase
    implements SharedStateDiscreteSampler {
    /** Inverse cumulative probability function. */
    private final DiscreteInverseCumulativeProbabilityFunction function;
    /** Underlying source of randomness. */
    private final UniformRandomProvider rng;

    /**
     * Create an instance.
     *
     * @param rng Generator of uniformly distributed random numbers.
     * @param function Inverse cumulative probability function.
     */
    public InverseTransformDiscreteSampler(UniformRandomProvider rng,
                                           DiscreteInverseCumulativeProbabilityFunction function) {
        super(null);
        this.rng = rng;
        this.function = function;
    }

    /** {@inheritDoc} */
    @Override
    public int sample() {
        return function.inverseCumulativeProbability(rng.nextDouble());
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return function.toString() + " (inverse method) [" + rng.toString() + "]";
    }

    /**
     * {@inheritDoc}
     *
     * <p>Note: The new sampler will share the inverse cumulative probability function. This
     * must be suitable for concurrent use to ensure thread safety.</p>
     *
     * @since 1.3
     */
    @Override
    public SharedStateDiscreteSampler withUniformRandomProvider(UniformRandomProvider rng) {
        return new InverseTransformDiscreteSampler(rng, function);
    }

    /**
     * Create a new inverse-transform discrete sampler.
     *
     * <p>To use the sampler to
     * {@link org.apache.commons.rng.sampling.SharedStateSampler share state} the function must be
     * suitable for concurrent use.</p>
     *
     * @param rng Generator of uniformly distributed random numbers.
     * @param function Inverse cumulative probability function.
     * @return the sampler
     * @see #withUniformRandomProvider(UniformRandomProvider)
     * @since 1.3
     */
    public static SharedStateDiscreteSampler of(UniformRandomProvider rng,
                                                DiscreteInverseCumulativeProbabilityFunction function) {
        return new InverseTransformDiscreteSampler(rng, function);
    }
}
