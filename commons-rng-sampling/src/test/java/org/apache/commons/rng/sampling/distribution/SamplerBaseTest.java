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

import org.apache.commons.rng.UniformRandomProvider;
import org.apache.commons.rng.simple.RandomSource;
import org.junit.Assert;
import org.junit.Test;

/**
 * Test for the {@link SamplerBase}. The class is deprecated but is public. The methods
 * should be tested to ensure correct functionality.
 */
public class SamplerBaseTest {
    /**
     * Extends the {@link SamplerBase} to allow access to methods.
     */
    @SuppressWarnings("deprecation")
    private static class SimpleSampler extends SamplerBase {
        SimpleSampler(UniformRandomProvider rng) {
            super(rng);
        }

        @Override
        public double nextDouble() {
            return super.nextDouble();
        }

        @Override
        public int nextInt() {
            return super.nextInt();
        }

        @Override
        public int nextInt(int max) {
            return super.nextInt(max);
        }

        @Override
        public long nextLong() {
            return super.nextLong();
        }
    }

    @Test
    public void testNextMethods() {
        final UniformRandomProvider rng1 = RandomSource.create(RandomSource.SPLIT_MIX_64, 0L);
        final UniformRandomProvider rng2 = RandomSource.create(RandomSource.SPLIT_MIX_64, 0L);
        final SimpleSampler sampler = new SimpleSampler(rng2);
        final int n = 256;
        for (int i = 0; i < 3; i++) {
            Assert.assertEquals(rng1.nextDouble(), sampler.nextDouble(), 0);
            Assert.assertEquals(rng1.nextInt(), sampler.nextInt());
            Assert.assertEquals(rng1.nextInt(n), sampler.nextInt(n));
            Assert.assertEquals(rng1.nextLong(), sampler.nextLong());
        }
    }

    @Test
    public void testToString() {
        final UniformRandomProvider rng = RandomSource.create(RandomSource.SPLIT_MIX_64, 0L);
        final SimpleSampler sampler = new SimpleSampler(rng);
        Assert.assertTrue(sampler.toString().contains("rng"));
    }
}
