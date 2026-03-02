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

import java.util.concurrent.ThreadLocalRandom;
import org.apache.commons.rng.UniformRandomProvider;
import org.apache.commons.rng.examples.jmh.core.LXMBenchmark.UnsignedMultiplyHighSource;
import org.apache.commons.rng.simple.RandomSource;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;

/**
 * Executes benchmark to compare the speed of generation of random numbers from the
 * various 128-bit LXM providers. This targets use of different methods to compute the
 * 128-bit multiplication from two 64-bit numbers.
 */
public class LXMGenerationPerformance extends AbstractBenchmark {
    /** The long value. Must NOT be final to prevent JVM optimisation! */
    private long longValue;

    /**
     * The benchmark state (retrieve the various "RandomSource"s).
     */
    @State(Scope.Benchmark)
    public static class Sources {
        /**
         * RNG providers.
         */
        @Param({"L128_X128_MIX_ORIGINAL",
                "L128_X256_MIX_ORIGINAL",
                "L128_X1024_MIX_ORIGINAL",
                "L128_X128_MIX_MH",
                "L128_X128_MIX_MH",
                "L128_X1024_MIX_MH",
                "L128_X128_MIX",
                "L128_X256_MIX",
                "L128_X1024_MIX"})
        private String randomSourceName;

        /** RNG. */
        private UniformRandomProvider provider;

        /**
         * Gets the generator.
         *
         * @return the RNG.
         */
        public UniformRandomProvider getGenerator() {
            return provider;
        }

        /** Instantiates generator. This need only be done once per set of iterations. */
        @Setup(Level.Trial)
        public void setup() {
            if ("L128_X128_MIX_ORIGINAL".equals(randomSourceName)) {
                provider = new L128X128MixOriginal(longSeed(6));
            } else if ("L128_X256_MIX_ORIGINAL".equals(randomSourceName)) {
                provider = new L128X256MixOriginal(longSeed(8));
            } else if ("L128_X1024_MIX_ORIGINAL".equals(randomSourceName)) {
                provider = new L128X1024MixOriginal(longSeed(20));
            } else if ("L128_X128_MIX_MH".equals(randomSourceName)) {
                provider = new L128X128MixMH(longSeed(6));
            } else if ("L128_X256_MIX_MH".equals(randomSourceName)) {
                provider = new L128X256MixMH(longSeed(8));
            } else if ("L128_X1024_MIX_MH".equals(randomSourceName)) {
                provider = new L128X1024MixMH(longSeed(20));
            } else {
                final RandomSource randomSource = RandomSource.valueOf(randomSourceName);
                provider = randomSource.create();
            }
        }

        /**
         * @param size Seed size.
         * @return the long[] seed
         */
        private static long[] longSeed(int size) {
            return ThreadLocalRandom.current().longs(size).toArray();
        }
    }

    /**
     * Perform a 64-bit mixing function using Doug Lea's 64-bit mix constants and shifts.
     *
     * <p>This is based on the original 64-bit mix function of Austin Appleby's
     * MurmurHash3 modified to use a single mix constant and 32-bit shifts, which may have
     * a performance advantage on some processors. The code is provided in Steele and
     * Vigna's paper.
     *
     * @param x the input value
     * @return the output value
     */
    static long lea64(long x) {
        x = (x ^ (x >>> 32)) * 0xdaba0b6eb09322e3L;
        x = (x ^ (x >>> 32)) * 0xdaba0b6eb09322e3L;
        return x ^ (x >>> 32);
    }

