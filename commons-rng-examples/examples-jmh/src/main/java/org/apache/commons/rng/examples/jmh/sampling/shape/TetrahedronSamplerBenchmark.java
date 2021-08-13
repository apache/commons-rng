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
 * Executes benchmark to compare the speed of generating samples within a tetrahedron.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@State(Scope.Benchmark)
@Fork(value = 1, jvmArgs = { "-server", "-Xms512M", "-Xmx512M" })
public class TetrahedronSamplerBenchmark {
    /** Name for the baseline method. */
    private static final String BASELINE = "Baseline";
    /** Name for the method using array coordinates. */
    private static final String ARRAY = "Array";
    /** Name for the method using non-array (primitive) coordinates. */
    private static final String NON_ARRAY = "NonArray";
    /** Name for the method using array coordinates and inline sample algorithm. */
    private static final String ARRAY_INLINE = "ArrayInline";
    /** Name for the method using non-array (primitive) coordinates and inline sample algorithm. */
    private static final String NON_ARRAY_INLINE = "NonArrayInline";
    /** Error message for an unknown sampler type. */
    private static final String UNKNOWN_SAMPLER = "Unknown sampler type: ";

    /**
     * The base class for sampling from a tetrahedron.
     *
     * <ul>
     *  <li>
     *   Uses the algorithm described in:
     *   <blockquote>
     *    Rocchini, C. and Cignoni, P. (2001)<br>
     *    <i>Generating Random Points in a Tetrahedron</i>.<br>
     *    <strong>Journal of Graphics Tools</strong> 5(4), pp. 9-12.
     *   </blockquote>
     *  </li>
     * </ul>
     *
     * @see <a href="https://doi.org/10.1080/10867651.2000.10487528">
     *   Rocchini, C. &amp; Cignoni, P. (2001) Journal of Graphics Tools 5, pp. 9-12</a>
     */
    private abstract static class TetrahedronSampler implements ObjectSampler<double[]> {
        /** The source of randomness. */
        private final UniformRandomProvider rng;

        /**
         * @param rng Source of randomness.
         */
        TetrahedronSampler(UniformRandomProvider rng) {
            this.rng = rng;
        }

        @Override
        public double[] sample() {
            double s = rng.nextDouble();
            double t = rng.nextDouble();
            final double u = rng.nextDouble();
            // Care is taken to ensure the 3 deviates remain in the 2^53 dyadic rationals in [0, 1).
            // The following are exact for all the 2^53 dyadic rationals:
            // 1 - u; u in [0, 1]
            // u - 1; u in [0, 1]
            // u + 1; u in [-1, 0]
            // u + v; u in [-1, 0], v in [0, 1]
            // u + v; u, v in [0, 1], u + v <= 1

            // Cut and fold with the plane s + t = 1
            if (s + t > 1) {
                // (s, t, u) = (1 - s, 1 - t, u)                if s + t > 1
                s = 1 - s;
                t = 1 - t;
            }
            // Now s + t <= 1.
            // Cut and fold with the planes t + u = 1 and s + t + u = 1.
            final double tpu = t + u;
            final double sptpu = s + tpu;
            if (sptpu > 1) {
                if (tpu > 1) {
                    // (s, t, u) = (s, 1 - u, 1 - s - t)        if t + u > 1
                    // 1 - s - (1-u) - (1-s-t) == u - 1 + t
                    return createSample(u - 1 + t, s, 1 - u, 1 - s - t);
                }
                // (s, t, u) = (1 - t - u, t, s + t + u - 1)    if t + u <= 1
                // 1 - (1-t-u) - t - (s+t+u-1) == 1 - s - t
                return createSample(1 - s - t, 1 - tpu, t, s - 1 + tpu);
            }
            return createSample(1 - sptpu, s, t, u);
        }

        /**
         * Creates the sample given the random variates {@code s}, {@code t} and {@code u} in the
         * interval {@code [0, 1]} and {@code s + t + u <= 1}. The sum {@code 1 - s - t - u} is
         * provided. The sample can be obtained from the tetrahedron {@code abcd} using:
         *
         * <pre>
         * p = (1 - s - t - u)a + sb + tc + ud
         * </pre>
         *
         * @param p1msmtmu plus 1 minus s minus t minus u (1 - s - t - u)
         * @param s the first variate s
         * @param t the second variate t
         * @param u the third variate u
         * @return the sample
         */
        protected abstract double[] createSample(double p1msmtmu, double s, double t, double u);
    }

    /**
     * Sample from a tetrahedron using array coordinates.
     */
    private static class ArrayTetrahedronSampler extends TetrahedronSampler {
        // CHECKSTYLE: stop JavadocVariableCheck
        private final double[] a;
        private final double[] b;
        private final double[] c;
        private final double[] d;
        // CHECKSTYLE: resume JavadocVariableCheck

