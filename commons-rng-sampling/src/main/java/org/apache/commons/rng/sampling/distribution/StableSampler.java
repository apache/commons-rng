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
package org.apache.commons.rng.sampling.distribution;

import org.apache.commons.rng.UniformRandomProvider;

/**
 * Samples from a stable distribution.
 *
 * <p>Several different parameterizations exist for the stable distribution.
 * This sampler uses the 0-parameterization distribution described in Nolan (2020) "Univariate Stable
 * Distributions: Models for Heavy Tailed Data". Springer Series in Operations Research and
 * Financial Engineering. Springer. Sections 1.7 and 3.3.3.
 *
 * <p>The random variable \( X \) has
 * the stable distribution \( S(\alpha, \beta, \gamma, \delta; 0) \) if its characteristic
 * function is given by:
 *
 * <p>\[ E(e^{iuX}) = \begin{cases} \exp \left (- \gamma^\alpha |u|^\alpha \left [1 - i \beta (\tan \frac{\pi \alpha}{2})(\text{sgn}(u)) \right ] + i \delta u \right ) &amp; \alpha \neq 1 \\
 * \exp \left (- \gamma |u| \left [1 + i \beta \frac{2}{\pi} (\text{sgn}(u)) \log |u| \right ] + i \delta u \right ) &amp; \alpha = 1 \end{cases} \]
 *
 * <p>The function is continuous with respect to all the parameters; the parameters \( \alpha \)
 * and \( \beta \) determine the shape and the parameters \( \gamma \) and \( \delta \) determine
 * the scale and location. The support of the distribution is:
 *
 * <p>\[ \text{support} f(x|\alpha,\beta,\gamma,\delta; 0) = \begin{cases} [\delta - \gamma \tan \frac{\pi \alpha}{2}, \infty) &amp; \alpha \lt 1\ and\ \beta = 1 \\
 * (-\infty, \delta + \gamma \tan \frac{\pi \alpha}{2}] &amp; \alpha \lt 1\ and\ \beta = -1 \\
 * (-\infty, \infty) &amp; otherwise \end{cases} \]
 *
 * <p>The implementation uses the Chambers-Mallows-Stuck (CMS) method as described in:
 * <ul>
 *  <li>Chambers, Mallows &amp; Stuck (1976) "A Method for Simulating Stable Random Variables".
 *      Journal of the American Statistical Association. 71 (354): 340–344.
 *  <li>Weron (1996) "On the Chambers-Mallows-Stuck method for simulating skewed stable
 *      random variables". Statistics &amp; Probability Letters. 28 (2): 165–171.
 * </ul>
 *
 * @see <a href="https://en.wikipedia.org/wiki/Stable_distribution">Stable distribution (Wikipedia)</a>
 * @see <a href="https://link.springer.com/book/10.1007/978-3-030-52915-4">Nolan (2020) Univariate Stable Distributions</a>
 * @see <a href="https://doi.org/10.1080%2F01621459.1976.10480344">Chambers et al (1976) JOASA 71: 340-344</a>
 * @see <a href="https://doi.org/10.1016%2F0167-7152%2895%2900113-1">Weron (1996).
 * Statistics &amp; Probability Letters. 28 (2): 165–171.</a>
 * @since 1.4
 */
public abstract class StableSampler implements SharedStateContinuousSampler {
    /** pi / 2. */
    private static final double PI_2 = Math.PI / 2;
    /** The alpha value for the Gaussian case. */
    private static final double ALPHA_GAUSSIAN = 2;
    /** The alpha value for the Cauchy case. */
    private static final double ALPHA_CAUCHY = 1;
    /** The alpha value for the Levy case. */
    private static final double ALPHA_LEVY = 0.5;
    /** The alpha value for the {@code alpha -> 0} to switch to using the Weron formula.
     * Note that small alpha requires robust correction of infinite samples. */
    private static final double ALPHA_SMALL = 0.02;
    /** The beta value for the Levy case. */
    private static final double BETA_LEVY = 1.0;
    /** The gamma value for the normalized case. */
    private static final double GAMMA_1 = 1.0;
    /** The delta value for the normalized case. */
    private static final double DELTA_0 = 0.0;
    /** The tau value for zero. When tau is zero, this is effectively {@code beta = 0}. */
    private static final double TAU_ZERO = 0.0;
    /**
     * The lower support for the distribution.
     * This is the lower bound of {@code (-inf, +inf)}
     * If the sample is not within this bound ({@code lower < x}) then it is either
     * infinite or NaN and the result should be checked.
     */
    private static final double LOWER = Double.NEGATIVE_INFINITY;
    /**
     * The upper support for the distribution.
     * This is the upper bound of {@code (-inf, +inf)}.
     * If the sample is not within this bound ({@code x < upper}) then it is either
     * infinite or NaN and the result should be checked.
     */
    private static final double UPPER = Double.POSITIVE_INFINITY;

    /** Underlying source of randomness. */
    private final UniformRandomProvider rng;

