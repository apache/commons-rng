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
 * Executes benchmark to compare the speed of generation of Poisson distributed random numbers.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@State(Scope.Benchmark)
@Fork(value = 1, jvmArgs = {"-server", "-Xms128M", "-Xmx128M"})
public class PoissonSamplersPerformance {
    /**
     * The mean for the call to {@link Math#exp(double)}.
     */
    @State(Scope.Benchmark)
    public static class Means {
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

        /**
         * Gets the mean.
         *
         * @return the mean
         */
        public double getMean() {
            return mean;
        }
    }

    /**
     * The {@link DiscreteSampler} samplers to use for testing. Creates the sampler for each
     * {@link RandomSource} in the default
     * {@link org.apache.commons.rng.examples.jmh.RandomSources RandomSources}.
     */
    @State(Scope.Benchmark)
    public static class Sources {
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
         * RNG providers.
         *
         * <p>Use different speeds.</p>
         *
         * @see <a href="https://commons.apache.org/proper/commons-rng/userguide/rng.html">
         *      Commons RNG user guide</a>
         */
        @Param({
                "WELL_44497_B",
                //"ISAAC",
                "XO_RO_SHI_RO_128_PLUS",
                })
        private String randomSourceName;

        /**
         * The sampler type.
         */
        @Param({"SmallMeanPoissonSampler",
                "KempSmallMeanPoissonSampler",
                "BoundedKempSmallMeanPoissonSampler",
                "KempSmallMeanPoissonSamplerP50",
                "KempSmallMeanPoissonSamplerBinarySearch",
                "KempSmallMeanPoissonSamplerGuideTable",
                "LargeMeanPoissonSampler",
                "TinyMeanPoissonSampler",
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
                "64",
                })
        private double mean;

        /** RNG. */
        private UniformRandomProvider generator;

        /** The factory. */
        private DiscreteSamplerFactory factory;

        /** The sampler. */
        private DiscreteSampler sampler;

        /**
         * @return The RNG.
         */
        public UniformRandomProvider getGenerator() {
            return generator;
        }

        /**
         * Gets the sampler.
         *
         * @return The sampler.
         */
        public DiscreteSampler getSampler() {
            return sampler;
        }

