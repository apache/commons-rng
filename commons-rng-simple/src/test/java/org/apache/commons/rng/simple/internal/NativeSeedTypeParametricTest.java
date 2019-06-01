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

import org.apache.commons.rng.simple.internal.ProviderBuilder.NativeSeedType;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import java.lang.reflect.Array;

/**
 * Tests for the {@link ProviderBuilder.NativeSeedType} seed conversions. This test
 * ensures that a seed can be created or converted from any supported input seed to each
 * supported native seed type.
 */
@RunWith(value=Parameterized.class)
public class NativeSeedTypeParametricTest {
    /** The supported seeds for conversion to a native seed type. */
    private static final Object[] SUPPORTED_SEEDS = {
        Integer.valueOf(1),
        Long.valueOf(2),
        new int[] { 3, 4, 5 },
        new long[] { 6, 7, 8 },
        new byte[] { 9, 10, 11 },
    };
    /** Example unsupported seeds for conversion to a native seed type. */
    private static final Object[] UNSUPPORTED_SEEDS = {
        null,
        Double.valueOf(Math.PI),
    };

    /** The class type of the native seed. */
    private final Class<?> type;
    /** The native seed type enum instance. */
    private final NativeSeedType nativeSeedType;

    /**
     * Initializes the test instance.
     *
     * @param type The type of the native seed.
     */
    public NativeSeedTypeParametricTest(Class<?> type) {
        this.type = type;
        nativeSeedType = NativeSeedType.createNativeSeedType(type);
    }

    /**
     * Gets the supported native seed types.
     *
     * @return the types
     */
    @Parameters
    public static Object[] getTypes() {
        // This is a list of the class types that are supported native seeds.
        return new Object[] {
            Integer.class,
            Long.class,
            int[].class,
            long[].class
        };
    }

    /**
     * Test the seed can be created as the correct type.
     */
    @Test
    public void testCreateSeed() {
        final int size = 3;
        final Object seed = nativeSeedType.createSeed(size);
        Assert.assertNotNull(seed);
        Assert.assertEquals("Seed was not the correct class", type, seed.getClass());
        if (type.isArray()) {
            Assert.assertEquals("Seed was not created the correct length", size, Array.getLength(seed));
        }
    }

    /**
     * Test the seed can be converted to the correct type from any of the supported input types.
     */
    @Test
    public void testConvertSupportedSeed() {
        // Size can be ignored during conversion and so it not asserted
        final int size = 3;
        for (final Object input : SUPPORTED_SEEDS) {
            final Object seed = nativeSeedType.convertSeed(input, size);
            final String msg = input.getClass() + " input seed was not converted";
            Assert.assertNotNull(msg, seed);
            Assert.assertEquals(msg, type, seed.getClass());
        }
    }

    /**
     * Test unsupported input seed types are rejected.
     */
    @Test
    public void testCannotConvertUnsupportedSeed() {
        final int size = 3;
        for (final Object input : UNSUPPORTED_SEEDS) {
            try {
                nativeSeedType.convertSeed(input, size);
                Assert.fail(input.getClass() + " input seed was not rejected as unsupported");
            } catch (UnsupportedOperationException ex) {
                // This is expected
            }
        }
    }
}
