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

import java.util.Arrays;

/**
 * Distribution sampler that uses the <a
 * href="https://en.wikipedia.org/wiki/Alias_method">Alias method</a>. It can be used to
 * sample from {@code n} values each with an associated probability. If all unique items
 * are assigned the same probability it is more efficient to use the {@link DiscreteUniformSampler}.
 *
 * <p>This implementation is based on the detailed explanation of the alias method by
 * Keith Schartz and implements Vose's algorithm.</p>
 *
 * <ul>
 *  <li>
 *   <blockquote>
 *    Vose, M.D.,
 *    <i>A linear algorithm for generating random numbers with a given distribution,</i>
 *     IEEE Transactions on Software Engineering, 17, 972-975, 1991.
 *   </blockquote>
 *  </li>
 * </ul>
 *
 * <p>The algorithm will sample values in {@code O(1)} time after a pre-processing step of
 * {@code O(n)} time.</p>
 *
 * <p>The alias tables are constructed using fraction probabilities with an assumed denominator
 * of 2<sup>53</sup>. In the generic case sampling uses {@link UniformRandomProvider#nextInt(int)}
 * and the upper 53-bits from {@link UniformRandomProvider#nextLong()}.</p>
 *
 * <p>Zero padding the input probabilities can be used to make more sampling more efficient.
 * Any zero entry will always be aliased removing the requirement to compute a {@code long}.
 * Increased sampling speed comes at the cost of increased storage space. The algorithm requires
 * approximately 12 bytes of storage per input probability, that is {@code n * 12} for size
 * {@code n}. Zero-padding only requires 4 bytes of storage per padded value as the probability is
 * known to be zero. A table can be padded to a power of 2 using the utility function
 * {@link #create(UniformRandomProvider, double[], int)} to construct the sampler.</p>
 *
 * <p>An optimisation is performed for small table sizes that are a power of 2. In this case the
 * sampling uses 1 or 2 calls from {@link UniformRandomProvider#nextInt()} to generate up to
 * 64-bits for creation of an 11-bit index and 53-bits for the {@code long}. This optimisation
 * requires a generator with a high cycle length for the lower order bits.</p>
 *
 * <p>Larger table sizes that are a power of 2 will benefit from fast algorithms for
 * {@link UniformRandomProvider#nextInt(int)} that exploit the power of 2.</p>
 *
 * @since 1.3
 * @see <a href="https://en.wikipedia.org/wiki/Alias_method">Alias Method</a>
 * @see <a href="http://www.keithschwarz.com/darts-dice-coins/">Darts, Dice, and Coins:
 * Sampling from a Discrete Distribution by Keith Schwartz</a>
 * @see <a href="https://ieeexplore.ieee.org/document/92917">Vose (1991) IEEE Transactions
 * on Software Engineering 17, 972-975.</a>
 */
