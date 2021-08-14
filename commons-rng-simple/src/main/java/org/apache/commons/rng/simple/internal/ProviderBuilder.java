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

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import org.apache.commons.rng.UniformRandomProvider;
import org.apache.commons.rng.RestorableUniformRandomProvider;
import org.apache.commons.rng.core.source32.JDKRandom;
import org.apache.commons.rng.core.source32.Well512a;
import org.apache.commons.rng.core.source32.Well1024a;
import org.apache.commons.rng.core.source32.Well19937a;
import org.apache.commons.rng.core.source32.Well19937c;
import org.apache.commons.rng.core.source32.Well44497a;
import org.apache.commons.rng.core.source32.Well44497b;
import org.apache.commons.rng.core.source32.ISAACRandom;
import org.apache.commons.rng.core.source32.MersenneTwister;
import org.apache.commons.rng.core.source32.MiddleSquareWeylSequence;
import org.apache.commons.rng.core.source32.MultiplyWithCarry256;
import org.apache.commons.rng.core.source32.KISSRandom;
import org.apache.commons.rng.core.source32.XoRoShiRo64Star;
import org.apache.commons.rng.core.source32.XoRoShiRo64StarStar;
import org.apache.commons.rng.core.source32.XoShiRo128Plus;
import org.apache.commons.rng.core.source32.XoShiRo128PlusPlus;
import org.apache.commons.rng.core.source32.XoShiRo128StarStar;
import org.apache.commons.rng.core.source32.PcgXshRr32;
import org.apache.commons.rng.core.source32.PcgXshRs32;
import org.apache.commons.rng.core.source32.PcgMcgXshRr32;
import org.apache.commons.rng.core.source32.PcgMcgXshRs32;
import org.apache.commons.rng.core.source32.DotyHumphreySmallFastCounting32;
import org.apache.commons.rng.core.source32.JenkinsSmallFast32;
import org.apache.commons.rng.core.source64.SplitMix64;
import org.apache.commons.rng.core.source64.XorShift1024Star;
import org.apache.commons.rng.core.source64.XorShift1024StarPhi;
import org.apache.commons.rng.core.source64.TwoCmres;
import org.apache.commons.rng.core.source64.XoRoShiRo1024PlusPlus;
import org.apache.commons.rng.core.source64.XoRoShiRo1024Star;
import org.apache.commons.rng.core.source64.XoRoShiRo1024StarStar;
import org.apache.commons.rng.core.source64.MersenneTwister64;
import org.apache.commons.rng.core.source64.XoRoShiRo128Plus;
import org.apache.commons.rng.core.source64.XoRoShiRo128PlusPlus;
import org.apache.commons.rng.core.source64.XoRoShiRo128StarStar;
import org.apache.commons.rng.core.source64.XoShiRo256Plus;
import org.apache.commons.rng.core.source64.XoShiRo256PlusPlus;
import org.apache.commons.rng.core.source64.XoShiRo256StarStar;
import org.apache.commons.rng.core.source64.XoShiRo512Plus;
import org.apache.commons.rng.core.source64.XoShiRo512PlusPlus;
import org.apache.commons.rng.core.source64.XoShiRo512StarStar;
import org.apache.commons.rng.core.source64.PcgRxsMXs64;
import org.apache.commons.rng.core.source64.DotyHumphreySmallFastCounting64;
import org.apache.commons.rng.core.source64.JenkinsSmallFast64;

/**
 * RNG builder.
 * <p>
 * It uses reflection to find the factory method of the RNG implementation,
 * and performs seed type conversions.
 * </p>
 */
public final class ProviderBuilder {
    /** Error message. */
    private static final String INTERNAL_ERROR_MSG = "Internal error: Please file a bug report";

    /**
     * Class only contains static method.
     */
    private ProviderBuilder() {}

    /**
     * Creates a RNG instance.
     *
     * @param source RNG specification.
     * @return a new RNG instance.
     * @throws IllegalArgumentException if argument data to initialize the
     * generator implemented by the given {@code source} is missing.
     * @since 1.3
     */
    public static RestorableUniformRandomProvider create(final RandomSourceInternal source) {
        // Delegate to the random source allowing generator specific implementations.
        return source.create();
    }

