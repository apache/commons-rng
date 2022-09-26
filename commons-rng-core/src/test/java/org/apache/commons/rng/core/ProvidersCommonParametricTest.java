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
package org.apache.commons.rng.core;

import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.function.LongSupplier;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import org.apache.commons.rng.UniformRandomProvider;
import org.apache.commons.rng.RestorableUniformRandomProvider;
import org.apache.commons.rng.RandomProviderState;

/**
 * Tests which all generators must pass.
 */
class ProvidersCommonParametricTest {
    private static Iterable<RestorableUniformRandomProvider> getList() {
        return ProvidersList.list();
    }

    // Precondition tests

    @ParameterizedTest
    @MethodSource("getList")
    void testPreconditionNextInt(UniformRandomProvider generator) {
        Assertions.assertThrows(IllegalArgumentException.class, () -> generator.nextInt(-1));
        Assertions.assertThrows(IllegalArgumentException.class, () -> generator.nextInt(0));
    }

    @ParameterizedTest
    @MethodSource("getList")
    void testPreconditionNextLong(UniformRandomProvider generator) {
        Assertions.assertThrows(IllegalArgumentException.class, () -> generator.nextLong(-1));
        Assertions.assertThrows(IllegalArgumentException.class, () -> generator.nextLong(0));
    }

