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

package org.apache.commons.rng.examples.jmh;

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
import org.apache.commons.rng.simple.CachedUniformRandomProviderFactory;
import org.apache.commons.rng.simple.RandomSource;
import org.apache.commons.rng.core.source32.IntProvider;
import org.apache.commons.rng.core.source64.LongProvider;

/**
 * Executes benchmark to compare the speed of generation of random numbers
 * from the various source providers if wrapped with a cached provider.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@State(Scope.Benchmark)
@Fork(value = 1, jvmArgs = {"-server", "-Xms128M", "-Xmx128M"})
public class CachedGenerationPerformance {
    /** Number of samples per run. */
    private static final int NUM_SAMPLES = 1000000;

    /**
     * Retrieve the various "RandomSource"s for testing.
     *
     * <p>All source use an int source of randomness (see {@link IntProvider}).
     */
    @State(Scope.Benchmark)
    public static class SourcesInt {
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
                "MWC_256",
                "KISS"})
        private String randomSourceName;

        /** RNG. */
        private UniformRandomProvider provider;

        /**
         * @return the RNG.
         */
        public UniformRandomProvider getGenerator() {
            return provider;
        }

        /** Instantiates generator. */
        @Setup
        public void setup() {
            final RandomSource randomSource = RandomSource.valueOf(randomSourceName);
            provider = RandomSource.create(randomSource);
        }
    }

    /**
     * The benchmark state (retrieve the various "RandomSource"s).
      *
     * <p>All source use a long source of randomness (see {@link LongProvider}).
     */
    @State(Scope.Benchmark)
    public static class SourcesLong {
        /**
         * RNG providers.
         */
        @Param({"SPLIT_MIX_64",
                "XOR_SHIFT_1024_S",
                "TWO_CMRES",
                "MT_64"})
        private String randomSourceName;

        /** RNG. */
        private UniformRandomProvider provider;

        /**
         * @return the RNG.
         */
        public UniformRandomProvider getGenerator() {
            return provider;
        }

        /** Instantiates generator. */
        @Setup
        public void setup() {
            final RandomSource randomSource = RandomSource.valueOf(randomSourceName);
            provider = RandomSource.create(randomSource);
        }
    }

    /**
     * Flag to indicate the provider should be wrapped with a cache.
     */
    @Param({"false", "true"})
    private boolean cache;

    /**
     * Exercises {@link UniformRandomProvider#nextBoolean()}.
     *
     * @param sampler Sampler.
     * @param bh Data sink.
     */
    private static void runNextBoolean(UniformRandomProvider rng,
                                       Blackhole bh) {
        for (int i = 0; i < NUM_SAMPLES; i++) {
            bh.consume(rng.nextBoolean());
        }
    }

    /**
     * Exercises {@link UniformRandomProvider#nextInt()}.
     *
     * @param sampler Sampler.
     * @param bh Data sink.
     */
    private static void runNextInt(UniformRandomProvider rng,
                                   Blackhole bh) {
        for (int i = 0; i < NUM_SAMPLES; i++) {
            bh.consume(rng.nextInt());
        }
    }

    /**
     * @param sources Source of randomness.
     * @param bh Data sink.
     */
    @Benchmark
    public void nextBooleanIntProvider(SourcesInt sources,
                                       Blackhole bh) {
        UniformRandomProvider rng = sources.getGenerator();
        if (cache) {
            rng = CachedUniformRandomProviderFactory.wrap(rng);
        }
        runNextBoolean(rng, bh);
    }

    /**
     * @param sources Source of randomness.
     * @param bh Data sink.
     */
    @Benchmark
    public void nextBooleanLongProvider(SourcesLong sources,
                                        Blackhole bh) {
        UniformRandomProvider rng = sources.getGenerator();
        if (cache) {
            rng = CachedUniformRandomProviderFactory.wrap(rng);
        }
        runNextBoolean(rng, bh);
    }

    /**
     * @param sources Source of randomness.
     * @param bh Data sink.
     */
    @Benchmark
    public void nextIntLongProvider(SourcesLong sources,
                                    Blackhole bh) {
        UniformRandomProvider rng = sources.getGenerator();
        if (cache) {
            rng = CachedUniformRandomProviderFactory.wrap(rng);
        }
        runNextInt(rng, bh);
    }
}