    /**
     * Creates a RNG instance.
     *
     * @param source RNG specification.
     * @param seed Seed value.  It can be {@code null} (in which case a
     * random value will be used).
     * @param args Additional arguments to the implementation's constructor.
     * @return a new RNG instance.
     * @throws UnsupportedOperationException if the seed type is invalid.
     * @throws IllegalArgumentException if argument data to initialize the
     * generator implemented by the given {@code source} is invalid.
     */
    public static RestorableUniformRandomProvider create(final RandomSourceInternal source,
                                                         final Object seed,
                                                         final Object[] args) {
        // Delegate to the random source allowing generator specific implementations.
        // This method checks arguments for null and calls the appropriate internal method.
        if (args != null) {
            return source.create(seed, args);
        }
        return seed == null ?
                source.create() :
                source.create(seed);
    }

    /**
     * Identifiers of the generators.
     */
    public enum RandomSourceInternal {
        /** Source of randomness is {@link JDKRandom}. */
        JDK(JDKRandom.class,
            1,
            NativeSeedType.LONG),
        /** Source of randomness is {@link Well512a}. */
        WELL_512_A(Well512a.class,
                   16,
                   NativeSeedType.INT_ARRAY),
        /** Source of randomness is {@link Well1024a}. */
        WELL_1024_A(Well1024a.class,
                    32,
                    NativeSeedType.INT_ARRAY),
        /** Source of randomness is {@link Well19937a}. */
        WELL_19937_A(Well19937a.class,
                     624,
                     NativeSeedType.INT_ARRAY),
        /** Source of randomness is {@link Well19937c}. */
        WELL_19937_C(Well19937c.class,
                     624,
                     NativeSeedType.INT_ARRAY),
        /** Source of randomness is {@link Well44497a}. */
        WELL_44497_A(Well44497a.class,
                     1391,
                     NativeSeedType.INT_ARRAY),
        /** Source of randomness is {@link Well44497b}. */
        WELL_44497_B(Well44497b.class,
                     1391,
                     NativeSeedType.INT_ARRAY),
        /** Source of randomness is {@link MersenneTwister}. */
        MT(MersenneTwister.class,
           624,
           NativeSeedType.INT_ARRAY),
        /** Source of randomness is {@link ISAACRandom}. */
        ISAAC(ISAACRandom.class,
              256,
              NativeSeedType.INT_ARRAY),
        /** Source of randomness is {@link SplitMix64}. */
        SPLIT_MIX_64(SplitMix64.class,
                     1,
                     NativeSeedType.LONG),
        /** Source of randomness is {@link XorShift1024Star}. */
        XOR_SHIFT_1024_S(XorShift1024Star.class,
                         16,
                         NativeSeedType.LONG_ARRAY),
        /** Source of randomness is {@link TwoCmres}. */
        TWO_CMRES(TwoCmres.class,
                  1,
                  NativeSeedType.INT),
        /**
         * Source of randomness is {@link TwoCmres} with explicit selection
         * of the two subcycle generators.
         */
        TWO_CMRES_SELECT(TwoCmres.class,
                         1,
                         NativeSeedType.INT,
                         Integer.TYPE,
                         Integer.TYPE),
        /** Source of randomness is {@link MersenneTwister64}. */
        MT_64(MersenneTwister64.class,
              312,
              NativeSeedType.LONG_ARRAY),
        /** Source of randomness is {@link MultiplyWithCarry256}. */
        MWC_256(MultiplyWithCarry256.class,
                257,
                NativeSeedType.INT_ARRAY),
        /** Source of randomness is {@link KISSRandom}. */
        KISS(KISSRandom.class,
             4,
             NativeSeedType.INT_ARRAY),
        /** Source of randomness is {@link XorShift1024StarPhi}. */
        XOR_SHIFT_1024_S_PHI(XorShift1024StarPhi.class,
                             16,
                             NativeSeedType.LONG_ARRAY),
        /** Source of randomness is {@link XoRoShiRo64Star}. */
        XO_RO_SHI_RO_64_S(XoRoShiRo64Star.class,
                          2,
                          NativeSeedType.INT_ARRAY),
        /** Source of randomness is {@link XoRoShiRo64StarStar}. */
        XO_RO_SHI_RO_64_SS(XoRoShiRo64StarStar.class,
                           2,
                           NativeSeedType.INT_ARRAY),
        /** Source of randomness is {@link XoShiRo128Plus}. */
        XO_SHI_RO_128_PLUS(XoShiRo128Plus.class,
                           4,
                           NativeSeedType.INT_ARRAY),
        /** Source of randomness is {@link XoShiRo128StarStar}. */
        XO_SHI_RO_128_SS(XoShiRo128StarStar.class,
                         4,
                         NativeSeedType.INT_ARRAY),
        /** Source of randomness is {@link XoRoShiRo128Plus}. */
        XO_RO_SHI_RO_128_PLUS(XoRoShiRo128Plus.class,
                              2,
                              NativeSeedType.LONG_ARRAY),
        /** Source of randomness is {@link XoRoShiRo128StarStar}. */
        XO_RO_SHI_RO_128_SS(XoRoShiRo128StarStar.class,
                            2,
                            NativeSeedType.LONG_ARRAY),
        /** Source of randomness is {@link XoShiRo256Plus}. */
        XO_SHI_RO_256_PLUS(XoShiRo256Plus.class,
                           4,
                           NativeSeedType.LONG_ARRAY),
        /** Source of randomness is {@link XoShiRo256StarStar}. */
        XO_SHI_RO_256_SS(XoShiRo256StarStar.class,
                         4,
                         NativeSeedType.LONG_ARRAY),
        /** Source of randomness is {@link XoShiRo512Plus}. */
        XO_SHI_RO_512_PLUS(XoShiRo512Plus.class,
                           8,
                           NativeSeedType.LONG_ARRAY),
        /** Source of randomness is {@link XoShiRo512StarStar}. */
        XO_SHI_RO_512_SS(XoShiRo512StarStar.class,
                         8,
                         NativeSeedType.LONG_ARRAY),
        /** Source of randomness is {@link PcgXshRr32}. */
        PCG_XSH_RR_32(PcgXshRr32.class,
                2,
                NativeSeedType.LONG_ARRAY),
        /** Source of randomness is {@link PcgXshRs32}. */
        PCG_XSH_RS_32(PcgXshRs32.class,
                2,
                NativeSeedType.LONG_ARRAY),
        /** Source of randomness is {@link PcgRxsMXs64}. */
        PCG_RXS_M_XS_64(PcgRxsMXs64.class,
                2,
                NativeSeedType.LONG_ARRAY),
        /** Source of randomness is {@link PcgMcgXshRr32}. */
        PCG_MCG_XSH_RR_32(PcgMcgXshRr32.class,
                1,
                NativeSeedType.LONG),
        /** Source of randomness is {@link PcgMcgXshRs32}. */
        PCG_MCG_XSH_RS_32(PcgMcgXshRs32.class,
                1,
                NativeSeedType.LONG),
        /** Source of randomness is {@link MiddleSquareWeylSequence}. */
        MSWS(MiddleSquareWeylSequence.class,
             3,
             NativeSeedType.LONG_ARRAY) {
            @Override
            protected Object createSeed() {
                return createMswsSeed(SeedFactory.createLong());
            }

            @Override
            protected Object convertSeed(final Object seed) {
                // Allow seeding with primitives to generate a good seed
                if (seed instanceof Integer) {
                    return createMswsSeed((Integer) seed);
                } else if (seed instanceof Long) {
                    return createMswsSeed((Long) seed);
                }
                // Other types (e.g. the native long[]) are handled by the default conversion
                return super.convertSeed(seed);
            }

            @Override
            protected byte[] createByteArraySeed(final UniformRandomProvider source) {
                return NativeSeedType.convertSeedToBytes(createMswsSeed(source));
            }

            /**
             * Creates the full length seed array from the input seed.
             *
             * @param seed the seed
             * @return the seed array
             */
            private long[] createMswsSeed(final long seed) {
                return createMswsSeed(new SplitMix64(seed));
            }

            /**
             * Creates the full length seed array from the input seed using the method
             * recommended for the generator. This is a high quality Weyl increment composed
             * of a hex character permutation.
             *
             * @param source Source of randomness.
             * @return the seed array
             */
            private long[] createMswsSeed(final UniformRandomProvider source) {
                final long increment = SeedUtils.createLongHexPermutation(source);
                // The initial state should not be low complexity but the Weyl
                // state can be any number.
                final long state = increment;
                final long weylState = source.nextLong();
                return new long[] {state, weylState, increment};
            }
        },
        /** Source of randomness is {@link DotyHumphreySmallFastCounting32}. */
        SFC_32(DotyHumphreySmallFastCounting32.class,
               3,
               NativeSeedType.INT_ARRAY),
        /** Source of randomness is {@link DotyHumphreySmallFastCounting64}. */
        SFC_64(DotyHumphreySmallFastCounting64.class,
               3,
               NativeSeedType.LONG_ARRAY),
        /** Source of randomness is {@link JenkinsSmallFast32}. */
        JSF_32(JenkinsSmallFast32.class,
               1,
               NativeSeedType.INT),
        /** Source of randomness is {@link JenkinsSmallFast64}. */
        JSF_64(JenkinsSmallFast64.class,
               1,
               NativeSeedType.LONG),
        /** Source of randomness is {@link XoShiRo128PlusPlus}. */
        XO_SHI_RO_128_PP(XoShiRo128PlusPlus.class,
                         4,
                         NativeSeedType.INT_ARRAY),
        /** Source of randomness is {@link XoRoShiRo128PlusPlus}. */
        XO_RO_SHI_RO_128_PP(XoRoShiRo128PlusPlus.class,
                            2,
                            NativeSeedType.LONG_ARRAY),
        /** Source of randomness is {@link XoShiRo256PlusPlus}. */
        XO_SHI_RO_256_PP(XoShiRo256PlusPlus.class,
                         4,
                         NativeSeedType.LONG_ARRAY),
        /** Source of randomness is {@link XoShiRo512PlusPlus}. */
        XO_SHI_RO_512_PP(XoShiRo512PlusPlus.class,
                         8,
                         NativeSeedType.LONG_ARRAY),
        /** Source of randomness is {@link XoRoShiRo1024PlusPlus}. */
        XO_RO_SHI_RO_1024_PP(XoRoShiRo1024PlusPlus.class,
                             16,
                             NativeSeedType.LONG_ARRAY),
        /** Source of randomness is {@link XoRoShiRo1024Star}. */
        XO_RO_SHI_RO_1024_S(XoRoShiRo1024Star.class,
                            16,
                            NativeSeedType.LONG_ARRAY),
        /** Source of randomness is {@link XoRoShiRo1024StarStar}. */
        XO_RO_SHI_RO_1024_SS(XoRoShiRo1024StarStar.class,
                             16,
                             NativeSeedType.LONG_ARRAY),
        /** Source of randomness is {@link PcgXshRr32}. */
        PCG_XSH_RR_32_OS(PcgXshRr32.class,
                1,
                NativeSeedType.LONG),
        /** Source of randomness is {@link PcgXshRs32}. */
        PCG_XSH_RS_32_OS(PcgXshRs32.class,
                1,
                NativeSeedType.LONG),
        /** Source of randomness is {@link PcgRxsMXs64}. */
        PCG_RXS_M_XS_64_OS(PcgRxsMXs64.class,
                1,
                NativeSeedType.LONG);

