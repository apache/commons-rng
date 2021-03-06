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

package org.apache.commons.rng.examples.jmh.sampling.distribution;

import org.apache.commons.rng.UniformRandomProvider;
import org.apache.commons.rng.sampling.distribution.ContinuousSampler;
import org.apache.commons.rng.sampling.distribution.DiscreteSampler;
import org.apache.commons.rng.sampling.distribution.ZigguratSampler;
import org.apache.commons.rng.sampling.distribution.ZigguratNormalizedGaussianSampler;
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
 * Executes a benchmark to compare the speed of generation of random numbers
 * using variations of the ziggurat method.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@State(Scope.Benchmark)
@Fork(value = 1, jvmArgs = {"-server", "-Xms128M", "-Xmx128M"})
public class ZigguratSamplerPerformance {
    /** The name for the {@link ZigguratNormalizedGaussianSampler}. */
    private static final String GAUSSIAN_128 = "Gaussian128";
    /** The name for a copy of the {@link ZigguratNormalizedGaussianSampler} with a table of size 256. */
    private static final String GAUSSIAN_256 = "Gaussian256";
    /** The name for the {@link ZigguratSampler.NormalizedGaussian}. */
    private static final String MOD_GAUSSIAN = "ModGaussian";

    /**
     * The value.
     *
     * <p>This must NOT be final!</p>
     */
    private double value;

