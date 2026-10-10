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
import org.apache.commons.rng.sampling.SharedStateObjectSampler;

/**
 * Sampling from a <a href="https://en.wikipedia.org/wiki/Dirichlet_distribution">Dirichlet
 * distribution</a>.
 *
 * <p>Sampling uses:</p>
 *
 * <ul>
 *   <li>{@link UniformRandomProvider#nextLong()}</li>
 *   <li>{@link UniformRandomProvider#nextDouble()}</li>
 * </ul>
 *
 * @since 1.4
 */
public abstract class DirichletSampler implements SharedStateObjectSampler<double[]> {
    /** The minimum number of categories. */
    private static final int MIN_CATGEORIES = 2;
    /** The minimum alpha before using log samples. See RNG-195. */
    private static final double MIN_ALPHA = 0x1p-5;
    /** The maximum alpha before using log samples. See RNG-195. */
    private static final double MAX_ALPHA = 0x1p53;
    /** The threshold for tiny alpha to use scaling.
     * A tiny alpha is any value where log1p(alpha/E) + log(u) == log(u) for u in (0, 1).
     * The smallest magnitude for log(u) is 2^-53 so a tiny alpha is 2^-53 * 2^-53 = 2^-106.
     * However this must be lowered by the amount of scaling
     * applied to alpha. Set using a conservatively low threshold so that
     * TINY_ALPHA * ALPHA_SCALE is below 2^-106. */
    private static final double TINY_ALPHA = 0x1p-163;
    /** Scaling to apply to tiny alpha where all log gamma samplers
     * will return samples as log(u) / alpha with u in [0, 1).
     * Any scaled alpha must be below 2^-106 after scaling so the log(u) / alpha
     * pathway is always used, and log1p(alpha/E) cannot be added to log(u).
     * Set so that log(2^-53) / (scale * MIN_VALUE) is finite allowing support
     * for alpha down to the lowest non-zero double value when the uniform deviate
     * is the smallest non-zero value. */
    private static final double ALPHA_SCALE = 0x1p56;

    /** Normalizer for the gamma samples. */
    private final Normalizer normalizer;
    /** RNG (used for the toString() method). */
    private final UniformRandomProvider rng;

    /**
     * Normalize an array of samples to unit length.
     */
    // package-private for testing
    enum Normalizer {
        /** Normalise real-valued samples.
         * The sum of the samples is expected to be a finite normal double.
         * A sub-normal sum cannot be normalised as this uses multiplication by the
         * reciprocal of the sum (which overflows when sum is tiny). */
        REAL {
            @Override
            boolean normalize(double[] x) {
                // Samples are never length zero
                double norm = x[0];
                for (int i = 1; i < x.length; i++) {
                    norm += x[i];
                }
                // Normalize by dividing by the sum of the samples
                norm = 1.0 / norm;
                // Detect an invalid normalization,
                // e.g. cases of all zero samples, or one or more infinite samples.
                if (isNonZeroPositiveFinite(norm)) {
                    // Normalise
                    for (int i = 0; i < x.length; i++) {
                        x[i] *= norm;
                    }
                    return true;
                }
                return false;
            }
        },
        /** Normalise log-valued samples.
         * This will normalize any sample even including infinity (or NaN). */
        LOG {
            @Override
            boolean normalize(double[] x) {
                return normalize(x, 1.0);
            }
        },
        /** Normalise scaled log-valued samples.
         * This will normalize any sample even including infinity (or NaN). */
        SCALED_LOG {
            @Override
            boolean normalize(double[] x) {
                return normalize(x, ALPHA_SCALE);
            }
        };

        /**
         * Normalise the sample to unit length using the sum.
         *
         * <p>Returns false if the sample could not be normalized. This occurs when
         * all terms underflow and the sum is zero, or one or more terms
         * overflow and the sum is infinity.
         *
         * @param x Sample.
         * @return true if the sample was normalized
         */
        abstract boolean normalize(double[] x);

        /**
         * Normalise the log sample to unit length using the sum.
         * Log samples are multiplied by the provided {@code scale}. Used
         * when log samples originate from a scaled alpha parameter passed
         * to the gamma sampler.
         *
         * @param x Sample.
         * @param scale Scale.
         * @return true
         */
        static boolean normalize(double[] x, double scale) {
            // Scale log numbers so we can sum them.
            // Subtracting the maximum log value stabilizes the sum.
            // The maximum exp term will be 1, all other terms <=1 and the sum is finite.
            double max = x[0];
            for (int i = 1; i < x.length; i++) {
                max = Math.max(max, x[i]);
            }
            double norm = 0;
            if (Double.isFinite(max)) {
                // A finite max will normalize any sample including -infinity.
                for (int i = 0; i < x.length; i++) {
                    // If scale > 1 then log samples are moved further apart.
                    // It is likely only the max will contribute to the sum
                    // and other sample values are 0.
                    x[i] = Math.exp((x[i] - max) * scale);
                    norm += x[i];
                }
            } else {
                // Any value matching the max is normalised to length 1.
                // A side effect is that any NaNs will normalize to 1
                // as Math.max puts NaN above all other numbers.
                // NaNs or positive infinity are not expected from the log gamma samples.
                final long b = Double.doubleToLongBits(max);
                for (int i = 0; i < x.length; i++) {
                    x[i] = Double.doubleToLongBits(x[i]) == b ? 1 : 0;
                    norm += x[i];
                }
            }
            // Note: In contrast to the real case where samples
            // are expected across all categories, log samples
            // for tiny alpha are typically biased to 1 category.
            // Using divide ensures extreme samples for tiny
            // alpha have 1.0 in the chosen category and not 1-2^53
            // which can happen when multiplying by the reciprocal.
            for (int i = 0; i < x.length; i++) {
                x[i] /= norm;
            }
            return true;
        }
    }

