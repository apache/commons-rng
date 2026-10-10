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

import java.math.BigDecimal;
import java.math.MathContext;
import java.time.Duration;
import java.util.Arrays;
import java.util.function.Supplier;
import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.stat.correlation.Covariance;
import org.apache.commons.math3.stat.descriptive.moment.Mean;
import org.apache.commons.math3.stat.inference.ChiSquareTest;
import org.apache.commons.rng.UniformRandomProvider;
import org.apache.commons.rng.core.source64.SplitMix64;
import org.apache.commons.rng.sampling.RandomAssert;
import org.apache.commons.rng.sampling.distribution.DirichletSampler.Normalizer;
import org.apache.commons.rng.simple.RandomSource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Test for {@link DirichletSampler}.
 */
class DirichletSamplerTest {
    /** Threshold for tiny alpha.
     * Set where log1p(alpha/Math.E) + log(u) == log(u) for u in [0, 1). */
    private static final double TINY_ALPHA = 0x1p-106;

    @Test
    void testRealNormalizer() {
        final Normalizer normalizer = Normalizer.REAL;
        // Fail on all zeros
        assertNormalizer(normalizer, new double[] {0, 0, 0}, null);
        // Fail when sum is infinite
        assertNormalizer(normalizer, new double[] {Double.POSITIVE_INFINITY, 1, 1}, null);
        assertNormalizer(normalizer, new double[] {Double.MAX_VALUE, Double.MAX_VALUE}, null);
        // A sub-normal sum cannot divide using multiplication of the reciprocal
        assertNormalizer(normalizer, new double[] {Double.MIN_VALUE, Double.MIN_VALUE}, null);
        assertNormalizer(normalizer, new double[] {0, Double.MIN_VALUE}, null);
        assertNormalizer(normalizer, new double[] {0x1p-1025, 0x1p-1025}, null);
        // Normalize to unit length
        assertNormalizer(normalizer, new double[] {1, 1}, new double[] {0.5, 0.5});
        assertNormalizer(normalizer, new double[] {1, 2, 3, 4}, new double[] {1.0 / 10, 2.0 / 10, 3.0 / 10, 4.0 / 10});
        assertNormalizer(normalizer, new double[] {0, Double.MAX_VALUE}, new double[] {0, 1});
        assertNormalizer(normalizer, new double[] {0, Double.MIN_NORMAL}, new double[] {0, 1});
    }

    @Test
    void testLogNormalizer() {
        final Normalizer normalizer = Normalizer.LOG;
        final double inf = Double.POSITIVE_INFINITY;
        // Infinite sample
        assertNormalizer(normalizer, new double[] {inf, 1, 1}, new double[] {1, 0, 0});
        assertNormalizer(normalizer, new double[] {inf, 1, inf}, new double[] {0.5, 0, 0.5});
        // Finite max normalises even with -infinity
        assertNormalizer(normalizer, new double[] {-inf, Math.log(Double.MIN_VALUE)}, new double[] {0, 1});
        // Sum of exp(x_i) overflows
        assertNormalizer(normalizer, new double[] {Double.MAX_VALUE, Double.MAX_VALUE}, new double[] {0.5, 0.5});
        // Sum of exp(x_i) underflows
        assertNormalizer(normalizer, new double[] {-Double.MAX_VALUE, -Double.MAX_VALUE}, new double[] {0.5, 0.5});
        // Normalize to unit length
        // Equal log values
        final double[] e1 = {1.0 / 3, 1.0 / 3, 1.0 / 3};
        assertNormalizer(normalizer, new double[] {0, 0, 0}, e1);
        assertNormalizer(normalizer, new double[] {42, 42, 42}, e1);
        assertNormalizer(normalizer, new double[] {-13, -13, -13}, e1);
        // Scale independent normalisation of finite real values
        final double[] e2 = {Math.exp(-1), Math.exp(-2), Math.exp(-3)};
        Normalizer.REAL.normalize(e2);
        assertNormalizer(normalizer, new double[] {-1, -2, -3}, e2);
        assertNormalizer(normalizer, new double[] {-11, -12, -13}, e2);
        assertNormalizer(normalizer, new double[] {-10001, -10002, -10003}, e2);
        assertNormalizer(normalizer, new double[] {10003, 10002, 10001}, e2);
        // Finite max normalises even with -infinity
        final double[] e3 = {Math.exp(-1), Math.exp(-2), Math.exp(-inf)};
        Normalizer.REAL.normalize(e3);
        assertNormalizer(normalizer, new double[] {-1, -2, -inf}, e3);

        // Side effect: NaN is considered the maximum sample.
        final double nan = Double.NaN;
        assertNormalizer(normalizer, new double[] {nan, 1, 1}, new double[] {1, 0, 0});
        assertNormalizer(normalizer, new double[] {nan, 1, nan}, new double[] {0.5, 0, 0.5});
        assertNormalizer(normalizer, new double[] {inf, nan}, new double[] {0, 1});
    }