    /**
     * Add the two values as if unsigned 64-bit longs to produce the high 64-bits
     * of the 128-bit unsigned result.
     *
     * <h2>Warning</h2>
     *
     * <p>This method is computing a carry bit for a 128-bit linear congruential
     * generator (LCG). The method is <em>not</em> applicable to all arguments.
     * Some computations can be dropped if the {@code right} argument is assumed to
     * be the LCG addition, which should be odd to ensure a full period LCG.
     *
     * @param left the left argument
     * @param right the right argument (assumed to have the lowest bit set to 1)
     * @return the carry (either 0 or 1)
     */
    static long unsignedAddHigh(long left, long right) {
        // Method compiles to 13 bytes as Java byte code.
        // This is below the default of 35 for code inlining.
        //
        // The unsigned add of left + right may have a 65-bit result.
        // If both values are shifted right by 1 then the sum will be
        // within a 64-bit long. The right is assumed to have a low
        // bit of 1 which has been lost in the shift. The method must
        // compute if a 1 was shifted off the left which would have
        // triggered a carry when adding to the right's assumed 1.
        // The intermediate 64-bit result is shifted
        // 63 bits to obtain the most significant bit of the 65-bit result.
        // Using -1 is the same as a shift of (64 - 1) as only the last 6 bits
        // are used by the shift but requires 1 less byte in java byte code.
        //
        //    01100001      left
        // +  10011111      right always has low bit set to 1
        //
        //    0110000   1   carry last bit of left
        // +  1001111   |
        // +        1 <-+
        // = 10000000       carry bit generated
        return ((left >>> 1) + (right >>> 1) + (left & 1)) >>> -1;
    }

    /**
     * Class adapted from the original implementation of LXM generators.
     */
    abstract static class AbstractL128 implements UniformRandomProvider {
        /** Low half of 128-bit LCG multiplier. The upper half is {@code 1L}. */
        static final long ML = 0xd605bbb58c8abbfdL;

        /** High half of the 128-bit per-instance LCG additive parameter.
         * Cannot be final to support RestorableUniformRandomProvider. */
        protected long lah;
        /** Low half of the 128-bit per-instance LCG additive parameter (must be odd).
         * Cannot be final to support RestorableUniformRandomProvider. */
        protected long lal;
        /** High half of the 128-bit state of the LCG generator. */
        protected long lsh;
        /** Low half of the 128-bit state of the LCG generator. */
        protected long lsl;

        /**
         * Creates a new instance.
         *
         * @param seed Initial seed.
         */
        AbstractL128(long[] seed) {
            lah = seed[0];
            // Additive parameter must be odd
            lal = seed[1] | 1;
            lsh = seed[2];
            lsl = seed[3];
        }
    }

    /**
     * Adapted from the original implementation of 128-bit LCG and 128-bit Xor-based generator.
     */
    static class L128X128MixOriginal extends AbstractL128 {
        /** State 0 of the XBG. */
        private long x0;
        /** State 1 of the XBG. */
        private long x1;

        /**
         * Creates a new instance.
         *
         * @param seed Initial seed.
         */
        L128X128MixOriginal(long[] seed) {
            super(seed);
            x0 = seed[4];
            x1 = seed[5];
        }

        /** {@inheritDoc} */
        @Override
        public long nextLong() {
            // LXM generate.
            // Old state is used for the output allowing parallel pipelining
            // on processors that support multiple concurrent instructions.

            final long s0 = x0;
            final long sh = lsh;

            // Mix
            final long z = lea64(sh + s0);

            // LCG update
            // The LCG is, in effect, "s = m * s + a" where m = ((1LL << 64) + ML)
            final long sl = lsl;
            final long al = lal;
            final long u = ML * sl;
            // High half
            lsh = ML * sh + UnsignedMultiplyHighSource.unsignedMultiplyHigh(ML, sl) + sl + lah +
                  // Carry propagation
                  unsignedAddHigh(u, al);
            // Low half
            lsl = u + al;

            // XBG update
            long s1 = x1;

            s1 ^= s0;
            x0 = Long.rotateLeft(s0, 24) ^ s1 ^ (s1 << 16); // a, b
            x1 = Long.rotateLeft(s1, 37); // c

            return z;
        }
    }

    /**
     * Adapted from the original implementation of 128-bit LCG and 256-bit Xor-based generator.
     */
    static class L128X256MixOriginal extends AbstractL128 {
        /** State 0 of the XBG. */
        private long x0;
        /** State 1 of the XBG. */
        private long x1;
        /** State 2 of the XBG. */
        private long x2;
        /** State 3 of the XBG. */
        private long x3;

        /**
         * Creates a new instance.
         *
         * @param seed Initial seed.
         */
        L128X256MixOriginal(long[] seed) {
            super(seed);
            x0 = seed[4];
            x1 = seed[5];
            x2 = seed[6];
            x3 = seed[7];
        }