        /** Instantiates sampler. */
        @Setup
        public void setup() {
            final RandomSource randomSource = RandomSource.valueOf(randomSourceName);
            generator = RandomSource.create(randomSource);

            // This would benefit from Java 8 Supplier<DiscreteSampler> lambda function
            if ("SmallMeanPoissonSampler".equals(samplerType)) {
                factory = new DiscreteSamplerFactory() {
                    @Override
                    public DiscreteSampler create() {
                        return SmallMeanPoissonSampler.of(generator, mean);
                    }
                };
            } else if ("KempSmallMeanPoissonSampler".equals(samplerType)) {
                factory = new DiscreteSamplerFactory() {
                    @Override
                    public DiscreteSampler create() {
                        return KempSmallMeanPoissonSampler.of(generator, mean);
                    }
                };
            } else if ("BoundedKempSmallMeanPoissonSampler".equals(samplerType)) {
                factory = new DiscreteSamplerFactory() {
                    @Override
                    public DiscreteSampler create() {
                        return new BoundedKempSmallMeanPoissonSampler(generator, mean);
                    }
                };
            } else if ("KempSmallMeanPoissonSamplerP50".equals(samplerType)) {
                factory = new DiscreteSamplerFactory() {
                    @Override
                    public DiscreteSampler create() {
                        return new KempSmallMeanPoissonSamplerP50(generator, mean);
                    }
                };
            } else if ("KempSmallMeanPoissonSamplerBinarySearch".equals(samplerType)) {
                factory = new DiscreteSamplerFactory() {
                    @Override
                    public DiscreteSampler create() {
                        return new KempSmallMeanPoissonSamplerBinarySearch(generator, mean);
                    }
                };
            } else if ("KempSmallMeanPoissonSamplerGuideTable".equals(samplerType)) {
                factory = new DiscreteSamplerFactory() {
                    @Override
                    public DiscreteSampler create() {
                        return new KempSmallMeanPoissonSamplerGuideTable(generator, mean);
                    }
                };
            } else if ("LargeMeanPoissonSampler".equals(samplerType)) {
                factory = new DiscreteSamplerFactory() {
                    @Override
                    public DiscreteSampler create() {
                        // Note this is not valid when mean < 1
                        return LargeMeanPoissonSampler.of(generator, mean);
                    }
                };
            } else if ("TinyMeanPoissonSampler".equals(samplerType)) {
                factory = new DiscreteSamplerFactory() {
                    @Override
                    public DiscreteSampler create() {
                        // Note this is only valid when mean < -Math.exp(0x1p-32) == 22.18
                        return new TinyMeanPoissonSampler(generator, mean);
                    }
                };
            }
            sampler = factory.create();
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
     * <p>Note: This is similar to {@link KempSmallMeanPoissonSampler} but the sample is
     * bounded by 1000 * mean.</p>
     *
     * @see <a href="https://www.jstor.org/stable/2346348">Kemp, A.W. (1981) JRSS Vol. 30, pp. 249-253</a>
     */
    static class BoundedKempSmallMeanPoissonSampler
        implements DiscreteSampler {
        /** Underlying source of randomness. */
        private final UniformRandomProvider rng;
        /**
         * Pre-compute {@code Math.exp(-mean)}.
         * Note: This is the probability of the Poisson sample {@code p(x=0)}.
         */
        private final double p0;
        /** Pre-compute {@code 1000 * mean} as the upper limit of the sample. */
        private final int limit;
        /**
         * The mean of the Poisson sample.
         */
        private final double mean;

        /**
         * @param rng  Generator of uniformly distributed random numbers.
         * @param mean Mean.
         * @throws IllegalArgumentException if {@code mean <= 0} or {@code mean > 700}.
         */
        BoundedKempSmallMeanPoissonSampler(UniformRandomProvider rng,
                                            double mean) {
            if (mean <= 0) {
                throw new IllegalArgumentException();
            }

            p0 = Math.exp(-mean);
            if (p0 == 0) {
                throw new IllegalArgumentException();
            }
            // The returned sample is bounded by 1000 * mean
            limit = (int) Math.ceil(1000 * mean);
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
                // is positive. This check is added to ensure a simple bounds in the event that
                // p == 0
                if (x == limit) {
                    return x;
                }
            }
            return x;
        }
    }

