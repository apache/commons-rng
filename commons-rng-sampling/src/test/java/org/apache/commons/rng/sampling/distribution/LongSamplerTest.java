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

import java.util.concurrent.ThreadLocalRandom;
import org.apache.commons.rng.sampling.RandomAssert;
import org.apache.commons.rng.simple.RandomSource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Tests the default methods in the {@link LongSampler} interface.
 */
class LongSamplerTest {
    @Test
    void testSamplesUnlimitedSize() {
        final LongSampler s = RandomAssert.createRNG()::nextLong;
        Assertions.assertEquals(Long.MAX_VALUE, s.samples().spliterator().estimateSize());
    }

    @RepeatedTest(value = 3)
    void testSamples() {
        final long seed = RandomSource.createLong();
        final LongSampler s1 = RandomSource.SPLIT_MIX_64.create(seed)::nextLong;
        final LongSampler s2 = RandomSource.SPLIT_MIX_64.create(seed)::nextLong;
        final int count = ThreadLocalRandom.current().nextInt(3, 13);
        Assertions.assertArrayEquals(createSamples(s1, count),
                                     s2.samples().limit(count).toArray());
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2, 5, 13})
    void testSamples(int streamSize) {
        final long seed = RandomSource.createLong();
        final LongSampler s1 = RandomSource.SPLIT_MIX_64.create(seed)::nextLong;
        final LongSampler s2 = RandomSource.SPLIT_MIX_64.create(seed)::nextLong;
        Assertions.assertArrayEquals(createSamples(s1, streamSize),
                                     s2.samples(streamSize).toArray());
    }

    /**
     * Creates an array of samples.
     *
     * @param sampler Source of samples.
     * @param count Number of samples.
     * @return the samples
     */
    private static long[] createSamples(LongSampler sampler, int count) {
        final long[] data = new long[count];
        for (int i = 0; i < count; i++) {
            data[i] = sampler.sample();
        }
        return data;
    }
}
