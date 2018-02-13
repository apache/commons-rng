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

package org.apache.commons.rng.examples.quadrature;

import org.apache.commons.rng.UniformRandomProvider;
import org.apache.commons.rng.simple.RandomSource;

/**
 * <a href="https://en.wikipedia.org/wiki/Monte_Carlo_integration">Monte-Carlo method</a>
 * for approximating an integral on a n-dimensional unit cube.
 */
public abstract class MonteCarloIntegration {
    /** RNG. */
    private final UniformRandomProvider rng;
    /** Integration domain dimension. */
    private final int dimension;

    /**
     * Simulation constructor.
     *
     * @param source RNG algorithm.
     * @param dimension Integration domain dimension.
     */
    public MonteCarloIntegration(RandomSource source,
                                 int dimension) {
        this.rng = RandomSource.create(source);
        this.dimension = dimension;
    }

    /**
     * Run the Monte-Carlo integration.
     *
     * @param n Number of random points to generate.
     * @return the integral.
     */
    public double integrate(long n) {
        double result = 0;
        long inside = 0;
        long total = 0;
        while (total < n) {
            if (isInside(generateU01())) {
                ++inside;
            }

            ++total;
            result = inside / (double) total;
        }

        return result;
    }

    /**
     * Indicates whether the given points is inside the region whose
     * integral is computed.
     *
     * @param point Point whose coordinates are random numbers uniformly
     * distributed in the unit interval.
     * @return {@code true} if the {@code point} is inside.
     */
    protected abstract boolean isInside(double ... point);

    /**
     * @return a value from a random sequence uniformly distributed
     * in the {@code [0, 1)} interval.
     */
    private double[] generateU01() {
        final double[] rand = new double[dimension];

        for (int i = 0; i < dimension; i++) {
            rand[i] = rng.nextDouble();
        }

        return rand;
    }
}
