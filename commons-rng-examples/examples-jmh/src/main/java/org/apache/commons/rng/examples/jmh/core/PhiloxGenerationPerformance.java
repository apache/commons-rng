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

import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;
import org.apache.commons.rng.UniformRandomProvider;
import org.apache.commons.rng.core.util.NumberFactory;
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
 * various Philox providers.
 *
 * <p>Performance Notes
 *
 * <p>Some improvements to performance are observed on JDK 8 for the inline and zero
 * variations of the 32-bit generator. On higher JDKs the performance of the original
 * implementation is consistently the best and the modified variants are either the
 * same speed but can be significantly slower. This typically occurs for the inline
 * variant which may not be efficiently targeted by the JVM optimisations.
 * The variations are left for future development.
 *
 * <p>The 64-bit generator can be made significantly faster if it uses the 64-bit
 * multiplication methods available in the {@link Math} class (see RNG-188).
 */
public class PhiloxGenerationPerformance extends AbstractBenchmark {
    /** The int value. Must NOT be final to prevent JVM optimisation! */
    private int intValue;
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
        @Param({"PHILOX_4X32_ORIGINAL",
                "PHILOX_4X32_CONST",
                "PHILOX_4X32_ZERO",
                "PHILOX_4X32_INLINE",
                "PHILOX_4X32",
                "PHILOX_4X64_ORIGINAL",
                "PHILOX_4X64_MH",
                "PHILOX_4X64_UMH",
                "PHILOX_4X64"})
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
            if ("PHILOX_4X32_ORIGINAL".equals(randomSourceName)) {
                provider = new Philox4x32Original(intSeed());
            } else if ("PHILOX_4X32_CONST".equals(randomSourceName)) {
                provider = new Philox4x32Const(intSeed());
            } else if ("PHILOX_4X32_ZERO".equals(randomSourceName)) {
                provider = new Philox4x32Zero(intSeed());
            } else if ("PHILOX_4X32_INLINE".equals(randomSourceName)) {
                provider = new Philox4x32Inline(intSeed());
            } else if ("PHILOX_4X64_ORIGINAL".equals(randomSourceName)) {
                provider = new Philox4x64Original(longSeed());
            } else if ("PHILOX_4X64_MH".equals(randomSourceName)) {
                provider = new Philox4x64MH(longSeed());
            } else if ("PHILOX_4X64_UMH".equals(randomSourceName)) {
                provider = new Philox4x64UMH(longSeed());
            } else {
                final RandomSource randomSource = RandomSource.valueOf(randomSourceName);
                provider = randomSource.create();
            }
        }

        /**
         * @return the int[] seed
         */
        private static int[] intSeed() {
            return ThreadLocalRandom.current().ints(6).toArray();
        }

        /**
         * @return the long[] seed
         */
        private static long[] longSeed() {
            return ThreadLocalRandom.current().longs(6).toArray();
        }
    }

    /**
     * Class adapted from the original implementation of Philox4x32.
     */
    static class Philox4x32Original implements UniformRandomProvider {
        /** Philox 32-bit mixing constant for counter 0. */
        protected static final int K_PHILOX_10_A = 0x9E3779B9;
        /** Philox 32-bit mixing constant for counter 1. */
        protected static final int K_PHILOX_10_B = 0xBB67AE85;
        /** Philox 32-bit constant for key 0. */
        protected static final int K_PHILOX_SA = 0xD2511F53;
        /** Philox 32-bit constant for key 1. */
        protected static final int K_PHILOX_SB = 0xCD9E8D57;
        /** Internal buffer size. */
        protected static final int PHILOX_BUFFER_SIZE = 4;

        /** Counter 0. */
        protected int counter0;
        /** Counter 1. */
        protected int counter1;
        /** Counter 2. */
        protected int counter2;
        /** Counter 3. */
        protected int counter3;
        /** Output buffer. */
        protected final int[] buffer = new int[PHILOX_BUFFER_SIZE];
        /** Key low bits. */
        protected int key0;
        /** Key high bits. */
        protected int key1;
        /** Output buffer index. When at the end of the buffer the counter is
         * incremented and the buffer regenerated. */
        protected int bufferPosition;

        /**
         * Creates a new instance based on an array of int containing, key (first two ints) and
         * the counter (next 4 ints, low bits = first int). The counter is not scrambled and may
         * be used to create contiguous blocks with size a multiple of 4 ints. For example,
         * setting seed[2] = 1 is equivalent to start with seed[2]=0 and calling {@link #next()} 4 times.
         *
         * @param seed Array of size 6 defining key0,key1,counter0,counter1,counter2,counter3.
         *             If the size is smaller, zero values are assumed.
         */
        Philox4x32Original(int[] seed) {
            final int[] input = seed.length < 6 ? Arrays.copyOf(seed, 6) : seed;
            key0 = input[0];
            key1 = input[1];
            counter0 = input[2];
            counter1 = input[3];
            counter2 = input[4];
            counter3 = input[5];
            bufferPosition = PHILOX_BUFFER_SIZE;
        }

        /** {@inheritDoc} */
        @Override
        public int nextInt() {
            final int p = bufferPosition;
            if (p < PHILOX_BUFFER_SIZE) {
                bufferPosition = p + 1;
                return buffer[p];
            }
            incrementCounter();
            rand10();
            bufferPosition = 1;
            return buffer[0];
        }

        /**
         * Increment the counter by one.
         */
        protected void incrementCounter() {
            if (++counter0 != 0) {
                return;
            }
            if (++counter1 != 0) {
                return;
            }
            if (++counter2 != 0) {
                return;
            }
            ++counter3;
        }

        /**
         * Perform 10 rounds, using counter0, counter1, counter2, counter3 as starting point.
         * It updates the buffer member variable, but no others.
         */
        protected void rand10() {
            buffer[0] = counter0;
            buffer[1] = counter1;
            buffer[2] = counter2;
            buffer[3] = counter3;

            int k0 = key0;
            int k1 = key1;

            //unrolled loop for performance
            singleRound(buffer, k0, k1);
            k0 += K_PHILOX_10_A;
            k1 += K_PHILOX_10_B;
            singleRound(buffer, k0, k1);
            k0 += K_PHILOX_10_A;
            k1 += K_PHILOX_10_B;
            singleRound(buffer, k0, k1);
            k0 += K_PHILOX_10_A;
            k1 += K_PHILOX_10_B;
            singleRound(buffer, k0, k1);
            k0 += K_PHILOX_10_A;
            k1 += K_PHILOX_10_B;
            singleRound(buffer, k0, k1);
            k0 += K_PHILOX_10_A;
            k1 += K_PHILOX_10_B;
            singleRound(buffer, k0, k1);
            k0 += K_PHILOX_10_A;
            k1 += K_PHILOX_10_B;
            singleRound(buffer, k0, k1);
            k0 += K_PHILOX_10_A;
            k1 += K_PHILOX_10_B;
            singleRound(buffer, k0, k1);
            k0 += K_PHILOX_10_A;
            k1 += K_PHILOX_10_B;
            singleRound(buffer, k0, k1);
            k0 += K_PHILOX_10_A;
            k1 += K_PHILOX_10_B;
            singleRound(buffer, k0, k1);
        }

        /**
         * Performs a single round of philox.
         *
         * @param counter Counter, which will be updated after each call.
         * @param key0 Key low bits.
         * @param key1 Key high bits.
         */
        protected static void singleRound(int[] counter, int key0, int key1) {
            final long product0 = (K_PHILOX_SA & 0xffff_ffffL) * (counter[0] & 0xffff_ffffL);
            final int hi0 = (int) (product0 >>> 32);
            final long product1 = (K_PHILOX_SB & 0xffff_ffffL) * (counter[2] & 0xffff_ffffL);
            final int hi1 = (int) (product1 >>> 32);

            counter[0] = hi1 ^ counter[1] ^ key0;
            counter[1] = (int) product1;
            counter[2] = hi0 ^ counter[3] ^ key1;
            counter[3] = (int) product0;
        }

        @Override
        public long nextLong() {
            return NumberFactory.makeLong(nextInt(), nextInt());
        }
    }

    /**
     * Updated to use pre-computed long constants.
     */
    static final class Philox4x32Const extends Philox4x32Original {
        /** Philox 32-bit constant for key 0 as an unsigned integer. */
        private static final long K_PHILOX_SAL = Integer.toUnsignedLong(0xD2511F53);
        /** Philox 32-bit constant for key 1 as an unsigned integer. */
        private static final long K_PHILOX_SBL = Integer.toUnsignedLong(0xCD9E8D57);

        /**
         * Creates a new instance.
         *
         * @param seed Seed.
         */
        Philox4x32Const(int[] seed) {
            super(seed);
        }

        @Override
        protected void rand10() {
            buffer[0] = counter0;
            buffer[1] = counter1;
            buffer[2] = counter2;
            buffer[3] = counter3;

            int k0 = key0;
            int k1 = key1;

            //unrolled loop for performance
            singleRoundL(buffer, k0, k1);
            k0 += K_PHILOX_10_A;
            k1 += K_PHILOX_10_B;
            singleRoundL(buffer, k0, k1);
            k0 += K_PHILOX_10_A;
            k1 += K_PHILOX_10_B;
            singleRoundL(buffer, k0, k1);
            k0 += K_PHILOX_10_A;
            k1 += K_PHILOX_10_B;
            singleRoundL(buffer, k0, k1);
            k0 += K_PHILOX_10_A;
            k1 += K_PHILOX_10_B;
            singleRoundL(buffer, k0, k1);
            k0 += K_PHILOX_10_A;
            k1 += K_PHILOX_10_B;
            singleRoundL(buffer, k0, k1);
            k0 += K_PHILOX_10_A;
            k1 += K_PHILOX_10_B;
            singleRoundL(buffer, k0, k1);
            k0 += K_PHILOX_10_A;
            k1 += K_PHILOX_10_B;
            singleRoundL(buffer, k0, k1);
            k0 += K_PHILOX_10_A;
            k1 += K_PHILOX_10_B;
            singleRoundL(buffer, k0, k1);
            k0 += K_PHILOX_10_A;
            k1 += K_PHILOX_10_B;
            singleRoundL(buffer, k0, k1);
        }

        /**
         * Performs a single round of philox.
         *
         * @param counter Counter, which will be updated after each call.
         * @param key0 Key low bits.
         * @param key1 Key high bits.
         */
        private static void singleRoundL(int[] counter, int key0, int key1) {
            final long product0 = K_PHILOX_SAL * (counter[0] & 0xffff_ffffL);
            final int hi0 = (int) (product0 >>> 32);
            final long product1 = K_PHILOX_SBL * (counter[2] & 0xffff_ffffL);
            final int hi1 = (int) (product1 >>> 32);

            counter[0] = hi1 ^ counter[1] ^ key0;
            counter[1] = (int) product1;
            counter[2] = hi0 ^ counter[3] ^ key1;
            counter[3] = (int) product0;
        }
    }

    /**
     * Updated to use count the buffer position down to zero.
     *
     * <p>Note: This implementation will output the incorrect order for each
     * block of 4. This is for simplicity during testing.
     */
    static final class Philox4x32Zero extends Philox4x32Original {
        /**
         * Creates a new instance.
         *
         * @param seed Seed.
         */
        Philox4x32Zero(int[] seed) {
            super(seed);
            bufferPosition = 0;
        }

        /** {@inheritDoc} */
        @Override
        public int nextInt() {
            int p = bufferPosition - 1;
            if (p < 0) {
                incrementCounter();
                rand10();
                p = 3;
            }
            bufferPosition = p;
            return buffer[p];
        }
    }

    /**
     * Updated to inline the entire buffer update with pre-computed long constants.
     */
    static final class Philox4x32Inline extends Philox4x32Original {
        /**
         * Creates a new instance.
         *
         * @param seed Seed.
         */
        Philox4x32Inline(int[] seed) {
            super(seed);
        }

        /**
         * Perform 10 rounds, using counter0, counter1, counter2, counter3 as starting point.
         * It updates the buffer member variable, but no others.
         */
        protected void rand10() {
            final long sa = K_PHILOX_SA & 0xffff_ffffL;
            final long sb = K_PHILOX_SB & 0xffff_ffffL;

            long c0 = counter0;
            int c1 = counter1;
            long c2 = counter2;
            int c3 = counter3;

            long product0;
            long product1;
            int hi0;
            int hi1;

            // Unrolled loop for performance
            int k0 = key0;
            int k1 = key1;
            product0 = sa * (c0 & 0xffff_ffffL);
            hi0 = (int) (product0 >>> 32);
            product1 = sb * (c2 & 0xffff_ffffL);
            hi1 = (int) (product1 >>> 32);

            c0 = hi1 ^ c1 ^ k0;
            c1 = (int) product1;
            c2 = hi0 ^ c3 ^ k1;
            c3 = (int) product0;

            k0 += K_PHILOX_10_A;
            k1 += K_PHILOX_10_B;
            product0 = sa * (c0 & 0xffff_ffffL);
            hi0 = (int) (product0 >>> 32);
            product1 = sb * (c2 & 0xffff_ffffL);
            hi1 = (int) (product1 >>> 32);

            c0 = hi1 ^ c1 ^ k0;
            c1 = (int) product1;
            c2 = hi0 ^ c3 ^ k1;
            c3 = (int) product0;

            k0 += K_PHILOX_10_A;
            k1 += K_PHILOX_10_B;
            product0 = sa * (c0 & 0xffff_ffffL);
            hi0 = (int) (product0 >>> 32);
            product1 = sb * (c2 & 0xffff_ffffL);
            hi1 = (int) (product1 >>> 32);

            c0 = hi1 ^ c1 ^ k0;
            c1 = (int) product1;
            c2 = hi0 ^ c3 ^ k1;
            c3 = (int) product0;

            k0 += K_PHILOX_10_A;
            k1 += K_PHILOX_10_B;
            product0 = sa * (c0 & 0xffff_ffffL);
            hi0 = (int) (product0 >>> 32);
            product1 = sb * (c2 & 0xffff_ffffL);
            hi1 = (int) (product1 >>> 32);

            c0 = hi1 ^ c1 ^ k0;
            c1 = (int) product1;
            c2 = hi0 ^ c3 ^ k1;
            c3 = (int) product0;

            k0 += K_PHILOX_10_A;
            k1 += K_PHILOX_10_B;
            product0 = sa * (c0 & 0xffff_ffffL);
            hi0 = (int) (product0 >>> 32);
            product1 = sb * (c2 & 0xffff_ffffL);
            hi1 = (int) (product1 >>> 32);

            c0 = hi1 ^ c1 ^ k0;
            c1 = (int) product1;
            c2 = hi0 ^ c3 ^ k1;
            c3 = (int) product0;

            k0 += K_PHILOX_10_A;
            k1 += K_PHILOX_10_B;
            product0 = sa * (c0 & 0xffff_ffffL);
            hi0 = (int) (product0 >>> 32);
            product1 = sb * (c2 & 0xffff_ffffL);
            hi1 = (int) (product1 >>> 32);

            c0 = hi1 ^ c1 ^ k0;
            c1 = (int) product1;
            c2 = hi0 ^ c3 ^ k1;
            c3 = (int) product0;

            k0 += K_PHILOX_10_A;
            k1 += K_PHILOX_10_B;
            product0 = sa * (c0 & 0xffff_ffffL);
            hi0 = (int) (product0 >>> 32);
            product1 = sb * (c2 & 0xffff_ffffL);
            hi1 = (int) (product1 >>> 32);

            c0 = hi1 ^ c1 ^ k0;
            c1 = (int) product1;
            c2 = hi0 ^ c3 ^ k1;
            c3 = (int) product0;

            k0 += K_PHILOX_10_A;
            k1 += K_PHILOX_10_B;
            product0 = sa * (c0 & 0xffff_ffffL);
            hi0 = (int) (product0 >>> 32);
            product1 = sb * (c2 & 0xffff_ffffL);
            hi1 = (int) (product1 >>> 32);

            c0 = hi1 ^ c1 ^ k0;
            c1 = (int) product1;
            c2 = hi0 ^ c3 ^ k1;
            c3 = (int) product0;

            k0 += K_PHILOX_10_A;
            k1 += K_PHILOX_10_B;
            product0 = sa * (c0 & 0xffff_ffffL);
            hi0 = (int) (product0 >>> 32);
            product1 = sb * (c2 & 0xffff_ffffL);
            hi1 = (int) (product1 >>> 32);

            c0 = hi1 ^ c1 ^ k0;
            c1 = (int) product1;
            c2 = hi0 ^ c3 ^ k1;
            c3 = (int) product0;

            k0 += K_PHILOX_10_A;
            k1 += K_PHILOX_10_B;
            product0 = sa * (c0 & 0xffff_ffffL);
            hi0 = (int) (product0 >>> 32);
            product1 = sb * (c2 & 0xffff_ffffL);
            hi1 = (int) (product1 >>> 32);

            c0 = hi1 ^ c1 ^ k0;
            c1 = (int) product1;
            c2 = hi0 ^ c3 ^ k1;
            c3 = (int) product0;

            buffer[0] = (int) c0;
            buffer[1] = c1;
            buffer[2] = (int) c2;
            buffer[3] = c3;
        }
    }

    /**
     * Class adapted from the original implementation of Philox4x64.
     */
    static class Philox4x64Original implements UniformRandomProvider {
        /** Philox 64-bit mixing constant for counter 0. */
        protected static final long PHILOX_M0 = 0xD2E7470EE14C6C93L;
        /** Philox 64-bit mixing constant for counter 1. */
        protected static final long PHILOX_M1 = 0xCA5A826395121157L;
        /** Philox 64-bit constant for key 0. */
        protected static final long PHILOX_W0 = 0x9E3779B97F4A7C15L;
        /** Philox 64-bit constant for key 1. */
        protected static final long PHILOX_W1 = 0xBB67AE8584CAA73BL;
        /** Internal buffer size. */
        protected static final int PHILOX_BUFFER_SIZE = 4;

        /** Counter 0. */
        protected long counter0;
        /** Counter 1. */
        protected long counter1;
        /** Counter 2. */
        protected long counter2;
        /** Counter 3. */
        protected long counter3;
        /** Output buffer. */
        protected final long[] buffer = new long[PHILOX_BUFFER_SIZE];
        /** Key low bits. */
        protected long key0;
        /** Key high bits. */
        protected long key1;
        /** Output buffer index. When at the end of the buffer the counter is
         * incremented and the buffer regenerated. */
        protected int bufferPosition;

        /**
         * Creates a new instance given 6 long numbers containing, key (first two longs) and
         * the counter (next 4 longs, low bits = first long). The counter is not scrambled and may
         * be used to create contiguous blocks with size a multiple of 4 longs. For example,
         * setting seed[2] = 1 is equivalent to start with seed[2]=0 and calling {@link #next()} 4 times.
         *
         * @param seed Array of size 6 defining key0,key1,counter0,counter1,counter2,counter3.
         *             If the size is smaller, zero values are assumed.
         */
        Philox4x64Original(long[] seed) {
            final long[] input = seed.length < 6 ? Arrays.copyOf(seed, 6) : seed;
            key0 = input[0];
            key1 = input[1];
            counter0 = input[2];
            counter1 = input[3];
            counter2 = input[4];
            counter3 = input[5];
            bufferPosition = PHILOX_BUFFER_SIZE;
        }

        /** {@inheritDoc} */
        @Override
        public long nextLong() {
            final int p = bufferPosition;
            if (p < PHILOX_BUFFER_SIZE) {
                bufferPosition = p + 1;
                return buffer[p];
            }
            incrementCounter();
            rand10();
            bufferPosition = 1;
            return buffer[0];
        }

        /**
         * Increment the counter by one.
         */
        protected void incrementCounter() {
            if (++counter0 != 0) {
                return;
            }
            if (++counter1 != 0) {
                return;
            }
            if (++counter2 != 0) {
                return;
            }
            ++counter3;
        }

        /**
         * Perform 10 rounds, using counter0, counter1, counter2, counter3 as starting point.
         * It updates the buffer member variable, but no others.
         */
        protected void rand10() {
            buffer[0] = counter0;
            buffer[1] = counter1;
            buffer[2] = counter2;
            buffer[3] = counter3;

            long k0 = key0;
            long k1 = key1;

            //unrolled loop for performance
            singleRound(buffer, k0, k1);
            k0 += PHILOX_W0;
            k1 += PHILOX_W1;
            singleRound(buffer, k0, k1);
            k0 += PHILOX_W0;
            k1 += PHILOX_W1;
            singleRound(buffer, k0, k1);
            k0 += PHILOX_W0;
            k1 += PHILOX_W1;
            singleRound(buffer, k0, k1);
            k0 += PHILOX_W0;
            k1 += PHILOX_W1;
            singleRound(buffer, k0, k1);
            k0 += PHILOX_W0;
            k1 += PHILOX_W1;
            singleRound(buffer, k0, k1);
            k0 += PHILOX_W0;
            k1 += PHILOX_W1;
            singleRound(buffer, k0, k1);
            k0 += PHILOX_W0;
            k1 += PHILOX_W1;
            singleRound(buffer, k0, k1);
            k0 += PHILOX_W0;
            k1 += PHILOX_W1;
            singleRound(buffer, k0, k1);
            k0 += PHILOX_W0;
            k1 += PHILOX_W1;
            singleRound(buffer, k0, k1);
        }


        /**
         * Performs a single round of philox.
         *
         * @param counter Counter, which will be updated after each call.
         * @param key0 Key low bits.
         * @param key1 Key high bits.
         */
        private static void singleRound(long[] counter, long key0, long key1) {
            final long lo0 = PHILOX_M0 * counter[0];
            final long hi0 = UnsignedMultiplyHighSource.unsignedMultiplyHigh(PHILOX_M0, counter[0]);
            final long lo1 = PHILOX_M1 * counter[2];
            final long hi1 = UnsignedMultiplyHighSource.unsignedMultiplyHigh(PHILOX_M1, counter[2]);

            counter[0] = hi1 ^ counter[1] ^ key0;
            counter[1] = lo1;
            counter[2] = hi0 ^ counter[3] ^ key1;
            counter[3] = lo0;
        }
    }

    /**
     * Updated to use a MethodHandle to Math.multiplyHigh (UMH) (Java 9+).
     * If not available then revert to a default implementation.
     */
    static final class Philox4x64MH extends Philox4x64Original {
        /**
         * Creates a new instance.
         *
         * @param seed Seed.
         */
        Philox4x64MH(long[] seed) {
            super(seed);
        }

        @Override
        protected void rand10() {
            buffer[0] = counter0;
            buffer[1] = counter1;
            buffer[2] = counter2;
            buffer[3] = counter3;

            long k0 = key0;
            long k1 = key1;

            //unrolled loop for performance
            singleRound(buffer, k0, k1);
            k0 += PHILOX_W0;
            k1 += PHILOX_W1;
            singleRound(buffer, k0, k1);
            k0 += PHILOX_W0;
            k1 += PHILOX_W1;
            singleRound(buffer, k0, k1);
            k0 += PHILOX_W0;
            k1 += PHILOX_W1;
            singleRound(buffer, k0, k1);
            k0 += PHILOX_W0;
            k1 += PHILOX_W1;
            singleRound(buffer, k0, k1);
            k0 += PHILOX_W0;
            k1 += PHILOX_W1;
            singleRound(buffer, k0, k1);
            k0 += PHILOX_W0;
            k1 += PHILOX_W1;
            singleRound(buffer, k0, k1);
            k0 += PHILOX_W0;
            k1 += PHILOX_W1;
            singleRound(buffer, k0, k1);
            k0 += PHILOX_W0;
            k1 += PHILOX_W1;
            singleRound(buffer, k0, k1);
            k0 += PHILOX_W0;
            k1 += PHILOX_W1;
            singleRound(buffer, k0, k1);
        }


        /**
         * Performs a single round of philox.
         *
         * @param counter Counter, which will be updated after each call.
         * @param key0 Key low bits.
         * @param key1 Key high bits.
         */
        private static void singleRound(long[] counter, long key0, long key1) {
            final long lo0 = PHILOX_M0 * counter[0];
            final long hi0 = UnsignedMultiplyHighSource.mhMultiplyHigh(PHILOX_M0, counter[0]);
            final long lo1 = PHILOX_M1 * counter[2];
            final long hi1 = UnsignedMultiplyHighSource.mhMultiplyHigh(PHILOX_M1, counter[2]);

            counter[0] = hi1 ^ counter[1] ^ key0;
            counter[1] = lo1;
            counter[2] = hi0 ^ counter[3] ^ key1;
            counter[3] = lo0;
        }
    }

    /**
     * Updated to use a MethodHandle to Math.unsignedMultiplyHigh (UMH) (Java 18+).
     * If not available then revert to a default implementation.
     */
    static final class Philox4x64UMH extends Philox4x64Original {
        /**
         * Creates a new instance.
         *
         * @param seed Seed.
         */
        Philox4x64UMH(long[] seed) {
            super(seed);
        }

        @Override
        protected void rand10() {
            buffer[0] = counter0;
            buffer[1] = counter1;
            buffer[2] = counter2;
            buffer[3] = counter3;

            long k0 = key0;
            long k1 = key1;

            //unrolled loop for performance
            singleRound(buffer, k0, k1);
            k0 += PHILOX_W0;
            k1 += PHILOX_W1;
            singleRound(buffer, k0, k1);
            k0 += PHILOX_W0;
            k1 += PHILOX_W1;
            singleRound(buffer, k0, k1);
            k0 += PHILOX_W0;
            k1 += PHILOX_W1;
            singleRound(buffer, k0, k1);
            k0 += PHILOX_W0;
            k1 += PHILOX_W1;
            singleRound(buffer, k0, k1);
            k0 += PHILOX_W0;
            k1 += PHILOX_W1;
            singleRound(buffer, k0, k1);
            k0 += PHILOX_W0;
            k1 += PHILOX_W1;
            singleRound(buffer, k0, k1);
            k0 += PHILOX_W0;
            k1 += PHILOX_W1;
            singleRound(buffer, k0, k1);
            k0 += PHILOX_W0;
            k1 += PHILOX_W1;
            singleRound(buffer, k0, k1);
            k0 += PHILOX_W0;
            k1 += PHILOX_W1;
            singleRound(buffer, k0, k1);
        }


        /**
         * Performs a single round of philox.
         *
         * @param counter Counter, which will be updated after each call.
         * @param key0 Key low bits.
         * @param key1 Key high bits.
         */
        private static void singleRound(long[] counter, long key0, long key1) {
            final long lo0 = PHILOX_M0 * counter[0];
            final long hi0 = UnsignedMultiplyHighSource.mhUnsignedMultiplyHigh(PHILOX_M0, counter[0]);
            final long lo1 = PHILOX_M1 * counter[2];
            final long hi1 = UnsignedMultiplyHighSource.mhUnsignedMultiplyHigh(PHILOX_M1, counter[2]);

            counter[0] = hi1 ^ counter[1] ^ key0;
            counter[1] = lo1;
            counter[2] = hi0 ^ counter[3] ^ key1;
            counter[3] = lo0;
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
     * Baseline for a JMH method call returning a {@code long}.
     *
     * @return the value
     */
    @Benchmark
    public long baselineLong() {
        return longValue;
    }

    /**
     * Exercise the {@link UniformRandomProvider#nextInt()} method.
     *
     * @param sources Source of randomness.
     * @return the int
     */
    @Benchmark
    public int nextInt(Sources sources) {
        return sources.getGenerator().nextInt();
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
