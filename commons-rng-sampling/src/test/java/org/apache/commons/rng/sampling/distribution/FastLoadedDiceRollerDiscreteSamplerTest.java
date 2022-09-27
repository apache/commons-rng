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

import java.util.Arrays;
import java.util.function.DoubleUnaryOperator;
import java.util.stream.Stream;
import org.apache.commons.math3.stat.descriptive.moment.Mean;
import org.apache.commons.math3.stat.inference.ChiSquareTest;
import org.apache.commons.rng.UniformRandomProvider;
import org.apache.commons.rng.sampling.RandomAssert;
import org.apache.commons.rng.simple.RandomSource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Test for the {@link FastLoadedDiceRollerDiscreteSampler}.
 */
class FastLoadedDiceRollerDiscreteSamplerTest {
    /**
     * Creates the sampler.
     *
     * @param frequencies Observed frequencies.
     * @return the FLDR sampler
     */
    private static SharedStateDiscreteSampler createSampler(long... frequencies) {
        final UniformRandomProvider rng = RandomAssert.createRNG();
        return FastLoadedDiceRollerDiscreteSampler.of(rng, frequencies);
    }

    /**
     * Creates the sampler.
     *
     * @param weights Weights.
     * @return the FLDR sampler
     */
    private static SharedStateDiscreteSampler createSampler(double... weights) {
        final UniformRandomProvider rng = RandomAssert.createRNG();
        return FastLoadedDiceRollerDiscreteSampler.of(rng, weights);
    }

    /**
     * Return a stream of invalid frequencies for a discrete distribution.
     *
     * @return the stream of invalid frequencies
     */
    static Stream<long[]> testFactoryConstructorFrequencies() {
        return Stream.of(
            // Null or empty
            (long[]) null,
            new long[0],
            // Negative
            new long[] {-1, 2, 3},
            new long[] {1, -2, 3},
            new long[] {1, 2, -3},
            // Overflow of sum
            new long[] {Long.MAX_VALUE, Long.MAX_VALUE},
            // x+x+2 == 0
            new long[] {Long.MAX_VALUE, Long.MAX_VALUE, 2},
            // x+x+x == x - 2 (i.e. positive)
            new long[] {Long.MAX_VALUE, Long.MAX_VALUE, Long.MAX_VALUE},
            // Zero sum
            new long[1],
            new long[4]
        );
    }

    @ParameterizedTest
    @MethodSource
    void testFactoryConstructorFrequencies(long[] frequencies) {
        Assertions.assertThrows(IllegalArgumentException.class, () -> createSampler(frequencies));
    }

    /**
     * Return a stream of invalid weights for a discrete distribution.
     *
     * @return the stream of invalid weights
     */
    static Stream<double[]> testFactoryConstructorWeights() {
        return Stream.of(
            // Null or empty
            (double[]) null,
            new double[0],
            // Negative, infinite or NaN
            new double[] {-1, 2, 3},
            new double[] {1, -2, 3},
            new double[] {1, 2, -3},
            new double[] {Double.POSITIVE_INFINITY, 2, 3},
            new double[] {1, Double.POSITIVE_INFINITY, 3},
            new double[] {1, 2, Double.POSITIVE_INFINITY},
            new double[] {Double.NaN, 2, 3},
            new double[] {1, Double.NaN, 3},
            new double[] {1, 2, Double.NaN},
            // Zero sum
            new double[1],
            new double[4]
        );
    }

    @ParameterizedTest
    @MethodSource
    void testFactoryConstructorWeights(double[] weights) {
        Assertions.assertThrows(IllegalArgumentException.class, () -> createSampler(weights));
    }

    @Test
    void testToString() {
        for (final long[] observed : new long[][] {{42}, {1, 2, 3}}) {
            final SharedStateDiscreteSampler sampler = createSampler(observed);
            Assertions.assertTrue(sampler.toString().toLowerCase().contains("fast loaded dice roller"));
        }
    }

    @Test
    void testSingleCategory() {
        final int n = 13;
        final int[] expected = new int[n];
        Assertions.assertArrayEquals(expected, createSampler(42).samples(n).toArray());
        Assertions.assertArrayEquals(expected, createSampler(0.55).samples(n).toArray());
    }

    @Test
    void testSingleFrequency() {
        final long[] frequencies = new long[5];
        final int category = 2;
        frequencies[category] = 1;
        final SharedStateDiscreteSampler sampler = createSampler(frequencies);
        final int n = 7;
        final int[] expected = new int[n];
        Arrays.fill(expected, category);
        Assertions.assertArrayEquals(expected, sampler.samples(n).toArray());
    }

