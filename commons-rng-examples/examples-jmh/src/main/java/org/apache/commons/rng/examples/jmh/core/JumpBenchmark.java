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

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.function.ToLongFunction;
import org.apache.commons.rng.ArbitrarilyJumpableUniformRandomProvider;
import org.apache.commons.rng.JumpableUniformRandomProvider;
import org.apache.commons.rng.LongJumpableUniformRandomProvider;
import org.apache.commons.rng.UniformRandomProvider;
import org.apache.commons.rng.core.source32.IntProvider;
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
 * Executes benchmark for jump operations of jumpable RNGs.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@State(Scope.Benchmark)
@Fork(value = 1, jvmArgs = { "-server", "-Xms128M", "-Xmx128M" })
public class JumpBenchmark {
    /**
     * Encapsulates a method to jump an RNG.
     */
    @State(Scope.Benchmark)
    public abstract static class BaseJumpableSource {
        /**
         * Distance to skip before jumping.
         * Used to advance the state of a generator before repeat jumps.
         */
        @Param({"0"})
        private int skip;

        /** The generator of the next RNG copy from a jump. */
        private Supplier<UniformRandomProvider> gen;

        /**
         * Perform a jump.
         *
         * @return the value
         */
        UniformRandomProvider jump() {
            return gen.get();
        }

        /**
         * Create the jump function.
         */
        @Setup
        public void setup() {
            gen = createJumpFunction();
        }

        /**
         * Creates the RNG.
         *
         * @param randomSourceName the random source name
         * @return the RNG
         */
        UniformRandomProvider createRNG(String randomSourceName) {
            final UniformRandomProvider rng = RandomSource.valueOf(randomSourceName).create();
            if (skip > 0) {
                // Skip the primary output of the generator.
                // Assumes either 32-bit or 64-bit output.
                final ToLongFunction<UniformRandomProvider> fun = rng instanceof IntProvider ?
                    UniformRandomProvider::nextInt :
                    UniformRandomProvider::nextLong;
                for (int i = skip; --i >= 0;) {
                    fun.applyAsLong(rng);
                }
            }
            return rng;
        }

        /**
         * Creates the jump function.
         * The jump will copy the RNG and then move forward the state of the source
         * RNG by a large number of steps. The copy is returned.
         *
         * @return the copy RNG
         */
        protected abstract Supplier<UniformRandomProvider> createJumpFunction();
    }

    /**
     * Exercise the {@link JumpableUniformRandomProvider#jump()} function.
     */
    public static class JumpableSource extends BaseJumpableSource {
        /**
         * RNG providers.
         *
         * <p>Note: Some providers have exactly the same jump method and state size
         * so are commented out.
         */
        @Param({"XOR_SHIFT_1024_S",
                //"XOR_SHIFT_1024_S_PHI",
                "XO_SHI_RO_128_PLUS",
                //"XO_SHI_RO_128_SS",
                "XO_RO_SHI_RO_128_PLUS",
                //"XO_RO_SHI_RO_128_SS",
                "XO_SHI_RO_256_PLUS",
                //"XO_SHI_RO_256_SS",
                "XO_SHI_RO_512_PLUS",
                //"XO_SHI_RO_512_SS",
                //"XO_SHI_RO_128_PP",
                "XO_RO_SHI_RO_128_PP", // Different update from XO_SHI_RO_128_PLUS
                //"XO_SHI_RO_256_PP",
                //"XO_SHI_RO_512_PP",
                "XO_RO_SHI_RO_1024_PP",
                //"XO_RO_SHI_RO_1024_S",
                //"XO_RO_SHI_RO_1024_SS",
                // Although the LXM jump is the same for all generators with the same LCG
                // the performance is different as it captures state copy overhead.
                //"L64_X128_SS",
                "L64_X128_MIX",
                "L64_X256_MIX",
                "L64_X1024_MIX",
                "L128_X128_MIX",
                "L128_X256_MIX",
                "L128_X1024_MIX",
                "L32_X64_MIX",
                "PHILOX_4X32",
                "PHILOX_4X64"})
        private String randomSourceName;

        /** {@inheritDoc} */
        @Override
        protected Supplier<UniformRandomProvider> createJumpFunction() {
            final UniformRandomProvider rng = createRNG(randomSourceName);
            if (rng instanceof JumpableUniformRandomProvider) {
                return ((JumpableUniformRandomProvider) rng)::jump;
            }
            throw new IllegalStateException("Invalid jump source: " + randomSourceName);
        }
    }

