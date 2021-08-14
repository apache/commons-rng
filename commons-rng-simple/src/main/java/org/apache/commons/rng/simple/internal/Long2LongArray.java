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

/**
 * Uses a {@code Long} value to seed a {@link SplitMix64} RNG and
 * create a {@code long[]} with the requested number of random
 * values.
 *
 * @since 1.0
 */
public class Long2LongArray implements Seed2ArrayConverter<Long, long[]> {
    /** Size of the output array. */
    private final int size;

    /**
     * @param size Size of the output array.
     */
    public Long2LongArray(final int size) {
        this.size = size;
    }


    /** {@inheritDoc} */
    @Override
    public long[] convert(final Long seed) {
        return convertSeed(seed, size);
    }

    /**
     * {@inheritDoc}
     *
     * @since 1.3
     */
    @Override
    public long[] convert(final Long seed, final int outputSize) {
        return convertSeed(seed, outputSize);
    }

    /**
     * Convert the seed.
     *
     * @param seed Input seed.
     * @param size Output array size.
     * @return the converted seed.
     */
    private static long[] convertSeed(final Long seed, final int size) {
        final long[] out = new long[size];
        final SplitMix64 rng = new SplitMix64(seed);
        for (int i = 0; i < size; i++) {
            out[i] = rng.nextLong();
        }

        return out;
    }
}
