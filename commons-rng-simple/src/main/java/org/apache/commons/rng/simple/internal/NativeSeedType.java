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

import org.apache.commons.rng.core.util.NumberFactory;

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
 *
 * @since 1.3
 */
public enum NativeSeedType {
    /** The seed type is {@code Integer}. */
    INT(Integer.class, 4) {
        @Override
        public Integer createSeed(int size, int from, int to) {
            return SeedFactory.createInt();
        }
        @Override
        protected Integer convert(Integer seed, int size) {
            return seed;
        }
        @Override
        protected Integer convert(Long seed, int size) {
            return Conversions.long2Int(seed);
        }
        @Override
        protected Integer convert(int[] seed, int size) {
            return Conversions.intArray2Int(seed);
        }
        @Override
        protected Integer convert(long[] seed, int size) {
            return Conversions.longArray2Int(seed);
        }
        @Override
        protected Integer convert(byte[] seed, int size) {
            return Conversions.byteArray2Int(seed);
        }
    },
    /** The seed type is {@code Long}. */
    LONG(Long.class, 8) {
        @Override
        public Long createSeed(int size, int from, int to) {
            return SeedFactory.createLong();
        }
        @Override
        protected Long convert(Integer seed, int size) {
            return Conversions.int2Long(seed);
        }
        @Override
        protected Long convert(Long seed, int size) {
            return seed;
        }
        @Override
        protected Long convert(int[] seed, int size) {
            return Conversions.intArray2Long(seed);
        }
        @Override
        protected Long convert(long[] seed, int size) {
            return Conversions.longArray2Long(seed);
        }
        @Override
        protected Long convert(byte[] seed, int size) {
            return Conversions.byteArray2Long(seed);
        }
    },
    /** The seed type is {@code int[]}. */
    INT_ARRAY(int[].class, 4) {
        @Override
        public int[] createSeed(int size, int from, int to) {
            // Limit the number of calls to the synchronized method. The generator
            // will support self-seeding.
            return SeedFactory.createIntArray(Math.min(size, RANDOM_SEED_ARRAY_SIZE),
                                              from, to);
        }
        @Override
        protected int[] convert(Integer seed, int size) {
            return Conversions.int2IntArray(seed, size);
        }
        @Override
        protected int[] convert(Long seed, int size) {
            return Conversions.long2IntArray(seed, size);
        }
        @Override
        protected int[] convert(int[] seed, int size) {
            return seed;
        }
        @Override
        protected int[] convert(long[] seed, int size) {
            // Avoid zero filling seeds that are too short
            return Conversions.longArray2IntArray(seed,
                Math.min(size, Conversions.intSizeFromLongSize(seed.length)));
        }
        @Override
        protected int[] convert(byte[] seed, int size) {
            // Avoid zero filling seeds that are too short
            return Conversions.byteArray2IntArray(seed,
                Math.min(size, Conversions.intSizeFromByteSize(seed.length)));
        }
    },
    /** The seed type is {@code long[]}. */
    LONG_ARRAY(long[].class, 8) {
        @Override
        public long[] createSeed(int size, int from, int to) {
            // Limit the number of calls to the synchronized method. The generator
            // will support self-seeding.
            return SeedFactory.createLongArray(Math.min(size, RANDOM_SEED_ARRAY_SIZE),
                                               from, to);
        }
        @Override
        protected long[] convert(Integer seed, int size) {
            return Conversions.int2LongArray(seed, size);
        }
        @Override
        protected long[] convert(Long seed, int size) {
            return Conversions.long2LongArray(seed, size);
        }
        @Override
        protected long[] convert(int[] seed, int size) {
            // Avoid zero filling seeds that are too short
            return Conversions.intArray2LongArray(seed,
                Math.min(size, Conversions.longSizeFromIntSize(seed.length)));
        }
        @Override
        protected long[] convert(long[] seed, int size) {
            return seed;
        }
        @Override
        protected long[] convert(byte[] seed, int size) {
            // Avoid zero filling seeds that are too short
            return Conversions.byteArray2LongArray(seed,
                Math.min(size, Conversions.longSizeFromByteSize(seed.length)));
        }
    };

