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

package org.apache.commons.rng.examples.jmh.sampling;

import org.apache.commons.rng.UniformRandomProvider;
import org.apache.commons.rng.sampling.ObjectSampler;
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
 * Executes benchmark to compare the speed of generating samples on the surface of an
 * N-dimension unit sphere.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@State(Scope.Benchmark)
@Fork(value = 1, jvmArgs = { "-server", "-Xms128M", "-Xmx128M" })
public class UnitSphereSamplerBenchmark {
    /** Name for the baseline method. */
    private static final String BASELINE = "Baseline";
    /** Name for the non-array based method. */
    private static final String NON_ARRAY = "NonArray";
    /** Name for the array based method. */
    private static final String ARRAY = "Array";
    /** Error message for an unknown sampler type. */
    private static final String UNKNOWN_SAMPLER = "Unknown sampler type: ";

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
        @Param({"100"})
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
        /** Name for the masked int method. */
        private static final String MASKED_INT = "maskedInt";
        /** Name for the masked long method. */
        private static final String MASKED_LONG = "maskedLong";
        /** Name for the boolean method. */
        private static final String BOOLEAN = "boolean";
        /** The value 1.0 in raw long bits. */
        private static final long ONE = Double.doubleToRawLongBits(1.0);
        /** Mask to extract the sign bit from an integer (as a long). */
        private static final long SIGN_BIT = 1L << 31;

        /** The sampler type. */
        @Param({BASELINE, SIGNED_DOUBLE, MASKED_INT, MASKED_LONG, BOOLEAN, ARRAY})
        private String type;

