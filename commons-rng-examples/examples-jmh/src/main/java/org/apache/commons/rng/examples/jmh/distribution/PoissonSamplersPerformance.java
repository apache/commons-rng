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
import org.apache.commons.rng.examples.jmh.RandomSources;
import org.apache.commons.rng.sampling.distribution.DiscreteSampler;
import org.apache.commons.rng.sampling.distribution.KempSmallMeanPoissonSampler;
import org.apache.commons.rng.sampling.distribution.LargeMeanPoissonSampler;
import org.apache.commons.rng.sampling.distribution.SmallMeanPoissonSampler;
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
 * Executes benchmark to compare the speed of generation of random numbers
 * from the various source providers for different types of {@link DiscreteSampler}.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@State(Scope.Benchmark)
@Fork(value = 1, jvmArgs = {"-server", "-Xms128M", "-Xmx128M"})
public class PoissonSamplersPerformance {
    /**
     * The {@link DiscreteSampler} samplers to use for testing. Creates the sampler for each
     * {@link RandomSource} in the default {@link RandomSources}.
     */
    @State(Scope.Benchmark)
    public static class Sources extends RandomSources {
        /**
         * The sampler type.
         */
        @Param({"SmallMeanPoissonSampler",
                "OriginalKempSmallMeanPoissonSampler",
                "KempSmallMeanPoissonSampler",
                "LargeMeanPoissonSampler",
                })
        private String samplerType;

        /**
         * The Poisson mean. This is set at a level where the small mean sampler is to be used
         * in preference to the large mean sampler.
         */
        @Param({"0.25",
                "0.5",
                "1",
                "2",
                "4",
                "8",
                "16",
                "32",
                })
        private double mean;

        /** The sampler. */
        private DiscreteSampler sampler;

        /**
         * @return the sampler.
         */
        public DiscreteSampler getSampler() {
            return sampler;
        }

        /** Instantiates sampler. */
        @Override
        @Setup
        public void setup() {
            super.setup();
            final UniformRandomProvider rng = getGenerator();
            if ("SmallMeanPoissonSampler".equals(samplerType)) {
                sampler = new SmallMeanPoissonSampler(rng, mean);
            } else if ("OriginalKempSmallMeanPoissonSampler".equals(samplerType)) {
                sampler = new OriginalKempSmallMeanPoissonSampler(rng, mean);
            } else if ("KempSmallMeanPoissonSampler".equals(samplerType)) {
                sampler = new KempSmallMeanPoissonSampler(rng, mean);
            } else if ("LargeMeanPoissonSampler".equals(samplerType)) {
                // Note this is not valid when mean < 1
                sampler = new LargeMeanPoissonSampler(rng, mean);
            }
        }
    }

    /**
     * Kemp sampler for the <a href="http://mathworld.wolfram.com/PoissonDistribution.html">Poisson
     * distribution</a>.
     *
     * <ul>
     *  <li>
     *   For small means, a Poisson process is simulated using uniform deviates, as
     *   described in Kemp, A, W, (1981) Efficient Generation of Logarithmically Distributed
     *   Pseudo-Random Variables. Journal of the Royal Statistical Society. Vol. 30, No. 3, pp. 249-253.
     *  </li>
     * </ul>
     *
     * <p>Note: This is the original algorithm by Kemp. It is included here for performance
     * testing. The version in the RNG sampling module implements a hedge on the cumulative
     * probability set at 50%. This saves computation in half of the samples.</p>
     *
     * @see <a href="https://www.jstor.org/stable/2346348">Kemp, A.W. (1981) JRSS Vol. 30, pp. 249-253</a>
     */
    static class OriginalKempSmallMeanPoissonSampler
        implements DiscreteSampler {
        /** Underlying source of randomness. */
        private final UniformRandomProvider rng;
        /**
         * Pre-compute {@code Math.exp(-mean)}.
         * Note: This is the probability of the Poisson sample {@code p(x=0)}.
         */
        private final double p0;
        /**
         * The mean of the Poisson sample.
         */
        private final double mean;

        /**
         * @param rng  Generator of uniformly distributed random numbers.
         * @param mean Mean.
         * @throws IllegalArgumentException if {@code mean <= 0} or {@code mean > 700}.
         */
        OriginalKempSmallMeanPoissonSampler(UniformRandomProvider rng,
                                            double mean) {
            if (mean <= 0) {
                throw new IllegalArgumentException("mean is not strictly positive: " + mean);
            }

            p0 = Math.exp(-mean);
            if (p0 == 0) {
                throw new IllegalArgumentException("No probability for mean " + mean);
            }
            this.rng = rng;
            this.mean = mean;
        }

        /** {@inheritDoc} */
        @Override
        public int sample() {
            // Note on the algorithm:
            // - X is the unknown sample deviate (the output of the algorithm)
            // - x is the current value from the distribution
            // - p is the probability of the current value x, p(X=x)
            // - u is effectively the cumulative probability that the sample X
            //   is equal or above the current value x, p(X>=x)
            // So if p(X>=x) > p(X=x) the sample must be above x, otherwise it is x
            double u = rng.nextDouble();
            int x = 0;
            double p = p0;
            while (u > p) {
                u -= p;
                // Compute the next probability using a recurrence relation.
                // p(x+1) = p(x) * mean / (x+1)
                p *= mean / ++x;
                // The algorithm listed in Kemp (1981) does not check that the rolling probability
                // is positive. This check is added to ensure no errors when the limit of the summation
                // 1 - sum(p(x)) is above 0 due to cumulative error in floating point arithmetic.
                if (p == 0) {
                    return x;
                }
            }
            return x;
        }
    }

    // Benchmarks methods below.

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