    // Implementation notes
    //
    // The Chambers-Mallows-Stuck (CMS) method uses a uniform deviate u in (0, 1) and an
    // exponential deviate w to compute a stable deviate. Chambers et al (1976) published
    // a formula for alpha = 1 and alpha != 1. The function is discontinuous at alpha = 1
    // and to address this a trigonmoic rearrangement was provided using half angles that
    // is continuous with respect to alpha. The original discontinuous formulas were proven
    // in Weron (1996). The CMS rearrangement creates a deviate in the 0-parameterization
    // defined by Nolan (2020); the original discontinuous functions create a deviate in the
    // 1-parameterization defined by Nolan. A shift can be used to convert one parameterisation
    // to the other. The shift is the magnitude of the zeta term from the 1-parameterisation.
    // The following table shows how the zeta term -> inf when alpha -> 1 for
    // different beta (hence the discontinuity in the function):
    //
    // Zeta
    //             Beta
    // Alpha       1.0         0.5         0.25        0.1         0.0
    // 0.001       0.001571    0.0007854   0.0003927   0.0001571   0.0
    // 0.01        0.01571     0.007855    0.003927    0.001571    0.0
    // 0.05        0.07870     0.03935     0.01968     0.007870    0.0
    // 0.01        0.01571     0.007855    0.003927    0.001571    0.0
    // 0.1         0.1584      0.07919     0.03960     0.01584     0.0
    // 0.5         1.000       0.5000      0.2500      0.1000      0.0
    // 0.9         6.314       3.157       1.578       0.6314      0.0
    // 0.95        12.71       6.353       3.177       1.271       0.0
    // 0.99        63.66       31.83       15.91       6.366       0.0
    // 0.995       127.3       63.66       31.83       12.73       0.0
    // 0.999       636.6       318.3       159.2       63.66       0.0
    // 0.9995      1273        636.6       318.3       127.3       0.0
    // 0.9999      6366        3183        1592        636.6       0.0
    // 1.0         1.633E+16   8.166E+15   4.083E+15   1.633E+15   0.0
    //
    // For numerical simulation the 0-parameterization is favoured as it is continuous
    // with respect to all the parameters. When approaching alpha = 1 the large magnitude
    // of the zeta term used to shift the 1-parameterization results in cancellation and the
    // number of bits of the output sample is effected. This sampler uses the CMS method with
    // the continuous function as the base for the implementation. However it is not suitable
    // for all values of alpha and beta.
    //
    // The method computes a value log(z) with z in the interval (0, inf). When z is 0 or infinite
    // the computation can return invalid results. The open bound for the deviate u avoids
    // generating an extreme value that results in cancellation, z=0 and an invalid expression.
    // However due to floating point error this can occur
    // when u is close to 0 or 1, and beta is -1 or 1. Thus it is not enough to create
    // u by avoiding 0 or 1 and further checks are required.
    // The division by the deviate w also results in an invalid expression as the term z becomes
    // infinite as w -> 0. It should be noted that such events are extremely rare
    // (frequency in the 1 in 10^15), or will not occur at all depending on the parameters alpha
    // and beta.
    //
    // When alpha -> 0 then the distribution is extremely long tailed and the expression
    // using log(z) often computes infinity. Certain parameters can create NaN due to
    // 0 / 0, 0 * inf, or inf - inf. Thus the implementation must check the final result
    // and perform a correction if required, or generate another sample.
    // Correcting the original CMS formula has many edge cases depending on parameters. The
    // alternative formula provided by Weron is easier to correct when infinite values are
    // created. This correction is made easier by knowing that u is not 0 or 1 as certain
    // conditions on the intermediate terms can be eliminated. The implementation
    // thus generates u in the open interval (0,1) but leaves w unchecked and potentially 0.
    // The sample is generated and the result tested against the expected support. This detects
    // any NaN and infinite values. Incorrect samples due to the inability to compute log(z)
    // (extremely rare) and samples where alpha -> 0 has resulted in an infinite expression
    // for the value d are corrected using the Weron formula and returned within the support.
    //
    // The CMS algorithm is continuous for the parameters. However when alpha=1 or beta=0
    // many terms cancel and these cases are handled with specialised implementations.
    // The beta=0 case implements the same CMS algorithm with certain terms eliminated.
    // Correction uses the alternative Weron formula. When alpha=1 the CMS algorithm can
    // be corrected from infinite cases due to assumptions on the intermediate terms.
    //
    // The following table show the failure frequency (result not finite or, when beta=+/-1,
    // within the support) for the CMS algorithm computed using 2^30 random deviates.
    //
    // CMS failure rate
    //             Beta
    // Alpha       1.0         0.5         0.25        0.1         0.0
    // 1.999       0.0         0.0         0.0         0.0         0.0
    // 1.99        0.0         0.0         0.0         0.0         0.0
    // 1.9         0.0         0.0         0.0         0.0         0.0
    // 1.5         0.0         0.0         0.0         0.0         0.0
    // 1.1         0.0         0.0         0.0         0.0         0.0
    // 1.0         0.0         0.0         0.0         0.0         0.0
    // 0.9         0.0         0.0         0.0         0.0         0.0
    // 0.5         0.0         0.0         0.0         0.0         0.0
    // 0.25        0.0         0.0         0.0         0.0         0.0
    // 0.1         0.0         0.0         0.0         0.0         0.0
    // 0.05        0.0003458   0.0         0.0         0.0         0.0
    // 0.02        0.009028    6.938E-7    7.180E-7    7.320E-7    6.873E-7
    // 0.01        0.004878    0.0008555   0.0008553   0.0008554   0.0008570
    // 0.005       0.1519      0.02896     0.02897     0.02897     0.02897
    // 0.001       0.6038      0.3903      0.3903      0.3903      0.3903
    //
    // The sampler switches to using the error checked Weron implementation when alpha < 0.02.
    // Unit tests demonstrate the two samplers (CMS or Weron) product the same result within
    // a tolerance. The switch point is based on a consistent failure rate above 1 in a million.
    // At this point zeta is small and cancellation leading to loss of bits in the sample is
    // minimal.
    //
    // In common use the sampler will not have a measurable failure rate. The output will
    // be continuous as alpha -> 1 and beta -> 0. The evaluated function produces symmetric
    // samples when u and beta are mirrored around 0.5 and 0 respectively. To achieve this
    // the computation of certain parameters has been changed from the original implementation
    // to avoid evaluating Math.tan outside the interval (-pi/2, pi/2).
    //
    // Note: Chambers et al (1976) use an approximation to tan(x) / x in the RSTAB routine.
    // A JMH performance test is available in the RNG examples module comparing Math.tan
    // with various approximations. The functions are faster than Math.tan(x) / x.
    // This implementation uses a higher accuracy approximation than the original RSTAB
    // implementation; it has a mean ULP difference to Math.tan of less than 1 and has
    // a noticeable performance gain.

    /**
     * Base class for implementations of a stable distribution that requires an exponential
     * random deviate.
     */
    private abstract static class BaseStableSampler extends StableSampler {
        /** pi/2 scaled by 2^-53. */
        private static final double PI_2_SCALED = 0x1.0p-54 * Math.PI;
        /** pi/4 scaled by 2^-53. */
        private static final double PI_4_SCALED = 0x1.0p-55 * Math.PI;
        /** -pi / 2. */
        private static final double NEG_PI_2 = -Math.PI / 2;
        /** -pi / 4. */
        private static final double NEG_PI_4 = -Math.PI / 4;

        /** The exponential sampler. */
        private final ContinuousSampler expSampler;

        /**
         * @param rng Underlying source of randomness
         */
        BaseStableSampler(UniformRandomProvider rng) {
            super(rng);
            expSampler = ZigguratSampler.Exponential.of(rng);
        }

        /**
         * Gets a random value for the omega parameter ({@code w}).
         * This is an exponential random variable with mean 1.
         *
         * <p>Warning: For simplicity this does not check the variate is not 0.
         * The calling CMS algorithm should detect and handle incorrect samples as a result
         * of this unlikely edge case.
         *
         * @return omega
         */
        double getOmega() {
            // Note: Ideally this should not have a value of 0 as the CMS algorithm divides
            // by w and it creates infinity. This can result in NaN output.
            // Under certain parameterizations non-zero small w also creates NaN output.
            // Thus output should be checked regardless.
            return expSampler.sample();
        }

        /**
         * Gets a random value for the phi parameter.
         * This is a uniform random variable in {@code (-pi/2, pi/2)}.
         *
         * @return phi
         */
        double getPhi() {
            // See getPhiBy2 for method details.
            final double x = (nextLong() >> 10) * PI_2_SCALED;
            // Deliberate floating-point equality check
            if (x == NEG_PI_2) {
                return getPhi();
            }
            return x;
        }

        /**
         * Gets a random value for the phi parameter divided by 2.
         * This is a uniform random variable in {@code (-pi/4, pi/4)}.
         *
         * <p>Note: Ideally this should not have a value of -pi/4 or pi/4 as the CMS algorithm
         * can generate infinite values when the phi/2 uniform deviate is +/-pi/4. This
         * can result in NaN output. Under certain parameterizations phi/2 close to the limits
         * also create NaN output. Thus output should be checked regardless. Avoiding
         * the extreme values simplifies the number of checks that are required.
         *
         * @return phi / 2
         */
        double getPhiBy2() {
            // As per o.a.c.rng.core.utils.NumberFactory.makeDouble(long) but using a
            // signed shift of 10 in place of an unsigned shift of 11. With a factor of 2^-53
            // this would produce a double in [-1, 1).
            // Here the multiplication factor incorporates pi/4 to avoid a separate
            // multiplication.
            final double x = (nextLong() >> 10) * PI_4_SCALED;
            // Deliberate floating-point equality check
            if (x == NEG_PI_4) {
                // Sample again using recursion.
                // A stack overflow due to a broken RNG will eventually occur
                // rather than the alternative which is an infinite loop
                // while x == -pi/4.
                return getPhiBy2();
            }
            return x;
        }
    }