    /**
     * Kemp sampler for the Poisson distribution.
     *
     * <p>Note: This is a modification of the original algorithm by Kemp. It implements a hedge
     * on the cumulative probability set at 50%. This saves computation in half of the samples.</p>
     */
    static class KempSmallMeanPoissonSamplerP50
        implements DiscreteSampler {
        /** The value of p=0.5. */
        private static final double ONE_HALF = 0.5;
        /**
         * The threshold that defines the cumulative probability for the long tail of the
         * Poisson distribution. Above this threshold the recurrence relation that computes the
         * next probability must check that the p-value is not zero.
         */
        private static final double LONG_TAIL_THRESHOLD = 0.999;

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
         * Pre-compute the cumulative probability for all samples up to and including x.
         * This is F(x) = sum of p(X<=x).
         *
         * <p>The value is computed at approximately 50% allowing the algorithm to choose to start
         * at value (x+1) approximately half of the time.
         */
        private final double fx;
        /**
         * Store the value (x+1) corresponding to the next value after the cumulative probability is
         * above 50%.
         */
        private final int x1;
        /**
         * Store the probability value p(x+1), allowing the algorithm to start from the point x+1.
         */
        private final double px1;

        /**
         * Create a new instance.
         *
         * <p>This is valid for means as large as approximately {@code 744}.</p>
         *
         * @param rng  Generator of uniformly distributed random numbers.
         * @param mean Mean.
         * @throws IllegalArgumentException if {@code mean <= 0} or {@code Math.exp(-mean) == 0}.
         */
        KempSmallMeanPoissonSamplerP50(UniformRandomProvider rng,
                                       double mean) {
            if (mean <= 0) {
                throw new IllegalArgumentException();
            }

            this.rng = rng;
            p0 = Math.exp(-mean);
            this.mean = mean;

            // Pre-compute a hedge value for the cumulative probability at approximately 50%.
            // This is only done when p0 is less than the long tail threshold.
            // The result is that the rolling probability computation should never hit the
            // long tail where p reaches zero.
            if (p0 <= LONG_TAIL_THRESHOLD) {
                // Check the edge case for no probability
                if (p0 == 0) {
                    throw new IllegalArgumentException();
                }

                double p = p0;
                int x = 0;
                // Sum is cumulative probability F(x) = sum p(X<=x)
                double sum = p;
                while (sum < ONE_HALF) {
                    // Compute the next probability using a recurrence relation.
                    // p(x+1) = p(x) * mean / (x+1)
                    p *= mean / ++x;
                    sum += p;
                }
                fx = sum;
                x1 = x + 1;
                px1 = p * mean / x1;
            } else {
                // Always start at zero.
                // Note: If NaN is input as the mean this path is executed and the sample is always zero.
                fx = 0;
                x1 = 0;
                px1 = p0;
            }
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
            final double u = rng.nextDouble();

            if (u <= fx) {
                // Sample from the lower half of the distribution starting at zero
                return sampleBeforeLongTail(u, 0, p0);
            }

            // Sample from the upper half of the distribution starting at cumulative probability fx.
            // This is reached when u > fx and sample X > x.

            // If below the long tail threshold then omit the check on the asymptote of p -> zero
            if (u <= LONG_TAIL_THRESHOLD) {
                return sampleBeforeLongTail(u - fx, x1, px1);
            }

            return sampleWithinLongTail(u - fx, x1, px1);
        }

        /**
         * Compute the sample assuming it is <strong>not</strong> in the long tail of the distribution.
         *
         * <p>This avoids a check on the next probability value assuming that the cumulative probability
         * is at a level where the long tail of the Poisson distribution will not be reached.
         *
         * @param u the remaining cumulative probability (p(X>x))
         * @param x the current sample value X
         * @param p the current probability of the sample (p(X=x))
         * @return the sample X
         */
        private int sampleBeforeLongTail(double u, int x, double p) {
            while (u > p) {
                // Update the remaining cumulative probability
                u -= p;
                // Compute the next probability using a recurrence relation.
                // p(x+1) = p(x) * mean / (x+1)
                p *= mean / ++x;
                // The algorithm listed in Kemp (1981) does not check that the rolling probability
                // is positive (non-zero). This is omitted here on the assumption that the cumulative
                // probability will not be in the long tail where the probability asymptotes to zero.
            }
            return x;
        }

        /**
         * Compute the sample assuming it is in the long tail of the distribution.
         *
         * <p>This requires a check on the next probability value which is expected to asymptote to zero.
         *
         * @param u the remaining cumulative probability
         * @param x the current sample value X
         * @param p the current probability of the sample (p(X=x))
         * @return the sample X
         */
        private int sampleWithinLongTail(double u, int x, double p) {
            while (u > p) {
                // Update the remaining cumulative probability
                u -= p;
                // Compute the next probability using a recurrence relation.
                // p(x+1) = p(x) * mean / (x+1)
                p *= mean / ++x;
                // The algorithm listed in Kemp (1981) does not check that the rolling probability
                // is positive. This check is added to ensure no errors when the limit of the summation
                // 1 - sum(p(x)) is above 0 due to cumulative error in floating point arithmetic when
                // in the long tail of the distribution.
                if (p == 0) {
                    return x;
                }
            }
            return x;
        }
    }

