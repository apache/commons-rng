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
package org.apache.commons.rng.simple;

import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import org.apache.commons.rng.UniformRandomProvider;
import org.apache.commons.rng.JumpableUniformRandomProvider;
import org.apache.commons.rng.LongJumpableUniformRandomProvider;
import org.apache.commons.rng.RandomProviderState;
import org.apache.commons.rng.RestorableUniformRandomProvider;
import org.apache.commons.rng.core.RandomProviderDefaultState;
import org.apache.commons.rng.core.source64.SplitMix64;

/**
 * Tests which all generators must pass.
 */
class ProvidersCommonParametricTest {
    private static Iterable<ProvidersList.Data> getProvidersTestData() {
        return ProvidersList.list();
    }

    // Seeding tests.

    @ParameterizedTest
    @MethodSource("getProvidersTestData")
    void testUnsupportedSeedType(ProvidersList.Data data) {
        final byte seed = 123;
        Assertions.assertThrows(UnsupportedOperationException.class,
            () -> data.getSource().create(seed, data.getArgs()));
    }

    /**
     * Test the factory create method returns the same class as the instance create method.
     */
    @ParameterizedTest
    @MethodSource("getProvidersTestData")
    void testFactoryCreateMethod(ProvidersList.Data data) {
        final RandomSource originalSource = data.getSource();
        final Object originalSeed = data.getSeed();
        final Object[] originalArgs = data.getArgs();
        // Cannot test providers that require arguments
        Assumptions.assumeTrue(originalArgs == null);
        @SuppressWarnings("deprecation")
        final UniformRandomProvider rng = RandomSource.create(data.getSource());
        final UniformRandomProvider generator = originalSource.create(originalSeed, originalArgs);
        Assertions.assertEquals(generator.getClass(), rng.getClass());
    }

    /**
     * Test the factory create method returns the same class as the instance create method
     * and produces the same output.
     */
    @ParameterizedTest
    @MethodSource("getProvidersTestData")
    void testFactoryCreateMethodWithSeed(ProvidersList.Data data) {
        final RandomSource originalSource = data.getSource();
        final Object originalSeed = data.getSeed();
        final Object[] originalArgs = data.getArgs();
        final UniformRandomProvider generator = originalSource.create(originalSeed, originalArgs);
        @SuppressWarnings("deprecation")
        final UniformRandomProvider rng1 = RandomSource.create(originalSource, originalSeed, originalArgs);
        Assertions.assertEquals(rng1.getClass(), generator.getClass());
        // Check the output
        final UniformRandomProvider rng2 = originalSource.create(originalSeed, originalArgs);
        for (int i = 0; i < 10; i++) {
            Assertions.assertEquals(rng2.nextLong(), rng1.nextLong());
        }
    }

    /**
     * Test the create method throws an {@link IllegalArgumentException} if passed the wrong
     * arguments.
     */
    @ParameterizedTest
    @MethodSource("getProvidersTestData")
    void testCreateMethodThrowsWithIncorrectArguments(ProvidersList.Data data) {
        final RandomSource originalSource = data.getSource();
        final Object[] originalArgs = data.getArgs();
        if (originalArgs == null) {
            // Try passing arguments to a provider that does not require them
            int arg1 = 123;
            double arg2 = 456.0;
            Assertions.assertThrows(IllegalArgumentException.class,
                () -> originalSource.create(arg1, arg2),
                () -> "Source does not require arguments: " + originalSource);
        } else {
            // Try no arguments for a provider that does require them
            Assertions.assertThrows(IllegalArgumentException.class,
                () -> originalSource.create(),
                () -> "Source requires arguments: " + originalSource);
        }
    }

    @ParameterizedTest
    @MethodSource("getProvidersTestData")
    void testAllSeedTypes(ProvidersList.Data data) {
        final RandomSource originalSource = data.getSource();
        final Object originalSeed = data.getSeed();
        final Object[] originalArgs = data.getArgs();
        final Integer intSeed = -12131415;
        final Long longSeed = -1213141516171819L;
        final int[] intArraySeed = new int[] {0, 11, -22, 33, -44, 55, -66, 77, -88, 99};
        final long[] longArraySeed = new long[] {11111L, -222222L, 3333333L, -44444444L};
        final byte[] byteArraySeed = new byte[] {-128, -91, -45, -32, -1, 0, 11, 23, 54, 88, 127};

        final Object[] seeds = new Object[] {null,
                                             intSeed,
                                             longSeed,
                                             intArraySeed,
                                             longArraySeed,
                                             byteArraySeed};

        int nonNativeSeedCount = 0;
        int seedCount = 0;
        for (Object s : seeds) {
            ++seedCount;
            if (originalSource.isNativeSeed(s)) {
                Assertions.assertNotNull(s, "Identified native seed is null");
                Assertions.assertEquals(s.getClass(), originalSeed.getClass(),
                    "Incorrect identification of native seed type");
            } else {
                ++nonNativeSeedCount;
            }

            originalSource.create(s, originalArgs);
        }

        Assertions.assertEquals(6, seedCount);
        Assertions.assertEquals(5, nonNativeSeedCount);
    }

