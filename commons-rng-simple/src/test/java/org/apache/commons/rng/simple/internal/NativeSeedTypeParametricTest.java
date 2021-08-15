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

import org.junit.jupiter.api.Assertions;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import java.lang.reflect.Array;
import java.util.function.Supplier;

/**
 * Tests for the {@link NativeSeedType} seed conversions. This test
 * ensures that a seed can be created or converted from any supported input seed to each
 * supported native seed type.
 */
@RunWith(value = Parameterized.class)
public class NativeSeedTypeParametricTest {
    /** This is a list of the class types that are supported native seeds. */
    private static final Object[] SUPPORTED_NATIVE_TYPES = {
        Integer.class,
        Long.class,
        int[].class,
        long[].class
    };
    /** Example supported seeds for conversion to a native seed type. */
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

    /** The native seed type enum instance. */
    private final NativeSeedType nativeSeedType;
    /** The class type of the native seed. */
    private final Class<?> type;

    /**
     * Initializes the test instance.
     *
     * @param type The type of the native seed.
     */
    public NativeSeedTypeParametricTest(Class<?> type) {
        this.type = type;
        nativeSeedType = findNativeSeedType(type);
    }

    /**
     * Gets the supported native seed types.
     *
     * @return the types
     */
    @Parameters
    public static Object[] getTypes() {
        // Check that there are enum values for all supported types.
        // This ensures the test is maintained to correspond to the enum.
        Assertions.assertEquals(SUPPORTED_NATIVE_TYPES.length, NativeSeedType.values().length,
            "Incorrect number of enum values for the supported native types");

        return SUPPORTED_NATIVE_TYPES;
    }

    /**
     * Creates the native seed type.
     *
     * @param type Class of the native seed.
     * @return the native seed type
     */
    private static NativeSeedType findNativeSeedType(Class<?> type) {
        for (final NativeSeedType nativeSeedType : NativeSeedType.values()) {
            if (type.equals(nativeSeedType.getType())) {
                return nativeSeedType;
            }
        }
        throw new AssertionError("No enum matching the type: " + type);
    }

    /**
     * Test the seed can be created as the correct type.
     */
    @Test
    public void testCreateSeed() {
        final int size = 3;
        final Object seed = nativeSeedType.createSeed(size);
        Assertions.assertNotNull(seed);
        Assertions.assertEquals(type, seed.getClass(), "Seed was not the correct class");
        if (type.isArray()) {
            Assertions.assertEquals(size, Array.getLength(seed), "Seed was not created the correct length");
        }
    }

    /**
     * Test the seed can be created, converted to a byte[] and then back to the native type.
     */
    @Test
    public void testConvertSeedToBytes() {
        final int size = 3;
        final Object seed = nativeSeedType.createSeed(size);
        Assertions.assertNotNull(seed, "Null seed");

        final byte[] bytes = NativeSeedType.convertSeedToBytes(seed);
        Assertions.assertNotNull(bytes, "Null byte[] seed");

        final Object seed2 = nativeSeedType.convertSeed(bytes, size);
        if (type.isArray()) {
            // This handles nested primitive arrays
            Assertions.assertArrayEquals(new Object[] {seed}, new Object[] {seed2},
                "byte[] seed was not converted back");
        } else {
            Assertions.assertEquals(seed, seed2, "byte[] seed was not converted back");
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
            final Supplier<String> msg = () -> input.getClass() + " input seed was not converted";
            Assertions.assertNotNull(seed, msg);
            Assertions.assertEquals(type, seed.getClass(), msg);
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
                Assertions.fail(input.getClass() + " input seed was not rejected as unsupported");
            } catch (UnsupportedOperationException ex) {
                // This is expected
            }
        }
    }
}
