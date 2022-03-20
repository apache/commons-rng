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

import java.util.function.IntSupplier;
import org.apache.commons.rng.UniformRandomProvider;
import org.apache.commons.rng.core.source64.LongProvider;
import org.apache.commons.rng.examples.jmh.RandomSources;
import org.apache.commons.rng.simple.RandomSource;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;

/**
 * Executes a benchmark to compare the speed of generation of random numbers from the
 * various source providers using the bit cache verses simple generation.
 */
public class CachedNextGenerationPerformance extends AbstractBenchmark {
    /** The value. Must NOT be final to prevent JVM optimisation! */
    private boolean booleanValue;
    /** The value. Must NOT be final to prevent JVM optimisation! */
    private int intValue;

    /**
     * Provides a function to obtain a boolean value from the various "RandomSource"s.
     * It exercise the default nextBoolean() method (which may use
     * a cache of bits) against a sign test on the native output.
     */
    @State(Scope.Benchmark)
    public static class BooleanSources extends RandomSources {
        /** Functional interface for a boolean generator. */
        interface BooleanSupplier {
            /**
             * @return the boolean
             */
            boolean getAsBoolean();
        }

        /**
         * The method to create the boolean value.
         */
        @Param({"nextBoolean", "signTest"})
        private String method;

        /** The generator. */
        private BooleanSupplier generator;

        /**
         * @return the next boolean
         */
        boolean next() {
            return generator.getAsBoolean();
        }

        /** {@inheritDoc} */
        @Override
        @Setup
        public void setup() {
            // Create the generator.
            super.setup();
            final UniformRandomProvider rng = getGenerator();

            // Create the method to generate the boolean
            if ("signTest".equals(method)) {
                if (rng instanceof LongProvider) {
                    generator = () -> rng.nextLong() < 0;
                } else {
                    // Assumed IntProvider
                    generator = () -> rng.nextInt() < 0;
                }
            } else if ("nextBoolean".equals(method)) {
                // Do not use a method handle 'rng::nextBoolean' for the nextBoolean
                // to attempt to maintain a comparable lambda function. The JVM may
                // optimise this away.
                generator = () -> rng.nextBoolean();
            } else {
                throw new IllegalStateException("Unknown boolean method: " + method);
            }
        }
    }
    /**
     * Provides a function to obtain an int value from the various "RandomSource"s
     * that produce 64-bit output.
     * It exercise the default nextInt() method (which may use
     * a cache of bits) against a shift on the native output.
     */
    @State(Scope.Benchmark)
    public static class IntSources {
        /**
         * RNG providers. This list is maintained in the order of the {@link RandomSource} enum.
         *
         * <p>Include only those that are a {@link LongProvider}.</p>
         */
        @Param({"SPLIT_MIX_64",
                "XOR_SHIFT_1024_S",
                "TWO_CMRES",
                "MT_64",
                "XOR_SHIFT_1024_S_PHI",
                "XO_RO_SHI_RO_128_PLUS",
                "XO_RO_SHI_RO_128_SS",
                "XO_SHI_RO_256_PLUS",
                "XO_SHI_RO_256_SS",
                "XO_SHI_RO_512_PLUS",
                "XO_SHI_RO_512_SS",
                "PCG_RXS_M_XS_64",
                "SFC_64",
                "JSF_64",
                "XO_RO_SHI_RO_128_PP",
                "XO_SHI_RO_256_PP",
                "XO_SHI_RO_512_PP",
                "XO_RO_SHI_RO_1024_PP",
                "XO_RO_SHI_RO_1024_S",
                "XO_RO_SHI_RO_1024_SS",
                "PCG_RXS_M_XS_64_OS"})
        private String randomSourceName;

        /**
         * The method to create the int value.
         */
        @Param({"nextInt", "shiftLong"})
        private String method;

        /** The generator. */
        private IntSupplier gen;

        /**
         * @return the next int
         */
        int next() {
            return gen.getAsInt();
        }

        /** Create the int source. */
        @Setup
        public void setup() {
            final UniformRandomProvider rng = RandomSource.valueOf(randomSourceName).create();
            if (!(rng instanceof LongProvider)) {
                throw new IllegalStateException("Not a LongProvider: " + rng.getClass().getName());
            }

            // Create the method to generate the int
            if ("shiftLong".equals(method)) {
                gen = () -> (int) (rng.nextLong() >>> 32);
            } else if ("nextInt".equals(method)) {
                // Do not use a method handle 'rng::nextInt' for the nextInt
                // to attempt to maintain a comparable lambda function. The JVM may
                // optimise this away.
                gen = () -> rng.nextInt();
            } else {
                throw new IllegalStateException("Unknown int method: " + method);
            }
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
     * Baseline for a JMH method call returning a {@code boolean}.
     *
     * @return the value
     */
    @Benchmark
    public boolean baselineBoolean() {
        return booleanValue;
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
     * Exercise the boolean generation method.
     *
     * @param sources Source of randomness.
     * @return the boolean
     */
    @Benchmark
    public boolean nextBoolean(BooleanSources sources) {
        return sources.next();
    }

    /**
     * Exercise the int generation method.
     *
     * @param sources Source of randomness.
     * @return the int
     */
    @Benchmark
    public int nextInt(IntSources sources) {
        return sources.next();
    }
}
