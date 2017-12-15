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
 * <p>
 * Implementation is based on this <a href="http://www.jstatsoft.org/article/view/v005i08/ziggurat.pdf">paper</a>
 * </p>
 *
 * @since 1.1
 */
public class ZigguratNormalizedGaussianSampler
    extends SamplerBase
    implements NormalizedGaussianSampler {

     // Generates values from Gaussian (normal) probability distribution
     // It uses two tables, integers KN and reals WN. Some 99% of the time,
     // the required x is produced by:
     // generate a random 32-bit integer j and let i be the index formed from
     // the rightmost 8 bits of j. If j < k_i return x = j * w_i.

    /** Auxiliary table (see code) */
    private static final long[] KN = new long[128];
    /** Auxiliary table (see code) */
    private static final double[] WN = new double[128];
    /** Auxiliary table (see code) */
    private static final double[] FN = new double[128];

    static {
         // Filling the tables.
         //  k_0 = 2^32 * r * f(dn) / vn
         //  k_i = 2^32 * ( x_{i-1} / x_i )
         //  w_0 = 0.5^32 * vn / f(dn)
         //  w_i = 0.5^32 * x_i
         //  f(dn) = exp(-0.5 * dn * dn)
         // where "dn" is the rightmost x_i
         //       "vn" is the area of the rectangle
        final double m = 2147483648.0; // 2^31.

        // Provides z(r) = 0, where z(r): x_255 = r, vn = r * f(r) + integral_r^inf f(x) dx
        final double vn = 9.91256303526217e-3;

        double dn = 3.442619855899;
        double tn = dn;
        double e = Math.exp(-0.5 * dn * dn);
        final double q = vn / e;

        KN[0] = (long) ((dn / q) * m);
        KN[1] = 0;

        WN[0] = q / m;
        WN[127] = dn / m;

        FN[0] = 1;
        FN[127] = e;

        for (int i = 126; i >= 1; i--){
            e = Math.exp(-0.5 * dn * dn);
            dn = Math.sqrt(-2 * Math.log(vn / dn + e));
            KN[i+1] = (long) ((dn / tn) * m);
            tn = dn;
            FN[i] = e;
            WN[i] = dn / m;
        }
    }

    /**
     * @param rng Generator of uniformly distributed random numbers.
     */
    public ZigguratNormalizedGaussianSampler(UniformRandomProvider rng) {
        super(rng);
    }

    /** {@inheritDoc} */
    @Override
    public double sample() {
        final int j = nextInt();
        final int i = j & 127;
        return (j < KN[i]) ? j * WN[i] : nfix(j, i);
    }

    /**
     * Gets the value from the tail of the distribution.
     *
     * @param hz Start random integer.
     * @param iz Corresponding to hz cell's number.
     * @return the requested random value.
     */
    private double nfix(int hz,
                        int iz) {
        // The start of the right tail.
        final double r = 3.442619855899;

        double uni;
        double x;
        double y;

        while (true) {
            uni = 0.5 + hz * 0.2328306e-9;
            x = hz * WN[iz];
            // iz == 0 handles the base strip.
            if (iz == 0) {
                // Return x from the tail.
                do {
                    y = -Math.log(uni);
                    x = y * 0.2904764;
                    uni = 0.5 + nextLong() * 0.2328306e-9;
                } while (y + y < x * x);
                return (hz > 0) ? r + x : -r - x;
            }
            // iz > 0 handles the wedges of other strips.
            if (FN[iz] + uni * (FN[iz - 1] - FN[iz]) < Math.exp(-0.5 * x * x)) {
                return x;
            }
            hz = nextInt();
            iz = hz & 127;
            if (Math.abs(hz) < KN[iz]) {
                return hz * WN[iz];
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return "Ziggurat normalized Gaussian deviate [" + super.toString() + "]";
    }
}
