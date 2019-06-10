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
import java.util.HashMap;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.apache.commons.rng.UniformRandomProvider;
import org.apache.commons.rng.simple.RandomSource;

/**
 * Test class for {@link DiscreteProbabilityCollectionSampler}.
 */
public class DiscreteProbabilityCollectionSamplerTest {
    /** RNG. */
    private final UniformRandomProvider rng = RandomSource.create(RandomSource.WELL_1024_A);

    @Test(expected = IllegalArgumentException.class)
    public void testPrecondition1() {
        // Size mismatch
        new DiscreteProbabilityCollectionSampler<Double>(rng,
                                                         Arrays.asList(new Double[] {1d, 2d}),
                                                         new double[] {0d});
    }
    @Test(expected = IllegalArgumentException.class)
    public void testPrecondition2() {
        // Negative probability
        new DiscreteProbabilityCollectionSampler<Double>(rng,
                                                         Arrays.asList(new Double[] {1d, 2d}),
                                                         new double[] {0d, -1d});
    }
    @Test(expected = IllegalArgumentException.class)
    public void testPrecondition3() {
        // Probabilities do not sum above 0
        new DiscreteProbabilityCollectionSampler<Double>(rng,
                                                         Arrays.asList(new Double[] {1d, 2d}),
                                                         new double[] {0d, 0d});
    }
    @Test(expected = IllegalArgumentException.class)
    public void testPrecondition4() {
        // NaN probability
        new DiscreteProbabilityCollectionSampler<Double>(rng,
                                                         Arrays.asList(new Double[] {1d, 2d}),
                                                         new double[] {0d, Double.NaN});
    }
    @Test(expected = IllegalArgumentException.class)
    public void testPrecondition5() {
        // Infinite probability
        new DiscreteProbabilityCollectionSampler<Double>(rng,
                                                         Arrays.asList(new Double[] {1d, 2d}),
                                                         new double[] {0d, Double.POSITIVE_INFINITY});
    }
    @Test(expected = IllegalArgumentException.class)
    public void testPrecondition6() {
        // Empty Map<T, Double> not allowed
        new DiscreteProbabilityCollectionSampler<Double>(rng,
                                                         new HashMap<Double, Double>());
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

    /**
     * Edge-case test:
     * Create a sampler that will return 1 for nextDouble() forcing the binary search to
     * identify the end item of the cumulative probability array.
     */
    @Test
    public void testSampleWithProbabilityAtLastItem() {
        sampleWithProbabilityForLastItem(false);
    }

    /**
     * Edge-case test:
     * Create a sampler that will return over 1 for nextDouble() forcing the binary search to
     * identify insertion at the end of the cumulative probability array.
     */
    @Test
    public void testSampleWithProbabilityPastLastItem() {
        sampleWithProbabilityForLastItem(true);
    }

    private static void sampleWithProbabilityForLastItem(boolean pastLast) {
        // Ensure the samples pick probability 0 (the first item) and then
        // a probability (for the second item) that hits an edge case.
        final double probability = pastLast ? 1.1 : 1;
        final UniformRandomProvider dummyRng = new UniformRandomProvider() {
            private int count;
            // CHECKSTYLE: stop all
            public long nextLong(long n) { return 0; }
            public long nextLong() { return 0; }
            public int nextInt(int n) { return 0; }
            public int nextInt() { return 0; }
            public float nextFloat() { return 0; }
            // Return 0 then the given probability
            public double nextDouble() { return (count++ == 0) ? 0 : probability; }
            public void nextBytes(byte[] bytes, int start, int len) {}
            public void nextBytes(byte[] bytes) {}
            public boolean nextBoolean() { return false; }
            // CHECKSTYLE: resume all
        };

        final List<Double> items = Arrays.asList(new Double[] {1d, 2d});
        final DiscreteProbabilityCollectionSampler<Double> sampler =
            new DiscreteProbabilityCollectionSampler<Double>(dummyRng,
                                                             items,
                                                             new double[] {0.5, 0.5});
        final Double item1 = sampler.sample();
        final Double item2 = sampler.sample();
        // Check they are in the list
        Assert.assertTrue("Sample item1 is not from the list", items.contains(item1));
        Assert.assertTrue("Sample item2 is not from the list", items.contains(item2));
        // Test the two samples are different items
        Assert.assertNotSame("Item1 and 2 should be different", item1, item2);
    }

    /**
     * Test the SharedStateSampler implementation.
     */
    @Test
    public void testSharedStateSampler() {
        final UniformRandomProvider rng1 = RandomSource.create(RandomSource.SPLIT_MIX_64, 0L);
        final UniformRandomProvider rng2 = RandomSource.create(RandomSource.SPLIT_MIX_64, 0L);
        final List<Double> items = Arrays.asList(new Double[] {1d, 2d, 3d, 4d});
        final DiscreteProbabilityCollectionSampler<Double> sampler1 =
            new DiscreteProbabilityCollectionSampler<Double>(rng1,
                                                             items,
                                                             new double[] {0.1, 0.2, 0.3, 04});
        final DiscreteProbabilityCollectionSampler<Double> sampler2 = sampler1.withUniformRandomProvider(rng2);
        RandomAssert.assertProduceSameSequence(
            new RandomAssert.Sampler<Double>() {
                @Override
                public Double sample() {
                    return sampler1.sample();
                }
            },
            new RandomAssert.Sampler<Double>() {
                @Override
                public Double sample() {
                    return sampler2.sample();
                }
            });
    }
}
