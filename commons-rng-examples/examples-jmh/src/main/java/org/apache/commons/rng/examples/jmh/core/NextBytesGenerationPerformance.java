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
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;

/**
 * Executes benchmark to compare the speed of generation of random numbers from the
 * various source providers for {@link UniformRandomProvider#nextBytes(byte[])}.
 */
public class NextBytesGenerationPerformance extends AbstractBenchmark {
    /**
     * The value. This is a pre-allocated array. Must NOT be final to prevent JVM
     * optimisation!
     */
    private byte[] value = new byte[BaselineGenerationPerformance.NEXT_BYTES_SIZE];

    /**
     * The benchmark state (retrieve the various "RandomSource"s).
     */
    @State(Scope.Benchmark)
    public static class Sources extends BaselineSources {
        /** {@inheritDoc} */
        @Override
        protected UniformRandomProvider createBaseline() {
            return BaselineUtils.getNextBytes();
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
     * Baseline for a JMH method call returning a {@code byte[]}.
     *
     * @return the value
     */
    @Benchmark
    public byte[] baselineBytes() {
        return value;
    }

    /**
     * Exercise the {@link UniformRandomProvider#nextBytes(byte[])} method.
     *
     * @param sources Source of randomness.
     * @return the boolean
     */
    @Benchmark
    public byte[] nextBytes(Sources sources) {
        // The array allocation is not part of the benchmark.
        sources.getGenerator().nextBytes(value);
        return value;
    }
}
