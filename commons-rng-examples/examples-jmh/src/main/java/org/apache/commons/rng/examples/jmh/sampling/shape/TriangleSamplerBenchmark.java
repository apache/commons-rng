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
import org.apache.commons.rng.sampling.UnitSphereSampler;
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
import org.openjdk.jmh.infra.Blackhole;
import java.util.concurrent.TimeUnit;

/**
 * Executes benchmark to compare the speed of generating samples within an N-dimension triangle.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@State(Scope.Benchmark)
@Fork(value = 1, jvmArgs = { "-server", "-Xms512M", "-Xmx512M" })
public class TriangleSamplerBenchmark {
    /** Name for the baseline method. */
    private static final String BASELINE = "Baseline";
    /** Name for the baseline method including a 50:50 if statement. */
    private static final String BASELINE_IF = "BaselineIf";
    /** Name for the method using vectors. */
    private static final String VECTORS = "Vectors";
    /** Name for the method using the coordinates. */
    private static final String COORDINATES = "Coordinates";
    /** Error message for an unknown sampler type. */
    private static final String UNKNOWN_SAMPLER = "Unknown sampler type: ";

    /**
     * Base class for a sampler using vectors: {@code p = a + sv + tw}.
     */
    private abstract static class VectorTriangleSampler implements ObjectSampler<double[]> {
        /** The source of randomness. */
        private final UniformRandomProvider rng;

        /**
         * Create an instance.
         *
         * @param rng the source of randomness
         */
        VectorTriangleSampler(UniformRandomProvider rng) {
            this.rng = rng;
        }

        /**
         * @return a random Cartesian point inside the triangle.
         */
        @Override
        public double[] sample() {
            final double s = rng.nextDouble();
            final double t = rng.nextDouble();
            if (s + t > 1) {
                return createSample(1.0 - s, 1.0 - t);
            }
            return createSample(s, t);
        }

        /**
         * Creates the sample given the random variates {@code s} and {@code t} in the
         * interval {@code [0, 1]} and {@code s + t <= 1}.
         * The sample can be obtained from the triangle abc with v = b-a and w = c-a using:
         * <pre>
         * p = a + sv + tw
         * </pre>
         *
         * @param s the first variate s
         * @param t the second variate t
         * @return the sample
         */
        protected abstract double[] createSample(double s, double t);
    }

    /**
     * Base class for a sampler using coordinates: {@code a(1 - s - t) + sb + tc}.
     */
    private abstract static class CoordinateTriangleSampler implements ObjectSampler<double[]> {
        /** The source of randomness. */
        private final UniformRandomProvider rng;

        /**
         * Create an instance.
         *
         * @param rng the source of randomness
         */
        CoordinateTriangleSampler(UniformRandomProvider rng) {
            this.rng = rng;
        }

        /**
         * @return a random Cartesian point inside the triangle.
         */
        @Override
        public double[] sample() {
            final double s = rng.nextDouble();
            final double t = rng.nextDouble();
            final double spt = s + t;
            if (spt > 1) {
                // Transform: s1 = 1 - s; t1 = 1 - t.
                // Compute: 1 - s1 - t1
                // Do not assume (1 - (1-s) - (1-t)) is (s + t - 1), i.e. (spt - 1.0),
                // to avoid loss of a random bit due to rounding when s + t > 1.
                // An exact sum is (s - 1 + t).
                return createSample(s - 1.0 + t, 1.0 - s, 1.0 - t);
            }
            // Here s + t is exact so can be subtracted to make 1 - s - t
            return createSample(1.0 - spt, s, t);
        }

        /**
         * Creates the sample given the random variates {@code s} and {@code t} in the
         * interval {@code [0, 1]} and {@code s + t <= 1}. The sum {@code 1 - s - t} is provided.
         * The sample can be obtained from the triangle abc using:
         * <pre>
         * p = a(1 - s - t) + sb + tc
         * </pre>
         *
         * @param p1msmt plus 1 minus s minus t (1 - s - t)
         * @param s the first variate s
         * @param t the second variate t
         * @return the sample
         */
        protected abstract double[] createSample(double p1msmt, double s, double t);
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
        @Param({"1000"})
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
         * Create the source of randomness and the sampler.
         */
        @Setup(Level.Iteration)
        public void setup() {
            // This could be configured using @Param
            final UniformRandomProvider rng = RandomSource.XO_RO_SHI_RO_128_PP.create();
            final int dimension = getDimension();
            final UnitSphereSampler s = UnitSphereSampler.of(rng, dimension);
            final double[] a = s.sample();
            final double[] b = s.sample();
            final double[] c = s.sample();
            sampler = createSampler(rng, a, b, c);
        }

        /**
         * Gets the dimension of the triangle vertices.
         *
         * @return the dimension
         */
        protected abstract int getDimension();

        /**
         * Creates the triangle sampler.
         *
         * @param rng the source of randomness
         * @param a The first vertex.
         * @param b The second vertex.
         * @param c The third vertex.
         * @return the sampler
         */
        protected abstract ObjectSampler<double[]> createSampler(UniformRandomProvider rng,
                                                                 double[] a, double[] b, double[] c);
    }

    /**
     * The 2D triangle sampler.
     */
    @State(Scope.Benchmark)
    public static class Sampler2D extends SamplerData {
        /** The sampler type. */
        @Param({BASELINE, BASELINE_IF, VECTORS, COORDINATES})
        private String type;

        /** {@inheritDoc} */
        @Override
        protected int getDimension() {
            return 2;
        }

        /** {@inheritDoc} */
        @Override
        protected ObjectSampler<double[]> createSampler(final UniformRandomProvider rng,
                                                        final double[] a, final double[] b, final double[] c) {
            if (BASELINE.equals(type)) {
                return () -> {
                    final double s = rng.nextDouble();
                    final double t = rng.nextDouble();
                    return new double[] {s, t};
                };
            } else if (BASELINE_IF.equals(type)) {
                return () -> {
                    final double s = rng.nextDouble();
                    final double t = rng.nextDouble();
                    if (s + t > 1) {
                        return new double[] {s, t};
                    }
                    return new double[] {t, s};
                };
            } else if (VECTORS.equals(type)) {
                return new VectorTriangleSampler2D(rng, a, b, c);
            } else if (COORDINATES.equals(type)) {
                return new CoordinateTriangleSampler2D(rng, a, b, c);
            }
            throw new IllegalStateException(UNKNOWN_SAMPLER + type);
        }

        /**
         * Sample vectors in 2D.
         */
        private static class VectorTriangleSampler2D extends VectorTriangleSampler {
            // CHECKSTYLE: stop JavadocVariableCheck
            private final double ax;
            private final double ay;
            private final double vx;
            private final double vy;
            private final double wx;
            private final double wy;
            // CHECKSTYLE: resume JavadocVariableCheck

            /**
             * @param rng the source of randomness
             * @param a The first vertex.
             * @param b The second vertex.
             * @param c The third vertex.
             */
            VectorTriangleSampler2D(UniformRandomProvider rng, double[] a, double[] b, double[] c) {
                super(rng);
                ax = a[0];
                ay = a[1];
                vx = b[0] - ax;
                vy = b[1] - ay;
                wx = c[0] - ax;
                wy = c[1] - ay;
            }

            @Override
            protected double[] createSample(double s, double t) {
                return new double[] {ax + s * vx + t * wx,
                                     ay + s * vy + t * wy};
            }
        }

        /**
         * Sample using coordinates in 2D.
         */
        private static class CoordinateTriangleSampler2D extends CoordinateTriangleSampler {
            // CHECKSTYLE: stop JavadocVariableCheck
            private final double ax;
            private final double ay;
            private final double bx;
            private final double by;
            private final double cx;
            private final double cy;
            // CHECKSTYLE: resume JavadocVariableCheck

            /**
             * @param rng the source of randomness
             * @param a The first vertex.
             * @param b The second vertex.
             * @param c The third vertex.
             */
            CoordinateTriangleSampler2D(UniformRandomProvider rng, double[] a, double[] b, double[] c) {
                super(rng);
                ax = a[0];
                ay = a[1];
                bx = b[0];
                by = b[1];
                cx = c[0];
                cy = c[1];
            }

            @Override
            protected double[] createSample(double p1msmt, double s, double t) {
                return new double[] {p1msmt * ax + s * bx + t * cx,
                                     p1msmt * ay + s * by + t * cy};
            }
        }    }

    /**
     * The 3D triangle sampler.
     */
    @State(Scope.Benchmark)
    public static class Sampler3D extends SamplerData {
        /** The sampler type. */
        @Param({BASELINE, BASELINE_IF, VECTORS, COORDINATES})
        private String type;

        /** {@inheritDoc} */
        @Override
        protected int getDimension() {
            return 3;
        }

        /** {@inheritDoc} */
        @Override
        protected ObjectSampler<double[]> createSampler(final UniformRandomProvider rng,
                                                        final double[] a, final double[] b, final double[] c) {
            if (BASELINE.equals(type)) {
                return () -> {
                    final double s = rng.nextDouble();
                    final double t = rng.nextDouble();
                    return new double[] {s, t, s};
                };
            } else if (BASELINE_IF.equals(type)) {
                return () -> {
                    final double s = rng.nextDouble();
                    final double t = rng.nextDouble();
                    if (s + t > 1) {
                        return new double[] {s, t, s};
                    }
                    return new double[] {t, s, t};
                };
            } else if (VECTORS.equals(type)) {
                return new VectorTriangleSampler3D(rng, a, b, c);
            } else if (COORDINATES.equals(type)) {
                return new CoordinateTriangleSampler3D(rng, a, b, c);
            }
            throw new IllegalStateException(UNKNOWN_SAMPLER + type);
        }

        /**
         * Sample vectors in 3D.
         */
        private static class VectorTriangleSampler3D extends VectorTriangleSampler {
            // CHECKSTYLE: stop JavadocVariableCheck
            private final double ax;
            private final double ay;
            private final double az;
            private final double vx;
            private final double vy;
            private final double vz;
            private final double wx;
            private final double wy;
            private final double wz;
            // CHECKSTYLE: resume JavadocVariableCheck

            /**
             * @param rng the source of randomness
             * @param a The first vertex.
             * @param b The second vertex.
             * @param c The third vertex.
             */
            VectorTriangleSampler3D(UniformRandomProvider rng, double[] a, double[] b, double[] c) {
                super(rng);
                ax = a[0];
                ay = a[1];
                az = a[2];
                vx = b[0] - ax;
                vy = b[1] - ay;
                vz = b[2] - az;
                wx = c[0] - ax;
                wy = c[1] - ay;
                wz = c[2] - az;
            }

            @Override
            protected double[] createSample(double s, double t) {
                return new double[] {ax + s * vx + t * wx,
                                     ay + s * vy + t * wy,
                                     az + s * vz + t * wz};
            }
        }

        /**
         * Sample using coordinates in 3D.
         */
        private static class CoordinateTriangleSampler3D extends CoordinateTriangleSampler {
            // CHECKSTYLE: stop JavadocVariableCheck
            private final double ax;
            private final double ay;
            private final double az;
            private final double bx;
            private final double by;
            private final double bz;
            private final double cx;
            private final double cy;
            private final double cz;
            // CHECKSTYLE: resume JavadocVariableCheck

            /**
             * @param rng the source of randomness
             * @param a The first vertex.
             * @param b The second vertex.
             * @param c The third vertex.
             */
            CoordinateTriangleSampler3D(UniformRandomProvider rng, double[] a, double[] b, double[] c) {
                super(rng);
                ax = a[0];
                ay = a[1];
                az = a[2];
                bx = b[0];
                by = b[1];
                bz = b[2];
                cx = c[0];
                cy = c[1];
                cz = c[2];
            }

            @Override
            protected double[] createSample(double p1msmt, double s, double t) {
                return new double[] {p1msmt * ax + s * bx + t * cx,
                                     p1msmt * ay + s * by + t * cy,
                                     p1msmt * az + s * bz + t * cz};
            }
        }
    }

    /**
     * The ND triangle sampler.
     */
    @State(Scope.Benchmark)
    public static class SamplerND extends SamplerData {
        /** The number of dimensions. */
        @Param({"2", "3", "4", "8", "16", "32"})
        private int dimension;
        /** The sampler type. */
        @Param({BASELINE, BASELINE_IF, VECTORS, COORDINATES})
        private String type;

        /** {@inheritDoc} */
        @Override
        protected int getDimension() {
            return dimension;
        }

        /** {@inheritDoc} */
        @Override
        protected ObjectSampler<double[]> createSampler(final UniformRandomProvider rng,
                                                        final double[] a, final double[] b, final double[] c) {
            if (BASELINE.equals(type)) {
                return () -> {
                    double s = rng.nextDouble();
                    double t = rng.nextDouble();
                    final double[] x = new double[a.length];
                    for (int i = 0; i < x.length; i++) {
                        x[i] = s;
                        s = t;
                        t = x[i];
                    }
                    return x;
                };
            } else if (BASELINE_IF.equals(type)) {
                return () -> {
                    double s = rng.nextDouble();
                    double t = rng.nextDouble();
                    final double[] x = new double[a.length];
                    if (s + t > 1) {
                        for (int i = 0; i < x.length; i++) {
                            x[i] = t;
                            t = s;
                            s = x[i];
                        }
                        return x;
                    }
                    for (int i = 0; i < x.length; i++) {
                        x[i] = s;
                        s = t;
                        t = x[i];
                    }
                    return x;
                };
            } else if (VECTORS.equals(type)) {
                return new VectorTriangleSamplerND(rng, a, b, c);
            } else if (COORDINATES.equals(type)) {
                return new CoordinateTriangleSamplerND(rng, a, b, c);
            }
            throw new IllegalStateException(UNKNOWN_SAMPLER + type);
        }

        /**
         * Sample vectors in ND.
         */
        private static class VectorTriangleSamplerND extends VectorTriangleSampler {
            // CHECKSTYLE: stop JavadocVariableCheck
            private final double[] a;
            private final double[] v;
            private final double[] w;
            // CHECKSTYLE: resume JavadocVariableCheck

            /**
             * @param rng the source of randomness
             * @param a The first vertex.
             * @param b The second vertex.
             * @param c The third vertex.
             */
            VectorTriangleSamplerND(UniformRandomProvider rng, double[] a, double[] b, double[] c) {
                super(rng);
                this.a = a.clone();
                v = new double[a.length];
                w = new double[a.length];
                for (int i = 0; i < a.length; i++) {
                    v[i] = b[i] - a[i];
                    w[i] = c[i] - a[i];
                }
            }

            @Override
            protected double[] createSample(double s, double t) {
                final double[] x = new double[a.length];
                for (int i = 0; i < x.length; i++) {
                    x[i] = a[i] + s * v[i] + t * w[i];
                }
                return x;
            }
        }

        /**
         * Sample using coordinates in ND.
         */
        private static class CoordinateTriangleSamplerND extends CoordinateTriangleSampler {
            // CHECKSTYLE: stop JavadocVariableCheck
            private final double[] a;
            private final double[] b;
            private final double[] c;
            // CHECKSTYLE: resume JavadocVariableCheck

            /**
             * @param rng the source of randomness
             * @param a The first vertex.
             * @param b The second vertex.
             * @param c The third vertex.
             */
            CoordinateTriangleSamplerND(UniformRandomProvider rng, double[] a, double[] b, double[] c) {
                super(rng);
                this.a = a.clone();
                this.b = b.clone();
                this.c = c.clone();
            }

            @Override
            protected double[] createSample(double p1msmt, double s, double t) {
                final double[] x = new double[a.length];
                for (int i = 0; i < x.length; i++) {
                    x[i] = p1msmt * a[i] + s * b[i] + t * c[i];
                }
                return x;
            }
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
     * Generation of uniform samples from a 2D triangle.
     *
     * @param bh Data sink
     * @param data Input data.
     */
    @Benchmark
    public void create2D(Blackhole bh, Sampler2D data) {
        runSampler(bh, data);
    }

    /**
     * Generation of uniform samples from a 3D triangle.
     *
     * @param bh Data sink
     * @param data Input data.
     */
    @Benchmark
    public void create3D(Blackhole bh, Sampler3D data) {
        runSampler(bh, data);
    }

    /**
     * Generation of uniform samples from an ND triangle.
     *
     * @param bh Data sink
     * @param data Input data.
     */
    @Benchmark
    public void createND(Blackhole bh, SamplerND data) {
        runSampler(bh, data);
    }
}