    /**
     * Defines method to use for creating {@code int} index values from a random long.
     */
    @State(Scope.Benchmark)
    public static class IndexSources {
        /** The method to obtain the index. */
        @Param({"CastMask", "MaskCast"})
        private String method;

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
            // Use a fast generator
            final UniformRandomProvider rng = RandomSource.XO_RO_SHI_RO_128_PP.create();
            if ("CastMask".equals(method)) {
                sampler = new DiscreteSampler() {
                    @Override
                    public int sample() {
                        final long x = rng.nextLong();
                        return ((int) x) & 0xff;
                    }
                };
            } else if ("MaskCast".equals(method)) {
                sampler = new DiscreteSampler() {
                    @Override
                    public int sample() {
                        final long x = rng.nextLong();
                        return (int) (x & 0xff);
                    }
                };
            } else {
                throwIllegalStateException(method);
            }
        }
    }

    /**
     * Sampler that generates values of type {@code long}.
     */
    interface LongSampler {
        /**
         * @return a sample.
         */
        long sample();
    }

    /**
     * Defines method to use for creating unsigned {@code long} values.
     */
    @State(Scope.Benchmark)
    public static class LongSources {
        /** The method to obtain the long. */
        @Param({"Mask", "Shift"})
        private String method;

        /** The sampler. */
        private LongSampler sampler;

        /**
         * @return the sampler.
         */
        public LongSampler getSampler() {
            return sampler;
        }

        /** Instantiates sampler. */
        @Setup
        public void setup() {
            // Use a fast generator
            final UniformRandomProvider rng = RandomSource.XO_RO_SHI_RO_128_PP.create();
            if ("Mask".equals(method)) {
                sampler = new LongSampler() {
                    @Override
                    public long sample() {
                        return rng.nextLong() & Long.MAX_VALUE;
                    }
                };
            } else if ("Shift".equals(method)) {
                sampler = new LongSampler() {
                    @Override
                    public long sample() {
                        return rng.nextLong() >>> 1;
                    }
                };
            } else {
                throwIllegalStateException(method);
            }
        }
    }

    /**
     * The samplers to use for testing the ziggurat method.
     * Defines the RandomSource and the sampler type.
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
        @Param({"XO_RO_SHI_RO_128_PP",
                "MWC_256",
                "JDK"})
        private String randomSourceName;

        /**
         * The sampler type.
         */
        @Param({GAUSSIAN_128, GAUSSIAN_256, "Exponential", MOD_GAUSSIAN, "ModExponential",
                "ModGaussian2", "ModExponential2"})
        private String type;

        /** The sampler. */
        private ContinuousSampler sampler;

        /**
         * @return the sampler.
         */
        public ContinuousSampler getSampler() {
            return sampler;
        }

        /** Instantiates sampler. */
        @Setup
        public void setup() {
            final RandomSource randomSource = RandomSource.valueOf(randomSourceName);
            final UniformRandomProvider rng = randomSource.create();
            if (GAUSSIAN_128.equals(type)) {
                sampler = new ZigguratNormalizedGaussianSampler128(rng);
            } else if (GAUSSIAN_256.equals(type)) {
                sampler = ZigguratNormalizedGaussianSampler.of(rng);
            } else if ("Exponential".equals(type)) {
                sampler = new ZigguratExponentialSampler(rng);
            } else if (MOD_GAUSSIAN.equals(type)) {
                sampler = ZigguratSampler.NormalizedGaussian.of(rng);
            } else if ("ModExponential".equals(type)) {
                sampler = ZigguratSampler.Exponential.of(rng);
            } else if ("ModGaussian2".equals(type)) {
                sampler = new ModifiedZigguratNormalizedGaussianSampler(rng);
            } else if ("ModExponential2".equals(type)) {
                sampler = new ModifiedZigguratExponentialSampler(rng);
            } else {
                throwIllegalStateException(type);
            }
        }
    }

    /**
     * The samplers to use for testing the ziggurat method with sequential sample generation.
     * Defines the RandomSource and the sampler type.
     *
     * <p>This specifically targets the Gaussian sampler. The modified ziggurat sampler
     * for the exponential distribution is always faster than the standard zigurat sampler.
     * The modified ziggurat sampler is faster on single samples than the standard sampler
     * but on repeat calls to generate multiple deviates the standard sampler can be faster
     * depending on the JDK (modern JDKs are faster with the 'old' sampler).
     */
    @State(Scope.Benchmark)
    public static class SequentialSources {
        /**
         * RNG providers.
         *
         * <p>Use different speeds.</p>
         *
         * @see <a href="https://commons.apache.org/proper/commons-rng/userguide/rng.html">
         *      Commons RNG user guide</a>
         */
        @Param({"XO_RO_SHI_RO_128_PP",
                //"MWC_256",
                //"JDK"
                })
        private String randomSourceName;

        /** The sampler type. */
        @Param({GAUSSIAN_128, GAUSSIAN_256, MOD_GAUSSIAN})
        private String type;

        /** The size. */
        @Param({"1", "2", "3", "4", "5", "10", "20", "40"})
        private int size;

        /** The sampler. */
        private ContinuousSampler sampler;

        /**
         * @return the sampler.
         */
        public ContinuousSampler getSampler() {
            return sampler;
        }

        /** Instantiates sampler. */
        @Setup
        public void setup() {
            final RandomSource randomSource = RandomSource.valueOf(randomSourceName);
            final UniformRandomProvider rng = randomSource.create();
            ContinuousSampler s = null;
            if (GAUSSIAN_128.equals(type)) {
                s = new ZigguratNormalizedGaussianSampler128(rng);
            } else if (GAUSSIAN_256.equals(type)) {
                s = ZigguratNormalizedGaussianSampler.of(rng);
            } else if (MOD_GAUSSIAN.equals(type)) {
                s = ZigguratSampler.NormalizedGaussian.of(rng);
            } else {
                throwIllegalStateException(type);
            }
            sampler = createSampler(size, s);
        }

        /**
         * Creates the sampler for the specified number of samples.
         *
         * @param size the size
         * @param s the sampler to create the samples
         * @return the sampler
         */
        private static ContinuousSampler createSampler(int size, ContinuousSampler s) {
            // Create size samples
            switch (size) {
            case 1:
                return new Size1Sampler(s);
            case 2:
                return new Size2Sampler(s);
            case 3:
                return new Size3Sampler(s);
            case 4:
                return new Size4Sampler(s);
            case 5:
                return new Size5Sampler(s);
            default:
                return new SizeNSampler(s, size);
            }
        }

        /**
         * Create a specified number of samples from an underlying sampler.
         */
        abstract static class SizeSampler implements ContinuousSampler {
            /** The sampler. */
            protected ContinuousSampler delegate;

            /**
             * @param delegate the sampler to create the samples
             */
            SizeSampler(ContinuousSampler delegate) {
                this.delegate = delegate;
            }
        }

        /**
         * Create 1 sample from the sampler.
         */
        static class Size1Sampler extends SizeSampler {
            /**
             * @param delegate the sampler to create the samples
             */
            Size1Sampler(ContinuousSampler delegate) {
                super(delegate);
            }

            @Override
            public double sample() {
                return delegate.sample();
            }
        }

        /**
         * Create 2 samples from the sampler.
         */
        static class Size2Sampler extends SizeSampler {
            /**
             * @param delegate the sampler to create the samples
             */
            Size2Sampler(ContinuousSampler delegate) {
                super(delegate);
            }

            @Override
            public double sample() {
                delegate.sample();
                return delegate.sample();
            }
        }

        /**
         * Create 3 samples from the sampler.
         */
        static class Size3Sampler extends SizeSampler {
            /**
             * @param delegate the sampler to create the samples
             */
            Size3Sampler(ContinuousSampler delegate) {
                super(delegate);
            }

            @Override
            public double sample() {
                delegate.sample();
                delegate.sample();
                return delegate.sample();
            }
        }

        /**
         * Create 4 samples from the sampler.
         */
        static class Size4Sampler extends SizeSampler {
            /**
             * @param delegate the sampler to create the samples
             */
            Size4Sampler(ContinuousSampler delegate) {
                super(delegate);
            }

            @Override
            public double sample() {
                delegate.sample();
                delegate.sample();
                delegate.sample();
                return delegate.sample();
            }
        }

        /**
         * Create 5 samples from the sampler.
         */
        static class Size5Sampler extends SizeSampler {
            /**
             * @param delegate the sampler to create the samples
             */
            Size5Sampler(ContinuousSampler delegate) {
                super(delegate);
            }

            @Override
            public double sample() {
                delegate.sample();
                delegate.sample();
                delegate.sample();
                return delegate.sample();
            }
        }

        /**
         * Create N samples from the sampler.
         */
        static class SizeNSampler extends SizeSampler {
            /** The number of samples. */
            private final int size;

            /**
             * @param delegate the sampler to create the samples
             * @param size the size
             */
            SizeNSampler(ContinuousSampler delegate, int size) {
                super(delegate);
                if (size < 1) {
                    throw new IllegalArgumentException("Size must be above zero: " + size);
                }
                this.size = size;
            }

            @Override
            public double sample() {
                for (int i = size - 1; i != 0; i--) {
                    delegate.sample();
                }
                return delegate.sample();
            }
        }
    }

    /**
     * <a href="https://en.wikipedia.org/wiki/Ziggurat_algorithm">
     * Marsaglia and Tsang "Ziggurat" method</a> for sampling from a NormalizedGaussian
     * distribution with mean 0 and standard deviation 1.
     *
     * <p>This is a copy of {@link ZigguratNormalizedGaussianSampler} using a table size of 256.
     */
    static class ZigguratNormalizedGaussianSampler128 implements ContinuousSampler {
        /** Start of tail. */
        private static final double R = 3.442619855899;
        /** Inverse of R. */
        private static final double ONE_OVER_R = 1 / R;
        /** Index of last entry in the tables (which have a size that is a power of 2). */
        private static final int LAST = 127;
        /** Auxiliary table. */
        private static final long[] K;
        /** Auxiliary table. */
        private static final double[] W;
        /** Auxiliary table. */
        private static final double[] F;
        /**
         * The multiplier to convert the least significant 53-bits of a {@code long} to a {@code double}.
         * Taken from org.apache.commons.rng.core.util.NumberFactory.
         */
        private static final double DOUBLE_MULTIPLIER = 0x1.0p-53d;

        /** Underlying source of randomness. */
        private final UniformRandomProvider rng;

        static {
            // Filling the tables.
            // Rectangle area.
            final double v = 9.91256303526217e-3;
            // Direction support uses the sign bit so the maximum magnitude from the long is 2^63
            final double max = Math.pow(2, 63);
            final double oneOverMax = 1d / max;

            K = new long[LAST + 1];
            W = new double[LAST + 1];
            F = new double[LAST + 1];

            double d = R;
            double t = d;
            double fd = pdf(d);
            final double q = v / fd;

            K[0] = (long) ((d / q) * max);
            K[1] = 0;

            W[0] = q * oneOverMax;
            W[LAST] = d * oneOverMax;

            F[0] = 1;
            F[LAST] = fd;

            for (int i = LAST - 1; i >= 1; i--) {
                d = Math.sqrt(-2 * Math.log(v / d + fd));
                fd = pdf(d);

                K[i + 1] = (long) ((d / t) * max);
                t = d;

                F[i] = fd;

                W[i] = d * oneOverMax;
            }
        }

        /**
         * @param rng Generator of uniformly distributed random numbers.
         */
        ZigguratNormalizedGaussianSampler128(UniformRandomProvider rng) {
            this.rng = rng;
        }

        /** {@inheritDoc} */
        @Override
        public double sample() {
            final long j = rng.nextLong();
            final int i = ((int) j) & LAST;
            if (Math.abs(j) < K[i]) {
                // This branch is called about 0.972101 times per sample.
                return j * W[i];
            }
            return fix(j, i);
        }

        /**
         * Gets the value from the tail of the distribution.
         *
         * @param hz Start random integer.
         * @param iz Index of cell corresponding to {@code hz}.
         * @return the requested random value.
         */
        private double fix(long hz,
                           int iz) {
            if (iz == 0) {
                // Base strip.
                // This branch is called about 5.7624515E-4 times per sample.
                double y;
                double x;
                do {
                    // Avoid infinity by creating a non-zero double.
                    y = -Math.log(makeNonZeroDouble(rng.nextLong()));
                    x = -Math.log(makeNonZeroDouble(rng.nextLong())) * ONE_OVER_R;
                } while (y + y < x * x);

                final double out = R + x;
                return hz > 0 ? out : -out;
            }
            // Wedge of other strips.
            // This branch is called about 0.027323 times per sample.
            final double x = hz * W[iz];
            if (F[iz] + rng.nextDouble() * (F[iz - 1] - F[iz]) < pdf(x)) {
                // This branch is called about 0.014961 times per sample.
                return x;
            }
            // Try again.
            // This branch is called about 0.012362 times per sample.
            return sample();
        }

        /**
         * Creates a {@code double} in the interval {@code (0, 1]} from a {@code long} value.
         *
         * @param v Number.
         * @return a {@code double} value in the interval {@code (0, 1]}.
         */
        private static double makeNonZeroDouble(long v) {
            // This matches the method in o.a.c.rng.core.util.NumberFactory.makeDouble(long)
            // but shifts the range from [0, 1) to (0, 1].
            return ((v >>> 11) + 1L) * DOUBLE_MULTIPLIER;
        }

        /**
         * Compute the Gaussian probability density function {@code f(x) = e^-0.5x^2}.
         *
         * @param x Argument.
         * @return \( e^{-\frac{x^2}{2}} \)
         */
        private static double pdf(double x) {
            return Math.exp(-0.5 * x * x);
        }
    }

    /**
     * <a href="https://en.wikipedia.org/wiki/Ziggurat_algorithm">
     * Marsaglia and Tsang "Ziggurat" method</a> for sampling from an exponential
     * distribution.
     *
     * <p>The algorithm is explained in this
     * <a href="http://www.jstatsoft.org/article/view/v005i08/ziggurat.pdf">paper</a>
     * and this implementation has been adapted from the C code provided therein.</p>
     */
    static class ZigguratExponentialSampler implements ContinuousSampler {
        /** Start of tail. */
        private static final double R = 7.69711747013104972;
        /** Index of last entry in the tables (which have a size that is a power of 2). */
        private static final int LAST = 255;
        /** Auxiliary table. */
        private static final long[] K;
        /** Auxiliary table. */
        private static final double[] W;
        /** Auxiliary table. */
        private static final double[] F;

        /** Underlying source of randomness. */
        private final UniformRandomProvider rng;

        static {
            // Filling the tables.
            // Rectangle area.
            final double v = 0.0039496598225815571993;
            // No support for unsigned long so the upper bound is 2^63
            final double max = Math.pow(2, 63);
            final double oneOverMax = 1d / max;

            K = new long[LAST + 1];
            W = new double[LAST + 1];
            F = new double[LAST + 1];

            double d = R;
            double t = d;
            double fd = pdf(d);
            final double q = v / fd;

            K[0] = (long) ((d / q) * max);
            K[1] = 0;

            W[0] = q * oneOverMax;
            W[LAST] = d * oneOverMax;

            F[0] = 1;
            F[LAST] = fd;

            for (int i = LAST - 1; i >= 1; i--) {
                d = -Math.log(v / d + fd);
                fd = pdf(d);

                K[i + 1] = (long) ((d / t) * max);
                t = d;

                F[i] = fd;

                W[i] = d * oneOverMax;
            }
        }

        /**
         * @param rng Generator of uniformly distributed random numbers.
         */
        ZigguratExponentialSampler(UniformRandomProvider rng) {
            this.rng = rng;
        }

        /** {@inheritDoc} */
        @Override
        public double sample() {
            // An unsigned long in [0, 2^63)
            final long j = rng.nextLong() >>> 1;
            final int i = ((int) j) & LAST;
            if (j < K[i]) {
                // This branch is called about 0.977777 times per call into createSample.
                // Note: Frequencies have been empirically measured for the first call to
                // createSample; recursion due to retries have been ignored. Frequencies sum to 1.
                return j * W[i];
            }
            return fix(j, i);
        }

        /**
         * Gets the value from the tail of the distribution.
         *
         * @param jz Start random integer.
         * @param iz Index of cell corresponding to {@code jz}.
         * @return the requested random value.
         */
        private double fix(long jz,
                           int iz) {
            if (iz == 0) {
                // Base strip.
                // This branch is called about 0.000448867 times per call into createSample.
                return R - Math.log(rng.nextDouble());
            }
            // Wedge of other strips.
            final double x = jz * W[iz];
            if (F[iz] + rng.nextDouble() * (F[iz - 1] - F[iz]) < pdf(x)) {
                // This branch is called about 0.0107820 times per call into createSample.
                return x;
            }
            // Try again.
            // This branch is called about 0.0109920 times per call into createSample
            // i.e. this is the recursion frequency.
            return sample();
        }

        /**
         * Compute the exponential probability density function {@code f(x) = e^-x}.
         *
         * @param x Argument.
         * @return {@code e^-x}
         */
        private static double pdf(double x) {
            return Math.exp(-x);
        }
    }

    /**
     * Modified Ziggurat method for sampling from a Gaussian distribution with mean 0 and standard deviation 1.
     *
     * <p>Uses the algorithm from:
     *
     * <blockquote>
     * McFarland, C.D. (2016)<br>
     * "A modified ziggurat algorithm for generating exponentially and normally distributed pseudorandom numbers".<br>
     * <i>Journal of Statistical Computation and Simulation</i> <b>86</b>, 1281-1294.
     * </blockquote>
     *
     * @see <a href="https://www.tandfonline.com/doi/abs/10.1080/00949655.2015.1060234">
     * McFarland (2016) JSCS 86, 1281-1294</a>
     */
    static class ModifiedZigguratNormalizedGaussianSampler implements ContinuousSampler {
        /** Maximum i value for early exit. */
        private static final int I_MAX = 253;
        /** The point where the Gaussian switches from convex to concave. */
        private static final int J_INFLECTION = 205;
        /** Mask to create an unsigned long from a signed long. */
        private static final long MAX_INT64 = 0x7fffffffffffffffL;
        /** Used for largest deviations of f(x) from y_i. This is negated on purpose. */
        private static final long MAX_IE = -2269182951627975918L;
        /** Used for largest deviations of f(x) from y_i. */
        private static final long MIN_IE = 760463704284035181L;
        /** 2^63. */
        private static final double TWO_POW_63 = 0x1.0p63;
        /** Beginning of tail. */
        private static final double X_0 = 3.6360066255;
        /** 1/X_0. */
        private static final double ONE_OVER_X_0 = 1d / X_0;

        /** The alias map. An integer in [0, 255] stored as a byte to save space. */
        private static final byte[] MAP = toBytes(
            new int[] {0, 0, 239, 2, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 253, 253, 253, 253, 253, 253, 253, 253, 253, 253,
                253, 253, 253, 253, 253, 253, 253, 253, 253, 253, 253, 253, 253, 253, 253, 253, 253, 253, 253, 253, 253,
                253, 253, 253, 253, 253, 253, 253, 253, 253, 253, 253, 253, 253, 253, 253, 253, 253, 253, 253, 253, 253,
                253, 253, 253, 253, 253, 253, 253, 253, 253, 253, 253, 253, 253, 253, 253, 253, 253, 253, 253, 253, 253,
                253, 253, 253, 253, 253, 253, 253, 253, 253, 253, 253, 253, 253, 253, 253, 253, 253, 253, 253, 253, 253,
                253, 253, 253, 253, 253, 253, 253, 253, 253, 253, 253, 253, 253, 253, 253, 253, 253, 253, 253, 253, 253,
                253, 253, 253, 253, 253, 253, 253, 253, 253, 253, 253, 253, 253, 253, 253, 253, 253, 253, 253, 253, 253,
                253, 253, 253, 253, 253, 253, 253, 253, 253, 253, 253, 253, 253, 253, 253, 253, 253, 253, 253, 253, 253,
                253, 253, 253, 253, 253, 253, 253, 253, 253, 253, 253, 253, 253, 253, 253, 253, 253, 253, 253, 253, 253,
                253, 252, 252, 252, 252, 252, 252, 252, 252, 252, 252, 251, 251, 251, 251, 251, 251, 251, 250, 250, 250,
                250, 250, 249, 249, 249, 248, 248, 248, 247, 247, 247, 246, 246, 245, 244, 244, 243, 242, 240, 2, 2, 3,
                3, 0, 0, 240, 241, 242, 243, 244, 245, 246, 247, 248, 249, 250, 251, 252, 253, 1, 0, 0});
        /** The alias inverse PMF. */
        private static final long[] IPMF = {9223372036854775807L, 1100243796532604147L, 7866600928978262881L,
            6788754710654027089L, 9022865200181691852L, 6522434035205505475L, 4723064097359993915L,
            3360495653216419673L, 2289663232373874393L, 1423968905551925104L, 708364817827802907L, 106102487305606492L,
            -408333464665790208L, -853239722779020926L, -1242095211825517037L, -1585059631105792592L,
            -1889943050287164616L, -2162852901990665326L, -2408637386594506662L, -2631196530262949902L,
            -2833704942520918738L, -3018774289025815052L, -3188573753472182441L, -3344920681707440829L,
            -3489349705062145773L, -3623166100042174483L, -3747487436868330185L, -3863276422712168700L,
            -3971367044063122321L, -4072485557029853479L, -4167267476830907467L, -4256271432240150335L,
            -4339990541927301065L, -4418861817133796561L, -4493273980372371289L, -4563574004462236379L,
            -4630072609770443287L, -4693048910430993529L, -4752754358862853623L, -4809416110052798111L,
            -4863239903586974371L, -4914412541515869230L, -4963104028439154501L, -5009469424769141876L,
            -5053650458856546947L, -5095776932695070785L, -5135967952544950843L, -5174333008451188631L,
            -5210972924952682065L, -5245980700100453012L, -5279442247516290042L, -5311437055462362013L,
            -5342038772315636138L, -5371315728843324427L, -5399331404632497705L, -5426144845493682879L,
            -5451811038474681565L, -5476381248265612057L, -5499903320558330825L, -5522421955752302985L,
            -5543978956085246988L, -5564613449659052023L, -5584362093436129148L, -5603259257517445975L,
            -5621337193070977432L, -5638626184974114133L, -5655154691220924683L, -5670949470294749069L,
            -5686035697601828417L, -5700437072199142976L, -5714175914219797723L, -5727273255295211389L,
            -5739748920272022600L, -5751621603810396917L, -5762908939773930926L, -5773627565914992314L,
            -5783793183152402325L, -5793420610475612574L, -5802523835894645221L, -5811116062947594592L,
            -5819209754516104281L, -5826816672854561136L, -5833947916825296227L, -5840613956570562553L,
            -5846824665591787384L, -5852589350491064419L, -5857916778480708968L, -5862815203334817577L,
            -5867292388935689664L, -5871355631762300668L, -5875011781262872391L, -5878267259039068099L,
            -5881128076579864607L, -5883599852028866964L, -5885687825288587920L, -5887396872144944193L,
            -5888731517955022366L, -5889695949247708370L, -5890294025706661862L, -5890529289910843690L,
            -5890404977676009159L, -5889924026487152552L, -5889089083913561497L, -5887902514965187544L,
            -5886366408898350160L, -5884482585690660648L, -5882252601321067956L, -5879677752995005083L,
            -5876759083794187244L, -5873497386318817608L, -5869893206505495615L, -5865946846617000703L,
            -5861658367354170141L, -5857027590486142268L, -5852054100063403979L, -5846737243971479915L,
            -5841076134082348607L, -5835069647234555080L, -5828716424754558627L, -5822014871949050545L,
            -5814963157357505615L, -5807559211080035948L, -5799800723447248431L, -5791685142338046612L,
            -5783209670985131963L, -5774371264582507258L, -5765166627072198898L, -5755592207057629351L,
            -5745644193442020886L, -5735318510777140130L, -5724610813433637581L, -5713516480385064197L,
            -5702030608511931905L, -5690148005851000163L, -5677863184109376595L, -5665170350903283020L,
            -5652063400924584736L, -5638535907000098559L, -5624581109999495711L, -5610191908627545348L,
            -5595360848093635657L, -5580080108034221525L, -5564341489875517452L, -5548136403221361788L,
            -5531455851545388194L, -5514290416638313566L, -5496630242181647032L, -5478465016761708394L,
            -5459783954986630496L, -5440575777891763554L, -5420828692432362328L, -5400530368638759086L,
            -5379667916699386572L, -5358227861294079939L, -5336196115274289941L, -5313557951078348351L,
            -5290297970633413513L, -5266400072915204637L, -5241847420213976978L, -5216622401043707762L,
            -5190706591719514516L, -5164080714589163015L, -5136724594099061292L, -5108617109269271995L,
            -5079736143458208314L, -5050058530461699570L, -5019559997031867155L, -4988215101008300666L,
            -4955997165600740840L, -4922878208651982466L, -4888828866780310778L, -4853818314258448763L,
            -4817814175855136221L, -4780782432601640934L, -4742687321746673837L, -4703491227581398702L,
            -4663154564978669839L, -4621635653358718847L, -4578890580370737438L, -4534873055659651863L,
            -4489534251700544707L, -4442822631898778778L, -4394683764809052552L, -4345060121983309848L,
            -4293890858708851568L, -4241111576153757717L, -4186654061692562932L, -4130446006804691432L,
            -4072410698657642967L, -4012466683838341666L, -3950527400304957273L, -3886500774061817392L,
            -3820288777467775968L, -3751786943594814089L, -3680883832433444937L, -3607460442623855428L,
            -3531389562483238811L, -3452535052936037985L, -3370751053395794721L, -3285881101589156030L,
            -3197757155301271700L, -3106198503156390075L, -3011010550911843739L, -2911983463883482375L,
            -2808890647470171482L, -2701487041141041038L, -2589507199690499622L, -2472663129329060997L,
            -2350641842139723534L, -2223102583769914387L, -2089673683729348624L, -1949948966045216354L,
            -1803483646855866339L, -1649789631524907106L, -1488330106094837958L, -1318513295725471712L,
            -1139685236971903433L, -951121376551959675L, -752016768184573709L, -541474585687415681L,
            -318492605680814263L, -81947227248966622L, 169425512568350963L, 437052607277165029L, 722551297569085274L,
            1027761939300002045L, 1354787941578333500L, 1706044619204253453L, 2084319374409947591L,
            2492846399638817506L, 2935400169348911565L, 3416413484613541924L, 3941127949861028145L,
            4515787798749165735L, 5147892401484995974L, 5846529325380992513L, 6622819682194933019L,
            7490522659874903812L, 8466869998278641829L, 8216968526368126501L, 4550693915471153825L,
            7628019504122306461L, 6605080500893076940L, 7121156327637209657L, 2484871780365768829L,
            7179104797069433749L, 7066086283825115773L, 1516500120794063563L, 216305945442773460L, 6295963418513296140L,
            2889316805672339623L, -2712587580543026574L, 6562498853519217374L, 7975754821145999232L,
            -9223372036854775807L, -9223372036854775807L};
        /**
         * The precomputed ziggurat lengths, denoted X_i in the main text. X_i = length of
         * ziggurat layer i.
         */
        private static final double[] X = {3.94216628254e-19, 3.72049450041e-19, 3.58270244806e-19, 3.48074762365e-19,
            3.39901771719e-19, 3.33037783603e-19, 3.27094388176e-19, 3.21835771325e-19, 3.17107585418e-19,
            3.1280307407e-19, 3.08845206558e-19, 3.05176506241e-19, 3.01752902926e-19, 2.98539834407e-19,
            2.95509674628e-19, 2.92639979885e-19, 2.899122587e-19, 2.87311087802e-19, 2.84823463271e-19,
            2.82438315352e-19, 2.80146139647e-19, 2.77938712618e-19, 2.75808869214e-19, 2.73750326983e-19,
            2.71757545434e-19, 2.69825612475e-19, 2.67950151888e-19, 2.66127247304e-19, 2.6435337928e-19,
            2.6262537282e-19, 2.60940353352e-19, 2.59295709543e-19, 2.57689061732e-19, 2.56118234977e-19,
            2.54581235934e-19, 2.53076232924e-19, 2.51601538678e-19, 2.50155595336e-19, 2.48736961354e-19,
            2.47344300031e-19, 2.45976369429e-19, 2.44632013479e-19, 2.43310154111e-19, 2.42009784271e-19,
            2.40729961704e-19, 2.39469803409e-19, 2.38228480673e-19, 2.37005214619e-19, 2.35799272207e-19,
            2.34609962621e-19, 2.33436634011e-19, 2.32278670547e-19, 2.31135489743e-19, 2.30006540027e-19,
            2.28891298528e-19, 2.27789269059e-19, 2.26699980275e-19, 2.25622983985e-19, 2.24557853607e-19,
            2.23504182749e-19, 2.22461583905e-19, 2.21429687253e-19, 2.20408139549e-19, 2.19396603103e-19,
            2.18394754837e-19, 2.17402285409e-19, 2.164188984e-19, 2.15444309566e-19, 2.14478246135e-19,
            2.13520446164e-19, 2.12570657924e-19, 2.11628639347e-19, 2.10694157491e-19, 2.09766988055e-19,
            2.08846914916e-19, 2.079337297e-19, 2.0702723138e-19, 2.06127225897e-19, 2.05233525809e-19,
            2.04345949953e-19, 2.03464323137e-19, 2.02588475842e-19, 2.01718243948e-19, 2.00853468469e-19,
            1.99993995309e-19, 1.9913967503e-19, 1.9829036263e-19, 1.97445917335e-19, 1.96606202405e-19,
            1.95771084943e-19, 1.94940435722e-19, 1.9411412902e-19, 1.93292042452e-19, 1.92474056827e-19,
            1.91660056003e-19, 1.90849926746e-19, 1.90043558606e-19, 1.89240843788e-19, 1.88441677035e-19,
            1.87645955517e-19, 1.86853578721e-19, 1.8606444835e-19, 1.85278468221e-19, 1.84495544175e-19,
            1.83715583984e-19, 1.82938497262e-19, 1.82164195388e-19, 1.81392591419e-19, 1.80623600019e-19,
            1.7985713738e-19, 1.79093121154e-19, 1.78331470384e-19, 1.77572105435e-19, 1.76814947933e-19,
            1.76059920701e-19, 1.753069477e-19, 1.74555953971e-19, 1.73806865576e-19, 1.73059609547e-19,
            1.72314113829e-19, 1.71570307233e-19, 1.70828119379e-19, 1.7008748065e-19, 1.69348322146e-19,
            1.68610575631e-19, 1.67874173493e-19, 1.67139048692e-19, 1.66405134721e-19, 1.6567236556e-19,
            1.64940675631e-19, 1.64209999755e-19, 1.63480273116e-19, 1.62751431209e-19, 1.62023409806e-19,
            1.61296144913e-19, 1.60569572726e-19, 1.59843629593e-19, 1.59118251972e-19, 1.58393376391e-19,
            1.57668939404e-19, 1.56944877552e-19, 1.56221127324e-19, 1.55497625108e-19, 1.54774307158e-19,
            1.54051109542e-19, 1.53327968107e-19, 1.52604818431e-19, 1.51881595777e-19, 1.51158235054e-19,
            1.50434670764e-19, 1.49710836959e-19, 1.48986667191e-19, 1.48262094465e-19, 1.47537051186e-19,
            1.46811469107e-19, 1.46085279278e-19, 1.4535841199e-19, 1.44630796717e-19, 1.43902362058e-19,
            1.43173035676e-19, 1.42442744238e-19, 1.41711413344e-19, 1.40978967466e-19, 1.40245329873e-19,
            1.39510422558e-19, 1.38774166165e-19, 1.38036479905e-19, 1.37297281475e-19, 1.36556486972e-19,
            1.35814010798e-19, 1.35069765568e-19, 1.34323662007e-19, 1.33575608847e-19, 1.32825512715e-19,
            1.32073278015e-19, 1.31318806805e-19, 1.30561998669e-19, 1.29802750579e-19, 1.29040956749e-19,
            1.28276508483e-19, 1.2750929401e-19, 1.26739198313e-19, 1.25966102948e-19, 1.25189885844e-19,
            1.24410421101e-19, 1.23627578765e-19, 1.22841224598e-19, 1.2205121982e-19, 1.21257420848e-19,
            1.20459679002e-19, 1.19657840201e-19, 1.18851744634e-19, 1.18041226403e-19, 1.17226113142e-19,
            1.16406225609e-19, 1.15581377245e-19, 1.14751373693e-19, 1.13916012285e-19, 1.13075081485e-19,
            1.12228360281e-19, 1.11375617531e-19, 1.10516611251e-19, 1.09651087832e-19, 1.08778781199e-19,
            1.07899411881e-19, 1.07012685997e-19, 1.06118294148e-19, 1.05215910191e-19, 1.043051899e-19,
            1.0338576948e-19, 1.02457263929e-19, 1.01519265222e-19, 1.00571340295e-19, 9.96130287997e-20,
            9.86438405995e-20, 9.76632529648e-20, 9.66707074276e-20, 9.56656062409e-20, 9.46473083804e-20,
            9.36151250173e-20, 9.25683143709e-20, 9.15060758376e-20, 9.04275432677e-20, 8.93317772338e-20,
            8.82177561023e-20, 8.70843656749e-20, 8.59303871096e-20, 8.47544827642e-20, 8.35551795085e-20,
            8.23308489336e-20, 8.10796837291e-20, 7.97996692841e-20, 7.84885492861e-20, 7.71437837009e-20,
            7.57624969795e-20, 7.43414135785e-20, 7.28767768074e-20, 7.13642454435e-20, 6.97987602408e-20,
            6.81743689448e-20, 6.64839929862e-20, 6.47191103452e-20, 6.28693148131e-20, 6.09216875483e-20,
            5.88598735756e-20, 5.66626751161e-20, 5.43018136309e-20, 5.17381717445e-20, 4.89150317224e-20,
            4.57447418908e-20, 4.20788025686e-20, 3.76259867224e-20, 3.16285898059e-20, 0.0};
        /** Overhang table. Y_i = f(X_i). */
        private static final double[] Y = {1.45984107966e-22, 3.00666134279e-22, 4.61297288151e-22, 6.26633500492e-22,
            7.95945247619e-22, 9.68746550217e-22, 1.14468770024e-21, 1.32350363044e-21, 1.50498576921e-21,
            1.68896530007e-21, 1.87530253827e-21, 2.06387984237e-21, 2.25459669136e-21, 2.44736615188e-21,
            2.64211227278e-21, 2.83876811879e-21, 3.03727425675e-21, 3.23757757e-21, 3.43963031579e-21,
            3.6433893658e-21, 3.84881558689e-21, 4.05587333095e-21, 4.26453001043e-21, 4.47475574223e-21,
            4.68652304654e-21, 4.89980659028e-21, 5.11458296721e-21, 5.3308305082e-21, 5.5485291167e-21,
            5.76766012527e-21, 5.98820616992e-21, 6.21015107954e-21, 6.43347977823e-21, 6.65817819857e-21,
            6.88423320459e-21, 7.1116325228e-21, 7.34036468049e-21, 7.57041895029e-21, 7.80178530014e-21,
            8.03445434816e-21, 8.26841732173e-21, 8.50366602039e-21, 8.74019278201e-21, 8.97799045203e-21,
            9.21705235531e-21, 9.45737227039e-21, 9.69894440593e-21, 9.94176337898e-21, 1.01858241951e-20,
            1.04311222301e-20, 1.0677653213e-20, 1.09254132104e-20, 1.11743986124e-20, 1.14246061187e-20,
            1.16760327269e-20, 1.19286757204e-20, 1.21825326583e-20, 1.24376013654e-20, 1.2693879923e-20,
            1.29513666605e-20, 1.32100601473e-20, 1.34699591858e-20, 1.37310628045e-20, 1.39933702514e-20,
            1.42568809885e-20, 1.4521594686e-20, 1.47875112175e-20, 1.50546306552e-20, 1.53229532653e-20,
            1.55924795044e-20, 1.58632100153e-20, 1.61351456238e-20, 1.64082873355e-20, 1.66826363327e-20,
            1.69581939719e-20, 1.72349617811e-20, 1.75129414576e-20, 1.77921348663e-20, 1.80725440373e-20,
            1.83541711644e-20, 1.86370186038e-20, 1.89210888728e-20, 1.92063846482e-20, 1.94929087658e-20,
            1.97806642193e-20, 2.00696541597e-20, 2.03598818948e-20, 2.06513508884e-20, 2.09440647607e-20,
            2.12380272876e-20, 2.15332424009e-20, 2.18297141884e-20, 2.21274468943e-20, 2.24264449191e-20,
            2.27267128206e-20, 2.30282553143e-20, 2.33310772738e-20, 2.36351837324e-20, 2.39405798832e-20,
            2.42472710808e-20, 2.45552628422e-20, 2.48645608479e-20, 2.5175170944e-20, 2.54870991431e-20,
            2.58003516259e-20, 2.61149347436e-20, 2.64308550193e-20, 2.67481191499e-20, 2.70667340088e-20,
            2.73867066474e-20, 2.77080442982e-20, 2.80307543767e-20, 2.83548444847e-20, 2.86803224123e-20,
            2.90071961414e-20, 2.93354738484e-20, 2.96651639078e-20, 2.99962748948e-20, 3.03288155897e-20,
            3.06627949809e-20, 3.09982222687e-20, 3.13351068696e-20, 3.16734584202e-20, 3.20132867816e-20,
            3.23546020438e-20, 3.26974145302e-20, 3.30417348029e-20, 3.33875736673e-20, 3.37349421775e-20,
            3.40838516421e-20, 3.44343136293e-20, 3.4786339973e-20, 3.51399427794e-20, 3.54951344328e-20,
            3.58519276026e-20, 3.62103352501e-20, 3.65703706358e-20, 3.69320473266e-20, 3.7295379204e-20,
            3.76603804721e-20, 3.80270656658e-20, 3.83954496597e-20, 3.87655476775e-20, 3.91373753011e-20,
            3.95109484807e-20, 3.98862835454e-20, 4.02633972133e-20, 4.06423066034e-20, 4.10230292468e-20,
            4.14055830991e-20, 4.1789986553e-20, 4.21762584518e-20, 4.25644181026e-20, 4.29544852916e-20,
            4.33464802983e-20, 4.3740423912e-20, 4.41363374476e-20, 4.45342427632e-20, 4.49341622781e-20,
            4.53361189911e-20, 4.5740136501e-20, 4.61462390263e-20, 4.65544514274e-20, 4.69647992292e-20,
            4.73773086444e-20, 4.77920065987e-20, 4.82089207569e-20, 4.86280795501e-20, 4.90495122048e-20,
            4.94732487728e-20, 4.98993201633e-20, 5.03277581761e-20, 5.07585955372e-20, 5.11918659356e-20,
            5.16276040629e-20, 5.20658456539e-20, 5.25066275307e-20, 5.29499876488e-20, 5.33959651452e-20,
            5.38446003902e-20, 5.42959350421e-20, 5.47500121042e-20, 5.52068759864e-20, 5.566657257e-20,
            5.61291492763e-20, 5.65946551399e-20, 5.70631408865e-20, 5.75346590156e-20, 5.80092638886e-20,
            5.8487011823e-20, 5.89679611927e-20, 5.94521725351e-20, 5.99397086661e-20, 6.04306348026e-20,
            6.09250186942e-20, 6.14229307644e-20, 6.19244442624e-20, 6.24296354262e-20, 6.29385836583e-20,
            6.34513717154e-20, 6.39680859128e-20, 6.44888163458e-20, 6.5013657129e-20, 6.55427066567e-20,
            6.60760678847e-20, 6.66138486374e-20, 6.71561619424e-20, 6.7703126396e-20, 6.82548665622e-20,
            6.88115134113e-20, 6.93732047997e-20, 6.9940085999e-20, 7.05123102793e-20, 7.10900395534e-20,
            7.16734450906e-20, 7.22627083097e-20, 7.28580216611e-20, 7.3459589613e-20, 7.4067629755e-20,
            7.46823740371e-20, 7.53040701672e-20, 7.59329831907e-20, 7.65693972825e-20, 7.72136177895e-20,
            7.78659735664e-20, 7.85268196595e-20, 7.91965404039e-20, 7.9875553017e-20, 8.05643117889e-20,
            8.12633129964e-20, 8.19731007037e-20, 8.26942736526e-20, 8.34274935088e-20, 8.41734948075e-20,
            8.49330970528e-20, 8.57072195782e-20, 8.64968999859e-20, 8.73033172957e-20, 8.81278213789e-20,
            8.89719709282e-20, 8.98375832393e-20, 9.07268006979e-20, 9.16421814841e-20, 9.25868264067e-20,
            9.35645614803e-20, 9.45802100126e-20, 9.56400155509e-20, 9.67523347705e-20, 9.79288516978e-20,
            9.91869058575e-20, 1.00554562713e-19, 1.02084073773e-19, 1.03903609932e-19, 1.08420217249e-19};

        /** Underlying source of randomness. */
        private final UniformRandomProvider rng;
        /** Exponential sampler used for the long tail. */
        private final ContinuousSampler exponential;

        /**
         * @param rng Generator of uniformly distributed random numbers.
         */
        ModifiedZigguratNormalizedGaussianSampler(UniformRandomProvider rng) {
            this.rng = rng;
            exponential = new ModifiedZigguratExponentialSampler(rng);
        }

        /** {@inheritDoc} */
        @Override
        public double sample() {
            final long xx = rng.nextLong();
            // Float multiplication squashes these last 8 bits, so they can be used to sample i
            final int i = ((int) xx) & 0xff;

            if (i < I_MAX) {
                // Early exit.
                // Branch frequency: 0.988280
                return X[i] * xx;
            }

            long u1 = randomInt63();
            // Another squashed, recyclable bit
            // double sign_bit = u1 & 0x100 ? 1. : -1.
            // Use 2 - 1 or 0 - 1
            final double signBit = ((u1 >>> 7) & 0x2) - 1.0;
            final int j = normSampleA();
            // Four kinds of overhangs:
            //  j = 0                :  Sample from tail
            //  0 < j < J_INFLECTION :  Overhang is concave; only sample from Lower-Left triangle
            //  j = J_INFLECTION     :  Must sample from entire overhang rectangle
            //  j > J_INFLECTION     :  Overhangs are convex; implicitly accept point in Lower-Left triangle
            //
            // Conditional statements are arranged such that the more likely outcomes are first.
            double x;
            if (j > J_INFLECTION) {
                // Convex overhang
                // Branch frequency: 0.00891413
                for (;;) {
                    x = fastPrngSampleX(j, u1);
                    final long uDiff = randomInt63() - u1;
                    if (uDiff > MIN_IE) {
                        break;
                    }
                    if (uDiff < MAX_IE) {
                        continue;
                    }
                    // Long.MIN_VALUE is used as an unsigned int with value 2^63:
                    // uy = Long.MIN_VALUE - (ux + uDiff)
                    if (fastPrngSampleY(j, Long.MIN_VALUE - (u1 + uDiff)) < Math.exp(-0.5 * x * x)) {
                        break;
                    }
                    u1 = randomInt63();
                }
            } else if (j == 0) {
                // Tail
                // Branch frequency: 0.000277067
                do {
                    x = ONE_OVER_X_0 * exponential.sample();
                } while (exponential.sample() < 0.5 * x * x);
                x += X_0;
            } else if (j < J_INFLECTION) {
                // Concave overhang
                // Branch frequency: 0.00251223
                for (;;) {
                    // U_x <- min(U_1, U_2)
                    // distance <- | U_1 - U_2 |
                    // U_y <- 1 - (U_x + distance)
                    long uDiff = randomInt63() - u1;
                    if (uDiff < 0) {
                        uDiff = -uDiff;
                        u1 -= uDiff;
                    }
                    x = fastPrngSampleX(j, u1);
                    if (uDiff > MIN_IE)  {
                        break;
                    }
                    if (fastPrngSampleY(j, Long.MIN_VALUE - (u1 + uDiff)) < Math.exp(-0.5 * x * x)) {
                        break;
                    }
                    u1 = randomInt63();
                }
            } else {
                // Inflection point
                // Branch frequency: 0.0000161147
                for (;;) {
                    x = fastPrngSampleX(j, u1);
                    if (fastPrngSampleY(j, randomInt63()) < Math.exp(-0.5 * x * x)) {
                        break;
                    }
                    u1 = randomInt63();
                }
            }
            return signBit * x;
        }

        /**
         * Alias sampling.
         * See http://scorevoting.net/WarrenSmithPages/homepage/sampling.abs
         *
         * @return the alias
         */
        private int normSampleA() {
            final long x = rng.nextLong();
            // j <- I(0, 256)
            final int j = ((int) x) & 0xff;
            return x >= IPMF[j] ? MAP[j] & 0xff : j;
        }

        /**
         * Return a positive long in {@code [0, 2^63)}.
         *
         * @return the long
         */
        private long randomInt63() {
            return rng.nextLong() & MAX_INT64;
        }

        /**
         * Auxilary function to see if rejection sampling is required in the overhang.
         * See Fig. 2 in the main text.
         *
         * @param j j
         * @param ux ux
         * @return the sample
         */
        private static double fastPrngSampleX(int j, long ux) {
            return X[j] * TWO_POW_63 + (X[j - 1] - X[j]) * ux;
        }

        /**
         * Auxilary function to see if rejection sampling is required in the overhang.
         * See Fig. 2 in the main text.
         *
         * @param i i
         * @param uy uy
         * @return the sample
         */
        private static double fastPrngSampleY(int i, long uy) {
            return Y[i - 1] * TWO_POW_63 + (Y[i] - Y[i - 1]) * uy;
        }

        /**
         * Helper function to convert {@code int} values to bytes using a narrowing primitive conversion.
         *
         * @param values Integer values.
         * @return the bytes
         */
        private static byte[] toBytes(int[] values) {
            final byte[] bytes = new byte[values.length];
            for (int i = 0; i < bytes.length; i++) {
                bytes[i] = (byte) values[i];
            }
            return bytes;
        }
    }

    /**
     * Modified Ziggurat method for sampling from an exponential distribution.
     *
     * <p>Uses the algorithm from:
     *
     * <blockquote>
     * McFarland, C.D. (2016)<br>
     * "A modified ziggurat algorithm for generating exponentially and normally distributed pseudorandom numbers".<br>
     * <i>Journal of Statistical Computation and Simulation</i> <b>86</b>, 1281-1294.
     * </blockquote>
     *
     * @see <a href="https://www.tandfonline.com/doi/abs/10.1080/00949655.2015.1060234">
     * McFarland (2016) JSCS 86, 1281-1294</a>
     */
    static class ModifiedZigguratExponentialSampler implements ContinuousSampler {
        /** Maximum i value for early exit. */
        private static final int I_MAX = 252;
        /** Mask to create an unsigned long from a signed long. */
        private static final long MAX_INT64 = 0x7fffffffffffffffL;
        /** Maximum distance value for early exit. */
        private static final long IE_MAX = 513303011048449572L;
        /** 2^53. */
        private static final double TWO_POW_63 = 0x1.0p63;
        /** Beginning of tail. */
        private static final double X_0 = 7.56927469415;

        /** The alias map. An integer in [0, 255] stored as a byte to save space. */
        private static final byte[] MAP = toBytes(new int[] {0, 0, 1, 235, 3, 4, 5, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1,
            1, 1, 1, 2, 2, 252, 252, 252, 252, 252, 252, 252, 252, 252, 252, 252, 252, 252, 252, 252, 252, 252, 252,
            252, 252, 252, 252, 252, 252, 252, 252, 252, 252, 252, 252, 252, 252, 252, 252, 252, 252, 252, 252, 252,
            252, 252, 252, 252, 252, 252, 252, 252, 252, 252, 252, 252, 252, 252, 252, 252, 252, 252, 252, 252, 252,
            252, 252, 252, 252, 252, 252, 252, 252, 252, 252, 252, 252, 252, 252, 252, 252, 252, 252, 252, 252, 252,
            252, 252, 252, 252, 252, 252, 252, 252, 252, 252, 252, 252, 252, 252, 252, 252, 252, 252, 252, 252, 252,
            252, 252, 252, 252, 252, 252, 252, 252, 252, 252, 252, 252, 252, 252, 252, 252, 252, 252, 252, 252, 252,
            252, 252, 252, 252, 252, 252, 252, 252, 252, 252, 252, 252, 252, 252, 252, 252, 252, 252, 252, 252, 252,
            252, 252, 252, 252, 252, 252, 252, 252, 252, 251, 251, 251, 251, 251, 251, 251, 251, 251, 251, 251, 251,
            251, 250, 250, 250, 250, 250, 250, 250, 249, 249, 249, 249, 249, 249, 248, 248, 248, 248, 247, 247, 247,
            247, 246, 246, 246, 245, 245, 244, 244, 243, 243, 242, 241, 241, 240, 239, 237, 3, 3, 4, 4, 6, 0, 0, 0, 0,
            236, 237, 238, 239, 240, 241, 242, 243, 244, 245, 246, 247, 248, 249, 250, 251, 252, 2, 0, 0, 0});
        /** The alias inverse PMF. */
        private static final long[] IPMF = {9223372036854775328L, 1623796909450838420L, 2664290944894293715L,
            7387971354164060121L, 6515064486552739054L, 8840508362680717952L, 6099647593382935246L,
            7673130333659513959L, 6220332867583438265L, 5045979640552813853L, 4075305837223955667L,
            3258413672162525563L, 2560664887087762661L, 1957224924672899759L, 1429800935350577626L, 964606309710808357L,
            551043923599587249L, 180827629096890397L, -152619738120023526L, -454588624410291449L, -729385126147774875L,
            -980551509819446846L, -1211029700667463872L, -1423284293868547154L, -1619396356369050292L,
            -1801135830956212822L, -1970018048575618008L, -2127348289059705241L, -2274257249303686299L,
            -2411729520096655228L, -2540626634159182525L, -2661705860113406462L, -2775635634532448735L,
            -2883008316030465121L, -2984350790383654722L, -3080133339198118434L, -3170777096303091107L,
            -3256660348483804932L, -3338123885075152741L, -3415475560473282822L, -3488994201966444710L,
            -3558932970354470759L, -3625522261068041096L, -3688972217741992040L, -3749474917563782729L,
            -3807206277531056234L, -3862327722496827274L, -3914987649156779787L, -3965322714631865323L,
            -4013458973776912076L, -4059512885612767084L, -4103592206186241133L, -4145796782586128173L,
            -4186219260694363437L, -4224945717447258894L, -4262056226866285614L, -4297625367836519694L,
            -4331722680528537423L, -4364413077437472623L, -4395757214229418223L, -4425811824915119504L,
            -4454630025296932688L, -4482261588141311280L, -4508753193105271888L, -4534148654077804689L,
            -4558489126279970065L, -4581813295192216657L, -4604157549138257681L, -4625556137145250418L,
            -4646041313519109426L, -4665643470413305970L, -4684391259530326642L, -4702311703971761747L,
            -4719430301145086931L, -4735771117539946355L, -4751356876102103699L, -4766209036859128403L,
            -4780347871386013331L, -4793792531638892019L, -4806561113635122292L, -4818670716409312756L,
            -4830137496634465780L, -4840976719260854452L, -4851202804490332660L, -4860829371376460084L,
            -4869869278311657652L, -4878334660640771092L, -4886236965617427412L, -4893586984900802772L,
            -4900394884772702964L, -4906670234238885493L, -4912422031164489589L, -4917658726580136309L,
            -4922388247283532373L, -4926618016851059029L, -4930354975163335189L, -4933605596540651285L,
            -4936375906575303797L, -4938671497741365845L, -4940497543854575637L, -4941858813449629493L,
            -4942759682136114997L, -4943204143989086773L, -4943195822025527893L, -4942737977813206357L,
            -4941833520255033237L, -4940485013586738773L, -4938694684624359381L, -4936464429291795925L,
            -4933795818458825557L, -4930690103114057941L, -4927148218896868949L, -4923170790008275925L,
            -4918758132519202261L, -4913910257091645845L, -4908626871126550421L, -4902907380349522964L,
            -4896750889844289364L, -4890156204540514772L, -4883121829162554452L, -4875645967641803284L,
            -4867726521994894420L, -4859361090668136340L, -4850546966345097428L, -4841281133215539220L,
            -4831560263698486164L, -4821380714613453652L, -4810738522790066260L, -4799629400105482131L,
            -4788048727936313747L, -4775991551010508883L, -4763452570642098131L, -4750426137329511059L,
            -4736906242696389331L, -4722886510751361491L, -4708360188440098835L, -4693320135461437394L,
            -4677758813316075410L, -4661668273553512594L, -4645040145179234642L, -4627865621182772242L,
            -4610135444140937425L, -4591839890849345681L, -4572968755929937937L, -4553511334358205905L,
            -4533456402849118097L, -4512792200036279121L, -4491506405372581072L, -4469586116675402576L,
            -4447017826233108176L, -4423787395382268560L, -4399880027458432847L, -4375280239014115151L,
            -4349971829190464271L, -4323937847117722127L, -4297160557210950158L, -4269621402214950094L,
            -4241300963840749518L, -4212178920821845518L, -4182234004204468173L, -4151443949668868493L,
            -4119785446662289613L, -4087234084103201932L, -4053764292396157324L, -4019349281473091724L,
            -3983960974549676683L, -3947569937258407435L, -3910145301787369227L, -3871654685619016074L,
            -3832064104425399050L, -3791337878631545353L, -3749438533114317833L, -3706326689447995081L,
            -3661960950051848712L, -3616297773528535240L, -3569291340409179143L, -3520893408440946503L,
            -3471053156460654726L, -3419717015797782918L, -3366828488034800645L, -3312327947826472069L,
            -3256152429334011012L, -3198235394669703364L, -3138506482563184963L, -3076891235255163586L,
            -3013310801389731586L, -2947681612411375617L, -2879915029671665601L, -2809916959107518656L,
            -2737587429961872959L, -2662820133571326270L, -2585501917733374398L, -2505512231579382333L,
            -2422722515205206076L, -2336995527534112187L, -2248184604988688954L, -2156132842510798521L,
            -2060672187261006776L, -1961622433929382455L, -1858790108950092598L, -1751967229002903349L,
            -1640929916937143604L, -1525436855617592627L, -1405227557075244850L, -1280020420662660017L,
            -1149510549536587824L, -1013367289578705710L, -871231448632088621L, -722712146453685035L,
            -567383236774420522L, -404779231966955560L, -234390647591522471L, -55658667960120229L, 132030985907824093L,
            329355128892810847L, 537061298001092449L, 755977262693571427L, 987022116608031845L, 1231219266829421544L,
            1489711711346525930L, 1763780090187560429L, 2054864117341776240L, 2364588157623792755L,
            2694791916990483702L, 3047567482883492729L, 3425304305830814717L, 3830744187097279873L,
            4267048975685831301L, 4737884547990035082L, 5247525842198997007L, 5800989391535354004L,
            6404202162993293978L, 7064218894258529185L, 7789505049452340392L, 8590309807749443504L,
            7643763810684498323L, 8891950541491447639L, 5457384281016226081L, 9083704440929275131L,
            7976211653914439517L, 8178631350487107662L, 2821287825726743868L, 6322989683301723979L,
            4309503753387603546L, 4685170734960182655L, 8404845967535219911L, 7330522972447586582L,
            1960945799077017972L, 4742910674644930459L, -751799822533465632L, 7023456603741994979L,
            3843116882594690323L, 3927231442413903597L, -9223372036854775807L, -9223372036854775807L,
            -9223372036854775807L};
        /** The precomputed ziggurat lengths, denoted X_i in the main text. */
        private static final double[] X_I = {8.20662406753e-19, 7.39737323516e-19, 6.91333133779e-19, 6.5647358821e-19,
            6.29125399598e-19, 6.06572241296e-19, 5.87352761037e-19, 5.70588505285e-19, 5.55709456916e-19,
            5.42324389037e-19, 5.30152976965e-19, 5.18987392577e-19, 5.0866922618e-19, 4.99074929388e-19,
            4.90106258944e-19, 4.81683790106e-19, 4.73742386536e-19, 4.66227958072e-19, 4.59095090178e-19,
            4.52305277907e-19, 4.45825588164e-19, 4.39627631264e-19, 4.33686759671e-19, 4.27981436185e-19,
            4.22492730271e-19, 4.17203912535e-19, 4.12100125225e-19, 4.07168112259e-19, 4.0239599631e-19,
            3.97773093429e-19, 3.93289757853e-19, 3.88937251293e-19, 3.84707632187e-19, 3.80593661382e-19,
            3.76588721385e-19, 3.7268674692e-19, 3.68882164922e-19, 3.65169842488e-19, 3.61545041533e-19,
            3.58003379153e-19, 3.54540792845e-19, 3.51153509888e-19, 3.478380203e-19, 3.44591052889e-19,
            3.41409553966e-19, 3.38290668387e-19, 3.35231722623e-19, 3.32230209587e-19, 3.29283775028e-19,
            3.26390205282e-19, 3.23547416228e-19, 3.20753443311e-19, 3.18006432505e-19, 3.15304632118e-19,
            3.12646385343e-19, 3.10030123469e-19, 3.07454359701e-19, 3.049176835e-19, 3.02418755411e-19,
            2.99956302321e-19, 2.97529113107e-19, 2.95136034631e-19, 2.92775968057e-19, 2.90447865454e-19,
            2.88150726664e-19, 2.85883596399e-19, 2.83645561563e-19, 2.81435748768e-19, 2.79253322026e-19,
            2.77097480612e-19, 2.74967457073e-19, 2.72862515379e-19, 2.70781949192e-19, 2.68725080264e-19,
            2.66691256932e-19, 2.64679852713e-19, 2.62690264997e-19, 2.60721913814e-19, 2.58774240685e-19,
            2.56846707542e-19, 2.54938795718e-19, 2.53050004991e-19, 2.51179852691e-19, 2.49327872862e-19,
            2.47493615466e-19, 2.45676645638e-19, 2.43876542983e-19, 2.42092900908e-19, 2.40325326001e-19,
            2.38573437435e-19, 2.36836866406e-19, 2.35115255607e-19, 2.33408258722e-19, 2.31715539953e-19,
            2.3003677357e-19, 2.28371643478e-19, 2.2671984282e-19, 2.2508107358e-19, 2.23455046227e-19,
            2.21841479361e-19, 2.20240099382e-19, 2.18650640175e-19, 2.17072842808e-19, 2.15506455249e-19,
            2.13951232087e-19, 2.12406934276e-19, 2.10873328882e-19, 2.09350188851e-19, 2.07837292773e-19,
            2.06334424671e-19, 2.04841373792e-19, 2.03357934403e-19, 2.01883905608e-19, 2.00419091156e-19,
            1.98963299272e-19, 1.97516342486e-19, 1.96078037473e-19, 1.94648204892e-19, 1.93226669243e-19,
            1.9181325872e-19, 1.90407805074e-19, 1.89010143478e-19, 1.87620112397e-19, 1.86237553469e-19,
            1.8486231138e-19, 1.83494233754e-19, 1.82133171034e-19, 1.80778976379e-19, 1.79431505561e-19,
            1.78090616856e-19, 1.76756170954e-19, 1.75428030858e-19, 1.74106061794e-19, 1.7279013112e-19,
            1.71480108238e-19, 1.7017586451e-19, 1.68877273172e-19, 1.67584209255e-19, 1.66296549505e-19,
            1.65014172306e-19, 1.63736957602e-19, 1.62464786823e-19, 1.61197542813e-19, 1.59935109756e-19,
            1.58677373107e-19, 1.57424219521e-19, 1.56175536784e-19, 1.54931213746e-19, 1.5369114025e-19,
            1.52455207068e-19, 1.51223305837e-19, 1.49995328986e-19, 1.48771169674e-19, 1.47550721726e-19,
            1.46333879563e-19, 1.4512053814e-19, 1.43910592874e-19, 1.42703939586e-19, 1.41500474425e-19,
            1.40300093807e-19, 1.39102694344e-19, 1.37908172772e-19, 1.36716425886e-19, 1.35527350466e-19,
            1.34340843201e-19, 1.3315680062e-19, 1.31975119012e-19, 1.3079569435e-19, 1.29618422208e-19,
            1.28443197683e-19, 1.27269915307e-19, 1.26098468959e-19, 1.24928751776e-19, 1.23760656057e-19,
            1.22594073168e-19, 1.21428893439e-19, 1.20265006056e-19, 1.19102298955e-19, 1.17940658704e-19,
            1.16779970383e-19, 1.15620117456e-19, 1.14460981638e-19, 1.13302442758e-19, 1.12144378607e-19,
            1.10986664787e-19, 1.0982917454e-19, 1.08671778581e-19, 1.07514344905e-19, 1.06356738599e-19,
            1.05198821625e-19, 1.04040452605e-19, 1.02881486575e-19, 1.01721774741e-19, 1.00561164199e-19,
            9.93994976483e-20, 9.82366130767e-20, 9.70723434263e-20, 9.59065162307e-20, 9.47389532242e-20,
            9.35694699202e-20, 9.23978751546e-20, 9.12239705906e-20, 9.00475501809e-20, 8.88683995826e-20,
            8.76862955198e-20, 8.65010050861e-20, 8.53122849831e-20, 8.41198806844e-20, 8.29235255165e-20,
            8.1722939648e-20, 8.05178289728e-20, 7.93078838751e-20, 7.80927778595e-20, 7.68721660284e-20,
            7.5645683384e-20, 7.44129429302e-20, 7.31735335451e-20, 7.19270175876e-20, 7.06729281977e-20,
            6.94107662395e-20, 6.81399968293e-20, 6.68600453746e-20, 6.55702930402e-20, 6.42700715334e-20,
            6.29586570809e-20, 6.16352634381e-20, 6.02990337322e-20, 5.89490308929e-20, 5.75842263599e-20,
            5.62034866696e-20, 5.48055574135e-20, 5.3389043909e-20, 5.1952387718e-20, 5.04938378663e-20,
            4.90114152226e-20, 4.75028679334e-20, 4.59656150013e-20, 4.4396673898e-20, 4.27925663021e-20,
            4.11491932734e-20, 3.94616667626e-20, 3.77240771314e-20, 3.59291640862e-20, 3.40678366911e-20,
            3.21284476416e-20, 3.00956469164e-20, 2.79484694556e-20, 2.56569130487e-20, 2.31752097568e-20,
            2.04266952283e-20, 1.72617703302e-20, 1.32818892594e-20, 0.0};
        /** Overhang table. */
        private static final double[] Y = {5.59520549511e-23, 1.18025099827e-22, 1.84444233867e-22, 2.54390304667e-22,
            3.27376943115e-22, 4.03077321327e-22, 4.81254783195e-22, 5.61729148966e-22, 6.44358205404e-22,
            7.29026623435e-22, 8.15638884563e-22, 9.04114536835e-22, 9.94384884864e-22, 1.0863906046e-21,
            1.18007997755e-21, 1.27540755348e-21, 1.37233311764e-21, 1.47082087944e-21, 1.57083882574e-21,
            1.67235819844e-21, 1.7753530675e-21, 1.87979997851e-21, 1.98567765878e-21, 2.09296677041e-21,
            2.201649701e-21, 2.31171038523e-21, 2.42313415161e-21, 2.53590759014e-21, 2.65001843742e-21,
            2.76545547637e-21, 2.88220844835e-21, 3.00026797575e-21, 3.11962549361e-21, 3.24027318888e-21,
            3.36220394642e-21, 3.48541130074e-21, 3.60988939279e-21, 3.7356329311e-21, 3.86263715686e-21,
            3.99089781236e-21, 4.12041111239e-21, 4.25117371845e-21, 4.38318271516e-21, 4.51643558895e-21,
            4.65093020852e-21, 4.78666480711e-21, 4.92363796621e-21, 5.06184860075e-21, 5.20129594544e-21,
            5.34197954236e-21, 5.48389922948e-21, 5.62705513018e-21, 5.77144764362e-21, 5.9170774359e-21,
            6.06394543192e-21, 6.21205280795e-21, 6.36140098478e-21, 6.51199162141e-21, 6.66382660935e-21,
            6.81690806729e-21, 6.97123833635e-21, 7.12681997563e-21, 7.28365575824e-21, 7.44174866764e-21,
            7.60110189437e-21, 7.76171883308e-21, 7.92360307983e-21, 8.08675842978e-21, 8.25118887504e-21,
            8.41689860281e-21, 8.58389199384e-21, 8.752173621e-21, 8.92174824817e-21, 9.0926208293e-21,
            9.26479650768e-21, 9.43828061539e-21, 9.61307867302e-21, 9.78919638943e-21, 9.96663966183e-21,
            1.01454145759e-20, 1.03255274063e-20, 1.05069846171e-20, 1.06897928622e-20, 1.08739589867e-20,
            1.10594900275e-20, 1.12463932147e-20, 1.14346759725e-20, 1.16243459211e-20, 1.18154108781e-20,
            1.20078788602e-20, 1.22017580851e-20, 1.23970569735e-20, 1.25937841516e-20, 1.27919484529e-20,
            1.29915589212e-20, 1.31926248126e-20, 1.33951555991e-20, 1.35991609708e-20, 1.38046508394e-20,
            1.40116353411e-20, 1.42201248406e-20, 1.44301299338e-20, 1.46416614524e-20, 1.48547304671e-20,
            1.50693482921e-20, 1.5285526489e-20, 1.55032768718e-20, 1.57226115107e-20, 1.59435427376e-20,
            1.61660831506e-20, 1.63902456195e-20, 1.6616043291e-20, 1.68434895946e-20, 1.70725982479e-20,
            1.73033832633e-20, 1.75358589536e-20, 1.77700399393e-20, 1.80059411545e-20, 1.82435778548e-20,
            1.84829656238e-20, 1.87241203814e-20, 1.89670583912e-20, 1.92117962687e-20, 1.94583509899e-20,
            1.97067399002e-20, 1.99569807232e-20, 2.02090915706e-20, 2.04630909515e-20, 2.07189977831e-20,
            2.09768314011e-20, 2.12366115708e-20, 2.14983584983e-20, 2.17620928428e-20, 2.20278357286e-20,
            2.2295608758e-20, 2.2565434025e-20, 2.28373341287e-20, 2.31113321878e-20, 2.33874518561e-20,
            2.36657173374e-20, 2.39461534023e-20, 2.42287854051e-20, 2.4513639301e-20, 2.48007416649e-20,
            2.50901197103e-20, 2.53818013093e-20, 2.56758150136e-20, 2.59721900756e-20, 2.62709564716e-20,
            2.65721449254e-20, 2.68757869323e-20, 2.71819147857e-20, 2.74905616033e-20, 2.78017613558e-20,
            2.81155488957e-20, 2.84319599887e-20, 2.87510313451e-20, 2.90728006545e-20, 2.939730662e-20,
            2.97245889962e-20, 3.00546886272e-20, 3.03876474879e-20, 3.07235087261e-20, 3.10623167078e-20,
            3.14041170641e-20, 3.17489567409e-20, 3.20968840504e-20, 3.24479487265e-20, 3.28022019823e-20,
            3.31596965706e-20, 3.35204868483e-20, 3.38846288435e-20, 3.42521803272e-20, 3.46232008885e-20,
            3.4997752014e-20, 3.53758971719e-20, 3.57577019011e-20, 3.61432339058e-20, 3.65325631548e-20,
            3.69257619879e-20, 3.73229052281e-20, 3.77240703013e-20, 3.81293373632e-20, 3.85387894342e-20,
            3.89525125438e-20, 3.93705958834e-20, 3.97931319704e-20, 4.02202168223e-20, 4.06519501444e-20,
            4.10884355286e-20, 4.15297806682e-20, 4.19760975869e-20, 4.24275028853e-20, 4.28841180055e-20,
            4.3346069516e-20, 4.38134894182e-20, 4.42865154775e-20, 4.47652915804e-20, 4.52499681207e-20,
            4.57407024181e-20, 4.62376591717e-20, 4.67410109528e-20, 4.72509387408e-20, 4.77676325071e-20,
            4.82912918521e-20, 4.88221267023e-20, 4.93603580729e-20, 4.99062189052e-20, 5.04599549866e-20,
            5.10218259653e-20, 5.15921064692e-20, 5.21710873452e-20, 5.2759077033e-20, 5.33564030933e-20,
            5.39634139104e-20, 5.45804805963e-20, 5.52079991245e-20, 5.58463927299e-20, 5.64961146142e-20,
            5.71576510093e-20, 5.7831524655e-20, 5.85182987638e-20, 5.92185815588e-20, 5.99330314883e-20,
            6.06623632468e-20, 6.14073547584e-20, 6.21688553205e-20, 6.29477951501e-20, 6.37451966432e-20,
            6.45621877375e-20, 6.54000178819e-20, 6.62600772633e-20, 6.71439201451e-20, 6.80532934473e-20,
            6.89901720881e-20, 6.99568031586e-20, 7.09557617949e-20, 7.19900227889e-20, 7.30630537391e-20,
            7.41789382663e-20, 7.53425421342e-20, 7.65597421711e-20, 7.78377498634e-20, 7.9185582674e-20,
            8.06147755374e-20, 8.21405027698e-20, 8.37834459783e-20, 8.55731292497e-20, 8.75544596696e-20,
            8.98023880577e-20, 9.24624714212e-20, 9.5919641345e-20, 1.08420217249e-19};

        /** Underlying source of randomness. */
        private final UniformRandomProvider rng;

        /**
         * @param rng Generator of uniformly distributed random numbers.
         */
        ModifiedZigguratExponentialSampler(UniformRandomProvider rng) {
            this.rng = rng;
        }

        /** {@inheritDoc} */
        @Override
        public double sample() {
            final long x = rng.nextLong();
            // Float multiplication squashes these last 8 bits, so they can be used to sample i
            final int i = ((int) x) & 0xff;

            if (i < I_MAX) {
                // Early exit.
                // This branch is called about 0.984374 times per call into createSample.
                // Note: Frequencies have been empirically measured for the first call to
                // createSample; recursion due to retries have been ignored. Frequencies sum to 1.
                return X_I[i] * (x & MAX_INT64);
            }
            // For the first call into createSample:
            // Recursion frequency = 0.000515560
            // Overhang frequency  = 0.0151109
            final int j = expSampleA();
            return j == 0 ? X_0 + sample() : expOverhang(j);
        }

        /**
         * Alias sampling.
         * See http://scorevoting.net/WarrenSmithPages/homepage/sampling.abs
         *
         * @return the alias
         */
        private int expSampleA() {
            final long x = rng.nextLong();
            // j <- I(0, 256)
            final int j = ((int) x) & 0xff;
            return x >= IPMF[j] ? MAP[j] & 0xff : j;
        }

        /**
         * Draws a PRN from overhang.
         *
         * @param j Index j (must be {@code > 0})
         * @return the sample
         */
        private double expOverhang(int j) {
            // To sample a unit right-triangle:
            // U_x <- min(U_1, U_2)
            // distance <- | U_1 - U_2 |
            // U_y <- 1 - (U_x + distance)
            long ux = randomInt63();
            long uDistance = randomInt63() - ux;
            if (uDistance < 0) {
                uDistance = -uDistance;
                ux -= uDistance;
            }
            // _FAST_PRNG_SAMPLE_X(xj, ux)
            final double x = fastPrngSampleX(j, ux);
            if (uDistance >= IE_MAX) {
                // Frequency (per call into createSample): 0.0136732
                // Frequency (per call into expOverhang):  0.904857
                // Early Exit: x < y - epsilon
                return x;
            }
            // Frequency per call into createSample:
            // Return    = 0.00143769
            // Recursion = 1e-8
            // Frequency per call into expOverhang:
            // Return    = 0.0951426
            // Recursion = 6.61774e-07

            // _FAST_PRNG_SAMPLE_Y(j, pow(2, 63) - (ux + uDistance))
            // Long.MIN_VALUE is used as an unsigned int with value 2^63:
            // uy = Long.MIN_VALUE - (ux + uDistance)
            return fastPrngSampleY(j, Long.MIN_VALUE - (ux + uDistance)) <= Math.exp(-x) ? x : expOverhang(j);
        }

        /**
         * Return a positive long in {@code [0, 2^63)}.
         *
         * @return the long
         */
        private long randomInt63() {
            return rng.nextLong() & MAX_INT64;
        }

        /**
         * Auxilary function to see if rejection sampling is required in the overhang.
         * See Fig. 2 in the main text.
         *
         * @param j j
         * @param ux ux
         * @return the sample
         */
        private static double fastPrngSampleX(int j, long ux) {
            return X_I[j] * TWO_POW_63 + (X_I[j - 1] - X_I[j]) * ux;
        }

        /**
         * Auxilary function to see if rejection sampling is required in the overhang.
         * See Fig. 2 in the main text.
         *
         * @param i i
         * @param uy uy
         * @return the sample
         */
        private static double fastPrngSampleY(int i, long uy) {
            return Y[i - 1] * TWO_POW_63 + (Y[i] - Y[i - 1]) * uy;
        }

        /**
         * Helper function to convert {@code int} values to bytes using a narrowing primitive conversion.
         *
         * @param values Integer values.
         * @return the bytes
         */
        private static byte[] toBytes(int[] values) {
            final byte[] bytes = new byte[values.length];
            for (int i = 0; i < bytes.length; i++) {
                bytes[i] = (byte) values[i];
            }
            return bytes;
        }
    }

    /**
     * Throw an illegal state exception for the unknown parameter.
     *
     * @param parameter Parameter name
     */
    private static void throwIllegalStateException(String parameter) {
        throw new IllegalStateException("Unknown: " + parameter);
    }

    /**
     * Baseline for the JMH timing overhead for production of an {@code double} value.
     *
     * @return the {@code double} value
     */
    @Benchmark
    public double baseline() {
        return value;
    }

    /**
     * Benchmark methods for obtaining an index from the lower bits of a long.
     *
     * <p>Note: This is disabled as there is no measurable difference between methods.
     *
     * @param sources Source of randomness.
     * @return the sample value
     */
    //@Benchmark
    public int getIndex(IndexSources sources) {
        return sources.getSampler().sample();
    }

    /**
     * Benchmark methods for obtaining an unsigned long.
     *
     * <p>Note: This is disabled as there is no measurable difference between methods.
     *
     * @param sources Source of randomness.
     * @return the sample value
     */
    //@Benchmark
    public long getUnsignedLong(LongSources sources) {
        return sources.getSampler().sample();
    }

    /**
     * Run the sampler.
     *
     * @param sources Source of randomness.
     * @return the sample value
     */
    @Benchmark
    public double sample(Sources sources) {
        return sources.getSampler().sample();
    }

    /**
     * Run the sampler to generate a number of samples sequentially.
     *
     * @param sources Source of randomness.
     * @return the sample value
     */
    @Benchmark
    public double sequentialSample(SequentialSources sources) {
        return sources.getSampler().sample();
    }
}