    @Test
    void testScaledLogNormalizer() {
        final Normalizer normalizer = Normalizer.SCALED_LOG;
        final double inf = Double.POSITIVE_INFINITY;
        // Infinite sample
        assertNormalizer(normalizer, new double[] {inf, 1, 1}, new double[] {1, 0, 0});
        assertNormalizer(normalizer, new double[] {inf, 1, inf}, new double[] {0.5, 0, 0.5});
        // Finite max normalises even with -infinity
        assertNormalizer(normalizer, new double[] {-inf, Math.log(Double.MIN_VALUE)}, new double[] {0, 1});
        // Sum of exp(x_i) overflows
        assertNormalizer(normalizer, new double[] {Double.MAX_VALUE, Double.MAX_VALUE}, new double[] {0.5, 0.5});
        // Sum of exp(x_i) underflows
        assertNormalizer(normalizer, new double[] {-Double.MAX_VALUE, -Double.MAX_VALUE}, new double[] {0.5, 0.5});
        // Normalize to unit length
        // Equal log values
        final double[] e1 = {1.0 / 3, 1.0 / 3, 1.0 / 3};
        assertNormalizer(normalizer, new double[] {0, 0, 0}, e1);
        assertNormalizer(normalizer, new double[] {42, 42, 42}, e1);
        assertNormalizer(normalizer, new double[] {-13, -13, -13}, e1);
        // Scale independent normalisation of finite real values.
        // ---
        // Log values are scaled by 2^56 so we have to downscale the provided sample.
        final double downscale = 0x1p-56;
        // ---
        final double[] e2 = {Math.exp(-1), Math.exp(-2), Math.exp(-3)};
        Normalizer.REAL.normalize(e2);
        assertNormalizer(normalizer, new double[] {-1 * downscale, -2 * downscale, -3 * downscale}, e2);
        assertNormalizer(normalizer, new double[] {-4 * downscale, -5 * downscale, -6 * downscale}, e2);
        assertNormalizer(normalizer, new double[] {-400 * downscale, -401 * downscale, -402 * downscale}, e2);
        // Finite max normalises even with -infinity
        final double[] e3 = {Math.exp(-1), Math.exp(-2), Math.exp(-inf)};
        Normalizer.REAL.normalize(e3);
        assertNormalizer(normalizer, new double[] {-1 * downscale, -2 * downscale, -inf}, e3);

        // Side effect: NaN is considered the maximum sample.
        final double nan = Double.NaN;
        assertNormalizer(normalizer, new double[] {nan, 1, 1}, new double[] {1, 0, 0});
        assertNormalizer(normalizer, new double[] {nan, 1, nan}, new double[] {0.5, 0, 0.5});
        assertNormalizer(normalizer, new double[] {inf, nan}, new double[] {0, 1});
    }

    private static void assertNormalizer(Normalizer normalizer, double[] x, double[] expected) {
        final double[] y = x.clone();
        final boolean result = normalizer.normalize(x);
        final Supplier<String> msg = () -> Arrays.toString(y) + " -> " + Arrays.toString(x);
        if (expected == null) {
            Assertions.assertFalse(result, msg);
            Assertions.assertArrayEquals(y, x, msg);
        } else {
            Assertions.assertTrue(result, msg);
            final double sum = Arrays.stream(expected).sum();
            Assertions.assertEquals(1, sum, 0x1p-52, "Expected sum should be 1");
            // Normalization may not be exact
            for (int i = 0; i < x.length; i++) {
                Assertions.assertEquals(expected[i], x[i], Math.ulp(expected[i]), msg);
            }
        }
    }

    @Test
    void testDistributionThrowsWithInvalidNumberOfCategories() {
        final UniformRandomProvider rng = RandomAssert.seededRNG();
        Assertions.assertThrows(IllegalArgumentException.class,
            () -> DirichletSampler.of(rng, 1.0));
    }

