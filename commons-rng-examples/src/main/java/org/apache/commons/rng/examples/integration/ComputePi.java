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
package org.apache.commons.rng.examples.integration;

import org.apache.commons.rng.simple.RandomSource;

/**
 * Computation of \( \pi \) using Monte-Carlo integration.
 *
 * The computation estimates the value by computing the probability that
 * a point \( p = (x, y) \) will lie in the circle of radius \( r = 1 \)
 * inscribed in the square of side \( r = 1 \).
 * The probability could be computed by \[ area_{circle} / area_{square} \],
 * where \( area_{circle} = \pi * r^2 \) and \( area_{square} = 4 r^2 \).
 * Hence, the probability is \( \frac{\pi}{4} \).
 *
 * The Monte Carlo simulation will produce \( N \) points.
 * Defining \( N_c \) as the number of point that satisfy \( x^2 + y^2 \le 1 \),
 * we will have \( \frac{N_c}{N} \approx \frac{\pi}{4} \).
 */
public class ComputePi extends MonteCarloIntegration {
    /** Domain dimension. */
    private static final int DIMENSION = 2;

    /**
     * @param source RNG algorithm.
     */
    public ComputePi(RandomSource source) {
        super(source, DIMENSION);
    }

    /**
     * Program entry point.
     *
     * @param args Arguments.
     * The order is as follows:
     * <ol>
     *  <li>
     *   Number of random 2-dimensional points to generate.
     *  </li>
     *  <li>
     *   {@link RandomSource Random source identifier}.
     *  </li>
     * </ol>
     */
    public static void main(String[] args) {
        if (args.length != 2) {
            throw new IllegalStateException("Missing arguments");
        }

        final long numPoints = Long.valueOf(args[0]);
        final RandomSource randomSource = RandomSource.valueOf(args[1]);

        final ComputePi piApp = new ComputePi(randomSource);
        final double piMC = piApp.compute(numPoints);

        //CHECKSTYLE: stop all
        System.out.println("After generating " + (DIMENSION * numPoints) +
                           " random numbers, the error on 𝛑 is " + Math.abs(piMC - Math.PI));
        //CHECKSTYLE: resume all
    }

    /**
     * @param numPoints Number of random points to generate.
     * @return the approximate value of pi.
     */
    public double compute(long numPoints) {
        return 4 * integrate(numPoints);
    }

    /** {@inheritDoc} */
    @Override
    protected boolean isInside(double ... rand) {
        final double r2 = rand[0] * rand[0] + rand[1] * rand[1];
        return r2 <= 1;
    }
}
