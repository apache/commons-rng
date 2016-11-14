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

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

/**
 * Tests for random deviates generators.
 */
@RunWith(value=Parameterized.class)
public class ContinuousSamplerParametricTest {
    /** Sampler under test. */
    private final ContinuousSamplerTestData sampler;

    /**
     * Initializes generator instance.
     *
     * @param rng RNG to be tested.
     */
    public ContinuousSamplerParametricTest(ContinuousSamplerTestData data) {
        sampler = data;
    }

    @Parameters(name = "{index}: data={0}")
    public static Iterable<ContinuousSamplerTestData[]> getList() {
        return ContinuousSamplersList.list();
    }

    @Test
    public void testSampling() {
        check(20000, sampler.getSampler(), sampler.getDeciles());
    }

    /**
     * Performs a chi-square test of homogeneity of the observed
     * distribution with the expected distribution.
     * Tests are performed at the 1% level and an average failure rate
     * higher than 5% causes the test case to fail.
     *
     * @param sampler Sampler.
     * @param sampleSize Number of random values to generate.
     * @param deciles Deciles.
     */
    private void check(long sampleSize,
                       ContinuousSampler sampler,
                       double[] deciles) {
        final int numTests = 50;

        // Do not change (statistical test assumes that dof = 9).
        final int numBins = 10; // dof = numBins - 1

        // Run the tests.
        int numFailures = 0;

        final double[] expected = new double[numBins];
        for (int k = 0; k < numBins; k++) {
            expected[k] = sampleSize / (double) numBins;
        }

        final long[] observed = new long[numBins];
        // Chi-square critical value with 9 degrees of freedom
        // and 1% significance level.
        final double chi2CriticalValue = 21.67;

        // For storing chi2 larger than the critical value.
        final List<Double> failedStat = new ArrayList<Double>();
        try {
            final int lastDecileIndex = numBins - 1;
            for (int i = 0; i < numTests; i++) {
                Arrays.fill(observed, 0);
                SAMPLE: for (long j = 0; j < sampleSize; j++) {
                    final double value = sampler.sample();

                    for (int k = 0; k < lastDecileIndex; k++) {
                        if (value < deciles[k]) {
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
                    // System.out.println("bin[" + k + "]" +
                    //                    " obs=" + observed[k] +
                    //                    " exp=" + expected[k]);
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

        if ((double) numFailures / (double) numTests > 0.05) {
            Assert.fail(sampler + ": Too many failures for sample size = " + sampleSize +
                        " (" + numFailures + " out of " + numTests + " tests failed, " +
                        "chi2=" + Arrays.toString(failedStat.toArray(new Double[0])));
        }
    }
}
