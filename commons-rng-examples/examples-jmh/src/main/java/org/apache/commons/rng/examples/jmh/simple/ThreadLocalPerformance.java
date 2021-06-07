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

package org.apache.commons.rng.examples.jmh.simple;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Threads;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.OutputTimeUnit;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import org.apache.commons.rng.UniformRandomProvider;
import org.apache.commons.rng.simple.RandomSource;
import org.apache.commons.rng.simple.ThreadLocalRandomSource;

/**
 * Executes benchmark to compare the speed of generation of low frequency
 * random numbers on multiple-threads.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@State(Scope.Benchmark)
@Fork(value = 1, jvmArgs = {"-server", "-Xms128M", "-Xmx128M"})
public class ThreadLocalPerformance {
    /**
     * Number of random values to generate.
     */
    @Param({"0", "1", "10", "100"})
    private int numValues;

    /**
     * The benchmark state (to retrieve the various "RandomSource"s).
     */
    @State(Scope.Benchmark)
    public static class Sources {
        /** The random source. */
        protected RandomSource randomSource;

        /**
         * RNG providers.
         */
        @Param({"SPLIT_MIX_64"})
        private String randomSourceName;

        /**
         * @return the random source
         */
        public RandomSource getRandomSource() {
            return randomSource;
        }

        /** Instantiates the random source. */
        @Setup
        public void setup() {
            randomSource = RandomSource.valueOf(randomSourceName);
        }
    }

    /**
     * The benchmark state (to retrieve the various "RandomSource"s thread locally).
     */
    @State(Scope.Benchmark)
    public static class LocalSources extends Sources {
        /** The thread-local random provider. */
        private ThreadLocal<UniformRandomProvider> rng;

        /**
         * @return the random number generator
         */
        public UniformRandomProvider getRNG() {
            return rng.get();
        }

        /** Instantiates the ThreadLocal holding the random source. */
        @Override
        @Setup
        public void setup() {
            super.setup();

            rng = new ThreadLocal<UniformRandomProvider>() {
                @Override
                protected UniformRandomProvider initialValue() {
                    return randomSource.create();
                }
            };
        }
    }

    /**
     * @return the result
     */
    @Benchmark
    @Threads(4)
    public long threadLocalRandom() {
        final ThreadLocalRandom rng = ThreadLocalRandom.current();
        long result = 0;
        for (int i = 0; i < numValues; i++) {
            result = result ^ rng.nextLong();
        }
        return result;
    }

    /**
     * @return the result
     */
    @Benchmark
    @Threads(4)
    public long threadLocalRandomWrapped() {
        final ThreadLocalRandom rand = ThreadLocalRandom.current();
        final UniformRandomProvider rng = new UniformRandomProvider() {
            // CHECKSTYLE: stop all
            @Override
            public void nextBytes(byte[] bytes) { /* Ignore this. */ }
            @Override
            public void nextBytes(byte[] bytes, int start, int len) { /* Ignore this. */ }
            @Override
            public int nextInt() { return rand.nextInt(); }
            @Override
            public int nextInt(int n) { return rand.nextInt(n); }
            @Override
            public long nextLong() { return rand.nextLong(); }
            @Override
            public long nextLong(long n) { return rand.nextLong(n); }
            @Override
            public boolean nextBoolean() { return rand.nextBoolean(); }
            @Override
            public float nextFloat() { return rand.nextFloat(); }
            @Override
            public double nextDouble() { return rand.nextDouble(); }
            // CHECKSTYLE: resume all
        };
        long result = 0;
        for (int i = 0; i < numValues; i++) {
            result = result ^ rng.nextLong();
        }
        return result;
    }

    /**
     * @param sources Source of randomness.
     * @return the result
     */
    @Benchmark
    @Threads(4)
    public long randomSourceCreate(Sources sources) {
        final UniformRandomProvider rng = sources.getRandomSource().create();
        long result = 0;
        for (int i = 0; i < numValues; i++) {
            result = result ^ rng.nextLong();
        }
        return result;
    }

    /**
     * @param sources Source of randomness.
     * @return the result
     */
    @Benchmark
    @Threads(4)
    public long threadLocalRandomSourceCurrent(Sources sources) {
        final UniformRandomProvider rng = ThreadLocalRandomSource.current(sources.getRandomSource());
        long result = 0;
        for (int i = 0; i < numValues; i++) {
            result = result ^ rng.nextLong();
        }
        return result;
    }

    /**
     * @param localSources Local source of randomness.
     * @return the result
     */
    @Benchmark
    @Threads(4)
    public long threadLocalUniformRandomProvider(LocalSources localSources) {
        final UniformRandomProvider rng = localSources.getRNG();
        long result = 0;
        for (int i = 0; i < numValues; i++) {
            result = result ^ rng.nextLong();
        }
        return result;
    }
}
