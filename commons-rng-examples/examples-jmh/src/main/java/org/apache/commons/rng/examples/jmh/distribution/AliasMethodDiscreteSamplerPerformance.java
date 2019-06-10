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

package org.apache.commons.rng.examples.jmh.distribution;

import org.apache.commons.rng.UniformRandomProvider;
import org.apache.commons.rng.sampling.distribution.AliasMethodDiscreteSampler;
import org.apache.commons.rng.sampling.distribution.DiscreteSampler;
import org.apache.commons.rng.simple.RandomSource;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

import java.util.concurrent.TimeUnit;

/**
 * Executes benchmark to test the {@link AliasMethodDiscreteSampler} using different
 * zero padding on the input probability distribution.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@State(Scope.Benchmark)
@Fork(value = 1, jvmArgs = {"-server", "-Xms128M", "-Xmx128M"})
public class AliasMethodDiscreteSamplerPerformance {
    /**
     * The discrete probability distribution and a sampler. Used to test the sample speed and
     * construction speed of different sized distributions.
     */
    @State(Scope.Benchmark)
    public static class DistributionData {
        /**
         * The distribution size.
         */
        @Param({"7", "8", "9", "15", "16", "17", "31", "32", "33", "63", "64", "65"})
        private int size;

        /**
         * The sampler alpha parameter.
         */
        @Param({"-1", "0", "1", "2"})
        private int alpha;

        /** The probabilities. */
        private double[] probabilities;

        /** The sampler. */
        private DiscreteSampler sampler;

        /**
         * @return the alpha.
         */
        public int getAlpha() {
            return alpha;
        }

        /**
         * @return the probabilities.
         */
        public double[] getProbabilities() {
            return probabilities;
        }

        /**
         * @return the sampler.
         */
        public DiscreteSampler getSampler() {
            return sampler;
        }

        /** Create the distribution and sampler. */
        @Setup
        public void setup() {
            probabilities = createProbabilities(size);
            UniformRandomProvider rng = RandomSource.create(RandomSource.SPLIT_MIX_64);
            sampler = AliasMethodDiscreteSampler.create(rng, probabilities, alpha);
        }

        /**
         * Creates the probabilities for a discrete distribution of the given size.
         *
         * <p>This is not normalised to sum to 1. The sampler should handle this.</p>
         *
         * @param size Size of the distribution.
         * @return the probabilities
         */
        private static double[] createProbabilities(int size) {
            final double[] probabilities = new double[size];
            for (int i = 0; i < size; i++) {
                probabilities[i] = (i + 1.0) / size;
            }
            return probabilities;
        }
    }

    /**
     * The value.
     *
     * <p>This must NOT be final!</p>
     */
    private int value;

    // Benchmarks methods below.

    /**
     * Baseline for the JMH timing overhead for production of an {@code int} value.
     *
     * @return the {@code int} value
     */
    @Benchmark
    public int baseline() {
        return value;
    }

    /**
     * Run the sampler.
     *
     * @param data Contains the distribution sampler.
     * @return the sample value
     */
    @Benchmark
    public int sample(DistributionData data) {
        return data.getSampler().sample();
    }

    /**
     * Create the sampler.
     *
     * @param dist Contains the sampler constructor parameters.
     * @return the sampler
     */
    @Benchmark
    public Object createSampler(DistributionData dist) {
        // For the construction the RNG can be null
        return AliasMethodDiscreteSampler.create(null, dist.getProbabilities(), dist.getAlpha());
    }
}
