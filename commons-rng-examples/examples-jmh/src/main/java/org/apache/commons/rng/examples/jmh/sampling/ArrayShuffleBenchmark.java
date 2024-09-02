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

package org.apache.commons.rng.examples.jmh.sampling;

import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.stream.IntStream;
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
import org.openjdk.jmh.annotations.Warmup;

/**
 * Executes benchmark to compare the speed of shuffling an array.
 *
 * <p>Batched shuffle samples have been adapted from the blog post:
 * <a href="https://lemire.me/blog/2024/08/17/faster-random-integer-generation-with-batching/">
 * Daniel Lemire: Faster random integer generation with batching</a>.
 * The samples provided in the blog and the referenced paper are for a 64-bit
 * source of randomness which requires native support for 128-bit multiplication.
 * These have been modified for a 32-bit source of randomness.
 *
 * <ul>
 * <li>Nevin Brackett-Rozinsky, Daniel Lemire,
 * Batched Ranged Random Integer Generation, Software: Practice and Experience (to appear)
 * <a href="https://arxiv.org/abs/2408.06213">arXiv:2408.06213M</a>
 * </ul>
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@State(Scope.Benchmark)
@Fork(value = 1, jvmArgs = { "-server", "-Xms128M", "-Xmx128M" })
public class ArrayShuffleBenchmark {
    /** 2^32. Used for the bounded random algorithm. This is required as the original
     * method used (-bound % bound) for (2^L % bound) which only works for unsigned integer
     * modulus. */
    private static final long POW_32 = 1L << 32;
    /** 2^15. Length threshold to sample 2 integers from a random 32-bit value. */
    private static final int POW_15 = 1 << 15;
    /** 2^9. Length threshold to sample 3 integers from a random 32-bit value. */
    private static final int POW_9 = 1 << 9;
    /** 2^6. Length threshold to sample 4 integers from a random 32-bit value. */
    private static final int POW_6 = 1 << 6;
    /** Mask the lower 32-bit of a long. */
    private static final long MASK_32 = 0xffffffffL;

    /**
     * The data for the shuffle. Contains the data size and the random generators.
     */
    @State(Scope.Benchmark)
    public static class ShuffleData {
        /**
         * The list size.
         *
         * <p>Note: The 32-bit based shuffle2 method has a size threshold of 2^15
         * (32768) for creating two samples from each 32-bit random value.
         * Speed-up is most obvious for arrays below this size.
         */
        @Param({"4", "16", "64", "256", "1024", "4096", "8192", "16384", "32768", "65536", "262148", "1048592"})
        private int size;

        /** The data. */
        private int[] data;

        /**
         * @return the data
         */
        public int[] getData() {
            return data;
        }

        /**
         * Create the data.
         */
        @Setup
        public void setup() {
            data = IntStream.range(0, size).toArray();
        }
    }

    /**
     * Defines the {@link RandomSource} for testing.
     */
    @State(Scope.Benchmark)
    public static class RngSource {
        /**
         * RNG providers.
         *
         * <p>Use different speeds.</p>
         *
         * @see <a href="https://commons.apache.org/proper/commons-rng/userguide/rng.html">
         *      Commons RNG user guide</a>
         */
        @Param({"XO_RO_SHI_RO_128_PP",
                //"MWC_256",
                //"JDK"
            })
        private String randomSourceName;

        /** RNG. */
        private UniformRandomProvider rng;

        /**
         * Gets the source of randomness.
         *
         * @return RNG
         */
        public UniformRandomProvider getRNG() {
            return rng;
        }

        /**
         * Look-up the {@link RngSource} from the name and instantiates the generator.
         */
        @Setup
        public void setup() {
            rng = RandomSource.valueOf(randomSourceName).create();
        }
    }

    /**
     * Defines the shuffle method.
     */
    @State(Scope.Benchmark)
    public static class ShuffleMethod {
        /**
         * Method name.
         */
        @Param({"shuffle1",
            // Effectively the same speed as shuffle1
            //"shuffle1a",
            "shuffle2", "shuffle3", "shuffle4",
            // Effectively the same speed as shuffle2; the range method is marginally slower
            //"shuffle2a", "shuffle2range",
            })
        private String method;

        /** Shuffle function. */
        private BiConsumer<UniformRandomProvider, int[]> fun;

        /**
         * Gets the source of randomness.
         *
         * @return RNG
         */
        public BiConsumer<UniformRandomProvider, int[]> getMethod() {
            return fun;
        }

        /**
         * Look-up the {@link RngSource} from the name and instantiates the generator.
         */
        @Setup
        public void setup() {
            if ("shuffle1".equals(method)) {
                fun = ArrayShuffleBenchmark::shuffle1;
            } else if ("shuffle1a".equals(method)) {
                fun = ArrayShuffleBenchmark::shuffle1a;
            } else if ("shuffle2".equals(method)) {
                fun = ArrayShuffleBenchmark::shuffle2;
            } else if ("shuffle2a".equals(method)) {
                fun = ArrayShuffleBenchmark::shuffle2a;
            } else if ("shuffle2range".equals(method)) {
                fun = (rng, a) -> ArrayShuffleBenchmark.shuffle2(rng, a, 0, a.length);
            } else if ("shuffle3".equals(method)) {
                fun = ArrayShuffleBenchmark::shuffle3;
            } else if ("shuffle4".equals(method)) {
                fun = ArrayShuffleBenchmark::shuffle4;
            } else {
                throw new IllegalStateException("Unknown shuffle method: " + method);
            }
        }
    }

    /**
     * Swaps the two specified elements in the array.
     *
     * @param array Array.
     * @param i First index.
     * @param j Second index.
     */
    private static void swap(int[] array, int i, int j) {
        final int tmp = array[i];
        array[i] = array[j];
        array[j] = tmp;
    }

    /**
     * Shuffles the entries of the given array.
     * Uses a Fisher-Yates shuffle.
     *
     * @param rng Source of randomness.
     * @param array Array whose entries will be shuffled (in-place).
     * @return a reference to the given array
     */
    static int[] shuffle1(UniformRandomProvider rng, int[] array) {
        for (int i = array.length; i > 1; i--) {
            swap(array, i - 1, rng.nextInt(i));
        }
        return array;
    }

    /**
     * Generates an {@code int} value between 0 (inclusive) and the specified value
     * (exclusive).
     *
     * <p>Taken from {@code o.a.c.rng.UniformRandomProviderSupport}. This is used
     * to benchmark elimination of the conditional check for a negative index in
     * {@link UniformRandomProvider#nextInt(int)}.
     *
     * @param source Source of randomness.
     * @param n Bound on the random number to be returned. Must be strictly positive.
     * @return a random {@code int} value between 0 (inclusive) and {@code n} (exclusive).
     */
    static int nextInt(UniformRandomProvider source,
                       int n) {
        // Lemire (2019): Fast Random Integer Generation in an Interval
        // https://arxiv.org/abs/1805.10941
        long m = (source.nextInt() & 0xffffffffL) * n;
        long l = m & 0xffffffffL;
        if (l < n) {
            // 2^32 % n
            final final long t = POW_32 % n;
            while (l < t) {
                m = (source.nextInt() & 0xffffffffL) * n;
                l = m & 0xffffffffL;
            }
        }
        return (int) (m >>> 32);
    }

    /**
     * Shuffles the entries of the given array.
     * Uses a Fisher-Yates shuffle.
     *
     * @param rng Source of randomness.
     * @param array Array whose entries will be shuffled (in-place).
     * @return a reference to the given array
     */
    static int[] shuffle1a(UniformRandomProvider rng, int[] array) {
        for (int i = array.length; i > 1; i--) {
            swap(array, i - 1, nextInt(rng, i));
        }
        return array;
    }

    /**
     * Return two random values in {@code [0, range1)} and {@code [0, range2)}. The
     * product bound is used for the reject algorithm. See Brackett-Rozinsky and Lemire.
     *
     * <p>The product bound can be any positive integer {@code >= range1*range2}.
     * It may be updated to become {@code range1*range2}.
     *
     * @param range1 Range 1.
     * @param range2 Range 2.
     * @param productBound Product bound.
     * @param rng Source of randomness.
     * @return [i1, i2]
     */
    static int[] randomBounded2(int range1, int range2, int[] productBound, UniformRandomProvider rng) {
        long m = (rng.nextInt() & MASK_32) * range1;
        // result1 and result2 are the top 32-bits of the long
        long r1 = m;
        // Leftover bits * range2
        m = (m & MASK_32) * range2;
        long r2 = m;
        // Leftover bits must be unsigned
        long l = m & MASK_32;
        if (l < productBound[0]) {
            final int bound = range1 * range2;
            productBound[0] = bound;
            if (l < bound) {
                // 2^32 % bound
                final long t = POW_32 % bound;
                while (l < t) {
                    m = (rng.nextInt() & MASK_32) * range1;
                    r1 = m;
                    m = (m & MASK_32) * range2;
                    r2 = m;
                    l = m & MASK_32;
                }
            }
        }
        // Convert to [0, range1), [0, range2)
        return new int[] {(int) (r1 >> 32), (int) (r2 >> 32)};
    }

    /**
     * Shuffles the entries of the given array.
     *
     * @param rng Source of randomness.
     * @param array Array whose entries will be shuffled (in-place).
     * @return a reference to the given array
     */
    static int[] shuffle2(UniformRandomProvider rng, int[] array) {
        int i = array.length;
        // The threshold provided in the Brackett-Rozinsky and Lemire paper
        // is the power of 2 below 20724. Note that the product 2^15*2^15
        // is representable using signed integers.
        for (; i > POW_15; i--) {
            swap(array, i - 1, rng.nextInt(i));
        }
        // Batches of 2 for sizes up to 2^15 elements
        final int[] productBound = {i * (i - 1)};
        for (; i > 1; i -= 2) {
            final int[] indices = randomBounded2(i, i - 1, productBound, rng);
            final int index1 = indices[0];
            final int index2 = indices[1];
            swap(array, i - 1, index1);
            swap(array, i - 2, index2);
        }
        return array;
    }

    /**
     * Shuffles the entries of the given array.
     *
     * @param rng Source of randomness.
     * @param array Array whose entries will be shuffled (in-place).
     * @param from Lower-bound (inclusive) of the sub-range.
     * @param to Upper-bound (exclusive) of the sub-range.
     * @return a reference to the given array
     */
    static int[] shuffle2(UniformRandomProvider rng, int[] array, int from, int to) {
        int i = to - from;
        // The threshold provided in the Brackett-Rozinsky and Lemire paper
        // is the power of 2 below 20724. Note that the product 2^15*2^15
        // is representable using signed integers.
        for (; i > POW_15; i--) {
            swap(array, from + i - 1, from + rng.nextInt(i));
        }
        // Batches of 2 for sizes up to 2^15 elements
        final int[] productBound = {i * (i - 1)};
        for (; i > 1; i -= 2) {
            final int[] indices = randomBounded2(i, i - 1, productBound, rng);
            final int index1 = indices[0];
            final int index2 = indices[1];
            swap(array, from + i - 1, from + index1);
            swap(array, from + i - 2, from + index2);
        }
        return array;
    }

    /**
     * Return two random values in {@code [0, range1)} and {@code [0, range2)}. The
     * product bound is used for the reject algorithm. See Brackett-Rozinsky and Lemire.
     *
     * <p>The product bound can be any positive integer {@code >= range1*range2}.
     * It may be updated to become {@code range1*range2}.
     *
     * @param range1 Range 1.
     * @param range2 Range 2.
     * @param bound Product bound.
     * @param rng Source of randomness.
     * @param indices Output indices.
     * @return updated bound
     */
    static int randomBounded2a(int range1, int range2, int bound, UniformRandomProvider rng,
            int[] indices) {
        long m = (rng.nextInt() & MASK_32) * range1;
        // result1 and result2 are the top 32-bits of the long
        long r1 = m;
        // Leftover bits * range2
        m = (m & MASK_32) * range2;
        long r2 = m;
        // Leftover bits must be unsigned
        long l = m & MASK_32;
        if (l < bound) {
            bound = range1 * range2;
            if (l < bound) {
                // 2^32 % bound
                final long t = POW_32 % bound;
                while (l < t) {
                    m = (rng.nextInt() & MASK_32) * range1;
                    r1 = m;
                    m = (m & MASK_32) * range2;
                    r2 = m;
                    l = m & MASK_32;
                }
            }
        }
        // Convert to [0, range1), [0, range2)
        indices[0] = (int) (r1 >> 32);
        indices[1] = (int) (r2 >> 32);
        return bound;
    }

    /**
     * Shuffles the entries of the given array. Uses a variant of the random bounded
     * integer generation.
     *
     * @param rng Source of randomness.
     * @param array Array whose entries will be shuffled (in-place).
     * @return a reference to the given array
     */
    static int[] shuffle2a(UniformRandomProvider rng, int[] array) {
        int i = array.length;
        // The threshold provided in the Brackett-Rozinsky and Lemire paper
        // is the power of 2 below 20724. Note that the product 2^15*2^15
        // is representable using signed integers.
        for (; i > POW_15; i--) {
            swap(array, i - 1, rng.nextInt(i));
        }
        // Batches of 2 for sizes up to 2^15 elements
        final int[] indices = new int[2];
        int bound = i * (i - 1);
        for (; i > 1; i -= 2) {
            bound = randomBounded2a(i, i - 1, bound, rng, indices);
            swap(array, i - 1, indices[0]);
            swap(array, i - 2, indices[1]);
        }
        return array;
    }

    /**
     * Return three random values in {@code [0, range1)}, {@code [0, range2)}
     * and {@code [0, range3)}. The
     * product bound is used for the reject algorithm. See Brackett-Rozinsky and Lemire.
     *
     * <p>The product bound can be any positive integer {@code >= range1*range2*range3}.
     * It may be updated to become {@code range1*range2*range3}.
     *
     * @param range1 Range 1.
     * @param range2 Range 2.
     * @param range3 Range 3.
     * @param productBound Product bound.
     * @param rng Source of randomness.
     * @return [i1, i2, i3]
     */
    static int[] randomBounded3(int range1, int range2, int range3, int[] productBound, UniformRandomProvider rng) {
        long m = (rng.nextInt() & MASK_32) * range1;
        // result1 and result2 are the top 32-bits of the long
        long r1 = m;
        // Leftover bits * range2
        m = (m & MASK_32) * range2;
        long r2 = m;
        m = (m & MASK_32) * range3;
        long r3 = m;
        // Leftover bits must be unsigned
        long l = m & MASK_32;
        if (l < productBound[0]) {
            final int bound = range1 * range2 * range3;
            productBound[0] = bound;
            if (l < bound) {
                // 2^32 % bound
                final long t = POW_32 % bound;
                while (l < t) {
                    m = (rng.nextInt() & MASK_32) * range1;
                    r1 = m;
                    m = (m & MASK_32) * range2;
                    r2 = m;
                    m = (m & MASK_32) * range3;
                    r3 = m;
                    l = m & MASK_32;
                }
            }
        }
        // Convert to [0, range1), [0, range2), [0, range3)
        return new int[] {(int) (r1 >> 32), (int) (r2 >> 32), (int) (r3 >> 32)};
    }

    /**
     * Shuffles the entries of the given array.
     *
     * @param rng Source of randomness.
     * @param array Array whose entries will be shuffled (in-place).
     * @return a reference to the given array
     */
    static int[] shuffle3(UniformRandomProvider rng, int[] array) {
        int i = array.length;
        // The threshold provided in the Brackett-Rozinsky and Lemire paper
        // is the power of 2 below 20724. Note that the product 2^15*2^15
        // is representable using signed integers.
        for (; i > POW_15; i--) {
            swap(array, i - 1, rng.nextInt(i));
        }
        // Batches of 2 for sizes up to 2^15 elements
        final int[] productBound = {i * (i - 1)};
        for (; i > POW_9; i -= 2) {
            final int[] indices = randomBounded2(i, i - 1, productBound, rng);
            final int index1 = indices[0];
            final int index2 = indices[1];
            swap(array, i - 1, index1);
            swap(array, i - 2, index2);
        }
        // Batches of 3 for sizes up to 2^9 elements (power of 2 below 581)
        productBound[0] = i * (i - 1) * (i - 2);
        for (; i > 3; i -= 3) {
            final int[] indices = randomBounded3(i, i - 1, i - 2, productBound, rng);
            final int index1 = indices[0];
            final int index2 = indices[1];
            final int index3 = indices[2];
            swap(array, i - 1, index1);
            swap(array, i - 2, index2);
            swap(array, i - 3, index3);
        }
        // Finish
        for (; i > 1; i--) {
            swap(array, i - 1, rng.nextInt(i));
        }
        return array;
    }

    /**
     * Return four random values in {@code [0, range1)}, {@code [0, range2)},
     * {@code [0, range3)} and {@code [0, range4)}. The
     * product bound is used for the reject algorithm. See Brackett-Rozinsky and Lemire.
     *
     * <p>The product bound can be any positive integer {@code >= range1*range2*range3*range4}.
     * It may be updated to become {@code range1*range2*range3*range4}.
     *
     * @param range1 Range 1.
     * @param range2 Range 2.
     * @param range3 Range 3.
     * @param range4 Range 4.
     * @param productBound Product bound.
     * @param rng Source of randomness.
     * @return [i1, i2, i3, i4]
     */
    static int[] randomBounded4(int range1, int range2, int range3, int range4, int[] productBound,
            UniformRandomProvider rng) {
        long m = (rng.nextInt() & MASK_32) * range1;
        // result1 and result2 are the top 32-bits of the long
        long r1 = m;
        // Leftover bits * range2
        m = (m & MASK_32) * range2;
        long r2 = m;
        m = (m & MASK_32) * range3;
        long r3 = m;
        m = (m & MASK_32) * range4;
        long r4 = m;
        // Leftover bits must be unsigned
        long l = m & MASK_32;
        if (l < productBound[0]) {
            final int bound = range1 * range2 * range3 * range4;
            productBound[0] = bound;
            if (l < bound) {
                // 2^32 % bound
                final long t = POW_32 % bound;
                while (l < t) {
                    m = (rng.nextInt() & MASK_32) * range1;
                    r1 = m;
                    m = (m & MASK_32) * range2;
                    r2 = m;
                    m = (m & MASK_32) * range3;
                    r3 = m;
                    m = (m & MASK_32) * range4;
                    r4 = m;
                    l = m & MASK_32;
                }
            }
        }
        // Convert to [0, range1), [0, range2), [0, range3), [0, range4)
        return new int[] {(int) (r1 >> 32), (int) (r2 >> 32), (int) (r3 >> 32), (int) (r4 >> 32)};
    }

    /**
     * Shuffles the entries of the given array.
     *
     * @param rng Source of randomness.
     * @param array Array whose entries will be shuffled (in-place).
     * @return a reference to the given array
     */
    static int[] shuffle4(UniformRandomProvider rng, int[] array) {
        int i = array.length;
        // The threshold provided in the Brackett-Rozinsky and Lemire paper
        // is the power of 2 below 20724. Note that the product 2^15*2^15
        // is representable using signed integers.
        for (; i > POW_15; i--) {
            swap(array, i - 1, rng.nextInt(i));
        }
        // Batches of 2 for sizes up to 2^15 elements
        final int[] productBound = {i * (i - 1)};
        for (; i > POW_9; i -= 2) {
            final int[] indices = randomBounded2(i, i - 1, productBound, rng);
            final int index1 = indices[0];
            final int index2 = indices[1];
            swap(array, i - 1, index1);
            swap(array, i - 2, index2);
        }
        // Batches of 3 for sizes up to 2^9 elements (power of 2 below 581)
        productBound[0] = i * (i - 1) * (i - 2);
        for (; i > POW_6; i -= 3) {
            final int[] indices = randomBounded3(i, i - 1, i - 2, productBound, rng);
            final int index1 = indices[0];
            final int index2 = indices[1];
            final int index3 = indices[2];
            swap(array, i - 1, index1);
            swap(array, i - 2, index2);
            swap(array, i - 3, index3);
        }
        // Batches of 4 for sizes up to 2^6 elements (power of 2 below 109)
        productBound[0] = i * (i - 1) * (i - 2) * (i - 3);
        for (; i > 4; i -= 4) {
            final int[] indices = randomBounded4(i, i - 1, i - 2, i - 3, productBound, rng);
            final int index1 = indices[0];
            final int index2 = indices[1];
            final int index3 = indices[2];
            final int index4 = indices[3];
            swap(array, i - 1, index1);
            swap(array, i - 2, index2);
            swap(array, i - 3, index3);
            swap(array, i - 4, index4);
        }
        // Finish
        for (; i > 1; i--) {
            swap(array, i - 1, rng.nextInt(i));
        }
        return array;
    }

    /**
     * Performs a shuffle.
     *
     * @param data Shuffle data.
     * @param source Source of randomness.
     * @param method Shuffle method.
     * @return the shuffled data
     */
    @Benchmark
    public Object shuffle(ShuffleData data, RngSource source, ShuffleMethod method) {
        final int[] a = data.getData();
        method.getMethod().accept(source.getRNG(), a);
        return a;
    }
}