    @Test
    void testSingleWeight() {
        final double[] weights = new double[5];
        final int category = 3;
        weights[category] = 1.5;
        final SharedStateDiscreteSampler sampler = createSampler(weights);
        final int n = 6;
        final int[] expected = new int[n];
        Arrays.fill(expected, category);
        Assertions.assertArrayEquals(expected, sampler.samples(n).toArray());
    }

    @Test
    void testIndexOfNonZero() {
        Assertions.assertThrows(IllegalStateException.class,
            () -> FastLoadedDiceRollerDiscreteSampler.indexOfNonZero(new long[3]));
        final long[] data = new long[3];
        for (int i = 0; i < data.length; i++) {
            data[i] = 13;
            Assertions.assertEquals(i, FastLoadedDiceRollerDiscreteSampler.indexOfNonZero(data));
            data[i] = 0;
        }
    }

    @ParameterizedTest
    @ValueSource(longs = {0, 1, -1, Integer.MAX_VALUE, 1L << 34})
    void testCheckArraySize(long size) {
        // This is the same value as the sampler
        final int max = Integer.MAX_VALUE - 8;
        // Note: The method does not test for negatives.
        // This is not required when validating a positive int multiplied by another positive int.
        if (size > max) {
            Assertions.assertThrows(IllegalArgumentException.class,
                () -> FastLoadedDiceRollerDiscreteSampler.checkArraySize(size));
        } else {
            Assertions.assertEquals((int) size, FastLoadedDiceRollerDiscreteSampler.checkArraySize(size));
        }
    }

    /**
     * Return a stream of expected frequencies for a discrete distribution.
     *
     * @return the stream of expected frequencies
     */
    static Stream<long[]> testSamplesFrequencies() {
        return Stream.of(
            // Single category
            new long[] {0, 0, 42, 0, 0},
            // Sum to a power of 2
            new long[] {1, 1, 2, 3, 1},
            new long[] {0, 1, 1, 0, 2, 3, 1, 0},
            // Do not sum to a power of 2
            new long[] {1, 2, 3, 1, 3},
            new long[] {1, 0, 2, 0, 3, 1, 3},
            // Large frequencies
            new long[] {5126734627834L, 213267384684832L, 126781236718L, 71289979621378L}
        );
    }

    /**
     * Check the distribution of samples match the expected probabilities.
     *
     * @param expectedFrequencies Expected frequencies.
     */
    @ParameterizedTest
    @MethodSource
    void testSamplesFrequencies(long[] expectedFrequencies) {
        final SharedStateDiscreteSampler sampler = createSampler(expectedFrequencies);
        final int numberOfSamples = 10000;
        final long[] samples = new long[expectedFrequencies.length];
        sampler.samples(numberOfSamples).forEach(x -> samples[x]++);

        // Handle a test with some zero-probability observations by mapping them out
        int mapSize = 0;
        double sum = 0;
        for (final double f : expectedFrequencies) {
            if (f != 0) {
                mapSize++;
                sum += f;
            }
        }

        // Single category will break the Chi-square test
        if (mapSize == 1) {
            int index = 0;
            while (index < expectedFrequencies.length) {
                if (expectedFrequencies[index] != 0) {
                    break;
                }
                index++;
            }
            Assertions.assertEquals(numberOfSamples, samples[index], "Invalid single category samples");
            return;
        }

        final double[] expected = new double[mapSize];
        final long[] observed = new long[mapSize];
        for (int i = 0; i < expectedFrequencies.length; i++) {
            if (expectedFrequencies[i] != 0) {
                --mapSize;
                expected[mapSize] = expectedFrequencies[i] / sum;
                observed[mapSize] = samples[i];
            } else {
                Assertions.assertEquals(0, samples[i], "No samples expected from zero probability");
            }
        }

        final ChiSquareTest chiSquareTest = new ChiSquareTest();
        // Pass if we cannot reject null hypothesis that the distributions are the same.
        Assertions.assertFalse(chiSquareTest.chiSquareTest(expected, observed, 0.001));
    }