    /**
     * Sample from a Dirichlet distribution with different concentration parameters
     * for each category.
     */
    private static final class GeneralDirichletSampler extends DirichletSampler {
        /** Samplers for each category. */
        private final SharedStateContinuousSampler[] samplers;

        /**
         * Constructs a new instance.
         *
         * @param rng Generator of uniformly distributed random numbers.
         * @param samplers Samplers for each category.
         * @param normalizer Normalizer for the samples.
         */
        GeneralDirichletSampler(UniformRandomProvider rng,
                                SharedStateContinuousSampler[] samplers,
                                Normalizer normalizer) {
            super(rng, normalizer);
            // Array is stored directly as it is generated within the DirichletSampler class
            this.samplers = samplers;
        }

        @Override
        protected int getK() {
            return samplers.length;
        }

        @Override
        protected double nextGamma(int i) {
            return samplers[i].sample();
        }

        @Override
        public GeneralDirichletSampler withUniformRandomProvider(UniformRandomProvider rng) {
            final SharedStateContinuousSampler[] newSamplers = new SharedStateContinuousSampler[samplers.length];
            for (int i = 0; i < newSamplers.length; i++) {
                newSamplers[i] = samplers[i].withUniformRandomProvider(rng);
            }
            return new GeneralDirichletSampler(rng, newSamplers, getNormalizer());
        }
    }

    /**
     * Sample from a symmetric Dirichlet distribution with the same concentration parameter
     * for each category.
     */
    private static final class SymmetricDirichletSampler extends DirichletSampler {
        /** Number of categories. */
        private final int k;
        /** Sampler for the categories. */
        private final SharedStateContinuousSampler sampler;

        /**
         * Constructs a new instance.
         *
         * @param rng Generator of uniformly distributed random numbers.
         * @param k Number of categories.
         * @param sampler Sampler for the categories.
         * @param normalizer Normalizer for the samples.
         */
        SymmetricDirichletSampler(UniformRandomProvider rng,
                                  int k,
                                  SharedStateContinuousSampler sampler,
                                  Normalizer normalizer) {
            super(rng, normalizer);
            this.k = k;
            this.sampler = sampler;
        }

        @Override
        protected int getK() {
            return k;
        }

        @Override
        protected double nextGamma(int i) {
            return sampler.sample();
        }

        @Override
        public SymmetricDirichletSampler withUniformRandomProvider(UniformRandomProvider rng) {
            return new SymmetricDirichletSampler(rng, k, sampler.withUniformRandomProvider(rng), getNormalizer());
        }
    }

    /**
     * Constructs a new instance.
     *
     * @param rng Generator of uniformly distributed random numbers.
     * @param normalizer Normalizer for the samples.
     */
    DirichletSampler(UniformRandomProvider rng, Normalizer normalizer) {
        this.rng = rng;
        this.normalizer = normalizer;
    }

    /**
     * Gets the normalizer.
     *
     * @return the normalizer
     */
    Normalizer getNormalizer() {
        return normalizer;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return "Dirichlet deviate [" + rng.toString() + "]";
    }

    /** {@inheritDoc} */
    @Override
    public double[] sample() {
        // Create Gamma(alpha_i, 1) deviates for all alpha
        final double[] y = new double[getK()];
        for (int i = 0; i < y.length; i++) {
            y[i] = nextGamma(i);
        }
        if (!normalizer.normalize(y)) {
            // Sample again using recursion.
            // A stack overflow due to a broken RNG will eventually occur
            // rather than the alternative which is an infinite loop.
            return sample();
        }
        return y;
    }

    /**
     * Gets the number of categories.
     *
     * @return k
     */
    protected abstract int getK();

    /**
     * Create a gamma sample for the given category.
     *
     * @param category Category.
     * @return the sample
     */
    protected abstract double nextGamma(int category);

    /** {@inheritDoc} */
    // Redeclare the signature to return a DirichletSampler not a SharedStateObjectSampler<double[]>
    @Override
    public abstract DirichletSampler withUniformRandomProvider(UniformRandomProvider rng);

