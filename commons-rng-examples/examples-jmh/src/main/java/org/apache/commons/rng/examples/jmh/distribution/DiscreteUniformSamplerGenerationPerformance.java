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

import org.apache.commons.rng.RestorableUniformRandomProvider;
import org.apache.commons.rng.UniformRandomProvider;
import org.apache.commons.rng.sampling.distribution.DiscreteUniformSampler;
import org.apache.commons.rng.sampling.distribution.SharedStateDiscreteSampler;
import org.apache.commons.rng.simple.RandomSource;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;

import java.util.concurrent.TimeUnit;

/**
 * Executes benchmark to compare the speed of generation of integer numbers in a positive range
 * using the {@link DiscreteUniformSampler} or {@link UniformRandomProvider#nextInt(int)}.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@State(Scope.Benchmark)
@Fork(value = 1, jvmArgs = { "-server", "-Xms128M", "-Xmx128M" })
public class DiscreteUniformSamplerGenerationPerformance {
    /** The number of samples. */
    @Param({
        "1",
        "2",
        "4",
        "8",
        "16",
        "1000000",
        })
    private int samples;

    /**
     * The benchmark state (retrieve the various "RandomSource"s).
     */
    @State(Scope.Benchmark)
    public static class Sources {
        /**
         * RNG providers.
         *
         * <p>Use different speeds.</p>
         *
         * @see <a href="https://commons.apache.org/proper/commons-rng/userguide/rng.html">
         *      Commons RNG user guide</a>
         */
        @Param({"SPLIT_MIX_64",
                // Comment in for slower generators
                //"MWC_256", "KISS", "WELL_1024_A",
                //"WELL_44497_B"
                })
        private String randomSourceName;

        /** RNG. */
        private RestorableUniformRandomProvider generator;

        /**
         * @return the RNG.
         */
        public UniformRandomProvider getGenerator() {
            return generator;
        }

        /** Instantiates generator. */
        @Setup
        public void setup() {
            final RandomSource randomSource = RandomSource.valueOf(randomSourceName);
            generator = RandomSource.create(randomSource);
        }
    }

    /**
     * The upper range for the {@code int} generation.
     */
    @State(Scope.Benchmark)
    public static class IntRange {
        /**
         * The upper range for the {@code int} generation.
         *
         * <p>Note that the while loop uses a rejection algorithm. From the Javadoc for java.util.Random:</p>
         *
         * <pre>
         * "The probability of a value being rejected depends on n. The
         * worst case is n=2^30+1, for which the probability of a reject is 1/2,
         * and the expected number of iterations before the loop terminates is 2."
         * </pre>
         */
        @Param({
            "256", // Even: 1 << 8
            "257", // Prime number
            "1073741825", // Worst case: (1 << 30) + 1
            })
        private int upperBound;

        /**
         * Gets the upper bound.
         *
         * @return the upper bound
         */
        public int getUpperBound() {
            return upperBound;
        }
    }

    // Benchmark methods.
    // Avoid consuming the generated values inside the loop. Use a sum and
    // consume at the end. This reduces the run-time as the BlackHole has
    // a relatively high overhead compared with number generation.
    // Subtracting the baseline from the other timings provides a measure
    // of the extra work done by the algorithm to produce unbiased samples in a range.

    /**
     * @param bh the data sink
     * @param source the source
     */
    @Benchmark
    public void nextIntBaseline(Blackhole bh, Sources source) {
        int sum = 0;
        for (int i = samples; i-- != 0;) {
            sum += source.getGenerator().nextInt();
        }
        bh.consume(sum);
    }

    /**
     * @param bh the data sink
     * @param source the source
     * @param range the range
     */
    @Benchmark
    public void nextIntRange(Blackhole bh, Sources source, IntRange range) {
        final int n = range.getUpperBound();
        int sum = 0;
        for (int i = samples; i-- != 0;) {
            sum += source.getGenerator().nextInt(n);
        }
        bh.consume(sum);
    }

    /**
     * @param bh the data sink
     * @param source the source
     * @param range the range
     */
    @Benchmark
    public void nextDiscreteUniformSampler(Blackhole bh, Sources source, IntRange range) {
        // Note: The sampler upper bound is inclusive.
        final SharedStateDiscreteSampler sampler = DiscreteUniformSampler.of(
                source.getGenerator(), 0, range.getUpperBound() - 1);
        int sum = 0;
        for (int i = samples; i-- != 0;) {
            sum += sampler.sample();
        }
        bh.consume(sum);
    }
}
