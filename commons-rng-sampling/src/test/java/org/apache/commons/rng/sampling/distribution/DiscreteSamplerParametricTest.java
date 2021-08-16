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
import java.util.List;
import java.util.ArrayList;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import org.apache.commons.math3.stat.inference.ChiSquareTest;

/**
 * Tests for random deviates generators.
 */
class DiscreteSamplerParametricTest {
    private static Iterable<DiscreteSamplerTestData> getSamplerTestData() {
        return DiscreteSamplersList.list();
    }

    @ParameterizedTest
    @MethodSource("getSamplerTestData")
    void testSampling(DiscreteSamplerTestData data) {
        final int sampleSize = 10000;
        // Probabilities are normalised by the chi-square test
        check(sampleSize,
              data.getSampler(),
              data.getPoints(),
              data.getProbabilities());
    }

    /**
     * Performs a chi-square test of homogeneity of the observed
     * distribution with the expected distribution.
     * An average failure rate higher than 5% causes the test case
     * to fail.
     *
     * @param sampler Sampler.
     * @param sampleSize Number of random values to generate.
     * @param points Outcomes.
     * @param expected Expected counts of the given outcomes.
     */
    private static void check(long sampleSize,
                              DiscreteSampler sampler,
                              int[] points,
                              double[] expected) {
        final ChiSquareTest chiSquareTest = new ChiSquareTest();
        final int numTests = 50;

        // Run the tests.
        int numFailures = 0;

        final int numBins = points.length;
        final long[] observed = new long[numBins];

        // For storing chi2 larger than the critical value.
        final List<Double> failedStat = new ArrayList<>();
        try {
            for (int i = 0; i < numTests; i++) {
                Arrays.fill(observed, 0);
                SAMPLE: for (long j = 0; j < sampleSize; j++) {
                    final int value = sampler.sample();

                    for (int k = 0; k < numBins; k++) {
                        if (value == points[k]) {
                            ++observed[k];
                            continue SAMPLE;
                        }
                    }
                }

                if (chiSquareTest.chiSquareTest(expected, observed, 0.01)) {
                    failedStat.add(chiSquareTest.chiSquareTest(expected, observed));
                    ++numFailures;
                }
            }
        } catch (Exception e) {
            // Should never happen.
            throw new RuntimeException("Unexpected", e);
        }

        // The expected number of failed tests can be modelled as a Binomial distribution
        // B(n, p) with n=50, p=0.01 (50 tests with a 1% significance level).
        // The cumulative probability of the number of failed tests (X) is:
        // x     P(X>x)
        // 1     0.0894
        // 2     0.0138
        // 3     0.0016

        if (numFailures > 3) { // Test will fail with 0.16% probability
            Assertions.fail(sampler + ": Too many failures for sample size = " + sampleSize +
                            " (" + numFailures + " out of " + numTests + " tests failed, " +
                            "chi2=" + Arrays.toString(failedStat.toArray(new Double[0])));
        }
    }
}