    /**
     * Class for implementations of a stable distribution transformed by scale and location.
     */
    private static class TransformedStableSampler extends StableSampler {
        /** Underlying normalized stable sampler. */
        private final StableSampler sampler;
        /** The scale parameter. */
        private final double gamma;
        /** The location parameter. */
        private final double delta;

        /**
         * @param sampler Normalized stable sampler.
         * @param gamma Scale parameter. Must be strictly positive.
         * @param delta Location parameter.
         */
        TransformedStableSampler(StableSampler sampler, double gamma, double delta) {
            // No RNG required
            super(null);
            this.sampler = sampler;
            this.gamma = gamma;
            this.delta = delta;
        }

        @Override
        public double sample() {
            return gamma * sampler.sample() + delta;
        }

        @Override
        public StableSampler withUniformRandomProvider(UniformRandomProvider rng) {
            return new TransformedStableSampler(sampler.withUniformRandomProvider(rng),
                                                gamma, delta);
        }

        @Override
        public String toString() {
            // Avoid a null pointer from the unset RNG instance in the parent class
            return sampler.toString();
        }
    }

    /**
     * Implement the {@code alpha = 2} stable distribution case (Gaussian distribution).
     */
    private static class GaussianStableSampler extends StableSampler {
        /** sqrt(2). */
        private static final double ROOT_2 = Math.sqrt(2);

        /** Underlying normalized Gaussian sampler. */
        private final NormalizedGaussianSampler sampler;
        /** The standard deviation. */
        private final double stdDev;
        /** The mean. */
        private final double mean;

        /**
         * @param rng Underlying source of randomness
         * @param gamma Scale parameter. Must be strictly positive.
         * @param delta Location parameter.
         */
        GaussianStableSampler(UniformRandomProvider rng, double gamma, double delta) {
            super(rng);
            this.sampler = ZigguratSampler.NormalizedGaussian.of(rng);
            // A standardized stable sampler with alpha=2 has variance 2.
            // Set the standard deviation as sqrt(2) * scale.
            // Avoid this being infinity to avoid inf * 0 in the sample
            this.stdDev = Math.min(Double.MAX_VALUE, ROOT_2 * gamma);
            this.mean = delta;
        }

        /**
         * @param rng Underlying source of randomness
         * @param source Source to copy.
         */
        GaussianStableSampler(UniformRandomProvider rng, GaussianStableSampler source) {
            super(rng);
            this.sampler = ZigguratSampler.NormalizedGaussian.of(rng);
            this.stdDev = source.stdDev;
            this.mean = source.mean;
        }

        @Override
        public double sample() {
            return stdDev * sampler.sample() + mean;
        }

        @Override
        public GaussianStableSampler withUniformRandomProvider(UniformRandomProvider rng) {
            return new GaussianStableSampler(rng, this);
        }
    }

    /**
     * Implement the {@code alpha = 1} and {@code beta = 0} stable distribution case
     * (Cauchy distribution).
     */
    private static class CauchyStableSampler extends BaseStableSampler {
        /** The scale parameter. */
        private final double gamma;
        /** The location parameter. */
        private final double delta;

        /**
         * @param rng Underlying source of randomness
         * @param gamma Scale parameter. Must be strictly positive.
         * @param delta Location parameter.
         */
        CauchyStableSampler(UniformRandomProvider rng, double gamma, double delta) {
            super(rng);
            this.gamma = gamma;
            this.delta = delta;
        }

        /**
         * @param rng Underlying source of randomness
         * @param source Source to copy.
         */
        CauchyStableSampler(UniformRandomProvider rng, CauchyStableSampler source) {
            super(rng);
            this.gamma = source.gamma;
            this.delta = source.delta;
        }

        @Override
        public double sample() {
            // Note:
            // The CMS beta=0 with alpha=1 sampler reduces to:
            // S = 2 * a / a2, with a = tan(x), a2 = 1 - a^2, x = phi/2
            // This is a double angle identity for tan:
            // 2 * tan(x) / (1 - tan^2(x)) = tan(2x)
            // Here we use the double angle identity for consistency with the other samplers.
            final double phiby2 = getPhiBy2();
            final double a = phiby2 * SpecialMath.tan2(phiby2);
            final double a2 = 1 - a * a;
            final double x = 2 * a / a2;
            return gamma * x + delta;
        }

        @Override
        public CauchyStableSampler withUniformRandomProvider(UniformRandomProvider rng) {
            return new CauchyStableSampler(rng, this);
        }
    }

    /**
     * Implement the {@code alpha = 0.5} and {@code beta = 1} stable distribution case
     * (Levy distribution).
     *
     * Note: This sampler can be used to output the symmetric case when
     * {@code beta = -1} by negating {@code gamma}.
     */
    private static class LevyStableSampler extends StableSampler {
        /** Underlying normalized Gaussian sampler. */
        private final NormalizedGaussianSampler sampler;
        /** The scale parameter. */
        private final double gamma;
        /** The location parameter. */
        private final double delta;

        /**
         * @param rng Underlying source of randomness
         * @param gamma Scale parameter. Must be strictly positive.
         * @param delta Location parameter.
         */
        LevyStableSampler(UniformRandomProvider rng, double gamma, double delta) {
            super(rng);
            this.sampler = ZigguratSampler.NormalizedGaussian.of(rng);
            this.gamma = gamma;
            this.delta = delta;
        }

        /**
         * @param rng Underlying source of randomness
         * @param source Source to copy.
         */
        LevyStableSampler(UniformRandomProvider rng, LevyStableSampler source) {
            super(rng);
            this.sampler = ZigguratSampler.NormalizedGaussian.of(rng);
            this.gamma = source.gamma;
            this.delta = source.delta;
        }

        @Override
        public double sample() {
            // Levy(Z) = 1 / N(0,1)^2, where N(0,1) is a standard normalized variate
            final double norm = sampler.sample();
            // Here we must transform from the 1-parameterization to the 0-parameterization.
            // This is a shift of -beta * tan(pi * alpha / 2) = -1 when alpha=0.5, beta=1.
            final double z = (1.0 / (norm * norm)) - 1.0;
            // In the 0-parameterization the scale and location are a linear transform.
            return gamma * z + delta;
        }

        @Override
        public LevyStableSampler withUniformRandomProvider(UniformRandomProvider rng) {
            return new LevyStableSampler(rng, this);
        }
    }

