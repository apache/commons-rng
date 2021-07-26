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

package org.apache.commons.rng.examples.jmh.sampling.distribution;

import org.apache.commons.rng.UniformRandomProvider;
import org.apache.commons.rng.examples.jmh.RandomSources;
import org.apache.commons.rng.sampling.distribution.BoxMullerNormalizedGaussianSampler;
import org.apache.commons.rng.sampling.distribution.MarsagliaNormalizedGaussianSampler;
import org.apache.commons.rng.sampling.distribution.NormalizedGaussianSampler;
import org.apache.commons.rng.sampling.distribution.ZigguratNormalizedGaussianSampler;
import org.apache.commons.rng.sampling.distribution.ZigguratSampler;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import java.util.concurrent.TimeUnit;
import java.util.Random;

/**
 * Benchmark to compare the speed of generation of normally-distributed random
 * numbers of {@link Random#nextGaussian()} against implementations of
 * {@link NormalizedGaussianSampler}.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@State(Scope.Benchmark)
@Fork(value = 1, jvmArgs = {"-server", "-Xms128M", "-Xmx128M"})
public class NextGaussianPerformance {
    /**
     * The value.
     *
     * <p>This must NOT be final!</p>
     */
    private double value;

    /**
     * The {@link Random} to use for testing.
     * This uses a non-final instance created for the benchmark to make a fair comparison
     * to the other Gaussian samplers by using the same setup and sampler access.
     */
    @State(Scope.Benchmark)
    public static class JDKSource {
        /** JDK's generator. */
        private Random random;

        /**
         * @return the sampler.
         */
        public Random getSampler() {
            return random;
        }

        /** Instantiates sampler. */
        @Setup
        public void setup() {
            random = new Random();
        }
    }

    /**
     * The {@link NormalizedGaussianSampler} samplers to use for testing. Creates the sampler for each
     * {@link org.apache.commons.rng.simple.RandomSource RandomSource} in the default
     * {@link RandomSources}.
     */
    @State(Scope.Benchmark)
    public static class Sources extends RandomSources {
        /** The sampler type. */
        @Param({"BoxMullerNormalizedGaussianSampler",
                "MarsagliaNormalizedGaussianSampler",
                "ZigguratNormalizedGaussianSampler",
                "ZigguratSampler.NormalizedGaussian"})
        private String samplerType;

        /** The sampler. */
        private NormalizedGaussianSampler sampler;

        /**
         * @return the sampler.
         */
        public NormalizedGaussianSampler getSampler() {
            return sampler;
        }

        /** Instantiates sampler. */
        @Override
        @Setup
        public void setup() {
            super.setup();
            final UniformRandomProvider rng = getGenerator();
            if ("BoxMullerNormalizedGaussianSampler".equals(samplerType)) {
                sampler = BoxMullerNormalizedGaussianSampler.of(rng);
            } else if ("MarsagliaNormalizedGaussianSampler".equals(samplerType)) {
                sampler = MarsagliaNormalizedGaussianSampler.of(rng);
            } else if ("ZigguratNormalizedGaussianSampler".equals(samplerType)) {
                sampler = ZigguratNormalizedGaussianSampler.of(rng);
            } else if ("ZigguratSampler.NormalizedGaussian".equals(samplerType)) {
                sampler = ZigguratSampler.NormalizedGaussian.of(rng);
            } else {
                throw new IllegalStateException("Unknown sampler type: " + samplerType);
            }
        }
    }

    /**
     * Baseline for the JMH timing overhead for production of an {@code double} value.
     *
     * @return the {@code double} value
     */
    @Benchmark
    public double baseline() {
        return value;
    }

    /**
     * Run JDK {@link Random} gaussian sampler.
     *
     * @param source Source of randomness.
     * @return the sample value
     */
    @Benchmark
    public double sampleJDK(JDKSource source) {
        return source.getSampler().nextGaussian();
    }

    /**
     * Run the {@link NormalizedGaussianSampler} sampler.
     *
     * @param sources Source of randomness.
     * @return the sample value
     */
    @Benchmark
    public double sample(Sources sources) {
        return sources.getSampler().sample();
    }
}
