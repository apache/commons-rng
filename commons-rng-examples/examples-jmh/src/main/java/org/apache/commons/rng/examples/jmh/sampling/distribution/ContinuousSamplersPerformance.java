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
import org.apache.commons.rng.sampling.distribution.AhrensDieterExponentialSampler;
import org.apache.commons.rng.sampling.distribution.AhrensDieterMarsagliaTsangGammaSampler;
import org.apache.commons.rng.sampling.distribution.BoxMullerNormalizedGaussianSampler;
import org.apache.commons.rng.sampling.distribution.ChengBetaSampler;
import org.apache.commons.rng.sampling.distribution.ContinuousSampler;
import org.apache.commons.rng.sampling.distribution.ContinuousUniformSampler;
import org.apache.commons.rng.sampling.distribution.InverseTransformParetoSampler;
import org.apache.commons.rng.sampling.distribution.LevySampler;
import org.apache.commons.rng.sampling.distribution.LogNormalSampler;
import org.apache.commons.rng.sampling.distribution.MarsagliaNormalizedGaussianSampler;
import org.apache.commons.rng.sampling.distribution.StableSampler;
import org.apache.commons.rng.sampling.distribution.ZigguratSampler;
import org.apache.commons.rng.sampling.distribution.ZigguratNormalizedGaussianSampler;

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
 * Executes benchmark to compare the speed of generation of random numbers
 * from the various source providers for different types of {@link ContinuousSampler}.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@State(Scope.Benchmark)
@Fork(value = 1, jvmArgs = {"-server", "-Xms128M", "-Xmx128M"})
public class ContinuousSamplersPerformance {
    /**
     * The value.
     *
     * <p>This must NOT be final!</p>
     */
    private double value;

    /**
     * The {@link ContinuousSampler} samplers to use for testing. Creates the sampler for each
     * {@link org.apache.commons.rng.simple.RandomSource RandomSource} in the default
     * {@link RandomSources}.
     */
    @State(Scope.Benchmark)
    public static class Sources extends RandomSources {
        /**
         * The sampler type.
         */
        @Param({"BoxMullerNormalizedGaussianSampler",
                "MarsagliaNormalizedGaussianSampler",
                "ZigguratNormalizedGaussianSampler",
                "ZigguratSampler.NormalizedGaussian",
                "AhrensDieterExponentialSampler",
                "ZigguratSampler.Exponential",
                "AhrensDieterGammaSampler",
                "MarsagliaTsangGammaSampler",
                "LevySampler",
                "LogNormalBoxMullerNormalizedGaussianSampler",
                "LogNormalMarsagliaNormalizedGaussianSampler",
                "LogNormalZigguratNormalizedGaussianSampler",
                "LogNormalSampler.ZigguratSampler.NormalizedGaussian",
                "ChengBetaSampler",
                "ContinuousUniformSampler",
                "InverseTransformParetoSampler",
                "StableSampler"})
        private String samplerType;

        /** The sampler. */
        private ContinuousSampler sampler;

        /**
         * @return the sampler.
         */
        public ContinuousSampler getSampler() {
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
            } else if ("AhrensDieterExponentialSampler".equals(samplerType)) {
                sampler = AhrensDieterExponentialSampler.of(rng, 4.56);
            } else if ("ZigguratSampler.Exponential".equals(samplerType)) {
                sampler = ZigguratSampler.Exponential.of(rng, 4.56);
            } else if ("AhrensDieterGammaSampler".equals(samplerType)) {
                // This tests the Ahrens-Dieter algorithm since alpha < 1
                sampler = AhrensDieterMarsagliaTsangGammaSampler.of(rng, 0.76, 9.8);
            } else if ("MarsagliaTsangGammaSampler".equals(samplerType)) {
                // This tests the Marsaglia-Tsang algorithm since alpha > 1
                sampler = AhrensDieterMarsagliaTsangGammaSampler.of(rng, 12.34, 9.8);
            } else if ("LevySampler".equals(samplerType)) {
                sampler = LevySampler.of(rng, 1.23, 4.56);
            } else if ("LogNormalBoxMullerNormalizedGaussianSampler".equals(samplerType)) {
                sampler = LogNormalSampler.of(BoxMullerNormalizedGaussianSampler.of(rng), 12.3, 4.6);
            } else if ("LogNormalMarsagliaNormalizedGaussianSampler".equals(samplerType)) {
                sampler = LogNormalSampler.of(MarsagliaNormalizedGaussianSampler.of(rng), 12.3, 4.6);
            } else if ("LogNormalZigguratNormalizedGaussianSampler".equals(samplerType)) {
                sampler = LogNormalSampler.of(ZigguratNormalizedGaussianSampler.of(rng), 12.3, 4.6);
            } else if ("LogNormalSampler.ZigguratSampler.NormalizedGaussian".equals(samplerType)) {
                sampler = LogNormalSampler.of(ZigguratSampler.NormalizedGaussian.of(rng), 12.3, 4.6);
            } else if ("ChengBetaSampler".equals(samplerType)) {
                sampler = ChengBetaSampler.of(rng, 0.45, 6.7);
            } else if ("ContinuousUniformSampler".equals(samplerType)) {
                sampler = ContinuousUniformSampler.of(rng, 123.4, 5678.9);
            } else if ("InverseTransformParetoSampler".equals(samplerType)) {
                sampler = InverseTransformParetoSampler.of(rng, 23.45, 0.1234);
            } else if ("StableSampler".equals(samplerType)) {
                sampler = StableSampler.of(rng, 1.3, 0.2);
            }
        }
    }

    // Benchmarks methods below.

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
     * Run the sampler.
     *
     * @param sources Source of randomness.
     * @return the sample value
     */
    @Benchmark
    public double sample(Sources sources) {
        return sources.getSampler().sample();
    }
}