    /**
     * Creates a new Dirichlet distribution sampler.
     *
     * @param rng Generator of uniformly distributed random numbers.
     * @param alpha Concentration parameters.
     * @return the sampler
     * @throws IllegalArgumentException if the number of concentration parameters
     * is less than 2; or if any concentration parameter is not strictly positive.
     */
    public static DirichletSampler of(UniformRandomProvider rng,
                                      double... alpha) {
        validateNumberOfCategories(alpha.length);
        // Check for the maximum alpha.
        // The mean of each gamma sample is the concentration parameter alpha.
        // All samples must be normalised to unit length using the sum.
        // Use log samples when samples will likely over/underflow on the largest
        // sample (create a zero or infinite sample). These can be scaled by the
        // largest sample before summing.
        double maxAlpha = alpha[0];
        for (int i = 1; i < alpha.length; i++) {
            maxAlpha = Math.max(maxAlpha, alpha[i]);
        }
        final boolean logSamples = requireLogSamples(maxAlpha);
        final SharedStateContinuousSampler[] samplers = new SharedStateContinuousSampler[alpha.length];
        for (int i = 0; i < samplers.length; i++) {
            samplers[i] = createSampler(rng, alpha[i], logSamples, maxAlpha);
        }
        return new GeneralDirichletSampler(rng, samplers, createNormalizer(logSamples, maxAlpha));
    }

    /**
     * Creates a new symmetric Dirichlet distribution sampler using the same concentration
     * parameter for each category.
     *
     * @param rng Generator of uniformly distributed random numbers.
     * @param k Number of categories.
     * @param alpha Concentration parameter.
     * @return the sampler
     * @throws IllegalArgumentException if the number of categories is
     * less than 2; or if the concentration parameter is not strictly positive.
     */
    public static DirichletSampler symmetric(UniformRandomProvider rng,
                                             int k,
                                             double alpha) {
        validateNumberOfCategories(k);
        final boolean logSamples = requireLogSamples(alpha);
        final SharedStateContinuousSampler sampler = createSampler(rng, alpha, logSamples, alpha);
        return new SymmetricDirichletSampler(rng, k, sampler, createNormalizer(logSamples, alpha));
    }

    /**
     * Validate the number of categories.
     *
     * @param k Number of categories.
     * @throws IllegalArgumentException if the number of categories is
     * less than 2.
     */
    private static void validateNumberOfCategories(int k) {
        if (k < MIN_CATGEORIES) {
            throw new IllegalArgumentException("Invalid number of categories: " + k);
        }
    }

    /**
     * Creates a gamma sampler for a category with the given concentration parameter.
     *
     * @param rng Generator of uniformly distributed random numbers.
     * @param alpha Concentration parameter.
     * @param logSamples Return samples using the natural logarithm of the sample.
     * @param maxAlpha Maximum alpha.
     * @return the sampler
     * @throws IllegalArgumentException if the concentration parameter is not strictly positive.
     */
    private static SharedStateContinuousSampler createSampler(UniformRandomProvider rng,
                                                              double alpha,
                                                              boolean logSamples,
                                                              double maxAlpha) {
        InternalUtils.requireStrictlyPositiveFinite(alpha, "alpha concentration");
        // Create a Gamma(shape=alpha, scale=1) sampler.
        if (alpha == 1 && !logSamples) {
            // Special case
            // Gamma(shape=1, scale=1) == Exponential(mean=1)
            return ZigguratSampler.Exponential.of(rng);
        }

        // -----
        // Alpha scaling
        //
        // When alpha is very small 1/alpha will overflow.
        // This invalidates the gamma sampler which creates samples using:
        //   u in [0, 1) : p = u * (1 + alpha / E) ~ u
        //   x = p^(1/alpha)
        // For any p < 1 the result is zero. Log samples use:
        //   log(p) / alpha
        // This is stable until alpha approaches MIN_NORMAL where it can overflow.
        // Since this sampler normalizes log samples using scaling so they can be
        // summed the solution to tiny alpha is to perform some scaling on alpha
        // when all samplers use the same gamma sampler method. Log samples
        // must be rescaled before normalisation:
        // log(p) / alpha = scale * log(p) / (scale * alpha)
        // -----
        final double a = maxAlpha < TINY_ALPHA ? alpha * ALPHA_SCALE : alpha;
        return AhrensDieterMarsagliaTsangGammaSampler.of(rng, a, 1, logSamples);
    }

    /**
     * Return true if the value is non-zero, positive and finite.
     *
     * @param x Value.
     * @return true if non-zero positive finite
     */
    private static boolean isNonZeroPositiveFinite(double x) {
        return x > 0 && x < Double.POSITIVE_INFINITY;
    }

    /**
     * Check if the gamma sampler should use log samples. This extends support for
     * small and large concentration parameters where the samples cannot be normalised
     * to unit length without underflow or overflow.
     *
     * @param alpha Maximum alpha parameter.
     * @return true if log samples are required
     */
    private static boolean requireLogSamples(double alpha) {
        return alpha < MIN_ALPHA || alpha > MAX_ALPHA;
    }

    /**
     * Creates the normalizer for the samples.
     *
     * @param logSamples Return samples using the natural logarithm of the sample.
     * @param alpha Maximum alpha parameter.
     * @return the normalizer
     */
    private static Normalizer createNormalizer(boolean logSamples, double alpha) {
        if (logSamples) {
            return alpha < TINY_ALPHA ? Normalizer.SCALED_LOG : Normalizer.LOG;
        }
        return Normalizer.REAL;
    }
}