    /**
     * Return a stream of expected weights for a discrete distribution.
     *
     * @return the stream of expected weights
     */
    static Stream<double[]> testSamplesWeights() {
        return Stream.of(
            // Single category
            new double[] {0, 0, 0.523, 0, 0},
            // Sum to a power of 2
            new double[] {0.125, 0.125, 0.25, 0.375, 0.125},
            new double[] {0, 0.125, 0.125, 0.25, 0, 0.375, 0.125, 0},
            // Do not sum to a power of 2
            new double[] {0.1, 0.2, 0.3, 0.1, 0.3},
            new double[] {0.1, 0, 0.2, 0, 0.3, 0.1, 0.3},
            // Sub-normal numbers
            new double[] {5 * Double.MIN_NORMAL, 2 * Double.MIN_NORMAL, 3 * Double.MIN_NORMAL, 9 * Double.MIN_NORMAL},
            new double[] {2 * Double.MIN_NORMAL, Double.MIN_NORMAL, 0.5 * Double.MIN_NORMAL, 0.75 * Double.MIN_NORMAL},
            new double[] {Double.MIN_VALUE, 2 * Double.MIN_VALUE, 3 * Double.MIN_VALUE, 7 * Double.MIN_VALUE},
            // Large range of magnitude
            new double[] {1.0, 2.0, Math.scalb(3.0, -32), Math.scalb(4.0, -65), 5.0},
            new double[] {Math.scalb(1.0, 35), Math.scalb(2.0, 35), Math.scalb(3.0, -32), Math.scalb(4.0, -65), Math.scalb(5.0, 35)},
            // Sum to infinite
            new double[] {Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE / 2, Double.MAX_VALUE / 4}
        );
    }

    /**
     * Check the distribution of samples match the expected weights.
     *
     * @param weights Category weights.
     */
    @ParameterizedTest
    @MethodSource
    void testSamplesWeights(double[] weights) {
        final SharedStateDiscreteSampler sampler = createSampler(weights);
        final int numberOfSamples = 10000;
        final long[] samples = new long[weights.length];
        sampler.samples(numberOfSamples).forEach(x -> samples[x]++);

        // Handle a test with some zero-probability observations by mapping them out
        int mapSize = 0;
        double sum = 0;
        // Handle infinite sum using a rolling mean for normalisation
        final Mean mean = new Mean();
        for (final double w : weights) {
            if (w != 0) {
                mapSize++;
                sum += w;
                mean.increment(w);
            }
        }

        // Single category will break the Chi-square test
        if (mapSize == 1) {
            int index = 0;
            while (index < weights.length) {
                if (weights[index] != 0) {
                    break;
                }
                index++;
            }
            Assertions.assertEquals(numberOfSamples, samples[index], "Invalid single category samples");
            return;
        }

        final double mu = mean.getResult();
        final int n = mapSize;
        final double s = sum;
        final DoubleUnaryOperator normalise = Double.isInfinite(sum) ?
            x -> (x / mu) * n :
            x -> x / s;

        final double[] expected = new double[mapSize];
        final long[] observed = new long[mapSize];
        for (int i = 0; i < weights.length; i++) {
            if (weights[i] != 0) {
                --mapSize;
                expected[mapSize] = normalise.applyAsDouble(weights[i]);
                observed[mapSize] = samples[i];
            } else {
                Assertions.assertEquals(0, samples[i], "No samples expected from zero probability");
            }
        }

        final ChiSquareTest chiSquareTest = new ChiSquareTest();
        // Pass if we cannot reject null hypothesis that the distributions are the same.
        Assertions.assertFalse(chiSquareTest.chiSquareTest(expected, observed, 0.001));
    }

    /**
     * Return a stream of expected frequencies for a discrete distribution where the frequencies
     * can be converted to a {@code double} without loss of precision.
     *
     * @return the stream of expected frequencies
     */
    static Stream<long[]> testSamplesWeightsMatchesFrequencies() {
        // Reuse the same frequencies.
        // Those that cannot be converted to a double are ignored by the test.
        return testSamplesFrequencies();
    }

    /**
     * Check the distribution of samples when the frequencies can be converted to weights without
     * loss of precision.
     *
     * @param frequencies Expected frequencies.
     */
    @ParameterizedTest
    @MethodSource
    void testSamplesWeightsMatchesFrequencies(long[] frequencies) {
        final double[] weights = new double[frequencies.length];
        for (int i = 0; i < frequencies.length; i++) {
            final double w = frequencies[i];
            Assumptions.assumeTrue((long) w == frequencies[i]);
            // Ensure the exponent is set in the event of simple frequencies
            weights[i] = Math.scalb(w, -35);
        }
        final long seed = RandomSource.createLong();
        final UniformRandomProvider rng1 = RandomSource.SPLIT_MIX_64.create(seed);
        final UniformRandomProvider rng2 = RandomSource.SPLIT_MIX_64.create(seed);
        final SharedStateDiscreteSampler sampler1 =
            FastLoadedDiceRollerDiscreteSampler.of(rng1, frequencies);
        final SharedStateDiscreteSampler sampler2 =
            FastLoadedDiceRollerDiscreteSampler.of(rng2, weights);
        RandomAssert.assertProduceSameSequence(sampler1, sampler2);
    }

