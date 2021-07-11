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
 * Marsaglia and Tsang "Ziggurat" method</a> for sampling from a Gaussian
 * distribution with mean 0 and standard deviation 1.
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
 * @since 1.1
 */
public class ZigguratNormalizedGaussianSampler
    implements NormalizedGaussianSampler, SharedStateContinuousSampler {
    /** Start of tail. */
    private static final double R = 3.6541528853610088;
    /** Inverse of R. */
    private static final double ONE_OVER_R = 1 / R;
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
        final double v = 0.00492867323399;
        // Direction support uses the sign bit so the maximum magnitude from the long is 2^63
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
            d = Math.sqrt(-2 * Math.log(v / d + fd));
            fd = pdf(d);

            K[i + 1] = (long) ((d / t) * max);
            t = d;

            F[i] = fd;

            W[i] = d * oneOverMax;
        }
    }

    /**
     * @param rng Generator of uniformly distributed random numbers.
     */
    public ZigguratNormalizedGaussianSampler(UniformRandomProvider rng) {
        this.rng = rng;
    }

    /** {@inheritDoc} */
    @Override
    public double sample() {
        final long j = rng.nextLong();
        final int i = ((int) j) & LAST;
        if (Math.abs(j) < K[i]) {
            // This branch is called about 0.985086 times per sample.
            return j * W[i];
        }
        return fix(j, i);
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return "Ziggurat normalized Gaussian deviate [" + rng.toString() + "]";
    }

    /**
     * Gets the value from the tail of the distribution.
     *
     * @param hz Start random integer.
     * @param iz Index of cell corresponding to {@code hz}.
     * @return the requested random value.
     */
    private double fix(long hz,
                       int iz) {
        if (iz == 0) {
            // Base strip.
            // This branch is called about 2.55224E-4 times per sample.
            double y;
            double x;
            do {
                // Avoid infinity by creating a non-zero double.
                // Note: The extreme value y from -Math.log(2^-53) is (to 4 sf):
                // y = 36.74
                // The largest value x where 2y < x^2 is false is sqrt(2*36.74):
                // x = 8.571
                // The extreme tail is:
                // out = +/- 12.01
                // To generate this requires longs of 0 and then (1377 << 11).
                y = -Math.log(InternalUtils.makeNonZeroDouble(rng.nextLong()));
                x = -Math.log(InternalUtils.makeNonZeroDouble(rng.nextLong())) * ONE_OVER_R;
            } while (y + y < x * x);

            final double out = R + x;
            return hz > 0 ? out : -out;
        }
        // Wedge of other strips.
        // This branch is called about 0.0146584 times per sample.
        final double x = hz * W[iz];
        if (F[iz] + rng.nextDouble() * (F[iz - 1] - F[iz]) < pdf(x)) {
            // This branch is called about 0.00797887 times per sample.
            return x;
        }
        // Try again.
        // This branch is called about 0.00667957 times per sample.
        return sample();
    }

    /**
     * Compute the Gaussian probability density function {@code f(x) = e^-0.5x^2}.
     *
     * @param x Argument.
     * @return \( e^{-\frac{x^2}{2}} \)
     */
    private static double pdf(double x) {
        return Math.exp(-0.5 * x * x);
    }

    /**
     * {@inheritDoc}
     *
     * @since 1.3
     */
    @Override
    public SharedStateContinuousSampler withUniformRandomProvider(UniformRandomProvider rng) {
        return new ZigguratNormalizedGaussianSampler(rng);
    }

    /**
     * Create a new normalised Gaussian sampler.
     *
     * @param <S> Sampler type.
     * @param rng Generator of uniformly distributed random numbers.
     * @return the sampler
     * @since 1.3
     */
    @SuppressWarnings("unchecked")
    public static <S extends NormalizedGaussianSampler & SharedStateContinuousSampler> S
            of(UniformRandomProvider rng) {
        return (S) new ZigguratNormalizedGaussianSampler(rng);
    }
}
