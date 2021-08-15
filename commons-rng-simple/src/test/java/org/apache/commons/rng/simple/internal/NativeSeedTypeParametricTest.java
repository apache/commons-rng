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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Tests for the {@link NativeSeedType} seed conversions. This test
 * ensures that a seed can be created or converted from any supported input seed to each
 * supported native seed type.
 */
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

    /**
     * Check that there are enum values for all supported types.
     * This ensures the test is maintained to correspond to the enum.
     */
    @Test
    public void testNativeSeedTypeEnum() {
        Set<Class<?>> supported = Arrays.stream(SUPPORTED_NATIVE_TYPES)
            .map(o -> (Class<?>) o)
            .collect(Collectors.toSet());
        Assertions.assertEquals(SUPPORTED_NATIVE_TYPES.length, supported.size(),
            "Class type of supported seeds should be unique");

        final NativeSeedType[] values = NativeSeedType.values();
        Assertions.assertEquals(SUPPORTED_NATIVE_TYPES.length, values.length,
            "Incorrect number of enum values for the supported native types");

        // Remove each
        Arrays.stream(values).map(NativeSeedType::getType).forEach(supported::remove);
        Assertions.assertEquals(0, supported.size());
    }

    /**
     * Test the seed can be created as the correct type.
     *
     * @param nativeSeedType Native seed type.
     */
    @ParameterizedTest
    @EnumSource
    public void testCreateSeed(NativeSeedType nativeSeedType) {
        final int size = 3;
        final Object seed = nativeSeedType.createSeed(size);
        Assertions.assertNotNull(seed);
        final Class<?> type = nativeSeedType.getType();
        Assertions.assertEquals(type, seed.getClass(), "Seed was not the correct class");
        if (type.isArray()) {
            Assertions.assertEquals(size, Array.getLength(seed), "Seed was not created the correct length");
        }
    }

    /**
     * Test the seed can be created, converted to a byte[] and then back to the native type.
     *
     * @param nativeSeedType Native seed type.
     */
    @ParameterizedTest
    @EnumSource
    public void testConvertSeedToBytes(NativeSeedType nativeSeedType) {
        final int size = 3;
        final Object seed = nativeSeedType.createSeed(size);
        Assertions.assertNotNull(seed, "Null seed");

        final byte[] bytes = NativeSeedType.convertSeedToBytes(seed);
        Assertions.assertNotNull(bytes, "Null byte[] seed");

        final Object seed2 = nativeSeedType.convertSeed(bytes, size);
        if (nativeSeedType.getType().isArray()) {
            // This handles nested primitive arrays
            Assertions.assertArrayEquals(new Object[] {seed}, new Object[] {seed2},
                "byte[] seed was not converted back");
        } else {
            Assertions.assertEquals(seed, seed2, "byte[] seed was not converted back");
        }
    }

    /**
     * Test the seed can be converted to the correct type from any of the supported input types.
     *
     * @param nativeSeedType The native seed type enum instance.
     */
    @ParameterizedTest
    @EnumSource
    public void testConvertSupportedSeed(NativeSeedType nativeSeedType) {
        // Size can be ignored during conversion and so it not asserted
        final int size = 3;
        for (final Object input : SUPPORTED_SEEDS) {
            final Object seed = nativeSeedType.convertSeed(input, size);
            final Supplier<String> msg = () -> input.getClass() + " input seed was not converted";
            Assertions.assertNotNull(seed, msg);
            Assertions.assertEquals(nativeSeedType.getType(), seed.getClass(), msg);
        }
    }

    /**
     * Test unsupported input seed types are rejected.
     *
     * @param nativeSeedType The native seed type enum instance.
     */
    @ParameterizedTest
    @EnumSource
    public void testCannotConvertUnsupportedSeed(NativeSeedType nativeSeedType) {
        final int size = 3;
        for (final Object input : UNSUPPORTED_SEEDS) {
            Assertions.assertThrows(UnsupportedOperationException.class,
                    () -> nativeSeedType.convertSeed(input, size));
        }
    }
}