        /** {@inheritDoc} */
        @Override
        protected ObjectSampler<double[]> createSampler(final UniformRandomProvider rng) {
            if (BASELINE.equals(type)) {
                return () -> {
                    return new double[] {1.0};
                };
            } else if (SIGNED_DOUBLE.equals(type)) {
                return () -> {
                    // (1 - 0) or (1 - 2)
                    // Use the sign bit
                    return new double[] {1.0 - ((rng.nextInt() >>> 30) & 0x2)};
                };
            } else if (MASKED_INT.equals(type)) {
                return () -> {
                    // Shift the sign bit and combine with the bits for a double of 1.0
                    return new double[] {Double.longBitsToDouble(ONE | ((rng.nextInt() & SIGN_BIT) << 32))};
                };
            } else if (MASKED_LONG.equals(type)) {
                return () -> {
                    // Combine the sign bit with the bits for a double of 1.0
                    return new double[] {Double.longBitsToDouble(ONE | (rng.nextLong() & Long.MIN_VALUE))};
                };
            } else if (BOOLEAN.equals(type)) {
                return () -> {
                    return new double[] {rng.nextBoolean() ? -1.0 : 1.0};
                };
            } else if (ARRAY.equals(type)) {
                return new ArrayBasedUnitSphereSampler(1, rng);
            }
            throw new IllegalStateException(UNKNOWN_SAMPLER + type);
        }
    }

    /**
     * The 2D unit circle sampler.
     */
    @State(Scope.Benchmark)
    public static class Sampler2D extends SamplerData {
        /** The sampler type. */
        @Param({BASELINE, ARRAY, NON_ARRAY})
        private String type;

        /** {@inheritDoc} */
        @Override
        protected ObjectSampler<double[]> createSampler(final UniformRandomProvider rng) {
            if (BASELINE.equals(type)) {
                return () -> new double[] {1.0, 0.0};
            } else if (ARRAY.equals(type)) {
                return new ArrayBasedUnitSphereSampler(2, rng);
            } else if (NON_ARRAY.equals(type)) {
                return new UnitSphereSampler2D(rng);
            }
            throw new IllegalStateException(UNKNOWN_SAMPLER + type);
        }

        /**
         * Sample from a 2D unit sphere.
         */
        private static class UnitSphereSampler2D implements ObjectSampler<double[]> {
            /** Sampler used for generating the individual components of the vectors. */
            private final NormalizedGaussianSampler sampler;

            /**
             * @param rng the source of randomness
             */
            UnitSphereSampler2D(UniformRandomProvider rng) {
                sampler = ZigguratSampler.NormalizedGaussian.of(rng);
            }

            @Override
            public double[] sample() {
                final double x = sampler.sample();
                final double y = sampler.sample();
                final double sum = x * x + y * y;

                if (sum == 0) {
                    // Zero-norm vector is discarded.
                    return sample();
                }

                final double f = 1.0 / Math.sqrt(sum);
                return new double[] {x * f, y * f};
            }
        }
    }

    /**
     * The 3D unit sphere sampler.
     */
    @State(Scope.Benchmark)
    public static class Sampler3D extends SamplerData {
        /** The sampler type. */
        @Param({BASELINE, ARRAY, NON_ARRAY})
        private String type;

        /** {@inheritDoc} */
        @Override
        protected ObjectSampler<double[]> createSampler(final UniformRandomProvider rng) {
            if (BASELINE.equals(type)) {
                return () -> new double[] {1.0, 0.0, 0.0};
            } else if (ARRAY.equals(type)) {
                return new ArrayBasedUnitSphereSampler(3, rng);
            } else if (NON_ARRAY.equals(type)) {
                return new UnitSphereSampler3D(rng);
            }
            throw new IllegalStateException(UNKNOWN_SAMPLER + type);
        }

        /**
         * Sample from a 3D unit sphere.
         */
        private static class UnitSphereSampler3D implements ObjectSampler<double[]> {
            /** Sampler used for generating the individual components of the vectors. */
            private final NormalizedGaussianSampler sampler;

            /**
             * @param rng the source of randomness
             */
            UnitSphereSampler3D(UniformRandomProvider rng) {
                sampler = ZigguratSampler.NormalizedGaussian.of(rng);
            }

            @Override
            public double[] sample() {
                final double x = sampler.sample();
                final double y = sampler.sample();
                final double z = sampler.sample();
                final double sum = x * x + y * y + z * z;

                if (sum == 0) {
                    // Zero-norm vector is discarded.
                    return sample();
                }

                final double f = 1.0 / Math.sqrt(sum);
                return new double[] {x * f, y * f, z * f};
            }
        }
    }

    /**
     * The 4D unit hypersphere sampler.
     */
    @State(Scope.Benchmark)
    public static class Sampler4D extends SamplerData {
        /** The sampler type. */
        @Param({BASELINE, ARRAY, NON_ARRAY})
        private String type;

        /** {@inheritDoc} */
        @Override
        protected ObjectSampler<double[]> createSampler(final UniformRandomProvider rng) {
            if (BASELINE.equals(type)) {
                return () -> new double[] {1.0, 0.0, 0.0, 0.0};
            } else if (ARRAY.equals(type)) {
                return new ArrayBasedUnitSphereSampler(4, rng);
            } else if (NON_ARRAY.equals(type)) {
                return new UnitSphereSampler4D(rng);
            }
            throw new IllegalStateException(UNKNOWN_SAMPLER + type);
        }

        /**
         * Sample from a 4D unit hypersphere.
         */
        private static class UnitSphereSampler4D implements ObjectSampler<double[]> {
            /** Sampler used for generating the individual components of the vectors. */
            private final NormalizedGaussianSampler sampler;

            /**
             * @param rng the source of randomness
             */
            UnitSphereSampler4D(UniformRandomProvider rng) {
                sampler = ZigguratSampler.NormalizedGaussian.of(rng);
            }

            @Override
            public double[] sample() {
                final double x = sampler.sample();
                final double y = sampler.sample();
                final double z = sampler.sample();
                final double a = sampler.sample();
                final double sum = x * x + y * y + z * z + a * a;

                if (sum == 0) {
                    // Zero-norm vector is discarded.
                    return sample();
                }

                final double f = 1.0 / Math.sqrt(sum);
                return new double[] {x * f, y * f, z * f, a * f};
            }
        }
    }

    /**
     * Sample from a unit sphere using an array based method.
     */
    private static class ArrayBasedUnitSphereSampler implements ObjectSampler<double[]> {
        /** Space dimension. */
        private final int dimension;
        /** Sampler used for generating the individual components of the vectors. */
        private final NormalizedGaussianSampler sampler;

        /**
         * @param dimension space dimension
         * @param rng the source of randomness
         */
        ArrayBasedUnitSphereSampler(int dimension, UniformRandomProvider rng) {
            this.dimension = dimension;
            sampler = ZigguratSampler.NormalizedGaussian.of(rng);
        }

        @Override
        public double[] sample() {
            final double[] v = new double[dimension];

            // Pick a point by choosing a standard Gaussian for each element,
            // and then normalize to unit length.
            double sum = 0;
            for (int i = 0; i < dimension; i++) {
                final double x = sampler.sample();
                v[i] = x;
                sum += x * x;
            }

            if (sum == 0) {
                // Zero-norm vector is discarded.
                return sample();
            }

            final double f = 1 / Math.sqrt(sum);
            for (int i = 0; i < dimension; i++) {
                v[i] *= f;
            }

            return v;
        }
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
     * Generation of uniform samples from a 2D unit circle.
     *
     * @param bh Data sink
     * @param data Input data.
     */
    @Benchmark
    public void create2D(Blackhole bh, Sampler2D data) {
        runSampler(bh, data);
    }

    /**
     * Generation of uniform samples from a 3D unit sphere.
     *
     * @param bh Data sink
     * @param data Input data.
     */
    @Benchmark
    public void create3D(Blackhole bh, Sampler3D data) {
        runSampler(bh, data);
    }

    /**
     * Generation of uniform samples from a 4D unit sphere.
     *
     * @param bh Data sink
     * @param data Input data.
     */
    @Benchmark
    public void create4D(Blackhole bh, Sampler4D data) {
        runSampler(bh, data);
    }
}
