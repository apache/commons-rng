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

import org.apache.commons.math3.distribution.ChiSquaredDistribution;
import org.apache.commons.math3.stat.inference.ChiSquareTest;

/**
 * Tests for random deviates generators.
 */
@RunWith(value=Parameterized.class)
public class DiscreteSamplerParametricTest {
    /** Sampler under test. */
    private final DiscreteSamplerTestData sampler;

    /**
     * Initializes generator instance.
     *
     * @param rng RNG to be tested.
     */
    public DiscreteSamplerParametricTest(DiscreteSamplerTestData data) {
        sampler = data;
    }

    @Parameters(name = "{index}: data={0}")
    public static Iterable<DiscreteSamplerTestData[]> getList() {
        return DiscreteSamplersList.list();
    }

    @Test
    public void testSampling() {
        final int sampleSize = 10000;

        final double[] prob = sampler.getProbabilities();
        final int len = prob.length; 
        final double[] expected = new double[len];
        for (int i = 0; i < len; i++) {
            expected[i] = prob[i] * sampleSize;
        }
        check(sampleSize,
              sampler.getSampler(),
              sampler.getPoints(),
              expected);
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
    private void check(long sampleSize,
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
        final List<Double> failedStat = new ArrayList<Double>();
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

                if (chiSquareTest.chiSquareTest(expected, observed, 0.001)) {
                    failedStat.add(chiSquareTest.chiSquareTest(expected, observed));
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
