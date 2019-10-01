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

import org.apache.commons.rng.UniformRandomProvider;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;

import java.util.concurrent.TimeUnit;

/**
 * Benchmarks to check linearity in the baseline implementations of {@link UniformRandomProvider}.
 *
 * <p>These ordinarily do not need to be run. The benchmarks can be used to determine
 * if the baseline scales linearly with workload. If not then the JVM has removed the
 * baseline from the testing loop given that its result is predictable. The ideal
 * baseline will:</p>
 *
 * <ul>
 *  <li>Run as fast as possible
 *  <li>Not be removed from the execution path
 * </ul>
 *
 * <p>The results of this benchmark should be plotted for each method using [numValues] vs [run time]
 * to check linearity.</p>
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 10, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 10, time = 1, timeUnit = TimeUnit.SECONDS)
@State(Scope.Benchmark)
@Fork(value = 1, jvmArgs = { "-server", "-Xms128M", "-Xmx128M" })
public class BaselineGenerationPerformance {
    /**
     * The size of the array for testing {@link UniformRandomProvider#nextBytes(byte[])}.
     *
     * <p>This is a small prime number (127). This satisfies the following requirements:</p>
     *
     * <ul>
     *   <li>The number of bytes will be allocated when testing so the allocation overhead
     *   should be small.
     *   <li>The number must be set so that filling the bytes from an {@code int} or {@code long}
     *   source has no advantage for the 32-bit source, e.g. the same number of underlying bits have
     *   to be generated. Note: 127 / 4 ~ 32 ints or 127 / 8 ~ 16 longs.
     *   <li>The number should not be a factor of 4 to prevent filling completely using a 32-bit
     *   source. This tests the edge case of partial fill.
     * </ul>
     */
    static final int NEXT_BYTES_SIZE = 127;

    /**
     * The upper limit for testing {@link UniformRandomProvider#nextInt(int)}.
     *
     * <p>This is the biggest prime number for an {@code int} (2147483629) to give a worst case
     * run-time for the method.</p>
     */
    static final int NEXT_INT_LIMIT = 2_147_483_629;

    /**
     * The upper limit for testing {@link UniformRandomProvider#nextLong(long)}.
     *
     * <p>This is the biggest prime number for a {@code long} (9223372036854775783L) to
     * give a worst case run-time for the method.</p>
     */
    static final long NEXT_LONG_LIMIT = 9_223_372_036_854_775_783L;

    /**
     * The provider for testing {@link UniformRandomProvider#nextByte()} and
     * {@link UniformRandomProvider#nextByte(int)}.
     */
    private UniformRandomProvider nextBytesProvider = BaselineUtils.getNextBytes();

    /**
     * The provider for testing {@link UniformRandomProvider#nextInt()} and
     * {@link UniformRandomProvider#nextInt(int)}.
     */
    private UniformRandomProvider nextIntProvider = BaselineUtils.getNextInt();

    /**
     * The provider for testing {@link UniformRandomProvider#nextLong()} and
     * {@link UniformRandomProvider#nextLong(long)}.
     */
    private UniformRandomProvider nextLongProvider = BaselineUtils.getNextLong();

    /**
     * The provider for testing {@link UniformRandomProvider#nextBoolean()}.
     */
    private UniformRandomProvider nextBooleanProvider = BaselineUtils.getNextBoolean();

    /**
     * The provider for testing {@link UniformRandomProvider#nextFloat()}.
     */
    private UniformRandomProvider nextFloatProvider = BaselineUtils.getNextFloat();

    /**
     * The provider for testing {@link UniformRandomProvider#nextDouble()}.
     */
    private UniformRandomProvider nextDoubleProvider = BaselineUtils.getNextDouble();

    /**
     * Number of random values to generate when testing linearity. This must be high to avoid
     * JIT optimisation of small loop constructs.
     *
     * <p>Note: Following the convention in the JMH Blackhole::consumCPU(long) method
     * the loops are constructed to count down (although since there is no consumption
     * of the loop counter the loop construct may be rewritten anyway).</p>
     */
    @Param({"50000", "100000", "150000", "200000", "250000"})
    private int numValues;

    /**
     * Exercise the {@link UniformRandomProvider#nextBytes(byte[])} method.
     *
     * <p>Note: Currently there is not a test for
     * {@link UniformRandomProvider#nextBytes(byte[], int, int)} since the two methods are
     * implemented by the base Int/LongProvider class using the same code.</p>
     *
     * @param bh Data sink.
     */
    @Benchmark
    public void nextBytes(Blackhole bh) {
        // The array allocation is not part of the benchmark.
        final byte[] result = new byte[NEXT_BYTES_SIZE];
        for (int i = numValues; i > 0; i--) {
            nextBytesProvider.nextBytes(result);
            bh.consume(result);
        }
    }

    /**
     * Exercise the {@link UniformRandomProvider#nextInt()} method.
     *
     * @param bh Data sink.
     */
    @Benchmark
    public void nextInt(Blackhole bh) {
        for (int i = numValues; i > 0; i--) {
            bh.consume(nextIntProvider.nextInt());
        }
    }

    /**
     * Exercise the {@link UniformRandomProvider#nextInt(int)} method.
     *
     * @param bh Data sink.
     */
    @Benchmark
    public void nextIntN(Blackhole bh) {
        for (int i = numValues; i > 0; i--) {
            bh.consume(nextIntProvider.nextInt(NEXT_INT_LIMIT));
        }
    }

    /**
     * Exercise the {@link UniformRandomProvider#nextLong()} method.
     *
     * @param bh Data sink.
     */
    @Benchmark
    public void nextLong(Blackhole bh) {
        for (int i = numValues; i > 0; i--) {
            bh.consume(nextLongProvider.nextLong());
        }
    }

    /**
     * Exercise the {@link UniformRandomProvider#nextLong(long)} method.
     *
     * @param bh Data sink.
     */
    @Benchmark
    public void nextLongN(Blackhole bh) {
        for (int i = numValues; i > 0; i--) {
            bh.consume(nextLongProvider.nextLong(NEXT_LONG_LIMIT));
        }
    }

    /**
     * Exercise the {@link UniformRandomProvider#nextBoolean()} method.
     *
     * @param bh Data sink.
     */
    @Benchmark
    public void nextBoolean(Blackhole bh) {
        for (int i = numValues; i > 0; i--) {
            bh.consume(nextBooleanProvider.nextBoolean());
        }
    }

    /**
     * Exercise the {@link UniformRandomProvider#nextFloat()} method.
     *
     * @param bh Data sink.
     */
    @Benchmark
    public void nextFloat(Blackhole bh) {
        for (int i = numValues; i > 0; i--) {
            bh.consume(nextFloatProvider.nextFloat());
        }
    }

    /**
     * Exercise the {@link UniformRandomProvider#nextDouble()} method.
     *
     * @param bh Data sink.
     */
    @Benchmark
    public void nextDouble(Blackhole bh) {
        for (int i = numValues; i > 0; i--) {
            bh.consume(nextDoubleProvider.nextDouble());
        }
    }
}
