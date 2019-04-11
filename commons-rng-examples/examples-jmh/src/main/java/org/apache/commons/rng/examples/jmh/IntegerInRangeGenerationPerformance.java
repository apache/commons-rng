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

import org.apache.commons.rng.RestorableUniformRandomProvider;
import org.apache.commons.rng.UniformRandomProvider;
import org.apache.commons.rng.core.util.NumberFactory;
import org.apache.commons.rng.simple.RandomSource;
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

/**
 * Executes benchmark to compare the speed of generation of integer numbers in a positive range
 * using the integer primitives as a source of randomness.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@State(Scope.Benchmark)
@Fork(value = 1, jvmArgs = { "-server", "-Xms128M", "-Xmx128M" })
public class IntegerInRangeGenerationPerformance {
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
                "WELL_44497_B"
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

    /**
     * The upper range for the {@code long} generation.
     */
    @State(Scope.Benchmark)
    public static class LongRange {
        /**
         * The upper range for the {@code long} generation.
         *
         * <p>Note that the while loop uses a rejection algorithm so set a worst case scenario
         * (see IntRange).</p>
         */
        @Param({
            "256", // Even: 1 << 8
            "257", // Prime number
            "1073741825", // Worst case for int: (1 << 30) + 1
            "4294967296", // Lowest even that is not an int: 1L << 32
            "4294967297", // Lowest odd
            "4503599627370497", // Worst case for long: (1 << 62) + 1
            })
        private long upperBound;

        /**
         * Gets the upper bound.
         *
         * @return the upper bound
         */
        public long getUpperBound() {
            return upperBound;
        }
    }

    /**
     * Generates an {@code int} value between 0 (inclusive) and the
     * specified value (exclusive) using a bit source of randomness.
     *
     * @param rng Value to use as a source of randomness.
     * @param n Bound on the random number to be returned. Must be positive.
     * @return a random {@code int} value between 0 (inclusive) and {@code n}
     * (exclusive).
     * @throws IllegalArgumentException if {@code n} is negative.
     */
    private static int nextInt(UniformRandomProvider rng, int n) {
        if (n <= 0) {
            throw new IllegalArgumentException();
        }
        final int nm1 = n - 1;
        if ((n & nm1) == 0) {
            // Range is a power of 2
            return rng.nextInt() & nm1;
        }
        int bits;
        int val;
        do {
            bits = rng.nextInt() >>> 1;
            val = bits % n;
        } while (bits - val + nm1 < 0);
        return val;
    }

    /**
     * Generates an {@code long} value between 0 (inclusive) and the
     * specified value (exclusive) using a bit source of randomness.
     *
     * @param rng Value to use as a source of randomness.
     * @param n Bound on the random number to be returned. Must be positive.
     * @return a random {@code long} value between 0 (inclusive) and {@code n}
     * (exclusive).
     * @throws IllegalArgumentException if {@code n} is negative.
     */
    private static long nextLong(UniformRandomProvider rng, long n) {
        if (n <= 0) {
            throw new IllegalArgumentException();
        }
        final long nm1 = n - 1;
        if ((n & nm1) == 0L) {
            // Range is a power of 2
            return rng.nextLong() & nm1;
        }
        long bits;
        long val;
        do {
            bits = rng.nextLong() >>> 1;
            val = bits % n;
        } while (bits - val + nm1 < 0);
        return val;
    }

    // Benchmark methods

    /**
     * @param source the source
     * @return the result
     */
    @Benchmark
    public int nextIntBaseline(Sources source) {
        return source.getGenerator().nextInt();
    }

    /**
     * @param source the source
     * @param range the range
     * @return the result
     */
    @Benchmark
    public int nextIntRejectionMethod(Sources source, IntRange range) {
        return nextInt(source.getGenerator(), range.getUpperBound());
    }

    /**
     * @param source the source
     * @param range the range
     * @return the result
     */
    @Benchmark
    public long nextIntNumberFactory(Sources source, IntRange range) {
        return NumberFactory.makeIntInRange(source.getGenerator().nextInt(), range.getUpperBound());
    }

    /**
     * @param source the source
     * @return the result
     */
    @Benchmark
    public long nextLongBaseline(Sources source) {
        return source.getGenerator().nextLong();
    }

    /**
     * @param source the source
     * @param range the range
     * @return the result
     */
    @Benchmark
    public long nextLongRejectionMethod(Sources source, LongRange range) {
        return nextLong(source.getGenerator(), range.getUpperBound());
    }

    /**
     * @param source the source
     * @param range the range
     * @return the result
     */
    @Benchmark
    public long nextLongNumberFactory(Sources source, LongRange range) {
        return NumberFactory.makeLongInRange(source.getGenerator().nextLong(), range.getUpperBound());
    }
}
