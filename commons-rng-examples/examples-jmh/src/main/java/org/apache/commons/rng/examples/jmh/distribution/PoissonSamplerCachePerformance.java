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

import java.util.concurrent.TimeUnit;

import org.apache.commons.rng.RandomProviderState;
import org.apache.commons.rng.RestorableUniformRandomProvider;
import org.apache.commons.rng.UniformRandomProvider;
import org.apache.commons.rng.sampling.PermutationSampler;
import org.apache.commons.rng.sampling.distribution.DiscreteSampler;
import org.apache.commons.rng.sampling.distribution.PoissonSampler;
import org.apache.commons.rng.sampling.distribution.PoissonSamplerCache;
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
import org.openjdk.jmh.infra.Blackhole;

/**
 * Executes benchmark to compare the speed of generation of Poisson random numbers when using a
 * cache.
 *
 * <p>The benchmark is designed for a worse case scenario of Poisson means that are uniformly spread
 * over a range and non-integer. A single sample is required per mean, E.g.
 *
 * <pre>
 * int min = 40;
 * int max = 1000;
 * int range = max - min;
 * UniformRandomProvider rng = ...;
 *
 * // Compare ...
 * for (int i = 0; i < 1000; i++) {
 *   new PoissonSampler(rng, min + rng.nextDouble() * range).sample();
 * }
 *
 * // To ...
 * PoissonSamplerCache cache = new PoissonSamplerCache(min, max);
 * for (int i = 0; i < 1000; i++) {
 *   PoissonSamplerCache.createPoissonSampler(rng, min + rng.nextDouble() * range).sample();
 * }
 * </pre>
 *
 * <p>The alternative scenario where the means are integer is not considered as this could be easily
 * handled by creating an array to hold the PoissonSamplers for each mean. This does not require any
 * specialised caching of state and is simple enough to perform for single threaded applications:
 *
 * <pre>
 * public class SimpleUnsafePoissonSamplerCache {
 *   int min = 50;
 *   int max = 100;
 *   PoissonSampler[] samplers = new PoissonSampler[max - min + 1];
 *
 *   public PoissonSampler createPoissonSampler(UniformRandomProvider rng, int mean) {
 *     if (mean < min || mean > max) {
 *       return new PoissonSampler(rng, mean);
 *     }
 *     int index = mean - min;
 *     PoissonSampler sample = samplers[index];
 *     if (sampler == null) {
 *       sampler = new PoissonSampler(rng, mean);
 *       samplers[index] = sampler;
 *     }
 *     return sampler;
 *   }
 * }
 * </pre>
 *
 * Note that in this example the UniformRandomProvider is also cached and so this is only
 * applicable to a single threaded application.
 *
 * <p>Re-written to use the PoissonSamplerCache would provide a new PoissonSampler per call in a
 * thread-safe manner:
 *
 * <pre>
 * public class SimplePoissonSamplerCache {
 *   int min = 50;
 *   int max = 100;
 *   PoissonSamplerCache samplers = new PoissonSamplerCache(min, max);
 *
 *   public PoissonSampler createPoissonSampler(UniformRandomProvider rng, int mean) {
 *       return samplers.createPoissonSampler(rng, mean);
 *   }
 * }
 * </pre>
*/
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@State(Scope.Benchmark)
@Fork(value = 1, jvmArgs = { "-server", "-Xms128M", "-Xmx128M" })
public class PoissonSamplerCachePerformance {
    /** Number of samples per run. */
    private static final int NUM_SAMPLES = 100000;
    /**
     * Number of range samples.
     *
     * <p>Note: The LargeMeanPoissonSampler will not use a SmallMeanPoissonSampler
     * if the mean is an integer. This will occur if the [range sample] * range is
     * an integer.
     *
     * <p>If the SmallMeanPoissonSampler is not used then the cache has more
     * advantage over the uncached version as relatively more time is spent in
     * initialising the algorithm.
     *
     * <p>To avoid this use a prime number above the maximum range
     * (currently 4096). Any number (n/RANGE_SAMPLES) * range will not be integer
     * with n<RANGE_SAMPLES and range<RANGE_SAMPLES (unless n==0).
     */
    private static final int RANGE_SAMPLE_SIZE = 4099;
    /** The size of the seed. */
    private static final int SEED_SIZE = 128;

    /**
     * Seed used to ensure the tests are the same. This can be different per
     * benchmark, but should be the same within the benchmark.
     */
    private static final int[] SEED;

    /**
     * The range sample. Should contain doubles in the range 0 inclusive to 1 exclusive.
     *
     * <p>The range sample is used to create a mean using:
     * rangeMin + sample * (rangeMax - rangeMin).
     *
     * <p>Ideally this should be large enough to fully sample the
     * range when expressed as discrete integers, i.e. no sparseness, and random.
     */
    private static final double[] RANGE_SAMPLE;