    @ParameterizedTest
    @MethodSource("getList")
    void testPreconditionNextBytes(UniformRandomProvider generator) {
        Assertions.assertThrows(NullPointerException.class, () -> generator.nextBytes(null, 0, 0));
        final int size = 10;
        final int num = 1;
        final byte[] buf = new byte[size];
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> generator.nextBytes(buf, -1, num));
        // Edge-case allowed by JDK range checks (see RNG-170)
        generator.nextBytes(buf, size, 0);
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> generator.nextBytes(buf, size, 1));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> generator.nextBytes(buf, size, -1));
        final int offset = 2;
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> generator.nextBytes(buf, offset, size - offset + 1));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> generator.nextBytes(buf, offset, -1));
        // offset + length overflows
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> generator.nextBytes(buf, offset, Integer.MAX_VALUE));
        // Should be OK
        generator.nextBytes(buf, 0, size);
        generator.nextBytes(buf, 0, size - offset);
        generator.nextBytes(buf, offset, size - offset);
        // Should be consistent with no length
        final byte[] empty = {};
        generator.nextBytes(empty);
        generator.nextBytes(empty, 0, 0);
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> generator.nextBytes(empty, 0, 1));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> generator.nextBytes(empty, 0, -1));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> generator.nextBytes(empty, -1, 0));
    }

    // Uniformity tests

    @ParameterizedTest
    @MethodSource("getList")
    void testUniformNextBytesFullBuffer(UniformRandomProvider generator) {
        // Value chosen to exercise all the code lines in the
        // "nextBytes" methods.
        final int size = 23;
        final byte[] buffer = new byte[size];

        final Runnable nextMethod = new Runnable() {
            @Override
            public void run() {
                generator.nextBytes(buffer);
            }
        };

        Assertions.assertTrue(isUniformNextBytes(buffer, 0, size, nextMethod), generator::toString);
    }

    @ParameterizedTest
    @MethodSource("getList")
    void testUniformNextBytesPartialBuffer(UniformRandomProvider generator) {
        final int totalSize = 1234;
        final int offset = 567;
        final int size = 89;

        final byte[] buffer = new byte[totalSize];

        final Runnable nextMethod = new Runnable() {
            @Override
            public void run() {
                generator.nextBytes(buffer, offset, size);
            }
        };

        // Test should pass for the part of the buffer where values are put.
        Assertions.assertTrue(isUniformNextBytes(buffer, offset, offset + size, nextMethod), generator::toString);

        // The parts of the buffer where no values are put should be zero.
        for (int i = 0; i < offset; i++) {
            Assertions.assertEquals(0, buffer[i], generator::toString);
        }
        for (int i = offset + size; i < totalSize; i++) {
            Assertions.assertEquals(0, buffer[i], generator::toString);
        }
    }

    @ParameterizedTest
    @MethodSource("getList")
    void testUniformNextIntegerInRange(UniformRandomProvider generator) {
        // Statistical test uses 10 bins so tests are invalid below this level
        checkNextIntegerInRange(generator, 10, 1000);
        checkNextIntegerInRange(generator, 12, 1000);
        checkNextIntegerInRange(generator, 31, 1000);
        checkNextIntegerInRange(generator, 32, 1000);
        checkNextIntegerInRange(generator, 2016128993, 1000);
        checkNextIntegerInRange(generator, 1834691456, 1000);
        checkNextIntegerInRange(generator, 869657561, 1000);
        checkNextIntegerInRange(generator, 1570504788, 1000);
    }

    @ParameterizedTest
    @MethodSource("getList")
    void testUniformNextLongInRange(UniformRandomProvider generator) {
        // Statistical test uses 10 bins so tests are invalid below this level
        checkNextLongInRange(generator, 11, 1000);
        checkNextLongInRange(generator, 19, 1000);
        checkNextLongInRange(generator, 31, 1000);
        checkNextLongInRange(generator, 32, 1000);

        final long q = Long.MAX_VALUE / 4;
        checkNextLongInRange(generator, q, 1000);
        checkNextLongInRange(generator, 2 * q, 1000);
        checkNextLongInRange(generator, 3 * q, 1000);
    }

    @ParameterizedTest
    @MethodSource("getList")
    void testUniformNextFloat(UniformRandomProvider generator) {
        checkNextFloat(generator, 1000);
    }

    @ParameterizedTest
    @MethodSource("getList")
    void testUniformNextDouble(UniformRandomProvider generator) {
        checkNextDouble(generator, 1000);
    }

    @ParameterizedTest
    @MethodSource("getList")
    void testUniformNextIntRandomWalk(UniformRandomProvider generator) {
        final Callable<Boolean> nextMethod = new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                return generator.nextInt() >= 0;
            }
        };

        checkRandomWalk(generator, 1000, nextMethod);
    }

    @ParameterizedTest
    @MethodSource("getList")
    void testUniformNextLongRandomWalk(UniformRandomProvider generator) {
        final Callable<Boolean> nextMethod = new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                return generator.nextLong() >= 0;
            }
        };

        checkRandomWalk(generator, 1000, nextMethod);
    }

    @ParameterizedTest
    @MethodSource("getList")
    void testUniformNextBooleanRandomWalk(UniformRandomProvider generator) {
        final Callable<Boolean> nextMethod = new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                return generator.nextBoolean();
            }
        };

        checkRandomWalk(generator, 1000, nextMethod);
    }

    // State save and restore tests.

    @ParameterizedTest
    @MethodSource("getList")
    void testStateSettable(RestorableUniformRandomProvider generator) {
        // Should be fairly large in order to ensure that all the internal
        // state is away from its initial settings.
        final int n = 10000;

        // Save.
        final RandomProviderState state = generator.saveState();
        // Store some values.
        final List<Number> listOrig = makeList(n, generator);
        // Discard a few more.
        final List<Number> listDiscard = makeList(n, generator);
        Assertions.assertNotEquals(0, listDiscard.size());
        Assertions.assertNotEquals(listOrig, listDiscard);
        // Reset.
        generator.restoreState(state);
        // Replay.
        final List<Number> listReplay = makeList(n, generator);
        Assertions.assertNotSame(listOrig, listReplay);
        // Check that the restored state is the same as the original.
        Assertions.assertEquals(listOrig, listReplay);
    }

    @ParameterizedTest
    @MethodSource("getList")
    void testStateWrongSize(RestorableUniformRandomProvider generator) {
        final RandomProviderState state = new DummyGenerator().saveState();
        // Try to restore with an invalid state (wrong size).
        Assertions.assertThrows(IllegalStateException.class, () -> generator.restoreState(state));
    }

    @ParameterizedTest
    @MethodSource("getList")
    void testRestoreForeignState(RestorableUniformRandomProvider generator) {
        final RandomProviderState state = new RandomProviderState() {};
        Assertions.assertThrows(IllegalArgumentException.class, () -> generator.restoreState(state));
    }

    ///// Support methods below.

    /**
     * Populates a list with random numbers.
     *
     * @param n Loop counter.
     * @param generator RNG under test.
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
     * Checks that the generator values can be placed into 256 bins with
     * approximately equal number of counts.
     * Test allows to select only part of the buffer for performing the
     * statistics.
     *
     * @param buffer Buffer to be filled.
     * @param first First element (included) of {@code buffer} range for
     * which statistics must be taken into account.
     * @param last Last element (excluded) of {@code buffer} range for
     * which statistics must be taken into account.
     * @param nextMethod Method that fills the given {@code buffer}.
     * @return {@code true} if the distribution is uniform.
     */
    private static boolean isUniformNextBytes(byte[] buffer,
                                              int first,
                                              int last,
                                              Runnable nextMethod) {
        final int sampleSize = 10000;

        // Number of possible values (do not change).
        final int byteRange = 256;
        // Chi-square critical value with 255 degrees of freedom
        // and 1% significance level.
        final double chi2CriticalValue = 310.45738821990585;

        // Bins.
        final long[] observed = new long[byteRange];
        final double[] expected = new double[byteRange];

        Arrays.fill(expected, sampleSize * (last - first) / (double) byteRange);

        try {
            for (int k = 0; k < sampleSize; k++) {
                nextMethod.run();

                for (int i = first; i < last; i++) {
                    // Convert byte to an index in [0, 255]
                    ++observed[buffer[i] & 0xff];
                }
            }
        } catch (final Exception e) {
            // Should never happen.
            throw new RuntimeException("Unexpected");
        }

        // Compute chi-square.
        double chi2 = 0;
        for (int k = 0; k < byteRange; k++) {
            final double diff = observed[k] - expected[k];
            chi2 += diff * diff / expected[k];
        }

        // Statistics check.
        return chi2 <= chi2CriticalValue;
    }

    /**
     * Checks that the generator values can be placed into 2 bins with
     * approximately equal number of counts.
     * The test uses the expectation from a fixed-step "random walk".
     *
     * @param generator Generator.
     * @param sampleSize Number of random values generated.
     * @param nextMethod Method that returns {@code true} if the generated
     * values are to be placed in the first bin, {@code false} if it must
     * go to the second bin.
     */
    private static void checkRandomWalk(UniformRandomProvider generator,
                                        int sampleSize,
                                        Callable<Boolean> nextMethod) {
        int walk = 0;

        try {
            for (int k = 0; k < sampleSize; ++k) {
                if (nextMethod.call()) {
                    ++walk;
                } else {
                    --walk;
                }
            }
        } catch (final Exception e) {
            // Should never happen.
            throw new RuntimeException("Unexpected");
        }

        final double actual = Math.abs(walk);
        final double max = Math.sqrt(sampleSize) * 2.576;
        Assertions.assertTrue(actual < max,
            () -> generator + ": Walked too far astray: " + actual + " > " + max +
            " (test will fail randomly about 1 in 100 times)");
    }

    /**
     * Tests uniformity of the distribution produced by {@code nextInt(int)}.
     *
     * @param generator Generator.
     * @param max Upper bound.
     * @param sampleSize Number of random values generated.
     */
    private static void checkNextIntegerInRange(final UniformRandomProvider generator,
                                                final int max,
                                                int sampleSize) {
        final LongSupplier nextMethod = () -> generator.nextInt(max);

        checkNextInRange(generator, max, sampleSize, nextMethod);
    }

    /**
     * Tests uniformity of the distribution produced by {@code nextLong(long)}.
     *
     * @param generator Generator.
     * @param max Upper bound.
     * @param sampleSize Number of random values generated.
     */
    private static void checkNextLongInRange(final UniformRandomProvider generator,
                                             long max,
                                             int sampleSize) {
        final LongSupplier nextMethod = () -> generator.nextLong(max);

        checkNextInRange(generator, max, sampleSize, nextMethod);
    }

    /**
     * Tests uniformity of the distribution produced by {@code nextFloat()}.
     *
     * @param generator Generator.
     * @param sampleSize Number of random values generated.
     */
    private static void checkNextFloat(final UniformRandomProvider generator,
                                       int sampleSize) {
        final int max = 1234;
        final LongSupplier nextMethod = () -> (int) (max * generator.nextFloat());

        checkNextInRange(generator, max, sampleSize, nextMethod);
    }

    /**
     * Tests uniformity of the distribution produced by {@code nextDouble()}.
     *
     * @param generator Generator.
     * @param sampleSize Number of random values generated.
     */
    private static void checkNextDouble(final UniformRandomProvider generator,
                                        int sampleSize) {
        final int max = 578;
        final LongSupplier nextMethod = () -> (int) (max * generator.nextDouble());

        checkNextInRange(generator, max, sampleSize, nextMethod);
    }

    /**
     * Tests uniformity of the distribution produced by the given
     * {@code nextMethod}.
     * It performs a chi-square test of homogeneity of the observed
     * distribution with the expected uniform distribution.
     * Repeat tests are performed at the 1% level and the total number of failed
     * tests is tested at the 0.5% significance level.
     *
     * @param generator Generator.
     * @param n Upper bound.
     * @param nextMethod method to call.
     * @param sampleSize Number of random values generated.
     */
    private static void checkNextInRange(final UniformRandomProvider generator,
                                         long n,
                                         int sampleSize,
                                         LongSupplier nextMethod) {
        final int numTests = 500;

        // Do not change (statistical test assumes that dof = 9).
        final int numBins = 10; // dof = numBins - 1

        // Set up bins.
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
                    final long value = nextMethod.getAsLong();
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
        } catch (final Exception e) {
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
     * @param generator Generator.
     * @param chunkSize Size of the small buffer.
     * @param numChunks Number of chunks that make the large buffer.
     */
    static void checkNextBytesChunks(RestorableUniformRandomProvider generator,
                                     int chunkSize,
                                     int numChunks) {
        final byte[] b1 = new byte[chunkSize * numChunks];
        final byte[] b2 = new byte[chunkSize];

        final RandomProviderState state = generator.saveState();

        // Generate the chunks in a single call.
        generator.nextBytes(b1);

        // Reset to previous state.
        generator.restoreState(state);

        // Generate the chunks in consecutive calls.
        for (int i = 0; i < numChunks; i++) {
            generator.nextBytes(b2);
        }

        // Store last "chunkSize" bytes of b1 into b3.
        final byte[] b3 = new byte[chunkSize];
        System.arraycopy(b1, b1.length - b3.length, b3, 0, b3.length);

        // Sequence of calls must be the same.
        Assertions.assertArrayEquals(b2, b3,
            () -> "chunkSize=" + chunkSize + " numChunks=" + numChunks);
    }

    /**
     * Dummy class for checking that restoring fails when an invalid state is used.
     */
    class DummyGenerator extends org.apache.commons.rng.core.source32.IntProvider {
        /** {@inheritDoc} */
        @Override
        public int next() {
            return 4; // https://www.xkcd.com/221/
        }

        /** {@inheritDoc} */
        @Override
        protected byte[] getStateInternal() {
            return new byte[0];
        }

        /** {@inheritDoc} */
        @Override
        protected void setStateInternal(byte[] s) {
            // No state.
        }
    }
}
