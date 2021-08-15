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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.Test;
import org.apache.commons.rng.UniformRandomProvider;
import org.apache.commons.rng.simple.RandomSource;

/**
 * Tests for {@link CollectionSampler}.
 */
public class CollectionSamplerTest {

    @Test
    public void testSampleTrivial() {
        final ArrayList<String> list = new ArrayList<String>();
        list.add("Apache");
        list.add("Commons");
        list.add("RNG");

        final CollectionSampler<String> sampler =
            new CollectionSampler<String>(RandomSource.MWC_256.create(),
                                          list);
        final String word = sampler.sample();
        for (String w : list) {
            if (word.equals(w)) {
                return;
            }
        }
        Assertions.fail(word + " not in list");
    }

    @Test
    public void testSamplePrecondition() {
        // Must fail for empty collection.
        Assertions.assertThrows(IllegalArgumentException.class,
            () -> new CollectionSampler<String>(RandomSource.SPLIT_MIX_64.create(0L),
                                                new ArrayList<String>()));
    }

    /**
     * Test the SharedStateSampler implementation.
     */
    @Test
    public void testSharedStateSampler() {
        final UniformRandomProvider rng1 = RandomSource.SPLIT_MIX_64.create(0L);
        final UniformRandomProvider rng2 = RandomSource.SPLIT_MIX_64.create(0L);
        final List<String> list = Arrays.asList("Apache", "Commons", "RNG");
        final CollectionSampler<String> sampler1 =
            new CollectionSampler<String>(rng1, list);
        final CollectionSampler<String> sampler2 = sampler1.withUniformRandomProvider(rng2);
        RandomAssert.assertProduceSameSequence(sampler1, sampler2);
    }
}