    /**
     * Test scaled weights. The sampler uses the relative magnitude of weights and the
     * output should be invariant to scaling. The weights are sampled from the 2^53 dyadic
     * rationals in [0, 1). A scale factor of -1021 is the lower limit if a weight is
     * 2^-53 to maintain a non-zero weight. The upper limit is 1023 if a weight is 1 to avoid
     * infinite values. Note that it does not matter if the sum of weights is infinite; only
     * the individual weights must be finite.
     *
     * @param scaleFactor the scale factor
     */
    @ParameterizedTest
    @ValueSource(ints = {1023, 67, 1, -59, -1020, -1021})
    void testScaledWeights(int scaleFactor) {
        // Weights in [0, 1)
        final double[] w1 = RandomAssert.createRNG().doubles(10).toArray();
        final double scale = Math.scalb(1.0, scaleFactor);
        final double[] w2 = Arrays.stream(w1).map(x -> x * scale).toArray();
        final long seed = RandomSource.createLong();
        final UniformRandomProvider rng1 = RandomSource.SPLIT_MIX_64.create(seed);
        final UniformRandomProvider rng2 = RandomSource.SPLIT_MIX_64.create(seed);
        final SharedStateDiscreteSampler sampler1 =
            FastLoadedDiceRollerDiscreteSampler.of(rng1, w1);
        final SharedStateDiscreteSampler sampler2 =
            FastLoadedDiceRollerDiscreteSampler.of(rng2, w2);
        RandomAssert.assertProduceSameSequence(sampler1, sampler2);
    }

    /**
     * Test the alpha parameter removes small relative weights.
     * Weights should be removed if they are {@code 2^alpha} smaller than the largest
     * weight.
     *
     * @param alpha Alpha parameter
     */
    @ParameterizedTest
    @ValueSource(ints = {13, 30, 53})
    void testAlphaRemovesWeights(int alpha) {
        // The small weight must be > 2^alpha smaller so scale by (alpha + 1)
        final double small = Math.scalb(1.0, -(alpha + 1));
        final double[] w1 = {1, 0.5, 0.5, 0};
        final double[] w2 = {1, 0.5, 0.5, small};
        final long seed = RandomSource.createLong();
        final UniformRandomProvider rng1 = RandomSource.SPLIT_MIX_64.create(seed);
        final UniformRandomProvider rng2 = RandomSource.SPLIT_MIX_64.create(seed);
        final UniformRandomProvider rng3 = RandomSource.SPLIT_MIX_64.create(seed);

        final int n = 10;
        final int[] s1 = FastLoadedDiceRollerDiscreteSampler.of(rng1, w1).samples(n).toArray();
        final int[] s2 = FastLoadedDiceRollerDiscreteSampler.of(rng2, w2, alpha).samples(n).toArray();
        final int[] s3 = FastLoadedDiceRollerDiscreteSampler.of(rng3, w2, alpha + 1).samples(n).toArray();

        Assertions.assertArrayEquals(s1, s2, "alpha parameter should ignore the small weight");
        Assertions.assertFalse(Arrays.equals(s1, s3), "alpha+1 parameter should not ignore the small weight");
    }

    static Stream<long[]> testSharedStateSampler() {
        return Stream.of(
            new long[] {42},
            new long[] {1, 1, 2, 3, 1}
        );
    }

    @ParameterizedTest
    @MethodSource
    void testSharedStateSampler(long[] frequencies) {
        final UniformRandomProvider rng1 = RandomSource.SPLIT_MIX_64.create(0L);
        final UniformRandomProvider rng2 = RandomSource.SPLIT_MIX_64.create(0L);
        final SharedStateDiscreteSampler sampler1 =
            FastLoadedDiceRollerDiscreteSampler.of(rng1, frequencies);
        final SharedStateDiscreteSampler sampler2 = sampler1.withUniformRandomProvider(rng2);
        RandomAssert.assertProduceSameSequence(sampler1, sampler2);
    }
}
