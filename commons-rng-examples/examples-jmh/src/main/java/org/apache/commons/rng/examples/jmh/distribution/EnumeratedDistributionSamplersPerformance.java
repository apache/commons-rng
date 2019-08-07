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

import org.apache.commons.math3.distribution.BinomialDistribution;
import org.apache.commons.math3.distribution.IntegerDistribution;
import org.apache.commons.math3.distribution.PoissonDistribution;
import org.apache.commons.rng.UniformRandomProvider;
import org.apache.commons.rng.sampling.distribution.AliasMethodDiscreteSampler;
import org.apache.commons.rng.sampling.distribution.DiscreteSampler;
import org.apache.commons.rng.sampling.distribution.GuideTableDiscreteSampler;
import org.apache.commons.rng.sampling.distribution.MarsagliaTsangWangDiscreteSampler;
import org.apache.commons.rng.simple.RandomSource;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/**
 * Executes benchmark to compare the speed of generation of random numbers from an enumerated
 * discrete probability distribution.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@State(Scope.Benchmark)
@Fork(value = 1, jvmArgs = {"-server", "-Xms128M", "-Xmx128M"})
public class EnumeratedDistributionSamplersPerformance {
    /**
     * The random sources to use for testing. This is a smaller list than all the possible
     * random sources; the list is composed of generators of different speeds.
     */
    @State(Scope.Benchmark)
    public static class LocalRandomSources {
        /**
         * RNG providers.
         *
         * <p>Use different speeds.</p>
         *
         * @see <a href="https://commons.apache.org/proper/commons-rng/userguide/rng.html">
         *      Commons RNG user guide</a>
         */
        @Param({"WELL_44497_B",
                "ISAAC",
                "XO_RO_SHI_RO_128_PLUS",
                })
        private String randomSourceName;

        /** RNG. */
        private UniformRandomProvider generator;

        /**
         * @return the RNG.
         */
        public UniformRandomProvider getGenerator() {
            return generator;
        }

        /** Create the random source. */
        @Setup
        public void setup() {
            final RandomSource randomSource = RandomSource.valueOf(randomSourceName);
            generator = RandomSource.create(randomSource);
        }
    }

    /**
     * The {@link DiscreteSampler} samplers to use for testing. Creates the sampler for each
     * random source.
     *
     * <p>This class is abstract. The probability distribution is created by implementations.</p>
     */
    @State(Scope.Benchmark)
    public abstract static class SamplerSources extends LocalRandomSources {
        /**
         * A factory for creating DiscreteSampler objects.
         */
        interface DiscreteSamplerFactory {
            /**
             * Creates the sampler.
             *
             * @return the sampler
             */
            DiscreteSampler create();
        }

        /**
         * The sampler type.
         */
        @Param({"BinarySearchDiscreteSampler",
                "AliasMethodDiscreteSampler",
                "GuideTableDiscreteSampler",
                "MarsagliaTsangWangDiscreteSampler",

                // Uncomment to test non-default parameters
                //"AliasMethodDiscreteSamplerNoPad", // Not optimal for sampling
                //"AliasMethodDiscreteSamplerAlpha1",
                //"AliasMethodDiscreteSamplerAlpha2",

                // The AliasMethod memory requirement doubles for each alpha increment.
                // A fair comparison is to use 2^alpha for the equivalent guide table method.
                //"GuideTableDiscreteSamplerAlpha2",
                //"GuideTableDiscreteSamplerAlpha4",
                })
        private String samplerType;

        /** The factory. */
        private DiscreteSamplerFactory factory;

        /** The sampler. */
        private DiscreteSampler sampler;

        /**
         * Gets the sampler.
         *
         * @return the sampler.
         */
        public DiscreteSampler getSampler() {
            return sampler;
        }

        /** Create the distribution (per iteration as it may vary) and instantiates sampler. */
        @Override
        @Setup(Level.Iteration)
        public void setup() {
            super.setup();

            final double[] probabilities = createProbabilities();
            createSamplerFactory(getGenerator(), probabilities);
            sampler = factory.create();
        }

        /**
         * Creates the probabilities for the distribution.
         *
         * @return The probabilities.
         */
        protected abstract double[] createProbabilities();

        /**
         * Creates the sampler factory.
         *
         * @param rng The random generator.
         * @param probabilities The probabilities.
         */
        private void createSamplerFactory(final UniformRandomProvider rng,
            final double[] probabilities) {
            // This would benefit from Java 8 lambda functions
            if ("BinarySearchDiscreteSampler".equals(samplerType)) {
                factory = new DiscreteSamplerFactory() {
                    @Override
                    public DiscreteSampler create() {
                        return new BinarySearchDiscreteSampler(rng, probabilities);
                    }
                };
            } else if ("AliasMethodDiscreteSampler".equals(samplerType)) {
                factory = new DiscreteSamplerFactory() {
                    @Override
                    public DiscreteSampler create() {
                        return AliasMethodDiscreteSampler.of(rng, probabilities);
                    }
                };
            } else if ("AliasMethodDiscreteSamplerNoPad".equals(samplerType)) {
                factory = new DiscreteSamplerFactory() {
                    @Override
                    public DiscreteSampler create() {
                        return AliasMethodDiscreteSampler.of(rng, probabilities, -1);
                    }
                };
            } else if ("AliasMethodDiscreteSamplerAlpha1".equals(samplerType)) {
                factory = new DiscreteSamplerFactory() {
                    @Override
                    public DiscreteSampler create() {
                        return AliasMethodDiscreteSampler.of(rng, probabilities, 1);
                    }
                };
            } else if ("AliasMethodDiscreteSamplerAlpha2".equals(samplerType)) {
                factory = new DiscreteSamplerFactory() {
                    @Override
                    public DiscreteSampler create() {
                        return AliasMethodDiscreteSampler.of(rng, probabilities, 2);
                    }
                };
            } else if ("GuideTableDiscreteSampler".equals(samplerType)) {
                factory = new DiscreteSamplerFactory() {
                    @Override
                    public DiscreteSampler create() {
                        return GuideTableDiscreteSampler.of(rng, probabilities);
                    }
                };
            } else if ("GuideTableDiscreteSamplerAlpha2".equals(samplerType)) {
                factory = new DiscreteSamplerFactory() {
                    @Override
                    public DiscreteSampler create() {
                        return GuideTableDiscreteSampler.of(rng, probabilities, 2);
                    }
                };
            } else if ("GuideTableDiscreteSamplerAlpha8".equals(samplerType)) {
                factory = new DiscreteSamplerFactory() {
                    @Override
                    public DiscreteSampler create() {
                        return GuideTableDiscreteSampler.of(rng, probabilities, 8);
                    }
                };
            } else if ("MarsagliaTsangWangDiscreteSampler".equals(samplerType)) {
                factory = new DiscreteSamplerFactory() {
                    @Override
                    public DiscreteSampler create() {
                        return MarsagliaTsangWangDiscreteSampler.Enumerated.of(rng, probabilities);
                    }
                };
            } else {
                throw new IllegalStateException();
            }
        }

        /**
         * Creates a new instance of the sampler.
         *
         * @return The sampler.
         */
        public DiscreteSampler createSampler() {
            return factory.create();
        }
    }

    /**
     * Define known probability distributions for testing. These are expected to have well
     * behaved cumulative probability functions.
     */
    @State(Scope.Benchmark)
    public static class KnownDistributionSources extends SamplerSources {
        /** The cumulative probability limit for unbounded distributions. */
        private static final double CUMULATIVE_PROBABILITY_LIMIT = 1 - 1e-9;

        /**
         * The distribution.
         */
        @Param({"Binomial_N67_P0.7",
                "Geometric_P0.2",
                "4SidedLoadedDie",
                "Poisson_Mean3.14",
                "Poisson_Mean10_Mean20",
                })
        private String distribution;

        /** {@inheritDoc} */
        @Override
        protected double[] createProbabilities() {
            if ("Binomial_N67_P0.7".equals(distribution)) {
                final int trials = 67;
                final double probabilityOfSuccess = 0.7;
                final BinomialDistribution dist = new BinomialDistribution(null, trials, probabilityOfSuccess);
                return createProbabilities(dist, 0, trials);
            } else if ("Geometric_P0.2".equals(distribution)) {
                final double probabilityOfSuccess = 0.2;
                final double probabilityOfFailure = 1 - probabilityOfSuccess;
                // https://en.wikipedia.org/wiki/Geometric_distribution
                // PMF = (1-p)^k * p
                // k is number of failures before a success
                double p = 1.0; // (1-p)^0
                // Build until the cumulative function is big
                double[] probabilities = new double[100];
                double sum = 0;
                int k = 0;
                while (k < probabilities.length) {
                    probabilities[k] = p * probabilityOfSuccess;
                    sum += probabilities[k++];
                    if (sum > CUMULATIVE_PROBABILITY_LIMIT) {
                        break;
                    }
                    // For the next PMF
                    p *= probabilityOfFailure;
                }
                return Arrays.copyOf(probabilities, k);
            } else if ("4SidedLoadedDie".equals(distribution)) {
                return new double[] {1.0 / 2, 1.0 / 3, 1.0 / 12, 1.0 / 12};
            } else if ("Poisson_Mean3.14".equals(distribution)) {
                final double mean = 3.14;
                final IntegerDistribution dist = createPoissonDistribution(mean);
                final int max = dist.inverseCumulativeProbability(CUMULATIVE_PROBABILITY_LIMIT);
                return createProbabilities(dist, 0, max);
            } else if ("Poisson_Mean10_Mean20".equals(distribution)) {
                // Create a Bimodel using two Poisson distributions
                final double mean1 = 10;
                final double mean2 = 20;
                final IntegerDistribution dist1 = createPoissonDistribution(mean2);
                final int max = dist1.inverseCumulativeProbability(CUMULATIVE_PROBABILITY_LIMIT);
                double[] p1 = createProbabilities(dist1, 0, max);
                double[] p2 = createProbabilities(createPoissonDistribution(mean1), 0, max);
                for (int i = 0; i < p1.length; i++) {
                    p1[i] += p2[i];
                }
                // Leave to the distribution to normalise the sum
                return p1;
            }
            throw new IllegalStateException();
        }

        /**
         * Creates the poisson distribution.
         *
         * @param mean the mean
         * @return the distribution
         */
        private static IntegerDistribution createPoissonDistribution(double mean) {
            return new PoissonDistribution(null, mean,
                PoissonDistribution.DEFAULT_EPSILON, PoissonDistribution.DEFAULT_MAX_ITERATIONS);
        }

        /**
         * Creates the probabilities from the distribution.
         *
         * @param dist the distribution
         * @param lower the lower bounds (inclusive)
         * @param upper the upper bounds (inclusive)
         * @return the probabilities
         */
        private static double[] createProbabilities(IntegerDistribution dist, int lower, int upper) {
            double[] probabilities = new double[upper - lower + 1];
            for (int i = 0, x = lower; x <= upper; i++, x++) {
                probabilities[i] = dist.probability(x);
            }
            return probabilities;
        }
    }

    /**
     * Define random probability distributions of known size for testing. These are random but
     * the average cumulative probability function will be a straight line given the increment
     * average is 0.5.
     */
    @State(Scope.Benchmark)
    public static class RandomDistributionSources extends SamplerSources {
        /**
         * The distribution size.
         * These are spaced half-way between powers-of-2 to minimise the advantage of
         * padding by the Alias method sampler.
         */
        @Param({"6",
                //"12",
                //"24",
                //"48",
                "96",
                //"192",
                //"384",
                // Above 2048 forces the Alias method to use more than 64-bits for sampling
                "3072"
                })
        private int randomNonUniformSize;

        /** {@inheritDoc} */
        @Override
        protected double[] createProbabilities() {
            final double[] probabilities = new double[randomNonUniformSize];
            final ThreadLocalRandom rng = ThreadLocalRandom.current();
            for (int i = 0; i < probabilities.length; i++) {
                probabilities[i] = rng.nextDouble();
            }
            return probabilities;
        }
    }

    /**
     * Compute a sample by binary search of the cumulative probability distribution.
     */
    static final class BinarySearchDiscreteSampler
        implements DiscreteSampler {
        /** Underlying source of randomness. */
        private final UniformRandomProvider rng;
        /**
         * The cumulative probability table.
         */
        private final double[] cumulativeProbabilities;

        /**
         * @param rng Generator of uniformly distributed random numbers.
         * @param probabilities The probabilities.
         * @throws IllegalArgumentException if {@code probabilities} is null or empty, a
         * probability is negative, infinite or {@code NaN}, or the sum of all
         * probabilities is not strictly positive.
         */
        BinarySearchDiscreteSampler(UniformRandomProvider rng,
                                    double[] probabilities) {
            // Minimal set-up validation
            if (probabilities == null || probabilities.length == 0) {
                throw new IllegalArgumentException("Probabilities must not be empty.");
            }

            final int size = probabilities.length;
            cumulativeProbabilities = new double[size];

            double sumProb = 0;
            int count = 0;
            for (final double prob : probabilities) {
                if (prob < 0 ||
                    Double.isInfinite(prob) ||
                    Double.isNaN(prob)) {
                    throw new IllegalArgumentException("Invalid probability: " +
                                                       prob);
                }

                // Compute and store cumulative probability.
                sumProb += prob;
                cumulativeProbabilities[count++] = sumProb;
            }

            if (Double.isInfinite(sumProb) || sumProb <= 0) {
                throw new IllegalArgumentException("Invalid sum of probabilities: " + sumProb);
            }

            this.rng = rng;

            // Normalise cumulative probability.
            for (int i = 0; i < size; i++) {
                final double norm = cumulativeProbabilities[i] / sumProb;
                cumulativeProbabilities[i] = (norm < 1) ? norm : 1.0;
            }
        }

        /** {@inheritDoc} */
        @Override
        public int sample() {
            final double u = rng.nextDouble();

            // Java binary search
            //int index = Arrays.binarySearch(cumulativeProbabilities, u);
            //if (index < 0) {
            //    index = -index - 1;
            //}
            //
            //return index < cumulativeProbabilities.length ?
            //    index :
            //    cumulativeProbabilities.length - 1;

            // Binary search within known cumulative probability table.
            // Find x so that u > f[x-1] and u <= f[x].
            // This is a looser search than Arrays.binarySearch:
            // - The output is x = upper.
            // - The table stores probabilities where f[0] is >= 0 and the max == 1.0.
            // - u should be >= 0 and <= 1 (or the random generator is broken).
            // - It avoids comparisons using Double.doubleToLongBits.
            // - It avoids the low likelihood of equality between two doubles for fast exit
            //   so uses only 1 compare per loop.
            int lower = 0;
            int upper = cumulativeProbabilities.length - 1;
            while (lower < upper) {
                final int mid = (lower + upper) >>> 1;
                final double midVal = cumulativeProbabilities[mid];
                if (u > midVal) {
                    // Change lower such that
                    // u > f[lower - 1]
                    lower = mid + 1;
                } else {
                    // Change upper such that
                    // u <= f[upper]
                    upper = mid;
                }
            }
            return upper;
        }
    }

    /**
     * The value for the baseline generation of an {@code int} value.
     *
     * <p>This must NOT be final!</p>
     */
    private int value;

    // Benchmarks methods below.

    /**
     * Baseline for the JMH timing overhead for production of an {@code int} value.
     *
     * @return the {@code int} value
     */
    @Benchmark
    public int baselineInt() {
        return value;
    }

    /**
     * Baseline for the production of a {@code double} value.
     * This is used to assess the performance of the underlying random source.
     *
     * @param sources Source of randomness.
     * @return the {@code int} value
     */
    @Benchmark
    public int baselineNextDouble(LocalRandomSources sources) {
        return sources.getGenerator().nextDouble() < 0.5 ? 1 : 0;
    }

    /**
     * Run the sampler.
     *
     * @param sources Source of randomness.
     * @return the sample value
     */
    @Benchmark
    public int sampleKnown(KnownDistributionSources sources) {
        return sources.getSampler().sample();
    }

    /**
     * Run the sampler.
     *
     * @param sources Source of randomness.
     * @return the sample value
     */
    @Benchmark
    public int singleSampleKnown(KnownDistributionSources sources) {
        return sources.createSampler().sample();
    }

    /**
     * Run the sampler.
     *
     * @param sources Source of randomness.
     * @return the sample value
     */
    @Benchmark
    public int sampleRandom(RandomDistributionSources sources) {
        return sources.getSampler().sample();
    }

    /**
     * Run the sampler.
     *
     * @param sources Source of randomness.
     * @return the sample value
     */
    @Benchmark
    public int singleSampleRandom(RandomDistributionSources sources) {
        return sources.createSampler().sample();
    }
}
