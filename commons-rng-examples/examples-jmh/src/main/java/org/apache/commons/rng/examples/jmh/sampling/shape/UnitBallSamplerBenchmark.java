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

package org.apache.commons.rng.examples.jmh.sampling.shape;

import org.apache.commons.rng.UniformRandomProvider;
import org.apache.commons.rng.sampling.ObjectSampler;
import org.apache.commons.rng.sampling.distribution.ContinuousSampler;
import org.apache.commons.rng.sampling.distribution.NormalizedGaussianSampler;
import org.apache.commons.rng.sampling.distribution.ZigguratSampler;
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
import java.util.concurrent.TimeUnit;

/**
 * Executes benchmark to compare the speed of generating samples within an N-dimension unit ball.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@State(Scope.Benchmark)
@Fork(value = 1, jvmArgs = { "-server", "-Xms128M", "-Xmx128M" })
public class UnitBallSamplerBenchmark {
    /** Name for the baseline method. */
    private static final String BASELINE = "Baseline";
    /** Name for the rejection method. */
    private static final String REJECTION = "Rejection";
    /** Name for the disk-point picking method. */
    private static final String DISK_POINT = "DiskPoint";
    /** Name for the ball-point picking method. */
    private static final String BALL_POINT = "BallPoint";
    /** Name for the method moving from the surface of the hypersphere to an internal point. */
    private static final String HYPERSPHERE_INTERNAL = "HypersphereInternal";
    /** Name for the picking from the surface of a greater dimension hypersphere and discarding 2 points. */
    private static final String HYPERSPHERE_DISCARD = "HypersphereDiscard";
    /** Error message for an unknown sampler type. */
    private static final String UNKNOWN_SAMPLER = "Unknown sampler type: ";
    /** The mask to extract the lower 53-bits from a long. */
    private static final long LOWER_53_BITS = -1L >>> 11;

    /**
     * Base class for a sampler using a provided source of randomness.
     */
    private abstract static class BaseSampler implements ObjectSampler<double[]> {
        /** The source of randomness. */
        protected UniformRandomProvider rng;

        /**
         * Create an instance.
         *
         * @param rng the source of randomness
         */
        BaseSampler(UniformRandomProvider rng) {
            this.rng = rng;
        }
    }

    /**
     * Base class for the sampler data.
     * Contains the source of randomness and the number of samples.
     * The sampler should be created by a sub-class of the data.
     */
    @State(Scope.Benchmark)
    public abstract static class SamplerData {
        /** The sampler. */
        private ObjectSampler<double[]> sampler;

        /** The number of samples. */
        @Param({//"1",
            "100"})
        private int size;

        /**
         * Gets the size.
         *
         * @return the size
         */
        public int getSize() {
            return size;
        }

        /**
         * Gets the sampler.
         *
         * @return the sampler
         */
        public ObjectSampler<double[]> getSampler() {
            return sampler;
        }

        /**
         * Create the source of randomness.
         */
        @Setup
        public void setup() {
            // This could be configured using @Param
            final UniformRandomProvider rng = RandomSource.XO_RO_SHI_RO_128_PP.create();
            sampler = createSampler(rng);
        }

        /**
         * Creates the sampler.
         *
         * @param rng the source of randomness
         * @return the sampler
         */
        protected abstract ObjectSampler<double[]> createSampler(UniformRandomProvider rng);
    }

    /**
     * The 1D unit line sampler.
     */
    @State(Scope.Benchmark)
    public static class Sampler1D extends SamplerData {
        /** Name for the signed double method. */
        private static final String SIGNED_DOUBLE = "signedDouble";
        /** Name for the signed double method, version 2. */
        private static final String SIGNED_DOUBLE2 = "signedDouble2";
        /** Name for the two doubles method. */
        private static final String TWO_DOUBLES = "twoDoubles";
        /** Name for the boolean double method. */
        private static final String BOOLEAN_DOUBLE = "booleanDouble";

        /** The sampler type. */
        @Param({BASELINE, SIGNED_DOUBLE, SIGNED_DOUBLE2, TWO_DOUBLES, BOOLEAN_DOUBLE})
        private String type;

        /** {@inheritDoc} */
        @Override
        protected ObjectSampler<double[]> createSampler(final UniformRandomProvider rng) {
            if (BASELINE.equals(type)) {
                return () -> new double[] {0.5};
            } else if (SIGNED_DOUBLE.equals(type)) {
                // Sample [-1, 1) uniformly
                return () -> new double[] {makeSignedDouble(rng.nextLong())};
            } else if (SIGNED_DOUBLE2.equals(type)) {
                // Sample [-1, 1) uniformly
                return () -> new double[] {makeSignedDouble2(rng.nextLong())};
            } else if (TWO_DOUBLES.equals(type)) {
                // Sample [-1, 1) excluding -0.0 but also missing the final 1.0 - 2^-53.
                // The 1.0 could be adjusted to 1.0 - 2^-53 to create the interval (-1, 1).
                return () -> new double[] {rng.nextDouble() + rng.nextDouble() - 1.0};
            } else if (BOOLEAN_DOUBLE.equals(type)) {
                // This will sample (-1, 1) including -0.0 and 0.0
                return () -> new double[] {rng.nextBoolean() ? -rng.nextDouble() : rng.nextDouble()};
            }
            throw new IllegalStateException(UNKNOWN_SAMPLER + type);
        }
    }

    /**
     * The 2D unit disk sampler.
     */
    @State(Scope.Benchmark)
    public static class Sampler2D extends SamplerData {
        /** The sampler type. */
        @Param({BASELINE, REJECTION, DISK_POINT, HYPERSPHERE_INTERNAL, HYPERSPHERE_DISCARD})
        private String type;

        /** {@inheritDoc} */
        @Override
        protected ObjectSampler<double[]> createSampler(final UniformRandomProvider rng) {
            if (BASELINE.equals(type)) {
                return () -> new double[] {0.5, 0};
            } else if (REJECTION.equals(type)) {
                return new RejectionSampler(rng);
            } else if (DISK_POINT.equals(type)) {
                return new DiskPointSampler(rng);
            } else if (HYPERSPHERE_INTERNAL.equals(type)) {
                return new HypersphereInternalSampler(rng);
            } else if (HYPERSPHERE_DISCARD.equals(type)) {
                return new HypersphereDiscardSampler(rng);
            }
            throw new IllegalStateException(UNKNOWN_SAMPLER + type);
        }

        /**
         * Sample using a simple rejection method.
         */
        private static class RejectionSampler extends BaseSampler {
            /**
             * @param rng the source of randomness
             */
            RejectionSampler(UniformRandomProvider rng) {
                super(rng);
            }

            @Override
            public double[] sample() {
                // Generate via rejection method of a circle inside a square of edge length 2.
                // This should compute approximately 2^2 / pi = 1.27 square positions per sample.
                double x;
                double y;
                do {
                    x = makeSignedDouble(rng.nextLong());
                    y = makeSignedDouble(rng.nextLong());
                } while (x * x + y * y > 1.0);
                return new double[] {x, y};
            }
        }

        /**
         * Sample using disk point picking.
         * @see <a href="https://mathworld.wolfram.com/DiskPointPicking.html">Disk point picking</a>
         */
        private static class DiskPointSampler extends BaseSampler {
            /** 2 pi. */
            private static final double TWO_PI = 2 * Math.PI;

            /**
             * @param rng the source of randomness
             */
            DiskPointSampler(UniformRandomProvider rng) {
                super(rng);
            }

            @Override
            public double[] sample() {
                final double t = TWO_PI * rng.nextDouble();
                final double r = Math.sqrt(rng.nextDouble());
                final double x = r * Math.cos(t);
                final double y = r * Math.sin(t);
                return new double[] {x, y};
            }
        }

        /**
         * Choose a uniform point X on the unit hypersphere, then multiply it by U<sup>1/n</sup>
         * where U in [0, 1].
         * @see <a href="https://mathoverflow.net/questions/309567/sampling-a-uniformly-distributed-point-inside-a-hypersphere">
         * Sampling a uniformly distributed point INSIDE a hypersphere?</a>
         */
        private static class HypersphereInternalSampler extends BaseSampler {
            /** The normal distribution. */
            private final NormalizedGaussianSampler normal;

            /**
             * @param rng the source of randomness
             */
            HypersphereInternalSampler(UniformRandomProvider rng) {
                super(rng);
                normal = ZigguratSampler.NormalizedGaussian.of(rng);
            }

            @Override
            public double[] sample() {
                final double x = normal.sample();
                final double y = normal.sample();
                final double sum = x * x + y * y;
                // Note: Handle the possibility of a zero sum and invalid inverse
                if (sum == 0) {
                    return sample();
                }
                // Take a point on the unit hypersphere then multiply it by U^1/n
                final double f = Math.sqrt(rng.nextDouble()) / Math.sqrt(sum);
                return new double[] {x * f, y * f};
            }
        }

        /**
         * Take a random point on the (n+1)-dimensional hypersphere and drop two coordinates.
         * Remember that the (n+1)-hypersphere is the unit sphere of R^(n+2).
         * @see <a href="https://mathoverflow.net/questions/309567/sampling-a-uniformly-distributed-point-inside-a-hypersphere">
         * Sampling a uniformly distributed point INSIDE a hypersphere?</a>
         */
        private static class HypersphereDiscardSampler extends BaseSampler {
            /** The normal distribution. */
            private final NormalizedGaussianSampler normal;

            /**
             * @param rng the source of randomness
             */
            HypersphereDiscardSampler(UniformRandomProvider rng) {
                super(rng);
                normal = ZigguratSampler.NormalizedGaussian.of(rng);
            }

            @Override
            public double[] sample() {
                // Discard 2 samples from the coordinate but include in the sum
                final double x0 = normal.sample();
                final double x1 = normal.sample();
                final double x = normal.sample();
                final double y = normal.sample();
                final double sum = x0 * x0 + x1 * x1 + x * x + y * y;
                // Note: Handle the possibility of a zero sum and invalid inverse
                if (sum == 0) {
                    return sample();
                }
                final double f = 1.0 / Math.sqrt(sum);
                return new double[] {x * f, y * f};
            }
        }
    }

    /**
     * The 3D unit ball sampler.
     */
    @State(Scope.Benchmark)
    public static class Sampler3D extends SamplerData {
        /** The sampler type. */
        @Param({BASELINE, REJECTION, BALL_POINT, HYPERSPHERE_INTERNAL, HYPERSPHERE_DISCARD})
        private String type;

        /** {@inheritDoc} */
        @Override
        protected ObjectSampler<double[]> createSampler(final UniformRandomProvider rng) {
            if (BASELINE.equals(type)) {
                return () -> new double[] {0.5, 0, 0};
            } else if (REJECTION.equals(type)) {
                return new RejectionSampler(rng);
            } else if (BALL_POINT.equals(type)) {
                return new BallPointSampler(rng);
            } else if (HYPERSPHERE_INTERNAL.equals(type)) {
                return new HypersphereInternalSampler(rng);
            } else if (HYPERSPHERE_DISCARD.equals(type)) {
                return new HypersphereDiscardSampler(rng);
            }
            throw new IllegalStateException(UNKNOWN_SAMPLER + type);
        }

        /**
         * Sample using a simple rejection method.
         */
        private static class RejectionSampler extends BaseSampler {
            /**
             * @param rng the source of randomness
             */
            RejectionSampler(UniformRandomProvider rng) {
                super(rng);
            }

            @Override
            public double[] sample() {
                // Generate via rejection method of a ball inside a cube of edge length 2.
                // This should compute approximately 2^3 / (4pi/3) = 1.91 cube positions per sample.
                double x;
                double y;
                double z;
                do {
                    x = makeSignedDouble(rng.nextLong());
                    y = makeSignedDouble(rng.nextLong());
                    z = makeSignedDouble(rng.nextLong());
                } while (x * x + y * y + z * z > 1.0);
                return new double[] {x, y, z};
            }
        }

        /**
         * Sample using ball point picking.
         * @see <a href="https://mathworld.wolfram.com/BallPointPicking.html">Ball point picking</a>
         */
        private static class BallPointSampler extends BaseSampler {
            /** The normal distribution. */
            private final NormalizedGaussianSampler normal;
            /** The exponential distribution. */
            private final ContinuousSampler exp;

            /**
             * @param rng the source of randomness
             */
            BallPointSampler(UniformRandomProvider rng) {
                super(rng);
                normal = ZigguratSampler.NormalizedGaussian.of(rng);
                // Exponential(mean=2) == Chi-squared distribution(degrees freedom=2)
                // thus is the equivalent of the HypersphereDiscardSampler.
                // Here we use mean = 1 and scale the output later.
                exp = ZigguratSampler.Exponential.of(rng);
            }

            @Override
            public double[] sample() {
                final double x = normal.sample();
                final double y = normal.sample();
                final double z = normal.sample();
                // Include the exponential sample. It has mean 1 so multiply by 2.
                final double sum = exp.sample() * 2 + x * x + y * y + z * z;
                // Note: Handle the possibility of a zero sum and invalid inverse
                if (sum == 0) {
                    return sample();
                }
                final double f = 1.0 / Math.sqrt(sum);
                return new double[] {x * f, y * f, z * f};
            }
        }

        /**
         * Choose a uniform point X on the unit hypersphere, then multiply it by U<sup>1/n</sup>
         * where U in [0, 1].
         * @see <a href="https://mathoverflow.net/questions/309567/sampling-a-uniformly-distributed-point-inside-a-hypersphere">
         * Sampling a uniformly distributed point INSIDE a hypersphere?</a>
         */
        private static class HypersphereInternalSampler extends BaseSampler {
            /** The normal distribution. */
            private final NormalizedGaussianSampler normal;

            /**
             * @param rng the source of randomness
             */
            HypersphereInternalSampler(UniformRandomProvider rng) {
                super(rng);
                normal = ZigguratSampler.NormalizedGaussian.of(rng);
            }

            @Override
            public double[] sample() {
                final double x = normal.sample();
                final double y = normal.sample();
                final double z = normal.sample();
                final double sum = x * x + y * y + z * z;
                // Note: Handle the possibility of a zero sum and invalid inverse
                if (sum == 0) {
                    return sample();
                }
                // Take a point on the unit hypersphere then multiply it by U^1/n
                final double f = Math.cbrt(rng.nextDouble()) / Math.sqrt(sum);
                return new double[] {x * f, y * f, z * f};
            }
        }

        /**
         * Take a random point on the (n+1)-dimensional hypersphere and drop two coordinates.
         * Remember that the (n+1)-hypersphere is the unit sphere of R^(n+2).
         * @see <a href="https://mathoverflow.net/questions/309567/sampling-a-uniformly-distributed-point-inside-a-hypersphere">
         * Sampling a uniformly distributed point INSIDE a hypersphere?</a>
         */
        private static class HypersphereDiscardSampler extends BaseSampler {
            /** The normal distribution. */
            private final NormalizedGaussianSampler normal;

            /**
             * @param rng the source of randomness
             */
            HypersphereDiscardSampler(UniformRandomProvider rng) {
                super(rng);
                normal = ZigguratSampler.NormalizedGaussian.of(rng);
            }

            @Override
            public double[] sample() {
                // Discard 2 samples from the coordinate but include in the sum
                final double x0 = normal.sample();
                final double x1 = normal.sample();
                final double x = normal.sample();
                final double y = normal.sample();
                final double z = normal.sample();
                final double sum = x0 * x0 + x1 * x1 + x * x + y * y + z * z;
                // Note: Handle the possibility of a zero sum and invalid inverse
                if (sum == 0) {
                    return sample();
                }
                final double f = 1.0 / Math.sqrt(sum);
                return new double[] {x * f, y * f, z * f};
            }
        }
    }

    /**
     * The ND unit ball sampler.
     */
    @State(Scope.Benchmark)
    public static class SamplerND extends SamplerData {
        /** The sampler type. */
        @Param({BASELINE, REJECTION, BALL_POINT, HYPERSPHERE_INTERNAL, HYPERSPHERE_DISCARD})
        private String type;
        /** The number of dimensions. */
        @Param({"3", "4", "5"})
        private int dimension;

        /** {@inheritDoc} */
        @Override
        protected ObjectSampler<double[]> createSampler(final UniformRandomProvider rng) {
            if (BASELINE.equals(type)) {
                return new ObjectSampler<double[]>() {
                    private final int dim = dimension;
                    @Override
                    public double[] sample() {
                        final double[] sample = new double[dim];
                        for (int i = 0; i < dim; i++) {
                            sample[i] = 0.01;
                        }
                        return sample;
                    }
                };
            } else if (REJECTION.equals(type)) {
                return new RejectionSampler(rng, dimension);
            } else if (BALL_POINT.equals(type)) {
                return new BallPointSampler(rng, dimension);
            } else if (HYPERSPHERE_INTERNAL.equals(type)) {
                return new HypersphereInternalSampler(rng, dimension);
            } else if (HYPERSPHERE_DISCARD.equals(type)) {
                return new HypersphereDiscardSampler(rng, dimension);
            }
            throw new IllegalStateException(UNKNOWN_SAMPLER + type);
        }

        /**
         * Sample using a simple rejection method.
         */
        private static class RejectionSampler extends BaseSampler {
            /** The dimension. */
            private final int dimension;

            /**
             * @param rng the source of randomness
             * @param dimension the dimension
             */
            RejectionSampler(UniformRandomProvider rng, int dimension) {
                super(rng);
                this.dimension = dimension;
            }

            @Override
            public double[] sample() {
                // Generate via rejection method of a ball inside a hypercube of edge length 2.
                final double[] sample = new double[dimension];
                double sum;
                do {
                    sum = 0;
                    for (int i = 0; i < dimension; i++) {
                        final double x = makeSignedDouble(rng.nextLong());
                        sum += x * x;
                        sample[i] = x;
                    }
                } while (sum > 1.0);
                return sample;
            }
        }

        /**
         * Sample using ball point picking.
         * @see <a href="https://mathworld.wolfram.com/BallPointPicking.html">Ball point picking</a>
         */
        private static class BallPointSampler extends BaseSampler {
            /** The dimension. */
            private final int dimension;
            /** The normal distribution. */
            private final NormalizedGaussianSampler normal;
            /** The exponential distribution. */
            private final ContinuousSampler exp;

            /**
             * @param rng the source of randomness
             * @param dimension the dimension
             */
            BallPointSampler(UniformRandomProvider rng, int dimension) {
                super(rng);
                this.dimension = dimension;
                normal = ZigguratSampler.NormalizedGaussian.of(rng);
                // Exponential(mean=2) == Chi-squared distribution(degrees freedom=2)
                // thus is the equivalent of the HypersphereDiscardSampler.
                // Here we use mean = 1 and scale the output later.
                exp = ZigguratSampler.Exponential.of(rng);
            }

            @Override
            public double[] sample() {
                final double[] sample = new double[dimension];
                // Include the exponential sample. It has mean 1 so multiply by 2.
                double sum = exp.sample() * 2;
                for (int i = 0; i < dimension; i++) {
                    final double x = normal.sample();
                    sum += x * x;
                    sample[i] = x;
                }
                // Note: Handle the possibility of a zero sum and invalid inverse
                if (sum == 0) {
                    return sample();
                }
                final double f = 1.0 / Math.sqrt(sum);
                for (int i = 0; i < dimension; i++) {
                    sample[i] *= f;
                }
                return sample;
            }
        }

        /**
         * Choose a uniform point X on the unit hypersphere, then multiply it by U<sup>1/n</sup>
         * where U in [0, 1].
         * @see <a href="https://mathoverflow.net/questions/309567/sampling-a-uniformly-distributed-point-inside-a-hypersphere">
         * Sampling a uniformly distributed point INSIDE a hypersphere?</a>
         */
        private static class HypersphereInternalSampler extends BaseSampler {
            /** The dimension. */
            private final int dimension;
            /** The normal distribution. */
            private final NormalizedGaussianSampler normal;
            /** Reciprocal of the dimension. */
            private final double power;

            /**
             * @param rng the source of randomness
             * @param dimension the dimension
             */
            HypersphereInternalSampler(UniformRandomProvider rng, int dimension) {
                super(rng);
                this.dimension = dimension;
                power = 1.0 / dimension;
                normal = ZigguratSampler.NormalizedGaussian.of(rng);
            }

            @Override
            public double[] sample() {
                final double[] sample = new double[dimension];
                double sum = 0;
                for (int i = 0; i < dimension; i++) {
                    final double x = normal.sample();
                    sum += x * x;
                    sample[i] = x;
                }
                // Note: Handle the possibility of a zero sum and invalid inverse
                if (sum == 0) {
                    return sample();
                }
                // Take a point on the unit hypersphere then multiply it by U^1/n
                final double f = Math.pow(rng.nextDouble(), power) / Math.sqrt(sum);
                for (int i = 0; i < dimension; i++) {
                    sample[i] *= f;
                }
                return sample;
            }
        }

        /**
         * Take a random point on the (n+1)-dimensional hypersphere and drop two coordinates.
         * Remember that the (n+1)-hypersphere is the unit sphere of R^(n+2).
         * @see <a href="https://mathoverflow.net/questions/309567/sampling-a-uniformly-distributed-point-inside-a-hypersphere">
         * Sampling a uniformly distributed point INSIDE a hypersphere?</a>
         */
        private static class HypersphereDiscardSampler extends BaseSampler {
            /** The dimension. */
            private final int dimension;
            /** The normal distribution. */
            private final NormalizedGaussianSampler normal;

            /**
             * @param rng the source of randomness
             * @param dimension the dimension
             */
            HypersphereDiscardSampler(UniformRandomProvider rng, int dimension) {
                super(rng);
                this.dimension = dimension;
                normal = ZigguratSampler.NormalizedGaussian.of(rng);
            }

            @Override
            public double[] sample() {
                final double[] sample = new double[dimension];
                // Discard 2 samples from the coordinate but include in the sum
                final double x0 = normal.sample();
                final double x1 = normal.sample();
                double sum = x0 * x0 + x1 * x1;
                for (int i = 0; i < dimension; i++) {
                    final double x = normal.sample();
                    sum += x * x;
                    sample[i] = x;
                }
                // Note: Handle the possibility of a zero sum and invalid inverse
                if (sum == 0) {
                    return sample();
                }
                final double f = 1.0 / Math.sqrt(sum);
                for (int i = 0; i < dimension; i++) {
                    sample[i] *= f;
                }
                return sample;
            }
        }
    }

    /**
     * Creates a signed double in the range {@code [-1, 1)}. The magnitude is sampled evenly
     * from the 2<sup>54</sup> dyadic rationals in the range.
     *
     * <p>Note: This method will not return samples for both -0.0 and 0.0.
     *
     * @param bits the bits
     * @return the double
     */
    private static double makeSignedDouble(long bits) {
        // As per o.a.c.rng.core.utils.NumberFactory.makeDouble(long) but using a signed
        // shift of 10 in place of an unsigned shift of 11.
        return (bits >> 10) * 0x1.0p-53d;
    }

    /**
     * Creates a signed double in the range {@code [-1, 1)}. The magnitude is sampled evenly
     * from the 2<sup>54</sup> dyadic rationals in the range.
     *
     * <p>Note: This method will not return samples for both -0.0 and 0.0.
     *
     * @param bits the bits
     * @return the double
     */
    private static double makeSignedDouble2(long bits) {
        // Use the upper 54 bits on the assumption they are more random.
        // The sign bit generates a value of 0 or 1 for subtraction.
        // The next 53 bits generates a positive number in the range [0, 1).
        // [0, 1) - (0 or 1) => [-1, 1)
        return (((bits >>> 10) & LOWER_53_BITS) * 0x1.0p-53d) - (bits >>> 63);
    }

    /**
     * Run the sampler for the configured number of samples.
     *
     * @param bh Data sink
     * @param data Input data.
     */
    private static void runSampler(Blackhole bh, SamplerData data) {
        final ObjectSampler<double[]> sampler = data.getSampler();
        for (int i = data.getSize() - 1; i >= 0; i--) {
            bh.consume(sampler.sample());
        }
    }

    /**
     * Generation of uniform samples on a 1D unit line.
     *
     * @param bh Data sink
     * @param data Input data.
     */
    @Benchmark
    public void create1D(Blackhole bh, Sampler1D data) {
        runSampler(bh, data);
    }

    /**
     * Generation of uniform samples from a 2D unit disk.
     *
     * @param bh Data sink
     * @param data Input data.
     */
    @Benchmark
    public void create2D(Blackhole bh, Sampler2D data) {
        runSampler(bh, data);
    }

    /**
     * Generation of uniform samples from a 3D unit ball.
     *
     * @param bh Data sink
     * @param data Input data.
     */
    @Benchmark
    public void create3D(Blackhole bh, Sampler3D data) {
        runSampler(bh, data);
    }

    /**
     * Generation of uniform samples from an ND unit ball.
     *
     * @param bh Data sink
     * @param data Input data.
     */
    @Benchmark
    public void createND(Blackhole bh, SamplerND data) {
        runSampler(bh, data);
    }
}
