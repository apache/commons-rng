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
import java.util.concurrent.Callable;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;

import org.junit.Assert;
import org.junit.Test;
import org.junit.Assume;
import org.junit.Ignore;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import org.apache.commons.rng.UniformRandomProvider;
import org.apache.commons.rng.RandomProviderState;
import org.apache.commons.rng.RestorableUniformRandomProvider;
import org.apache.commons.rng.core.RandomProviderDefaultState;
import org.apache.commons.rng.sampling.ContinuousSampler;

/**
 * Tests for random deviates generators using the "inverse method".
 */
@RunWith(value=Parameterized.class)
public class InverseMethodContinuousSamplerParametricTest {
    /** Sampler under test. */
    private final ContinuousSamplerTestData sampler;

    /**
     * Initializes generator instance.
     *
     * @param rng RNG to be tested.
     */
    public InverseMethodContinuousSamplerParametricTest(ContinuousSamplerTestData data) {
        sampler = data;
    }

    @Parameters(name = "{index}: data={0}")
    public static Iterable<ContinuousSamplerTestData[]> getList() {
        return ContinuousSamplersList.list();
    }

    @Test
    public void testSampling() {
        check(10000, sampler.getSampler(), sampler.getDeciles());
    }

    /**
     * Performs a chi-square test of homogeneity of the observed
     * distribution with the expected distribution.
     * Tests are performed at the 1% level and an average failure rate
     * higher than 2% (i.e. more than 20 null hypothesis rejections)
     * causes the test case to fail.
     *
     * @param sampler Sampler.
     * @param sampleSize Number of random values to generate.
     * @param deciles Deciles.
     */
    private void check(long sampleSize,
                       ContinuousSampler sampler,
                       double[] deciles) {
        final int numTests = 500;

        // Do not change (statistical test assumes that dof = 10).
        final int numBins = 10;

        // Run the tests.
        int numFailures = 0;

        final double[] expected = new double[numBins];
        for (int k = 0; k < numBins; k++) {
            expected[k] = sampleSize / (double) numBins;
        }

        final long[] observed = new long[numBins];
        // Chi-square critical value with 10 degrees of freedom
        // and 1% significance level.
        final double chi2CriticalValue = 23.209;

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
                    ++numFailures;
                }
            }
        } catch (Exception e) {
            // Should never happen.
            throw new RuntimeException("Unexpected", e);
        }

        if ((double) numFailures / (double) numTests > 0.02) {
            Assert.fail(sampler + ": Too many failures for sample size = " + sampleSize +
                        " (" + numFailures + " out of " + numTests + " tests failed)");
        }
    }
}
