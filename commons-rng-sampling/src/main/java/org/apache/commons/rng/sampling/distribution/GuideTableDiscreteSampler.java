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
 * Compute a sample from {@code n} values each with an associated probability. If all unique items
 * are assigned the same probability it is more efficient to use the {@link DiscreteUniformSampler}.
 *
 * <p>The cumulative probability distribution is searched using a guide table to set an
 * initial start point. This implementation is based on:</p>
 *
 * <blockquote>
 *  Devroye, Luc (1986). Non-Uniform Random Variate Generation.
 *  New York: Springer-Verlag. Chapter 3.2.4 "The method of guide tables" p. 96.
 * </blockquote>
 *
 * <p>The size of the guide table can be controlled using a parameter. A larger guide table
 * will improve performance at the cost of storage space.</p>
 *
 * <p>Sampling uses {@link UniformRandomProvider#nextDouble()}.</p>
 *
 * @see <a href="http://en.wikipedia.org/wiki/Probability_distribution#Discrete_probability_distribution">
 * Discrete probability distribution (Wikipedia)</a>
 * @since 1.3
 */
public final class GuideTableDiscreteSampler
    implements SharedStateDiscreteSampler {
    /** The default value for {@code alpha}. */
    private static final double DEFAULT_ALPHA = 1.0;
    /** Underlying source of randomness. */
    private final UniformRandomProvider rng;
    /**
     * The cumulative probability table ({@code f(x)}).
     */
    private final double[] cumulativeProbabilities;
    /**
     * The inverse cumulative probability guide table. This is a guide map between the cumulative
     * probability (f(x)) and the value x. It is used to set the initial point for search
     * of the cumulative probability table.
     *
     * <p>The index in the map is obtained using {@code p * map.length} where {@code p} is the
     * known cumulative probability {@code f(x)} or a uniform random deviate {@code u}. The value
     * stored at the index is value {@code x+1} when {@code p = f(x)} such that it is the
     * exclusive upper bound on the sample value {@code x} for searching the cumulative probability
     * table {@code f(x)}. The search of the cumulative probability is towards zero.</p>
     */
    private final int[] guideTable;

    /**
     * @param rng Generator of uniformly distributed random numbers.
     * @param cumulativeProbabilities The cumulative probability table ({@code f(x)}).
     * @param guideTable The inverse cumulative probability guide table.
     */
    private GuideTableDiscreteSampler(UniformRandomProvider rng,
                                      double[] cumulativeProbabilities,
                                      int[] guideTable) {
        this.rng = rng;
        this.cumulativeProbabilities = cumulativeProbabilities;
        this.guideTable = guideTable;
    }

    /** {@inheritDoc} */
    @Override
    public int sample() {
        // Compute a probability
        final double u = rng.nextDouble();

        // Initialise the search using the guide table to find an initial guess.
        // The table provides an upper bound on the sample (x+1) for a known
        // cumulative probability (f(x)).
        int x = guideTable[getGuideTableIndex(u, guideTable.length)];
        // Search down.
        // In the edge case where u is 1.0 then 'x' will be 1 outside the range of the
        // cumulative probability table and this will decrement to a valid range.
        // In the case where 'u' is mapped to the same guide table index as a lower
        // cumulative probability f(x) (due to rounding down) then this will not decrement
        // and return the exclusive upper bound (x+1).
        while (x != 0 && u <= cumulativeProbabilities[x - 1]) {
            x--;
        }
        return x;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return "Guide table deviate [" + rng.toString() + "]";
    }

    /** {@inheritDoc} */
    @Override
    public SharedStateDiscreteSampler withUniformRandomProvider(UniformRandomProvider rng) {
        return new GuideTableDiscreteSampler(rng, cumulativeProbabilities, guideTable);
    }

    /**
     * Create a new sampler for an enumerated distribution using the given {@code probabilities}.
     * The samples corresponding to each probability are assumed to be a natural sequence
     * starting at zero.
     *
     * <p>The size of the guide table is {@code probabilities.length}.</p>
     *
     * @param rng Generator of uniformly distributed random numbers.
     * @param probabilities The probabilities.
     * @return the sampler
     * @throws IllegalArgumentException if {@code probabilities} is null or empty, a
     * probability is negative, infinite or {@code NaN}, or the sum of all
     * probabilities is not strictly positive.
     */
    public static SharedStateDiscreteSampler of(UniformRandomProvider rng,
                                                double[] probabilities) {
        return of(rng, probabilities, DEFAULT_ALPHA);
    }

    /**
     * Create a new sampler for an enumerated distribution using the given {@code probabilities}.
     * The samples corresponding to each probability are assumed to be a natural sequence
     * starting at zero.
     *
     * <p>The size of the guide table is {@code alpha * probabilities.length}.</p>
     *
     * @param rng Generator of uniformly distributed random numbers.
     * @param probabilities The probabilities.
     * @param alpha The alpha factor used to set the guide table size.
     * @return the sampler
     * @throws IllegalArgumentException if {@code probabilities} is null or empty, a
     * probability is negative, infinite or {@code NaN}, the sum of all
     * probabilities is not strictly positive, or {@code alpha} is not strictly positive.
     */
    public static SharedStateDiscreteSampler of(UniformRandomProvider rng,
                                                double[] probabilities,
                                                double alpha) {
        validateParameters(probabilities, alpha);

        final int size = probabilities.length;
        final double[] cumulativeProbabilities = new double[size];

        double sumProb = 0;
        int count = 0;
        for (final double prob : probabilities) {
            InternalUtils.validateProbability(prob);

            // Compute and store cumulative probability.
            sumProb += prob;
            cumulativeProbabilities[count++] = sumProb;
        }

        if (Double.isInfinite(sumProb) || sumProb <= 0) {
            throw new IllegalArgumentException("Invalid sum of probabilities: " + sumProb);
        }

        // Note: The guide table is at least length 1. Compute the size avoiding overflow
        // in case (alpha * size) is too large.
        final int guideTableSize = (int) Math.ceil(alpha * size);
        final int[] guideTable = new int[Math.max(guideTableSize, guideTableSize + 1)];

        // Compute and store cumulative probability.
        for (int x = 0; x < size; x++) {
            final double norm = cumulativeProbabilities[x] / sumProb;
            cumulativeProbabilities[x] = (norm < 1) ? norm : 1.0;

            // Set the guide table value as an exclusive upper bound (x + 1)
            final int index = getGuideTableIndex(cumulativeProbabilities[x], guideTable.length);
            guideTable[index] = x + 1;
        }

        // Edge case for round-off
        cumulativeProbabilities[size - 1] = 1.0;
        // The final guide table entry is (maximum value of x + 1)
        guideTable[guideTable.length - 1] = size;

        // The first non-zero value in the guide table is from f(x=0).
        // Any probabilities mapped below this must be sample x=0 so the
        // table may initially be filled with zeros.

        // Fill missing values in the guide table.
        for (int i = 1; i < guideTable.length; i++) {
            guideTable[i] = Math.max(guideTable[i - 1], guideTable[i]);
        }

        return new GuideTableDiscreteSampler(rng, cumulativeProbabilities, guideTable);
    }

    /**
     * Validate the parameters.
     *
     * @param probabilities The probabilities.
     * @param alpha The alpha factor used to set the guide table size.
     * @throws IllegalArgumentException if {@code probabilities} is null or empty, or
     * {@code alpha} is not strictly positive.
     */
    private static void validateParameters(double[] probabilities, double alpha) {
        if (probabilities == null || probabilities.length == 0) {
            throw new IllegalArgumentException("Probabilities must not be empty.");
        }
        if (alpha <= 0) {
            throw new IllegalArgumentException("Alpha must be strictly positive.");
        }
    }

    /**
     * Gets the guide table index for the probability. This is obtained using
     * {@code p * (tableLength - 1)} so is inside the length of the table.
     *
     * @param p Cumulative probability.
     * @param tableLength Table length.
     * @return the guide table index.
     */
    private static int getGuideTableIndex(double p, int tableLength) {
        // Note: This is only ever called when p is in the range of the cumulative
        // probability table. So assume 0 <= p <= 1.
        return (int) (p * (tableLength - 1));
    }
}