        /** Source type. */
        private final Class<? extends UniformRandomProvider> rng;
        /** Native seed size. Used for array seeds. */
        private final int nativeSeedSize;
        /** Define the parameter types of the data needed to build the generator. */
        private final Class<?>[] args;
        /** Native seed type. Used to create a seed or convert input seeds. */
        private final NativeSeedType nativeSeedType;
        /**
         * The constructor.
         * This is discovered using the constructor parameter types and stored for re-use.
         */
        private Constructor<?> rngConstructor;

        /**
         * Create a new instance.
         *
         * @param rng Source type.
         * @param nativeSeedSize Native seed size (array types only).
         * @param nativeSeedType Native seed type.
         * @param args Additional data needed to create a generator instance.
         */
        RandomSourceInternal(final Class<? extends UniformRandomProvider> rng,
                             final int nativeSeedSize,
                             final NativeSeedType nativeSeedType,
                             final Class<?>... args) {
            this.rng = rng;
            this.nativeSeedSize = nativeSeedSize;
            this.nativeSeedType = nativeSeedType;
            // Build the complete list of class types for the constructor
            this.args = (Class<?>[]) Array.newInstance(args.getClass().getComponentType(), 1 + args.length);
            this.args[0] = nativeSeedType.getType();
            System.arraycopy(args, 0, this.args, 1, args.length);
        }

