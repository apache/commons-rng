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

import org.apache.commons.math3.util.FastMath;
import org.apache.commons.rng.UniformRandomProvider;
import org.apache.commons.rng.sampling.distribution.ContinuousSampler;
import org.apache.commons.rng.sampling.distribution.DiscreteSampler;
import org.apache.commons.rng.sampling.distribution.LongSampler;
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
import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;
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

    // Production versions

    /** The name for a copy of the {@link ZigguratNormalizedGaussianSampler} with a table of size 128.
     * This matches the version in Commons RNG release v1.1 to 1.3. */
    static final String GAUSSIAN_128 = "Gaussian128";
    /** The name for the {@link ZigguratNormalizedGaussianSampler} (table of size 256).
     * This is the version in Commons RNG release v1.4+. */
    static final String GAUSSIAN_256 = "Gaussian256";
    /** The name for the {@link org.apache.commons.rng.sampling.distribution.ZigguratSampler.NormalizedGaussian}. */
    static final String MOD_GAUSSIAN = "ModGaussian";
    /** The name for the {@link org.apache.commons.rng.sampling.distribution.ZigguratSampler.Exponential}. */
    static final String MOD_EXPONENTIAL = "ModExponential";

    // Testing versions

    /** The name for the {@link ZigguratExponentialSampler} with a table of size 256.
     * This is an exponential sampler using Marsaglia's ziggurat method. */
    static final String EXPONENTIAL = "Exponential";

    /** The name for the {@link ModifiedZigguratNormalizedGaussianSampler}.
     * This is a base implementation of McFarland's ziggurat method. */
    static final String MOD_GAUSSIAN2 = "ModGaussian2";
    /** The name for the {@link ModifiedZigguratNormalizedGaussianSamplerSimpleOverhangs}. */
    static final String MOD_GAUSSIAN_SIMPLE_OVERHANGS = "ModGaussianSimpleOverhangs";
    /** The name for the {@link ModifiedZigguratNormalizedGaussianSamplerInlining}. */
    static final String MOD_GAUSSIAN_INLINING = "ModGaussianInlining";
    /** The name for the {@link ModifiedZigguratNormalizedGaussianSamplerInliningShift}. */
    static final String MOD_GAUSSIAN_INLINING_SHIFT = "ModGaussianInliningShift";
    /** The name for the {@link ModifiedZigguratNormalizedGaussianSamplerInliningSimpleOverhangs}. */
    static final String MOD_GAUSSIAN_INLINING_SIMPLE_OVERHANGS = "ModGaussianInliningSimpleOverhangs";
    /** The name for the {@link ModifiedZigguratNormalizedGaussianSamplerIntMap}. */
    static final String MOD_GAUSSIAN_INT_MAP = "ModGaussianIntMap";
    /** The name for the {@link ModifiedZigguratNormalizedGaussianSamplerEMaxTable}. */
    static final String MOD_GAUSSIAN_E_MAX_TABLE = "ModGaussianEMaxTable";
    /** The name for the {@link ModifiedZigguratNormalizedGaussianSamplerEMax2}. */
    static final String MOD_GAUSSIAN_E_MAX_2 = "ModGaussianEMax2";
    /** The name for the {@link ModifiedZigguratNormalizedGaussianSampler512} using a table size of 512. */
    static final String MOD_GAUSSIAN_512 = "ModGaussian512";

    /** The name for the {@link ModifiedZigguratExponentialSampler}.
     * This is a base implementation of McFarland's ziggurat method. */
    static final String MOD_EXPONENTIAL2 = "ModExponential2";
    /** The name for the {@link ModifiedZigguratExponentialSamplerSimpleOverhangs}. */
    static final String MOD_EXPONENTIAL_SIMPLE_OVERHANGS = "ModExponentialSimpleOverhangs";
    /** The name for the {@link ModifiedZigguratExponentialSamplerInlining}. */
    static final String MOD_EXPONENTIAL_INLINING = "ModExponentialInlining";
    /** The name for the {@link ModifiedZigguratExponentialSamplerLoop}. */
    static final String MOD_EXPONENTIAL_LOOP = "ModExponentialLoop";
    /** The name for the {@link ModifiedZigguratExponentialSamplerLoop2}. */
    static final String MOD_EXPONENTIAL_LOOP2 = "ModExponentialLoop2";
    /** The name for the {@link ModifiedZigguratExponentialSamplerRecursion}. */
    static final String MOD_EXPONENTIAL_RECURSION = "ModExponentialRecursion";
    /** The name for the {@link ModifiedZigguratExponentialSamplerIntMap}. */
    static final String MOD_EXPONENTIAL_INT_MAP = "ModExponentialIntMap";
    /** The name for the {@link ModifiedZigguratExponentialSamplerEMaxTable}. */
    static final String MOD_EXPONENTIAL_E_MAX_TABLE = "ModExponentialEmaxTable";
    /** The name for the {@link ModifiedZigguratExponentialSamplerEMax2}. */
    static final String MOD_EXPONENTIAL_E_MAX_2 = "ModExponentialEmax2";
    /** The name for the {@link ModifiedZigguratExponentialSampler512} using a table size of 512. */
    static final String MOD_EXPONENTIAL_512 = "ModExponential512";

    /** Mask to create an unsigned long from a signed long. */
    private static final long MAX_INT64 = 0x7fffffffffffffffL;
    /** 2^53. */
    private static final double TWO_POW_63 = 0x1.0p63;

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
                sampler = () -> {
                    final long x = rng.nextLong();
                    return ((int) x) & 0xff;
                };
            } else if ("MaskCast".equals(method)) {
                sampler = () -> {
                    final long x = rng.nextLong();
                    return (int) (x & 0xff);
                };
            } else {
                throwIllegalStateException(method);
            }
        }
    }

    /**
     * Defines method to use for creating an index values from a random long and comparing
     * it to an {@code int} limit.
     */
    @State(Scope.Benchmark)
    public static class IndexCompareSources {
        /** The method to compare the index against the limit. */
        @Param({"CastMaskIntCompare", "MaskCastIntCompare", "MaskLongCompareCast"})
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
            // The limit:
            // exponential = 252
            // gaussian = 253
            final int limit = 253;
            // Use a fast generator:
            final UniformRandomProvider rng = RandomSource.XO_RO_SHI_RO_128_PP.create();
            if ("CastMaskIntCompare".equals(method)) {
                sampler = () -> {
                    final long x = rng.nextLong();
                    final int i = ((int) x) & 0xff;
                    if (i < limit) {
                        return i;
                    }
                    return 0;
                };
            } else if ("MaskCastIntCompare".equals(method)) {
                sampler = () -> {
                    final long x = rng.nextLong();
                    final int i = (int) (x & 0xff);
                    if (i < limit) {
                        return i;
                    }
                    return 0;
                };
            } else if ("MaskLongCompareCast".equals(method)) {
                sampler = () -> {
                    final long x = rng.nextLong();
                    final long i = x & 0xff;
                    if (i < limit) {
                        return (int) i;
                    }
                    return 0;
                };
            } else {
                throwIllegalStateException(method);
            }
        }
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
            // Use a fast generator:
            // Here we use a simple linear congruential generator
            // which should have constant speed and a random upper bit.
            final long[] s = {ThreadLocalRandom.current().nextLong()};
            if ("Mask".equals(method)) {
                sampler = () -> {
                    final long x = s[0];
                    s[0] = updateLCG(x);
                    return x & Long.MAX_VALUE;
                };
            } else if ("Shift".equals(method)) {
                sampler = () -> {
                    final long x = s[0];
                    s[0] = updateLCG(x);
                    return x >>> 1;
                };
            } else {
                throwIllegalStateException(method);
            }
        }
    }

    /**
     * Defines method to use for interpolating the X or Y tables from unsigned {@code long} values.
     */
    @State(Scope.Benchmark)
    public static class InterpolationSources {
        /** The method to perform interpolation. */
        @Param({"U1", "1minusU2", "U_1minusU"})
        private String method;

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
            // Use a fast generator:
            final UniformRandomProvider rng = RandomSource.XO_RO_SHI_RO_128_PP.create();
            // Get an x table. This is length 254.
            // We will sample from this internally to avoid index out-of-bounds issues.
            final int start = 42;
            final double[] x = Arrays.copyOfRange(
                    ModifiedZigguratNormalizedGaussianSampler.getX(), start, start + 129);
            final int mask = 127;
            if ("U1".equals(method)) {
                sampler = () -> {
                    final long u = rng.nextLong();
                    final int j = 1 + (((int) u) & mask);
                    // double multiply
                    // double add
                    // double subtract
                    // long convert to double
                    // double multiply
                    // int subtract
                    // three index loads
                    // Same as sampleX(x, j, u)
                    return x[j] * TWO_POW_63 + (x[j - 1] - x[j]) * u;
                };
            } else if ("1minusU2".equals(method)) {
                sampler = () -> {
                    final long u = rng.nextLong();
                    final int j = 1 + (((int) u) & mask);
                    // Since u is in [0, 2^63) to create (1 - u) using Long.MIN_VALUE
                    // as an unsigned integer of 2^63.
                    // double multiply
                    // double add
                    // double subtract
                    // long subtract
                    // long convert to double
                    // double multiply
                    // int subtract * 2
                    // three index loads
                    // Same as sampleY(x, j, u)
                    return x[j - 1] * TWO_POW_63 + (x[j] - x[j - 1]) * (Long.MIN_VALUE - u);
                };
            } else if ("U_1minusU".equals(method)) {
                sampler = () -> {
                    final long u = rng.nextLong();
                    final int j = 1 + (((int) u) & mask);
                    // Interpolation between bounds a and b using:
                    // a * u + b * (1 - u) == b + u * (a - b)
                    // long convert to double
                    // double multiply
                    // double add
                    // long subtract
                    // long convert to double
                    // double multiply
                    // int subtract
                    // two index loads
                    return x[j - 1] * u + x[j] * (Long.MIN_VALUE - u);
                };
            } else {
                throwIllegalStateException(method);
            }
        }
    }

    /**
     * Defines method to extract a sign bit from a {@code long} value.
     */
    @State(Scope.Benchmark)
    public static class SignBitSources {
        /** The method to obtain the sign bit. */
        @Param({"ifNegative", "ifSignBit", "ifBit", "bitSubtract", "signBitSubtract"})
        private String method;

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
            // Use a fast generator:
            final UniformRandomProvider rng = RandomSource.XO_RO_SHI_RO_128_PP.create();

            if ("ifNegative".equals(method)) {
                sampler = () -> {
                    final long x = rng.nextLong();
                    return x < 0 ? -1.0 : 1.0;
                };
            } else if ("ifSignBit".equals(method)) {
                sampler = () -> {
                    final long x = rng.nextLong();
                    return (x >>> 63) == 0 ? -1.0 : 1.0;
                };
            } else if ("ifBit".equals(method)) {
                sampler = () -> {
                    final long x = rng.nextLong();
                    return (x & 0x100) == 0 ? -1.0 : 1.0;
                };
            } else if ("bitSubtract".equals(method)) {
                sampler = () -> {
                    final long x = rng.nextLong();
                    return ((x >>> 7) & 0x2) - 1.0;
                };
            } else if ("signBitSubtract".equals(method)) {
                sampler = () -> {
                    final long x = rng.nextLong();
                    return ((x >>> 62) & 0x2) - 1.0;
                };
            } else {
                throwIllegalStateException(method);
            }
        }
    }

    /**
     * Defines method to use for computing {@code exp(x)} when {@code -8 <= x <= 0}.
     */
    @State(Scope.Benchmark)
    public static class ExpSources {
        /** The method to compute exp(x). */
        @Param({"noop", "Math.exp", "FastMath.exp"})
        private String method;

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
            // Use a fast generator:
            // Here we use a simple linear congruential generator
            // which should have constant speed and a random upper bit.
            final long[] s = {ThreadLocalRandom.current().nextLong()};
            // From a random long we create a value in (-8, 0] by using the
            // method to generate [0, 1) and then multiplying by -8:
            // (x >>> 11) * 0x1.0p-53 * -0x1.0p3
            if ("noop".equals(method)) {
                sampler = () -> {
                    final long x = s[0];
                    s[0] = updateLCG(x);
                    return (x >>> 11) * -0x1.0p-50;
                };
            } else if ("Math.exp".equals(method)) {
                sampler = () -> {
                    final long x = s[0];
                    s[0] = updateLCG(x);
                    return Math.exp((x >>> 11) * -0x1.0p-50);
                };
            } else if ("FastMath.exp".equals(method)) {
                sampler = () -> {
                    final long x = s[0];
                    s[0] = updateLCG(x);
                    return FastMath.exp((x >>> 11) * -0x1.0p-50);
                };
            } else {
                throwIllegalStateException(method);
            }
        }
    }

    /**
     * Update the state of the linear congruential generator.
     * <pre>
     *  s = m*s + a
     * </pre>
     *
     * <p>This can be used when the upper bits of the long are important.
     * The lower bits will not be very random. Each bit has a period of
     * 2^p where p is the bit significance.
     *
     * @param state the state
     * @return the new state
     */
    private static long updateLCG(long state) {
        // m is the multiplier used for the LCG component of the JDK 17 generators.
        // a can be any odd number.
        // Use the golden ratio from a SplitMix64 generator.
        return 0xd1342543de82ef95L * state + 0x9e3779b97f4a7c15L;

    }

    /**
     * The samplers to use for testing the ziggurat method.
     */
    @State(Scope.Benchmark)
    public abstract static class Sources {
        /**
         * The sampler type.
         */
        @Param({// Production versions
                GAUSSIAN_128, GAUSSIAN_256, MOD_GAUSSIAN, MOD_EXPONENTIAL,
                // Experimental Marsaglia exponential ziggurat sampler
                EXPONENTIAL,
                // Experimental McFarland Gaussian ziggurat samplers
                MOD_GAUSSIAN2, MOD_GAUSSIAN_SIMPLE_OVERHANGS,
                MOD_GAUSSIAN_INLINING, MOD_GAUSSIAN_INLINING_SHIFT,
                MOD_GAUSSIAN_INLINING_SIMPLE_OVERHANGS, MOD_GAUSSIAN_INT_MAP,
                MOD_GAUSSIAN_E_MAX_TABLE, MOD_GAUSSIAN_E_MAX_2, MOD_GAUSSIAN_512,
                // Experimental McFarland Gaussian ziggurat samplers
                MOD_EXPONENTIAL2, MOD_EXPONENTIAL_SIMPLE_OVERHANGS, MOD_EXPONENTIAL_INLINING,
                MOD_EXPONENTIAL_LOOP, MOD_EXPONENTIAL_LOOP2,
                MOD_EXPONENTIAL_RECURSION, MOD_EXPONENTIAL_INT_MAP,
                MOD_EXPONENTIAL_E_MAX_TABLE, MOD_EXPONENTIAL_E_MAX_2, MOD_EXPONENTIAL_512})
        protected String type;

        /**
         * Creates the sampler.
         *
         * @param type Type of sampler
         * @param rng RNG
         * @return the sampler
         */
        static ContinuousSampler createSampler(String type, UniformRandomProvider rng) {
            if (GAUSSIAN_128.equals(type)) {
                return new ZigguratNormalizedGaussianSampler128(rng);
            } else if (GAUSSIAN_256.equals(type)) {
                return ZigguratNormalizedGaussianSampler.of(rng);
            } else if (MOD_GAUSSIAN.equals(type)) {
                return ZigguratSampler.NormalizedGaussian.of(rng);
            } else if (MOD_EXPONENTIAL.equals(type)) {
                return ZigguratSampler.Exponential.of(rng);
            } else if (EXPONENTIAL.equals(type)) {
                return new ZigguratExponentialSampler(rng);
            } else if (MOD_GAUSSIAN2.equals(type)) {
                return new ModifiedZigguratNormalizedGaussianSampler(rng);
            } else if (MOD_GAUSSIAN_SIMPLE_OVERHANGS.equals(type)) {
                return new ModifiedZigguratNormalizedGaussianSamplerSimpleOverhangs(rng);
            } else if (MOD_GAUSSIAN_INLINING.equals(type)) {
                return new ModifiedZigguratNormalizedGaussianSamplerInlining(rng);
            } else if (MOD_GAUSSIAN_INLINING_SHIFT.equals(type)) {
                return new ModifiedZigguratNormalizedGaussianSamplerInliningShift(rng);
            } else if (MOD_GAUSSIAN_INLINING_SIMPLE_OVERHANGS.equals(type)) {
                return new ModifiedZigguratNormalizedGaussianSamplerInliningSimpleOverhangs(rng);
            } else if (MOD_GAUSSIAN_INT_MAP.equals(type)) {
                return new ModifiedZigguratNormalizedGaussianSamplerIntMap(rng);
            } else if (MOD_GAUSSIAN_E_MAX_TABLE.equals(type)) {
                return new ModifiedZigguratNormalizedGaussianSamplerEMaxTable(rng);
            } else if (MOD_GAUSSIAN_E_MAX_2.equals(type)) {
                return new ModifiedZigguratNormalizedGaussianSamplerEMax2(rng);
            } else if (MOD_GAUSSIAN_512.equals(type)) {
                return new ModifiedZigguratNormalizedGaussianSampler512(rng);
            } else if (MOD_EXPONENTIAL2.equals(type)) {
                return new ModifiedZigguratExponentialSampler(rng);
            } else if (MOD_EXPONENTIAL_SIMPLE_OVERHANGS.equals(type)) {
                return new ModifiedZigguratExponentialSamplerSimpleOverhangs(rng);
            } else if (MOD_EXPONENTIAL_INLINING.equals(type)) {
                return new ModifiedZigguratExponentialSamplerInlining(rng);
            } else if (MOD_EXPONENTIAL_LOOP.equals(type)) {
                return new ModifiedZigguratExponentialSamplerLoop(rng);
            } else if (MOD_EXPONENTIAL_LOOP2.equals(type)) {
                return new ModifiedZigguratExponentialSamplerLoop2(rng);
            } else if (MOD_EXPONENTIAL_RECURSION.equals(type)) {
                return new ModifiedZigguratExponentialSamplerRecursion(rng);
            } else if (MOD_EXPONENTIAL_INT_MAP.equals(type)) {
                return new ModifiedZigguratExponentialSamplerIntMap(rng);
            } else if (MOD_EXPONENTIAL_E_MAX_TABLE.equals(type)) {
                return new ModifiedZigguratExponentialSamplerEMaxTable(rng);
            } else if (MOD_EXPONENTIAL_E_MAX_2.equals(type)) {
                return new ModifiedZigguratExponentialSamplerEMax2(rng);
            } else if (MOD_EXPONENTIAL_512.equals(type)) {
                return new ModifiedZigguratExponentialSampler512(rng);
            } else {
                throw new IllegalStateException("Unknown type: " + type);
            }
        }
    }

    /**
     * The samplers to use for testing the ziggurat method with single sample generation.
     * Defines the RandomSource and the sampler type.
     */
    @State(Scope.Benchmark)
    public static class SingleSources extends Sources {
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
            sampler = createSampler(type, rng);
        }
    }

    /**
     * The samplers to use for testing the ziggurat method with sequential sample generation.
     * Defines the RandomSource and the sampler type.
     *
     * <p>This specifically targets repeat calls to the same sampler.
     * Performance should scale linearly with the size. A plot of size against time
     * can identify outliers and should allow ranking of different methods.
     *
     * <p>Note: Testing using a single call to the sampler may return different relative
     * performance of the samplers than when testing using multiple calls. This is due to the
     * single calls being performed multiple times by the JMH framework rather than a
     * single block of code. Rankings should be consistent and the optimal sampler method
     * chosen using both sets of results.
     */
    @State(Scope.Benchmark)
    public static class SequentialSources extends Sources {
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

        /** The size. */
        @Param({"1", "2", "4", "8", "16", "32", "64"})
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
            final ContinuousSampler s = createSampler(type, rng);
            sampler = createSequentialSampler(size, s);
        }

        /**
         * Creates the sampler for the specified number of samples.
         *
         * @param size the size
         * @param s the sampler to create the samples
         * @return the sampler
         */
        private static ContinuousSampler createSequentialSampler(int size, ContinuousSampler s) {
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
                delegate.sample();
                return delegate.sample();
            }
        }

        /**
         * Create N samples from the sampler.
         */
        static class SizeNSampler extends SizeSampler {
            /** The number of samples minus 1. */
            private final int sizeM1;

            /**
             * @param delegate the sampler to create the samples
             * @param size the size
             */
            SizeNSampler(ContinuousSampler delegate, int size) {
                super(delegate);
                if (size < 1) {
                    throw new IllegalArgumentException("Size must be above zero: " + size);
                }
                this.sizeM1 = size - 1;
            }

            @Override
            public double sample() {
                for (int i = sizeM1; i != 0; i--) {
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
     * <p>This class uses the same tables as the production version
     * {@link org.apache.commons.rng.sampling.distribution.ZigguratSampler.NormalizedGaussian}
     * with the overhang sampling matching the reference c implementation. Methods and members
     * are protected to allow the implementation to be modified in sub-classes.
     *
     * @see <a href="https://www.tandfonline.com/doi/abs/10.1080/00949655.2015.1060234">
     * McFarland (2016) JSCS 86, 1281-1294</a>
     */
    static class ModifiedZigguratNormalizedGaussianSampler implements ContinuousSampler {
        // Ziggurat volumes:
        // Inside the layers              = 98.8281%  (253/256)
        // Fraction outside the layers:
        // concave overhangs              = 76.1941%
        // inflection overhang            =  0.1358%
        // convex overhangs               = 21.3072%
        // tail                           =  2.3629%

        /** The number of layers in the ziggurat. Maximum i value for early exit. */
        protected static final int I_MAX = 253;
        /** The point where the Gaussian switches from convex to concave.
         * This is the largest value of X[j] below 1. */
        protected static final int J_INFLECTION = 204;
        /** Maximum epsilon distance of convex pdf(x) above the hypotenuse value for early rejection.
         * Equal to approximately 0.2460 scaled by 2^63. This is negated on purpose as the
         * distance for a point (x,y) above the hypotenuse is negative:
         * {@code (|d| < max) == (d >= -max)}. */
        protected static final long CONVEX_E_MAX = -2269182951627976004L;
        /** Maximum distance of concave pdf(x) below the hypotenuse value for early exit.
         * Equal to approximately 0.08244 scaled by 2^63. */
        protected static final long CONCAVE_E_MAX = 760463704284035184L;
        /** Beginning of tail. Equal to X[0] * 2^63. */
        protected static final double X_0 = 3.6360066255009455861;
        /** 1/X_0. Used for tail sampling. */
        protected static final double ONE_OVER_X_0 = 1.0 / X_0;

        /** The alias map. An integer in [0, 255] stored as a byte to save space. */
        protected static final byte[] MAP = toBytes(
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
        protected static final long[] IPMF = {9223372036854775296L, 1100243796534090752L, 7866600928998383104L,
            6788754710675124736L, 9022865200181688320L, 6522434035205502208L, 4723064097360024576L,
            3360495653216416000L, 2289663232373870848L, 1423968905551920384L, 708364817827798016L, 106102487305601280L,
            -408333464665794560L, -853239722779025152L, -1242095211825521408L, -1585059631105762048L,
            -1889943050287169024L, -2162852901990669824L, -2408637386594511104L, -2631196530262954496L,
            -2833704942520925696L, -3018774289025787392L, -3188573753472222208L, -3344920681707410944L,
            -3489349705062150656L, -3623166100042179584L, -3747487436868335360L, -3863276422712173824L,
            -3971367044063130880L, -4072485557029824000L, -4167267476830916608L, -4256271432240159744L,
            -4339990541927306752L, -4418861817133802240L, -4493273980372377088L, -4563574004462246656L,
            -4630072609770453760L, -4693048910430964992L, -4752754358862894848L, -4809416110052769536L,
            -4863239903586985984L, -4914412541515875840L, -4963104028439161088L, -5009469424769119232L,
            -5053650458856559360L, -5095776932695077632L, -5135967952544929024L, -5174333008451230720L,
            -5210972924952654336L, -5245980700100460288L, -5279442247516297472L, -5311437055462369280L,
            -5342038772315650560L, -5371315728843297024L, -5399331404632512768L, -5426144845448965120L,
            -5451811038519422464L, -5476381248265593088L, -5499903320558339072L, -5522421955752311296L,
            -5543978956085263616L, -5564613449659060480L, -5584362093436146432L, -5603259257517428736L,
            -5621337193070986240L, -5638626184974132224L, -5655154691220933888L, -5670949470294763008L,
            -5686035697601807872L, -5700437072199152384L, -5714175914219812352L, -5727273255295220992L,
            -5739748920271997440L, -5751621603810412032L, -5762908939773946112L, -5773627565915007744L,
            -5783793183152377600L, -5793420610475628544L, -5802523835894661376L, -5811116062947570176L,
            -5819209754516120832L, -5826816672854571776L, -5833947916825278208L, -5840613956570608128L,
            -5846824665591763456L, -5852589350491075328L, -5857916778480726528L, -5862815203334800384L,
            -5867292388935742464L, -5871355631762284032L, -5875011781262890752L, -5878267259039093760L,
            -5881128076579883520L, -5883599852028851456L, -5885687825288565248L, -5887396872144963840L,
            -5888731517955042304L, -5889695949247728384L, -5890294025706689792L, -5890529289910829568L,
            -5890404977675987456L, -5889924026487208448L, -5889089083913555968L, -5887902514965209344L,
            -5886366408898372096L, -5884482585690639872L, -5882252601321090304L, -5879677752995027712L,
            -5876759083794175232L, -5873497386318840832L, -5869893206505510144L, -5865946846617024256L,
            -5861658367354159104L, -5857027590486131456L, -5852054100063428352L, -5846737243971504640L,
            -5841076134082373632L, -5835069647234580480L, -5828716424754549248L, -5822014871949021952L,
            -5814963157357531648L, -5807559211080072192L, -5799800723447229952L, -5791685142338073344L,
            -5783209670985158912L, -5774371264582489344L, -5765166627072226560L, -5755592207057667840L,
            -5745644193442049280L, -5735318510777133824L, -5724610813433666560L, -5713516480340333056L,
            -5702030608556698112L, -5690148005851018752L, -5677863184109371904L, -5665170350903313408L,
            -5652063400924580608L, -5638535907000141312L, -5624581109999480320L, -5610191908627599872L,
            -5595360848093632768L, -5580080108034218752L, -5564341489875549952L, -5548136403221394688L,
            -5531455851545399296L, -5514290416593586944L, -5496630242226406656L, -5478465016761742848L,
            -5459783954986665216L, -5440575777891777024L, -5420828692432397824L, -5400530368638773504L,
            -5379667916699401728L, -5358227861294116864L, -5336196115274292224L, -5313557951078385920L,
            -5290297970633451520L, -5266400072915222272L, -5241847420214015744L, -5216622401043726592L,
            -5190706591719534080L, -5164080714589203200L, -5136724594099067136L, -5108617109269313024L,
            -5079736143458214912L, -5050058530461741312L, -5019559997031891968L, -4988215100963582976L,
            -4955997165645491968L, -4922878208652041728L, -4888828866780320000L, -4853818314258475776L,
            -4817814175855180032L, -4780782432601701888L, -4742687321746719232L, -4703491227581444608L,
            -4663154564978699264L, -4621635653358766336L, -4578890580370785792L, -4534873055659683584L,
            -4489534251700611840L, -4442822631898829568L, -4394683764809104128L, -4345060121983362560L,
            -4293890858708922880L, -4241111576153830144L, -4186654061692619008L, -4130446006804747776L,
            -4072410698657718784L, -4012466683838401024L, -3950527400305017856L, -3886500774061896704L,
            -3820288777467837184L, -3751786943594897664L, -3680883832433527808L, -3607460442623922176L,
            -3531389562483324160L, -3452535052891361792L, -3370751053395887872L, -3285881101633968128L,
            -3197757155301365504L, -3106198503156485376L, -3011010550911937280L, -2911983463883580928L,
            -2808890647470271744L, -2701487041141149952L, -2589507199690603520L, -2472663129329160192L,
            -2350641842139870464L, -2223102583770035200L, -2089673683684728576L, -1949948966090106880L,
            -1803483646855993856L, -1649789631480328192L, -1488330106139747584L, -1318513295725618176L,
            -1139685236927327232L, -951121376596854784L, -752016768184775936L, -541474585642866432L,
            -318492605725778432L, -81947227249193216L, 169425512612864512L, 437052607232193536L, 722551297568809984L,
            1027761939299714304L, 1354787941622770432L, 1706044619203941632L, 2084319374409574144L,
            2492846399593711360L, 2935400169348532480L, 3416413484613111552L, 3941127949860576256L,
            4515787798793437952L, 5147892401439714304L, 5846529325380406016L, 6622819682216655360L,
            7490522659874166016L, 8466869998277892096L, 8216968526387345408L, 4550693915488934656L,
            7628019504138977280L, 6605080500908005888L, 7121156327650272512L, 2484871780331574272L,
            7179104797032803328L, 7066086283830045440L, 1516500120817362944L, 216305945438803456L, 6295963418525324544L,
            2889316805630113280L, -2712587580533804032L, 6562498853538167040L, 7975754821147501312L,
            -9223372036854775808L, -9223372036854775808L};
        /**
         * The precomputed ziggurat lengths, denoted X_i in the main text. X_i = length of
         * ziggurat layer i. Values have been scaled by 2^-63.
         */
        protected static final double[] X = {3.9421662825398133E-19, 3.720494500411901E-19, 3.582702448062868E-19,
            3.480747623654025E-19, 3.3990177171882136E-19, 3.330377836034014E-19, 3.270943881761755E-19,
            3.21835771324951E-19, 3.171075854184043E-19, 3.1280307407034065E-19, 3.088452065580402E-19,
            3.051765062410735E-19, 3.01752902925846E-19, 2.985398344070532E-19, 2.9550967462801797E-19,
            2.9263997988491663E-19, 2.8991225869977476E-19, 2.873110878022629E-19, 2.8482346327101335E-19,
            2.824383153519439E-19, 2.801461396472703E-19, 2.7793871261807797E-19, 2.758088692141121E-19,
            2.737503269830876E-19, 2.7175754543391047E-19, 2.6982561247538484E-19, 2.6795015188771505E-19,
            2.6612724730440033E-19, 2.6435337927976633E-19, 2.626253728202844E-19, 2.609403533522414E-19,
            2.5929570954331E-19, 2.5768906173214726E-19, 2.561182349771961E-19, 2.545812359339336E-19,
            2.530762329237246E-19, 2.51601538677984E-19, 2.501555953364619E-19, 2.487369613540316E-19,
            2.4734430003079206E-19, 2.4597636942892726E-19, 2.446320134791245E-19, 2.4331015411139206E-19,
            2.4200978427132955E-19, 2.407299617044588E-19, 2.3946980340903347E-19, 2.3822848067252674E-19,
            2.37005214619318E-19, 2.357992722074133E-19, 2.346099626206997E-19, 2.3343663401054455E-19,
            2.322786705467384E-19, 2.3113548974303765E-19, 2.300065400270424E-19, 2.2889129852797606E-19,
            2.2778926905921897E-19, 2.266999802752732E-19, 2.2562298398527416E-19, 2.245578536072726E-19,
            2.235041827493391E-19, 2.2246158390513294E-19, 2.214296872529625E-19, 2.2040813954857555E-19,
            2.19396603102976E-19, 2.183947548374962E-19, 2.1740228540916853E-19, 2.164188984001652E-19,
            2.1544430956570613E-19, 2.1447824613540345E-19, 2.1352044616350571E-19, 2.1257065792395107E-19,
            2.1162863934653125E-19, 2.1069415749082026E-19, 2.0976698805483467E-19, 2.0884691491567363E-19,
            2.0793372969963634E-19, 2.0702723137954107E-19, 2.061272258971713E-19, 2.0523352580895635E-19,
            2.0434594995315797E-19, 2.0346432313698148E-19, 2.0258847584216418E-19, 2.0171824394771313E-19,
            2.008534684685753E-19, 1.9999399530912015E-19, 1.9913967503040585E-19, 1.9829036263028144E-19,
            1.9744591733545175E-19, 1.9660620240469857E-19, 1.9577108494251485E-19, 1.9494043572246307E-19,
            1.941141290196216E-19, 1.9329204245152935E-19, 1.9247405682708168E-19, 1.9166005600287074E-19,
            1.9084992674649826E-19, 1.900435586064234E-19, 1.8924084378793725E-19, 1.8844167703488436E-19,
            1.8764595551677749E-19, 1.868535787209745E-19, 1.8606444834960934E-19, 1.8527846822098793E-19,
            1.8449554417517928E-19, 1.8371558398354868E-19, 1.8293849726199566E-19, 1.8216419538767393E-19,
            1.8139259141898448E-19, 1.8062360001864453E-19, 1.7985713737964743E-19, 1.7909312115393845E-19,
            1.78331470383642E-19, 1.7757210543468428E-19, 1.7681494793266395E-19, 1.760599207008314E-19,
            1.753069477000441E-19, 1.7455595397057217E-19, 1.7380686557563475E-19, 1.7305960954655264E-19,
            1.7231411382940904E-19, 1.7157030723311378E-19, 1.7082811937877138E-19, 1.7008748065025788E-19,
            1.6934832214591352E-19, 1.686105756312635E-19, 1.6787417349268046E-19, 1.6713904869190636E-19,
            1.6640513472135291E-19, 1.6567236556010242E-19, 1.6494067563053266E-19, 1.6420999975549115E-19,
            1.6348027311594532E-19, 1.627514312090366E-19, 1.6202340980646725E-19, 1.6129614491314931E-19,
            1.605695727260459E-19, 1.598436295931348E-19, 1.591182519724249E-19, 1.5839337639095554E-19,
            1.57668939403708E-19, 1.569448775523589E-19, 1.562211273238026E-19, 1.554976251083707E-19,
            1.547743071576727E-19, 1.540511095419833E-19, 1.5332796810709688E-19, 1.5260481843056974E-19,
            1.5188159577726683E-19, 1.5115823505412761E-19, 1.5043467076406199E-19, 1.4971083695888395E-19,
            1.4898666719118714E-19, 1.4826209446506113E-19, 1.4753705118554365E-19, 1.468114691066983E-19,
            1.4608527927820112E-19, 1.453584119903145E-19, 1.4463079671711862E-19, 1.4390236205786415E-19,
            1.4317303567630177E-19, 1.4244274423783481E-19, 1.4171141334433217E-19, 1.4097896746642792E-19,
            1.4024532987312287E-19, 1.3951042255849034E-19, 1.3877416616527576E-19, 1.3803647990516385E-19,
            1.3729728147547174E-19, 1.3655648697200824E-19, 1.3581401079782068E-19, 1.35069765567529E-19,
            1.3432366200692418E-19, 1.3357560884748263E-19, 1.3282551271542047E-19, 1.3207327801488087E-19,
            1.3131880680481524E-19, 1.3056199866908076E-19, 1.2980275057923788E-19, 1.2904095674948608E-19,
            1.2827650848312727E-19, 1.2750929400989213E-19, 1.2673919831340482E-19, 1.2596610294799512E-19,
            1.2518988584399374E-19, 1.2441042110056523E-19, 1.2362757876504165E-19, 1.2284122459762072E-19,
            1.2205121982017852E-19, 1.2125742084782245E-19, 1.2045967900166973E-19, 1.196578402011802E-19,
            1.1885174463419555E-19, 1.180412264026409E-19, 1.1722611314162064E-19, 1.164062256093911E-19,
            1.1558137724540874E-19, 1.1475137369333185E-19, 1.1391601228549047E-19, 1.1307508148492592E-19,
            1.1222836028063025E-19, 1.1137561753107903E-19, 1.1051661125053526E-19, 1.0965108783189755E-19,
            1.0877878119905372E-19, 1.0789941188076655E-19, 1.070126859970364E-19, 1.0611829414763286E-19,
            1.0521591019102928E-19, 1.0430518990027552E-19, 1.0338576948035472E-19, 1.0245726392923699E-19,
            1.015192652220931E-19, 1.0057134029488235E-19, 9.961302879967281E-20, 9.864384059945991E-20,
            9.766325296475582E-20, 9.667070742762345E-20, 9.566560624086667E-20, 9.464730838043321E-20,
            9.361512501732351E-20, 9.256831437088728E-20, 9.150607583763877E-20, 9.042754326772572E-20,
            8.933177723376368E-20, 8.821775610232788E-20, 8.708436567489232E-20, 8.593038710961216E-20,
            8.475448276424435E-20, 8.355517950846234E-20, 8.233084893358536E-20, 8.107968372912985E-20,
            7.979966928413386E-20, 7.848854928607274E-20, 7.714378370093469E-20, 7.576249697946757E-20,
            7.434141357848533E-20, 7.287677680737843E-20, 7.136424544352537E-20, 6.979876024076107E-20,
            6.817436894479905E-20, 6.648399298619854E-20, 6.471911034516277E-20, 6.28693148131037E-20,
            6.092168754828126E-20, 5.885987357557682E-20, 5.666267511609098E-20, 5.430181363089457E-20,
            5.173817174449422E-20, 4.8915031722398545E-20, 4.57447418907553E-20, 4.2078802568583416E-20,
            3.762598672240476E-20, 3.162858980588188E-20, 0.0};
        /** The precomputed ziggurat heights, denoted Y_i in the main text. Y_i = f(X_i).
         * Values have been scaled by 2^-63. */
        protected static final double[] Y = {1.4598410796619063E-22, 3.0066613427942797E-22, 4.612972881510347E-22,
            6.266335004923436E-22, 7.959452476188154E-22, 9.687465502170504E-22, 1.144687700237944E-21,
            1.3235036304379167E-21, 1.504985769205313E-21, 1.6889653000719298E-21, 1.8753025382711626E-21,
            2.063879842369519E-21, 2.2545966913644708E-21, 2.44736615188018E-21, 2.6421122727763533E-21,
            2.8387681187879908E-21, 3.0372742567457284E-21, 3.237577569998659E-21, 3.439630315794878E-21,
            3.64338936579978E-21, 3.848815586891231E-21, 4.0558733309492775E-21, 4.264530010428359E-21,
            4.474755742230507E-21, 4.686523046535558E-21, 4.899806590277526E-21, 5.114582967210549E-21,
            5.330830508204617E-21, 5.548529116703176E-21, 5.767660125269048E-21, 5.988206169917846E-21,
            6.210151079544222E-21, 6.433479778225721E-21, 6.65817819857139E-21, 6.884233204589318E-21,
            7.11163252279571E-21, 7.340364680490309E-21, 7.570418950288642E-21, 7.801785300137974E-21,
            8.034454348157002E-21, 8.268417321733312E-21, 8.503666020391502E-21, 8.740192782010952E-21,
            8.97799045202819E-21, 9.217052355306144E-21, 9.457372270392882E-21, 9.698944405926943E-21,
            9.941763378975842E-21, 1.0185824195119818E-20, 1.043112223011477E-20, 1.0677653212987396E-20,
            1.0925413210432004E-20, 1.1174398612392891E-20, 1.1424606118728715E-20, 1.1676032726866302E-20,
            1.1928675720361027E-20, 1.2182532658289373E-20, 1.2437601365406785E-20, 1.2693879923010674E-20,
            1.2951366660454145E-20, 1.321006014726146E-20, 1.3469959185800733E-20, 1.3731062804473644E-20,
            1.3993370251385596E-20, 1.4256880988463136E-20, 1.452159468598837E-20, 1.4787511217522902E-20,
            1.505463065519617E-20, 1.5322953265335218E-20, 1.5592479504415048E-20, 1.5863210015310328E-20,
            1.6135145623830982E-20, 1.6408287335525592E-20, 1.6682636332737932E-20, 1.6958193971903124E-20,
            1.7234961781071113E-20, 1.7512941457646084E-20, 1.7792134866331487E-20, 1.807254403727107E-20,
            1.8354171164377277E-20, 1.8637018603838945E-20, 1.8921088872801004E-20, 1.9206384648209468E-20,
            1.9492908765815636E-20, 1.9780664219333857E-20, 2.006965415974784E-20, 2.035988189476086E-20,
            2.0651350888385696E-20, 2.094406476067054E-20, 2.1238027287557466E-20, 2.1533242400870487E-20,
            2.1829714188430474E-20, 2.2127446894294597E-20, 2.242644491911827E-20, 2.2726712820637798E-20,
            2.3028255314272276E-20, 2.3331077273843558E-20, 2.3635183732413286E-20, 2.3940579883236352E-20,
            2.4247271080830277E-20, 2.455526284216033E-20, 2.4864560847940368E-20, 2.5175170944049622E-20,
            2.548709914306593E-20, 2.5800351625915997E-20, 2.6114934743643687E-20, 2.6430855019297323E-20,
            2.674811914993741E-20, 2.7066734008766247E-20, 2.7386706647381193E-20, 2.770804429815356E-20,
            2.803075437673527E-20, 2.835484448469575E-20, 2.868032241229163E-20, 2.9007196141372126E-20,
            2.933547384842322E-20, 2.966516390775399E-20, 2.9996274894828624E-20, 3.0328815589748056E-20,
            3.066279498088529E-20, 3.099822226867876E-20, 3.133510686958861E-20, 3.167345842022056E-20,
            3.201328678162299E-20, 3.235460204376261E-20, 3.2697414530184806E-20, 3.304173480286495E-20,
            3.338757366725735E-20, 3.373494217754894E-20, 3.408385164212521E-20, 3.443431362925624E-20,
            3.4786339973011376E-20, 3.5139942779411164E-20, 3.549513443282617E-20, 3.585192760263246E-20,
            3.621033525013417E-20, 3.6570370635764384E-20, 3.693204732657588E-20, 3.729537920403425E-20,
            3.76603804721264E-20, 3.8027065665798284E-20, 3.839544965973665E-20, 3.876554767751017E-20,
            3.9137375301086406E-20, 3.951094848074217E-20, 3.988628354538543E-20, 4.0263397213308566E-20,
            4.064230660339354E-20, 4.1023029246790967E-20, 4.140558309909644E-20, 4.178998655304882E-20,
            4.217625845177682E-20, 4.256441810262176E-20, 4.29544852915662E-20, 4.334648029830012E-20,
            4.3740423911958146E-20, 4.4136337447563716E-20, 4.4534242763218286E-20, 4.4934162278076256E-20,
            4.5336118991149025E-20, 4.5740136500984466E-20, 4.614623902627128E-20, 4.655445142742113E-20,
            4.696479922918509E-20, 4.737730864436494E-20, 4.779200659868417E-20, 4.820892075688811E-20,
            4.8628079550147814E-20, 4.9049512204847653E-20, 4.9473248772842596E-20, 4.9899320163277674E-20,
            5.032775817606897E-20, 5.0758595537153414E-20, 5.1191865935622696E-20, 5.162760406286606E-20,
            5.2065845653856416E-20, 5.2506627530725194E-20, 5.294998764878345E-20, 5.3395965145159426E-20,
            5.3844600390237576E-20, 5.429593504209936E-20, 5.475001210418387E-20, 5.520687598640507E-20,
            5.566657256998382E-20, 5.612914927627579E-20, 5.659465513990248E-20, 5.706314088652056E-20,
            5.753465901559692E-20, 5.800926388859122E-20, 5.848701182298758E-20, 5.89679611926598E-20,
            5.945217253510347E-20, 5.99397086661226E-20, 6.043063480261893E-20, 6.092501869420053E-20,
            6.142293076440286E-20, 6.192444426240153E-20, 6.242963542619394E-20, 6.293858365833621E-20,
            6.345137171544756E-20, 6.396808591283496E-20, 6.448881634575274E-20, 6.501365712899535E-20,
            6.554270665673171E-20, 6.607606788473072E-20, 6.66138486374042E-20, 6.715616194241298E-20,
            6.770312639595058E-20, 6.825486656224641E-20, 6.881151341132782E-20, 6.937320479965968E-20,
            6.994008599895911E-20, 7.05123102792795E-20, 7.109003955339717E-20, 7.16734450906448E-20,
            7.226270830965578E-20, 7.285802166105734E-20, 7.34595896130358E-20, 7.406762975496755E-20,
            7.468237403705282E-20, 7.530407016722667E-20, 7.593298319069855E-20, 7.656939728248375E-20,
            7.721361778948768E-20, 7.786597356641702E-20, 7.852681965945675E-20, 7.919654040385056E-20,
            7.987555301703797E-20, 8.056431178890163E-20, 8.126331299642618E-20, 8.19731007037063E-20,
            8.269427365263403E-20, 8.342749350883679E-20, 8.417349480745342E-20, 8.493309705283207E-20,
            8.57072195782309E-20, 8.64968999859307E-20, 8.730331729565533E-20, 8.81278213788595E-20,
            8.897197092819667E-20, 8.983758323931406E-20, 9.072680069786954E-20, 9.164218148406354E-20,
            9.258682640670276E-20, 9.356456148027886E-20, 9.458021001263618E-20, 9.564001555085036E-20,
            9.675233477050313E-20, 9.792885169780883E-20, 9.918690585753133E-20, 1.0055456271343397E-19,
            1.0208407377305566E-19, 1.0390360993240711E-19, 1.0842021724855044E-19};

        /** Underlying source of randomness. */
        protected final UniformRandomProvider rng;
        /** Exponential sampler used for the long tail. */
        protected final ContinuousSampler exponential;

        /**
         * @param rng Generator of uniformly distributed random numbers.
         */
        ModifiedZigguratNormalizedGaussianSampler(UniformRandomProvider rng) {
            this.rng = rng;
            exponential = new ModifiedZigguratExponentialSampler(rng);
        }

        /**
         * Provide access to the precomputed ziggurat lengths.
         *
         * <p>This is package-private to allow usage in the interpolation test.
         *
         * @return x
         */
        static double[] getX() {
            return X;
        }

        /** {@inheritDoc} */
        @Override
        public double sample() {
            final long xx = nextLong();
            // Float multiplication squashes these last 8 bits, so they can be used to sample i
            final int i = ((int) xx) & 0xff;

            if (i < I_MAX) {
                // Early exit.
                return X[i] * xx;
            }

            // Recycle bits then advance RNG:
            long u1 = xx & MAX_INT64;
            // Another squashed, recyclable bit
            // double sign_bit = u1 & 0x100 ? 1. : -1.
            // Use 2 - 1 or 0 - 1
            final double signBit = ((u1 >>> 7) & 0x2) - 1.0;
            final int j = selectRegion();
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
                for (;;) {
                    x = sampleX(X, j, u1);
                    final long uDistance = randomInt63() - u1;
                    if (uDistance >= 0) {
                        // Lower-left triangle
                        break;
                    }
                    if (uDistance >= CONVEX_E_MAX &&
                        // Within maximum distance of f(x) from the triangle hypotenuse.
                        sampleY(Y, j, u1 + uDistance) < Math.exp(-0.5 * x * x)) {
                        break;
                    }
                    // uDistance < E_MAX (upper-right triangle) or rejected as above the curve
                    u1 = randomInt63();
                }
            } else if (j < J_INFLECTION) {
                if (j == 0) {
                    // Tail
                    // Note: Although less frequent than the next branch, j == 0 is a subset of
                    // j < J_INFLECTION and must be first.
                    do {
                        x = ONE_OVER_X_0 * exponential.sample();
                    } while (exponential.sample() < 0.5 * x * x);
                    x += X_0;
                } else {
                    // Concave overhang
                    for (;;) {
                        // U_x <- min(U_1, U_2)
                        // distance <- | U_1 - U_2 |
                        // U_y <- 1 - (U_x + distance)
                        long uDistance = randomInt63() - u1;
                        if (uDistance < 0) {
                            // Upper-right triangle. Reflect in hypotenuse.
                            uDistance = -uDistance;
                            u1 -= uDistance;
                        }
                        x = sampleX(X, j, u1);
                        if (uDistance > CONCAVE_E_MAX ||
                            sampleY(Y, j, u1 + uDistance) < Math.exp(-0.5 * x * x)) {
                            break;
                        }
                        u1 = randomInt63();
                    }
                }
            } else {
                // Inflection point
                for (;;) {
                    x = sampleX(X, j, u1);
                    if (sampleY(Y, j, randomInt63()) < Math.exp(-0.5 * x * x)) {
                        break;
                    }
                    u1 = randomInt63();
                }
            }
            return signBit * x;
        }

        /**
         * Select the overhang region or the tail using alias sampling.
         *
         * @return the region
         */
        protected int selectRegion() {
            final long x = nextLong();
            // j in [0, 256)
            final int j = ((int) x) & 0xff;
            // map to j in [0, N] with N the number of layers of the ziggurat
            return x >= IPMF[j] ? MAP[j] & 0xff : j;
        }

        /**
         * Generates a {@code long}.
         *
         * @return the long
         */
        protected long nextLong() {
            return rng.nextLong();
        }

        /**
         * Return a positive long in {@code [0, 2^63)}.
         *
         * @return the long
         */
        protected long randomInt63() {
            return rng.nextLong() & MAX_INT64;
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
     * Modified Ziggurat method for sampling from a Gaussian distribution with mean 0 and standard deviation 1.
     *
     * <p>Uses the algorithm from McFarland, C.D. (2016).
     *
     * <p>This implementation uses simple overhangs and does not exploit the precomputed
     * distances of the concave and convex overhangs. The implementation matches the c-reference
     * compiled using -DSIMPLE_OVERHANGS for the non-tail overhangs.
     *
     * <p>Note: The tail exponential sampler does not use simple overhangs. This facilitates
     * performance comparison to the fast overhang method by keeping tail sampling the same.
     */
    static class ModifiedZigguratNormalizedGaussianSamplerSimpleOverhangs
        extends ModifiedZigguratNormalizedGaussianSampler {

        /**
         * @param rng Generator of uniformly distributed random numbers.
         */
        ModifiedZigguratNormalizedGaussianSamplerSimpleOverhangs(UniformRandomProvider rng) {
            super(rng);
        }

        /** {@inheritDoc} */
        @Override
        public double sample() {
            final long xx = nextLong();
            // Float multiplication squashes these last 8 bits, so they can be used to sample i
            final int i = ((int) xx) & 0xff;

            if (i < I_MAX) {
                // Early exit.
                return X[i] * xx;
            }

            // Another squashed, recyclable bit
            // double sign_bit = u1 & 0x100 ? 1. : -1.
            // Use 2 - 1 or 0 - 1
            final double signBit = ((xx >>> 7) & 0x2) - 1.0;
            final int j = selectRegion();

            // Simple overhangs
            double x;
            if (j == 0) {
                // Tail
                do {
                    x = ONE_OVER_X_0 * exponential.sample();
                } while (exponential.sample() < 0.5 * x * x);
                x += X_0;
            } else {
                // Rejection sampling

                // Recycle bits then advance RNG:
                long u1 = xx & MAX_INT64;
                for (;;) {
                    x = sampleX(X, j, u1);
                    if (sampleY(Y, j, randomInt63()) < Math.exp(-0.5 * x * x)) {
                        break;
                    }
                    u1 = randomInt63();
                }
            }
            return signBit * x;
        }
    }

    /**
     * Modified Ziggurat method for sampling from a Gaussian distribution with mean 0 and standard deviation 1.
     *
     * <p>Uses the algorithm from McFarland, C.D. (2016).
     *
     * <p>This implementation separates sampling of the main ziggurat and sampling from the edge
     * into different methods. This allows inlining of the main sample method.
     */
    static class ModifiedZigguratNormalizedGaussianSamplerInlining
        extends ModifiedZigguratNormalizedGaussianSampler {

        /**
         * @param rng Generator of uniformly distributed random numbers.
         */
        ModifiedZigguratNormalizedGaussianSamplerInlining(UniformRandomProvider rng) {
            super(rng);
        }

        /** {@inheritDoc} */
        @Override
        public double sample() {
            // Ideally this method byte code size should be below -XX:MaxInlineSize
            // (which defaults to 35 bytes). This compiles to 33 bytes.

            final long xx = nextLong();
            // Float multiplication squashes these last 8 bits, so they can be used to sample i
            final int i = ((int) xx) & 0xff;

            if (i < I_MAX) {
                // Early exit.
                return X[i] * xx;
            }

            return edgeSample(xx);
        }

        /**
         * Create the sample from the edge of the ziggurat.
         *
         * <p>This method has been extracted to fit the main sample method within 35 bytes (the
         * default size for a JVM to inline a method).
         *
         * @param xx Initial random deviate
         * @return a sample
         */
        private double edgeSample(long xx) {
            // Recycle bits then advance RNG:
            long u1 = xx & MAX_INT64;
            // Another squashed, recyclable bit
            // double sign_bit = u1 & 0x100 ? 1. : -1.
            // Use 2 - 1 or 0 - 1
            final double signBit = ((u1 >>> 7) & 0x2) - 1.0;
            final int j = selectRegion();
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
                for (;;) {
                    x = sampleX(X, j, u1);
                    final long uDistance = randomInt63() - u1;
                    if (uDistance >= 0) {
                        // Lower-left triangle
                        break;
                    }
                    if (uDistance >= CONVEX_E_MAX &&
                        // Within maximum distance of f(x) from the triangle hypotenuse.
                        sampleY(Y, j, u1 + uDistance) < Math.exp(-0.5 * x * x)) {
                        break;
                    }
                    // uDistance < CONVEX_E_MAX (upper-right triangle) or rejected as above the curve
                    u1 = randomInt63();
                }
            } else if (j < J_INFLECTION) {
                if (j == 0) {
                    // Tail
                    // Note: Although less frequent than the next branch, j == 0 is a subset of
                    // j < J_INFLECTION and must be first.
                    do {
                        x = ONE_OVER_X_0 * exponential.sample();
                    } while (exponential.sample() < 0.5 * x * x);
                    x += X_0;
                } else {
                    // Concave overhang
                    for (;;) {
                        // U_x <- min(U_1, U_2)
                        // distance <- | U_1 - U_2 |
                        // U_y <- 1 - (U_x + distance)
                        long uDistance = randomInt63() - u1;
                        if (uDistance < 0) {
                            uDistance = -uDistance;
                            u1 -= uDistance;
                        }
                        x = sampleX(X, j, u1);
                        if (uDistance > CONCAVE_E_MAX ||
                            sampleY(Y, j, u1 + uDistance) < Math.exp(-0.5 * x * x)) {
                            break;
                        }
                        u1 = randomInt63();
                    }
                }
            } else {
                // Inflection point
                for (;;) {
                    x = sampleX(X, j, u1);
                    if (sampleY(Y, j, randomInt63()) < Math.exp(-0.5 * x * x)) {
                        break;
                    }
                    u1 = randomInt63();
                }
            }
            return signBit * x;
        }
    }

    /**
     * Modified Ziggurat method for sampling from a Gaussian distribution with mean 0 and standard deviation 1.
     *
     * <p>Uses the algorithm from McFarland, C.D. (2016).
     *
     * <p>This implementation separates sampling of the main ziggurat and sampling from the edge
     * into different methods. This allows inlining of the main sample method.
     *
     * <p>Positive longs are created using bit shifts (not masking). The y coordinate is
     * generated with u2 not (1-u2) which avoids a subtraction.
     */
    static class ModifiedZigguratNormalizedGaussianSamplerInliningShift
        extends ModifiedZigguratNormalizedGaussianSampler {

        /**
         * @param rng Generator of uniformly distributed random numbers.
         */
        ModifiedZigguratNormalizedGaussianSamplerInliningShift(UniformRandomProvider rng) {
            super(rng);
        }

        /** {@inheritDoc} */
        @Override
        public double sample() {
            // Ideally this method byte code size should be below -XX:MaxInlineSize
            // (which defaults to 35 bytes). This compiles to 33 bytes.

            final long xx = nextLong();
            // Float multiplication squashes these last 8 bits, so they can be used to sample i
            final int i = ((int) xx) & 0xff;

            if (i < I_MAX) {
                // Early exit.
                return X[i] * xx;
            }

            return edgeSample(xx);
        }

        /**
         * Create the sample from the edge of the ziggurat.
         *
         * <p>This method has been extracted to fit the main sample method within 35 bytes (the
         * default size for a JVM to inline a method).
         *
         * @param xx Initial random deviate
         * @return a sample
         */
        private double edgeSample(long xx) {
            // Recycle bits.
            // Remove sign bit to create u1.
            long u1 = (xx << 1) >>> 1;
            // Extract the sign bit:
            // Use 2 - 1 or 0 - 1
            final double signBit = ((xx >>> 62) & 0x2) - 1.0;
            final int j = selectRegion();
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
                for (;;) {
                    x = interpolateSample(X, j, u1);
                    final long uDistance = (nextLong() >>> 1) - u1;
                    if (uDistance >= 0) {
                        // Lower-left triangle
                        break;
                    }
                    if (uDistance >= CONVEX_E_MAX &&
                        // Within maximum distance of f(x) from the triangle hypotenuse.
                        // u2 = (u1 + uDistance)
                        interpolateSample(Y, j, u1 + uDistance) < Math.exp(-0.5 * x * x)) {
                        break;
                    }
                    // uDistance < CONVEX_E_MAX (upper-right triangle) or rejected as above the curve
                    u1 = nextLong() >>> 1;
                }
            } else if (j < J_INFLECTION) {
                if (j == 0) {
                    // Tail
                    // Note: Although less frequent than the next branch, j == 0 is a subset of
                    // j < J_INFLECTION and must be first.
                    do {
                        x = ONE_OVER_X_0 * exponential.sample();
                    } while (exponential.sample() < 0.5 * x * x);
                    x += X_0;
                } else {
                    // Concave overhang
                    for (;;) {
                        // U_x <- min(U_1, U_2)
                        // distance <- | U_1 - U_2 |
                        // U_y <- 1 - (U_x + distance)
                        long uDistance = (nextLong() >>> 1) - u1;
                        if (uDistance < 0) {
                            uDistance = -uDistance;
                            u1 -= uDistance;
                        }
                        x = interpolateSample(X, j, u1);
                        if (uDistance > CONCAVE_E_MAX ||
                            interpolateSample(Y, j, u1 + uDistance) < Math.exp(-0.5 * x * x)) {
                            break;
                        }
                        u1 = nextLong() >>> 1;
                    }
                }
            } else {
                // Inflection point
                for (;;) {
                    x = interpolateSample(X, j, u1);
                    if (interpolateSample(Y, j, nextLong() >>> 1) < Math.exp(-0.5 * x * x)) {
                        break;
                    }
                    u1 = nextLong() >>> 1;
                }
            }
            return signBit * x;
        }

        /**
         * Interpolate x from the uniform deviate.
         * <pre>
         *  value = x[j] + u * (x[j-1] - x[j])
         * </pre>
         * <p>If x is the precomputed lengths of the ziggurat (X) then x[j-1] is larger and this adds
         * a delta to x[j].
         * <p>If x is the precomputed pdf for the lengths of the ziggurat (Y) then x[j-1] is smaller
         * larger and this subtracts a delta from x[j].
         *
         * @param x x
         * @param j j
         * @param u uniform deviate
         * @return the sample
         */
        private static double interpolateSample(double[] x, int j, long u) {
            return x[j] * TWO_POW_63 + (x[j - 1] - x[j]) * u;
        }
    }

    /**
     * Modified Ziggurat method for sampling from a Gaussian distribution with mean 0 and standard deviation 1.
     *
     * <p>Uses the algorithm from McFarland, C.D. (2016).
     *
     * <p>This implementation extracts the separates the sample method for the main ziggurat
     * from the edge sampling. This allows inlining of the main sample method.
     *
     * <p>This implementation uses simple overhangs and does not exploit the precomputed
     * distances of the concave and convex overhangs. The implementation matches the c-reference
     * compiled using -DSIMPLE_OVERHANGS for the non-tail overhangs.
     *
     * <p>Note: The tail exponential sampler does not use simple overhangs. This facilitates
     * performance comparison to the fast overhang method by keeping tail sampling the same.
     */
    static class ModifiedZigguratNormalizedGaussianSamplerInliningSimpleOverhangs
        extends ModifiedZigguratNormalizedGaussianSampler {

        /**
         * @param rng Generator of uniformly distributed random numbers.
         */
        ModifiedZigguratNormalizedGaussianSamplerInliningSimpleOverhangs(UniformRandomProvider rng) {
            super(rng);
        }

        /** {@inheritDoc} */
        @Override
        public double sample() {
            // Ideally this method byte code size should be below -XX:MaxInlineSize
            // (which defaults to 35 bytes). This compiles to 33 bytes.

            final long xx = nextLong();
            // Float multiplication squashes these last 8 bits, so they can be used to sample i
            final int i = ((int) xx) & 0xff;

            if (i < I_MAX) {
                // Early exit.
                return X[i] * xx;
            }

            return edgeSample(xx);
        }

        /**
         * Create the sample from the edge of the ziggurat.
         *
         * <p>This method has been extracted to fit the main sample method within 35 bytes (the
         * default size for a JVM to inline a method).
         *
         * @param xx Initial random deviate
         * @return a sample
         */
        private double edgeSample(long xx) {
            // Another squashed, recyclable bit
            // double sign_bit = u1 & 0x100 ? 1. : -1.
            // Use 2 - 1 or 0 - 1
            final double signBit = ((xx >>> 7) & 0x2) - 1.0;
            final int j = selectRegion();

            // Simple overhangs
            double x;
            if (j == 0) {
                // Tail
                do {
                    x = ONE_OVER_X_0 * exponential.sample();
                } while (exponential.sample() < 0.5 * x * x);
                x += X_0;
            } else {
                // Rejection sampling

                // Recycle bits then advance RNG:
                long u1 = xx & MAX_INT64;
                for (;;) {
                    x = sampleX(X, j, u1);
                    if (sampleY(Y, j, randomInt63()) < Math.exp(-0.5 * x * x)) {
                        break;
                    }
                    u1 = randomInt63();
                }
            }
            return signBit * x;
        }
    }

    /**
     * Modified Ziggurat method for sampling from a Gaussian distribution with mean 0 and standard deviation 1.
     *
     * <p>Uses the algorithm from McFarland, C.D. (2016).
     *
     * <p>This is a copy of {@link ModifiedZigguratNormalizedGaussianSampler} using
     * an integer map in-place of a byte map look-up table. Note the underlying exponential
     * sampler does not use an integer map. This sampler is used for the tail with a frequency
     * of approximately 0.000276321 and should not impact performance comparisons.
     */
    static class ModifiedZigguratNormalizedGaussianSamplerIntMap
        extends ModifiedZigguratNormalizedGaussianSampler {

        /** The alias map. An integer in [0, 255] stored as a byte to save space. */
        private static final int[] INT_MAP = {0, 0, 239, 2, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 253, 253, 253, 253, 253,
            253, 253, 253, 253, 253, 253, 253, 253, 253, 253, 253, 253, 253, 253, 253, 253, 253, 253, 253, 253, 253,
            253, 253, 253, 253, 253, 253, 253, 253, 253, 253, 253, 253, 253, 253, 253, 253, 253, 253, 253, 253, 253,
            253, 253, 253, 253, 253, 253, 253, 253, 253, 253, 253, 253, 253, 253, 253, 253, 253, 253, 253, 253, 253,
            253, 253, 253, 253, 253, 253, 253, 253, 253, 253, 253, 253, 253, 253, 253, 253, 253, 253, 253, 253, 253,
            253, 253, 253, 253, 253, 253, 253, 253, 253, 253, 253, 253, 253, 253, 253, 253, 253, 253, 253, 253, 253,
            253, 253, 253, 253, 253, 253, 253, 253, 253, 253, 253, 253, 253, 253, 253, 253, 253, 253, 253, 253, 253,
            253, 253, 253, 253, 253, 253, 253, 253, 253, 253, 253, 253, 253, 253, 253, 253, 253, 253, 253, 253, 253,
            253, 253, 253, 253, 253, 253, 253, 253, 253, 253, 253, 253, 253, 253, 253, 253, 253, 253, 253, 253, 253,
            253, 253, 253, 253, 253, 253, 252, 252, 252, 252, 252, 252, 252, 252, 252, 252, 251, 251, 251, 251, 251,
            251, 251, 250, 250, 250, 250, 250, 249, 249, 249, 248, 248, 248, 247, 247, 247, 246, 246, 245, 244, 244,
            243, 242, 240, 2, 2, 3, 3, 0, 0, 240, 241, 242, 243, 244, 245, 246, 247, 248, 249, 250, 251, 252, 253, 1, 0,
            0};

        /**
         * @param rng Generator of uniformly distributed random numbers.
         */
        ModifiedZigguratNormalizedGaussianSamplerIntMap(UniformRandomProvider rng) {
            super(rng);
        }

        /** {@inheritDoc} */
        @Override
        protected int selectRegion() {
            final long x = nextLong();
            // j in [0, 256)
            final int j = ((int) x) & 0xff;
            // map to j in [0, N] with N the number of layers of the ziggurat
            return x >= IPMF[j] ? INT_MAP[j] : j;
        }
    }

    /**
     * Modified Ziggurat method for sampling from a Gaussian distribution with mean 0 and standard deviation 1.
     *
     * <p>Uses the algorithm from McFarland, C.D. (2016).
     *
     * <p>This is a copy of {@link ModifiedZigguratNormalizedGaussianSampler} using
     * a table to store the maximum epsilon value for each overhang.
     */
    static class ModifiedZigguratNormalizedGaussianSamplerEMaxTable
        extends ModifiedZigguratNormalizedGaussianSampler {

        /** The deviation of concave pdf(x) below the hypotenuse value for early exit scaled by 2^63.
         * 253 entries with overhang {@code j} corresponding to entry {@code j-1}.
         *
         * <p>Stored as absolute distances. These are negated for the convex overhangs on initialization. */
        private static final long[] E_MAX_TABLE = {760463704284035184L, 448775940668074322L, 319432059030582634L,
            248136886892509167L, 202879887821507213L, 171571664564813181L, 148614265661634224L, 131055242060438178L,
            117188580518411866L, 105959220286966343L, 96679401617437000L, 88881528890695590L, 82236552845322455L,
            76506171300171850L, 71513549901780258L, 67124696158442860L, 63236221616146940L, 59767073533081827L,
            56652810532069337L, 53841553364303819L, 51291065360753073L, 48966611131444450L, 46839361730231995L,
            44885190189779505L, 43083750299904955L, 41417763838249446L, 39872463215195657L, 38435151378929884L,
            37094851170183015L, 35842023608135061L, 34668339797973811L, 33566494917490850L, 32530055495300595L,
            31553333229961898L, 30631280119818406L, 29759400819129686L, 28933679006953617L, 28150515222628953L,
            27406674137103216L, 26699239630262959L, 26025576358437882L, 25383296743791121L, 24770232513666098L,
            24184410074620943L, 23624029131570806L, 23087444063833159L, 22573147652048958L, 22079756816885946L,
            21606000085203013L, 21150706544370418L, 20712796082591192L, 20291270743857678L, 19885207051787867L,
            19493749177966922L, 19116102848340569L, 18751529896268709L, 18399343383557859L, 18058903221544231L,
            17729612233426885L, 17410912606822884L, 17102282692149351L, 16803234108116672L, 16513309120493202L,
            16232078264496960L, 15959138184784472L, 15694109670143667L, 15436635862703561L, 15186380623831584L,
            14943027040942155L, 14706276061226881L, 14475845239883253L, 14251467591786361L, 14032890536754294L,
            13819874929609696L, 13612194167177793L, 13409633365176500L, 13211988598685834L, 13019066200522936L,
            12830682112422215L, 12646661284424425L, 12466837118331739L, 12291050951483677L, 12119151577470956L,
            11950994800721809L, 11786443022181594L, 11625364853565635L, 11467634757892067L, 11313132714210236L,
            11161743904626184L, 11013358421893618L, 10867870995988906L, 10725180738227453L, 10585190901599062L,
            10447808656112693L, 10312944878042918L, 10180513952058881L, 10050433585302955L, 9922624632558803L,
            9797010931719358L, 9673519148825347L, 9552078632004422L, 9432621273689757L, 9315081380547200L,
            9199395550580725L, 9085502556926974L, 8973343237884485L, 8862860392758412L, 8753998683127701L,
            8646704539174329L, 8540926070735305L, 8436612982764049L, 8333716494908564L, 8232189264931586L,
            8131985315719003L, 8033059965636513L, 7935369762012110L, 7838872417532931L, 7743526749360846L,
            7649292620780855L, 7556130885207418L, 7464003332385685L, 7372872636629289L, 7282702306949403L,
            7193456638934045L, 7105100668244386L, 7017600125602463L, 6930921393146734L, 6845031462041107L,
            6759897891224838L, 6675488767195822L, 6591772664721566L, 6508718608380146L, 6426296034828168L,
            6344474755702722L, 6263224921061057L, 6182516983265422L, 6102321661219813L, 6022609904868391L,
            5943352859861905L, 5864521832301635L, 5786088253467533L, 5708023644436456L, 5630299580496017L,
            5552887655255304L, 5475759444352708L, 5398886468659678L, 5322240156870143L, 5245791807368878L,
            5169512549259789L, 5093373302437038L, 5017344736568650L, 4941397228861186L, 4865500820464432L,
            4789625171364815L, 4713739513611018L, 4637812602700734L, 4561812666947717L, 4485707354637964L,
            4409463678763447L, 4333047959114789L, 4256425761488008L, 4179561833749943L, 4102420038479365L,
            4024963281881178L, 3947153438645186L, 3868951272390456L, 3790316351307711L, 3711206958575720L,
            3631579997089249L, 3551390887994563L, 3470593462479259L, 3389139846213138L, 3306980335775941L,
            3224063266344542L, 3140334869838942L, 3055739122646026L, 2970217581950400L, 2883709209602502L,
            2796150182338912L, 2707473687051021L, 2617609699653324L, 2526484745953020L, 2434021642745634L,
            2340139217177559L, 2244752002202266L, 2147769905731505L, 2049097850839701L, 1948635384132493L,
            1846276249145715L, 1741907921439271L, 1635411101964517L, 1526659165422377L, 1415517561001961L,
            1301843164664438L, 1185483586365029L, 1066276445521246L, 944048652015702L, 818615792062560L,
            689781896027189L, 557340453963802L, 421079947136144L, 280810745547724L, 136572537955918L, 20305634136204L,
            169472579551785L, 328795810543866L, 494085614296539L, 665530089254391L, 843517138081672L, 1028504363150172L,
            1221002487033413L, 1421575123173034L, 1630842969010940L, 1849490036801539L, 2078271380701587L,
            2318022280314205L, 2569669030578262L, 2834241595132002L, 3112888469933608L, 3406894199714926L,
            3717700104287726L, 4046928914960108L, 4396414204558612L, 4768235732059441L, 5164762133803925L,
            5588702804307930L, 6043171358058752L, 6531763802579722L, 7058655558989014L, 7628722851132691L,
            8247695913715760L, 8922354192593482L, 9660777606582816L, 10472673600425443L, 11369808078043702L,
            12366580875504986L, 13480805713009702L, 14734784789701036L, 16156816731707853L, 17783356724741365L,
            19662183995497761L, 21857171972777613L, 24455696683167282L, 27580563854327149L, 31410046818804399L,
            36213325371048583L, 42417257237736629L, 50742685211596715L, 62513643454971809L, 80469460944360062L,
            111428640794724271L, 179355568638601057L, 2269182951627976004L};

        static {
            // Negate the E_MAX table for the convex overhangs
            for (int j = J_INFLECTION; j < E_MAX_TABLE.length; j++) {
                E_MAX_TABLE[j] = -E_MAX_TABLE[j];
            }
        }

        /**
         * @param rng Generator of uniformly distributed random numbers.
         */
        ModifiedZigguratNormalizedGaussianSamplerEMaxTable(UniformRandomProvider rng) {
            super(rng);
        }

        /** {@inheritDoc} */
        @Override
        public double sample() {
            final long xx = nextLong();
            // Float multiplication squashes these last 8 bits, so they can be used to sample i
            final int i = ((int) xx) & 0xff;

            if (i < I_MAX) {
                // Early exit.
                return X[i] * xx;
            }

            // Recycle bits then advance RNG:
            long u1 = xx & MAX_INT64;
            // Another squashed, recyclable bit
            // double sign_bit = u1 & 0x100 ? 1. : -1.
            // Use 2 - 1 or 0 - 1
            final double signBit = ((u1 >>> 7) & 0x2) - 1.0;
            final int j = selectRegion();
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
                final long eMax = E_MAX_TABLE[j - 1];
                for (;;) {
                    x = sampleX(X, j, u1);
                    final long uDistance = randomInt63() - u1;
                    if (uDistance >= 0) {
                        // Lower-left triangle
                        break;
                    }
                    if (uDistance >= eMax &&
                        // Within maximum distance of f(x) from the triangle hypotenuse.
                        sampleY(Y, j, u1 + uDistance) < Math.exp(-0.5 * x * x)) {
                        break;
                    }
                    // uDistance < E_MAX (upper-right triangle) or rejected as above the curve
                    u1 = randomInt63();
                }
            } else if (j < J_INFLECTION) {
                if (j == 0) {
                    // Tail
                    // Note: Although less frequent than the next branch, j == 0 is a subset of
                    // j < J_INFLECTION and must be first.
                    do {
                        x = ONE_OVER_X_0 * exponential.sample();
                    } while (exponential.sample() < 0.5 * x * x);
                    x += X_0;
                } else {
                    // Concave overhang
                    final long eMax = E_MAX_TABLE[j - 1];
                    for (;;) {
                        // U_x <- min(U_1, U_2)
                        // distance <- | U_1 - U_2 |
                        // U_y <- 1 - (U_x + distance)
                        long uDistance = randomInt63() - u1;
                        if (uDistance < 0) {
                            // Upper-right triangle. Reflect in hypotenuse.
                            uDistance = -uDistance;
                            u1 -= uDistance;
                        }
                        x = sampleX(X, j, u1);
                        if (uDistance > eMax ||
                            sampleY(Y, j, u1 + uDistance) < Math.exp(-0.5 * x * x)) {
                            break;
                        }
                        u1 = randomInt63();
                    }
                }
            } else {
                // Inflection point
                for (;;) {
                    x = sampleX(X, j, u1);
                    if (sampleY(Y, j, randomInt63()) < Math.exp(-0.5 * x * x)) {
                        break;
                    }
                    u1 = randomInt63();
                }
            }
            return signBit * x;
        }
    }

    /**
     * Modified Ziggurat method for sampling from a Gaussian distribution with mean 0 and standard deviation 1.
     *
     * <p>Uses the algorithm from McFarland, C.D. (2016).
     *
     * <p>This is a copy of {@link ModifiedZigguratNormalizedGaussianSampler} using
     * two thresholds for the maximum epsilon value for convex and concave overhangs.
     *
     * <p>Note: The normal curve is very well approximated by the straight line of
     * the triangle hypotenuse in the majority of cases. As the convex overhang approaches x=0
     * the curve is significantly different. For the concave overhangs the curve is increasingly
     * different approaching the tail. This can be exploited using two maximum deviation thresholds.
     */
    static class ModifiedZigguratNormalizedGaussianSamplerEMax2
        extends ModifiedZigguratNormalizedGaussianSampler {

        // Ziggurat volumes:
        // Inside the layers              = 98.8281%  (253/256)
        // Fraction outside the layers:
        // concave overhangs              = 76.1941%
        // inflection overhang            =  0.1358%
        // convex overhangs               = 21.3072%
        // tail                           =  2.3629%

        // Separation of convex overhangs:
        // (Cut made to separate large final overhang with a very different E max.)
        //                                                E_MAX
        // j = 253                        = 72.0882%      0.246
        // 204 < j < 253                  = 27.9118%      0.0194

        // Separation of concave overhangs:
        // (Cut made between overhangs requiring above or below 0.02 for E max.
        // This choice is somewhat arbitrary. Increasing j will reduce the second E max but makes
        // the branch less predictable as the major path is used less.)
        // 1 <= j <= 5                    = 12.5257%      0.0824
        // 5 < j < 204                    = 87.4743%      0.0186

        /** The convex overhang region j below which the second maximum deviation constant is valid. */
        private static final int CONVEX_J2 = 253;
        /** The concave overhang region j above which the second maximum deviation constant is valid. */
        private static final int CONCAVE_J2 = 5;

        /** Maximum epsilon distance of convex pdf(x) above the hypotenuse value for early rejection
         * for overhangs {@code 204 < j < 253}.
         * Equal to approximately 0.0194 scaled by 2^63. This is negated on purpose.
         *
         * <p>This threshold increases the area of the early reject triangle by:
         * (1-0.0194)^2 / (1-0.246)^2 = 69.1%. */
        private static final long CONVEX_E_MAX_2 = -179355568638601057L;

        /** Maximum deviation of concave pdf(x) below the hypotenuse value for early exit
         * for overhangs {@code 5 < j < 204}.
         * Equal to approximately 0.0186 scaled by 2^63.
         *
         * <p>This threshold increases the area of the early exit triangle by:
         * (1-0.0186)^2 / (1-0.0824)^2 = 14.4%. */
        private static final long CONCAVE_E_MAX_2 = 171571664564813181L;

        /**
         * @param rng Generator of uniformly distributed random numbers.
         */
        ModifiedZigguratNormalizedGaussianSamplerEMax2(UniformRandomProvider rng) {
            super(rng);
        }

        /** {@inheritDoc} */
        @Override
        public double sample() {
            final long xx = nextLong();
            // Float multiplication squashes these last 8 bits, so they can be used to sample i
            final int i = ((int) xx) & 0xff;

            if (i < I_MAX) {
                // Early exit.
                return X[i] * xx;
            }

            // Recycle bits then advance RNG:
            long u1 = xx & MAX_INT64;
            // Another squashed, recyclable bit
            // double sign_bit = u1 & 0x100 ? 1. : -1.
            // Use 2 - 1 or 0 - 1
            final double signBit = ((u1 >>> 7) & 0x2) - 1.0;
            final int j = selectRegion();
            // Four kinds of overhangs:
            //  j = 0                :  Sample from tail
            //  0 < j < J_INFLECTION :  Overhang is concave; only sample from Lower-Left triangle
            //  j = J_INFLECTION     :  Must sample from entire overhang rectangle
            //  j > J_INFLECTION     :  Overhangs are convex; implicitly accept point in Lower-Left triangle
            //
            // Conditional statements are arranged such that the more likely outcomes are first.
            double x;
            if (j > J_INFLECTION) {
                // Convex overhang:
                // j < J2 frequency: 0.279118
                final long eMax = j < CONVEX_J2 ? CONVEX_E_MAX_2 : CONVEX_E_MAX;
                for (;;) {
                    x = sampleX(X, j, u1);
                    final long uDistance = randomInt63() - u1;
                    if (uDistance >= 0) {
                        // Lower-left triangle
                        break;
                    }
                    if (uDistance >= eMax &&
                        // Within maximum distance of f(x) from the triangle hypotenuse.
                        sampleY(Y, j, u1 + uDistance) < Math.exp(-0.5 * x * x)) {
                        break;
                    }
                    // uDistance < E_MAX (upper-right triangle) or rejected as above the curve
                    u1 = randomInt63();
                }
            } else if (j < J_INFLECTION) {
                if (j == 0) {
                    // Tail
                    // Note: Although less frequent than the next branch, j == 0 is a subset of
                    // j < J_INFLECTION and must be first.
                    do {
                        x = ONE_OVER_X_0 * exponential.sample();
                    } while (exponential.sample() < 0.5 * x * x);
                    x += X_0;
                } else {
                    // Concave overhang
                    // j > J2 frequency: 0.874743
                    final long eMax = j > CONCAVE_J2 ? CONCAVE_E_MAX_2 : CONCAVE_E_MAX;
                    for (;;) {
                        // U_x <- min(U_1, U_2)
                        // distance <- | U_1 - U_2 |
                        // U_y <- 1 - (U_x + distance)
                        long uDistance = randomInt63() - u1;
                        if (uDistance < 0) {
                            // Upper-right triangle. Reflect in hypotenuse.
                            uDistance = -uDistance;
                            u1 -= uDistance;
                        }
                        x = sampleX(X, j, u1);
                        if (uDistance > eMax ||
                            sampleY(Y, j, u1 + uDistance) < Math.exp(-0.5 * x * x)) {
                            break;
                        }
                        u1 = randomInt63();
                    }
                }
            } else {
                // Inflection point
                for (;;) {
                    x = sampleX(X, j, u1);
                    if (sampleY(Y, j, randomInt63()) < Math.exp(-0.5 * x * x)) {
                        break;
                    }
                    u1 = randomInt63();
                }
            }
            return signBit * x;
        }
    }

    /**
     * Modified Ziggurat method for sampling from a Gaussian distribution with mean 0 and standard deviation 1.
     *
     * <p>Uses the algorithm from McFarland, C.D. (2016).
     *
     * <p>This is a copy of {@link ModifiedZigguratNormalizedGaussianSampler} using
     * a table size of 512.
     */
    static class ModifiedZigguratNormalizedGaussianSampler512 implements ContinuousSampler {
        // Ziggurat volumes:
        // Inside the layers              = 99.4141%  (509/512)
        // Fraction outside the layers:
        // concave overhangs              = 75.5775%
        // inflection overhang            =  0.0675%
        // convex overhangs               = 22.2196%
        // tail                           =  2.1354%

        /** The number of layers in the ziggurat. Maximum i value for early exit. */
        protected static final int I_MAX = 509;
        /** The point where the Gaussian switches from convex to concave.
         * This is the largest value of X[j] below 1. */
        protected static final int J_INFLECTION = 409;
        /** Maximum epsilon distance of convex pdf(x) above the hypotenuse value for early rejection.
         * Equal to approximately 0.2477 scaled by 2^63. This is negated on purpose as the
         * distance for a point (x,y) above the hypotenuse is negative:
         * {@code (|d| < max) == (d >= -max)}. */
        protected static final long CONVEX_E_MAX = -2284356979160975476L;
        /** Maximum distance of concave pdf(x) below the hypotenuse value for early exit.
         * Equal to approximately 0.08284 scaled by 2^63. */
        protected static final long CONCAVE_E_MAX = 764138791244619676L;
        /** Beginning of tail. Equal to X[0] * 2^63. */
        protected static final double X_0 = 3.8358644648571882;
        /** 1/X_0. Used for tail sampling. */
        protected static final double ONE_OVER_X_0 = 1.0 / X_0;

        /** The alias map. */
        private static final int[] MAP = {0, 0, 480, 2, 3, 4, 5, 6, 7, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 1, 1, 1, 1, 1, 509, 509, 509, 509, 509, 509, 509, 509, 509, 509, 509, 509, 509, 509, 509, 509, 509,
            509, 509, 509, 509, 509, 509, 509, 509, 509, 509, 509, 509, 509, 509, 509, 509, 509, 509, 509, 509, 509,
            509, 509, 509, 509, 509, 509, 509, 509, 509, 509, 509, 509, 509, 509, 509, 509, 509, 509, 509, 509, 509,
            509, 509, 509, 509, 509, 509, 509, 509, 509, 509, 509, 509, 509, 509, 509, 509, 509, 509, 509, 509, 509,
            509, 509, 509, 509, 509, 509, 509, 509, 509, 509, 509, 509, 509, 509, 509, 509, 509, 509, 509, 509, 509,
            509, 509, 509, 509, 509, 509, 509, 509, 509, 509, 509, 509, 509, 509, 509, 509, 509, 509, 509, 509, 509,
            509, 509, 509, 509, 509, 509, 509, 509, 509, 509, 509, 509, 509, 509, 509, 509, 509, 509, 509, 509, 509,
            509, 509, 509, 509, 509, 509, 509, 509, 509, 509, 509, 509, 509, 509, 509, 509, 509, 509, 509, 509, 509,
            509, 509, 509, 509, 509, 509, 509, 509, 509, 509, 509, 509, 509, 509, 509, 509, 509, 509, 509, 509, 509,
            509, 509, 509, 509, 509, 509, 509, 509, 509, 509, 509, 509, 509, 509, 509, 509, 509, 509, 509, 509, 509,
            509, 509, 509, 509, 509, 509, 509, 509, 509, 509, 509, 509, 509, 509, 509, 509, 509, 509, 509, 509, 509,
            509, 509, 509, 509, 509, 509, 509, 509, 509, 509, 509, 509, 509, 509, 509, 509, 509, 509, 509, 509, 509,
            509, 509, 509, 509, 509, 509, 509, 509, 509, 509, 509, 509, 509, 509, 509, 509, 509, 509, 509, 509, 509,
            509, 509, 509, 509, 509, 509, 509, 509, 509, 509, 509, 509, 509, 509, 509, 509, 509, 509, 509, 509, 509,
            509, 509, 509, 509, 509, 509, 509, 509, 509, 509, 509, 509, 509, 509, 509, 509, 509, 509, 509, 509, 509,
            509, 509, 509, 509, 509, 509, 509, 509, 509, 509, 509, 508, 508, 508, 508, 508, 508, 508, 508, 508, 508,
            508, 508, 508, 508, 508, 508, 508, 508, 508, 508, 508, 508, 508, 507, 507, 507, 507, 507, 507, 507, 507,
            507, 507, 507, 507, 507, 507, 506, 506, 506, 506, 506, 506, 506, 506, 506, 506, 505, 505, 505, 505, 505,
            505, 505, 505, 504, 504, 504, 504, 504, 504, 503, 503, 503, 503, 503, 502, 502, 502, 502, 501, 501, 501,
            501, 500, 500, 500, 500, 499, 499, 499, 498, 498, 498, 497, 497, 496, 496, 495, 495, 494, 494, 493, 493,
            492, 491, 490, 489, 488, 487, 486, 484, 2, 2, 2, 2, 2, 3, 3, 3, 4, 4, 4, 5, 5, 6, 7, 8, 0, 0, 0, 0, 0, 0, 0,
            481, 482, 483, 484, 485, 486, 487, 488, 489, 490, 491, 492, 493, 494, 495, 496, 497, 498, 499, 500, 501,
            502, 503, 504, 505, 506, 507, 508, 509, 1, 0, 0};
        /** The alias inverse PMF. */
        private static final long[] IPMF = {9223372036854775296L, 3232176979959565312L, 2248424156373566976L,
            5572490326155894272L, 3878549168685540352L, 6596558088755235840L, 7126388452799016960L,
            5909258326277172224L, 6789036995587703808L, 8550801776550481920L, 7272880214327450112L,
            6197723851568405504L, 5279475843641833984L, 4485290838628302848L, 3790998004182467072L,
            3178396845257682944L, 2633509168847815680L, 2145413694861947904L, 1705448049989382144L,
            1306649397437548544L, 943354160611131392L, 610906287013685760L, 305441095029422592L, 23722736478678016L,
            -236979677660888064L, -478987634076675072L, -704287768537463808L, -914590611781799424L,
            -1111377331889550848L, -1295937239757415424L, -1469398133880157184L, -1632751030331424768L,
            -1786870462753421312L, -1932531249141885952L, -2070422432472262144L, -2201158928905888256L,
            -2325291321647773696L, -2443314131355272192L, -2555672836734271488L, -2662769858897092096L,
            -2764969686945558528L, -2862603283332330496L, -2955971885350600192L, -3045350299719277056L,
            -3130989762881722368L, -3213120439420999168L, -3291953604062304256L, -3367683558060156416L,
            -3440489316130714624L, -3510536092827323904L, -3577976620246562816L, -3642952314681374720L,
            -3705594315191049216L, -3766024408614111744L, -3824355856527138304L, -3880694134271785472L,
            -3935137594960746496L, -3987778065544580608L, -4038701385199136256L, -4087987887601244160L,
            -4135712841168144384L, -4181946844655158784L, -4226756186527602176L, -4270203172038951424L,
            -4312346420932761088L, -4353241136943226880L, -4392939356053354496L, -4431490172784719360L,
            -4468939945001798656L, -4505332485123679232L, -4540709232733563392L, -4575109415088551424L,
            -4608570193416988672L, -4641126797904314880L, -4672812652574308352L, -4703659490673809920L,
            -4733697460624237568L, -4762955224068688384L, -4791460047779233792L, -4819237887169053184L,
            -4846313464821579264L, -4872710343112062464L, -4898450992673160192L, -4923556853979476992L,
            -4948048396148470272L, -4971945172796024320L, -4995265870892981248L, -5018028360085760000L,
            -5040249735872740864L, -5061946361747427840L, -5083133907806415360L, -5103827387715595776L,
            -5124041191022579712L, -5143789117574014464L, -5163084406061926400L, -5181939762086439936L,
            -5200367384806330368L, -5218378992989818368L, -5235985846759801344L, -5253198770233210880L,
            -5270028172706144256L, -5286484068673344512L, -5302576093914087424L, -5318313526819882496L,
            -5333705300501412864L, -5348760022140801536L, -5363485985612222976L, -5377891185738658816L,
            -5391983331261941760L, -5405769858949649408L, -5419257942659722240L, -5432454506730834944L,
            -5445366236058309120L, -5457999585571643392L, -5470360790218524160L, -5482455874386169344L,
            -5494290660050575872L, -5505870774716076032L, -5517201660378635264L, -5528288579187974144L,
            -5539136622578390528L, -5549750716118886400L, -5560135626660535808L, -5570295969034180608L,
            -5580236210799400960L, -5589960678379259904L, -5599473562706778112L, -5608778923054202880L,
            -5617880692891545088L, -5626782684460370944L, -5635488592684399104L, -5644001999389030912L,
            -5652326378260390400L, -5660465096501542912L, -5668421421481986048L, -5676198522069054464L,
            -5683799472034193408L, -5691227254524439552L, -5698484764964738048L, -5705574811911234048L,
            -5712500123062644224L, -5719263345561367040L, -5725867049400364032L, -5732313730758826496L,
            -5738605812086640640L, -5744745647569117184L, -5750735522378352640L, -5756577656307920896L,
            -5762274206097785856L, -5767827266307192832L, -5773238871631092224L, -5778510999439789056L,
            -5783645569528274432L, -5788644448981466624L, -5793509449834629120L, -5798242334409694208L,
            -5802844813858308096L, -5807318551213207040L, -5811665162349513216L, -5815886217028536832L,
            -5819983240762728448L, -5823957715198579712L, -5827811080259974144L, -5831544734136989696L,
            -5835160035614893056L, -5838658304467378688L, -5842040821904171520L, -5845308832880873984L,
            -5848463546172629504L, -5851506134738812416L, -5854437738541255168L, -5857259462674584064L,
            -5859972381032173568L, -5862577534420617216L, -5865075933349880320L, -5867468558168720896L,
            -5869756358001916928L, -5871940255655389696L, -5874021142765756416L, -5875999885243720704L,
            -5877877321138562048L, -5879654262030006784L, -5881331493491406336L, -5882909775710032384L,
            -5884389844282934784L, -5885772409590032896L, -5887058159412298240L, -5888247756760180736L,
            -5889341841868359680L, -5890341033726286848L, -5891245927036046336L, -5892057096967517184L,
            -5892775095254208000L, -5893400454293159424L, -5893933684945675776L, -5894375277894875136L,
            -5894725704281316864L, -5894985415097101824L, -5895154842211865088L, -5895234398367037440L,
            -5895224478119281152L, -5895125456527683584L, -5894937691150572032L, -5894661521100592128L,
            -5894297268546692608L, -5893845237129657344L, -5893305713928932352L, -5892678968199164928L,
            -5891965253512273920L, -5891164805272775680L, -5890277843462263808L, -5889304570970719232L,
            -5888245175227536896L, -5887099827242714112L, -5885868681480997376L, -5884551878016265728L,
            -5883149540187518976L, -5881661776186659840L, -5880088679051159040L, -5878430325617481216L,
            -5876686779113256448L, -5874858086097725440L, -5872944278695313920L, -5870945374625903616L,
            -5868861375946597376L, -5866692270114584576L, -5864438029808936448L, -5862098613329829376L,
            -5859673963988850688L, -5857164010304659456L, -5854568667030923264L, -5851887832571710976L,
            -5849121393007995904L, -5846269217914838016L, -5843331163565322752L, -5840307071023708160L,
            -5837196767034538496L, -5834000063619345920L, -5830716757850101760L, -5827346633080182272L,
            -5823889457015247872L, -5820344982925702656L, -5816712949734398464L, -5812993080309509120L,
            -5809185084612210688L, -5805288655397134848L, -5801303472071718912L, -5797229197542626816L,
            -5793065480979245568L, -5788811954902762496L, -5784468237204753408L, -5780033929729368064L,
            -5775508619016246784L, -5770891876025206272L, -5766183254901072896L, -5761382294937170432L,
            -5756488518102026240L, -5751501431126126080L, -5746420523619011584L, -5741245267817476608L,
            -5735975121281433088L, -5730609522034737152L, -5725147892326605312L, -5719589637112302080L,
            -5713934142693986816L, -5708180778990178816L, -5702328896016054784L, -5696377827537145856L,
            -5690326887295941632L, -5684175370772383744L, -5677922555045085184L, -5671567696296352256L,
            -5665110033102450176L, -5658548782530205696L, -5651883142913629696L, -5645112290875286016L,
            -5638235383069228544L, -5631251554336663040L, -5624159919711433728L, -5616959570005672448L,
            -5609649575759112704L, -5602228984633701376L, -5594696820961281536L, -5587052085776743936L,
            -5579293757104064512L, -5571420787942931456L, -5563432107812764160L, -5555326619765914112L,
            -5547103202855799296L, -5538760709287606272L, -5530297965182673920L, -5521713769559916032L,
            -5513006894391551488L, -5504176082674265088L, -5495220049712259584L, -5486137481881783808L,
            -5476927034862822400L, -5467587334392330752L, -5458116976167262720L, -5448514522422561280L,
            -5438778504885873664L, -5428907421454137344L, -5418899736307948032L, -5408753879414383104L,
            -5398468245670757888L, -5388041193950201856L, -5377471046142717440L, -5366756087011503104L,
            -5355894562291467776L, -5344884679094671360L, -5333724604104164352L, -5322412462390110720L,
            -5310946337190221824L, -5299324269391752192L, -5287544254227943936L, -5275604243076108288L,
            -5263502140384954880L, -5251235803114780672L, -5238803040260463616L, -5226201609860500992L,
            -5213429220396639232L, -5200483526689941504L, -5187362130814278656L, -5174062579249043456L,
            -5160582362536272384L, -5146918913003260928L, -5133069603645442048L, -5119031746455840768L,
            -5104802590846193664L, -5090379322288435200L, -5075759059905230848L, -5060938855168388608L,
            -5045915690136155648L, -5030686475123909120L, -5015248047351192576L, -4999597168201581568L,
            -4983730521429875200L, -4967644711103290880L, -4951336259902648320L, -4934801604331100672L,
            -4918037096313518080L, -4901038996556133376L, -4883803475152384512L, -4866326606521733120L,
            -4848604368549652480L, -4830632638500906496L, -4812407189788950016L, -4793923690356844544L,
            -4775177697581510656L, -4756164656502554112L, -4736879895545858048L, -4717318622225770496L,
            -4697475921931613696L, -4677346750468523008L, -4656925933129603072L, -4636208158837948416L,
            -4615187976257695744L, -4593859789223797248L, -4572217851458716672L, -4550256263721420288L,
            -4527968965738787328L, -4505349732602470400L, -4482392170596992000L, -4459089707595035648L,
            -4435435591206243840L, -4411422880135513088L, -4387044438367173632L, -4362292928394443264L,
            -4337160804216032256L, -4311640303794398208L, -4285723442340192768L, -4259402002490114048L,
            -4232667527557880832L, -4205511312870738944L, -4177924395156510720L, -4149897544928282624L,
            -4121421255517056000L, -4092485732978126848L, -4063080884857030656L, -4033196309983527424L,
            -4002821285452889600L, -3971944754955299840L, -3940555315053852672L, -3908641203541062144L,
            -3876190282046383616L, -3843190024121122816L, -3809627498134461952L, -3775489350771450880L,
            -3740761791237469184L, -3705430570995169280L, -3669480966726291456L, -3632897760460073472L,
            -3595665216111472128L, -3557767061510936064L, -3519186461826909184L, -3479905997291117056L,
            -3439907636209809920L, -3399172709277578752L, -3357681881315035136L, -3315415118925273600L,
            -3272351662607639040L, -3228469989824430592L, -3183747783206633472L, -3138161891189001728L,
            -3091688288710896128L, -3044302038074020352L, -2995977242912180224L, -2946687001898157056L,
            -2896403361643595776L, -2845097261899269632L, -2792738483041489920L, -2739295585832944128L,
            -2684735849802800128L, -2629025208318833152L, -2572128177407577088L, -2514007780971290112L,
            -2454625474210264064L, -2393941056301004288L, -2331912582463081472L, -2268496267820575232L,
            -2203646385063500288L, -2137315156805725696L, -2069452637212809728L, -2000006589650226176L,
            -1928922352643083264L, -1856142697389204480L, -1781607676495585792L, -1705254458055283200L,
            -1627017152968592896L, -1546826624346926592L, -1464610284716681216L, -1380291880647352832L,
            -1293791253775522304L, -1205024091744495104L, -1113901652592291328L, -1020330470192921600L,
            -924212036524504576L, -825442455107712512L, -723912067910252544L, -619505050467681792L,
            -512098970851033088L, -401564310549775872L, -287763946399309312L, -170552579279989760L, -49776119384483328L,
            74728993660240384L, 203136528144344576L, 335631271254235136L, 472409917558910464L, 613682045009508352L,
            759671188745830912L, 910616024566752768L, 1066771674457471488L, 1228411150191648768L, 1395826950915523584L,
            1569332837797146112L, 1749265802232386560L, 1935988261896280064L, 2129890506883624960L,
            2331393435927328256L, 2540951622664266752L, 2759056758111070720L, 2986241520688130560L,
            3223083948864353280L, 3470212380253329408L, 3728311054271952896L, 3998126479196178432L,
            4280474694750398976L, 4576249573583467520L, 4886432342140325376L, 5212102538692988928L,
            5554450665570580992L, 5914792845058133504L, 6294587870200374272L, 6695457110790894080L,
            7119207852916254720L, 7567860785399318528L, 8043682516018548736L, 8549224231795344384L,
            9087367898325182976L, 2686433895585048064L, 3738049383351049728L, 5447119168340997632L,
            7862383433967084544L, 3564125970404768256L, 7561510489765503488L, 4795007330430237184L,
            2822776087249664512L, 1739199416312530432L, 1651067950292976640L, 2680178586723521024L,
            4966620264299254784L, 8672981687287425024L, 5102073364374541824L, 2973953244697943040L,
            2586847729538911744L, 4294392489026305536L, 8524338069805147648L, 5657549542711693824L,
            6039225349920109568L, -188437589949716992L, -1600155058499635712L, 3512467924097231872L,
            5888549710424099840L, 8328892371572499456L, 3607211374099356160L, 952847323024169472L,
            -3124063727137777664L, -844200026382571008L, 1173948101715934208L, -9223372036854775808L,
            -9223372036854775808L};
        /**
         * The precomputed ziggurat lengths, denoted X_i in the main text. X_i = length of
         * ziggurat layer i. Values have been scaled by 2^-63.
         */
        private static final double[] X = {4.1588525861581104e-19, 3.9503459916661627e-19, 3.821680975424891e-19,
            3.727004572118549e-19, 3.6514605982514084e-19, 3.5882746800626676e-19, 3.533765597048754e-19,
            3.485701802710972e-19, 3.4426246437790343e-19, 3.403526438955331e-19, 3.367680884141069e-19,
            3.334546590044289e-19, 3.303708833135166e-19, 3.274842648111545e-19, 3.2476884984205335e-19,
            3.2220356973333147e-19, 3.1977107871867837e-19, 3.174569193558674e-19, 3.152489103227302e-19,
            3.1313668890638164e-19, 3.111113634164771e-19, 3.091652452001866e-19, 3.0729163928347323e-19,
            3.0548467885197293e-19, 3.0373919296832198e-19, 3.0205059980435e-19, 3.0041481968537306e-19,
            2.9882820368030914e-19, 2.9728747450808126e-19, 2.957896772888449e-19, 2.943321382295927e-19,
            2.929124297535286e-19, 2.915283409000532e-19, 2.901778520645554e-19, 2.8885911333390034e-19,
            2.875704258185245e-19, 2.8631022549560207e-19, 2.8507706916730813e-19, 2.8386962220934334e-19,
            2.8268664784176013e-19, 2.815269976998848e-19, 2.803896035201538e-19, 2.7927346978581e-19,
            2.7817766720204774e-19, 2.7710132689045585e-19, 2.7604363520934134e-19, 2.750038291204046e-19,
            2.739811920338075e-19, 2.7297505007336127e-19, 2.7198476871169526e-19, 2.710097497321303e-19,
            2.700494284797851e-19, 2.691032713693778e-19, 2.681707736213845e-19, 2.6725145720181144e-19,
            2.6634486894391515e-19, 2.6545057883285584e-19, 2.6456817843655206e-19, 2.636972794679811e-19,
            2.6283751246588273e-19, 2.6198852558231173e-19, 2.6114998346678357e-19, 2.6032156623788965e-19,
            2.5950296853425164e-19, 2.586938986375541e-19, 2.5789407766116098e-19, 2.5710323879849453e-19,
            2.5632112662595285e-19, 2.555474964556656e-19, 2.547821137338583e-19, 2.5402475348100586e-19,
            2.532751997703282e-19, 2.5253324524150594e-19, 2.517986906467905e-19, 2.5107134442694197e-19,
            2.503510223146645e-19, 2.4963754696341833e-19, 2.4893074759967754e-19, 2.4823045969687053e-19,
            2.475365246693953e-19, 2.4684878958523816e-19, 2.4616710689584975e-19, 2.4549133418204393e-19,
            2.448213339147886e-19, 2.4415697322984843e-19, 2.4349812371532436e-19, 2.4284466121121044e-19,
            2.42196465620158e-19, 2.4155342072870043e-19, 2.409154140382495e-19, 2.402823366052261e-19,
            2.3965408288973736e-19, 2.390305506122548e-19, 2.384116406177891e-19, 2.377972567470941e-19,
            2.3718730571446517e-19, 2.3658169699173055e-19, 2.3598034269805933e-19, 2.353831574952395e-19,
            2.3479005848810095e-19, 2.342009651297822e-19, 2.336157991315592e-19, 2.33034484376974e-19,
            2.3245694684001817e-19, 2.3188311450714236e-19, 2.313129173028786e-19, 2.307462870188744e-19,
            2.3018315724615257e-19, 2.2962346331042115e-19, 2.290671422102686e-19, 2.285141325580919e-19,
            2.279643745236112e-19, 2.2741780977983687e-19, 2.2687438145136136e-19, 2.263340340648552e-19,
            2.2579671350165625e-19, 2.2526236695234495e-19, 2.247309428732065e-19, 2.2420239094448635e-19,
            2.236766620303496e-19, 2.231537081404625e-19, 2.2263348239311565e-19, 2.2211593897981584e-19,
            2.216010331312762e-19, 2.2108872108473813e-19, 2.205789600525628e-19, 2.200717081920328e-19,
            2.1956692457630854e-19, 2.1906456916648504e-19, 2.185646027847011e-19, 2.1806698708825153e-19,
            2.1757168454465786e-19, 2.170786584076557e-19, 2.1658787269405759e-19, 2.1609929216145222e-19,
            2.156128822867056e-19, 2.1512860924522763e-19, 2.1464643989097195e-19, 2.1416634173713822e-19,
            2.1368828293754638e-19, 2.1321223226865536e-19, 2.1273815911219872e-19, 2.122660334384123e-19,
            2.1179582578982903e-19, 2.1132750726561794e-19, 2.1086104950644545e-19, 2.103964246798376e-19,
            2.099336054660232e-19, 2.09472565044239e-19, 2.090132770794785e-19, 2.0855571570966692e-19,
            2.0809985553324565e-19, 2.0764567159715066e-19, 2.0719313938516928e-19, 2.0674223480666103e-19,
            2.0629293418562883e-19, 2.0584521425012706e-19, 2.05399052121994e-19, 2.049544253068964e-19,
            2.0451131168467476e-19, 2.0406968949997815e-19, 2.0362953735317784e-19, 2.0319083419154967e-19,
            2.027535593007156e-19, 2.0231769229633464e-19, 2.0188321311603464e-19, 2.014501020115762e-19,
            2.0101833954124038e-19, 2.0058790656243254e-19, 2.0015878422449446e-19, 1.9973095396171768e-19,
            1.9930439748655098e-19, 1.988790967829954e-19, 1.984550341001801e-19, 1.9803219194611336e-19,
            1.9761055308160207e-19, 1.9719010051433485e-19, 1.967708174931225e-19, 1.9635268750229097e-19,
            1.9593569425622174e-19, 1.955198216940345e-19, 1.9510505397440756e-19, 1.9469137547053159e-19,
            1.9427877076519196e-19, 1.9386722464597592e-19, 1.9345672210060023e-19, 1.9304724831235547e-19,
            1.9263878865566334e-19, 1.9223132869174325e-19, 1.918248541643845e-19, 1.9141935099582133e-19,
            1.9101480528270662e-19, 1.9061120329218205e-19, 1.9020853145804105e-19, 1.8980677637698202e-19,
            1.8940592480494855e-19, 1.8900596365355448e-19, 1.8860687998659074e-19, 1.8820866101661152e-19,
            1.8781129410159744e-19, 1.874147667416932e-19, 1.8701906657601756e-19, 1.8662418137954315e-19,
            1.862300990600444e-19, 1.85836807655111e-19, 1.8544429532922543e-19, 1.8505255037090195e-19,
            1.8466156118988594e-19, 1.8427131631441095e-19, 1.8388180438851245e-19, 1.8349301416939593e-19,
            1.8310493452485817e-19, 1.8271755443075968e-19, 1.823308629685471e-19, 1.8194484932282376e-19,
            1.8155950277896707e-19, 1.8117481272079125e-19, 1.8079076862825417e-19, 1.804073600752067e-19,
            1.8002457672718337e-19, 1.7964240833923322e-19, 1.7926084475378945e-19, 1.7887987589857656e-19,
            1.7849949178455405e-19, 1.7811968250389552e-19, 1.7774043822800183e-19, 1.773617492055474e-19,
            1.7698360576055882e-19, 1.7660599829052424e-19, 1.7622891726453298e-19, 1.7585235322144435e-19,
            1.754762967680843e-19, 1.751007385774697e-19, 1.7472566938705867e-19, 1.743510799970264e-19,
            1.7397696126856576e-19, 1.7360330412221144e-19, 1.7323009953618705e-19, 1.7285733854477456e-19,
            1.7248501223670475e-19, 1.7211311175356858e-19, 1.7174162828824813e-19, 1.7137055308336676e-19,
            1.709998774297575e-19, 1.7062959266494937e-19, 1.7025969017167022e-19, 1.6989016137636627e-19,
            1.6952099774773704e-19, 1.6915219079528517e-19, 1.6878373206788065e-19, 1.6841561315233867e-19,
            1.6804782567201046e-19, 1.6768036128538652e-19, 1.6731321168471171e-19, 1.6694636859461146e-19,
            1.6657982377072854e-19, 1.662135689983699e-19, 1.6584759609116298e-19, 1.6548189688972057e-19,
            1.6511646326031438e-19, 1.6475128709355596e-19, 1.6438636030308492e-19, 1.640216748242637e-19,
            1.6365722261287829e-19, 1.632929956438448e-19, 1.6292898590992037e-19, 1.625651854204191e-19,
            1.6220158619993133e-19, 1.618381802870466e-19, 1.6147495973307924e-19, 1.6111191660079618e-19,
            1.6074904296314645e-19, 1.6038633090199211e-19, 1.600237725068394e-19, 1.5966135987357025e-19,
            1.5929908510317342e-19, 1.5893694030047434e-19, 1.5857491757286376e-19, 1.5821300902902412e-19,
            1.5785120677765333e-19, 1.5748950292618545e-19, 1.5712788957950753e-19, 1.5676635883867226e-19,
            1.5640490279960565e-19, 1.560435135518094e-19, 1.5568218317705714e-19, 1.553209037480841e-19,
            1.5495966732726971e-19, 1.545984659653122e-19, 1.54237291699895e-19, 1.5387613655434405e-19,
            1.5351499253627542e-19, 1.531538516362328e-19, 1.527927058263138e-19, 1.5243154705878517e-19,
            1.5207036726468514e-19, 1.517091583524134e-19, 1.5134791220630703e-19, 1.509866206852025e-19,
            1.506252756209824e-19, 1.5026386881710618e-19, 1.499023920471249e-19, 1.4954083705317816e-19,
            1.4917919554447328e-19, 1.488174591957453e-19, 1.4845561964569755e-19, 1.480936684954214e-19,
            1.4773159730679478e-19, 1.4736939760085815e-19, 1.470070608561676e-19, 1.4664457850712328e-19,
            1.462819419422733e-19, 1.4591914250259098e-19, 1.4555617147972526e-19, 1.4519302011422298e-19,
            1.4482967959372176e-19, 1.4446614105111258e-19, 1.4410239556267103e-19, 1.437384341461557e-19,
            1.433742477588729e-19, 1.4300982729570602e-19, 1.4264516358710902e-19, 1.4228024739706157e-19,
            1.4191506942098562e-19, 1.4154962028362133e-19, 1.4118389053686103e-19, 1.4081787065753974e-19,
            1.4045155104518092e-19, 1.4008492201969533e-19, 1.3971797381903188e-19, 1.3935069659677833e-19,
            1.3898308041971047e-19, 1.3861511526528749e-19, 1.3824679101909196e-19, 1.3787809747221246e-19,
            1.3750902431856644e-19, 1.3713956115216185e-19, 1.3676969746429448e-19, 1.3639942264067964e-19,
            1.3602872595851507e-19, 1.3565759658347313e-19, 1.3528602356661948e-19, 1.3491399584125558e-19,
            1.345415022196824e-19, 1.3416853138988266e-19, 1.3379507191211783e-19, 1.3342111221543818e-19,
            1.3304664059410112e-19, 1.3267164520389587e-19, 1.3229611405837e-19, 1.3192003502495473e-19,
            1.315433958209853e-19, 1.311661840096121e-19, 1.3078838699559864e-19, 1.3040999202100255e-19,
            1.3003098616073445e-19, 1.2965135631799062e-19, 1.2927108921955436e-19, 1.2889017141096131e-19,
            1.285085892515231e-19, 1.281263289092043e-19, 1.2774337635534646e-19, 1.2735971735923388e-19,
            1.269753374824942e-19, 1.2659022207332785e-19, 1.262043562605593e-19, 1.2581772494750294e-19,
            1.2543031280563616e-19, 1.250421042680721e-19, 1.2465308352282341e-19, 1.2426323450584884e-19,
            1.238725408938736e-19, 1.2348098609697396e-19, 1.230885532509165e-19, 1.2269522520924136e-19,
            1.22300984535079e-19, 1.2190581349268875e-19, 1.2150969403870742e-19, 1.2111260781309555e-19,
            1.2071453612976755e-19, 1.203154599668928e-19, 1.199153599568521e-19, 1.1951421637583513e-19,
            1.191120091330619e-19, 1.187087177596121e-19, 1.1830432139684355e-19, 1.1789879878438187e-19,
            1.1749212824766058e-19, 1.1708428768499157e-19, 1.1667525455414306e-19, 1.1626500585840243e-19,
            1.1585351813209874e-19, 1.1544076742555945e-19, 1.150267292894733e-19, 1.1461137875863088e-19,
            1.1419469033501178e-19, 1.1377663797018575e-19, 1.1335719504699387e-19, 1.129363343604726e-19,
            1.1251402809798253e-19, 1.1209024781850075e-19, 1.1166496443103302e-19, 1.112381481720998e-19,
            1.1080976858224708e-19, 1.1037979448152952e-19, 1.0994819394391104e-19, 1.0951493427052315e-19,
            1.0907998196171864e-19, 1.0864330268785362e-19, 1.0820486125872634e-19, 1.0776462159159718e-19,
            1.0732254667770796e-19, 1.0687859854721439e-19, 1.0643273823243869e-19, 1.059849257293434e-19,
            1.055351199571202e-19, 1.0508327871578037e-19, 1.0462935864162494e-19, 1.0417331516046436e-19,
            1.0371510243844727e-19, 1.0325467333034829e-19, 1.0279197932515293e-19, 1.0232697048876598e-19,
            1.0185959540365581e-19, 1.013898011052333e-19, 1.009175330147477e-19, 1.0044273486846454e-19,
            9.996534864287244e-20, 9.948531447564429e-20, 9.900257058205621e-20, 9.851705316654313e-20,
            9.80286963290422e-20, 9.753743196574596e-20, 9.704318966385453e-20, 9.654589658987947e-20,
            9.60454773710132e-20, 9.554185396903316e-20, 9.503494554616229e-20, 9.452466832225325e-20,
            9.401093542260497e-20, 9.349365671565409e-20, 9.297273863971075e-20, 9.244808401782692e-20,
            9.191959185979496e-20, 9.13871571501726e-20, 9.085067062111788e-20, 9.03100185086906e-20,
            8.976508229113516e-20, 8.921573840749998e-20, 8.866185795476897e-20, 8.810330636147831e-20,
            8.753994303556269e-20, 8.697162098391639e-20, 8.639818640086038e-20, 8.581947822237302e-20,
            8.523532764256077e-20, 8.464555758841113e-20, 8.404998214837225e-20, 8.344840594973324e-20,
            8.2840623479122e-20, 8.222641833968076e-20, 8.160556243760346e-20, 8.097781508970379e-20,
            8.034292204250112e-20, 7.9700614391934e-20, 7.905060739119745e-20, 7.839259913230684e-20,
            7.772626908475868e-20, 7.705127647202012e-20, 7.636725846344508e-20, 7.567382815548135e-20,
            7.497057231156425e-20, 7.425704882472158e-20, 7.353278386042921e-20, 7.279726862938938e-20,
            7.204995573030996e-20, 7.12902549910028e-20, 7.051752872162244e-20, 6.97310862758892e-20,
            6.893017779370733e-20, 6.811398697040956e-20, 6.728162266220774e-20, 6.643210909198763e-20,
            6.556437436119487e-20, 6.467723689788537e-20, 6.376938937203267e-20, 6.28393794784348e-20,
            6.188558681299899e-20, 6.090619483242316e-20, 5.989915656489546e-20, 5.886215229253252e-20,
            5.779253679755271e-20, 5.668727286516658e-20, 5.554284642744753e-20, 5.435515678916088e-20,
            5.3119372426547794e-20, 5.182973825957843e-20, 5.047931295220812e-20, 4.9059602658819333e-20,
            4.756003683510097e-20, 4.5967194527575314e-20, 4.4263619567465964e-20, 4.242592320713738e-20,
            4.042157155716674e-20, 3.820304293025196e-20, 3.5696135223547374e-20, 3.277315994615917e-20,
            2.9176892376585344e-20, 2.4195231151204545e-20, 0.0};
        /** The precomputed ziggurat heights, denoted Y_i in the main text. Y_i = f(X_i).
         * Values have been scaled by 2^-63. */
        private static final double[] Y = {6.918899098832948e-23, 1.4202990535697683e-22, 2.1732316399259404e-22,
            2.9452908326033447e-22, 3.733322926509216e-22, 4.535231475217251e-22, 5.349509633185051e-22,
            6.175015744699501e-22, 7.01085131749555e-22, 7.856288599286739e-22, 8.710724705333867e-22,
            9.573651062292227e-22, 1.0444632219043918e-21, 1.1323290661676009e-21, 1.2209295628733119e-21,
            1.3102354679393653e-21, 1.4002207209079843e-21, 1.4908619375774252e-21, 1.582138006957303e-21,
            1.6740297667860313e-21, 1.7665197391694204e-21, 1.8595919128929468e-21, 1.9532315624377367e-21,
            2.0474250961975478e-21, 2.1421599281741028e-21, 2.2374243687320324e-21, 2.3332075309631245e-21,
            2.429499249937991e-21, 2.5262900126775297e-21, 2.6235708971028823e-21, 2.7213335185536967e-21,
            2.8195699827241005e-21, 2.9182728440709958e-21, 3.0174350689128386e-21, 3.1170500025683573e-21,
            3.217111339990819e-21, 3.3176130994398206e-21, 3.4185495988032995e-21, 3.5199154342406966e-21,
            3.621705460866417e-21, 3.723914775232861e-21, 3.8265386994058555e-21, 3.929572766453532e-21,
            4.03301270719345e-21, 4.1368544380629805e-21, 4.241094049995082e-21, 4.345727798196275e-21,
            4.450752092736164e-21, 4.5561634898686684e-21, 4.661958684014447e-21, 4.7681345003420505e-21,
            4.874687887892369e-21, 4.981615913197017e-21, 5.0889157543466486e-21, 5.196584695469838e-21,
            5.304620121587273e-21, 5.413019513809608e-21, 5.5217804448504855e-21, 5.630900574829082e-21,
            5.740377647338979e-21, 5.8502094857624066e-21, 5.9603939898108565e-21, 6.070929132274824e-21,
            6.181812955967001e-21, 6.2930435708446384e-21, 6.404619151298078e-21, 6.516537933593546e-21,
            6.628798213459353e-21, 6.741398343805535e-21, 6.85433673256781e-21, 6.967611840667461e-21,
            7.081222180079459e-21, 7.195166312001694e-21, 7.309442845118826e-21, 7.424050433954681e-21,
            7.538987777307655e-21, 7.65425361676395e-21, 7.769846735283895e-21, 7.885765955856934e-21,
            8.002010140221158e-21, 8.118578187643616e-21, 8.235469033757848e-21, 8.352681649455356e-21,
            8.47021503982796e-21, 8.58806824315818e-21, 8.706240329954993e-21, 8.824730402032477e-21,
            8.943537591629026e-21, 9.06266106056498e-21, 9.182099999436611e-21, 9.301853626844622e-21,
            9.421921188655315e-21, 9.542301957292816e-21, 9.662995231060764e-21, 9.784000333491997e-21,
            9.905316612724868e-21, 1.0026943440904879e-20, 1.0148880213610417e-20, 1.0271126349301457e-20,
            1.0393681288790118e-20, 1.0516544494732088e-20, 1.0639715451137926e-20, 1.076319366290336e-20,
            1.088697865535769e-20, 1.1011069973829531e-20, 1.1135467183229089e-20, 1.1260169867646259e-20,
            1.1385177629963887e-20, 1.1510490091485507e-20, 1.1636106891576963e-20, 1.176202768732133e-20,
            1.1888252153186578e-20, 1.2014779980705474e-20, 1.2141610878167194e-20, 1.2268744570320195e-20,
            1.2396180798085917e-20, 1.2523919318282839e-20, 1.2651959903360538e-20, 1.2780302341143342e-20,
            1.2908946434583201e-20, 1.3037892001521468e-20, 1.3167138874459203e-20, 1.3296686900335739e-20,
            1.342653594031518e-20, 1.3556685869580544e-20, 1.3687136577135297e-20, 1.3817887965612e-20,
            1.3948939951087837e-20, 1.4080292462906762e-20, 1.421194544350808e-20, 1.4343898848261192e-20,
            1.447615264530638e-20, 1.4608706815401313e-20, 1.474156135177323e-20, 1.4874716259976492e-20,
            1.5008171557755435e-20, 1.5141927274912272e-20, 1.527598345317996e-20, 1.5410340146099826e-20,
            1.554499741890387e-20, 1.5679955348401513e-20, 1.5815214022870783e-20, 1.5950773541953694e-20,
            1.6086634016555787e-20, 1.622279556874966e-20, 1.6359258331682407e-20, 1.649602244948686e-20,
            1.6633088077196497e-20, 1.6770455380664e-20, 1.690812453648326e-20, 1.7046095731914825e-20,
            1.7184369164814694e-20, 1.7322945043566328e-20, 1.7461823587015858e-20, 1.760100502441039e-20,
            1.77404895953393e-20, 1.7880277549678543e-20, 1.8020369147537807e-20, 1.8160764659210518e-20,
            1.8301464365126615e-20, 1.844246855580801e-20, 1.8583777531826755e-20, 1.872539160376576e-20,
            1.8867311092182093e-20, 1.900953632757279e-20, 1.91520676503431e-20, 1.9294905410777168e-20,
            1.9438049969011095e-20, 1.9581501695008306e-20, 1.9725260968537243e-20, 1.9869328179151295e-20,
            2.0013703726170975e-20, 2.015838801866827e-20, 2.030338147545316e-20, 2.0448684525062275e-20,
            2.0594297605749654e-20, 2.074022116547955e-20, 2.0886455661921353e-20, 2.1033001562446463e-20,
            2.1179859344127223e-20, 2.132702949373782e-20, 2.1474512507757144e-20, 2.1622308892373594e-20,
            2.177041916349182e-20, 2.191884384674137e-20, 2.2067583477487234e-20, 2.221663860084227e-20,
            2.2366009771681506e-20, 2.251569755465829e-20, 2.2665702524222297e-20, 2.2816025264639365e-20,
            2.2966666370013165e-20, 2.3117626444308697e-20, 2.326890610137758e-20, 2.342050596498518e-20,
            2.357242666883951e-20, 2.3724668856621966e-20, 2.387723318201982e-20, 2.4030120308760546e-20,
            2.4183330910647897e-20, 2.433686567159983e-20, 2.4490725285688176e-20, 2.464491045718012e-20,
            2.4799421900581494e-20, 2.4954260340681856e-20, 2.5109426512601375e-20, 2.5264921161839525e-20,
            2.5420745044325602e-20, 2.5576898926471056e-20, 2.5733383585223655e-20, 2.5890199808123478e-20,
            2.604734839336077e-20, 2.620483014983564e-20, 2.6362645897219624e-20, 2.6520796466019148e-20,
            2.667928269764085e-20, 2.6838105444458826e-20, 2.699726556988379e-20, 2.715676394843416e-20,
            2.731660146580911e-20, 2.747677901896354e-20, 2.7637297516185117e-20, 2.77981578771732e-20,
            2.795936103311988e-20, 2.812090792679301e-20, 2.8282799512621313e-20, 2.8445036756781554e-20,
            2.860762063728785e-20, 2.877055214408307e-20, 2.893383227913239e-20, 2.909746205651907e-20,
            2.9261442502542373e-20, 2.9425774655817774e-20, 2.9590459567379366e-20, 2.975549830078463e-20,
            2.992089193222143e-20, 3.0086641550617475e-20, 3.0252748257752036e-20, 3.041921316837019e-20,
            3.058603741029946e-20, 3.075322212456892e-20, 3.0920768465530904e-20, 3.108867760098517e-20,
            3.125695071230579e-20, 3.142558899457052e-20, 3.159459365669304e-20, 3.176396592155776e-20,
            3.193370702615749e-20, 3.210381822173386e-20, 3.227430077392068e-20, 3.244515596289013e-20,
            3.261638508350196e-20, 3.278798944545568e-20, 3.2959970373445826e-20, 3.313232920732032e-20,
            3.330506730224202e-20, 3.347818602885351e-20, 3.3651686773445136e-20, 3.382557093812645e-20,
            3.399983994100101e-20, 3.417449521634467e-20, 3.434953821478746e-20, 3.4524970403498974e-20,
            3.470079326637752e-20, 3.487700830424301e-20, 3.505361703503359e-20, 3.523062099400631e-20,
            3.540802173394162e-20, 3.558582082535201e-20, 3.576401985669479e-20, 3.5942620434589016e-20,
            3.612162418403681e-20, 3.630103274864904e-20, 3.6480847790875463e-20, 3.6661070992239485e-20,
            3.684170405357761e-20, 3.702274869528362e-20, 3.720420665755768e-20, 3.7386079700660407e-20,
            3.7568369605172066e-20, 3.7751078172256915e-20, 3.7934207223932926e-20, 3.811775860334692e-20,
            3.830173417505529e-20, 3.848613582531033e-20, 3.86709654623525e-20, 3.8856225016708545e-20,
            3.904191644149573e-20, 3.922804171273228e-20, 3.9414602829654225e-20, 3.960160181503872e-20,
            3.978904071553405e-20, 3.9976921601996467e-20, 4.016524656983399e-20, 4.035401773935739e-20,
            4.0543237256138495e-20, 4.0732907291375956e-20, 4.092303004226878e-20, 4.111360773239764e-20,
            4.1304642612114334e-20, 4.1496136958939466e-20, 4.1688093077968586e-20, 4.1880513302287083e-20,
            4.207339999339389e-20, 4.226675554163437e-20, 4.2460582366642555e-20, 4.265488291779298e-20,
            4.2849659674662346e-20, 4.3044915147501306e-20, 4.3240651877716606e-20, 4.343687243836386e-20,
            4.3633579434651235e-20, 4.383077550445433e-20, 4.402846331884258e-20, 4.4226645582617465e-20,
            4.44253250348628e-20, 4.4624504449507564e-20, 4.482418663590143e-20, 4.502437443940352e-20,
            4.522507074198459e-20, 4.5426278462843173e-20, 4.562800055903592e-20, 4.583024002612264e-20,
            4.603299989882643e-20, 4.6236283251709275e-20, 4.6440093199863635e-20, 4.66444328996204e-20,
            4.684930554927375e-20, 4.705471438982336e-20, 4.726066270573451e-20, 4.746715382571657e-20,
            4.7674191123520394e-20, 4.7881778018755285e-20, 4.8089917977726014e-20, 4.829861451429048e-20,
            4.8507871190738755e-20, 4.871769161869405e-20, 4.8928079460036324e-20, 4.913903842784919e-20,
            4.935057228739092e-20, 4.9562684857090195e-20, 4.977538000956748e-20, 4.9988661672682747e-20,
            5.0202533830610375e-20, 5.0417000524942233e-20, 5.0632065855819656e-20, 5.084773398309539e-20,
            5.106400912752639e-20, 5.1280895571998574e-20, 5.1498397662784496e-20, 5.1716519810835054e-20,
            5.1935266493106415e-20, 5.215464225392324e-20, 5.237465170637957e-20, 5.259529953377852e-20,
            5.281659049111218e-20, 5.303852940658309e-20, 5.326112118316879e-20, 5.348437080023076e-20,
            5.370828331516958e-20, 5.3932863865127733e-20, 5.4158117668741825e-20, 5.4384050027946e-20,
            5.461066632982836e-20, 5.483797204854241e-20, 5.506597274727535e-20, 5.529467408027557e-20,
            5.55240817949413e-20, 5.575420173397287e-20, 5.598503983759089e-20, 5.62166021458229e-20,
            5.644889480086096e-20, 5.668192404949321e-20, 5.691569624561198e-20, 5.715021785280148e-20,
            5.738549544700838e-20, 5.762153571929838e-20, 5.785834547870229e-20, 5.809593165515515e-20,
            5.833430130253229e-20, 5.857346160178616e-20, 5.881341986418809e-20, 5.905418353467947e-20,
            5.929576019533679e-20, 5.953815756895528e-20, 5.978138352275646e-20, 6.00254460722246e-20,
            6.02703533850779e-20, 6.051611378537999e-20, 6.076273575779827e-20, 6.101022795201527e-20,
            6.125859918729998e-20, 6.150785845724639e-20, 6.175801493468681e-20, 6.200907797678796e-20,
            6.226105713033821e-20, 6.251396213723503e-20, 6.276780294018204e-20, 6.30225896886053e-20,
            6.327833274479995e-20, 6.353504269031761e-20, 6.37927303326069e-20, 6.405140671191905e-20,
            6.431108310849226e-20, 6.457177105002818e-20, 6.483348231947592e-20, 6.509622896313888e-20,
            6.536002329912114e-20, 6.562487792613134e-20, 6.589080573266256e-20, 6.615781990656834e-20,
            6.642593394505607e-20, 6.669516166512051e-20, 6.696551721444132e-20, 6.723701508277054e-20,
            6.750967011383736e-20, 6.778349751779927e-20, 6.805851288427119e-20, 6.83347321959656e-20,
            6.861217184297964e-20, 6.889084863776731e-20, 6.917077983083762e-20, 6.945198312722278e-20,
            6.973447670376329e-20, 7.001827922726053e-20, 7.030340987355113e-20, 7.058988834756152e-20,
            7.087773490440541e-20, 7.11669703715918e-20, 7.145761617241673e-20, 7.174969435061705e-20,
            7.204322759637158e-20, 7.233823927374128e-20, 7.263475344964812e-20, 7.293279492450004e-20,
            7.323238926457911e-20, 7.353356283631946e-20, 7.383634284261274e-20, 7.414075736129108e-20,
            7.444683538595064e-20, 7.475460686929375e-20, 7.50641027691839e-20, 7.537535509762588e-20,
            7.568839697290355e-20, 7.600326267512978e-20, 7.631998770548812e-20, 7.663860884947315e-20,
            7.695916424446765e-20, 7.728169345202874e-20, 7.760623753529426e-20, 7.793283914196369e-20,
            7.826154259335684e-20, 7.859239398010826e-20, 7.892544126511772e-20, 7.926073439444657e-20,
            7.959832541692984e-20, 7.993826861336392e-20, 8.028062063623232e-20, 8.062544066104959e-20,
            8.097279055053708e-20, 8.132273503299865e-20, 8.167534189644072e-20, 8.20306822001853e-20,
            8.238883050596047e-20, 8.274986513072568e-20, 8.311386842380793e-20, 8.348092707129561e-20,
            8.385113243107135e-20, 8.422458090237601e-20, 8.460137433439796e-20, 8.498162047909447e-20,
            8.53654334942995e-20, 8.575293450418301e-20, 8.614425222533939e-20, 8.653952366824257e-20,
            8.693889492557206e-20, 8.734252206106456e-20, 8.775057211517437e-20, 8.816322424706147e-20,
            8.858067103642946e-20, 8.900311997372407e-20, 8.943079517345898e-20, 8.9863939353342e-20,
            9.030281613194174e-20, 9.074771271056305e-20, 9.119894302175006e-20, 9.165685144874882e-20,
            9.212181724922669e-20, 9.259425985526568e-20, 9.30746452740382e-20, 9.35634938854098e-20,
            9.406139003264672e-20, 9.45689939436514e-20, 9.50870567233135e-20, 9.56164394555198e-20,
            9.615813789991196e-20, 9.671331495418217e-20, 9.728334413500305e-20, 9.786986909344308e-20,
            9.847488715746005e-20, 9.910087013741517e-20, 9.975094533894022e-20, 1.004291788159128e-19,
            1.0114104330564085e-19, 1.0189424721829993e-19, 1.0270034796927329e-19, 1.0357834330014222e-19,
            1.0456455804293095e-19, 1.0575382882239675e-19, 1.0842021724855044e-19};

        /** Underlying source of randomness. */
        protected final UniformRandomProvider rng;
        /** Exponential sampler used for the long tail. */
        protected final ContinuousSampler exponential;

        /**
         * @param rng Generator of uniformly distributed random numbers.
         */
        ModifiedZigguratNormalizedGaussianSampler512(UniformRandomProvider rng) {
            this.rng = rng;
            exponential = new ModifiedZigguratExponentialSampler512(rng);
        }

        /** {@inheritDoc} */
        @Override
        public double sample() {
            final long xx = nextLong();
            // Float multiplication squashes these last 9 bits, so they can be used to sample i
            final int i = ((int) xx) & 0x1ff;

            if (i < I_MAX) {
                // Early exit.
                return X[i] * xx;
            }

            // Recycle bits then advance RNG:
            long u1 = xx & MAX_INT64;
            // Another squashed, recyclable bit
            // Use 2 - 1 or 0 - 1
            final double signBit = ((u1 >>> 8) & 0x2) - 1.0;
            final int j = selectRegion();
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
                for (;;) {
                    x = sampleX(X, j, u1);
                    final long uDistance = randomInt63() - u1;
                    if (uDistance >= 0) {
                        // Lower-left triangle
                        break;
                    }
                    if (uDistance >= CONVEX_E_MAX &&
                        // Within maximum distance of f(x) from the triangle hypotenuse.
                        sampleY(Y, j, u1 + uDistance) < Math.exp(-0.5 * x * x)) {
                        break;
                    }
                    // uDistance < E_MAX (upper-right triangle) or rejected as above the curve
                    u1 = randomInt63();
                }
            } else if (j < J_INFLECTION) {
                if (j == 0) {
                    // Tail
                    // Note: Although less frequent than the next branch, j == 0 is a subset of
                    // j < J_INFLECTION and must be first.
                    do {
                        x = ONE_OVER_X_0 * exponential.sample();
                    } while (exponential.sample() < 0.5 * x * x);
                    x += X_0;
                } else {
                    // Concave overhang
                    for (;;) {
                        // U_x <- min(U_1, U_2)
                        // distance <- | U_1 - U_2 |
                        // U_y <- 1 - (U_x + distance)
                        long uDistance = randomInt63() - u1;
                        if (uDistance < 0) {
                            uDistance = -uDistance;
                            u1 -= uDistance;
                        }
                        x = sampleX(X, j, u1);
                        if (uDistance > CONCAVE_E_MAX ||
                            sampleY(Y, j, u1 + uDistance) < Math.exp(-0.5 * x * x)) {
                            break;
                        }
                        u1 = randomInt63();
                    }
                }
            } else {
                // Inflection point
                for (;;) {
                    x = sampleX(X, j, u1);
                    if (sampleY(Y, j, randomInt63()) < Math.exp(-0.5 * x * x)) {
                        break;
                    }
                    u1 = randomInt63();
                }
            }
            return signBit * x;
        }

        /**
         * Select the overhang region or the tail using alias sampling.
         *
         * @return the region
         */
        protected int selectRegion() {
            final long x = nextLong();
            // j in [0, 512)
            final int j = ((int) x) & 0x1ff;
            // map to j in [0, N] with N the number of layers of the ziggurat
            return x >= IPMF[j] ? MAP[j] : j;
        }

        /**
         * Generates a {@code long}.
         *
         * @return the long
         */
        protected long nextLong() {
            return rng.nextLong();
        }

        /**
         * Return a positive long in {@code [0, 2^63)}.
         *
         * @return the long
         */
        protected long randomInt63() {
            return rng.nextLong() & MAX_INT64;
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
     * <p>This class uses the same tables as the production version
     * {@link org.apache.commons.rng.sampling.distribution.ZigguratSampler.Exponential}
     * with the overhang sampling matching the reference c implementation. Methods and members
     * are protected to allow the implementation to be modified in sub-classes.
     *
     * @see <a href="https://www.tandfonline.com/doi/abs/10.1080/00949655.2015.1060234">
     * McFarland (2016) JSCS 86, 1281-1294</a>
     */
    static class ModifiedZigguratExponentialSampler implements ContinuousSampler {
        // Ziggurat volumes:
        // Inside the layers              = 98.4375%  (252/256)
        // Fraction outside the layers:
        // concave overhangs              = 96.6972%
        // tail                           =  3.3028%

        /** The number of layers in the ziggurat. Maximum i value for early exit. */
        protected static final int I_MAX = 252;
        /** Maximum deviation of concave pdf(x) below the hypotenuse value for early exit.
         * Equal to approximately 0.0926 scaled by 2^63. */
        protected static final long E_MAX = 853965788476313646L;
        /** Beginning of tail. Equal to X[0] * 2^63. */
        protected static final double X_0 = 7.569274694148063;

        /** The alias map. An integer in [0, 255] stored as a byte to save space. */
        protected static final byte[] MAP = toBytes(new int[] {0, 0, 1, 235, 3, 4, 5, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            1, 1, 1, 1, 2, 2, 252, 252, 252, 252, 252, 252, 252, 252, 252, 252, 252, 252, 252, 252, 252, 252, 252, 252,
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
        protected static final long[] IPMF = {9223372036854774016L, 1623796909450834944L, 2664290944894291200L,
            7387971354164060928L, 6515064486552723200L, 8840508362680718848L, 6099647593382936320L,
            7673130333659513856L, 6220332867583438080L, 5045979640552813824L, 4075305837223955456L,
            3258413672162525440L, 2560664887087762432L, 1957224924672899584L, 1429800935350577408L, 964606309710808320L,
            551043923599587072L, 180827629096890368L, -152619738120023552L, -454588624410291456L, -729385126147774976L,
            -980551509819447040L, -1211029700667463936L, -1423284293868548352L, -1619396356369050368L,
            -1801135830956211712L, -1970018048575618048L, -2127348289059705344L, -2274257249303686400L,
            -2411729520096655360L, -2540626634159181056L, -2661705860113406464L, -2775635634532450560L,
            -2883008316030465280L, -2984350790383654912L, -3080133339198116352L, -3170777096303091200L,
            -3256660348483819008L, -3338123885075136256L, -3415475560473299200L, -3488994201966428160L,
            -3558932970354473216L, -3625522261068041216L, -3688972217741989376L, -3749474917563782656L,
            -3807206277531056128L, -3862327722496843520L, -3914987649156779776L, -3965322714631865344L,
            -4013458973776895488L, -4059512885612783360L, -4103592206186241024L, -4145796782586128128L,
            -4186219260694347008L, -4224945717447275264L, -4262056226866285568L, -4297625367836519680L,
            -4331722680528537344L, -4364413077437472512L, -4395757214229401600L, -4425811824915135744L,
            -4454630025296932608L, -4482261588141290496L, -4508753193105288192L, -4534148654077808896L,
            -4558489126279958272L, -4581813295192216576L, -4604157549138257664L, -4625556137145255168L,
            -4646041313519104512L, -4665643470413305856L, -4684391259530326528L, -4702311703971761664L,
            -4719430301145103360L, -4735771117539946240L, -4751356876102087168L, -4766209036859133952L,
            -4780347871386013440L, -4793792531638892032L, -4806561113635132672L, -4818670716409306624L,
            -4830137496634465536L, -4840976719260837888L, -4851202804490348800L, -4860829371376460032L,
            -4869869278311657472L, -4878334660640771072L, -4886236965617427200L, -4893586984900802560L,
            -4900394884772702720L, -4906670234238885376L, -4912422031164496896L, -4917658726580119808L,
            -4922388247283532288L, -4926618016851066624L, -4930354975163335168L, -4933605596540651264L,
            -4936375906575303936L, -4938671497741366016L, -4940497543854575616L, -4941858813449629440L,
            -4942759682136114944L, -4943204143989086720L, -4943195822025528064L, -4942737977813206528L,
            -4941833520255033344L, -4940485013586738944L, -4938694684624359424L, -4936464429291795968L,
            -4933795818458825728L, -4930690103114057984L, -4927148218896864000L, -4923170790008275968L,
            -4918758132519213568L, -4913910257091645696L, -4908626871126539264L, -4902907380349533952L,
            -4896750889844272896L, -4890156204540531200L, -4883121829162554368L, -4875645967641781248L,
            -4867726521994927104L, -4859361090668103424L, -4850546966345113600L, -4841281133215539200L,
            -4831560263698491904L, -4821380714613447424L, -4810738522790066176L, -4799629400105481984L,
            -4788048727936307200L, -4775991551010514944L, -4763452570642114304L, -4750426137329494528L,
            -4736906242696389120L, -4722886510751377664L, -4708360188440089088L, -4693320135461421056L,
            -4677758813316108032L, -4661668273553489152L, -4645040145179241472L, -4627865621182772224L,
            -4610135444140930048L, -4591839890849345536L, -4572968755929961472L, -4553511334358205696L,
            -4533456402849101568L, -4512792200036279040L, -4491506405372580864L, -4469586116675402496L,
            -4447017826233107968L, -4423787395382284800L, -4399880027458416384L, -4375280239014115072L,
            -4349971829190472192L, -4323937847117721856L, -4297160557210933504L, -4269621402214949888L,
            -4241300963840749312L, -4212178920821861632L, -4182234004204451584L, -4151443949668877312L,
            -4119785446662287616L, -4087234084103201536L, -4053764292396156928L, -4019349281473081856L,
            -3983960974549692672L, -3947569937258423296L, -3910145301787345664L, -3871654685619032064L,
            -3832064104425388800L, -3791337878631544832L, -3749438533114327552L, -3706326689447984384L,
            -3661960950051848192L, -3616297773528534784L, -3569291340409189376L, -3520893408440946176L,
            -3471053156460654336L, -3419717015797782528L, -3366828488034805504L, -3312327947826460416L,
            -3256152429334010368L, -3198235394669719040L, -3138506482563172864L, -3076891235255162880L,
            -3013310801389730816L, -2947681612411374848L, -2879915029671670784L, -2809916959107513856L,
            -2737587429961866240L, -2662820133571325696L, -2585501917733380096L, -2505512231579385344L,
            -2422722515205211648L, -2336995527534088448L, -2248184604988727552L, -2156132842510765056L,
            -2060672187261025536L, -1961622433929371904L, -1858790108950105600L, -1751967229002895616L,
            -1640929916937142784L, -1525436855617582592L, -1405227557075253248L, -1280020420662650112L,
            -1149510549536596224L, -1013367289578704896L, -871231448632104192L, -722712146453667840L,
            -567383236774436096L, -404779231966938368L, -234390647591545856L, -55658667960119296L, 132030985907841280L,
            329355128892811776L, 537061298001085184L, 755977262693564160L, 987022116608033280L, 1231219266829431296L,
            1489711711346518528L, 1763780090187553792L, 2054864117341795072L, 2364588157623768832L,
            2694791916990503168L, 3047567482883476224L, 3425304305830816256L, 3830744187097297920L,
            4267048975685830400L, 4737884547990017280L, 5247525842198998272L, 5800989391535355392L,
            6404202162993295360L, 7064218894258540544L, 7789505049452331520L, 8590309807749444864L,
            7643763810684489984L, 8891950541491446016L, 5457384281016206080L, 9083704440929284096L,
            7976211653914433280L, 8178631350487117568L, 2821287825726744832L, 6322989683301709568L,
            4309503753387611392L, 4685170734960170496L, 8404845967535199744L, 7330522972447554048L,
            1960945799076992000L, 4742910674644899072L, -751799822533509888L, 7023456603741959936L,
            3843116882594676224L, 3927231442413903104L, -9223372036854775808L, -9223372036854775808L,
            -9223372036854775808L};
        /**
         * The precomputed ziggurat lengths, denoted X_i in the main text. X_i = length of
         * ziggurat layer i. Values have been scaled by 2^-63.
         */
        protected static final double[] X = {8.206624067534882E-19, 7.397373235160728E-19, 6.913331337791529E-19,
            6.564735882096453E-19, 6.291253995981851E-19, 6.065722412960496E-19, 5.873527610373727E-19,
            5.705885052853694E-19, 5.557094569162239E-19, 5.423243890374395E-19, 5.301529769650878E-19,
            5.189873925770806E-19, 5.086692261799833E-19, 4.990749293879647E-19, 4.901062589444954E-19,
            4.816837901064919E-19, 4.737423865364471E-19, 4.662279580719682E-19, 4.590950901778405E-19,
            4.523052779065815E-19, 4.458255881635396E-19, 4.396276312636838E-19, 4.336867596710647E-19,
            4.2798143618469714E-19, 4.224927302706489E-19, 4.172039125346411E-19, 4.1210012522465616E-19,
            4.0716811225869233E-19, 4.0239599631006903E-19, 3.9777309342877357E-19, 3.93289757853345E-19,
            3.8893725129310323E-19, 3.8470763218720385E-19, 3.8059366138180143E-19, 3.765887213854473E-19,
            3.7268674692030177E-19, 3.688821649224816E-19, 3.651698424880007E-19, 3.6154504153287473E-19,
            3.5800337915318032E-19, 3.545407928453343E-19, 3.5115350988784242E-19, 3.478380203003096E-19,
            3.4459105288907336E-19, 3.4140955396563316E-19, 3.3829066838741162E-19, 3.3523172262289E-19,
            3.3223020958685874E-19, 3.292837750280447E-19, 3.263902052820205E-19, 3.2354741622810815E-19,
            3.207534433108079E-19, 3.180064325047861E-19, 3.1530463211820845E-19, 3.1264638534265134E-19,
            3.100301234693421E-19, 3.07454359701373E-19, 3.049176835000556E-19, 3.0241875541094565E-19,
            2.999563023214455E-19, 2.975291131074259E-19, 2.9513603463113224E-19, 2.9277596805684267E-19,
            2.9044786545442563E-19, 2.8815072666416712E-19, 2.858835963990693E-19, 2.8364556156331615E-19,
            2.81435748767798E-19, 2.7925332202553125E-19, 2.770974806115288E-19, 2.7496745707320232E-19,
            2.7286251537873397E-19, 2.7078194919206054E-19, 2.687250802641905E-19, 2.666912569315344E-19,
            2.646798527127889E-19, 2.6269026499668434E-19, 2.6072191381359757E-19, 2.5877424068465143E-19,
            2.568467075424817E-19, 2.549387957183548E-19, 2.530500049907748E-19, 2.511798526911271E-19,
            2.4932787286227806E-19, 2.474936154663866E-19, 2.456766456384867E-19, 2.438765429826784E-19,
            2.4209290090801527E-19, 2.403253260014054E-19, 2.3857343743505147E-19, 2.368368664061465E-19,
            2.3511525560671253E-19, 2.3340825872163284E-19, 2.3171553995306794E-19, 2.3003677356958333E-19,
            2.283716434784348E-19, 2.2671984281957174E-19, 2.250810735800194E-19, 2.234550462273959E-19,
            2.2184147936140775E-19, 2.2024009938224424E-19, 2.186506401748684E-19, 2.1707284280826716E-19,
            2.1550645524878675E-19, 2.1395123208673778E-19, 2.124069342755064E-19, 2.1087332888245875E-19,
            2.0935018885097035E-19, 2.0783729277295508E-19, 2.0633442467130712E-19, 2.0484137379170616E-19,
            2.0335793440326865E-19, 2.018839056075609E-19, 2.0041909115551697E-19, 1.9896329927183254E-19,
            1.975163424864309E-19, 1.9607803747261946E-19, 1.9464820489157862E-19, 1.9322666924284314E-19,
            1.9181325872045647E-19, 1.904078050744948E-19, 1.8901014347767504E-19, 1.8762011239677479E-19,
            1.8623755346860768E-19, 1.8486231138030984E-19, 1.8349423375370566E-19, 1.8213317103353295E-19,
            1.8077897637931708E-19, 1.7943150556069476E-19, 1.7809061685599652E-19, 1.7675617095390567E-19,
            1.7542803085801941E-19, 1.741060617941453E-19, 1.727901311201724E-19, 1.7148010823836362E-19,
            1.7017586450992059E-19, 1.6887727317167824E-19, 1.6758420925479093E-19, 1.6629654950527621E-19,
            1.650141723062866E-19, 1.6373695760198277E-19, 1.624647868228856E-19, 1.6119754281258616E-19,
            1.5993510975569615E-19, 1.586773731069231E-19, 1.5742421952115544E-19, 1.5617553678444595E-19,
            1.5493121374578016E-19, 1.5369114024951992E-19, 1.524552070684102E-19, 1.5122330583703858E-19,
            1.499953289856356E-19, 1.4877116967410352E-19, 1.4755072172615974E-19, 1.4633387956347966E-19,
            1.4512053813972103E-19, 1.439105928743099E-19, 1.4270393958586506E-19, 1.415004744251338E-19,
            1.4030009380730888E-19, 1.3910269434359025E-19, 1.3790817277185197E-19, 1.3671642588626657E-19,
            1.3552735046573446E-19, 1.3434084320095729E-19, 1.3315680061998685E-19, 1.3197511901207148E-19,
            1.3079569434961214E-19, 1.2961842220802957E-19, 1.28443197683331E-19, 1.2726991530715219E-19,
            1.2609846895903523E-19, 1.2492875177568625E-19, 1.237606560569394E-19, 1.225940731681333E-19,
            1.2142889343858445E-19, 1.2026500605581765E-19, 1.1910229895518744E-19, 1.1794065870449425E-19,
            1.1677997038316715E-19, 1.1562011745554883E-19, 1.144609816377787E-19, 1.1330244275772562E-19,
            1.1214437860737343E-19, 1.109866647870073E-19, 1.0982917454048923E-19, 1.086717785808435E-19,
            1.0751434490529747E-19, 1.0635673859884002E-19, 1.0519882162526621E-19, 1.0404045260457141E-19,
            1.0288148657544097E-19, 1.0172177474144965E-19, 1.0056116419943559E-19, 9.939949764834668E-20,
            9.823661307666745E-20, 9.70723434263201E-20, 9.590651623069063E-20, 9.47389532241542E-20,
            9.356946992015904E-20, 9.239787515456947E-20, 9.122397059055647E-20, 9.004755018085287E-20,
            8.886839958264763E-20, 8.768629551976745E-20, 8.6501005086071E-20, 8.531228498314119E-20,
            8.411988068438521E-20, 8.292352551651342E-20, 8.17229396480345E-20, 8.051782897283921E-20,
            7.930788387509923E-20, 7.809277785952443E-20, 7.687216602842904E-20, 7.564568338396512E-20,
            7.441294293017913E-20, 7.317353354509333E-20, 7.192701758763107E-20, 7.067292819766679E-20,
            6.941076623950036E-20, 6.813999682925642E-20, 6.686004537461023E-20, 6.557029304021008E-20,
            6.427007153336853E-20, 6.295865708092356E-20, 6.163526343814314E-20, 6.02990337321517E-20,
            5.894903089285018E-20, 5.758422635988593E-20, 5.62034866695974E-20, 5.480555741349931E-20,
            5.3389043909003295E-20, 5.1952387717989917E-20, 5.0493837866338355E-20, 4.901141522262949E-20,
            4.7502867933366117E-20, 4.5965615001265455E-20, 4.4396673897997565E-20, 4.279256630214859E-20,
            4.1149193273430015E-20, 3.9461666762606287E-20, 3.7724077131401685E-20, 3.592916408620436E-20,
            3.4067836691100565E-20, 3.2128447641564046E-20, 3.0095646916399994E-20, 2.794846945559833E-20,
            2.5656913048718645E-20, 2.317520975680391E-20, 2.042669522825129E-20, 1.7261770330213488E-20,
            1.3281889259442579E-20, 0.0};
        /** The precomputed ziggurat heights, denoted Y_i in the main text. Y_i = f(X_i).
         * Values have been scaled by 2^-63. */
        protected static final double[] Y = {5.595205495112736E-23, 1.1802509982703313E-22, 1.844442338673583E-22,
            2.543903046669831E-22, 3.2737694311509334E-22, 4.0307732132706715E-22, 4.812547831949511E-22,
            5.617291489658331E-22, 6.443582054044353E-22, 7.290266234346368E-22, 8.156388845632194E-22,
            9.041145368348222E-22, 9.94384884863992E-22, 1.0863906045969114E-21, 1.1800799775461269E-21,
            1.2754075534831208E-21, 1.372333117637729E-21, 1.4708208794375214E-21, 1.5708388257440445E-21,
            1.6723581984374566E-21, 1.7753530675030514E-21, 1.8797999785104595E-21, 1.9856776587832504E-21,
            2.0929667704053244E-21, 2.201649700995824E-21, 2.311710385230618E-21, 2.4231341516125464E-21,
            2.535907590142089E-21, 2.650018437417054E-21, 2.765455476366039E-21, 2.8822084483468604E-21,
            3.000267975754771E-21, 3.1196254936130377E-21, 3.240273188880175E-21, 3.3622039464187092E-21,
            3.485411300740904E-21, 3.6098893927859475E-21, 3.735632931097177E-21, 3.862637156862005E-21,
            3.990897812355284E-21, 4.120411112391895E-21, 4.251173718448891E-21, 4.383182715163374E-21,
            4.5164355889510656E-21, 4.6509302085234806E-21, 4.7866648071096E-21, 4.923637966211997E-21,
            5.061848600747899E-21, 5.201295945443473E-21, 5.341979542364895E-21, 5.483899229483096E-21,
            5.627055130180635E-21, 5.7714476436191935E-21, 5.917077435895068E-21, 6.063945431917703E-21,
            6.212052807953168E-21, 6.3614009847804375E-21, 6.511991621413643E-21, 6.6638266093481696E-21,
            6.816908067292628E-21, 6.971238336352438E-21, 7.126819975634082E-21, 7.283655758242034E-21,
            7.441748667643017E-21, 7.601101894374635E-21, 7.761718833077541E-21, 7.923603079832257E-21,
            8.086758429783484E-21, 8.251188875036333E-21, 8.416898602810326E-21, 8.58389199383831E-21,
            8.752173620998646E-21, 8.921748248170071E-21, 9.09262082929965E-21, 9.264796507675128E-21,
            9.438280615393829E-21, 9.613078673021033E-21, 9.789196389431416E-21, 9.966639661827884E-21,
            1.0145414575932636E-20, 1.0325527406345955E-20, 1.0506984617068672E-20, 1.0689792862184811E-20,
            1.0873958986701341E-20, 1.10594900275424E-20, 1.1246393214695825E-20, 1.1434675972510121E-20,
            1.1624345921140471E-20, 1.181541087814266E-20, 1.2007878860214202E-20, 1.2201758085082226E-20,
            1.239705697353804E-20, 1.2593784151618565E-20, 1.2791948452935152E-20, 1.29915589211506E-20,
            1.3192624812605428E-20, 1.3395155599094805E-20, 1.3599160970797774E-20, 1.3804650839360727E-20,
            1.4011635341137284E-20, 1.4220124840587164E-20, 1.4430129933836705E-20, 1.46416614524042E-20,
            1.485473046709328E-20, 1.5069348292058084E-20, 1.5285526489044053E-20, 1.5503276871808626E-20,
            1.5722611510726402E-20, 1.5943542737583543E-20, 1.6166083150566702E-20, 1.6390245619451956E-20,
            1.6616043290999594E-20, 1.684348959456108E-20, 1.7072598247904713E-20, 1.7303383263267072E-20,
            1.7535858953637607E-20, 1.777003993928424E-20, 1.8005941154528286E-20, 1.8243577854777398E-20,
            1.8482965623825808E-20, 1.8724120381431627E-20, 1.8967058391181452E-20, 1.9211796268653192E-20,
            1.9458350989888484E-20, 1.9706739900186868E-20, 1.9956980723234356E-20, 2.0209091570579904E-20,
            2.0463090951473895E-20, 2.0718997783083593E-20, 2.097683140110135E-20, 2.123661157076213E-20,
            2.1498358498287976E-20, 2.1762092842777868E-20, 2.2027835728562592E-20, 2.229560875804522E-20,
            2.256543402504904E-20, 2.2837334128696004E-20, 2.311133218784001E-20, 2.3387451856080863E-20,
            2.366571733738611E-20, 2.394615340234961E-20, 2.422878540511741E-20, 2.451363930101321E-20,
            2.4800741664897764E-20, 2.5090119710298442E-20, 2.5381801309347597E-20, 2.56758150135705E-20,
            2.5972190075566336E-20, 2.6270956471628253E-20, 2.6572144925351523E-20, 2.687578693228184E-20,
            2.718191478565915E-20, 2.7490561603315974E-20, 2.7801761355793055E-20, 2.811554889573917E-20,
            2.8431959988666534E-20, 2.8751031345137833E-20, 2.907280065446631E-20, 2.9397306620015486E-20,
            2.9724588996191657E-20, 3.005468862722811E-20, 3.038764748786764E-20, 3.072350872605708E-20,
            3.1062316707775905E-20, 3.140411706412999E-20, 3.174895674085097E-20, 3.2096884050352357E-20,
            3.2447948726504914E-20, 3.280220198230601E-20, 3.315969657063137E-20, 3.352048684827223E-20,
            3.388462884347689E-20, 3.4252180327233346E-20, 3.4623200888548644E-20, 3.4997752014001677E-20,
            3.537589717186906E-20, 3.5757701901149035E-20, 3.61432339058358E-20, 3.65325631548274E-20,
            3.692576198788357E-20, 3.732290522808698E-20, 3.7724070301302117E-20, 3.812933736317104E-20,
            3.8538789434235234E-20, 3.895251254382786E-20, 3.93705958834424E-20, 3.979313197035144E-20,
            4.022021682232577E-20, 4.0651950144388133E-20, 4.1088435528630944E-20, 4.152978066823271E-20,
            4.197609758692658E-20, 4.242750288530745E-20, 4.2884118005513604E-20, 4.334606951598745E-20,
            4.381348941821026E-20, 4.428651547752084E-20, 4.476529158037235E-20, 4.5249968120658306E-20,
            4.574070241805442E-20, 4.6237659171683015E-20, 4.674101095281837E-20, 4.7250938740823415E-20,
            4.776763250705122E-20, 4.8291291852069895E-20, 4.8822126702292804E-20, 4.936035807293385E-20,
            4.990621890518202E-20, 5.045995498662554E-20, 5.1021825965285324E-20, 5.159210646917826E-20,
            5.2171087345169234E-20, 5.2759077033045284E-20, 5.335640309332586E-20, 5.396341391039951E-20,
            5.458048059625925E-20, 5.520799912453558E-20, 5.584639272987383E-20, 5.649611461419377E-20,
            5.715765100929071E-20, 5.783152465495663E-20, 5.851829876379432E-20, 5.921858155879171E-20,
            5.99330314883387E-20, 6.066236324679689E-20, 6.1407354758435E-20, 6.216885532049976E-20,
            6.294779515010373E-20, 6.37451966432144E-20, 6.456218773753799E-20, 6.54000178818891E-20,
            6.626007726330934E-20, 6.714392014514662E-20, 6.80532934473017E-20, 6.8990172088133E-20,
            6.99568031585645E-20, 7.095576179487843E-20, 7.199002278894508E-20, 7.306305373910546E-20,
            7.417893826626688E-20, 7.534254213417312E-20, 7.655974217114297E-20, 7.783774986341285E-20,
            7.918558267402951E-20, 8.06147755373533E-20, 8.214050276981807E-20, 8.378344597828052E-20,
            8.557312924967816E-20, 8.75544596695901E-20, 8.980238805770688E-20, 9.246247142115109E-20,
            9.591964134495172E-20, 1.0842021724855044E-19};

        /** Underlying source of randomness. */
        protected final UniformRandomProvider rng;

        /**
         * @param rng Generator of uniformly distributed random numbers.
         */
        ModifiedZigguratExponentialSampler(UniformRandomProvider rng) {
            this.rng = rng;
        }

        /** {@inheritDoc} */
        @Override
        public double sample() {
            return createSample();
        }

        /**
         * Creates the exponential sample with {@code mean = 1}.
         *
         * <p>Note: This has been extracted to a separate method so that the recursive call
         * when sampling tries again targets this function. This allows sub-classes
         * to override the sample method to generate a sample with a different mean using:
         * <pre>
         * @Override
         * public double sample() {
         *     return super.sample() * mean;
         * }
         * </pre>
         * Otherwise the sub-class {@code sample()} method will recursively call
         * the overloaded sample() method when trying again which creates a bad sample due
         * to compound multiplication of the mean.
         *
         * @return the sample
         */
        protected double createSample() {
            final long x = nextLong();
            // Float multiplication squashes these last 8 bits, so they can be used to sample i
            final int i = ((int) x) & 0xff;

            if (i < I_MAX) {
                // Early exit.
                return X[i] * (x & MAX_INT64);
            }
            final int j = selectRegion();
            return j == 0 ? X_0 + createSample() : sampleOverhang(j);
        }

        /**
         * Select the overhang region or the tail using alias sampling.
         *
         * @return the region
         */
        protected int selectRegion() {
            final long x = nextLong();
            // j in [0, 256)
            final int j = ((int) x) & 0xff;
            // map to j in [0, N] with N the number of layers of the ziggurat
            return x >= IPMF[j] ? MAP[j] & 0xff : j;
        }

        /**
         * Sample from overhang region {@code j}.
         *
         * @param j Index j (must be {@code > 0})
         * @return the sample
         */
        protected double sampleOverhang(int j) {
            // Sample from the triangle:
            //    X[j],Y[j]
            //        |\-->u1
            //        | \  |
            //        |  \ |
            //        |   \|    Overhang j (with hypotenuse not pdf(x))
            //        |    \
            //        |    |\
            //        |    | \
            //        |    u2 \
            //        +-------- X[j-1],Y[j-1]
            // u2 = u1 + (u2 - u1) = u1 + uDistance
            // If u2 < u1 then reflect in the hypotenuse by swapping u1 and u2.
            long u1 = randomInt63();
            long uDistance = randomInt63() - u1;
            if (uDistance < 0) {
                uDistance = -uDistance;
                u1 -= uDistance;
            }
            final double x = sampleX(X, j, u1);
            if (uDistance >= E_MAX) {
                // Early Exit: x < y - epsilon
                return x;
            }

            return sampleY(Y, j, u1 + uDistance) <= Math.exp(-x) ? x : sampleOverhang(j);
        }

        /**
         * Generates a {@code long}.
         *
         * @return the long
         */
        protected long nextLong() {
            return rng.nextLong();
        }

        /**
         * Return a positive long in {@code [0, 2^63)}.
         *
         * @return the long
         */
        protected long randomInt63() {
            return rng.nextLong() & MAX_INT64;
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
     * <p>Uses the algorithm from McFarland, C.D. (2016).
     *
     * <p>This implementation uses simple overhangs and does not exploit the precomputed
     * distances of the convex overhang.
     *
     * <p>Note: It is not expected that this method is faster as the simple overhangs do
     * not exploit the property that the exponential PDF is always concave. Thus any upper-right
     * triangle sample at the edge of the ziggurat can be reflected and tested against the
     * lower-left triangle limit.
     */
    static class ModifiedZigguratExponentialSamplerSimpleOverhangs
        extends ModifiedZigguratExponentialSampler {

        /**
         * @param rng Generator of uniformly distributed random numbers.
         */
        ModifiedZigguratExponentialSamplerSimpleOverhangs(UniformRandomProvider rng) {
            super(rng);
        }

        /** {@inheritDoc} */
        @Override
        protected double sampleOverhang(int j) {
            final double x = sampleX(X, j, randomInt63());
            return sampleY(Y, j, randomInt63()) <= Math.exp(-x) ? x : sampleOverhang(j);
        }
    }

    /**
     * Modified Ziggurat method for sampling from an exponential distribution.
     *
     * <p>Uses the algorithm from McFarland, C.D. (2016).
     *
     * <p>This implementation separates sampling of the main ziggurat and sampling from the edge
     * into different methods. This allows inlining of the main sample method.
     *
     * <p>The sampler will output different values due to the use of a bit shift to generate
     * unsigned integers. This removes the requirement to load the mask MAX_INT64
     * and ensures the method is under 35 bytes.
     */
    static class ModifiedZigguratExponentialSamplerInlining
        extends ModifiedZigguratExponentialSampler {

        /**
         * @param rng Generator of uniformly distributed random numbers.
         */
        ModifiedZigguratExponentialSamplerInlining(UniformRandomProvider rng) {
            super(rng);
        }

        /** {@inheritDoc} */
        @Override
        public double sample() {
            // Ideally this method byte code size should be below -XX:MaxInlineSize
            // (which defaults to 35 bytes). This compiles to 34 bytes.

            final long x = nextLong();
            // Float multiplication squashes these last 8 bits, so they can be used to sample i
            final int i = ((int) x) & 0xff;

            if (i < I_MAX) {
                // Early exit.
                return X[i] * (x >>> 1);
            }

            return edgeSample();
        }

        /**
         * Create the sample from the edge of the ziggurat.
         *
         * <p>This method has been extracted to fit the main sample method within 35 bytes (the
         * default size for a JVM to inline a method).
         *
         * @return a sample
         */
        private double edgeSample() {
            final int j = selectRegion();
            return j == 0 ? sampleAdd(X_0) : sampleOverhang(j);
        }

        /**
         * Creates a sample and adds it to the current value. This exploits the memoryless
         * exponential distribution by generating a sample and adding it to the existing end
         * of the previous ziggurat.
         *
         * <p>The method will use recursion in the event of a new tail. Passing the
         * existing value allows the potential to optimise the memory stack.
         *
         * @param x0 Current value
         * @return the sample
         */
        private double sampleAdd(double x0) {
            final long x = nextLong();
            final int i = ((int) x) & 0xff;

            if (i < I_MAX) {
                // Early exit.
                return x0 + X[i] * (x >>> 1);
            }
            // Edge of the ziggurat
            final int j = selectRegion();
            return j == 0 ? sampleAdd(x0 + X_0) : x0 + sampleOverhang(j);
        }
    }

    /**
     * Modified Ziggurat method for sampling from an exponential distribution.
     *
     * <p>Uses the algorithm from McFarland, C.D. (2016).
     *
     * <p>This implementation separates sampling of the main ziggurat and sampling from the edge
     * into different methods. This allows inlining of the main sample method.
     *
     * <p>The sampler will output different values due to the use of a bit shift to generate
     * unsigned integers. This removes the requirement to load the mask MAX_INT64
     * and ensures the method is under 35 bytes.
     *
     * <p>Tail sampling outside of the main sample method is performed in a loop.
     */
    static class ModifiedZigguratExponentialSamplerLoop
        extends ModifiedZigguratExponentialSampler {

        /**
         * @param rng Generator of uniformly distributed random numbers.
         */
        ModifiedZigguratExponentialSamplerLoop(UniformRandomProvider rng) {
            super(rng);
        }

        /** {@inheritDoc} */
        @Override
        public double sample() {
            // Ideally this method byte code size should be below -XX:MaxInlineSize
            // (which defaults to 35 bytes). This compiles to 34 bytes.

            final long x = nextLong();
            // Float multiplication squashes these last 8 bits, so they can be used to sample i
            final int i = ((int) x) & 0xff;

            if (i < I_MAX) {
                // Early exit.
                return X[i] * (x >>> 1);
            }

            return edgeSample();
        }

        /**
         * Create the sample from the edge of the ziggurat.
         *
         * <p>This method has been extracted to fit the main sample method within 35 bytes (the
         * default size for a JVM to inline a method).
         *
         * @return a sample
         */
        private double edgeSample() {
            int j = selectRegion();
            if (j != 0) {
                return sampleOverhang(j);
            }

            // Perform a new sample and add it to the start of the tail.
            double x0 = X_0;
            for (;;) {
                final long x = nextLong();
                final int i = ((int) x) & 0xff;

                if (i < I_MAX) {
                    // Early exit.
                    return x0 + X[i] * (x >>> 1);
                }
                // Edge of the ziggurat
                j = selectRegion();
                if (j != 0) {
                    return x0 + sampleOverhang(j);
                }
                // Another tail sample
                x0 += X_0;
            }
        }
    }

    /**
     * Modified Ziggurat method for sampling from an exponential distribution.
     *
     * <p>Uses the algorithm from McFarland, C.D. (2016).
     *
     * <p>This implementation separates sampling of the main ziggurat and sampling from the edge
     * into different methods. This allows inlining of the main sample method.
     *
     * <p>The sampler will output different values due to the use of a bit shift to generate
     * unsigned integers. This removes the requirement to load the mask MAX_INT64
     * and ensures the method is under 35 bytes.
     *
     * <p>Tail sampling outside of the main sample method is performed in a loop. No recursion
     * is used. The first random deviate is recycled if the sample if from the edge.
     */
    static class ModifiedZigguratExponentialSamplerLoop2
        extends ModifiedZigguratExponentialSampler {

        /**
         * @param rng Generator of uniformly distributed random numbers.
         */
        ModifiedZigguratExponentialSamplerLoop2(UniformRandomProvider rng) {
            super(rng);
        }

        /** {@inheritDoc} */
        @Override
        public double sample() {
            // Ideally this method byte code size should be below -XX:MaxInlineSize
            // (which defaults to 35 bytes). This compiles to 35 bytes.

            final long x = nextLong();
            // Float multiplication squashes these last 8 bits, so they can be used to sample i
            final int i = ((int) x) & 0xff;

            if (i < I_MAX) {
                // Early exit.
                return X[i] * (x >>> 1);
            }

            // Recycle x as the upper 56 bits have not been used.

            return edgeSample(x);
        }

        /**
         * Create the sample from the edge of the ziggurat.
         *
         * <p>This method has been extracted to fit the main sample method within 35 bytes (the
         * default size for a JVM to inline a method).
         *
         * @param xx Initial random deviate
         * @return a sample
         */
        private double edgeSample(long xx) {
            int j = selectRegion();
            if (j != 0) {
                return sampleOverhang(j, xx);
            }

            // Perform a new sample and add it to the start of the tail.
            double x0 = X_0;
            for (;;) {
                final long x = nextLong();
                final int i = ((int) x) & 0xff;

                if (i < I_MAX) {
                    // Early exit.
                    return x0 + X[i] * (x >>> 1);
                }
                // Edge of the ziggurat
                j = selectRegion();
                if (j != 0) {
                    return x0 + sampleOverhang(j, x);
                }
                // Another tail sample
                x0 += X_0;
            }
        }

        /**
         * Sample from overhang region {@code j}.
         *
         * <p>This does not use recursion.
         *
         * @param j Index j (must be {@code > 0})
         * @param xx Initial random deviate
         * @return the sample
         */
        protected double sampleOverhang(int j, long xx) {
            // Recycle the initial random deviate.
            // Shift right to make an unsigned long.
            for (long u1 = xx >>> 1;; u1 = nextLong() >>> 1) {
                // Sample from the triangle:
                //    X[j],Y[j]
                //        |\-->u1
                //        | \  |
                //        |  \ |
                //        |   \|    Overhang j (with hypotenuse not pdf(x))
                //        |    \
                //        |    |\
                //        |    | \
                //        |    u2 \
                //        +-------- X[j-1],Y[j-1]
                // u2 = u1 + (u2 - u1) = u1 + uDistance
                // If u2 < u1 then reflect in the hypotenuse by swapping u1 and u2.
                long uDistance = (nextLong() >>> 1) - u1;
                if (uDistance < 0) {
                    uDistance = -uDistance;
                    u1 -= uDistance;
                }
                final double x = sampleX(X, j, u1);
                if (uDistance >= E_MAX) {
                    // Early Exit: x < y - epsilon
                    return x;
                }
                if (sampleY(Y, j, u1 + uDistance) <= Math.exp(-x)) {
                    return x;
                }
            }
        }
    }

    /**
     * Modified Ziggurat method for sampling from an exponential distribution.
     *
     * <p>Uses the algorithm from McFarland, C.D. (2016).
     *
     * <p>This implementation separates sampling of the main ziggurat and the recursive
     * sampling from the edge into different methods.
     */
    static class ModifiedZigguratExponentialSamplerRecursion
        extends ModifiedZigguratExponentialSampler {

        /**
         * @param rng Generator of uniformly distributed random numbers.
         */
        ModifiedZigguratExponentialSamplerRecursion(UniformRandomProvider rng) {
            super(rng);
        }

        /** {@inheritDoc} */
        @Override
        public double sample() {
            final long x = nextLong();
            // Float multiplication squashes these last 8 bits, so they can be used to sample i
            final int i = ((int) x) & 0xff;

            if (i < I_MAX) {
                // Early exit.
                return X[i] * (x & MAX_INT64);
            }
            final int j = selectRegion();
            return j == 0 ? sampleAdd(X_0) : sampleOverhang(j);
        }

        /**
         * Creates a sample and adds it to the current value. This exploits the memoryless
         * exponential distribution by generating a sample and adding it to the existing end
         * of the previous ziggurat.
         *
         * <p>The method will use recursion in the event of a new tail. Passing the
         * existing value allows the potential to optimise the memory stack.
         *
         * @param x0 Current value
         * @return the sample
         */
        private double sampleAdd(double x0) {
            final long x = nextLong();
            final int i = ((int) x) & 0xff;

            if (i < I_MAX) {
                // Early exit.
                return x0 + X[i] * (x & MAX_INT64);
            }
            // Edge of the ziggurat
            final int j = selectRegion();
            return j == 0 ? sampleAdd(x0 + X_0) : x0 + sampleOverhang(j);
        }
    }

    /**
     * Modified Ziggurat method for sampling from an exponential distribution.
     *
     * <p>Uses the algorithm from McFarland, C.D. (2016).
     *
     * <p>This is a copy of {@link ModifiedZigguratExponentialSampler} using
     * an integer map in-place of a byte map look-up table.
     */
    static class ModifiedZigguratExponentialSamplerIntMap
        extends ModifiedZigguratExponentialSampler {

        /** The alias map. */
        private static final int[] INT_MAP = {0, 0, 1, 235, 3, 4, 5, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 2, 2,
            252, 252, 252, 252, 252, 252, 252, 252, 252, 252, 252, 252, 252, 252, 252, 252, 252, 252, 252, 252, 252,
            252, 252, 252, 252, 252, 252, 252, 252, 252, 252, 252, 252, 252, 252, 252, 252, 252, 252, 252, 252, 252,
            252, 252, 252, 252, 252, 252, 252, 252, 252, 252, 252, 252, 252, 252, 252, 252, 252, 252, 252, 252, 252,
            252, 252, 252, 252, 252, 252, 252, 252, 252, 252, 252, 252, 252, 252, 252, 252, 252, 252, 252, 252, 252,
            252, 252, 252, 252, 252, 252, 252, 252, 252, 252, 252, 252, 252, 252, 252, 252, 252, 252, 252, 252, 252,
            252, 252, 252, 252, 252, 252, 252, 252, 252, 252, 252, 252, 252, 252, 252, 252, 252, 252, 252, 252, 252,
            252, 252, 252, 252, 252, 252, 252, 252, 252, 252, 252, 252, 252, 252, 252, 252, 252, 252, 252, 252, 252,
            252, 252, 252, 252, 252, 252, 251, 251, 251, 251, 251, 251, 251, 251, 251, 251, 251, 251, 251, 250, 250,
            250, 250, 250, 250, 250, 249, 249, 249, 249, 249, 249, 248, 248, 248, 248, 247, 247, 247, 247, 246, 246,
            246, 245, 245, 244, 244, 243, 243, 242, 241, 241, 240, 239, 237, 3, 3, 4, 4, 6, 0, 0, 0, 0, 236, 237, 238,
            239, 240, 241, 242, 243, 244, 245, 246, 247, 248, 249, 250, 251, 252, 2, 0, 0, 0};

        /**
         * @param rng Generator of uniformly distributed random numbers.
         */
        ModifiedZigguratExponentialSamplerIntMap(UniformRandomProvider rng) {
            super(rng);
        }

        /** {@inheritDoc} */
        @Override
        protected int selectRegion() {
            final long x = nextLong();
            // j in [0, 256)
            final int j = ((int) x) & 0xff;
            // map to j in [0, N] with N the number of layers of the ziggurat
            return x >= IPMF[j] ? INT_MAP[j] : j;
        }
    }

    /**
     * Modified Ziggurat method for sampling from an exponential distribution.
     *
     * <p>Uses the algorithm from McFarland, C.D. (2016).
     *
     * <p>This is a copy of {@link ModifiedZigguratExponentialSampler} using
     * a table to store the maximum epsilon value for each overhang.
     */
    static class ModifiedZigguratExponentialSamplerEMaxTable
        extends ModifiedZigguratExponentialSampler {

        /** The deviation of concave pdf(x) below the hypotenuse value for early exit scaled by 2^63.
         * 252 entries with overhang {@code j} corresponding to entry {@code j-1}. */
        private static final long[] E_MAX_TABLE = {853965788476313647L, 513303011048449571L, 370159258027176883L,
            290559192811411702L, 239682322342140259L, 204287432757591023L, 178208980447816578L, 158179811652601808L,
            142304335868356315L, 129406004988661986L, 118715373346558305L, 109707765835789898L, 102012968856398207L,
            95362200070363889L, 89555545194518719L, 84441195786718132L, 79901778875942314L, 75845102376072521L,
            72197735928174183L, 68900462142397564L, 65904991388597543L, 63171548466382494L, 60667072432484929L,
            58363855085733832L, 56238498180198598L, 54271105522401119L, 52444650416313533L, 50744475573288311L,
            49157894191680121L, 47673869089531616L, 46282752622782375L, 44976074355904424L, 43746366552358124L,
            42587019846569729L, 41492163173714974L, 40456563326820177L, 39475540494600872L, 38544896888152443L,
            37660856147942019L, 36820011676708837L, 36019282399893713L, 35255874736108745L, 34527249783140581L,
            33831094903027707L, 33165299032708110L, 32527931162123068L, 31917221515265728L, 31331545045961871L,
            30769406922648640L, 30229429727799711L, 29710342140081525L, 29210968902516770L, 28730221909221458L,
            28267092267755223L, 27820643214642802L, 27390003778888310L, 26974363102873684L, 26572965342371777L,
            26185105077881714L, 25810123178421168L, 25447403066532673L, 25096367339794542L, 24756474709733050L,
            24427217223862121L, 24108117740746397L, 23798727631587496L, 23498624684961198L, 23207411194051023L,
            22924712208092083L, 22650173931802985L, 22383462258391643L, 22124261423306770L, 21872272767292587L,
            21627213598531964L, 21388816144739113L, 21156826587012934L, 20931004168108672L, 20711120368524119L,
            20496958144463729L, 20288311222329444L, 20084983444908931L, 19886788164900174L, 19693547681827322L,
            19505092718772996L, 19321261935686425L, 19141901476327133L, 18966864546167569L, 18796011018823163L,
            18629207068791541L, 18466324828481792L, 18307242067684946L, 18151841893803319L, 18000012471293326L,
            17851646758910343L, 17706642263461846L, 17564900808880742L, 17426328319528561L, 17290834616728420L,
            17158333227603225L, 17028741205374556L, 16901978960337609L, 16777970100794092L, 16656641283277552L,
            16537922071455934L, 16421744803147429L, 16308044464921141L, 16196758573800601L, 16087827065618528L,
            15981192189607877L, 15876798408841619L, 15774592306164744L, 15674522495285722L, 15576539536717537L,
            15480595858282956L, 15386645679917351L, 15294644942520693L, 15204551240628355L, 15116323758686143L,
            15029923210730485L, 14945311783286871L, 14862453081314005L, 14781312077032665L, 14701855061488253L,
            14624049598708474L, 14547864482325100L, 14473269694538314L, 14400236367311981L, 14328736745694203L,
            14258744153165819L, 14190232958926857L, 14123178547035436L, 14057557287324133L, 13993346508018831L,
            13930524469996132L, 13869070342616606L, 13808964181079506L, 13750186905245534L, 13692720279883516L,
            13636546896296650L, 13581650155291926L, 13528014251457894L, 13475624158722351L, 13424465617162582L,
            13374525121048117L, 13325789908096374L, 13278247949928556L, 13231887943714024L, 13186699304997313L,
            13142672161706220L, 13099797349339341L, 13058066407342105L, 13017471576677033L, 12978005798604683L,
            12939662714691649L, 12902436668069432L, 12866322705969175L, 12831316583568516L, 12797414769185138L,
            12764614450860914L, 12732913544387857L, 12702310702829356L, 12672805327602182L, 12644397581185285L,
            12617088401539963L, 12590879518319943L, 12565773470976553L, 12541773628858256L, 12518884213426018L,
            12497110322714335L, 12476457958178394L, 12456934054089150L, 12438546509646839L, 12421304224007330L,
            12405217134429885L, 12390296257782254L, 12376553735658483L, 12364002883391013L, 12352658243275075L,
            12342535642345809L, 12333652255094142L, 12326026671543609L, 12319678971157050L, 12314630803093705L,
            12310905473395866L, 12308528039744429L, 12307525414502757L, 12307926476843287L, 12309762194847939L,
            12313065758579233L, 12317872725234489L, 12324221177635088L, 12332151897455469L, 12341708554772055L,
            12352937915715590L, 12365890070240929L, 12380618682293187L, 12397181264956129L, 12415639483521735L,
            12436059489828363L, 12458512291688487L, 12483074161780576L, 12509827091021440L, 12538859292191186L,
            12570265760462982L, 12604148898533980L, 12640619215280889L, 12679796108318231L, 12721808742570859L,
            12766797039034927L, 12814912790376408L, 12866320922993068L, 12921200928753560L, 12979748493987873L,
            13042177358607040L, 13108721444725108L, 13179637302147239L, 13255206927961732L, 13335741029756972L,
            13421582817340701L, 13513112427153506L, 13610752108037106L, 13714972328197193L, 13826299003251241L,
            13945322097066639L, 14072705914699465L, 14209201495705644L, 14355661634264763L, 14513059211087015L,
            14682509737034964L, 14865299303251008L, 15062919542085557L, 15277111779554570L, 15509923383418374L,
            15763780505978643L, 16041583185668067L, 16346831428994830L, 16683794981864260L, 17057745936956694L,
            17475283735768697L, 17944799475531562L, 18477156350176671L, 19086716702655372L, 19792946841346963L,
            20623030105811966L, 21616339529831424L, 22832582766564173L, 24367856249313960L, 26389803907514902L,
            29226958795272911L, 33654855924781132L, 42320562697765039L, 141207843617418268L};

        /**
         * @param rng Generator of uniformly distributed random numbers.
         */
        ModifiedZigguratExponentialSamplerEMaxTable(UniformRandomProvider rng) {
            super(rng);
        }

        @Override
        protected double sampleOverhang(int j) {
            // Look-up E_MAX
            return sampleOverhang(j, E_MAX_TABLE[j - 1]);
        }

        /**
         * Sample from overhang region {@code j}.
         *
         * @param j Index j (must be {@code > 0})
         * @param eMax Maximum deviation of concave pdf(x) below the hypotenuse value for early exit
         * @return the sample
         */
        private double sampleOverhang(int j, long eMax) {
            long u1 = randomInt63();
            long uDistance = randomInt63() - u1;
            if (uDistance < 0) {
                uDistance = -uDistance;
                u1 -= uDistance;
            }
            final double x = sampleX(X, j, u1);
            if (uDistance >= eMax) {
                // Early Exit: x < y - epsilon
                return x;
            }

            return sampleY(Y, j, u1 + uDistance) <= Math.exp(-x) ? x : sampleOverhang(j, eMax);
        }
    }

    /**
     * Modified Ziggurat method for sampling from an exponential distribution.
     *
     * <p>Uses the algorithm from McFarland, C.D. (2016).
     *
     * <p>This is a copy of {@link ModifiedZigguratExponentialSampler} using
     * two values for the maximum epsilon value for the overhangs.
     *
     * <p>Note: The exponential curve is very well approximated by the straight line of
     * the triangle hypotenuse in the majority of cases. Only as the overhang approaches the
     * tail or close to x=0 does the curve differ more than 0.01 (expressed as a fraction of
     * the triangle height). This can be exploited using two maximum deviation thresholds.
     */
    static class ModifiedZigguratExponentialSamplerEMax2
        extends ModifiedZigguratExponentialSampler {

        // Ziggurat volumes:
        // Inside the layers              = 98.4375%  (252/256)
        // Fraction outside the layers:
        // concave overhangs              = 96.6972%
        // tail (j=0)                     =  3.3028%
        //
        // Separation of tail with large maximum deviation from hypotenuse
        // 0 <= j <= 8                    =  7.9913%
        // 1 <= j <= 8                    =  4.6885%
        // 9 <= j                         = 92.0087%

        /** The overhang region j where the second maximum deviation constant is valid. */
        private static final int J2 = 9;

        /** Maximum deviation of concave pdf(x) below the hypotenuse value for early exit
         * for overhangs {@code 9 <= j <= 253}.
         * Equal to approximately 0.0154 scaled by 2^63.
         *
         * <p>This threshold increases the area of the early exit triangle by:
         * (1-0.0154)^2 / (1-0.0926)^2 = 17.74%. */
        private static final long E_MAX_2 = 142304335868356315L;

        /**
         * @param rng Generator of uniformly distributed random numbers.
         */
        ModifiedZigguratExponentialSamplerEMax2(UniformRandomProvider rng) {
            super(rng);
        }

        @Override
        protected double createSample() {
            final long x = nextLong();
            // Float multiplication squashes these last 8 bits, so they can be used to sample i
            final int i = ((int) x) & 0xff;

            if (i < I_MAX) {
                // Early exit.
                return X[i] * (x & MAX_INT64);
            }
            final int j = selectRegion();

            if (j < J2) {
                // Long tail frequency: 0.0799
                // j==0 (tail) frequency: 0.033028
                return j == 0 ? X_0 + createSample() : sampleOverhang(j, E_MAX);
            }
            // Majority of curve can use a smaller threshold.
            // Frequency: 0.920087
            return sampleOverhang(j, E_MAX_2);
        }

        /**
         * Sample from overhang region {@code j}.
         *
         * @param j Index j (must be {@code > 0})
         * @param eMax Maximum deviation of concave pdf(x) below the hypotenuse value for early exit
         * @return the sample
         */
        private double sampleOverhang(int j, long eMax) {
            long u1 = randomInt63();
            long uDistance = randomInt63() - u1;
            if (uDistance < 0) {
                uDistance = -uDistance;
                u1 -= uDistance;
            }
            final double x = sampleX(X, j, u1);
            if (uDistance >= eMax) {
                // Early Exit: x < y - epsilon
                return x;
            }

            return sampleY(Y, j, u1 + uDistance) <= Math.exp(-x) ? x : sampleOverhang(j, eMax);
        }
    }

    /**
     * Modified Ziggurat method for sampling from an exponential distribution.
     *
     * <p>Uses the algorithm from McFarland, C.D. (2016).
     *
     * <p>This is a copy of {@link ModifiedZigguratExponentialSampler} using
     * a table size of 512.
     */
    static class ModifiedZigguratExponentialSampler512 implements ContinuousSampler {
        // Ziggurat volumes:
        // Inside the layers              = 99.2188%  (508/512)
        // Fraction outside the layers:
        // concave overhangs              = 97.0103%
        // tail                           =  2.9897%

        /** The number of layers in the ziggurat. Maximum i value for early exit. */
        protected static final int I_MAX = 508;
        /** Maximum deviation of concave pdf(x) below the hypotenuse value for early exit.
         * Equal to approximately 0.0919 scaled by 2^63. */
        protected static final long E_MAX = 847415790149374213L;
        /** Beginning of tail. Equal to X[0] * 2^63. */
        protected static final double X_0 = 8.362025281328359;

        /** The alias map. */
        private static final int[] MAP = {0, 0, 1, 473, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 2, 2, 2, 2, 508, 508, 508, 508,
            508, 508, 508, 508, 508, 508, 508, 508, 508, 508, 508, 508, 508, 508, 508, 508, 508, 508, 508, 508, 508,
            508, 508, 508, 508, 508, 508, 508, 508, 508, 508, 508, 508, 508, 508, 508, 508, 508, 508, 508, 508, 508,
            508, 508, 508, 508, 508, 508, 508, 508, 508, 508, 508, 508, 508, 508, 508, 508, 508, 508, 508, 508, 508,
            508, 508, 508, 508, 508, 508, 508, 508, 508, 508, 508, 508, 508, 508, 508, 508, 508, 508, 508, 508, 508,
            508, 508, 508, 508, 508, 508, 508, 508, 508, 508, 508, 508, 508, 508, 508, 508, 508, 508, 508, 508, 508,
            508, 508, 508, 508, 508, 508, 508, 508, 508, 508, 508, 508, 508, 508, 508, 508, 508, 508, 508, 508, 508,
            508, 508, 508, 508, 508, 508, 508, 508, 508, 508, 508, 508, 508, 508, 508, 508, 508, 508, 508, 508, 508,
            508, 508, 508, 508, 508, 508, 508, 508, 508, 508, 508, 508, 508, 508, 508, 508, 508, 508, 508, 508, 508,
            508, 508, 508, 508, 508, 508, 508, 508, 508, 508, 508, 508, 508, 508, 508, 508, 508, 508, 508, 508, 508,
            508, 508, 508, 508, 508, 508, 508, 508, 508, 508, 508, 508, 508, 508, 508, 508, 508, 508, 508, 508, 508,
            508, 508, 508, 508, 508, 508, 508, 508, 508, 508, 508, 508, 508, 508, 508, 508, 508, 508, 508, 508, 508,
            508, 508, 508, 508, 508, 508, 508, 508, 508, 508, 508, 508, 508, 508, 508, 508, 508, 508, 508, 508, 508,
            508, 508, 508, 507, 507, 507, 507, 507, 507, 507, 507, 507, 507, 507, 507, 507, 507, 507, 507, 507, 507,
            507, 507, 507, 507, 507, 507, 507, 507, 507, 507, 507, 506, 506, 506, 506, 506, 506, 506, 506, 506, 506,
            506, 506, 506, 506, 506, 506, 506, 505, 505, 505, 505, 505, 505, 505, 505, 505, 505, 505, 504, 504, 504,
            504, 504, 504, 504, 504, 504, 503, 503, 503, 503, 503, 503, 503, 503, 502, 502, 502, 502, 502, 502, 501,
            501, 501, 501, 501, 500, 500, 500, 500, 500, 499, 499, 499, 499, 498, 498, 498, 498, 497, 497, 497, 496,
            496, 496, 495, 495, 495, 494, 494, 493, 493, 492, 492, 491, 491, 490, 490, 489, 489, 488, 487, 486, 485,
            485, 484, 482, 481, 479, 477, 3, 3, 3, 3, 3, 4, 4, 4, 5, 5, 5, 6, 6, 6, 7, 7, 8, 9, 9, 10, 13, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 474, 475, 476, 477, 478, 479, 480, 481, 482, 483, 484, 485, 486, 487, 488, 489, 490,
            491, 492, 493, 494, 495, 496, 497, 498, 499, 500, 501, 502, 503, 504, 505, 506, 507, 508, 2, 0, 0, 0};
        /** The alias inverse PMF. */
        private static final long[] IPMF = {9223372036854775296L, 2636146522269100032L, 3561072052814771200L,
            3481882573634022912L, 7116525960784167936L, 3356999026233157120L, 4185343482988790272L,
            7712945794725800960L, 7827282421678200832L, 5031524219672503296L, 8479703117927504896L,
            8798557826262976512L, 6461753601163497984L, 5374576640182409216L, 8285209933846173696L,
            7451775918468278784L, 6710527725555065344L, 6046590166603477504L, 5448165515863960576L,
            4905771306157685248L, 4411694943948817408L, 3959596233157434880L, 3544212893101320192L,
            3161139128795137536L, 2806656884941820416L, 2477605671376185344L, 2171281018524273664L,
            1885354452557434880L, 1617809833614047744L, 1366892269237658112L, 1131066787615028736L, 908984654997058560L,
            699455731348318720L, 501425633583915520L, 313956755007992832L, 136212399366961152L, -32556553021349888L,
            -193022908001625600L, -345792716399554560L, -491413423379838464L, -630380845888988672L,
            -763145170929260544L, -890116131912690176L, -1011667492054083072L, -1128140941080192000L,
            -1239849493252469760L, -1347080459893031424L, -1450098057542626304L, -1549145703018891776L,
            -1644448038535791104L, -1736212723361025536L, -1824632022940825600L, -1909884221816344064L,
            -1992134882804800512L, -2071537971688286208L, -2148236863947458560L, -2222365247780633088L,
            -2294047935714820096L, -2363401595468253696L, -2430535409323403776L, -2495551670069566976L,
            -2558546320552583680L, -2619609442985608192L, -2678825703417424896L, -2736274756101145600L,
            -2792031611938902016L, -2846166974686572544L, -2898747548177031680L, -2949836317447342592L,
            -2999492806329770496L, -3047773313785532928L, -3094731131006235648L, -3140416741094473728L,
            -3184878002938374144L, -3228160320727380992L, -3270306800406949376L, -3311358394234071552L,
            -3351354034484032512L, -3390330757247591424L, -3428323817169865728L, -3465366793898720256L,
            -3501491690934902784L, -3536729027513080832L, -3571107924081618944L, -3604656181899062784L,
            -3637400357214628352L, -3669365830461451264L, -3700576870849548288L, -3731056696714091520L,
            -3760827531940795392L, -3789910658766305792L, -3818326467220366848L, -3846094501460680704L,
            -3873233503224223232L, -3899761452604410880L, -3925695606345008640L, -3951052533825223680L,
            -3975848150898364416L, -4000097751731727360L, -4023816038786208256L, -4047017151058499584L,
            -4069714690707166208L, -4091921748165281792L, -4113650925843118592L, -4134914360510158336L,
            -4155723744443228160L, -4176090345419594752L, -4196025025626374656L, -4215538259557954560L,
            -4234640150960326144L, -4253340448884000768L, -4271648562898405376L, -4289573577519592448L,
            -4307124265897231872L, -4324309102806436352L, -4341136276983992320L, -4357613702847894016L,
            -4373749031636836352L, -4389549662001094144L, -4405022750077366784L, -4420175219077067264L,
            -4435013768413905920L, -4449544882397231104L, -4463774838515782144L, -4477709715332459008L,
            -4491355400012626944L, -4504717595505328640L, -4517801827395838976L, -4530613450446389760L,
            -4543157654842793472L, -4555439472160934400L, -4567463781068195840L, -4579235312773997568L,
            -4590758656240654848L, -4602038263168300032L, -4613078452764409856L, -4623883416308989952L,
            -4634457221524290048L, -4644803816761235456L, -4654927035008246784L, -4664830597734270976L,
            -4674518118570737664L, -4683993106843208704L, -4693258970957511168L, -4702319021648195072L,
            -4711176475095642112L, -4719834455917983232L, -4728296000042890240L, -4736564057465815040L,
            -4744641494898972160L, -4752531098316131328L, -4760235575398025216L, -4767757557883156992L,
            -4775099603826595840L, -4782264199773566464L, -4789253762848160256L, -4796070642763833856L,
            -4802717123757088768L, -4809195426448159744L, -4815507709632380416L, -4821656072004040192L,
            -4827642553816744448L, -4833469138481454080L, -4839137754106220032L, -4844650274978936320L,
            -4850008522996425728L, -4855214269039884288L, -4860269234302464000L, -4865175091566906368L,
            -4869933466437730816L, -4874545938529117696L, -4879014042609741312L, -4883339269707099136L,
            -4887523068171446784L, -4891566844702813184L, -4895471965340231168L, -4899239756417071616L,
            -4902871505481196544L, -4906368462183164928L, -4909731839132712448L, -4912962812724644864L,
            -4916062523935916544L, -4919032079093866496L, -4921872550617327616L, -4924584977732045824L,
            -4927170367159303168L, -4929629693781258752L, -4931963901281752576L, -4934173902764700160L,
            -4936260581349647360L, -4938224790746291200L, -4940067355807971840L, -4941789073065357824L,
            -4943390711239933952L, -4944873011739753984L, -4946236689135308800L, -4947482431619080192L,
            -4948610901446864896L, -4949622735363246080L, -4950518545009749504L, -4951298917317924864L,
            -4951964414887600640L, -4952515576348487168L, -4952952916709260288L, -4953276927691216384L,
            -4953488078048885760L, -4953586813876376064L, -4953573558902138368L, -4953448714769485312L,
            -4953212661305179136L, -4952865756776645632L, -4952408338136501760L, -4951840721255068672L,
            -4951163201143173120L, -4950376052162316800L, -4949479528224847360L, -4948473862982736384L,
            -4947359270007034368L, -4946135942956237312L, -4944804055734229504L, -4943363762640024064L,
            -4941815198506020352L, -4940158478827333120L, -4938393699882143232L, -4936520938842464768L,
            -4934540253875459072L, -4932451684236201472L, -4930255250350992896L, -4927950953893274112L,
            -4925538777848453632L, -4923018686572188672L, -4920390625839525888L, -4917654522885157888L,
            -4914810286435288576L, -4911857806732278272L, -4908796955549192192L, -4905627586197715456L,
            -4902349533527011840L, -4898962613914057216L, -4895466625247106560L, -4891861346899963904L,
            -4888146539697684992L, -4884321945875628032L, -4880387289029549568L, -4876342274056310272L,
            -4872186587089345536L, -4867919895422610944L, -4863541847428798464L, -4859052072467801600L,
            -4854450180787865088L, -4849735763417182208L, -4844908392047856128L, -4839967618911950848L,
            -4834912976647099392L, -4829743978155096576L, -4824460116451147776L, -4819060864504044544L,
            -4813545675067768320L, -4807913980504056832L, -4802165192594749952L, -4796298702346892288L,
            -4790313879785735168L, -4784210073740736512L, -4777986611619728896L, -4771642799174403072L,
            -4765177920255393792L, -4758591236556836352L, -4751881987350666752L, -4745049389210733568L,
            -4738092635724898304L, -4731010897197732864L, -4723803320339894784L, -4716469027948174336L,
            -4709007118571786240L, -4701416666168034304L, -4693696719744952320L, -4685846302991571968L,
            -4677864413895422464L, -4669750024346546688L, -4661502079728433664L, -4653119498494611968L,
            -4644601171731585024L, -4635945962706888192L, -4627152706402486784L, -4618220209032402944L,
            -4609147247545243136L, -4599932569110216704L, -4590574890586744832L, -4581072897977050112L,
            -4571425245860782592L, -4561630556812469760L, -4551687420800102912L, -4541594394564215808L,
            -4531350000978089984L, -4520952728387175936L, -4510401029928645120L, -4499693322828163584L,
            -4488827987675754496L, -4477803367678713856L, -4466617767890585600L, -4455269454416637440L,
            -4443756653594040832L, -4432077551146284544L, -4420230291311382528L, -4408212975942271488L,
            -4396023663579625472L, -4383660368494419456L, -4371121059701586432L, -4358403659940780544L,
            -4345506044627643904L, -4332426040768207872L, -4319161425842345472L, -4305709926649207296L,
            -4292069218116598784L, -4278236922073029120L, -4264210605978586624L, -4249987781616803840L,
            -4235565903742823424L, -4220942368688843264L, -4206114512923164672L, -4191079611563289088L,
            -4175834876839676928L, -4160377456508627456L, -4144704432214183936L, -4128812817794444800L,
            -4112699557531435520L, -4096361524344448000L, -4079795517919899648L, -4062998262780994048L,
            -4045966406289958912L, -4028696516583542784L, -4011185080437596160L, -3993428501058901504L,
            -3975423095799736320L, -3957165093794900480L, -3938650633514732032L, -3919875760234452992L,
            -3900836423412657152L, -3881528473978691584L, -3861947661522247168L, -3842089631384604160L,
            -3821949921642690048L, -3801523959987316736L, -3780807060485497856L, -3759794420226106880L,
            -3738481115842412032L, -3716862099905139200L, -3694932197183221248L, -3672686100763535872L,
            -3650118368025705472L, -3627223416464823808L, -3603995519354882048L, -3580428801247209472L,
            -3556517233294936064L, -3532254628397208576L, -3507634636153634816L, -3482650737621700608L,
            -3457296239866780160L, -3431564270296417792L, -3405447770767880704L, -3378939491458745856L,
            -3352031984490172416L, -3324717597289475072L, -3296988465680861184L, -3268836506691269120L,
            -3240253411056924672L, -3211230635415641088L, -3181759394171538432L, -3151830651013152256L,
            -3121435110069826048L, -3090563206687621632L, -3059205097804578816L, -3027350651907195904L,
            -2994989438544094720L, -2962110717375283712L, -2928703426733917184L, -2894756171672258048L,
            -2860257211467748352L, -2825194446557172224L, -2789555404872457216L, -2753327227540304896L,
            -2716496653917054464L, -2679050005916396544L, -2640973171596317184L, -2602251587958849024L,
            -2562870222922691584L, -2522813556418801152L, -2482065560560435712L, -2440609678833421312L,
            -2398428804250509824L, -2355505256407986688L, -2311820757381316608L, -2267356406388873216L,
            -2222092653150109696L, -2176009269860336640L, -2129085321695611904L, -2081299135757883392L,
            -2032628268363105280L, -1983049470568565760L, -1932538651826487296L, -1881070841645808640L,
            -1828620149131529728L, -1775159720264906752L, -1720661692774047744L, -1665097148437127680L,
            -1608436062642298880L, -1550647251022831104L, -1491698312963664384L, -1431555571764694528L,
            -1370184011228092416L, -1307547208415545344L, -1243607262304812544L, -1178324718048254976L,
            -1111658486516102144L, -1043565758774211584L, -974001915124345856L, -902920428295306752L,
            -830272760342546432L, -756008252773183488L, -680074009369660928L, -602414771142033408L,
            -522972782779506176L, -441687649923274752L, -358496186511785984L, -273332251384862720L,
            -186126573250838016L, -96806563039876096L, -5296112566262784L, 88484621680143872L, 184619450912608768L,
            283196404027497984L, 384307996761360384L, 488051521767595008L, 594529361535797760L, 703849326283634688L,
            816125019179379200L, 931476231515671552L, 1050029370739642368L, 1171917924579436544L, 1297282964870890496L,
            1426273695110094848L, 1559048046230309888L, 1695773325638197760L, 1836626925155011584L,
            1981797094206053888L, 2131483785391109632L, 2285899580478475776L, 2445270705902480384L,
            2609838148036959744L, 2779858879886981120L, 2955607212425994240L, 3137376285625809920L,
            3325479716351016448L, 3520253422737882112L, 3722057647546029568L, 3931279206301515776L,
            4148333989964127232L, 4373669756431084544L, 4607769250590803968L, 4851153699010013696L,
            5104386732888089088L, 5368078801878179840L, 5642892152071508992L, 5929546454235676672L,
            6228825183767088640L, 6541582872345928704L, 6868753373709051392L, 7211359313221728768L,
            7570522924211155456L, 7947478514849258496L, 8343586859683210240L, 8760351872223335936L,
            9199439992591985152L, 3921213358161086464L, 4850044974529517568L, 6296921656268977664L,
            8292973759562619904L, 3075143244722959872L, 6274953360832545280L, 2210268915646535680L,
            6778561557392493568L, 4052431235721115136L, 2017158148825034240L, 9030121550070927872L,
            8695700292397356032L, 749256839723897344L, 2212185722591297024L, 4780810392134832640L, 8573064486620626432L,
            4685427019526152192L, 2026848442171050496L, 795427058175291392L, 1220057396927800832L, 3568726050706383360L,
            8159594562977490432L, 5249819115171273728L, 5086123243830243840L, 8282283418103647744L,
            4836310890809390080L, 6126145310249794560L, 2267629071146367488L, 5974727006028887552L,
            8264939763687273472L, 608340989510339584L, 828635949469412864L, 8856898102852928512L, 1720475810403128832L,
            3683415679219046400L, -1284830592014387712L, -9223372036854775808L, -9223372036854775808L,
            -9223372036854775808L};
        /**
         * The precomputed ziggurat lengths, denoted X_i in the main text. X_i = length of
         * ziggurat layer i. Values have been scaled by 2^-63.
         */
        private static final double[] X = {9.066125976394918e-19, 8.263176964596684e-19, 7.784282104206918e-19,
            7.440138137244747e-19, 7.170634551247567e-19, 6.948734962698102e-19, 6.759905994143853e-19,
            6.595417386676109e-19, 6.44960764869756e-19, 6.318593172398609e-19, 6.199592614043609e-19,
            6.090544855608598e-19, 5.989879531319532e-19, 5.896372336649496e-19, 5.809050077944015e-19,
            5.727126242519077e-19, 5.649956016143745e-19, 5.577004098080976e-19, 5.507821175595968e-19,
            5.442026402205546e-19, 5.379294128626943e-19, 5.319343704005985e-19, 5.261931531836978e-19,
            5.206844807204844e-19, 5.153896525288391e-19, 5.102921463263689e-19, 5.0537729161623905e-19,
            5.006320022905563e-19, 4.960445558817678e-19, 4.916044100170685e-19, 4.873020487905351e-19,
            4.831288533806017e-19, 4.790769924576001e-19, 4.7513932885346785e-19, 4.713093396787531e-19,
            4.675810476249156e-19, 4.639489616220167e-19, 4.604080253621108e-19, 4.569535724684215e-19,
            4.535812873056953e-19, 4.502871706000609e-19, 4.470675091764292e-19, 4.43918849234975e-19,
            4.408379726809414e-19, 4.378218760981047e-19, 4.348677520190027e-19, 4.3197297219702936e-19,
            4.2913507262878116e-19, 4.263517401111986e-19, 4.236208001483923e-19, 4.209402060485899e-19,
            4.1830802907323973e-19, 4.1572244951862374e-19, 4.1318174862592063e-19, 4.106843012289693e-19,
            4.0822856906037985e-19, 4.058130946464292e-19, 4.0343649572961074e-19, 4.0109746016499096e-19,
            3.987947412428338e-19, 3.965271533954313e-19, 3.942935682508446e-19, 3.920929110004198e-19,
            3.8992415705058057e-19, 3.877863289325851e-19, 3.856784934467374e-19, 3.8359975902000546e-19,
            3.8154927325817224e-19, 3.795262206755667e-19, 3.7752982058712154e-19, 3.7555932514901226e-19,
            3.7361401753547253e-19, 3.7169321024057316e-19, 3.69796243494815e-19, 3.6792248378733595e-19,
            3.6607132248538204e-19, 3.6424217454345153e-19, 3.6243447729520606e-19, 3.6064768932185395e-19,
            3.5888128939126416e-19, 3.571347754625651e-19, 3.554076637514328e-19, 3.5369948785167605e-19,
            3.520097979090946e-19, 3.5033815984391743e-19, 3.4868415461842876e-19, 3.470473775466644e-19,
            3.454274376433074e-19, 3.438239570091394e-19, 3.4223657025060954e-19, 3.4066492393126986e-19,
            3.3910867605299972e-19, 3.375674955650951e-19, 3.36041061899445e-19, 3.3452906453014644e-19,
            3.330312025560311e-19, 3.3154718430468554e-19, 3.300767269566494e-19, 3.2861955618856956e-19,
            3.2717540583417136e-19, 3.2574401756198997e-19, 3.2432514056887605e-19, 3.2291853128835623e-19,
            3.2152395311299247e-19, 3.2014117612994074e-19, 3.1876997686896117e-19, 3.1741013806218296e-19,
            3.1606144841497054e-19, 3.147237023872807e-19, 3.133966999849381e-19, 3.120802465602945e-19,
            3.107741526217679e-19, 3.094782336517918e-19, 3.081923099327311e-19, 3.069162063803502e-19,
            3.056497523844424e-19, 3.0439278165625384e-19, 3.0314513208235683e-19, 3.0190664558464783e-19,
            3.006771679861636e-19, 2.994565488824278e-19, 2.982446415180565e-19, 2.97041302668365e-19,
            2.9584639252573667e-19, 2.946597745905224e-19, 2.9348131556625803e-19, 2.923108852589938e-19,
            2.9114835648054477e-19, 2.899936049554792e-19, 2.888465092316728e-19, 2.8770695059426596e-19,
            2.865748129828689e-19, 2.8544998291186936e-19, 2.843323493937027e-19, 2.8322180386495424e-19,
            2.8211824011516853e-19, 2.810215542182466e-19, 2.7993164446631984e-19, 2.788484113059931e-19,
            2.7777175727685546e-19, 2.767015869521629e-19, 2.756378068816009e-19, 2.745803255360393e-19,
            2.7352905325419784e-19, 2.724839021911418e-19, 2.7144478626853394e-19, 2.7041162112657045e-19,
            2.6938432407753285e-19, 2.683628140608911e-19, 2.673470115998956e-19, 2.663368387595991e-19,
            2.653322191062523e-19, 2.6433307766801955e-19, 2.6333934089696193e-19, 2.623509366322412e-19,
            2.6136779406449473e-19, 2.603898437013396e-19, 2.5941701733396064e-19, 2.5844924800474283e-19,
            2.574864699759092e-19, 2.565286186991254e-19, 2.5557563078603673e-19, 2.546274439797022e-19,
            2.5368399712689353e-19, 2.5274523015122715e-19, 2.5181108402709945e-19, 2.508815007543965e-19,
            2.499564233339501e-19, 2.4903579574371396e-19, 2.481195629156353e-19, 2.472076707131958e-19,
            2.4630006590960037e-19, 2.4539669616659003e-19, 2.4449751001385796e-19, 2.4360245682904773e-19,
            2.427114868183142e-19, 2.418245509974277e-19, 2.4094160117340305e-19, 2.4006258992663645e-19,
            2.391874705935326e-19, 2.3831619724960594e-19, 2.3744872469304068e-19, 2.3658500842869427e-19,
            2.357250046525298e-19, 2.3486867023646363e-19, 2.340159627136144e-19, 2.3316684026394113e-19,
            2.3232126170025724e-19, 2.3147918645460906e-19, 2.3064057456500724e-19, 2.298053866624993e-19,
            2.289735839585738e-19, 2.2814512823288434e-19, 2.2731998182128476e-19, 2.264981076041651e-19,
            2.2567946899507905e-19, 2.248640299296549e-19, 2.240517548547797e-19, 2.232426087180504e-19,
            2.2243655695748146e-19, 2.2163356549146385e-19, 2.2083360070896563e-19, 2.2003662945996843e-19,
            2.192426190461318e-19, 2.1845153721167946e-19, 2.176633521345008e-19, 2.1687803241746032e-19,
            2.1609554707991062e-19, 2.153158655494016e-19, 2.145389576535809e-19, 2.1376479361227967e-19,
            2.1299334402977885e-19, 2.1222457988725017e-19, 2.1145847253536737e-19, 2.1069499368708242e-19,
            2.0993411541056253e-19, 2.0917581012228278e-19, 2.0842005058027066e-19, 2.076668098774977e-19,
            2.0691606143541438e-19, 2.061677789976242e-19, 2.0542193662369327e-19, 2.0467850868309113e-19,
            2.039374698492599e-19, 2.031987950938075e-19, 2.0246245968082214e-19, 2.0172843916130424e-19,
            2.0099670936771298e-19, 2.0026724640862402e-19, 1.9954002666349563e-19, 1.9881502677754006e-19,
            1.9809222365669745e-19, 1.9737159446270942e-19, 1.966531166082896e-19, 1.959367677523886e-19,
            1.9522252579555087e-19, 1.9451036887536058e-19, 1.9380027536197503e-19, 1.9309222385374197e-19,
            1.9238619317289982e-19, 1.9168216236135756e-19, 1.9098011067655294e-19, 1.9028001758738615e-19,
            1.8958186277022764e-19, 1.8888562610499751e-19, 1.88191287671315e-19, 1.874988277447156e-19,
            1.8680822679293485e-19, 1.8611946547225595e-19, 1.8543252462392036e-19, 1.8474738527059914e-19,
            1.840640286129235e-19, 1.833824360260732e-19, 1.8270258905642057e-19, 1.820244694182295e-19,
            1.8134805899040715e-19, 1.8067333981330703e-19, 1.8000029408558255e-19, 1.793289041610889e-19,
            1.786591525458323e-19, 1.779910218949653e-19, 1.773244950098264e-19, 1.7665955483502355e-19,
            1.75996184455559e-19, 1.7533436709399558e-19, 1.7467408610766236e-19, 1.7401532498589862e-19,
            1.7335806734733532e-19, 1.727022969372126e-19, 1.7204799762473213e-19, 1.7139515340044364e-19,
            1.70743748373664e-19, 1.7009376676992816e-19, 1.694451929284709e-19, 1.6879801129973803e-19,
            1.6815220644292632e-19, 1.6750776302355125e-19, 1.6686466581104123e-19, 1.6622289967635762e-19,
            1.655824495896394e-19, 1.6494330061787188e-19, 1.64305437922578e-19, 1.6366884675753161e-19,
            1.6303351246649206e-19, 1.6239942048095857e-19, 1.61766556317944e-19, 1.6113490557776692e-19,
            1.6050445394186123e-19, 1.598751871706023e-19, 1.5924709110114872e-19, 1.5862015164529922e-19,
            1.5799435478736328e-19, 1.5736968658204502e-19, 1.5674613315233954e-19, 1.561236806874404e-19,
            1.55502315440658e-19, 1.5488202372734756e-19, 1.5426279192284608e-19, 1.536446064604174e-19,
            1.5302745382920442e-19, 1.5241132057218782e-19, 1.5179619328415014e-19, 1.5118205860964477e-19,
            1.505689032409686e-19, 1.4995671391613777e-19, 1.4934547741686533e-19, 1.4873518056654034e-19,
            1.4812581022820734e-19, 1.47517353302545e-19, 1.4690979672584366e-19, 1.4630312746798053e-19,
            1.456973325303913e-19, 1.4509239894403808e-19, 1.444883137673718e-19, 1.438850640842889e-19,
            1.4328263700208072e-19, 1.4268101964937506e-19, 1.4208019917406874e-19, 1.4148016274125018e-19,
            1.4088089753111074e-19, 1.4028239073684423e-19, 1.396846295625332e-19, 1.3908760122102091e-19,
            1.3849129293176794e-19, 1.3789569191869245e-19, 1.3730078540799268e-19, 1.3670656062595043e-19,
            1.361130047967149e-19, 1.3552010514006475e-19, 1.3492784886914775e-19, 1.343362231881967e-19,
            1.337452152902197e-19, 1.3315481235466403e-19, 1.3256500154505191e-19, 1.3197577000658667e-19,
            1.3138710486372786e-19, 1.3079899321773381e-19, 1.3021142214417001e-19, 1.296243786903814e-19,
            1.2903784987292747e-19, 1.2845182267497766e-19, 1.2786628404366604e-19, 1.2728122088740255e-19,
            1.2669662007313963e-19, 1.2611246842359178e-19, 1.2552875271440607e-19, 1.2494545967128163e-19,
            1.2436257596703557e-19, 1.2378008821861334e-19, 1.231979829840411e-19, 1.2261624675931726e-19,
            1.2203486597524123e-19, 1.2145382699417624e-19, 1.2087311610674357e-19, 1.2029271952844551e-19,
            1.1971262339621383e-19, 1.1913281376488073e-19, 1.1855327660356906e-19, 1.1797399779199848e-19,
            1.1739496311670397e-19, 1.1681615826716313e-19, 1.1623756883182844e-19, 1.1565918029406041e-19,
            1.1508097802795774e-19, 1.1450294729408e-19, 1.1392507323505832e-19, 1.133473408710896e-19,
            1.1276973509530898e-19, 1.1219224066903596e-19, 1.1161484221688824e-19, 1.1103752422175824e-19,
            1.1046027101964606e-19, 1.0988306679434292e-19, 1.0930589557195864e-19, 1.0872874121528646e-19,
            1.0815158741799823e-19, 1.0757441769866241e-19, 1.0699721539457744e-19, 1.0641996365541205e-19,
            1.0584264543664434e-19, 1.0526524349279041e-19, 1.0468774037041343e-19, 1.0411011840090299e-19,
            1.0353235969301472e-19, 1.0295444612515902e-19, 1.0237635933742761e-19, 1.0179808072334579e-19,
            1.0121959142133761e-19, 1.0064087230589085e-19, 1.0006190397840746e-19, 9.948266675772465e-20,
            9.890314067029116e-20, 9.832330543998192e-20, 9.77431404775337e-20, 9.716262486958349e-20,
            9.658173736728982e-20, 9.600045637451667e-20, 9.541875993555806e-20, 9.483662572238009e-20,
            9.425403102135636e-20, 9.36709527194705e-20, 9.308736728995855e-20, 9.250325077736202e-20,
            9.191857878196082e-20, 9.133332644355302e-20, 9.074746842454686e-20, 9.016097889232776e-20,
            8.957383150086088e-20, 8.898599937148752e-20, 8.83974550728703e-20, 8.780817060003978e-20,
            8.721811735249173e-20, 8.662726611128059e-20, 8.603558701505162e-20, 8.544304953494964e-20,
            8.484962244833846e-20, 8.425527381126013e-20, 8.365997092955832e-20, 8.306368032858488e-20,
            8.246636772140225e-20, 8.186799797538864e-20, 8.126853507714563e-20, 8.066794209560029e-20,
            8.006618114318622e-20, 7.946321333497837e-20, 7.885899874564743e-20, 7.825349636408865e-20,
            7.764666404556869e-20, 7.703845846122129e-20, 7.642883504470902e-20, 7.58177479358532e-20,
            7.520514992101743e-20, 7.459099237001235e-20, 7.397522516926911e-20, 7.335779665100703e-20,
            7.273865351809722e-20, 7.211774076429659e-20, 7.149500158949814e-20, 7.087037730961018e-20,
            7.024380726064147e-20, 6.96152286965294e-20, 6.89845766802037e-20, 6.835178396732923e-20,
            6.771678088211617e-20, 6.707949518452485e-20, 6.64398519281239e-20, 6.579777330778369e-20,
            6.515317849630154e-20, 6.450598346895782e-20, 6.385610081489444e-20, 6.320343953408397e-20,
            6.254790481851989e-20, 6.188939781610152e-20, 6.122781537550966e-20, 6.056304977016756e-20,
            5.989498839915096e-20, 5.922351346264923e-20, 5.854850160927849e-20, 5.786982355220268e-20,
            5.718734365062205e-20, 5.650091945273011e-20, 5.581040119571103e-20, 5.511563125773486e-20,
            5.441644355619287e-20, 5.3712662885580816e-20, 5.3004104187460606e-20, 5.2290571743781936e-20,
            5.157185828349097e-20, 5.084774399074948e-20, 5.011799540118182e-20, 4.9382364170293304e-20,
            4.8640585695477616e-20, 4.789237756975016e-20, 4.7137437841375266e-20, 4.637544304873078e-20,
            4.5606045993857614e-20, 4.482887321089667e-20, 4.40435220766595e-20, 4.324955749943926e-20,
            4.2446508108220653e-20, 4.163386184685984e-20, 4.081106085546285e-20, 3.997749549257881e-20,
            3.9132497314870153e-20, 3.827533078275297e-20, 3.740518339709212e-20, 3.6521153887674253e-20,
            3.5622237960646127e-20, 3.470731095738857e-20, 3.377510656357792e-20, 3.282419040754845e-20,
            3.1852926960062177e-20, 3.085943752802367e-20, 2.9841546217580957e-20, 2.879670935396108e-20,
            2.772192169113539e-20, 2.661358930495413e-20, 2.5467353391178318e-20, 2.4277839478989958e-20,
            2.303828920285994e-20, 2.1739999061414015e-20, 2.037142499071807e-20, 1.8916669455626665e-20,
            1.7352728004959384e-20, 1.56439466915464e-20, 1.3729109753658277e-20, 1.1483304366567183e-20,
            8.53241131926212e-21, 0.0};
        /** The precomputed ziggurat heights, denoted Y_i in the main text. Y_i = f(X_i).
         * Values have been scaled by 2^-63. */
        private static final double[] Y = {2.532379772713818e-23, 5.310835823923221e-23, 8.26022457065042e-23,
            1.134603744347431e-22, 1.4547828564666417e-22, 1.7851865078182134e-22, 2.1248195450141036e-22,
            2.472922973401801e-22, 2.8288961626257085e-22, 3.1922503684288075e-22, 3.5625791217647975e-22,
            3.9395384018258527e-22, 4.322832822347196e-22, 4.712205689742199e-22, 5.107431651499161e-22,
            5.508311133935702e-22, 5.914666050219951e-22, 6.32633643158829e-22, 6.743177743380035e-22,
            7.165058718271907e-22, 7.591859586388e-22, 8.023470614308747e-22, 8.459790887588371e-22,
            8.900727287454353e-22, 9.346193623979475e-22, 9.7961098965457e-22, 1.0250401658767016e-21,
            1.070899946982291e-21, 1.117183841780181e-21, 1.1638857703464862e-21, 1.211000027502763e-21,
            1.2585212506275e-21, 1.3064443911684845e-21, 1.3547646893321801e-21, 1.4034776515135506e-21,
            1.4525790301004666e-21, 1.5020648053444311e-21, 1.5519311690365945e-21, 1.60217450976698e-21,
            1.6527913995771288e-21, 1.7037785818432865e-21, 1.7551329602497868e-21, 1.8068515887312395e-21,
            1.858931662278158e-21, 1.9113705085142316e-21, 1.964165579965045e-21, 2.0173144469479217e-21,
            2.0708147910210758e-21, 2.1246643989375575e-21, 2.178861157055798e-21, 2.233403046164028e-21,
            2.28828813668061e-21, 2.343514584196453e-21, 2.39908062532932e-21, 2.4549845738630073e-21,
            2.5112248171471613e-21, 2.5677998127359586e-21, 2.6247080852460518e-21, 2.681948223416096e-21,
            2.73951887735187e-21, 2.7974187559425357e-21, 2.8556466244349068e-21, 2.914201302153814e-21,
            2.973081660357729e-21, 3.0322866202197593e-21, 3.0918151509250072e-21, 3.151666267876049e-21,
            3.2118390309989992e-21, 3.2723325431432467e-21, 3.3331459485685267e-21, 3.3942784315135004e-21,
            3.455729214840488e-21, 3.51749755875142e-21, 3.579582759570448e-21, 3.641984148589033e-21,
            3.7047010909696115e-21, 3.7677329847042535e-21, 3.831079259624996e-21, 3.8947393764627574e-21,
            3.958712825951976e-21, 4.0229991279783226e-21, 4.087597830767002e-21, 4.152508510109364e-21,
            4.2177307686256654e-21, 4.283264235061998e-21, 4.34910856361952e-21, 4.4152634333142514e-21,
            4.481728547365803e-21, 4.5485036326135354e-21, 4.6155884389587065e-21, 4.682982738831299e-21,
            4.750686326680256e-21, 4.818699018485977e-21, 4.887020651293957e-21, 4.955651082768556e-21,
            5.0245901907659065e-21, 5.093837872925075e-21, 5.163394046276591e-21, 5.2332586468675656e-21,
            5.303431629402611e-21, 5.3739129668998646e-21, 5.444702650361428e-21, 5.515800688457595e-21,
            5.5872071072242535e-21, 5.658921949772901e-21, 5.730945276012736e-21, 5.803277162384305e-21,
            5.875917701604242e-21, 5.948867002420633e-21, 6.022125189378572e-21, 6.0956924025955166e-21,
            6.169568797546033e-21, 6.243754544855583e-21, 6.3182498301029956e-21, 6.393054853631293e-21,
            6.468169830366561e-21, 6.5435949896445654e-21, 6.61933057504483e-21, 6.695376844231907e-21,
            6.7717340688035926e-21, 6.848402534145828e-21, 6.925382539294077e-21, 7.002674396800946e-21,
            7.080278432609835e-21, 7.158194985934437e-21, 7.236424409143883e-21, 7.314967067653352e-21,
            7.393823339819985e-21, 7.472993616843931e-21, 7.552478302674367e-21, 7.632277813920362e-21,
            7.71239257976641e-21, 7.792823041892541e-21, 7.873569654398836e-21, 7.954632883734263e-21,
            8.036013208629687e-21, 8.117711120034972e-21, 8.199727121060036e-21, 8.282061726919792e-21,
            8.364715464882844e-21, 8.447688874223886e-21, 8.530982506179678e-21, 8.614596923908543e-21,
            8.698532702453287e-21, 8.782790428707488e-21, 8.867370701385059e-21, 8.952274130993032e-21,
            9.037501339807492e-21, 9.123052961852595e-21, 9.208929642882634e-21, 9.295132040367049e-21,
            9.381660823478398e-21, 9.468516673083161e-21, 9.555700281735394e-21, 9.643212353673148e-21,
            9.731053604817628e-21, 9.819224762775044e-21, 9.907726566841127e-21, 9.996559768008253e-21,
            1.0085725128975164e-20, 1.0175223424159242e-20, 1.0265055439711297e-20, 1.0355221973532876e-20,
            1.0445723835296011e-20, 1.0536561846465444e-20, 1.0627736840323247e-20, 1.0719249661995874e-20,
            1.0811101168483563e-20, 1.0903292228692131e-20, 1.0995823723467109e-20, 1.1088696545630196e-20,
            1.118191160001806e-20, 1.1275469803523424e-20, 1.1369372085138467e-20, 1.14636193860005e-20,
            1.155821265943994e-20, 1.1653152871030541e-20, 1.1748440998641906e-20, 1.1844078032494261e-20,
            1.1940064975215492e-20, 1.2036402841900448e-20, 1.2133092660172487e-20, 1.2230135470247318e-20,
            1.2327532324999065e-20, 1.2425284290028643e-20, 1.2523392443734356e-20, 1.2621857877384821e-20,
            1.2720681695194142e-20, 1.2819865014399386e-20, 1.2919408965340364e-20, 1.3019314691541709e-20,
            1.311958334979729e-20, 1.3220216110256947e-20, 1.332121415651558e-20, 1.342257868570459e-20,
            1.35243109085857e-20, 1.3626412049647167e-20, 1.3728883347202408e-20, 1.3831726053491038e-20,
            1.393494143478237e-20, 1.403853077148138e-20, 1.4142495358237157e-20, 1.424683650405386e-20,
            1.4351555532404212e-20, 1.445665378134558e-20, 1.45621326036386e-20, 1.466799336686844e-20,
            1.4774237453568695e-20, 1.4880866261347982e-20, 1.498788120301918e-20, 1.5095283706731464e-20,
            1.520307521610505e-20, 1.53112571903688e-20, 1.5419831104500607e-20, 1.5528798449370678e-20,
            1.5638160731887733e-20, 1.5747919475148132e-20, 1.5858076218588015e-20, 1.596863251813844e-20,
            1.607958994638363e-20, 1.619095009272232e-20, 1.6302714563532247e-20, 1.6414884982337896e-20,
            1.6527462989981453e-20, 1.6640450244797108e-20, 1.675384842278868e-20, 1.6867659217810695e-20,
            1.6981884341752875e-20, 1.7096525524728213e-20, 1.721158451526456e-20, 1.7327063080499912e-20,
            1.7442963006381335e-20, 1.7559286097867713e-20, 1.767603417913626e-20, 1.779320909379297e-20,
            1.7910812705086994e-20, 1.8028846896129066e-20, 1.8147313570114e-20, 1.82662146505474e-20,
            1.8385552081476575e-20, 1.8505327827725806e-20, 1.8625543875136006e-20, 1.874620223080887e-20,
            1.8867304923355594e-20, 1.8988854003150238e-20, 1.911085154258786e-20, 1.9233299636347447e-20,
            1.9356200401659812e-20, 1.9479555978580487e-20, 1.9603368530267746e-20, 1.9727640243265847e-20,
            1.9852373327793618e-20, 1.9977570018038445e-20, 2.010323257245582e-20, 2.0229363274074546e-20,
            2.03559644308077e-20, 2.0483038375769486e-20, 2.061058746759809e-20, 2.073861409078469e-20,
            2.0867120656008704e-20, 2.099610960047943e-20, 2.1125583388284243e-20, 2.1255544510743418e-20,
            2.1385995486771794e-20, 2.151693886324738e-20, 2.1648377215387072e-20, 2.1780313147129627e-20,
            2.1912749291526057e-20, 2.204568831113762e-20, 2.2179132898441537e-20, 2.2313085776244664e-20,
            2.2447549698105236e-20, 2.2582527448762922e-20, 2.2718021844577323e-20, 2.285403573397516e-20,
            2.2990571997906318e-20, 2.3127633550308938e-20, 2.3265223338583805e-20, 2.3403344344078233e-20,
            2.3541999582579633e-20, 2.3681192104819063e-20, 2.3820924996984908e-20, 2.396120138124703e-20,
            2.4102024416291538e-20, 2.4243397297866505e-20, 2.438532325933887e-20, 2.452780557226279e-20,
            2.467084754695973e-20, 2.4814452533110573e-20, 2.4958623920360058e-20, 2.5103365138933835e-20,
            2.5248679660268454e-20, 2.539457099765463e-20, 2.5541042706894083e-20, 2.5688098386970325e-20,
            2.583574168073375e-20, 2.5983976275601352e-20, 2.61328059042715e-20, 2.6282234345454126e-20,
            2.643226542461671e-20, 2.6582903014746527e-20, 2.6734151037129546e-20, 2.6886013462146377e-20,
            2.7038494310085842e-20, 2.7191597651976473e-20, 2.7345327610436566e-20, 2.7499688360543205e-20,
            2.7654684130720776e-20, 2.781031920364953e-20, 2.7966597917194705e-20, 2.8123524665356816e-20,
            2.8281103899243675e-20, 2.8439340128064684e-20, 2.859823792014815e-20, 2.875780190398213e-20,
            2.891803676927963e-20, 2.907894726806858e-20, 2.9240538215807684e-20, 2.9402814492528446e-20,
            2.9565781044004507e-20, 2.972944288294883e-20, 2.9893805090239645e-20, 3.005887281617601e-20,
            3.02246512817638e-20, 3.039114578003308e-20, 3.055836167738775e-20, 3.072630441498844e-20,
            3.0894979510169736e-20, 3.106439255789257e-20, 3.123454923223312e-20, 3.1405455287909063e-20,
            3.157711656184457e-20, 3.174953897477506e-20, 3.1922728532893043e-20, 3.209669132953634e-20,
            3.2271433546919926e-20, 3.2446961457912936e-20, 3.26232814278621e-20, 3.280039991646321e-20,
            3.2978323479682113e-20, 3.3157058771726903e-20, 3.3336612547072853e-20, 3.3516991662541965e-20,
            3.369820307943883e-20, 3.388025386574477e-20, 3.406315119837208e-20, 3.424690236548051e-20,
            3.4431514768858065e-20, 3.4616995926368224e-20, 3.4803353474466e-20, 3.4990595170785116e-20,
            3.517872889679876e-20, 3.5367762660556557e-20, 3.555770459950039e-20, 3.5748562983361843e-20,
            3.594034621714422e-20, 3.6133062844192156e-20, 3.6326721549351945e-20, 3.652133116222594e-20,
            3.671690066052438e-20, 3.691343917351836e-20, 3.711095598559755e-20, 3.7309460539936645e-20,
            3.750896244227467e-20, 3.770947146481129e-20, 3.79109975502247e-20, 3.811355081581572e-20,
            3.831714155778291e-20, 3.852178025563392e-20, 3.872747757673826e-20, 3.893424438102728e-20,
            3.914209172584698e-20, 3.935103087096993e-20, 3.956107328377266e-20, 3.977223064458528e-20,
            3.998451485222028e-20, 4.019793802968809e-20, 4.041251253010693e-20, 4.0628250942815264e-20,
            4.08451660996954e-20, 4.106327108171703e-20, 4.1282579225710425e-20, 4.1503104131378944e-20,
            4.172485966856146e-20, 4.194785998475555e-20, 4.2172119512913083e-20, 4.2397652979520307e-20,
            4.26244754129752e-20, 4.285260215227567e-20, 4.3082048856032696e-20, 4.3312831511823486e-20,
            4.35449664459004e-20, 4.377847033327237e-20, 4.4013360208176433e-20, 4.424965347495792e-20,
            4.44873679193791e-20, 4.472652172037699e-20, 4.4967133462292346e-20, 4.5209222147593173e-20,
            4.545280721011741e-20, 4.5697908528860855e-20, 4.5944546442338236e-20, 4.619274176354652e-20,
            4.6442515795561947e-20, 4.6693890347803697e-20, 4.694688775299949e-20, 4.720153088489046e-20,
            4.745784317671508e-20, 4.771584864051452e-20, 4.797557188730451e-20, 4.8237038148161644e-20,
            4.850027329627554e-20, 4.876530387002128e-20, 4.903215709711068e-20, 4.930086091988455e-20,
            4.957144402181263e-20, 4.9843935855272545e-20, 5.011836667068401e-20, 5.039476754708001e-20,
            5.0673170424202713e-20, 5.0953608136218094e-20, 5.123611444715016e-20, 5.152072408814347e-20,
            5.1807472796670477e-20, 5.2096397357809406e-20, 5.2387535647727967e-20, 5.268092667951877e-20,
            5.2976610651543945e-20, 5.3274628998459006e-20, 5.357502444509989e-20, 5.387784106343208e-20,
            5.4183124332777464e-20, 5.449092120355254e-20, 5.480128016477185e-20, 5.511425131559201e-20,
            5.542988644119667e-20, 5.574823909334829e-20, 5.606936467596343e-20, 5.639332053609944e-20,
            5.672016606077763e-20, 5.704996278010726e-20, 5.738277447721936e-20, 5.771866730556852e-20,
            5.805770991421623e-20, 5.839997358176981e-20, 5.874553235972036e-20, 5.909446322599911e-20,
            5.944684624965775e-20, 5.98027647676747e-20, 6.016230557499789e-20, 6.052555912905671e-20,
            6.089261977011434e-20, 6.126358595898756e-20, 6.163856053383874e-20, 6.201765098794606e-20,
            6.240096977058771e-20, 6.278863461343729e-20, 6.318076888516832e-20, 6.357750197730895e-20,
            6.397896972478401e-20, 6.438531486503798e-20, 6.479668754015973e-20, 6.521324584704252e-20,
            6.563515644132436e-20, 6.60625952016853e-20, 6.649574796205078e-20, 6.693481132039372e-20,
            6.737999353417576e-20, 6.78315155140629e-20, 6.82896119294466e-20, 6.87545324415615e-20,
            6.922654308270048e-20, 6.970592780328698e-20, 7.01929902125074e-20, 7.068805554299649e-20,
            7.119147287592182e-20, 7.170361767000327e-20, 7.222489464688847e-20, 7.275574109635319e-20,
            7.329663067862371e-20, 7.384807781854906e-20, 7.44106428084867e-20, 7.498493776510197e-20,
            7.557163362186687e-20, 7.617146838671345e-20, 7.678525695702381e-20, 7.741390286755859e-20,
            7.805841245914747e-20, 7.871991210882315e-20, 7.939966937313497e-20, 8.009911919213852e-20,
            8.081989672282774e-20, 8.156387898169583e-20, 8.233323837990194e-20, 8.313051260166436e-20,
            8.395869739690673e-20, 8.482137224231827e-20, 8.572287440027122e-20, 8.666854644243818e-20,
            8.766509934779227e-20, 8.872116535656441e-20, 8.984817900681022e-20, 9.106186379912279e-20,
            9.238493381053173e-20, 9.385252216864754e-20, 9.552479913718005e-20, 9.752412557725429e-20,
            1.0021490936397442e-19, 1.0842021724855044e-19};

        /** Underlying source of randomness. */
        protected final UniformRandomProvider rng;

        /**
         * @param rng Generator of uniformly distributed random numbers.
         */
        ModifiedZigguratExponentialSampler512(UniformRandomProvider rng) {
            this.rng = rng;
        }

        /** {@inheritDoc} */
        @Override
        public double sample() {
            return createSample();
        }

        /**
         * Creates the exponential sample with {@code mean = 1}.
         *
         * <p>Note: This has been extracted to a separate method so that the recursive call
         * when sampling tries again targets this function. This allows sub-classes
         * to override the sample method to generate a sample with a different mean using:
         * <pre>
         * @Override
         * public double sample() {
         *     return super.sample() * mean;
         * }
         * </pre>
         * Otherwise the sub-class {@code sample()} method will recursively call
         * the overloaded sample() method when trying again which creates a bad sample due
         * to compound multiplication of the mean.
         *
         * @return the sample
         */
        final double createSample() {
            final long x = nextLong();
            // Float multiplication squashes these last 9 bits, so they can be used to sample i
            final int i = ((int) x) & 0x1ff;

            if (i < I_MAX) {
                // Early exit.
                return X[i] * (x & MAX_INT64);
            }
            final int j = selectRegion();
            return j == 0 ? X_0 + createSample() : sampleOverhang(j);
        }

        /**
         * Select the overhang region or the tail using alias sampling.
         *
         * @return the region
         */
        protected int selectRegion() {
            final long x = nextLong();
            // j in [0, 512)
            final int j = ((int) x) & 0x1ff;
            // map to j in [0, N] with N the number of layers of the ziggurat
            return x >= IPMF[j] ? MAP[j] : j;
        }

        /**
         * Sample from overhang region {@code j}.
         *
         * @param j Index j (must be {@code > 0})
         * @return the sample
         */
        protected double sampleOverhang(int j) {
            // Sample from the triangle:
            //    X[j],Y[j]
            //        |\-->u1
            //        | \  |
            //        |  \ |
            //        |   \|    Overhang j (with hypotenuse not pdf(x))
            //        |    \
            //        |    |\
            //        |    | \
            //        |    u2 \
            //        +-------- X[j-1],Y[j-1]
            // u2 = u1 + (u2 - u1) = u1 + uDistance
            // If u2 < u1 then reflect in the hypotenuse by swapping u1 and u2.
            long u1 = randomInt63();
            long uDistance = randomInt63() - u1;
            if (uDistance < 0) {
                uDistance = -uDistance;
                u1 -= uDistance;
            }
            final double x = sampleX(X, j, u1);
            if (uDistance >= E_MAX) {
                // Early Exit: x < y - epsilon
                return x;
            }
            return sampleY(Y, j, u1 + uDistance) <= Math.exp(-x) ? x : sampleOverhang(j);
        }

        /**
         * Generates a {@code long}.
         *
         * @return the long
         */
        protected long nextLong() {
            return rng.nextLong();
        }

        /**
         * Return a positive long in {@code [0, 2^63)}.
         *
         * @return the long
         */
        protected long randomInt63() {
            return rng.nextLong() & MAX_INT64;
        }
    }

    /**
     * Compute the x value of the point in the overhang region from the uniform deviate.
     * <pre>{@code
     *    X[j],Y[j]
     *        |\ |
     *        | \|
     *        |  \
     *        |  |\    Ziggurat overhang j (with hypotenuse not pdf(x))
     *        |  | \
     *        |  u2 \
     *        |      \
     *        |-->u1  \
     *        +-------- X[j-1],Y[j-1]
     *
     *   x = X[j] + u1 * (X[j-1] - X[j])
     * }</pre>
     *
     * @param x Ziggurat data table X. Values assumed to be scaled by 2^-63.
     * @param j Index j. Value assumed to be above zero.
     * @param u1 Uniform deviate. Value assumed to be in {@code [0, 2^63)}.
     * @return y
     */
    static double sampleX(double[] x, int j, long u1) {
        return x[j] * TWO_POW_63 + u1 * (x[j - 1] - x[j]);
    }

    /**
     * Compute the y value of the point in the overhang region from the uniform deviate.
     * <pre>{@code
     *    X[j],Y[j]
     *        |\ |
     *        | \|
     *        |  \
     *        |  |\    Ziggurat overhang j (with hypotenuse not pdf(x))
     *        |  | \
     *        |  u2 \
     *        |      \
     *        |-->u1  \
     *        +-------- X[j-1],Y[j-1]
     *
     *   y = Y[j-1] + (1-u2) * (Y[j] - Y[j-1])
     * }</pre>
     *
     * @param y Ziggurat data table Y. Values assumed to be scaled by 2^-63.
     * @param j Index j. Value assumed to be above zero.
     * @param u2 Uniform deviate. Value assumed to be in {@code [0, 2^63)}.
     * @return y
     */
    static double sampleY(double[] y, int j, long u2) {
        // Note: u2 is in [0, 2^63)
        // Long.MIN_VALUE is used as an unsigned int with value 2^63:
        // 1 - u2 = Long.MIN_VALUE - u2
        //
        // The subtraction from 1 can be avoided with:
        // y = Y[j] + u2 * (Y[j-1] - Y[j])
        // This is effectively sampleX(y, j, u2)
        // Tests show the alternative is 1 ULP different with approximately 3% frequency.
        // It has not been measured more than 1 ULP different.
        return y[j - 1] * TWO_POW_63 + (Long.MIN_VALUE - u2) * (y[j] - y[j - 1]);
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
     * Benchmark methods for obtaining an index from the lower bits of a long and
     * comparing them to a limit then returning the index as an {@code int}.
     *
     * <p>Note: This is disabled as there is no measurable difference between methods.
     *
     * @param sources Source of randomness.
     * @return the sample value
     */
    //@Benchmark
    public int compareIndex(IndexCompareSources sources) {
        return sources.getSampler().sample();
    }

    /**
     * Benchmark methods for obtaining an unsigned long.
     *
     * <p>Note: This is disabled. Either there is no measurable difference between methods
     * or the bit shift method is marginally faster depending on JDK and platform.
     *
     * @param sources Source of randomness.
     * @return the sample value
     */
    //@Benchmark
    public long getUnsignedLong(LongSources sources) {
        return sources.getSampler().sample();
    }

    /**
     * Benchmark methods for obtaining an interpolated value from an unsigned long.
     *
     * <p>Note: This is disabled. The speed is typically: U1, 1minusU2, U_1minusU.
     *
     * @param sources Source of randomness.
     * @return the sample value
     */
    //@Benchmark
    public double interpolate(InterpolationSources sources) {
        return sources.getSampler().sample();
    }

    /**
     * Benchmark methods for obtaining a sign value from a long.
     *
     * <p>Note: This is disabled. The branchless versions using a subtraction of
     * 2 - 1 or 0 - 1 are faster.
     *
     * @param sources Source of randomness.
     * @return the sample value
     */
    //@Benchmark
    public double signBit(SignBitSources sources) {
        return sources.getSampler().sample();
    }

    /**
     * Benchmark methods for obtaining {@code exp(z)} when {@code -8 <= z <= 0}.
     *
     * <p>Note: This is disabled. On JDK 8 FastMath is faster. On JDK 11 Math.exp is
     * a hotspot intrinsic and may be faster based on the platform architecture.
     * Example results:
     *
     * <pre>
     *                     JDK 8                     JDK 11
     *                     i7-6820HQ     E5-1680     i7-6820HQ     E5-1680     3960X
     * noop                    4.523       3.871         4.351       4.206     3.936
     * Math.exp               61.350      48.519        22.552      19.195    12.568
     * FastMath.exp           33.858      24.469        31.396      24.568    11.159
     * </pre>
     *
     * <p>The ziggurat sampler avoids calls to Math.exp in the majority of cases
     * when using McFarland's fast method for overhangs which exploit the known
     * maximum difference between pdf(x) and the triangle hypotenuse. Example data
     * of the frequency that Math.exp is called per sample from the base
     * implementations (using n=2^31):
     *
     * <pre>
     *            Calls            FastOverhangs     SimpleOverhangs
     * Exp        exp(-x)          0.00271           0.0307
     * Normal     exp(-0.5*x*x)    0.00359           0.0197*
     *
     * * Increases to 0.0198 if the tail exponential sampler uses simple overhangs
     * </pre>
     *
     * <p>Note that the maximum difference between pdf(x) and the triangle
     * hypotenuse is smaller for the exponential distribution; thus the fast method
     * can avoid using Math.exp more often. In the case of simple overhangs the
     * normal distribution has more region covered by the ziggurat thus has a lower
     * frequency of overhang sampling.
     *
     * <p>A significant speed-up of the exp function may improve run-time of the
     * simple overhangs method. Any effect on the fast method is less likely to be
     * noticed. This is observed in benchmark times which show an improvement of the
     * simple overhangs method for the exponential relative to other methods on JDK
     * 11 vs 8. However the simple overhangs is still slower as the exponential
     * distribution is always concave and upper-right triangle samples are avoided
     * by the fast overhang method.
     *
     * @param sources Source of randomness.
     * @return the sample value
     */
    //@Benchmark
    public double exp(ExpSources sources) {
        return sources.getSampler().sample();
    }

    /**
     * Run the sampler.
     *
     * @param sources Source of randomness.
     * @return the sample value
     */
    @Benchmark
    public double sample(SingleSources sources) {
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