    @ParameterizedTest
    @MethodSource("getProvidersTestData")
    void testNullSeed(ProvidersList.Data data) {
        final RandomSource originalSource = data.getSource();
        final Object[] originalArgs = data.getArgs();
        // Note: This is the only test that explicitly calls RandomSource.create() with no other arguments.
        final UniformRandomProvider rng = originalArgs == null ?
            originalSource.create() :
            originalSource.create(null, originalArgs);
        checkNextIntegerInRange(rng, 10, 10000);
    }

    @ParameterizedTest
    @MethodSource("getProvidersTestData")
    void testEmptyIntArraySeed(ProvidersList.Data data) {
        final RandomSource originalSource = data.getSource();
        final Object[] originalArgs = data.getArgs();
        final int[] empty = new int[0];
        Assumptions.assumeTrue(originalSource.isNativeSeed(empty));

        // Exercise the default seeding procedure.
        final UniformRandomProvider rng = originalSource.create(empty, originalArgs);
        checkNextIntegerInRange(rng, 10, 20000);
    }

    @ParameterizedTest
    @MethodSource("getProvidersTestData")
    void testEmptyLongArraySeed(ProvidersList.Data data) {
        final RandomSource originalSource = data.getSource();
        final Object[] originalArgs = data.getArgs();
        final long[] empty = new long[0];
        Assumptions.assumeTrue(originalSource.isNativeSeed(empty));
        // The Middle-Square Weyl Sequence generator cannot self-seed
        Assumptions.assumeFalse(originalSource == RandomSource.MSWS);

        // Exercise the default seeding procedure.
        final UniformRandomProvider rng = originalSource.create(empty, originalArgs);
        checkNextIntegerInRange(rng, 10, 10000);
    }

    @ParameterizedTest
    @MethodSource("getProvidersTestData")
    void testZeroIntArraySeed(ProvidersList.Data data) {
        final RandomSource originalSource = data.getSource();
        final Object[] originalArgs = data.getArgs();
        // Exercise capacity to escape all "zero" state.
        final int[] zero = new int[2000]; // Large enough to fill the entire state with zeroes.
        final UniformRandomProvider rng = originalSource.create(zero, originalArgs);
        Assumptions.assumeTrue(createsNonZeroLongOutput(rng, 2000),
            () -> "RNG is non-functional with an all zero seed: " + originalSource);
        checkNextIntegerInRange(rng, 10, 10000);
    }

    @ParameterizedTest
    @MethodSource("getProvidersTestData")
    void testZeroLongArraySeed(ProvidersList.Data data) {
        final RandomSource originalSource = data.getSource();
        final Object[] originalArgs = data.getArgs();
        // Exercise capacity to escape all "zero" state.
        final long[] zero = new long[2000]; // Large enough to fill the entire state with zeroes.
        final UniformRandomProvider rng = originalSource.create(zero, originalArgs);
        Assumptions.assumeTrue(createsNonZeroLongOutput(rng, 2000),
            () -> "RNG is non-functional with an all zero seed: " + originalSource);
        checkNextIntegerInRange(rng, 10, 10000);
    }

    @ParameterizedTest
    @MethodSource("getProvidersTestData")
    void testRandomSourceCreateSeed(ProvidersList.Data data) {
        final RandomSource originalSource = data.getSource();
        final Object[] originalArgs = data.getArgs();
        final byte[] seed = originalSource.createSeed();
        final UniformRandomProvider rng = originalSource.create(seed, originalArgs);
        checkNextIntegerInRange(rng, 10, 10000);
    }

    @ParameterizedTest
    @MethodSource("getProvidersTestData")
    void testRandomSourceCreateSeedFromRNG(ProvidersList.Data data) {
        final RandomSource originalSource = data.getSource();
        final Object[] originalArgs = data.getArgs();
        final byte[] seed = originalSource.createSeed(new SplitMix64(RandomSource.createLong()));
        final UniformRandomProvider rng = originalSource.create(seed, originalArgs);
        checkNextIntegerInRange(rng, 10, 10000);
    }

    // State save and restore tests.