    @Test
    void testDistributionThrowsWithZeroConcentration() {
        final UniformRandomProvider rng = RandomAssert.seededRNG();
        Assertions.assertThrows(IllegalArgumentException.class,
            () -> DirichletSampler.of(rng, 1.0, 0.0));
    }

    @Test
    void testDistributionThrowsWithNaNConcentration() {
        final UniformRandomProvider rng = RandomAssert.seededRNG();
        Assertions.assertThrows(IllegalArgumentException.class,
            () -> DirichletSampler.of(rng, 1.0, Double.NaN));
    }

    @Test
    void testDistributionThrowsWithInfiniteConcentration() {
        final UniformRandomProvider rng = RandomAssert.seededRNG();
        Assertions.assertThrows(IllegalArgumentException.class,
            () -> DirichletSampler.of(rng, 1.0, Double.POSITIVE_INFINITY));
    }

    @Test
    void testSymmetricDistributionThrowsWithInvalidNumberOfCategories() {
        final UniformRandomProvider rng = RandomAssert.seededRNG();
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> DirichletSampler.symmetric(rng, 1, 1.0));
    }

    @Test
    void testSymmetricDistributionThrowsWithZeroConcentration() {
        final UniformRandomProvider rng = RandomAssert.seededRNG();
        Assertions.assertThrows(IllegalArgumentException.class,
            () -> DirichletSampler.symmetric(rng, 2, 0.0));
    }

    @Test
    void testSymmetricDistributionThrowsWithNaNConcentration() {
        final UniformRandomProvider rng = RandomAssert.seededRNG();
        Assertions.assertThrows(IllegalArgumentException.class,
            () -> DirichletSampler.symmetric(rng, 2, Double.NaN));
    }

    @Test
    void testSymmetricDistributionThrowsWithInfiniteConcentration() {
        final UniformRandomProvider rng = RandomAssert.seededRNG();
        Assertions.assertThrows(IllegalArgumentException.class,
            () -> DirichletSampler.symmetric(rng, 2, Double.POSITIVE_INFINITY));
    }

    /**
     * Create condition so that all samples are zero and it is impossible to normalise the
     * samples to sum to 1. These should be ignored and the sample is repeated until
     * normalisation is possible.
     */
    @Test
    void testInvalidSampleIsIgnored() {
        // An RNG implementation which should create zero samples from the underlying
        // exponential sampler for an initial sequence.
        final UniformRandomProvider rng = new SplitMix64(0L) {
            private int i;

            @Override
            public long next() {
                return i++ < 10 ? 0L : super.next();
            }
        };

        // Alpha=1 will use an exponential sampler
        final DirichletSampler sampler = DirichletSampler.symmetric(rng, 2, 1.0);
        assertSample(2, sampler.sample());
    }

    /**
     * Test the assumption that a scaled alpha parameter to a log gamma sampler will
     * return the scaled samples. This works when alpha is very small as the sampler
     * creates samples using log(p) / alpha == scale * log(p) / (scale * alpha).
     */
    @Test
    void testScalingAssumptions() {
        // The threshold where log1p(alpha/E) + log(u) == log(u)
        // The smallest magnitude for log(u) in [0, 1) is log(1-2^-53) = -1.1102230246251565E-16.
        final double l = Math.log(Math.nextDown(1.0));
        Assertions.assertEquals(l, Math.log1p(TINY_ALPHA / Math.E) + l);

        final RandomSource randomSource = RandomSource.XO_RO_SHI_RO_128_PP;
        final byte[] seed = randomSource.createSeed();
        final UniformRandomProvider rng = randomSource.create(seed);
        final double scale = 0x1p53;
        final double tiny = TINY_ALPHA / scale;
        for (int i = 0; i < 10; i++) {
            final double alpha1 = rng.nextDouble() * tiny;
            final double alpha2 = alpha1 * scale;
            final UniformRandomProvider rng1 = randomSource.create(seed);
            final UniformRandomProvider rng2 = randomSource.create(seed);
            // log samples
            final SharedStateContinuousSampler sampler1 = AhrensDieterMarsagliaTsangGammaSampler.of(rng1, alpha1, 1, true);
            final SharedStateContinuousSampler sampler2 = AhrensDieterMarsagliaTsangGammaSampler.of(rng2, alpha2, 1, true);
            RandomAssert.assertProduceSameSequence(sampler1, () -> sampler2.sample() * scale);
        }
    }

    /**
     * Test extreme concentration parameters.
     * This tests concentration parameters that failed in a previous version
     * due to a sum of gamma samples that was either zero or infinity. This could
     * not be used to normalise the sum to 1.
     * Critical thresholds were 1e-19 and 2^1023 for unbounded recursion.
     * See RNG-195.
     */
    @Test
    void testExtremeConcentration() {
        // Thresholds to use log gamma samples are 2^-5 and 2^53.
        // The assertion checks the sum of the sample is 1.
        assertExtremeConcentration(false, 0, 1e-5, 1e-6, 1e-7);
        assertExtremeConcentration(false, 0, 1e18, 1e19, 1e20);
        assertExtremeConcentration(true, 3, 1e50);
        assertExtremeConcentration(true, 3, 1e-8);

        // Tiny alpha ~ 2^-106
        assertExtremeConcentration(false, 0, TINY_ALPHA / 1.5, TINY_ALPHA / 2, TINY_ALPHA, 2.5);
        assertExtremeConcentration(true, 2, Double.MIN_VALUE);
        // One dominant concentration
        assertExtremeConcentration(false, 0, TINY_ALPHA / 1e200, TINY_ALPHA / 1.5, TINY_ALPHA / 1e100);
        // Two dominant concentrations
        assertExtremeConcentration(false, 0, TINY_ALPHA / 1.5, TINY_ALPHA / 1.5, TINY_ALPHA / 1e100);

        // Large alpha
        assertExtremeConcentration(false, 0, 0x1p1022, 0x1p1023, 0x1.5p1023);
        assertExtremeConcentration(true, 2, Double.MAX_VALUE);

        // A large alpha and tiny alpha will create some log samples as -infinity.
        // Normalisation should handle this and this category will never be observed.
        assertExtremeConcentration(false, 0, 0x1p1023, TINY_ALPHA / 2);
        assertExtremeConcentration(false, 0, Double.MIN_VALUE, Double.MAX_VALUE);
    }

    private static void assertExtremeConcentration(boolean symmetric, int k, double... alpha) {
        final RandomSource randomSource = RandomSource.XO_RO_SHI_RO_128_PP;
        final byte[] seed = randomSource.createSeed();
        final UniformRandomProvider rng1 = randomSource.create(seed);
        final DirichletSampler sampler1;
        final double[] alphas;
        if (symmetric) {
            sampler1 = DirichletSampler.symmetric(rng1, k, alpha[0]);
            alphas = new double[k];
            Arrays.fill(alphas, alpha[0]);
        } else {
            sampler1 = DirichletSampler.of(rng1, alpha);
            alphas = alpha.clone();
        }

        // Test sample generation does not infinitely recurse.
        // 10 milliseconds is fine on a fast machine; add some headroom
        final int n = 100;
        final Duration timeout = Duration.ofMillis(100);
        final double[][][] samples = new double[1][][];

        Assertions.assertTimeoutPreemptively(timeout,
            () -> samples[0] = sampler1.samples(n).toArray(double[][]::new));
        final double[][] x = samples[0];
        Assertions.assertNotNull(x);

        if (symmetric) {
            // Check the general case matches
            final UniformRandomProvider rng2 = randomSource.create(seed);
            final DirichletSampler sampler2 = DirichletSampler.of(rng2, alphas);

            Assertions.assertTimeoutPreemptively(timeout,
                () -> samples[0] = sampler2.samples(n).toArray(double[][]::new));
            final double[][] y = samples[0];
            // Check nested arrays
            Assertions.assertArrayEquals(x, y);
        }

        // Checks on the sample. Correct length and sum to 1.
        for (int i = 0; i < n; i++) {
            assertSample(alphas.length, x[i]);
        }

        // Extreme concentrations have predictable output
        final double minAlpha = Arrays.stream(alpha).min().getAsDouble();
        final double maxAlpha = Arrays.stream(alpha).max().getAsDouble();

        // If alpha is above 1e33 then the probability of a gamma sample p(x=alpha) = 1.
        // Check if the samples are all the same. The samples should be the normalised
        // log alpha values.
        if (minAlpha > 1e33) {
            final double[] expected = Arrays.stream(alphas).map(Math::log).toArray();
            Normalizer.LOG.normalize(expected);
            for (int i = 0; i < n; i++) {
                Assertions.assertArrayEquals(expected, x[i], "All samples should be the same");
            }
        }

        // If alpha is below 2^-53 then the log gamma samples are log(u) / alpha.
        // If alpha is all the same then the samples are expected to be 1.0 in a single
        // category and 0.0 elsewhere (assuming u output from the RNG is not identical
        // within a sample).
        // If the alpha magnitude difference is very large across categories some categories
        // will never be observed.
        if (maxAlpha < TINY_ALPHA) {
            // Count observed categories
            final long[] observed = new long[alphas.length];
            if (symmetric) {
                // A symmetric sample for a tiny concentration is to pick one of the
                // categories as 1.0 and all others 0.0.
                // Note a previous check ensured the sum was 1.0. Here we test for
                // categories being 1.0 and all others zero, or allow a rare case of 0.5
                // in two categories (chance of 2^-53 for two p output from the RNG to match).
                for (int i = 0; i < n; i++) {
                    final double[] s = x[i];
                    int cOne = 0;
                    int cHalf = 0;
                    int cZero = 0;
                    for (int j = 0; j < s.length; j++) {
                        if (Math.abs(s[j] - 1.0) < 1e15) {
                            cOne++;
                            observed[j]++;
                        } else if (Math.abs(s[j] - 0.5) < 1e15) {
                            cHalf++;
                            observed[j]++;
                        } else if (Math.abs(s[j]) < 1e15) {
                            cZero++;
                            // not observed
                        }
                    }
                    // Expected 1.0, 0.5 or 0.0. Fail if anything else was present.
                    Assertions.assertEquals(s.length, cOne + cHalf + cZero,
                        () -> Arrays.toString(s));
                }
                // The observed should follow a uniform distribution across categories
                final ChiSquareTest chiSquareTest = new ChiSquareTest();
                final double[] expected = new double[alphas.length];
                Arrays.fill(expected, 1.0 / alphas.length);
                Assertions.assertFalse(chiSquareTest.chiSquareTest(expected, observed, 0.001),
                    () -> "Symetric case was not uniform: " + Arrays.toString(observed));
            } else {
                // Assume all alphas are different as the symmetric case was not used.
                // Flag any alpha not expected by comparison to max alpha.
                // Identify the case where for all p in [0, 1):
                //   log(p1) / alpha1 > log(p2) / alpha2
                //   log(p1) / log(p2) > alpha1 / alpha2
                // Extreme finite sample when p1 = 2^-53 and p2 = 1-2^-53:
                // -36.74 / -1.11e-16 = 3.31e17
                // If an alpha is much smaller than the max then it should not be observed
                // assuming p != 0 for the dominant concentration.
                // This test can fail by chance so check the allowed category across all samples.
                final int[] allowed = Arrays.stream(alphas).mapToInt(a -> maxAlpha > a * 4e17 ? 0 : 1).toArray();
                for (int i = 0; i < n; i++) {
                    final double[] s = x[i];
                    for (int j = 0; j < s.length; j++) {
                        if (s[j] != 0) {
                            observed[j]++;
                        }
                    }
                }
                // Check the allowed categories
                for (int j = 0; j < alphas.length; j++) {
                    // The disallowed category can occur by chance (frequency 2^-53 when the max
                    // alpha sample had p=0 from the RNG). This is very unlikely multiple times.
                    if (allowed[j] == 0 && observed[j] > 1) {
                        Assertions.fail(String.format("%s : Unexpected observation for tiny alpha %s",
                            Arrays.toString(alphas), Arrays.toString(observed)));
                    }
                }
            }
        }
    }

    @Test
    void testSharedStateSampler() {
        assertSharedStateSampler(1, 2, 3);
        // Tiny alpha
        assertSharedStateSampler(TINY_ALPHA / 1.5, TINY_ALPHA / 2, TINY_ALPHA / 2.5);
        // Large alpha
        assertSharedStateSampler(1e100, 2e100, 3e100);
        // Small alpha (requires log samples) with a category with alpha=1
        // that can use an exponential sampler
        assertSharedStateSampler(1, 1e-5, 1e-3);
    }

    private static void assertSharedStateSampler(double... alpha) {
        final RandomSource randomSource = RandomSource.XO_RO_SHI_RO_128_PP;
        final byte[] seed = randomSource.createSeed();
        final UniformRandomProvider rng1 = randomSource.create(seed);
        final UniformRandomProvider rng2 = randomSource.create(seed);
        final DirichletSampler sampler1 = DirichletSampler.of(rng1, alpha);
        final DirichletSampler sampler2 = sampler1.withUniformRandomProvider(rng2);
        RandomAssert.assertProduceSameSequence(sampler1, sampler2);
    }

    @Test
    void testSharedStateSamplerForSymmetricCase() {
        assertSharedStateSamplerForSymmetricCase(2, 1.5);
        // Tiny alpha
        assertSharedStateSamplerForSymmetricCase(3, TINY_ALPHA / 1.5);
        // Large alpha
        assertSharedStateSamplerForSymmetricCase(2, 2.5e50);
        // Exponential sampler
        assertSharedStateSamplerForSymmetricCase(4, 1);
    }

    private static void assertSharedStateSamplerForSymmetricCase(int k, double alpha) {
        final RandomSource randomSource = RandomSource.XO_RO_SHI_RO_128_PP;
        final byte[] seed = randomSource.createSeed();
        final UniformRandomProvider rng1 = randomSource.create(seed);
        final UniformRandomProvider rng2 = randomSource.create(seed);
        final DirichletSampler sampler1 = DirichletSampler.symmetric(rng1, k, alpha);
        final DirichletSampler sampler2 = sampler1.withUniformRandomProvider(rng2);
        RandomAssert.assertProduceSameSequence(sampler1, sampler2);
    }

    @Test
    void testSymmetricCaseMatchesGeneralCase() {
        assertSymmetricCaseMatchesGeneralCase(3, 0.5);
        assertSymmetricCaseMatchesGeneralCase(3, 1.0);
        assertSymmetricCaseMatchesGeneralCase(3, 1.5);
        // Tiny alpha
        assertSymmetricCaseMatchesGeneralCase(4, TINY_ALPHA / 1.5);
        // Large alpha
        assertSymmetricCaseMatchesGeneralCase(2, 42e50);
    }

    private static void assertSymmetricCaseMatchesGeneralCase(int k, double alpha) {
        final RandomSource randomSource = RandomSource.XO_RO_SHI_RO_128_PP;
        final byte[] seed = randomSource.createSeed();
        final UniformRandomProvider rng1 = randomSource.create(seed);
        final UniformRandomProvider rng2 = randomSource.create(seed);
        final double[] alphas = new double[k];
        Arrays.fill(alphas, alpha);
        final DirichletSampler sampler1 = DirichletSampler.symmetric(rng1, k, alpha);
        final DirichletSampler sampler2 = DirichletSampler.of(rng2, alphas);
        RandomAssert.assertProduceSameSequence(sampler1, sampler2);
    }

    /**
     * Test the toString method. This is added to ensure coverage.
     */
    @Test
    void testToString() {
        final UniformRandomProvider rng = RandomAssert.seededRNG();
        final DirichletSampler sampler1 = DirichletSampler.symmetric(rng, 2, 1.0);
        final DirichletSampler sampler2 = DirichletSampler.of(rng, 0.5, 1, 1.5);
        Assertions.assertTrue(sampler1.toString().toLowerCase().contains("dirichlet"));
        Assertions.assertTrue(sampler2.toString().toLowerCase().contains("dirichlet"));
    }

    @Test
    void testSampling1() {
        assertSamples(1, 2, 3);
    }

    @Test
    void testSampling2() {
        assertSamples(1, 1, 1);
    }

    @Test
    void testSampling3() {
        assertSamples(0.5, 1, 1.5);
    }

    @Test
    void testSampling4() {
        assertSamples(1, 3);
    }

    @Test
    void testSampling5() {
        assertSamples(1, 2, 3, 4);
    }

    @Test
    void testSampling6() {
        assertSamples(1000, 2000, 3000);
    }

    @Test
    void testSampling7() {
        // Log samples are used when max alpha < 2^-5 or > 2^53
        assertSamples(0x1p-6, 0x2p-6, 0x3p-6);
    }

    @Test
    void testSampling8() {
        // Log samples are used when max alpha < 2^-5 or > 2^53
        assertSamples(0x1p53, 0x2p53, 0x3p53);
    }

    @Test
    void testSampling9() {
        // tiny alpha.
        // BigDecimal handles the small values to compute the mean and variance.
        assertSamples(TINY_ALPHA / 1.5, TINY_ALPHA / 2.5, TINY_ALPHA / 2);
    }

    /**
     * Assert samples from the distribution. The variates are tested against the expected
     * mean and covariance for the given concentration parameters.
     *
     * @param alpha Concentration parameters.
     */
    private static void assertSamples(double... alpha) {
        // No fixed seed. Failed tests will be repeated by the JUnit test runner.
        final UniformRandomProvider rng = RandomAssert.createRNG();
        final DirichletSampler sampler = DirichletSampler.of(rng, alpha);
        final int k = alpha.length;
        final double[][] samples = new double[100000][];
        for (int i = 0; i < samples.length; i++) {
            final double[] x = sampler.sample();
            assertSample(k, x);
            samples[i] = x;
        }

        // Computation of moments:
        // https://en.wikipedia.org/wiki/Dirichlet_distribution#Moments
        // Perform in extended precision to handle extreme concentration parameters.
        // 20 digits of precision when dividing is enough for a double result.
        final MathContext mc = new MathContext(20);

        // Compute the sum of the concentration parameters: alpha0
        BigDecimal alpha0 = BigDecimal.ZERO;
        for (int i = 0; i < k; i++) {
            alpha0 = alpha0.add(bd(alpha[i]));
        }

        // Use a moderate tolerance.
        // Differences are usually observed in the 3rd significant figure.
        // When the alpha is large the covariance is very low and computation of
        // actual may be inaccurate. Here differences are in the 1st or
        // 2nd significant figure.
        final double meanRelativeTolerance = 5e-2;
        final double varRelativeTolerance = alpha0.doubleValue() > 0x1p53 ? 0.1 : 5e-2;

        // Mean = alpha[i] / alpha0
        final double[] means = getColumnMeans(samples);
        for (int i = 0; i < k; i++) {
            final double mean = bd(alpha[i]).divide(alpha0, mc).doubleValue();
            Assertions.assertEquals(mean, means[i], mean * meanRelativeTolerance, "Mean");
        }

        // Variance = alpha_i (alpha_0 - alpha_i) / alpha_0^2 (alpha_0 + 1)
        // Covariance = -alpha_i * alpha_j / alpha_0^2 (alpha_0 + 1)
        final double[][] covars = getCovariance(samples);
        final BigDecimal denom = alpha0.pow(2).multiply(alpha0.add(BigDecimal.ONE));
        for (int i = 0; i < k; i++) {
            final double variance = bd(alpha[i]).multiply(alpha0.subtract(bd(alpha[i])))
                .divide(denom, mc).doubleValue();
            Assertions.assertEquals(variance, covars[i][i], variance * varRelativeTolerance, "Variance");
            for (int j = i + 1; j < k; j++) {
                final double covar = bd(-alpha[i]).multiply(bd(alpha[j])).divide(denom, mc).doubleValue();
                Assertions.assertEquals(covar, covars[i][j], Math.abs(covar) * varRelativeTolerance, "Covariance");
            }
        }
    }

    /**
     * Create a BigDecimal from the double value.
     *
     * @param x Value.
     * @return the BigDecimal
     */
    private static BigDecimal bd(double x) {
        return new BigDecimal(x);
    }

    /**
     * Assert the sample has the correct length and sums to 1.
     *
     * @param k Expected number of categories.
     * @param x Sample.
     */
    private static void assertSample(int k, double[] x) {
        Assertions.assertEquals(k, x.length, "Number of categories");
        // There are always at least 2 categories
        double sum = x[0] + x[1];
        // Sum the rest
        for (int i = 2; i < x.length; i++) {
            sum += x[i];
        }
        Assertions.assertEquals(1.0, sum, 2 * Math.ulp(1.0), "Invalid sum");
    }

    /**
     * Gets the column means. This is done using the same method as the means in the
     * Apache Commons Math Covariance class by using the Mean class.
     *
     * @param data the data
     * @return the column means
     */
    private static double[] getColumnMeans(double[][] data) {
        final Array2DRowRealMatrix m = new Array2DRowRealMatrix(data, false);
        final Mean mean = new Mean();
        final double[] means = new double[m.getColumnDimension()];
        for (int i = 0; i < means.length; i++) {
            means[i] = mean.evaluate(m.getColumn(i));
        }
        return means;
    }

    /**
     * Gets the covariance.
     *
     * @param data the data
     * @return the covariance
     */
    private static double[][] getCovariance(double[][] data) {
        final Array2DRowRealMatrix m = new Array2DRowRealMatrix(data, false);
        return new Covariance(m).getCovarianceMatrix().getData();
    }
}
