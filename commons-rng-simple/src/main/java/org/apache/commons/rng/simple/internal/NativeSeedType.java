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
        public Integer createSeed(final int size) {
            return SeedFactory.createInt();
        }
        @Override
        protected Integer convert(final Integer seed, final int size) {
            return seed;
        }
        @Override
        protected Integer convert(final Long seed, final int size) {
            return LONG_TO_INT.convert(seed);
        }
        @Override
        protected Integer convert(final int[] seed, final int size) {
            return INT_ARRAY_TO_INT.convert(seed);
        }
        @Override
        protected Integer convert(final long[] seed, final int size) {
            return LONG_TO_INT.convert(LONG_ARRAY_TO_LONG.convert(seed));
        }
        @Override
        protected Integer convert(final byte[] seed, final int size) {
            return INT_ARRAY_TO_INT.convert(BYTE_ARRAY_TO_INT_ARRAY.convert(seed));
        }
    },
    /** The seed type is {@code Long}. */
    LONG(Long.class, 8) {
        @Override
        public Long createSeed(final int size) {
            return SeedFactory.createLong();
        }
        @Override
        protected Long convert(final Integer seed, final int size) {
            return INT_TO_LONG.convert(seed);
        }
        @Override
        protected Long convert(final Long seed, final int size) {
            return seed;
        }
        @Override
        protected Long convert(final int[] seed, final int size) {
            return INT_TO_LONG.convert(INT_ARRAY_TO_INT.convert(seed));
        }
        @Override
        protected Long convert(final long[] seed, final int size) {
            return LONG_ARRAY_TO_LONG.convert(seed);
        }
        @Override
        protected Long convert(final byte[] seed, final int size) {
            return LONG_ARRAY_TO_LONG.convert(BYTE_ARRAY_TO_LONG_ARRAY.convert(seed));
        }
    },
    /** The seed type is {@code int[]}. */
    INT_ARRAY(int[].class, 4) {
        @Override
        public int[] createSeed(final int size) {
            // Limit the number of calls to the synchronized method. The generator
            // will support self-seeding.
            return SeedFactory.createIntArray(Math.min(size, RANDOM_SEED_ARRAY_SIZE));
        }
        @Override
        protected int[] convert(final Integer seed, final int size) {
            return LONG_TO_INT_ARRAY.convert(INT_TO_LONG.convert(seed), size);
        }
        @Override
        protected int[] convert(final Long seed, final int size) {
            return LONG_TO_INT_ARRAY.convert(seed, size);
        }
        @Override
        protected int[] convert(final int[] seed, final int size) {
            return seed;
        }
        @Override
        protected int[] convert(final long[] seed, final int size) {
            return LONG_ARRAY_TO_INT_ARRAY.convert(seed);
        }
        @Override
        protected int[] convert(final byte[] seed, final int size) {
            return BYTE_ARRAY_TO_INT_ARRAY.convert(seed);
        }
    },
    /** The seed type is {@code long[]}. */
    LONG_ARRAY(long[].class, 8) {
        @Override
        public long[] createSeed(final int size) {
            // Limit the number of calls to the synchronized method. The generator
            // will support self-seeding.
            return SeedFactory.createLongArray(Math.min(size, RANDOM_SEED_ARRAY_SIZE));
        }
        @Override
        protected long[] convert(final Integer seed, final int size) {
            return LONG_TO_LONG_ARRAY.convert(INT_TO_LONG.convert(seed), size);
        }
        @Override
        protected long[] convert(final Long seed, final int size) {
            return LONG_TO_LONG_ARRAY.convert(seed, size);
        }
        @Override
        protected long[] convert(final int[] seed, final int size) {
            return INT_ARRAY_TO_LONG_ARRAY.convert(seed);
        }
        @Override
        protected long[] convert(final long[] seed, final int size) {
            return seed;
        }
        @Override
        protected long[] convert(final byte[] seed, final int size) {
            return BYTE_ARRAY_TO_LONG_ARRAY.convert(seed);
        }
    };

    /** Error message for unrecognised seed types. */
    private static final String UNRECOGNISED_SEED = "Unrecognized seed type: ";
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
    NativeSeedType(final Class<?> type, final int bytes) {
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
    public abstract Object createSeed(int size);

    /**
     * Converts the input seed from any of the supported seed types to the native seed type.
     * If the output is an array the required size of the array can be specified.
     *
     * @param seed Input seed.
     * @param size The size of the output seed (array types only).
     * @return the native seed.
     * @throws UnsupportedOperationException if the {@code seed} type is invalid.
     */
    public Object convertSeed(final Object seed,
                              final int size) {
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

        throw new UnsupportedOperationException(UNRECOGNISED_SEED + seed);
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
    public static byte[] convertSeedToBytes(final Object seed) {
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

        throw new UnsupportedOperationException(UNRECOGNISED_SEED + seed);
    }
}