        /**
         * Gets the implementing class of the random source.
         *
         * @return the random source class.
         */
        public Class<?> getRng() {
            return rng;
        }

        /**
         * Gets the class of the native seed.
         *
         * @return the seed class.
         */
        Class<?> getSeed() {
            return args[0];
        }

        /**
         * Gets the parameter types of the data needed to build the generator.
         *
         * @return the data needed to build the generator.
         */
        Class<?>[] getArgs() {
            return args;
        }

        /**
         * Checks whether the type of given {@code seed} is the native type
         * of the implementation.
         *
         * @param <SEED> Seed type.
         *
         * @param seed Seed value.
         * @return {@code true} if the seed can be passed to the builder
         * for this RNG type.
         */
        public <SEED> boolean isNativeSeed(final SEED seed) {
            return seed != null && getSeed().equals(seed.getClass());
        }

        /**
         * Gets the number of seed bytes required to seed the implementing class represented by
         * this random source.
         *
         * @return the number of seed bytes
         */
        private int getSeedByteSize() {
            return nativeSeedSize * nativeSeedType.getBytes();
        }

        /**
         * Creates a RNG instance.
         *
         * <p>This method can be over-ridden to allow fast construction of a generator
         * with low seeding cost that has no additional constructor arguments.</p>
         *
         * @return a new RNG instance.
         */
        RestorableUniformRandomProvider create() {
            // Create a seed.
            final Object nativeSeed = createSeed();
            // Instantiate.
            return create(getConstructor(), new Object[] {nativeSeed});
        }

