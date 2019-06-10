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
package org.apache.commons.rng.simple.internal;

import org.apache.commons.rng.simple.internal.ProviderBuilder.RandomSourceInternal;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

/**
 * Tests for the {@link ProviderBuilder.RandomSourceInternal} seed conversions. This test
 * ensures that all random sources can create a seed or convert any supported seed to the
 * correct type for the constructor.
 */
@RunWith(value = Parameterized.class)
public class RandomSourceInternalParametricTest {
    /** The supported seeds for conversion to a native seed type. */
    private static final Object[] SUPPORTED_SEEDS = {
        Integer.valueOf(1),
        Long.valueOf(2),
        new int[] {3, 4, 5},
        new long[] {6, 7, 8},
        new byte[] {9, 10, 11},
    };
    /** Example unsupported seeds for conversion to a native seed type. */
    private static final Object[] UNSUPPORTED_SEEDS = {
        null,
        Double.valueOf(Math.PI),
    };

    /** Internal identifier for the random source. */
    private final RandomSourceInternal randomSourceInternal;
    /** The class type of the native seed. */
    private final Class<?> type;

    /**
     * Initializes the test instance.
     *
     * @param randomSourceInternal Internal identifier for the random source.
     */
    public RandomSourceInternalParametricTest(RandomSourceInternal randomSourceInternal) {
        this.randomSourceInternal = randomSourceInternal;
        // The first constructor argument is always the seed type
        this.type = randomSourceInternal.getArgs()[0];
    }

    /**
     * Gets the supported native seed types.
     *
     * @return the types
     */
    @Parameters
    public static Object[] getTypes() {
        return RandomSourceInternal.values();
    }

    /**
     * Test the seed can be created as the correct type.
     */
    @Test
    public void testCreateSeed() {
        final Object seed = randomSourceInternal.createSeed();
        Assert.assertNotNull(seed);
        Assert.assertEquals("Seed was not the correct class", type, seed.getClass());
        Assert.assertTrue("Seed was not identified as the native type", randomSourceInternal.isNativeSeed(seed));
    }

    /**
     * Test the seed can be converted to the correct type from any of the supported input types.
     */
    @Test
    public void testConvertSupportedSeed() {
        for (final Object input : SUPPORTED_SEEDS) {
            final Object seed = randomSourceInternal.convertSeed(input);
            final String msg = input.getClass() + " input seed was not converted";
            Assert.assertNotNull(msg, seed);
            Assert.assertEquals(msg, type, seed.getClass());
            Assert.assertTrue(msg, randomSourceInternal.isNativeSeed(seed));
        }
    }

    /**
     * Test unsupported input seed types are rejected.
     */
    @Test
    public void testCannotConvertUnsupportedSeed() {
        for (final Object input : UNSUPPORTED_SEEDS) {
            try {
                randomSourceInternal.convertSeed(input);
                Assert.fail(input.getClass() + " input seed was not rejected as unsupported");
            } catch (UnsupportedOperationException ex) {
                // This is expected
            }
        }
    }
}