    static {
        // Build a random seed for all the tests
        SEED = new int[SEED_SIZE];
        final UniformRandomProvider rng = RandomSource.create(RandomSource.MWC_256);
        for (int i = 0; i < SEED.length; i++) {
            SEED[i] = rng.nextInt();
        }

        final int size = RANGE_SAMPLE_SIZE;
        final int[] sample = PermutationSampler.natural(size);
        PermutationSampler.shuffle(rng, sample);

        RANGE_SAMPLE = new double[size];
        for (int i = 0; i < size; i++) {
            // Note: This will have one occurrence of zero in the range.
            // This will create at least one LargeMeanPoissonSampler that will
            // not use a SmallMeanPoissonSampler. The different performance of this
            // will be lost among the other samples.
            RANGE_SAMPLE[i] = (double) sample[i] / size;
        }
    }

    /**
     * The benchmark state (retrieve the various "RandomSource"s).
     */
    @State(Scope.Benchmark)
    public static class Sources {
        /**
         * RNG providers.
         *
         * <p>Use different speeds.
         *
         * @see <a href="https://commons.apache.org/proper/commons-rng/userguide/rng.html">
         *      Commons RNG user guide</a>
         */
        @Param({ "SPLIT_MIX_64",
            // Comment in for slower generators
            //"MWC_256", "KISS", "WELL_1024_A", "WELL_44497_B"
            })
        private String randomSourceName;

        /** RNG. */
        private RestorableUniformRandomProvider generator;

        /**
         * The state of the generator at the start of the test (for reproducible
         * results).
         */
        private RandomProviderState state;

        /**
         * @return the RNG.
         */
        public UniformRandomProvider getGenerator() {
            generator.restoreState(state);
            return generator;
        }

        /** Instantiates generator. */
        @Setup
        public void setup() {
            final RandomSource randomSource = RandomSource
                    .valueOf(randomSourceName);
            // Use the same seed
            generator = RandomSource.create(randomSource, SEED.clone());
            state = generator.saveState();
        }
    }

    /**
     * The range of mean values for testing the cache.
     */
    @State(Scope.Benchmark)
    public static class MeanRange {
        /**
         * Test range.
         *
         * <p>The covers the best case scenario of caching everything (range=1) and upwards
         * in powers of 4.
         */
        @Param({ "1", "4", "16", "64", "256", "1024", "4096"})
        private double range;

        /**
         * Gets the mean.
         *
         * @param i the index
         * @return the mean
         */
        public double getMean(int i) {
            return getMin() + RANGE_SAMPLE[i % RANGE_SAMPLE.length] * range;
        }

        /**
         * Gets the min of the range.
         *
         * @return the min
         */
        public double getMin() {
            return PoissonSamplerCache.getMinimumCachedMean();
        }

        /**
         * Gets the max of the range.
         *
         * @return the max
         */
        public double getMax() {
            return getMin() + range;
        }
    }

    /**
     * A factory for creating Poisson sampler objects.
     */
    @FunctionalInterface
    private interface PoissonSamplerFactory {
        /**
         * Creates a new Poisson sampler object.
         *
         * @param mean the mean
         * @return The sampler
         */
        DiscreteSampler createPoissonSampler(double mean);
    }

    /**
     * Exercises a poisson sampler created for a single use with a range of means.
     *
     * @param factory The factory.
     * @param range   The range of means.
     * @param bh      Data sink.
     */
    private static void runSample(PoissonSamplerFactory factory,
                                  MeanRange range,
                                  Blackhole bh) {
        for (int i = 0; i < NUM_SAMPLES; i++) {
            bh.consume(factory.createPoissonSampler(range.getMean(i)).sample());
        }
    }

    // Benchmarks methods below.

    /**
     * @param sources Source of randomness.
     * @param range   The range.
     * @param bh      Data sink.
     */
    @Benchmark
    public void runPoissonSampler(Sources sources,
                                  MeanRange range,
                                  Blackhole bh) {
        final UniformRandomProvider r = sources.getGenerator();
        final PoissonSamplerFactory factory = new PoissonSamplerFactory() {
            @Override
            public DiscreteSampler createPoissonSampler(double mean) {
                return new PoissonSampler(r, mean);
            }
        };
        runSample(factory, range, bh);
    }

    /**
     * @param sources Source of randomness.
     * @param range   The range.
     * @param bh      Data sink.
     */
    @Benchmark
    public void runPoissonSamplerCacheWhenEmpty(Sources sources,
                                                MeanRange range,
                                                Blackhole bh) {
        final UniformRandomProvider r = sources.getGenerator();
        final PoissonSamplerCache cache = new PoissonSamplerCache(0, 0);
        final PoissonSamplerFactory factory = new PoissonSamplerFactory() {
            @Override
            public DiscreteSampler createPoissonSampler(double mean) {
                return cache.createPoissonSampler(r, mean);
            }
        };
        runSample(factory, range, bh);
    }

    /**
     * @param sources Source of randomness.
     * @param range   The range.
     * @param bh      Data sink.
     */
    @Benchmark
    public void runPoissonSamplerCache(Sources sources,
                                       MeanRange range,
                                       Blackhole bh) {
        final UniformRandomProvider r = sources.getGenerator();
        final PoissonSamplerCache cache = new PoissonSamplerCache(
                range.getMin(), range.getMax());
        final PoissonSamplerFactory factory = new PoissonSamplerFactory() {
            @Override
            public DiscreteSampler createPoissonSampler(double mean) {
                return cache.createPoissonSampler(r, mean);
            }
        };
        runSample(factory, range, bh);
    }
}
