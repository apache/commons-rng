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

import org.apache.commons.rng.UniformRandomProvider;
import org.apache.commons.rng.simple.RandomSource;
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
import org.openjdk.jmh.annotations.Threads;
import org.openjdk.jmh.annotations.Warmup;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Executes a benchmark to compare the speed of generating an array of {@code int/long} values
 * in a thread-safe way.
 *
 * <p>Uses an upper limit of 128 for the size of an array seed.</p>
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 10, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 10, time = 1, timeUnit = TimeUnit.SECONDS)
@State(Scope.Benchmark)
@Fork(value = 1, jvmArgs = { "-server", "-Xms512M", "-Xmx512M" })
public class SeedArrayGenerationPerformance {
    /**
     * The lock to own when using the generator. This lock is unfair and there is no
     * particular access order for waiting threads.
     *
     * <p>This is used as an alternative to {@code synchronized} statements.</p>
     */
    private static final ReentrantLock UNFAIR_LOCK = new ReentrantLock(false);

    /**
     * The lock to own when using the generator. This lock is fair and the longest waiting
     * thread will be favoured.
     *
     * <p>This is used as an alternative to {@code synchronized} statements.</p>
     */
    private static final ReentrantLock FAIR_LOCK = new ReentrantLock(true);

    /** The int[] value. Must NOT be final to prevent JVM optimisation! */
    private int[] intValue;

    /** The long[] value. Must NOT be final to prevent JVM optimisation! */
    private long[] longValue;

    /**
     * The RandomSource to test.
     */
    @State(Scope.Benchmark)
    public static class SeedRandomSources {
        /**
         * RNG providers to test.
         * For seed generation only long period generators should be considered.
         */
        @Param({"WELL_44497_B",
                "XOR_SHIFT_1024_S_PHI",
                })
        private String randomSourceName;

        /** RNG. */
        private UniformRandomProvider generator;

        /**
         * Gets the generator.
         *
         * @return the RNG.
         */
        public UniformRandomProvider getGenerator() {
            return generator;
        }

        /**
         * Look-up the {@link RandomSource} from the name and instantiates the generator.
         */
        @Setup
        public void setup() {
            generator = RandomSource.create(RandomSource.valueOf(randomSourceName));
        }
    }

    /**
     * The number of values that are required to seed a generator.
     */
    @State(Scope.Benchmark)
    public static class SeedSizes {
        /** The number of values. */
        @Param({"2", "4", "8", "16", "32", "64", "128"})
        private int size;

        /**
         * Gets the number of {@code int} values required.
         *
         * @return the size
         */
        public int getSize() {
            return size;
        }
    }

    /**
     * Define the number of seed values to create and the number to compute per synchronisation on
     * the generator.
     */
    @State(Scope.Benchmark)
    public static class TestSizes {
        /** The number of values. */
        @Param({"2", "4", "8", "16", "32", "64", "128"})
        private int size;

        /** The block size is the number of values to compute per synchronisation on the generator. */
        @Param({"2", "4", "8", "16", "32", "64", "128"})
        private int blockSize;

        /**
         * Gets the number of {@code int} values required.
         *
         * @return the size
         */
        public int getSize() {
            return size;
        }

        /**
         * @return the block size
         */
        public int getBlockSize() {
            return blockSize;
        }

        /**
         * Verify the size parameters. This throws an exception if the block size exceeds the seed
         * size as the test is redundant. JMH will catch the exception and run the next benchmark.
         */
        @Setup
        public void setup() {
            if (getBlockSize() > getSize()) {
                throw new AssertionError("Skipping benchmark: Block size is above seed size");
            }
        }
    }

    /**
     * Get the next {@code int} from the RNG. This is synchronized on the generator.
     *
     * @param rng Random generator.
     * @return the int
     */
    private static int nextInt(UniformRandomProvider rng) {
        synchronized (rng) {
            return rng.nextInt();
        }
    }

    /**
     * Get the next {@code long} from the RNG. This is synchronized on the generator.
     *
     * @param rng Random generator.
     * @return the long
     */
    private static long nextLong(UniformRandomProvider rng) {
        synchronized (rng) {
            return rng.nextLong();
        }
    }

    /**
     * Fill the array between {@code start} inclusive and {@code end} exclusive from the RNG.
     * This is synchronized on the generator.
     *
     * @param rng Random generator.
     * @param array Array data.
     * @param start Start (inclusive).
     * @param end End (exclusive).
     */
    private static void nextInt(UniformRandomProvider rng, int[] array, int start, int end) {
        synchronized (rng) {
            for (int i = start; i < end; i++) {
                array[i] = rng.nextInt();
            }
        }
    }