    /**
     * Implement the generic stable distribution case: {@code alpha < 2} and
     * {@code beta != 0}. This routine assumes {@code alpha != 1}.
     *
     * <p>Implements the Chambers-Mallows-Stuck (CMS) method using the
     * formula provided in Weron (1996) "On the Chambers-Mallows-Stuck method for
     * simulating skewed stable random variables" Statistics &amp; Probability
     * Letters. 28 (2): 165–171. This method is easier to correct from infinite and
     * NaN results by boxing intermediate infinite values.
     *
     * <p>The formula produces a stable deviate from the 1-parameterization that is
     * discontinuous at {@code alpha=1}. A shift is used to create the 0-parameterization.
     * This shift is very large as {@code alpha -> 1} and the output loses bits of precision
     * in the deviate due to cancellation. It is not recommended to use this sampler when
     * {@code alpha -> 1} except for edge case correction.
     *
     * <p>This produces non-NaN output for all parameters alpha, beta, u and w with
     * the correct orientation for extremes of the distribution support.
     * The formulas used are symmetric with regard to beta and u.
     *
     * @see <a href="https://doi.org/10.1016%2F0167-7152%2895%2900113-1">Weron, R
     * (1996). Statistics &amp; Probability Letters. 28 (2): 165–171.</a>
     */
    static class WeronStableSampler extends BaseStableSampler {
        /** Epsilon (1 - alpha). */
        protected final double eps;
        /** Alpha (1 - eps). */
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
        /** The inclusive lower support for the distribution. */
        protected final double lower;
        /** The inclusive upper support for the distribution. */
        protected final double upper;

        /**
         * @param rng Underlying source of randomness
         * @param alpha Stability parameter. Must be in the interval {@code (0, 2]}.
         * @param beta Skewness parameter. Must be in the interval {@code [-1, 1]}.
         */
        WeronStableSampler(UniformRandomProvider rng, double alpha, double beta) {
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
            // Note: These terms are used interchangeably in formulas
            //    1         1
            // -------  = -----
            // (1-eps)    alpha
            inv1mEps = 1.0 / meps1;
            //    1             eps     (1-alpha)     1
            // -------  - 1 = ------- = --------- = ----- - 1
            // (1-eps)        (1-eps)     alpha     alpha
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

        /**
         * @param rng Underlying source of randomness
         * @param source Source to copy.
         */
        WeronStableSampler(UniformRandomProvider rng, WeronStableSampler source) {
            super(rng);
            this.eps = source.eps;
            this.meps1 = source.meps1;
            this.zeta = source.zeta;
            this.atanZeta = source.atanZeta;
            this.scale = source.scale;
            this.inv1mEps = source.inv1mEps;
            this.epsDiv1mEps = source.epsDiv1mEps;
            this.lower = source.lower;
            this.upper = source.upper;
        }

        @Override
        public double sample() {
            final double phi = getPhi();
            final double w = getOmega();
            return createSample(phi, w);
        }

        /**
         * Create the sample. This routine is robust to edge cases and returns a deviate
         * at the extremes of the support. It correctly handles {@code alpha -> 0} when
         * the sample is increasingly likely to be +/- infinity.
         *
         * @param phi Uniform deviate in {@code (-pi/2, pi/2)}
         * @param w Exponential deviate
         * @return x
         */
        protected double createSample(double phi, double w) {
            // Here we use the formula provided by Weron for the 1-parameterization.
            // Note: Adding back zeta creates the 0-parameterization defined in Nolan (1998):
            // X ~ S0_alpha(s,beta,u0) with s=1, u0=0 for a standard random variable.
            // As alpha -> 1 the translation zeta to create the stable deviate
            // in the 0-parameterization is increasingly large as tan(pi/2) -> infinity.
            // The max translation is approximately 1e16.
            // Without this translation the stable deviate is in the 1-parameterization
            // and the function is not continuous with respect to alpha.
            // Due to the large zeta when alpha -> 1 the number of bits of the output variable
            // are very low due to cancellation.

            // As alpha -> 0 or 2 then zeta -> 0 and cancellation is not relevant.
            // The formula can be modified for infinite terms to compute a result for extreme
            // deviates u and w when the CMS formula fails.

            // Note the following term is subject to floating point error:
            // xi = atan(-zeta) / alpha
            // alphaPhiXi = alpha * (phi + xi)
            // This is required: cos(phi - alphaPhiXi) > 0 => phi - alphaPhiXi in (-pi/2, pi/2).
            // Thus we compute atan(-zeta) and use it to compute two terms:
            // [1] alpha * (phi + xi) = alpha * (phi + atan(-zeta) / alpha) = alpha * phi + atan(-zeta)
            // [2] phi - alpha * (phi + xi) = phi - alpha * phi - atan(-zeta) = (1-alpha) * phi - atan(-zeta)

            // Compute terms
            // Either term can be infinite or 0. Certain parameters compute 0 * inf.
            // t1=inf occurs alpha -> 0.
            // t1=0 occurs when beta = tan(-alpha * phi) / tan(alpha * pi / 2).
            // t2=inf occurs when w -> 0 and alpha -> 0.
            // t2=0 occurs when alpha -> 0 and phi -> pi/2.
            // Detect zeros and return as zeta.

            // Note sin(alpha * phi + atanZeta) is zero when:
            // alpha * phi = -atan(-zeta)
            // tan(-alpha * phi) = -zeta
            //                   = beta * tan(alpha * pi / 2)
            // Since |phi| < pi/2 this requires beta to have an opposite sign to phi
            // and a magnitude < 1. This is possible and in this case avoid a possible
            // 0 / 0 by setting the result as if term t1=0 and the result is zeta.
            double t1 = Math.sin(meps1 * phi + atanZeta);
            if (t1 == 0) {
                return zeta;
            }
            // Since cos(phi) is in (0, 1] this term will not create a
            // large magnitude to create t1 = 0.
            t1 /= Math.pow(Math.cos(phi), inv1mEps);

            // Iff Math.cos(eps * phi - atanZeta) is zero then 0 / 0 can occur if w=0.
            // Iff Math.cos(eps * phi - atanZeta) is below zero then NaN will occur
            // in the power function. These cases are avoided by phi=(-pi/2, pi/2) and direct
            // use of arctan(-zeta).
            final double t2 = Math.pow(Math.cos(eps * phi - atanZeta) / w, epsDiv1mEps);
            if (t2 == 0) {
                return zeta;
            }

            final double x = t1 * t2 * scale + zeta;

            // Check the bounds. Applies when alpha < 1 and beta = +/-1.
            if (x <= lower) {
                return lower;
            }
            return x < upper ? x : upper;
        }

        @Override
        public WeronStableSampler withUniformRandomProvider(UniformRandomProvider rng) {
            return new WeronStableSampler(rng, this);
        }
    }

    /**
     * Implement the generic stable distribution case: {@code alpha < 2} and
     * {@code beta != 0}. This routine assumes {@code alpha != 1}.
     *
     * <p>Implements the Chambers-Mallows-Stuck (CMS) method from Chambers, et al
     * (1976) A Method for Simulating Stable Random Variables. Journal of the
     * American Statistical Association Vol. 71, No. 354, pp. 340-344.
     *
     * <p>The formula produces a stable deviate from the 0-parameterization that is
     * continuous at {@code alpha=1}.
     *
     * <p>This is an implementation of the Fortran routine RSTAB. In the event the
     * computation fails then an alternative computation is performed using the
     * formula provided in Weron (1996) "On the Chambers-Mallows-Stuck method for
     * simulating skewed stable random variables" Statistics &amp; Probability
     * Letters. 28 (2): 165–171. This method is easier to correct from infinite and
     * NaN results. The error correction path is extremely unlikely to occur during
     * use unless {@code alpha -> 0}. In general use it requires the random deviates
     * w or u are extreme. See the unit tests for conditions that create them.
     *
     * <p>This produces non-NaN output for all parameters alpha, beta, u and w with
     * the correct orientation for extremes of the distribution support.
     * The formulas used are symmetric with regard to beta and u.
     */
    static class CMSStableSampler extends WeronStableSampler {
        /** 1/2. */
        private static final double HALF = 0.5;
        /** Cache of expression value used in generation. */
        private final double tau;

