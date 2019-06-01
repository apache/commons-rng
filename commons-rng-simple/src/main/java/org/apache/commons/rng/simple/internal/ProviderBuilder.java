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

import java.util.Arrays;
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
import org.apache.commons.rng.core.source32.MultiplyWithCarry256;
import org.apache.commons.rng.core.source32.KISSRandom;
import org.apache.commons.rng.core.source32.XoRoShiRo64Star;
import org.apache.commons.rng.core.source32.XoRoShiRo64StarStar;
import org.apache.commons.rng.core.source32.XoShiRo128Plus;
import org.apache.commons.rng.core.source32.XoShiRo128StarStar;
import org.apache.commons.rng.core.source64.SplitMix64;
import org.apache.commons.rng.core.source64.XorShift1024Star;
import org.apache.commons.rng.core.source64.XorShift1024StarPhi;
import org.apache.commons.rng.core.source64.TwoCmres;
import org.apache.commons.rng.core.source64.MersenneTwister64;
import org.apache.commons.rng.core.source64.XoRoShiRo128Plus;
import org.apache.commons.rng.core.source64.XoRoShiRo128StarStar;
import org.apache.commons.rng.core.source64.XoShiRo256Plus;
import org.apache.commons.rng.core.source64.XoShiRo256StarStar;
import org.apache.commons.rng.core.source64.XoShiRo512Plus;
import org.apache.commons.rng.core.source64.XoShiRo512StarStar;

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
     * @throws IllegalStateException if data is missing to initialize the
     * generator implemented by the given {@code source}.
     */
    public static RestorableUniformRandomProvider create(RandomSourceInternal source) {
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
     * @throws IllegalStateException if data is missing to initialize the
     * generator implemented by the given {@code source}.
     */
    public static RestorableUniformRandomProvider create(RandomSourceInternal source,
                                                         Object seed,
                                                         Object[] args) {
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
     * The native seed type. Contains values for all native seed types and methods
     * to convert supported seed types to the native seed type.
     *
     * <p>Valid native seed types are:</p>
     * <ul>
     *  <li>{@code Integer}</li>
     *  <li>{@code Long}</li>
     *  <li>{@code int[]}</li>
     *  <li>{@code long[]}</li>
     * </ul>
     *
     * <p>Valid types for seed conversion are:</p>
     * <ul>
     *  <li>{@code Integer} (or {@code int})</li>
     *  <li>{@code Long} (or {@code long})</li>
     *  <li>{@code int[]}</li>
     *  <li>{@code long[]}</li>
     *  <li>{@code byte[]}</li>
     * </ul>
     */
    enum NativeSeedType {
        /** The seed type is {@code Integer}. */
        INT {
            @Override
            public Integer createSeed(int size) {
                return SeedFactory.createInt();
            }
            @Override
            protected Integer convert(Integer seed, int size) {
                return seed;
            }
            @Override
            protected Integer convert(Long seed, int size) {
                return LONG_TO_INT.convert(seed);
            }
            @Override
            protected Integer convert(int[] seed, int size) {
                return INT_ARRAY_TO_INT.convert(seed);
            }
            @Override
            protected Integer convert(long[] seed, int size) {
                return LONG_TO_INT.convert(LONG_ARRAY_TO_LONG.convert(seed));
            }
            @Override
            protected Integer convert(byte[] seed, int size) {
                return INT_ARRAY_TO_INT.convert(BYTE_ARRAY_TO_INT_ARRAY.convert(seed));
            }
        },
        /** The seed type is {@code Long}. */
        LONG {
            @Override
            public Long createSeed(int size) {
                return SeedFactory.createLong();
            }
            @Override
            protected Long convert(Integer seed, int size) {
                return INT_TO_LONG.convert(seed);
            }
            @Override
            protected Long convert(Long seed, int size) {
                return seed;
            }
            @Override
            protected Long convert(int[] seed, int size) {
                return INT_TO_LONG.convert(INT_ARRAY_TO_INT.convert(seed));
            }
            @Override
            protected Long convert(long[] seed, int size) {
                return LONG_ARRAY_TO_LONG.convert(seed);
            }
            @Override
            protected Long convert(byte[] seed, int size) {
                return LONG_ARRAY_TO_LONG.convert(BYTE_ARRAY_TO_LONG_ARRAY.convert(seed));
            }
        },
        /** The seed type is {@code int[]}. */
        INT_ARRAY {
            @Override
            public int[] createSeed(int size) {
                // Limit the number of calls to the synchronized method. The generator
                // will support self-seeding.
                return SeedFactory.createIntArray(Math.min(size, RANDOM_SEED_ARRAY_SIZE));
            }
            @Override
            protected int[] convert(Integer seed, int size) {
                return LONG_TO_INT_ARRAY.convert(INT_TO_LONG.convert(seed), size);
            }
            @Override
            protected int[] convert(Long seed, int size) {
                return LONG_TO_INT_ARRAY.convert(seed, size);
            }
            @Override
            protected int[] convert(int[] seed, int size) {
                return seed;
            }
            @Override
            protected int[] convert(long[] seed, int size) {
                return LONG_ARRAY_TO_INT_ARRAY.convert(seed);
            }
            @Override
            protected int[] convert(byte[] seed, int size) {
                return BYTE_ARRAY_TO_INT_ARRAY.convert(seed);
            }
        },
        /** The seed type is {@code long[]}. */
        LONG_ARRAY {
            @Override
            public long[] createSeed(int size) {
                // Limit the number of calls to the synchronized method. The generator
                // will support self-seeding.
                return SeedFactory.createLongArray(Math.min(size, RANDOM_SEED_ARRAY_SIZE));
            }
            @Override
            protected long[] convert(Integer seed, int size) {
                return LONG_TO_LONG_ARRAY.convert(INT_TO_LONG.convert(seed), size);
            }
            @Override
            protected long[] convert(Long seed, int size) {
                return LONG_TO_LONG_ARRAY.convert(seed, size);
            }
            @Override
            protected long[] convert(int[] seed, int size) {
                return INT_ARRAY_TO_LONG_ARRAY.convert(seed);
            }
            @Override
            protected long[] convert(long[] seed, int size) {
                return seed;
            }
            @Override
            protected long[] convert(byte[] seed, int size) {
                return BYTE_ARRAY_TO_LONG_ARRAY.convert(seed);
            }
        };

        /** Maximum length of the seed array (for creating array seeds). */
        private static final int RANDOM_SEED_ARRAY_SIZE = 128;
        /** Convert {@code Long} to {@code Integer}. */
        private static final Long2Int LONG_TO_INT = new Long2Int();
        /** Convert {@code Integer} to {@code Long}. */
        private static final Int2Long INT_TO_LONG = new Int2Long();
        /** Convert {@code Long} to {@code int[]}. */
        private static final Long2IntArray LONG_TO_INT_ARRAY = new Long2IntArray(0);
        /** Convert {@code Long} to {@code long[]}. */
        private static final Long2LongArray LONG_TO_LONG_ARRAY = new Long2LongArray(0);
        /** Convert {@code long[]} to {@code Long}. */
        private static final LongArray2Long LONG_ARRAY_TO_LONG = new LongArray2Long();
        /** Convert {@code int[]} to {@code Integer}. */
        private static final IntArray2Int INT_ARRAY_TO_INT = new IntArray2Int();
        /** Convert {@code long[]} to {@code int[]}. */
        private static final LongArray2IntArray LONG_ARRAY_TO_INT_ARRAY = new LongArray2IntArray();
        /** Convert {@code Long} to {@code long[]}. */
        private static final IntArray2LongArray INT_ARRAY_TO_LONG_ARRAY = new IntArray2LongArray();
        /** Convert {@code byte[]} to {@code int[]}. */
        private static final ByteArray2IntArray BYTE_ARRAY_TO_INT_ARRAY = new ByteArray2IntArray();
        /** Convert {@code byte[]} to {@code long[]}. */
        private static final ByteArray2LongArray BYTE_ARRAY_TO_LONG_ARRAY = new ByteArray2LongArray();

        /**
         * Creates the seed. The output seed type is determined by the native seed type. If the
         * output is an array the required size of the array can be specified.
         *
         * @param size The size of the seed (array types only).
         * @return the seed
         */
        public abstract Object createSeed(int size);

        /**
         * Converts the input seed from any of the supported seed types to the native seed type.
         * If the output is an array the required size of the array can be specified.
         *
         * @param seed Input seed.
         * @param size The size of the output seed (array types only).
         * @return the native seed.
         * @throw UnsupportedOperationException if the {@code seed} type is invalid.
         */
        public Object convertSeed(Object seed,
                                  int size) {
            // Convert to native type.
            // Each method must be overridden by specific implementations.

            if (seed instanceof Integer) {
                return convert((Integer) seed, size);
            } else if (seed instanceof Long) {
                return convert((Long) seed, size);
            } else if (seed instanceof int[]) {
                return convert((int[]) seed, size);
            } else if (seed instanceof long[]) {
                return convert((long[]) seed, size);
            } else if (seed instanceof byte[]) {
                return convert((byte[]) seed, size);
            }

            throw new UnsupportedOperationException("Unrecognized seed type");
        }

        /**
         * Convert the input {@code Integer} seed to the native seed type.
         *
         * @param seed Input seed.
         * @param size The size of the output seed (array types only).
         * @return the native seed.
         */
        protected abstract Object convert(Integer seed, int size);

        /**
         * Convert the input {@code Long} seed to the native seed type.
         *
         * @param seed Input seed.
         * @param size The size of the output seed (array types only).
         * @return the native seed.
         */
        protected abstract Object convert(Long seed, int size);

        /**
         * Convert the input {@code int[]} seed to the native seed type.
         *
         * @param seed Input seed.
         * @param size The size of the output seed (array types only).
         * @return the native seed.
         */
        protected abstract Object convert(int[] seed, int size);

        /**
         * Convert the input {@code long[]} seed to the native seed type.
         *
         * @param seed Input seed.
         * @param size The size of the output seed (array types only).
         * @return the native seed.
         */
        protected abstract Object convert(long[] seed, int size);

        /**
         * Convert the input {@code byte[]} seed to the native seed type.
         *
         * @param seed Input seed.
         * @param size The size of the output seed (array types only).
         * @return the native seed.
         */
        protected abstract Object convert(byte[] seed, int size);

        /**
         * Creates the native seed type.
         *
         * @param type Class of the native seed.
         * @return the native seed type
         */
        public static NativeSeedType createNativeSeedType(Class<?> type) {
            if (type.equals(Integer.class)) {
                return NativeSeedType.INT;
            } else if (type.equals(Long.class)) {
                return NativeSeedType.LONG;
            } else if (type.equals(int[].class)) {
                return NativeSeedType.INT_ARRAY;
            } else if (type.equals(long[].class)) {
                return NativeSeedType.LONG_ARRAY;
            } else {
                // Unsupported seed type
                throw new IllegalStateException(INTERNAL_ERROR_MSG);
            }
        }
    }

    /**
     * Identifiers of the generators.
     */
    public enum RandomSourceInternal {
        /** Source of randomness is {@link JDKRandom}. */
        JDK(JDKRandom.class,
            1,
            Long.class),
        /** Source of randomness is {@link Well512a}. */
        WELL_512_A(Well512a.class,
                   16,
                   int[].class),
        /** Source of randomness is {@link Well1024a}. */
        WELL_1024_A(Well1024a.class,
                    32,
                    int[].class),
        /** Source of randomness is {@link Well19937a}. */
        WELL_19937_A(Well19937a.class,
                     624,
                     int[].class),
        /** Source of randomness is {@link Well19937c}. */
        WELL_19937_C(Well19937c.class,
                     624,
                     int[].class),
        /** Source of randomness is {@link Well44497a}. */
        WELL_44497_A(Well44497a.class,
                     1391,
                     int[].class),
        /** Source of randomness is {@link Well44497b}. */
        WELL_44497_B(Well44497b.class,
                     1391,
                     int[].class),
        /** Source of randomness is {@link MersenneTwister}. */
        MT(MersenneTwister.class,
           624,
           int[].class),
        /** Source of randomness is {@link ISAACRandom}. */
        ISAAC(ISAACRandom.class,
              256,
              int[].class),
        /** Source of randomness is {@link SplitMix64}. */
        SPLIT_MIX_64(SplitMix64.class,
                     1,
                     Long.class),
        /** Source of randomness is {@link XorShift1024Star}. */
        XOR_SHIFT_1024_S(XorShift1024Star.class,
                         16,
                         long[].class),
        /** Source of randomness is {@link TwoCmres}. */
        TWO_CMRES(TwoCmres.class,
                  1,
                  Integer.class),
        /**
         * Source of randomness is {@link TwoCmres} with explicit selection
         * of the two subcycle generators.
         */
        TWO_CMRES_SELECT(TwoCmres.class,
                         1,
                         Integer.class,
                         Integer.TYPE,
                         Integer.TYPE),
        /** Source of randomness is {@link MersenneTwister64}. */
        MT_64(MersenneTwister64.class,
              312,
              long[].class),
        /** Source of randomness is {@link MultiplyWithCarry256}. */
        MWC_256(MultiplyWithCarry256.class,
                257,
                int[].class),
        /** Source of randomness is {@link KISSRandom}. */
        KISS(KISSRandom.class,
             4,
             int[].class),
        /** Source of randomness is {@link XorShift1024StarPhi}. */
        XOR_SHIFT_1024_S_PHI(XorShift1024StarPhi.class,
                             16,
                             long[].class),
        /** Source of randomness is {@link XoRoShiRo64Star}. */
        XO_RO_SHI_RO_64_S(XoRoShiRo64Star.class,
                          2,
                          int[].class),
        /** Source of randomness is {@link XoRoShiRo64StarStar}. */
        XO_RO_SHI_RO_64_SS(XoRoShiRo64StarStar.class,
                           2,
                           int[].class),
        /** Source of randomness is {@link XoShiRo128Plus}. */
        XO_SHI_RO_128_PLUS(XoShiRo128Plus.class,
                           4,
                           int[].class),
        /** Source of randomness is {@link XoShiRo128StarStar}. */
        XO_SHI_RO_128_SS(XoShiRo128StarStar.class,
                         4,
                         int[].class),
        /** Source of randomness is {@link XoRoShiRo128Plus}. */
        XO_RO_SHI_RO_128_PLUS(XoRoShiRo128Plus.class,
                              2,
                              long[].class),
        /** Source of randomness is {@link XoRoShiRo128StarStar}. */
        XO_RO_SHI_RO_128_SS(XoRoShiRo128StarStar.class,
                            2,
                            long[].class),
        /** Source of randomness is {@link XoShiRo256Plus}. */
        XO_SHI_RO_256_PLUS(XoShiRo256Plus.class,
                           4,
                           long[].class),
        /** Source of randomness is {@link XoShiRo256StarStar}. */
        XO_SHI_RO_256_SS(XoShiRo256StarStar.class,
                         4,
                         long[].class),
        /** Source of randomness is {@link XoShiRo512Plus}. */
        XO_SHI_RO_512_PLUS(XoShiRo512Plus.class,
                           8,
                           long[].class),
        /** Source of randomness is {@link XoShiRo512StarStar}. */
        XO_SHI_RO_512_SS(XoShiRo512StarStar.class,
                         8,
                         long[].class);

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
         * This is discovered using the constructor parameter types and cached.
         */
        private Constructor<?> constructor;

        /**
         * Create a new instance.
         *
         * @param rng Source type.
         * @param nativeSeedSize Native seed size (array types only).
         * @param args Data needed to create a generator instance. The first element
         * must be the native seed type.
         */
        RandomSourceInternal(Class<? extends UniformRandomProvider> rng,
                             int nativeSeedSize,
                             Class<?>... args) {
            this.rng = rng;
            this.nativeSeedSize = nativeSeedSize;
            this.args = Arrays.copyOf(args, args.length);
            // Look-up the native seed type from the class of the seed
            nativeSeedType = NativeSeedType.createNativeSeedType(args[0]);
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
        public <SEED> boolean isNativeSeed(SEED seed) {
            return seed != null && getSeed().equals(seed.getClass());
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
            checkSeed(nativeSeed);
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
        RestorableUniformRandomProvider create(Object seed) {
            // Convert seed to native type.
            final Object nativeSeed = convertSeed(seed);
            checkSeed(nativeSeed);
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
        RestorableUniformRandomProvider create(Object seed,
                                               Object[] constructorArgs) {
            final Object nativeSeed = createNativeSeed(seed);

            // Build a single array with all the arguments to be passed
            // (in the right order) to the constructor.
            Object[] all = new Object[constructorArgs.length + 1];
            all[0] = nativeSeed;
            System.arraycopy(constructorArgs, 0, all, 1, constructorArgs.length);

            // Instantiate.
            return create(getConstructor(), all);
        }

        /**
         * Creates a native seed.
         *
         * <p>This method should be over-ridden to satisfy seed requirements for the generator,
         * for example if a seed must contain non-zero bits.</p>
         *
         * @return the native seed
         */
        Object createSeed() {
            return nativeSeedType.createSeed(nativeSeedSize);
        }


        /**
         * Converts a seed from any of the supported seed types to a native seed.
         *
         * @param seed Input seed (must not be null).
         * @return the native seed
         * @throw UnsupportedOperationException if the {@code seed} type is invalid.
         */
        Object convertSeed(Object seed) {
            return nativeSeedType.convertSeed(seed, nativeSeedSize);
        }

        /**
         * Creates a native seed from any of the supported seed types.
         *
         * @param seed Input seed (may be null).
         * @return the native seed.
         * @throw UnsupportedOperationException if the {@code seed} type cannot be converted.
         */
        private Object createNativeSeed(Object seed) {
            // Convert to native type.
            Object nativeSeed;

            if (seed == null) {
                nativeSeed = createSeed();
            } else {
                nativeSeed = convertSeed(seed);
            }

            checkSeed(nativeSeed);

            return nativeSeed;
        }

        /**
         * Check the seed is a native seed.
         *
         * @param seed the seed
         * @throws IllegalStateException if not a native seed.
         */
        private void checkSeed(Object seed) {
            if (!isNativeSeed(seed)) {
                // Conversion setup is wrong.
                throw new IllegalStateException(INTERNAL_ERROR_MSG);
            }
        }

        /**
         * Gets the constructor.
         *
         * @return the RNG constructor.
         */
        private Constructor<?> getConstructor() {
            Constructor<?> con = constructor;
            if (con == null) {
                con = createConstructor();
                constructor = con;
            }
            return con;
        }
        /**
         * Creates a constructor.
         *
         * @return a RNG constructor.
         */
        private Constructor<?> createConstructor() {
            try {
                return getRng().getConstructor(getArgs());
            } catch (NoSuchMethodException e) {
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
        private static RestorableUniformRandomProvider create(Constructor<?> rng,
                                                              Object[] args) {
            try {
                return (RestorableUniformRandomProvider) rng.newInstance(args);
            } catch (InvocationTargetException e) {
                throw new IllegalStateException(INTERNAL_ERROR_MSG, e);
            } catch (InstantiationException e) {
                throw new IllegalStateException(INTERNAL_ERROR_MSG, e);
            } catch (IllegalAccessException e) {
                throw new IllegalStateException(INTERNAL_ERROR_MSG, e);
            }
        }
    }
}
