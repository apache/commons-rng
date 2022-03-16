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

/**
 * Creates an {@code int[]} from a {@code long[]}.
 *
 * <p>Note: From version 1.5 this conversion uses the long bytes in little-endian order.
 * The output {@code int[]} can be converted back using {@link IntArray2LongArray}.
 *
 * @since 1.0
 */
public class LongArray2IntArray implements Seed2ArrayConverter<long[], int[]> {
    /** {@inheritDoc} */
    @Override
    public int[] convert(long[] seed) {
        // Full length conversion
        return convertSeed(seed, SeedUtils.intSizeFromLongSize(seed.length));
    }

    /**
     * {@inheritDoc}
     *
     * @since 1.5
     */
    @Override
    public int[] convert(long[] seed, int outputSize) {
        return convertSeed(seed, outputSize);
    }

    /**
     * Creates an array of {@code int} values from a sequence of bytes. The integers are
     * filled in little-endian order (least significant byte first).
     *
     * @param input Input bytes
     * @param length Output length
     * @return an array of {@code int}.
     */
    private static int[] convertSeed(long[] input, int length) {
        final int[] output = new int[length];

        // Overflow-safe minimum using long
        final int n = (int) Math.min(input.length * 2L, length);
        // Little-endian fill
        // Alternate low/high 32-bits from each long
        for (int i = 0; i < n; i++) {
            // i              = int index
            // i >> 1         = long index
            // i & 0x1        = int number in the long  [0, 1]
            // (i & 0x1) << 5 = little-endian long shift to the int {0, 32}
            output[i] = (int)((input[i >> 1]) >>> ((i & 0x1) << 5));
        }

        return output;
    }
}
