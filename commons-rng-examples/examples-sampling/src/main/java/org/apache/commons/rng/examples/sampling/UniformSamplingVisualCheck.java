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

package org.apache.commons.rng.examples.sampling;

import org.apache.commons.rng.UniformRandomProvider;
import org.apache.commons.rng.simple.RandomSource;
import org.apache.commons.rng.sampling.distribution.ZigguratNormalizedGaussianSampler;
import org.apache.commons.rng.sampling.distribution.MarsagliaNormalizedGaussianSampler;
import org.apache.commons.rng.sampling.distribution.BoxMullerNormalizedGaussianSampler;
import org.apache.commons.rng.sampling.distribution.ContinuousSampler;

/**
 * Creates 2D plot of sampling output.
 * It is a "manual" check that could help ensure that no artefacts 
 * exist in some tiny region of the expected range, due to loss of
 * accuracy, e.g. when porting C code based on 32-bits "float" to
 * "Commons RNG" that uses Java "double" (64-bits).
 */
public class UniformSamplingVisualCheck {
    /** RNG. */
    private final UniformRandomProvider rng = RandomSource.create(RandomSource.XOR_SHIFT_1024_S);
    /** Samplers. */
    private final ContinuousSampler[] samplers = new ContinuousSampler[] {
        new ZigguratNormalizedGaussianSampler(rng),
        new MarsagliaNormalizedGaussianSampler(rng),
        new BoxMullerNormalizedGaussianSampler(rng),
    };

    /**
     * Program entry point.
     *
     * @param args Unused.
     */
    public static void main(String[] args) {
        final float lo = 0.1f;
        final int bands = 2;
        float hi = lo;
        for (int i = 0; i < bands; i++) {
            hi = Math.nextUp(hi);
        }
        System.out.printf("# lo=%.10e hi=%.10e", lo, hi);
        System.out.println();

        final UniformSamplingVisualCheck app = new UniformSamplingVisualCheck();

        while (true) {
            System.out.printf("%.16e\t", app.rng.nextDouble());

            for (ContinuousSampler s : app.samplers) {
                while (true) {
                    final double r = s.sample();
                    if (r < lo ||
                        r > hi) {
                        // Discard numbers outside the tiny region.
                         continue;
                     }

                    System.out.printf("%.16e ", r);
                    break;
                }
            }

            System.out.println();
        }
    }
}