        /**
         * @param rng the source of randomness
         * @param a The first vertex.
         * @param b The second vertex.
         * @param c The third vertex.
         * @param d The fourth vertex.
         */
        ArrayTetrahedronSampler(UniformRandomProvider rng,
                                double[] a, double[] b, double[] c, double[] d) {
            super(rng);
            this.a = a.clone();
            this.b = b.clone();
            this.c = c.clone();
            this.d = d.clone();
        }

        @Override
        protected double[] createSample(double p1msmtmu, double s, double t, double u) {
            return new double[] {p1msmtmu * a[0] + s * b[0] + t * c[0] + u * d[0],
                                 p1msmtmu * a[1] + s * b[1] + t * c[1] + u * d[1],
                                 p1msmtmu * a[2] + s * b[2] + t * c[2] + u * d[2]};
        }
    }

    /**
     * Sample from a tetrahedron using non-array coordinates.
     */
    private static class NonArrayTetrahedronSampler extends TetrahedronSampler {
        // CHECKSTYLE: stop JavadocVariableCheck
        private final double ax;
        private final double bx;
        private final double cx;
        private final double dx;
        private final double ay;
        private final double by;
        private final double cy;
        private final double dy;
        private final double az;
        private final double bz;
        private final double cz;
        private final double dz;
        // CHECKSTYLE: resume JavadocVariableCheck

        /**
         * @param rng the source of randomness
         * @param a The first vertex.
         * @param b The second vertex.
         * @param c The third vertex.
         * @param d The fourth vertex.
         */
        NonArrayTetrahedronSampler(UniformRandomProvider rng,
                                   double[] a, double[] b, double[] c, double[] d) {
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
            dx = d[0];
            dy = d[1];
            dz = d[2];
        }

        @Override
        protected double[] createSample(double p1msmtmu, double s, double t, double u) {
            return new double[] {p1msmtmu * ax + s * bx + t * cx + u * dx,
                                 p1msmtmu * ay + s * by + t * cy + u * dy,
                                 p1msmtmu * az + s * bz + t * cz + u * dz};
        }
    }

    /**
     * Sample from a tetrahedron using array coordinates with an inline sample algorithm
     * in-place of a method call.
     */
    private static class ArrayInlineTetrahedronSampler implements ObjectSampler<double[]> {
        // CHECKSTYLE: stop JavadocVariableCheck
        private final double[] a;
        private final double[] b;
        private final double[] c;
        private final double[] d;
        private final UniformRandomProvider rng;
        // CHECKSTYLE: resume JavadocVariableCheck

        /**
         * @param rng the source of randomness
         * @param a The first vertex.
         * @param b The second vertex.
         * @param c The third vertex.
         * @param d The fourth vertex.
         */
        ArrayInlineTetrahedronSampler(UniformRandomProvider rng,
                                      double[] a, double[] b, double[] c, double[] d) {
            this.a = a.clone();
            this.b = b.clone();
            this.c = c.clone();
            this.d = d.clone();
            this.rng = rng;
        }

        @Override
        public double[] sample() {
            double s = rng.nextDouble();
            double t = rng.nextDouble();
            double u = rng.nextDouble();

            if (s + t > 1) {
                // (s, t, u) = (1 - s, 1 - t, u)                    if s + t > 1
                s = 1 - s;
                t = 1 - t;
            }

            double p1msmtmu;
            final double tpu = t + u;
            final double sptpu = s + tpu;
            if (sptpu > 1) {
                if (tpu > 1) {
                    // (s, t, u) = (s, 1 - u, 1 - s - t)            if t + u > 1
                    final double tt = t;
                    p1msmtmu = u - 1 + t;
                    t = 1 - u;
                    u = 1 - s - tt;
                } else {
                    // (s, t, u) = (1 - t - u, t, s + t + u - 1)    if t + u <= 1
                    final double ss = s;
                    p1msmtmu = 1 - s - t;
                    s = 1 - tpu;
                    u = ss - 1 + tpu;
                }
            } else {
                p1msmtmu = 1 - sptpu;
            }

            return new double[] {p1msmtmu * a[0] + s * b[0] + t * c[0] + u * d[0],
                                 p1msmtmu * a[1] + s * b[1] + t * c[1] + u * d[1],
                                 p1msmtmu * a[2] + s * b[2] + t * c[2] + u * d[2]};
        }
    }

