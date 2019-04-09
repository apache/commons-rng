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
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;

/**
 * Executes benchmark to compare the speed of generation of random numbers from the
 * various source providers for {@link UniformRandomProvider#nextBoolean()}.
 */
public class NextBooleanGenerationPerformance extends AbstractBenchmark {
    /**
     * The benchmark state (retrieve the various "RandomSource"s).
     */
    @State(Scope.Benchmark)
    public static class Sources extends BaselineSources {
        @Override
        protected UniformRandomProvider createBaseline() {
            return BaselineUtils.getNextBoolean();
        }
    }

    /** The value. Must NOT be final to prevent JVM optimisation! */
    private boolean value;

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
        return value;
    }

    /**
     * Exercise the {@link UniformRandomProvider#nextBoolean()} method.
     *
     * @param sources Source of randomness.
     * @return the boolean
     */
    @Benchmark
    public boolean nextBoolean(Sources sources) {
        return sources.getGenerator().nextBoolean();
    }
}