    /**
     * Kemp sampler for the Poisson distribution.
     *
     * <p>Note: This is a modification of the original algorithm by Kemp. It stores the
     * cumulative probability table for repeat use. The table is searched using a binary
     * search algorithm.</p>
     */
    static class KempSmallMeanPoissonSamplerBinarySearch
        implements DiscreteSampler {
        /**
         * Store the cumulative probability table size for integer means so that 99.99%
         * of the Poisson distribution is covered. This is done until the table size is
         * 2 * mean.
         *
         * <p>At higher mean the expected range is mean + 4 * sqrt(mean). To avoid the sqrt
         * the conservative limit of 2 * mean is used.
         */
        private static final int[] TABLE_SIZE = {
            /* mean 1 to 10. */
            8, 10, 12, 14, 16, 18, 20, 22, 24, 25,
            /* mean 11 to 20. */
            27, 29, 30, 32, 33, 35, 36, 38, 39, 41,
        };

        /** Underlying source of randomness. */
        private final UniformRandomProvider rng;
        /**
         * The mean of the Poisson sample.
         */
        private final double mean;
        /**
         * Store the cumulative probability for all samples up to and including x.
         * This is F(x) = sum of p(X<=x).
         *
         * <p>This table is initialised to store cumulative probabilities for x up to 2 * mean
         * or 99.99% (whichever is larger).
         */
        private final double[] fx;
        /**
         * Store the value x corresponding to the last stored cumulative probability.
         */
        private int lastX;
        /**
         * Store the probability value p(x) corresponding to last stored cumulative probability,
         * allowing the algorithm to start from the point x.
         */
        private double px;

        /**
         * Create a new instance.
         *
         * <p>This is valid for means as large as approximately {@code 744}.</p>
         *
         * @param rng  Generator of uniformly distributed random numbers.
         * @param mean Mean.
         * @throws IllegalArgumentException if {@code mean <= 0} or {@code Math.exp(-mean) == 0}.
         */
        KempSmallMeanPoissonSamplerBinarySearch(UniformRandomProvider rng,
                                                double mean) {
            if (mean <= 0) {
                throw new IllegalArgumentException();
            }

            px = Math.exp(-mean);
            if (px > 0) {
                this.rng = rng;
                this.mean = mean;

                // Initialise the cumulative probability table.
                // The supported mean where p(x=0) > 0 sets a limit of around 744 so this will always be
                // possible.
                final int upperMean = (int) Math.ceil(mean);
                fx = new double[(upperMean < TABLE_SIZE.length) ? TABLE_SIZE[upperMean] : upperMean * 2];
                fx[0] = px;
            } else {
                // This will catch NaN mean values
                throw new IllegalArgumentException();
            }
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
            final double u = rng.nextDouble();

            if (u <= fx[lastX]) {
                // Binary search within known cumulative probability table.
                // Find x so that u > f[x-1] and u <= f[x].
                // This is a looser search than Arrays.binarySearch:
                // - The output is x = upper.
                // - The pre-condition check ensures u <= f[upper] at the start.
                // - The table stores probabilities where f[0] is non-zero.
                // - u should be >= 0 (or the random generator is broken).
                // - It avoids comparisons using Double.doubleToLongBits.
                // - It avoids the low likelihood of equality between two doubles so uses
                //   only 1 compare per loop.
                // - It uses a weighted middle anticipating that the cumulative distribution
                //   is skewed as the expected use case is a small mean.
                int lower = 0;
                int upper = lastX;
                while (lower < upper) {
                    // Weighted middle is 1/4 of the range between lower and upper
                    final int mid = (3 * lower + upper) >>> 2;
                    final double midVal = fx[mid];
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

            // The sample is above x
            int x1 = lastX + 1;

            // Fill the cumulative probability table if possible
            while (x1 < fx.length) {
                // Compute the next probability using a recurrence relation.
                // p(x+1) = p(x) * mean / (x+1)
                px = nextProbability(px, x1);
                // Compute the next cumulative probability f(x+1) and update
                final double sum = fx[lastX] + px;
                fx[++lastX] = sum;
                // Check if this is the correct sample
                if (u <= sum) {
                    return lastX;
                }
                x1 = lastX + 1;
            }

            // The sample is above the range of the cumulative probability table.
            // Compute using the Kemp method.
            // This requires the remaining cumulative probability and the probability for x+1.
            return sampleWithinLongTail(u - fx[lastX], x1, nextProbability(px, x1));
        }

        /**
         * Compute the next probability using a recurrence relation.
         *
         * <pre>
         * p(x + 1) = p(x) * mean / (x + 1)
         * </pre>
         *
         * @param p the probability of x
         * @param x1 the value of x+1
         * @return the probability of x+1
         */
        private double nextProbability(double p, int x1) {
            return p * mean / x1;
        }

        /**
         * Compute the sample assuming it is in the long tail of the distribution.
         *
         * <p>This requires a check on the next probability value which is expected to asymptote to zero.
         *
         * @param u the remaining cumulative probability
         * @param x the current sample value X
         * @param p the current probability of the sample (p(X=x))
         * @return the sample X
         */
        private int sampleWithinLongTail(double u, int x, double p) {
            while (u > p) {
                // Update the remaining cumulative probability
                u -= p;
                p = nextProbability(p, ++x);
                // The algorithm listed in Kemp (1981) does not check that the rolling probability
                // is positive. This check is added to ensure no errors when the limit of the summation
                // 1 - sum(p(x)) is above 0 due to cumulative error in floating point arithmetic when
                // in the long tail of the distribution.
                if (p == 0) {
                    return x;
                }
            }
            return x;
        }
    }
    /**
     * Sampler for the <a href="http://mathworld.wolfram.com/PoissonDistribution.html">Poisson
     * distribution</a>.
     *
     * <p>Note: This is a modification of the original algorithm by Kemp. It stores the
     * cumulative probability table for repeat use. The table is computed dynamically and
     * searched using a guide table.</p>
     */
    static class KempSmallMeanPoissonSamplerGuideTable implements DiscreteSampler {
        /**
         * Store the cumulative probability table size for integer means so that 99.99% of the
         * Poisson distribution is covered. This is done until the table size is 2 * mean.
         *
         * <p>At higher mean the expected range is mean + 4 * sqrt(mean). To avoid the sqrt
         * the conservative limit of 2 * mean is used.
         */
        private static final int[] TABLE_SIZE = {
            /* mean 1 to 10. */
            8, 10, 12, 14, 16, 18, 20, 22, 24, 25,
            /* mean 11 to 20. */
            27, 29, 30, 32, 33, 35, 36, 38, 39, 41, };

        /** Underlying source of randomness. */
        private final UniformRandomProvider rng;
        /**
         * The mean of the Poisson sample.
         */
        private final double mean;
        /**
         * Store the cumulative probability for all samples up to and including x. This is
         * F(x) = sum of p(X<=x).
         *
         * <p>This table is initialised to store cumulative probabilities for x up to 2 * mean
         * or 99.99% (whichever is larger).
         */
        private final double[] cumulativeProbability;
        /**
         * Store the value x corresponding to the last stored cumulative probability.
         */
        private int tabulatedX;
        /**
         * Store the probability value p(x), allowing the algorithm to start from the point x.
         */
        private double probabilityX;
        /**
         * The inverse cumulative probability guide table. This is a map between the cumulative
         * probability (f(x)) and the value x. It is used to set the initial point for search
         * of the cumulative probability table.
         *
         * <p>The index into the table is obtained using {@code f(x) * guideTable.length}. The value
         * stored at the index is value {@code x+1} such that it is the exclusive upper bound
         * on the sample value for searching the cumulative probability table. It requires the
         * table search is towards zero.</p>
         *
         * <p>Note: Using x+1 ensures that the table can be zero filled upon initialisation and
         * any index with zero has yet to be allocated.</p>
         *
         * <p>The guide table should never be used when the input f(x) is above the current range of
         * the cumulative probability table. This would create an index that has not been
         * allocated a mapping.
         */
        private final int[] guideTable;

        /**
         * Create a new instance.
         *
         * <p>This is valid for means as large as approximately {@code 744}.</p>
         *
         * @param rng Generator of uniformly distributed random numbers.
         * @param mean Mean.
         * @throws IllegalArgumentException if {@code mean <= 0} or
         * {@code Math.exp(-mean) == 0}.
         */
        KempSmallMeanPoissonSamplerGuideTable(UniformRandomProvider rng, double mean) {
            if (mean <= 0) {
                throw new IllegalArgumentException("mean is not strictly positive: " + mean);
            }

            probabilityX = Math.exp(-mean);
            if (probabilityX > 0) {
                this.rng = rng;
                this.mean = mean;

                // Initialise the cumulative probability table.
                // The supported mean where p(x=0) > 0 sets a limit of around 744 so the cast to int
                // will always be possible.
                final int upperMean = (int) Math.ceil(mean);
                final int size = (upperMean < TABLE_SIZE.length) ? TABLE_SIZE[upperMean] : upperMean * 2;
                cumulativeProbability = new double[size];
                cumulativeProbability[0] = probabilityX;

                guideTable = new int[cumulativeProbability.length + 1];
                initialiseGuideTable(probabilityX);
            } else {
                // This will catch NaN mean values
                throw new IllegalArgumentException("No probability for mean " + mean);
            }
        }

        /**
         * Initialise the cumulative probability guide table. All guide indices at or below the
         * index corresponding to the given probability will be set to 1.
         *
         * @param p0 the probability for x=0
         */
        private void initialiseGuideTable(double p0) {
            for (int index = getGuideTableIndex(p0); index >= 0; index--) {
                guideTable[index] = 1;
            }
        }

        /**
         * Fill the inverse cumulative probability guide table. Set the index corresponding to the
         * given probability to x+1 establishing an exclusive upper bound on x for the probability.
         * All unused guide indices below the index will also be set to x+1.
         *
         * @param p the cumulative probability
         * @param x the sample value x
         */
        private void updateGuideTable(double p, int x) {
            // Always set the current index as the guide table is the exclusive upper bound
            // for searching the cumulative probability table for any value p.
            // Then fill any lower positions that are not allocated.
            final int x1 = x + 1;
            int index = getGuideTableIndex(p);
            do {
                guideTable[index--] = x1;
            } while (index > 0 && guideTable[index] == 0);
        }

        /**
         * Gets the guide table index for the probability. This is obtained using
         * {@code p * (guideTable.length - 1)} so is inside the length of the table.
         *
         * @param p the cumulative probability
         * @return the guide table index
         */
        private int getGuideTableIndex(double p) {
            // Note: This is only ever called when p is in the range of the cumulative
            // probability table. So assume 0 <= p <= 1.
            return (int) (p * (guideTable.length - 1));
        }

        /** {@inheritDoc} */
        @Override
        public int sample() {
            // Note on the algorithm:
            // 1. Compute a cumulative probability with a uniform deviate (u).
            // 2. If the probability lies within the tabulated cumulative probabilities
            //    then find the sample value.
            // 3. If possible expand the tabulated cumulative probabilities up to the value u.
            // 4. If value u exceeds the capacity for the tabulated cumulative probabilities
            //    then compute the sample value dynamically without storing the probabilities.

            // Compute a probability
            final double u = rng.nextDouble();

            // Check the tabulated cumulative probability table
            if (u <= cumulativeProbability[tabulatedX]) {
                // Initialise the search using a guide table to find an initial guess.
                // The table provides an upper bound on the sample for a known cumulative probability.
                int sample = guideTable[getGuideTableIndex(u)] - 1;
                // If u is above the sample probability (this occurs due to truncation)
                // then return the next value up.
                if (u > cumulativeProbability[sample]) {
                    return sample + 1;
                }
                // Search down
                while (sample != 0 && u <= cumulativeProbability[sample - 1]) {
                    sample--;
                }
                return sample;
            }

            // The sample is above the tabulated cumulative probability for x
            int x1 = tabulatedX + 1;

            // Fill the cumulative probability table if possible
            while (x1 < cumulativeProbability.length) {
                probabilityX = nextProbability(probabilityX, x1);
                // Compute the next cumulative probability f(x+1) and update
                final double sum = cumulativeProbability[tabulatedX] + probabilityX;
                cumulativeProbability[++tabulatedX] = sum;
                updateGuideTable(sum, tabulatedX);
                // Check if this is the correct sample
                if (u <= sum) {
                    return tabulatedX;
                }
                x1 = tabulatedX + 1;
            }

            // The sample is above the range of the cumulative probability table.
            // Compute using the Kemp method.
            // This requires the remaining cumulative probability and the probability for x+1.
            return sampleWithinLongTail(u - cumulativeProbability[tabulatedX], x1, nextProbability(probabilityX, x1));
        }

        /**
         * Compute the next probability using a recurrence relation.
         *
         * <pre>
         * p(x + 1) = p(x) * mean / (x + 1)
         * </pre>
         *
         * @param px the probability of x
         * @param x1 the value of x+1
         * @return the probability of x+1
         */
        private double nextProbability(double px, int x1) {
            return px * mean / x1;
        }

        /**
         * Compute the sample assuming it is in the long tail of the distribution.
         *
         * <p>This requires a check on the next probability value which is expected to
         * asymptote to zero.
         *
         * @param u the remaining cumulative probability
         * @param x the current sample value X
         * @param p the current probability of the sample (p(X=x))
         * @return the sample X
         */
        private int sampleWithinLongTail(double u, int x, double p) {
            // Note on the algorithm:
            // - X is the unknown sample deviate (the output of the algorithm)
            // - x is the current value from the distribution
            // - p is the probability of the current value x, p(X=x)
            // - u is effectively the cumulative probability that the sample X
            // is equal or above the current value x, p(X>=x)
            // So if p(X>=x) > p(X=x) the sample must be above x, otherwise it is x
            while (u > p) {
                // Update the remaining cumulative probability
                u -= p;
                p = nextProbability(p, ++x);
                // The algorithm listed in Kemp (1981) does not check that the rolling
                // probability is positive. This check is added to ensure no errors when the
                // limit of the summation 1 - sum(p(x)) is above 0 due to cumulative error in
                // floating point arithmetic when in the long tail of the distribution.
                if (p == 0) {
                    break;
                }
            }
            return x;
        }
    }

    /**
     * Sampler for the <a href="http://mathworld.wolfram.com/PoissonDistribution.html">Poisson distribution</a>.
     *
     * <ul>
     *  <li>
     *   For small means, a Poisson process is simulated using uniform deviates, as
     *   described in Knuth (1969). Seminumerical Algorithms. The Art of Computer Programming,
     *   Volume 2. Addison Wesley.
     *  </li>
     * </ul>
     *
     * <p>This sampler is suitable for {@code mean < 20}.</p>
     *
     * <p>Sampling uses {@link UniformRandomProvider#nextInt()} and 32-bit integer arithmetic.</p>
     *
     * @see <a href="https://en.wikipedia.org/wiki/Poisson_distribution#Generating_Poisson-distributed_random_variables">
     * Poisson random variables</a>
     */
    static class TinyMeanPoissonSampler
        implements DiscreteSampler {
        /** Pre-compute Poisson probability p(n=0) mapped to the range of a 32-bit unsigned fraction. */
        private final long p0;
        /** Underlying source of randomness. */
        private final UniformRandomProvider rng;

        /**
         * @param rng  Generator of uniformly distributed random numbers.
         * @param mean Mean.
         * @throws IllegalArgumentException if {@code mean <= 0} or {@code Math.exp(-mean) * (1L << 32)}
         * is not positive.
         */
        TinyMeanPoissonSampler(UniformRandomProvider rng,
                               double mean) {
            this.rng = rng;
            if (mean <= 0) {
                throw new IllegalArgumentException();
            }
            // Math.exp(-mean) is the probability of a Poisson distribution for n=0 (p(n=0)).
            // This is mapped to a 32-bit range as the numerator of a 32-bit fraction
            // for use in optimised 32-bit arithmetic.
            p0 = (long) (Math.exp(-mean) * 0x100000000L);
            if (p0 == 0) {
                throw new IllegalArgumentException("No p(x=0) probability for mean: " + mean);
            }
        }

        /** {@inheritDoc} */
        @Override
        public int sample() {
            int n = 0;
            // The unsigned 32-bit sample represents the fraction x / 2^32 where x is [0,2^32-1].
            // The upper bound is exclusive hence the fraction is a uniform deviate from [0,1).
            long r = nextUnsigned32();
            // The limit is the probability p(n=0) converted to an unsigned fraction.
            while (r >= p0) {
                // Compute the fraction:
                //  r       [0,2^32)      2^32
                // ----  *  --------   /  ----
                // 2^32       2^32        2^32
                // This rounds down the fraction numerator when the lower 32-bits are discarded.
                r = (r * nextUnsigned32()) >>> 32;
                n++;
            }
            // Ensure the value is positive in the worst case scenario of a broken
            // generator that returns 0xffffffff for each sample. This will cause
            // the integer counter to overflow 2^31-1 but not exceed 2^32. The fraction
            // multiplication effectively turns r into a counter decrementing from 2^32-1
            // to zero.
            return (n >= 0) ? n : Integer.MAX_VALUE;
        }

        /**
         * Get the next unsigned 32-bit random integer.
         *
         * @return the random integer
         */
        private long nextUnsigned32() {
            return rng.nextInt() & 0xffffffffL;
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
     * Baseline for {@link Math#exp(double)}.
     *
     * @param mean the mean
     * @return the value
     */
    @Benchmark
    public double baselineExp(Means mean) {
        return Math.exp(-mean.getMean());
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

    /**
     * Run the sampler.
     *
     * @param sources Source of randomness.
     * @return the sample value
     */
    @Benchmark
    public int singleSample(Sources sources) {
        return sources.createSampler().sample();
    }
}
