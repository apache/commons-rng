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

import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

import java.util.concurrent.Callable;

import org.apache.commons.rng.sampling.distribution.ZigguratNormalizedGaussianSampler;
import org.apache.commons.rng.sampling.distribution.MarsagliaNormalizedGaussianSampler;
import org.apache.commons.rng.sampling.distribution.BoxMullerNormalizedGaussianSampler;
import org.apache.commons.rng.sampling.distribution.ContinuousSampler;

/**
 * Creates 2D plot of sampling output.
 * It is a "manual" check that could help ensure that no artifacts
 * exist in some tiny region of the expected range, due to loss of
 * accuracy, e.g. when porting C code based on 32-bits "float" to
 * "Commons RNG" that uses Java "double" (64-bits).
 */
@Command(name = "visual",
         description = "Show output from a tiny region of the sampler.")
class UniformSamplingVisualCheckCommand implements Callable<Void> {
    /** The standard options. */
    @Mixin
    private StandardOptions reusableOptions;

    /** The lower bound of the tiny range. */
    @Option(names = {"-l", "--low"},
            description = "The lower bound (default: ${DEFAULT-VALUE}).")
    private float lo = 0.1f;

    /** The number of bands of the tiny range. */
    @Option(names = {"-b", "--bands"},
            description = "The number of bands for the range (default: ${DEFAULT-VALUE}).")
    private int bands = 2;

    /** Number of samples to be generated. */
    @Option(names = {"-s", "--samples"},
        description = "The number of samples in the tiny range (default: ${DEFAULT-VALUE}).")
    private int numSamples = 50;

    /** RNG. */
    private final UniformRandomProvider rng = RandomSource.create(RandomSource.XOR_SHIFT_1024_S_PHI);
    /** Samplers. */
    private final ContinuousSampler[] samplers = new ContinuousSampler[] {
        ZigguratNormalizedGaussianSampler.of(rng),
        MarsagliaNormalizedGaussianSampler.of(rng),
        BoxMullerNormalizedGaussianSampler.of(rng),
    };

    // Allow System.out
    // CHECKSTYLE: stop RegexpCheck

    /**
     * Prints a template generators list to stdout.
     */
    @Override
    public Void call() {
        float hi = lo;
        for (int i = 0; i < bands; i++) {
            hi = Math.nextUp(hi);
        }
        System.out.printf("# lower=%.16e%n", lo);
        System.out.printf("# upper=%.16e%n", hi);

        for (int i = 0; i < samplers.length; i++) {
            System.out.printf("# [%d] %s%n", i, samplers[i].getClass().getSimpleName());
        }

        int n = 0;
        while (++n < numSamples) {
            System.out.printf("[%d]", n, rng.nextDouble());

            for (ContinuousSampler s : samplers) {
                while (true) {
                    final double r = s.sample();
                    if (r < lo ||
                        r > hi) {
                        // Discard numbers outside the tiny region.
                        continue;
                    }

                    System.out.printf("\t%.16e", r);
                    break;
                }
            }

            System.out.println();
        }

        return null;
    }
}
