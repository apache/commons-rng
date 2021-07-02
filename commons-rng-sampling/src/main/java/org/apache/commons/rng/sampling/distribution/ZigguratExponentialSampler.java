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
 * <a href="https://en.wikipedia.org/wiki/Ziggurat_algorithm">
 * Marsaglia and Tsang "Ziggurat" method</a> for sampling from an exponential
 * distribution.
 *
 * <p>The algorithm is explained in this
 * <a href="http://www.jstatsoft.org/article/view/v005i08/ziggurat.pdf">paper</a>
 * and this implementation has been adapted from the C code provided therein.</p>
 *
 * <p>Sampling uses:</p>
 *
 * <ul>
 *   <li>{@link UniformRandomProvider#nextLong()}
 *   <li>{@link UniformRandomProvider#nextDouble()}
 * </ul>
 *
 * @since 1.4
 */
public class ZigguratExponentialSampler implements SharedStateContinuousSampler {
    /** Start of tail. */
    private static final double R = 7.69711747013104972;
    /** Index of last entry in the tables (which have a size that is a power of 2). */
    private static final int LAST = 255;
    /** Auxiliary table. */
    private static final long[] K;
    /** Auxiliary table. */
    private static final double[] W;
    /** Auxiliary table. */
    private static final double[] F;

    /** Underlying source of randomness. */
    private final UniformRandomProvider rng;

    static {
        // Filling the tables.
        // Rectangle area.
        final double v = 0.0039496598225815571993;
        // No support for unsigned long so the upper bound is 2^63
        final double max = Math.pow(2, 63);
        final double oneOverMax = 1d / max;

        K = new long[LAST + 1];
        W = new double[LAST + 1];
        F = new double[LAST + 1];

        double d = R;
        double t = d;
        double fd = pdf(d);
        final double q = v / fd;

        K[0] = (long) ((d / q) * max);
        K[1] = 0;

        W[0] = q * oneOverMax;
        W[LAST] = d * oneOverMax;

        F[0] = 1;
        F[LAST] = fd;

        for (int i = LAST - 1; i >= 1; i--) {
            d = -Math.log(v / d + fd);
            fd = pdf(d);

            K[i + 1] = (long) ((d / t) * max);
            t = d;

            F[i] = fd;

            W[i] = d * oneOverMax;
        }
    }

    /**
     * Specialisation of the ZigguratExponentialSampler which multiplies the standard
     * exponential result by the mean.
     */
    private static class ZigguratExponentialMeanSampler extends ZigguratExponentialSampler {
        /** Mean. */
        private final double mean;

        /**
         * @param rng Generator of uniformly distributed random numbers.
         * @param mean Mean.
         */
        ZigguratExponentialMeanSampler(UniformRandomProvider rng, double mean) {
            super(rng);
            this.mean = mean;
        }

        @Override
        public double sample() {
            return createSample() * mean;
        }

        @Override
        public ZigguratExponentialMeanSampler withUniformRandomProvider(UniformRandomProvider rng) {
            return new ZigguratExponentialMeanSampler(rng, this.mean);
        }
    }

    /**
     * @param rng Generator of uniformly distributed random numbers.
     */
    private ZigguratExponentialSampler(UniformRandomProvider rng) {
        this.rng = rng;
    }

    /** {@inheritDoc} */
    @Override
    public double sample() {
        return createSample();
    }

    /**
     * Creates the exponential sample with {@code mean = 1}.
     *
     * <p>Note: This has been extracted to a separate method so that the recursive call
     * when sampling tries again targets this function. Otherwise the sub-class
     * {@code ZigguratExponentialMeanSampler.sample()} method will recursively call
     * the overloaded sample() method when trying again which creates a bad sample due
     * to compound multiplication of the mean.
     *
     * @return the sample
     */
    final double createSample() {
        // An unsigned long in [0, 2^63)
        final long j = rng.nextLong() >>> 1;
        final int i = ((int) j) & LAST;
        if (j < K[i]) {
            // This branch is called about 0.977777 times per call into createSample.
            // Note: Frequencies have been empirically measured for the first call to
            // createSample; recursion due to retries have been ignored. Frequencies sum to 1.
            return j * W[i];
        }
        return fix(j, i);
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return "Ziggurat exponential deviate [" + rng.toString() + "]";
    }

    /**
     * Gets the value from the tail of the distribution.
     *
     * @param jz Start random integer.
     * @param iz Index of cell corresponding to {@code jz}.
     * @return the requested random value.
     */
    private double fix(long jz,
                       int iz) {
        if (iz == 0) {
            // Base strip.
            // This branch is called about 0.000448867 times per call into createSample.
            return R - Math.log(rng.nextDouble());
        }
        // Wedge of other strips.
        final double x = jz * W[iz];
        if (F[iz] + rng.nextDouble() * (F[iz - 1] - F[iz]) < pdf(x)) {
            // This branch is called about 0.0107820 times per call into createSample.
            return x;
        }
        // Try again.
        // This branch is called about 0.0109920 times per call into createSample
        // i.e. this is the recursion frequency.
        return createSample();
    }

    /**
     * Compute the exponential probability density function {@code f(x) = e^-x}.
     *
     * @param x Argument.
     * @return {@code e^-x}
     */
    private static double pdf(double x) {
        return Math.exp(-x);
    }

    /** {@inheritDoc} */
    @Override
    public ZigguratExponentialSampler withUniformRandomProvider(UniformRandomProvider rng) {
        return new ZigguratExponentialSampler(rng);
    }

    /**
     * Create a new exponential sampler with {@code mean = 1}.
     *
     * @param rng Generator of uniformly distributed random numbers.
     * @return the sampler
     */
    public static ZigguratExponentialSampler of(UniformRandomProvider rng) {
        return new ZigguratExponentialSampler(rng);
    }

    /**
     * Create a new exponential sampler with the specified {@code mean}.
     *
     * @param rng Generator of uniformly distributed random numbers.
     * @param mean Mean.
     * @return the sampler
     * @throws IllegalArgumentException if the mean is not strictly positive ({@code mean <= 0})
     */
    public static ZigguratExponentialSampler of(UniformRandomProvider rng, double mean) {
        if (mean > 0) {
            return new ZigguratExponentialMeanSampler(rng, mean);
        }
        throw new IllegalArgumentException("Mean is not strictly positive: " + mean);
    }
}