    /** Error message for unrecognized seed types. */
    private static final String UNRECOGNISED_SEED = "Unrecognized seed type: ";
    /** Maximum length of the seed array (for creating array seeds). */
    private static final int RANDOM_SEED_ARRAY_SIZE = 128;

    /** Define the class type of the native seed. */
    private final Class<?> type;

    /**
     * Define the number of bytes required to represent the native seed. If the type is
     * an array then this represents the size of a single value of the type.
     */
    private final int bytes;

    /**
     * Instantiates a new native seed type.
     *
     * @param type Define the class type of the native seed.
     * @param bytes Define the number of bytes required to represent the native seed.
     */
    NativeSeedType(Class<?> type, int bytes) {
        this.type = type;
        this.bytes = bytes;
    }

    /**
     * Gets the class type of the native seed.
     *
     * @return the type
     */
    public Class<?> getType() {
        return type;
    }

    /**
     * Gets the number of bytes required to represent the native seed type. If the type is
     * an array then this represents the size of a single value of the type.
     *
     * @return the number of bytes
     */
    public int getBytes() {
        return bytes;
    }

    /**
     * Creates the seed. The output seed type is determined by the native seed type. If the
     * output is an array the required size of the array can be specified.
     *
     * @param size The size of the seed (array types only).
     * @return the seed
     */
    public Object createSeed(int size) {
        // Maintain behaviour since 1.3 to ensure position [0] of array seeds is non-zero.
        return createSeed(size, 0, Math.min(size, 1));
    }

    /**
     * Creates the seed. The output seed type is determined by the native seed type. If
     * the output is an array the required size of the array can be specified and a
     * sub-range that must not be all-zero.
     *
     * @param size The size of the seed (array types only).
     * @param from The start of the not all-zero sub-range (inclusive; array types only).
     * @param to The end of the not all-zero sub-range (exclusive; array types only).
     * @return the seed
     * @throws IndexOutOfBoundsException if the sub-range is out of bounds
     * @since 1.5
     */
    public abstract Object createSeed(int size, int from, int to);

    /**
     * Converts the input seed from any of the supported seed types to the native seed type.
     * If the output is an array the required size of the array can be specified.
     *
     * @param seed Input seed.
     * @param size The size of the output seed (array types only).
     * @return the native seed.
     * @throws UnsupportedOperationException if the {@code seed} type is invalid.
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

        throw new UnsupportedOperationException(unrecognizedSeedMessage(seed));
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
     * Converts the input seed from any of the supported seed types to bytes.
     *
     * @param seed Input seed.
     * @return the seed bytes.
     * @throws UnsupportedOperationException if the {@code seed} type is invalid.
     */
    public static byte[] convertSeedToBytes(Object seed) {
        if (seed instanceof Integer) {
            return NumberFactory.makeByteArray((Integer) seed);
        } else if (seed instanceof Long) {
            return NumberFactory.makeByteArray((Long) seed);
        } else if (seed instanceof int[]) {
            return NumberFactory.makeByteArray((int[]) seed);
        } else if (seed instanceof long[]) {
            return NumberFactory.makeByteArray((long[]) seed);
        } else if (seed instanceof byte[]) {
            return (byte[]) seed;
        }

        throw new UnsupportedOperationException(unrecognizedSeedMessage(seed));
    }

    /**
     * Create an unrecognized seed message. This will add the class type of the seed.
     *
     * @param seed the seed
     * @return the message
     */
    private static String unrecognizedSeedMessage(Object seed) {
        return UNRECOGNISED_SEED + ((seed == null) ? "null" : seed.getClass().getName());
    }
}