    /**
     * Exercise the {@link LongJumpableUniformRandomProvider#longJump()} function.
     *
     * <p>Note: Any RNG with a long jump function also has a jump function.
     * This list should be a subset of {@link JumpableSource}. Testing both methods
     * is redundant unless the long jump function requires a different routine.
     * Providers listed here have a different long jump and may be slower/faster
     * than the corresponding jump function.
     *
     * <p>Note: To test other providers the benchmark may be invoked using the
     * JMH command line:
     * <pre>
     * java -jar target/examples-jmh.jar JumpBenchmark.longJump -p randomSourceName=L64_X128_MIX,L128_X128_MIX
     * </pre>
     */
    public static class LongJumpableSource extends BaseJumpableSource {
        /**
         * Select RNG providers.
         */
        @Param({
            // Requires the LCG to be advanced 2^32 rather than 1 cycle which
            // can use precomputed coefficients.
            "L64_X128_MIX",
            "L64_X256_MIX",
            "L64_X1024_MIX",
            // Requires the LCG to be advanced 2^64 rather than 1 cycle which
            // leaves the entire lower state unchanged and is computationally simpler
            // using precomputed coefficients.
            "L128_X128_MIX",
            "L128_X256_MIX",
            "L128_X1024_MIX",
            // Requires the LCG to be advanced 2^16 rather than 1 cycle which
            // can use precomputed coefficients.
            "L32_X64_MIX"})
        private String randomSourceName;


        /** {@inheritDoc} */
        @Override
        protected Supplier<UniformRandomProvider> createJumpFunction() {
            final UniformRandomProvider rng = createRNG(randomSourceName);
            if (rng instanceof LongJumpableUniformRandomProvider) {
                return ((LongJumpableUniformRandomProvider) rng)::longJump;
            }
            throw new IllegalStateException("Invalid long jump source: " + randomSourceName);
        }
    }

    /**
     * Exercise the {@link ArbitrarilyJumpableUniformRandomProvider#jump(double)} function,
     * or the {@link ArbitrarilyJumpableUniformRandomProvider#jumpPowerOfTwo(int)} function.
     *
     * <p>The power-of-two jump function is called if the distance is an exact {@code int} value.
     *
     * <p>To jump a small arbitrary amount specify the distance with a fractional component,
     * e.g. jump 123 using 123.5, otherwise a power-of-2 jump of 123 will be called.
     */
    public static class ArbitrarilyJumpableSource extends BaseJumpableSource {
        /**
         * Select RNG providers.
         */
        @Param({
            "PHILOX_4X32",
            "PHILOX_4X64"})
        private String randomSourceName;

        /** Distance to jump.
         * Default: 2^99 + 2^49; 2^99 */
        @Param({"6.338253001141153E29", "99"})
        private double distance;

        /** {@inheritDoc} */
        @Override
        protected Supplier<UniformRandomProvider> createJumpFunction() {
            final UniformRandomProvider rng = createRNG(randomSourceName);
            if (rng instanceof ArbitrarilyJumpableUniformRandomProvider) {
                final ArbitrarilyJumpableUniformRandomProvider gen = (ArbitrarilyJumpableUniformRandomProvider) rng;
                // Switch to a power-of-2 jump if an int
                final int logDistance = (int) distance;
                if ((double) logDistance == distance) {
                    return () -> gen.jumpPowerOfTwo(logDistance);
                }
                final double jumpDistance = Math.floor(distance);
                return () -> gen.jump(jumpDistance);
            }
            throw new IllegalStateException("Invalid arbitrary jump source: " + randomSourceName);
        }
    }

    /**
     * Jump benchmark.
     *
     * @param data Source of the jump
     * @return the copy
     */
    @Benchmark
    public UniformRandomProvider jump(JumpableSource data) {
        return data.jump();
    }

    /**
     * Long jump benchmark.
     *
     * @param data Source of the jump
     * @return the copy
     */
    @Benchmark
    public UniformRandomProvider longJump(LongJumpableSource data) {
        return data.jump();
    }

    /**
     * Arbitrary jump benchmark.
     *
     * @param data Source of the jump
     * @return the copy
     */
    @Benchmark
    public UniformRandomProvider arbitraryJump(ArbitrarilyJumpableSource data) {
        return data.jump();
    }
}