        /**
         * @param rng Underlying source of randomness
         * @param alpha Stability parameter. Must be in the interval {@code (0, 2]}.
         * @param beta Skewness parameter. Must be in the interval {@code [-1, 1]}.
         */
        CMSStableSampler(UniformRandomProvider rng, double alpha, double beta) {
            super(rng, alpha, beta);

            // Compute the RSTAB pre-factor.
            tau = getTau(alpha, beta);
        }

        /**
         * @param rng Underlying source of randomness
         * @param source Source to copy.
         */
        CMSStableSampler(UniformRandomProvider rng, CMSStableSampler source) {
            super(rng, source);
            this.tau = source.tau;
        }

        /**
         * Gets tau. This is a factor used in the CMS algorithm. If this is zero then
         * a special case of {@code beta -> 0} has occurred.
         *
         * @param alpha Stability parameter. Must be in the interval {@code (0, 2]}.
         * @param beta Skewness parameter. Must be in the interval {@code [-1, 1]}.
         * @return tau
         */
        static double getTau(double alpha, double beta) {
            final double eps = 1 - alpha;
            final double meps1 = 1 - eps;
            // Compute RSTAB prefactor
            double tau;

            // tau is symmetric around alpha=1
            // tau -> beta / pi/2 as alpha -> 1
            // tau -> 0 as alpha -> 2 or 0
            // Avoid calling tan as the value approaches the domain limit [-pi/2, pi/2].
            if (Math.abs(eps) < HALF) {
                // 0.5 < alpha < 1.5. Note: This works when eps=0 as tan(0) / 0 == 1.
                tau = beta / (SpecialMath.tan2(eps * PI_2) * PI_2);
            } else {
                // alpha >= 1.5 or alpha <= 0.5.
                // Do not call tan with alpha > 1 as it wraps in the domain [-pi/2, pi/2].
                // Since pi is approximate the symmetry is lost by wrapping.
                // Keep within the domain using (2-alpha).
                if (meps1 > 1) {
                    tau = beta * PI_2 * eps * (2 - meps1) * -SpecialMath.tan2((2 - meps1) * PI_2);
                } else {
                    tau = beta * PI_2 * eps * meps1 * SpecialMath.tan2(meps1 * PI_2);
                }
            }

            return tau;
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
            // The method is implemented as per the RSTAB routine with the exceptions:
            // 1. The function tan2(x) is implemented with a higher precision approximation.
            // 2. The sample is tested against the expected distribution support.
            // Infinite intermediate terms that create infinite or NaN are corrected by
            // switching the formula and handling infinite terms.

            // Compute some tangents
            // a in (-1, 1)
            // bb in [1, 4/pi)
            // b in (-1, 1)
            final double a = phiby2 * SpecialMath.tan2(phiby2);
            final double bb = SpecialMath.tan2(eps * phiby2);
            final double b = eps * phiby2 * bb;

            // Compute some necessary subexpressions
            final double da = a * a;
            final double db = b * b;
            // a2 in (0, 1]
            final double a2 = 1 - da;
            // a2p in [1, 2)
            final double a2p = 1 + da;
            // b2 in (0, 1]
            final double b2 = 1 - db;
            // b2p in [1, 2)
            final double b2p = 1 + db;

            // Compute coefficient.
            // numerator=0 is not possible *in theory* when the uniform deviate generating phi
            // is in the open interval (0, 1). In practice it is possible to obtain <=0 due
            // to round-off error, typically when beta -> +/-1 and phiby2 -> -/+pi/4.
            // This can happen for any alpha.
            final double z = a2p * (b2 + 2 * phiby2 * bb * tau) / (w * a2 * b2p);

            // Compute the exponential-type expression
            // Note: z may be infinite, typically when w->0 and a2->0.
            // This can produce NaN under certain parameterizations due to multiplication by 0.
            final double alogz = Math.log(z);
            final double d = SpecialMath.d2(epsDiv1mEps * alogz) * (alogz * inv1mEps);

            // Pre-compute the multiplication factor.
            // The numerator may be zero. The denominator is not zero as a2 is bounded to
            // above zero when the uniform deviate that generates phiby2 is not 0 or 1.
            // The min value of a2 is 2^-52. Assume f cannot be infinite as the numerator
            // is computed with a in (-1, 1); b in (-1, 1); phiby2 in (-pi/4, pi/4); tau in
            // [-2/pi, 2/pi]; bb in [1, 4/pi); a2 in (0, 1] limiting the numerator magnitude.
            final double f = (2 * ((a - b) * (1 + a * b) - phiby2 * tau * bb * (b * a2 - 2 * a))) /
                    (a2 * b2p);

            // Compute the stable deviate:
            final double x = (1 + eps * d) * f + tau * d;

            // Test the support
            if (lower < x && x < upper) {
                return x;
            }

            // Error correction path:
            // x is at the bounds, infinite or NaN (created by 0 / 0,  0 * inf, or inf - inf).
            // This is caused by extreme parameterizations of alpha or beta, or extreme values
            // from the random deviates.
            // Alternatively alpha < 1 and beta = +/-1 and the sample x is at the edge or
            // outside the support due to floating point error.

            // Here we use the formula provided by Weron which is easier to correct
            // when deviates are extreme or alpha -> 0. The formula is not continuous
            // as alpha -> 1 without a shift which reduces the precision of the sample;
            // for rare edge case correction this has minimal effect on sampler output.
            return createSample(phiby2 * 2, w);
        }

        @Override
        public CMSStableSampler withUniformRandomProvider(UniformRandomProvider rng) {
            return new CMSStableSampler(rng, this);
        }
    }

    /**
     * Implement the stable distribution case: {@code alpha == 1} and {@code beta != 0}.
     *
     * <p>Implements the same algorithm as the {@link CMSStableSampler} with
     * the {@code alpha} assumed to be 1.
     *
     * <p>This sampler specifically requires that {@code beta / (pi/2) != 0}; otherwise
     * the parameters equal {@code alpha=1, beta=0} as the Cauchy distribution case.
     */
    static class Alpha1CMSStableSampler extends BaseStableSampler {
        /** Cache of expression value used in generation. */
        private final double tau;

        /**
         * @param rng Underlying source of randomness
         * @param beta Skewness parameter. Must be in the interval {@code [-1, 1]}.
         */
        Alpha1CMSStableSampler(UniformRandomProvider rng, double beta) {
            super(rng);
            tau = beta / PI_2;
        }

        /**
         * @param rng Underlying source of randomness
         * @param source Source to copy.
         */
        Alpha1CMSStableSampler(UniformRandomProvider rng, Alpha1CMSStableSampler source) {
            super(rng);
            this.tau = source.tau;
        }

