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
 * Creates a {@code int[]} from a {@code byte[]}.
 *
 * @since 1.0
 */
public class ByteArray2IntArray implements Seed2ArrayConverter<byte[], int[]> {
    /** {@inheritDoc} */
    @Override
    public int[] convert(byte[] seed) {
        // Full length conversion
        return convertSeed(seed, SeedUtils.intSizeFromByteSize(seed.length));
    }

    /**
     * {@inheritDoc}
     *
     * @since 1.5
     */
    @Override
    public int[] convert(byte[] seed, int outputSize) {
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
    private static int[] convertSeed(byte[] input, int length) {
        final int[] output = new int[length];

        // Overflow-safe minimum using long
        final int n = (int) Math.min(input.length, length * (long) Integer.BYTES);
        // Little-endian fill
        for (int i = 0; i < n; i++) {
            // i              = byte index
            // i >> 2         = integer index
            // i & 0x3        = byte number in the integer  [0, 3]
            // (i & 0x3) << 3 = little-endian byte shift to the integer {0, 8, 16, 24}
            output[i >> 2] |= (input[i] & 0xff) << ((i & 0x3) << 3);
        }

        return output;
    }
}
