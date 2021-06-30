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
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;

import org.junit.Assert;
import org.junit.Test;
import org.junit.Assume;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

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
@RunWith(value = Parameterized.class)
public class ProvidersCommonParametricTest {
    /** RNG under test. */
    private final UniformRandomProvider generator;
    /** RNG specifier. */
    private final RandomSource originalSource;
    /** Seed (constructor's first parameter). */
    private final Object originalSeed;
    /** Constructor's additional parameters. */
    private final Object[] originalArgs;

    /**
     * Initializes the test instance.
     *
     * @param data Random source (and seed arguments) to be tested.
     */
    public ProvidersCommonParametricTest(ProvidersList.Data data) {
        originalSource = data.getSource();
        originalSeed = data.getSeed();
        originalArgs = data.getArgs();
        generator = originalSource.create(originalSeed, originalArgs);
    }

    @Parameters(name = "{index}: data={0}")
    public static Iterable<ProvidersList.Data[]> getList() {
        return ProvidersList.list();
    }

    // Seeding tests.

    @Test(expected = UnsupportedOperationException.class)
    public void testUnsupportedSeedType() {
        final byte seed = 123;
        originalSource.create(seed, originalArgs);
    }

    /**
     * Test the factory create method returns the same class as the instance create method.
     */
    @Test
    public void testFactoryCreateMethod() {
        // Cannot test providers that require arguments
        Assume.assumeTrue(originalArgs == null);
        @SuppressWarnings("deprecation")
        final UniformRandomProvider rng = RandomSource.create(originalSource);
        Assert.assertEquals(generator.getClass(), rng.getClass());
    }

    /**
     * Test the factory create method returns the same class as the instance create method
     * and produces the same output.
     */
    @Test
    public void testFactoryCreateMethodWithSeed() {
        @SuppressWarnings("deprecation")
        final UniformRandomProvider rng1 = RandomSource.create(originalSource, originalSeed, originalArgs);
        Assert.assertEquals(rng1.getClass(), generator.getClass());
        // Check the output
        final UniformRandomProvider rng2 = originalSource.create(originalSeed, originalArgs);
        for (int i = 0; i < 10; i++) {
            Assert.assertEquals(rng2.nextLong(), rng1.nextLong());
        }
    }

    /**
     * Test the create method throws an {@link IllegalArgumentException} if passed the wrong
     * arguments.
     */
    @Test
    public void testCreateMethodThrowsWithIncorrectArguments() {
        if (originalArgs == null) {
            try {
                // Try passing arguments to a provider that does not require them
                int arg1 = 123;
                double arg2 = 456.0;
                originalSource.create(arg1, arg2);
                Assert.fail("Source does not require arguments: " + originalSource);
            } catch (IllegalArgumentException ex) {
                // Expected
            }
        } else {
            try {
                // Try no arguments for a provider that does require them
                originalSource.create();
                Assert.fail("Source requires arguments: " + originalSource);
            } catch (IllegalArgumentException ex) {
                // Expected
            }
        }
    }

    @Test
    public void testAllSeedTypes() {
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
                Assert.assertNotNull("Identified native seed is null", s);
                Assert.assertEquals("Incorrect identification of native seed type",
                                    s.getClass(), originalSeed.getClass());
            } else {
                ++nonNativeSeedCount;
            }