        @Override
        public double sample() {
            final double phiby2 = getPhiBy2();
            final double w = getOmega();

            // Compute some tangents
            final double a = phiby2 * SpecialMath.tan2(phiby2);

            // Compute some necessary subexpressions
            final double da = a * a;
            final double a2 = 1 - da;
            final double a2p = 1 + da;

            // Compute coefficient.

            // numerator=0 is not possible when the uniform deviate generating phi
            // is in the open interval (0, 1) and alpha=1.
            final double z = a2p * (1 + 2 * phiby2 * tau) / (w * a2);

            // Compute the exponential-type expression
            // Note: z may be infinite, typically when w->0 and a2->0.
            // This can produce NaN under certain parameterizations due to multiplication by 0.
            // When alpha=1 the expression
            // d = d2((eps / (1-eps)) * alogz) * (alogz / (1-eps)) is eliminated to 1 * log(z)
            final double d = Math.log(z);

            // Pre-compute the multiplication factor.
            final double f = (2 * (a - phiby2 * tau * (-2 * a))) / a2;

            // Compute the stable deviate:
            // This does not require correction as f is finite (as per the alpha != 1 case),
            // tau is non-zero and only d can be infinite due to an extreme w -> 0.
            return f + tau * d;
        }

        @Override
        public Alpha1CMSStableSampler withUniformRandomProvider(UniformRandomProvider rng) {
            return new Alpha1CMSStableSampler(rng, this);
        }
    }

    /**
     * Implement the generic stable distribution case: {@code alpha < 2} and {@code beta == 0}.
     *
     * <p>Implements the same algorithm as the {@link WeronStableSampler} with
     * the {@code beta} assumed to be 0.
     *
     * <p>This routine assumes {@code alpha != 1}; {@code alpha=1, beta=0} is the Cauchy
     * distribution case.
     */
    static class Beta0WeronStableSampler extends BaseStableSampler {
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
         * @param alpha Stability parameter. Must be in the interval {@code (0, 2]}.
         */
        Beta0WeronStableSampler(UniformRandomProvider rng, double alpha) {
            super(rng);
            eps = 1 - alpha;
            meps1 = 1 - eps;
            inv1mEps = 1.0 / meps1;
            epsDiv1mEps = inv1mEps - 1;
        }

        /**
         * @param rng Underlying source of randomness
         * @param source Source to copy.
         */
        Beta0WeronStableSampler(UniformRandomProvider rng, Beta0WeronStableSampler source) {
            super(rng);
            this.eps = source.eps;
            this.meps1 = source.meps1;
            this.inv1mEps = source.inv1mEps;
            this.epsDiv1mEps = source.epsDiv1mEps;
        }

        @Override
        public double sample() {
            final double phi = getPhi();
            final double w = getOmega();
            return createSample(phi, w);
        }

        /**
         * Create the sample. This routine is robust to edge cases and returns a deviate
         * at the extremes of the support. It correctly handles {@code alpha -> 0} when
         * the sample is increasingly likely to be +/- infinity.
         *
         * @param phi Uniform deviate in {@code (-pi/2, pi/2)}
         * @param w Exponential deviate
         * @return x
         */
        protected double createSample(double phi, double w) {
            // As per the Weron sampler with beta=0 and terms eliminated.
            // Note that if alpha=1 this reduces to sin(phi) / cos(phi) => Cauchy case.

            // Compute terms.
            // Either term can be infinite or 0. Certain parameters compute 0 * inf.
            // Detect zeros and return as 0.

            // Note sin(alpha * phi) is only ever zero when phi=0. No value of alpha
            // multiplied by small phi can create zero due to the limited
            // precision of alpha imposed by alpha = 1 - (1-alpha). At this point cos(phi) = 1.
            // Thus 0/0 cannot occur.
            final double t1 = Math.sin(meps1 * phi) / Math.pow(Math.cos(phi), inv1mEps);
            if (t1 == 0) {
                return 0;
            }
            final double t2 = Math.pow(Math.cos(eps * phi) / w, epsDiv1mEps);
            if (t2 == 0) {
                return 0;
            }
            return t1 * t2;
        }

        @Override
        public Beta0WeronStableSampler withUniformRandomProvider(UniformRandomProvider rng) {
            return new Beta0WeronStableSampler(rng, this);
        }
    }

    /**
     * Implement the generic stable distribution case: {@code alpha < 2} and {@code beta == 0}.
     *
     * <p>Implements the same algorithm as the {@link CMSStableSampler} with
     * the {@code beta} assumed to be 0.
     *
     * <p>This routine assumes {@code alpha != 1}; {@code alpha=1, beta=0} is the Cauchy
     * distribution case.
     */
    static class Beta0CMSStableSampler extends Beta0WeronStableSampler {
        /**
         * @param rng Underlying source of randomness
         * @param alpha Stability parameter. Must be in the interval {@code (0, 2]}.
         */
        Beta0CMSStableSampler(UniformRandomProvider rng, double alpha) {
            super(rng, alpha);
        }

        /**
         * @param rng Underlying source of randomness
         * @param source Source to copy.
         */
        Beta0CMSStableSampler(UniformRandomProvider rng, Beta0CMSStableSampler source) {
            super(rng, source);
        }

        @Override
        public double sample() {
            final double phiby2 = getPhiBy2();
            final double w = getOmega();

            // Compute some tangents
            final double a = phiby2 * SpecialMath.tan2(phiby2);
            final double b = eps * phiby2 * SpecialMath.tan2(eps * phiby2);
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
            final double d = SpecialMath.d2(epsDiv1mEps * alogz) * (alogz * inv1mEps);

            // Pre-compute the multiplication factor.
            // The numerator may be zero. The denominator is not zero as a2 is bounded to
            // above zero when the uniform deviate that generates phiby2 is not 0 or 1.
            final double f = (2 * ((a - b) * (1 + a * b))) / (a2 * b2p);

            // Compute the stable deviate:
            final double x = (1 + eps * d) * f;

            // Test the support
            if (LOWER < x && x < UPPER) {
                return x;
            }

            // Error correction path.
            // Here we use the formula provided by Weron which is easier to correct
            // when deviates are extreme or alpha -> 0.
            return createSample(phiby2 * 2, w);
        }

        @Override
        public Beta0CMSStableSampler withUniformRandomProvider(UniformRandomProvider rng) {
            return new Beta0CMSStableSampler(rng, this);
        }
    }

    /**
     * Implement special math functions required by the CMS algorithm.
     */
    static final class SpecialMath {
        /** pi/4. */
        private static final double PI_4 = Math.PI / 4;
        /** 4/pi. */
        private static final double FOUR_PI = 4 / Math.PI;
        /** tan2 product constant. */
        private static final double P0 = -0.5712939549476836914932149599e10;
        /** tan2 product constant. */
        private static final double P1 = 0.4946855977542506692946040594e9;
        /** tan2 product constant. */
        private static final double P2 = -0.9429037070546336747758930844e7;
        /** tan2 product constant. */
        private static final double P3 = 0.5282725819868891894772108334e5;
        /** tan2 product constant. */
        private static final double P4 = -0.6983913274721550913090621370e2;
        /** tan2 quotient constant. */
        private static final double Q0 = -0.7273940551075393257142652672e10;
        /** tan2 quotient constant. */
        private static final double Q1 = 0.2125497341858248436051062591e10;
        /** tan2 quotient constant. */
        private static final double Q2 = -0.8000791217568674135274814656e8;
        /** tan2 quotient constant. */
        private static final double Q3 = 0.8232855955751828560307269007e6;
        /** tan2 quotient constant. */
        private static final double Q4 = -0.2396576810261093558391373322e4;
        /**
         * The threshold to switch to using {@link Math#expm1(double)}. The following
         * table shows the mean (max) ULP difference between using expm1 and exp using
         * random +/-x with different exponents (n=2^30):
         *
         * <pre>
         * x        exp  positive x                 negative x
         * 64.0      6   0.10004021506756544  (2)   0.0                   (0)
         * 32.0      5   0.11177831795066595  (2)   0.0                   (0)
         * 16.0      4   0.0986650362610817   (2)   9.313225746154785E-10 (1)
         * 8.0       3   0.09863092936575413  (2)   4.9658119678497314E-6 (1)
         * 4.0       2   0.10015273280441761  (2)   4.547201097011566E-4  (1)
         * 2.0       1   0.14359260816127062  (2)   0.005623611621558666  (2)
         * 1.0       0   0.20160607434809208  (2)   0.03312791418284178   (2)
         * 0.5      -1   0.3993037799373269   (2)   0.28186883218586445   (2)
         * 0.25     -2   0.6307008266448975   (2)   0.5192863345146179    (2)
         * 0.125    -3   1.3862918205559254   (4)   1.386285437270999     (4)
         * 0.0625   -4   2.772640804760158    (8)   2.772612397558987     (8)
         * </pre>
         *
         * <p>The threshold of 0.5 has a mean ULP below 0.5 and max ULP of 2. The
         * transition is monotonic. Neither is true for the next threshold of 0.25.
         */
        private static final double SWITCH_TO_EXPM1 = 0.5;

