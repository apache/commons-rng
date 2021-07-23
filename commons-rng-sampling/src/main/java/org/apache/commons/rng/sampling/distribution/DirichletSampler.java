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
 *   <li>{@link UniformRandomProvider#nextLong()}
 *   <li>{@link UniformRandomProvider#nextDouble()}
 * </ul>
 *
 * @since 1.4
 */
public abstract class DirichletSampler implements SharedStateObjectSampler<double[]> {
    /** The minimum number of categories. */
    private static final int MIN_CATGEORIES = 2;

    /** RNG (used for the toString() method). */
    private final UniformRandomProvider rng;

    /**
     * Sample from a Dirichlet distribution with different concentration parameters
     * for each category.
     */
    private static final class GeneralDirichletSampler extends DirichletSampler {
        /** Samplers for each category. */
        private final SharedStateContinuousSampler[] samplers;

        /**
         * @param rng Generator of uniformly distributed random numbers.
         * @param samplers Samplers for each category.
         */
        GeneralDirichletSampler(UniformRandomProvider rng,
                                SharedStateContinuousSampler[] samplers) {
            super(rng);
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
            return new GeneralDirichletSampler(rng, newSamplers);
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
         * @param rng Generator of uniformly distributed random numbers.
         * @param k Number of categories.
         * @param sampler Sampler for the categories.
         */
        SymmetricDirichletSampler(UniformRandomProvider rng,
                                  int k,
                                  SharedStateContinuousSampler sampler) {
            super(rng);
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
            return new SymmetricDirichletSampler(rng, k, sampler.withUniformRandomProvider(rng));
        }
    }

    /**
     * @param rng Generator of uniformly distributed random numbers.
     */
    private DirichletSampler(UniformRandomProvider rng) {
        this.rng = rng;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return "Dirichlet deviate [" + rng.toString() + "]";
    }

    @Override
    public double[] sample() {
        // Create Gamma(alpha_i, 1) deviates for all alpha
        final double[] y = new double[getK()];
        double norm = 0;
        for (int i = 0; i < y.length; i++) {
            final double yi = nextGamma(i);
            norm += yi;
            y[i] = yi;
        }
        // Normalize by dividing by the sum of the samples
        norm = 1.0 / norm;
        // Detect an invalid normalization, e.g. cases of all zero samples
        if (!isNonZeroPositiveFinite(norm)) {
            // Sample again using recursion.
            // A stack overflow due to a broken RNG will eventually occur
            // rather than the alternative which is an infinite loop.
            return sample();
        }
        // Normalise
        for (int i = 0; i < y.length; i++) {
            y[i] *= norm;
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
        final SharedStateContinuousSampler[] samplers = new SharedStateContinuousSampler[alpha.length];
        for (int i = 0; i < samplers.length; i++) {
            samplers[i] = createSampler(rng, alpha[i]);
        }
        return new GeneralDirichletSampler(rng, samplers);
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
        final SharedStateContinuousSampler sampler = createSampler(rng, alpha);
        return new SymmetricDirichletSampler(rng, k, sampler);
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
     * @return the sampler
     * @throws IllegalArgumentException if the concentration parameter is not strictly positive.
     */
    private static SharedStateContinuousSampler createSampler(UniformRandomProvider rng,
                                                              double alpha) {
        // Negation of logic will detect NaN
        if (!isNonZeroPositiveFinite(alpha)) {
            throw new IllegalArgumentException("Invalid concentration: " + alpha);
        }
        // Create a Gamma(shape=alpha, scale=1) sampler.
        if (alpha == 1) {
            // Special case
            // Gamma(shape=1, scale=1) == Exponential(mean=1)
            return ZigguratSampler.Exponential.of(rng);
        }
        return AhrensDieterMarsagliaTsangGammaSampler.of(rng, alpha, 1);
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
}
