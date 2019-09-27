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

import org.apache.commons.rng.core.source64.SplitMix64;
import org.apache.commons.rng.simple.internal.ProviderBuilder.RandomSourceInternal;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import java.util.EnumMap;

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
    /** The expected byte size of the seed for each RandomSource. */
    private static final EnumMap<RandomSourceInternal, Integer> EXPECTED_SEED_BYTES =
            new EnumMap<RandomSourceInternal, Integer>(RandomSourceInternal.class);

    static {
        final int intBytes = 4;
        final int longBytes = 8;
        EXPECTED_SEED_BYTES.put(RandomSourceInternal.JDK, longBytes * 1);
        EXPECTED_SEED_BYTES.put(RandomSourceInternal.WELL_512_A, intBytes * 16);
        EXPECTED_SEED_BYTES.put(RandomSourceInternal.WELL_1024_A, intBytes * 32);
        EXPECTED_SEED_BYTES.put(RandomSourceInternal.WELL_19937_A, intBytes * 624);
        EXPECTED_SEED_BYTES.put(RandomSourceInternal.WELL_19937_C, intBytes * 624);
        EXPECTED_SEED_BYTES.put(RandomSourceInternal.WELL_44497_A, intBytes * 1391);
        EXPECTED_SEED_BYTES.put(RandomSourceInternal.WELL_44497_B, intBytes * 1391);
        EXPECTED_SEED_BYTES.put(RandomSourceInternal.MT, intBytes * 624);
        EXPECTED_SEED_BYTES.put(RandomSourceInternal.ISAAC, intBytes * 256);
        EXPECTED_SEED_BYTES.put(RandomSourceInternal.SPLIT_MIX_64, longBytes * 1);
        EXPECTED_SEED_BYTES.put(RandomSourceInternal.XOR_SHIFT_1024_S, longBytes * 16);
        EXPECTED_SEED_BYTES.put(RandomSourceInternal.TWO_CMRES, intBytes * 1);
        EXPECTED_SEED_BYTES.put(RandomSourceInternal.TWO_CMRES_SELECT, intBytes * 1);
        EXPECTED_SEED_BYTES.put(RandomSourceInternal.MT_64, longBytes * 312);
        EXPECTED_SEED_BYTES.put(RandomSourceInternal.MWC_256, intBytes * 257);
        EXPECTED_SEED_BYTES.put(RandomSourceInternal.KISS, intBytes * 4);
        EXPECTED_SEED_BYTES.put(RandomSourceInternal.XOR_SHIFT_1024_S_PHI, longBytes * 16);
        EXPECTED_SEED_BYTES.put(RandomSourceInternal.XO_RO_SHI_RO_64_S, intBytes * 2);
        EXPECTED_SEED_BYTES.put(RandomSourceInternal.XO_RO_SHI_RO_64_SS, intBytes * 2);
        EXPECTED_SEED_BYTES.put(RandomSourceInternal.XO_SHI_RO_128_PLUS, intBytes * 4);
        EXPECTED_SEED_BYTES.put(RandomSourceInternal.XO_SHI_RO_128_SS, intBytes * 4);
        EXPECTED_SEED_BYTES.put(RandomSourceInternal.XO_RO_SHI_RO_128_PLUS, longBytes * 2);
        EXPECTED_SEED_BYTES.put(RandomSourceInternal.XO_RO_SHI_RO_128_SS, longBytes * 2);
        EXPECTED_SEED_BYTES.put(RandomSourceInternal.XO_SHI_RO_256_PLUS, longBytes * 4);
        EXPECTED_SEED_BYTES.put(RandomSourceInternal.XO_SHI_RO_256_SS, longBytes * 4);
        EXPECTED_SEED_BYTES.put(RandomSourceInternal.XO_SHI_RO_512_PLUS, longBytes * 8);
        EXPECTED_SEED_BYTES.put(RandomSourceInternal.XO_SHI_RO_512_SS, longBytes * 8);
        EXPECTED_SEED_BYTES.put(RandomSourceInternal.PCG_XSH_RR_32, longBytes * 2);
        EXPECTED_SEED_BYTES.put(RandomSourceInternal.PCG_XSH_RS_32, longBytes * 2);
        EXPECTED_SEED_BYTES.put(RandomSourceInternal.PCG_RXS_M_XS_64, longBytes * 2);
        EXPECTED_SEED_BYTES.put(RandomSourceInternal.PCG_MCG_XSH_RR_32, longBytes * 1);
        EXPECTED_SEED_BYTES.put(RandomSourceInternal.PCG_MCG_XSH_RS_32, longBytes * 1);
        EXPECTED_SEED_BYTES.put(RandomSourceInternal.MSWS, longBytes * 3);
        EXPECTED_SEED_BYTES.put(RandomSourceInternal.SFC_32, intBytes * 3);
        EXPECTED_SEED_BYTES.put(RandomSourceInternal.SFC_64, longBytes * 3);
        EXPECTED_SEED_BYTES.put(RandomSourceInternal.JSF_32, intBytes * 1);
        EXPECTED_SEED_BYTES.put(RandomSourceInternal.JSF_64, longBytes * 1);
        EXPECTED_SEED_BYTES.put(RandomSourceInternal.XO_SHI_RO_128_PP, intBytes * 4);
        EXPECTED_SEED_BYTES.put(RandomSourceInternal.XO_RO_SHI_RO_128_PP, longBytes * 2);
        EXPECTED_SEED_BYTES.put(RandomSourceInternal.XO_SHI_RO_256_PP, longBytes * 4);
        EXPECTED_SEED_BYTES.put(RandomSourceInternal.XO_SHI_RO_512_PP, longBytes * 8);
        EXPECTED_SEED_BYTES.put(RandomSourceInternal.XO_RO_SHI_RO_1024_PP, longBytes * 16);
        EXPECTED_SEED_BYTES.put(RandomSourceInternal.XO_RO_SHI_RO_1024_S, longBytes * 16);
        EXPECTED_SEED_BYTES.put(RandomSourceInternal.XO_RO_SHI_RO_1024_SS, longBytes * 16);
        // ... add more here.
        // Verify the seed byte size is reflected in the enum javadoc for RandomSource.
    }

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

    /**
     * Test the seed byte size is reported as the size of a int/long primitive for Int/Long
     * seed types and a multiple of it for int[]/long[] types.
     */
    @Test
    public void testCreateSeedBytesSizeIsPositiveAndMultipleOf4Or8() {
        // This should be the full length seed
        final byte[] seed = randomSourceInternal.createSeedBytes(new SplitMix64(12345L));

        final int size = seed.length;
        Assert.assertNotEquals("Seed is empty", 0, size);

        if (randomSourceInternal.isNativeSeed(Integer.valueOf(0))) {
            Assert.assertEquals("Expect 4 bytes for Integer", 4, size);
        } else if (randomSourceInternal.isNativeSeed(Long.valueOf(0))) {
            Assert.assertEquals("Expect 8 bytes for Long", 8, size);
        } else if (randomSourceInternal.isNativeSeed(new int[0])) {
            Assert.assertEquals("Expect 4n bytes for int[]", 0, size % 4);
        } else if (randomSourceInternal.isNativeSeed(new long[0])) {
            Assert.assertEquals("Expect 8n bytes for long[]", 0, size % 8);
        } else {
            Assert.fail("Unknown native seed type");
        }
    }

    /**
     * Test the seed byte size against the expected value.
     *
     * <p>The expected values are maintained in a table and must be manually updated
     * for new generators. This test forms an additional cross-reference check that the
     * seed size in RandomSourceInternal has been correctly set and the size should map to
     * the array size in the RandomSource javadoc (if applicable).
     */
    @Test
    public void testCreateSeedBytes() {
        // This should be the full length seed
        final byte[] seed = randomSourceInternal.createSeedBytes(new SplitMix64(12345L));
        final int size = seed.length;

        final Integer expected = EXPECTED_SEED_BYTES.get(randomSourceInternal);
        Assert.assertNotNull("Missing expected seed byte size: " + randomSourceInternal, expected);
        Assert.assertEquals(randomSourceInternal.toString(),
                            expected.intValue(), size);
    }
}