        /** No instances. */
        private SpecialMath() {}

        /**
         * Evaluate {@code (exp(x) - 1) / x}. For {@code x} in the range {@code [-inf, inf]} returns
         * a result in {@code [0, inf]}.
         *
         * <ul>
         * <li>For {@code x=-inf} this returns {@code 0}.
         * <li>For {@code x=0} this returns {@code 1}.
         * <li>For {@code x=inf} this returns {@code inf}.
         * <li>For {@code x=nan} this returns {@code nan}.
         * </ul>
         *
         * <p> This corrects {@code 0 / 0} and {@code inf / inf} division from
         * {@code NaN} to either {@code 1} or the upper bound respectively.
         *
         * @param x value to evaluate
         * @return {@code (exp(x) - 1) / x}.
         */
        static double d2(double x) {
            // Here expm1 is only used when use of expm1 and exp consistently
            // compute different results by more than 0.5 ULP.
            if (Math.abs(x) < SWITCH_TO_EXPM1) {
                // Deliberate comparison to floating-point zero
                if (x == 0) {
                    // Avoid 0 / 0 error
                    return 1.0;
                }
                return Math.expm1(x) / x;
            }
            // No use of expm1. Accuracy as x moves away from 0 is not required as the result
            // is divided by x and the accuracy of the final result is within a few ULP.
            if (x < Double.POSITIVE_INFINITY) {
                return (Math.exp(x) - 1) / x;
            }
            // Upper bound (or NaN)
            return x;
        }

        /**
         * Evaluate {@code tan(x) / x}.
         *
         * <p>For {@code x} in the range {@code [0, pi/4]} this returns
         * a value in the range {@code [1, 4 / pi]}.
         *
         * <p>The following properties are desirable for the CMS algorithm:
         *
         * <ul>
         * <li>For {@code x=0} this returns {@code 1}.
         * <li>For {@code x=pi/4} this returns {@code 4/pi}.
         * <li>For {@code x=pi/4} this multiplied by {@code x} returns {@code 1}.
         * </ul>
         *
         * <p>This method is called by the CMS algorithm when {@code x < pi/4}.
         * In this case the method is almost as accurate as {@code Math.tan(x) / x}, does
         * not require checking for {@code x=0} and is faster.
         *
         * @param x the x
         * @return {@code tan(x) / x}.
         */
        static double tan2(double x) {
            if (Math.abs(x) > PI_4) {
                // Reduction is not supported. Delegate to the JDK.
                return Math.tan(x) / x;
            }

            // Testing with approximation 4283 from Hart et al, as used in the RSTAB
            // routine, showed the method was not accurate enough for use with
            // double computation. Hart et al state it has max relative error = 1e-10.66.
            // For tan(x) / x with x in [0, pi/4] values outside [1, 4/pi] were computed.
            // When testing verses Math.tan(x) the mean ULP difference is 93436.3.

            // Approximation 4288 from Hart et al (1968, P. 252).
            // Max relative error = 1e-26.68 (for tan(x)).
            // When testing verses Math.tan(x) the mean ULP difference is 0.590597.

            // The approximation is defined as:
            // tan(x*pi/4) = x * P(x^2) / Q(x^2)
            //   with P and Q polynomials of x squared.
            //
            // To create tan(x):
            // tan(x) = xi * P(xi^2) / Q(xi^2), xi = x * 4/pi
            // tan(x) / x = xi * P(xi^2) / Q(xi^2) / x
            // tan(x) / x = 4/pi * (P(xi^2) / Q(xi^2))
            //            = P(xi^2) / (pi/4 * Q(xi^2))
            // The later has a smaller mean ULP difference to Math.tan(x) / x.
            final double xi = x * FOUR_PI;

            // Use the power form with a reverse summation order to have smaller
            // magnitude terms first. Note: x < 1 so greater powers are smaller.
            // This has essentially the same accuracy as the nested form of the polynomials
            // for a marginal performance increase. See JMH examples for performance tests.
            final double x2 = xi * xi;
            final double x4 = x2 * x2;
            final double x6 = x4 * x2;
            final double x8 = x4 * x4;
            return          (x8 * P4 + x6 * P3 + x4 * P2 + x2 * P1 + P0) /
                    (PI_4 * (x8 * x2 + x8 * Q4 + x6 * Q3 + x4 * Q2 + x2 * Q1 + Q0));
        }
    }

    /**
     * @param rng Generator of uniformly distributed random numbers.
     */
    StableSampler(UniformRandomProvider rng) {
        this.rng = rng;
    }

    /**
     * Generate a sample from a stable distribution.
     *
     * <p>The distribution uses the 0-parameterization: S(alpha, beta, gamma, delta; 0).
     */
    @Override
    public abstract double sample();

    /** {@inheritDoc} */
    // Redeclare the signature to return a StableSampler not a SharedStateContinuousSampler
    @Override
    public abstract StableSampler withUniformRandomProvider(UniformRandomProvider rng);

    /**
     * Generates a {@code long} value.
     * Used by algorithm implementations without exposing access to the RNG.
     *
     * @return the next random value
     */
    long nextLong() {
        return rng.nextLong();
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        // All variations use the same string representation, i.e. no changes
        // for the Gaussian, Levy or Cauchy case.
        return "Stable deviate [" + rng.toString() + "]";
    }

    /**
     * Creates a standardized sampler of a stable distribution with zero location and unit scale.
     *
     * <p>Special cases:
     *
     * <ul>
     * <li>{@code alpha=2} returns a Gaussian distribution sampler with
     *     {@code mean=0} and {@code variance=2} (Note: {@code beta} has no effect on the distribution).
     * <li>{@code alpha=1} and {@code beta=0} returns a Cauchy distribution sampler with
     *     {@code location=0} and {@code scale=1}.
     * <li>{@code alpha=0.5} and {@code beta=1} returns a Levy distribution sampler with
     *     {@code location=-1} and {@code scale=1}. This location shift is due to the
     *     0-parameterization of the stable distribution.
     * </ul>
     *
     * <p>Note: To allow the computation of the stable distribution the parameter alpha
     * is validated using {@code 1 - alpha} in the interval {@code [-1, 1)}.
     *
     * @param rng Generator of uniformly distributed random numbers.
     * @param alpha Stability parameter. Must be in the interval {@code (0, 2]}.
     * @param beta Skewness parameter. Must be in the interval {@code [-1, 1]}.
     * @return the sampler
     * @throws IllegalArgumentException if {@code 1 - alpha < -1}; or {@code 1 - alpha >= 1};
     * or {@code beta < -1}; or {@code beta > 1}.
     */
    public static StableSampler of(UniformRandomProvider rng,
                                   double alpha,
                                   double beta) {
        validateParameters(alpha, beta);
        return create(rng, alpha, beta);
    }

