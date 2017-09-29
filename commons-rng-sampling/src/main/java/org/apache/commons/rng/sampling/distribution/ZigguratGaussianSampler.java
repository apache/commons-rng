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
 * Gaussian Sampling by Ziggurat algorithm: https://en.wikipedia.org/wiki/Ziggurat_algorithm.
 *
 * based on
 * The Ziggurat Method for Generating Random Variables
 * by George Marsaglia and Wai Wan Tsang
 *
 * @since 1.0
 */

public class ZigguratGaussianSampler
    extends SamplerBase
    implements NormalizedGaussianSampler {

    /**
     * Generates values from Gaussian (normal) probability distribution
     */

    private static final int[] KN = new int[128];
    private static final double[] WN = new double[128];
    private static final double[] FN = new double[128];

    /**
     * Initialize tables.
     */
    static {
        /**
         * Filling the tables.
         */
        final double m = 2147483648.0;

        double dn=3.442619855899;
        double tn=dn;
        double vn=9.91256303526217e-3;
        double q=vn/Math.exp(-.5*dn*dn);

        KN[0]= (int) ((dn/q)*m);
        KN[1]= 0;

        WN[0]= q/m;
        WN[127]= dn/m;

        FN[0]= 1.0;
        FN[127]= Math.exp(-.5*dn*dn);

        for (int i=126; i>=1; i--){
            dn=Math.sqrt(-2.*Math.log(vn/dn+Math.exp(-.5*dn*dn)));
            KN[i+1]= (int) ((dn/tn)*m);
            tn=dn;
            FN[i]= Math.exp(-.5*dn*dn);
            WN[i]= (dn/m);
        }
    }

    public ZigguratGaussianSampler(UniformRandomProvider rng) {
        super(rng);
    }

    @Override
    public double sample() {
        int j=nextInt();
        int i=j&127;
        return (j < KN[i]) ? j*WN[i] : nfix(j,i);
    }

    private double nfix(int hz, int iz) {
        /* The start of the right tail */
        double r = 3.442619855899;

        double uni;
        double x;
        double y;

        while (true) {
            uni = .5 + hz * .2328306e-9;
            x = hz * WN[iz];
            /* iz==0, handles the base strip */
            if (iz == 0) {
                /* return x from the tail */
                do {
                    x = -Math.log(uni) * 0.2904764;
                    y = -Math.log(uni);
                    uni = .5 + nextInt() * .2328306e-9;
                } while (y + y < x * x);
                return (hz > 0) ? r + x : -r - x;
            }
            /* iz>0, handle the wedges of other strips */
            if (FN[iz] + uni * (FN[iz - 1] - FN[iz]) < Math.exp(-.5 * x * x)) {
                return x;
            }
            hz=nextInt();
            iz=hz&127;
            if (Math.abs(hz) < KN[iz]) {
                return (hz * WN[iz]);
            }
        }
    }

    @Override
    public String toString() {
        return "Ziggurat Gaussian deviate [" + super.toString() + "]";
    }

}