        /**
         * Creates a RNG instance. It is assumed the seed is not {@code null}.
         *
         * <p>This method can be over-ridden to allow fast construction of a generator
         * with low seed conversion cost that has no additional constructor arguments.</p>
         *
         * @param seed Seed value. It must not be {@code null}.
         * @return a new RNG instance.
         * @throws UnsupportedOperationException if the seed type is invalid.
         */
        RestorableUniformRandomProvider create(final Object seed) {
            // Convert seed to native type.
            final Object nativeSeed = convertSeed(seed);
            // Instantiate.
            return create(getConstructor(), new Object[] {nativeSeed});
        }

        /**
         * Creates a RNG instance. This constructs a RNG using reflection and will error
         * if the constructor arguments do not match those required by the RNG's constructor.
         *
         * @param seed Seed value. It can be {@code null} (in which case a suitable
         * seed will be generated).
         * @param constructorArgs Additional arguments to the implementation's constructor.
         * It must not be {@code null}.
         * @return a new RNG instance.
         * @throws UnsupportedOperationException if the seed type is invalid.
         */
        RestorableUniformRandomProvider create(final Object seed,
                                               final Object[] constructorArgs) {
            final Object nativeSeed = createNativeSeed(seed);

            // Build a single array with all the arguments to be passed
            // (in the right order) to the constructor.
            final Object[] all = new Object[constructorArgs.length + 1];
            all[0] = nativeSeed;
            System.arraycopy(constructorArgs, 0, all, 1, constructorArgs.length);

            // Instantiate.
            return create(getConstructor(), all);
        }

