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

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.infra.Blackhole;
import java.util.concurrent.TimeUnit;

import org.apache.commons.rng.UniformRandomProvider;
import org.apache.commons.rng.simple.RandomSource;
import org.apache.commons.rng.sampling.distribution.ContinuousSampler;
import org.apache.commons.rng.sampling.distribution.DiscreteSampler;
import org.apache.commons.rng.sampling.distribution.BoxMullerNormalizedGaussianSampler;
import org.apache.commons.rng.sampling.distribution.MarsagliaNormalizedGaussianSampler;
import org.apache.commons.rng.sampling.distribution.ZigguratNormalizedGaussianSampler;
import org.apache.commons.rng.sampling.distribution.AhrensDieterExponentialSampler;
import org.apache.commons.rng.sampling.distribution.AhrensDieterMarsagliaTsangGammaSampler;
import org.apache.commons.rng.sampling.distribution.LogNormalSampler;
import org.apache.commons.rng.sampling.distribution.ChengBetaSampler;
import org.apache.commons.rng.sampling.distribution.ContinuousUniformSampler;
import org.apache.commons.rng.sampling.distribution.DiscreteUniformSampler;
import org.apache.commons.rng.sampling.distribution.RejectionInversionZipfSampler;
import org.apache.commons.rng.sampling.distribution.PoissonSampler;

/**
 * Executes benchmark to compare the speed of generation of random numbers
 * from the various source providers.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@State(Scope.Benchmark)
@Fork(value = 1, jvmArgs = {"-server", "-Xms128M", "-Xmx128M"})
public class SamplersPerformance {
    /** Number of samples per run. */
    private static final int NUM_SAMPLES = 10000000;

    /**
     * The benchmark state (retrieve the various "RandomSource"s).
     */
    @State(Scope.Benchmark)
    public static class Sources {
        /**
         * RNG providers.
         */
        @Param({"JDK",
                "WELL_512_A",
                "WELL_1024_A",
                "WELL_19937_A",
                "WELL_19937_C",
                "WELL_44497_A",
                "WELL_44497_B",
                "MT",
                "ISAAC",
                "SPLIT_MIX_64",
                "MWC_256",
                "KISS",
                "XOR_SHIFT_1024_S",
                "TWO_CMRES",
                "MT_64" })
        private String randomSourceName;

        /** RNG. */
        private UniformRandomProvider generator;

        /**
         * @return the RNG.
         */
        public UniformRandomProvider getGenerator() {
            return generator;
        }

        /** Intantiates generator. */
        @Setup
        public void setup() {
            final RandomSource randomSource = RandomSource.valueOf(randomSourceName);
            generator = RandomSource.create(randomSource);
        }
    }

    /**
     * Exercises a continuous sampler.
     *
     * @param sampler Sampler.
     * @param bh Data sink.
     */
    private void runSample(ContinuousSampler sampler,
                           Blackhole bh) {
        for (int i = 0; i < NUM_SAMPLES; i++) {
            bh.consume(sampler.sample());
        }
    }

    /**
     * Exercises a discrete sampler.
     *
     * @param sampler Sampler.
     * @param bh Data sink.
     */
    private void runSample(DiscreteSampler sampler,
                           Blackhole bh) {
        for (int i = 0; i < NUM_SAMPLES; i++) {
            bh.consume(sampler.sample());
        }
    }

    // Benchmarks methods below.

    /**
     * @param sources Source of randomness.
     * @param bh Data sink.
     */
    @Benchmark
    public void runBoxMullerNormalizedGaussianSampler(Sources sources,
                                                      Blackhole bh) {
        runSample(new BoxMullerNormalizedGaussianSampler(sources.getGenerator()), bh);
    }

    /**
     * @param sources Source of randomness.
     * @param bh Data sink.
     */
    @Benchmark
    public void runMarsagliaNormalizedGaussianSampler(Sources sources,
                                                      Blackhole bh) {
        runSample(new MarsagliaNormalizedGaussianSampler(sources.getGenerator()), bh);
    }

    /**
     * @param sources Source of randomness.
     * @param bh Data sink.
     */
    @Benchmark
    public void runZigguratNormalizedGaussianSampler(Sources sources,
                                                     Blackhole bh) {
        runSample(new ZigguratNormalizedGaussianSampler(sources.getGenerator()), bh);
    }

    /**
     * @param sources Source of randomness.
     * @param bh Data sink.
     */
    @Benchmark
    public void runAhrensDieterExponentialSampler(Sources sources,
                                                  Blackhole bh) {
        runSample(new AhrensDieterExponentialSampler(sources.getGenerator(), 4.56), bh);
    }

    /**
     * @param sources Source of randomness.
     * @param bh Data sink.
     */
    @Benchmark
    public void runAhrensDieterMarsagliaTsangGammaSampler(Sources sources,
                                                          Blackhole bh) {
        runSample(new AhrensDieterMarsagliaTsangGammaSampler(sources.getGenerator(), 9.8, 0.76), bh);
    }

    /**
     * @param sources Source of randomness.
     * @param bh Data sink.
     */
    @Benchmark
    public void runBoxMullerLogNormalSampler(Sources sources,
                                             Blackhole bh) {
        runSample(new LogNormalSampler(new BoxMullerNormalizedGaussianSampler(sources.getGenerator()), 12.3, 4.6), bh);
    }

    /**
     * @param sources Source of randomness.
     * @param bh Data sink.
     */
    @Benchmark
    public void runMarsagliaLogNormalSampler(Sources sources,
                                             Blackhole bh) {
        runSample(new LogNormalSampler(new MarsagliaNormalizedGaussianSampler(sources.getGenerator()), 12.3, 4.6), bh);
    }

    /**
     * @param sources Source of randomness.
     * @param bh Data sink.
     */
    @Benchmark
    public void runZigguratLogNormalSampler(Sources sources,
                                            Blackhole bh) {
        runSample(new LogNormalSampler(new ZigguratNormalizedGaussianSampler(sources.getGenerator()), 12.3, 4.6), bh);
    }

    /**
     * @param sources Source of randomness.
     * @param bh Data sink.
     */
    @Benchmark
    public void runChengBetaSampler(Sources sources,
                                    Blackhole bh) {
        runSample(new ChengBetaSampler(sources.getGenerator(), 0.45, 6.7), bh);
    }

    /**
     * @param sources Source of randomness.
     * @param bh Data sink.
     */
    @Benchmark
    public void runContinuousUniformSampler(Sources sources,
                                            Blackhole bh) {
        runSample(new ContinuousUniformSampler(sources.getGenerator(), 123.4, 5678.9), bh);
    }

    /**
     * @param sources Source of randomness.
     * @param bh Data sink.
     */
    @Benchmark
    public void runDiscreteUniformSampler(Sources sources,
                                          Blackhole bh) {
        runSample(new DiscreteUniformSampler(sources.getGenerator(), -98, 76), bh);
    }

    /**
     * @param sources Source of randomness.
     * @param bh Data sink.
     */
    @Benchmark
    public void runRejectionInversionZipfSampler(Sources sources,
                                                 Blackhole bh) {
        runSample(new RejectionInversionZipfSampler(sources.getGenerator(), 43, 2.1), bh);
    }

    /**
     * @param sources Source of randomness.
     * @param bh Data sink.
     */
    @Benchmark
    public void runPoissonSampler(Sources sources,
                                  Blackhole bh) {
        runSample(new PoissonSampler(sources.getGenerator(), 8.9), bh);
    }
}
