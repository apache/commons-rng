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
import org.apache.commons.rng.core.source32.IntProvider;
import org.apache.commons.rng.sampling.PermutationSampler;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OperationsPerInvocation;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/**
 * Executes benchmark to compare the speed of random number generators to create
 * an int value in a range.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@State(Scope.Benchmark)
@Fork(value = 1, jvmArgs = { "-server", "-Xms128M", "-Xmx128M" })
public class RngNextIntInRangeBenchmark {
    /** The value. Must NOT be final to prevent JVM optimisation! */
    private int intValue;

    /**
     * The upper range for the {@code int} generation.
     */
    @State(Scope.Benchmark)
    public static class IntRange {
        /**
         * The upper range for the {@code int} generation.
         *
         * <p>Note that the while loop uses a rejection algorithm. From the Javadoc for
         * java.util.Random:</p>
         *
         * <pre>
         * "The probability of a value being rejected depends on n. The
         * worst case is n=2^30+1, for which the probability of a reject is 1/2,
         * and the expected number of iterations before the loop terminates is 2."
         * </pre>
         */
        @Param({"16", "17", "256", "257", "4096", "4097",
                // Worst case power-of-2: (1 << 30)
                "1073741824",
                // Worst case: (1 << 30) + 1
                "1073741825", })
        private int n;

        /**
         * Gets the upper bound {@code n}.
         *
         * @return the upper bound
         */
        public int getN() {
            return n;
        }
    }

    /**
     * The data used for the shuffle benchmark.
     */
    @State(Scope.Benchmark)
    public static class IntData {
        /**
         * The size of the data.
         */
        @Param({ "4", "16", "256", "4096", "16384" })
        private int size;

        /** The data. */
        private int[] data;

        /**
         * Gets the data.
         *
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
            data = PermutationSampler.natural(size);
        }
    }

    /**
     * The source generator.
     */
    @State(Scope.Benchmark)
    public static class Source {
        /**
         * The name of the generator.
         */
        @Param({ "jdk", "jdkPow2", "lemire", "lemirePow2", "lemire31", "lemire31Pow2"})
        private String name;

        /** The random generator. */
        private UniformRandomProvider rng;

        /**
         * Gets the random generator.
         *
         * @return the generator
         */
        public UniformRandomProvider getRng() {
            return rng;
        }

        /** Create the generator. */
        @Setup
        public void setup() {
            final long seed = ThreadLocalRandom.current().nextLong();
            if ("jdk".equals(name)) {
                rng = new JDK(seed);
            } else if ("jdkPow2".equals(name)) {
                rng = new JDKPow2(seed);
            } else if ("lemire".equals(name)) {
                rng = new Lemire(seed);
            } else if ("lemirePow2".equals(name)) {
                rng = new LemirePow2(seed);
            } else if ("lemire31".equals(name)) {
                rng = new Lemire31(seed);
            } else if ("lemire31Pow2".equals(name)) {
                rng = new Lemire31Pow2(seed);
            }
        }
    }

    /**
     * Implement the SplitMix algorithm from {@link java.util.SplittableRandom
     * SplittableRandom} to output 32-bit int values.
     *
     * <p>This is a base generator to test nextInt(int) methods.
     */
    abstract static class SplitMix32 extends IntProvider {
        /**
         * The golden ratio, phi, scaled to 64-bits and rounded to odd.
         */
        private static final long GOLDEN_RATIO = 0x9e3779b97f4a7c15L;

        /** The state. */
        protected long state;

        /**
         * Create a new instance.
         *
         * @param seed the seed
         */
        SplitMix32(long seed) {
            state = seed;
        }

        @Override
        public int next() {
            long key = state += GOLDEN_RATIO;
            // 32 high bits of Stafford variant 4 mix64 function as int:
            // http://zimbry.blogspot.com/2011/09/better-bit-mixing-improving-on.html
            key = (key ^ (key >>> 33)) * 0x62a9d9ed799705f5L;
            return (int) (((key ^ (key >>> 28)) * 0xcb24d0a5c88c35b3L) >>> 32);
        }

        /**
         * Check the value is strictly positive.
         *
         * @param n the value
         */
        void checkStrictlyPositive(int n) {
            if (n <= 0) {
                throw new IllegalArgumentException("not strictly positive: " + n);
            }
        }
    }

    /**
     * Implement the nextInt(int) method of the JDK excluding the case for a power-of-2 range.
     */
    static class JDK extends SplitMix32 {
        /**
         * Create a new instance.
         *
         * @param seed the seed
         */
        JDK(long seed) {
            super(seed);
        }

        @Override
        public int nextInt(int n) {
            checkStrictlyPositive(n);

            int bits;
            int val;
            do {
                bits = next() >>> 1;
                val = bits % n;
            } while (bits - val + n - 1 < 0);

            return val;
        }
    }

    /**
     * Implement the nextInt(int) method of the JDK with a case for a power-of-2 range.
     */
    static class JDKPow2 extends SplitMix32 {
        /**
         * Create a new instance.
         *
         * @param seed the seed
         */
        JDKPow2(long seed) {
            super(seed);
        }

        @Override
        public int nextInt(int n) {
            checkStrictlyPositive(n);

            final int nm1 = n - 1;
            if ((n & nm1) == 0) {
                // Power of 2
                return next() & nm1;
            }

            int bits;
            int val;
            do {
                bits = next() >>> 1;
                val = bits % n;
            } while (bits - val + nm1 < 0);

            return val;
        }
    }

    /**
     * Implement the nextInt(int) method of Lemire (2019).
     *
     * @see <a href="https://arxiv.org/abs/1805.10941SplittableRandom"> Lemire
     * (2019): Fast Random Integer Generation in an Interval</a>
     */
    static class Lemire extends SplitMix32 {
        /** 2^32. */
        static final long POW_32 = 1L << 32;

        /**
         * Create a new instance.
         *
         * @param seed the seed
         */
        Lemire(long seed) {
            super(seed);
        }

        @Override
        public int nextInt(int n) {
            checkStrictlyPositive(n);

            long m = (next() & 0xffffffffL) * n;
            long l = m & 0xffffffffL;
            if (l < n) {
                // 2^32 % n
                final long t = POW_32 % n;
                while (l < t) {
                    m = (next() & 0xffffffffL) * n;
                    l = m & 0xffffffffL;
                }
            }
            return (int) (m >>> 32);
        }
    }

    /**
     * Implement the nextInt(int) method of Lemire (2019) with a case for a power-of-2 range.
     */
    static class LemirePow2 extends SplitMix32 {
        /** 2^32. */
        static final long POW_32 = 1L << 32;

        /**
         * Create a new instance.
         *
         * @param seed the seed
         */
        LemirePow2(long seed) {
            super(seed);
        }

        @Override
        public int nextInt(int n) {
            checkStrictlyPositive(n);

            final int nm1 = n - 1;
            if ((n & nm1) == 0) {
                // Power of 2
                return next() & nm1;
            }

            long m = (next() & 0xffffffffL) * n;
            long l = m & 0xffffffffL;
            if (l < n) {
                // 2^32 % n
                final long t = POW_32 % n;
                while (l < t) {
                    m = (next() & 0xffffffffL) * n;
                    l = m & 0xffffffffL;
                }
            }
            return (int) (m >>> 32);
        }
    }

    /**
     * Implement the nextInt(int) method of Lemire (2019) modified to 31-bit arithmetic to use
     * an int modulus operation.
     */
    static class Lemire31 extends SplitMix32 {
        /** 2^32. */
        static final long POW_32 = 1L << 32;

        /**
         * Create a new instance.
         *
         * @param seed the seed
         */
        Lemire31(long seed) {
            super(seed);
        }

        @Override
        public int nextInt(int n) {
            checkStrictlyPositive(n);

            long m = (nextInt() & 0x7fffffffL) * n;
            long l = m & 0x7fffffffL;
            if (l < n) {
                // 2^31 % n
                final long t = (Integer.MIN_VALUE - n) % n;
                while (l < t) {
                    m = (nextInt() & 0x7fffffffL) * n;
                    l = m & 0x7fffffffL;
                }
            }
            return (int) (m >>> 31);
        }
    }

    /**
     * Implement the nextInt(int) method of Lemire (2019) modified to 31-bit arithmetic to use
     * an int modulus operation, with a case for a power-of-2 range.
     */
    static class Lemire31Pow2 extends SplitMix32 {
        /** 2^32. */
        static final long POW_32 = 1L << 32;

        /**
         * Create a new instance.
         *
         * @param seed the seed
         */
        Lemire31Pow2(long seed) {
            super(seed);
        }

        @Override
        public int nextInt(int n) {
            checkStrictlyPositive(n);

            final int nm1 = n - 1;
            if ((n & nm1) == 0) {
                // Power of 2
                return next() & nm1;
            }

            long m = (nextInt() & 0x7fffffffL) * n;
            long l = m & 0x7fffffffL;
            if (l < n) {
                // 2^31 % n
                final long t = (Integer.MIN_VALUE - n) % n;
                while (l < t) {
                    m = (nextInt() & 0x7fffffffL) * n;
                    l = m & 0x7fffffffL;
                }
            }
            return (int) (m >>> 31);
        }
    }

    /**
     * Baseline for a JMH method call returning an {@code int}.
     *
     * @return the value
     */
    @Benchmark
    public int baselineInt() {
        return intValue;
    }

    /**
     * Exercise the {@link UniformRandomProvider#nextInt()} method.
     *
     * @param range the range
     * @param source Source of randomness.
     * @return the int
     */
    @Benchmark
    public int nextIntN(IntRange range, Source source) {
        return source.getRng().nextInt(range.getN());
    }

    /**
     * Exercise the {@link UniformRandomProvider#nextInt()} method in a loop.
     *
     * @param range the range
     * @param source Source of randomness.
     * @return the int
     */
    @Benchmark
    @OperationsPerInvocation(65536)
    public int nextIntNloop65536(IntRange range, Source source) {
        int sum = 0;
        for (int i = 0; i < 65536; i++) {
            sum += source.getRng().nextInt(range.getN());
        }
        return sum;
    }

    /**
     * Exercise the {@link UniformRandomProvider#nextInt(int)} method by shuffling
     * data.
     *
     * @param data the data
     * @param source Source of randomness.
     * @return the shuffle data
     */
    @Benchmark
    public int[] shuffle(IntData data, Source source) {
        final int[] array = data.getData();
        shuffle(array, source.getRng());
        return array;
    }

    /**
     * Perform a Fischer-Yates shuffle.
     *
     * @param array the array
     * @param rng the random generator
     */
    private static void shuffle(int[] array, UniformRandomProvider rng) {
        for (int i = array.length - 1; i > 0; i--) {
            // Swap index with any position down to 0
            final int j = rng.nextInt(i);
            final int tmp = array[j];
            array[j] = array[i];
            array[i] = tmp;
        }
    }

    /**
     * Exercise the {@link UniformRandomProvider#nextInt(int)} method by creating
     * random indices for shuffling data.
     *
     * @param data the data
     * @param source Source of randomness.
     * @return the sum
     */
    @Benchmark
    public int pseudoShuffle(IntData data, Source source) {
        int sum = 0;
        for (int i = data.getData().length - 1; i > 0; i--) {
            sum += source.getRng().nextInt(i);
        }
        return sum;
    }
}
