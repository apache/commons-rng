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

import org.junit.Assert;
import org.junit.Test;
import org.apache.commons.rng.simple.RandomSource;
import org.apache.commons.rng.UniformRandomProvider;
import org.apache.commons.rng.core.source64.SplitMix64;

/**
 * Test for {@link UnitSphereSampler}.
 */
public class UnitSphereSamplerTest {
    @Test(expected = IllegalArgumentException.class)
    public void testPrecondition() {
        new UnitSphereSampler(0, null);
    }

    /**
     * Test the distribution of points in two dimensions.
     */
    @Test
    public void testDistribution2D() {
        UniformRandomProvider rng = RandomSource.create(RandomSource.XOR_SHIFT_1024_S, 17399225432L);
        UnitSphereSampler generator = new UnitSphereSampler(2, rng);

        // In 2D, angles with a given vector should be uniformly distributed.
        final int[] angleBuckets = new int[100];
        final int steps = 1000000;
        for (int i = 0; i < steps; ++i) {
            final double[] v = generator.nextVector();
            Assert.assertEquals(2, v.length);
            Assert.assertEquals(1, length(v), 1e-10);
            // Compute angle formed with vector (1, 0)?
            // Cosine of angle is their dot product, because both are unit length.
            // Dot product here is just the first element of the vector by construction.
            final double angle = Math.acos(v[0]);
            final int bucket = (int) (angle / Math.PI * angleBuckets.length);
            ++angleBuckets[bucket];
        }

        // Simplistic test for roughly even distribution.
        final int expectedBucketSize = steps / angleBuckets.length;
        for (int bucket : angleBuckets) {
            Assert.assertTrue("Bucket count " + bucket + " vs expected " + expectedBucketSize,
                              Math.abs(expectedBucketSize - bucket) < 350);
        }
    }

    /** Cf. RNG-55. */
    @Test(expected = StackOverflowError.class)
    public void testBadProvider1() {
        final UniformRandomProvider bad = new UniformRandomProvider() {
                // CHECKSTYLE: stop all
                public long nextLong(long n) { return 0; }
                public long nextLong() { return 0; }
                public int nextInt(int n) { return 0; }
                public int nextInt() { return 0; }
                public float nextFloat() { return 0; }
                public double nextDouble() { return 0;}
                public void nextBytes(byte[] bytes, int start, int len) {}
                public void nextBytes(byte[] bytes) {}
                public boolean nextBoolean() { return false; }
                // CHECKSTYLE: resume all
            };

        new UnitSphereSampler(1, bad).nextVector();
    }

    /** Cf. RNG-55. */
    @Test
    public void testBadProvider1ThenGoodProvider() {
        // Create a provider that will create a bad first sample but then recover.
        // This checks recursion will return a good value.
        final UniformRandomProvider bad = new SplitMix64(0L) {
                private int count;
                // CHECKSTYLE: stop all
                public long nextLong() { return (count++ == 0) ? 0 : super.nextLong(); }
                public double nextDouble() { return (count++ == 0) ? 0 : super.nextDouble(); }
                // CHECKSTYLE: resume all
            };

        final double[] vector = new UnitSphereSampler(1, bad).nextVector();
        Assert.assertEquals(1, vector.length);
    }

    /**
     * Test to demonstrate that using floating-point equality of the norm squared with
     * zero is valid. Any norm squared after zero should produce a valid scaling factor.
     */
    @Test
    public void testNextNormSquaredAfterZeroIsValid() {
        // The sampler explicitly handles length == 0 using recursion.
        // Anything above zero should be valid.
        final double normSq = Math.nextAfter(0, 1);
        // Map to the scaling factor
        final double f = 1 / Math.sqrt(normSq);
        // As long as this is finite positive then the sampler is valid
        Assert.assertTrue(f > 0 && f <= Double.MAX_VALUE);
    }

    /**
     * Test the SharedStateSampler implementation.
     */
    @Test
    public void testSharedStateSampler() {
        final UniformRandomProvider rng1 = RandomSource.create(RandomSource.SPLIT_MIX_64, 0L);
        final UniformRandomProvider rng2 = RandomSource.create(RandomSource.SPLIT_MIX_64, 0L);
        final int n = 3;
        final UnitSphereSampler sampler1 =
            new UnitSphereSampler(n, rng1);
        final UnitSphereSampler sampler2 = sampler1.withUniformRandomProvider(rng2);
        RandomAssert.assertProduceSameSequence(
            new RandomAssert.Sampler<double[]>() {
                @Override
                public double[] sample() {
                    return sampler1.nextVector();
                }
            },
            new RandomAssert.Sampler<double[]>() {
                @Override
                public double[] sample() {
                    return sampler2.nextVector();
                }
            });
    }

    /**
     * @return the length (L2-norm) of given vector.
     */
    private static double length(double[] vector) {
        double total = 0;
        for (double d : vector) {
            total += d * d;
        }
        return Math.sqrt(total);
    }
}
