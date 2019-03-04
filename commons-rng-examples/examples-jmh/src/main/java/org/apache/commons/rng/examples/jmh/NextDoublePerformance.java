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
public class NextDoublePerformance {
    // Mimic the generation of the SplitMix64 algorithm
    // and a SplitMix32 algorithm to get a spread of input numbers.

    /**
     * The 64-bit golden ratio number.
     */
    private static final long GOLDEN_64 = 0x9e3779b97f4a7c15L;
    /**
     * The 32-bit golden ratio number.
     */
    private static final long GOLDEN_32 = 0x9e3779b9;

    /** The long state. */
    private long longState = ThreadLocalRandom.current().nextLong();
    /** The int state. */
    private int intState = ThreadLocalRandom.current().nextInt();

    /**
     * Get the next long in the sequence.
     *
     * @return the long
     */
    private long nextLong() {
        return longState += GOLDEN_64;
    }

    /**
     * Get the next int in the sequence.
     *
     * @return the int
     */
    private int nextInt() {
        return intState += GOLDEN_32;
    }

    // Benchmark methods

    /**
     * @return the long
     */
    @Benchmark
    public long nextDoubleBaseline() {
        return nextLong();
    }

    /**
     * @return the double
     */
    @Benchmark
    public double nextDoubleUsingBitsToDouble() {
        // Combine 11 bit unsigned exponent with value 1023 (768 + 255) with 52 bit mantissa
        // 0x300L = 256 + 512 = 768
        // 0x0ff  = 255
        // This makes a number in the range 1.0 to 2.0 so subtract 1.0
        return Double.longBitsToDouble(0x3ffL << 52 | nextLong() >>> 12) - 1.0;
    }

    /**
     * @return the double
     */
    @Benchmark
    public double nextDoubleUsingMultiply52bits() {
        return (nextLong() >>> 12) * 0x1.0p-52d; // 1.0 / (1L << 52)
    }

    /**
     * @return the double
     */
    @Benchmark
    public double nextDoubleUsingMultiply53bits() {
        return (nextLong() >>> 11) * 0x1.0p-53d; // 1.0 / (1L << 53)
    }

    /**
     * @return the int
     */
    @Benchmark
    public int nextFloatBaseline() {
        return nextInt();
    }

    /**
     * @return the float
     */
    @Benchmark
    public float nextFloatUsingBitsToFloat() {
        // Combine 8 bit unsigned exponent with value 127 (112 + 15) with 23 bit mantissa
        // 0x70 = 64 + 32 + 16 = 112
        // 0x0f = 15
        // This makes a number in the range 1.0f to 2.0f so subtract 1.0f
        return Float.intBitsToFloat(0x7f << 23 | nextInt() >>> 9) - 1.0f;
    }

    /**
     * @return the float
     */
    @Benchmark
    public float nextFloatUsingMultiply23bits() {
        return (nextInt() >>> 9) * 0x1.0p-23f; // 1.0f / (1 << 23)
    }

    /**
     * @return the float
     */
    @Benchmark
    public float nextFloatUsingMultiply24bits() {
        return (nextInt() >>> 8) * 0x1.0p-24f; // 1.0f / (1 << 24)
    }
}
