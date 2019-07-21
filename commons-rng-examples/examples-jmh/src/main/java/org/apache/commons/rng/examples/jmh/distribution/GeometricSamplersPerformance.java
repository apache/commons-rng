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

package org.apache.commons.rng.examples.jmh.distribution;

import org.apache.commons.rng.UniformRandomProvider;
import org.apache.commons.rng.sampling.distribution.DiscreteInverseCumulativeProbabilityFunction;
import org.apache.commons.rng.sampling.distribution.DiscreteSampler;
import org.apache.commons.rng.sampling.distribution.GeometricSampler;
import org.apache.commons.rng.sampling.distribution.InverseTransformDiscreteSampler;
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

import java.util.concurrent.TimeUnit;

/**
 * Executes a benchmark to compare the speed of generation of Geometric random numbers
 * using different methods.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@State(Scope.Benchmark)
@Fork(value = 1, jvmArgs = {"-server", "-Xms128M", "-Xmx128M"})
public class GeometricSamplersPerformance {
    /**
     * The samplers's to use for testing. Defines the RandomSource, probability of success
     * and the type of Geometric sampler.
     */
    @State(Scope.Benchmark)
    public static class Sources {
        /**
         * RNG providers.
         *
         * <p>Use different speeds.</p>
         *
         * @see <a href="https://commons.apache.org/proper/commons-rng/userguide/rng.html">
         *      Commons RNG user guide</a>
         */
        @Param({"SPLIT_MIX_64",
                "MWC_256",
                "JDK"})
        private String randomSourceName;

        /**
         * The probability of success.
         */
        @Param({"0.1", "0.3"})
        private double probabilityOfSuccess;

        /**
         * The sampler type.
         */
        @Param({"GeometricSampler", "InverseTransformDiscreteSampler"})
        private String samplerType;

        /** The sampler. */
        private DiscreteSampler sampler;

        /**
         * @return the sampler.
         */
        public DiscreteSampler getSampler() {
            return sampler;
        }

        /** Instantiates sampler. */
        @Setup
        public void setup() {
            final RandomSource randomSource = RandomSource.valueOf(randomSourceName);
            final UniformRandomProvider rng = RandomSource.create(randomSource);
            if ("GeometricSampler".equals(samplerType)) {
                sampler = GeometricSampler.of(rng, probabilityOfSuccess);
            } else {
                final DiscreteInverseCumulativeProbabilityFunction geometricFunction =
                    new GeometricDiscreteInverseCumulativeProbabilityFunction(probabilityOfSuccess);
                sampler = InverseTransformDiscreteSampler.of(rng, geometricFunction);
            }
        }
    }

    /**
     * Define the inverse cumulative probability function for the Geometric distribution.
     *
     * <p>Adapted from org.apache.commons.math3.distribution.GeometricDistribution.
     */
    private static class GeometricDiscreteInverseCumulativeProbabilityFunction
            implements DiscreteInverseCumulativeProbabilityFunction {
        /**
         * {@code log(1 - p)} where p is the probability of success.
         */
        private final double log1mProbabilityOfSuccess;

        /**
         * @param probabilityOfSuccess the probability of success
         */
        GeometricDiscreteInverseCumulativeProbabilityFunction(double probabilityOfSuccess) {
            // No validation that 0 < p <= 1
            log1mProbabilityOfSuccess = Math.log1p(-probabilityOfSuccess);
        }

        @Override
        public int inverseCumulativeProbability(double cumulativeProbability) {
            // This is the equivalent of floor(log(u)/ln(1-p))
            // where:
            // u = cumulative probability
            // p = probability of success
            // See: https://en.wikipedia.org/wiki/Geometric_distribution#Related_distributions
            // ---
            // Note: if cumulativeProbability == 0 then log1p(-0) is zero and the result
            // after the range check is 0.
            // Note: if cumulativeProbability == 1 then log1p(-1) is negative infinity, the result
            // of the divide is positive infinity and the result after the range check is
            // Integer.MAX_VALUE.
            return Math.max(0, (int) Math.ceil(Math.log1p(-cumulativeProbability) / log1mProbabilityOfSuccess - 1));
        }
    }

    /**
     * The value.
     *
     * <p>This must NOT be final!</p>
     */
    private int value;

    /**
     * Baseline for the JMH timing overhead for production of an {@code int} value.
     *
     * @return the {@code int} value
     */
    @Benchmark
    public int baseline() {
        return value;
    }

    /**
     * Run the sampler.
     *
     * @param sources Source of randomness.
     * @return the sample value
     */
    @Benchmark
    public int sample(Sources sources) {
        return sources.getSampler().sample();
    }
}