        /** {@inheritDoc} */
        @Override
        public long nextLong() {
            // LXM generate.
            // Old state is used for the output allowing parallel pipelining
            // on processors that support multiple concurrent instructions.

            long s0 = x0;
            final long sh = lsh;

            // Mix
            final long z = lea64(sh + s0);

            // LCG update
            // The LCG is, in effect, "s = m * s + a" where m = ((1LL << 64) + ML)
            final long sl = lsl;
            final long al = lal;
            final long u = ML * sl;
            // High half
            lsh = ML * sh + UnsignedMultiplyHighSource.unsignedMultiplyHigh(ML, sl) + sl + lah +
                  // Carry propagation
                  unsignedAddHigh(u, al);
            // Low half
            lsl = u + al;

            // XBG update
            long s1 = x1;
            long s2 = x2;
            long s3 = x3;

            final long t = s1 << 17;

            s2 ^= s0;
            s3 ^= s1;
            s1 ^= s2;
            s0 ^= s3;

            s2 ^= t;

            s3 = Long.rotateLeft(s3, 45);

            x0 = s0;
            x1 = s1;
            x2 = s2;
            x3 = s3;

            return z;
        }
    }

    /**
     * Adapted from the original implementation of 128-bit LCG and 1024-bit Xor-based generator.
     */
    static class L128X1024MixOriginal extends AbstractL128 {
        /** State of the XBG. */
        private final long[] x = new long[20];
        /** Index in "state" array. */
        private int index;

        /**
         * Creates a new instance.
         *
         * @param seed Initial seed.
         */
        L128X1024MixOriginal(long[] seed) {
            super(seed);
            System.arraycopy(seed, 4, x, 0, 16);
            // Initialising to 15 ensures that (index + 1) % 16 == 0 and the
            // first state picked from the XBG generator is state[0].
            index = 15;
        }

        /** {@inheritDoc} */
        @Override
        public long nextLong() {
            // LXM generate.
            // Old state is used for the output allowing parallel pipelining
            // on processors that support multiple concurrent instructions.

            final int q = index;
            index = (q + 1) & 15;
            final long s0 = x[index];
            long s15 = x[q];
            final long sh = lsh;

            // Mix
            final long z = lea64(sh + s0);

            // LCG update
            // The LCG is, in effect, "s = m * s + a" where m = ((1LL << 64) + ML)
            final long sl = lsl;
            final long al = lal;
            final long u = ML * sl;
            // High half
            lsh = ML * sh + UnsignedMultiplyHighSource.unsignedMultiplyHigh(ML, sl) + sl + lah +
                  // Carry propagation
                  unsignedAddHigh(u, al);
            // Low half
            lsl = u + al;

            // XBG update
            s15 ^= s0;
            x[q] = Long.rotateLeft(s0, 25) ^ s15 ^ (s15 << 27);
            x[index] = Long.rotateLeft(s15, 36);

            return z;
        }
    }

    /**
     * Adapted from the original implementation of 128-bit LCG and 128-bit Xor-based generator
     * to use a dynamic call to the best available multiply high method.
     */
    static class L128X128MixMH extends AbstractL128 {
        /** State 0 of the XBG. */
        private long x0;
        /** State 1 of the XBG. */
        private long x1;

        /**
         * Creates a new instance.
         *
         * @param seed Initial seed.
         */
        L128X128MixMH(long[] seed) {
            super(seed);
            x0 = seed[4];
            x1 = seed[5];
        }

        /** {@inheritDoc} */
        @Override
        public long nextLong() {
            // LXM generate.
            // Old state is used for the output allowing parallel pipelining
            // on processors that support multiple concurrent instructions.

            final long s0 = x0;
            final long sh = lsh;

            // Mix
            final long z = lea64(sh + s0);

            // LCG update
            // The LCG is, in effect, "s = m * s + a" where m = ((1LL << 64) + ML)
            final long sl = lsl;
            final long al = lal;
            final long u = ML * sl;
            // High half
            lsh = ML * sh + UnsignedMultiplyHighSource.mhDynamicMultiplyHigh(ML, sl) + sl + lah +
                  // Carry propagation
                  unsignedAddHigh(u, al);
            // Low half
            lsl = u + al;

            // XBG update
            long s1 = x1;

            s1 ^= s0;
            x0 = Long.rotateLeft(s0, 24) ^ s1 ^ (s1 << 16); // a, b
            x1 = Long.rotateLeft(s1, 37); // c

            return z;
        }
    }

