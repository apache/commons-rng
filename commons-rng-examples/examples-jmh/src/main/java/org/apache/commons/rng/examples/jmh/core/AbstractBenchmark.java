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

import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

import java.util.concurrent.TimeUnit;

/**
 * Declares the JMH annotations for the benchmarks to compare the speed of generation of
 * random numbers from the various source providers for
 * {@link org.apache.commons.rng.UniformRandomProvider UniformRandomProvider}.
 *
 * <p>Note: Implementing this as an {@code @interface} annotation results in errors as the
 * meta-annotation is not expanded by the JMH annotation processor. The processor does however
 * allow all annotations to be inherited from abstract classes.</p>
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 10, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 10, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(value = 1, jvmArgs = { "-server", "-Xms128M", "-Xmx128M" })
@State(Scope.Benchmark)
public abstract class AbstractBenchmark {
    /**
     * Create a new instance.
     */
    protected AbstractBenchmark() {
        // Hide public constructor to prevent instantiation
    }

    // Empty. Serves as an annotation placeholder.
}
