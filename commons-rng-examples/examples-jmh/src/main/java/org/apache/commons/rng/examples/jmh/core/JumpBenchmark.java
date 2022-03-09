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
import org.apache.commons.rng.JumpableUniformRandomProvider;
import org.apache.commons.rng.LongJumpableUniformRandomProvider;
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
         * <p>Note: Some providers have exactly the same jump method so are commented out.
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
                //"L64_X128_SS",
                "L64_X128_MIX",
                "L64_X256_MIX",
                "L64_X1024_MIX",
                "L128_X128_MIX",
                "L128_X256_MIX",
                "L128_X1024_MIX"})
        private String randomSourceName;

        /** {@inheritDoc} */
        @Override
        protected Supplier<UniformRandomProvider> createJumpFunction() {
            final UniformRandomProvider rng = RandomSource.valueOf(randomSourceName).create();
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
     * is redundant unless the long jump function requires a more expensive routine.
     * Providers listed here are expected to have a slower long jump.
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
            })
        private String randomSourceName;


        /** {@inheritDoc} */
        @Override
        protected Supplier<UniformRandomProvider> createJumpFunction() {
            final UniformRandomProvider rng = RandomSource.valueOf(randomSourceName).create();
            if (rng instanceof LongJumpableUniformRandomProvider) {
                return ((LongJumpableUniformRandomProvider) rng)::longJump;
            }
            throw new IllegalStateException("Invalid long jump source: " + randomSourceName);
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
     * @param data Source of the long jump
     * @return the copy
     */
    @Benchmark
    public UniformRandomProvider longJump(LongJumpableSource data) {
        return data.jump();
    }
}