public class AliasMethodDiscreteSampler
    implements DiscreteSampler, SharedStateSampler<AliasMethodDiscreteSampler> {
    /**
     * The default alpha factor for zero-padding an input probability table. The default
     * value will pad the probabilities by to the next power-of-2.
     */
    private static final int DEFAULT_ALPHA = 0;
    /** The value zero for a {@code double}. */
    private static final double ZERO = 0.0;
    /** The value 1.0 represented as the numerator of a fraction with denominator 2<sup>53</sup>. */
    private static final long ONE_AS_NUMERATOR = 1L << 53;
    /**
     * The multiplier to convert a {@code double} probability in the range {@code [0, 1]}
     * to the numerator of a fraction with denominator 2<sup>53</sup>.
     */
    private static final double CONVERT_TO_NUMERATOR = ONE_AS_NUMERATOR;
    /**
     * The maximum size of the small alias table. This is 2<sup>11</sup>.
     */
    private static final int MAX_SMALL_POWER_2_SIZE = 1 << 11;

    /** Underlying source of randomness. */
    protected final UniformRandomProvider rng;

    /**
     * The probability table. During sampling a random index into this table is selected.
     * A random probability is compared to the value at this index: if lower then the sample is the
     * index; if higher then the sample uses the corresponding entry in the alias table.
     *
     * <p>This has entries up to the last non-zero element since there is no need to store
     * probabilities of zero. This is an optimisation for zero-padded input. Any zero value will
     * always be aliased so any look-up index outside this table always uses the alias.</p>
     *
     * <p>Note that a uniform double in the range [0,1) can be generated using 53-bits from a long
     * to sample all the dyadic rationals with a denominator of 2<sup>53</sup>
     * (e.g. see org.apache.commons.rng.core.utils.NumberFactory.makeDouble(long)). To avoid
     * computation of a double and comparison to the probability as a double the probabilities are
     * stored as 53-bit longs to use integer arithmetic. This is the equivalent of storing the
     * numerator of a fraction with the denominator of 2<sup>53</sup>.</p>
     *
     * <p>During conversion of the probability to a double it is rounded up to the next integer
     * value. This ensures the functionality of comparing a uniform deviate distributed evenly on
     * the interval 1/2^53 to the unevenly distributed probability is equivalent, i.e. a uniform
     * deviate is either below the probability or above it:
     *
     * <pre>
     * Uniform deviate
     *  1/2^53    2/2^53    3/2^53    4/2^53
     * --|---------|---------|---------|---
     *      ^
     *      |
     *  probability
     *             ^
     *             |
     *         rounded up
     * </pre>
     *
     * <p>Round-up ensures a non-zero probability is always non-zero and zero probability remains
     * zero. Thus any item with a non-zero input probability can always be sampled, and a zero
     * input probability cannot be sampled.</p>
     *
     * @see <a href="https://en.wikipedia.org/wiki/Dyadic_rational">Dyadic rational</a>
     */
    protected final long[] probability;

    /**
     * The alias table. During sampling if the random probability is not below the entry in the
     * probability table then the sample is the alias.
     */
    protected final int[] alias;

    /**
     * Sample from the computed tables exploiting the small power-of-two table size.
     * This implements a variant of the optimised algorithm as per Vose (1991):
     *
     * <pre>
     * bits = obtained required number of random bits
     * v = (some of the bits) * constant1
     * j = (rest of the bits) * constant2
     * if v &lt; prob[j] then
     *   return j
     * else
     *   return alias[j]
     * </pre>
     *
     * <p>This is a variant because the bits are not multiplied by constants. In the case of
     * {@code v} the constant is a scale that is pre-applied to the probability table. In the
     * case of {@code j} the constant is not used to scale a deviate to an index; the index is
     * from a power-of-2 range and so the bits are used directly.</p>
     *
     * <p>This is implemented using up to 64 bits from the random generator.
     * The index for the table is computed using a mask to extract up to 11 of the lower bits
     * from an integer. The probability is computed using a second integer combined with the
     * remaining bits to create 53-bits for the numerator of a fraction with denominator
     * 2<sup>53</sup>. This is only computed on demand.</p>
     *
     * <p>Note: This supports a table size of up to 2^11, or 2048, exclusive. Any larger requires
     * consuming more than 64-bits and the algorithm is not more efficient than the
     * {@link AliasMethodDiscreteSampler}.</p>
     *
     * <p>Sampling uses 1 or 2 calls to {@link UniformRandomProvider#nextInt()}.</p>
     */
    private static class SmallTableAliasMethodDiscreteSampler extends AliasMethodDiscreteSampler {
        /** The mask to isolate the lower bits. */
        private final int mask;

        /**
         * Create a new instance.
         *
         * @param rng Generator of uniformly distributed random numbers.
         * @param probability Probability table.
         * @param alias Alias table.
         */
        SmallTableAliasMethodDiscreteSampler(final UniformRandomProvider rng,
                                             final long[] probability,
                                             final int[] alias) {
            super(rng, probability, alias);
            // Assume the table size is a power of 2 and create the mask
            mask = alias.length - 1;
        }

        @Override
        public int sample() {
            final int bits = rng.nextInt();
            // Isolate lower bits
            final int j = bits & mask;

            // Optimisation for zero-padded input tables
            if (j >= probability.length) {
                // No probability must use the alias
                return alias[j];
            }

            // Create a uniform random deviate as a long.
            // This replicates functionality from the o.a.c.rng.core.utils.NumberFactory.makeLong
            final long longBits = (((long) rng.nextInt()) << 32) | (bits & 0xffffffffL);

            // Choose between the two. Use a 53-bit long for the probability.
            return (longBits >>> 11) < probability[j] ? j : alias[j];
        }

        /** {@inheritDoc} */
        @Override
        public SmallTableAliasMethodDiscreteSampler withUniformRandomProvider(UniformRandomProvider rng) {
            return new SmallTableAliasMethodDiscreteSampler(rng, probability, alias);
        }
    }

    /**
     * Creates a sampler.
     *
     * <p>The input parameters are not validated and must be correctly computed alias tables.</p>
     *
     * @param rng Generator of uniformly distributed random numbers.
     * @param probability Probability table.
     * @param alias Alias table.
     */
    private AliasMethodDiscreteSampler(final UniformRandomProvider rng,
                                       final long[] probability,
                                       final int[] alias) {
        this.rng = rng;
        // Deliberate direct storage of input arrays
        this.probability = probability;
        this.alias = alias;
    }

    /** {@inheritDoc} */
    @Override
    public int sample() {
        // This implements the algorithm as per Vose (1991):
        // v = uniform()  in [0, 1)
        // j = uniform(n) in [0, n)
        // if v < prob[j] then
        //   return j
        // else
        //   return alias[j]

        final int j = rng.nextInt(alias.length);

        // Optimisation for zero-padded input tables
        if (j >= probability.length) {
            // No probability must use the alias
            return alias[j];
        }

        // Note: We could check the probability before computing a deviate.
        // p(j) == 0  => alias[j]
        // p(j) == 1  => j
        // However it is assumed these edge cases are rare:
        //
        // The probability table will be 1 for approximately 1/n samples, i.e. only the
        // last unpaired probability. This is only worth checking for when the table size (n)
        // is small. But in that case the user should zero-pad the table for performance.
        //
        // The probability table will be 0 when an input probability was zero. We
        // will assume this is also rare if modelling a discrete distribution where
        // all samples are possible. The edge case for zero-padded tables is handled above.

        // Choose between the two. Use a 53-bit long for the probability.
        return (rng.nextLong() >>> 11) < probability[j] ? j : alias[j];
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return "Alias method [" + rng.toString() + "]";
    }

    /** {@inheritDoc} */
    @Override
    public AliasMethodDiscreteSampler withUniformRandomProvider(UniformRandomProvider rng) {
        return new AliasMethodDiscreteSampler(rng, probability, alias);
    }

    /**
     * Creates a sampler.
     *
     * <p>The probabilities will be normalised using their sum. The only requirement
     * is the sum is strictly positive.</p>
     *
     * <p>Where possible this method zero-pads the probabilities so the length is the next
     * power-of-two. Padding is bounded by the upper limit on the size of an array.</p>
     *
     * <p>To avoid zero-padding use the
     * {@link #create(UniformRandomProvider, double[], int)} method with a negative
     * {@code alpha} factor.</p>
     *
     * @param rng Generator of uniformly distributed random numbers.
     * @param probabilities The list of probabilities.
     * @return the sampler
     * @throws IllegalArgumentException if {@code probabilities} is null or empty, a
     * probability is negative, infinite or {@code NaN}, or the sum of all
     * probabilities is not strictly positive.
     * @see #create(UniformRandomProvider, double[], int)
     */
    public static AliasMethodDiscreteSampler create(final UniformRandomProvider rng,
                                                    final double[] probabilities) {
        return create(rng, probabilities, DEFAULT_ALPHA);
    }

    /**
     * Creates a sampler.
     *
     * <p>The probabilities will be normalised using their sum. The only requirement
     * is the sum is strictly positive.</p>
     *
     * <p>Where possible this method zero-pads the probabilities to improve sampling
     * efficiency. Padding is bounded by the upper limit on the size of an array and
     * controlled by the {@code alpha} argument. Set to negative to disable
     * padding.</p>
     *
     * <p>For each zero padded value an entry is added to the tables which is always
     * aliased. This can be sampled with fewer bits required from the
     * {@link UniformRandomProvider}. Increasing the padding of zeros increases the
     * chance of using this fast path to selecting a sample. The penalty is
     * two-fold: initialisation is bounded by {@code O(n)} time with {@code n} the
     * size <strong>after</strong> padding; an additional memory cost of 4 bytes per
     * padded value.</p>
     *
     * <p>Zero padding to any length improves performance; using a power of 2 allows
     * the index into the tables to be more efficiently generated. The argument
     * {@code alpha} controls the level of padding. Positive values of {@code alpha}
     * represent a scale factor in powers of 2. The size of the input array will be
     * increased by a factor of 2<sup>alpha</sup> and then rounded-up to the next
     * power of 2. Padding is bounded by the upper limit on the size of an
     * array.</p>
     *
     * <p>The chance of executing the slow path is upper bounded at
     * 2<sup>-alpha</sup> when padding is enabled. Each successive doubling of
     * padding will have diminishing performance gains.</p>
     *
     * @param rng Generator of uniformly distributed random numbers.
     * @param probabilities The list of probabilities.
     * @param alpha The alpha factor controlling the zero padding.
     * @return the sampler
     * @throws IllegalArgumentException if {@code probabilities} is null or empty, a
     * probability is negative, infinite or {@code NaN}, or the sum of all
     * probabilities is not strictly positive.
     */
    public static AliasMethodDiscreteSampler create(final UniformRandomProvider rng,
                                                    final double[] probabilities,
                                                    int alpha) {
        // The Alias method balances N categories with counts around the mean into N sections,
        // each allocated 'mean' observations.
        //
        // Consider 4 categories with counts 6,3,2,1. The histogram can be balanced into a
        // 2D array as 4 sections with a height of the mean:
        //
        // 6
        // 6
        // 6
        // 63   => 6366   --
        // 632     6326    |-- mean
        // 6321    6321   --
        //
        // section abcd
        //
        // Each section is divided as:
        // a: 6=1/1
        // b: 3=1/1
        // c: 2=2/3; 6=1/3   (6 is the alias)
        // d: 1=1/3; 6=2/3   (6 is the alias)
        //
        // The sample is obtained by randomly selecting a section, then choosing which category
        // from the pair based on a uniform random deviate.

        final double sumProb = InternalUtils.validateProbabilities(probabilities);

        // Allow zero-padding
        final int n = computeSize(probabilities.length, alpha);

        // Partition into small and large by splitting on the average.
        final double mean = sumProb / n;
        // The cardinality of smallSize + largeSize = n.
        // So fill the same array from either end.
        final int[] indices = new int[n];
        int large = n;
        int small = 0;
        for (int i = 0; i < probabilities.length; i++) {
            if (probabilities[i] >= mean) {
                indices[--large] = i;
            } else {
                indices[small++] = i;
            }
        }

        small = fillRemainingIndices(probabilities.length, indices, small);

        // This may be smaller than the input length if the probabilities were already padded.
        final int nonZeroIndex = findLastNonZeroIndex(probabilities);

        // The probabilities are modified so use a copy.
        // Note: probabilities are required only up to last nonZeroIndex
        final double[] remainingProbabilities = Arrays.copyOf(probabilities, nonZeroIndex + 1);

        // Allocate the final tables.
        // Probability table may be truncated (when zero padded).
        // The alias table is full length.
        final long[] probability = new long[remainingProbabilities.length];
        final int[] alias = new int[n];

        // This loop uses each large in turn to fill the alias table for small probabilities that
        // do not reach the requirement to fill an entire section alone (i.e. p < mean).
        // Since the sum of the small should be less than the sum of the large it should use up
        // all the small first. However floating point round-off can result in
        // misclassification of items as small or large. The Vose algorithm handles this using
        // a while loop conditioned on the size of both sets and a subsequent loop to use
        // unpaired items.
        while (large != n && small != 0) {
            // Index of the small and the large probabilities.
            final int j = indices[--small];
            final int k = indices[large++];

            // Optimisation for zero-padded input:
            // p(j) = 0 above the last nonZeroIndex
            if (j > nonZeroIndex) {
                // The entire amount for the section is taken from the alias.
                remainingProbabilities[k] -= mean;
            } else {
                final double pj = remainingProbabilities[j];

                // Item j is a small probability that is below the mean.
                // Compute the weight of the section for item j: pj / mean.
                // This is scaled by 2^53 and the ceiling function used to round-up
                // the probability to a numerator of a fraction in the range [1,2^53].
                // Ceiling ensures non-zero values.
                probability[j] = (long) Math.ceil(CONVERT_TO_NUMERATOR * (pj / mean));

                // The remaining amount for the section is taken from the alias.
                // Effectively: probabilities[k] -= (mean - pj)
                remainingProbabilities[k] += pj - mean;
            }

            // If not j then the alias is k
            alias[j] = k;

            // Add the remaining probability from large to the appropriate list.
            if (remainingProbabilities[k] >= mean) {
                indices[--large] = k;
            } else {
                indices[small++] = k;
            }
        }

        // Final loop conditions to consume unpaired items.
        // Note: The large set should never be non-empty but this can occur due to round-off
        // error so consume from both.
        fillTable(probability, alias, indices, 0, small);
        fillTable(probability, alias, indices, large, n);

        // Change the algorithm for small power of 2 sized tables
        return isSmallPowerOf2(n) ?
            new SmallTableAliasMethodDiscreteSampler(rng, probability, alias) :
            new AliasMethodDiscreteSampler(rng, probability, alias);
    }

    /**
     * Allocate the remaining indices from zero padding as small probabilities. The
     * number to add is from the length of the probability array to the length of
     * the padded probability array (which is the same length as the indices array).
     *
     * @param length Length of probability array.
     * @param indices Indices.
     * @param small Number of small indices.
     * @return the updated number of small indices
     */
    private static int fillRemainingIndices(final int length, final int[] indices, int small) {
        int updatedSmall = small;
        for (int i = length; i < indices.length; i++) {
            indices[updatedSmall++] = i;
        }
        return updatedSmall;
    }

    /**
     * Find the last non-zero index in the probabilities. This may be smaller than
     * the input length if the probabilities were already padded.
     *
     * @param probabilities The list of probabilities.
     * @return the index
     */
    private static int findLastNonZeroIndex(final double[] probabilities) {
        // No bounds check is performed when decrementing as the array contains at least one
        // value above zero.
        int nonZeroIndex = probabilities.length - 1;
        while (probabilities[nonZeroIndex] == ZERO) {
            nonZeroIndex--;
        }
        return nonZeroIndex;
    }

    /**
     * Compute the size after padding. A value of {@code alpha < 0} disables
     * padding. Otherwise the length will be increased by 2<sup>alpha</sup>
     * rounded-up to the next power of 2.
     *
     * @param length Length of probability array.
     * @param alpha The alpha factor controlling the zero padding.
     * @return the padded size
     */
    private static int computeSize(int length, int alpha) {
        if (alpha < 0) {
            // No padding
            return length;
        }
        // Use the number of leading zeros function to find the next power of 2,
        // i.e. ceil(log2(x))
        int pow2 = 32 - Integer.numberOfLeadingZeros(length - 1);
        // Increase by the alpha. Clip this to limit to a positive integer (2^30)
        pow2 = Math.min(30, pow2 + alpha);
        // Use max to handle a length above the highest possible power of 2
        return Math.max(length, 1 << pow2);
    }

    /**
     * Fill the tables using unpaired items that are in the range between {@code start} inclusive
     * and {@code end} exclusive.
     *
     * <p>Anything left must fill the entire section so the probability table is set
     * to 1 and there is no alias. This will occur for 1/n samples, i.e. the last
     * remaining unpaired probability. Note: When the tables are zero-padded the
     * remaining indices are from an input probability that is above zero so the
     * index will be allowed in the truncated probability array and no
     * index-out-of-bounds exception will occur.
     *
     * @param probability Probability table.
     * @param alias Alias table.
     * @param indices Unpaired indices.
     * @param start Start position.
     * @param end End position.
     */
    private static void fillTable(long[] probability, int[] alias, int[] indices, int start, int end) {
        for (int i = start; i < end; i++) {
            final int index = indices[i];
            probability[index] = ONE_AS_NUMERATOR;
            alias[index] = index;
        }
    }

    /**
     * Checks if the size is a small power of 2 so can be supported by the
     * {@link SmallTableAliasMethodDiscreteSampler}.
     *
     * @param n Size of the alias table.
     * @return true if supported by {@link SmallTableAliasMethodDiscreteSampler}
     */
    private static boolean isSmallPowerOf2(int n) {
        return n <= MAX_SMALL_POWER_2_SIZE && (n & (n - 1)) == 0;
    }
}