    @SuppressWarnings("unused")
    @ParameterizedTest
    @MethodSource("getProvidersTestData")
    void testUnrestorable(ProvidersList.Data data) {
        final RandomSource originalSource = data.getSource();
        final Object originalSeed = data.getSeed();
        final Object[] originalArgs = data.getArgs();
        // Create two generators of the same type as the one being tested.
        final UniformRandomProvider rng1 = originalSource.create(originalSeed, originalArgs);
        final UniformRandomProvider rng2 = RandomSource.unrestorable(originalSource.create(originalSeed, originalArgs));

        // Ensure that they generate the same values.
        RandomAssert.assertProduceSameSequence(rng1, rng2);

        // Cast must work.
        final RestorableUniformRandomProvider restorable = (RestorableUniformRandomProvider) rng1;
        // Cast must fail.
        Assertions.assertThrows(ClassCastException.class, () -> {
            RestorableUniformRandomProvider dummy = (RestorableUniformRandomProvider) rng2;
        });
    }

    @ParameterizedTest
    @MethodSource("getProvidersTestData")
    void testSerializingState(ProvidersList.Data data)
        throws IOException,
               ClassNotFoundException {
        final UniformRandomProvider generator = data.getSource().create(data.getSeed(), data.getArgs());

        // Large "n" is not necessary here as we only test the serialization.
        final int n = 100;

        // Cast is OK: all instances created by this library inherit from "BaseProvider".
        final RestorableUniformRandomProvider restorable = (RestorableUniformRandomProvider) generator;

        // Save.
        final RandomProviderState stateOrig = restorable.saveState();
        // Serialize.
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(bos);
        oos.writeObject(((RandomProviderDefaultState) stateOrig).getState());

        // Store some values.
        final List<Number> listOrig = makeList(n, generator);

        // Discard a few more.
        final List<Number> listDiscard = makeList(n, generator);
        Assertions.assertNotEquals(0, listDiscard.size());
        Assertions.assertNotEquals(listOrig, listDiscard);

        // Retrieve from serialized stream.
        ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
        ObjectInputStream ois = new ObjectInputStream(bis);
        final RandomProviderState stateNew = new RandomProviderDefaultState((byte[]) ois.readObject());

        Assertions.assertNotSame(stateOrig, stateNew);

        // Reset.
        restorable.restoreState(stateNew);

        // Replay.
        final List<Number> listReplay = makeList(n, generator);
        Assertions.assertNotSame(listOrig, listReplay);

        // Check that the serialized data recreated the orginal state.
        Assertions.assertEquals(listOrig, listReplay);
    }

    @ParameterizedTest
    @MethodSource("getProvidersTestData")
    void testUnrestorableToString(ProvidersList.Data data) {
        final UniformRandomProvider generator = data.getSource().create(data.getSeed(), data.getArgs());
        Assertions.assertEquals(generator.toString(),
                                RandomSource.unrestorable(generator).toString());
    }

    @ParameterizedTest
    @MethodSource("getProvidersTestData")
    void testSupportedInterfaces(ProvidersList.Data data) {
        final RandomSource originalSource = data.getSource();
        final Object[] originalArgs = data.getArgs();
        final UniformRandomProvider rng = originalSource.create(null, originalArgs);
        Assertions.assertEquals(rng instanceof JumpableUniformRandomProvider,
                                originalSource.isJumpable(),
                                "isJumpable");
        Assertions.assertEquals(rng instanceof LongJumpableUniformRandomProvider,
                                originalSource.isLongJumpable(),
                                "isLongJumpable");
    }

    ///// Support methods below.


    // The methods
    //   * makeList
    //   * checkNextIntegerInRange
    //   * checkNextInRange
    // have been copied from "src/test" in module "commons-rng-core".
    // TODO: check whether it is possible to have a single implementation.

    /**
     * Populates a list with random numbers.
     *
     * @param n Loop counter.
     * @param generator Random generator.
     * @return a list containing {@code 11 * n} random numbers.
     */
    private static List<Number> makeList(int n, UniformRandomProvider generator) {
        final List<Number> list = new ArrayList<>();

        for (int i = 0; i < n; i++) {
            // Append 11 values.
            list.add(generator.nextInt());
            list.add(generator.nextInt(21));
            list.add(generator.nextInt(436));
            list.add(generator.nextLong());
            list.add(generator.nextLong(157894));
            list.add(generator.nextLong(5745833));
            list.add(generator.nextFloat());
            list.add(generator.nextFloat());
            list.add(generator.nextDouble());
            list.add(generator.nextDouble());
            list.add(generator.nextBoolean() ? 1 : 0);
        }

        return list;
    }