            originalSource.create(s, originalArgs);
        }

        Assert.assertEquals(6, seedCount);
        Assert.assertEquals(5, nonNativeSeedCount);
    }

    @Test
    public void testNullSeed() {
        // Note: This is the only test that explicitly calls RandomSource.create() with no other arguments.
        final UniformRandomProvider rng = originalArgs == null ?
            originalSource.create() :
            originalSource.create(null, originalArgs);
        checkNextIntegerInRange(rng, 10, 10000);
    }

    @Test
    public void testEmptyIntArraySeed() {
        final int[] empty = new int[0];
        Assume.assumeTrue(originalSource.isNativeSeed(empty));

        // Exercise the default seeding procedure.
        final UniformRandomProvider rng = originalSource.create(empty, originalArgs);
        checkNextIntegerInRange(rng, 10, 20000);
    }

    @Test
    public void testEmptyLongArraySeed() {
        final long[] empty = new long[0];
        Assume.assumeTrue(originalSource.isNativeSeed(empty));
        // The Middle-Square Weyl Sequence generator cannot self-seed
        Assume.assumeFalse(originalSource == RandomSource.MSWS);

        // Exercise the default seeding procedure.
        final UniformRandomProvider rng = originalSource.create(empty, originalArgs);
        checkNextIntegerInRange(rng, 10, 10000);
    }

    @Test
    public void testZeroIntArraySeed() {
        // Exercise capacity to escape all "zero" state.
        final int[] zero = new int[2000]; // Large enough to fill the entire state with zeroes.
        final UniformRandomProvider rng = originalSource.create(zero, originalArgs);
        Assume.assumeTrue("RNG is non-functional with an all zero seed: " + originalSource,
                createsNonZeroLongOutput(rng, 2000));
        checkNextIntegerInRange(rng, 10, 10000);
    }

    @Test
    public void testZeroLongArraySeed() {
        // Exercise capacity to escape all "zero" state.
        final long[] zero = new long[2000]; // Large enough to fill the entire state with zeroes.
        final UniformRandomProvider rng = originalSource.create(zero, originalArgs);
        Assume.assumeTrue("RNG is non-functional with an all zero seed: " + originalSource,
                createsNonZeroLongOutput(rng, 2000));
        checkNextIntegerInRange(rng, 10, 10000);
    }

    @Test
    public void testRandomSourceCreateSeed() {
        final byte[] seed = originalSource.createSeed();
        final UniformRandomProvider rng = originalSource.create(seed, originalArgs);
        checkNextIntegerInRange(rng, 10, 10000);
    }

    @Test
    public void testRandomSourceCreateSeedFromRNG() {
        final byte[] seed = originalSource.createSeed(new SplitMix64(RandomSource.createLong()));
        final UniformRandomProvider rng = originalSource.create(seed, originalArgs);
        checkNextIntegerInRange(rng, 10, 10000);
    }

    // State save and restore tests.

    @Test
    public void testUnrestorable() {
        // Create two generators of the same type as the one being tested.
        final UniformRandomProvider rng1 = originalSource.create(originalSeed, originalArgs);
        final UniformRandomProvider rng2 = RandomSource.unrestorable(originalSource.create(originalSeed, originalArgs));

        // Ensure that they generate the same values.
        RandomAssert.assertProduceSameSequence(rng1, rng2);

        // Cast must work.
        final RestorableUniformRandomProvider restorable = (RestorableUniformRandomProvider) rng1;
        // Cast must fail.
        try {
            final RestorableUniformRandomProvider dummy = (RestorableUniformRandomProvider) rng2;
            Assert.fail("Cast should have failed");
        } catch (ClassCastException e) {
            // Expected.
        }
    }

    @Test
    public void testSerializingState()
        throws IOException,
               ClassNotFoundException {
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
        final List<Number> listOrig = makeList(n);

        // Discard a few more.
        final List<Number> listDiscard = makeList(n);
        Assert.assertNotEquals(0, listDiscard.size());
        Assert.assertNotEquals(listOrig, listDiscard);

        // Retrieve from serialized stream.
        ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
        ObjectInputStream ois = new ObjectInputStream(bis);
        final RandomProviderState stateNew = new RandomProviderDefaultState((byte[]) ois.readObject());

        Assert.assertNotSame(stateOrig, stateNew);

        // Reset.
        restorable.restoreState(stateNew);

        // Replay.
        final List<Number> listReplay = makeList(n);
        Assert.assertNotSame(listOrig, listReplay);

        // Check that the serialized data recreated the orginal state.
        Assert.assertEquals(listOrig, listReplay);
    }

    @Test
    public void testUnrestorableToString() {
        Assert.assertEquals(generator.toString(),
                            RandomSource.unrestorable(generator).toString());
    }

    @Test
    public void testSupportedInterfaces() {
        final UniformRandomProvider rng = originalSource.create(null, originalArgs);
        Assert.assertEquals("isJumpable", rng instanceof JumpableUniformRandomProvider,
                            originalSource.isJumpable());
        Assert.assertEquals("isLongJumpable", rng instanceof LongJumpableUniformRandomProvider,
                            originalSource.isLongJumpable());
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
     * @return a list containing {@code 11 * n} random numbers.
     */
    private List<Number> makeList(int n) {
        final List<Number> list = new ArrayList<Number>();

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

        checkNextInRange(max, sampleSize, nextMethod);
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
     */
    private <T extends Number> void checkNextInRange(T max,
                                                     int sampleSize,
                                                     Callable<T> nextMethod) {
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
        final double chi2CriticalValue = 21.67;

        try {
            for (int i = 0; i < numTests; i++) {
                Arrays.fill(observed, 0);
                for (int j = 0; j < sampleSize; j++) {
                    final long value = nextMethod.call().longValue();
                    Assert.assertTrue("Range", (value >= 0) && (value < n));

                    for (int k = 0; k < numBins; k++) {
                        if (value < binUpperBounds[k]) {
                            ++observed[k];
                            break;
                        }
                    }
                }

                // Compute chi-square.
                double chi2 = 0;
                for (int k = 0; k < numBins; k++) {
                    final double diff = observed[k] - expected[k];
                    chi2 += diff * diff / expected[k];
                }

                // Statistics check.
                if (chi2 > chi2CriticalValue) {
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
            Assert.fail(generator + ": Too many failures for n = " + n +
                        " (" + numFailures + " out of " + numTests + " tests failed)");
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