    /**
     * Fill the array between {@code start} inclusive and {@code end} exclusive from the
     * RNG. This is synchronized on the generator.
     *
     * @param rng Random generator.
     * @param array Array data.
     * @param start Start (inclusive).
     * @param end End (exclusive).
     */
    private static void nextLong(UniformRandomProvider rng, long[] array, int start, int end) {
        synchronized (rng) {
            for (int i = start; i < end; i++) {
                array[i] = rng.nextLong();
            }
        }
    }

    /**
     * Get the next {@code int} from the RNG. The lock is used to guard access to the generator.
     *
     * @param lock Lock guarding access to the generator.
     * @param rng Random generator.
     * @return the int
     */
    private static int nextInt(Lock lock, UniformRandomProvider rng) {
        lock.lock();
        try {
            return rng.nextInt();
        } finally {
            lock.unlock();
        }
    }

    /**
     * Get the next {@code long} from the RNG. The lock is used to guard access to the generator.
     *
     * @param lock Lock guarding access to the generator.
     * @param rng Random generator.
     * @return the long
     */
    private static long nextLong(Lock lock, UniformRandomProvider rng) {
        lock.lock();
        try {
            return rng.nextLong();
        } finally {
            lock.unlock();
        }
    }

    /**
     * Fill the array between {@code start} inclusive and {@code end} exclusive from the RNG.
     * The lock is used to guard access to the generator.
     *
     * @param lock Lock guarding access to the generator.
     * @param rng Random generator.
     * @param array Array data.
     * @param start Start (inclusive).
     * @param end End (exclusive).
     */
    private static void nextInt(Lock lock, UniformRandomProvider rng, int[] array, int start, int end) {
        lock.lock();
        try {
            for (int i = start; i < end; i++) {
                array[i] = rng.nextInt();
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * Fill the array between {@code start} inclusive and {@code end} exclusive from the RNG.
     * The lock is used to guard access to the generator.
     *
     * @param lock Lock guarding access to the generator.
     * @param rng Random generator.
     * @param array Array data.
     * @param start Start (inclusive).
     * @param end End (exclusive).
     */
    private static void nextLong(Lock lock, UniformRandomProvider rng, long[] array, int start, int end) {
        lock.lock();
        try {
            for (int i = start; i < end; i++) {
                array[i] = rng.nextLong();
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * Baseline for a JMH method call with no return value.
     */
    @Benchmark
    public void baselineVoid() {
        // Do nothing, this is a baseline
    }

    /**
     * Baseline for a JMH method call returning an {@code int[]}.
     *
     * @return the value
     */
    @Benchmark
    public int[] baselineIntArray() {
        return intValue;
    }

    /**
     * Baseline for a JMH method call returning an {@code long[]}.
     *
     * @return the value
     */
    @Benchmark
    public long[] baselineLongArray() {
        return longValue;
    }

    // The following methods use underscores to make parsing the results output easier.
    // They are not documented as the names are self-documenting.

    // CHECKSTYLE: stop MethodName

    /**
     * @param sources Source of randomness.
     * @param sizes Size of the seed.
     * @return the seed
     */
    @Benchmark
    public int[] createIntArraySeed(SeedRandomSources sources, SeedSizes sizes) {
        final UniformRandomProvider rng = sources.getGenerator();
        final int[] seed = new int[sizes.getSize()];
        for (int i = 0; i < seed.length; i++) {
            seed[i] = rng.nextInt();
        }
        return seed;
    }

    /**
     * @param sources Source of randomness.
     * @param sizes Size of the seed.
     * @return the seed
     */
    @Benchmark
    public long[] createLongArraySeed(SeedRandomSources sources, SeedSizes sizes) {
        final UniformRandomProvider rng = sources.getGenerator();
        final long[] seed = new long[sizes.getSize()];
        for (int i = 0; i < seed.length; i++) {
            seed[i] = rng.nextLong();
        }
        return seed;
    }

    /**
     * @param sources Source of randomness.
     * @param sizes Size of the seed.
     * @return the seed
     */
    @Benchmark
    public int[] Threads1_createIntArraySeed_Sync(SeedRandomSources sources, SeedSizes sizes) {
        final UniformRandomProvider rng = sources.getGenerator();
        final int[] seed = new int[sizes.getSize()];
        for (int i = 0; i < seed.length; i++) {
            seed[i] = nextInt(rng);
        }
        return seed;
    }

    /**
     * @param sources Source of randomness.
     * @param sizes Size of the seed.
     * @return the seed
     */
    @Benchmark
    public long[] Threads1_createLongArraySeed_Sync(SeedRandomSources sources, SeedSizes sizes) {
        final UniformRandomProvider rng = sources.getGenerator();
        final long[] seed = new long[sizes.getSize()];
        for (int i = 0; i < seed.length; i++) {
            seed[i] = nextLong(rng);
        }
        return seed;
    }

    /**
     * @param sources Source of randomness.
     * @param sizes Size of the seed.
     * @return the seed
     */
    @Benchmark
    public int[] Threads1_createIntArraySeed_UnfairLock(SeedRandomSources sources, SeedSizes sizes) {
        final UniformRandomProvider rng = sources.getGenerator();
        final int[] seed = new int[sizes.getSize()];
        for (int i = 0; i < seed.length; i++) {
            seed[i] = nextInt(UNFAIR_LOCK, rng);
        }
        return seed;
    }

    /**
     * @param sources Source of randomness.
     * @param sizes Size of the seed.
     * @return the seed
     */
    @Benchmark
    public long[] Threads1_createLongArraySeed_UnfairLock(SeedRandomSources sources, SeedSizes sizes) {
        final UniformRandomProvider rng = sources.getGenerator();
        final long[] seed = new long[sizes.getSize()];
        for (int i = 0; i < seed.length; i++) {
            seed[i] = nextLong(UNFAIR_LOCK, rng);
        }
        return seed;
    }

    /**
     * @param sources Source of randomness.
     * @param sizes Size of the seed.
     * @return the seed
     */
    @Benchmark
    public int[] Threads1_createIntArraySeed_FairLock(SeedRandomSources sources, SeedSizes sizes) {
        final UniformRandomProvider rng = sources.getGenerator();
        final int[] seed = new int[sizes.getSize()];
        for (int i = 0; i < seed.length; i++) {
            seed[i] = nextInt(FAIR_LOCK, rng);
        }
        return seed;
    }

    /**
     * @param sources Source of randomness.
     * @param sizes Size of the seed.
     * @return the seed
     */
    @Benchmark
    public long[] Threads1_createLongArraySeed_FairLock(SeedRandomSources sources, SeedSizes sizes) {
        final UniformRandomProvider rng = sources.getGenerator();
        final long[] seed = new long[sizes.getSize()];
        for (int i = 0; i < seed.length; i++) {
            seed[i] = nextLong(FAIR_LOCK, rng);
        }
        return seed;
    }

    /**
     * @param sources Source of randomness.
     * @param sizes Size of the seed and compute blocks.
     * @return the seed
     */
    @Benchmark
    public int[] Threads1_createIntArraySeedBlocks_Sync(SeedRandomSources sources, TestSizes sizes) {
        final UniformRandomProvider rng = sources.getGenerator();
        final int[] seed = new int[sizes.getSize()];
        for (int i = 0; i < seed.length; i += sizes.getBlockSize()) {
            nextInt(rng, seed, i, Math.min(i + sizes.getBlockSize(), seed.length));
        }
        return seed;
    }

    /**
     * @param sources Source of randomness.
     * @param sizes Size of the seed and compute blocks.
     * @return the seed
     */
    @Benchmark
    public long[] Threads1_createLongArraySeedBlocks_Sync(SeedRandomSources sources, TestSizes sizes) {
        final UniformRandomProvider rng = sources.getGenerator();
        final long[] seed = new long[sizes.getSize()];
        for (int i = 0; i < seed.length; i += sizes.getBlockSize()) {
            nextLong(rng, seed, i, Math.min(i + sizes.getBlockSize(), seed.length));
        }
        return seed;
    }

    /**
     * @param sources Source of randomness.
     * @param sizes Size of the seed and compute blocks.
     * @return the seed
     */
    @Benchmark
    public int[] Threads1_createIntArraySeedBlocks_UnfairLock(SeedRandomSources sources, TestSizes sizes) {
        final UniformRandomProvider rng = sources.getGenerator();
        final int[] seed = new int[sizes.getSize()];
        for (int i = 0; i < seed.length; i += sizes.getBlockSize()) {
            nextInt(UNFAIR_LOCK, rng, seed, i, Math.min(i + sizes.getBlockSize(), seed.length));
        }
        return seed;
    }

    /**
     * @param sources Source of randomness.
     * @param sizes Size of the seed and compute blocks.
     * @return the seed
     */
    @Benchmark
    public long[] Threads1_createLongArraySeedBlocks_UnfairLock(SeedRandomSources sources, TestSizes sizes) {
        final UniformRandomProvider rng = sources.getGenerator();
        final long[] seed = new long[sizes.getSize()];
        for (int i = 0; i < seed.length; i += sizes.getBlockSize()) {
            nextLong(UNFAIR_LOCK, rng, seed, i, Math.min(i + sizes.getBlockSize(), seed.length));
        }
        return seed;
    }

    /**
     * @param sources Source of randomness.
     * @param sizes Size of the seed and compute blocks.
     * @return the seed
     */
    @Benchmark
    public int[] Threads1_createIntArraySeedBlocks_FairLock(SeedRandomSources sources, TestSizes sizes) {
        final UniformRandomProvider rng = sources.getGenerator();
        final int[] seed = new int[sizes.getSize()];
        for (int i = 0; i < seed.length; i += sizes.getBlockSize()) {
            nextInt(FAIR_LOCK, rng, seed, i, Math.min(i + sizes.getBlockSize(), seed.length));
        }
        return seed;
    }

    /**
     * @param sources Source of randomness.
     * @param sizes Size of the seed and compute blocks.
     * @return the seed
     */
    @Benchmark
    public long[] Threads1_createLongArraySeedBlocks_FairLock(SeedRandomSources sources, TestSizes sizes) {
        final UniformRandomProvider rng = sources.getGenerator();
        final long[] seed = new long[sizes.getSize()];
        for (int i = 0; i < seed.length; i += sizes.getBlockSize()) {
            nextLong(FAIR_LOCK, rng, seed, i, Math.min(i + sizes.getBlockSize(), seed.length));
        }
        return seed;
    }

    /**
     * @param sources Source of randomness.
     * @param sizes Size of the seed.
     * @return the seed
     */
    @Benchmark
    @Threads(4)
    public int[] Threads4_createIntArraySeed_Sync(SeedRandomSources sources, SeedSizes sizes) {
        final UniformRandomProvider rng = sources.getGenerator();
        final int[] seed = new int[sizes.getSize()];
        for (int i = 0; i < seed.length; i++) {
            seed[i] = nextInt(rng);
        }
        return seed;
    }

    /**
     * @param sources Source of randomness.
     * @param sizes Size of the seed.
     * @return the seed
     */
    @Benchmark
    @Threads(4)
    public long[] Threads4_createLongArraySeed_Sync(SeedRandomSources sources, SeedSizes sizes) {
        final UniformRandomProvider rng = sources.getGenerator();
        final long[] seed = new long[sizes.getSize()];
        for (int i = 0; i < seed.length; i++) {
            seed[i] = nextLong(rng);
        }
        return seed;
    }

    /**
     * @param sources Source of randomness.
     * @param sizes Size of the seed.
     * @return the seed
     */
    @Benchmark
    @Threads(4)
    public int[] Threads4_createIntArraySeed_UnfairLock(SeedRandomSources sources, SeedSizes sizes) {
        final UniformRandomProvider rng = sources.getGenerator();
        final int[] seed = new int[sizes.getSize()];
        for (int i = 0; i < seed.length; i++) {
            seed[i] = nextInt(UNFAIR_LOCK, rng);
        }
        return seed;
    }

    /**
     * @param sources Source of randomness.
     * @param sizes Size of the seed.
     * @return the seed
     */
    @Benchmark
    @Threads(4)
    public long[] Threads4_createLongArraySeed_UnfairLock(SeedRandomSources sources, SeedSizes sizes) {
        final UniformRandomProvider rng = sources.getGenerator();
        final long[] seed = new long[sizes.getSize()];
        for (int i = 0; i < seed.length; i++) {
            seed[i] = nextLong(UNFAIR_LOCK, rng);
        }
        return seed;
    }

    /**
     * @param sources Source of randomness.
     * @param sizes Size of the seed.
     * @return the seed
     */
    @Benchmark
    @Threads(4)
    public int[] Threads4_createIntArraySeed_FairLock(SeedRandomSources sources, SeedSizes sizes) {
        final UniformRandomProvider rng = sources.getGenerator();
        final int[] seed = new int[sizes.getSize()];
        for (int i = 0; i < seed.length; i++) {
            seed[i] = nextInt(FAIR_LOCK, rng);
        }
        return seed;
    }

    /**
     * @param sources Source of randomness.
     * @param sizes Size of the seed.
     * @return the seed
     */
    @Benchmark
    @Threads(4)
    public long[] Threads4_createLongArraySeed_FairLock(SeedRandomSources sources, SeedSizes sizes) {
        final UniformRandomProvider rng = sources.getGenerator();
        final long[] seed = new long[sizes.getSize()];
        for (int i = 0; i < seed.length; i++) {
            seed[i] = nextLong(FAIR_LOCK, rng);
        }
        return seed;
    }

    /**
     * @param sources Source of randomness.
     * @param sizes Size of the seed and compute blocks.
     * @return the seed
     */
    @Benchmark
    @Threads(4)
    public int[] Threads4_createIntArraySeedBlocks_Sync(SeedRandomSources sources, TestSizes sizes) {
        final UniformRandomProvider rng = sources.getGenerator();
        final int[] seed = new int[sizes.getSize()];
        for (int i = 0; i < seed.length; i += sizes.getBlockSize()) {
            nextInt(rng, seed, i, Math.min(i + sizes.getBlockSize(), seed.length));
        }
        return seed;
    }

    /**
     * @param sources Source of randomness.
     * @param sizes Size of the seed and compute blocks.
     * @return the seed
     */
    @Benchmark
    @Threads(4)
    public long[] Threads4_createLongArraySeedBlocks_Sync(SeedRandomSources sources, TestSizes sizes) {
        final UniformRandomProvider rng = sources.getGenerator();
        final long[] seed = new long[sizes.getSize()];
        for (int i = 0; i < seed.length; i += sizes.getBlockSize()) {
            nextLong(rng, seed, i, Math.min(i + sizes.getBlockSize(), seed.length));
        }
        return seed;
    }

    /**
     * @param sources Source of randomness.
     * @param sizes Size of the seed and compute blocks.
     * @return the seed
     */
    @Benchmark
    @Threads(4)
    public int[] Threads4_createIntArraySeedBlocks_UnfairLock(SeedRandomSources sources, TestSizes sizes) {
        final UniformRandomProvider rng = sources.getGenerator();
        final int[] seed = new int[sizes.getSize()];
        for (int i = 0; i < seed.length; i += sizes.getBlockSize()) {
            nextInt(UNFAIR_LOCK, rng, seed, i, Math.min(i + sizes.getBlockSize(), seed.length));
        }
        return seed;
    }

    /**
     * @param sources Source of randomness.
     * @param sizes Size of the seed and compute blocks.
     * @return the seed
     */
    @Benchmark
    @Threads(4)
    public long[] Threads4_createLongArraySeedBlocks_UnfairLock(SeedRandomSources sources, TestSizes sizes) {
        final UniformRandomProvider rng = sources.getGenerator();
        final long[] seed = new long[sizes.getSize()];
        for (int i = 0; i < seed.length; i += sizes.getBlockSize()) {
            nextLong(UNFAIR_LOCK, rng, seed, i, Math.min(i + sizes.getBlockSize(), seed.length));
        }
        return seed;
    }

    /**
     * @param sources Source of randomness.
     * @param sizes Size of the seed and compute blocks.
     * @return the seed
     */
    @Benchmark
    @Threads(4)
    public int[] Threads4_createIntArraySeedBlocks_FairLock(SeedRandomSources sources, TestSizes sizes) {
        final UniformRandomProvider rng = sources.getGenerator();
        final int[] seed = new int[sizes.getSize()];
        for (int i = 0; i < seed.length; i += sizes.getBlockSize()) {
            nextInt(FAIR_LOCK, rng, seed, i, Math.min(i + sizes.getBlockSize(), seed.length));
        }
        return seed;
    }

    /**
     * @param sources Source of randomness.
     * @param sizes Size of the seed and compute blocks.
     * @return the seed
     */
    @Benchmark
    @Threads(4)
    public long[] Threads4_createLongArraySeedBlocks_FairLock(SeedRandomSources sources, TestSizes sizes) {
        final UniformRandomProvider rng = sources.getGenerator();
        final long[] seed = new long[sizes.getSize()];
        for (int i = 0; i < seed.length; i += sizes.getBlockSize()) {
            nextLong(FAIR_LOCK, rng, seed, i, Math.min(i + sizes.getBlockSize(), seed.length));
        }
        return seed;
    }
}
