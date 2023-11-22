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
import org.apache.commons.rng.sampling.SharedStateSampler;

/**
 * Functions used by some of the samplers.
 * This class is not part of the public API, as it would be
 * better to group these utilities in a dedicated component.
 */
final class InternalUtils { // Class is package-private on purpose; do not make it public.
    /** All long-representable factorials. */
    private static final long[] FACTORIALS = {
        1L,                1L,                  2L,
        6L,                24L,                 120L,
        720L,              5040L,               40320L,
        362880L,           3628800L,            39916800L,
        479001600L,        6227020800L,         87178291200L,
        1307674368000L,    20922789888000L,     355687428096000L,
        6402373705728000L, 121645100408832000L, 2432902008176640000L };

    /** The first array index with a non-zero log factorial. */
    private static final int BEGIN_LOG_FACTORIALS = 2;

    /**
     * The multiplier to convert the least significant 53-bits of a {@code long} to a {@code double}.
     * Taken from org.apache.commons.rng.core.util.NumberFactory.
     */
    private static final double DOUBLE_MULTIPLIER = 0x1.0p-53d;

    /** Utility class. */
    private InternalUtils() {}

    /**
     * @param n Argument.
     * @return {@code n!}
     * @throws IndexOutOfBoundsException if the result is too large to be represented
     * by a {@code long} (i.e. if {@code n > 20}), or {@code n} is negative.
     */
    static long factorial(int n)  {
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
    static double validateProbabilities(double[] probabilities) {
        if (probabilities == null || probabilities.length == 0) {
            throw new IllegalArgumentException("Probabilities must not be empty.");
        }

        double sumProb = 0;
        for (final double prob : probabilities) {
            sumProb += requirePositiveFinite(prob, "probability");
        }

        return requireStrictlyPositiveFinite(sumProb, "sum of probabilities");
    }

    /**
     * Checks the value {@code x} is finite.
     *
     * @param x Value.
     * @param name Name of the value.
     * @return x
     * @throws IllegalArgumentException if {@code x} is non-finite
     */
    static double requireFinite(double x, String name) {
        if (!Double.isFinite(x)) {
            throw new IllegalArgumentException(name + " is not finite: " + x);
        }
        return x;
    }

    /**
     * Checks the value {@code x >= 0} and is finite.
     * Note: This method allows {@code x == -0.0}.
     *
     * @param x Value.
     * @param name Name of the value.
     * @return x
     * @throws IllegalArgumentException if {@code x < 0} or is non-finite
     */
    static double requirePositiveFinite(double x, String name) {
        if (!(x >= 0 && x < Double.POSITIVE_INFINITY)) {
            throw new IllegalArgumentException(
                name + " is not positive and finite: " + x);
        }
        return x;
    }

    /**
     * Checks the value {@code x > 0} and is finite.
     *
     * @param x Value.
     * @param name Name of the value.
     * @return x
     * @throws IllegalArgumentException if {@code x <= 0} or is non-finite
     */
    static double requireStrictlyPositiveFinite(double x, String name) {
        if (!(x > 0 && x < Double.POSITIVE_INFINITY)) {
            throw new IllegalArgumentException(
                name + " is not strictly positive and finite: " + x);
        }
        return x;
    }

    /**
     * Checks the value {@code x >= 0}.
     * Note: This method allows {@code x == -0.0}.
     *
     * @param x Value.
     * @param name Name of the value.
     * @return x
     * @throws IllegalArgumentException if {@code x < 0}
     */
    static double requirePositive(double x, String name) {
        // Logic inversion detects NaN
        if (!(x >= 0)) {
            throw new IllegalArgumentException(name + " is not positive: " + x);
        }
        return x;
    }

    /**
     * Checks the value {@code x > 0}.
     *
     * @param x Value.
     * @param name Name of the value.
     * @return x
     * @throws IllegalArgumentException if {@code x <= 0}
     */
    static double requireStrictlyPositive(double x, String name) {
        // Logic inversion detects NaN
        if (!(x > 0)) {
            throw new IllegalArgumentException(name + " is not strictly positive: " + x);
        }
        return x;
    }

    /**
     * Checks the value is within the range: {@code min <= x < max}.
     *
     * @param min Minimum (inclusive).
     * @param max Maximum (exclusive).
     * @param x Value.
     * @param name Name of the value.
     * @return x
     * @throws IllegalArgumentException if {@code x < min || x >= max}.
     */
    static double requireRange(double min, double max, double x, String name) {
        if (!(min <= x && x < max)) {
            throw new IllegalArgumentException(
                String.format("%s not within range: %s <= %s < %s", name, min, x, max));
        }
        return x;
    }

    /**
     * Checks the value is within the closed range: {@code min <= x <= max}.
     *
     * @param min Minimum (inclusive).
     * @param max Maximum (inclusive).
     * @param x Value.
     * @param name Name of the value.
     * @return x
     * @throws IllegalArgumentException if {@code x < min || x > max}.
     */
    static double requireRangeClosed(double min, double max, double x, String name) {
        if (!(min <= x && x <= max)) {
            throw new IllegalArgumentException(
                String.format("%s not within closed range: %s <= %s <= %s", name, min, x, max));
        }
        return x;
    }

    /**
     * Create a new instance of the given sampler using
     * {@link SharedStateSampler#withUniformRandomProvider(UniformRandomProvider)}.
     *
     * @param sampler Source sampler.
     * @param rng Generator of uniformly distributed random numbers.
     * @return the new sampler
     * @throws UnsupportedOperationException if the underlying sampler is not a
     * {@link SharedStateSampler} or does not return a {@link NormalizedGaussianSampler} when
     * sharing state.
     */
    static NormalizedGaussianSampler newNormalizedGaussianSampler(
            NormalizedGaussianSampler sampler,
            UniformRandomProvider rng) {
        if (!(sampler instanceof SharedStateSampler<?>)) {
            throw new UnsupportedOperationException("The underlying sampler cannot share state");
        }
        final Object newSampler = ((SharedStateSampler<?>) sampler).withUniformRandomProvider(rng);
        if (!(newSampler instanceof NormalizedGaussianSampler)) {
            throw new UnsupportedOperationException(
                "The underlying sampler did not create a normalized Gaussian sampler");
        }
        return (NormalizedGaussianSampler) newSampler;
    }

    /**
     * Creates a {@code double} in the interval {@code [0, 1)} from a {@code long} value.
     *
     * @param v Number.
     * @return a {@code double} value in the interval {@code [0, 1)}.
     */
    static double makeDouble(long v) {
        // This matches the method in o.a.c.rng.core.util.NumberFactory.makeDouble(long)
        // without adding an explicit dependency on that module.
        return (v >>> 11) * DOUBLE_MULTIPLIER;
    }

    /**
     * Creates a {@code double} in the interval {@code (0, 1]} from a {@code long} value.
     *
     * @param v Number.
     * @return a {@code double} value in the interval {@code (0, 1]}.
     */
    static double makeNonZeroDouble(long v) {
        // This matches the method in o.a.c.rng.core.util.NumberFactory.makeDouble(long)
        // but shifts the range from [0, 1) to (0, 1].
        return ((v >>> 11) + 1L) * DOUBLE_MULTIPLIER;
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
