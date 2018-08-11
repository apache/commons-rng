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
 * Discrete uniform distribution sampler.
 *
 * @since 1.0
 */
public class DiscreteUniformSampler
    extends SamplerBase
    implements DiscreteSampler {
    /** Lower bound. */
    private final int lower;
    /** Upper bound. */
    private final int upper;
    /** Underlying source of randomness. */
    private final UniformRandomProvider rng;

    /**
     * @param rng Generator of uniformly distributed random numbers.
     * @param lower Lower bound (inclusive) of the distribution.
     * @param upper Upper bound (inclusive) of the distribution.
     * @throws IllegalArgumentException if {@code lower > upper}.
     */
    public DiscreteUniformSampler(UniformRandomProvider rng,
                                  int lower,
                                  int upper) {
        super(null);
        this.rng = rng;
        if (lower > upper) {
            throw new IllegalArgumentException(lower  + " > " + upper);
        }

        this.lower = lower;
        this.upper = upper;
    }

    /** {@inheritDoc} */
    @Override
    public int sample() {
        final int max = (upper - lower) + 1;
        if (max <= 0) {
            // The range is too wide to fit in a positive int (larger
            // than 2^31); as it covers more than half the integer range,
            // we use a simple rejection method.
            while (true) {
                final int r = rng.nextInt();
                if (r >= lower &&
                    r <= upper) {
                    return r;
                }
            }
        } else {
            // We can shift the range and directly generate a positive int.
            return lower + rng.nextInt(max);
        }
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return "Uniform deviate [" + rng.toString() + "]";
    }
}