    /**
     * Creates a sampler of a stable distribution. This applies a transformation to the
     * standardized sampler.
     *
     * <p>The random variable \( X \) has
     * the stable distribution \( S(\alpha, \beta, \gamma, \sigma; 0) \) if:
     *
     * <p>\[ X = \gamma Z_0 + \delta \]
     *
     * <p>where \( Z_0 = S(\alpha, \beta; 0) \) is a standardized stable distribution.
     *
     * <p>Note: To allow the computation of the stable distribution the parameter alpha
     * is validated using {@code 1 - alpha} in the interval {@code [-1, 1)}.
     *
     * @param rng Generator of uniformly distributed random numbers.
     * @param alpha Stability parameter. Must be in the interval {@code (0, 2]}.
     * @param beta Skewness parameter. Must be in the interval {@code [-1, 1]}.
     * @param gamma Scale parameter. Must be strictly positive and finite.
     * @param delta Location parameter. Must be finite.
     * @return the sampler
     * @throws IllegalArgumentException if {@code 1 - alpha < -1}; or {@code 1 - alpha >= 1};
     * or {@code beta < -1}; or {@code beta > 1}; or {@code gamma <= 0}; or
     * {@code gamma} or {@code delta} are not finite.
     * @see #of(UniformRandomProvider, double, double)
     */
    public static StableSampler of(UniformRandomProvider rng,
                                   double alpha,
                                   double beta,
                                   double gamma,
                                   double delta) {
        validateParameters(alpha, beta, gamma, delta);

        // Choose the algorithm.
        // Reuse the special cases as they have transformation support.

        if (alpha == ALPHA_GAUSSIAN) {
            // Note: beta has no effect and is ignored.
            return new GaussianStableSampler(rng, gamma, delta);
        }

        // Note: As beta -> 0 the result cannot be computed differently to beta = 0.
        if (alpha == ALPHA_CAUCHY && CMSStableSampler.getTau(ALPHA_CAUCHY, beta) == TAU_ZERO) {
            return new CauchyStableSampler(rng, gamma, delta);
        }

        if (alpha == ALPHA_LEVY && Math.abs(beta) == BETA_LEVY) {
            // Support mirroring for negative beta by inverting the beta=1 Levy sample
            // using a negative gamma. Note: The delta is not mirrored as it is a shift
            // applied to the scaled and mirrored distribution.
            return new LevyStableSampler(rng, beta * gamma, delta);
        }

        // Standardized sampler
        final StableSampler sampler = create(rng, alpha, beta);
        // Transform
        return new TransformedStableSampler(sampler, gamma, delta);
    }

    /**
     * Creates a standardized sampler of a stable distribution with zero location and unit scale.
     *
     * @param rng Generator of uniformly distributed random numbers.
     * @param alpha Stability parameter. Must be in the interval {@code (0, 2]}.
     * @param beta Skewness parameter. Must be in the interval {@code [-1, 1]}.
     * @return the sampler
     */
    private static StableSampler create(UniformRandomProvider rng,
                                        double alpha,
                                        double beta) {
        // Choose the algorithm.
        // The special case samplers have transformation support and use gamma=1.0, delta=0.0.
        // As alpha -> 0 the computation increasingly requires correction
        // of infinity to the distribution support.

        if (alpha == ALPHA_GAUSSIAN) {
            // Note: beta has no effect and is ignored.
            return new GaussianStableSampler(rng, GAMMA_1, DELTA_0);
        }

        // Note: As beta -> 0 the result cannot be computed differently to beta = 0.
        // This is based on the computation factor tau:
        final double tau = CMSStableSampler.getTau(alpha, beta);

        if (tau == TAU_ZERO) {
            // Symmetric case (beta skew parameter is effectively zero)
            if (alpha == ALPHA_CAUCHY) {
                return new CauchyStableSampler(rng, GAMMA_1, DELTA_0);
            }
            if (alpha <= ALPHA_SMALL) {
                // alpha -> 0 requires robust error correction
                return new Beta0WeronStableSampler(rng, alpha);
            }
            return new Beta0CMSStableSampler(rng, alpha);
        }

        // Here beta is significant.

        if (alpha == 1) {
            return new Alpha1CMSStableSampler(rng, beta);
        }

        if (alpha == ALPHA_LEVY && Math.abs(beta) == BETA_LEVY) {
            // Support mirroring for negative beta by inverting the beta=1 Levy sample
            // using a negative gamma. Note: The delta is not mirrored as it is a shift
            // applied to the scaled and mirrored distribution.
            return new LevyStableSampler(rng, beta, DELTA_0);
        }

        if (alpha <= ALPHA_SMALL) {
            // alpha -> 0 requires robust error correction
            return new WeronStableSampler(rng, alpha, beta);
        }

        return new CMSStableSampler(rng, alpha, beta);
    }

    /**
     * Validate the parameters are in the correct range.
     *
     * @param alpha Stability parameter. Must be in the interval {@code (0, 2]}.
     * @param beta Skewness parameter. Must be in the interval {@code [-1, 1]}.
     * @throws IllegalArgumentException if {@code 1 - alpha < -1}; or {@code 1 - alpha >= 1};
     * or {@code beta < -1}; or {@code beta > 1}.
     */
    private static void validateParameters(double alpha, double beta) {
        // The epsilon (1-alpha) value must be in the interval [-1, 1).
        // Logic inversion will identify NaN
        final double eps = 1 - alpha;
        if (!(-1 <= eps && eps < 1)) {
            throw new IllegalArgumentException("alpha is not in the interval (0, 2]: " + alpha);
        }
        if (!(-1 <= beta && beta <= 1)) {
            throw new IllegalArgumentException("beta is not in the interval [-1, 1]: " + beta);
        }
    }

    /**
     * Validate the parameters are in the correct range.
     *
     * @param alpha Stability parameter. Must be in the interval {@code (0, 2]}.
     * @param beta Skewness parameter. Must be in the interval {@code [-1, 1]}.
     * @param gamma Scale parameter. Must be strictly positive and finite.
     * @param delta Location parameter. Must be finite.
     * @throws IllegalArgumentException if {@code 1 - alpha < -1}; or {@code 1 - alpha >= 1};
     * or {@code beta < -1}; or {@code beta > 1}; or {@code gamma <= 0}; or
     * {@code gamma} or {@code delta} are not finite.
     */
    private static void validateParameters(double alpha, double beta,
                                           double gamma, double delta) {
        validateParameters(alpha, beta);

        // Logic inversion will identify NaN
        if (!(0 < gamma && gamma <= Double.MAX_VALUE)) {
            throw new IllegalArgumentException("gamma is not strictly positive and finite: " + gamma);
        }
        if (!Double.isFinite(delta)) {
            throw new IllegalArgumentException("delta is not finite: " + delta);
        }
    }
}