    /**
     * Tests uniformity of the distribution produced by {@code nextInt(int)}.
     *
     * @param rng Generator.
     * @param max Upper bound.
     * @param sampleSize Number of random values generated.
     * @param generator Random generator.
     */
    private void checkNextIntegerInRange(final UniformRandomProvider rng,
                                         final int max,
                                         int sampleSize) {
        final Callable<Integer> nextMethod = new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                return rng.nextInt(max);
            }
        };

        checkNextInRange(max, sampleSize, nextMethod, rng);
    }

    /**
     * Tests uniformity of the distribution produced by the given
     * {@code nextMethod}.
     * It performs a chi-square test of homogeneity of the observed
     * distribution with the expected uniform distribution.
     * Repeat tests are performed at the 1% level and the total number of failed
     * tests is tested at the 0.5% significance level.
     *
     * @param max Upper bound.
     * @param nextMethod method to call.
     * @param sampleSize Number of random values generated.
     * @param generator Random generator.
     */
    private static <T extends Number> void checkNextInRange(T max,
                                                            int sampleSize,
                                                            Callable<T> nextMethod,
                                                            UniformRandomProvider generator) {
        final int numTests = 500;

        // Do not change (statistical test assumes that dof = 9).
        final int numBins = 10; // dof = numBins - 1

        // Set up bins.
        final long n = max.longValue();
        final long[] binUpperBounds = new long[numBins];
        final double step = n / (double) numBins;
        for (int k = 0; k < numBins; k++) {
            binUpperBounds[k] = (long) ((k + 1) * step);
        }
        // Rounding error occurs on the long value of 2305843009213693951L
        binUpperBounds[numBins - 1] = n;

        // Run the tests.
        int numFailures = 0;

        final double[] expected = new double[numBins];
        long previousUpperBound = 0;
        for (int k = 0; k < numBins; k++) {
            final long range = binUpperBounds[k] - previousUpperBound;
            expected[k] = sampleSize * (range / (double) n);
            previousUpperBound = binUpperBounds[k];
        }

        final int[] observed = new int[numBins];
        // Chi-square critical value with 9 degrees of freedom
        // and 1% significance level.
        final double chi2CriticalValue = 21.665994333461924;

        // For storing chi2 larger than the critical value.
        final List<Double> failedStat = new ArrayList<>();
        try {
            final int lastDecileIndex = numBins - 1;
            for (int i = 0; i < numTests; i++) {
                Arrays.fill(observed, 0);
                SAMPLE: for (int j = 0; j < sampleSize; j++) {
                    final long value = nextMethod.call().longValue();
                    Assertions.assertTrue(value >= 0 && value < n, "Range");

                    for (int k = 0; k < lastDecileIndex; k++) {
                        if (value < binUpperBounds[k]) {
                            ++observed[k];
                            continue SAMPLE;
                        }
                    }
                    ++observed[lastDecileIndex];
                }

                // Compute chi-square.
                double chi2 = 0;
                for (int k = 0; k < numBins; k++) {
                    final double diff = observed[k] - expected[k];
                    chi2 += diff * diff / expected[k];
                }

                // Statistics check.
                if (chi2 > chi2CriticalValue) {
                    failedStat.add(chi2);
                    ++numFailures;
                }
            }
        } catch (Exception e) {
            // Should never happen.
            throw new RuntimeException("Unexpected", e);
        }

        // The expected number of failed tests can be modelled as a Binomial distribution
        // B(n, p) with n=500, p=0.01 (500 tests with a 1% significance level).
        // The cumulative probability of the number of failed tests (X) is:
        // x     P(X>x)
        // 10    0.0132
        // 11    0.00521
        // 12    0.00190

        if (numFailures > 11) { // Test will fail with 0.5% probability
            Assertions.fail(String.format(
                    "%s: Too many failures for n = %d, sample size = %d " +
                    "(%d out of %d tests failed, chi2 > %.3f=%s)",
                    generator, n, sampleSize, numFailures, numTests, chi2CriticalValue,
                    failedStat.stream().map(d -> String.format("%.3f", d))
                              .collect(Collectors.joining(", ", "[", "]"))));
        }
    }

    /**
     * Return true if the generator creates non-zero output from
     * {@link UniformRandomProvider#nextLong()} within the given number of cycles.
     *
     * @param rng Random generator.
     * @param cycles Number of cycles.
     * @return true if non-zero output
     */
    private static boolean createsNonZeroLongOutput(UniformRandomProvider rng,
                                                    int cycles) {
        boolean nonZero = false;
        for (int i = 0; i < cycles; i++) {
            if (rng.nextLong() != 0) {
                nonZero = true;
            }
        }
        return nonZero;
    }
}
