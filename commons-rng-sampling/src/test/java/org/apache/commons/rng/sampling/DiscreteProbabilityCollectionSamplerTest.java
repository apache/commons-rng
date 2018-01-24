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

package org.apache.commons.rng.sampling;

import java.util.Arrays;
import org.junit.Assert;
import org.junit.Test;
import org.apache.commons.rng.UniformRandomProvider;
import org.apache.commons.rng.simple.RandomSource;

/**
 * Test class for {@link DiscreteProbabilityCollectionSampler}.
 */
public class DiscreteProbabilityCollectionSamplerTest {
    /** RNG. */
    private static final UniformRandomProvider rng = RandomSource.create(RandomSource.WELL_1024_A);

    @Test(expected=IllegalArgumentException.class)
    public void testPrecondition1() {
        new DiscreteProbabilityCollectionSampler<Double>(rng,
                                                         Arrays.asList(new Double[] {1d, 2d}),
                                                         new double[] {0d});
    }
    @Test(expected=IllegalArgumentException.class)
    public void testPrecondition2() {
        new DiscreteProbabilityCollectionSampler<Double>(rng,
                                                         Arrays.asList(new Double[] {1d, 2d}),
                                                         new double[] {0d, -1d});
    }
    @Test(expected=IllegalArgumentException.class)
    public void testPrecondition3() {
        new DiscreteProbabilityCollectionSampler<Double>(rng,
                                                         Arrays.asList(new Double[] {1d, 2d}),
                                                         new double[] {0d, 0d});
    }
    @Test(expected=IllegalArgumentException.class)
    public void testPrecondition4() {
        new DiscreteProbabilityCollectionSampler<Double>(rng,
                                                         Arrays.asList(new Double[] {1d, 2d}),
                                                         new double[] {0d, Double.NaN});
    }
    @Test(expected=IllegalArgumentException.class)
    public void testPrecondition5() {
        new DiscreteProbabilityCollectionSampler<Double>(rng,
                                                         Arrays.asList(new Double[] {1d, 2d}),
                                                         new double[] {0d, Double.POSITIVE_INFINITY});
    }

    @Test
    public void testSample() {
        final DiscreteProbabilityCollectionSampler<Double> sampler =
            new DiscreteProbabilityCollectionSampler<Double>(rng,
                                                             Arrays.asList(new Double[] {3d, -1d, 3d, 7d, -2d, 8d}),
                                                             new double[] {0.2, 0.2, 0.3, 0.3, 0, 0});
        final double expectedMean = 3.4;
        final double expectedVariance = 7.84;

        final int n = 100000000;
        double sum = 0;
        double sumOfSquares = 0;
        for (int i = 0; i < n; i++) {
            final double rand = sampler.sample();
            sum += rand;
            sumOfSquares += rand * rand;
        }

        final double mean = sum / n;
        Assert.assertEquals(expectedMean, mean, 1e-3);
        final double variance = sumOfSquares / n - mean * mean;
        Assert.assertEquals(expectedVariance, variance, 2e-3);
    }
}