    /**
     * Sample from a tetrahedron using non-array coordinates with an inline sample algorithm
     * in-place of a method call.
     */
    private static class NonArrayInlineTetrahedronSampler implements ObjectSampler<double[]> {
        // CHECKSTYLE: stop JavadocVariableCheck
        private final double ax;
        private final double bx;
        private final double cx;
        private final double dx;
        private final double ay;
        private final double by;
        private final double cy;
        private final double dy;
        private final double az;
        private final double bz;
        private final double cz;
        private final double dz;
        private final UniformRandomProvider rng;
        // CHECKSTYLE: resume JavadocVariableCheck

        /**
         * @param rng the source of randomness
         * @param a The first vertex.
         * @param b The second vertex.
         * @param c The third vertex.
         * @param d The fourth vertex.
         */
        NonArrayInlineTetrahedronSampler(UniformRandomProvider rng,
                                         double[] a, double[] b, double[] c, double[] d) {
            ax = a[0];
            ay = a[1];
            az = a[2];
            bx = b[0];
            by = b[1];
            bz = b[2];
            cx = c[0];
            cy = c[1];
            cz = c[2];
            dx = d[0];
            dy = d[1];
            dz = d[2];
            this.rng = rng;
        }

        @Override
        public double[] sample() {
            double s = rng.nextDouble();
            double t = rng.nextDouble();
            double u = rng.nextDouble();

            if (s + t > 1) {
                // (s, t, u) = (1 - s, 1 - t, u)                    if s + t > 1
                s = 1 - s;
                t = 1 - t;
            }

            double p1msmtmu;
            final double tpu = t + u;
            final double sptpu = s + tpu;
            if (sptpu > 1) {
                if (tpu > 1) {
                    // (s, t, u) = (s, 1 - u, 1 - s - t)            if t + u > 1
                    final double tt = t;
                    p1msmtmu = u - 1 + t;
                    t = 1 - u;
                    u = 1 - s - tt;
                } else {
                    // (s, t, u) = (1 - t - u, t, s + t + u - 1)    if t + u <= 1
                    final double ss = s;
                    p1msmtmu = 1 - s - t;
                    s = 1 - tpu;
                    u = ss - 1 + tpu;
                }
            } else {
                p1msmtmu = 1 - sptpu;
            }

            return new double[] {p1msmtmu * ax + s * bx + t * cx + u * dx,
                                 p1msmtmu * ay + s * by + t * cy + u * dy,
                                 p1msmtmu * az + s * bz + t * cz + u * dz};
        }
    }

    /**
     * Contains the sampler and the number of samples.
     */
    @State(Scope.Benchmark)
    public static class SamplerData {
        /** The sampler. */
        private ObjectSampler<double[]> sampler;

        /** The number of samples. */
        @Param({"1", "10", "100", "1000", "10000"})
        private int size;

        /** The sampler type. */
        @Param({BASELINE, ARRAY, NON_ARRAY, ARRAY_INLINE, NON_ARRAY_INLINE})
        private String type;

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
            final UniformRandomProvider rng = RandomSource.XO_SHI_RO_256_PP.create();
            final UnitSphereSampler s = UnitSphereSampler.of(rng, 3);
            final double[] a = s.sample();
            final double[] b = s.sample();
            final double[] c = s.sample();
            final double[] d = s.sample();
            sampler = createSampler(rng, a, b, c, d);
        }

        /**
         * Creates the tetrahedron sampler.
         *
         * @param a The first vertex.
         * @param b The second vertex.
         * @param c The third vertex.
         * @param d The fourth vertex.
         * @param rng the source of randomness
         * @return the sampler
         */
        private ObjectSampler<double[]> createSampler(final UniformRandomProvider rng,
                                                      double[] a, double[] b, double[] c, double[] d) {
            if (BASELINE.equals(type)) {
                return () -> {
                    final double s = rng.nextDouble();
                    final double t = rng.nextDouble();
                    final double u = rng.nextDouble();
                    return new double[] {s, t, u};
                };
            } else if (ARRAY.equals(type)) {
                return new ArrayTetrahedronSampler(rng, a, b, c, d);
            } else if (NON_ARRAY.equals(type)) {
                return new NonArrayTetrahedronSampler(rng, a, b, c, d);
            } else if (ARRAY_INLINE.equals(type)) {
                return new ArrayInlineTetrahedronSampler(rng, a, b, c, d);
            } else if (NON_ARRAY_INLINE.equals(type)) {
                return new NonArrayInlineTetrahedronSampler(rng, a, b, c, d);
            }
            throw new IllegalStateException(UNKNOWN_SAMPLER + type);
        }
    }

    /**
     * Run the sampler for the configured number of samples.
     *
     * @param bh Data sink
     * @param data Input data.
     */
    @Benchmark
    public void sample(Blackhole bh, SamplerData data) {
        final ObjectSampler<double[]> sampler = data.getSampler();
        for (int i = data.getSize() - 1; i >= 0; i--) {
            bh.consume(sampler.sample());
        }
    }
}
