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

package org.apache.commons.rng.examples.jmh.core;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.OutputTimeUnit;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/**
 * Executes benchmark to compare the speed of generation of floating point
 * numbers from the integer primitives.
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@State(Scope.Benchmark)
@Fork(value = 1, jvmArgs = { "-server", "-Xms128M", "-Xmx128M" })
public class FloatingPointGenerationPerformance {
    /**
     * Mimic the generation of the SplitMix64 algorithm.
     *
     * <p>The final mixing step must be included otherwise the output numbers are sequential
     * and the test may run with a lack of numbers with higher order bits.</p>
     */
    @State(Scope.Benchmark)
    public static class LongSource {
        /** The state. */
        private long state = ThreadLocalRandom.current().nextLong();

        /**
         * Get the next long.
         *
         * @return the long
         */
        public final long nextLong() {
            long z = state += 0x9e3779b97f4a7c15L;
            z = (z ^ (z >>> 30)) * 0xbf58476d1ce4e5b9L;
            z = (z ^ (z >>> 27)) * 0x94d049bb133111ebL;
            return z ^ (z >>> 31);
        }

        /**
         * Get the next int.
         *
         * <p>Returns the 32 high bits of Stafford variant 4 mix64 function as int.
         *
         * @return the int
         */
        public final int nextInt() {
            long z = state += 0x9e3779b97f4a7c15L;
            z = (z ^ (z >>> 33)) * 0x62a9d9ed799705f5L;
            return (int)(((z ^ (z >>> 28)) * 0xcb24d0a5c88c35b3L) >>> 32);
        }
    }

    // Benchmark methods

    /**
     * @param source the source
     * @return the long
     */
    @Benchmark
    public long nextDoubleBaseline(LongSource source) {
        return source.nextLong();
    }

    /**
     * @param source the source
     * @return the double
     */
    @Benchmark
    public double nextDoubleUsingBitsToDouble(LongSource source) {
        // Combine 11 bit unsigned exponent with value 1023 (768 + 255) with 52 bit mantissa
        // 0x300L = 256 + 512 = 768
        // 0x0ff  = 255
        // This makes a number in the range 1.0 to 2.0 so subtract 1.0
        return Double.longBitsToDouble(0x3ffL << 52 | source.nextLong() >>> 12) - 1.0;
    }

    /**
     * @param source the source
     * @return the double
     */
    @Benchmark
    public double nextDoubleUsingMultiply52bits(LongSource source) {
        return (source.nextLong() >>> 12) * 0x1.0p-52d; // 1.0 / (1L << 52)
    }

    /**
     * @param source the source
     * @return the double
     */
    @Benchmark
    public double nextDoubleUsingMultiply53bits(LongSource source) {
        return (source.nextLong() >>> 11) * 0x1.0p-53d; // 1.0 / (1L << 53)
    }

    /**
     * @param source the source
     * @return the int
     */
    @Benchmark
    public int nextFloatBaseline(LongSource source) {
        return source.nextInt();
    }

    /**
     * @param source the source
     * @return the float
     */
    @Benchmark
    public float nextFloatUsingBitsToFloat(LongSource source) {
        // Combine 8 bit unsigned exponent with value 127 (112 + 15) with 23 bit mantissa
        // 0x70 = 64 + 32 + 16 = 112
        // 0x0f = 15
        // This makes a number in the range 1.0f to 2.0f so subtract 1.0f
        return Float.intBitsToFloat(0x7f << 23 | source.nextInt() >>> 9) - 1.0f;
    }

    /**
     * @param source the source
     * @return the float
     */
    @Benchmark
    public float nextFloatUsingMultiply23bits(LongSource source) {
        return (source.nextInt() >>> 9) * 0x1.0p-23f; // 1.0f / (1 << 23)
    }

    /**
     * @param source the source
     * @return the float
     */
    @Benchmark
    public float nextFloatUsingMultiply24bits(LongSource source) {
        return (source.nextInt() >>> 8) * 0x1.0p-24f; // 1.0f / (1 << 24)
    }
}