        /**
         * Creates a native seed.
         *
         * <p>The default implementation creates a seed of the native type and, for array seeds,
         * ensures not all bits are zero.</p>
         *
         * <p>This method should be over-ridden to satisfy seed requirements for the generator.</p>
         *
         * @return the native seed
         * @since 1.3
         */
        protected Object createSeed() {
            return nativeSeedType.createSeed(nativeSeedSize);
        }

        /**
         * Creates a {@code byte[]} seed using the provided source of randomness.
         *
         * <p>The default implementation creates a full-length seed and ensures not all bits
         * are zero.</p>
         *
         * <p>This method should be over-ridden to satisfy seed requirements for the generator.</p>
         *
         * @param source Source of randomness.
         * @return the byte[] seed
         * @since 1.3
         */
        protected byte[] createByteArraySeed(final UniformRandomProvider source) {
            return SeedFactory.createByteArray(source, getSeedByteSize());
        }

        /**
         * Converts a seed from any of the supported seed types to a native seed.
         *
         * <p>The default implementation delegates to the native seed type conversion.</p>
         *
         * <p>This method should be over-ridden to satisfy seed requirements for the generator.</p>
         *
         * @param seed Input seed (must not be null).
         * @return the native seed
         * @throws UnsupportedOperationException if the {@code seed} type is invalid.
         * @since 1.3
         */
        protected Object convertSeed(final Object seed) {
            return nativeSeedType.convertSeed(seed, nativeSeedSize);
        }

        /**
         * Creates a native seed from any of the supported seed types.
         *
         * @param seed Input seed (may be null).
         * @return the native seed.
         * @throws UnsupportedOperationException if the {@code seed} type cannot be converted.
         */
        private Object createNativeSeed(final Object seed) {
            return seed == null ?
                createSeed() :
                convertSeed(seed);
        }

        /**
         * Creates a seed suitable for the implementing class represented by this random source.
         *
         * <p>It will satisfy the seed size and any other seed requirements for the
         * implementing class. The seed is converted from the native type to bytes.</p>
         *
         * @return the seed bytes
         * @since 1.3
         */
        public final byte[] createSeedBytes() {
            // Custom implementations can override createSeed
            final Object seed = createSeed();
            return NativeSeedType.convertSeedToBytes(seed);
        }

        /**
         * Creates a seed suitable for the implementing class represented by this random source
         * using the supplied source of randomness.
         *
         * <p>It will satisfy the seed size and any other seed requirements for the
         * implementing class. The seed is converted from the native type to bytes.</p>
         *
         * @param source Source of randomness.
         * @return the seed bytes
         * @since 1.3
         */
        public final byte[] createSeedBytes(final UniformRandomProvider source) {
            // Custom implementations can override createByteArraySeed
            return createByteArraySeed(source);
        }

        /**
         * Gets the constructor.
         *
         * @return the RNG constructor.
         */
        private Constructor<?> getConstructor() {
            // The constructor never changes so it is stored for re-use.
            Constructor<?> constructor = rngConstructor;
            if (constructor == null) {
                // If null this is either the first attempt to find it or
                // look-up previously failed and this method will throw
                // upon each invocation.
                constructor = createConstructor();
                rngConstructor = constructor;
            }
            return constructor;
        }

        /**
         * Creates a constructor.
         *
         * @return a RNG constructor.
         */
        private Constructor<?> createConstructor() {
            try {
                return getRng().getConstructor(getArgs());
            } catch (final NoSuchMethodException e) {
                // Info in "RandomSourceInternal" is inconsistent with the
                // constructor of the implementation.
                throw new IllegalStateException(INTERNAL_ERROR_MSG, e);
            }
        }

        /**
         * Creates a RNG.
         *
         * @param rng RNG specification.
         * @param args Arguments to the implementation's constructor.
         * @return a new RNG instance.
         */
        private static RestorableUniformRandomProvider create(final Constructor<?> rng,
                                                              final Object[] args) {
            try {
                return (RestorableUniformRandomProvider) rng.newInstance(args);
            } catch (final InvocationTargetException | InstantiationException | IllegalAccessException e) {
                throw new IllegalStateException(INTERNAL_ERROR_MSG, e);
            }
        }
    }
}
