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

/**
 * Functions used by some of the samplers.
 * This class is not part of the public API, as it would be
 * better to group these utilities in a dedicated component.
 */
final class InternalUtils { // Class is package-private on purpose; do not make it public.
    /** All long-representable factorials. */
    private static final long[] FACTORIALS = new long[] {
        1L,                1L,                  2L,
        6L,                24L,                 120L,
        720L,              5040L,               40320L,
        362880L,           3628800L,            39916800L,
        479001600L,        6227020800L,         87178291200L,
        1307674368000L,    20922789888000L,     355687428096000L,
        6402373705728000L, 121645100408832000L, 2432902008176640000L };

    /** The first array index with a non-zero log factorial. */
    private static final int BEGIN_LOG_FACTORIALS = 2;

    /** Utility class. */
    private InternalUtils() {}

    /**
     * @param n Argument.
     * @return {@code n!}
     * @throws IndexOutOfBoundsException if the result is too large to be represented
     * by a {@code long} (i.e. if {@code n > 20}), or {@code n} is negative.
     */
    public static long factorial(int n)  {
        return FACTORIALS[n];
    }

    /**
     * Validate the probabilities sum to a finite positive number.
     *
     * @param probabilities the probabilities
     * @return the sum
     * @throws IllegalArgumentException if {@code probabilities} is null or empty, a
     * probability is negative, infinite or {@code NaN}, or the sum of all
     * probabilities is not strictly positive.
     */
    public static double validateProbabilities(double[] probabilities) {
        if (probabilities == null || probabilities.length == 0) {
            throw new IllegalArgumentException("Probabilities must not be empty.");
        }

        double sumProb = 0;
        for (final double prob : probabilities) {
            validateProbability(prob);
            sumProb += prob;
        }

        if (Double.isInfinite(sumProb) || sumProb <= 0) {
            throw new IllegalArgumentException("Invalid sum of probabilities: " + sumProb);
        }
        return sumProb;
    }

    /**
     * Validate the probability is a finite positive number.
     *
     * @param probability Probability.
     * @throws IllegalArgumentException if {@code probability} is negative, infinite or {@code NaN}.
     */
    public static void validateProbability(double probability) {
        if (probability < 0 ||
            Double.isInfinite(probability) ||
            Double.isNaN(probability)) {
            throw new IllegalArgumentException("Invalid probability: " +
                                               probability);
        }
    }

    /**
     * Class for computing the natural logarithm of the factorial of {@code n}.
     * It allows to allocate a cache of precomputed values.
     * In case of cache miss, computation is performed by a call to
     * {@link InternalGamma#logGamma(double)}.
     */
    public static final class FactorialLog {
        /**
         * Precomputed values of the function:
         * {@code LOG_FACTORIALS[i] = log(i!)}.
         */
        private final double[] logFactorials;

        /**
         * Creates an instance, reusing the already computed values if available.
         *
         * @param numValues Number of values of the function to compute.
         * @param cache Existing cache.
         * @throws NegativeArraySizeException if {@code numValues < 0}.
         */
        private FactorialLog(int numValues,
                             double[] cache) {
            logFactorials = new double[numValues];

            int endCopy;
            if (cache != null && cache.length > BEGIN_LOG_FACTORIALS) {
                // Copy available values.
                endCopy = Math.min(cache.length, numValues);
                System.arraycopy(cache, BEGIN_LOG_FACTORIALS, logFactorials, BEGIN_LOG_FACTORIALS,
                    endCopy - BEGIN_LOG_FACTORIALS);
            } else {
                // All values to be computed
                endCopy = BEGIN_LOG_FACTORIALS;
            }

            // Compute remaining values.
            for (int i = endCopy; i < numValues; i++) {
                if (i < FACTORIALS.length) {
                    logFactorials[i] = Math.log(FACTORIALS[i]);
                } else {
                    logFactorials[i] = logFactorials[i - 1] + Math.log(i);
                }
            }
        }

        /**
         * Creates an instance with no precomputed values.
         *
         * @return an instance with no precomputed values.
         */
        public static FactorialLog create() {
            return new FactorialLog(0, null);
        }

        /**
         * Creates an instance with the specified cache size.
         *
         * @param cacheSize Number of precomputed values of the function.
         * @return a new instance where {@code cacheSize} values have been
         * precomputed.
         * @throws IllegalArgumentException if {@code n < 0}.
         */
        public FactorialLog withCache(final int cacheSize) {
            return new FactorialLog(cacheSize, logFactorials);
        }

        /**
         * Computes {@code log(n!)}.
         *
         * @param n Argument.
         * @return {@code log(n!)}.
         * @throws IndexOutOfBoundsException if {@code numValues < 0}.
         */
        public double value(final int n) {
            // Use cache of precomputed values.
            if (n < logFactorials.length) {
                return logFactorials[n];
            }

            // Use cache of precomputed factorial values.
            if (n < FACTORIALS.length) {
                return Math.log(FACTORIALS[n]);
            }

            // Delegate.
            return InternalGamma.logGamma(n + 1.0);
        }
    }
}
