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
import org.apache.commons.rng.sampling.distribution.StableSampler;
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

import java.util.concurrent.TimeUnit;

/**
 * Executes a benchmark to compare the speed of generation of stable random numbers
 * using different methods.
 *
 * <p>The test specifically includes a test of different versions of the sampler
 * when {@code beta=0}; {@code alpha=1}; or the generic case {@code alpha != 1} and
 * {@code beta != 0}.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@State(Scope.Benchmark)
@Fork(value = 1, jvmArgs = {"-server", "-Xms128M", "-Xmx128M"})
public class StableSamplerPerformance {
    /** The name of the baseline method. */
    static final String BASELINE = "baseline";

    /**
     * Samples from a stable distribution with zero location and unit scale.
     * Two implementations are available:
     *
     * <ol>
     * <li>Implements the Chambers-Mallows-Stuck (CMS) method from Chambers, et al
     * (1976) A Method for Simulating Stable Random Variables. Journal of the
     * American Statistical Association Vol. 71, No. 354, pp. 340-344.
     * This is a translation of the FORTRAN routine RSTAB; it computes a rearrangement
     * of the original formula to use double-angle identities and output a 0-parameterization
     * stable deviate.
     *
     * <li>The implementation uses the Chambers-Mallows-Stuck (CMS) method using
     * the formula proven in Weron (1996) "On the Chambers-Mallows-Stuck method for
     * simulating skewed stable random variables". Statistics &amp; Probability
     * Letters. 28 (2): 165–171. This outputs a 1-parameterization stable deviate and
     * has been modified to the 0-parameterization using a translation.
     * </ol>
     *
     * <p>The code has been copied from o.a.c.rng.sampling.distributions.StableSampler.
     * The classes have been renamed to StableRandomGenerator to distinguish them.
     * The copy implementation of the CMS algorithm RSTAB uses Math.tan to compute
     * tan(x) / x. The main StableSampler implements the CMS RSTAB algorithm using
     * fast approximation to the Math.tan function.
     * The implementation of the Weron formula has been copied as it is not possible
     * to instantiate these samplers directly. They are used for edge case computations.
     *
     * @see <a href="https://en.wikipedia.org/wiki/Stable_distribution">Stable
     * distribution (Wikipedia)</a>
     * @see <a href="https://doi.org/10.1080%2F01621459.1976.10480344">Chambers et
     * al (1976) JOASA 71: 340-344</a>
     * @see <a
     * href="https://citeseerx.ist.psu.edu/viewdoc/summary?doi=10.1.1.46.3280">Weron
     * (1996) Statistics &amp; Probability Letters. 28: 165–171</a>
     */
    abstract static class StableRandomGenerator implements ContinuousSampler {
        /** The lower support for the distribution. This is the lower bound of {@code (-inf, +inf)}. */
        static final double LOWER = Double.NEGATIVE_INFINITY;
        /** The upper support for the distribution. This is the upper bound of {@code (-inf, +inf)}. */
        static final double UPPER = Double.POSITIVE_INFINITY;
        /** pi / 2. */
        static final double PI_2 = Math.PI / 2;
        /** pi / 4. */
        static final double PI_4 = Math.PI / 4;
        /** pi/2 scaled by 2^-53. */
        static final double PI_2_SCALED = 0x1.0p-54 * Math.PI;
        /** pi/4 scaled by 2^-53. */
        static final double PI_4_SCALED = 0x1.0p-55 * Math.PI;
        /** Underlying source of randomness. */
        private final UniformRandomProvider rng;
        /** The exponential sampler. */
        private final ContinuousSampler expSampler;


        /**
         * @param rng Underlying source of randomness
         */
        StableRandomGenerator(final UniformRandomProvider rng) {
            this.rng = rng;
            expSampler = ZigguratSampler.Exponential.of(rng);
        }

        /**
         * Gets a random value for the omega parameter.
         * This is an exponential random variable with mean 1.
         *
         * @return omega
         */
        double getOmega() {
            return expSampler.sample();
        }

        /**
         * Gets a random value for the phi parameter.
         * This is a uniform random variable in {@code (-pi/2, pi/2)}.
         *
         * @return phi
         */
        double getPhi() {
            final double x = (rng.nextLong() >> 10) * PI_2_SCALED;
            if (x == -PI_2) {
                return getPhi();
            }
            return x;
        }

        /**
         * Gets a random value for the phi/2 parameter.
         * This is a uniform random variable in {@code (-pi/4, pi/4)}.
         *
         * @return phi/2
         */
        double getPhiBy2() {
            final double x = (rng.nextLong() >> 10) * PI_4_SCALED;
            if (x == -PI_4) {
                return getPhiBy2();
            }
            return x;
        }

        /**
         * Creates a sampler of a stable distribution with zero location and unit scale.
         *
         * <p>WARNING: Parameters are not validated.
         *
         * @param rng Generator of uniformly distributed random numbers.
         * @param alpha Stability parameter. Must be in range {@code (0, 2]}.
         * @param beta Skewness parameter. Must be in range {@code [-1, 1]}.
         * @param weron Set to true to use the Weron formulas. Default is CMS.
         * @return the sampler
         */
        static ContinuousSampler of(UniformRandomProvider rng,
                                    double alpha,
                                    double beta,
                                    boolean weron) {
            // WARNING:
            // No parameter validation
            // No Alpha=2 case
            // No Alpha=1, beta=0 case
            if (weron) {
                if (beta == 0.0) {
                    return new Beta0WeronStableRandomGenerator(rng, alpha);
                }
                if (alpha == 1.0) {
                    return new Alpha1WeronStableRandomGenerator(rng, alpha);
                }
                return new WeronStableRandomGenerator(rng, alpha, beta);
            }

            if (beta == 0.0) {
                return new Beta0CMSStableRandomGenerator(rng, alpha);
            }
            if (alpha == 1.0) {
                return new Alpha1CMSStableRandomGenerator(rng, alpha);
            }
            return new CMSStableRandomGenerator(rng, alpha, beta);
        }

        /**
         * Clip the sample {@code x} to the inclusive limits of the support.
         *
         * @param x Sample x.
         * @param lower Lower bound.
         * @param upper Upper bound.
         * @return x in [lower, upper]
         */
        static double clipToSupport(double x, double lower, double upper) {
            if (x < lower) {
                return lower;
            }
            return x < upper ? x : upper;
        }
    }

    /**
     * A baseline for a generator that uses an exponential sample and a uniform deviate in the
     * range {@code [-pi/2, pi/2]}.
     */
    private static class BaselineStableRandomGenerator extends StableRandomGenerator {
        /**
         * @param rng Underlying source of randomness
         */
        BaselineStableRandomGenerator(UniformRandomProvider rng) {
            super(rng);
        }

        @Override
        public double sample() {
            return getOmega() * getPhi();
        }
    }

    /**
     * Implement the generic stable distribution case: {@code alpha < 2} and {@code beta != 0},
     * and {@code alpha != 0}.
     *
     * <p>Implements the Weron formula.
     */
    private static class WeronStableRandomGenerator extends StableRandomGenerator {
        /** The lower support for the distribution. */
        protected final double lower;
        /** The upper support for the distribution. */
        protected final double upper;
        /** Epsilon (1 - alpha). */
        protected final double eps;
        /** Epsilon (1 - alpha). */
        protected final double meps1;
        /** Cache of expression value used in generation. */
        protected final double zeta;
        /** Cache of expression value used in generation. */
        protected final double atanZeta;
        /** Cache of expression value used in generation. */
        protected final double scale;
        /** 1 / alpha = 1 / (1 - eps). */
        protected final double inv1mEps;
        /** (1 / alpha) - 1 = eps / (1 - eps). */
        protected final double epsDiv1mEps;

        /**
         * @param rng Underlying source of randomness
         * @param alpha Stability parameter. Must be in range {@code (0, 2]}.
         * @param beta Skewness parameter. Must be in range {@code [-1, 1]}.
         */
        WeronStableRandomGenerator(UniformRandomProvider rng, double alpha, double beta) {
            super(rng);

            eps = 1 - alpha;
            // When alpha < 0.5, 1 - eps == alpha is not always true as the reverse is not exact.
            // Here we store 1 - eps in place of alpha. Thus eps + (1 - eps) = 1.
            meps1 = 1 - eps;

            // Compute pre-factors for the Weron formula used during error correction.
            if (meps1 > 1) {
                // Avoid calling tan outside the domain limit [-pi/2, pi/2].
                zeta = beta * Math.tan((2 - meps1) * PI_2);
            } else {
                zeta = -beta * Math.tan(meps1 * PI_2);
            }

            // Do not store xi = Math.atan(-zeta) / meps1 due to floating-point division errors.
            // Directly store Math.atan(-zeta).
            atanZeta = Math.atan(-zeta);
            scale = Math.pow(1 + zeta * zeta, 0.5 / meps1);
            inv1mEps = 1.0 / meps1;
            epsDiv1mEps = inv1mEps - 1;

            // Compute the support. This applies when alpha < 1 and beta = +/-1
            if (alpha < 1 && Math.abs(beta) == 1) {
                if (beta == 1) {
                    // alpha < 0, beta = 1
                    lower = zeta;
                    upper = UPPER;
                } else {
                    // alpha < 0, beta = -1
                    lower = LOWER;
                    upper = zeta;
                }
            } else {
                lower = LOWER;
                upper = UPPER;
            }
        }

        @Override
        public double sample() {
            final double w = getOmega();
            final double phi = getPhi();

            // Add back zeta
            // This creates the parameterization defined in Nolan (1998):
            // X ~ S0_alpha(s,beta,u0) with s=1, u0=0 for a standard random variable.

            double t1 = Math.sin(meps1 * phi + atanZeta) / Math.pow(Math.cos(phi), inv1mEps);
            double t2 = Math.pow(Math.cos(eps * phi - atanZeta) / w, epsDiv1mEps);

            // Term t1 and t2 can be zero or infinite.
            // Used a boxed infinity to avoid inf * 0.
            double unbox1 = 1;
            double unbox2 = 1;
            if (Double.isInfinite(t1)) {
                t1 = Math.copySign(Double.MAX_VALUE, t1);
                unbox1 = unbox2 = Double.MAX_VALUE;
            }
            if (Double.isInfinite(t2)) {
                t2 = Math.copySign(Double.MAX_VALUE, t2);
                unbox1 = unbox2 = Double.MAX_VALUE;
            }
            // Note: The order of the product must be maintained to unbox the infinity
            return clipToSupport(t1 * t2 * unbox1 * unbox2 * scale + zeta, lower, upper);
        }
    }

    /**
     * Implement the {@code alpha == 1} and {@code beta != 0} stable distribution
     * case.
     *
     * <p>Implements the Weron formula.
     */
    private static class Alpha1WeronStableRandomGenerator extends StableRandomGenerator {
        /** Skewness parameter. */
        private final double beta;

        /**
         * @param rng Underlying source of randomness
         * @param beta Skewness parameter. Must be in range {@code [-1, 1]}.
         */
        Alpha1WeronStableRandomGenerator(UniformRandomProvider rng, double beta) {
            super(rng);
            this.beta = beta;
        }

        @Override
        public double sample() {
            final double w = getOmega();
            final double phi = getPhi();
            // Generic stable distribution with alpha = 1.
            // Note: betaPhi cannot be zero when phi is limited to < pi/2.
            // This eliminates divide by 0 errors.
            final double betaPhi = PI_2 + beta * phi;
            final double x = (betaPhi * Math.tan(phi) -
                    beta * Math.log(PI_2 * w * Math.cos(phi) / betaPhi)) / PI_2;
            // When w -> 0 this computes +/- infinity.
            return x;
        }
    }

    /**
     * Implement the {@code alpha < 2} and {@code beta = 0} stable distribution case.
     *
     * <p>Implements the Weron formula.
     */
    private static class Beta0WeronStableRandomGenerator extends StableRandomGenerator {
        /** Epsilon (1 - alpha). */
        protected final double eps;
        /** Epsilon (1 - alpha). */
        protected final double meps1;
        /** 1 / alpha = 1 / (1 - eps). */
        protected final double inv1mEps;
        /** (1 / alpha) - 1 = eps / (1 - eps). */
        protected final double epsDiv1mEps;

        /**
         * @param rng Underlying source of randomness
         * @param alpha Stability parameter. Must be in range {@code (0, 2]}.
         */
        Beta0WeronStableRandomGenerator(UniformRandomProvider rng, double alpha) {
            super(rng);
            eps = 1 - alpha;
            meps1 = 1 - eps;
            inv1mEps = 1.0 / meps1;
            epsDiv1mEps = inv1mEps - 1;
        }

        @Override
        public double sample() {
            final double w = getOmega();
            final double phi = getPhi();
            // Compute terms
            double t1 = Math.sin(meps1 * phi) / Math.pow(Math.cos(phi), inv1mEps);
            double t2 = Math.pow(Math.cos(eps * phi) / w, epsDiv1mEps);

            double unbox1 = 1;
            double unbox2 = 1;
            if (Double.isInfinite(t1)) {
                t1 = Math.copySign(Double.MAX_VALUE, t1);
                unbox1 = unbox2 = Double.MAX_VALUE;
            }
            if (Double.isInfinite(t2)) {
                t2 = Math.copySign(Double.MAX_VALUE, t2);
                unbox1 = unbox2 = Double.MAX_VALUE;
            }
            // Note: The order of the product must be maintained to unbox the infinity
            return t1 * t2 * unbox1 * unbox2;
        }
    }

    /**
     * Implement the generic stable distribution case: {@code alpha < 2} and
     * {@code beta != 0}. This routine assumes {@code alpha != 1}.
     *
     * <p>Implements the CMS formula using the RSTAB algorithm.
     */
    static class CMSStableRandomGenerator extends WeronStableRandomGenerator {
        /** 1/2. */
        private static final double HALF = 0.5;
        /** Cache of expression value used in generation. */
        private final double tau;

        /**
         * @param rng Underlying source of randomness
         * @param alpha Stability parameter. Must be in the interval {@code (0, 2]}.
         * @param beta Skewness parameter. Must be in the interval {@code [-1, 1]}.
         */
        CMSStableRandomGenerator(UniformRandomProvider rng, double alpha, double beta) {
            super(rng, alpha, beta);

            // Compute the RSTAB pre-factors.
            // tau = -eps * tan(alpha * Phi0)
            // with Phi0 = alpha^-1 * arctan(beta * tan(pi alpha / 2)).

            // tau is symmetric around alpha=1
            // tau -> beta / pi/2 as alpha -> 1
            // tau -> 0 as alpha -> 2 or 0
            // Avoid calling tan as the value approaches the domain limit [-pi/2, pi/2].
            if (eps == 0) {
                // alpha == 1
                tau = beta / PI_2;
            } else if (Math.abs(eps) < HALF) {
                // 0.5 < alpha < 1.5
                tau = eps * beta / Math.tan(eps * PI_2);
            } else {
                // alpha >= 1.5 or alpha <= 0.5.
                // Do not call tan with alpha > 1 as it wraps in the domain [-pi/2, pi/2].
                // Since pi is approximate the symmetry is lost by wrapping.
                // Keep within the domain using (2-alpha).
                if (meps1 > 1) {
                    tau = eps * beta * -Math.tan((2 - meps1) * PI_2);
                } else {
                    tau = eps * beta * Math.tan(meps1 * PI_2);
                }
            }

        }

        @Override
        public double sample() {
            final double phiby2 = getPhiBy2();
            final double w = getOmega();

            // Compute as per the RSTAB routine.

            // Generic stable distribution that is continuous as alpha -> 1.
            // This is a trigonomic rearrangement of equation 4.1 from Chambers et al (1976)
            // as implemented in the Fortran program RSTAB.
            // Uses the special functions:
            // tan2 = tan(x) / x
            // d2 = (exp(x) - 1) / x
            // The method is implemented as per the RSTAB routine but using Math.tan
            // for the tan2 function

            // Compute some tangents
            final double a = Math.tan(phiby2);
            final double b = Math.tan(eps * phiby2);
            final double bb = b == 0 ? 1 : b / (eps * phiby2);

            // Compute some necessary subexpressions
            final double da = a * a;
            final double db = b * b;
            final double a2 = 1 - da;
            final double a2p = 1 + da;
            final double b2 = 1 - db;
            final double b2p = 1 + db;

            // Compute coefficient.
            final double numerator = b2 + 2 * phiby2 * bb * tau;
            final double z = a2p * numerator / (w * a2 * b2p);

            // Compute the exponential-type expression
            final double alogz = Math.log(z);
            final double d = D2Source.d2(epsDiv1mEps * alogz) * (alogz * inv1mEps);

            // Pre-compute the multiplication factor.
            final double f = (2 * ((a - b) * (1 + a * b) - phiby2 * tau * bb * (b * a2 - 2 * a))) /
                    (a2 * b2p);

            // Compute the stable deviate:
            final double x = (1 + eps * d) * f + tau * d;

            // Test the support
            if (lower < x && x < upper) {
                return x;
            }

            // No error correction path here!
            // Return something so that the test for the support is not optimised away.
            return 0;
        }
    }

    /**
     * Implement the stable distribution case: {@code alpha == 1} and {@code beta != 0}.
     *
     * <p>Implements the same algorithm as the {@link CMSStableRandomGenerator} with
     * the {@code alpha} assumed to be 1.
     *
     * <p>This routine assumes {@code beta != 0}; {@code alpha=1, beta=0} is the Cauchy
     * distribution case.
     */
    static class Alpha1CMSStableRandomGenerator extends StableRandomGenerator {
        /** Cache of expression value used in generation. */
        private final double tau;

        /**
         * @param rng Underlying source of randomness
         * @param beta Skewness parameter. Must be in the interval {@code [-1, 1]}.
         */
        Alpha1CMSStableRandomGenerator(UniformRandomProvider rng, double beta) {
            super(rng);
            tau = beta / PI_2;
        }

        @Override
        public double sample() {
            final double phiby2 = getPhiBy2();
            final double w = getOmega();

            // Compute some tangents
            final double a = Math.tan(phiby2);

            // Compute some necessary subexpressions
            final double da = a * a;
            final double a2 = 1 - da;
            final double a2p = 1 + da;

            // Compute coefficient.
            final double z = a2p * (1 + 2 * phiby2 * tau) / (w * a2);

            // Compute the exponential-type expression
            final double d = Math.log(z);

            // Pre-compute the multiplication factor.
            final double f = (2 * (a - phiby2 * tau * (-2 * a))) / a2;

            // Compute the stable deviate:
            return f + tau * d;
        }
    }

    /**
     * Implement the generic stable distribution case: {@code alpha < 2} and {@code beta == 0}.
     *
     * <p>Implements the same algorithm as the {@link CMSStableRandomGenerator} with
     * the {@code beta} assumed to be 0.
     *
     * <p>This routine assumes {@code alpha != 1}; {@code alpha=1, beta=0} is the Cauchy
     * distribution case.
     */
    static class Beta0CMSStableRandomGenerator extends Beta0WeronStableRandomGenerator {
        /**
         * @param rng Underlying source of randomness
         * @param alpha Stability parameter. Must be in the interval {@code (0, 2]}.
         */
        Beta0CMSStableRandomGenerator(UniformRandomProvider rng, double alpha) {
            super(rng, alpha);
        }

        @Override
        public double sample() {
            final double phiby2 = getPhiBy2();
            final double w = getOmega();

            // Compute some tangents
            final double a = Math.tan(phiby2);
            final double b = Math.tan(eps * phiby2);
            // Compute some necessary subexpressions
            final double da = a * a;
            final double db = b * b;
            final double a2 = 1 - da;
            final double a2p = 1 + da;
            final double b2 = 1 - db;
            final double b2p = 1 + db;
            // Compute coefficient.
            final double z = a2p * b2 / (w * a2 * b2p);

            // Compute the exponential-type expression
            final double alogz = Math.log(z);
            final double d = D2Source.d2(epsDiv1mEps * alogz) * (alogz * inv1mEps);

            // Pre-compute the multiplication factor.
            final double f = (2 * ((a - b) * (1 + a * b))) / (a2 * b2p);

            // Compute the stable deviate:
            final double x = (1 + eps * d) * f;

            // Test the support
            if (LOWER < x && x < UPPER) {
                return x;
            }

            // No error correction path here!
            // Return something so that the test for the support is not optimised away.
            return 0;
        }
    }

    /**
     * Defines the {@link RandomSource} for testing a {@link ContinuousSampler}.
     */
    public abstract static class SamplerSource {
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
         * Gets the source of randomness.
         *
         * @return RNG
         */
        public UniformRandomProvider getRNG() {
            return RandomSource.valueOf(randomSourceName).create();
        }

        /**
         * @return the sampler.
         */
        public abstract ContinuousSampler getSampler();
    }

    /**
     * Source for a uniform random deviate in an open interval, e.g. {@code (-0.5, 0.5)}.
     */
    @State(Scope.Benchmark)
    public static class UniformRandomSource extends SamplerSource {
        /** The lower limit of (-pi/4, pi/4). */
        private static final double NEG_PI_4 = -Math.PI / 4;
        /** pi/4 scaled by 2^-53. */
        private static final double PI_4_SCALED = 0x1.0p-55 * Math.PI;

        /** Method to generate the uniform deviate. */
        @Param({BASELINE, "nextDoubleNot0", "nextDoubleNot0Recurse",
                "nextLongNot0", "nextDoubleShifted", "nextLongShifted",
                "signedShift", "signedShiftPi4", "signedShiftPi4b"})
        private String method;

        /** The sampler. */
        private ContinuousSampler sampler;

        /** {@inheritDoc} */
        @Override
        public ContinuousSampler getSampler() {
            return sampler;
        }

        /** Instantiates sampler. */
        @Setup
        public void setup() {
            final UniformRandomProvider rng = getRNG();
            if (BASELINE.equals(method)) {
                sampler = rng::nextDouble;
            } else if ("nextDoubleNot0".equals(method)) {
                sampler = () -> {
                    // Sample the 2^53 dyadic rationals in [0, 1) with zero excluded
                    double x;
                    do {
                        x = rng.nextDouble();
                    } while (x == 0);
                    return x - 0.5;
                };
            } else if ("nextDoubleNot0Recurse".equals(method)) {
                sampler = new ContinuousSampler() {
                    @Override
                    public double sample() {
                        // Sample the 2^53 dyadic rationals in [0, 1) with zero excluded.
                        // Use recursion to generate a stack overflow with a bad provider.
                        // This is better than an infinite loop.
                        final double x = rng.nextDouble();
                        if (x == 0) {
                            return sample();
                        }
                        return x - 0.5;
                    }
                };
            } else if ("nextLongNot0".equals(method)) {
                sampler = () -> {
                    // Sample the 2^53 dyadic rationals in [0, 1) with zero excluded
                    long x;
                    do {
                        x = rng.nextLong() >>> 11;
                    } while (x == 0);
                    return x * 0x1.0p-53 - 0.5;
                };
            } else if ("nextDoubleShifted".equals(method)) {
                sampler = () -> {
                    // Sample the 2^52 dyadic rationals in [0, 1) and shift by 2^-53.
                    // No infinite loop but the deviate loses 1 bit of randomness.
                    return 0x1.0p-53 + (rng.nextLong() >>> 12) * 0x1.0p-52 - 0.5;
                };
            } else if ("nextLongShifted".equals(method)) {
                sampler = () -> {
                    // Sample the 2^53 dyadic rationals in [0, 1) but set the lowest
                    // bit. This result in 2^52 dyadic rationals in (0, 1) to avoid 0.
                    return ((rng.nextLong() >>> 11) | 0x1L) * 0x1.0p-53 - 0.5;
                };
            } else if ("signedShift".equals(method)) {
                sampler = new ContinuousSampler() {
                    @Override
                    public double sample() {
                        // As per o.a.c.rng.core.utils.NumberFactory.makeDouble(long) but using a
                        // signed shift of 10 in place of an unsigned shift of 11, and updating the
                        // multiplication factor to 2^-54.
                        final double x = (rng.nextLong() >> 10) * 0x1.0p-54;
                        if (x == -0.5) {
                            // Avoid the extreme of the bounds
                            return sample();
                        }
                        return x;
                    }
                };
            } else if ("signedShiftPi4".equals(method)) {
                // Note: This does generate u in (-0.5, 0.5) which must be scaled by pi
                // or pi/2 for the CMS algorithm but directly generates in (-pi/4, pi/4).
                sampler = new ContinuousSampler() {
                    @Override
                    public double sample() {
                        // As per o.a.c.rng.core.utils.NumberFactory.makeDouble(long) but using a
                        // signed shift of 10 in place of an unsigned shift of 11, and updating the
                        // multiplication factor to 2^-54 * pi/2
                        final double x = (rng.nextLong() >> 10) * PI_4_SCALED;
                        if (x == NEG_PI_4) {
                            // Avoid the extreme of the bounds
                            return sample();
                        }
                        return x;
                    }
                };
            } else if ("signedShiftPi4b".equals(method)) {
                // Note: This does generate u in (-0.5, 0.5) which must be scaled by pi
                // or pi/2 for the CMS algorithm but directly generates in (-pi/4, pi/4).
                sampler = new ContinuousSampler() {
                    @Override
                    public double sample() {
                        final long x = rng.nextLong();
                        if (x == Long.MIN_VALUE) {
                            // Avoid the extreme of the bounds
                            return sample();
                        }
                        // As per o.a.c.rng.core.utils.NumberFactory.makeDouble(long) but using a
                        // signed shift of 10 in place of an unsigned shift of 11, and updating the
                        // multiplication factor to 2^-54 * pi/2
                        return (x >> 10) * PI_4_SCALED;
                    }
                };
            } else {
                throw new IllegalStateException("Unknown method: " + method);
            }
        }
    }

    /**
     * Source for testing implementations of tan(x) / x. The function must work on a value
     * in the range {@code [0, pi/4]}.
     *
     * <p>The tan(x) / x function is required for the trigonomic rearrangement of the CMS
     * formula to create a stable random variate.
     */
    @State(Scope.Benchmark)
    public static class TanSource extends SamplerSource {
        /** pi / 2. */
        private static final double PI_2 = Math.PI / 2;
        /** pi / 4. */
        private static final double PI_4 = Math.PI / 4;
        /** 4 / pi. */
        private static final double FOUR_PI = 4 / Math.PI;

        /** Method to generate tan(x) / x. */
        @Param({BASELINE, "tan", "tan4283", "tan4288", "tan4288b", "tan4288c"})
        private String method;

        /** The sampler. */
        private ContinuousSampler sampler;

        /** {@inheritDoc} */
        @Override
        public ContinuousSampler getSampler() {
            return sampler;
        }

        /** Instantiates sampler. */
        @Setup
        public void setup() {
            final UniformRandomProvider rng = getRNG();
            if (BASELINE.equals(method)) {
                sampler = () -> {
                    // A value in [-pi/4, pi/4]
                    return PI_2 * (rng.nextDouble() - 0.5);
                };
            } else if ("tan".equals(method)) {
                sampler = () -> {
                    final double x = PI_2 * (rng.nextDouble() - 0.5);
                    // Require tan(0) / 0 = 1 and not NaN
                    return x == 0 ? 1.0 : Math.tan(x) / x;
                };
            } else if ("tan4283".equals(method)) {
                sampler = () -> {
                    final double x = PI_2 * (rng.nextDouble() - 0.5);
                    return x * tan4283(x);
                };
            } else if ("tan4288".equals(method)) {
                sampler = () -> {
                    final double x = PI_2 * (rng.nextDouble() - 0.5);
                    return x * tan4288(x);
                };
            } else if ("tan4288b".equals(method)) {
                sampler = () -> {
                    final double x = PI_2 * (rng.nextDouble() - 0.5);
                    return x * tan4288b(x);
                };
            } else if ("tan4288c".equals(method)) {
                sampler = () -> {
                    final double x = PI_2 * (rng.nextDouble() - 0.5);
                    return x * tan4288c(x);
                };
            } else {
                throw new IllegalStateException("Unknown tan method: " + method);
            }
        }

        // Mean (max) ULP is very low
        // 2^30 random samples in [0, pi / 4)
        //          tan(x)                    tan(x) / x
        // tan4283  93436.25534446817         201185 : 68313.16171793547     128079
        // tan4288      0.5898815048858523         4 : 0.40416045393794775        3
        // tan4288b     0.8608690425753593         8 : 0.6117749745026231         5
        // tan4288c     0.5905972588807344         4 : 0.4047176940366626         3

        /**
         * Evaluate {@code tan(x) / x}.
         *
         * <p>For {@code x} in the range {@code [0, pi/4]} this ideally should return
         * a value in the range {@code [1, 4 / pi]}. However due to lack of precision
         * this is not the case for the extremes of the interval. It is not recommended
         * to use this function in the CMS algorithm when computing the routine with
         * double precision. The original RSTAB routine is for single precision floats.
         *
         * <p>Uses tan 4283 from Hart, JF et al (1968) Computer Approximations,
         * New York: John Wiley &amp; Sons, Inc.
         *
         * <p>This is the original method used in the RSTAB function by Chambers et al (1976).
         * It has been updated to use the full precision constants provided in Hart.
         *
         * @param x the x
         * @return {@code tan(x) / x}.
         */
        static double tan4283(double x) {
            double xa = Math.abs(x);
            if (xa > PI_4) {
                return Math.tan(x) / x;
            }
            // Approximation 4283 from Hart et al (1968, P. 251).
            // Max relative error = 1e-10.66.
            // When testing verses Math.tan(x) the mean ULP difference was 93436.3
            final double p0 = 0.129221035031569917e3;
            final double p1 = -0.8876623770211723e1;
            final double p2 = 0.52864445522248e-1;
            final double q0 = 0.164529331810168605e3;
            final double q1 = -0.45132056100598961e2;
            // q2 = 1.0

            xa = xa / PI_4;
            final double xx = xa * xa;

            // Polynomial implemented as per Chambers (1976) using the nested form.
            return (p0 + xx * (p1 + xx * p2)) / (PI_4 * (q0 + xx * (q1 + xx /* * q2 */)));
        }

        /**
         * Evaluate {@code tan(x) / x}.
         *
         * <p>For {@code x} in the range {@code [0, pi/4]} this returns
         * a value in the range {@code [1, 4 / pi]}.
         *
         * <p>Uses tan 4288 from Hart, JF et al (1968) Computer Approximations,
         * New York: John Wiley &amp; Sons, Inc.
         *
         * <p>This is a higher precision method provided in Hart.
         * It has the properties that {@code tan(x) / x = 1, x = 0} and
         * {@code tan(x) = 1, x = pi / 4}.
         *
         * @param x the x
         * @return {@code tan(x) / x}.
         */
        static double tan4288(double x) {
            double xa = Math.abs(x);
            if (xa > PI_4) {
                return Math.tan(x) / x;
            }

            // Approximation 4288 from Hart et al (1968, P. 252).
            // Max relative error = 1e-26.68 (for tan(x))
            // When testing verses Math.tan(x) the mean ULP difference was 0.589882
            final double p0 = -0.5712939549476836914932149599e+10;
            final double p1 = +0.4946855977542506692946040594e+9;
            final double p2 = -0.9429037070546336747758930844e+7;
            final double p3 = +0.5282725819868891894772108334e+5;
            final double p4 = -0.6983913274721550913090621370e+2;

            final double q0 = -0.7273940551075393257142652672e+10;
            final double q1 = +0.2125497341858248436051062591e+10;
            final double q2 = -0.8000791217568674135274814656e+8;
            final double q3 = +0.8232855955751828560307269007e+6;
            final double q4 = -0.2396576810261093558391373322e+4;
            // q5 = 1.0

            xa = xa * FOUR_PI;
            final double xx = xa * xa;

            // Polynomial implemented as per Chambers (1976) using the nested form.
            return         (p0 + xx * (p1 + xx * (p2 + xx * (p3 + xx * p4)))) /
                   (PI_4 * (q0 + xx * (q1 + xx * (q2 + xx * (q3 + xx * (q4 + xx /* * q5 */))))));
        }

        /**
         * Evaluate {@code tan(x) / x}.
         *
         * <p>For {@code x} in the range {@code [0, pi/4]} this returns
         * a value in the range {@code [1, 4 / pi]}.
         *
         * <p>Uses tan 4288 from Hart, JF et al (1968) Computer Approximations,
         * New York: John Wiley &amp; Sons, Inc.
         *
         * @param x the x
         * @return {@code tan(x) / x}.
         */
        static double tan4288b(double x) {
            double xa = Math.abs(x);
            if (xa > PI_4) {
                return Math.tan(x) / x;
            }

            final double p0 = -0.5712939549476836914932149599e+10;
            final double p1 = +0.4946855977542506692946040594e+9;
            final double p2 = -0.9429037070546336747758930844e+7;
            final double p3 = +0.5282725819868891894772108334e+5;
            final double p4 = -0.6983913274721550913090621370e+2;

            final double q0 = -0.7273940551075393257142652672e+10;
            final double q1 = +0.2125497341858248436051062591e+10;
            final double q2 = -0.8000791217568674135274814656e+8;
            final double q3 = +0.8232855955751828560307269007e+6;
            final double q4 = -0.2396576810261093558391373322e+4;
            // q5 = 1.0

            xa = xa * FOUR_PI;
            // Rearrange the polynomial to the power form.
            // Allows parallel computation of terms.
            // This is faster but has been tested to have lower accuracy verses Math.tan.
            // When testing verses Math.tan(x) the mean ULP difference was 0.860869
            final double x2 = xa * xa;
            final double x4 = x2 * x2;
            final double x6 = x4 * x2;
            final double x8 = x4 * x4;

            return  (p0 + x2 * p1 + x4 * p2 + x6 * p3 + x8 * p4) /
                    (PI_4 * (q0 + x2 * q1 + x4 * q2 + x6 * q3 + x8 * q4 + x8 * x2));
        }

        /**
         * Evaluate {@code tan(x) / x}.
         *
         * <p>For {@code x} in the range {@code [0, pi/4]} this returns
         * a value in the range {@code [1, 4 / pi]}.
         *
         * <p>Uses tan 4288 from Hart, JF et al (1968) Computer Approximations,
         * New York: John Wiley &amp; Sons, Inc.
         *
         * @param x the x
         * @return {@code tan(x) / x}.
         */
        static double tan4288c(double x) {
            if (Math.abs(x) > PI_4) {
                return Math.tan(x) / x;
            }

            final double p0 = -0.5712939549476836914932149599e+10;
            final double p1 = +0.4946855977542506692946040594e+9;
            final double p2 = -0.9429037070546336747758930844e+7;
            final double p3 = +0.5282725819868891894772108334e+5;
            final double p4 = -0.6983913274721550913090621370e+2;

            final double q0 = -0.7273940551075393257142652672e+10;
            final double q1 = +0.2125497341858248436051062591e+10;
            final double q2 = -0.8000791217568674135274814656e+8;
            final double q3 = +0.8232855955751828560307269007e+6;
            final double q4 = -0.2396576810261093558391373322e+4;
            // q5 = 1.0

            final double xi = x * FOUR_PI;
            // Rearrange the polynomial to the power form.
            // Allows parallel computation of terms.
            // This is faster and has been tested to have accuracy similar to the nested form.
            // When testing verses Math.tan(x) the mean ULP difference was 0.590597
            final double x2 = xi * xi;
            final double x4 = x2 * x2;
            final double x6 = x4 * x2;
            final double x8 = x4 * x4;

            // Reverse summation order to have least significant terms first.
            return  (x8 * p4 + x6 * p3 + x4 * p2 + x2 * p1 + p0) /
                    (PI_4 * (x8 * x2 + x8 * q4 + x6 * q3 + x4 * q2 + x2 * q1 + q0));
        }
    }

    /**
     * Source for testing implementations of {@code (exp(x) - 1) / x}. The function must
     * work on a value in the range {@code [-inf, +inf]}.
     *
     * <p>The d2 function is required for the trigonomic rearrangement of the CMS
     * formula to create a stable random variate.
     *
     * <p>For testing the value x is generated as a uniform deviate with a range around
     * zero ({@code [-scale/2, +scale/2]}) using a configurable scale parameter.
     */
    @State(Scope.Benchmark)
    public static class D2Source extends SamplerSource {
        /** Method to generate (exp(x) - 1) / x. */
        @Param({BASELINE, "expm1", "expm1b", "exp", "hybrid"})
        private String method;
        /** Scale for the random value x. */
        @Param({"0.12345", "1.2345", "12.345", "123.45"})
        private double scale;

        /** The sampler. */
        private ContinuousSampler sampler;

        /** {@inheritDoc} */
        @Override
        public ContinuousSampler getSampler() {
            return sampler;
        }

        /** Instantiates sampler. */
        @Setup
        public void setup() {
            final double s = scale;
            final UniformRandomProvider rng = getRNG();
            if (BASELINE.equals(method)) {
                sampler = () -> s * (rng.nextDouble() - 0.5);
            } else if ("expm1".equals(method)) {
                sampler = () -> expm1(s * (rng.nextDouble() - 0.5));
            } else if ("expm1b".equals(method)) {
                sampler = () -> expm1b(s * (rng.nextDouble() - 0.5));
            } else if ("exp".equals(method)) {
                sampler = () -> exp(s * (rng.nextDouble() - 0.5));
            } else if ("hybrid".equals(method)) {
                sampler = () -> hybrid(s * (rng.nextDouble() - 0.5));
            } else {
                throw new IllegalStateException("Unknown d2 method: " + method);
            }
        }

        /**
         * Evaluate {@code (exp(x) - 1) / x}. For {@code x} in the range {@code [-inf, inf]} returns
         * a result in {@code [0, inf]}.
         *
         * <ul>
         * <li>For {@code x=-inf} this returns {@code 0}.
         * <li>For {@code x=0} this returns {@code 1}.
         * <li>For {@code x=inf} this returns {@code inf}.
         * </ul>
         *
         * <p> This corrects {@code 0 / 0} and {@code inf / inf} division from
         * {@code NaN} to either {@code 1} or the upper bound respectively.
         *
         * @param x value to evaluate
         * @return {@code (exp(x) - 1) / x}.
         */
        static double d2(double x) {
            return hybrid(x);
        }

        /**
         * Evaluate {@code (exp(x) - 1) / x}.
         *
         * @param x value to evaluate
         * @return {@code (exp(x) - 1) / x}.
         */
        static double expm1(double x) {
            // Here we use a conditional to detect both edge cases, which are then corrected.
            final double d2 = Math.expm1(x) / x;
            if (Double.isNaN(d2)) {
                // Correct edge cases.
                if (x == 0) {
                    return 1.0;
                }
                // x must have been +infinite or NaN
                return x;
            }
            return d2;
        }

        /**
         * Evaluate {@code (exp(x) - 1) / x}.
         *
         * @param x value to evaluate
         * @return {@code (exp(x) - 1) / x}.
         */
        static double expm1b(double x) {
            // Edge cases
            if (x == 0) {
                return 1;
            }
            if (x == Double.POSITIVE_INFINITY) {
                return x;
            }
            return Math.expm1(x) / x;
        }

        /**
         * Evaluate {@code (exp(x) - 1) / x}.
         *
         * @param x value to evaluate
         * @return {@code (exp(x) - 1) / x}.
         */
        static double exp(double x) {
            // No use of expm1.
            // Here we use a conditional to detect both edge cases, which are then corrected.
            final double d2 = (Math.exp(x) - 1) / x;
            if (Double.isNaN(d2)) {
                // Correct edge cases.
                if (x == 0) {
                    return 1.0;
                }
                // x must have been +infinite or NaN
                return x;
            }
            return d2;
        }

        /**
         * Evaluate {@code (exp(x) - 1) / x}.
         *
         * @param x value to evaluate
         * @return {@code (exp(x) - 1) / x}.
         */
        static double hybrid(double x) {
            // This is the threshold where use of expm1 and exp consistently
            // compute different results by more than 0.5 ULP.
            if (Math.abs(x) < 0.5) {
                if (x == 0) {
                    return 1;
                }
                return Math.expm1(x) / x;
            }
            // No use of expm1. Accuracy as x moves away from 0 is not required as the result
            // is divided by x and the accuracy of the final result is the same.
            if (x == Double.POSITIVE_INFINITY) {
                return x;
            }
            return (Math.exp(x) - 1) / x;
        }
    }

    /**
     * Baseline with an exponential deviate and a uniform deviate.
     */
    @State(Scope.Benchmark)
    public static class BaselineSamplerSource extends SamplerSource {
        /** The sampler. */
        private ContinuousSampler sampler;

        /** {@inheritDoc} */
        @Override
        public ContinuousSampler getSampler() {
            return sampler;
        }

        /** Instantiates sampler. */
        @Setup
        public void setup() {
            sampler = new BaselineStableRandomGenerator(getRNG());
        }
    }

    /**
     * The sampler to use for testing. Defines the RandomSource and the type of
     * stable distribution sampler.
     */
    public abstract static class StableSamplerSource extends SamplerSource {
        /** The sampler type. */
        @Param({"CMS", "CMS_weron", "CMS_tan"})
        private String samplerType;

        /** The sampler. */
        private ContinuousSampler sampler;

        /**
         * The alpha value.
         *
         * @return alpha
         */
        abstract double getAlpha();

        /**
         * The beta value.
         *
         * @return beta
         */
        abstract double getBeta();

        /** {@inheritDoc} */
        @Override
        public ContinuousSampler getSampler() {
            return sampler;
        }

        /** Instantiates sampler. */
        @Setup
        public void setup() {
            final UniformRandomProvider rng = getRNG();
            if ("CMS".equals(samplerType)) {
                sampler = StableSampler.of(rng, getAlpha(), getBeta());
            } else if ("CMS_weron".equals(samplerType)) {
                sampler = StableRandomGenerator.of(rng, getAlpha(), getBeta(), true);
            } else if ("CMS_tan".equals(samplerType)) {
                sampler = StableRandomGenerator.of(rng, getAlpha(), getBeta(), false);
            } else {
                throw new IllegalStateException("Unknown sampler: " + samplerType);
            }
        }
    }

    /**
     * Sampling with {@code alpha = 1} and {@code beta != 0}.
     */
    @State(Scope.Benchmark)
    public static class Alpha1StableSamplerSource extends StableSamplerSource {
        /** The beta value. */
        @Param({"0.1", "0.4", "0.7"})
        private double beta;

        /** {@inheritDoc} */
        @Override
        double getAlpha() {
            return 1;
        }

        /** {@inheritDoc} */
        @Override
        double getBeta() {
            return beta;
        }
    }

    /**
     * Sampling with {@code alpha != 1} and {@code beta = 0}.
     */
    @State(Scope.Benchmark)
    public static class Beta0StableSamplerSource extends StableSamplerSource {
        /** The alpha value. Use a range around 1. */
        @Param({
            //"0.2", "0.3", "0.4", "0.5", "0.6", "0.7", "0.8", "0.9", "0.99", "1.1", "1.2", "1.3", "1.4", "1.5"
            // The Weron formula is fast at alpha=0.5; possibly due to the two power
            // functions using a power of 1 and 2 respectively.
            //"0.45", "0.48", "0.5", "0.52", "0.55"
            "0.3", "0.5", "0.7", "0.9", "1.1", "1.3", "1.5", "1.7"
            })
        private double alpha;

        /** {@inheritDoc} */
        @Override
        double getAlpha() {
            return alpha;
        }

        /** {@inheritDoc} */
        @Override
        double getBeta() {
            return 0;
        }
    }

    /**
     * Sampling with {@code alpha != 1} and {@code beta != 0}.
     */
    @State(Scope.Benchmark)
    public static class GeneralStableSamplerSource extends Beta0StableSamplerSource {
        /** The beta value. */
        @Param({"0.1", "0.4", "0.7"})
        private double beta;

        /** {@inheritDoc} */
        @Override
        double getBeta() {
            return beta;
        }
    }

    /**
     * Test methods for producing a uniform deviate in an open interval, e.g. {@code (-0.5, 0.5)}.
     * This is a requirement of the stable distribution CMS algorithm.
     *
     * @param source Source of randomness.
     * @return the {@code double} value
     */
    @Benchmark
    public double nextUniformDeviate(UniformRandomSource source) {
        return source.getSampler().sample();
    }

    /**
     * Test methods for producing {@code tan(x) / x} in the range {@code [0, pi/4]}.
     * This is a requirement of the stable distribution CMS algorithm.
     *
     * @param source Source of randomness.
     * @return the {@code tan(x)} value
     */
    @Benchmark
    public double nextTan(TanSource source) {
        return source.getSampler().sample();
    }

    /**
     * Test methods for producing {@code (exp(x) - 1) / x}.
     * This is a requirement of the stable distribution CMS algorithm.
     *
     * @param source Source of randomness.
     * @return the {@code (exp(x) - 1) / x} value
     */
    @Benchmark
    public double nextD2(D2Source source) {
        return source.getSampler().sample();
    }

    /**
     * Baseline for any sampler that uses an exponential deviate and a uniform deviate.
     *
     * @param source Source of randomness.
     * @return the {@code double} value
     */
    @Benchmark
    public double sampleBaseline(BaselineSamplerSource source) {
        return source.getSampler().sample();
    }

    /**
     * Run the stable sampler with {@code alpha = 1}.
     *
     * @param source Source of randomness.
     * @return the sample value
     */
    @Benchmark
    public double sampleAlpha1(Alpha1StableSamplerSource source) {
        return source.getSampler().sample();
    }

    /**
     * Run the stable sampler with {@code beta = 0}.
     *
     * @param source Source of randomness.
     * @return the sample value
     */
    @Benchmark
    public double sampleBeta0(Beta0StableSamplerSource source) {
        return source.getSampler().sample();
    }

    /**
     * Run the stable sampler.
     *
     * @param source Source of randomness.
     * @return the sample value
     */
    @Benchmark
    public double sample(GeneralStableSamplerSource source) {
        return source.getSampler().sample();
    }
}