    /**
     * Adapted from the original implementation of 128-bit LCG and 256-bit Xor-based generator
     * to use a dynamic call to the best available multiply high method.
     */
    static class L128X256MixMH extends AbstractL128 {
        /** State 0 of the XBG. */
        private long x0;
        /** State 1 of the XBG. */
        private long x1;
        /** State 2 of the XBG. */
        private long x2;
        /** State 3 of the XBG. */
        private long x3;

        /**
         * Creates a new instance.
         *
         * @param seed Initial seed.
         */
        L128X256MixMH(long[] seed) {
            super(seed);
            x0 = seed[4];
            x1 = seed[5];
            x2 = seed[6];
            x3 = seed[7];
        }

        /** {@inheritDoc} */
        @Override
        public long nextLong() {
            // LXM generate.
            // Old state is used for the output allowing parallel pipelining
            // on processors that support multiple concurrent instructions.

            long s0 = x0;
            final long sh = lsh;

            // Mix
            final long z = lea64(sh + s0);

            // LCG update
            // The LCG is, in effect, "s = m * s + a" where m = ((1LL << 64) + ML)
            final long sl = lsl;
            final long al = lal;
            final long u = ML * sl;
            // High half
            lsh = ML * sh + UnsignedMultiplyHighSource.mhDynamicMultiplyHigh(ML, sl) + sl + lah +
                  // Carry propagation
                  unsignedAddHigh(u, al);
            // Low half
            lsl = u + al;

            // XBG update
            long s1 = x1;
            long s2 = x2;
            long s3 = x3;

            final long t = s1 << 17;

            s2 ^= s0;
            s3 ^= s1;
            s1 ^= s2;
            s0 ^= s3;

            s2 ^= t;

            s3 = Long.rotateLeft(s3, 45);

            x0 = s0;
            x1 = s1;
            x2 = s2;
            x3 = s3;

            return z;
        }
    }

    /**
     * Adapted from the original implementation of 128-bit LCG and 1024-bit Xor-based generator
     * to use a dynamic call to the best available multiply high method.
     */
    static class L128X1024MixMH extends AbstractL128 {
        /** State of the XBG. */
        private final long[] x = new long[20];
        /** Index in "state" array. */
        private int index;

        /**
         * Creates a new instance.
         *
         * @param seed Initial seed.
         */
        L128X1024MixMH(long[] seed) {
            super(seed);
            System.arraycopy(seed, 4, x, 0, 16);
            // Initialising to 15 ensures that (index + 1) % 16 == 0 and the
            // first state picked from the XBG generator is state[0].
            index = 15;
        }

        /** {@inheritDoc} */
        @Override
        public long nextLong() {
            // LXM generate.
            // Old state is used for the output allowing parallel pipelining
            // on processors that support multiple concurrent instructions.

            final int q = index;
            index = (q + 1) & 15;
            final long s0 = x[index];
            long s15 = x[q];
            final long sh = lsh;

            // Mix
            final long z = lea64(sh + s0);

            // LCG update
            // The LCG is, in effect, "s = m * s + a" where m = ((1LL << 64) + ML)
            final long sl = lsl;
            final long al = lal;
            final long u = ML * sl;
            // High half
            lsh = ML * sh + UnsignedMultiplyHighSource.mhDynamicMultiplyHigh(ML, sl) + sl + lah +
                  // Carry propagation
                  unsignedAddHigh(u, al);
            // Low half
            lsl = u + al;

            // XBG update
            s15 ^= s0;
            x[q] = Long.rotateLeft(s0, 25) ^ s15 ^ (s15 << 27);
            x[index] = Long.rotateLeft(s15, 36);

            return z;
        }
    }

    /**
     * Baseline for a JMH method call returning a {@code long}.
     *
     * @return the value
     */
    @Benchmark
    public long baselineLong() {
        return longValue;
    }

    /**
     * Exercise the {@link UniformRandomProvider#nextLong()} method.
     *
     * @param sources Source of randomness.
     * @return the long
     */
    @Benchmark
    public long nextLong(Sources sources) {
        return sources.getGenerator().nextLong();
    }
}
